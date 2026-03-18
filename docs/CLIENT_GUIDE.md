# Connections CLI Client - Manuale Utente e Architettura

Questo documento descrive i comandi disponibili nel client CLI di Connections, la loro corrispondenza con il protocollo ufficiale del progetto e le scelte architettoniche effettuate.

## 1. Comandi CLI e Corrispondenza Protocollo

Il client implementa un set di comandi semplificato per migliorare l'esperienza utente (UX) e permettere una modularizzazione pulita del codice. Di seguito la mappatura tra i comandi digitati dall'utente e le operazioni JSON inviate al server (come richiesto dall'Allegato 1).

### Gestione Account
| Comando CLI | Operazione Protocollo | Descrizione |
|-------------|-----------------------|-------------|
| `register [user] [pass]` | `register` | Registra un nuovo utente. |
| `login [user] [pass]` | `login` | Autentica l'utente e avvia la sessione. |
| `logout` | `logout` | Termina la sessione corrente. |
| `update` | `updateCredentials` | Cambia username o password (richiede parametri interattivi). |

### Partita in Corso (Current Game)
Questi comandi si riferiscono implicitamente alla partita globale attualmente attiva sul server.
| Comando CLI | Operazione Protocollo | Parametri JSON | Descrizione |
|-------------|-----------------------|----------------|-------------|
| `submit [w1-w4]` | `submitProposal` | `words: [...]` | Invia una proposta di 4 parole. |
| `status` | `requestGameInfo` | `gameId: null` | Visualizza errori, tempo e parole rimaste. |
| `stats` | `requestGameStats` | `gameId: null` | Statistiche globali di partecipazione alla partita corrente. |

### Storico e Statistiche (Past/Global)
Questi comandi richiedono esplicitamente un ID o mostrano dati globali persistenti.
| Comando CLI | Operazione Protocollo | Parametri JSON | Descrizione |
|-------------|-----------------------|----------------|-------------|
| `info [id]` | `requestGameInfo` | `gameId: INT` | Mostra la soluzione e l'esito di una partita passata. |
| `global [id]` | `requestGameStats` | `gameId: INT` | Statistiche di partecipazione di una partita passata. |
| `ranking [k] [user]` | `requestLeaderboard` | `topPlayers: K, playerName: user` | Classifica globale. |
| `history` | `requestPlayerStats` | - | Riepilogo delle tue statistiche personali globali. |

---

## 2. Razionale del Design

### Libertà dei Nomi vs Protocollo
Sebbene il protocollo di rete utilizzi nomi operazione come `requestGameInfo` per scopi diversi (partita in corso vs passata), nel client abbiamo scelto di distinguere questi casi tramite comandi diversi (`status` vs `info [id]`).

**Vantaggi di questa scelta:**
1. **Modularizzazione del Codice**: Lo `switch` nel `ClientController` è diventato lineare e privo di logiche condizionali complesse basate sul numero di argomenti. Ogni comando mappa direttamente a un metodo specifico di un modulo.
2. **Chiarezza per l'Utente**: L'utente percepisce chiaramente la differenza tra guardare "come sta andando ora" (`status`) e "come è andata quella volta" (`info 42`).
3. **Robustezza**: Riduce il rischio di inviare ID non validi o nulli per errore, poiché la logica di creazione della richiesta è separata alla base.

### Architettura Modulare
Il client è diviso in tre moduli principali coordinati da un `ClientController`:
- **UserModule**: Gestisce l'identità e la sessione.
- **GameModule**: Gestisce l'interazione con la partita attiva.
- **HistoryModule**: Gestisce l'accesso ai dati storici e globali.

Questa struttura rende il client facilmente espandibile (es. aggiunta di nuove viste o statistiche) senza dover modificare il nucleo della gestione della connessione TCP.
