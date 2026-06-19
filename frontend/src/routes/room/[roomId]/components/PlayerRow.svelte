<script lang="ts">
	interface Props {
		name: string;
		icon: string;
		timeMs: number;
		isActive: boolean;
		isYou: boolean;
		showReady?: boolean;
	}

	let { name, icon, timeMs, isActive, isYou, showReady = false }: Props = $props();

	function formatMs(ms: number): string {
		const safe = Number.isFinite(ms) ? Math.max(0, Math.floor(ms)) : 0;
		const totalSeconds = Math.floor(safe / 1000);
		const minutes = Math.floor(totalSeconds / 60);
		const seconds = totalSeconds % 60;
		return `${minutes}:${seconds.toString().padStart(2, '0')}`;
	}
</script>

<div class="player-row" class:player-row--active={isActive}>
	<div class="player-info">
		<span class="player-icon">{icon}</span>
		<span class="player-name">{name}</span>
		{#if isYou}<span class="player-you-badge">Bạn</span>{/if}
		{#if showReady}
			<span class="player-ready-badge">
				<svg width="9" height="9" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><polyline points="20 6 9 17 4 12"/></svg>
				Sẵn sàng
			</span>
		{/if}
	</div>
	<div class="player-clock" class:clock--urgent={timeMs < 30000}>
		{formatMs(timeMs)}
	</div>
</div>

<style>
	.player-row {
		display: flex;
		align-items: center;
		justify-content: space-between;
		padding: 0.85rem 1rem;
		transition: background 0.2s;
	}

	.player-row--active {
		background: var(--accent-dim);
		border-left: 3px solid var(--accent);
	}

	.player-info {
		display: flex;
		align-items: center;
		gap: 0.5rem;
		min-width: 0;
	}

	.player-icon {
		font-size: 1rem;
		flex-shrink: 0;
	}

	.player-name {
		font-size: 0.875rem;
		font-weight: 600;
		color: var(--text-primary);
		white-space: nowrap;
		overflow: hidden;
		text-overflow: ellipsis;
		max-width: 110px;
	}

	.player-you-badge {
		font-size: 0.65rem;
		font-weight: 700;
		letter-spacing: 0.06em;
		text-transform: uppercase;
		color: var(--accent-light);
		background: var(--accent-dim);
		border: 1px solid var(--border-accent);
		border-radius: 99px;
		padding: 0.1rem 0.4rem;
		flex-shrink: 0;
	}

	.player-ready-badge {
		display: inline-flex;
		align-items: center;
		gap: 0.25rem;
		font-size: 0.62rem;
		font-weight: 700;
		letter-spacing: 0.05em;
		text-transform: uppercase;
		color: var(--success-light, #4ade80);
		background: rgba(74, 222, 128, 0.08);
		border: 1px solid rgba(74, 222, 128, 0.3);
		border-radius: 99px;
		padding: 0.1rem 0.4rem;
		flex-shrink: 0;
	}

	.player-clock {
		font-size: 1.05rem;
		font-weight: 700;
		font-variant-numeric: tabular-nums;
		color: var(--text-secondary);
		flex-shrink: 0;
	}

	.clock--urgent {
		color: var(--error-light);
		animation: urgentPulse 1s ease-in-out infinite;
	}

	@keyframes urgentPulse {
		0%,
		100% {
			opacity: 1;
		}
		50% {
			opacity: 0.5;
		}
	}
</style>
