<template>
  <span :class="['zh-price', 'zh-price--' + size]">
    <span v-if="showSymbol" class="zh-price__symbol">¥</span>
    <span class="zh-price__int zh-mono">{{ displayInt }}</span>
    <span v-if="decimal" class="zh-price__decimal zh-mono">.{{ decimal }}</span>
    <span v-if="original && original > price" class="zh-price__original">¥{{ formatOriginal }}</span>
  </span>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = withDefaults(defineProps<{
  price: number | string
  original?: number | string
  size?: 'sm' | 'md' | 'lg' | 'xl' | '2xl'
  showSymbol?: boolean
}>(), {
  size: 'md',
  showSymbol: true,
})

function formatNumber(n: number | string): string {
  const num = typeof n === 'string' ? parseFloat(n) : n
  if (isNaN(num)) return '0'
  return num.toLocaleString('en-US')
}

const priceNum = computed(() => typeof props.price === 'string' ? parseFloat(props.price) : props.price)
const displayInt = computed(() => formatNumber(Math.floor(priceNum.value)))
const decimal = computed(() => {
  const d = (priceNum.value % 1).toFixed(2).slice(2)
  return d === '00' ? '' : d
})
const formatOriginal = computed(() => {
  if (!props.original) return ''
  return formatNumber(props.original)
})
</script>

<style lang="scss" scoped>
.zh-price {
  display: inline-flex;
  align-items: baseline;
  gap: 2px;
  color: var(--zh-color-danger);
  font-weight: 700;

  &__symbol {
    font-size: 0.7em;
    margin-right: 1px;
  }
  &__int { letter-spacing: -0.02em; }
  &__decimal { font-size: 0.8em; }
  &__original {
    margin-left: 8px;
    font-size: 0.7em;
    color: var(--zh-color-text-tertiary);
    text-decoration: line-through;
    font-weight: 400;
  }

  &--sm { font-size: 14px; }
  &--md { font-size: 18px; }
  &--lg { font-size: 24px; }
  &--xl { font-size: 32px; }
  &--2xl { font-size: 40px; }
}
</style>
