<template>
  <div class="zh-empty">
    <div class="zh-empty__svg" v-html="svg" />
    <div class="zh-empty__title">{{ title }}</div>
    <div v-if="description" class="zh-empty__desc">{{ description }}</div>
    <slot />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = withDefaults(defineProps<{
  title?: string
  description?: string
  type?: 'empty' | 'error' | 'search' | 'cart'
}>(), { title: '暂无数据', type: 'empty' })

const svg = computed(() => svgs[props.type])
const svgs: Record<string, string> = {
  empty: '<svg width="120" height="120" viewBox="0 0 120 120" fill="none"><defs><linearGradient id="g1" x1="0" y1="0" x2="1" y2="1"><stop stop-color="#7C3AED"/><stop offset="1" stop-color="#06B6D4"/></linearGradient></defs><circle cx="60" cy="60" r="50" fill="url(#g1)" opacity="0.1"/><circle cx="60" cy="60" r="35" fill="url(#g1)" opacity="0.2"/><circle cx="60" cy="60" r="20" fill="url(#g1)"/><text x="60" y="68" text-anchor="middle" font-size="20" fill="white">∅</text></svg>',
  error: '<svg width="120" height="120" viewBox="0 0 120 120"><circle cx="60" cy="60" r="50" fill="#FEE2E2"/><text x="60" y="70" text-anchor="middle" font-size="40">⚠</text></svg>',
  search: '<svg width="120" height="120" viewBox="0 0 120 120"><defs><linearGradient id="g2" x1="0" y1="0" x2="1" y2="1"><stop stop-color="#7C3AED"/><stop offset="1" stop-color="#06B6D4"/></linearGradient></defs><circle cx="55" cy="55" r="35" stroke="url(#g2)" stroke-width="6" fill="none"/><line x1="80" y1="80" x2="100" y2="100" stroke="url(#g2)" stroke-width="6" stroke-linecap="round"/></svg>',
  cart: '<svg width="120" height="120" viewBox="0 0 120 120"><defs><linearGradient id="g3" x1="0" y1="0" x2="1" y2="1"><stop stop-color="#7C3AED"/><stop offset="1" stop-color="#06B6D4"/></linearGradient></defs><path d="M30 30 h15 l8 50 h35 l8-40 h-50" stroke="url(#g3)" stroke-width="5" fill="none" stroke-linejoin="round" stroke-linecap="round"/><circle cx="55" cy="95" r="5" fill="#7C3AED"/><circle cx="85" cy="95" r="5" fill="#06B6D4"/></svg>',
}
</script>

<style lang="scss" scoped>
.zh-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 24px;
  text-align: center;

  &__svg { margin-bottom: 16px; }
  &__title {
    font-size: var(--zh-font-size-lg);
    color: var(--zh-color-text-secondary);
    font-weight: 500;
    margin-bottom: 4px;
  }
  &__desc {
    font-size: var(--zh-font-size-sm);
    color: var(--zh-color-text-tertiary);
    margin-bottom: 16px;
  }
}
</style>
