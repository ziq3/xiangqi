<script lang="ts">
	import { onMount } from 'svelte';
	import { derived } from 'svelte/store';
	import { authStore, resolveDisplayName } from '$lib/stores/auth';
	import favicon from '$lib/assets/favicon.svg';

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
	<nav class="nav-links">
		<a href="/">Trang chu</a>
		{#if !$authStore.user}
			<a href="/login">Dang nhap</a>
			<a href="/register">Dang ky</a>
		{/if}
	</nav>

	<div class="session-controls">
		<span class="display-name">Nguoi choi: {$displayName}</span>
		{#if $authStore.user}
			<button type="button" onclick={() => authStore.logout()}>Dang xuat</button>
		{/if}
	</div>
</header>

{@render children()}

<style>
	.topbar {
		display: flex;
		justify-content: space-between;
		align-items: center;
		gap: 1rem;
		padding: 0.75rem 1rem;
		border-bottom: 1px solid #ddd;
		margin-bottom: 1rem;
		flex-wrap: wrap;
	}

	.nav-links {
		display: flex;
		gap: 0.75rem;
	}

	.nav-links a {
		text-decoration: none;
		color: #1f2937;
		font-weight: 600;
	}

	.session-controls {
		display: flex;
		align-items: center;
		gap: 0.75rem;
	}

	.display-name {
		font-size: 0.9rem;
		color: #374151;
	}

	button {
		padding: 0.4rem 0.7rem;
		border: 1px solid #cbd5e1;
		background: #f8fafc;
		border-radius: 6px;
		cursor: pointer;
	}
</style>
