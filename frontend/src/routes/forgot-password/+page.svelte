<script lang="ts">
    import { supabase } from '$lib/supabaseClient';

    let email = '';
    let errorMessage = '';
    let successMessage = '';
    let loading = false;

    async function handleResetPassword() {
        loading = true;
        errorMessage = '';
        successMessage = '';
        
        const { error } = await supabase.auth.resetPasswordForEmail(email, {
            redirectTo: window.location.origin + '/reset-password'
        });

        if (error) {
            errorMessage = error.message;
        } else {
            successMessage = 'Vui lòng kiểm tra email của bạn để đặt lại mật khẩu.';
        }
        
        loading = false;
    }
</script>

<svelte:head>
    <title>Quên Mật Khẩu</title>
</svelte:head>

<h2>Quên Mật Khẩu</h2>

{#if errorMessage}
    <p style="color: red;">{errorMessage}</p>
{/if}
{#if successMessage}
    <p style="color: green;">{successMessage}</p>
{/if}

<form on:submit|preventDefault={handleResetPassword}>
    <input type="email" placeholder="Email" bind:value={email} required />
    <button type="submit" disabled={loading}>
        {loading ? 'Đang gửi...' : 'Gửi liên kết đặt lại'}
    </button>
</form>

<div style="margin-top: 1rem;">
    <a href="/login" style="text-decoration: none; color: #3b82f6;">Quay lại đăng nhập</a>
</div>
