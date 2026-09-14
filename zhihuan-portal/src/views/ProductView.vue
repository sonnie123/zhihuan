<template>
  <div class="product zh-anim-fade-in" v-if="product">
    <div class="product__main">
      <!-- 左侧:图片 + 详情 -->
      <div class="product__left">
        <!-- 图集 -->
        <div class="product-gallery">
          <div class="product-gallery__main">
            <img :src="activeImage" :alt="product.title" />
            <span class="product-gallery__count">{{ activeIdx + 1 }} / {{ product.images.length }}</span>
            <span class="product-gallery__zoom">
              <component :is="icon.Image" />
            </span>
          </div>
          <div class="product-gallery__thumbs">
            <div
              v-for="(img, i) in product.images"
              :key="i"
              :class="['product-gallery__thumb', i === activeIdx && 'is-active']"
              @click="activeIdx = i"
            >
              <img :src="img" :alt="`图${i + 1}`" />
            </div>
          </div>
        </div>

        <!-- 价格 + 标题 -->
        <div class="product-info">
          <div class="product-price">
            <ZhPrice :price="product.price" size="2xl" :original="product.originalPrice" />
            <span v-if="aiRange" class="product-price__ai" @click="aiPanelOpen = !aiPanelOpen">
              <component :is="icon.Sparkle" />
              AI 估价 ¥{{ aiRange.low }}-{{ aiRange.high }}
            </span>
          </div>
          <h1 class="product-title">{{ product.title }}</h1>
          <div class="product-tags">
            <ZhTag v-for="t in product.tags" :key="t" type="primary">{{ t }}</ZhTag>
            <ZhTag type="success">{{ product.conditionText }}</ZhTag>
            <ZhTag type="warning">议价</ZhTag>
            <ZhTag>包邮</ZhTag>
          </div>

          <Transition name="fade">
            <div v-if="aiPanelOpen && aiRange" class="product-ai-box">
              <div class="product-ai-box__head">
                <component :is="icon.Sparkle" />
                <strong>AI 估价依据</strong>
              </div>
              <div class="product-ai-box__body">
                <p>{{ aiRange.reason }}</p>
                <div class="product-ai-box__chart">
                  <div class="product-ai-box__chart-bar" :style="{ left: aiBarLeft + '%', width: aiBarWidth + '%' }">
                    <span class="product-ai-box__chart-mid">中位 ¥{{ product.price }}</span>
                  </div>
                </div>
                <div class="product-ai-box__range">
                  <span>¥{{ aiRange.low }}</span>
                  <span>¥{{ aiRange.high }}</span>
                </div>
              </div>
            </div>
          </Transition>
        </div>

        <!-- 卖家 -->
        <div class="product-seller zh-glass">
          <ZhAvatar :src="product.seller?.avatar" :name="product.seller?.nickname" size="xl" :verified="true" />
          <div class="product-seller__info">
            <div class="product-seller__name">
              {{ product.seller?.nickname }}
              <ZhTag type="primary" size="sm">个人认证</ZhTag>
            </div>
            <div class="product-seller__bio">{{ product.seller?.bio }}</div>
            <div class="product-seller__stats">
              <div><strong class="zh-mono">{{ product.seller?.followerCount }}</strong><span>粉丝</span></div>
              <div><strong class="zh-mono">{{ product.seller?.favoriteCount }}</strong><span>商品</span></div>
              <div><strong class="zh-mono">96.5%</strong><span>好评率</span></div>
            </div>
          </div>
          <div class="product-seller__action">
            <ZhButton type="outline" size="sm">+ 关注</ZhButton>
            <ZhButton type="text" size="sm">进店逛逛 →</ZhButton>
          </div>
        </div>

        <!-- 描述 -->
        <div class="product-section">
          <h3 class="product-section__title">商品描述</h3>
          <div class="product-section__body">
            <p v-for="(line, i) in descLines" :key="i">{{ line }}</p>
          </div>
        </div>

        <!-- 参数 -->
        <div class="product-section">
          <h3 class="product-section__title">商品参数</h3>
          <div class="product-params">
            <div v-for="p in params" :key="p.label" class="product-params__row">
              <span class="product-params__label">{{ p.label }}</span>
              <span class="product-params__value">{{ p.value }}</span>
            </div>
          </div>
        </div>

        <!-- 评价 -->
        <div class="product-section">
          <h3 class="product-section__title">
            评价 <span class="product-section__count">(128)</span>
          </h3>
          <div class="product-rating">
            <div class="product-rating__overview">
              <div class="product-rating__score">4.9</div>
              <div class="product-rating__stars">★★★★★</div>
              <div class="product-rating__sub">综合评分</div>
            </div>
            <div class="product-rating__tags">
              <ZhTag type="success">描述一致 99%</ZhTag>
              <ZhTag type="success">服务态度好 98%</ZhTag>
              <ZhTag type="success">物流快速 96%</ZhTag>
              <ZhTag type="warning">回复速度一般 3%</ZhTag>
            </div>
          </div>
          <div v-for="(r, i) in reviews" :key="i" class="product-review">
            <div class="product-review__head">
              <ZhAvatar :src="r.avatar" :name="r.name" size="sm" />
              <div>
                <div class="product-review__name">{{ r.name }}</div>
                <div class="product-review__meta">{{ r.time }} · {{ r.spec }}</div>
              </div>
              <div class="product-review__stars">{{ '★'.repeat(r.score) }}</div>
            </div>
            <p class="product-review__content">{{ r.content }}</p>
          </div>
        </div>
      </div>

      <!-- 右侧:固定操作区 -->
      <aside class="product__right">
        <div class="product-actions-card">
          <div class="product-actions-card__price">
            <ZhPrice :price="product.price" size="xl" :original="product.originalPrice" />
          </div>
          <div class="product-actions-card__title">{{ product.title }}</div>
          <div class="product-actions-card__meta">
            <span>{{ product.location }}</span>
            <span>·</span>
            <span>{{ product.viewCount }} 看过</span>
            <span>·</span>
            <span>{{ product.favoriteCount }} 想要</span>
          </div>

          <div class="product-actions-card__guarantee">
            <div><component :is="icon.Shield" />担保交易</div>
            <div><component :is="icon.Check" />7天无理由</div>
            <div><component :is="icon.Sparkle" />AI 验真</div>
          </div>

          <div class="product-actions-card__btns">
            <ZhButton type="outline" block @click="$router.push('/im/chat/c1')">
              <component :is="icon.Message" />
              联系卖家
            </ZhButton>
            <ZhButton type="primary" block @click="onBuy">
              <component :is="icon.Cart" />
              立即购买
            </ZhButton>
            <ZhButton type="accent" block @click="onBargain">
              <component :is="icon.Sparkle" />
              AI 议价
            </ZhButton>
          </div>
        </div>

        <!-- 同款比价 -->
        <div class="product-similar-card">
          <h4 class="product-similar-card__title">
            <component :is="icon.Sparkle" />
            同款比价
          </h4>
          <div class="product-similar-card__list">
            <div v-for="(s, i) in similar" :key="i" class="product-similar-card__item">
              <ZhImage :src="s.coverImage" :width="60" :height="60" />
              <div class="product-similar-card__info">
                <div class="product-similar-card__name">{{ s.title.slice(0, 18) }}...</div>
                <ZhPrice :price="s.price" size="sm" />
              </div>
            </div>
          </div>
        </div>
      </aside>
    </div>
  </div>
  <ZhEmpty v-else title="商品不存在" description="该商品可能已下架" />
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { productApi } from '@/api/modules/product.api'
import type { ProductDTO } from '@/types/entity/product'
import icons from '@/design/icons'
import ZhButton from '@/design/components/ZhButton.vue'
import ZhPrice from '@/design/components/ZhPrice.vue'
import ZhImage from '@/design/components/ZhImage.vue'
import ZhAvatar from '@/design/components/ZhAvatar.vue'
import ZhTag from '@/design/components/ZhTag.vue'
import ZhEmpty from '@/design/components/ZhEmpty.vue'

const route = useRoute()
const icon = icons
const product = ref<ProductDTO | null>(null)
const similar = ref<ProductDTO[]>([])
const activeIdx = ref(0)
const aiPanelOpen = ref(true)

const activeImage = computed(() => product.value?.images[activeIdx.value] || '')
const descLines = computed(() => product.value?.description.split('\n') || [])
const aiRange = computed(() => product.value?.aiPriceRange || { low: 5500, high: 6300, reason: '基于近 30 天同型号同成色 1,283 条成交记录分析' })

const aiBarLeft = computed(() => {
  const r = aiRange.value
  if (!r) return 0
  const total = r.high * 1.2
  return (r.low / total) * 100
})
const aiBarWidth = computed(() => {
  const r = aiRange.value
  if (!r) return 0
  const total = r.high * 1.2
  return ((r.high - r.low) / total) * 100
})

const params = [
  { label: '品牌', value: 'Apple' },
  { label: '型号', value: 'iPhone 15 Pro' },
  { label: '颜色', value: '原色钛金属' },
  { label: '存储', value: '256GB' },
  { label: '成色', value: '9 成新' },
  { label: '电池效率', value: '96%' },
  { label: '购买时间', value: '2023-10' },
  { label: '是否在保', value: '是 (至 2024-10)' },
  { label: '发货地', value: '北京 朝阳' },
]

const reviews = [
  { name: '张同学', avatar: 'https://api.dicebear.com/7.x/avataaars/svg?seed=u1', time: '3 天前', spec: '256G 原色钛金属', score: 5, content: '卖家发货很快,机器和描述完全一致,几乎全新,电池效率也很高,很满意的一次购物!' },
  { name: '李先生', avatar: 'https://api.dicebear.com/7.x/avataaars/svg?seed=u2', time: '1 周前', spec: '256G 原色钛金属', score: 5, content: '专业卖家,沟通顺畅,验机报告齐全,价格合理。' },
  { name: '王女士', avatar: 'https://api.dicebear.com/7.x/avataaars/svg?seed=u3', time: '2 周前', spec: '512G 白色钛金属', score: 4, content: '整体不错,快递稍慢,但是机器成色真的很好。' },
]

async function load() {
  const id = Number(route.params.id)
  product.value = await productApi.getById(id)
  similar.value = await productApi.getSimilar(id)
}

function onBuy() {
  alert('下单功能 - 即将跳转结算页 (Phase 2)')
}
function onBargain() {
  alert('AI 议价 - 跳转 IM 议价页')
}

watch(() => route.params.id, load)
onMounted(load)
</script>

<style lang="scss" scoped>
.product {
  padding: 24px 0 64px;
  &__main {
    display: grid;
    grid-template-columns: 1fr 360px;
    gap: 32px;
    max-width: var(--zh-content-wide);
    margin: 0 auto;
    padding: 0 32px;
  }
}

.product-gallery {
  position: sticky;
  top: calc(var(--zh-header-height) + 16px);
  &__main {
    position: relative;
    aspect-ratio: 1;
    background: white;
    border-radius: var(--zh-radius-lg);
    overflow: hidden;
    margin-bottom: 12px;
    box-shadow: var(--zh-shadow-sm);
    img { width: 100%; height: 100%; object-fit: cover; }
  }
  &__count {
    position: absolute;
    bottom: 12px;
    right: 12px;
    padding: 4px 10px;
    background: rgba(0, 0, 0, 0.5);
    backdrop-filter: blur(8px);
    color: white;
    border-radius: var(--zh-radius-full);
    font-size: var(--zh-font-size-xs);
  }
  &__zoom {
    position: absolute;
    top: 12px;
    right: 12px;
    width: 36px;
    height: 36px;
    background: rgba(0, 0, 0, 0.5);
    backdrop-filter: blur(8px);
    color: white;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    cursor: pointer;
    transition: all var(--zh-duration-base) var(--zh-easing-standard);
    svg { width: 16px; height: 16px; }
    &:hover { background: rgba(0, 0, 0, 0.7); }
  }
  &__thumbs {
    display: flex;
    gap: 8px;
    overflow-x: auto;
    scrollbar-width: none;
    &::-webkit-scrollbar { display: none; }
  }
  &__thumb {
    flex-shrink: 0;
    width: 72px;
    height: 72px;
    border-radius: var(--zh-radius-sm);
    overflow: hidden;
    cursor: pointer;
    border: 2px solid transparent;
    transition: all var(--zh-duration-base) var(--zh-easing-standard);
    img { width: 100%; height: 100%; object-fit: cover; }
    &.is-active { border-color: var(--zh-color-primary); }
  }
}

.product-info {
  background: white;
  border-radius: var(--zh-radius-lg);
  padding: 24px;
  margin-top: 24px;
  box-shadow: var(--zh-shadow-sm);
}
.product-price {
  display: flex;
  align-items: baseline;
  gap: 12px;
  margin-bottom: 12px;
  &__ai {
    display: inline-flex;
    align-items: center;
    gap: 4px;
    padding: 4px 10px;
    background: var(--zh-gradient-primary-soft);
    color: var(--zh-color-primary);
    border-radius: var(--zh-radius-full);
    font-size: var(--zh-font-size-xs);
    font-weight: 500;
    cursor: pointer;
    transition: all var(--zh-duration-base) var(--zh-easing-standard);
    svg { width: 12px; height: 12px; }
    &:hover { background: var(--zh-gradient-primary); color: white; }
  }
}
.product-title {
  font-size: var(--zh-font-size-2xl);
  font-weight: 600;
  line-height: 1.4;
  color: var(--zh-color-text);
  margin-bottom: 16px;
}
.product-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}
.product-ai-box {
  margin-top: 16px;
  padding: 16px;
  background: var(--zh-gradient-primary-soft);
  border-radius: var(--zh-radius-md);
  border: 1px solid rgba(124, 58, 237, 0.15);
  &__head {
    display: flex;
    align-items: center;
    gap: 6px;
    color: var(--zh-color-primary);
    margin-bottom: 8px;
    svg { width: 14px; height: 14px; }
  }
  &__body {
    color: var(--zh-color-text-secondary);
    font-size: var(--zh-font-size-sm);
    line-height: 1.6;
  }
  &__chart {
    position: relative;
    height: 6px;
    background: var(--zh-color-border-light);
    border-radius: var(--zh-radius-full);
    margin: 12px 0 4px;
  }
  &__chart-bar {
    position: absolute;
    top: -2px;
    height: 10px;
    background: var(--zh-gradient-primary);
    border-radius: var(--zh-radius-full);
  }
  &__chart-mid {
    position: absolute;
    top: -28px;
    left: 50%;
    transform: translateX(-50%);
    padding: 2px 8px;
    background: var(--zh-color-text);
    color: white;
    border-radius: var(--zh-radius-sm);
    font-size: 11px;
    white-space: nowrap;
    &::after {
      content: '';
      position: absolute;
      bottom: -4px;
      left: 50%;
      transform: translateX(-50%);
      border: 4px solid transparent;
      border-top-color: var(--zh-color-text);
    }
  }
  &__range {
    display: flex;
    justify-content: space-between;
    font-size: var(--zh-font-size-xs);
    color: var(--zh-color-text-tertiary);
    font-family: var(--zh-font-family-mono);
  }
}

.product-seller {
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 20px 24px;
  border-radius: var(--zh-radius-lg);
  margin-top: 16px;
  background: rgba(255, 255, 255, 0.7);
  border: 1px solid rgba(124, 58, 237, 0.1);
  &__info { flex: 1; }
  &__name {
    font-size: var(--zh-font-size-lg);
    font-weight: 600;
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 4px;
  }
  &__bio {
    color: var(--zh-color-text-tertiary);
    font-size: var(--zh-font-size-sm);
    margin-bottom: 12px;
  }
  &__stats {
    display: flex;
    gap: 24px;
    div {
      display: flex;
      flex-direction: column;
      align-items: center;
    }
    strong {
      font-size: var(--zh-font-size-lg);
      color: var(--zh-color-text);
    }
    span {
      font-size: var(--zh-font-size-xs);
      color: var(--zh-color-text-tertiary);
      margin-top: 2px;
    }
  }
  &__action {
    display: flex;
    flex-direction: column;
    gap: 8px;
    align-items: flex-end;
  }
}

.product-section {
  background: white;
  border-radius: var(--zh-radius-lg);
  padding: 24px;
  margin-top: 16px;
  box-shadow: var(--zh-shadow-sm);
  &__title {
    font-size: var(--zh-font-size-lg);
    font-weight: 600;
    margin-bottom: 16px;
    color: var(--zh-color-text);
  }
  &__count {
    font-size: var(--zh-font-size-sm);
    color: var(--zh-color-text-tertiary);
    font-weight: 400;
  }
  &__body {
    color: var(--zh-color-text-secondary);
    line-height: 1.8;
    font-size: var(--zh-font-size-base);
    p { margin-bottom: 8px; }
  }
}

.product-params {
  &__row {
    display: flex;
    padding: 8px 0;
    border-bottom: 1px solid var(--zh-color-border-light);
    &:last-child { border-bottom: none; }
  }
  &__label {
    width: 100px;
    color: var(--zh-color-text-tertiary);
    font-size: var(--zh-font-size-sm);
  }
  &__value {
    color: var(--zh-color-text);
    font-size: var(--zh-font-size-sm);
  }
}

.product-rating {
  display: flex;
  align-items: center;
  gap: 24px;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--zh-color-border-light);
  margin-bottom: 16px;
  &__overview { text-align: center; }
  &__score {
    font-size: 32px;
    font-weight: 800;
    color: var(--zh-color-warning);
    line-height: 1;
  }
  &__stars { color: var(--zh-color-warning); margin-top: 4px; }
  &__sub { font-size: var(--zh-font-size-xs); color: var(--zh-color-text-tertiary); }
  &__tags { display: flex; flex-wrap: wrap; gap: 6px; flex: 1; }
}

.product-review {
  padding: 12px 0;
  border-bottom: 1px solid var(--zh-color-border-light);
  &:last-child { border-bottom: none; }
  &__head {
    display: flex;
    align-items: center;
    gap: 10px;
    margin-bottom: 8px;
  }
  &__name { font-size: var(--zh-font-size-sm); font-weight: 500; }
  &__meta { font-size: var(--zh-font-size-xs); color: var(--zh-color-text-tertiary); }
  &__stars { margin-left: auto; color: var(--zh-color-warning); }
  &__content { color: var(--zh-color-text-secondary); font-size: var(--zh-font-size-sm); line-height: 1.6; }
}

.product__right {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.product-actions-card {
  position: sticky;
  top: calc(var(--zh-header-height) + 16px);
  background: white;
  border-radius: var(--zh-radius-lg);
  padding: 24px;
  box-shadow: var(--zh-shadow-base);
  border: 1px solid var(--zh-color-border-light);
  &__price { margin-bottom: 12px; }
  &__title {
    font-size: var(--zh-font-size-base);
    color: var(--zh-color-text);
    line-height: 1.5;
    margin-bottom: 8px;
    overflow: hidden;
    text-overflow: ellipsis;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
  }
  &__meta {
    display: flex;
    gap: 6px;
    color: var(--zh-color-text-tertiary);
    font-size: var(--zh-font-size-xs);
    margin-bottom: 16px;
  }
  &__guarantee {
    display: flex;
    gap: 12px;
    margin-bottom: 16px;
    padding: 12px;
    background: var(--zh-color-bg);
    border-radius: var(--zh-radius-md);
    div {
      display: inline-flex;
      align-items: center;
      gap: 4px;
      font-size: var(--zh-font-size-xs);
      color: var(--zh-color-text-secondary);
    }
    svg { width: 14px; height: 14px; color: var(--zh-color-primary); }
  }
  &__btns {
    display: flex;
    flex-direction: column;
    gap: 10px;
  }
}

.product-similar-card {
  background: white;
  border-radius: var(--zh-radius-lg);
  padding: 20px;
  box-shadow: var(--zh-shadow-sm);
  &__title {
    display: flex;
    align-items: center;
    gap: 6px;
    font-size: var(--zh-font-size-base);
    font-weight: 600;
    margin-bottom: 12px;
    color: var(--zh-color-text);
    svg { width: 14px; height: 14px; color: var(--zh-color-primary); }
  }
  &__item {
    display: flex;
    gap: 10px;
    padding: 8px 0;
    border-bottom: 1px solid var(--zh-color-border-light);
    cursor: pointer;
    transition: background var(--zh-duration-base) var(--zh-easing-standard);
    &:hover { background: var(--zh-color-bg); }
    &:last-child { border-bottom: none; }
  }
  &__info { flex: 1; min-width: 0; }
  &__name {
    font-size: var(--zh-font-size-sm);
    color: var(--zh-color-text);
    margin-bottom: 4px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}
</style>
