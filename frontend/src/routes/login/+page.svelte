<script lang="ts">
    import { supabase } from '$lib/supabaseClient';
    import { goto } from '$app/navigation';

    let email = '';
    let password = '';
    let errorMessage = '';
    let loading = false;

    async function handleLogin() {
        loading = true;
        const { data, error } = await supabase.auth.signInWithPassword({
            email: email,
            password: password
        });

        if (error) {
            errorMessage = error.message;
        } else {
            // Redirect to home page on successful login
            goto('/'); 
        }
        
        loading = false;
    }
</script>

<svelte:head>
    <title>Đăng Nhập</title>
</svelte:head>

<h2>Đăng Nhập</h2>

{#if errorMessage}
    <p style="color: red;">{errorMessage}</p>
{/if}

<form on:submit|preventDefault={handleLogin}>
    <input type="email" placeholder="Email" bind:value={email} required />
    <input type="password" placeholder="Password" bind:value={password} required />
    <button type="submit" disabled={loading}>
        {loading ? 'Logging in...' : 'Login'}
    </button>
</form>