export type DetectedLanguage = 'no' | 'en' | 'unknown'

const NORWEGIAN_CHARS = /[æøåÆØÅ]/

const NORWEGIAN_WORDS = new Set([
  'hva', 'hvem', 'hvilke', 'hvilken', 'hvilket', 'hvorfor', 'hvordan', 'hvor',
  'kan', 'har', 'er', 'var', 'skal', 'vil', 'meg', 'deg', 'seg', 'det',
  'denne', 'dette', 'disse', 'den', 'om', 'fra', 'til', 'med', 'på', 'for',
  'ved', 'etter', 'av', 'sin', 'sitt', 'sine', 'min', 'mitt', 'mine',
  'din', 'ditt', 'dine', 'og', 'eller', 'men', 'ikke', 'også', 'når',
  'fortell', 'forklar', 'beskriv', 'gi', 'vis', 'hei', 'takk', 'jobbet',
  'studerer', 'prosjekter', 'prosjekt', 'emner', 'lagde', 'nettsiden',
])

const ENGLISH_WORDS = new Set([
  'what', 'who', 'which', 'why', 'how', 'where', 'when',
  'can', 'has', 'have', 'is', 'are', 'was', 'were', 'will', 'would', 'should',
  'the', 'this', 'that', 'these', 'those', 'from', 'with', 'about', 'for',
  'and', 'but', 'not', 'also', 'tell', 'explain', 'describe', 'give', 'show',
  'hello', 'thanks', 'worked', 'studies', 'projects', 'project', 'courses',
  'does', 'did', 'do', 'his', 'her', 'your', 'you', 'me', 'him',
])

function tokenize(text: string): string[] {
  return text
    .toLowerCase()
    .replace(/[^\p{L}\p{N}\s]/gu, ' ')
    .split(/\s+/)
    .filter((w) => w.length > 0)
}

/**
 * Heuristic language detection for short user prompts.
 * Returns 'no' for Norwegian, 'en' for English, 'unknown' if inconclusive.
 */
export function detectLanguage(text: string): DetectedLanguage {
  if (!text || text.trim().length === 0) return 'unknown'

  if (NORWEGIAN_CHARS.test(text)) return 'no'

  const words = tokenize(text)
  if (words.length === 0) return 'unknown'

  let noScore = 0
  let enScore = 0

  for (const w of words) {
    if (NORWEGIAN_WORDS.has(w)) noScore++
    if (ENGLISH_WORDS.has(w)) enScore++
  }

  if (noScore === 0 && enScore === 0) return 'unknown'
  if (noScore > enScore) return 'no'
  if (enScore > noScore) return 'en'
  return 'unknown'
}
