import { ApiError } from '$lib/types/game';

interface RequestOptions {
	method?: 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE';
	body?: unknown;
	headers?: Record<string, string>;
}

export async function apiRequest<T>(path: string, options: RequestOptions = {}): Promise<T> {
	const response = await fetch(path, {
		method: options.method ?? 'GET',
		headers: {
			'Content-Type': 'application/json',
			...options.headers
		},
		body: options.body ? JSON.stringify(options.body) : undefined
	});

	if (!response.ok) {
		let message = 'Request failed';

		try {
			const errorPayload = (await response.json()) as Record<string, unknown>;
			if (typeof errorPayload.message === 'string') {
				message = errorPayload.message;
			}
		} catch {
			message = response.statusText || message;
		}

		throw new ApiError(message, response.status);
	}

	if (response.status === 204) {
		return undefined as T;
	}

	return (await response.json()) as T;
}
