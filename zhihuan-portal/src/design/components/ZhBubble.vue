<template>
  <div :class="['zh-bubble', 'zh-bubble--' + (isMe ? 'me' : 'them'), type && 'zh-bubble--' + type]">
    <slot />
  </div>
</template>

<script setup lang="ts">
defineProps<{ isMe?: boolean; type?: 'text' | 'image' | 'product' | 'system' }>()
</script>

<style lang="scss" scoped>
.zh-bubble {
  max-width: 70%;
  padding: 10px 14px;
  border-radius: 16px;
  font-size: var(--zh-font-size-base);
  line-height: 1.5;
  word-break: break-word;
  position: relative;

  &--me {
    align-self: flex-end;
    background: var(--zh-gradient-primary);
    color: white;
    border-bottom-right-radius: 4px;
    box-shadow: var(--zh-shadow-primary);
  }
  &--them {
    align-self: flex-start;
    background: white;
    color: var(--zh-color-text);
    border: 1px solid var(--zh-color-border-light);
    border-bottom-left-radius: 4px;
  }
  &--system {
    align-self: center;
    background: var(--zh-color-border-light);
    color: var(--zh-color-text-tertiary);
    font-size: var(--zh-font-size-xs);
    padding: 4px 12px;
    border-radius: var(--zh-radius-full);
  }
  &--product { padding: 0; background: transparent; border: none; box-shadow: none; max-width: 80%; }
  &--image { padding: 4px; }
}
</style>
