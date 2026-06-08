export type KrisefikserLabel =
  | 'reset'
  | 'landingEnter'
  | 'cursorToKomIGang'
  | 'komIGangClick'
  | 'registrationReveal'
  | 'cursorToHusstand'
  | 'husstandClick'
  | 'husstandReveal'
  | 'cursorToKriser'
  | 'kriserClick'
  | 'kriserReveal'
  | 'loopEnd'

export type KrisefikserNavTab = 'none' | 'kriser' | 'husstand'

export interface KrisefikserScene {
  order: number
  label: KrisefikserLabel
  description: string
  holdAfterSeconds: number | null
  activeNav: KrisefikserNavTab
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

export const SCENES: readonly KrisefikserScene[] = [
  {
    order: 0,
    label: 'reset',
    description: 'Restore every animated property.',
    holdAfterSeconds: null,
    activeNav: 'none',
  },
  {
    order: 1,
    label: 'landingEnter',
    description: 'Landing page hero and info cards stagger in.',
    holdAfterSeconds: 1.2,
    activeNav: 'none',
  },
  {
    order: 2,
    label: 'cursorToKomIGang',
    description: 'Cursor moves to Kom i gang nå button.',
    holdAfterSeconds: null,
    activeNav: 'none',
  },
  {
    order: 3,
    label: 'komIGangClick',
    description: 'Click on Kom i gang nå.',
    holdAfterSeconds: 0.3,
    activeNav: 'none',
  },
  {
    order: 4,
    label: 'registrationReveal',
    description: 'Crossfade to registration form.',
    holdAfterSeconds: 0.8,
    activeNav: 'none',
  },
  {
    order: 5,
    label: 'cursorToHusstand',
    description: 'Cursor moves to Husstand nav link.',
    holdAfterSeconds: null,
    activeNav: 'none',
  },
  {
    order: 6,
    label: 'husstandClick',
    description: 'Click Husstand in navbar.',
    holdAfterSeconds: 0.3,
    activeNav: 'husstand',
  },
  {
    order: 7,
    label: 'husstandReveal',
    description: 'Crossfade to husstand page.',
    holdAfterSeconds: 1.0,
    activeNav: 'husstand',
  },
  {
    order: 8,
    label: 'cursorToKriser',
    description: 'Cursor moves to Kriser nav link.',
    holdAfterSeconds: null,
    activeNav: 'husstand',
  },
  {
    order: 9,
    label: 'kriserClick',
    description: 'Click Kriser in navbar.',
    holdAfterSeconds: 0.3,
    activeNav: 'kriser',
  },
  {
    order: 10,
    label: 'kriserReveal',
    description: 'Crossfade to kriser page.',
    holdAfterSeconds: 1.5,
    activeNav: 'kriser',
  },
  {
    order: 11,
    label: 'loopEnd',
    description: 'Hold before loop restart.',
    holdAfterSeconds: 1.2,
    activeNav: 'kriser',
  },
] as const

export const COPY = {
  brand: 'Krisefikser.app',
  nav: {
    kart: 'Kart',
    kriser: 'Kriser',
    husstand: 'Husstand',
    login: 'Logg inn',
  },
  landing: {
    headline: 'Vær forberedt når krisen rammer',
    subcopy:
      'Krisefikser.app hjelper deg å øke din egenberedskap og gir deg tilgang til viktig informasjon før, under og etter en krise.',
    ctaPrimary: 'Kom i gang nå',
    ctaSecondary: 'Se kart',
    infoTitle: 'Generell kriseinformasjon',
    infoSubcopy:
      'Her kan du lære hva du bør gjøre før, under og etter en krise. Test din kunnskap med interaktive quizer.',
    cards: [
      {
        title: 'Før krisen',
        badge: 'Inneholder quiz!',
        icon: 'plus',
        color: 'blue',
      },
      {
        title: 'Under krisen',
        badge: 'Inneholder quiz!',
        icon: 'warning',
        color: 'yellow',
      },
      {
        title: 'Etter krisen',
        badge: 'Inneholder quiz!',
        icon: 'check',
        color: 'green',
      },
    ],
  },
  registration: {
    title: 'Registrer deg',
    firstName: 'Ola',
    lastName: 'Nordmann',
    email: 'clozet.adm.demo@gmail.com',
    password: '••••••••',
    privacy: 'Jeg godtar personvernerklæringen',
    verifying: 'Verifying...',
  },
  husstand: {
    title: 'Kom i gang med husstand',
    subcopy:
      'Husstander hjelper deg å organisere beredskapslageret ditt og planlegge møtepunkter for familien din i en krisesituasjon.',
    createTitle: 'Opprett en husstand',
    createDesc:
      'Start en ny husstand for deg og din familie. Du kan invitere andre medlemmer til å delta og samarbeide om beredskapslageret ditt.',
    createCta: 'Registrer deg for å opprette',
    joinTitle: 'Bli med i en husstand',
    joinDesc:
      'Har du blitt invitert? For å bli med i en eksisterende husstand må du motta en invitasjon. Se invitasjonen på din profil.',
    joinCta: 'Logg inn',
    benefitsTitle: 'Fordeler med en husstand',
    benefits: [
      { title: 'Samarbeid', desc: 'Del beredskapsplaner med familien' },
      { title: 'Organisering', desc: 'Hold oversikt over lageret' },
      { title: 'Møtepunkter', desc: 'Planlegg trygge møteplasser' },
    ],
  },
  kriser: {
    title: 'Kriser og hendelser',
    subcopy:
      'Her finner du en oversikt over kommende, pågående og avsluttede kriser og hendelser. Hold deg oppdatert på situasjonen i ditt område.',
    reflections: 'Offentlige Refleksjoner',
    tabs: ['Alle hendelser', 'Pågående', 'Kommende', 'Avsluttede'],
    events: [
      {
        title: 'Temporary Shelter',
        status: 'Pågående',
        statusColor: 'red',
        desc: 'Temporary accommodation available for displaced residents.',
        date: 'Startet: 21. januar 1970 kl. 14:28',
      },
      {
        title: 'Disaster Relief Center',
        status: 'Kommende',
        statusColor: 'blue',
        desc: 'Distribution point for emergency supplies and assistance.',
        date: 'Starter: 21. januar 1970 kl. 14:39',
      },
      {
        title: 'Traffic Accident',
        status: 'Pågående',
        statusColor: 'red',
        desc: 'Major traffic disruption. Please use alternative routes.',
        date: 'Startet: 21. januar 1970 kl. 14:29',
      },
    ],
    mapTitle: 'Krisekart',
    mapCta: 'Se fullstendig kart',
    mapLink: 'Åpne detaljert kart',
  },
} as const

export const CLICK_TARGETS = {
  komIGangClick: {
    label: 'komIGangClick' as const,
    selector: '[data-demo-kom-i-gang]',
    notes: 'Landing hero primary CTA.',
  },
  husstandClick: {
    label: 'husstandClick' as const,
    selector: '[data-demo-nav-husstand]',
    notes: 'Navbar Husstand link.',
  },
  kriserClick: {
    label: 'kriserClick' as const,
    selector: '[data-demo-nav-kriser]',
    notes: 'Navbar Kriser link.',
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
    selector: '[data-demo-landing-panel]',
    properties: { autoAlpha: 1 },
  },
  {
    selector: '[data-demo-registration-panel]',
    properties: { autoAlpha: 0 },
  },
  {
    selector: '[data-demo-husstand-panel]',
    properties: { autoAlpha: 0 },
  },
  {
    selector: '[data-demo-kriser-panel]',
    properties: { autoAlpha: 0 },
  },
  {
    selector: '[data-demo-kom-i-gang]',
    properties: { scale: 1 },
  },
] as const

export const DATA_ATTRIBUTES = {
  root: 'data-demo-root',
  nav: 'data-demo-nav',
  navKriser: 'data-demo-nav-kriser',
  navHusstand: 'data-demo-nav-husstand',
  loginBtn: 'data-demo-login-btn',
  landingPanel: 'data-demo-landing-panel',
  registrationPanel: 'data-demo-registration-panel',
  husstandPanel: 'data-demo-husstand-panel',
  kriserPanel: 'data-demo-kriser-panel',
  heroSection: 'data-demo-hero-section',
  headline: 'data-demo-headline',
  subcopy: 'data-demo-subcopy',
  komIGang: 'data-demo-kom-i-gang',
  seKart: 'data-demo-se-kart',
  infoSection: 'data-demo-info-section',
  infoCards: 'data-demo-info-cards',
  cursor: 'data-demo-cursor',
  cursorRipple: 'data-demo-cursor-ripple',
} as const
