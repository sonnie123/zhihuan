<template>
  <span :class="['zh-tag', type && 'zh-tag--' + type, size && 'zh-tag--' + size, gradient && 'zh-tag--gradient']">
    <span v-if="$slots.icon" class="zh-tag__icon"><slot name="icon" /></span>
    <slot />
  </span>
</template>

<script setup lang="ts">
withDefaults(defineProps<{
  type?: 'primary' | 'success' | 'warning' | 'danger' | 'info' | 'neutral'
  size?: 'sm' | 'md'
  gradient?: boolean
}>(), { type: 'neutral', size: 'sm' })
</script>

<style lang="scss" scoped>
.zh-tag {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 2px 8px;
  border-radius: var(--zh-radius-sm);
  font-size: var(--zh-font-size-xs);
  font-weight: 500;
  line-height: 1.5;
  white-space: nowrap;

  &--sm { padding: 1px 6px; font-size: 11px; }
  &--md { padding: 4px 10px; font-size: var(--zh-font-size-xs); }

  &--primary { background: rgba(124, 58, 237, 0.1); color: var(--zh-color-primary); }
  &--success { background: var(--zh-color-success-light); color: var(--zh-color-success); }
  &--warning { background: var(--zh-color-warning-light); color: var(--zh-color-warning); }
  &--danger { background: var(--zh-color-danger-light); color: var(--zh-color-danger); }
  &--info { background: var(--zh-color-info-light); color: var(--zh-color-info); }
  &--neutral { background: var(--zh-color-border-light); color: var(--zh-color-text-secondary); }

  &--gradient {
    background: var(--zh-gradient-primary);
    color: white;
    font-weight: 600;
  }
}
</style>
