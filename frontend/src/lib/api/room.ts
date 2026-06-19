import { apiRequest } from '$lib/api/client';
import { parseRoomState, type RoomState } from '$lib/types/game';

export async function createRoom(hostName: string, isBotGame = false): Promise<RoomState> {
	const response = await apiRequest<unknown>(
		`/api/room/create?hostName=${encodeURIComponent(hostName)}&isBotGame=${isBotGame}`,
		{
			method: 'POST'
		}
	);

	return parseRoomState(response);
}

export async function joinRoom(roomId: string, playerName: string): Promise<RoomState> {
	const response = await apiRequest<unknown>(
		`/api/room/${encodeURIComponent(roomId)}/join?playerName=${encodeURIComponent(playerName)}`,
		{
			method: 'POST'
		}
	);

	return parseRoomState(response);
}

export async function getRoom(roomId: string): Promise<RoomState> {
	const response = await apiRequest<unknown>(`/api/room/${encodeURIComponent(roomId)}`);
	return parseRoomState(response);
}

export async function readyRoom(roomId: string, side: 'HOST' | 'GUEST'): Promise<RoomState> {
	const response = await apiRequest<unknown>(
		`/api/room/${encodeURIComponent(roomId)}/ready?side=${side}`,
		{ method: 'POST' }
	);
	return parseRoomState(response);
}

export async function updateRoomFen(roomId: string, fen: string, move: string, isCheckmate: boolean = false): Promise<RoomState> {
	const response = await apiRequest<unknown>(`/api/room/${encodeURIComponent(roomId)}/move`, {
		method: 'POST',
		body: { fen, move, checkmate: isCheckmate }
	});

	return parseRoomState(response);
}

export async function listMatches(): Promise<RoomState[]> {
	const response = await apiRequest<unknown[]>('/api/user/listmatch');
	if (!Array.isArray(response)) {
		return [];
	}
	return response.map(parseRoomState);
}
