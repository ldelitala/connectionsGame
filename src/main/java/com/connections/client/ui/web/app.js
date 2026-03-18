let ws;
let currentUsername = "";
let selectedWords = [];
let maxMistakes = 4;
let currentMistakes = 0;

// DOM Elements
const authSection = document.getElementById('auth-section');
const gameSection = document.getElementById('game-section');
const waitSection = document.getElementById('wait-section');
const userInfo = document.getElementById('user-info');
const usernameDisplay = document.getElementById('username-display');
const board = document.getElementById('board');
const solvedGroupsContainer = document.getElementById('solved-groups-container');
const btnSubmit = document.getElementById('btn-submit');
const dotsContainer = document.querySelector('.dots');
const waitCountdown = document.getElementById('wait-countdown');

function connectWebSocket() {
    ws = new WebSocket("ws://localhost:8001");

    ws.onopen = () => {
        console.log("Connected to Bridge.");
    };

    ws.onmessage = (event) => {
        const data = JSON.parse(event.data);
        console.log("Received:", data);

        // Handle Notifications
        if (data.type) {
            handleNotification(data);
            return;
        }

        // Handle Responses
        if (data.status) {
            handleResponse(data);
        }
    };

    ws.onclose = () => {
        showToast("Connection lost. Reconnecting...");
        setTimeout(connectWebSocket, 3000);
    };
}

function sendRequest(req) {
    if (ws && ws.readyState === WebSocket.OPEN) {
        ws.send(JSON.stringify(req));
    } else {
        showToast("Not connected to server.");
    }
}

// --- Auth Handling ---
document.getElementById('btn-login').onclick = () => {
    const u = document.getElementById('username').value;
    const p = document.getElementById('password').value;
    sendRequest({ operation: "login", username: u, psw: p });
};

document.getElementById('btn-register').onclick = () => {
    const u = document.getElementById('username').value;
    const p = document.getElementById('password').value;
    sendRequest({ operation: "register", name: u, psw: p });
};

document.getElementById('btn-logout').onclick = () => {
    sendRequest({ operation: "logout" });
    showAuth();
};

// --- Game Actions ---
document.getElementById('btn-shuffle').onclick = () => {
    const items = Array.from(board.querySelectorAll('.word-item'));
    items.sort(() => Math.random() - 0.5);
    items.forEach(item => board.appendChild(item));
};

document.getElementById('btn-deselect').onclick = () => {
    selectedWords = [];
    document.querySelectorAll('.word-item.selected').forEach(el => el.classList.remove('selected'));
    updateSubmitButton();
};

btnSubmit.onclick = () => {
    if (selectedWords.length === 4) {
        sendRequest({ operation: "submitProposal", words: selectedWords });
    }
};

// --- UI Updates ---
function showAuth() {
    authSection.classList.remove('hidden');
    gameSection.classList.add('hidden');
    waitSection.classList.add('hidden');
    userInfo.classList.add('hidden');
    currentUsername = "";
}

function showGame(username) {
    currentUsername = username;
    usernameDisplay.textContent = username;
    authSection.classList.add('hidden');
    waitSection.classList.add('hidden');
    gameSection.classList.remove('hidden');
    userInfo.classList.remove('hidden');
    
    // Request board state
    sendRequest({ operation: "requestGameInfo" });
}

function showWait() {
    gameSection.classList.add('hidden');
    waitSection.classList.remove('hidden');
}

function showToast(msg) {
    const toast = document.getElementById('toast');
    toast.textContent = msg;
    toast.classList.remove('hidden');
    setTimeout(() => toast.classList.add('hidden'), 3000);
}

function renderBoard(remainingWords, foundGroups, mistakes) {
    board.innerHTML = "";
    solvedGroupsContainer.innerHTML = "";
    selectedWords = [];
    updateSubmitButton();

    currentMistakes = mistakes;
    updateMistakeDots();

    // Render Solved Groups
    if (foundGroups && foundGroups.length > 0) {
        foundGroups.forEach((group, index) => {
            const el = document.createElement('div');
            el.className = `solved-group level-${index}`;
            el.innerHTML = `<strong>${group}</strong>`;
            solvedGroupsContainer.appendChild(el);
        });
    }

    // Render Remaining Words
    if (remainingWords) {
        remainingWords.forEach(word => {
            const el = document.createElement('div');
            el.className = 'word-item';
            el.textContent = word;
            el.onclick = () => toggleWord(el, word);
            board.appendChild(el);
        });
    }
}

function toggleWord(el, word) {
    if (selectedWords.includes(word)) {
        selectedWords = selectedWords.filter(w => w !== word);
        el.classList.remove('selected');
    } else {
        if (selectedWords.length < 4) {
            selectedWords.push(word);
            el.classList.add('selected');
        }
    }
    updateSubmitButton();
}

function updateSubmitButton() {
    btnSubmit.disabled = selectedWords.length !== 4;
}

function updateMistakeDots() {
    dotsContainer.innerHTML = "";
    for (let i = 0; i < maxMistakes; i++) {
        const dot = document.createElement('span');
        if (i < currentMistakes) dot.className = 'lost';
        dotsContainer.appendChild(dot);
    }
}

// --- Handlers ---
function handleResponse(res) {
    if (res.status === "ERROR" || res.status === "FORBIDDEN") {
        showToast(res.message);
        if (res.message.includes("Login required")) showAuth();
        return;
    }

    const op = res.operation;
    if (op === "login") {
        showGame(document.getElementById('username').value);
    } else if (op === "requestGameInfo") {
        if (res.data) renderBoard(res.data.remainingWords, res.data.foundGroups, res.data.mistakes);
    } else if (op === "submitProposal") {
        if (res.data) {
            if (res.data.status === "success") {
                // Wait briefly, then re-request board to get clean state
                setTimeout(() => sendRequest({ operation: "requestGameInfo" }), 500);
            } else {
                showToast("One away..." || "Incorrect guess!");
                currentMistakes++;
                updateMistakeDots();
                document.getElementById('btn-deselect').click();
            }
        }
    }
}

function handleNotification(n) {
    if (n.type === "new_game") {
        showToast("A new puzzle has started!");
        showGame(currentUsername);
    } else if (n.type === "game_end") {
        showToast("The puzzle has ended!");
    } else if (n.type === "player_summary") {
        showWait();
    } else if (n.type === "wait_countdown") {
        showWait();
        waitCountdown.textContent = n.seconds;
    }
}

// Start
connectWebSocket();
