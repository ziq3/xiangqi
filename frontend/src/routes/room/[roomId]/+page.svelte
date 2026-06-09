<script lang="ts">
	import { onMount } from 'svelte';
	import { page } from '$app/stores';
	import { gameStore } from '$lib/stores/game';
	import { authStore, resolveDisplayName } from '$lib/stores/auth';
	import type { RoomState } from '$lib/types/game';
	$: roomId = $page.params.roomId ?? '';

	let board: any = null;
	let game: any = null;
	let notice = '';

	$: room = $gameStore.room;
	$: currentPlayerName = resolveDisplayName($authStore);

	async function startGame() {
		if (!roomId) return;
		await gameStore.startGame(roomId);
	}

	function isMyTurn(activeRoom: RoomState | null): boolean {
		if (!activeRoom || activeRoom.status !== 'PLAYING') return false;
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
		gameStore.submitMove(roomId, currentFen).catch(() => {
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

		if (game.fen().split(' ')[0] === nextRoom.fen.split(' ')[0]) {
			return;
		}

		game.load(nextRoom.fen);
		board.position(nextRoom.fen, true);
	}

	function formatMs(ms: number): string {
		const safe = Number.isFinite(ms) ? Math.max(0, Math.floor(ms)) : 0;
		const totalSeconds = Math.floor(safe / 1000);
		const minutes = Math.floor(totalSeconds / 60);
		const seconds = totalSeconds % 60;
		return `${minutes}:${seconds.toString().padStart(2, '0')}`;
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
			board?.destroy();
			board = null;
			game = null;
		};
	});

	$: myTurn = isMyTurn(room);
	$: isBotMode = room?.guestName === 'BOT';
	$: isWaiting = room?.status === 'WAITING';
	$: isPlaying = room?.status === 'PLAYING';
	$: isFinished = room?.status === 'FINISHED';
	$: isHost = room?.hostName === currentPlayerName;
</script>

<svelte:head>
	<title>Cờ Tướng — Phòng {roomId}</title>
</svelte:head>

<div class="play-page">
	<!-- Board column -->
	<div class="board-col">
		<div id="myBoard"></div>
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
				{#if isBotMode}
					<span class="mode-dot mode-dot--bot"></span> Vs BOT
				{:else}
					<span class="mode-dot mode-dot--human"></span> Vs người chơi
				{/if}
			</div>
		{/if}

		<!-- Players & clocks -->
		{#if room}
			<div class="players-block">
				<div class="player-row" class:player-row--active={isPlaying && room.turn === 'HOST'}>
					<div class="player-info">
						<span class="player-icon">🔴</span>
						<span class="player-name">{room.hostName}</span>
						{#if isHost}<span class="player-you-badge">Bạn</span>{/if}
					</div>
					<div class="player-clock" class:clock--urgent={room.hostTimeMs < 30000}>
						{formatMs(room.hostTimeMs)}
					</div>
				</div>

				<div class="players-divider">
					<span class="vs-label">VS</span>
				</div>

				<div class="player-row" class:player-row--active={isPlaying && room.turn === 'GUEST'}>
					<div class="player-info">
						<span class="player-icon">{isBotMode ? '🤖' : '🔵'}</span>
						<span class="player-name">{room.guestName}</span>
						{#if !isHost && !isBotMode}<span class="player-you-badge">Bạn</span>{/if}
					</div>
					<div class="player-clock" class:clock--urgent={room.guestTimeMs < 30000}>
						{formatMs(room.guestTimeMs)}
					</div>
				</div>
			</div>
		{:else}
			<div class="loading-block">
				<span class="spinner"></span>
				<span>Đang tải phòng…</span>
			</div>
		{/if}

		<!-- Status -->
		<div class="status-block">
			{#if notice}
				<div class="notice-banner">
					<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
						<polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/>
					</svg>
					{notice}
				</div>
			{:else if isWaiting}
				<div class="status-pill status-pill--waiting">Chờ bắt đầu</div>
			{:else if isPlaying}
				<div class="status-pill" class:status-pill--myturn={myTurn} class:status-pill--wait={!myTurn}>
					{myTurn ? 'Lượt của bạn' : 'Chờ đối thủ…'}
				</div>
			{:else if isFinished}
				<div class="status-pill status-pill--finished">Ván đã kết thúc</div>
			{/if}
		</div>

		<!-- Error -->
		{#if $gameStore.error}
			<div class="alert alert-error" role="alert">
				<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
					<circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/>
				</svg>
				{$gameStore.error}
			</div>
		{/if}

		<!-- Start button -->
		{#if isWaiting && isHost}
			<button id="start-game-btn" class="btn btn-primary start-btn" on:click={startGame}>
				<svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
					<polygon points="5 3 19 12 5 21 5 3"/>
				</svg>
				Bắt đầu chơi
			</button>
		{/if}

		<!-- Invite hint -->
		{#if isBotMode && isWaiting}
			<p class="invite-hint">
				<svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
					<circle cx="18" cy="5" r="3"/><circle cx="6" cy="12" r="3"/><circle cx="18" cy="19" r="3"/>
					<line x1="8.59" y1="13.51" x2="15.42" y2="17.49"/><line x1="15.41" y1="6.51" x2="8.59" y2="10.49"/>
				</svg>
				Chia sẻ ID phòng để mời bạn bè thay thế BOT
			</p>
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

	/*
	  Board is 9 cols × 10 rows → aspect ratio width:height = 9:10 = 0.9
	  Available height = 100dvh - 60px (topbar) - 2rem (vertical padding)
	  Board width from height = available_height × 0.9
	  Also cap at available horizontal space = 100vw - 260px (panel) - 4.5rem (gap + h-padding)
	*/
	:global(#myBoard) {
		width: min(
			calc((100dvh - 60px - 2rem) * 0.9),
			calc(100vw - 260px - 4.5rem)
		) !important;
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

	.player-row {
		display: flex;
		align-items: center;
		justify-content: space-between;
		padding: 0.85rem 1rem;
		transition: background 0.2s;
	}

	.player-row--active {
		background: var(--accent-dim);
		border-left: 3px solid var(--accent);
	}

	.player-info {
		display: flex;
		align-items: center;
		gap: 0.5rem;
		min-width: 0;
	}

	.player-icon {
		font-size: 1rem;
		flex-shrink: 0;
	}

	.player-name {
		font-size: 0.875rem;
		font-weight: 600;
		color: var(--text-primary);
		white-space: nowrap;
		overflow: hidden;
		text-overflow: ellipsis;
		max-width: 110px;
	}

	.player-you-badge {
		font-size: 0.65rem;
		font-weight: 700;
		letter-spacing: 0.06em;
		text-transform: uppercase;
		color: var(--accent-light);
		background: var(--accent-dim);
		border: 1px solid var(--border-accent);
		border-radius: 99px;
		padding: 0.1rem 0.4rem;
		flex-shrink: 0;
	}

	.player-clock {
		font-size: 1.05rem;
		font-weight: 700;
		font-variant-numeric: tabular-nums;
		color: var(--text-secondary);
		flex-shrink: 0;
	}

	.clock--urgent {
		color: var(--error-light);
		animation: urgentPulse 1s ease-in-out infinite;
	}

	@keyframes urgentPulse {
		0%, 100% { opacity: 1; }
		50% { opacity: 0.5; }
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

	/* Status */
	.status-block {
		display: flex;
		flex-direction: column;
		gap: 0.5rem;
	}

	.status-pill {
		display: inline-flex;
		align-items: center;
		justify-content: center;
		padding: 0.5rem 0.75rem;
		border-radius: var(--radius-md);
		font-size: 0.82rem;
		font-weight: 700;
		letter-spacing: 0.03em;
		border: 1px solid;
	}

	.status-pill--waiting {
		color: var(--gold);
		background: var(--gold-dim);
		border-color: rgba(201,168,76,0.3);
	}

	.status-pill--myturn {
		color: #fff;
		background: var(--accent);
		border-color: var(--accent);
		box-shadow: 0 0 14px var(--accent-glow);
	}

	.status-pill--wait {
		color: var(--text-secondary);
		background: var(--bg-surface);
		border-color: var(--border);
	}

	.status-pill--finished {
		color: var(--text-muted);
		background: var(--bg-surface);
		border-color: var(--border);
	}

	.notice-banner {
		display: flex;
		align-items: center;
		gap: 0.5rem;
		padding: 0.65rem 0.85rem;
		background: rgba(201,168,76,0.1);
		border: 1px solid rgba(201,168,76,0.3);
		border-radius: var(--radius-md);
		color: var(--gold-light);
		font-weight: 700;
		font-size: 0.875rem;
	}

	/* Start btn */
	.start-btn {
		margin-top: 0.25rem;
	}

	/* Invite hint */
	.invite-hint {
		display: flex;
		align-items: flex-start;
		gap: 0.4rem;
		font-size: 0.78rem;
		color: var(--text-muted);
		line-height: 1.4;
	}

	.invite-hint svg {
		flex-shrink: 0;
		margin-top: 1px;
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
		:global(#myBoard) {
			width: min(90vw, 500px) !important;
		}
		.info-panel {
			height: auto;
		}
	}
</style>
