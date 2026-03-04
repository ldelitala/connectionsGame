# Progetto Connections - TODO List

Elenco dei miglioramenti da implementare per completare il progetto.

## 1. Refactoring Modulare (Client & Protocollo)
- [ ] **Riorganizzazione Protocollo (Common)**:
    - [ ] Rinominare `Operation.java` per coerenza (`GET_GAME_INFO`, `GET_GAME_STATS`, ecc.).
    - [ ] Aggiornare `GameInfoDTO` e `GameStatsDTO` per gestire rigorosamente le differenze tra partita "in corso" e "conclusa" come da specifica.
    - [ ] Dividere le classi `Request` in `UserRequest`, `GameRequest` e `HistoryRequest`.
- [ ] **Nuova Architettura Client**:
    - [ ] Implementare `ClientController` (orchestratore centrale del loop di comando).
    - [ ] Implementare `UserModule`: gestione `register`, `login`, `logout`.
    - [ ] Implementare `GameModule`: gestione `submit` (proposta), `info` (corrente), `stats` (corrente).
    - [ ] Implementare `HistoryModule`: gestione `info <id>`, `stats <id>`, `ranking`, `history`.
- [ ] **Miglioramento CLI**:
    - [ ] Spostare la logica di visualizzazione (formattazione DTO) all'interno dei rispettivi Moduli.
    - [ ] Gestire la visualizzazione asincrona delle notifiche UDP nel `ClientController`.

## 2. Persistenza Avanzata
- [ ] Creare un file `data/games_history.json` per salvare i risultati delle partite passate.
- [ ] Implementare il caricamento della cronologia all'avvio del server per rendere le statistiche globali persistenti ai riavvii.

## 3. Ottimizzazioni di Rete
- [ ] Valutare l'uso di un `ExecutorService` dedicato per l'invio delle notifiche UDP in `GameSessionManager` per evitare blocchi nel thread di gestione sessione.
- [ ] Implementare un meccanismo di heartbeat o timeout della sessione per rimuovere utenti inattivi.

## Completati (Recenti)
- [x] Refactoring Architetturale: Separazione tra Domain Models, DTOs e Protocolli.
- [x] Sistema di Risposte TCP Type-Safe tramite Generics e interfacce Marker.
- [x] Notifiche asincrone UDP per inizio partita e riepilogo finale (global/personal).
- [x] Validazione flessibile delle regole di gioco (spostata sul server).
- [x] Suite di test completa (17/17 passati).
