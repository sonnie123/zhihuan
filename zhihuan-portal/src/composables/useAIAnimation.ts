import { ref, reactive } from 'vue'
import type { AIStepResult, AIStepName } from '@/types/entity/ai'

// 编排 AI 步骤动画
export function useAIAnimation() {
  const steps = reactive<AIStepResult[]>([
    { name: 'image', label: '图像识别', status: 'pending' },
    { name: 'category', label: '类目预测', status: 'pending' },
    { name: 'price', label: 'AI 估价', status: 'pending' },
    { name: 'description', label: '文案生成', status: 'pending' },
    { name: 'audit', label: '风险审核', status: 'pending' },
  ])
  const progress = ref(0)
  const running = ref(false)

  async function run(callback: (name: AIStepName) => Promise<void>) {
    running.value = true
    for (let i = 0; i < steps.length; i++) {
      steps[i].status = 'running'
      progress.value = (i / steps.length) * 100
      const start = Date.now()
      try {
        await callback(steps[i].name)
        steps[i].status = 'success'
        steps[i].duration = Date.now() - start
      } catch (e: any) {
        steps[i].status = 'failed'
        steps[i].error = e?.message || '执行失败'
      }
      // 步骤间停顿
      await new Promise(r => setTimeout(r, 400))
    }
    progress.value = 100
    running.value = false
  }

  function reset() {
    steps.forEach(s => { s.status = 'pending'; s.duration = undefined; s.data = undefined })
    progress.value = 0
    running.value = false
  }

  return { steps, progress, running, run, reset }
}
