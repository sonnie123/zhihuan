<template>
  <div class="user zh-anim-fade-in" v-if="user">
    <!-- 顶部用户卡 - 大厂商务深色 -->
    <section class="user-hero">
      <div class="user-hero__bg"></div>
      <div class="user-hero__inner">
        <div class="user-hero__left">
          <div class="user-hero__avatar">
            <ZhAvatar :src="user.avatar" :name="user.nickname" size="2xl" :verified="user.realNameStatus === 1" />
            <div class="user-hero__level" v-if="user.creditScore >= 90">SVIP</div>
          </div>
          <div class="user-hero__info">
            <div class="user-hero__name">
              {{ user.nickname }}
              <span v-if="user.realNameStatus === 1" class="user-hero__verified">
                <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M9 12l2 2 4-4"/><circle cx="12" cy="12" r="10"/></svg>
                实名认证
              </span>
            </div>
            <div class="user-hero__id">用户 ID:{{ user.id }} · 注册于 2023.06</div>
            <div class="user-hero__bio">{{ user.bio || '这个人很懒,什么都没写~' }}</div>
            <div class="user-hero__tags">
              <span class="user-hero__tag">数码爱好者</span>
              <span class="user-hero__tag">信誉卖家</span>
              <span class="user-hero__tag">AI 优先体验官</span>
            </div>
          </div>
        </div>
        <div class="user-hero__right">
          <div class="user-hero__credit">
            <div class="user-hero__credit-label">智换信用分</div>
            <div class="user-hero__credit-score">
              <span class="user-hero__credit-value zh-mono">{{ user.creditScore }}</span>
              <span class="user-hero__credit-unit">/ 100</span>
            </div>
            <div class="user-hero__credit-bar">
              <div class="user-hero__credit-bar-fill" :style="{ width: user.creditScore + '%' }"></div>
            </div>
            <div class="user-hero__credit-tier">信用等级 · 极好</div>
          </div>
          <button class="user-hero__edit">
            <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 20h9"/><path d="M16.5 3.5a2.121 2.121 0 0 1 3 3L7 19l-4 1 1-4L16.5 3.5z"/></svg>
            编辑资料
          </button>
        </div>
        <div class="user-hero__stats">
          <div class="user-hero__stat">
            <div class="user-hero__stat-value zh-mono">{{ formatNumber(user.followingCount) }}</div>
            <div class="user-hero__stat-label">关注</div>
          </div>
          <div class="user-hero__stat-divider"></div>
          <div class="user-hero__stat">
            <div class="user-hero__stat-value zh-mono">{{ formatNumber(user.followerCount) }}</div>
            <div class="user-hero__stat-label">粉丝</div>
          </div>
          <div class="user-hero__stat-divider"></div>
          <div class="user-hero__stat">
            <div class="user-hero__stat-value zh-mono">{{ formatNumber(user.favoriteCount) }}</div>
            <div class="user-hero__stat-label">获赞</div>
          </div>
          <div class="user-hero__stat-divider"></div>
          <div class="user-hero__stat">
            <div class="user-hero__stat-value zh-mono">8.6w</div>
            <div class="user-hero__stat-label">浏览</div>
          </div>
        </div>
      </div>
    </section>

    <!-- 资产卡片组 - 商务白底 + 极简线性 -->
    <div class="user-assets">
      <div class="user-assets__item">
        <div class="user-assets__item-icon">
          <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"><path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z"/><polyline points="3.27 6.96 12 12.01 20.73 6.96"/><line x1="12" y1="22.08" x2="12" y2="12"/></svg>
        </div>
        <div>
          <div class="user-assets__item-value zh-mono">{{ stats.sellCount }}</div>
          <div class="user-assets__item-label">已卖出</div>
        </div>
      </div>
      <div class="user-assets__item">
        <div class="user-assets__item-icon">
          <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"><circle cx="9" cy="21" r="1"/><circle cx="20" cy="21" r="1"/><path d="M1 1h4l2.68 13.39a2 2 0 0 0 2 1.61h9.72a2 2 0 0 0 2-1.61L23 6H6"/></svg>
        </div>
        <div>
          <div class="user-assets__item-value zh-mono">{{ stats.buyCount }}</div>
          <div class="user-assets__item-label">已买入</div>
        </div>
      </div>
      <div class="user-assets__item">
        <div class="user-assets__item-icon">
          <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"><path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"/></svg>
        </div>
        <div>
          <div class="user-assets__item-value zh-mono">{{ stats.favoriteCount }}</div>
          <div class="user-assets__item-label">收藏</div>
        </div>
      </div>
      <div class="user-assets__item">
        <div class="user-assets__item-icon">
          <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="4" r="2"/><circle cx="8" cy="13" r="2"/><circle cx="16" cy="13" r="2"/><circle cx="10" cy="20" r="2"/><circle cx="14" cy="20" r="2"/><path d="M12 6v4M8 15l2 3M16 15l-2 3"/></svg>
        </div>
        <div>
          <div class="user-assets__item-value zh-mono">{{ stats.footprintCount }}</div>
          <div class="user-assets__item-label">足迹</div>
        </div>
      </div>
    </div>

    <!-- 我的订单 - 京东/淘宝风格 -->
    <section class="user-card">
      <div class="user-card__head">
        <h3>我的订单</h3>
        <a class="user-card__more">查看全部订单 <component :is="icon.ArrowRight" /></a>
      </div>
      <div class="user-orders">
        <div
          v-for="o in orderTypes"
          :key="o.label"
          class="user-order"
        >
          <div class="user-order__icon" v-html="o.svgWrap"></div>
          <div class="user-order__label">{{ o.label }}</div>
          <div v-if="o.badge > 0" class="user-order__badge">{{ o.badge }}</div>
        </div>
      </div>
    </section>

    <!-- 核心服务 + 工具 - 8 宫格极简 -->
    <section class="user-card">
      <div class="user-card__head">
        <h3>全部服务</h3>
        <a class="user-card__more">管理 <component :is="icon.ArrowRight" /></a>
      </div>
      <div class="user-services">
        <div
          v-for="s in services"
          :key="s.label"
          class="user-service"
        >
          <div class="user-service__icon" v-html="s.svgWrap"></div>
          <div class="user-service__label">{{ s.label }}</div>
          <div v-if="s.badge" class="user-service__badge">{{ s.badge }}</div>
        </div>
      </div>
    </section>

    <!-- AI 助手入口 - 深色商务卡片 -->
    <section class="user-ai-card" @click="$router.push('/im')">
      <div class="user-ai-card__inner">
        <div class="user-ai-card__left">
          <div class="user-ai-card__icon">
            <svg viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"><path d="M12 2L9.5 9 2 9.5l5.5 5L6 22l6-3.5 6 3.5-1.5-7.5 5.5-5L14.5 9z"/></svg>
          </div>
          <div>
            <h3>智换 AI 助手</h3>
            <p>智能识别 · 估价 · 文案 · 24h 议价</p>
          </div>
        </div>
        <div class="user-ai-card__right">
          <span>立即体验</span>
          <component :is="icon.ArrowRight" />
        </div>
      </div>
      <div class="user-ai-card__features">
        <div class="user-ai-card__feature">
          <span class="user-ai-card__feature-num">96.5%</span>
          <span class="user-ai-card__feature-label">识别准确率</span>
        </div>
        <div class="user-ai-card__feature">
          <span class="user-ai-card__feature-num">8min</span>
          <span class="user-ai-card__feature-label">节省上架时间</span>
        </div>
        <div class="user-ai-card__feature">
          <span class="user-ai-card__feature-num">12%</span>
          <span class="user-ai-card__feature-label">平均议价节省</span>
        </div>
      </div>
    </section>

    <!-- 推荐关注 -->
    <section class="user-card">
      <div class="user-card__head">
        <h3>推荐关注</h3>
        <a class="user-card__more">换一批 <component :is="icon.ArrowRight" /></a>
      </div>
      <div class="user-suggest">
        <div
          v-for="u in suggestions"
          :key="u.id"
          class="user-suggest__card"
        >
          <ZhAvatar :src="u.avatar" :name="u.nickname" size="lg" :verified="true" />
          <div class="user-suggest__name">{{ u.nickname }}</div>
          <div class="user-suggest__bio">{{ u.bio || '这个人很懒,什么都没写~' }}</div>
          <div class="user-suggest__credit">信用 {{ u.creditScore }}</div>
          <button class="user-suggest__btn">+ 关注</button>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useUserStore } from '@/stores/user'
import icons from '@/design/icons'
import ZhButton from '@/design/components/ZhButton.vue'
import ZhAvatar from '@/design/components/ZhAvatar.vue'
import ZhScore from '@/design/components/ZhScore.vue'

const icon = icons
const userStore = useUserStore()
const user = computed(() => userStore.current)
const stats = computed(() => userStore.stats)

function formatNumber(n: number) {
  if (!n) return '0'
  if (n >= 10000) return (n / 10000).toFixed(1) + 'w'
  return n.toString()
}

const orderTypes = [
  { svg: '<path d="M12 2v20M17 5H9.5a3.5 3.5 0 1 0 0 7h5a3.5 3.5 0 1 1 0 7H6"/>', label: '待付款', badge: 1 },
  { svg: '<path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z"/><polyline points="3.27 6.96 12 12.01 20.73 6.96"/><line x1="12" y1="22.08" x2="12" y2="12"/>', label: '待发货', badge: 0 },
  { svg: '<rect x="1" y="3" width="15" height="13"/><polygon points="16 8 20 8 23 11 23 16 16 16 16 8"/><circle cx="5.5" cy="18.5" r="2.5"/><circle cx="18.5" cy="18.5" r="2.5"/>', label: '待收货', badge: 1 },
  { svg: '<polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/>', label: '待评价', badge: 0 },
  { svg: '<polyline points="1 4 1 10 7 10"/><path d="M3.51 15a9 9 0 1 0 2.13-9.36L1 10"/>', label: '退款/售后', badge: 0 },
  { svg: '<line x1="8" y1="6" x2="21" y2="6"/><line x1="8" y1="12" x2="21" y2="12"/><line x1="8" y1="18" x2="21" y2="18"/><line x1="3" y1="6" x2="3.01" y2="6"/><line x1="3" y1="12" x2="3.01" y2="12"/><line x1="3" y1="18" x2="3.01" y2="18"/>', label: '全部订单', badge: 0 },
].map(o => ({ ...o, svgWrap: '<svg viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">' + o.svg + '</svg>' }))

const services = [
  { svg: '<path d="M2 9V7a2 2 0 0 1 2-2h16a2 2 0 0 1 2 2v2a2 2 0 1 0 0 4v2a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2v-2a2 2 0 1 0 0-4z"/><line x1="9" y1="2" x2="9" y2="22"/>', label: '优惠券', badge: 3 },
  { svg: '<path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"/><circle cx="12" cy="10" r="3"/>', label: '收货地址', badge: 0 },
  { svg: '<path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/>', label: '账户安全', badge: 0 },
  { svg: '<circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-4 0v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1 0-4h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 4 0v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 0 4h-.09a1.65 1.65 0 0 0-1.51 1z"/>', label: '账户设置', badge: 0 },
  { svg: '<path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"/><polyline points="22,6 12,13 2,6"/>', label: '消息中心', badge: 12 },
  { svg: '<path d="M3 12a9 9 0 1 0 9-9 9.75 9.75 0 0 0-6.74 2.74L3 8"/><polyline points="3 3 3 8 8 8"/>', label: '浏览足迹', badge: 0 },
  { svg: '<path d="M16 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="8.5" cy="7" r="4"/><line x1="20" y1="8" x2="20" y2="14"/><line x1="23" y1="11" x2="17" y2="11"/>', label: '邀请好友', badge: 0 },
  { svg: '<circle cx="12" cy="12" r="10"/><path d="M9.09 9a3 3 0 0 1 5.83 1c0 2-3 3-3 3M12 17h.01"/>', label: '帮助中心', badge: 0 },
].map(s => ({ ...s, svgWrap: '<svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">' + s.svg + '</svg>' }))

const suggestions = [
  { id: 1002, nickname: '数码评测君', bio: '专业手机/笔记本评测', creditScore: 96, avatar: '' },
  { id: 1003, nickname: '二手书铺子', bio: '正版二手书专卖', creditScore: 92, avatar: '' },
  { id: 1004, nickname: '极简生活馆', bio: '只卖好用不贵的好物', creditScore: 88, avatar: '' },
  { id: 1005, nickname: '咖啡器具控', bio: '半自动咖啡机玩家', creditScore: 94, avatar: '' },
]

onMounted(async () => {
  await userStore.loadCurrent()
})
</script>

<style lang="scss" scoped>
.user {
  background: #F1F5F9;
  min-height: 100vh;
  padding-bottom: 32px;
}

/* Hero */
.user-hero {
  position: relative;
  background: linear-gradient(135deg, #0F172A 0%, #1E293B 100%);
  color: white;
  padding: 48px 0 80px;
  margin-bottom: 32px;
  overflow: hidden;

  &__bg {
    position: absolute;
    inset: 0;
    background:
      radial-gradient(circle at 20% 30%, rgba(124, 58, 237, 0.15) 0%, transparent 50%),
      radial-gradient(circle at 80% 70%, rgba(245, 158, 11, 0.08) 0%, transparent 50%);
  }

  &__inner {
    position: relative;
    max-width: var(--zh-content-max-width, 1280px);
    margin: 0 auto;
    padding: 0 32px;
    display: grid;
    grid-template-columns: 1fr auto;
    grid-template-rows: auto auto;
    column-gap: 32px;
    row-gap: 32px;
  }

  &__left {
    display: flex;
    gap: 24px;
    align-items: center;
  }

  &__avatar {
    position: relative;
    flex-shrink: 0;
  }
  &__level {
    position: absolute;
    bottom: 0;
    right: -8px;
    padding: 2px 8px;
    background: linear-gradient(135deg, #F59E0B, #D97706);
    color: white;
    font-size: 10px;
    font-weight: 800;
    border-radius: 4px;
    letter-spacing: 1px;
    box-shadow: 0 4px 12px rgba(245, 158, 11, 0.4);
  }

  &__name {
    display: flex;
    align-items: center;
    gap: 10px;
    font-size: 26px;
    font-weight: 700;
    margin-bottom: 4px;
  }
  &__verified {
    display: inline-flex;
    align-items: center;
    gap: 4px;
    padding: 2px 8px;
    background: rgba(16, 185, 129, 0.15);
    color: #10B981;
    font-size: 12px;
    font-weight: 500;
    border-radius: 4px;
    border: 1px solid rgba(16, 185, 129, 0.3);
    svg { width: 12px; height: 12px; }
  }
  &__id {
    font-size: 13px;
    color: rgba(255, 255, 255, 0.5);
    font-family: 'DIN Alternate', monospace;
    margin-bottom: 8px;
  }
  &__bio {
    font-size: 14px;
    color: rgba(255, 255, 255, 0.7);
    margin-bottom: 12px;
  }
  &__tags {
    display: flex;
    gap: 8px;
  }
  &__tag {
    padding: 2px 10px;
    background: rgba(255, 255, 255, 0.08);
    border: 1px solid rgba(255, 255, 255, 0.15);
    border-radius: 4px;
    font-size: 12px;
    color: rgba(255, 255, 255, 0.85);
  }

  &__right {
    display: flex;
    flex-direction: column;
    align-items: flex-end;
    gap: 16px;
  }
  &__credit {
    background: rgba(255, 255, 255, 0.05);
    backdrop-filter: blur(20px);
    border: 1px solid rgba(255, 255, 255, 0.1);
    border-radius: 12px;
    padding: 16px 20px;
    min-width: 220px;
  }
  &__credit-label {
    font-size: 12px;
    color: rgba(255, 255, 255, 0.5);
    margin-bottom: 6px;
  }
  &__credit-score {
    display: flex;
    align-items: baseline;
    gap: 4px;
    margin-bottom: 10px;
  }
  &__credit-value {
    font-size: 36px;
    font-weight: 800;
    background: linear-gradient(135deg, #F59E0B 0%, #FBBF24 100%);
    -webkit-background-clip: text;
    background-clip: text;
    -webkit-text-fill-color: transparent;
    line-height: 1;
  }
  &__credit-unit {
    font-size: 12px;
    color: rgba(255, 255, 255, 0.4);
  }
  &__credit-bar {
    height: 4px;
    background: rgba(255, 255, 255, 0.1);
    border-radius: 2px;
    overflow: hidden;
    margin-bottom: 8px;
  }
  &__credit-bar-fill {
    height: 100%;
    background: linear-gradient(90deg, #F59E0B, #FBBF24);
    transition: width 1s var(--zh-easing-decelerate);
  }
  &__credit-tier {
    font-size: 11px;
    color: #F59E0B;
    font-weight: 600;
  }
  &__edit {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    padding: 8px 16px;
    background: rgba(255, 255, 255, 0.1);
    border: 1px solid rgba(255, 255, 255, 0.2);
    color: white;
    border-radius: 8px;
    font-size: 13px;
    cursor: pointer;
    transition: all var(--zh-duration-base);
    &:hover {
      background: rgba(255, 255, 255, 0.15);
      border-color: rgba(255, 255, 255, 0.3);
    }
  }

  &__stats {
    grid-column: 1 / 3;
    display: flex;
    align-items: center;
    background: rgba(255, 255, 255, 0.05);
    backdrop-filter: blur(20px);
    border: 1px solid rgba(255, 255, 255, 0.08);
    border-radius: 12px;
    padding: 16px 32px;
  }
  &__stat {
    flex: 1;
    text-align: center;
  }
  &__stat-value {
    font-size: 22px;
    font-weight: 700;
    color: white;
    margin-bottom: 2px;
  }
  &__stat-label {
    font-size: 12px;
    color: rgba(255, 255, 255, 0.5);
  }
  &__stat-divider {
    width: 1px;
    height: 32px;
    background: rgba(255, 255, 255, 0.1);
  }
}

/* 资产 */
.user-assets {
  max-width: var(--zh-content-max-width, 1280px);
  margin: -48px auto 24px;
  padding: 0 32px;
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  position: relative;
  z-index: 2;
  &__item {
    display: flex;
    align-items: center;
    gap: 14px;
    background: white;
    padding: 20px 24px;
    border-radius: 12px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04), 0 1px 2px rgba(0, 0, 0, 0.06);
    border: 1px solid rgba(0, 0, 0, 0.04);
    transition: all var(--zh-duration-base);
    cursor: pointer;
    &:hover {
      transform: translateY(-2px);
      box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08);
    }
  }
  &__item-icon {
    width: 40px;
    height: 40px;
    border-radius: 10px;
    background: linear-gradient(135deg, #F8FAFC, #E2E8F0);
    color: #475569;
    display: flex;
    align-items: center;
    justify-content: center;
  }
  &__item-value {
    font-size: 22px;
    font-weight: 700;
    color: #0F172A;
    line-height: 1.1;
  }
  &__item-label {
    font-size: 12px;
    color: #64748B;
    margin-top: 2px;
  }
}

/* 通用白色卡片 */
.user-card {
  max-width: var(--zh-content-max-width, 1280px);
  margin: 0 auto 16px;
  padding: 0 32px;
  &__head {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 12px;
    h3 {
      font-size: 18px;
      font-weight: 700;
      color: #0F172A;
    }
  }
  &__more {
    display: inline-flex;
    align-items: center;
    gap: 4px;
    color: #64748B;
    font-size: 13px;
    cursor: pointer;
    transition: color var(--zh-duration-base);
    svg { width: 14px; height: 14px; }
    &:hover { color: #0F172A; }
  }
}

/* 订单 */
.user-orders {
  background: white;
  border-radius: 12px;
  padding: 20px 16px;
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
  border: 1px solid rgba(0, 0, 0, 0.04);
}
.user-order {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 12px;
  cursor: pointer;
  border-radius: 8px;
  transition: background var(--zh-duration-base);
  &:hover { background: #F8FAFC; }
  &__icon {
    width: 32px;
    height: 32px;
    display: flex;
    align-items: center;
    justify-content: center;
    color: #0F172A;
    :deep(svg) { width: 24px; height: 24px; }
  }
  &__label {
    color: #475569;
    font-size: 13px;
  }
  &__badge {
    position: absolute;
    top: 4px;
    right: 16px;
    min-width: 18px;
    height: 18px;
    padding: 0 5px;
    background: #EF4444;
    color: white;
    border-radius: 9px;
    font-size: 11px;
    font-weight: 600;
    display: inline-flex;
    align-items: center;
    justify-content: center;
  }
}

/* 服务 */
.user-services {
  background: white;
  border-radius: 12px;
  padding: 20px;
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 8px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
  border: 1px solid rgba(0, 0, 0, 0.04);
}
.user-service {
  position: relative;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 14px;
  border-radius: 8px;
  cursor: pointer;
  transition: background var(--zh-duration-base);
  &:hover { background: #F8FAFC; }
  &__icon {
    width: 32px;
    height: 32px;
    display: flex;
    align-items: center;
    justify-content: center;
    color: #0F172A;
    :deep(svg) { width: 22px; height: 22px; }
  }
  &__label {
    color: #0F172A;
    font-size: 13px;
    font-weight: 500;
  }
  &__badge {
    position: absolute;
    top: 8px;
    right: 8px;
    padding: 1px 6px;
    background: #EF4444;
    color: white;
    border-radius: 8px;
    font-size: 10px;
    font-weight: 600;
  }
}

/* AI 助手入口 */
.user-ai-card {
  max-width: var(--zh-content-max-width, 1280px);
  margin: 0 auto 16px;
  padding: 0 32px;
  cursor: pointer;
  &__inner {
    background: linear-gradient(135deg, #1E293B 0%, #0F172A 100%);
    border-radius: 12px;
    padding: 24px 32px;
    display: flex;
    justify-content: space-between;
    align-items: center;
    color: white;
    position: relative;
    overflow: hidden;
    &::before {
      content: '';
      position: absolute;
      top: -50%;
      right: -10%;
      width: 300px;
      height: 300px;
      background: radial-gradient(circle, rgba(245, 158, 11, 0.2) 0%, transparent 60%);
      border-radius: 50%;
    }
  }
  &__left {
    display: flex;
    align-items: center;
    gap: 16px;
    position: relative;
    h3 {
      font-size: 18px;
      font-weight: 700;
      margin-bottom: 4px;
    }
    p {
      color: rgba(255, 255, 255, 0.6);
      font-size: 13px;
    }
  }
  &__icon {
    width: 48px;
    height: 48px;
    background: linear-gradient(135deg, #F59E0B, #D97706);
    border-radius: 10px;
    display: flex;
    align-items: center;
    justify-content: center;
    color: white;
    box-shadow: 0 4px 12px rgba(245, 158, 11, 0.3);
  }
  &__right {
    display: inline-flex;
    align-items: center;
    gap: 8px;
    padding: 8px 16px;
    background: rgba(255, 255, 255, 0.1);
    border: 1px solid rgba(255, 255, 255, 0.2);
    border-radius: 8px;
    font-size: 13px;
    color: white;
    transition: all var(--zh-duration-base);
    position: relative;
    svg { width: 14px; height: 14px; }
  }
  &__inner:hover &__right {
    background: rgba(255, 255, 255, 0.15);
  }
  &__features {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    background: rgba(255, 255, 255, 0.04);
    border: 1px solid rgba(255, 255, 255, 0.08);
    border-top: none;
    border-radius: 0 0 12px 12px;
    overflow: hidden;
  }
  &__feature {
    padding: 16px 24px;
    display: flex;
    flex-direction: column;
    gap: 4px;
    border-right: 1px solid rgba(255, 255, 255, 0.08);
    &:last-child { border-right: none; }
  }
  &__feature-num {
    font-size: 22px;
    font-weight: 800;
    color: #FBBF24;
    font-family: 'DIN Alternate', monospace;
  }
  &__feature-label {
    font-size: 12px;
    color: rgba(255, 255, 255, 0.5);
  }
}

/* 推荐关注 */
.user-suggest {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  &__card {
    background: white;
    border-radius: 12px;
    padding: 20px;
    text-align: center;
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
    border: 1px solid rgba(0, 0, 0, 0.04);
    transition: all var(--zh-duration-base);
    &:hover {
      transform: translateY(-2px);
      box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08);
    }
  }
  &__name {
    font-weight: 600;
    margin-top: 12px;
    color: #0F172A;
  }
  &__bio {
    color: #64748B;
    font-size: 12px;
    margin: 4px 0 8px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
  &__credit {
    color: #F59E0B;
    font-size: 12px;
    font-weight: 600;
    margin-bottom: 12px;
  }
  &__btn {
    width: 100%;
    padding: 6px 0;
    background: #0F172A;
    color: white;
    border: none;
    border-radius: 6px;
    font-size: 13px;
    font-weight: 500;
    cursor: pointer;
    transition: all var(--zh-duration-base);
    &:hover {
      background: #1E293B;
    }
  }
}
</style>
