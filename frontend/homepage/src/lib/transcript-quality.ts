import type { SpeechUiLang } from './realtime-voice'

const STOP_WORDS = new Set([
  'the',
  'and',
  'for',
  'with',
  'what',
  'does',
  'about',
  'tell',
  'his',
  'her',
  'him',
  'hva',
  'om',
  'og',
  'med',
  'kan',
  'du',
  'han',
  'hans',
  'kevin',
])

/** Returns true when Whisper output looks too short or garbled to trust. */
export function isTranscriptUncertain(text: string): boolean {
  const trimmed = text.trim()
  if (trimmed.length === 0) {
    return true
  }

  const meaningful = trimmed
    .toLowerCase()
    .split(/[^\p{L}\p{N}]+/u)
    .filter((part) => part.length > 0 && !STOP_WORDS.has(part))
    .join('')
  if (meaningful.length < 3) {
    return true
  }

  const letters = trimmed.match(/\p{L}/gu)?.length ?? 0
  if (trimmed.length > 0 && letters / trimmed.length < 0.5) {
    return true
  }

  return false
}

export function repeatRequestMessage(language: SpeechUiLang): string {
  return language === 'no'
    ? 'Beklager, jeg hørte deg ikke helt tydelig. Kan du si det på nytt?'
    : "Sorry, I didn't catch that clearly. Could you please say it again?"
}
