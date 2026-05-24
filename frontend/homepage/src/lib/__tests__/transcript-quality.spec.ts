import { describe, expect, it } from 'vitest'
import { isTranscriptUncertain, repeatRequestMessage } from '../transcript-quality'

describe('transcript-quality', () => {
  it('flags very short meaningful transcripts as uncertain', () => {
    expect(isTranscriptUncertain('hi')).toBe(true)
    expect(isTranscriptUncertain('kevin')).toBe(true)
  })

  it('accepts normal questions', () => {
    expect(isTranscriptUncertain('What does Kevin study at NTNU?')).toBe(false)
  })

  it('flags garbled transcripts with low letter ratio', () => {
    expect(isTranscriptUncertain('??? ### @@@')).toBe(true)
  })

  it('returns localized repeat prompts', () => {
    expect(repeatRequestMessage('en')).toContain('say it again')
    expect(repeatRequestMessage('no')).toContain('si det på nytt')
  })
})
