<template>
  <div class="home zh-anim-fade-in">
    <!-- Hero Banner -->
    <section class="home-hero">
      <div class="home-hero__inner">
        <div class="home-hero__content">
          <div class="home-hero__chip">
            <component :is="icon.Sparkle" />
            AI 驱动的二手交易平台
          </div>
          <h1 class="home-hero__title">
            让闲置<span class="home-hero__highlight">价值重生</span>
          </h1>
          <p class="home-hero__desc">拍照即可一键上架,AI 帮你识别商品、估价、写文案;买卖双方 AI Agent 24 小时在线议价,买卖更省心。</p>
          <div class="home-hero__cta">
            <ZhButton type="primary" size="lg" @click="$router.push('/publish')">
              <component :is="icon.Camera" />
              立即拍照发布
            </ZhButton>
            <ZhButton type="outline" size="lg" @click="$router.push('/im')">
              <component :is="icon.Message" />
              体验 AI 议价
            </ZhButton>
          </div>
          <div class="home-hero__stats">
            <div class="home-hero__stat">
              <div class="home-hero__stat-value">128,394</div>
              <div class="home-hero__stat-label">在售商品</div>
            </div>
            <div class="home-hero__stat">
              <div class="home-hero__stat-value">96.5%</div>
              <div class="home-hero__stat-label">AI 识别准确率</div>
            </div>
            <div class="home-hero__stat">
              <div class="home-hero__stat-value">¥1.2亿</div>
              <div class="home-hero__stat-label">累计交易额</div>
            </div>
          </div>
        </div>
        <div class="home-hero__art">
          <div class="home-hero__phone">
            <div class="home-hero__phone-screen">
              <div class="home-hero__phone-pulse"></div>
              <component :is="icon.Sparkle" class="home-hero__phone-icon" />
              <div class="home-hero__phone-text">AI 智能识别中...</div>
              <div class="home-hero__phone-progress">
                <div class="home-hero__phone-progress-bar"></div>
              </div>
            </div>
          </div>
          <div class="home-hero__float home-hero__float--1">
            <ZhPrice :price="6299" size="sm" />
            <div class="home-hero__float-label">AI 估价</div>
          </div>
          <div class="home-hero__float home-hero__float--2">
            <div class="home-hero__float-icon"><component :is="icon.Check" /></div>
            <div>已识别: iPhone 15 Pro</div>
          </div>
        </div>
      </div>
    </section>

    <!-- 类目 -->
    <section class="home-section">
      <div class="home-section__inner">
        <div class="home-categories">
          <div
            v-for="(cat, i) in categories"
            :key="cat.id"
            class="home-category zh-anim-fade-up"
            :style="{ animationDelay: (i * 0.04) + 's' }"
          >
            <div class="home-category__icon" v-html="getCategoryIcon(cat.icon)"></div>
            <div class="home-category__name">{{ cat.name }}</div>
          </div>
        </div>
      </div>
    </section>

    <!-- AI 智能推荐 -->
    <section class="home-section home-section--gradient">
      <div class="home-section__inner">
        <div class="home-section__header">
          <div>
            <div class="home-section__chip">
              <component :is="icon.Sparkle" />
              AI 为你精选
            </div>
            <h2 class="home-section__title">基于你的浏览偏好</h2>
            <p class="home-section__desc">智换 AI 分析你的浏览历史和兴趣标签,为你推荐最合适的闲置好物</p>
          </div>
          <a class="home-section__more">
            查看更多
            <component :is="icon.ArrowRight" />
          </a>
        </div>

        <div v-if="aiRecommend.length > 0" class="home-ai-grid">
          <article
            v-for="(p, i) in aiRecommend"
            :key="p.id"
            class="home-ai-card zh-anim-fade-up"
            :style="{ animationDelay: (i * 0.08) + 's' }"
            @click="$router.push('/product/' + p.id)"
          >
            <div class="home-ai-card__image">
              <ZhImage :src="p.coverImage" :alt="p.title" fit="cover" />
              <span class="home-ai-card__reason">
                <component :is="icon.Sparkle" />
                {{ aiReasons[i % aiReasons.length] }}
              </span>
            </div>
            <div class="home-ai-card__body">
              <h3 class="home-ai-card__title">{{ p.title }}</h3>
              <div class="home-ai-card__price">
                <ZhPrice :price="p.price" size="md" :original="p.originalPrice" />
              </div>
              <div class="home-ai-card__meta">
                <span>{{ p.viewCount }} 看过</span>
                <span class="dot">·</span>
                <span>{{ p.location }}</span>
              </div>
            </div>
          </article>
        </div>
        <div v-else class="home-ai-grid--empty">
          <div class="home-empty">📦 数据加载中,请稍候...</div>
        </div>
      </div>
    </section>

    <!-- 限时秒杀 -->
    <section class="home-section">
      <div class="home-section__inner">
        <div class="home-seckill">
          <div class="home-seckill__head">
            <div class="home-seckill__head-left">
              <span class="home-seckill__label">⚡ 限时秒杀</span>
              <span class="home-seckill__time">
                <span class="home-seckill__time-num">{{ timeStr[0] }}</span>
                <span class="home-seckill__time-colon">:</span>
                <span class="home-seckill__time-num">{{ timeStr[1] }}</span>
                <span class="home-seckill__time-colon">:</span>
                <span class="home-seckill__time-num">{{ timeStr[2] }}</span>
              </span>
            </div>
            <a class="home-section__more">查看全部 <component :is="icon.ArrowRight" /></a>
          </div>
          <div v-if="seckill.length > 0" class="home-seckill__list">
            <article
              v-for="(p, i) in seckill"
              :key="p.id"
              class="home-seckill__item zh-anim-fade-up"
              :style="{ animationDelay: (i * 0.06) + 's' }"
              @click="$router.push('/product/' + p.id)"
            >
              <ZhImage :src="p.coverImage" :alt="p.title" :width="180" :height="180" />
              <div class="home-seckill__item-price">
                <ZhPrice :price="Math.round(p.price * 0.7)" size="sm" />
              </div>
              <div class="home-seckill__item-title">{{ p.title }}</div>
            </article>
          </div>
        </div>
      </div>
    </section>

    <!-- 关注流 -->
    <section class="home-section">
      <div class="home-section__inner">
        <div class="home-section__header">
          <div>
            <h2 class="home-section__title">关注的 TA 发布了</h2>
            <p class="home-section__desc">关注的人的新动态实时推送</p>
          </div>
          <a class="home-section__more">管理关注 <component :is="icon.ArrowRight" /></a>
        </div>
        <div v-if="followFeed.length > 0" class="home-follow">
          <article
            v-for="(p, i) in followFeed"
            :key="p.id"
            class="home-follow__item zh-anim-fade-up"
            :style="{ animationDelay: (i * 0.08) + 's' }"
            @click="$router.push('/product/' + p.id)"
          >
            <div class="home-follow__head">
              <ZhAvatar :src="p.seller?.avatar" :name="p.seller?.nickname" size="sm" :verified="true" />
              <div class="home-follow__head-text">
                <div class="home-follow__name">{{ p.seller?.nickname }}</div>
                <div class="home-follow__time">10 分钟前</div>
              </div>
            </div>
            <ZhImage :src="p.coverImage" :alt="p.title" :height="260" />
            <div class="home-follow__body">
              <h3 class="home-follow__title">{{ p.title }}</h3>
              <ZhPrice :price="p.price" size="md" />
            </div>
          </article>
        </div>
      </div>
    </section>

    <!-- 猜你喜欢 -->
    <section class="home-section">
      <div class="home-section__inner">
        <div class="home-section__header">
          <div>
            <h2 class="home-section__title">猜你喜欢</h2>
            <p class="home-section__desc">基于协同过滤 + 向量召回的双路推荐</p>
          </div>
          <div class="home-tabs">
            <span v-for="(t, i) in tabs" :key="t" :class="['home-tab', activeTab === i && 'is-active']" @click="activeTab = i">{{ t }}</span>
          </div>
        </div>
        <div v-if="guess.length > 0" class="home-guess">
          <article
            v-for="(p, i) in guess"
            :key="p.id"
            class="home-guess__item zh-anim-fade-up"
            :style="{ animationDelay: ((i % 8) * 0.04) + 's' }"
            @click="$router.push('/product/' + p.id)"
          >
            <div class="home-guess__image">
              <ZhImage :src="p.coverImage" :alt="p.title" :height="220" />
              <button class="home-guess__heart" @click.stop>
                <component :is="icon.Heart" />
              </button>
            </div>
            <div class="home-guess__body">
              <h3 class="home-guess__title">{{ p.title }}</h3>
              <div class="home-guess__bottom">
                <ZhPrice :price="p.price" size="sm" />
                <span class="home-guess__similar">相似商品</span>
              </div>
              <div class="home-guess__meta">{{ p.location }} · {{ p.viewCount }} 看过</div>
            </div>
          </article>
        </div>
      </div>
    </section>

    <!-- 智换核心能力 / Features -->
    <section class="home-section home-features">
      <div class="home-section__inner">
        <div class="home-section__header">
          <div>
            <div class="home-section__chip">
              <component :is="icon.Sparkle" />
              智换核心能力
            </div>
            <h2 class="home-section__title">四大 AI 引擎,让闲置交易更聪明</h2>
            <p class="home-section__desc">从拍照上架到智能议价,智换 AI 全程参与,买卖双方都更省心</p>
          </div>
        </div>
        <div class="home-features__grid">
          <div class="home-feature zh-anim-fade-up" v-for="(f, i) in features" :key="f.title" :style="{ animationDelay: (i * 0.08) + 's' }">
            <div class="home-feature__icon" v-html="f.icon"></div>
            <h3 class="home-feature__title">{{ f.title }}</h3>
            <p class="home-feature__desc">{{ f.desc }}</p>
            <div class="home-feature__metric">
              <span class="home-feature__metric-value">{{ f.metric }}</span>
              <span class="home-feature__metric-label">{{ f.metricLabel }}</span>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- AI 议价场景演示 -->
    <section class="home-section home-bargain-demo">
      <div class="home-section__inner">
        <div class="home-bargain-demo__grid">
          <div class="home-bargain-demo__left">
            <div class="home-section__chip">
              <component :is="icon.Sparkle" />
              AI Multi-Agent 议价
            </div>
            <h2 class="home-bargain-demo__title">让 AI 帮你砍价,平均节省 12%</h2>
            <p class="home-bargain-demo__desc">买方 Agent + 卖方 Agent + 仲裁 Agent 三方协作,基于近 30 天同款成交数据,实时给出最优议价策略。</p>
            <ul class="home-bargain-demo__list">
              <li v-for="(b, i) in bargainBenefits" :key="i" v-html="b"></li>
            </ul>
            <ZhButton type="primary" size="lg" @click="$router.push('/im')">
              立即体验 <component :is="icon.ArrowRight" />
            </ZhButton>
          </div>
          <div class="home-bargain-demo__right">
            <div class="home-bargain-demo__chart">
              <div class="home-bargain-demo__chart-title">实时议价轨迹</div>
              <div class="home-bargain-demo__chart-track">
                <div class="home-bargain-demo__chart-line"></div>
                <div class="home-bargain-demo__chart-dot" v-for="(d, i) in bargainChart" :key="i" :style="{ left: d.x + '%', top: d.y + '%' }" :class="'home-bargain-demo__chart-dot--' + d.by">
                  <span class="home-bargain-demo__chart-label">{{ d.price }}</span>
                </div>
              </div>
              <div class="home-bargain-demo__chart-footer">
                <span>{{ bargainChart[0].price }}</span>
                <span class="home-bargain-demo__chart-final">成交 {{ bargainFinal }}</span>
                <span>{{ bargainChart[bargainChart.length-1].price }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- 信用分体系 / Credit -->
    <section class="home-section home-credit">
      <div class="home-section__inner">
        <div class="home-section__header">
          <div>
            <div class="home-section__chip">
              <component :is="icon.Sparkle" />
              智换信用分
            </div>
            <h2 class="home-section__title">基于交易行为的智能信用评估</h2>
            <p class="home-section__desc">信用分从 0 到 100,综合考量历史交易、评价、客服介入等 28 项指标</p>
          </div>
        </div>
        <div class="home-credit__grid">
          <div class="home-credit__item" v-for="(t, i) in creditTiers" :key="t.name" :style="{ animationDelay: (i * 0.1) + 's' }">
            <div class="home-credit__item-score">{{ t.range }}</div>
            <div class="home-credit__item-name">{{ t.name }}</div>
            <div class="home-credit__item-desc">{{ t.desc }}</div>
          </div>
        </div>
      </div>
    </section>

    <!-- 数据看板 / Stats -->
    <section class="home-section home-stats">
      <div class="home-section__inner">
        <div class="home-stats__grid">
          <div class="home-stats__item" v-for="(s, i) in bigStats" :key="i">
            <div class="home-stats__value">{{ s.value }}</div>
            <div class="home-stats__label">{{ s.label }}</div>
          </div>
        </div>
      </div>
    </section>

    <!-- 用户证言 / Testimonials -->
    <section class="home-section">
      <div class="home-section__inner">
        <div class="home-section__header">
          <div>
            <div class="home-section__chip">
              <component :is="icon.Sparkle" />
              用户怎么说
            </div>
            <h2 class="home-section__title">128 万用户的真实选择</h2>
          </div>
        </div>
        <div class="home-testimonials">
          <div class="home-testimonial" v-for="(t, i) in testimonials" :key="i" :style="{ animationDelay: (i * 0.08) + 's' }">
            <div class="home-testimonial__stars">★★★★★</div>
            <p class="home-testimonial__quote">{{ t.quote }}</p>
            <div class="home-testimonial__user">
              <div class="home-testimonial__avatar" :style="{ background: t.color }">{{ t.name[0] }}</div>
              <div>
                <div class="home-testimonial__name">{{ t.name }}</div>
                <div class="home-testimonial__meta">{{ t.role }} · 信用分 {{ t.credit }}</div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- CTA / 立即下载 -->
    <section class="home-cta">
      <div class="home-section__inner">
        <div class="home-cta__inner">
          <h2>现在加入智换,让你的闲置价值重生</h2>
          <p>拍照即上架,AI 全程托管,7×24h 议价不打烊</p>
          <div class="home-cta__buttons">
            <ZhButton type="primary" size="lg" @click="$router.push('/publish')">
              <component :is="icon.Camera" /> 立即体验
            </ZhButton>
            <ZhButton type="outline" size="lg">下载 App</ZhButton>
          </div>
        </div>
      </div>
    </section>

    <!-- Footer -->
    <footer class="home-footer">
      <div class="home-footer__inner">
        <div class="home-footer__brand">
          <div class="home-footer__logo">
            <div class="home-footer__logo-icon">Z</div>
            <span>智换</span>
          </div>
          <p>让闲置价值重生 - AI 驱动的二手交易平台</p>
        </div>
        <div class="home-footer__cols">
          <div>
            <h4>关于智换</h4>
            <a>关于我们</a>
            <a>用户协议</a>
            <a>隐私政策</a>
          </div>
          <div>
            <h4>帮助中心</h4>
            <a>常见问题</a>
            <a>卖家指南</a>
            <a>买家保障</a>
          </div>
          <div>
            <h4>联系方式</h4>
            <a>联系客服</a>
            <a>商务合作</a>
            <a>意见反馈</a>
          </div>
        </div>
      </div>
      <div class="home-footer__bottom">© 2024 智换 Zhihuan · 让闲置价值重生</div>
    </footer>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useProductStore } from '@/stores/product'
import icons from '@/design/icons'
import ZhButton from '@/design/components/ZhButton.vue'
import ZhPrice from '@/design/components/ZhPrice.vue'
import ZhImage from '@/design/components/ZhImage.vue'
import ZhAvatar from '@/design/components/ZhAvatar.vue'

const icon = icons
const productStore = useProductStore()

// 类目 icon 映射
const categoryIconMap: Record<string, string> = {
  phone: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"><rect x="6" y="2" width="12" height="20" rx="2"/><line x1="11" y1="18" x2="13" y2="18"/></svg>',
  shirt: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>',
  bag: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"><path d="M6 2 3 6v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V6l-3-4z"/><line x1="3" y1="6" x2="21" y2="6"/><path d="M16 10a4 4 0 0 1-8 0"/></svg>',
  lipstick: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"><path d="M9 11V7a3 3 0 1 1 6 0v4"/><path d="M5 11h14l-1.5 9H6.5L5 11z"/></svg>',
  home: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"><path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/><polyline points="9 22 9 12 15 12 15 22"/></svg>',
  book: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"><path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"/><path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"/></svg>',
  star: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"><polygon points="12 2 15 8.5 22 9.3 17 14 18.2 21 12 17.8 5.8 21 7 14 2 9.3 9 8.5 12 2"/></svg>',
  ball: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><path d="M2 12h20M12 2a15 15 0 0 1 0 20M12 2a15 15 0 0 0 0 20"/></svg>',
}
function getCategoryIcon(name: string) {
  return categoryIconMap[name] || categoryIconMap.phone
}

const categories = computed(() => productStore.categories)
const aiRecommend = computed(() => productStore.aiRecommend)
const seckill = computed(() => productStore.seckill)
const followFeed = computed(() => productStore.followFeed)
const guess = computed(() => productStore.guess)

const aiReasons = [
  '你喜欢数码',
  'AI 估价为同款最低',
  '卖家信用 96 分',
  '同款最近 30 天成交 128 笔',
  '同价位中浏览最多',
]

const tabs = ['为你推荐', '数码', '服饰', '鞋包', '美妆']
const activeTab = ref(0)

// 秒杀倒计时
const timeStr = ref(['02', '34', '56'])
let timer: any
onMounted(() => {
  productStore.loadHome()
  let total = 2 * 3600 + 34 * 60 + 56
  timer = setInterval(() => {
    if (total <= 0) total = 86400
    total--
    const h = Math.floor(total / 3600)
    const m = Math.floor((total % 3600) / 60)
    const s = total % 60
    timeStr.value = [h.toString().padStart(2, '0'), m.toString().padStart(2, '0'), s.toString().padStart(2, '0')]
  }, 1000)
})
onUnmounted(() => clearInterval(timer))

// 4 大 AI 能力
const features = [
  {
    title: 'AI 智能识别',
    desc: '拍照即可识别品牌、型号、成色,自动归类到正确类目',
    metric: '96.5%',
    metricLabel: '识别准确率',
    icon: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><path d="M23 19a2 2 0 0 1-2 2H3a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h4l2-3h6l2 3h4a2 2 0 0 1 2 2z"/><circle cx="12" cy="13" r="4"/></svg>',
  },
  {
    title: 'AI 估价',
    desc: '基于近 30 天同型号、同成色、上千条成交数据,给出合理价格区间',
    metric: '5950',
    metricLabel: 'iPhone 15 Pro 中位价',
    icon: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><line x1="12" y1="1" x2="12" y2="23"/><path d="M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6"/></svg>',
  },
  {
    title: 'AI 智能议价',
    desc: 'Multi-Agent 协作,买卖双方 AI 7x24 在线协商,平均节省 12%',
    metric: '12%',
    metricLabel: '平均节省',
    icon: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/></svg>',
  },
  {
    title: 'AI 风险审核',
    desc: '图片合规、商品描述、违禁词、价格异常 4 维自动审核',
    metric: '< 0.1%',
    metricLabel: '违规商品率',
    icon: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/></svg>',
  },
]

const bargainBenefits = [
  '✓ 基于同款近 30 天 1,283 条成交数据',
  '✓ 实时分析买卖双方心理价位',
  '✓ 智能让步策略,既不让买家吃亏,也不让卖家流失',
  '✓ 24h 不打烊,深夜也能砍价',
]

const bargainChart = [
  { by: 'seller', price: 6299, x: 0, y: 5 },
  { by: 'buyer',  price: 5800, x: 20, y: 70 },
  { by: 'ai',     price: 5950, x: 40, y: 50 },
  { by: 'seller', price: 6100, x: 60, y: 25 },
  { by: 'buyer',  price: 6050, x: 80, y: 35 },
  { by: 'ai',     price: 5950, x: 100, y: 50 },
]
const bargainFinal = 5950

const creditTiers = [
  { range: '90-100', name: '极好', desc: '享受平台担保金减免、优先推荐位' },
  { range: '80-89', name: '优秀', desc: '标准服务 + 部分高级权益' },
  { range: '70-79', name: '良好', desc: '标准服务,交易正常' },
  { range: '60-69', name: '一般', desc: '部分功能受限,需完善资料' },
  { range: '< 60', name: '待提升', desc: '无法使用议价和担保交易' },
]

const bigStats = [
  { value: '128 万+', label: '注册用户' },
  { value: '5.2 亿', label: '累计成交 GMV' },
  { value: '96.5%', label: 'AI 识别准确率' },
  { value: '24/7', label: 'AI 议价服务' },
]

const testimonials = [
  {
    name: '陈一',
    role: 'iPhone 卖家',
    credit: 96,
    color: 'linear-gradient(135deg, #7C3AED, #06B6D4)',
    quote: '原本挂闲鱼 3 个月没卖出去,智换 AI 帮我写文案、估价,2 天就成交了,价格还多卖了 500 块。',
  },
  {
    name: '林悦',
    role: '二手摄影爱好者',
    credit: 92,
    color: 'linear-gradient(135deg, #F59E0B, #EF4444)',
    quote: 'AI 议价太惊艳了,凌晨 2 点跟卖家 AI 谈,最后成交价比心理价位还低 8%。',
  },
  {
    name: '王志',
    role: '电子产品回收商',
    credit: 88,
    color: 'linear-gradient(135deg, #10B981, #06B6D4)',
    quote: '作为职业卖家,信用分体系帮我筛选出优质买家,坏账率从 8% 降到了 1% 以下。',
  },
]
</script>

<style lang="scss" scoped>
.home {
  color: var(--zh-color-text);
}

.home-section {
  padding: 32px 0;
  &__inner {
    max-width: var(--zh-content-max-width);
    margin: 0 auto;
    padding: 0 32px;
  }
  &--gradient {
    background: var(--zh-gradient-primary-soft);
    margin: 32px 0;
    padding: 48px 0;
  }
  &__header {
    display: flex;
    align-items: flex-end;
    justify-content: space-between;
    margin-bottom: 24px;
  }
  &__chip {
    display: inline-flex;
    align-items: center;
    gap: 4px;
    padding: 4px 10px;
    background: var(--zh-gradient-primary);
    color: white;
    border-radius: var(--zh-radius-full);
    font-size: var(--zh-font-size-xs);
    font-weight: 600;
    margin-bottom: 8px;
    svg { width: 12px; height: 12px; }
  }
  &__title {
    font-size: var(--zh-font-size-3xl);
    font-weight: 700;
    color: var(--zh-color-text);
  }
  &__desc {
    font-size: var(--zh-font-size-sm);
    color: var(--zh-color-text-tertiary);
    margin-top: 4px;
  }
  &__more {
    display: inline-flex;
    align-items: center;
    gap: 4px;
    color: var(--zh-color-primary);
    font-size: var(--zh-font-size-sm);
    font-weight: 500;
    cursor: pointer;
    transition: gap var(--zh-duration-base) var(--zh-easing-standard);
    svg { width: 14px; height: 14px; }
    &:hover { gap: 8px; }
  }
}

/* Hero */
.home-hero {
  position: relative;
  padding: 48px 0 64px;
  overflow: hidden;
  background:
    radial-gradient(ellipse at top left, rgba(124, 58, 237, 0.12) 0%, transparent 50%),
    radial-gradient(ellipse at bottom right, rgba(6, 182, 212, 0.12) 0%, transparent 50%);

  &__inner {
    max-width: var(--zh-content-wide);
    margin: 0 auto;
    padding: 0 32px;
    display: grid;
    grid-template-columns: 1.2fr 1fr;
    gap: 48px;
    align-items: center;
  }
  &__content { position: relative; z-index: 1; }
  &__chip {
    display: inline-flex;
    align-items: center;
    gap: 4px;
    padding: 6px 14px;
    background: rgba(255, 255, 255, 0.6);
    backdrop-filter: blur(20px);
    border: 1px solid rgba(124, 58, 237, 0.2);
    color: var(--zh-color-primary);
    border-radius: var(--zh-radius-full);
    font-size: var(--zh-font-size-sm);
    font-weight: 600;
    margin-bottom: 16px;
    svg { width: 14px; height: 14px; }
  }
  &__title {
    font-size: 56px;
    font-weight: 800;
    line-height: 1.1;
    letter-spacing: -0.02em;
    margin-bottom: 20px;
  }
  &__highlight {
    background: var(--zh-gradient-primary);
    -webkit-background-clip: text;
    background-clip: text;
    -webkit-text-fill-color: transparent;
    position: relative;
    &::after {
      content: '';
      position: absolute;
      left: 0;
      right: 0;
      bottom: 4px;
      height: 12px;
      background: rgba(124, 58, 237, 0.15);
      border-radius: 4px;
      z-index: -1;
    }
  }
  &__desc {
    font-size: var(--zh-font-size-lg);
    color: var(--zh-color-text-secondary);
    line-height: 1.7;
    margin-bottom: 32px;
    max-width: 540px;
  }
  &__cta {
    display: flex;
    gap: 16px;
    margin-bottom: 48px;
  }
  &__stats {
    display: flex;
    gap: 32px;
    padding: 20px 24px;
    background: rgba(255, 255, 255, 0.6);
    backdrop-filter: blur(20px);
    border: 1px solid rgba(255, 255, 255, 0.5);
    border-radius: var(--zh-radius-lg);
  }
  &__stat {
    &-value {
      font-size: var(--zh-font-size-2xl);
      font-weight: 800;
      background: var(--zh-gradient-primary);
      -webkit-background-clip: text;
      background-clip: text;
      -webkit-text-fill-color: transparent;
    }
    &-label {
      font-size: var(--zh-font-size-xs);
      color: var(--zh-color-text-tertiary);
      margin-top: 2px;
    }
  }

  &__art {
    position: relative;
    height: 480px;
  }
  &__phone {
    position: absolute;
    top: 50%;
    left: 50%;
    transform: translate(-50%, -50%);
    width: 280px;
    height: 420px;
    background: var(--zh-gradient-dark);
    border-radius: 36px;
    box-shadow: var(--zh-shadow-xl), 0 0 0 8px rgba(255, 255, 255, 0.3);
    padding: 12px;
    animation: zh-float 4s ease-in-out infinite;
  }
  &__phone-screen {
    width: 100%;
    height: 100%;
    background: linear-gradient(180deg, #1E1B4B 0%, #0F172A 100%);
    border-radius: 24px;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    color: white;
    position: relative;
    overflow: hidden;
  }
  &__phone-pulse {
    position: absolute;
    width: 120px;
    height: 120px;
    border-radius: 50%;
    background: var(--zh-gradient-primary);
    opacity: 0.4;
    animation: zh-breathe 2s ease-in-out infinite;
  }
  &__phone-icon {
    position: relative;
    width: 48px;
    height: 48px;
    color: white;
    z-index: 1;
  }
  &__phone-text {
    position: relative;
    z-index: 1;
    font-size: var(--zh-font-size-sm);
    color: rgba(255, 255, 255, 0.8);
    margin-top: 16px;
  }
  &__phone-progress {
    position: relative;
    z-index: 1;
    width: 160px;
    height: 4px;
    background: rgba(255, 255, 255, 0.1);
    border-radius: var(--zh-radius-full);
    margin-top: 12px;
    overflow: hidden;
  }
  &__phone-progress-bar {
    width: 70%;
    height: 100%;
    background: var(--zh-gradient-primary);
    border-radius: var(--zh-radius-full);
    animation: zh-progress-stripe 1.5s linear infinite;
    background-image: linear-gradient(45deg, rgba(255,255,255,.2) 25%, transparent 25%, transparent 50%, rgba(255,255,255,.2) 50%, rgba(255,255,255,.2) 75%, transparent 75%);
    background-size: 40px 40px;
  }

  &__float {
    position: absolute;
    padding: 12px 16px;
    background: white;
    border-radius: var(--zh-radius-md);
    box-shadow: var(--zh-shadow-md);
    display: flex;
    align-items: center;
    gap: 8px;
    z-index: 2;
    animation: zh-float 3.5s ease-in-out infinite;
    &--1 {
      top: 60px;
      right: 0;
      animation-delay: 0.5s;
    }
    &--2 {
      bottom: 80px;
      left: 0;
      font-size: var(--zh-font-size-sm);
      color: var(--zh-color-text);
      animation-delay: 1s;
    }
  }
  &__float-icon {
    width: 24px;
    height: 24px;
    border-radius: 50%;
    background: var(--zh-color-success);
    color: white;
    display: flex;
    align-items: center;
    justify-content: center;
    svg { width: 14px; height: 14px; }
  }
  &__float-label {
    font-size: var(--zh-font-size-xs);
    color: var(--zh-color-text-tertiary);
  }
}

/* Categories */
.home-categories {
  display: grid;
  grid-template-columns: repeat(8, 1fr);
  gap: 16px;
}
.home-category {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 16px 8px;
  border-radius: var(--zh-radius-lg);
  cursor: pointer;
  transition: all var(--zh-duration-base) var(--zh-easing-standard);
  &:hover {
    background: white;
    box-shadow: var(--zh-shadow-base);
    transform: translateY(-2px);
  }
  &__icon {
    width: 56px;
    height: 56px;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 32px;
    background: var(--zh-gradient-primary-soft);
    border-radius: 50%;
    transition: all var(--zh-duration-base) var(--zh-easing-spring);
  }
  &:hover &__icon { transform: scale(1.1); background: var(--zh-gradient-primary); }
  &__name {
    font-size: var(--zh-font-size-sm);
    color: var(--zh-color-text);
    font-weight: 500;
  }
}

/* AI Grid */
.home-ai-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
}
.home-ai-card {
  background: white;
  border-radius: var(--zh-radius-lg);
  overflow: hidden;
  cursor: pointer;
  transition: all var(--zh-duration-base) var(--zh-easing-standard);
  border: 1px solid var(--zh-color-border-light);
  &:hover {
    transform: translateY(-4px);
    box-shadow: var(--zh-shadow-md);
    border-color: var(--zh-color-primary-light);
  }
  &__image {
    position: relative;
    aspect-ratio: 1;
  }
  &__reason {
    position: absolute;
    bottom: 8px;
    left: 8px;
    padding: 4px 8px;
    background: var(--zh-gradient-primary);
    color: white;
    border-radius: var(--zh-radius-full);
    font-size: 11px;
    font-weight: 500;
    display: inline-flex;
    align-items: center;
    gap: 3px;
    backdrop-filter: blur(8px);
    svg { width: 10px; height: 10px; }
  }
  &__body { padding: 12px 14px; }
  &__title {
    font-size: var(--zh-font-size-base);
    font-weight: 500;
    color: var(--zh-color-text);
    line-height: 1.4;
    height: 2.8em;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
    margin-bottom: 8px;
  }
  &__price { margin-bottom: 6px; }
  &__meta {
    display: flex;
    align-items: center;
    gap: 4px;
    font-size: var(--zh-font-size-xs);
    color: var(--zh-color-text-tertiary);
    .dot { color: var(--zh-color-border); }
  }
}

/* 秒杀 */
.home-seckill {
  background: linear-gradient(135deg, rgba(245, 158, 11, 0.08) 0%, rgba(244, 63, 94, 0.05) 100%);
  border-radius: var(--zh-radius-lg);
  padding: 24px;
  border: 1px solid rgba(245, 158, 11, 0.15);
  &__head {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;
  }
  &__head-left {
    display: flex;
    align-items: center;
    gap: 16px;
  }
  &__label {
    font-size: var(--zh-font-size-xl);
    font-weight: 800;
    color: var(--zh-color-danger);
  }
  &__time {
    display: inline-flex;
    align-items: center;
    gap: 4px;
  }
  &__time-num {
    display: inline-block;
    padding: 4px 8px;
    background: var(--zh-color-text);
    color: white;
    border-radius: var(--zh-radius-sm);
    font-family: var(--zh-font-family-mono);
    font-weight: 700;
    font-size: var(--zh-font-size-base);
    min-width: 32px;
    text-align: center;
  }
  &__time-colon {
    color: var(--zh-color-text);
    font-weight: 700;
  }
  &__list {
    display: flex;
    gap: 16px;
    overflow-x: auto;
    scrollbar-width: none;
    &::-webkit-scrollbar { display: none; }
  }
  &__item {
    flex-shrink: 0;
    width: 180px;
    background: white;
    border-radius: var(--zh-radius-md);
    padding: 12px;
    cursor: pointer;
    transition: all var(--zh-duration-base) var(--zh-easing-standard);
    &:hover { transform: translateY(-2px); box-shadow: var(--zh-shadow-base); }
  }
  &__item-title {
    font-size: var(--zh-font-size-sm);
    color: var(--zh-color-text);
    margin-top: 8px;
    overflow: hidden;
    text-overflow: ellipsis;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    line-height: 1.4;
    height: 2.8em;
  }
}

/* 关注流 */
.home-follow {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
}
.home-follow__item {
  background: white;
  border-radius: var(--zh-radius-lg);
  overflow: hidden;
  cursor: pointer;
  transition: all var(--zh-duration-base) var(--zh-easing-standard);
  border: 1px solid var(--zh-color-border-light);
  &:hover { transform: translateY(-2px); box-shadow: var(--zh-shadow-md); }
  &__head {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 12px 14px;
  }
  &__name {
    font-size: var(--zh-font-size-sm);
    font-weight: 600;
    color: var(--zh-color-text);
  }
  &__time {
    font-size: var(--zh-font-size-xs);
    color: var(--zh-color-text-tertiary);
  }
  &__body { padding: 12px 14px; }
  &__title {
    font-size: var(--zh-font-size-sm);
    color: var(--zh-color-text);
    margin-bottom: 6px;
    overflow: hidden;
    text-overflow: ellipsis;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    line-height: 1.4;
  }
}

/* 猜你喜欢 */
.home-tabs {
  display: flex;
  gap: 4px;
}
.home-tab {
  padding: 6px 14px;
  font-size: var(--zh-font-size-sm);
  color: var(--zh-color-text-secondary);
  border-radius: var(--zh-radius-full);
  cursor: pointer;
  transition: all var(--zh-duration-base) var(--zh-easing-standard);
  &:hover { background: var(--zh-color-bg-hover); }
  &.is-active {
    background: var(--zh-gradient-primary);
    color: white;
    font-weight: 600;
  }
}
.home-guess {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 16px;
}
.home-guess__item {
  background: white;
  border-radius: var(--zh-radius-md);
  overflow: hidden;
  cursor: pointer;
  transition: all var(--zh-duration-base) var(--zh-easing-standard);
  border: 1px solid var(--zh-color-border-light);
  &:hover {
    transform: translateY(-4px);
    box-shadow: var(--zh-shadow-md);
  }
  &__image {
    position: relative;
  }
  &__heart {
    position: absolute;
    top: 8px;
    right: 8px;
    width: 32px;
    height: 32px;
    background: rgba(255, 255, 255, 0.9);
    backdrop-filter: blur(8px);
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    color: var(--zh-color-text-secondary);
    transition: all var(--zh-duration-base) var(--zh-easing-standard);
    svg { width: 16px; height: 16px; }
    &:hover {
      color: var(--zh-color-danger);
      background: white;
      transform: scale(1.1);
    }
  }
  &__body { padding: 10px 12px; }
  &__title {
    font-size: var(--zh-font-size-sm);
    color: var(--zh-color-text);
    margin-bottom: 6px;
    overflow: hidden;
    text-overflow: ellipsis;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    line-height: 1.4;
    height: 2.8em;
  }
  &__bottom {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 4px;
  }
  &__similar {
    font-size: 10px;
    padding: 1px 6px;
    background: var(--zh-color-info-light);
    color: var(--zh-color-info);
    border-radius: var(--zh-radius-sm);
  }
  &__meta {
    font-size: var(--zh-font-size-xs);
    color: var(--zh-color-text-tertiary);
  }
}

/* Footer */
.home-footer {
  margin-top: 64px;
  background: var(--zh-color-bg-card);
  border-top: 1px solid var(--zh-color-border-light);
  padding: 48px 0 24px;
  &__inner {
    max-width: var(--zh-content-wide);
    margin: 0 auto;
    padding: 0 32px;
    display: grid;
    grid-template-columns: 1.2fr 2fr;
    gap: 48px;
  }
  &__brand p {
    color: var(--zh-color-text-tertiary);
    font-size: var(--zh-font-size-sm);
    margin-top: 12px;
  }
  &__logo {
    display: flex;
    align-items: center;
    gap: 10px;
    font-size: var(--zh-font-size-xl);
    font-weight: 700;
    span { background: var(--zh-gradient-primary); -webkit-background-clip: text; background-clip: text; -webkit-text-fill-color: transparent; }
  }
  &__logo-icon {
    width: 36px;
    height: 36px;
    background: var(--zh-gradient-primary);
    border-radius: var(--zh-radius-base);
    color: white;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 18px;
    font-weight: 800;
  }
  &__cols {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 24px;
    h4 {
      font-size: var(--zh-font-size-base);
      color: var(--zh-color-text);
      margin-bottom: 12px;
    }
    a {
      display: block;
      font-size: var(--zh-font-size-sm);
      color: var(--zh-color-text-tertiary);
      padding: 4px 0;
      cursor: pointer;
      transition: color var(--zh-duration-base) var(--zh-easing-standard);
      &:hover { color: var(--zh-color-primary); }
    }
  }
  &__bottom {
    text-align: center;
    color: var(--zh-color-text-tertiary);
    font-size: var(--zh-font-size-xs);
    margin-top: 32px;
    padding-top: 24px;
    border-top: 1px solid var(--zh-color-border-light);
  }
}

/* Features 4 大能力 */
.home-features {
  background: linear-gradient(180deg, #FAFAFB 0%, #FFFFFF 100%);
}
.home-features__grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 24px;
}
.home-feature {
  background: white;
  border: 1px solid var(--zh-color-border-light);
  border-radius: var(--zh-radius-lg);
  padding: 32px 28px;
  transition: all var(--zh-duration-base) var(--zh-easing-standard);
}
.home-feature:hover {
  transform: translateY(-4px);
  box-shadow: var(--zh-shadow-md);
  border-color: rgba(124, 58, 237, 0.2);
}
.home-feature__icon {
  width: 48px;
  height: 48px;
  border-radius: var(--zh-radius-md);
  background: var(--zh-gradient-primary-soft);
  color: var(--zh-color-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 16px;
}
.home-feature__icon :deep(svg) { width: 24px; height: 24px; }
.home-feature__title { font-size: var(--zh-font-size-lg); font-weight: 700; margin-bottom: 8px; }
.home-feature__desc { color: var(--zh-color-text-secondary); font-size: var(--zh-font-size-sm); line-height: 1.6; min-height: 48px; }
.home-feature__metric {
  display: flex;
  align-items: baseline;
  gap: 8px;
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px dashed var(--zh-color-border);
}
.home-feature__metric-value {
  font-size: var(--zh-font-size-2xl);
  font-weight: 800;
  color: var(--zh-color-primary);
  font-family: 'DIN Alternate', 'Helvetica Neue', monospace;
}
.home-feature__metric-label { font-size: var(--zh-font-size-xs); color: var(--zh-color-text-tertiary); }

/* Bargain Demo */
.home-bargain-demo {
  background: linear-gradient(135deg, #0F172A 0%, #1E293B 100%);
  color: white;
  margin: 32px 0;
  padding: 64px 0;
  position: relative;
  overflow: hidden;
}
.home-bargain-demo::before {
  content: '';
  position: absolute;
  top: -50%;
  right: -10%;
  width: 600px;
  height: 600px;
  background: radial-gradient(circle, rgba(124, 58, 237, 0.3) 0%, transparent 70%);
  border-radius: 50%;
}
.home-bargain-demo::after {
  content: '';
  position: absolute;
  bottom: -30%;
  left: -10%;
  width: 500px;
  height: 500px;
  background: radial-gradient(circle, rgba(6, 182, 212, 0.25) 0%, transparent 70%);
  border-radius: 50%;
}
.home-bargain-demo__grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 64px;
  align-items: center;
  position: relative;
  z-index: 1;
}
.home-bargain-demo__left .home-section__chip {
  background: rgba(255, 255, 255, 0.1);
  color: #06B6D4;
  backdrop-filter: blur(8px);
}
.home-bargain-demo__title {
  font-size: 40px;
  font-weight: 800;
  line-height: 1.2;
  margin: 16px 0;
  background: linear-gradient(135deg, #FFFFFF 0%, #06B6D4 100%);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
}
.home-bargain-demo__desc {
  font-size: var(--zh-font-size-base);
  line-height: 1.7;
  color: rgba(255, 255, 255, 0.7);
  margin-bottom: 24px;
}
.home-bargain-demo__list {
  list-style: none;
  padding: 0;
  margin: 0 0 32px;
}
.home-bargain-demo__list li {
  padding: 8px 0;
  color: rgba(255, 255, 255, 0.85);
  font-size: var(--zh-font-size-sm);
}
.home-bargain-demo__right {
  background: rgba(255, 255, 255, 0.05);
  backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: var(--zh-radius-lg);
  padding: 32px;
}
.home-bargain-demo__chart-title {
  font-size: var(--zh-font-size-sm);
  color: rgba(255, 255, 255, 0.6);
  margin-bottom: 24px;
}
.home-bargain-demo__chart-track {
  position: relative;
  height: 200px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}
.home-bargain-demo__chart-line {
  position: absolute;
  left: 0;
  right: 0;
  top: 50%;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.2), transparent);
}
.home-bargain-demo__chart-dot {
  position: absolute;
  transform: translate(-50%, -50%);
  width: 40px;
  height: 40px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  font-weight: 700;
  color: white;
  animation: zh-bargain-fade-in 0.5s var(--zh-easing-decelerate) backwards;
}
.home-bargain-demo__chart-dot--seller { background: var(--zh-color-warning); }
.home-bargain-demo__chart-dot--buyer { background: var(--zh-color-info); }
.home-bargain-demo__chart-dot--ai {
  background: var(--zh-gradient-primary);
  box-shadow: 0 0 0 4px rgba(124, 58, 237, 0.3);
  animation: zh-bargain-fade-in 0.5s var(--zh-easing-decelerate) backwards, zh-pulse 2s ease-in-out infinite;
}
.home-bargain-demo__chart-label {
  font-family: 'DIN Alternate', monospace;
  font-weight: 700;
}
.home-bargain-demo__chart-footer {
  display: flex;
  justify-content: space-between;
  margin-top: 16px;
  font-size: var(--zh-font-size-sm);
  color: rgba(255, 255, 255, 0.6);
}
.home-bargain-demo__chart-final {
  color: var(--zh-color-success);
  font-weight: 700;
  font-size: var(--zh-font-size-base);
}
@keyframes zh-bargain-fade-in {
  from { opacity: 0; transform: translate(-50%, -50%) scale(0.5); }
  to { opacity: 1; transform: translate(-50%, -50%) scale(1); }
}
@keyframes zh-pulse {
  0%, 100% { box-shadow: 0 0 0 4px rgba(124, 58, 237, 0.3); }
  50% { box-shadow: 0 0 0 8px rgba(124, 58, 237, 0.1); }
}

/* Credit 信用分 */
.home-credit {
  background: var(--zh-color-bg);
}
.home-credit__grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 16px;
}
.home-credit__item {
  background: white;
  border-radius: var(--zh-radius-md);
  padding: 24px 20px;
  border: 1px solid var(--zh-color-border-light);
  transition: all var(--zh-duration-base) var(--zh-easing-standard);
  animation: zh-fade-up 0.5s var(--zh-easing-decelerate) backwards;
}
.home-credit__item:hover {
  transform: translateY(-2px);
  box-shadow: var(--zh-shadow-md);
}
.home-credit__item-score {
  font-size: var(--zh-font-size-xl);
  font-weight: 800;
  color: var(--zh-color-primary);
  font-family: 'DIN Alternate', monospace;
  margin-bottom: 4px;
}
.home-credit__item-name { font-size: var(--zh-font-size-base); font-weight: 600; margin-bottom: 8px; }
.home-credit__item-desc { font-size: var(--zh-font-size-xs); color: var(--zh-color-text-tertiary); line-height: 1.5; }

/* Stats 数据看板 */
.home-stats {
  background: white;
  border-top: 1px solid var(--zh-color-border-light);
  border-bottom: 1px solid var(--zh-color-border-light);
  padding: 48px 0;
}
.home-stats__grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 32px;
}
.home-stats__item {
  text-align: center;
  border-right: 1px solid var(--zh-color-border-light);
}
.home-stats__item:last-child { border-right: none; }
.home-stats__value {
  font-size: 40px;
  font-weight: 800;
  background: var(--zh-gradient-primary);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
  font-family: 'DIN Alternate', monospace;
  line-height: 1.1;
}
.home-stats__label {
  margin-top: 8px;
  color: var(--zh-color-text-secondary);
  font-size: var(--zh-font-size-sm);
}

/* Testimonials */
.home-testimonials {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 24px;
}
.home-testimonial {
  background: white;
  border: 1px solid var(--zh-color-border-light);
  border-radius: var(--zh-radius-lg);
  padding: 28px;
  transition: all var(--zh-duration-base) var(--zh-easing-standard);
  animation: zh-fade-up 0.5s var(--zh-easing-decelerate) backwards;
}
.home-testimonial:hover {
  transform: translateY(-4px);
  box-shadow: var(--zh-shadow-md);
}
.home-testimonial__stars {
  color: #F59E0B;
  font-size: 18px;
  letter-spacing: 2px;
  margin-bottom: 16px;
}
.home-testimonial__quote {
  color: var(--zh-color-text);
  line-height: 1.7;
  font-size: var(--zh-font-size-sm);
  margin-bottom: 20px;
  min-height: 96px;
}
.home-testimonial__user { display: flex; align-items: center; gap: 12px; }
.home-testimonial__avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-weight: 700;
}
.home-testimonial__name { font-weight: 600; font-size: var(--zh-font-size-sm); }
.home-testimonial__meta { font-size: var(--zh-font-size-xs); color: var(--zh-color-text-tertiary); margin-top: 2px; }

/* CTA */
.home-cta {
  padding: 80px 0;
  background: linear-gradient(135deg, #0F172A 0%, #1E1B4B 100%);
  position: relative;
  overflow: hidden;
}
.home-cta::before {
  content: '';
  position: absolute;
  inset: 0;
  background:
    radial-gradient(circle at 20% 50%, rgba(124, 58, 237, 0.4) 0%, transparent 50%),
    radial-gradient(circle at 80% 50%, rgba(6, 182, 212, 0.4) 0%, transparent 50%);
}
.home-cta__inner {
  text-align: center;
  color: white;
  position: relative;
}
.home-cta__inner h2 {
  font-size: 40px;
  font-weight: 800;
  margin-bottom: 12px;
}
.home-cta__inner p {
  font-size: var(--zh-font-size-lg);
  color: rgba(255, 255, 255, 0.8);
  margin-bottom: 32px;
}
.home-cta__buttons {
  display: flex;
  gap: 16px;
  justify-content: center;
}

@keyframes zh-fade-up {
  from { opacity: 0; transform: translateY(20px); }
  to { opacity: 1; transform: translateY(0); }
}

/* category icon 渲染 SVG */
.home-category__icon :deep(svg) {
  width: 28px;
  height: 28px;
}

/* category empty / skeleton */
.home-empty, .home-ai-grid--empty {
  padding: 60px 0;
  text-align: center;
  color: var(--zh-color-text-tertiary);
  font-size: var(--zh-font-size-sm);
}

</style>
