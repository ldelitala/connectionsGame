# Traccia per la Relazione del Progetto "Connections"

Questa traccia riassume le scelte tecniche e architettoniche fatte durante lo sviluppo, seguendo i punti richiesti dal PDF del progetto.

## 1. Scelte Progettuali e Interpretazioni
*   **Protocollo TCP over NIO:** Abbiamo scelto di usare Java NIO con `Selector` per gestire le connessioni TCP. Questo permette al server di scalare efficientemente gestendo più client con un unico thread di I/O, delegando la logica a una pool di thread separata.
*   **UDP Unicast (Nuovo Ordinamento):** Per le notifiche asincrone, abbiamo implementato un sistema Unicast basato su registrazione. Al login, il client comunica la sua porta UDP e il server mantiene una mappa delle sessioni attive. Questo ha permesso di inviare sia notifiche globali (nuovo gioco) che riepiloghi **personali** di fine partita in modo sicuro.
*   **Gestione del "Wait State":** Tra una partita e l'altra abbiamo introdotto un tempo di attesa configurabile. Durante questo tempo, il server invia notifiche UDP ogni secondo con un countdown, migliorando l'esperienza utente e prevenendo richieste a vuoto.
*   **Protocollo JSON Strutturato:** Abbiamo usato `Gson` per la serializzazione. Il protocollo è stato reso robusto tramite un sistema di "disambiguazione" (`Operation.resolve`) che distingue tra richieste di partite correnti o passate in base alla presenza del campo `gameId`.

## 2. Schema dei Thread
### Lato Server
*   **Main Thread (Selector Thread):** Dopo la fase di inizializzazione, il thread principale entra nel loop del `Selector` di Java NIO. Gestisce l'accettazione delle connessioni, la lettura e la scrittura asincrona sui socket senza mai bloccarsi sulla logica di business.
*   **Worker Pool (ThreadPoolExecutor):** Una pool di thread fissi (configurabile) che processa la logica di business (login, proposte, statistiche) scaricando il lavoro dal Main thread.
*   **Scheduler Thread:** Un `ScheduledExecutorService` dedicato a gestire il ciclo di vita dei puzzle (fine tempo e countdown tra round).
*   **Shutdown Hook Thread:** Un thread di sistema che gestisce la chiusura pulita delle risorse alla ricezione di un segnale di interrupt (Ctrl+C).

### Lato Client
*   **Main Thread:** Gestisce l'input dell'utente e il ciclo principale del gioco.
*   **Notification Thread:** Un thread separato che resta in ascolto costante sul socket UDP per ricevere e visualizzare le notifiche del server in tempo reale senza interrompere l'input dell'utente.

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

# Trova tutti i file java nella cartella main e compilali
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
