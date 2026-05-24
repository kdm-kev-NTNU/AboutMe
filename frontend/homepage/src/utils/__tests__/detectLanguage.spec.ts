import { describe, expect, it } from 'vitest'
import { detectLanguage } from '../detectLanguage'

describe('detectLanguage', () => {
  describe('Norwegian detection', () => {
    it('detects Norwegian by æ/ø/å characters', () => {
      expect(detectLanguage('Hva gjør Kevin?')).toBe('no')
      expect(detectLanguage('Fortell om bacheloroppgåven')).toBe('no')
      expect(detectLanguage('Ærlighet varer lengst')).toBe('no')
    })

    it('detects Norwegian by common function words', () => {
      expect(detectLanguage('Hva studerer Kevin?')).toBe('no')
      expect(detectLanguage('Hvilke prosjekter har Kevin jobbet med?')).toBe('no')
      expect(detectLanguage('Hvorfor lagde Kevin denne nettsiden?')).toBe('no')
      expect(detectLanguage('Kan du fortelle meg om Kevin?')).toBe('no')
    })

    it('detects Norwegian shortcut questions', () => {
      expect(detectLanguage('Hvilke emner har Kevin hatt?')).toBe('no')
      expect(detectLanguage('Hvem er Kevin?')).toBe('no')
    })
  })

  describe('English detection', () => {
    it('detects English by common function words', () => {
      expect(detectLanguage('What does Kevin study?')).toBe('en')
      expect(detectLanguage('Which projects has Kevin worked on?')).toBe('en')
      expect(detectLanguage('Why did Kevin create this website?')).toBe('en')
      expect(detectLanguage('Who is Kevin?')).toBe('en')
    })

    it('detects English shortcut questions', () => {
      expect(detectLanguage('Which courses has Kevin taken?')).toBe('en')
      expect(detectLanguage('Tell me about his projects')).toBe('en')
    })

    it('detects English with technical terms', () => {
      expect(detectLanguage('How does the RAG chat work?')).toBe('en')
      expect(detectLanguage('What is the tech stack?')).toBe('en')
    })
  })

  describe('unknown / ambiguous detection', () => {
    it('returns unknown for empty input', () => {
      expect(detectLanguage('')).toBe('unknown')
      expect(detectLanguage('   ')).toBe('unknown')
    })

    it('returns unknown for single technical term', () => {
      expect(detectLanguage('Kevin')).toBe('unknown')
      expect(detectLanguage('NTNU')).toBe('unknown')
      expect(detectLanguage('Spring Boot')).toBe('unknown')
    })

    it('returns unknown for ambiguous input', () => {
      expect(detectLanguage('Kevin 2024')).toBe('unknown')
    })
  })

  describe('priority: Norwegian chars override word counts', () => {
    it('Norwegian chars win even if English words are present', () => {
      expect(detectLanguage('What about Kevins bacheloroppgåve?')).toBe('no')
    })
  })
})
