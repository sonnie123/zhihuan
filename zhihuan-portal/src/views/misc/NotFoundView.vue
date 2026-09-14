<template>
  <div class="not-found">
    <div class="not-found__inner">
      <div class="not-found__code zh-mono">404</div>
      <h1>页面去火星了</h1>
      <p>你访问的页面不存在,可能已下架或被删除</p>
      <ZhButton type="primary" @click="$router.push('/')">回到首页</ZhButton>
    </div>
  </div>
</template>

<script setup lang="ts">
import ZhButton from '@/design/components/ZhButton.vue'
</script>

<style lang="scss" scoped>
.not-found {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--zh-gradient-primary-soft);
  &__inner {
    text-align: center;
    h1 { font-size: 32px; font-weight: 700; margin-bottom: 8px; }
    p { color: var(--zh-color-text-tertiary); margin-bottom: 24px; }
  }
  &__code {
    font-size: 120px;
    font-weight: 800;
    background: var(--zh-gradient-primary);
    -webkit-background-clip: text;
    background-clip: text;
    -webkit-text-fill-color: transparent;
    line-height: 1;
  }
}
</style>
