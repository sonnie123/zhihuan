<template>
  <div class="im-page zh-anim-fade-in">
    <!-- Left: Conversations -->
    <aside class="im-page__left">
      <div class="im-page__left-head">
        <router-link to="/" class="im-page__back" title="返回首页">
          <component :is="icon.ArrowLeft" />
        </router-link>
        <h2>消息</h2>
        <button class="im-page__icon-btn">
          <component :is="icon.Plus" />
        </button>
      </div>
      <div v-if="loading" class="im-page__loading">
        <span>加载中...</span>
      </div>
      <div v-else-if="imStore.conversations.length === 0" class="im-page__empty">
        <component :is="icon.Message" />
        <p>暂无会话</p>
        <router-link to="/" class="im-page__empty-link">去首页逛逛 →</router-link>
      </div>
      <div class="im-page__search">
        <component :is="icon.Search" />
        <input placeholder="搜索" />
      </div>
      <div class="im-page__list">
        <div
          v-for="(c, i) in imStore.conversations"
          :key="c.id"
          :class="['im-page__item', activeId === c.id && 'is-active']"
          @click="openChat(c.id)"
        >
          <ZhAvatar :src="c.targetUser.avatar" :name="c.targetUser.nickname" size="md" />
          <div class="im-page__item-content">
            <div class="im-page__item-row">
              <span class="im-page__item-name">{{ c.targetUser.nickname }}</span>
              <span class="im-page__item-time">{{ c.lastTime }}</span>
            </div>
            <div class="im-page__item-msg">
              <span v-if="c.pinned" class="im-page__item-tag">📌</span>
              <span v-if="c.muted" class="im-page__item-tag">🔕</span>
              <span class="im-page__item-text">{{ c.lastMessage }}</span>
              <span v-if="c.unreadCount > 0" class="im-page__item-badge">{{ c.unreadCount }}</span>
            </div>
          </div>
        </div>
      </div>
    </aside>

    <!-- Center: Chat -->
    <main class="im-page__center" v-if="activeConv">
      <div class="im-page__chat-head">
        <button v-if="route.params.id" class="im-page__back" @click="backToList()" title="返回消息列表">
          <component :is="icon.ArrowLeft" />
        </button>
        <div class="im-page__chat-head-info">
          <ZhAvatar :src="activeConv.targetUser.avatar" :name="activeConv.targetUser.nickname" size="sm" :verified="true" />
          <div>
            <div class="im-page__chat-head-name">{{ activeConv.targetUser.nickname }}</div>
            <div class="im-page__chat-head-status">
              <span class="online-dot"></span>
              在线
            </div>
          </div>
        </div>
        <div class="im-page__chat-head-actions">
          <button class="im-page__icon-btn" @click="rightPanelOpen = !rightPanelOpen">
            <component :is="icon.User" />
          </button>
        </div>
      </div>

      <div ref="messageList" class="im-page__messages">
        <!-- 商品卡片头部 -->
        <div class="im-page__product-header" v-if="activeConv.productId">
          <ZhImage :src="activeConv.productImage!" :width="60" :height="60" />
          <div class="im-page__product-header-info">
            <div class="im-page__product-header-title">{{ activeConv.productTitle }}</div>
            <ZhPrice :price="activeConv.productPrice!" size="sm" />
          </div>
          <button class="im-page__product-header-btn">查看商品</button>
        </div>

        <!-- 消息流 -->
        <div
          v-for="msg in messages"
          :key="msg.id"
          :class="['im-msg', msg.senderId === 0 ? 'is-system' : msg.senderId === currentUserId ? 'is-me' : 'is-them']"
        >
          <!-- 系统消息 -->
          <div v-if="msg.type === 'system'" class="im-msg__system">{{ msg.content }}</div>

          <!-- AI 议价卡 -->
          <div v-else-if="msg.type === 'ai_bargain' && msg.bargainData" class="im-msg__bargain">
            <div class="im-msg__bargain-head">
              <div class="im-msg__bargain-icon">
                <component :is="icon.Sparkle" />
              </div>
              <div>
                <div class="im-msg__bargain-title">AI 议价中</div>
                <div class="im-msg__bargain-sub">智换 Multi-Agent 为你协商</div>
              </div>
              <div v-if="msg.bargainData.status === 'agreed'" class="im-msg__bargain-status is-agreed">
                <component :is="icon.Check" /> 已成交
              </div>
            </div>

            <div class="im-msg__bargain-chart">
              <div class="im-msg__bargain-track"></div>
              <div class="im-msg__bargain-progress" :style="{ left: progressLeft(msg.bargainData) + '%' }"></div>
              <div
                v-for="(round, i) in msg.bargainData.rounds"
                :key="i"
                :class="['im-msg__bargain-point', 'im-msg__bargain-point--' + round.by]"
                :style="{ left: (round.price / msg.bargainData.initialPrice * 100) + '%' }"
                :title="'¥' + round.price"
              ></div>
            </div>

            <div class="im-msg__bargain-rounds">
              <div v-for="(round, i) in msg.bargainData.rounds" :key="i" :class="['im-msg__bargain-round', 'is-' + round.by]">
                <div class="im-msg__bargain-round-head">
                  <span class="im-msg__bargain-round-tag">{{ roundLabel(round.by) }}</span>
                  <span class="im-msg__bargain-round-price zh-mono">¥{{ round.price.toLocaleString() }}</span>
                </div>
                <p class="im-msg__bargain-round-msg">{{ round.message }}</p>
              </div>
            </div>

            <div v-if="msg.bargainData.status === 'negotiating'" class="im-msg__bargain-actions">
              <ZhButton type="primary" size="sm" block>
                接受 ¥{{ msg.bargainData.currentOffer.toLocaleString() }}
              </ZhButton>
              <ZhButton type="outline" size="sm" block>继续议价</ZhButton>
            </div>
            <div v-else-if="msg.bargainData.status === 'agreed'" class="im-msg__bargain-actions">
              <ZhButton type="primary" size="sm" block @click="onPay">
                <component :is="icon.Cart" /> 立即下单 ¥{{ msg.bargainData.finalPrice?.toLocaleString() }}
              </ZhButton>
            </div>
          </div>

          <!-- 普通消息 -->
          <ZhBubble v-else :is-me="msg.senderId === currentUserId" :type="msg.type as any">
            {{ msg.content }}
          </ZhBubble>
        </div>

        <div v-if="typing" class="im-msg is-them">
          <div class="im-msg__typing">
            <span></span><span></span><span></span>
          </div>
        </div>
      </div>

      <!-- 快捷回复 -->
      <div class="im-page__quick">
        <div
          v-for="q in quickReplies"
          :key="q"
          class="im-page__quick-item"
          @click="sendText(q)"
        >
          {{ q }}
        </div>
      </div>

      <!-- 输入区 -->
      <div class="im-page__input">
        <button class="im-page__icon-btn"><component :is="icon.Smile" /></button>
        <button class="im-page__icon-btn"><component :is="icon.Image" /></button>
        <input
          v-model="inputText"
          placeholder="输入消息..."
          @keydown.enter="sendText(inputText)"
        />
        <ZhButton type="primary" size="sm" @click="sendText(inputText)">
          <component :is="icon.Send" />
          发送
        </ZhButton>
      </div>
    </main>

    <main class="im-page__center im-page__center--empty" v-else>
      <div class="im-page__empty">
        <div class="im-page__empty-icon"><component :is="icon.Message" /></div>
        <h3>{{ route.params.id ? '会话不存在' : '选择一个会话开始聊天' }}</h3>
        <p>智换 AI Agent 24h 帮你议价,不用担心被已读不回</p>
        <div class="im-page__empty-actions">
          <ZhButton type="primary" @click="$router.push('/')">
            <component :is="icon.Home" /> 去首页逛逛
          </ZhButton>
          <ZhButton type="outline" v-if="route.params.id" @click="backToList()">
            <component :is="icon.ArrowLeft" /> 返回消息列表
          </ZhButton>
        </div>
        <div class="im-page__empty-features">
          <div class="im-page__empty-feature">
            <div class="im-page__empty-feature-num">01</div>
            <h4>24h 智能议价</h4>
            <p>AI Agent 全程在线,深夜也能砍价</p>
          </div>
          <div class="im-page__empty-feature">
            <div class="im-page__empty-feature-num">02</div>
            <h4>担保交易</h4>
            <p>先收货后付款,平台全额担保</p>
          </div>
          <div class="im-page__empty-feature">
            <div class="im-page__empty-feature-num">03</div>
            <h4>AI 验真</h4>
            <p>商品发布前 AI 风险审核,违规率&lt;0.1%</p>
          </div>
        </div>
      </div>
    </main>

    <!-- Right: Product Info -->
    <aside v-if="rightPanelOpen && activeConv" class="im-page__right">
      <div class="im-page__right-head">
        <h3>交易详情</h3>
        <button class="im-page__icon-btn" @click="rightPanelOpen = false">
          <component :is="icon.Close" />
        </button>
      </div>
      <div class="im-page__right-body">
        <div class="im-page__right-product">
          <ZhImage :src="activeConv.productImage!" :width="280" :height="280" />
          <div class="im-page__right-product-title">{{ activeConv.productTitle }}</div>
          <ZhPrice :price="activeConv.productPrice!" size="lg" />
        </div>

        <div class="im-page__right-section">
          <h4>交易进度</h4>
          <div class="im-page__timeline">
            <div class="im-page__timeline-item is-done">
              <div class="im-page__timeline-dot"></div>
              <div>
                <div class="im-page__timeline-title">已下单</div>
                <div class="im-page__timeline-time">09-14 10:30</div>
              </div>
            </div>
            <div class="im-page__timeline-item is-done">
              <div class="im-page__timeline-dot"></div>
              <div>
                <div class="im-page__timeline-title">已支付(资金担保中)</div>
                <div class="im-page__timeline-time">09-14 10:35</div>
              </div>
            </div>
            <div class="im-page__timeline-item">
              <div class="im-page__timeline-dot"></div>
              <div>
                <div class="im-page__timeline-title">待发货</div>
              </div>
            </div>
            <div class="im-page__timeline-item">
              <div class="im-page__timeline-dot"></div>
              <div>
                <div class="im-page__timeline-title">待收货</div>
              </div>
            </div>
            <div class="im-page__timeline-item">
              <div class="im-page__timeline-dot"></div>
              <div>
                <div class="im-page__timeline-title">交易完成</div>
              </div>
            </div>
          </div>
        </div>

        <div class="im-page__right-section">
          <h4>买家保障</h4>
          <div class="im-page__guarantee">
            <div><component :is="icon.Shield" />担保交易</div>
            <div><component :is="icon.Check" />7天无理由</div>
            <div><component :is="icon.Sparkle" />AI 验真</div>
          </div>
        </div>

        <div class="im-page__right-section">
          <h4>卖家信息</h4>
          <div class="im-page__seller">
            <ZhAvatar :src="activeConv.targetUser.avatar" :name="activeConv.targetUser.nickname" size="md" :verified="true" />
            <div>
              <div class="im-page__seller-name">{{ activeConv.targetUser.nickname }}</div>
              <div class="im-page__seller-credit">信用分 {{ activeConv.targetUser.creditScore }}</div>
            </div>
          </div>
        </div>
      </div>
    </aside>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, nextTick, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useImStore } from '@/stores/im'
import { useUserStore } from '@/stores/user'
import icons from '@/design/icons'
import ZhButton from '@/design/components/ZhButton.vue'
import ZhPrice from '@/design/components/ZhPrice.vue'
import ZhImage from '@/design/components/ZhImage.vue'
import ZhAvatar from '@/design/components/ZhAvatar.vue'
import ZhBubble from '@/design/components/ZhBubble.vue'

const icon = icons
const imStore = useImStore()
const userStore = useUserStore()
const currentUserId = 1
const activeId = ref('')
const rightPanelOpen = ref(true)
const inputText = ref('')
const typing = ref(false)
const loading = ref(true)
const messageList = ref<HTMLElement | null>(null)

const quickReplies = ['在吗?', '可以小刀吗?', '包邮吗?', '什么时候发货?']

const activeConv = computed(() => imStore.conversations.find(c => c.id === activeId.value))
const messages = computed(() => imStore.messages[activeId.value] || [])

function roundLabel(by: string) {
  return { buyer: '买家出价', seller: '卖家出价', ai: 'AI 建议' }[by] || by
}

function progressLeft(d: any) {
  const p = (d.currentOffer - d.targetPrice) / (d.initialPrice - d.targetPrice) * 100
  return Math.max(0, Math.min(100, p))
}

async function loadActive() {
  if (!activeId.value) return
  await imStore.loadMessages(activeId.value)
  await nextTick()
  if (messageList.value) {
    messageList.value.scrollTop = messageList.value.scrollHeight
  }
}

async function sendText(text: string) {
  if (!text?.trim()) return
  await imStore.sendMessage(text)
  inputText.value = ''
  await nextTick()
  if (messageList.value) {
    messageList.value.scrollTop = messageList.value.scrollHeight
  }
  // 模拟对方打字
  if (Math.random() > 0.5) {
    typing.value = true
    setTimeout(() => {
      typing.value = false
    }, 2000)
  }
}

function onPay() {
  alert('支付 - 进入担保交易流程 (Phase 2)')
}

watch(activeId, loadActive)
const route = useRoute()
const router = useRouter()

function openChat(id: string) {
  activeId.value = id
  router.push('/im/chat/' + id)
}

function backToList() {
  activeId.value = ''
  router.push('/im')
}

watch(() => route.params.id, (id) => {
  if (id) activeId.value = String(id)
  else if (imStore.conversations.length > 0 && !activeId.value) {
    // 列表页:不自动选中
  }
}, { immediate: true })

watch(activeId, (id) => {
  if (id) loadActive()
})

onMounted(async () => {
  try {
    await imStore.loadConversations()
    // 如果是 /im/chat/:id,从 route 取 activeId
    if (route.params.id) {
      activeId.value = String(route.params.id)
    }
    await loadActive()
  } finally {
    loading.value = false
  }
})
</script>

<style lang="scss" scoped>
.im-page {
  display: grid;
  grid-template-columns: 280px 1fr 360px;
  height: 100vh;
  background: var(--zh-color-bg);
  overflow: hidden;

  .im-page__left {
    background: white;
    border-right: 1px solid var(--zh-color-border-light);
    display: flex;
    flex-direction: column;
  }
  .im-page__left-head {
    display: flex;
    align-items: center;
    padding: 20px;
    h2 { font-size: var(--zh-font-size-xl); font-weight: 700; flex: 1; margin-left: 12px; }
  }
  .im-page__back {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 36px;
    height: 36px;
    border-radius: var(--zh-radius-md);
    color: var(--zh-color-text-secondary);
    transition: all var(--zh-duration-base) var(--zh-easing-standard);
    svg { width: 18px; height: 18px; }
    &:hover {
      background: var(--zh-color-bg);
      color: var(--zh-color-primary);
    }
  }
  .im-page__loading, &__empty {
    padding: 40px 20px;
    text-align: center;
    color: var(--zh-color-text-tertiary);
    font-size: var(--zh-font-size-sm);
    svg { width: 48px; height: 48px; color: var(--zh-color-border); margin-bottom: 12px; }
  }
  .im-page__empty-link {
    display: inline-block;
    margin-top: 12px;
    color: var(--zh-color-primary);
    font-weight: 500;
    text-decoration: none;
    &:hover { text-decoration: underline; }
  }
  .im-page__search {
    margin: 0 16px 12px;
    padding: 0 12px;
    background: var(--zh-color-bg);
    border-radius: var(--zh-radius-full);
    display: flex;
    align-items: center;
    gap: 8px;
    height: 36px;
    svg { width: 14px; height: 14px; color: var(--zh-color-text-tertiary); }
    input {
      flex: 1;
      background: transparent;
      border: none;
      outline: none;
      font-size: var(--zh-font-size-sm);
    }
  }
  .im-page__list {
    flex: 1;
    overflow-y: auto;
  }
  .im-page__item {
    display: flex;
    gap: 12px;
    padding: 12px 16px;
    cursor: pointer;
    transition: background var(--zh-duration-base) var(--zh-easing-standard);
    border-left: 3px solid transparent;
    &:hover { background: var(--zh-color-bg); }
    &.is-active {
      background: var(--zh-gradient-primary-soft);
      border-left-color: var(--zh-color-primary);
    }
  }
  .im-page__item-content { flex: 1; min-width: 0; }
  .im-page__item-row {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 4px;
  }
  .im-page__item-name { font-weight: 600; font-size: var(--zh-font-size-sm); }
  .im-page__item-time { font-size: var(--zh-font-size-xs); color: var(--zh-color-text-tertiary); }
  .im-page__item-msg {
    display: flex;
    align-items: center;
    gap: 4px;
    color: var(--zh-color-text-tertiary);
    font-size: var(--zh-font-size-xs);
  }
  .im-page__item-tag { font-size: 12px; }
  .im-page__item-text {
    flex: 1;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
  .im-page__item-badge {
    flex-shrink: 0;
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

  .im-page__center {
    display: flex;
    flex-direction: column;
    background: var(--zh-color-bg);
    &--empty {
      align-items: center;
      justify-content: center;
    }
  }
  .im-page__empty {
    text-align: center;
    color: var(--zh-color-text-tertiary);
    h3 { color: var(--zh-color-text); font-size: var(--zh-font-size-lg); font-weight: 600; margin-bottom: 8px; margin-top: 24px; }
  }
  .im-page__empty-icon {
    width: 96px;
    height: 96px;
    margin: 0 auto;
    background: var(--zh-gradient-primary-soft);
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    color: var(--zh-color-primary);
    svg { width: 40px; height: 40px; }
  }

  .im-page__chat-head {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 16px 24px;
    background: white;
    border-bottom: 1px solid var(--zh-color-border-light);
  }
  .im-page__chat-head-info {
    display: flex;
    align-items: center;
    gap: 10px;
  }
  .im-page__chat-head-name { font-weight: 600; font-size: var(--zh-font-size-base); }
  .im-page__chat-head-status {
    display: flex;
    align-items: center;
    gap: 4px;
    font-size: var(--zh-font-size-xs);
    color: var(--zh-color-success);
    .online-dot {
      width: 6px;
      height: 6px;
      background: var(--zh-color-success);
      border-radius: 50%;
      box-shadow: 0 0 0 3px rgba(16, 185, 129, 0.2);
    }
  }
  .im-page__chat-head-actions { display: flex; gap: 4px; }

  .im-page__product-header {
    display: flex;
    gap: 12px;
    align-items: center;
    padding: 12px 16px;
    background: var(--zh-gradient-primary-soft);
    border-radius: var(--zh-radius-md);
    margin-bottom: 12px;
  }
  .im-page__product-header-info { flex: 1; min-width: 0; }
  .im-page__product-header-title {
    font-size: var(--zh-font-size-sm);
    font-weight: 500;
    margin-bottom: 4px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
  .im-page__product-header-btn {
    padding: 6px 12px;
    background: white;
    border: 1px solid var(--zh-color-primary);
    color: var(--zh-color-primary);
    border-radius: var(--zh-radius-sm);
    font-size: var(--zh-font-size-xs);
    cursor: pointer;
    transition: all var(--zh-duration-base) var(--zh-easing-standard);
    &:hover { background: var(--zh-color-primary); color: white; }
  }

  .im-page__messages {
    flex: 1;
    padding: 20px 32px;
    overflow-y: auto;
    display: flex;
    flex-direction: column;
    gap: 12px;
  }

  .im-page__quick {
    display: flex;
    gap: 8px;
    padding: 8px 24px;
    overflow-x: auto;
    scrollbar-width: none;
    &::-webkit-scrollbar { display: none; }
  }
  .im-page__quick-item {
    flex-shrink: 0;
    padding: 6px 12px;
    background: white;
    border: 1px solid var(--zh-color-border);
    border-radius: var(--zh-radius-full);
    font-size: var(--zh-font-size-xs);
    cursor: pointer;
    transition: all var(--zh-duration-base) var(--zh-easing-standard);
    &:hover { background: var(--zh-color-primary); color: white; border-color: var(--zh-color-primary); }
  }

  .im-page__input {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 12px 24px;
    background: white;
    border-top: 1px solid var(--zh-color-border-light);
    input {
      flex: 1;
      height: 36px;
      padding: 0 12px;
      background: var(--zh-color-bg);
      border: 1px solid var(--zh-color-border);
      border-radius: var(--zh-radius-sm);
      font-size: var(--zh-font-size-sm);
      outline: none;
      transition: all var(--zh-duration-base) var(--zh-easing-standard);
      &:focus { background: white; border-color: var(--zh-color-primary); }
    }
  }

  .im-page__icon-btn {
    width: 36px;
    height: 36px;
    border-radius: var(--zh-radius-sm);
    color: var(--zh-color-text-secondary);
    transition: all var(--zh-duration-base) var(--zh-easing-standard);
    display: inline-flex;
    align-items: center;
    justify-content: center;
    svg { width: 18px; height: 18px; }
    &:hover { background: var(--zh-color-bg); color: var(--zh-color-text); }
  }

  .im-page__right {
    background: white;
    border-left: 1px solid var(--zh-color-border-light);
    display: flex;
    flex-direction: column;
  }
  .im-page__right-head {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 20px;
    border-bottom: 1px solid var(--zh-color-border-light);
    h3 { font-size: var(--zh-font-size-lg); font-weight: 600; }
  }
  .im-page__right-body { flex: 1; padding: 20px; overflow-y: auto; }
  .im-page__right-product {
    text-align: center;
    padding-bottom: 20px;
    border-bottom: 1px solid var(--zh-color-border-light);
    margin-bottom: 20px;
    &-title {
      font-size: var(--zh-font-size-sm);
      font-weight: 500;
      margin: 12px 0 8px;
      line-height: 1.5;
    }
  }
  .im-page__right-section {
    margin-bottom: 24px;
    h4 {
      font-size: var(--zh-font-size-base);
      font-weight: 600;
      margin-bottom: 12px;
    }
  }
  .im-page__timeline {
    display: flex;
    flex-direction: column;
    gap: 16px;
    padding-left: 8px;
    position: relative;
    &::before {
      content: '';
      position: absolute;
      left: 5px;
      top: 8px;
      bottom: 8px;
      width: 2px;
      background: var(--zh-color-border-light);
    }
  }
  .im-page__timeline-item {
    display: flex;
    gap: 12px;
    align-items: flex-start;
    position: relative;
    &.is-done &-dot { background: var(--zh-color-success); }
    &.is-done &-title { color: var(--zh-color-text); font-weight: 500; }
  }
  .im-page__timeline-dot {
    width: 12px;
    height: 12px;
    border-radius: 50%;
    background: var(--zh-color-border);
    border: 2px solid white;
    margin-top: 4px;
    flex-shrink: 0;
    z-index: 1;
  }
  .im-page__timeline-title { font-size: var(--zh-font-size-sm); color: var(--zh-color-text-tertiary); }
  .im-page__timeline-time { font-size: var(--zh-font-size-xs); color: var(--zh-color-text-tertiary); margin-top: 2px; }

  .im-page__guarantee {
    display: flex;
    flex-direction: column;
    gap: 8px;
    div {
      display: inline-flex;
      align-items: center;
      gap: 8px;
      font-size: var(--zh-font-size-sm);
      color: var(--zh-color-text-secondary);
    }
    svg { width: 16px; height: 16px; color: var(--zh-color-primary); }
  }

  .im-page__seller {
    display: flex;
    align-items: center;
    gap: 12px;
  }
  .im-page__seller-name { font-size: var(--zh-font-size-sm); font-weight: 600; }
  .im-page__seller-credit {
    font-size: var(--zh-font-size-xs);
    color: var(--zh-color-primary);
    margin-top: 4px;
  }
}

/* 消息样式 */
.im-msg {
  display: flex;
  flex-direction: column;
  &.is-me { align-items: flex-end; }
  &.is-them { align-items: flex-start; }
  &.is-system { align-items: center; }

  .im-page__system {
    padding: 4px 12px;
    background: var(--zh-color-border-light);
    color: var(--zh-color-text-tertiary);
    font-size: var(--zh-font-size-xs);
    border-radius: var(--zh-radius-full);
  }

  .im-page__typing {
    padding: 12px 16px;
    background: white;
    border-radius: 18px;
    display: flex;
    gap: 4px;
    span {
      width: 6px;
      height: 6px;
      background: var(--zh-color-text-tertiary);
      border-radius: 50%;
      animation: zh-bounce 1.4s infinite;
      &:nth-child(2) { animation-delay: 0.2s; }
      &:nth-child(3) { animation-delay: 0.4s; }
    }
  }
}

@keyframes zh-bounce {
  0%, 60%, 100% { transform: translateY(0); }
  30% { transform: translateY(-6px); }
}

/* AI 议价卡 */
.im-msg__bargain {
  max-width: 480px;
  background: white;
  border: 1.5px solid transparent;
  border-image: var(--zh-gradient-primary) 1;
  border-radius: var(--zh-radius-lg);
  padding: 16px;
  box-shadow: var(--zh-shadow-base);
  position: relative;
  background-image: linear-gradient(white, white), var(--zh-gradient-primary);
  background-clip: padding-box, border-box;
  background-origin: padding-box, border-box;

  &-head {
    display: flex;
    align-items: center;
    gap: 10px;
    margin-bottom: 12px;
  }
  &-icon {
    width: 36px;
    height: 36px;
    background: var(--zh-gradient-primary);
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    color: white;
    svg { width: 16px; height: 16px; }
  }
  &-title { font-weight: 700; font-size: var(--zh-font-size-base); }
  &-sub { font-size: var(--zh-font-size-xs); color: var(--zh-color-text-tertiary); }
  &-status {
    margin-left: auto;
    display: inline-flex;
    align-items: center;
    gap: 4px;
    padding: 4px 8px;
    border-radius: var(--zh-radius-full);
    font-size: var(--zh-font-size-xs);
    font-weight: 600;
    &.is-agreed { background: var(--zh-color-success-light); color: var(--zh-color-success); }
  }

  &-chart {
    position: relative;
    height: 30px;
    margin: 12px 0;
  }
  &-track {
    position: absolute;
    top: 14px;
    left: 0;
    right: 0;
    height: 2px;
    background: var(--zh-color-border);
  }
  &-progress {
    position: absolute;
    top: 14px;
    left: 0;
    height: 2px;
    background: var(--zh-gradient-primary);
    width: 50%;
    transition: width var(--zh-duration-slower) var(--zh-easing-decelerate);
  }
  &-point {
    position: absolute;
    top: 9px;
    width: 12px;
    height: 12px;
    border-radius: 50%;
    background: white;
    border: 2px solid var(--zh-color-primary);
    transform: translateX(-50%);
    &--buyer { border-color: var(--zh-color-info); background: var(--zh-color-info); }
    &--seller { border-color: var(--zh-color-warning); background: var(--zh-color-warning); }
    &--ai {
      background: white;
      border-color: var(--zh-color-primary);
      box-shadow: 0 0 0 3px rgba(124, 58, 237, 0.2);
    }
  }

  &-rounds {
    display: flex;
    flex-direction: column;
    gap: 8px;
    margin: 12px 0;
    max-height: 240px;
    overflow-y: auto;
  }
  &-round {
    background: var(--zh-color-bg);
    border-radius: var(--zh-radius-sm);
    padding: 8px 12px;
    &.is-buyer { border-left: 3px solid var(--zh-color-info); }
    &.is-seller { border-left: 3px solid var(--zh-color-warning); }
    &.is-ai {
      border-left: 3px solid var(--zh-color-primary);
      background: var(--zh-gradient-primary-soft);
    }
  }
  &-round-head {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 2px;
  }
  &-round-tag {
    font-size: 10px;
    font-weight: 600;
    padding: 1px 6px;
    background: white;
    border-radius: var(--zh-radius-sm);
  }
  &-round-price { font-size: var(--zh-font-size-sm); font-weight: 700; color: var(--zh-color-text); }
  &-round-msg { font-size: var(--zh-font-size-xs); color: var(--zh-color-text-secondary); }

  &-actions {
    display: flex;
    flex-direction: column;
    gap: 8px;
    margin-top: 12px;
    padding-top: 12px;
    border-top: 1px solid var(--zh-color-border-light);
  }
}

.im-page__center--empty {
    align-items: center;
    justify-content: center;
    padding: 40px;
  }
  .im-page__empty.im-page-empty-state {
    text-align: center;
    max-width: 800px;
    width: 100%;
  }
  .im-page__empty .im-page__empty-icon {
    width: 80px;
    height: 80px;
    margin: 0 auto 24px;
    background: var(--zh-gradient-primary-soft);
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    color: var(--zh-color-primary);
  }
  .im-page__empty .im-page__empty-icon svg { width: 36px; height: 36px; }
  .im-page__empty h3 { font-size: 24px; font-weight: 700; margin-bottom: 12px; }
  .im-page__empty > p { color: var(--zh-color-text-tertiary); font-size: var(--zh-font-size-base); margin-bottom: 24px; }
  .im-page__empty-actions {
    display: flex;
    gap: 12px;
    justify-content: center;
    margin-bottom: 48px;
  }
  .im-page__empty-features {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 24px;
    margin-top: 32px;
    padding-top: 32px;
    border-top: 1px solid var(--zh-color-border-light);
  }
  .im-page__empty-feature {
    text-align: left;
  }
  .im-page__empty-feature-num {
    display: inline-block;
    font-size: 32px;
    font-weight: 800;
    background: var(--zh-gradient-primary);
    -webkit-background-clip: text;
    background-clip: text;
    -webkit-text-fill-color: transparent;
    font-family: 'DIN Alternate', monospace;
    line-height: 1;
    margin-bottom: 12px;
  }
  .im-page__empty-feature h4 { font-size: var(--zh-font-size-base); font-weight: 600; margin-bottom: 6px; }
  .im-page__empty-feature p { color: var(--zh-color-text-tertiary); font-size: var(--zh-font-size-sm); line-height: 1.5; text-align: left; }

</style>
