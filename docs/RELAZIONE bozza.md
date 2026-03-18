# Premesse
Per trasparenza dichiaro che per sviluppare il progetto mi sono servito di Gemini CLI. Trovo che abituarsi ad usare queste tool sarà vitale per sopravvivere nell'ambiente di lavoro in futuro. Detto ciò io mi sono occupato di fare da software developer senior ovvero ho stabilito le interfacce e le strutture ed ho usato gemini cli per scrivere il codice secondo i miei specifici ordini. Tutto il codice è stato letto e riletto e capito a fondo. Gli unici componenti che sono stati costruiti senza la mia estrema supervisione sono l'engine e i dao del server: ho stabilito io la struttura con in coordinatori e i dao ma quando li ha implementati ha creato del codice difficile da leggere e con una marea di dipendenze e mi sarebbe piaciuto migliorarlo ma purtroppo avevo ormai finito il tempo a disposizione per questo progetto quindi mi sono solo assicurato che funzionasse tramite testing. Mi scuso quindi per la confusione di quelle classi. Ah anche il rendering dell'output del client è scritto da gemini senza la mia revisione ma perché odio il frontend. 

# Consigli sulla lettura del progetto
Siccome la parte più dettagliatamente specificata dal progetto erano i file json della comunicazione ho costruito tutto il progetto intorno a quello e ho cercato di far si che il protocollo fosse implementato nel modo migliore possibile. Consiglio quindi se si vuole capire il progetto di iniziare a leggere dalla cartella common e in particolare dal file più importante di tutto il progetto: operation.java. Qui infatti mi sono occupato di scrivere il "contratto" delle richieste e risposte tra client e server rispecchiando i campi operation specificati dal progetto. Ogni operazione specifica il tipo di oggetto di richiesta ed eventuali data tranfer object che il server deve fornire in risposta. Da qui ho notato che le operazioni erano di 3 tipi: usermanagment history&stats e gameplay così ho diviso gli enum di conseguenza e ho creato 3 sottoclassi di clientserver che le implementassero facendo in modo che avessero factory method per creare oggetti che rispecchiassero esattamente il protocollo richiesto. Dopo ho studiato ciò che il progetto chiedeva di far ricevere al client dal server come informazioni e ho creato i vari dto. Una volta capito questa parte del progetto è facile il resto l'ho scritto di conseguenza.

# L'architettura generale
In commmon c'è il protocollo di comunicazione le notifiche e un modello che faceva comodo fosse in comune. I client server insieme formano una architettura model view controller: il client implementa il view controller mentre il server il model. 

## Il client
Il client ha quindi una ui dove ho primastabilito i contratti di input e di output e dopo implementato input e output per la console. Questo perché farò generare all'ia una bella interfaccia grafica per far giocare il gioco ai miei amici sul mio server di casa. Il networkgateway implementa la comunicazione tcp in modo decontastualizzato (manda stringhe e riceve stringhe senza sapere nulla del progetto o della comunicazione) e ho implementato la logica di serializzazione e deserializzazione nel game engine (che fondamentalmente non è un vero game engine ma un input listener che fa anche da bridge tanto c'era poca roba di bridge da fare) Il game engine ha due stati loggedin e loggedout perché mi sono reso conto che a seconda di qeusto stato alcuni comandi non avrebbero dovuto funzionare. Tutto quello che ho descritto ora gira sul main thread in modo sequenziale: il provider si ferma ad ascoltare -> il game engine riceve l'input -> prepara la richiesta -> la manda al server -> aspetta risposta -> riceve risposta -> visualizza risposta -> torna al provider. In caso di qualsiasi malfunzionamento torna su clientmain e chiude. Invece la classe notificationservice gira su un thread deamon a parte e quando gli arrivano notifiche le manda in rendering sulla view (quindi è la view che eventualmente può coordinare come vengono visualizzate le cose)

## Server
La parte secondo me più bella e pulita del server è la componente della connessione. Qui infatti si implementa il cuore della comunicazione tcp nel nioserver. Il nio server implementa un ciclo select con accept read e write non bloccanti molto veloci. Contiene un threadpool di workers di taglia fissa configurabile nel file di configurazione e ogni volta che arriva una richiesta la da ai workers "grezza" attraverso un bridge che traduce la stringa nella clientrequest appropriata prima di mandarla all'engine e poi quando riceve la risposta dell'engine la serializza e la ridà al server da inviare. L'engine poi non è altro che un dispatcher o coordinatore che chiama i giusti moduli (che avevo chiamato coordinatori ma mi rendo conto adesso sarebbe stato più appropriato moduli). I moduli rispondo o grazie alle loro strutture dati ram condivise e sincronizzate  oppure con l'accesso sincronizzato ai file tramite i dao. Infine infatti abbiamo i dao che implementano la logica di gestione e sincronizzazione in lettura e scrittura dei file.

## 1. Scelte Progettuali e Interpretazioni
*   **Protocollo tcp non bloccante lato server**
* protocollo nio bloccante lato client
*   **UDP Unicast (Nuovo Ordinamento):** Per le notifiche asincrone, abbiamo implementato un sistema Unicast basato su registrazione. Al login, il client comunica la sua porta UDP e il server mantiene una mappa delle sessioni attive. Questo ha permesso di inviare sia notifiche globali (nuovo gioco) che riepiloghi **personali** di fine partita in modo sicuro.
*   **Gestione del "Wait State":** Tra una partita e l'altra abbiamo introdotto un tempo di attesa configurabile. Durante questo tempo, il server invia notifiche UDP ogni secondo con un countdown, migliorando l'esperienza utente e prevenendo richieste a vuoto.
*   **Protocollo JSON Strutturato:** Abbiamo usato `Gson` per la serializzazione. Il protocollo è stato reso robusto tramite un sistema di "disambiguazione" (`Operation.resolve`) che distingue tra richieste di partite correnti o passate in base alla presenza del campo `gameId`.

## 2. Schema dei Thread
### Lato Server

*   **Main Thread (Selector Thread):** Dopo la fase di inizializzazione, il thread principale entra nel loop del `Selector` di Java NIO. Gestisce l'accettazione delle connessioni, la lettura e la scrittura asincrona sui socket senza mai bloccarsi sulla logica di business.
*   **Worker Pool (ThreadPoolExecutor):** Una pool di thread fissi (configurabile) che processa la serializzazione/deserializzazione della comunicazione e la logica di business (login, proposte, statistiche) per non bloccare il selettore.
*   **Scheduler Thread:** Un `ScheduledExecutorService` dedicato a gestire il ciclo di vita dei puzzle (fine tempo e countdown tra round).
*   **Shutdown Hook Thread:** Gestisce la chiusura pulita delle risorse alla ricezione di un segnale di interrupt (Ctrl+C).

### Lato Client
*   **Main Thread:** Gestisce l'input dell'utente e il ciclo principale del gioco.
*   **Notification Thread:** Un thread separato che resta in ascolto costante sul socket UDP per ricevere e visualizzare le notifiche del server in tempo reale senza interrompere l'input dell'utente.
* *   **Shutdown Hook Thread:** Gestisce la chiusura pulita delle risorse alla ricezione di un segnale di interrupt (Ctrl+C).

## 3. Strutture Dati e Sincronizzazione
*   **ConcurrentHashMap:** Utilizzata sia per mappare gli utenti online (`UserCoordinator`) sia per gestire gli offset dei file e le sessioni nel server. Garantisce thread-safety senza blocchi pesanti.
*   **Atomic Logic / Synchronized:** I metodi critici nei DAO e nel `PuzzleCoordinator` (come la sottomissione di una proposta o l'aggiornamento dell'indice dei puzzle) sono stati sincronizzati per evitare race conditions tra i worker thread.
*   **Append-Only Persistence:** Per la persistenza degli utenti e dei log, abbiamo scelto una strategia "append-only". Invece di riscrivere l'intero file JSON (rischioso e lento), aggiungiamo una nuova riga JSON. Durante il caricamento, l'ultima versione di un record sovrascrive le precedenti, garantendo velocità e integrità dei dati.

## 4. Persistenza
*   `users.json`: Contiene le anagrafiche e le statistiche "career" degli utenti.
*   `games_log.json`: Uno storico dettagliato di ogni partita conclusa.
*   `server_state.json`: Salva l'indice dell'ultimo puzzle servito, permettendo al server di riprendere la rotazione corretta dopo un riavvio.

## 5. Istruzioni per Compilazione ed Esecuzione (da Terminale)

### Compilazione
Dalla cartella radice del progetto:
```bash
# Crea la cartella per i file compilati
mkdir -p bin

# Trova tutti i file java e compilali
find src/main/java -name "*.java" > sources.txt
javac -d bin -cp "libs/gson.jar" @sources.txt
rm sources.txt
```

### Esecuzione (senza JAR)
Dopo aver compilato come sopra:
*   **Server:** `java -cp "bin:libs/gson.jar" com.connections.server.ServerMain`
*   **Client:** `java -cp "bin:libs/gson.jar" com.connections.client.ClientMain`

### Esecuzione (con JAR)
Se hai già creato i file JAR:
*   **Server:** `java -jar Server.jar`
*   **Client:** `java -jar Client.jar`
