import { gsap } from "gsap";
import {
  CLICK_TARGETS,
  COPY,
  CURSOR_RULES,
  DATA_ATTRIBUTES,
  FILM_META,
  RESET_CHECKLIST,
  SCENES,
} from './film-script'
import {
  createTypewriterTween,
  getCenter,
  killMicPulse,
  moveCursor,
  performClick,
  startMicPulse,
} from '../../shared/film-animation-utils'
import type { MicPulseRef, SelectorFunc } from '../../shared/types'

const NAV_HOME_ACTIVE =
  "relative z-10 flex w-44 items-center justify-center rounded-full border border-blue-200 bg-blue-50 py-2 text-sm font-semibold text-blue-700";
const NAV_INACTIVE =
  "relative z-10 flex w-44 items-center justify-center rounded-full py-2 text-sm font-medium text-gray-500";

const HERO_CHILDREN_SELECTOR = [
  DATA_ATTRIBUTES.statusPill,
  DATA_ATTRIBUTES.headline,
  DATA_ATTRIBUTES.subcopy,
  DATA_ATTRIBUTES.voiceButton,
  DATA_ATTRIBUTES.textButton,
  DATA_ATTRIBUTES.micOrb,
]
  .map((attr) => `[${attr}]`)
  .join(", ");

const CLICK_SELECTORS = {
  cursor: `[${DATA_ATTRIBUTES.cursor}]`,
  ripple: `[${DATA_ATTRIBUTES.cursorRipple}]`,
} as const;

function sel(attr: keyof typeof DATA_ATTRIBUTES): string {
  return `[${DATA_ATTRIBUTES[attr]}]`;
}

function sceneHold(label: (typeof SCENES)[number]["label"]): number {
  return SCENES.find((s) => s.label === label)?.holdAfterSeconds ?? 0;
}

function setNavHome(q: SelectorFunc): void {
  const home = q(sel("navHome"))[0] as HTMLElement | undefined;
  const reason = q(sel("navReason"))[0] as HTMLElement | undefined;
  const how = q(sel("navHow"))[0] as HTMLElement | undefined;
  if (home) home.className = NAV_HOME_ACTIVE;
  if (reason) reason.className = NAV_INACTIVE;
  if (how) how.className = NAV_INACTIVE;
}

function runReset(q: SelectorFunc, micPulseRef: MicPulseRef): void {
  killMicPulse(micPulseRef);

  for (const item of RESET_CHECKLIST) {
    const targets = q(item.selector);
    if (
      item.selector === "[data-demo-chat-user-msg]" ||
      item.selector === "[data-demo-chat-ai-msg]"
    ) {
      targets.forEach((el) => {
        (el as HTMLElement).textContent = "";
      });
      gsap.set(targets, item.properties);
      continue;
    }
    gsap.set(targets, item.properties);
  }

  gsap.set(q(HERO_CHILDREN_SELECTOR), { autoAlpha: 0, y: 20 });
  gsap.set(q(sel("micOrb")), { scale: 1 });
  gsap.set(q(sel("voiceMicBtn")), { scale: 1 });
  gsap.set(q(sel("cursor")), { autoAlpha: 0 });
  gsap.set(q(sel("chatSend")), { scale: 1 });
  gsap.set(q(sel("chatAiMsg")), { y: 12 });
  gsap.set(q(sel("chatUserMsg")), { y: 12 });
  q(sel("chatInput")).forEach((el) => {
    (el as HTMLInputElement).value = "";
  });

  setNavHome(q);
}

/** Static hero frame for prefers-reduced-motion — no cursor, no loop. */
export function applyReducedMotionFallback(root: HTMLElement): void {
  const q = gsap.utils.selector(root);
  const micPulseRef: MicPulseRef = { current: null };
  runReset(q, micPulseRef);

  gsap.set(q(HERO_CHILDREN_SELECTOR), { autoAlpha: 0, y: 0 });
  gsap.set(q(sel("statusPill")), { autoAlpha: 1 });
  gsap.set(q(sel("headline")), { autoAlpha: 1 });
  gsap.set(q(sel("subcopy")), { autoAlpha: 1 });
  gsap.set(q(sel("cursor")), { autoAlpha: 0 });
  gsap.set(q(sel("voicePanel")), { autoAlpha: 0 });
  gsap.set(q(sel("chatPanel")), { autoAlpha: 0 });
}

export function buildFilmTimeline(root: HTMLElement): gsap.core.Timeline {
  const q = gsap.utils.selector(root);
  const micPulseRef: MicPulseRef = { current: null };
  const { frameWidth } = FILM_META;

  const tl = gsap.timeline({
    repeat: FILM_META.timeline.repeat,
    repeatDelay: FILM_META.timeline.repeatDelay,
    defaults: FILM_META.timeline.defaults,
  });

  // Scene 0: reset (position 0, runs every loop)
  tl.add(() => runReset(q, micPulseRef), 0);

  // Scene 1: heroEnter
  tl.addLabel("heroEnter");
  tl.fromTo(
    q(HERO_CHILDREN_SELECTOR),
    { autoAlpha: 0, y: 20 },
    { autoAlpha: 1, y: 0, stagger: 0.07, duration: 0.55 },
    "heroEnter",
  );
  tl.add(
    () => startMicPulse(q, micPulseRef, sel("micOrb"), "idle"),
    "heroEnter+=0.3",
  );
  tl.to({}, { duration: sceneHold("heroEnter") }, "heroEnter+=0.9");

  // Scene 2: cursorToOrb
  const orbEl = q(CLICK_TARGETS.orbClick.selector)[0];
  tl.addLabel("cursorToOrb");
  if (orbEl) {
    moveCursor(
      tl,
      q,
      root,
      orbEl,
      frameWidth,
      CURSOR_RULES,
      CLICK_SELECTORS.cursor,
      "cursorToOrb",
      { entryDuration: CURSOR_RULES.durationEnter },
    );
  }

  // Scene 3: orbClick
  if (orbEl) {
    const orbCenter = getCenter(orbEl, root, frameWidth);
    performClick(
      tl,
      q,
      orbCenter.x,
      orbCenter.y,
      "orbClick",
      CURSOR_RULES,
      CLICK_SELECTORS,
    );
    tl.to(
      q(sel("micOrb")),
      { scale: 1.06, duration: 0.1, yoyo: true, repeat: 1, ease: "power2.out" },
      "orbClick",
    );
    tl.to({}, { duration: sceneHold("orbClick") }, "orbClick+=0.2");
  }

  // Scene 4: voiceReveal
  tl.addLabel("voiceReveal");
  tl.add(() => killMicPulse(micPulseRef), "voiceReveal");
  tl.to(q(sel("heroCard")), { autoAlpha: 0, duration: 0.24 }, "voiceReveal");
  tl.to(
    q(sel("voicePanel")),
    { autoAlpha: 1, duration: 0.28 },
    "voiceReveal+=0.08",
  );

  // Scene 5: cursorToVoiceMic
  const voiceMicEl = q(CLICK_TARGETS.voiceMicClick.selector)[0];
  tl.addLabel("cursorToVoiceMic");
  if (voiceMicEl) {
    moveCursor(
      tl,
      q,
      root,
      voiceMicEl,
      frameWidth,
      CURSOR_RULES,
      CLICK_SELECTORS.cursor,
      "cursorToVoiceMic",
    );
  }

  // Scene 6: voiceMicClick
  if (voiceMicEl) {
    const voiceMicCenter = getCenter(voiceMicEl, root, frameWidth);
    performClick(
      tl,
      q,
      voiceMicCenter.x,
      voiceMicCenter.y,
      "voiceMicClick",
      CURSOR_RULES,
      CLICK_SELECTORS,
    );
    tl.to(
      q(sel("voiceMicBtn")),
      { scale: 1.06, duration: 0.1, yoyo: true, repeat: 1, ease: "power2.out" },
      "voiceMicClick",
    );
    tl.to({}, { duration: sceneHold("voiceMicClick") }, "voiceMicClick+=0.2");
  }

  // Scene 7: voiceListening
  tl.addLabel("voiceListening");
  tl.fromTo(
    q(sel("voiceStatus")),
    { autoAlpha: 0, y: 8 },
    { autoAlpha: 1, y: 0, duration: 0.3 },
    "voiceListening",
  );
  tl.add(
    () => startMicPulse(q, micPulseRef, sel("voiceMicBtn"), "intense"),
    "voiceListening",
  );
  tl.to({}, { duration: sceneHold("voiceListening") }, "voiceListening+=0.3");

  // Scene 6: cursorToTextChat
  const textChatEl = q(CLICK_TARGETS.textChatClick.selector)[0];
  tl.addLabel("cursorToTextChat");
  if (textChatEl) {
    moveCursor(
      tl,
      q,
      root,
      textChatEl,
      frameWidth,
      CURSOR_RULES,
      CLICK_SELECTORS.cursor,
      "cursorToTextChat",
    );
  }

  // Scene 7: textChatClick
  if (textChatEl) {
    const linkCenter = getCenter(textChatEl, root, frameWidth);
    performClick(
      tl,
      q,
      linkCenter.x,
      linkCenter.y,
      "textChatClick",
      CURSOR_RULES,
      CLICK_SELECTORS,
    );
    tl.to({}, { duration: sceneHold("textChatClick") }, "textChatClick+=0.2");
  }

  // Scene 10: chatReveal
  tl.addLabel("chatReveal");
  tl.add(() => killMicPulse(micPulseRef), "chatReveal");
  tl.to(q(sel("voicePanel")), { autoAlpha: 0, duration: 0.24 }, "chatReveal");
  tl.to(q(sel("voiceStatus")), { autoAlpha: 0, duration: 0.2 }, "chatReveal");
  tl.to(q(sel("langToggle")), { autoAlpha: 0, duration: 0.24 }, "chatReveal");
  tl.to(q(sel("social")), { autoAlpha: 0, duration: 0.24 }, "chatReveal");
  tl.to(
    q(sel("chatPanel")),
    { autoAlpha: 1, duration: 0.28 },
    "chatReveal+=0.08",
  );

  // Scene 9: chatTypeQuestion
  tl.addLabel("chatTypeQuestion");
  const userMsgEl = q(sel("chatUserMsg"))[0];
  const chatInputEl = q(sel("chatInput"))[0] as HTMLInputElement | undefined;
  const sendEl = q(CLICK_TARGETS.sendClick.selector)[0];
  const typingCharRate = 0.05;
  const typingDuration = COPY.chat.userQuestion.length * typingCharRate;
  if (chatInputEl) {
    const { proxy, vars } = createTypewriterTween(
      COPY.chat.userQuestion,
      typingCharRate,
      (slice) => {
        chatInputEl.value = slice;
      },
    );
    tl.to(proxy, vars, "chatTypeQuestion");
  }
  tl.to(
    {},
    { duration: sceneHold("chatTypeQuestion") },
    `chatTypeQuestion+=${typingDuration}`,
  );

  // Scene 10: cursorToSend
  tl.addLabel("cursorToSend");
  if (sendEl) {
    moveCursor(
      tl,
      q,
      root,
      sendEl,
      frameWidth,
      CURSOR_RULES,
      CLICK_SELECTORS.cursor,
      "cursorToSend",
    );
  }

  // Scene 11: chatSend
  if (sendEl) {
    const sendCenter = getCenter(sendEl, root, frameWidth);
    performClick(
      tl,
      q,
      sendCenter.x,
      sendCenter.y,
      "chatSend",
      CURSOR_RULES,
      CLICK_SELECTORS,
    );
    tl.call(
      () => {
        if (userMsgEl)
          (userMsgEl as HTMLElement).textContent = COPY.chat.userQuestion;
        if (chatInputEl) chatInputEl.value = "";
      },
      undefined,
      "chatSend+=0.1",
    );
    tl.fromTo(
      q(sel("chatUserMsg")),
      { autoAlpha: 0, y: 12 },
      { autoAlpha: 1, y: 0, duration: 0.28 },
      "chatSend+=0.1",
    );
    tl.to(
      q(sel("chatEmpty")),
      { autoAlpha: 0, duration: 0.2 },
      "chatSend+=0.1",
    );
    tl.to({}, { duration: sceneHold("chatSend") }, "chatSend+=0.3");
  }

  // Scene: chatThinking
  tl.addLabel("chatThinking");
  tl.to(q(sel("chatThinking")), { autoAlpha: 1, duration: 0.2 }, "chatThinking");
  tl.to({}, { duration: sceneHold("chatThinking") }, "chatThinking+=0.2");
  tl.to(
    q(sel("chatThinking")),
    { autoAlpha: 0, duration: 0.15 },
    `chatThinking+=${0.2 + sceneHold("chatThinking")}`,
  );

  // Scene: chatAiReply
  tl.addLabel("chatAiReply");
  const aiMsgEl = q(sel("chatAiMsg"))[0];
  const aiCharRate = 0.05;
  const aiStreamDuration = COPY.chat.aiReply.length * aiCharRate;
  tl.set(q(sel("chatAiMsg")), { autoAlpha: 1, y: 0 }, "chatAiReply");
  if (aiMsgEl) {
    const { proxy, vars } = createTypewriterTween(
      COPY.chat.aiReply,
      aiCharRate,
      (slice) => {
        (aiMsgEl as HTMLElement).textContent = slice;
      },
    );
    tl.to(proxy, vars, "chatAiReply");
  }
  tl.to(
    {},
    { duration: sceneHold("chatAiReply") },
    `chatAiReply+=${aiStreamDuration}`,
  );

  // Scene 13: loopEnd
  tl.addLabel("loopEnd");

  runReset(q, micPulseRef);

  return tl;
}
