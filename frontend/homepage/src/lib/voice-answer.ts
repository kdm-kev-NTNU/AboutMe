import type { RealtimeLookupConfidence, RealtimeLookupResponse, SpeechUiLang } from './realtime-voice'

function noneMessage(language: SpeechUiLang): string {
  return language === 'no'
    ? 'Jeg har ikke informasjon om det. Prøv gjerne å spørre om Kevin sine studier, prosjekter eller erfaring.'
    : "I don't have information about that. Try asking about Kevin's studies, projects, or experience."
}

function lowConfidencePrefix(language: SpeechUiLang): string {
  return language === 'no'
    ? 'Jeg er ikke helt sikker på hva du sa, men dette fant jeg: '
    : "I'm not sure I understood correctly, but here's what I found: "
}

function lowConfidenceSuffix(language: SpeechUiLang): string {
  return language === 'no'
    ? ' Kan du prøve å si det på en annen måte?'
    : ' Could you try rephrasing that?'
}

export function formatLookupForSpeech(response: RealtimeLookupResponse, language: SpeechUiLang): string {
  const confidence: RealtimeLookupConfidence = response.confidence ?? (response.found ? 'high' : 'none')
  const snippets = response.snippets
    .map((s) => s.text.trim())
    .filter((s) => s.length > 0)
    .slice(0, 2)

  if (confidence === 'none' || snippets.length === 0 || response.found !== true) {
    return noneMessage(language)
  }

  const body = snippets.join(' ')
  if (confidence === 'low') {
    return `${lowConfidencePrefix(language)}${body}${lowConfidenceSuffix(language)}`
  }

  return body
}
