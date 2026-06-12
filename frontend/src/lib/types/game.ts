export type Turn = 'HOST' | 'GUEST';

export type RoomStatus = 'WAITING' | 'PLAYING' | 'FINISHED';

export interface RoomState {
	roomId: string;
	hostName: string;
	guestName: string;
	turn: Turn;
	status: RoomStatus;
	fen: string;
	hostTimeMs: number;
	guestTimeMs: number;
	endReason: string | null;
	moveHistory: string;
}

export class ApiError extends Error {
	readonly status: number;

	constructor(message: string, status: number) {
		super(message);
		this.name = 'ApiError';
		this.status = status;
	}
}

function isTurn(value: unknown): value is Turn {
	return value === 'HOST' || value === 'GUEST';
}

function isRoomStatus(value: unknown): value is RoomStatus {
	return value === 'WAITING' || value === 'PLAYING' || value === 'FINISHED';
}

export function parseRoomState(payload: unknown): RoomState {
	if (!payload || typeof payload !== 'object') {
		throw new ApiError('Invalid room response payload', 500);
	}

	const candidate = payload as Record<string, unknown>;
	const { roomId, hostName, guestName, turn, status, fen, hostTimeMs, guestTimeMs, endReason, moveHistory } =
		candidate;

	if (
		typeof roomId !== 'string' ||
		typeof hostName !== 'string' ||
		typeof guestName !== 'string' ||
		!isTurn(turn) ||
		!isRoomStatus(status) ||
		typeof fen !== 'string'
	) {
		throw new ApiError('Unexpected room response shape', 500);
	}

	const safeHostTimeMs = typeof hostTimeMs === 'number' ? hostTimeMs : 0;
	const safeGuestTimeMs = typeof guestTimeMs === 'number' ? guestTimeMs : 0;
	const safeEndReason = typeof endReason === 'string' ? endReason : null;
	const safeMoveHistory = typeof moveHistory === 'string' ? moveHistory : '';

	return {
		roomId,
		hostName,
		guestName,
		turn,
		status,
		fen,
		hostTimeMs: safeHostTimeMs,
		guestTimeMs: safeGuestTimeMs,
		endReason: safeEndReason,
		moveHistory: safeMoveHistory
	};
}
