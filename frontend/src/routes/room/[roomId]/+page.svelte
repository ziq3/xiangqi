<script lang="ts">
	import { onMount } from 'svelte';
	import { page } from '$app/stores';
	import { gameStore } from '$lib/stores/game';
	import { authStore, resolveDisplayName } from '$lib/stores/auth';
	import type { RoomState } from '$lib/types/game';
	$: roomId = $page.params.roomId ?? '';

	let board: any = null;
	let game: any = null;

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
		});

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
</script>

<svelte:head>
	<title>Cờ tướng - Vs Computer</title>
</svelte:head>

<div id="myBoard"></div>

<div class="controls">
	{#if room && room.status === 'WAITING' && room.hostName === currentPlayerName}
		<button class="start-btn" on:click={startGame}>Bat dau choi</button>
	{/if}
</div>

{#if $gameStore.error}
	<p class="error">{$gameStore.error}</p>
{/if}

<p class="status">
	{#if room}
		Phong: {room.roomId} | Trang thai: {room.status} | Luot: {room.turn} | Che do: {room.guestName ===
		'BOT'
			? 'Vs BOT'
			: 'Vs nguoi choi'}
	{:else}
		Dang tai du lieu phong...
	{/if}
</p>

<style>
	#myBoard {
		width: min(88vh, 100vw);
	}

	.error {
		color: #b91c1c;
		margin: 0.5rem 0;
	}

	.status {
		margin-top: 0.5rem;
		font-size: 0.95rem;
		color: #334155;
	}

	.controls {
		margin: 1rem 0;
	}

	.start-btn {
		padding: 0.5rem 1rem;
		background-color: #2563eb;
		color: white;
		border: none;
		border-radius: 4px;
		font-weight: bold;
		cursor: pointer;
	}

	.start-btn:hover {
		background-color: #1d4ed8;
	}
</style>
