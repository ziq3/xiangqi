<script lang="ts">
	import { onMount } from 'svelte';
	import { page } from '$app/stores';
	import { gameStore } from '$lib/stores/game';
	import { engineStore } from '$lib/stores/engine';
	import EvalBar from '../room/[roomId]/components/EvalBar.svelte';
	import ReplayControls from '../room/[roomId]/components/ReplayControls.svelte';
	import MovesHistory from '../room/[roomId]/components/MovesHistory.svelte';

	let board: any = null;
	let game: any = null;

	let inputFen = '';
	let copiedFen = false;
	
	let historyMoves: string[] = [];
	let historyFens: string[] = [];
	let viewMoveIndex = -1;

	const startFen = "rnbakabnr/9/1c5c1/p1p1p1p1p/9/9/P1P1P1P1P/1C5C1/9/RNBAKABNR w - - 0 1";

	// We only show engine highlight if analysis is enabled.
	$: analysisEnabled = $engineStore.enabled;
	$: currentFen = (viewMoveIndex >= 0 && viewMoveIndex < historyFens.length)
		? historyFens[viewMoveIndex]
		: (historyFens.length > 0 ? historyFens[historyFens.length - 1] : startFen);

	$: {
		if (analysisEnabled && currentFen) {
			engineStore.analyze(currentFen);
		} else if (!analysisEnabled) {
			clearHighlight();
		}
	}

	$: {
		if (analysisEnabled && $engineStore.analysis?.bestMove) {
			applyHighlight($engineStore.analysis.bestMove);
		} else if (!analysisEnabled) {
			clearHighlight();
		}
	}

	// Update board and game when viewMoveIndex changes, but only if they differ
	$: {
		if (board && game && viewMoveIndex >= 0 && viewMoveIndex < historyFens.length) {
			const targetFen = historyFens[viewMoveIndex];
			if (game.fen().split(' ')[0] !== targetFen.split(' ')[0]) {
				game.load(targetFen);
				board.position(targetFen, true);
			}
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

	function onDrop(source: string, target: string, _piece: string, newPos: unknown, _oldPos: unknown) {
		if (!board || !game) return 'snapback';

		const move = game.move({ from: source, to: target });
		if (move === null) return 'snapback';

		const moveNotation = move.from + move.to;
		pushMove(moveNotation, game.fen());
	}

	function onDragStart(_source: string, piece: string) {
		if (!game) return false;
		if (game.game_over()) return false;

		if ((game.turn() === 'r' && piece.search(/^b/) !== -1) ||
			(game.turn() === 'b' && piece.search(/^r/) !== -1)) {
			return false;
		}
		return true;
	}

	function pushMove(moveNotation: string, newFen: string) {
		// If we are viewing a past move and make a new move, truncate history
		if (viewMoveIndex < historyFens.length - 1) {
			historyMoves = historyMoves.slice(0, viewMoveIndex);
			historyFens = historyFens.slice(0, viewMoveIndex + 1);
		}
		historyMoves = [...historyMoves, moveNotation];
		historyFens = [...historyFens, newFen];
		viewMoveIndex = historyFens.length - 1;
	}

	function loadCustomFen() {
		if (!inputFen || !game || !board) return;
		const valid = game.load(inputFen);
		if (valid) {
			board.position(inputFen, false);
			historyMoves = [];
			historyFens = [game.fen()];
			viewMoveIndex = 0;
			inputFen = '';
		} else {
			alert('FEN không hợp lệ!');
		}
	}

	function resetToStart() {
		if (!game || !board) return;
		game.load(startFen);
		board.position(startFen, false);
		historyMoves = [];
		historyFens = [startFen];
		viewMoveIndex = 0;
	}

	function copyCurrentFen() {
		if (!currentFen) return;
		if (typeof navigator !== 'undefined' && navigator.clipboard) {
			navigator.clipboard.writeText(currentFen).then(() => {
				copiedFen = true;
				setTimeout(() => (copiedFen = false), 2000);
			});
		}
	}

	function swapSides() {
		if (board) {
			board.orientation(board.orientation() === 'red' ? 'black' : 'red');
		}
	}

	let autoBotRed = false;
	let autoBotBlack = false;
	let loadingBotMove = false;

	$: {
		// Auto-trigger bot if it's its turn
		if (game && !loadingBotMove && !game.game_over() && currentFen) {
			const turn = game.turn();
			if ((turn === 'r' && autoBotRed) || (turn === 'b' && autoBotBlack)) {
				// Use setTimeout to avoid synchronous reactive loops
				setTimeout(() => triggerBotMove(turn), 50);
			}
		}
	}

	async function triggerBotMove(color: 'r' | 'b') {
		if (!game || !board || loadingBotMove) return;
		if (game.turn() !== color) return;
		if (game.game_over()) return;

		loadingBotMove = true;
		try {
			const res = await fetch(`/api/engine/bestmove?fen=${encodeURIComponent(currentFen)}`);
			if (!res.ok) throw new Error('Failed to fetch bot move');
			const data = await res.json();
			if (data.move && data.move !== '(none)') {
				const from = data.move.substring(0, 2);
				const to = data.move.substring(2, 4);
				const move = game.move({ from, to });
				if (move) {
					board.position(game.fen(), true);
					pushMove(data.move, game.fen());
				}
			} else {
				if (color === 'r') autoBotRed = false;
				else autoBotBlack = false;
				alert('Bot không tìm thấy nước đi (có thể đã hết cờ).');
			}
		} catch (error) {
			console.error(error);
			if (color === 'r') autoBotRed = false;
			else autoBotBlack = false;
			alert('Có lỗi xảy ra khi gọi Bot.');
		} finally {
			loadingBotMove = false;
		}
	}

	function toggleBot(color: 'r' | 'b') {
		if (color === 'r') {
			autoBotRed = !autoBotRed;
		} else {
			autoBotBlack = !autoBotBlack;
		}
	}

	async function loadRoomHistory(roomId: string) {
		try {
			const room = await gameStore.loadRoom(roomId);
			if (room && room.moveHistory) {
				const moves = room.moveHistory.trim().split(/\s+/).filter(m => m.length === 4);
				const tempGame = new Xiangqi();
				tempGame.load(startFen);
				const fens = [startFen];
				for (const m of moves) {
					tempGame.move({ from: m.substring(0, 2), to: m.substring(2, 4) });
					fens.push(tempGame.fen());
				}
				historyMoves = moves;
				historyFens = fens;
				viewMoveIndex = fens.length - 1;

				if (game && board) {
					const latestFen = historyFens[viewMoveIndex];
					game.load(latestFen);
					board.position(latestFen, false);
				}
			}
		} catch (err) {
			console.error("Failed to load room history:", err);
		}
	}

	onMount(() => {
		const nextGame = new Xiangqi();
		nextGame.load(startFen);
		game = nextGame;

		historyFens = [startFen];
		viewMoveIndex = 0;

		const config = {
			position: startFen,
			draggable: true,
			pieceTheme: '/img/xiangqipieces/wikimedia/{piece}.svg',
			boardTheme: '/img/xiangqiboards/wikimedia/xiangqiboard.svg',
			onDrop: onDrop,
			onDragStart: onDragStart
		};

		const nextBoard = window.Xiangqiboard('myBoardAnalysis', config);
		board = nextBoard;

		const roomId = $page.url.searchParams.get('roomId');
		if (roomId) {
			loadRoomHistory(roomId);
		}

		return () => {
			engineStore.setEnabled(false);
			board?.destroy();
			board = null;
			game = null;
		};
	});
</script>

<svelte:head>
	<title>Cờ Tướng — Phân tích & Học tập</title>
</svelte:head>

<div class="play-page">
	<div class="board-col">
		<div class="board-container" class:with-eval={$engineStore.enabled}>
			<div class="eval-and-board">
				{#if $engineStore.enabled}
					<EvalBar analysis={$engineStore.analysis} loading={$engineStore.loading} fen={currentFen} />
				{/if}
				<div id="myBoardAnalysis"></div>
			</div>
			
			<ReplayControls
				{viewMoveIndex}
				historyFensLength={historyFens.length}
				isPlaying={false}
				isFinished={true}
				{copiedFen}
				engineEnabled={$engineStore.enabled}
				showAnalysisToggle={false}
				onNavigate={(index) => viewMoveIndex = index}
				onCopyFen={copyCurrentFen}
				onToggleEngine={() => {}}
			/>
		</div>
	</div>

	<aside class="info-panel">
		<div class="analysis-header">
			<h2 class="analysis-title">Phân tích cờ</h2>
			<button class="btn btn-ghost btn-sm" on:click={swapSides}>
				<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
					<polyline points="16 3 21 3 21 8"></polyline>
					<line x1="4" y1="20" x2="21" y2="3"></line>
					<polyline points="21 16 21 21 16 21"></polyline>
					<line x1="15" y1="15" x2="21" y2="21"></line>
					<line x1="4" y1="4" x2="9" y2="9"></line>
				</svg>
				Đổi bên
			</button>
		</div>

		<div class="fen-control">
			<label for="fen-input" class="fen-label">Tải FEN</label>
			<div class="fen-input-group">
				<input id="fen-input" type="text" class="form-input" bind:value={inputFen} placeholder="Nhập chuỗi FEN..." autocomplete="off" />
				<button class="btn btn-primary btn-sm" on:click={loadCustomFen}>Tải</button>
			</div>
		</div>

		<div class="bot-controls">
			<button class="btn btn-danger btn-bot" class:active-bot={autoBotRed} on:click={() => toggleBot('r')}>
				🤖 Bot Đỏ: {autoBotRed ? 'ON' : 'OFF'}
			</button>
			<button class="btn btn-dark btn-bot" class:active-bot={autoBotBlack} on:click={() => toggleBot('b')}>
				🤖 Bot Đen: {autoBotBlack ? 'ON' : 'OFF'}
			</button>
			<button class="btn btn-outline btn-analyze-toggle" class:btn-analyze-active={$engineStore.enabled} on:click={() => engineStore.setEnabled(!$engineStore.enabled)}>
				<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="margin-right: 4px;">
					<circle cx="12" cy="12" r="10"></circle>
					<polyline points="12 6 12 12 16 14"></polyline>
				</svg>
				Phân tích: {$engineStore.enabled ? 'Bật' : 'Tắt'}
			</button>
		</div>
		
		<button class="btn btn-ghost" on:click={resetToStart}>
			Về vị trí ban đầu
		</button>

		{#if historyMoves.length > 0}
			<div class="history-wrapper">
				<MovesHistory
					{historyMoves}
					{viewMoveIndex}
					onSelectMove={(index) => viewMoveIndex = index}
				/>
			</div>
		{/if}

		<div class="fen-block">
			<div class="fen-header">
				<span class="fen-title">FEN hiện tại</span>
				<button class="btn-copy-fen" on:click={copyCurrentFen}>
					{#if copiedFen}
						<svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"></polyline></svg> Đã chép
					{:else}
						<svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><rect x="9" y="9" width="13" height="13" rx="2" ry="2"></rect><path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"></path></svg> Sao chép
					{/if}
				</button>
			</div>
			<div class="fen-value-container">
				<code class="fen-value" title={currentFen}>{currentFen}</code>
			</div>
		</div>
	</aside>
</div>

<style>
	/* Copying styles from the room page to match layout */
	.play-page {
		display: grid;
		grid-template-columns: 1fr 280px;
		gap: 1.5rem;
		align-items: center;
		padding: 1rem 1.5rem;
		height: calc(100dvh - 60px);
		box-sizing: border-box;
		overflow: hidden;
	}

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
		width: min(calc((100dvh - 60px - 2rem - 60px) * 0.9), calc(100vw - 280px - 4.5rem));
	}

	:global(#myBoardAnalysis) {
		width: 100% !important;
	}

	.info-panel {
		display: flex;
		flex-direction: column;
		gap: 1rem;
		height: 100%;
		padding: 0.25rem 0;
		overflow-y: auto;
	}

	.analysis-header {
		display: flex;
		justify-content: space-between;
		align-items: center;
		border-bottom: 1px solid var(--border);
		padding-bottom: 0.5rem;
	}

	.analysis-title {
		margin: 0;
		font-size: 1.1rem;
		font-weight: 700;
	}

	.fen-control {
		display: flex;
		flex-direction: column;
		gap: 0.4rem;
	}

	.fen-label {
		font-size: 0.75rem;
		font-weight: 600;
		color: var(--text-secondary);
	}

	.fen-input-group {
		display: flex;
		gap: 0.4rem;
	}

	.fen-input-group .form-input {
		font-size: 0.8rem;
		padding: 0.35rem 0.5rem;
	}

	.fen-input-group .btn-sm {
		font-size: 0.75rem;
		padding: 0.35rem 0.6rem;
	}

	.bot-controls {
		display: grid;
		grid-template-columns: 1fr 1fr;
		gap: 0.4rem;
	}

	.btn-bot {
		font-size: 0.72rem;
		padding: 0.35rem 0.2rem;
		display: flex;
		justify-content: center;
		align-items: center;
		transition: all 0.2s var(--ease);
		white-space: nowrap;
	}

	.active-bot {
		box-shadow: 0 0 8px currentColor;
		filter: brightness(1.2);
	}

	.btn-danger {
		background: #dc3545;
		color: #fff;
		border-color: #dc3545;
	}
	.btn-danger:hover:not(:disabled) { background: #c82333; }

	.btn-dark {
		background: #343a40;
		color: #fff;
		border-color: #343a40;
	}
	.btn-dark:hover:not(:disabled) { background: #23272b; }

	.btn-analyze-toggle {
		grid-column: 1 / -1;
		display: flex;
		justify-content: center;
		align-items: center;
		font-size: 0.75rem;
		font-weight: 600;
		color: var(--text-secondary);
		background: transparent;
		border: 1px solid var(--border-light);
		transition: all 0.2s var(--ease);
		padding: 0.35rem;
	}

	.btn-analyze-toggle:hover {
		background: var(--bg-raised);
		border-color: var(--border-accent);
		color: var(--accent-light);
	}

	.btn-analyze-toggle.btn-analyze-active {
		color: var(--accent-light);
		background: var(--accent-dim);
		border-color: var(--border-accent);
	}

	.history-wrapper {
		flex: 1;
		min-height: 150px;
		background: var(--bg-surface);
		border: 1px solid var(--border);
		border-radius: var(--radius-lg);
		overflow: hidden;
		display: flex;
		flex-direction: column;
	}

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

	.fen-block {
		background: var(--bg-surface);
		border: 1px solid var(--border);
		border-radius: var(--radius-lg);
		padding: 0.85rem;
		display: flex;
		flex-direction: column;
		gap: 0.5rem;
	}

	.fen-header {
		display: flex;
		align-items: center;
		justify-content: space-between;
	}

	.fen-title {
		font-size: 0.72rem;
		font-weight: 700;
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
		padding: 0.25rem 0.5rem;
		border-radius: var(--radius-sm);
		cursor: pointer;
	}

	.btn-copy-fen:hover {
		background: var(--bg-overlay);
		color: var(--text-primary);
		border-color: var(--border-accent);
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
		display: block;
		white-space: nowrap;
		overflow: hidden;
		text-overflow: ellipsis;
	}

	@media (max-width: 900px) {
		.play-page {
			grid-template-columns: 1fr;
			height: auto;
			overflow: auto;
		}
		.board-container {
			width: min(90vw, 500px);
		}
	}
</style>
