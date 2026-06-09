<script lang="ts">
	import { goto } from '$app/navigation';
	import { createRoom as createRoomApi, joinRoom as joinRoomApi } from '$lib/api/room';
	import { authStore, resolveDisplayName } from '$lib/stores/auth';

	let inputRoomId = '';
	let creating = false;
	let joining = false;
	let errorMessage = '';

	$: displayName = resolveDisplayName($authStore);

	async function createRoom() {
		errorMessage = '';
		creating = true;
		try {
			const room = await createRoomApi(displayName);
			goto(`/room/${room.roomId}`);
		} catch (error) {
			errorMessage = error instanceof Error ? error.message : 'Không thể tạo phòng';
		} finally {
			creating = false;
		}
	}

	async function joinRoom(roomId: string) {
		if (!roomId.trim()) {
			errorMessage = 'Vui lòng nhập ID phòng';
			return;
		}

		errorMessage = '';
		joining = true;
		try {
			const room = await joinRoomApi(roomId, displayName);
			goto(`/room/${room.roomId}`);
		} catch (error) {
			errorMessage =
				error instanceof Error ? error.message : 'Không thể vào phòng. Vui lòng kiểm tra lại ID.';
		} finally {
			joining = false;
		}
	}

	function handleGuestNameInput(event: Event) {
		const target = event.target as HTMLInputElement;
		authStore.setGuestName(target.value);
	}
</script>

<svelte:head>
	<title>Cờ Tướng Online — Chơi cờ tướng trực tuyến</title>
	<meta name="description" content="Chơi cờ tướng trực tuyến với bạn bè hoặc BOT. Tạo phòng ngay, miễn phí." />
</svelte:head>

<main class="lobby-page">
	<div class="lobby-hero">
		<div class="lobby-glyph" aria-hidden="true">將</div>
		<h1 class="lobby-title">Cờ Tướng Online</h1>
		<p class="lobby-sub">Thách đấu bạn bè hoặc chinh phục BOT. Chọn nước đi khôn ngoan nhất.</p>
	</div>

	{#if !$authStore.user}
		<div class="lobby-guest-banner">
			<span class="guest-label">Chơi với tên</span>
			<input
				type="text"
				class="form-input guest-name-input"
				value={$authStore.guestName}
				on:input={handleGuestNameInput}
				placeholder="Tên khách..."
				maxlength="30"
				aria-label="Tên người chơi khách"
			/>
			<span class="guest-or">hoặc</span>
			<a href="/login" class="btn btn-ghost btn-sm">Đăng nhập</a>
		</div>
	{/if}

	{#if errorMessage}
		<div class="alert alert-error lobby-error" role="alert">
			<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
				<circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/>
			</svg>
			{errorMessage}
		</div>
	{/if}

	<div class="lobby-actions">
		<!-- Create / Play vs BOT -->
		<div class="lobby-card">
			<p class="lobby-card-title">Chơi ngay</p>
			<button
				id="create-room-btn"
				class="btn btn-primary"
				on:click={createRoom}
				disabled={creating || joining}
			>
				{#if creating}
					<span class="spinner" aria-hidden="true"></span>
					Đang tạo phòng...
				{:else}
					<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
						<path d="M12 5v14M5 12h14"/>
					</svg>
					Tạo phòng &amp; chơi với BOT
				{/if}
			</button>
			<p class="lobby-card-hint">
				Bắt đầu ngay, gửi ID phòng để mời bạn bè thay thế BOT.
			</p>
		</div>

		<!-- Join room -->
		<div class="lobby-card">
			<p class="lobby-card-title">Vào phòng</p>
			<div class="lobby-join-row">
				<input
					id="room-id-input"
					type="text"
					class="form-input"
					bind:value={inputRoomId}
					placeholder="Nhập ID phòng..."
					autocomplete="off"
					on:keydown={(e) => e.key === 'Enter' && joinRoom(inputRoomId)}
				/>
				<button
					id="join-room-btn"
					class="btn btn-ghost"
					on:click={() => joinRoom(inputRoomId)}
					disabled={creating || joining}
				>
					{#if joining}
						<span class="spinner" aria-hidden="true"></span>
					{:else}
						Vào
					{/if}
				</button>
			</div>
		</div>
	</div>
</main>

<style>
	.lobby-guest-banner {
		display: flex;
		align-items: center;
		gap: 0.75rem;
		background: var(--bg-surface);
		border: 1px solid var(--border);
		border-radius: var(--radius-lg);
		padding: 0.6rem 1rem;
		margin-bottom: 0.875rem;
		flex-wrap: wrap;
		width: 100%;
		max-width: 420px;
	}

	.guest-label {
		font-size: 0.82rem;
		font-weight: 600;
		letter-spacing: 0.04em;
		text-transform: uppercase;
		color: var(--text-secondary);
		white-space: nowrap;
	}

	.guest-name-input {
		flex: 1;
		min-width: 120px;
		padding: 0.5rem 0.75rem;
		font-size: 0.9rem;
	}

	.guest-or {
		font-size: 0.8rem;
		color: var(--text-muted);
		white-space: nowrap;
	}

	.btn-sm {
		padding: 0.4rem 0.85rem;
		font-size: 0.82rem;
	}

	.lobby-error {
		width: 100%;
		max-width: 420px;
	}

	.lobby-card-hint {
		margin-top: 0.6rem;
		font-size: 0.8rem;
		color: var(--text-muted);
		line-height: 1.5;
	}
</style>
