<template>
  <div class="publish">
    <div class="publish__inner">
      <div class="publish__header">
        <h1 class="publish__title">
          <component :is="icon.Sparkle" />
          AI 智能发布
        </h1>
        <p class="publish__subtitle">拍照即可一键上架,AI 帮你识别、估价、写文案</p>
        <div class="publish__stepper">
          <ZhStepper :steps="['上传图片', 'AI 智能识别', '编辑发布', '提交审核']" :current="currentStep" :done="submitDone" />
        </div>
      </div>

      <div class="publish__body">
        <!-- Step 1: Upload -->
        <section v-show="currentStep === 0" class="publish-step zh-anim-fade-up">
          <div class="publish-upload" @click="triggerFile" @dragover.prevent @drop.prevent="onDrop">
            <input ref="fileInput" type="file" multiple accept="image/*" @change="onFileChange" hidden />
            <div v-if="!images.length" class="publish-upload__empty">
              <div class="publish-upload__icon">
                <div class="publish-upload__icon-ring"></div>
                <div class="publish-upload__icon-ring publish-upload__icon-ring--2"></div>
                <component :is="icon.Camera" />
              </div>
              <h3>拖拽图片到此处,或点击上传</h3>
              <p>支持 JPG/PNG,最多 9 张,单张不超过 10MB</p>
              <div class="publish-upload__btns">
                <ZhButton type="primary">
                  <component :is="icon.Camera" /> 拍照上传
                </ZhButton>
                <ZhButton type="outline">
                  <component :is="icon.Image" /> 从相册选择
                </ZhButton>
              </div>
              <div class="publish-upload__sample">
                <span>没商品可拍?试试示例:</span>
                <button @click.stop="useSample">使用示例图片</button>
              </div>
            </div>
            <div v-else class="publish-upload__preview">
              <div
                v-for="(img, i) in images"
                :key="i"
                class="publish-upload__item zh-anim-scale-in"
              >
                <img :src="img" :alt="`图${i + 1}`" />
                <button class="publish-upload__remove" @click.stop="removeImage(i)">
                  <component :is="icon.Close" />
                </button>
              </div>
              <div v-if="images.length < 9" class="publish-upload__add" @click="triggerFile">
                <component :is="icon.Plus" />
              </div>
            </div>
          </div>
        </section>

        <!-- Step 2: AI Recognition (核心高保真动画) -->
        <section v-show="currentStep === 1" class="publish-step zh-anim-fade-up">
          <div class="ai-stage">
            <div class="ai-stage__center">
              <div class="ai-stage__image">
                <img :src="images[0]" alt="" />
                <div class="ai-stage__scan" v-if="anyRunning"></div>
                <div class="ai-stage__nodes">
                  <div
                    v-for="(step, i) in aiSteps"
                    :key="step.name"
                    :class="['ai-stage__node', 'ai-stage__node--' + positionFor(i), step.status]"
                    :style="{ '--delay': i * 0.3 + 's' }"
                  >
                    <div class="ai-stage__node-inner">
                      <span class="ai-stage__node-icon">
                        <component :is="iconFor(step.name)" />
                      </span>
                      <span class="ai-stage__node-label">{{ step.label }}</span>
                    </div>
                    <div v-if="step.status === 'running'" class="ai-stage__node-pulse"></div>
                    <div v-if="step.status === 'success'" class="ai-stage__node-check">
                      <component :is="icon.Check" />
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <div class="ai-stage__progress">
              <div class="ai-stage__progress-bar" :style="{ width: progress + '%' }"></div>
            </div>
            <div class="ai-stage__progress-text">
              <span v-if="anyRunning">{{ currentLabel }}中...</span>
              <span v-else-if="allDone">✓ AI 识别完成,已为你生成完整商品信息</span>
              <span v-else>准备开始 AI 识别</span>
              <span class="ai-stage__progress-pct">{{ Math.round(progress) }}%</span>
            </div>

            <div class="ai-result" v-if="allDone">
              <!-- 标题 -->
              <div class="ai-result__item zh-anim-fade-up" :style="{ animationDelay: '0.2s' }">
                <div class="ai-result__label">
                  <component :is="icon.Sparkle" />
                  AI 生成的标题
                </div>
                <div class="ai-result__value typing">{{ draft.title }}<span class="caret">|</span></div>
              </div>

              <!-- 识别结果 -->
              <div class="ai-result__row">
                <div class="ai-result__item ai-result__item--half zh-anim-fade-up" :style="{ animationDelay: '0.4s' }">
                  <div class="ai-result__label">
                    <component :is="icon.Sparkle" />
                    识别结果
                  </div>
                  <div class="ai-result__chips">
                    <ZhTag type="primary" size="md">{{ draft.brand }}</ZhTag>
                    <ZhTag type="info" size="md">{{ draft.model }}</ZhTag>
                    <ZhTag type="success" size="md">{{ draft.color }}</ZhTag>
                    <ZhTag type="warning" size="md">成色 {{ draft.conditionScore }}/10</ZhTag>
                  </div>
                </div>

                <div class="ai-result__item ai-result__item--half zh-anim-fade-up" :style="{ animationDelay: '0.5s' }">
                  <div class="ai-result__label">
                    <component :is="icon.Sparkle" />
                    类目预测
                  </div>
                  <div class="ai-result__category">
                    <div
                      v-for="(c, i) in draft.categoryCandidates"
                      :key="i"
                      :class="['ai-result__category-card', i === 0 && 'is-top']"
                    >
                      <div class="ai-result__category-name">{{ c.name }}</div>
                      <div class="ai-result__category-conf zh-mono">{{ (c.confidence * 100).toFixed(0) }}%</div>
                    </div>
                  </div>
                </div>
              </div>

              <!-- 价格建议 -->
              <div class="ai-result__item zh-anim-fade-up" :style="{ animationDelay: '0.7s' }">
                <div class="ai-result__label">
                  <component :is="icon.Sparkle" />
                  AI 价格建议
                </div>
                <div class="ai-result__price">
                  <div class="ai-result__price-range zh-mono">¥{{ draft.priceLow }} - {{ draft.priceHigh }}</div>
                  <div class="ai-result__price-mid">
                    <span class="ai-result__price-badge">AI 推荐</span>
                    <span class="ai-result__price-value zh-mono">¥{{ draft.suggestedPrice?.toLocaleString() }}</span>
                  </div>
                </div>
                <div class="ai-result__reason">💡 {{ draft.priceReason }}</div>
              </div>

              <!-- 描述 -->
              <div class="ai-result__item zh-anim-fade-up" :style="{ animationDelay: '0.9s' }">
                <div class="ai-result__label">
                  <component :is="icon.Sparkle" />
                  AI 文案
                </div>
                <div class="ai-result__desc">
                  <p v-for="(line, i) in (draft.description || '').split('\n').filter(Boolean)" :key="i">{{ line }}</p>
                </div>
              </div>

              <!-- 审核结果 -->
              <div class="ai-result__item zh-anim-fade-up" :style="{ animationDelay: '1.1s' }">
                <div class="ai-result__label">
                  <component :is="icon.Sparkle" />
                  风险审核
                </div>
                <div :class="['ai-result__audit', 'ai-result__audit--' + draft.aiAuditStatus]">
                  <div class="ai-result__audit-icon">
                    <component :is="icon.Check" v-if="draft.aiAuditStatus === 'pass'" />
                    <component :is="icon.Close" v-else />
                  </div>
                  <div>
                    <div class="ai-result__audit-title">
                      <template v-if="draft.aiAuditStatus === 'pass'">审核通过</template>
                      <template v-else-if="draft.aiAuditStatus === 'warn'">需注意</template>
                      <template v-else>审核拒绝</template>
                    </div>
                    <div class="ai-result__audit-reason">{{ draft.auditReason }}</div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </section>

        <!-- Step 3: Edit -->
        <section v-show="currentStep === 2" class="publish-step zh-anim-fade-up">
          <div class="publish-edit">
            <div class="publish-edit__left">
              <div class="publish-edit__image">
                <img :src="images[0]" alt="" />
                <div class="publish-edit__image-overlay">
                  <component :is="icon.Image" />
                  <span>{{ images.length }} 张图片</span>
                </div>
              </div>
            </div>
            <div class="publish-edit__right">
              <div class="publish-edit__field">
                <label>标题</label>
                <input v-model="draft.title" placeholder="给你的商品起个好标题" />
                <span class="publish-edit__ai-tip">
                  <component :is="icon.Sparkle" /> AI 生成,可编辑
                </span>
              </div>
              <div class="publish-edit__field">
                <label>详细描述</label>
                <textarea v-model="draft.description" rows="8" placeholder="详细描述商品的使用情况、购买时间、瑕疵等"></textarea>
              </div>
              <div class="publish-edit__row">
                <div class="publish-edit__field">
                  <label>类目</label>
                  <select v-model="draft.categoryName">
                    <option v-for="c in categories" :key="c.id">{{ c.name }}</option>
                  </select>
                </div>
                <div class="publish-edit__field">
                  <label>成色</label>
                  <select v-model.number="draft.conditionScore">
                    <option :value="10">全新</option>
                    <option :value="9">几乎全新</option>
                    <option :value="7">轻微使用</option>
                    <option :value="5">明显使用</option>
                  </select>
                </div>
              </div>
              <div class="publish-edit__row">
                <div class="publish-edit__field">
                  <label>售价 (元)</label>
                  <input v-model.number="draft.suggestedPrice" type="number" />
                </div>
                <div class="publish-edit__field">
                  <label>原价 (元)</label>
                  <input type="number" placeholder="选填" />
                </div>
              </div>
              <div class="publish-edit__field">
                <label>所在地</label>
                <input value="北京 朝阳区" />
              </div>
              <div class="publish-edit__field">
                <label>交易方式</label>
                <div class="publish-edit__checks">
                  <label class="publish-edit__check is-checked">
                    <input type="checkbox" checked /> 担保交易
                  </label>
                  <label class="publish-edit__check is-checked">
                    <input type="checkbox" checked /> 支持包邮
                  </label>
                  <label class="publish-edit__check">
                    <input type="checkbox" /> 支持面交
                  </label>
                </div>
              </div>
            </div>
          </div>
        </section>

        <!-- Step 4: Submit -->
        <section v-show="currentStep === 3" class="publish-step zh-anim-fade-up">
          <div class="publish-submit">
            <div class="publish-submit__check" :class="{ 'is-loading': submitting, 'is-done': submitDone }">
              <div v-if="submitting" class="publish-submit__spinner"></div>
              <div v-else-if="submitDone" class="publish-submit__icon">
                <component :is="icon.Check" />
              </div>
              <div v-else class="publish-submit__icon">📦</div>
            </div>
            <h2 v-if="!submitting && !submitDone">即将提交审核</h2>
            <h2 v-else-if="submitting">提交中...</h2>
            <h2 v-else>发布成功!</h2>
            <p v-if="!submitting && !submitDone">AI 审核 + 人工抽检,通常 5-10 分钟完成</p>
            <p v-else-if="submitting">正在将商品数据同步到搜索、推荐、Feed 服务</p>
            <p v-else>
              <strong>AI 已为你节省约 8 分钟</strong> -
              传统上架需要手动写标题、描述、定价格,智换 AI 帮你一步搞定
            </p>
            <div v-if="submitDone" class="publish-submit__actions">
              <ZhButton type="outline" @click="$router.push('/user')">
                <component :is="icon.User" /> 查看我的商品
              </ZhButton>
              <ZhButton type="outline" @click="$router.push('/')">回到首页</ZhButton>
              <ZhButton type="primary" @click="$router.push('/product/1')">
                <component :is="icon.ArrowRight" /> 查看商品详情
              </ZhButton>
            </div>
          </div>
        </section>
      </div>

      <!-- Footer Actions -->
      <div v-if="currentStep < 3" class="publish__footer">
        <ZhButton v-if="currentStep > 0" type="outline" @click="currentStep--">
          <component :is="icon.ArrowLeft" /> 上一步
        </ZhButton>
        <span v-else></span>
        <div class="publish__footer-right">
          <ZhButton v-if="currentStep === 1" type="text" @click="skipAnimation" :disabled="!anyRunning && !allDone">
            {{ anyRunning ? '跳过动画' : '已生成' }}
          </ZhButton>
          <ZhButton
            type="primary"
            size="lg"
            :disabled="!canNext"
            @click="onNext"
          >
            {{ currentStep === 0 ? '开始 AI 识别' : currentStep === 2 ? '提交发布' : '下一步' }}
            <component :is="icon.ArrowRight" />
          </ZhButton>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onUnmounted, watch } from 'vue'
import { aiApi } from '@/api/modules/ai.api'
import icons from '@/design/icons'
import ZhButton from '@/design/components/ZhButton.vue'
import ZhStepper from '@/design/components/ZhStepper.vue'
import ZhTag from '@/design/components/ZhTag.vue'

const icon = icons
const currentStep = ref(0)
const fileInput = ref<HTMLInputElement | null>(null)
const images = ref<string[]>([])
const draft = ref<any>({
  title: '',
  description: '',
  brand: '',
  model: '',
  color: '',
  conditionScore: 9,
  categoryName: '',
  categoryCandidates: [],
  suggestedPrice: 0,
  priceLow: 0,
  priceHigh: 0,
  priceReason: '',
  aiAuditStatus: 'pass',
  auditReason: '',
})
const submitting = ref(false)
const submitDone = ref(false)
const skipFlag = ref(false)

const categories = [
  { id: 1, name: '手机' },
  { id: 2, name: '电脑' },
  { id: 3, name: '数码配件' },
  { id: 4, name: '服饰' },
  { id: 5, name: '鞋包' },
  { id: 6, name: '美妆' },
  { id: 7, name: '家居' },
  { id: 8, name: '图书' },
  { id: 9, name: '潮玩' },
  { id: 10, name: '运动户外' },
]

const SAMPLE_IMAGE = 'https://images.unsplash.com/photo-1592750475338-74b7b21085ab?w=800&h=800&fit=crop'

const aiSteps = ref([
  { name: 'image', label: '图像识别', status: 'pending' as 'pending' | 'running' | 'success' | 'failed' },
  { name: 'category', label: '类目预测', status: 'pending' as 'pending' | 'running' | 'success' | 'failed' },
  { name: 'price', label: 'AI 估价', status: 'pending' as 'pending' | 'running' | 'success' | 'failed' },
  { name: 'description', label: '文案生成', status: 'pending' as 'pending' | 'running' | 'success' | 'failed' },
  { name: 'audit', label: '风险审核', status: 'pending' as 'pending' | 'running' | 'success' | 'failed' },
])
const anyRunning = computed(() => aiSteps.value.some(s => s.status === 'running'))
const allDone = computed(() => aiSteps.value.every(s => s.status === 'success'))
const currentLabel = computed(() => aiSteps.value.find(s => s.status === 'running')?.label || '')
const progress = computed(() => {
  const done = aiSteps.value.filter(s => s.status === 'success').length
  const running = aiSteps.value.find(s => s.status === 'running')
  return (done + (running ? 0.5 : 0)) / aiSteps.value.length * 100
})
const canNext = computed(() => {
  if (currentStep.value === 0) return images.value.length > 0
  if (currentStep.value === 1) return allDone.value
  if (currentStep.value === 2) return draft.value.title && draft.value.suggestedPrice > 0
  return true
})

function triggerFile() { fileInput.value?.click() }
function onFileChange(e: Event) {
  const files = (e.target as HTMLInputElement).files
  if (!files) return
  Array.from(files).forEach(f => {
    const reader = new FileReader()
    reader.onload = () => images.value.push(reader.result as string)
    reader.readAsDataURL(f)
  })
  if (images.value.length > 9) images.value = images.value.slice(0, 9)
}
function onDrop(e: DragEvent) {
  e.preventDefault()
  const files = e.dataTransfer?.files
  if (!files) return
  Array.from(files).forEach(f => {
    const reader = new FileReader()
    reader.onload = () => images.value.push(reader.result as string)
    reader.readAsDataURL(f)
  })
}
function removeImage(i: number) { images.value.splice(i, 1) }
function useSample() {
    images.value = [SAMPLE_IMAGE]
    if (currentStep.value === 0) {
      currentStep.value = 1
      runAI()
    }
  }

function positionFor(i: number) {
  return ['top-left', 'top-right', 'right', 'bottom-right', 'bottom-left'][i]
}

function iconFor(name: string) {
  const map: any = { image: icon.Image, category: icon.Category, price: icon.Sparkle, description: icon.Plus, audit: icon.Shield }
  return map[name] || icon.Sparkle
}

async function runAI() {
  if (skipFlag.value) {
    aiSteps.value.forEach(s => s.status = 'success')
    return
  }
  // 模拟分步骤动画
  for (let i = 0; i < aiSteps.value.length; i++) {
    aiSteps.value[i].status = 'running'
    await new Promise(r => setTimeout(r, 1200))
    aiSteps.value[i].status = 'success'
  }
  // 拉取真实结果
  const result = await aiApi.getPublishResult('mock')
  Object.assign(draft.value, result)
}

function skipAnimation() {
  if (anyRunning.value) {
    skipFlag.value = true
    aiSteps.value.forEach(s => { if (s.status === 'pending' || s.status === 'running') s.status = 'success' })
  }
}

async function onNext() {
  if (currentStep.value === 0) {
    currentStep.value = 1
    runAI()
  } else if (currentStep.value === 1) {
    currentStep.value = 2
  } else if (currentStep.value === 2) {
    currentStep.value = 3
    submitting.value = true
    setTimeout(() => {
      submitting.value = false
      submitDone.value = true
    }, 2000)
  }
}

watch(() => currentStep.value, () => window.scrollTo({ top: 0, behavior: 'smooth' }))

onUnmounted(() => { skipFlag.value = false })
</script>

<style lang="scss" scoped>
.publish {
  min-height: calc(100vh - var(--zh-header-height) - var(--zh-tabbar-height));
  padding: 24px 0 120px;
  &__inner {
    max-width: 1024px;
    margin: 0 auto;
    padding: 0 32px;
  }
  &__header {
    text-align: center;
    margin-bottom: 32px;
  }
  &__title {
    display: inline-flex;
    align-items: center;
    gap: 10px;
    font-size: 36px;
    font-weight: 800;
    background: var(--zh-gradient-primary);
    -webkit-background-clip: text;
    background-clip: text;
    -webkit-text-fill-color: transparent;
    margin-bottom: 8px;
    svg { width: 32px; height: 32px; -webkit-text-fill-color: var(--zh-color-primary); }
  }
  &__subtitle {
    color: var(--zh-color-text-tertiary);
    font-size: var(--zh-font-size-base);
    margin-bottom: 32px;
  }
  &__stepper {
    max-width: 720px;
    margin: 0 auto;
  }
  &__body {
    background: white;
    border-radius: var(--zh-radius-lg);
    padding: 40px;
    box-shadow: var(--zh-shadow-base);
    min-height: 480px;
  }
  &__footer {
    position: sticky;
    bottom: 16px;
    margin: 32px 0 0;
    padding: 16px 24px;
    background: rgba(255, 255, 255, 0.85);
    backdrop-filter: blur(20px) saturate(180%);
    border: 1px solid var(--zh-color-border-light);
    border-radius: var(--zh-radius-lg);
    box-shadow: var(--zh-shadow-md);
    z-index: 10;
    display: flex;
    justify-content: space-between;
    align-items: center;
    &-right { display: flex; gap: 12px; }
  }
}

.publish-upload {
  border: 2px dashed var(--zh-color-border);
  border-radius: var(--zh-radius-lg);
  padding: 60px 40px;
  cursor: pointer;
  transition: all var(--zh-duration-base) var(--zh-easing-standard);
  &:hover {
    border-color: var(--zh-color-primary);
    background: var(--zh-gradient-primary-soft);
  }
  &__empty {
    text-align: center;
    h3 { font-size: var(--zh-font-size-xl); font-weight: 600; margin-bottom: 8px; }
    p { color: var(--zh-color-text-tertiary); margin-bottom: 24px; }
  }
  &__icon {
    position: relative;
    width: 96px;
    height: 96px;
    margin: 0 auto 24px;
    background: var(--zh-gradient-primary-soft);
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    color: var(--zh-color-primary);
    svg { width: 40px; height: 40px; position: relative; z-index: 1; }
  }
  &__icon-ring {
    position: absolute;
    inset: -8px;
    border: 2px solid var(--zh-color-primary);
    border-radius: 50%;
    opacity: 0.4;
    animation: zh-pulse-ring 2s ease-out infinite;
    &--2 { animation-delay: 1s; }
  }
  &__btns {
    display: flex;
    gap: 12px;
    justify-content: center;
    margin-bottom: 24px;
  }
  &__sample {
    color: var(--zh-color-text-tertiary);
    font-size: var(--zh-font-size-sm);
    button {
      color: var(--zh-color-primary);
      text-decoration: underline;
      margin-left: 8px;
    }
  }
  &__preview {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(120px, 1fr));
    gap: 12px;
  }
  &__item {
    position: relative;
    aspect-ratio: 1;
    border-radius: var(--zh-radius-md);
    overflow: hidden;
    img { width: 100%; height: 100%; object-fit: cover; }
  }
  &__remove {
    position: absolute;
    top: 4px;
    right: 4px;
    width: 24px;
    height: 24px;
    background: rgba(0, 0, 0, 0.5);
    color: white;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    backdrop-filter: blur(4px);
    svg { width: 12px; height: 12px; }
  }
  &__add {
    aspect-ratio: 1;
    border: 2px dashed var(--zh-color-border);
    border-radius: var(--zh-radius-md);
    display: flex;
    align-items: center;
    justify-content: center;
    color: var(--zh-color-text-tertiary);
    transition: all var(--zh-duration-base) var(--zh-easing-standard);
    svg { width: 32px; height: 32px; }
    &:hover { border-color: var(--zh-color-primary); color: var(--zh-color-primary); }
  }
}

/* AI Stage */
.ai-stage {
  &__center {
    display: flex;
    justify-content: center;
    margin-bottom: 32px;
  }
  &__image {
    position: relative;
    width: 360px;
    height: 360px;
    border-radius: var(--zh-radius-lg);
    overflow: hidden;
    box-shadow: var(--zh-shadow-lg);
    img { width: 100%; height: 100%; object-fit: cover; }
  }
  &__scan {
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    height: 3px;
    background: linear-gradient(90deg, transparent, #7C3AED, #06B6D4, transparent);
    box-shadow: 0 0 20px rgba(124, 58, 237, 0.8);
    animation: zh-scan-y 2.4s ease-in-out infinite;
    z-index: 2;
  }
  &__nodes {
    position: absolute;
    inset: 0;
    z-index: 3;
  }
  &__node {
    position: absolute;
    width: 80px;
    height: 80px;
    transition: all var(--zh-duration-slow) var(--zh-easing-spring);
    transform: scale(0);

    &--top-left { top: -40px; left: -40px; }
    &--top-right { top: -40px; right: -40px; }
    &--right { top: 50%; right: -40px; transform: translateY(-50%) scale(0); }
    &--bottom-right { bottom: -40px; right: -40px; }
    &--bottom-left { bottom: -40px; left: -40px; }

    &.pending { opacity: 0; transform: scale(0); }
    &.running { opacity: 1; transform: scale(1); }
    &.success { opacity: 1; transform: scale(1); }
    &--right.running, &--right.success { transform: translateY(-50%) scale(1); }
  }
  &__node-inner {
    width: 100%;
    height: 100%;
    background: white;
    border-radius: 50%;
    box-shadow: var(--zh-shadow-md);
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 2px;
    border: 2px solid var(--zh-color-border-light);
    transition: all var(--zh-duration-base) var(--zh-easing-standard);
  }
  &__node.running &__node-inner {
    border-color: var(--zh-color-primary);
    box-shadow: 0 0 0 4px rgba(124, 58, 237, 0.15), var(--zh-shadow-md);
    animation: zh-breathe 1.6s ease-in-out infinite;
  }
  &__node.success &__node-inner {
    background: var(--zh-gradient-primary);
    border-color: transparent;
    color: white;
  }
  &__node-icon { font-size: 24px; }
  &__node-label {
    font-size: 10px;
    font-weight: 500;
    color: var(--zh-color-text-tertiary);
  }
  &__node.success &__node-label { color: white; }
  &__node-pulse {
    position: absolute;
    inset: -8px;
    border: 2px solid var(--zh-color-primary);
    border-radius: 50%;
    opacity: 0.6;
    animation: zh-pulse-ring 1.6s ease-out infinite;
  }
  &__node-check {
    position: absolute;
    top: -4px;
    right: -4px;
    width: 24px;
    height: 24px;
    background: var(--zh-color-success);
    color: white;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    svg { width: 12px; height: 12px; }
    border: 2px solid white;
  }

  &__progress {
    height: 4px;
    background: var(--zh-color-border-light);
    border-radius: var(--zh-radius-full);
    overflow: hidden;
    margin-bottom: 8px;
  }
  &__progress-bar {
    height: 100%;
    background: var(--zh-gradient-primary);
    border-radius: var(--zh-radius-full);
    transition: width var(--zh-duration-slow) var(--zh-easing-decelerate);
  }
  &__progress-text {
    display: flex;
    justify-content: space-between;
    font-size: var(--zh-font-size-sm);
    color: var(--zh-color-text-secondary);
    margin-bottom: 32px;
  }
  &__progress-pct {
    font-family: var(--zh-font-family-mono);
    font-weight: 700;
    color: var(--zh-color-primary);
  }
}

@keyframes zh-scan-y {
  0%, 100% { top: 0; }
  50% { top: calc(100% - 3px); }
}

/* AI Result */
.ai-result {
  &__item {
    background: var(--zh-color-bg);
    border-radius: var(--zh-radius-md);
    padding: 16px 20px;
    margin-bottom: 12px;
  }
  &__row {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 12px;
    margin-bottom: 12px;
    .ai-result__item { margin-bottom: 0; }
  }
  &__label {
    display: inline-flex;
    align-items: center;
    gap: 4px;
    color: var(--zh-color-primary);
    font-size: var(--zh-font-size-sm);
    font-weight: 600;
    margin-bottom: 8px;
    svg { width: 14px; height: 14px; }
  }
  &__value {
    font-size: var(--zh-font-size-lg);
    font-weight: 500;
    color: var(--zh-color-text);
    line-height: 1.5;
  }
  &__chips {
    display: flex;
    flex-wrap: wrap;
    gap: 6px;
  }
  &__category {
    display: flex;
    flex-direction: column;
    gap: 6px;
  }
  &__category-card {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 8px 12px;
    background: white;
    border: 1px solid var(--zh-color-border-light);
    border-radius: var(--zh-radius-sm);
    transition: all var(--zh-duration-base) var(--zh-easing-standard);
    &.is-top {
      background: var(--zh-gradient-primary-soft);
      border-color: var(--zh-color-primary);
    }
  }
  &__category-name { font-size: var(--zh-font-size-sm); }
  &__category-conf { font-size: var(--zh-font-size-xs); color: var(--zh-color-primary); font-weight: 700; }
  &__price {
    display: flex;
    align-items: baseline;
    gap: 16px;
    margin-bottom: 8px;
  }
  &__price-range {
    font-size: var(--zh-font-size-base);
    color: var(--zh-color-text-tertiary);
  }
  &__price-mid {
    display: inline-flex;
    align-items: center;
    gap: 8px;
  }
  &__price-badge {
    padding: 2px 8px;
    background: var(--zh-gradient-primary);
    color: white;
    border-radius: var(--zh-radius-full);
    font-size: 10px;
    font-weight: 700;
  }
  &__price-value {
    font-size: var(--zh-font-size-2xl);
    font-weight: 800;
    color: var(--zh-color-danger);
  }
  &__reason {
    color: var(--zh-color-text-secondary);
    font-size: var(--zh-font-size-sm);
    line-height: 1.6;
  }
  &__desc {
    color: var(--zh-color-text-secondary);
    line-height: 1.7;
    p { margin-bottom: 4px; }
  }
  &__audit {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 12px 16px;
    border-radius: var(--zh-radius-sm);
    &--pass { background: var(--zh-color-success-light); }
    &--warn { background: var(--zh-color-warning-light); }
    &--reject { background: var(--zh-color-danger-light); }
  }
  &__audit-icon {
    width: 36px;
    height: 36px;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    color: white;
    flex-shrink: 0;
    svg { width: 18px; height: 18px; }
  }
  &__audit--pass &__audit-icon { background: var(--zh-color-success); }
  &__audit--warn &__audit-icon { background: var(--zh-color-warning); }
  &__audit--reject &__audit-icon { background: var(--zh-color-danger); }
  &__audit-title { font-weight: 600; }
  &__audit-reason { font-size: var(--zh-font-size-sm); color: var(--zh-color-text-secondary); }
}

.typing .caret {
  display: inline-block;
  width: 2px;
  height: 1em;
  background: var(--zh-color-primary);
  margin-left: 2px;
  animation: zh-typing-cursor 1s step-end infinite;
  vertical-align: middle;
}

/* Edit */
.publish-edit {
  display: grid;
  grid-template-columns: 360px 1fr;
  gap: 32px;
  &__left { position: sticky; top: 80px; height: fit-content; }
  &__image {
    position: relative;
    aspect-ratio: 1;
    border-radius: var(--zh-radius-md);
    overflow: hidden;
    img { width: 100%; height: 100%; object-fit: cover; }
  }
  &__image-overlay {
    position: absolute;
    bottom: 8px;
    left: 8px;
    right: 8px;
    padding: 6px 10px;
    background: rgba(0, 0, 0, 0.5);
    backdrop-filter: blur(8px);
    color: white;
    border-radius: var(--zh-radius-sm);
    font-size: var(--zh-font-size-xs);
    display: flex;
    align-items: center;
    gap: 6px;
    svg { width: 12px; height: 12px; }
  }
  &__field {
    margin-bottom: 20px;
    label {
      display: block;
      font-size: var(--zh-font-size-sm);
      font-weight: 500;
      color: var(--zh-color-text);
      margin-bottom: 6px;
    }
    input, textarea, select {
      width: 100%;
      padding: 10px 12px;
      background: var(--zh-color-bg);
      border: 1px solid var(--zh-color-border);
      border-radius: var(--zh-radius-sm);
      font-size: var(--zh-font-size-base);
      color: var(--zh-color-text);
      transition: all var(--zh-duration-base) var(--zh-easing-standard);
      outline: none;
      &:focus {
        background: white;
        border-color: var(--zh-color-primary);
        box-shadow: 0 0 0 3px rgba(124, 58, 237, 0.1);
      }
    }
    textarea { resize: vertical; line-height: 1.6; }
  }
  &__row {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 16px;
  }
  &__ai-tip {
    display: inline-flex;
    align-items: center;
    gap: 4px;
    color: var(--zh-color-primary);
    font-size: var(--zh-font-size-xs);
    margin-top: 4px;
    svg { width: 12px; height: 12px; }
  }
  &__checks {
    display: flex;
    gap: 12px;
  }
  &__check {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    padding: 6px 12px;
    background: var(--zh-color-bg);
    border-radius: var(--zh-radius-sm);
    cursor: pointer;
    transition: all var(--zh-duration-base) var(--zh-easing-standard);
    &.is-checked {
      background: var(--zh-gradient-primary-soft);
      color: var(--zh-color-primary);
    }
  }
}

/* Submit */
.publish-submit {
  text-align: center;
  padding: 60px 0;
  &__check {
    width: 100px;
    height: 100px;
    margin: 0 auto 24px;
    background: var(--zh-gradient-primary-soft);
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    color: var(--zh-color-primary);
    font-size: 48px;
    position: relative;
  }
  &__icon svg { width: 48px; height: 48px; }
  &__spinner {
    width: 48px;
    height: 48px;
    border: 4px solid var(--zh-color-border);
    border-top-color: var(--zh-color-primary);
    border-radius: 50%;
    animation: zh-spin 0.8s linear infinite;
  }
  &__check.is-done {
    background: var(--zh-gradient-primary);
    color: white;
  }
  h2 {
    font-size: var(--zh-font-size-2xl);
    font-weight: 700;
    margin-bottom: 12px;
  }
  p {
    color: var(--zh-color-text-secondary);
    max-width: 480px;
    margin: 0 auto 32px;
    line-height: 1.6;
  }
  &__actions {
    display: flex;
    gap: 12px;
    justify-content: center;
  }
}
</style>
