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
			successMessage = 'Đã gửi! Kiểm tra hộp thư của bạn để đặt lại mật khẩu.';
		}

		loading = false;
	}
</script>

<svelte:head>
	<title>Quên Mật Khẩu — Cờ Tướng Online</title>
	<meta name="description" content="Đặt lại mật khẩu tài khoản Cờ Tướng Online." />
</svelte:head>

<div class="auth-layout">
	<!-- Decorative panel -->
	<aside class="auth-panel">
		<div class="auth-panel-grid"></div>
		<div class="auth-panel-content">
			<div class="auth-panel-glyph">帥</div>
			<p class="auth-panel-title">Khôi phục tài khoản</p>
			<p class="auth-panel-sub">Nhập email để nhận liên kết đặt lại mật khẩu.</p>
		</div>
	</aside>

	<!-- Form side -->
	<div class="auth-form-side">
		<div class="auth-card">
			<div class="auth-logo">
				<span class="auth-logo-glyph">車</span>
				<span class="auth-logo-name">Cờ Tướng</span>
			</div>

			<h1 class="auth-heading">Quên mật khẩu?</h1>
			<p class="auth-subheading">Không sao. Chúng tôi sẽ gửi liên kết đặt lại cho bạn.</p>

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
				<form on:submit|preventDefault={handleResetPassword} novalidate>
					<div class="form-group">
						<label class="form-label" for="reset-email">Email</label>
						<input
							id="reset-email"
							type="email"
							class="form-input"
							placeholder="you@example.com"
							bind:value={email}
							required
							autocomplete="email"
						/>
					</div>

					<button type="submit" class="btn btn-primary" disabled={loading} id="forgot-submit">
						{#if loading}
							<span class="spinner" aria-hidden="true"></span>
							Đang gửi...
						{:else}
							Gửi liên kết đặt lại
						{/if}
					</button>
				</form>
			{/if}

			<p class="auth-footer-text text-center mt-2">
				<a href="/login" class="text-link">← Quay lại đăng nhập</a>
			</p>
		</div>
	</div>
</div>

<style>
	.auth-footer-text {
		font-size: 0.875rem;
		color: var(--text-secondary);
	}
</style>
