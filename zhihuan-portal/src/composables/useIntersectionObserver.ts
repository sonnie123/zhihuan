import { onMounted, onUnmounted, ref, type Ref } from 'vue'

export function useIntersectionObserver(
  target: Ref<HTMLElement | null>,
  callback: (entries: IntersectionObserverEntry[]) => void,
  options: IntersectionObserverInit = { threshold: 0.1 }
) {
  let observer: IntersectionObserver | null = null
  onMounted(() => {
    if (!target.value) return
    observer = new IntersectionObserver((entries) => {
      callback(entries)
    }, options)
    observer.observe(target.value)
  })
  onUnmounted(() => {
    observer?.disconnect()
  })
}
