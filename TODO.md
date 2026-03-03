# Progetto Connections - TODO List

Elenco dei miglioramenti da implementare per completare il progetto.

## 1. Persistenza Avanzata
- [ ] Creare un file `data/games_history.json` per salvare i risultati delle partite passate.
- [ ] Implementare il caricamento della cronologia all'avvio del server per rendere le statistiche globali persistenti ai riavvii.

## 2. Miglioramento CLI Client
- [ ] Separare la **Logica di Input** dalla **Logica di Visualizzazione** in `ClientMain`.
- [ ] Implementare una gestione elegante delle notifiche asincrone che arrivano mentre l'utente scrive (es. ristampare il prompt).
- [ ] Formattare i DTO ricevuti (es. tabelle per la classifica, colori per i gruppi di parole).

## 3. Ottimizzazioni di Rete
- [ ] Valutare l'uso di un `ExecutorService` dedicato per l'invio delle notifiche UDP in `GameSessionManager` per evitare blocchi nel thread di gestione sessione.
- [ ] Implementare un meccanismo di heartbeat o timeout della sessione per rimuovere utenti inattivi.

## Completati (Recenti)
- [x] Refactoring Architetturale: Separazione tra Domain Models, DTOs e Protocolli.
- [x] Sistema di Risposte TCP Type-Safe tramite Generics e interfacce Marker.
- [x] Notifiche asincrone UDP per inizio partita e riepilogo finale (global/personal).
- [x] Validazione flessibile delle regole di gioco (spostata sul server).
- [x] Suite di test completa (17/17 passati).
