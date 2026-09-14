<template>
  <div :class="['zh-avatar', 'zh-avatar--' + size, verified && 'is-verified']" :style="{ width: dim, height: dim }">
    <img v-if="src" :src="src" :alt="alt" loading="lazy" @error="onError" />
    <span v-else class="zh-avatar__fallback" :style="{ background: gradientFor(name) }">
      {{ (name || '?').charAt(0).toUpperCase() }}
    </span>
    <span v-if="verified" class="zh-avatar__badge" title="已实名认证">✓</span>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = withDefaults(defineProps<{
  src?: string
  name?: string
  alt?: string
  size?: 'xs' | 'sm' | 'md' | 'lg' | 'xl' | '2xl'
  verified?: boolean
}>(), { size: 'md' })

const sizeMap = { xs: 24, sm: 32, md: 40, lg: 56, xl: 80, '2xl': 120 }
const dim = computed(() => sizeMap[props.size] + 'px')

function onError(e: Event) {
  (e.target as HTMLImageElement).style.display = 'none'
}

function gradientFor(seed?: string) {
  const list = [
    'linear-gradient(135deg, #7C3AED, #06B6D4)',
    'linear-gradient(135deg, #F59E0B, #F43F5E)',
    'linear-gradient(135deg, #10B981, #06B6D4)',
    'linear-gradient(135deg, #8B5CF6, #EC4899)',
    'linear-gradient(135deg, #3B82F6, #06B6D4)',
  ]
  if (!seed) return list[0]
  const idx = seed.charCodeAt(0) % list.length
  return list[idx]
}
</script>

<style lang="scss" scoped>
.zh-avatar {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  overflow: visible;
  flex-shrink: 0;
  background: var(--zh-gradient-primary);
  color: white;
  font-weight: 600;

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
    border-radius: 50%;
  }

  &__fallback {
    width: 100%;
    height: 100%;
    display: flex;
    align-items: center;
    justify-content: center;
    color: white;
    font-size: 1.2em;
    font-weight: 700;
    border-radius: 50%;
  }

  &__badge {
    position: absolute;
    bottom: 0;
    right: 0;
    width: 30%;
    height: 30%;
    min-width: 14px;
    min-height: 14px;
    background: var(--zh-color-success);
    color: white;
    border-radius: 50%;
    border: 2px solid white;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 10px;
    font-weight: 700;
  }
}
</style>
