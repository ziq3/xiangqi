<script lang="ts">
	import { supabase } from '$lib/supabaseClient';
	import { goto } from '$app/navigation';

	let username = '';
	let email = '';
	let password = '';
	let errorMessage = '';
	let loading = false;

	async function handleRegister() {
		if (!username.trim()) {
			errorMessage = 'Vui lòng nhập tên người chơi';
			return;
		}

		loading = true;
		errorMessage = '';
		const { data, error } = await supabase.auth.signUp({
			email: email,
			password: password,
			options: {
				data: {
					username: username.trim()
				}
			}
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
	<title>Đăng Ký — Cờ Tướng Online</title>
	<meta name="description" content="Tạo tài khoản Cờ Tướng Online để bắt đầu chơi." />
</svelte:head>

<div class="auth-layout">
	<!-- Decorative panel -->
	<aside class="auth-panel">
		<div class="auth-panel-grid"></div>
		<div class="auth-panel-content">
			<div class="auth-panel-glyph">象</div>
			<p class="auth-panel-title">Tham gia chiến trường</p>
			<p class="auth-panel-sub">Tạo tài khoản và thách thức đối thủ khắp nơi.</p>
		</div>
	</aside>

	<!-- Form side -->
	<div class="auth-form-side">
		<div class="auth-card">
			<div class="auth-logo">
				<span class="auth-logo-glyph">車</span>
				<span class="auth-logo-name">Cờ Tướng</span>
			</div>

			<h1 class="auth-heading">Đăng ký</h1>
			<p class="auth-subheading">Tạo tài khoản miễn phí để bắt đầu.</p>

			{#if errorMessage}
				<div class="alert alert-error" role="alert">
					<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
						<circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/>
					</svg>
					{errorMessage}
				</div>
			{/if}

			<form on:submit|preventDefault={handleRegister} novalidate>
				<div class="form-group">
					<label class="form-label" for="reg-username">Tên người chơi</label>
					<input
						id="reg-username"
						type="text"
						class="form-input"
						placeholder="Ví dụ: Lữ Bố, Gia Cát Lượng..."
						bind:value={username}
						required
						autocomplete="username"
					/>
				</div>

				<div class="form-group">
					<label class="form-label" for="reg-email">Email</label>
					<input
						id="reg-email"
						type="email"
						class="form-input"
						placeholder="you@example.com"
						bind:value={email}
						required
						autocomplete="email"
					/>
				</div>

				<div class="form-group">
					<label class="form-label" for="reg-password">Mật khẩu</label>
					<input
						id="reg-password"
						type="password"
						class="form-input"
						placeholder="Tối thiểu 6 ký tự"
						bind:value={password}
						required
						minlength="6"
						autocomplete="new-password"
					/>
				</div>

				<button type="submit" class="btn btn-primary" disabled={loading} id="register-submit">
					{#if loading}
						<span class="spinner" aria-hidden="true"></span>
						Đang tạo tài khoản...
					{:else}
						Tạo tài khoản
					{/if}
				</button>
			</form>

			<p class="auth-footer-text text-center mt-2">
				Đã có tài khoản?
				<a href="/login" class="text-link">Đăng nhập</a>
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
