import { describe, expect, it } from 'vitest'
import { formatLookupForSpeech } from '../voice-answer'

describe('formatLookupForSpeech', () => {
  it('joins first two non-empty snippets', () => {
    const result = formatLookupForSpeech(
      {
        found: true,
        confidence: 'high',
        snippets: [
          { sourceType: 'profile', title: 'A', text: 'First' },
          { sourceType: 'rag', title: 'B', text: 'Second' },
          { sourceType: 'rag', title: 'C', text: 'Third' },
        ],
      },
      'en',
    )
    expect(result).toBe('First Second')
  })

  it('returns localized fallback when no snippets found', () => {
    expect(formatLookupForSpeech({ found: false, confidence: 'none', snippets: [] }, 'en')).toContain(
      "don't have information",
    )
    expect(formatLookupForSpeech({ found: false, confidence: 'none', snippets: [] }, 'no')).toContain('Jeg har ikke')
  })

  it('prefixes low-confidence answers with uncertainty messaging', () => {
    const result = formatLookupForSpeech(
      {
        found: true,
        confidence: 'low',
        snippets: [{ sourceType: 'profile', title: 'A', text: 'Maybe Kevin studies IT.' }],
      },
      'en',
    )
    expect(result).toContain('not sure I understood')
    expect(result).toContain('Maybe Kevin studies IT.')
    expect(result).toContain('rephrasing')
  })

  it('skips empty snippet text', () => {
    expect(
      formatLookupForSpeech(
        {
          found: true,
          confidence: 'high',
          snippets: [
            { sourceType: 'profile', title: 'A', text: '   ' },
            { sourceType: 'rag', title: 'B', text: 'Only this' },
          ],
        },
        'en',
      ),
    ).toBe('Only this')
  })
})
