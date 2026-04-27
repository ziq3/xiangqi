import { writable } from 'svelte/store';
import { createRoomChannel, type RoomChannel } from '$lib/realtime/roomChannel';
import { getRoom, updateRoomFen, startRoom as startRoomApi } from '$lib/api/room';
import type { RoomState } from '$lib/types/game';

interface GameState {
	room: RoomState | null;
	loading: boolean;
	error: string;
	pendingMove: boolean;
}

function createGameStore() {
	const { subscribe, update, set } = writable<GameState>({
		room: null,
		loading: false,
		error: '',
		pendingMove: false
	});

	let channel: RoomChannel | null = null;

	async function loadRoom(roomId: string) {
		update((current) => ({ ...current, loading: true, error: '' }));
		try {
			const room = await getRoom(roomId);
			update((current) => ({ ...current, room, loading: false }));
			return room;
		} catch (error) {
			const message = error instanceof Error ? error.message : 'Failed to load room';
			update((current) => ({ ...current, loading: false, error: message }));
			return null;
		}
	}

	async function startGame(roomId: string) {
		update((current) => ({ ...current, loading: true, error: '' }));
		try {
			const room = await startRoomApi(roomId);
			update((current) => ({ ...current, room, loading: false }));
			return room;
		} catch (error) {
			const message = error instanceof Error ? error.message : 'Failed to start game';
			update((current) => ({ ...current, loading: false, error: message }));
			return null;
		}
	}

	async function submitMove(roomId: string, fen: string) {
		let previousFen = '';
		update((current) => {
			previousFen = current.room?.fen ?? '';
			return {
				...current,
				pendingMove: true,
				error: '',
				room: current.room ? { ...current.room, fen } : current.room
			};
		});

		try {
			const room = await updateRoomFen(roomId, fen);
			update((current) => ({ ...current, room, pendingMove: false }));
			return room;
		} catch (error) {
			const message = error instanceof Error ? error.message : 'Move update failed';
			update((current) => ({
				...current,
				pendingMove: false,
				error: message,
				room: current.room ? { ...current.room, fen: previousFen } : current.room
			}));
			return null;
		}
	}

	async function startRoomSync(roomId: string) {
		channel?.stop();
		channel = createRoomChannel({
			roomId,
			onState: (room) => {
				update((current) => {
					if (current.pendingMove) {
						return current;
					}
					return { ...current, room };
				});
			},
			onError: (error) => {
				const message = error instanceof Error ? error.message : 'Room sync failed';
				update((current) => ({ ...current, error: message }));
			}
		});

		await channel.start();
	}

	function stopRoomSync() {
		channel?.stop();
		channel = null;
	}

	function reset() {
		stopRoomSync();
		set({
			room: null,
			loading: false,
			error: '',
			pendingMove: false
		});
	}

	return {
		subscribe,
		loadRoom,
		startGame,
		submitMove,
		startRoomSync,
		stopRoomSync,
		reset
	};
}

export const gameStore = createGameStore();
