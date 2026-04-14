<%@ page contentType="text/html;charset=UTF-8" %>
<!DOCTYPE html>
<html>

<head>
    <title>Cờ tướng - Human vs Human</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/xiangqiboard-0.3.3.min.css">
    <meta name="_csrf" content="${_csrf.token}">
    <meta name="_csrf_header" content="${_csrf.headerName}">
    <style>
        body {
            margin: 0;
            padding: 12px;
            font-family: sans-serif;
            display: flex;
            flex-direction: column;
            gap: 12px;
        }

        .toolbar {
            display: flex;
            flex-wrap: wrap;
            gap: 8px;
            align-items: center;
        }

        .toolbar input {
            padding: 6px 8px;
            min-width: 140px;
        }

        .toolbar button {
            padding: 6px 12px;
            cursor: pointer;
        }

        .top-right {
            position: fixed;
            top: 12px;
            right: 12px;
            z-index: 1000;
            display: flex;
            gap: 8px;
        }

        .status {
            min-height: 20px;
            font-size: 14px;
            color: #1f4d2b;
        }

        #myBoard {
            width: min(88vh, 100vw);
        }
    </style>
</head>

<body>
    <div class="toolbar">
        <input id="playerNameInput" placeholder="Player name" />
        <input id="roomIdInput" placeholder="Room ID" />
        <button id="createRoomBtn" type="button">Create room</button>
        <button id="joinRoomBtn" type="button">Join room</button>
        <button id="loadRoomBtn" type="button">Load room</button>
    </div>

    <div id="statusText" class="status">Create or join a room to start human-vs-human.</div>
    <div id="myBoard"></div>

    <div class="top-right">
        <a href="${pageContext.request.contextPath}/">Vs Computer</a>
        <a href="${pageContext.request.contextPath}/login">Login</a>
    </div>

    <script src="https://code.jquery.com/jquery-3.5.1.min.js"></script>
    <script src="${pageContext.request.contextPath}/js/xiangqiboard-0.3.3.min.js"></script>
    <script>
        const BASE = '${pageContext.request.contextPath}';

        const playerNameInput = document.getElementById('playerNameInput');
        const roomIdInput = document.getElementById('roomIdInput');
        const createRoomBtn = document.getElementById('createRoomBtn');
        const joinRoomBtn = document.getElementById('joinRoomBtn');
        const loadRoomBtn = document.getElementById('loadRoomBtn');
        const statusText = document.getElementById('statusText');

        const csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute('content') || '';
        const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content') || '';

        let currentMoveHistory = '';
        let activeRoomId = '';
        let activePlayerName = '';
        let latestRoomState = null;
        let moveInFlight = false;

        function apiHeaders() {
            const headers = {};
            if (csrfHeader && csrfToken) {
                headers[csrfHeader] = csrfToken;
            }
            return headers;
        }

        function setStatus(message, isError) {
            statusText.textContent = message;
            statusText.style.color = isError ? '#9a1f1f' : '#1f4d2b';
        }

        function encodeMoveForBoard(move) {
            if (!move || move.length < 4) {
                return null;
            }
            return move.substring(0, 2) + '-' + move.substring(2, 4);
        }

        function roomInfoText(state) {
            return 'Room ' + state.roomId +
                ' | Host: ' + (state.hostName || '-') +
                ' | Guest: ' + (state.guestName || '-') +
                ' | Turn: ' + state.turn +
                ' | Status: ' + state.status;
        }

        function syncUrl(roomId, playerName) {
            const url = new URL(window.location.href);
            if (roomId) {
                url.searchParams.set('roomId', roomId);
            } else {
                url.searchParams.delete('roomId');
            }

            if (playerName) {
                url.searchParams.set('playerName', playerName);
            } else {
                url.searchParams.delete('playerName');
            }
            window.history.replaceState({}, '', url.toString());
        }

        function renderFromState(state, animate) {
            latestRoomState = state;
            activeRoomId = state.roomId;
            roomIdInput.value = state.roomId;

            const moves = (state.moveHistory || '').trim().split(/\s+/).filter(Boolean);
            currentMoveHistory = moves.join(' ');

            board.position('start', false);
            for (const move of moves) {
                const mapped = encodeMoveForBoard(move);
                if (mapped) {
                    board.move(mapped, false);
                }
            }
            board.position(board.position(), animate);
            setStatus(roomInfoText(state), false);
            syncUrl(activeRoomId, activePlayerName);
        }

        function playerSide(state) {
            if (!activePlayerName) {
                return null;
            }
            if (state.hostName === activePlayerName) {
                return 'HOST';
            }
            if (state.guestName === activePlayerName) {
                return 'GUEST';
            }
            return null;
        }

        function canDrag(piece) {
            if (!latestRoomState) {
                return false;
            }
            const side = playerSide(latestRoomState);
            if (!side) {
                return false;
            }
            if (latestRoomState.status !== 'PLAYING') {
                return false;
            }
            if (latestRoomState.turn !== side) {
                return false;
            }
            if (side === 'HOST') {
                return !piece.match(/^b/);
            }
            return !piece.match(/^r/);
        }

        async function requestRoom(url, options) {
            const response = await fetch(url, options);
            if (!response.ok) {
                const text = await response.text();
                throw new Error(text || ('Request failed (' + response.status + ')'));
            }
            return response.json();
        }

        async function createRoom() {
            const playerName = playerNameInput.value.trim();
            if (!playerName) {
                setStatus('Player name is required to create room.', true);
                return;
            }
            activePlayerName = playerName;

            const state = await requestRoom(
                BASE + '/api/room/create?hostName=' + encodeURIComponent(playerName),
                { method: 'POST', headers: apiHeaders() }
            );
            renderFromState(state, true);
        }

        async function joinRoom() {
            const playerName = playerNameInput.value.trim();
            const roomId = roomIdInput.value.trim();
            if (!playerName) {
                setStatus('Player name is required to join room.', true);
                return;
            }
            if (!roomId) {
                setStatus('Room ID is required to join room.', true);
                return;
            }
            activePlayerName = playerName;

            const state = await requestRoom(
                BASE + '/api/room/' + encodeURIComponent(roomId) + '/join?playerName=' + encodeURIComponent(playerName),
                { method: 'POST', headers: apiHeaders() }
            );
            renderFromState(state, true);
        }

        async function loadRoom() {
            const roomId = roomIdInput.value.trim() || activeRoomId;
            if (!roomId) {
                setStatus('Enter room ID to load room state.', true);
                return;
            }

            const state = await requestRoom(
                BASE + '/api/room/' + encodeURIComponent(roomId),
                { method: 'GET' }
            );
            renderFromState(state, false);
        }

        async function sendMove(move) {
            if (!activeRoomId || !activePlayerName) {
                setStatus('Create/join a room before making a move.', true);
                return;
            }
            moveInFlight = true;
            try {
                const state = await requestRoom(
                    BASE + '/api/room/' + encodeURIComponent(activeRoomId) + '/move?playerName=' +
                    encodeURIComponent(activePlayerName) + '&move=' + encodeURIComponent(move),
                    { method: 'POST', headers: apiHeaders() }
                );
                renderFromState(state, true);
            } catch (error) {
                setStatus(error.message, true);
                await loadRoom();
            } finally {
                moveInFlight = false;
            }
        }

        function onDragStart(source, piece) {
            if (moveInFlight) {
                return false;
            }
            return canDrag(piece);
        }

        function onDrop(source, target) {
            const move = source + target;
            sendMove(move);
        }

        const board = Xiangqiboard('myBoard', {
            position: 'start',
            draggable: true,
            pieceTheme: BASE + '/img/xiangqipieces/wikimedia/{piece}.svg',
            onDragStart: onDragStart,
            onDrop: onDrop
        });

        createRoomBtn.addEventListener('click', () => {
            createRoom().catch(error => setStatus(error.message, true));
        });

        joinRoomBtn.addEventListener('click', () => {
            joinRoom().catch(error => setStatus(error.message, true));
        });

        loadRoomBtn.addEventListener('click', () => {
            loadRoom().catch(error => setStatus(error.message, true));
        });

        setInterval(() => {
            if (activeRoomId && !moveInFlight) {
                loadRoom().catch(() => {});
            }
        }, 2000);

        const params = new URLSearchParams(window.location.search);
        const roomIdParam = params.get('roomId');
        const playerNameParam = params.get('playerName');
        if (playerNameParam) {
            activePlayerName = playerNameParam;
            playerNameInput.value = playerNameParam;
        }
        if (roomIdParam) {
            roomIdInput.value = roomIdParam;
            loadRoom().catch(error => setStatus(error.message, true));
        }
    </script>
</body>

</html>
