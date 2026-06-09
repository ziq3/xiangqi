<script lang="ts">
    import { supabase } from '$lib/supabaseClient';
    import { goto } from '$app/navigation';
    import { onMount } from 'svelte';

    let newPassword = '';
    let errorMessage = '';
    let successMessage = '';
    let loading = false;

    onMount(() => {
        // Supabase will automatically parse the hash from the reset link and set the session.
        const { data: authListener } = supabase.auth.onAuthStateChange((event, session) => {
            if (event === 'PASSWORD_RECOVERY') {
                console.log('Password recovery mode enabled');
            }
        });

        return () => {
            authListener.subscription.unsubscribe();
        };
    });

    async function handleUpdatePassword() {
        loading = true;
        errorMessage = '';
        successMessage = '';
        
        const { data, error } = await supabase.auth.updateUser({
            password: newPassword
        });

        if (error) {
            errorMessage = error.message;
        } else {
            successMessage = 'Mật khẩu đã được cập nhật thành công. Đang chuyển hướng...';
            setTimeout(() => {
                goto('/'); 
            }, 2000);
        }
        
        loading = false;
    }
</script>

<svelte:head>
    <title>Đặt Lại Mật Khẩu</title>
</svelte:head>

<h2>Đặt Lại Mật Khẩu</h2>

{#if errorMessage}
    <p style="color: red;">{errorMessage}</p>
{/if}
{#if successMessage}
    <p style="color: green;">{successMessage}</p>
{/if}

<form on:submit|preventDefault={handleUpdatePassword}>
    <input type="password" placeholder="Mật khẩu mới" bind:value={newPassword} required minlength="6" />
    <button type="submit" disabled={loading || successMessage !== ''}>
        {loading ? 'Đang cập nhật...' : 'Cập nhật mật khẩu'}
    </button>
</form>
