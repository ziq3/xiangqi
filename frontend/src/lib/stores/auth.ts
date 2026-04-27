import { browser } from '$app/environment';
import { writable } from 'svelte/store';
import type { Session, User } from '@supabase/supabase-js';
import { supabase } from '$lib/supabaseClient';

export interface AuthState {
	initialized: boolean;
	session: Session | null;
	user: User | null;
	guestName: string;
}

const DEFAULT_GUEST_NAME = 'Guest';
const GUEST_KEY = 'xiangqi_guest_name';

function loadGuestName(): string {
	if (!browser) {
		return DEFAULT_GUEST_NAME;
	}

	const stored = window.localStorage.getItem(GUEST_KEY)?.trim();
	if (stored) {
		return stored;
	}

	window.localStorage.setItem(GUEST_KEY, DEFAULT_GUEST_NAME);
	return DEFAULT_GUEST_NAME;
}

function createAuthStore() {
	const { subscribe, update, set } = writable<AuthState>({
		initialized: false,
		session: null,
		user: null,
		guestName: loadGuestName()
	});

	let initialized = false;

	async function init() {
		if (!browser || initialized) {
			return;
		}

		initialized = true;
		const { data } = await supabase.auth.getSession();
		set({
			initialized: true,
			session: data.session,
			user: data.session?.user ?? null,
			guestName: loadGuestName()
		});

		supabase.auth.onAuthStateChange((_event, session) => {
			update((current) => ({
				...current,
				initialized: true,
				session,
				user: session?.user ?? null
			}));
		});
	}

	function setGuestName(name: string) {
		const normalized = name.trim() || DEFAULT_GUEST_NAME;
		if (browser) {
			window.localStorage.setItem(GUEST_KEY, normalized);
		}
		update((current) => ({ ...current, guestName: normalized }));
	}

	async function logout() {
		await supabase.auth.signOut();
	}

	return {
		subscribe,
		init,
		setGuestName,
		logout
	};
}

export const authStore = createAuthStore();

export function resolveDisplayName(state: AuthState): string {
	const metadataName = state.user?.user_metadata?.username;
	if (typeof metadataName === 'string' && metadataName.trim()) {
		return metadataName.trim();
	}
	if (state.user?.email) {
		return state.user.email;
	}
	return state.guestName;
}
