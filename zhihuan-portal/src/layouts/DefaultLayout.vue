<template>
  <div class="default-layout">
    <header :class="['layout-header', scrolled && 'is-scrolled']">
      <div class="layout-header__inner">
        <router-link to="/" class="layout-logo">
          <div class="layout-logo__icon">
            <span>Z</span>
          </div>
          <span class="layout-logo__text">智换</span>
        </router-link>

        <div class="layout-search" @click="$router.push('/')">
          <component :is="icon.Search" />
          <input placeholder="搜索闲置好物,试试 AI 智能搜索" readonly />
          <span class="layout-search__ai">
            <component :is="icon.Sparkle" />
            AI
          </span>
        </div>

        <nav class="layout-nav">
          <button class="layout-nav__icon-btn" @click="$router.push('/im')" aria-label="消息">
            <component :is="icon.Message" />
            <span v-if="unreadCount > 0" class="layout-nav__badge">{{ unreadCount }}</span>
          </button>
          <button class="layout-nav__icon-btn" aria-label="购物车">
            <component :is="icon.Cart" />
          </button>
          <router-link to="/user" class="layout-nav__avatar">
            <ZhAvatar v-if="userStore.current" :src="userStore.current.avatar" :name="userStore.current.nickname" size="sm" />
            <ZhAvatar v-else :name="'?'" size="sm" />
          </router-link>
        </nav>
      </div>
    </header>

    <main class="layout-main">
      <router-view />
    </main>

    <nav class="layout-tabbar">
      <router-link to="/" class="layout-tabbar__item" :class="{ active: $route.name === 'home' }">
        <component :is="icon.Home" />
        <span>首页</span>
      </router-link>
      <router-link to="/" class="layout-tabbar__item" :class="{ active: false }">
        <component :is="icon.Category" />
        <span>分类</span>
      </router-link>
      <router-link to="/publish" class="layout-tabbar__publish">
        <div class="layout-tabbar__publish-btn">
          <component :is="icon.Publish" />
        </div>
        <span>发布</span>
      </router-link>
      <router-link to="/im" class="layout-tabbar__item" :class="{ active: $route.path.startsWith('/im') }">
        <component :is="icon.Message" />
        <span>消息</span>
      </router-link>
      <router-link to="/user" class="layout-tabbar__item" :class="{ active: $route.name === 'user' }">
        <component :is="icon.User" />
        <span>我的</span>
      </router-link>
    </nav>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useUserStore } from '@/stores/user'
import { notificationApi } from '@/api/modules/notification.api'
import icons from '@/design/icons'
import ZhAvatar from '@/design/components/ZhAvatar.vue'

const icon = icons
const userStore = useUserStore()
const scrolled = ref(false)
const unreadCount = ref(0)

function onScroll() { scrolled.value = window.scrollY > 16 }
onMounted(() => {
  window.addEventListener('scroll', onScroll, { passive: true })
  notificationApi.getUnreadCount().then(r => unreadCount.value = r.count).catch(() => {})
})
onUnmounted(() => window.removeEventListener('scroll', onScroll))
</script>

<style lang="scss" scoped>
.default-layout {
  min-height: 100vh;
  padding-bottom: var(--zh-tabbar-height);
}

.layout-header {
  position: sticky;
  top: 0;
  z-index: 100;
  background: var(--zh-glass-bg);
  backdrop-filter: blur(20px) saturate(180%);
  -webkit-backdrop-filter: blur(20px) saturate(180%);
  border-bottom: 1px solid transparent;
  transition: all var(--zh-duration-base) var(--zh-easing-standard);

  &.is-scrolled {
    background: rgba(255, 255, 255, 0.85);
    box-shadow: var(--zh-shadow-sm);
    border-bottom-color: var(--zh-color-border-light);
  }

  &__inner {
    max-width: var(--zh-content-wide);
    margin: 0 auto;
    height: var(--zh-header-height);
    padding: 0 32px;
    display: flex;
    align-items: center;
    gap: 24px;
  }
}

.layout-logo {
  display: flex;
  align-items: center;
  gap: 10px;
  font-weight: 700;
  font-size: 18px;
  text-decoration: none;
  color: var(--zh-color-text);
  flex-shrink: 0;

  &__icon {
    width: 36px;
    height: 36px;
    background: var(--zh-gradient-primary);
    border-radius: var(--zh-radius-base);
    display: flex;
    align-items: center;
    justify-content: center;
    color: white;
    font-size: 18px;
    font-weight: 800;
    box-shadow: var(--zh-shadow-primary);
  }
  &__text {
    background: var(--zh-gradient-primary);
    -webkit-background-clip: text;
    background-clip: text;
    -webkit-text-fill-color: transparent;
  }
}

.layout-search {
  flex: 1;
  max-width: 480px;
  height: 40px;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 0 16px;
  background: var(--zh-color-bg);
  border: 1px solid var(--zh-color-border);
  border-radius: var(--zh-radius-full);
  cursor: pointer;
  transition: all var(--zh-duration-base) var(--zh-easing-standard);

  svg { width: 16px; height: 16px; color: var(--zh-color-text-tertiary); flex-shrink: 0; }
  input {
    flex: 1;
    background: transparent;
    border: none;
    outline: none;
    font-size: var(--zh-font-size-base);
    color: var(--zh-color-text);
    &::placeholder { color: var(--zh-color-text-tertiary); }
  }

  &__ai {
    display: inline-flex;
    align-items: center;
    gap: 3px;
    padding: 3px 8px;
    background: var(--zh-gradient-primary);
    color: white;
    border-radius: var(--zh-radius-full);
    font-size: 11px;
    font-weight: 700;
    svg { width: 11px; height: 11px; color: white; }
  }

  &:hover {
    border-color: var(--zh-color-primary-light);
    box-shadow: 0 0 0 3px rgba(124, 58, 237, 0.1);
  }
}

.layout-nav {
  display: flex;
  align-items: center;
  gap: 4px;
  flex-shrink: 0;

  &__icon-btn {
    position: relative;
    width: 40px;
    height: 40px;
    border-radius: var(--zh-radius-base);
    display: inline-flex;
    align-items: center;
    justify-content: center;
    color: var(--zh-color-text-secondary);
    transition: all var(--zh-duration-base) var(--zh-easing-standard);
    cursor: pointer;

    svg { width: 20px; height: 20px; }
    &:hover { background: var(--zh-color-bg-hover); color: var(--zh-color-text); }
  }

  &__badge {
    position: absolute;
    top: 6px;
    right: 6px;
    min-width: 16px;
    height: 16px;
    padding: 0 4px;
    background: var(--zh-color-danger);
    color: white;
    border-radius: var(--zh-radius-full);
    font-size: 10px;
    font-weight: 700;
    display: inline-flex;
    align-items: center;
    justify-content: center;
  }

  &__avatar { display: inline-flex; }
}

.layout-main {
  max-width: var(--zh-content-wide);
  margin: 0 auto;
  min-height: calc(100vh - var(--zh-header-height) - var(--zh-tabbar-height));
}

.layout-tabbar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  z-index: 100;
  height: var(--zh-tabbar-height);
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(20px) saturate(180%);
  -webkit-backdrop-filter: blur(20px) saturate(180%);
  border-top: 1px solid var(--zh-color-border-light);
  display: flex;
  align-items: center;
  justify-content: space-around;
  padding: 0 8px;

  &__item {
    flex: 1;
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 2px;
    color: var(--zh-color-text-tertiary);
    font-size: 11px;
    text-decoration: none;
    transition: color var(--zh-duration-base) var(--zh-easing-standard);

    svg { width: 22px; height: 22px; }
    &.active { color: var(--zh-color-primary); }
  }

  &__publish {
    flex: 1;
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 2px;
    text-decoration: none;
    color: var(--zh-color-text-tertiary);
    font-size: 11px;

    &-btn {
      width: 48px;
      height: 36px;
      background: var(--zh-gradient-primary);
      border-radius: var(--zh-radius-lg);
      display: flex;
      align-items: center;
      justify-content: center;
      color: white;
      box-shadow: var(--zh-shadow-primary);
      transition: all var(--zh-duration-base) var(--zh-easing-spring);
      svg { width: 24px; height: 24px; }
    }
    &:hover &-btn { transform: translateY(-2px) scale(1.05); }
  }
}
</style>
