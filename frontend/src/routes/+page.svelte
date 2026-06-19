<script lang="ts">
	import { onMount } from 'svelte';
	import { goto } from '$app/navigation';
	import { createRoom as createRoomApi, joinRoom as joinRoomApi, listMatches } from '$lib/api/room';
	import { authStore, resolveDisplayName } from '$lib/stores/auth';
	import type { RoomState } from '$lib/types/game';

	let inputRoomId = '';
	let creatingBotRoom = false;
	let creatingFriendRoom = false;
	let joining = false;
	let errorMessage = '';
	let pastMatches: RoomState[] = [];
	let loadingMatches = false;

	$: displayName = resolveDisplayName($authStore);

	$: if ($authStore.user) {
		void loadPastMatches();
	} else {
		pastMatches = [];
	}

	async function loadPastMatches() {
		loadingMatches = true;
		try {
			pastMatches = await listMatches();
		} catch (err) {
			console.error('Không thể tải danh sách ván đấu:', err);
		} finally {
			loadingMatches = false;
		}
	}

	async function playWithBot() {
		errorMessage = '';
		creatingBotRoom = true;
		try {
			const room = await createRoomApi(displayName, true);
			goto(`/room/${room.roomId}`);
		} catch (error) {
			errorMessage = error instanceof Error ? error.message : 'Không thể tạo phòng chơi với BOT';
		} finally {
			creatingBotRoom = false;
		}
	}


	async function createRoomWithFriend() {
		errorMessage = '';
		creatingFriendRoom = true;
		try {
			const room = await createRoomApi(displayName);
			goto(`/room/${room.roomId}`);
		} catch (error) {
			errorMessage = error instanceof Error ? error.message : 'Không thể tạo phòng';
		} finally {
			creatingFriendRoom = false;
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

	<div class="lobby-grid" class:lobby-grid--wide={$authStore.user}>
		<div class="lobby-cards-container">
			<!-- Option 1: Play with BOT -->
			<div class="lobby-card">
				<div class="lobby-card-header">
					<div class="lobby-card-icon lobby-card-icon--bot">
						<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
							<rect x="3" y="11" width="18" height="10" rx="2" />
							<circle cx="12" cy="5" r="2" />
							<path d="M12 7v4M8 15h.01M16 15h.01" />
						</svg>
					</div>
					<div>
						<h2 class="lobby-card-name">Chơi với BOT</h2>
						<p class="lobby-card-desc">Thi đấu cờ tướng với máy AI ngay lập tức.</p>
					</div>
				</div>
				<button
					id="play-bot-btn"
					class="btn btn-primary"
					on:click={playWithBot}
					disabled={creatingBotRoom || creatingFriendRoom || joining}
				>
					{#if creatingBotRoom}
						<span class="spinner" aria-hidden="true"></span>
						Đang tạo trận đấu...
					{:else}
						Bắt đầu ngay
					{/if}
				</button>
			</div>

			<!-- Option 2: Play with Friend -->
			<div class="lobby-card">
				<div class="lobby-card-header">
					<div class="lobby-card-icon lobby-card-icon--friend">
						<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
							<path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2" />
							<circle cx="9" cy="7" r="4" />
							<path d="M23 21v-2a4 4 0 0 0-3-3.87" />
							<path d="M16 3.13a4 4 0 0 1 0 7.75" />
						</svg>
					</div>
					<div>
						<h2 class="lobby-card-name">Tạo phòng chơi với bạn</h2>
						<p class="lobby-card-desc">Lấy mã phòng để mời bạn bè vào tranh tài.</p>
					</div>
				</div>
				<button
					id="play-friend-btn"
					class="btn btn-ghost"
					on:click={createRoomWithFriend}
					disabled={creatingBotRoom || creatingFriendRoom || joining}
				>
					{#if creatingFriendRoom}
						<span class="spinner" aria-hidden="true"></span>
						Đang tạo phòng...
					{:else}
						Tạo phòng chờ
					{/if}
				</button>
			</div>

			<!-- Option 3: Join Room -->
			<div class="lobby-card">
				<div class="lobby-card-header">
					<div class="lobby-card-icon lobby-card-icon--join">
						<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
							<path d="M15 3h4a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2h-4M10 17l5-5-5-5M15 12H3"/>
						</svg>
					</div>
					<div>
						<h2 class="lobby-card-name">Vào phòng</h2>
						<p class="lobby-card-desc">Nhập mã phòng từ bạn bè gửi để tham chiến.</p>
					</div>
				</div>
				<div class="lobby-join-row">
					<input
						id="room-id-input"
						type="text"
						class="form-input"
						bind:value={inputRoomId}
						placeholder="Mã phòng..."
						autocomplete="off"
						on:keydown={(e) => e.key === 'Enter' && joinRoom(inputRoomId)}
					/>
					<button
						id="join-room-btn"
						class="btn btn-ghost"
						on:click={() => joinRoom(inputRoomId)}
						disabled={creatingBotRoom || creatingFriendRoom || joining}
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

		<!-- Past matches list -->
		{#if $authStore.user}
			<div class="lobby-past-matches">
				<h2 class="lobby-section-title">Ván đấu đã chơi</h2>
				{#if loadingMatches}
					<div class="lobby-matches-loading">
						<span class="spinner" aria-hidden="true"></span> Đang tải lịch sử...
					</div>
				{:else if pastMatches.length === 0}
					<p class="lobby-no-matches">Bạn chưa chơi ván đấu nào.</p>
				{:else}
					<div class="lobby-matches-list">
						{#each pastMatches as match}
							<div class="lobby-match-row">
								<div class="match-info">
									<span class="match-room">Phòng: <code>{match.roomId}</code></span>
									<span class="match-vs">
										<strong class="player-red">{match.hostName}</strong>
										<span class="vs-text">vs</span>
										<strong class="player-black">{match.guestName}</strong>
									</span>
								</div>
								<div class="match-result">
									{#if match.endReason}
										<span class="result-badge">
											{#if match.endReason === 'TIMEOUT_HOST' || match.endReason === 'CHECKMATE_HOST'}
												{match.guestName} thắng
											{:else if match.endReason === 'TIMEOUT_GUEST' || match.endReason === 'CHECKMATE_GUEST'}
												{match.hostName} thắng
											{:else}
												Đã kết thúc
											{/if}
										</span>
									{:else}
										<span class="result-badge">Đã kết thúc</span>
									{/if}
								</div>
								<a href="/room/{match.roomId}" class="btn btn-ghost btn-sm btn-replay">
									Xem lại
								</a>
							</div>
						{/each}
					</div>
				{/if}
			</div>
		{/if}
	</div>
</main>

<style>
	:global(.lobby-page) {
		height: auto;
		min-height: calc(100dvh - 60px);
		overflow-y: auto;
		display: flex;
		flex-direction: column;
		align-items: center;
		justify-content: center;
		padding: 1.5rem;
		box-sizing: border-box;
	}

	@media (min-width: 900px) {
		:global(.lobby-page) {
			height: calc(100dvh - 60px);
			overflow: hidden;
		}
	}

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

	.lobby-card-header {
		display: flex;
		align-items: center;
		gap: 0.875rem;
		margin-bottom: 0.875rem;
	}

	.lobby-card-icon {
		display: flex;
		align-items: center;
		justify-content: center;
		width: 36px;
		height: 36px;
		border-radius: var(--radius-md);
		background: var(--bg-raised);
		border: 1px solid var(--border-light);
		flex-shrink: 0;
		transition: all 0.2s var(--ease);
	}

	.lobby-card:hover .lobby-card-icon {
		background: var(--bg-overlay);
		border-color: var(--border-accent);
	}

	.lobby-card-icon--bot {
		color: var(--gold-light);
	}
	.lobby-card-icon--friend {
		color: var(--accent-light);
	}
	.lobby-card-icon--join {
		color: var(--text-primary);
	}

	.lobby-card-name {
		font-size: 0.92rem;
		font-weight: 700;
		color: var(--text-primary);
		margin: 0;
		line-height: 1.2;
	}

	.lobby-card-desc {
		font-size: 0.78rem;
		color: var(--text-secondary);
		margin-top: 0.15rem;
		line-height: 1.4;
	}

	.lobby-grid {
		display: flex;
		flex-direction: column;
		gap: 1rem;
		width: 100%;
		max-width: 420px;
		margin: 0 auto;
		box-sizing: border-box;
	}

	.lobby-cards-container {
		display: flex;
		flex-direction: column;
		gap: 0.75rem;
		width: 100%;
	}

	.lobby-past-matches {
		width: 100%;
		max-width: 420px;
		background: var(--bg-surface);
		border: 1px solid var(--border);
		border-radius: var(--radius-lg);
		padding: 1.25rem;
		display: flex;
		flex-direction: column;
		gap: 0.75rem;
		box-sizing: border-box;
		margin-top: 1rem;
	}

	@media (min-width: 900px) {
		.lobby-grid {
			max-width: 960px;
			flex-direction: row;
			justify-content: center;
			align-items: stretch;
		}

		.lobby-cards-container {
			display: grid;
			grid-template-columns: repeat(3, 1fr);
			gap: 1rem;
		}

		.lobby-grid--wide {
			display: grid;
			grid-template-columns: 1.2fr 1fr;
			gap: 1.5rem;
			align-items: start;
		}

		.lobby-grid--wide .lobby-cards-container {
			display: flex;
			flex-direction: column;
			gap: 0.75rem;
		}

		.lobby-grid--wide .lobby-past-matches {
			margin-top: 0;
			height: 382px;
		}

		.lobby-grid--wide .lobby-matches-list {
			max-height: 270px;
		}
	}

	.lobby-section-title {
		font-size: 0.82rem;
		font-weight: 700;
		letter-spacing: 0.08em;
		text-transform: uppercase;
		color: var(--text-secondary);
		border-bottom: 1px solid var(--border);
		padding-bottom: 0.5rem;
		margin: 0;
	}

	.lobby-matches-loading,
	.lobby-no-matches {
		font-size: 0.875rem;
		color: var(--text-secondary);
		padding: 1rem 0;
		text-align: center;
	}

	.lobby-matches-list {
		display: flex;
		flex-direction: column;
		gap: 0.6rem;
		max-height: 220px;
		overflow-y: auto;
		padding-right: 4px;
	}

	.lobby-match-row {
		display: flex;
		align-items: center;
		justify-content: space-between;
		gap: 0.75rem;
		padding: 0.6rem;
		background: var(--bg-raised);
		border: 1px solid var(--border-light);
		border-radius: var(--radius-md);
		transition: all 0.15s var(--ease);
	}

	.lobby-match-row:hover {
		border-color: var(--border-accent);
		background: var(--bg-overlay);
	}

	.match-info {
		display: flex;
		flex-direction: column;
		gap: 0.15rem;
		min-width: 0;
		flex: 1;
	}

	.match-room {
		font-size: 0.72rem;
		color: var(--text-muted);
		text-transform: uppercase;
		font-weight: 600;
	}

	.match-room code {
		color: var(--gold);
		font-family: 'Courier New', monospace;
	}

	.match-vs {
		font-size: 0.85rem;
		color: var(--text-primary);
		white-space: nowrap;
		overflow: hidden;
		text-overflow: ellipsis;
	}

	.player-red {
		color: var(--accent-light);
	}

	.player-black {
		color: var(--text-primary);
	}

	.vs-text {
		color: var(--text-secondary);
		font-size: 0.75rem;
		margin: 0 0.15rem;
	}

	.match-result {
		font-size: 0.75rem;
		color: var(--text-secondary);
	}

	.result-badge {
		font-weight: 500;
		color: var(--text-secondary);
	}

	.btn-replay {
		padding: 0.35rem 0.6rem;
		font-size: 0.8rem;
		border-radius: var(--radius-sm);
		flex-shrink: 0;
	}
</style>
