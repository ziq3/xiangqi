<script lang="ts">
	import { onMount } from 'svelte';
	import { derived } from 'svelte/store';
	import { authStore, resolveDisplayName } from '$lib/stores/auth';
	import favicon from '$lib/assets/favicon.svg';
	import '$lib/assets/global.css';

	let { children } = $props();

	onMount(() => {
		void authStore.init();
	});

	const displayName = derived(authStore, ($authState) => resolveDisplayName($authState));
</script>

<svelte:head>
	<link rel="icon" href={favicon} />
</svelte:head>

<header class="topbar">
	<a href="/" class="topbar-brand">
		<span class="topbar-glyph">車</span>
		<span class="topbar-name">Cờ Tướng</span>
	</a>

	<nav class="nav-links">
		<a href="/" class="nav-item">Trang chủ</a>
		{#if !$authStore.user}
			<a href="/login" class="nav-item">Đăng nhập</a>
			<a href="/register" class="nav-item nav-item--cta">Đăng ký</a>
		{/if}
	</nav>

	<div class="session-controls">
		{#if $authStore.user}
			<span class="display-name">
				<span class="display-name-dot"></span>
				{$displayName}
			</span>
			<button type="button" class="btn-logout" onclick={() => authStore.logout()}>
				Đăng xuất
			</button>
		{/if}
	</div>
</header>

{@render children()}

<style>
	.topbar {
		position: sticky;
		top: 0;
		z-index: 100;
		display: flex;
		align-items: center;
		gap: 1.5rem;
		padding: 0 1.5rem;
		height: 60px;
		background: rgba(14, 14, 18, 0.85);
		backdrop-filter: blur(12px);
		-webkit-backdrop-filter: blur(12px);
		border-bottom: 1px solid var(--border);
	}

	.topbar-brand {
		display: flex;
		align-items: center;
		gap: 0.5rem;
		text-decoration: none;
		flex-shrink: 0;
	}

	.topbar-glyph {
		font-family: var(--font-serif);
		font-size: 1.5rem;
		color: var(--accent);
		line-height: 1;
		text-shadow: 0 0 20px var(--accent-glow);
	}

	.topbar-name {
		font-size: 0.95rem;
		font-weight: 700;
		letter-spacing: 0.03em;
		color: var(--text-primary);
	}

	.nav-links {
		display: flex;
		align-items: center;
		gap: 0.25rem;
		flex: 1;
	}

	.nav-item {
		padding: 0.4rem 0.75rem;
		border-radius: var(--radius-sm);
		font-size: 0.88rem;
		font-weight: 500;
		color: var(--text-secondary);
		text-decoration: none;
		transition: color 0.15s, background 0.15s;
	}
	.nav-item:hover {
		color: var(--text-primary);
		background: var(--bg-raised);
	}

	.nav-item--cta {
		background: var(--accent-dim);
		color: var(--accent-light);
		border: 1px solid var(--border-accent);
		margin-left: 0.25rem;
	}
	.nav-item--cta:hover {
		background: var(--accent-glow);
		color: #fff;
	}

	.session-controls {
		display: flex;
		align-items: center;
		gap: 0.75rem;
		margin-left: auto;
		flex-shrink: 0;
	}

	.display-name {
		display: flex;
		align-items: center;
		gap: 0.4rem;
		font-size: 0.85rem;
		color: var(--text-secondary);
	}

	.display-name-dot {
		width: 6px;
		height: 6px;
		border-radius: 50%;
		background: var(--success-light);
		box-shadow: 0 0 6px var(--success-light);
		animation: blink 2.5s ease-in-out infinite;
	}

	@keyframes blink {
		0%, 100% { opacity: 1; }
		50%       { opacity: 0.4; }
	}

	.btn-logout {
		padding: 0.35rem 0.8rem;
		background: transparent;
		border: 1px solid var(--border-light);
		border-radius: var(--radius-sm);
		color: var(--text-secondary);
		font-family: var(--font-sans);
		font-size: 0.8rem;
		font-weight: 500;
		cursor: pointer;
		transition: border-color 0.15s, color 0.15s, background 0.15s;
	}
	.btn-logout:hover {
		border-color: var(--border-accent);
		color: var(--text-primary);
		background: var(--accent-dim);
	}
</style>
