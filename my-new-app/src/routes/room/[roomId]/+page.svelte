<script lang="ts">
	import { onMount } from 'svelte';
	import { page } from '$app/stores';
	$: roomId = $page.params.roomId;

	let room: any = null;
	let board: any = null;
	let engineWorker: Worker;
	let game: any = null;
	function onDrop(source: string, target: string, piece: string, newPos: any, oldPos: any) {
		if (room && board) {
			const futureFen = window.Xiangqiboard.objToFen(newPos);

			console.log('Old FEN:', room.fen);
			console.log('New FEN:', futureFen);

			room.fen = futureFen;

			if (room.guestName == 'BOT') {
				sendCommand('position fen ' + room.fen);
				sendCommand('go movetime 500');
			}
			updateFen(room.fen);
		}
	}

	function sendCommand(command: string) {
		if (engineWorker) {
			engineWorker.postMessage(command);
		}
	}

	function onDragStart(source: string, piece: string) {
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

	async function fetchRoom() {
		const response = await fetch(`/api/room/${roomId}`, { method: 'GET' });
		room = await response.json();
	}

	function updateFen(fen: string) {
		fetch(`/api/room/${roomId}/move`, {
			method: 'POST',
			headers: {
				'Content-Type': 'application/json'
			},
			body: JSON.stringify({ move: fen })
		});
	}

	onMount(() => {
		let fetchInterval: ReturnType<typeof setInterval>;

		async function setup() {
			await fetchRoom();
			board = window.Xiangqiboard('myBoard', config);
			board.position(room.fen, false);

			engineWorker = new Worker(`/js/wrapper.js`);
			engineWorker.onmessage = function (event) {
				if (room.guestName !== 'BOT') return;
				const line = event.data;
				if (line.startsWith('bestmove')) {
					let bestMove = line.split(' ')[1];
					let moveForBoard = bestMove.substring(0, 2) + '-' + bestMove.substring(2, 4);
					board.move(moveForBoard);
					updateFen(board.fen());
					console.log('Bot Move:', board.fen);
				}
			};

			fetchInterval = setInterval(async () => {
				const oldFen = room?.fen;
				await fetchRoom();
				if (board && room?.fen && room.fen !== oldFen) {
					board.position(room.fen, false);
				}
			}, 500);
		}

		setup();

		return () => {
			if (fetchInterval) clearInterval(fetchInterval);
			if (engineWorker) engineWorker.terminate();
			if (board) board.destroy();
		};
	});
</script>

<svelte:head>
	<title>Cờ tướng - Vs Computer</title>
</svelte:head>

<div id="myBoard"></div>


<style>
	#myBoard {
		width: min(88vh, 100vw);
	}
</style>
