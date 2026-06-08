import {
  computed,
  onUnmounted,
  type Ref,
  shallowRef,
  watch,
} from 'vue'
import { gsap } from 'gsap'
import type { ApplyReducedMotionFallback, BuildFilmTimeline } from './types'

export function useFilmDemo(
  rootRef: Ref<HTMLElement | null>,
  buildTimeline: BuildFilmTimeline,
  applyFallback: ApplyReducedMotionFallback,
) {
  const timeline = shallowRef<gsap.core.Timeline | null>(null)
  const isPlaying = shallowRef(false)
  const isReducedMotion = shallowRef(false)
  let ctx: gsap.Context | undefined
  let matchMedia: ReturnType<typeof gsap.matchMedia> | undefined

  const canControl = computed(
    () => timeline.value !== null && !isReducedMotion.value,
  )

  function play(): void {
    if (!timeline.value) return
    timeline.value.play()
    isPlaying.value = true
  }

  function pause(): void {
    if (!timeline.value) return
    timeline.value.pause()
    isPlaying.value = false
  }

  function restart(): void {
    if (!timeline.value) return
    timeline.value.restart()
    isPlaying.value = true
  }

  function teardown(): void {
    timeline.value?.kill()
    timeline.value = null
    isPlaying.value = false
    matchMedia?.revert()
    matchMedia = undefined
    ctx?.revert()
    ctx = undefined
  }

  function setup(root: HTMLElement): void {
    teardown()

    ctx = gsap.context(() => {
      matchMedia = gsap.matchMedia()

      matchMedia.add('(prefers-reduced-motion: reduce)', () => {
        isReducedMotion.value = true
        isPlaying.value = false
        timeline.value?.kill()
        timeline.value = null
        applyFallback(root)
      })

      matchMedia.add('(prefers-reduced-motion: no-preference)', () => {
        isReducedMotion.value = false
        const tl = buildTimeline(root)
        timeline.value = tl
        tl.play()
        isPlaying.value = true

        return () => {
          tl.kill()
          timeline.value = null
          isPlaying.value = false
        }
      })
    }, root)
  }

  const stopWatch = watch(
    rootRef,
    (root) => {
      if (root) setup(root)
    },
    { immediate: true },
  )

  onUnmounted(() => {
    stopWatch()
    teardown()
  })

  return { timeline, isPlaying, isReducedMotion, canControl, play, pause, restart }
}
