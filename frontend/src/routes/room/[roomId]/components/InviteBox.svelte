<script lang="ts">
	interface Props {
		roomId: string;
		copiedId: boolean;
		copiedLink: boolean;
		onCopyId: () => void;
		onCopyLink: () => void;
	}

	let { roomId, copiedId, copiedLink, onCopyId, onCopyLink }: Props = $props();
</script>

<div class="invite-box">
	<h4 class="invite-box-title">Mời bạn chơi</h4>
	<p class="invite-box-desc">Gửi mã phòng hoặc liên kết này cho bạn bè để bắt đầu chơi:</p>

	<div class="invite-field">
		<span class="invite-field-label">Mã phòng</span>
		<div class="invite-copy-row">
			<code class="invite-code">{roomId}</code>
			<button class="btn-copy" onclick={onCopyId}>
				{copiedId ? 'Đã chép' : 'Sao chép'}
			</button>
		</div>
	</div>

	<div class="invite-field">
		<span class="invite-field-label">Liên kết phòng</span>
		<div class="invite-copy-row">
			<span class="invite-link-preview">
				{typeof window !== 'undefined' ? window.location.origin + '/room/' + roomId : ''}
			</span>
			<button class="btn-copy" onclick={onCopyLink}>
				{copiedLink ? 'Đã chép' : 'Sao chép'}
			</button>
		</div>
	</div>
</div>

<style>
	.invite-box {
		background: var(--bg-surface);
		border: 1px solid var(--border);
		border-radius: var(--radius-lg);
		padding: 1rem;
		display: flex;
		flex-direction: column;
		gap: 0.75rem;
		margin-top: 0.5rem;
	}

	.invite-box-title {
		font-size: 0.8rem;
		font-weight: 700;
		letter-spacing: 0.08em;
		text-transform: uppercase;
		color: var(--text-secondary);
		margin: 0;
	}

	.invite-box-desc {
		font-size: 0.75rem;
		color: var(--text-muted);
		line-height: 1.4;
		margin: 0;
	}

	.invite-field {
		display: flex;
		flex-direction: column;
		gap: 0.25rem;
	}

	.invite-field-label {
		font-size: 0.68rem;
		font-weight: 600;
		text-transform: uppercase;
		letter-spacing: 0.04em;
		color: var(--text-muted);
	}

	.invite-copy-row {
		display: flex;
		align-items: center;
		justify-content: space-between;
		background: var(--bg-base);
		border: 1px solid var(--border-light);
		border-radius: var(--radius-md);
		padding: 0.4rem 0.6rem;
		gap: 0.5rem;
		min-width: 0;
	}

	.invite-code {
		font-family: 'Courier New', monospace;
		font-weight: 700;
		color: var(--gold-light);
		font-size: 0.85rem;
	}

	.invite-link-preview {
		font-size: 0.75rem;
		color: var(--text-secondary);
		white-space: nowrap;
		overflow: hidden;
		text-overflow: ellipsis;
		flex: 1;
		min-width: 0;
	}

	.btn-copy {
		background: var(--bg-raised);
		border: 1px solid var(--border-light);
		color: var(--text-secondary);
		font-size: 0.7rem;
		font-weight: 600;
		padding: 0.25rem 0.5rem;
		border-radius: var(--radius-sm);
		cursor: pointer;
		transition: all 0.15s var(--ease);
		flex-shrink: 0;
	}

	.btn-copy:hover {
		background: var(--bg-overlay);
		color: var(--text-primary);
		border-color: var(--border-accent);
	}

	.btn-copy:active {
		transform: scale(0.95);
	}
</style>
