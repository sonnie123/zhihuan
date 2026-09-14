<template>
  <div :class="['zh-card', glass && 'zh-card--glass', hoverable && 'zh-card--hover', gradient && 'zh-card--gradient']">
    <slot />
  </div>
</template>

<script setup lang="ts">
defineProps<{
  glass?: boolean
  hoverable?: boolean
  gradient?: boolean
}>()
</script>

<style lang="scss" scoped>
.zh-card {
  background: var(--zh-color-bg-card);
  border-radius: var(--zh-radius-lg);
  box-shadow: var(--zh-shadow-sm);
  transition: all var(--zh-duration-base) var(--zh-easing-standard);
  overflow: hidden;

  &--glass {
    background: rgba(255, 255, 255, 0.7);
    backdrop-filter: blur(20px) saturate(180%);
    -webkit-backdrop-filter: blur(20px) saturate(180%);
    border: 1px solid rgba(255, 255, 255, 0.5);
  }

  &--gradient {
    background: var(--zh-gradient-aurora);
    background-size: 200% 200%;
    animation: zh-aurora 8s ease infinite;
    color: white;
  }

  &--hover:hover {
    transform: translateY(-4px);
    box-shadow: var(--zh-shadow-md);
  }
}
</style>
