# AboutMe

Personlig nettside og portfolio med tekstchat og stemme som kan svare ut fra offentlig innhold om utdanning, prosjekter og erfaring. Grensesnittet finnes på norsk og engelsk.

## Hva du finner på siden

- **Portfolio** — oversikt over prosjekter, karriere og bakgrunn  
- **Chat** — still spørsmål om innhold som er lagt inn som kunnskapsgrunnlag  
- **Stemme** — snakk med assistenten der det er slått på i utvikling  
- **Tilbakemelding** — send inn kommentarer eller forslag  

Personvern og informasjonskapsler er forklart på siden.

## Teknologi (kort fortalt)

Nettsiden er bygget som en moderne webapp: Vue på brukerflaten, Java/Spring på serveren, og en database med vektorlagring slik at svarene kan hente inn relevant tekst før de genereres.

**Sanntidsstemme** er valgfritt og kan gå via én av to leverandører når `PORTFOLIO_REALTIME_ENABLED=true`: **OpenAI Realtime** (WebRTC) eller **ElevenLabs Conversational AI** (WebRTC). Backend eksponerer tilgjengelige stemmemodeller og mint’er et kortlevd samtale-token til ElevenLabs (`POST /realtime/elevenlabs/token`), slik at **ElevenLabs API-nøkkel bare ligger på serveren**. Nettleseren bruker SDK-et **`@elevenlabs/client`** etter å ha hentet token fra backend. Slå ElevenLabs på med `PORTFOLIO_REALTIME_ELEVENLABS_ENABLED` og `ELEVENLABS_*` — se [`.env.example`](.env.example).

**Agent og kunnskapsgrunnlag mot ElevenLabs** kan synkroniseres med Python-scriptet [`scripts/sync_elevenlabs_portfolio_agent.py`](scripts/sync_elevenlabs_portfolio_agent.py) (avhengigheter i [`scripts/requirements-elevenlabs.txt`](scripts/requirements-elevenlabs.txt)). Tekstfiler og profiler som scriptet forventer, er beskrevet i [`backend/src/main/resources/realtime/FILES.example`](backend/src/main/resources/realtime/FILES.example). For agent-oppdateringer i repo er dette den stabile flyten; ElevenLabs MCP i Cursor er et hjelpeverktøy, ikke primær kilde for PATCH.

## Kjøre prosjektet lokalt

Du trenger Docker og en OpenAI API-nøkkel for tekstchat og RAG. For stemmesiden med ElevenLabs trenger du i tillegg API-nøkkel og agent-id som beskrevet i `.env.example`.

### Profil, prompts og RAG-maler (ikke i git)

Filer under `backend/src/main/resources/prompts/`, `…/templates/` og `…/realtime/` (system prompts, RAG-maler, `kevin-profile.json`, ElevenLabs-tekster m.m.) er **ikke** versjonert her — legg inn egne kopier lokalt før du bygger eller kjører. Se de sporede filene `FILES.example` og `*.st.example` i disse mappene for hvilke filnavn som forventes.

For `docker compose build` / produksjonsimage må du sørge for at disse filene finnes i `backend/src/main/resources/…` før image bygges (eller kopier dem inn i build-steget ditt).

### Frontend: portfolio-JSON (prosjekter, karriere, emner)

Filene `projects.*.json`, `education.*.json`, `workExperience.*.json` og `courses.*.json` under [`frontend/homepage/src/types/`](frontend/homepage/src/types/) er **ikke** versjonert. Uten lokale kopier bruker Vite de sporede `*.stub.json`-filene slik at bygg og tester fungerer. Se [`FILES.example`](frontend/homepage/src/types/FILES.example) der.

### Miljø og oppstart

1. Kopier [`.env.example`](.env.example) til `backend/.env` og sett `OPENAI_API_KEY`.  
2. Kjør:

```bash
docker compose up -d --build
```

Typisk så åpner du appen på [http://localhost:5173](http://localhost:5173). Mer detaljer for utvikling finnes i [frontend/homepage/README.md](frontend/homepage/README.md).

## Kontakt

Spørsmål eller tilbakemeldinger: [kevindmazali@gmail.com](mailto:kevindmazali@gmail.com)
