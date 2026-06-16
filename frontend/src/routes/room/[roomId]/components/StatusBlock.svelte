<script lang="ts">
	interface Props {
		notice: string;
		isWaiting: boolean;
		isPlaying: boolean;
		isFinished: boolean;
		myTurn: boolean;
	}

	let { notice, isWaiting, isPlaying, isFinished, myTurn }: Props = $props();
</script>

<div class="status-block">
	{#if notice}
		<div class="notice-banner">
			<svg
				width="16"
				height="16"
				viewBox="0 0 24 24"
				fill="none"
				stroke="currentColor"
				stroke-width="2"
				stroke-linecap="round"
				stroke-linejoin="round"
				aria-hidden="true"
			>
				<polygon
					points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"
				/>
			</svg>
			{notice}
		</div>
	{:else if isWaiting}
		<div class="status-pill status-pill--waiting">Chờ bắt đầu</div>
	{:else if isPlaying}
		<div class="status-pill" class:status-pill--myturn={myTurn} class:status-pill--wait={!myTurn}>
			{myTurn ? 'Lượt của bạn' : 'Chờ đối thủ…'}
		</div>
	{:else if isFinished}
		<div class="status-pill status-pill--finished">Ván đã kết thúc</div>
	{/if}
</div>

<style>
	.status-block {
		display: flex;
		flex-direction: column;
		gap: 0.5rem;
	}

	.status-pill {
		display: inline-flex;
		align-items: center;
		justify-content: center;
		padding: 0.5rem 0.75rem;
		border-radius: var(--radius-md);
		font-size: 0.82rem;
		font-weight: 700;
		letter-spacing: 0.03em;
		border: 1px solid;
	}

	.status-pill--waiting {
		color: var(--gold);
		background: var(--gold-dim);
		border-color: rgba(201, 168, 76, 0.3);
	}

	.status-pill--myturn {
		color: #fff;
		background: var(--accent);
		border-color: var(--accent);
		box-shadow: 0 0 14px var(--accent-glow);
	}

	.status-pill--wait {
		color: var(--text-secondary);
		background: var(--bg-surface);
		border-color: var(--border);
	}

	.status-pill--finished {
		color: var(--text-muted);
		background: var(--bg-surface);
		border-color: var(--border);
	}

	.notice-banner {
		display: flex;
		align-items: center;
		gap: 0.5rem;
		padding: 0.65rem 0.85rem;
		background: rgba(201, 168, 76, 0.1);
		border: 1px solid rgba(201, 168, 76, 0.3);
		border-radius: var(--radius-md);
		color: var(--gold-light);
		font-weight: 700;
		font-size: 0.875rem;
	}
</style>
