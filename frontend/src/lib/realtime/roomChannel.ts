import { parseRoomState, type RoomState } from '$lib/types/game';

interface RoomChannelOptions {
	roomId: string;
	onState: (room: RoomState) => void;
	onError: (error: unknown) => void;
}

export interface RoomChannel {
	start: () => Promise<void>;
	stop: () => void;
}

/**
 * Subscribes to a room's Server-Sent Events stream. The browser's EventSource
 * delivers the current state on connect and on every server-side change, and
 * transparently reconnects if the connection drops — so there is no polling.
 */
export function createRoomChannel(options: RoomChannelOptions): RoomChannel {
	let source: EventSource | undefined;

	return {
		start: async () => {
			if (source) {
				return;
			}

			source = new EventSource(`/api/room/${encodeURIComponent(options.roomId)}/events`);

			source.onmessage = (event) => {
				try {
					options.onState(parseRoomState(JSON.parse(event.data)));
				} catch (error) {
					options.onError(error);
				}
			};

			// EventSource auto-reconnects after an error, so keep the stream open and
			// just surface the hiccup rather than tearing it down.
			source.onerror = (error) => {
				options.onError(error);
			};
		},
		stop: () => {
			source?.close();
			source = undefined;
		}
	};
}
