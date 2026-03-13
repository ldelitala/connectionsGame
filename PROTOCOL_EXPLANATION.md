# Protocollo di Comunicazione - Connections Game

Questo documento descrive l'implementazione del protocollo di comunicazione per il progetto Connections 2025/26. Il protocollo è progettato per essere **type-safe**, **centralizzato** e strettamente conforme alle specifiche dell'Allegato 1 del PDF di progetto.

## 1. Architettura Core

Il protocollo si basa su tre pilastri fondamentali situati nel pacchetto `com.connections.common.protocol`:

### Operation (L'Unica Fonte di Verità)
L'enum `Operation` è il cuore del sistema. Ogni costante dell'enum rappresenta un'azione valida nel protocollo e definisce:
- Il nome dell'operazione nel JSON (`@SerializedName`).
- La classe Java per la richiesta (`ClientRequest`).
- La classe Java per la risposta (`ServerResponse`).
- L'eventuale oggetto dati contenuto nella risposta (`DTO`).

### Risoluzione delle Ambiguità
Il protocollo richiede che alcune operazioni condividano lo stesso nome (es. `requestGameInfo`). Il sistema risolve automaticamente l'ambiguità tramite il metodo `Operation.resolve()`:
- Se `gameId` è presente -> L'operazione viene risolta come `GET_PAST_PUZZLE_INFO`.
- Se `gameId` è assente -> L'operazione viene risolta come `GET_PUZZLE_INFO` (partita corrente).

## 2. Struttura dei Messaggi (TCP)

Tutte le comunicazioni avvengono tramite JSON su connessioni TCP gestite in modalità NIO (Non-blocking I/O).

### Richieste (Client -> Server)
Ogni richiesta eredita da `ClientRequest` e include il campo `operation`.
Esempio di registrazione:
```json
{
  "operation": "register",
  "name": "nome_utente",
  "psw": "password123"
}
```

### Risposte (Server -> Client)
Le risposte sono incapsulate in un oggetto `ServerResponse<T>` generico:
- **Operation**: L'operazione a cui la risposta fa riferimento (es. `register`, `login`).
- **Status**: `ok` o `error`.
- **Message**: Descrizione dell'esito o del tipo di errore.
- **Data**: (Opzionale) Un oggetto `DTO` (Data Transfer Object) contenente i dati richiesti (es. statistiche, classifica, stato della partita).

## 3. Notifiche Asincrone (UDP)

Per gli aggiornamenti in tempo reale che non richiedono una risposta immediata, il server utilizza il protocollo UDP tramite la classe `UdpNotifier`:
- **NewPuzzleNotification**: Inviata a tutti i client loggati all'inizio di ogni nuova partita.
- **PuzzleEndNotification**: Riepilogo globale inviato a fine partita.
- **PlayerPuzzleSummaryNotification**: Messaggio personalizzato per ogni giocatore con il proprio punteggio e statistiche.

## 4. Conformità alle Specifiche

Il sistema garantisce la piena compatibilità con l'Allegato 1 del progetto tramite:
- **Mapping dei Campi**: Uso di `@SerializedName` per mappare i campi Java (es. `username`) ai nomi JSON richiesti (es. `name` o `username` a seconda dell'operazione).
- **Consistenza**: L'intera logica di routing nel `RequestDispatcher` dipende esclusivamente dalle definizioni presenti in `Operation.java`.
- **Flessibilità**: Il sistema DTO permette di aggiungere nuovi campi alle risposte senza rompere la compatibilità con il parsing JSON di base.
