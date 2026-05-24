import type { RealtimeLookupResponse, SpeechUiLang } from './realtime-voice'

export function formatLookupForSpeech(response: RealtimeLookupResponse, language: SpeechUiLang): string {
  const snippets = response.snippets
    .map((s) => s.text.trim())
    .filter((s) => s.length > 0)
    .slice(0, 2)

  if (snippets.length === 0 || response.found !== true) {
    return language === 'no'
      ? 'Beklager, jeg fant ikke noe sikkert svar akkurat nå. Prøv gjerne å spørre på en annen måte.'
      : "Sorry, I couldn't find a reliable answer right now. Please try asking in a different way."
  }
  return snippets.join(' ')
}
