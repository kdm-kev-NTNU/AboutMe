import { gsap } from 'gsap'
import {
  CLICK_TARGETS,
  CURSOR_RULES,
  DATA_ATTRIBUTES,
  FILM_META,
  RESET_CHECKLIST,
  SCENES,
  type KrisefikserNavTab,
} from './film-script'
import { getCenter, moveCursor, performClick } from '../../shared/film-animation-utils'
import type { SelectorFunc } from '../../shared/types'

const NAV_BASE =
  'flex items-center gap-1.5 text-sm font-medium text-slate-700'
const NAV_ACTIVE =
  'flex items-center gap-1.5 text-sm font-semibold text-blue-600'

const PANELS = [
  'landingPanel',
  'registrationPanel',
  'husstandPanel',
  'kriserPanel',
] as const

const LANDING_CHILDREN_SELECTOR = [
  DATA_ATTRIBUTES.headline,
  DATA_ATTRIBUTES.subcopy,
  DATA_ATTRIBUTES.komIGang,
  DATA_ATTRIBUTES.seKart,
  DATA_ATTRIBUTES.infoSection,
]
  .map((attr) => `[${attr}]`)
  .join(', ')

const CLICK_SELECTORS = {
  cursor: `[${DATA_ATTRIBUTES.cursor}]`,
  ripple: `[${DATA_ATTRIBUTES.cursorRipple}]`,
} as const

function sel(attr: keyof typeof DATA_ATTRIBUTES): string {
  return `[${DATA_ATTRIBUTES[attr]}]`
}

function sceneHold(label: (typeof SCENES)[number]['label']): number {
  return SCENES.find((s) => s.label === label)?.holdAfterSeconds ?? 0
}

function setNavActive(q: SelectorFunc, active: KrisefikserNavTab): void {
  const kriser = q(sel('navKriser'))[0] as HTMLElement | undefined
  const husstand = q(sel('navHusstand'))[0] as HTMLElement | undefined

  if (kriser) kriser.className = active === 'kriser' ? NAV_ACTIVE : NAV_BASE
  if (husstand) husstand.className = active === 'husstand' ? NAV_ACTIVE : NAV_BASE
}

function hideAllPanelsExcept(
  q: SelectorFunc,
  visible: (typeof PANELS)[number],
): void {
  for (const panel of PANELS) {
    gsap.set(q(sel(panel)), { autoAlpha: panel === visible ? 1 : 0 })
  }
}

function runReset(q: SelectorFunc): void {
  for (const item of RESET_CHECKLIST) {
    gsap.set(q(item.selector), item.properties)
  }

  gsap.set(q(LANDING_CHILDREN_SELECTOR), { autoAlpha: 0, y: 20 })
  gsap.set(q(sel('komIGang')), { scale: 1 })
  setNavActive(q, 'none')
}

export function applyReducedMotionFallback(root: HTMLElement): void {
  const q = gsap.utils.selector(root)
  runReset(q)

  gsap.set(q(LANDING_CHILDREN_SELECTOR), { autoAlpha: 1, y: 0 })
  gsap.set(q(sel('cursor')), { autoAlpha: 0 })
  hideAllPanelsExcept(q, 'landingPanel')
}

export function buildFilmTimeline(root: HTMLElement): gsap.core.Timeline {
  const q = gsap.utils.selector(root)
  const { frameWidth } = FILM_META

  const tl = gsap.timeline({
    repeat: FILM_META.timeline.repeat,
    repeatDelay: FILM_META.timeline.repeatDelay,
    defaults: FILM_META.timeline.defaults,
  })

  // Scene 0: reset
  tl.add(() => runReset(q), 0)

  // Scene 1: landingEnter
  tl.addLabel('landingEnter')
  tl.fromTo(
    q(LANDING_CHILDREN_SELECTOR),
    { autoAlpha: 0, y: 20 },
    { autoAlpha: 1, y: 0, stagger: 0.08, duration: 0.5 },
    'landingEnter',
  )
  tl.to({}, { duration: sceneHold('landingEnter') }, 'landingEnter+=0.6')

  // Scene 2: cursorToKomIGang
  const komIGangEl = q(CLICK_TARGETS.komIGangClick.selector)[0]
  tl.addLabel('cursorToKomIGang')
  if (komIGangEl) {
    moveCursor(
      tl,
      q,
      root,
      komIGangEl,
      frameWidth,
      CURSOR_RULES,
      CLICK_SELECTORS.cursor,
      'cursorToKomIGang',
      { entryDuration: CURSOR_RULES.durationEnter },
    )
  }

  // Scene 3: komIGangClick
  if (komIGangEl) {
    const center = getCenter(komIGangEl, root, frameWidth)
    performClick(
      tl,
      q,
      center.x,
      center.y,
      'komIGangClick',
      CURSOR_RULES,
      CLICK_SELECTORS,
    )
    tl.to(
      q(sel('komIGang')),
      { scale: 0.96, duration: 0.1, yoyo: true, repeat: 1, ease: 'power2.out' },
      'komIGangClick',
    )
    tl.to({}, { duration: sceneHold('komIGangClick') }, 'komIGangClick+=0.2')
  }

  // Scene 4: registrationReveal
  tl.addLabel('registrationReveal')
  tl.to(q(sel('landingPanel')), { autoAlpha: 0, duration: 0.28 }, 'registrationReveal')
  tl.to(
    q(sel('registrationPanel')),
    { autoAlpha: 1, duration: 0.32 },
    'registrationReveal+=0.08',
  )
  tl.to({}, { duration: sceneHold('registrationReveal') }, 'registrationReveal+=0.4')

  // Scene 5: cursorToHusstand
  const husstandEl = q(CLICK_TARGETS.husstandClick.selector)[0]
  tl.addLabel('cursorToHusstand')
  if (husstandEl) {
    moveCursor(
      tl,
      q,
      root,
      husstandEl,
      frameWidth,
      CURSOR_RULES,
      CLICK_SELECTORS.cursor,
      'cursorToHusstand',
    )
  }

  // Scene 6: husstandClick
  if (husstandEl) {
    const center = getCenter(husstandEl, root, frameWidth)
    performClick(
      tl,
      q,
      center.x,
      center.y,
      'husstandClick',
      CURSOR_RULES,
      CLICK_SELECTORS,
    )
    tl.add(() => setNavActive(q, 'husstand'), 'husstandClick')
    tl.to({}, { duration: sceneHold('husstandClick') }, 'husstandClick+=0.2')
  }

  // Scene 7: husstandReveal
  tl.addLabel('husstandReveal')
  tl.to(q(sel('registrationPanel')), { autoAlpha: 0, duration: 0.24 }, 'husstandReveal')
  tl.to(q(sel('husstandPanel')), { autoAlpha: 1, duration: 0.28 }, 'husstandReveal+=0.08')
  tl.to({}, { duration: sceneHold('husstandReveal') }, 'husstandReveal+=0.4')

  // Scene 8: cursorToKriser
  const kriserEl = q(CLICK_TARGETS.kriserClick.selector)[0]
  tl.addLabel('cursorToKriser')
  if (kriserEl) {
    moveCursor(
      tl,
      q,
      root,
      kriserEl,
      frameWidth,
      CURSOR_RULES,
      CLICK_SELECTORS.cursor,
      'cursorToKriser',
    )
  }

  // Scene 9: kriserClick
  if (kriserEl) {
    const center = getCenter(kriserEl, root, frameWidth)
    performClick(tl, q, center.x, center.y, 'kriserClick', CURSOR_RULES, CLICK_SELECTORS)
    tl.add(() => setNavActive(q, 'kriser'), 'kriserClick')
    tl.to({}, { duration: sceneHold('kriserClick') }, 'kriserClick+=0.2')
  }

  // Scene 10: kriserReveal
  tl.addLabel('kriserReveal')
  tl.to(q(sel('husstandPanel')), { autoAlpha: 0, duration: 0.24 }, 'kriserReveal')
  tl.to(q(sel('kriserPanel')), { autoAlpha: 1, duration: 0.28 }, 'kriserReveal+=0.08')
  tl.to({}, { duration: sceneHold('kriserReveal') }, 'kriserReveal+=0.4')

  // Scene 11: loopEnd
  tl.addLabel('loopEnd')
  tl.to({}, { duration: sceneHold('loopEnd') }, 'loopEnd')

  return tl
}
