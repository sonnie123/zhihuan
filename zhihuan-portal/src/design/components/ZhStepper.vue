<template>
  <div class="zh-stepper">
    <div class="zh-stepper__progress" :style="{ width: progress + '%' }"></div>
    <div v-for="(step, i) in steps" :key="i" :class="['zh-stepper__step', getStatus(i)]">
      <div class="zh-stepper__node">
        <span v-if="getStatus(i) === 'success'" class="zh-stepper__check">✓</span>
        <span v-else-if="getStatus(i) === 'running'" class="zh-stepper__ring"></span>
        <span v-else>{{ i + 1 }}</span>
      </div>
      <div class="zh-stepper__label">{{ step }}</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  steps: string[]
  current: number
  done?: boolean
}>()

const progress = computed(() => {
  if (props.done) return 100
  if (props.current >= props.steps.length) return 100
  return (props.current / (props.steps.length - 1)) * 100
})

function getStatus(i: number) {
  if (props.done) return 'success'
  if (i < props.current) return 'success'
  if (i === props.current) return 'running'
  return 'pending'
}
</script>

<style lang="scss" scoped>
.zh-stepper {
  position: relative;
  display: flex;
  justify-content: space-between;
  padding: 24px 0;

  &__progress {
    position: absolute;
    top: 38px;
    left: 24px;
    right: 24px;
    height: 3px;
    background: var(--zh-gradient-primary);
    border-radius: var(--zh-radius-full);
    transition: width var(--zh-duration-slow) var(--zh-easing-decelerate);
    z-index: 0;
  }

  &__step {
    position: relative;
    z-index: 1;
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 8px;
  }

  &__node {
    width: 36px;
    height: 36px;
    border-radius: 50%;
    background: white;
    border: 2px solid var(--zh-color-border);
    display: flex;
    align-items: center;
    justify-content: center;
    font-weight: 600;
    color: var(--zh-color-text-tertiary);
    transition: all var(--zh-duration-base) var(--zh-easing-standard);
  }

  &__label {
    font-size: var(--zh-font-size-sm);
    color: var(--zh-color-text-tertiary);
    font-weight: 500;
    transition: color var(--zh-duration-base) var(--zh-easing-standard);
  }

  &__step.success &__node {
    background: var(--zh-gradient-primary);
    border-color: transparent;
    color: white;
  }
  &__step.success &__label { color: var(--zh-color-text); }

  &__step.running &__node {
    background: white;
    border-color: var(--zh-color-primary);
    color: var(--zh-color-primary);
    box-shadow: 0 0 0 4px rgba(124, 58, 237, 0.15);
    animation: zh-breathe 1.6s ease-in-out infinite;
  }
  &__step.running &__label { color: var(--zh-color-primary); font-weight: 600; }

  &__check { font-size: 16px; }
  &__ring {
    width: 14px;
    height: 14px;
    border: 2px solid var(--zh-color-primary);
    border-top-color: transparent;
    border-radius: 50%;
    animation: zh-spin 1s linear infinite;
  }
}
</style>
