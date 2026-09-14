<template>
  <Teleport to="body">
    <Transition name="slide-up">
      <div v-if="visible" :class="['zh-toast', 'zh-toast--' + type]">
        <span class="zh-toast__icon">{{ iconForType }}</span>
        <span class="zh-toast__msg">{{ message }}</span>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup lang="ts">
import { ref } from 'vue'

const visible = ref(false)
const message = ref('')
const type = ref<'success' | 'error' | 'info' | 'warning'>('success')

const icons = { success: '✓', error: '✕', info: 'ℹ', warning: '⚠' }
const iconForType = ref('✓')

export function showToast(msg: string, t: 'success' | 'error' | 'info' | 'warning' = 'success', duration = 2500) {
  message.value = msg
  type.value = t
  iconForType.value = icons[t]
  visible.value = true
  setTimeout(() => visible.value = false, duration)
}
</script>

<style lang="scss" scoped>
.zh-toast {
  position: fixed;
  top: 80px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 9999;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 20px;
  background: var(--zh-color-text);
  color: white;
  border-radius: var(--zh-radius-md);
  box-shadow: var(--zh-shadow-lg);
  font-size: var(--zh-font-size-base);
  font-weight: 500;

  &__icon {
    width: 20px;
    height: 20px;
    border-radius: 50%;
    background: white;
    color: var(--zh-color-text);
    display: inline-flex;
    align-items: center;
    justify-content: center;
    font-size: 12px;
    font-weight: 700;
  }
  &--success &__icon { background: var(--zh-color-success); color: white; }
  &--error &__icon { background: var(--zh-color-danger); color: white; }
  &--warning &__icon { background: var(--zh-color-warning); color: white; }
}
</style>
