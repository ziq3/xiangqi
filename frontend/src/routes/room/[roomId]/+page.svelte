<script lang="ts">
	import { onMount } from 'svelte';
	import { page } from '$app/stores';
	import { gameStore } from '$lib/stores/game';
	import { authStore, resolveDisplayName } from '$lib/stores/auth';
	import type { RoomState } from '$lib/types/game';
	import { engineStore } from '$lib/stores/engine';
	import EvalBar from './components/EvalBar.svelte';
	import ReplayControls from './components/ReplayControls.svelte';
	import PlayerRow from './components/PlayerRow.svelte';
	import MovesHistory from './components/MovesHistory.svelte';
	import InviteBox from './components/InviteBox.svelte';
	import StatusBlock from './components/StatusBlock.svelte';

	$: roomId = $page.params.roomId ?? '';

	let board: any = null;
	let game: any = null;
	let notice = '';
	
	let historyMoves: string[] = [];
	let historyFens: string[] = [];
	let viewMoveIndex = -1;

	$: room = $gameStore.room;
	$: currentPlayerName = resolveDisplayName($authStore);
	$: analysisEnabled = $engineStore.enabled;

	let copiedId = false;
	let copiedLink = false;
	let copiedFen = false;

	function copyRoomId() {
		navigator.clipboard.writeText(roomId).then(() => {
			copiedId = true;
			setTimeout(() => (copiedId = false), 2000);
		});
	}

	function copyRoomLink() {
		if (typeof window !== 'undefined') {
			navigator.clipboard.writeText(window.location.href).then(() => {
				copiedLink = true;
				setTimeout(() => (copiedLink = false), 2000);
			});
		}
	}

	$: currentFenToCopy = (viewMoveIndex >= 0 && viewMoveIndex < historyFens.length)
		? historyFens[viewMoveIndex]
		: (room?.fen || '');

	function copyCurrentFen() {
		if (!currentFenToCopy) return;
		if (typeof navigator !== 'undefined' && navigator.clipboard) {
			navigator.clipboard.writeText(currentFenToCopy).then(() => {
				copiedFen = true;
				setTimeout(() => (copiedFen = false), 2000);
			});
		}
	}

	// Reactively compute the FEN at each move index from moveHistory
	$: {
		if (room) {
			const rawHistory = room.moveHistory ? room.moveHistory.trim() : '';
			const moves = rawHistory ? rawHistory.split(/\s+/) : [];
			if (moves.join(' ') !== historyMoves.join(' ')) {
				const wasAtLatest = viewMoveIndex === -1 || viewMoveIndex === historyFens.length - 1;
				historyMoves = moves;
				const tempGame = new Xiangqi();
				const startFen = "rnbakabnr/9/1c5c1/p1p1p1p1p/9/9/P1P1P1P1P/1C5C1/9/RNBAKABNR w - - 0 1";
				tempGame.load(startFen);
				const fens = [startFen];
				for (const m of moves) {
					if (m.length === 4) {
						const from = m.substring(0, 2);
						const to = m.substring(2, 4);
						tempGame.move({ from, to });
						fens.push(tempGame.fen());
					}
				}
				historyFens = fens;
				// If we are at the latest or game is not playing, snap to the latest move
				if (wasAtLatest || room.status !== 'PLAYING') {
					viewMoveIndex = fens.length - 1;
				}
			}
		} else {
			historyMoves = [];
			historyFens = [];
			viewMoveIndex = -1;
		}
	}

	function clearHighlight() {
		if (typeof document !== 'undefined') {
			document.querySelectorAll('.best-move-highlight').forEach(el => el.classList.remove('best-move-highlight'));
		}
	}

	function applyHighlight(bestMove: string | null) {
		clearHighlight();
		if (!bestMove || bestMove.length < 4) return;
		const from = bestMove.substring(0, 2);
		const to = bestMove.substring(2, 4);
		if (typeof document !== 'undefined') {
			const fromSq = document.querySelector(`.square-${from}`);
			const toSq = document.querySelector(`.square-${to}`);
			if (fromSq) fromSq.classList.add('best-move-highlight');
			if (toSq) toSq.classList.add('best-move-highlight');
		}
	}

	// Reactively update the board and local game engine when viewMoveIndex changes
	$: {
		if (board && game && viewMoveIndex >= 0 && viewMoveIndex < historyFens.length) {
			const targetFen = historyFens[viewMoveIndex];
			if (game.fen().split(' ')[0] !== targetFen.split(' ')[0]) {
				game.load(targetFen);
				board.position(targetFen, true);
			}
			
			if (analysisEnabled) {
				engineStore.analyze(targetFen);
			} else {
				clearHighlight();
			}
		}
	}

	// Reactively apply highlights when analysis arrives from the realtime stream
	$: {
		if (analysisEnabled && $engineStore.analysis?.bestMove) {
			applyHighlight($engineStore.analysis.bestMove);
		} else if (!analysisEnabled) {
			clearHighlight();
		}
	}

	async function startGame() {
		if (!roomId) return;
		await gameStore.startGame(roomId);
	}

	function isMyTurn(activeRoom: RoomState | null): boolean {
		if (!activeRoom || activeRoom.status !== 'PLAYING') return false;
		if (viewMoveIndex !== historyFens.length - 1) return false;
		const isHost = activeRoom.hostName === currentPlayerName;
		const isGuest = activeRoom.guestName === currentPlayerName;
		return (isHost && activeRoom.turn === 'HOST') || (isGuest && activeRoom.turn === 'GUEST');
	}

	function rollbackToServerFen() {
		if (!room?.fen || !board || !game) return;
		board.position(room.fen, false);
		game.load(room.fen);
	}

	function onDrop(
		source: string,
		target: string,
		_piece: string,
		newPos: unknown,
		_oldPos: unknown
	) {
		if (!room || !board || !game) return 'snapback';
		if (!isMyTurn(room) || $gameStore.pendingMove) return 'snapback';

		const move = game.move({ from: source, to: target });
		if (move === null) return 'snapback';

		const currentFen = game.fen() || window.Xiangqiboard.objToFen(newPos);
		const moveNotation = move.from + move.to;
		const isCheckmate = typeof game.in_checkmate === 'function' && game.in_checkmate();
		gameStore.submitMove(roomId, currentFen, moveNotation, isCheckmate).catch(() => {
			rollbackToServerFen();
		});
	}

	function onDragStart(_source: string, piece: string) {
		if (!isMyTurn(room) || $gameStore.pendingMove) return false;
		if (!game) return false;
		if (game.game_over()) return false;

		if (
			(game.turn() === 'r' && piece.search(/^b/) !== -1) ||
			(game.turn() === 'b' && piece.search(/^r/) !== -1)
		) {
			return false;
		}

		return true;
	}

	const config = {
		position: 'start',
		draggable: true,
		pieceTheme: '/img/xiangqipieces/wikimedia/{piece}.svg',
		boardTheme: '/img/xiangqiboards/wikimedia/xiangqiboard.svg',
		onDrop: onDrop,
		onDragStart: onDragStart
	};

	function applyRoomToBoard(nextRoom: RoomState | null) {
		if (!board || !game || !nextRoom?.fen) return;

		// Only apply the server FEN to the board/game if we are viewing the latest move
		const isAtLatest = viewMoveIndex === historyFens.length - 1;
		if (!isAtLatest) {
			return;
		}

		if (game.fen().split(' ')[0] === nextRoom.fen.split(' ')[0]) {
			return;
		}

		game.load(nextRoom.fen);
		board.position(nextRoom.fen, true);
	}

	function updateNotice(activeRoom: RoomState | null) {
		if (!activeRoom || !game) {
			notice = '';
			return;
		}

		if (activeRoom.endReason === 'TIMEOUT_HOST') {
			notice = 'Hết giờ — HOST thua';
			return;
		}
		if (activeRoom.endReason === 'TIMEOUT_GUEST') {
			notice = 'Hết giờ — GUEST thua';
			return;
		}

		if (typeof game.in_checkmate === 'function' && game.in_checkmate()) {
			notice = 'Chiếu hết!';
			return;
		}
		if (typeof game.in_stalemate === 'function' && game.in_stalemate()) {
			notice = 'Hết nước đi (Thua)!';
			return;
		}
		if (typeof game.in_draw === 'function' && game.in_draw()) {
			notice = 'Hoà cờ.';
			return;
		}

		notice = '';
	}

	async function setupRoom(roomId: string) {
		const initialRoom = await gameStore.loadRoom(roomId);
		if (!initialRoom?.fen) {
			return undefined;
		}

		const nextGame = new Xiangqi();
		nextGame.load(initialRoom.fen);
		game = nextGame;

		const nextBoard = window.Xiangqiboard('myBoard', config);
		nextBoard.position(initialRoom.fen, false);
		board = nextBoard;

		await gameStore.startRoomSync(roomId);
		const unsubscribe = gameStore.subscribe((state) => {
			applyRoomToBoard(state.room);
			updateNotice(state.room);
		});

		updateNotice(initialRoom);

		return unsubscribe;
	}

	onMount(() => {
		void authStore.init();

		let unsubscribeStore: (() => void) | undefined;
		if (roomId) {
			void setupRoom(roomId).then((unsubscribe) => {
				unsubscribeStore = unsubscribe;
			});
		}

		return () => {
			unsubscribeStore?.();
			gameStore.stopRoomSync();
			engineStore.setEnabled(false);
			board?.destroy();
			board = null;
			game = null;
		};
	});

	$: myTurn = isMyTurn(room);
	$: isBotMode = room?.botGame === true;
	$: isWaiting = room?.status === 'WAITING';
	$: isPlaying = room?.status === 'PLAYING';
	$: isFinished = room?.status === 'FINISHED';
	$: isHost = room?.hostName === currentPlayerName;

	// Disable engine analysis when game starts playing (prevent cheating/visual bugs)
	$: {
		if (isPlaying && analysisEnabled) {
			engineStore.setEnabled(false);
		}
	}

	// Automatically submit checkmate/stalemate if it is our turn, the game is active, and we have no legal moves
	$: {
		if (room && room.status === 'PLAYING' && isMyTurn(room) && game && !$gameStore.pendingMove) {
			const hasNoMoves = typeof game.generate_moves === 'function' && game.generate_moves().length === 0;
			if (hasNoMoves) {
				const isCheckmate = typeof game.in_checkmate === 'function' && game.in_checkmate();
				const isStalemate = typeof game.in_stalemate === 'function' && game.in_stalemate();
				if (isCheckmate || isStalemate) {
					gameStore.submitMove(roomId, room.fen, '', true).catch(console.error);
				}
			}
		}
	}
</script>

<svelte:head>
	<title>Cờ Tướng — Phòng {roomId}</title>
</svelte:head>

<div class="play-page">
	<!-- Board column -->
	<div class="board-col">
		<div class="board-container" class:with-eval={$engineStore.enabled}>
			<div class="eval-and-board">
				{#if $engineStore.enabled}
					<EvalBar analysis={$engineStore.analysis} loading={$engineStore.loading} fen={currentFenToCopy} />
				{/if}
				<div id="myBoard"></div>
			</div>
			
			<!-- Replay Navigation Controls -->
			<ReplayControls
				{viewMoveIndex}
				historyFensLength={historyFens.length}
				{isPlaying}
				{isFinished}
				{copiedFen}
				engineEnabled={$engineStore.enabled}
				onNavigate={(index) => viewMoveIndex = index}
				onCopyFen={copyCurrentFen}
				onToggleEngine={() => engineStore.setEnabled(!$engineStore.enabled)}
			/>
		</div>
	</div>

	<!-- Info panel -->
	<aside class="info-panel">
		<!-- Room ID badge -->
		<div class="room-badge">
			<span class="room-badge-label">Phòng</span>
			<code class="room-badge-id">{roomId}</code>
		</div>

		<!-- Mode tag -->
		{#if room}
			<div class="mode-tag">
				{#if isWaiting && isBotMode}
					<span class="mode-dot mode-dot--waiting"></span> Chờ bạn chơi...
				{:else if isBotMode}
					<span class="mode-dot mode-dot--bot"></span> Vs BOT
				{:else}
					<span class="mode-dot mode-dot--human"></span> Vs người chơi
				{/if}
			</div>
		{/if}

		<!-- Players & clocks -->
		{#if room}
			<div class="players-block">
				<PlayerRow
					name={room.hostName}
					icon="🔴"
					timeMs={room.hostTimeMs}
					isActive={isPlaying && room.turn === 'HOST'}
					isYou={isHost}
				/>

				<div class="players-divider">
					<span class="vs-label">VS</span>
				</div>

				<PlayerRow
					name={room.guestName}
					icon={isBotMode ? '🤖' : '🔵'}
					timeMs={room.guestTimeMs}
					isActive={isPlaying && room.turn === 'GUEST'}
					isYou={!isHost && !isBotMode}
				/>
			</div>

			<!-- Moves history list -->
			{#if historyMoves.length > 0}
				<MovesHistory
					{historyMoves}
					{viewMoveIndex}
					onSelectMove={(index) => viewMoveIndex = index}
				/>
			{/if}

			<!-- FEN block (visible when viewing past match or when replay is active) -->
			{#if room && (isFinished || !isPlaying || viewMoveIndex < historyFens.length - 1)}
				<div class="fen-block">
					<div class="fen-header">
						<span class="fen-title">FEN nước hiện tại</span>
						<button class="btn-copy-fen" on:click={copyCurrentFen}>
							{#if copiedFen}
								<svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
									<polyline points="20 6 9 17 4 12"></polyline>
								</svg>
								Đã chép
							{:else}
								<svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
									<rect x="9" y="9" width="13" height="13" rx="2" ry="2"></rect>
									<path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"></path>
								</svg>
								Sao chép
							{/if}
						</button>
					</div>
					<div class="fen-value-container">
						<code class="fen-value" title={currentFenToCopy}>{currentFenToCopy}</code>
					</div>
				</div>
			{/if}
		{:else}
			<div class="loading-block">
				<span class="spinner"></span>
				<span>Đang tải phòng…</span>
			</div>
		{/if}

		<!-- Status -->
		<StatusBlock
			{notice}
			{isWaiting}
			{isPlaying}
			{isFinished}
			myTurn={myTurn}
		/>

		<!-- Error -->
		{#if $gameStore.error}
			<div class="alert alert-error" role="alert">
				<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
					<circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/>
				</svg>
				{$gameStore.error}
			</div>
		{/if}

		<!-- Invite Box -->
		{#if isWaiting}
			<InviteBox
				{roomId}
				{copiedId}
				{copiedLink}
				onCopyId={copyRoomId}
				onCopyLink={copyRoomLink}
			/>
		{/if}

		<!-- Start button -->
		{#if isWaiting && isHost}
			<button id="start-game-btn" class="btn btn-primary start-btn" on:click={startGame}>
				<svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
					<polygon points="5 3 19 12 5 21 5 3"/>
				</svg>
				Chơi với BOT ngay
			</button>
		{/if}
	</aside>
</div>

<style>
	.play-page {
		display: grid;
		grid-template-columns: 1fr 260px;
		gap: 1.5rem;
		align-items: center;
		padding: 1rem 1.5rem;
		height: calc(100dvh - 60px);
		box-sizing: border-box;
		overflow: hidden;
	}

	/* ── Board ── */
	.board-col {
		display: flex;
		align-items: center;
		justify-content: center;
		height: 100%;
		min-width: 0;
	}

	.board-container {
		display: flex;
		flex-direction: column;
		align-items: center;
		gap: 0.75rem;
		width: min(
			calc((100dvh - 60px - 2rem - 60px) * 0.9),
			calc(100vw - 260px - 4.5rem)
		);
	}

	:global(#myBoard) {
		width: 100% !important;
	}

	/* ── Info panel ── */
	.info-panel {
		display: flex;
		flex-direction: column;
		gap: 0.75rem;
		height: 100%;
		padding: 0.25rem 0;
		overflow-y: auto;
	}

	/* Room badge */
	.room-badge {
		display: flex;
		align-items: center;
		gap: 0.5rem;
		background: var(--bg-surface);
		border: 1px solid var(--border);
		border-radius: var(--radius-md);
		padding: 0.5rem 0.75rem;
	}

	.room-badge-label {
		font-size: 0.72rem;
		font-weight: 700;
		letter-spacing: 0.1em;
		text-transform: uppercase;
		color: var(--text-muted);
	}

	.room-badge-id {
		font-family: 'Courier New', monospace;
		font-size: 0.8rem;
		color: var(--gold);
		word-break: break-all;
	}

	/* Mode */
	.mode-tag {
		display: flex;
		align-items: center;
		gap: 0.4rem;
		font-size: 0.8rem;
		font-weight: 600;
		color: var(--text-secondary);
		letter-spacing: 0.02em;
	}

	.mode-dot {
		width: 7px;
		height: 7px;
		border-radius: 50%;
		display: inline-block;
	}
	.mode-dot--bot { background: var(--gold); box-shadow: 0 0 6px var(--gold); }
	.mode-dot--human { background: var(--success-light); box-shadow: 0 0 6px var(--success-light); }

	/* Players */
	.players-block {
		background: var(--bg-surface);
		border: 1px solid var(--border);
		border-radius: var(--radius-lg);
		overflow: hidden;
	}

	.players-divider {
		display: flex;
		align-items: center;
		justify-content: center;
		height: 24px;
		background: var(--bg-raised);
		border-top: 1px solid var(--border);
		border-bottom: 1px solid var(--border);
	}

	.vs-label {
		font-size: 0.65rem;
		font-weight: 800;
		letter-spacing: 0.12em;
		color: var(--text-muted);
	}

	/* Start btn */
	.start-btn {
		margin-top: 0.25rem;
	}

	.mode-dot--waiting {
		background: var(--gold-light);
		box-shadow: 0 0 6px var(--gold-light);
		animation: pulse 2s infinite;
	}

	@keyframes pulse {
		0% { opacity: 0.4; }
		50% { opacity: 1; }
		100% { opacity: 0.4; }
	}

	/* Loading */
	.loading-block {
		display: flex;
		align-items: center;
		gap: 0.6rem;
		color: var(--text-secondary);
		font-size: 0.875rem;
		padding: 1rem 0;
	}

	/* Alert override */
	:global(.alert) {
		font-size: 0.82rem;
	}

	/* Responsive: stack below 900px */
	@media (max-width: 900px) {
		.play-page {
			grid-template-columns: 1fr;
			height: auto;
			overflow: auto;
			padding: 1rem;
		}
		.board-col {
			height: auto;
		}
		.board-container {
			width: min(90vw, 500px);
		}
		:global(#myBoard) {
			width: 100% !important;
		}
		.info-panel {
			height: auto;
		}
	}

	/* Analysis Tools */
	.eval-and-board {
		display: flex;
		gap: 8px;
		width: 100%;
		align-items: stretch;
	}

	:global(.best-move-highlight) {
		background-color: rgba(50, 205, 50, 0.5) !important;
		box-shadow: inset 0 0 10px rgba(50, 205, 50, 0.8) !important;
	}

	/* ── FEN Block ── */
	.fen-block {
		background: var(--bg-surface);
		border: 1px solid var(--border);
		border-radius: var(--radius-lg);
		padding: 0.85rem;
		display: flex;
		flex-direction: column;
		gap: 0.5rem;
		margin-top: 0.25rem;
	}

	.fen-header {
		display: flex;
		align-items: center;
		justify-content: space-between;
	}

	.fen-title {
		font-size: 0.72rem;
		font-weight: 700;
		letter-spacing: 0.1em;
		text-transform: uppercase;
		color: var(--text-secondary);
	}

	.btn-copy-fen {
		display: inline-flex;
		align-items: center;
		gap: 0.3rem;
		background: var(--bg-raised);
		border: 1px solid var(--border-light);
		color: var(--text-secondary);
		font-size: 0.7rem;
		font-weight: 600;
		padding: 0.25rem 0.5rem;
		border-radius: var(--radius-sm);
		cursor: pointer;
		transition: all 0.15s var(--ease);
	}

	.btn-copy-fen:hover {
		background: var(--bg-overlay);
		color: var(--text-primary);
		border-color: var(--border-accent);
	}

	.btn-copy-fen:active {
		transform: scale(0.95);
	}

	.fen-value-container {
		background: var(--bg-base);
		border: 1px solid var(--border-light);
		border-radius: var(--radius-md);
		padding: 0.5rem;
		min-width: 0;
	}

	.fen-value {
		font-family: 'Courier New', monospace;
		font-size: 0.75rem;
		color: var(--gold-light);
		word-break: break-all;
		display: block;
		white-space: nowrap;
		overflow: hidden;
		text-overflow: ellipsis;
	}
</style>
