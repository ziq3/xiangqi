<script lang="ts">
	import { onMount } from 'svelte';
	import { page } from '$app/stores';
	import { gameStore } from '$lib/stores/game';
	import { authStore, resolveDisplayName } from '$lib/stores/auth';
	import type { RoomState } from '$lib/types/game';
	import { engineStore } from '$lib/stores/engine';

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

	function getEvalPercentage(analysis: any): string {
		if (!analysis) return '50%';
		if (analysis.mate !== null && analysis.mate !== undefined) {
			return analysis.mate > 0 ? '100%' : '0%';
		}
		if (analysis.scoreCp === undefined || analysis.scoreCp === null) return '50%';
		// Sigmoid-like or clamped scale. Let's say +1000 cp is 100% red, -1000 is 100% black.
		// A simple clamp:
		const clamped = Math.max(-1000, Math.min(1000, analysis.scoreCp));
		const percent = 50 + (clamped / 20);
		return `${percent}%`;
	}

	function formatScore(analysis: any): string {
		if (!analysis) return '';
		if (analysis.mate !== null && analysis.mate !== undefined) {
			return `M${Math.abs(analysis.mate)}`;
		}
		if (analysis.scoreCp !== null && analysis.scoreCp !== undefined) {
			const val = analysis.scoreCp / 100;
			return val > 0 ? `+${val.toFixed(2)}` : val.toFixed(2);
		}
		return '';
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
			engineStore.stopAnalysis();
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
		<div class="board-container" class:with-eval={$engineStore.enabled}>
			<div class="eval-and-board">
				{#if $engineStore.enabled}
					<div class="eval-bar">
						<div class="eval-fill eval-fill-black" style="height: {100 - parseFloat(getEvalPercentage($engineStore.analysis))}%;"></div>
						<div class="eval-fill eval-fill-red" style="height: {getEvalPercentage($engineStore.analysis)};"></div>
						<div class="eval-score" class:eval-score-black={(parseFloat(getEvalPercentage($engineStore.analysis)) || 50) < 50}>
							{#if $engineStore.loading}
								<span class="spinner-small"></span>
							{:else}
								{formatScore($engineStore.analysis)}
							{/if}
						</div>
					</div>
				{/if}
				<div id="myBoard"></div>
			</div>
			
			<!-- Replay Navigation Controls -->

			<div class="board-nav">
				<button class="btn btn-nav" on:click={() => viewMoveIndex = 0} disabled={viewMoveIndex <= 0} title="Bắt đầu ván">
					<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
						<polygon points="11 19 2 12 11 5 11 19"></polygon>
						<polygon points="22 19 13 12 22 5 22 19"></polygon>
					</svg>
				</button>
				<button class="btn btn-nav" on:click={() => viewMoveIndex = Math.max(0, viewMoveIndex - 1)} disabled={viewMoveIndex <= 0} title="Lùi 1 nước">
					<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
						<polygon points="19 20 9 12 19 4 19 20"></polygon>
						<line x1="5" y1="19" x2="5" y2="5"></line>
					</svg>
				</button>
				<button class="btn btn-nav" on:click={() => viewMoveIndex = Math.min(historyFens.length - 1, viewMoveIndex + 1)} disabled={viewMoveIndex >= historyFens.length - 1} title="Tiến 1 nước">
					<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
						<polygon points="5 4 15 12 5 20 5 4"></polygon>
						<line x1="19" y1="5" x2="19" y2="19"></line>
					</svg>
				</button>
				<button class="btn btn-nav" on:click={() => viewMoveIndex = historyFens.length - 1} disabled={viewMoveIndex >= historyFens.length - 1} title="Nước mới nhất">
					<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
						<polygon points="13 5 22 12 13 19 13 5"></polygon>
						<polygon points="2 5 11 12 2 19 2 5"></polygon>
					</svg>
				</button>
				{#if isFinished || !isPlaying}
					<div style="width: 1px; background: var(--border); margin: 0 4px;"></div>
					<button 
						class="btn btn-nav" 
						class:btn-analyze-active={$engineStore.enabled}
						on:click={() => engineStore.setEnabled(!$engineStore.enabled)}
						title="Bật/tắt phân tích Engine"
					>
						<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
							<circle cx="12" cy="12" r="10"></circle>
							<polyline points="12 6 12 12 16 14"></polyline>
						</svg>
					</button>
				{/if}
			</div>
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

			<!-- Moves history list -->
			{#if historyMoves.length > 0}
				<div class="moves-block">
					<div class="moves-header">
						<h3 class="moves-title">Lịch sử nước đi</h3>
					</div>
					<div class="moves-grid-scroll">
						<div class="moves-grid">
							{#each Array(Math.ceil(historyMoves.length / 2)) as _, index}
								{@const hostMove = historyMoves[index * 2]}
								{@const guestMove = historyMoves[index * 2 + 1]}
								<div class="move-pair-row">
									<span class="move-number">{index + 1}.</span>
									<button 
										class="move-btn" 
										class:move-btn--active={viewMoveIndex === index * 2 + 1}
										on:click={() => viewMoveIndex = index * 2 + 1}
									>
										{hostMove}
									</button>
									{#if guestMove}
										<button 
											class="move-btn" 
											class:move-btn--active={viewMoveIndex === index * 2 + 2}
											on:click={() => viewMoveIndex = index * 2 + 2}
										>
											{guestMove}
										</button>
									{:else}
										<span class="move-btn move-btn--empty">...</span>
									{/if}
								</div>
							{/each}
						</div>
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

		<!-- Invite Box -->
		{#if isWaiting}
			<div class="invite-box">
				<h4 class="invite-box-title">Mời bạn chơi</h4>
				<p class="invite-box-desc">Gửi mã phòng hoặc liên kết này cho bạn bè để bắt đầu chơi:</p>
				
				<div class="invite-field">
					<span class="invite-field-label">Mã phòng</span>
					<div class="invite-copy-row">
						<code class="invite-code">{roomId}</code>
						<button class="btn-copy" on:click={copyRoomId}>
							{copiedId ? 'Đã chép' : 'Sao chép'}
						</button>
					</div>
				</div>

				<div class="invite-field">
					<span class="invite-field-label">Liên kết phòng</span>
					<div class="invite-copy-row">
						<span class="invite-link-preview">{typeof window !== 'undefined' ? window.location.origin + '/room/' + roomId : ''}</span>
						<button class="btn-copy" on:click={copyRoomLink}>
							{copiedLink ? 'Đã chép' : 'Sao chép'}
						</button>
					</div>
				</div>
			</div>
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

	.board-nav {
		display: flex;
		justify-content: space-around;
		gap: 0.5rem;
		width: 100%;
		background: var(--bg-surface);
		border: 1px solid var(--border);
		border-radius: var(--radius-md);
		padding: 0.4rem;
		box-sizing: border-box;
	}

	.btn-nav {
		flex: 1;
		max-width: 60px;
		padding: 0.4rem;
		background: transparent;
		color: var(--text-secondary);
		border: 1px solid transparent;
		transition: all 0.15s var(--ease);
	}

	.btn-nav:hover:not(:disabled) {
		background: var(--bg-raised);
		color: var(--gold-light);
		border-color: var(--border-light);
	}

	.btn-nav:disabled {
		opacity: 0.25;
		cursor: not-allowed;
	}

	/* ── Moves Log ── */
	.moves-block {
		background: var(--bg-surface);
		border: 1px solid var(--border);
		border-radius: var(--radius-lg);
		padding: 0.85rem;
		display: flex;
		flex-direction: column;
		gap: 0.6rem;
		max-height: 250px;
		min-height: 140px;
	}

	.moves-title {
		font-size: 0.72rem;
		font-weight: 700;
		letter-spacing: 0.1em;
		text-transform: uppercase;
		color: var(--text-secondary);
		border-bottom: 1px solid var(--border);
		padding-bottom: 0.4rem;
		margin: 0;
	}

	.moves-grid-scroll {
		overflow-y: auto;
		flex: 1;
		padding-right: 2px;
	}

	.moves-grid {
		display: flex;
		flex-direction: column;
		gap: 0.25rem;
	}

	.move-pair-row {
		display: grid;
		grid-template-columns: 32px 1fr 1fr;
		align-items: center;
		gap: 0.4rem;
		font-size: 0.82rem;
	}

	.move-number {
		color: var(--text-muted);
		font-weight: 600;
		text-align: right;
		padding-right: 0.25rem;
	}

	.move-btn {
		background: transparent;
		border: 1px solid transparent;
		color: var(--text-secondary);
		border-radius: var(--radius-sm);
		padding: 0.25rem 0.4rem;
		text-align: left;
		cursor: pointer;
		font-family: 'Courier New', monospace;
		font-weight: 600;
		font-size: 0.85rem;
		transition: all 0.1s var(--ease);
	}

	.move-btn:hover {
		background: var(--bg-raised);
		color: var(--text-primary);
	}

	.move-btn--active {
		background: var(--accent-dim);
		color: var(--accent-light);
		border-color: var(--border-accent);
	}

	.move-btn--empty {
		color: var(--text-muted);
		cursor: default;
		padding-left: 0.4rem;
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

	/* Invite Box */
	.invite-box {
		background: var(--bg-surface);
		border: 1px solid var(--border);
		border-radius: var(--radius-lg);
		padding: 1rem;
		display: flex;
		flex-direction: column;
		gap: 0.75rem;
		margin-top: 0.5rem;
	}

	.invite-box-title {
		font-size: 0.8rem;
		font-weight: 700;
		letter-spacing: 0.08em;
		text-transform: uppercase;
		color: var(--text-secondary);
		margin: 0;
	}

	.invite-box-desc {
		font-size: 0.75rem;
		color: var(--text-muted);
		line-height: 1.4;
		margin: 0;
	}

	.invite-field {
		display: flex;
		flex-direction: column;
		gap: 0.25rem;
	}

	.invite-field-label {
		font-size: 0.68rem;
		font-weight: 600;
		text-transform: uppercase;
		letter-spacing: 0.04em;
		color: var(--text-muted);
	}

	.invite-copy-row {
		display: flex;
		align-items: center;
		justify-content: space-between;
		background: var(--bg-base);
		border: 1px solid var(--border-light);
		border-radius: var(--radius-md);
		padding: 0.4rem 0.6rem;
		gap: 0.5rem;
		min-width: 0;
	}

	.invite-code {
		font-family: 'Courier New', monospace;
		font-weight: 700;
		color: var(--gold-light);
		font-size: 0.85rem;
	}

	.invite-link-preview {
		font-size: 0.75rem;
		color: var(--text-secondary);
		white-space: nowrap;
		overflow: hidden;
		text-overflow: ellipsis;
		flex: 1;
		min-width: 0;
	}

	.btn-copy {
		background: var(--bg-raised);
		border: 1px solid var(--border-light);
		color: var(--text-secondary);
		font-size: 0.7rem;
		font-weight: 600;
		padding: 0.25rem 0.5rem;
		border-radius: var(--radius-sm);
		cursor: pointer;
		transition: all 0.15s var(--ease);
		flex-shrink: 0;
	}

	.btn-copy:hover {
		background: var(--bg-overlay);
		color: var(--text-primary);
		border-color: var(--border-accent);
	}

	.btn-copy:active {
		transform: scale(0.95);
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

	.eval-bar {
		width: 24px;
		background: var(--bg-surface);
		border: 1px solid var(--border);
		border-radius: var(--radius-sm);
		display: flex;
		flex-direction: column;
		position: relative;
		overflow: hidden;
		flex-shrink: 0;
	}

	.eval-fill {
		width: 100%;
		transition: height 0.3s ease-out;
	}

	.eval-fill-black {
		background-color: #2c2c2c;
	}

	.eval-fill-red {
		background-color: #d32f2f;
	}

	.eval-score {
		position: absolute;
		top: 50%;
		left: 50%;
		transform: translate(-50%, -50%);
		font-size: 0.65rem;
		font-weight: 700;
		color: #fff;
		z-index: 2;
		background: rgba(0, 0, 0, 0.4);
		padding: 2px 4px;
		border-radius: 4px;
		pointer-events: none;
	}

	.eval-score-black {
		color: #fff;
	}

	.moves-header {
		display: flex;
		align-items: center;
		justify-content: space-between;
		border-bottom: 1px solid var(--border);
		padding-bottom: 0.4rem;
		margin-bottom: 0.4rem;
	}

	.moves-title {
		border-bottom: none;
		padding-bottom: 0;
		margin-bottom: 0;
	}

	.btn-analyze-active {
		color: var(--accent-light) !important;
		background: var(--accent-dim) !important;
		border-color: var(--border-accent) !important;
	}

	.spinner-small {
		display: inline-block;
		width: 10px;
		height: 10px;
		border: 2px solid rgba(255,255,255,0.3);
		border-radius: 50%;
		border-top-color: #fff;
		animation: spin 1s ease-in-out infinite;
	}

	:global(.best-move-highlight) {
		background-color: rgba(50, 205, 50, 0.5) !important;
		box-shadow: inset 0 0 10px rgba(50, 205, 50, 0.8) !important;
	}

</style>
