import { gsap } from 'gsap'
import type { CursorRules, MicPulseRef, SelectorFunc } from './types'

export function getCenter(
  el: Element,
  root: HTMLElement,
  frameWidth: number,
): { x: number; y: number } {
  const rootRect = root.getBoundingClientRect()
  const elRect = el.getBoundingClientRect()
  const scale = rootRect.width / frameWidth
  return {
    x: (elRect.left + elRect.width / 2 - rootRect.left) / scale,
    y: (elRect.top + elRect.height / 2 - rootRect.top) / scale,
  }
}

export function cursorDuration(
  distance: number,
  rules: Pick<CursorRules, 'durationShort' | 'durationLong'>,
): number {
  const { durationShort, durationLong } = rules
  const mapped = gsap.utils.mapRange(
    0,
    900,
    durationShort.min,
    durationLong.max,
    distance,
  )
  return gsap.utils.clamp(durationShort.min, durationLong.max, mapped)
}

export function killMicPulse(micPulseRef: MicPulseRef): void {
  micPulseRef.current?.kill()
  micPulseRef.current = null
}

export function startMicPulse(
  q: SelectorFunc,
  micPulseRef: MicPulseRef,
  targetSelector: string,
  intensity: 'idle' | 'intense',
): void {
  killMicPulse(micPulseRef)
  const orb = q(targetSelector)[0]
  if (!orb) return

  const scale = intensity === 'idle' ? 1.03 : 1.07
  const duration = intensity === 'idle' ? 1.4 : 0.75

  micPulseRef.current = gsap.to(orb, {
    scale,
    duration,
    repeat: -1,
    yoyo: true,
    ease: 'sine.inOut',
  })
}

export function moveCursor(
  tl: gsap.core.Timeline,
  q: SelectorFunc,
  root: HTMLElement,
  targetEl: Element,
  frameWidth: number,
  rules: CursorRules,
  cursorSelector: string,
  position?: gsap.Position,
  options?: { entryDuration?: number },
): void {
  const cursor = q(cursorSelector)[0]
  if (!cursor) return

  const center = getCenter(targetEl, root, frameWidth)
  const curX = gsap.getProperty(cursor, 'x') as number
  const curY = gsap.getProperty(cursor, 'y') as number
  const distance = Math.hypot(center.x - curX, center.y - curY)
  const duration =
    options?.entryDuration ?? cursorDuration(distance, rules)

  tl.set(cursor, { autoAlpha: 1 }, position)
  tl.to(
    cursor,
    { x: center.x, y: center.y, duration, ease: rules.movementEase },
    position,
  )
}

export function performClick(
  tl: gsap.core.Timeline,
  q: SelectorFunc,
  x: number,
  y: number,
  label: string,
  rules: CursorRules,
  selectors: { cursor: string; ripple: string },
  position?: gsap.Position,
): void {
  const cursor = q(selectors.cursor)[0]
  const ripple = q(selectors.ripple)[0]
  if (!cursor || !ripple) return

  tl.addLabel(label, position)
  tl.to(
    cursor,
    {
      scale: rules.clickScale,
      duration: rules.clickScaleDuration,
      ease: 'power2.out',
    },
    label,
  )
  tl.set(ripple, { x, y, scale: 0.2, autoAlpha: 0.75 }, label)
  tl.to(
    ripple,
    {
      scale: rules.rippleScale,
      autoAlpha: 0,
      duration: rules.rippleDuration,
      ease: 'power2.out',
    },
    label,
  )
  tl.to(
    cursor,
    {
      scale: 1,
      duration: rules.releaseDuration,
      ease: rules.releaseEase,
    },
    `${label}+=${rules.releaseDelay}`,
  )
}

export function createTypewriterTween(
  text: string,
  charRate: number,
  onUpdate: (slice: string) => void,
): { proxy: { progress: number }; vars: gsap.TweenVars } {
  const proxy = { progress: 0 }
  return {
    proxy,
    vars: {
      progress: text.length,
      duration: text.length * charRate,
      ease: 'none',
      onUpdate() {
        onUpdate(text.slice(0, Math.round(proxy.progress)))
      },
    },
  }
}
