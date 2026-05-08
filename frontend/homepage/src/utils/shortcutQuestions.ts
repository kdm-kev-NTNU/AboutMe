export const SHORTCUT_ROTATION_MS = 6 * 60 * 60 * 1000

const introByLang = {
  en: [
    'Why did Kevin create this website?',
    'Which courses has Kevin taken?',
    'Which projects has Kevin worked on?',
    'Who is Kevin?',
  ],
  no: [
    'Hvorfor lagde Kevin denne nettsiden?',
    'Hvilke emner har Kevin hatt?',
    'Hvilke prosjekter har Kevin jobbet med?',
    'Hvem er Kevin?',
  ],
} as const

const techByLang = {
  en: [
    "What's the tech stack behind kevindmazali.me (Vue, Spring Boot, Postgres/pgvector)?",
    'How does the RAG chat and `/ask` work in your stack?',
    'Which AI and ops choices matter here (embeddings, budget, Docker/CI)?',
    "How does kevindmazali.me's stack compare to Krisefikser (Vue + Spring)?",
  ],
  no: [
    'Hva kjører kevindmazali.me teknisk — Vue, Spring Boot, Postgres/pgvector?',
    'Hvordan fungerer RAG-chatten og `/ask` i stacken din?',
    'Hvilke AI- og driftvalg er viktige her (embeddings, budsjett, Docker/CI)?',
    'Hva er lik og ulik mellom stacken på kevindmazali.me og Krisefikser (Vue + Spring)?',
  ],
} as const

/** Exposed for tests */
export function getIntroPool(lang: 'en' | 'no'): readonly string[] {
  return introByLang[lang]
}

/** Exposed for tests */
export function getTechPool(lang: 'en' | 'no'): readonly string[] {
  return techByLang[lang]
}

function mulberry32(seed: number): () => number {
  let a = seed >>> 0
  return () => {
    a = (a + 0x6d2b79f5) >>> 0
    let t = a
    t = Math.imul(t ^ (t >>> 15), t | 1)
    t ^= t + Math.imul(t ^ (t >>> 7), t | 61)
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296
  }
}

function mixSeed(bucket: number, lang: 'en' | 'no'): number {
  let h = bucket >>> 0
  h = Math.imul(h ^ (h >>> 16), 0x7feb352d)
  for (let i = 0; i < lang.length; i++) {
    h = Math.imul(h ^ lang.charCodeAt(i), 0x5bd1e995)
  }
  return h >>> 0
}

function shuffleInPlace<T>(arr: T[], random: () => number): void {
  for (let i = arr.length - 1; i > 0; i--) {
    const j = Math.floor(random() * (i + 1))
    ;[arr[i], arr[j]] = [arr[j], arr[i]]
  }
}

function sampleUnique<T>(items: readonly T[], n: number, random: () => number): T[] {
  const copy = [...items]
  shuffleInPlace(copy, random)
  return copy.slice(0, n)
}

export function shortcutRotationBucket(nowMs: number): number {
  return Math.floor(nowMs / SHORTCUT_ROTATION_MS)
}

/**
 * Deterministic per (6h UTC wall bucket, lang): two intro + two tech shortcuts, order shuffled.
 */
export function pickRotatingShortcuts(lang: 'en' | 'no', nowMs: number): string[] {
  const bucket = shortcutRotationBucket(nowMs)
  const random = mulberry32(mixSeed(bucket, lang))
  const intro = sampleUnique(introByLang[lang], 2, random)
  const tech = sampleUnique(techByLang[lang], 2, random)
  const merged = [...intro, ...tech]
  shuffleInPlace(merged, random)
  return merged
}
