<script lang="ts">
	import { supabase } from '$lib/supabaseClient';
	import { goto } from '$app/navigation';

	let email = '';
	let password = '';
	let errorMessage = '';
	let loading = false;

	async function handleLogin() {
		loading = true;
		errorMessage = '';
		const { data, error } = await supabase.auth.signInWithPassword({
			email: email,
			password: password
		});

		if (error) {
			errorMessage = error.message;
		} else {
			goto('/');
		}

		loading = false;
	}
</script>

<svelte:head>
	<title>Đăng Nhập — Cờ Tướng Online</title>
	<meta name="description" content="Đăng nhập vào Cờ Tướng Online để bắt đầu chơi." />
</svelte:head>

<div class="auth-layout">
	<!-- Decorative panel -->
	<aside class="auth-panel">
		<div class="auth-panel-grid"></div>
		<div class="auth-panel-content">
			<div class="auth-panel-glyph">將</div>
			<p class="auth-panel-title">Cờ Tướng Online</p>
			<p class="auth-panel-sub">Chiến đấu theo chiến thuật. Chiến thắng bằng trí tuệ.</p>
		</div>
	</aside>

	<!-- Form side -->
	<div class="auth-form-side">
		<div class="auth-card">
			<div class="auth-logo">
				<span class="auth-logo-glyph">車</span>
				<span class="auth-logo-name">Cờ Tướng</span>
			</div>

			<h1 class="auth-heading">Đăng nhập</h1>
			<p class="auth-subheading">Chào mừng trở lại. Tiếp tục hành trình của bạn.</p>

			{#if errorMessage}
				<div class="alert alert-error" role="alert">
					<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
						<circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/>
					</svg>
					{errorMessage}
				</div>
			{/if}

			<form on:submit|preventDefault={handleLogin} novalidate>
				<div class="form-group">
					<label class="form-label" for="email">Email</label>
					<input
						id="email"
						type="email"
						class="form-input"
						placeholder="you@example.com"
						bind:value={email}
						required
						autocomplete="email"
					/>
				</div>

				<div class="form-group">
					<div class="pw-label-row">
						<label class="form-label" for="password">Mật khẩu</label>
						<a href="/forgot-password" class="text-link" style="font-size: 0.78rem;">Quên mật khẩu?</a>
					</div>
					<input
						id="password"
						type="password"
						class="form-input"
						placeholder="••••••••"
						bind:value={password}
						required
						autocomplete="current-password"
					/>
				</div>

				<button type="submit" class="btn btn-primary" disabled={loading} id="login-submit">
					{#if loading}
						<span class="spinner" aria-hidden="true"></span>
						Đang đăng nhập...
					{:else}
						Đăng nhập
					{/if}
				</button>
			</form>

			<p class="auth-footer-text text-center mt-2">
				Chưa có tài khoản?
				<a href="/register" class="text-link">Đăng ký ngay</a>
			</p>
		</div>
	</div>
</div>

<style>
	.pw-label-row {
		display: flex;
		justify-content: space-between;
		align-items: center;
	}

	.auth-footer-text {
		font-size: 0.875rem;
		color: var(--text-secondary);
	}
</style>