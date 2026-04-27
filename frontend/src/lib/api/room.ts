import { apiRequest } from '$lib/api/client';
import { parseRoomState, type RoomState } from '$lib/types/game';

export async function createRoom(hostName: string): Promise<RoomState> {
	const response = await apiRequest<unknown>(
		`/api/room/create?hostName=${encodeURIComponent(hostName)}`,
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

export async function startRoom(roomId: string): Promise<RoomState> {
	const response = await apiRequest<unknown>(`/api/room/${encodeURIComponent(roomId)}/start`, {
		method: 'POST'
	});
	return parseRoomState(response);
}

export async function updateRoomFen(roomId: string, fen: string): Promise<RoomState> {
	const response = await apiRequest<unknown>(`/api/room/${encodeURIComponent(roomId)}/move`, {
		method: 'POST',
		body: { move: fen }
	});

	return parseRoomState(response);
}
