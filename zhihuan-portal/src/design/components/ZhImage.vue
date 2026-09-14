<template>
  <div :class="['zh-image', rounded && 'zh-image--rounded']" :style="containerStyle">
    <div v-if="!loaded" class="zh-image__skeleton zh-skeleton"></div>
    <img
      v-if="lazy ? visible : true"
      :src="src"
      :alt="alt"
      :loading="lazy ? 'lazy' : 'eager'"
      @load="onLoad"
      @error="onError"
      :class="['zh-image__img', loaded && 'is-loaded']"
    />
    <div v-if="errored" class="zh-image__error">
      <span>😢</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useIntersectionObserver } from '@/composables/useIntersectionObserver'

const props = withDefaults(defineProps<{
  src: string
  alt?: string
  width?: string | number
  height?: string | number
  rounded?: boolean
  lazy?: boolean
  fit?: 'cover' | 'contain'
}>(), { lazy: true, fit: 'cover' })

const target = ref<HTMLElement | null>(null)
const visible = ref(false)
const loaded = ref(false)
const errored = ref(false)

useIntersectionObserver(target as any, ([entry]) => {
  if (entry?.isIntersecting) visible.value = true
})

function onLoad() { loaded.value = true; errored.value = false }
function onError() { errored.value = true }

const containerStyle = computed(() => ({
  width: typeof props.width === 'number' ? props.width + 'px' : props.width || '100%',
  height: typeof props.height === 'number' ? props.height + 'px' : props.height || 'auto',
  aspectRatio: props.height ? 'auto' : '1 / 1',
}))
</script>

<style lang="scss" scoped>
.zh-image {
  position: relative;
  display: block;
  overflow: hidden;
  background: var(--zh-color-border-light);

  &--rounded { border-radius: var(--zh-radius-md); }

  &__skeleton {
    position: absolute;
    inset: 0;
  }
  &__img {
    width: 100%;
    height: 100%;
    object-fit: v-bind(fit);
    opacity: 0;
    transition: opacity var(--zh-duration-slow) var(--zh-easing-standard);
    &.is-loaded { opacity: 1; }
  }
  &__error {
    position: absolute;
    inset: 0;
    display: flex;
    align-items: center;
    justify-content: center;
    background: var(--zh-color-border-light);
    font-size: 32px;
  }
}
</style>
