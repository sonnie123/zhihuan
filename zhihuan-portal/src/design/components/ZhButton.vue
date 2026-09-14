<template>
  <button
    :class="['zh-btn', type && 'zh-btn--' + type, size && 'zh-btn--' + size, block && 'zh-btn--block', loading && 'is-loading']"
    :disabled="disabled || loading"
    @click="onClick"
  >
    <span v-if="loading" class="zh-btn__spinner"></span>
    <slot v-else />
  </button>
</template>

<script setup lang="ts">
const props = defineProps<{
  type?: 'primary' | 'outline' | 'ghost' | 'text' | 'danger' | 'accent'
  size?: 'sm' | 'md' | 'lg'
  block?: boolean
  loading?: boolean
  disabled?: boolean
}>()

const emit = defineEmits<{ (e: 'click', ev: MouseEvent): void }>()

function onClick(ev: MouseEvent) {
  if (!props.disabled && !props.loading) emit('click', ev)
}
</script>

<style lang="scss" scoped>
.zh-btn {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 8px 18px;
  font-size: var(--zh-font-size-base);
  font-weight: 500;
  border-radius: var(--zh-radius-base);
  cursor: pointer;
  transition: all var(--zh-duration-base) var(--zh-easing-standard);
  border: 1px solid transparent;
  white-space: nowrap;
  user-select: none;
  outline: none;

  &--sm { padding: 4px 12px; font-size: var(--zh-font-size-sm); border-radius: var(--zh-radius-sm); }
  &--lg { padding: 12px 24px; font-size: var(--zh-font-size-lg); border-radius: var(--zh-radius-md); }
  &--block { width: 100%; }

  &:active:not(:disabled) { transform: scale(0.97); }
  &:disabled { opacity: 0.5; cursor: not-allowed; }

  &--primary {
    background: var(--zh-gradient-primary);
    color: white;
    box-shadow: var(--zh-shadow-primary);
    &:hover:not(:disabled) {
      background: var(--zh-gradient-primary-hover);
      box-shadow: var(--zh-shadow-primary-lg);
      transform: translateY(-1px);
    }
  }

  &--outline {
    background: white;
    color: var(--zh-color-primary);
    border: 1.5px solid var(--zh-color-primary);
    &:hover:not(:disabled) {
      background: var(--zh-gradient-primary-soft);
      transform: translateY(-1px);
    }
  }

  &--ghost {
    background: rgba(255, 255, 255, 0.6);
    backdrop-filter: blur(12px);
    color: var(--zh-color-text);
    border: 1px solid rgba(255, 255, 255, 0.5);
    &:hover:not(:disabled) {
      background: rgba(255, 255, 255, 0.85);
    }
  }

  &--text {
    background: transparent;
    color: var(--zh-color-primary);
    padding: 4px 8px;
    &:hover:not(:disabled) { background: var(--zh-gradient-primary-soft); }
  }

  &--accent {
    background: var(--zh-gradient-accent);
    color: white;
    box-shadow: 0 8px 24px rgba(245, 158, 11, 0.25);
    &:hover:not(:disabled) { transform: translateY(-1px); }
  }

  &--danger {
    background: var(--zh-color-danger);
    color: white;
    &:hover:not(:disabled) { background: #DC2626; }
  }

  &.is-loading { pointer-events: none; }
}

.zh-btn__spinner {
  width: 14px;
  height: 14px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top-color: white;
  border-radius: 50%;
  animation: zh-spin 0.8s linear infinite;
}
</style>
