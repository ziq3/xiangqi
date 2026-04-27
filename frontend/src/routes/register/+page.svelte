<script lang="ts">
	import { supabase } from '$lib/supabaseClient';
	import { goto } from '$app/navigation';
	let email = '';
	let password = '';
	let errorMessage = '';
	let loading = false;
	async function handleRegister() {
		loading = true;
		const { data, error } = await supabase.auth.signUp({
			email: email,
			password: password
		});
		if (error) {
			errorMessage = error.message;
		} else {
			goto('/login');
		}
		loading = false;
	}
</script>

<svelte:head>
	<title>Đăng Ký</title>
</svelte:head>
<h2>Đăng Ký</h2>
{#if errorMessage}
	<p style="color: red;">{errorMessage}</p>
{/if}
<form on:submit|preventDefault={handleRegister}>
	<input type="email" placeholder="Email" bind:value={email} required />
	<input type="password" placeholder="Password" bind:value={password} required />
	<button type="submit" disabled={loading}>
		{loading ? 'Registering...' : 'Register'}
	</button>
</form>
