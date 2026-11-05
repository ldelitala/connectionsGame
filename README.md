# Relazione Progetto: Connections
**Studente:** Lorenzo Delitala
**Corso:** Reti e Laboratorio III  
**A.A.:** 2025/2026

---

## 1. Premesse e Metodologia
Per questo progetto ho deciso di adottare un approccio moderno: ho agito come **Software Architect e Senior Developer**, avvalendomi di **Gemini CLI** come assistente per la stesura materiale del codice. 

Voglio essere molto trasparente su questo: ho stabilito io l'architettura, i contratti di comunicazione e le interfacce. Ho dato ordini precisi all'IA e ho revisionato ogni singola funzione per assicurarmi che la logica fosse corretta. Certo, non tutto è perfetto: ho concentrato quasi tutto il mio tempo sulla robustezza della rete e del protocollo. Questo ha fatto sì che quando sono arrivato all'implementazione interna dell'Engine e dei DAO (che risultano a tratti un po' densi e "sporchi" di dipendenze) avrei voluto rifinirli meglio, ma avevo finito il mio tempo a disposizione. 

Ah, confesso una cosuccia: il rendering dell'output del client (colori, box, tabelle) è stato invece delegato quasi interamente all'IA senza revisione. Odio profondamente il frontend e ho preferito che si occupasse lei di rendere "carina" la CLI mentre io mi dedicavo ai veri problemi di concorrenza sul server.

---

## 2. Guida alla Lettura (Il "Cuore" del Progetto)
Se dovessi dare un consiglio su come leggere il mio codice, direi di andare dritti alla cartella `common`, nel file **`Operation.java`**. 

Per me, questo file è il contratto fondamentale del progetto. Siccome le specifiche JSON erano la parte più rigida della consegna, ho costruito tutto intorno a esse. In `Operation.java` ho definito ogni comando (login, register, guess, ecc.) associando a ciascuno di essi la sua classe di richiesta* e il relativo DTO di risposta. In questo modo sia client che server sanno cosa devono mandare e cosa devono ricevere.

Ho diviso le operazioni in tre macro-categorie:
1.  **User Management** (operazioni di utenza)
2.  **Gameplay** (operazioni sulla partita attiva)
3.  **History & Stats** (operazioni per il passato e le classifiche)

Una volta stabilito questo "contratto" tramite enum, il resto del progetto è venuto da sé come una conseguenza logica. Se capite come funziona `Operation.java`, avete capito quasi tutto il sistema.

* ogni macro-categoria di richiesta ha una classe specifica che offre factory methods per creare gli oggetti nell'esatto formato specificato dal progetto. In questo modo una volta scritte le tre classi non mi sono più dovuto preoccupare di guardare i formati dell'allegato.

---

## 3. Architettura e Concorrenza
Il sistema è un classico **MVC distribuito**. Il Client fa da View/Controller, il Server fa da Model e gestore dello stato.

### Il Client: Architettura Modulare
Il Client è stato progettato con una netta separazione tra la logica di visualizzazione (Frontend) e il "motore" applicativo (Backend). 

*   **Frontend (cartella `ui`):** La visualizzazione è completamente astratta. I file di interfaccia definiscono i contratti per l'input e l'output, mentre nella sottocartella `console` si trovano le implementazioni specifiche per la riga di comando. Questo design "a plugin" permetterebbe di sostituire la CLI con una GUI senza toccare una riga di logica di gioco.
*   **Engine (cartella `engine`):** È il vero cuore del client. Implementa l'interfaccia di ascolto dell'input (`InputListener`) e gestisce il flusso dell'applicazione. L'Engine riceve i comandi dall'utente, decide come "parlare" al server tramite il protocollo JSON e, una volta ricevuta la risposta, decide cosa mostrare alla view. Gestisce inoltre lo stato interno dell'utente tramite un **Pattern State** (`LoggedOut` vs `LoggedIn`).
*   **Network (cartella `network`):** Questo modulo si occupa esclusivamente del "trasporto". È stato mantenuto il più semplice possibile: implementa il `NetworkGateway` per la comunicazione TCP (invio/ricezione stringhe) e il `NotificationService` per restare in ascolto dei pacchetti UDP asincroni inviati dal server. 

In sintesi, l'Engine agisce come un direttore d'orchestra che coordina i polmoni (Network) e la voce (UI) del programma.

### Il Server e la Gestione dei Thread
La parte di cui vado più fiero è il **`NioServer`**. Ho implementato un ciclo `select` che non dorme mai. Ma la vera sfida è stata la concorrenza:
*   **Worker Pool:** Non potevo permettere che una lettura lenta su un socket bloccasse tutti gli altri. Così, il thread del Selector si occupa solo di accettare i dati e li "lancia" subito a un pool di worker. 
*   **ConcurrentHashMap:** Ho usato queste mappe ovunque per gestire le sessioni e gli utenti online. Mi hanno salvato la vita evitandomi di impazzire con i blocchi manuali ogni volta che qualcuno si connetteva o disconnetteva.
*   **Sincronizzazione in PuzzleCoordinator:** In questo modulo risiede la logica più sensibile e ammetto che la sincronizzazione attuale è piuttosto "conservativa". **L'ideale sarebbe stato utilizzare un meccanismo a due livelli (Fine-grained locking)**: un primo livello per garantire la mutua esclusione tra la sottomissione delle proposte e le fasi di transizione della partita (`load` e `finish`), e un secondo livello, più granulare, sincronizzato sullo **specifico username** dell'utente. Questo permetterebbe a centinaia di utenti di giocare contemporaneamente senza bloccarsi a vicenda, proteggendo al contempo le risorse del singolo giocatore. **Tuttavia, per semplicità e per garantire l'assoluta stabilità del prototipo nei tempi di consegna, in questo progetto ho optato per un blocco `synchronized` sull'intero oggetto**. Sebbene questo limiti leggermente il parallelismo puro, elimina alla radice ogni possibile race condition durante le fasi critiche del gioco.**
*   **Volatile per il Countdown:** Il passaggio allo stato di "attesa" tra una partita e l'altra usa un flag `volatile`. Questo assicura che quando il thread dello scheduler decide che la partita è finita, tutti i worker se ne accorgano all'istante e smettano di accettare proposte.

---

## 4. Persistenza e Ottimizzazioni (Espanso)
Si richiedeva di gestire i file come se fossero enormi. Caricare un file da 1GB in RAM per cercare un utente non era un'opzione.

### Il trucco dei Byte-Offset (PuzzlesDAO)
Per il database dei puzzle (`Connections_Data.json`), ho implementato un sistema di indicizzazione "pigro". All'avvio, il server legge il file una volta sola, ma non salva i testi. Salva solo la **posizione (l'offset in byte)** di dove inizia ogni puzzle. 
In questo modo, se devo caricare il puzzle numero 800, faccio una `seek()` istantanea a quel byte e leggo solo quell'oggetto JSON. È veloce, non occupa troppa memoria e scala parecchio.

### La strategia Append-Only (Users e History)
Riscrivere l'intero file degli utenti ogni volta che qualcuno vince una partita sarebbe stato un suicidio in termini di performance e sicurezza dei dati. Ho scelto la strategia **Append-Only**: 
*   Ogni "salvataggio" aggiunge semplicemente una riga in fondo al file. 
*   All'avvio, il server legge tutto dall'inizio alla fine. L'ultima versione incontrata di un utente è quella che conta. 
Questo rende le scritture velocissime e, in caso di crash del server a metà scrittura, al massimo perdo l'ultimo aggiornamento, ma tutto lo storico precedente resta intatto.

### JsonPersistence Utility
Per evitare di ripetere lo stesso codice in ogni DAO, ho creato una piccola utility `JsonPersistence`. Si occupa lei di configurare GSON e di assicurarsi che le cartelle esistano. È una piccolezza, ma ha reso il codice molto più pulito.

---

## 5. UI e Robustezza Finale
*   **UDP Unicast:** Ho preferito l'Unicast al Multicast (che per il nuovo ordinamento era opzionale). Mi permette di mandare riepiloghi personali a fine partita senza che gli altri "sbircino" i risultati altrui.
*   **Estetica CLI:** Ho usato i box Unicode perché vedere le parole in una griglia ordinata cambia completamente l'esperienza di gioco. I colori riflettono la difficoltà del gioco originale del NYT.
*   **Graceful Shutdown:** Ho aggiunto degli Shutdown Hook sia sul server che sul client. Se premi `Ctrl+C`, il sistema non "muore" male: chiude i canali, salva i dati pendenti e, lato client, prova a fare un logout pulito per avvisare il server.

---

## 6. Conclusioni
Il progetto è stato un esercizio incredibile di design del protocollo. Anche se alcune parti interne del server sono venute un po' confuse per via della velocità di sviluppo, sono estremamente soddisfatto della stabilità della rete e della logica di comunicazione. Il sistema regge bene lo stress, gestisce correttamente la persistenza e offre un'interfaccia utente che, per essere una CLI, è quasi divertente da usare.

---

## 7. Istruzioni per Compilazione ed Esecuzione
Per compilare ed eseguire il progetto da terminale (senza Maven), assicurarsi di essere nella cartella radice del progetto e che la libreria `gson.jar` sia presente in `libs/`.

### Compilazione
```bash
# Crea la cartella per i file compilati
mkdir -p bin

# Trova tutti i file java nella cartella main e compilali
find src/main/java -name "*.java" > sources.txt
javac -d bin -cp "libs/gson.jar" @sources.txt
rm sources.txt
```

### Esecuzione (senza JAR)
Dopo aver compilato:
*   **Server:** `java -cp "bin:libs/gson.jar" it.unipi.connections.server.ServerMain`
*   **Client:** `java -cp "bin:libs/gson.jar" it.unipi.connections.client.ClientMain`

### Esecuzione (tramite JAR)
Se si desidera utilizzare i file JAR pre-compilati:
*   **Server:** `java -jar Server.jar`
*   **Client:** `java -jar Client.jar`
