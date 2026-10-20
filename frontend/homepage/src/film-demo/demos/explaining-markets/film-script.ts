export type MarketsLabel =
  | 'reset'
  | 'competitionEnter'
  | 'cursorToAgent'
  | 'agentClick'
  | 'agentReveal'
  | 'cursorToResearch'
  | 'researchClick'
  | 'researchReveal'
  | 'loopEnd'

export type MarketsNavTab = 'competition' | 'agent' | 'research'

export interface MarketsScene {
  order: number
  label: MarketsLabel
  description: string
  holdAfterSeconds: number | null
  activeNav: MarketsNavTab
}

export interface ResetTarget {
  selector: string
  properties: Record<string, string | number>
  notes?: string
}

export const FILM_META = {
  frameWidth: 1280,
  frameHeight: 800,
  language: 'no' as const,
  timeline: {
    repeat: -1,
    repeatDelay: 1.5,
    defaults: { ease: 'power3.out' },
  },
} as const

export const SCENES: readonly MarketsScene[] = [
  {
    order: 0,
    label: 'reset',
    description: 'Restore every animated property.',
    holdAfterSeconds: null,
    activeNav: 'competition',
  },
  {
    order: 1,
    label: 'competitionEnter',
    description: 'Competition page title, metrics and question stagger in.',
    holdAfterSeconds: 1.4,
    activeNav: 'competition',
  },
  {
    order: 2,
    label: 'cursorToAgent',
    description: 'Cursor moves to Agenten in the sidebar.',
    holdAfterSeconds: null,
    activeNav: 'competition',
  },
  {
    order: 3,
    label: 'agentClick',
    description: 'Click Agenten in the sidebar.',
    holdAfterSeconds: 0.3,
    activeNav: 'agent',
  },
  {
    order: 4,
    label: 'agentReveal',
    description: 'Crossfade to Agenten page.',
    holdAfterSeconds: 1.2,
    activeNav: 'agent',
  },
  {
    order: 5,
    label: 'cursorToResearch',
    description: 'Cursor moves to Forskning in the sidebar.',
    holdAfterSeconds: null,
    activeNav: 'agent',
  },
  {
    order: 6,
    label: 'researchClick',
    description: 'Click Forskning in the sidebar.',
    holdAfterSeconds: 0.3,
    activeNav: 'research',
  },
  {
    order: 7,
    label: 'researchReveal',
    description: 'Crossfade to Forskning page.',
    holdAfterSeconds: 1.5,
    activeNav: 'research',
  },
  {
    order: 8,
    label: 'loopEnd',
    description: 'Hold before loop restart.',
    holdAfterSeconds: 1.2,
    activeNav: 'research',
  },
] as const

export const COPY = {
  brand: 'Explaining Markets',
  presenter: 'Presentert av Kevin Dennis Mazali · Q4 2026',
  liveStatus: 'Agenten er deployet på Modal',
  nav: {
    competition: 'Konkurransen',
    agent: 'Agenten',
    research: 'Forskning',
  },
  competition: {
    title: 'Explaining Markets: en AI-agent i live-konkurranse',
    intro:
      'Kvartalsvis, live out-of-sample benchmark for AI-systemer som forklarer aksjeavkastning rundt earnings.',
    metrics: [
      { label: 'Q4 2026-vindu', value: '12. okt – 15. des', delta: 'scoring pågår nå' },
      { label: 'Målingen', value: 'ΔR²', delta: 'utover earnings-surprise' },
      { label: 'Agentstatus', value: 'Live på Modal', delta: 'webhook + 5-min prediksjon' },
      { label: 'Kuraterte papers', value: '21+', delta: 'ukentlig arXiv-scan' },
    ],
    questionTitle: 'Spørsmålet agenten svarer på',
    question:
      'Etter en earningsmelding: hvor lander denne aksjens neste-dags markedsjusterte avkastning i kvartalets tverrsnitt?',
  },
  agent: {
    label: 'Agenten',
    title: 'Fra webhook til prediksjon på fem minutter',
    subcopy:
      'Når et selskap rapporterer, får agenten beskjed. Den svarer med en gang, tenker, og sender inn rangeringen.',
    clocks: [
      {
        value: '20 s',
        title: 'Svar med en gang',
        desc: 'Bekreft at meldingen er mottatt. Ellers prøver plattformen igjen.',
      },
      {
        value: '5 min',
        title: 'Prediksjonsvindu',
        desc: 'Les materialene, tenk, og send rangeringen før tiden går ut.',
      },
    ],
    rulesTitle: 'Tre spilleregler',
    rules: [
      {
        title: 'Alltid lever noe',
        desc: 'Hopper du over, får du din egen snitt-score. Bedre å sende en forsiktig gjetning.',
      },
      {
        title: 'Bare rekkefølge teller',
        desc: 'Å skalere tallene om hjelper ikke. Det er plasseringen i køen som scoreres.',
      },
      {
        title: 'Konstant = null poeng',
        desc: 'Å alltid si «midt på treet» gir nøyaktig null. Du må skille selskapene.',
      },
    ],
  },
  research: {
    label: 'Forskning',
    title: 'Hva agenten lærer hver uke',
    subcopy:
      'Hver fredag skanner en cron arXiv. Det som kan forbedre agenten, lander her som korte kort.',
    stats: [
      { value: '12', label: 'Nye papers denne uken' },
      { value: '3', label: 'Beholdt for agenten' },
      { value: '9', label: 'Forkastet' },
    ],
    weekHeading: 'Uke 42 · 15. okt 2026',
    paper: {
      title: 'Tone versus tall i earnings calls',
      meta: 'arXiv:2610.22043 · 15. okt 2026',
      body: 'Tone-signal utover consensus kan flytte ΔR² når preview allerede er priset inn.',
    },
  },
} as const

export const CLICK_TARGETS = {
  agentClick: {
    label: 'agentClick' as const,
    selector: '[data-demo-nav-agent]',
    notes: 'Sidebar Agenten link.',
  },
  researchClick: {
    label: 'researchClick' as const,
    selector: '[data-demo-nav-research]',
    notes: 'Sidebar Forskning link.',
  },
} as const

export const CURSOR_RULES = {
  movementEase: 'power2.out',
  durationShort: { min: 0.4, max: 0.6 },
  durationLong: { min: 0.7, max: 1.0 },
  durationEnter: 1.0,
  clickScale: 0.88,
  clickScaleDuration: 0.08,
  rippleScale: 3.4,
  rippleDuration: 0.54,
  releaseEase: 'back.out(2.2)',
  releaseDelay: 0.09,
  releaseDuration: 0.16,
  startPosition: { x: 1350, y: -40 },
} as const

export const RESET_CHECKLIST: readonly ResetTarget[] = [
  {
    selector: '[data-demo-cursor]',
    properties: {
      x: CURSOR_RULES.startPosition.x,
      y: CURSOR_RULES.startPosition.y,
      scale: 1,
      autoAlpha: 0,
    },
  },
  {
    selector: '[data-demo-cursor-ripple]',
    properties: { scale: 0.2, autoAlpha: 0 },
  },
  {
    selector: '[data-demo-competition-panel]',
    properties: { autoAlpha: 1 },
  },
  {
    selector: '[data-demo-agent-panel]',
    properties: { autoAlpha: 0 },
  },
  {
    selector: '[data-demo-research-panel]',
    properties: { autoAlpha: 0 },
  },
] as const

export const DATA_ATTRIBUTES = {
  root: 'data-demo-root',
  sidebar: 'data-demo-sidebar',
  navCompetition: 'data-demo-nav-competition',
  navAgent: 'data-demo-nav-agent',
  navResearch: 'data-demo-nav-research',
  competitionPanel: 'data-demo-competition-panel',
  agentPanel: 'data-demo-agent-panel',
  researchPanel: 'data-demo-research-panel',
  competitionTitle: 'data-demo-competition-title',
  competitionIntro: 'data-demo-competition-intro',
  competitionMetrics: 'data-demo-competition-metrics',
  competitionQuestion: 'data-demo-competition-question',
  agentHeading: 'data-demo-agent-heading',
  agentClocks: 'data-demo-agent-clocks',
  agentRules: 'data-demo-agent-rules',
  researchHeading: 'data-demo-research-heading',
  researchStats: 'data-demo-research-stats',
  researchPaper: 'data-demo-research-paper',
  cursor: 'data-demo-cursor',
  cursorRipple: 'data-demo-cursor-ripple',
} as const
