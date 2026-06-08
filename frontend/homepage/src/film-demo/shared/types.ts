import type { gsap } from 'gsap'

export type SelectorFunc = ReturnType<typeof gsap.utils.selector>
export type MicPulseRef = { current: gsap.core.Tween | null }

export type CursorRules = {
  movementEase: string
  durationShort: { min: number; max: number }
  durationLong: { min: number; max: number }
  clickScale: number
  clickScaleDuration: number
  rippleScale: number
  rippleDuration: number
  releaseEase: string
  releaseDelay: number
  releaseDuration: number
}

export type FilmLabel =
  | 'reset'
  | 'heroEnter'
  | 'cursorToOrb'
  | 'orbClick'
  | 'voiceReveal'
  | 'cursorToVoiceMic'
  | 'voiceMicClick'
  | 'voiceListening'
  | 'cursorToTextChat'
  | 'textChatClick'
  | 'chatReveal'
  | 'chatTypeQuestion'
  | 'cursorToSend'
  | 'chatSend'
  | 'chatThinking'
  | 'chatAiReply'
  | 'loopEnd'

export type NavTab = 'home' | 'reason' | 'how'

export interface FilmScene {
  order: number
  label: FilmLabel
  description: string
  instructions: readonly string[]
  holdAfterSeconds: number | null
  activeNav: NavTab
}

export interface ResetTarget {
  selector: string
  properties: Record<string, string | number>
  notes?: string
}

export interface FilmMeta {
  frameWidth: number
  frameHeight: number
  language: 'en' | 'no'
  timeline: {
    repeat: number
    repeatDelay: number
    defaults: { ease: string }
  }
}

export type BuildFilmTimeline = (root: HTMLElement) => gsap.core.Timeline
export type ApplyReducedMotionFallback = (root: HTMLElement) => void
