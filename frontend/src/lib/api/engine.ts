import { apiRequest } from '$lib/api/client';
import type { EngineAnalysis } from '$lib/types/engine';

export async function analyzeFen(fen: string, movetime?: number): Promise<EngineAnalysis> {
	let url = `/api/engine/analyze?fen=${encodeURIComponent(fen)}`;
	if (movetime) {
		url += `&movetime=${movetime}`;
	}
	const response = await apiRequest<EngineAnalysis>(url);
	return response;
}
