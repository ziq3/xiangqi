<script lang="ts">
	import { goto } from '$app/navigation';
	
	let inputRoomId = '';

	async function createRoom() {
		const response = await fetch('/api/room/create?hostName=Guest', {
			method: 'POST'
		});
		const data = await response.json();
		goto(`/room/${data.roomId}`);
	}
	async function joinRoom(roomId: string) {
		if (!roomId.trim()) {
			alert('Vui lòng nhập ID phòng');
			return;
		}
		const response = await fetch(`/api/room/${roomId}/join?playerName=Guest2`, {
			method: 'POST'
		});
		
		if (!response.ok) {
			alert('Không thể vào phòng. Vui lòng kiểm tra lại ID.');
			return;
		}
		
		const data = await response.json();
		goto(`/room/${data.roomId}`);
	}
</script>

<button on:click={createRoom}> Chơi cờ tướng </button>

<div>
	<input type="text" bind:value={inputRoomId} placeholder="Nhập ID phòng" />
	<button on:click={() => joinRoom(inputRoomId)}> Vào phòng </button>
</div>
