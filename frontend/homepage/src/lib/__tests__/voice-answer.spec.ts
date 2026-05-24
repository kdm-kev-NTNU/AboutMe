import { describe, expect, it } from 'vitest'
import { formatLookupForSpeech } from '../voice-answer'

describe('formatLookupForSpeech', () => {
  it('joins first two non-empty snippets', () => {
    const result = formatLookupForSpeech(
      {
        found: true,
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
    expect(formatLookupForSpeech({ found: false, snippets: [] }, 'en')).toContain("couldn't find")
    expect(formatLookupForSpeech({ found: false, snippets: [] }, 'no')).toContain('Beklager')
  })

  it('skips empty snippet text', () => {
    expect(
      formatLookupForSpeech(
        {
          found: true,
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
