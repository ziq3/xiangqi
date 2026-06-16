<script lang="ts">
	interface Props {
		historyMoves: string[];
		viewMoveIndex: number;
		onSelectMove: (index: number) => void;
	}

	let { historyMoves, viewMoveIndex, onSelectMove }: Props = $props();

	let movePairsCount = $derived(Math.ceil(historyMoves.length / 2));
</script>

<div class="moves-block">
	<div class="moves-header">
		<h3 class="moves-title">Lịch sử nước đi</h3>
	</div>
	<div class="moves-grid-scroll">
		<div class="moves-grid">
			{#each Array(movePairsCount) as _, index}
				{@const hostMove = historyMoves[index * 2]}
				{@const guestMove = historyMoves[index * 2 + 1]}
				<div class="move-pair-row">
					<span class="move-number">{index + 1}.</span>
					<button
						class="move-btn"
						class:move-btn--active={viewMoveIndex === index * 2 + 1}
						onclick={() => onSelectMove(index * 2 + 1)}
					>
						{hostMove}
					</button>
					{#if guestMove}
						<button
							class="move-btn"
							class:move-btn--active={viewMoveIndex === index * 2 + 2}
							onclick={() => onSelectMove(index * 2 + 2)}
						>
							{guestMove}
						</button>
					{:else}
						<span class="move-btn move-btn--empty">...</span>
					{/if}
				</div>
			{/each}
		</div>
	</div>
</div>

<style>
	.moves-block {
		background: var(--bg-surface);
		border: 1px solid var(--border);
		border-radius: var(--radius-lg);
		padding: 0.85rem;
		display: flex;
		flex-direction: column;
		gap: 0.6rem;
		max-height: 250px;
		min-height: 140px;
	}

	.moves-header {
		display: flex;
		align-items: center;
		justify-content: space-between;
		border-bottom: 1px solid var(--border);
		padding-bottom: 0.4rem;
		margin-bottom: 0.4rem;
	}

	.moves-title {
		font-size: 0.72rem;
		font-weight: 700;
		letter-spacing: 0.1em;
		text-transform: uppercase;
		color: var(--text-secondary);
		border-bottom: none;
		padding-bottom: 0;
		margin: 0;
	}

	.moves-grid-scroll {
		overflow-y: auto;
		flex: 1;
		padding-right: 2px;
	}

	.moves-grid {
		display: flex;
		flex-direction: column;
		gap: 0.25rem;
	}

	.move-pair-row {
		display: grid;
		grid-template-columns: 32px 1fr 1fr;
		align-items: center;
		gap: 0.4rem;
		font-size: 0.82rem;
	}

	.move-number {
		color: var(--text-muted);
		font-weight: 600;
		text-align: right;
		padding-right: 0.25rem;
	}

	.move-btn {
		background: transparent;
		border: 1px solid transparent;
		color: var(--text-secondary);
		border-radius: var(--radius-sm);
		padding: 0.25rem 0.4rem;
		text-align: left;
		cursor: pointer;
		font-family: 'Courier New', monospace;
		font-weight: 600;
		font-size: 0.85rem;
		transition: all 0.1s var(--ease);
	}

	.move-btn:hover {
		background: var(--bg-raised);
		color: var(--text-primary);
	}

	.move-btn--active {
		background: var(--accent-dim);
		color: var(--accent-light);
		border-color: var(--border-accent);
	}

	.move-btn--empty {
		color: var(--text-muted);
		cursor: default;
		padding-left: 0.4rem;
	}
</style>
