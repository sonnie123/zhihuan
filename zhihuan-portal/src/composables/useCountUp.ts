import { ref, watch, onMounted, type Ref } from 'vue'

export function useCountUp(source: Ref<number>, duration = 1200) {
  const display = ref(0)
  function animate(to: number) {
    const start = display.value
    const startTime = Date.now()
    function tick() {
      const t = Math.min((Date.now() - startTime) / duration, 1)
      const ease = 1 - Math.pow(1 - t, 3)
      display.value = Math.floor(start + (to - start) * ease)
      if (t < 1) requestAnimationFrame(tick)
    }
    tick()
  }
  onMounted(() => animate(source.value))
  watch(source, animate)
  return display
}
