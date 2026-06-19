<script lang="ts">
	interface Props {
		viewMoveIndex: number;
		historyFensLength: number;
		isPlaying: boolean;
		isFinished: boolean;
		copiedFen: boolean;
		engineEnabled: boolean;
		showAnalysisToggle?: boolean;
		onNavigate: (index: number) => void;
		onCopyFen: () => void;
		onToggleEngine: () => void;
	}

	let {
		viewMoveIndex,
		historyFensLength,
		isPlaying,
		isFinished,
		copiedFen,
		engineEnabled,
		showAnalysisToggle = true,
		onNavigate,
		onCopyFen,
		onToggleEngine
	}: Props = $props();
</script>

<div class="board-nav">
	<button
		class="btn btn-nav"
		onclick={() => onNavigate(0)}
		disabled={viewMoveIndex <= 0}
		title="Bắt đầu ván"
	>
		<svg
			width="18"
			height="18"
			viewBox="0 0 24 24"
			fill="none"
			stroke="currentColor"
			stroke-width="2"
			stroke-linecap="round"
			stroke-linejoin="round"
		>
			<polygon points="11 19 2 12 11 5 11 19"></polygon>
			<polygon points="22 19 13 12 22 5 22 19"></polygon>
		</svg>
	</button>
	<button
		class="btn btn-nav"
		onclick={() => onNavigate(Math.max(0, viewMoveIndex - 1))}
		disabled={viewMoveIndex <= 0}
		title="Lùi 1 nước"
	>
		<svg
			width="18"
			height="18"
			viewBox="0 0 24 24"
			fill="none"
			stroke="currentColor"
			stroke-width="2"
			stroke-linecap="round"
			stroke-linejoin="round"
		>
			<polygon points="19 20 9 12 19 4 19 20"></polygon>
			<line x1="5" y1="19" x2="5" y2="5"></line>
		</svg>
	</button>
	<button
		class="btn btn-nav"
		onclick={() => onNavigate(Math.min(historyFensLength - 1, viewMoveIndex + 1))}
		disabled={viewMoveIndex >= historyFensLength - 1}
		title="Tiến 1 nước"
	>
		<svg
			width="18"
			height="18"
			viewBox="0 0 24 24"
			fill="none"
			stroke="currentColor"
			stroke-width="2"
			stroke-linecap="round"
			stroke-linejoin="round"
		>
			<polygon points="5 4 15 12 5 20 5 4"></polygon>
			<line x1="19" y1="5" x2="19" y2="19"></line>
		</svg>
	</button>
	<button
		class="btn btn-nav"
		onclick={() => onNavigate(historyFensLength - 1)}
		disabled={viewMoveIndex >= historyFensLength - 1}
		title="Nước mới nhất"
	>
		<svg
			width="18"
			height="18"
			viewBox="0 0 24 24"
			fill="none"
			stroke="currentColor"
			stroke-width="2"
			stroke-linecap="round"
			stroke-linejoin="round"
		>
			<polygon points="13 5 22 12 13 19 13 5"></polygon>
			<polygon points="2 5 11 12 2 19 2 5"></polygon>
		</svg>
	</button>

	{#if isFinished || !isPlaying}
		<div style="width: 1px; background: var(--border); margin: 0 4px;"></div>
		{#if showAnalysisToggle}
			<button
				class="btn btn-nav"
				class:btn-analyze-active={engineEnabled}
				onclick={onToggleEngine}
				title="Bật/tắt phân tích Engine"
			>
				<svg
					width="18"
					height="18"
					viewBox="0 0 24 24"
					fill="none"
					stroke="currentColor"
					stroke-width="2"
					stroke-linecap="round"
					stroke-linejoin="round"
				>
					<circle cx="12" cy="12" r="10"></circle>
					<polyline points="12 6 12 12 16 14"></polyline>
				</svg>
			</button>
		{/if}
		<button
			class="btn btn-nav"
			class:btn-copy-active={copiedFen}
			onclick={onCopyFen}
			title={copiedFen ? 'Đã chép FEN!' : 'Sao chép FEN nước đi này'}
		>
			{#if copiedFen}
				<svg
					width="18"
					height="18"
					viewBox="0 0 24 24"
					fill="none"
					stroke="currentColor"
					stroke-width="2.5"
					stroke-linecap="round"
					stroke-linejoin="round"
				>
					<polyline points="20 6 9 17 4 12"></polyline>
				</svg>
			{:else}
				<svg
					width="18"
					height="18"
					viewBox="0 0 24 24"
					fill="none"
					stroke="currentColor"
					stroke-width="2"
					stroke-linecap="round"
					stroke-linejoin="round"
				>
					<rect x="9" y="9" width="13" height="13" rx="2" ry="2"></rect>
					<path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"></path>
				</svg>
			{/if}
		</button>
	{/if}
</div>

<style>
	.board-nav {
		display: flex;
		justify-content: space-around;
		gap: 0.5rem;
		width: 100%;
		background: var(--bg-surface);
		border: 1px solid var(--border);
		border-radius: var(--radius-md);
		padding: 0.4rem;
		box-sizing: border-box;
	}

	.btn-nav {
		flex: 1;
		max-width: 60px;
		padding: 0.4rem;
		background: transparent;
		color: var(--text-secondary);
		border: 1px solid transparent;
		transition: all 0.15s var(--ease);
	}

	.btn-nav:hover:not(:disabled) {
		background: var(--bg-raised);
		color: var(--gold-light);
		border-color: var(--border-light);
	}

	.btn-nav:disabled {
		opacity: 0.25;
		cursor: not-allowed;
	}

	.btn-analyze-active {
		color: var(--accent-light) !important;
		background: var(--accent-dim) !important;
		border-color: var(--border-accent) !important;
	}

	.btn-copy-active {
		color: var(--success-light) !important;
		background: rgba(45, 122, 79, 0.15) !important;
		border-color: rgba(45, 122, 79, 0.3) !important;
	}
</style>
