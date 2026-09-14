<template>
  <div class="zh-score" @click="onClick">
    <svg :width="size" :height="size" viewBox="0 0 100 100">
      <defs>
        <linearGradient :id="gradId" x1="0" y1="0" x2="1" y2="1">
          <stop offset="0" :stop-color="gradient[0]" />
          <stop offset="1" :stop-color="gradient[1]" />
        </linearGradient>
      </defs>
      <circle cx="50" cy="50" r="42" stroke="var(--zh-color-border-light)" stroke-width="6" fill="none" />
      <circle
        cx="50" cy="50" r="42"
        :stroke="'url(#' + gradId + ')'"
        stroke-width="6"
        fill="none"
        :stroke-dasharray="circumference"
        :stroke-dashoffset="dashOffset"
        stroke-linecap="round"
        transform="rotate(-90 50 50)"
        class="zh-score__progress"
      />
    </svg>
    <div class="zh-score__center">
      <div class="zh-score__value zh-mono">{{ animatedScore }}</div>
      <div v-if="label" class="zh-score__label">{{ label }}</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, computed, onMounted } from 'vue'

const props = withDefaults(defineProps<{
  score: number  // 0-100
  size?: number
  label?: string
  gradient?: [string, string]
}>(), {
  size: 96,
  gradient: () => ['#7C3AED', '#06B6D4'] as [string, string],
})

const emit = defineEmits<{ (e: 'click'): void }>()

const gradId = 'zh-score-grad-' + Math.random().toString(36).slice(2, 8)
const radius = 42
const circumference = 2 * Math.PI * radius

const animatedScore = ref(0)
const targetScore = computed(() => Math.max(0, Math.min(100, props.score)))
const dashOffset = computed(() => circumference * (1 - animatedScore.value / 100))

function animate() {
  const start = animatedScore.value
  const end = targetScore.value
  const duration = 1200
  const startTime = Date.now()
  function tick() {
    const elapsed = Date.now() - startTime
    const t = Math.min(elapsed / duration, 1)
    const ease = 1 - Math.pow(1 - t, 3)  // ease-out
    animatedScore.value = Math.round(start + (end - start) * ease)
    if (t < 1) requestAnimationFrame(tick)
  }
  tick()
}

onMounted(() => setTimeout(animate, 100))
watch(targetScore, animate)

function onClick() { emit('click') }
</script>

<style lang="scss" scoped>
.zh-score {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: transform var(--zh-duration-base) var(--zh-easing-spring);

  &:hover { transform: scale(1.04); }
  &__progress {
    transition: stroke-dashoffset var(--zh-duration-slower) var(--zh-easing-decelerate);
  }
  &__center {
    position: absolute;
    inset: 0;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
  }
  &__value {
    font-size: 28px;
    font-weight: 800;
    background: var(--zh-gradient-primary);
    -webkit-background-clip: text;
    background-clip: text;
    -webkit-text-fill-color: transparent;
    line-height: 1;
  }
  &__label {
    font-size: var(--zh-font-size-xs);
    color: var(--zh-color-text-tertiary);
    margin-top: 4px;
  }
}
</style>
