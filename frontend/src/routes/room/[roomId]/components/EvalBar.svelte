<script lang="ts">
	interface Props {
		analysis: any;
		loading: boolean;
	}

	let { analysis, loading }: Props = $props();

	function getEvalPercentage(analysis: any): string {
		if (!analysis) return '50%';
		if (analysis.mate !== null && analysis.mate !== undefined) {
			return analysis.mate > 0 ? '100%' : '0%';
		}
		if (analysis.scoreCp === undefined || analysis.scoreCp === null) return '50%';
		const clamped = Math.max(-1000, Math.min(1000, analysis.scoreCp));
		const percent = 50 + (clamped / 20);
		return `${percent}%`;
	}

	function formatScore(analysis: any): string {
		if (!analysis) return '';
		if (analysis.mate !== null && analysis.mate !== undefined) {
			return `M${Math.abs(analysis.mate)}`;
		}
		if (analysis.scoreCp !== null && analysis.scoreCp !== undefined) {
			const val = analysis.scoreCp / 100;
			return val > 0 ? `+${val.toFixed(2)}` : val.toFixed(2);
		}
		return '';
	}

	let evalPercentageStr = $derived(getEvalPercentage(analysis));
	let isBlackFavored = $derived((parseFloat(evalPercentageStr) || 50) < 50);
	let scoreText = $derived(formatScore(analysis));
</script>

<div class="eval-bar">
	<div class="eval-fill eval-fill-black" style="height: {100 - parseFloat(evalPercentageStr)}%;"></div>
	<div class="eval-fill eval-fill-red" style="height: {evalPercentageStr};"></div>
	<div class="eval-score" class:eval-score-black={isBlackFavored}>
		{#if loading}
			<span class="spinner-small"></span>
		{:else}
			{scoreText}
		{/if}
	</div>
</div>

<style>
	.eval-bar {
		width: 24px;
		background: var(--bg-surface);
		border: 1px solid var(--border);
		border-radius: var(--radius-sm);
		display: flex;
		flex-direction: column;
		position: relative;
		overflow: hidden;
		flex-shrink: 0;
	}

	.eval-fill {
		width: 100%;
		transition: height 0.3s ease-out;
	}

	.eval-fill-black {
		background-color: #2c2c2c;
	}

	.eval-fill-red {
		background-color: #d32f2f;
	}

	.eval-score {
		position: absolute;
		top: 50%;
		left: 50%;
		transform: translate(-50%, -50%);
		font-size: 0.65rem;
		font-weight: 700;
		color: #fff;
		z-index: 2;
		background: rgba(0, 0, 0, 0.4);
		padding: 2px 4px;
		border-radius: 4px;
		pointer-events: none;
	}

	.eval-score-black {
		color: #fff;
	}

	.spinner-small {
		display: inline-block;
		width: 10px;
		height: 10px;
		border: 2px solid rgba(255, 255, 255, 0.3);
		border-radius: 50%;
		border-top-color: #fff;
		animation: spin 1s ease-in-out infinite;
	}

	@keyframes spin {
		to {
			transform: rotate(360deg);
		}
	}
</style>
