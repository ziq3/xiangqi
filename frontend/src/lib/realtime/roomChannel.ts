import { getRoom } from '$lib/api/room';
import type { RoomState } from '$lib/types/game';

interface RoomChannelOptions {
	roomId: string;
	intervalMs?: number;
	onState: (room: RoomState) => void;
	onError: (error: unknown) => void;
}

export interface RoomChannel {
	start: () => Promise<void>;
	stop: () => void;
}

export function createRoomChannel(options: RoomChannelOptions): RoomChannel {
	const intervalMs = options.intervalMs ?? 500;
	let timer: ReturnType<typeof setInterval> | undefined;
	let active = false;

	async function sync() {
		try {
			const next = await getRoom(options.roomId);
			options.onState(next);
		} catch (error) {
			options.onError(error);
		}
	}

	return {
		start: async () => {
			if (active) {
				return;
			}
			active = true;
			await sync();
			timer = setInterval(() => {
				void sync();
			}, intervalMs);
		},
		stop: () => {
			active = false;
			if (timer) {
				clearInterval(timer);
				timer = undefined;
			}
		}
	};
}
