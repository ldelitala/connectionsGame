# Progetto Connections - TODO List

Elenco dei miglioramenti da implementare per raggiungere l'eccellenza architettonica e la robustezza richiesta dal progetto.

## 1. Persistenza della Cronologia Partite
Al momento la cronologia delle partite (`gameHistory` in `GameSessionManager`) è salvata solo in RAM.
- Creare un file `data/games_history.json`.
- Implementare il salvataggio dei risultati al termine di ogni partita (metodo `archiveCurrentGame`).
- Implementare il caricamento della cronologia all'avvio del server.
- Garantire che il server sia resiliente ai riavvii mantenendo le statistiche globali.

## 2. Classe di Costanti per il Protocollo
Centralizzare tutti i valori magici e le configurazioni di default in una classe `com.connections.common.ProtocolConstants`.
- Porta TCP di default (8080).
- Numero massimo di errori (4).
- Punteggi per gruppo indovinato (6, 12, 18).
- Penale per errore (-4).
- Timeout e nomi dei file JSON.

## 3. Pulizia e Refactoring del Client
`ClientMain` è attualmente troppo denso di responsabilità.
- Separare la **Logica di Input** (gestione dei comandi e loop principale) dalla **Logica di Visualizzazione**.
- Creare una classe `com.connections.client.view.ResponsePrinter` (o simile) per gestire la stampa formattata dei DTO (es. tabelle per la classifica, colori per i gruppi trovati).
- Migliorare l'esperienza utente nella visualizzazione dello stato della partita.
