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
})
