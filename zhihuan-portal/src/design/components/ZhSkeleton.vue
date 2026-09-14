<template>
  <div :class="['zh-skeleton-wrap', { 'is-row': row }]">
    <template v-if="row">
      <div v-for="i in count" :key="i" class="zh-skeleton-item zh-skeleton" :style="{ width: typeof width === 'number' ? width + 'px' : width, height: typeof height === 'number' ? height + 'px' : height }"></div>
    </template>
    <div v-else class="zh-skeleton-item zh-skeleton" :style="{ width: typeof width === 'number' ? width + 'px' : width, height: typeof height === 'number' ? height + 'px' : height }"></div>
  </div>
</template>

<script setup lang="ts">
withDefaults(defineProps<{
  width?: string | number
  height?: string | number
  row?: boolean
  count?: number
}>(), { count: 3 })
</script>

<style lang="scss" scoped>
.zh-skeleton-wrap {
  display: flex;
  flex-direction: column;
  gap: 12px;
  &.is-row { flex-direction: row; gap: 8px; }
}
.zh-skeleton-item { display: block; }
</style>
