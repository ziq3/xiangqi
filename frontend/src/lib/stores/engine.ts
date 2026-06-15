import { writable } from 'svelte/store';
import type { EngineAnalysis } from '$lib/types/engine';

interface EngineState {
	analysis: EngineAnalysis | null;
	loading: boolean;
	error: string;
	enabled: boolean;
}

function createEngineStore() {
	const { subscribe, update, set } = writable<EngineState>({
		analysis: null,
		loading: false,
		error: '',
		enabled: false
	});

	let eventSource: EventSource | null = null;

	function stopAnalysis() {
		if (eventSource) {
			eventSource.close();
			eventSource = null;
		}
	}

	function analyze(fen: string) {
		stopAnalysis();
		
		update((current) => {
			if (!current.enabled) return current;
			return { ...current, loading: true, error: '', analysis: null };
		});

		try {
			const url = `/api/engine/stream?fen=${encodeURIComponent(fen)}`;
			eventSource = new EventSource(url);

			eventSource.onmessage = (event) => {
				try {
					const result: EngineAnalysis = JSON.parse(event.data);
					update((current) => ({ ...current, analysis: result, loading: false }));
				} catch (err) {
					console.error("Failed to parse engine analysis stream data:", err);
				}
			};

			eventSource.onerror = (error) => {
				update((current) => ({ ...current, loading: false, error: 'Connection to analysis stream lost' }));
				stopAnalysis();
			};

		} catch (error) {
			const message = error instanceof Error ? error.message : 'Analysis failed to start';
			update((current) => ({ ...current, loading: false, error: message }));
		}
	}

	function setEnabled(enabled: boolean) {
		update((current) => {
			if (!enabled) {
				stopAnalysis();
				return { ...current, enabled, analysis: null, loading: false, error: '' };
			}
			return { ...current, enabled };
		});
	}

	function clear() {
		stopAnalysis();
		update((current) => ({ ...current, analysis: null, loading: false, error: '' }));
	}

	return {
		subscribe,
		analyze,
		setEnabled,
		clear,
		stopAnalysis
	};
}

export const engineStore = createEngineStore();
