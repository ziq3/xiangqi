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
			errorMessage = error instanceof Error ? error.message : 'Khong the tao phong';
		} finally {
			creating = false;
		}
	}

	async function joinRoom(roomId: string) {
		if (!roomId.trim()) {
			errorMessage = 'Vui long nhap ID phong';
			return;
		}

		errorMessage = '';
		joining = true;
		try {
			const room = await joinRoomApi(roomId, displayName);
			goto(`/room/${room.roomId}`);
		} catch (error) {
			errorMessage =
				error instanceof Error ? error.message : 'Khong the vao phong. Vui long kiem tra lai ID.';
		} finally {
			joining = false;
		}
	}

	function handleGuestNameInput(event: Event) {
		const target = event.target as HTMLInputElement;
		authStore.setGuestName(target.value);
	}
</script>

<h1>Co Tuong Online</h1>

{#if !$authStore.user}
	<label>
		Ten khach
		<input type="text" value={$authStore.guestName} on:input={handleGuestNameInput} />
	</label>
{/if}

{#if errorMessage}
	<p class="error">{errorMessage}</p>
{/if}

<button on:click={createRoom} disabled={creating || joining}>
	{creating ? 'Dang tao phong...' : 'Choi voi BOT'}
</button>

<p class="hint">Tao phong se bat dau che do dau voi BOT. Gui ID phong de nguoi choi khac vao thay BOT.</p>

<div>
	<input type="text" bind:value={inputRoomId} placeholder="Nhập ID phòng" />
	<button on:click={() => joinRoom(inputRoomId)} disabled={creating || joining}>
		{joining ? 'Dang vao phong...' : 'Vao phong'}
	</button>
</div>

<style>
	h1 {
		margin: 0.5rem 1rem;
	}

	label,
	div,
	button {
		margin: 0.5rem 1rem;
	}

	input {
		margin-left: 0.5rem;
		padding: 0.35rem 0.5rem;
	}

	button {
		padding: 0.45rem 0.8rem;
	}

	.error {
		color: #b91c1c;
		margin: 0.5rem 1rem;
	}

	.hint {
		color: #475569;
		font-size: 0.92rem;
		margin: 0.2rem 1rem 0.8rem;
	}
</style>
