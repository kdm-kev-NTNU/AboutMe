import { gsap } from 'gsap'
import {
  CLICK_TARGETS,
  CURSOR_RULES,
  DATA_ATTRIBUTES,
  FILM_META,
  RESET_CHECKLIST,
  SCENES,
  type MarketsNavTab,
} from './film-script'
import { getCenter, moveCursor, performClick } from '../../shared/film-animation-utils'
import type { SelectorFunc } from '../../shared/types'

const NAV_BASE =
  'flex items-center gap-2 rounded-lg px-3 py-2.5 text-sm font-medium text-slate-700'
const NAV_ACTIVE =
  'flex items-center gap-2 rounded-lg bg-sky-50 px-3 py-2.5 text-sm font-semibold text-sky-800'

const PANELS = ['competitionPanel', 'agentPanel', 'researchPanel'] as const

const COMPETITION_CHILDREN_SELECTOR = [
  DATA_ATTRIBUTES.competitionTitle,
  DATA_ATTRIBUTES.competitionIntro,
  DATA_ATTRIBUTES.competitionMetrics,
  DATA_ATTRIBUTES.competitionQuestion,
]
  .map((attr) => `[${attr}]`)
  .join(', ')

const AGENT_CHILDREN_SELECTOR = [
  DATA_ATTRIBUTES.agentHeading,
  DATA_ATTRIBUTES.agentClocks,
  DATA_ATTRIBUTES.agentRules,
]
  .map((attr) => `[${attr}]`)
  .join(', ')

const RESEARCH_CHILDREN_SELECTOR = [
  DATA_ATTRIBUTES.researchHeading,
  DATA_ATTRIBUTES.researchStats,
  DATA_ATTRIBUTES.researchPaper,
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

function setNavActive(q: SelectorFunc, active: MarketsNavTab): void {
  const competition = q(sel('navCompetition'))[0] as HTMLElement | undefined
  const agent = q(sel('navAgent'))[0] as HTMLElement | undefined
  const research = q(sel('navResearch'))[0] as HTMLElement | undefined

  if (competition) {
    competition.className = active === 'competition' ? NAV_ACTIVE : NAV_BASE
  }
  if (agent) {
    agent.className = active === 'agent' ? NAV_ACTIVE : NAV_BASE
  }
  if (research) {
    research.className = active === 'research' ? NAV_ACTIVE : NAV_BASE
  }
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

  gsap.set(q(COMPETITION_CHILDREN_SELECTOR), { autoAlpha: 0, y: 20 })
  gsap.set(q(AGENT_CHILDREN_SELECTOR), { autoAlpha: 1, y: 0 })
  gsap.set(q(RESEARCH_CHILDREN_SELECTOR), { autoAlpha: 1, y: 0 })
  setNavActive(q, 'competition')
}

export function applyReducedMotionFallback(root: HTMLElement): void {
  const q = gsap.utils.selector(root)
  runReset(q)

  gsap.set(q(COMPETITION_CHILDREN_SELECTOR), { autoAlpha: 1, y: 0 })
  gsap.set(q(sel('cursor')), { autoAlpha: 0 })
  hideAllPanelsExcept(q, 'competitionPanel')
  setNavActive(q, 'competition')
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

  // Scene 1: competitionEnter
  tl.addLabel('competitionEnter')
  tl.fromTo(
    q(COMPETITION_CHILDREN_SELECTOR),
    { autoAlpha: 0, y: 20 },
    { autoAlpha: 1, y: 0, stagger: 0.08, duration: 0.5 },
    'competitionEnter',
  )
  tl.to({}, { duration: sceneHold('competitionEnter') }, 'competitionEnter+=0.6')

  // Scene 2: cursorToAgent
  const agentEl = q(CLICK_TARGETS.agentClick.selector)[0]
  tl.addLabel('cursorToAgent')
  if (agentEl) {
    moveCursor(
      tl,
      q,
      root,
      agentEl,
      frameWidth,
      CURSOR_RULES,
      CLICK_SELECTORS.cursor,
      'cursorToAgent',
      { entryDuration: CURSOR_RULES.durationEnter },
    )
  }

  // Scene 3: agentClick
  if (agentEl) {
    const center = getCenter(agentEl, root, frameWidth)
    performClick(
      tl,
      q,
      center.x,
      center.y,
      'agentClick',
      CURSOR_RULES,
      CLICK_SELECTORS,
    )
    tl.add(() => setNavActive(q, 'agent'), 'agentClick')
    tl.to({}, { duration: sceneHold('agentClick') }, 'agentClick+=0.2')
  }

  // Scene 4: agentReveal
  tl.addLabel('agentReveal')
  tl.to(q(sel('competitionPanel')), { autoAlpha: 0, duration: 0.28 }, 'agentReveal')
  tl.fromTo(
    q(AGENT_CHILDREN_SELECTOR),
    { autoAlpha: 0, y: 16 },
    { autoAlpha: 1, y: 0, stagger: 0.06, duration: 0.4 },
    'agentReveal+=0.08',
  )
  tl.to(q(sel('agentPanel')), { autoAlpha: 1, duration: 0.32 }, 'agentReveal+=0.08')
  tl.to({}, { duration: sceneHold('agentReveal') }, 'agentReveal+=0.5')

  // Scene 5: cursorToResearch
  const researchEl = q(CLICK_TARGETS.researchClick.selector)[0]
  tl.addLabel('cursorToResearch')
  if (researchEl) {
    moveCursor(
      tl,
      q,
      root,
      researchEl,
      frameWidth,
      CURSOR_RULES,
      CLICK_SELECTORS.cursor,
      'cursorToResearch',
    )
  }

  // Scene 6: researchClick
  if (researchEl) {
    const center = getCenter(researchEl, root, frameWidth)
    performClick(
      tl,
      q,
      center.x,
      center.y,
      'researchClick',
      CURSOR_RULES,
      CLICK_SELECTORS,
    )
    tl.add(() => setNavActive(q, 'research'), 'researchClick')
    tl.to({}, { duration: sceneHold('researchClick') }, 'researchClick+=0.2')
  }

  // Scene 7: researchReveal
  tl.addLabel('researchReveal')
  tl.to(q(sel('agentPanel')), { autoAlpha: 0, duration: 0.24 }, 'researchReveal')
  tl.fromTo(
    q(RESEARCH_CHILDREN_SELECTOR),
    { autoAlpha: 0, y: 16 },
    { autoAlpha: 1, y: 0, stagger: 0.06, duration: 0.4 },
    'researchReveal+=0.08',
  )
  tl.to(q(sel('researchPanel')), { autoAlpha: 1, duration: 0.28 }, 'researchReveal+=0.08')
  tl.to({}, { duration: sceneHold('researchReveal') }, 'researchReveal+=0.5')

  // Scene 8: loopEnd
  tl.addLabel('loopEnd')
  tl.to({}, { duration: sceneHold('loopEnd') }, 'loopEnd')

  return tl
}
