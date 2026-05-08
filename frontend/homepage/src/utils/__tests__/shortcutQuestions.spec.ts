import { describe, expect, it } from 'vitest'
import {
  SHORTCUT_ROTATION_MS,
  getIntroPool,
  getTechPool,
  pickRotatingShortcuts,
  shortcutRotationBucket,
} from '../shortcutQuestions'

describe('pickRotatingShortcuts', () => {
  it('returns the same four questions for the same time within a 6h bucket', () => {
    const t = 1_704_000_000_000
    const deltaMs = 60_000
    expect(shortcutRotationBucket(t)).toBe(shortcutRotationBucket(t + deltaMs))
    const a = pickRotatingShortcuts('en', t)
    const b = pickRotatingShortcuts('en', t + deltaMs)
    expect(a).toEqual(b)
  })

  it('can differ across 6h bucket boundaries', () => {
    const t0 = 5_000_000_000
    const b0 = shortcutRotationBucket(t0)
    const t1 = t0 + SHORTCUT_ROTATION_MS
    expect(shortcutRotationBucket(t1)).toBe(b0 + 1)
    const en0 = pickRotatingShortcuts('en', t0)
    const en1 = pickRotatingShortcuts('en', t1)
    expect(en0).not.toEqual(en1)
  })

  it('returns four unique strings', () => {
    const q = pickRotatingShortcuts('no', 9_999_888_777_000)
    expect(q).toHaveLength(4)
    expect(new Set(q).size).toBe(4)
  })

  it('always picks exactly two intro and two tech questions per language', () => {
    const langs = ['en', 'no'] as const
    for (const lang of langs) {
      const intro = new Set(getIntroPool(lang))
      const tech = new Set(getTechPool(lang))
      for (let bucket = 0; bucket < 50; bucket++) {
        const t = bucket * SHORTCUT_ROTATION_MS + 12345
        const picked = pickRotatingShortcuts(lang, t)
        let introCount = 0
        let techCount = 0
        for (const s of picked) {
          if (intro.has(s)) introCount++
          else if (tech.has(s)) techCount++
          else throw new Error(`Unexpected shortcut: ${s}`)
        }
        expect(introCount).toBe(2)
        expect(techCount).toBe(2)
      }
    }
  })

  it('differs by language for the same timestamp', () => {
    const t = 8_888_777_666_000
    expect(pickRotatingShortcuts('en', t)).not.toEqual(pickRotatingShortcuts('no', t))
  })

  // --- Language pool content validation ---

  it('all English intro questions are in English', () => {
    const pool = getIntroPool('en')
    for (const q of pool) {
      expect(q).toMatch(/^[A-Z]/)
      expect(q).not.toMatch(/^(Hva|Hvem|Hvilke|Hvorfor|Hvordan|Fortell)/)
    }
  })

  it('all Norwegian intro questions are in Norwegian', () => {
    const pool = getIntroPool('no')
    for (const q of pool) {
      expect(q).toMatch(/^(Hva|Hvem|Hvilke|Hvorfor|Hvordan)/)
    }
  })

  it('all English tech questions are in English', () => {
    const pool = getTechPool('en')
    for (const q of pool) {
      expect(q).not.toMatch(/^(Hva|Hvem|Hvilke|Hvorfor|Hvordan)/)
    }
  })

  it('all Norwegian tech questions are in Norwegian', () => {
    const pool = getTechPool('no')
    for (const q of pool) {
      expect(q).toMatch(/^(Hva|Hvem|Hvilke|Hvorfor|Hvordan)/)
    }
  })

  it('intro pools for en and no have the same length', () => {
    expect(getIntroPool('en').length).toBe(getIntroPool('no').length)
  })

  it('tech pools for en and no have the same length', () => {
    expect(getTechPool('en').length).toBe(getTechPool('no').length)
  })

  it('no English question appears in the Norwegian pool', () => {
    const enIntro = new Set(getIntroPool('en'))
    const enTech = new Set(getTechPool('en'))
    for (const q of getIntroPool('no')) {
      expect(enIntro.has(q)).toBe(false)
    }
    for (const q of getTechPool('no')) {
      expect(enTech.has(q)).toBe(false)
    }
  })

  it('no Norwegian question appears in the English pool', () => {
    const noIntro = new Set(getIntroPool('no'))
    const noTech = new Set(getTechPool('no'))
    for (const q of getIntroPool('en')) {
      expect(noIntro.has(q)).toBe(false)
    }
    for (const q of getTechPool('en')) {
      expect(noTech.has(q)).toBe(false)
    }
  })

  it('English shortcuts only contain English text', () => {
    for (let bucket = 0; bucket < 100; bucket++) {
      const t = bucket * SHORTCUT_ROTATION_MS + 42
      const shortcuts = pickRotatingShortcuts('en', t)
      for (const s of shortcuts) {
        expect(s).not.toMatch(/^(Hva|Hvem|Hvilke|Hvorfor|Hvordan)/)
      }
    }
  })

  it('Norwegian shortcuts only contain Norwegian text', () => {
    for (let bucket = 0; bucket < 100; bucket++) {
      const t = bucket * SHORTCUT_ROTATION_MS + 42
      const shortcuts = pickRotatingShortcuts('no', t)
      for (const s of shortcuts) {
        expect(s).toMatch(/[æøåÆØÅ]|^(Hva|Hvem|Hvilke|Hvorfor|Hvordan)/)
      }
    }
  })

  it('all shortcut questions end with a question mark', () => {
    const langs = ['en', 'no'] as const
    for (const lang of langs) {
      for (const q of getIntroPool(lang)) {
        expect(q.trim().endsWith('?')).toBe(true)
      }
      for (const q of getTechPool(lang)) {
        expect(q.trim().endsWith('?')).toBe(true)
      }
    }
  })

  it('intro and tech pools do not overlap within the same language', () => {
    const langs = ['en', 'no'] as const
    for (const lang of langs) {
      const intro = new Set(getIntroPool(lang))
      const tech = new Set(getTechPool(lang))
      for (const q of tech) {
        expect(intro.has(q)).toBe(false)
      }
    }
  })
})
