<script lang="ts">
	import { supabase } from '$lib/supabaseClient';
	import { goto } from '$app/navigation';
	import { onMount } from 'svelte';

	let newPassword = '';
	let errorMessage = '';
	let successMessage = '';
	let loading = false;

	onMount(() => {
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
			successMessage = 'Mật khẩu đã được cập nhật thành công! Đang chuyển hướng...';
			setTimeout(() => {
				goto('/');
			}, 2000);
		}

		loading = false;
	}
</script>

<svelte:head>
	<title>Đặt Lại Mật Khẩu — Cờ Tướng Online</title>
	<meta name="description" content="Đặt mật khẩu mới cho tài khoản Cờ Tướng Online." />
</svelte:head>

<div class="auth-layout">
	<!-- Decorative panel -->
	<aside class="auth-panel">
		<div class="auth-panel-grid"></div>
		<div class="auth-panel-content">
			<div class="auth-panel-glyph">兵</div>
			<p class="auth-panel-title">Mật khẩu mới</p>
			<p class="auth-panel-sub">Chọn mật khẩu mạnh để bảo vệ tài khoản của bạn.</p>
		</div>
	</aside>

	<!-- Form side -->
	<div class="auth-form-side">
		<div class="auth-card">
			<div class="auth-logo">
				<span class="auth-logo-glyph">車</span>
				<span class="auth-logo-name">Cờ Tướng</span>
			</div>

			<h1 class="auth-heading">Đặt lại mật khẩu</h1>
			<p class="auth-subheading">Nhập mật khẩu mới cho tài khoản của bạn.</p>

			{#if errorMessage}
				<div class="alert alert-error" role="alert">
					<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
						<circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/>
					</svg>
					{errorMessage}
				</div>
			{/if}

			{#if successMessage}
				<div class="alert alert-success" role="status">
					<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
						<polyline points="20 6 9 17 4 12"/>
					</svg>
					{successMessage}
				</div>
			{/if}

			{#if !successMessage}
				<form on:submit|preventDefault={handleUpdatePassword} novalidate>
					<div class="form-group">
						<label class="form-label" for="new-password">Mật khẩu mới</label>
						<input
							id="new-password"
							type="password"
							class="form-input"
							placeholder="Tối thiểu 6 ký tự"
							bind:value={newPassword}
							required
							minlength="6"
							autocomplete="new-password"
						/>
					</div>

					<button
						type="submit"
						class="btn btn-primary"
						disabled={loading || successMessage !== ''}
						id="reset-submit"
					>
						{#if loading}
							<span class="spinner" aria-hidden="true"></span>
							Đang cập nhật...
						{:else}
							Cập nhật mật khẩu
						{/if}
					</button>
				</form>
			{/if}
		</div>
	</div>
</div>
