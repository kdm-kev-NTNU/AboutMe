import type { FilmLabel, FilmScene, NavTab, ResetTarget } from '../../shared/types'

export type { FilmLabel, FilmScene, NavTab, ResetTarget }

export const FILM_META = {
  frameWidth: 1280,
  frameHeight: 800,
  language: "en" as const,
  timeline: {
    repeat: -1,
    repeatDelay: 1.2,
    defaults: { ease: "power3.out" },
  },
} as const;

export const SCENES: readonly FilmScene[] = [
  {
    order: 0,
    label: "reset",
    description: "Restore every animated property (see RESET_CHECKLIST).",
    instructions: [
      "Register tl.add(() => { ... }, 0) before any other tweens.",
      "Loop RESET_CHECKLIST and gsap.set each selector/properties pair.",
      "Set nav Home active via classList on [data-demo-nav-home] (not GSAP).",
      "Kill or reset any nested mic pulse tween so loop 2 does not stack pulses.",
    ],
    holdAfterSeconds: null,
    activeNav: "home",
  },
  {
    order: 1,
    label: "heroEnter",
    description:
      "Full chrome visible; hero card and inner content stagger in; mic orb starts idle pulse.",
    instructions: [
      "Chrome (nav, lang, social, footer) can be visible from first paint; animate hero card + children.",
      "Stagger [data-demo-status-pill], [data-demo-headline], [data-demo-subcopy], buttons, [data-demo-mic-orb]: autoAlpha 0→1, y 20→0, stagger ~0.07s.",
      "Start idle mic pulse on orb (subtle scale or ring opacity loop) continuing into later scenes until voice intensifies.",
      'tl.addLabel("heroEnter"); after animations, tl.to({}, { duration: 1.2 }) for hold.',
    ],
    holdAfterSeconds: 1.2,
    activeNav: "home",
  },
  {
    order: 2,
    label: "cursorToOrb",
    description: "Fake cursor decelerates to mic orb center.",
    instructions: [
      "Ensure [data-demo-cursor] autoAlpha 1 if hidden during reset.",
      "Measure orb center relative to root; tween cursor x/y with CURSOR_RULES.movementEase.",
      "Duration: map distance to CURSOR_RULES.durationShort or durationLong.",
      'tl.addLabel("cursorToOrb").',
    ],
    holdAfterSeconds: null,
    activeNav: "home",
  },
  {
    order: 3,
    label: "orbClick",
    description: "Click squeeze and ripple on orb.",
    instructions: [
      'Call click helper at orb center with label "orbClick" (see CURSOR_RULES).',
      "Optional: brief scale bump on [data-demo-mic-orb].",
      "Hold 0.3s after click before voiceReveal.",
    ],
    holdAfterSeconds: 0.3,
    activeNav: "home",
  },
  {
    order: 4,
    label: "voiceReveal",
    description:
      "Cross-fade hero card content to fake voice panel; nav stays Home (instant).",
    instructions: [
      'tl.addLabel("voiceReveal").',
      "Fade out hero inner content or entire [data-demo-hero-card] autoAlpha 0 (~0.24s).",
      "Fade in [data-demo-voice-panel] autoAlpha 1 (~0.28s), offset slightly after hero fade.",
      "Nav stays Home — no animation.",
    ],
    holdAfterSeconds: null,
    activeNav: "home",
  },
  {
    order: 5,
    label: "cursorToVoiceMic",
    description: "Cursor moves to voice panel blue mic button.",
    instructions: [
      "Target CLICK_TARGETS.voiceMicClick.selector ([data-demo-voice-mic-btn]).",
      "Move cursor to mic center; same movement rules as cursorToOrb.",
      'tl.addLabel("cursorToVoiceMic").',
    ],
    holdAfterSeconds: null,
    activeNav: "home",
  },
  {
    order: 6,
    label: "voiceMicClick",
    description: "Click on voice panel mic to start listening.",
    instructions: [
      'Call click helper at voice mic center with label "voiceMicClick".',
      "Optional: brief scale bump on [data-demo-voice-mic-btn].",
      "Hold 0.3s after click before voiceListening.",
    ],
    holdAfterSeconds: 0.3,
    activeNav: "home",
  },
  {
    order: 7,
    label: "voiceListening",
    description:
      "Status badge Listening…; voice mic pulse intensifies; optional subtle ring animation.",
    instructions: [
      'tl.addLabel("voiceListening").',
      "Show [data-demo-voice-status] with COPY.voice.statusListening, autoAlpha 0→1.",
      "Show voice panel mic/control in Live state (COPY.voice.liveLabel).",
      "Intensify voice mic pulse; optional expanding ring on voice panel mic.",
      "Hold 1.5s so viewer registers listening state.",
    ],
    holdAfterSeconds: 1.5,
    activeNav: "home",
  },
  {
    order: 8,
    label: "cursorToTextChat",
    description: "Cursor moves to voice panel link Use text chat instead.",
    instructions: [
      "Target CLICK_TARGETS.textChatClick.selector ([data-demo-text-chat-cta]).",
      "Move cursor to link center; same movement rules as cursorToOrb.",
      'tl.addLabel("cursorToTextChat").',
    ],
    holdAfterSeconds: null,
    activeNav: "home",
  },
  {
    order: 9,
    label: "textChatClick",
    description: "Click on text chat CTA.",
    instructions: [
      'Click helper at link position with label "textChatClick".',
      "Hold 0.3s before chatReveal.",
    ],
    holdAfterSeconds: 0.3,
    activeNav: "home",
  },
  {
    order: 10,
    label: "chatReveal",
    description:
      "Cross-fade voice panel to fake chat panel; nav stays Home (instant).",
    instructions: [
      'tl.addLabel("chatReveal").',
      "Hide [data-demo-voice-panel] autoAlpha 0; hide [data-demo-voice-status].",
      "Show [data-demo-chat-panel] autoAlpha 1.",
      "Nav stays Home.",
    ],
    holdAfterSeconds: null,
    activeNav: "home",
  },
  {
    order: 11,
    label: "chatTypeQuestion",
    description:
      "Type user question into input only; user bubble stays hidden.",
    instructions: [
      'tl.addLabel("chatTypeQuestion").',
      "Type COPY.chat.userQuestion into [data-demo-chat-input] only.",
      'Use ease "none" for typing; ~0.04–0.06s per character.',
      "Hold 0.4s after typing completes.",
    ],
    holdAfterSeconds: 0.4,
    activeNav: "home",
  },
  {
    order: 12,
    label: "cursorToSend",
    description: "Cursor moves to chat Send button.",
    instructions: [
      "Target CLICK_TARGETS.sendClick.selector ([data-demo-chat-send]).",
      "Move cursor to button center; same movement rules as cursorToOrb.",
      'tl.addLabel("cursorToSend").',
    ],
    holdAfterSeconds: null,
    activeNav: "home",
  },
  {
    order: 13,
    label: "chatSend",
    description:
      "Cursor clicks Send; user message posts to bubble; input clears.",
    instructions: [
      'tl.addLabel("chatSend").',
      "performClick at Send button center.",
      "Copy COPY.chat.userQuestion to [data-demo-chat-user-msg]; clear [data-demo-chat-input].",
      "Reveal user bubble autoAlpha 0→1.",
      "Hold 0.2s after send.",
    ],
    holdAfterSeconds: 0.2,
    activeNav: "home",
  },
  {
    order: 14,
    label: "chatThinking",
    description: "Three-dot thinking indicator after user sends.",
    instructions: [
      'tl.addLabel("chatThinking").',
      "Show [data-demo-chat-thinking] autoAlpha 0→1.",
      "Hold ~2s so viewer sees bouncing dots.",
      "Hide [data-demo-chat-thinking] autoAlpha 0 before chatAiReply.",
    ],
    holdAfterSeconds: 2,
    activeNav: "home",
  },
  {
    order: 15,
    label: "chatAiReply",
    description: "AI reply streams in char-by-char (ease: none).",
    instructions: [
      'tl.addLabel("chatAiReply").',
      "Show [data-demo-chat-ai-msg] autoAlpha 1.",
      'Stream COPY.chat.aiReply into [data-demo-chat-ai-msg] with ease "none"; ~0.05s per character.',
      "Hold 1.5s on final frame.",
    ],
    holdAfterSeconds: 1.5,
    activeNav: "home",
  },
  {
    order: 16,
    label: "loopEnd",
    description: "Timeline ends; repeatDelay pause; reset runs at position 0.",
    instructions: [
      'tl.addLabel("loopEnd").',
      "No extra tweens required — FILM_META.timeline.repeatDelay handles pause.",
      "On repeat, reset at position 0 runs again; verify no property drift.",
    ],
    holdAfterSeconds: 1.2,
    activeNav: "home",
  },
] as const;

export const LABELS = SCENES.map((scene) => scene.label);

export const COPY = {
  hero: {
    statusPill: "Voice is available",
    headline: "Talk with Kevin's AI first.",
    subcopy:
      "Talk with an AI that knows my portfolio and can answer questions about projects, experience, and tech.",
    voiceButton: "Start voice chat",
    textButton: "Use text chat",
  },
  nav: {
    home: "Home",
    reason: "Experience and education",
    how: "How",
  },
  admin: "Admin",
  lang: {
    en: "EN",
    no: "NO",
    active: "en" as const,
  },
  footer: {
    privacy: "Privacy Policy",
    cookies: "Cookie Settings",
    accessibility: "Accessibility",
  },
  voice: {
    title: "Talk with Kevin's AI",
    textChatCta: "Use text chat instead",
    statusListening: "Listening…",
    connectLabel: "Start live voice",
    liveLabel: "Live",
    modelLabel: "Provider/model",
    voiceLabel: "Voice",
    reasoningLabel: "Reasoning",
    modelValue: "OpenAI GPT-Realtime-2",
    voiceValue: "Marin",
    reasoningValue: "Fast",
  },
  chat: {
    title: "Kevin's AI",
    badgeOnline: "Online",
    voiceChatButton: "Voice chat",
    disclaimer: "You are chatting with an AI assistant",
    disclaimerSub:
      "Replies are generated by a language model, not Kevin in person. They may be inaccurate or out of date.",
    disclaimerLink: "Read more in the privacy policy",
    emptyState: "No messages yet",
    providerLabel: "AI provider",
    providerOpenAI: "OpenAI",
    providerAnthropic: "Anthropic",
    modelLabel: "Model",
    modelValue: "GPT-5.4 mini (OPENAI) (Fast)",
    inputPlaceholder: "Ask Kevin's AI anything…",
    sendButton: "Send",
    userQuestion: "What are you working on lately?",
    aiReply:
      "Recently I've focused on realtime voice AI and making portfolio interactions feel more personal.",
  },
} as const;

export const CLICK_TARGETS = {
  orbClick: {
    label: "orbClick" as const,
    selector: "[data-demo-mic-orb]",
    notes: "Hero right-side orb.",
  },
  voiceMicClick: {
    label: "voiceMicClick" as const,
    selector: "[data-demo-voice-mic-btn]",
    notes: "Voice panel blue mic circle.",
  },
  textChatClick: {
    label: "textChatClick" as const,
    selector: "[data-demo-text-chat-cta]",
    notes: "Voice panel link, not hero button.",
  },
  sendClick: {
    label: "sendClick" as const,
    selector: "[data-demo-chat-send]",
    notes: "Chat panel Send button.",
  },
} as const;

export const NAV_STATES: Record<FilmLabel, NavTab> = {
  reset: "home",
  heroEnter: "home",
  cursorToOrb: "home",
  orbClick: "home",
  voiceReveal: "home",
  cursorToVoiceMic: "home",
  voiceMicClick: "home",
  voiceListening: "home",
  cursorToTextChat: "home",
  textChatClick: "home",
  chatReveal: "home",
  chatTypeQuestion: "home",
  cursorToSend: "home",
  chatSend: "home",
  chatThinking: "home",
  chatAiReply: "home",
  loopEnd: "home",
};

export const CURSOR_RULES = {
  movementEase: "power2.out",
  durationShort: { min: 0.4, max: 0.6 },
  durationLong: { min: 0.7, max: 1.0 },
  durationEnter: 1.0,
  clickScale: 0.88,
  clickScaleDuration: 0.08,
  rippleScale: 3.4,
  rippleDuration: 0.54,
  releaseEase: "back.out(2.2)",
  releaseDelay: 0.09,
  releaseDuration: 0.16,
  startPosition: { x: 1350, y: -40 },
} as const;

export const RESET_CHECKLIST: readonly ResetTarget[] = [
  {
    selector: "[data-demo-cursor]",
    properties: {
      x: CURSOR_RULES.startPosition.x,
      y: CURSOR_RULES.startPosition.y,
      scale: 1,
    },
    notes: "Tune x/y during markup.",
  },
  {
    selector: "[data-demo-hero-card]",
    properties: { autoAlpha: 1 },
  },
  {
    selector: "[data-demo-voice-panel]",
    properties: { autoAlpha: 0 },
  },
  {
    selector: "[data-demo-chat-panel]",
    properties: { autoAlpha: 0 },
  },
  {
    selector: "[data-demo-voice-status]",
    properties: { autoAlpha: 0 },
  },
  {
    selector: "[data-demo-chat-user-msg]",
    properties: { textContent: "", autoAlpha: 0, y: 12 },
  },
  {
    selector: "[data-demo-chat-ai-msg]",
    properties: { textContent: "", autoAlpha: 0 },
  },
  {
    selector: "[data-demo-cursor-ripple]",
    properties: { scale: 0.2, autoAlpha: 0 },
  },
  {
    selector: "[data-demo-chat-empty]",
    properties: { autoAlpha: 1 },
  },
  {
    selector: "[data-demo-chat-thinking]",
    properties: { autoAlpha: 0 },
  },
  {
    selector: "[data-demo-lang-toggle]",
    properties: { autoAlpha: 1 },
  },
  {
    selector: "[data-demo-social]",
    properties: { autoAlpha: 1 },
  },
] as const;

export const DATA_ATTRIBUTES = {
  root: "data-demo-root",
  nav: "data-demo-nav",
  navHome: "data-demo-nav-home",
  navReason: "data-demo-nav-reason",
  navHow: "data-demo-nav-how",
  admin: "data-demo-admin",
  heroCard: "data-demo-hero-card",
  statusPill: "data-demo-status-pill",
  headline: "data-demo-headline",
  subcopy: "data-demo-subcopy",
  voiceButton: "data-demo-voice-button",
  textButton: "data-demo-text-button",
  micOrb: "data-demo-mic-orb",
  langToggle: "data-demo-lang-toggle",
  social: "data-demo-social",
  footer: "data-demo-footer",
  voicePanel: "data-demo-voice-panel",
  voiceStatus: "data-demo-voice-status",
  voiceMicBtn: "data-demo-voice-mic-btn",
  textChatCta: "data-demo-text-chat-cta",
  chatPanel: "data-demo-chat-panel",
  chatEmpty: "data-demo-chat-empty",
  chatInput: "data-demo-chat-input",
  chatSend: "data-demo-chat-send",
  chatUserMsg: "data-demo-chat-user-msg",
  chatThinking: "data-demo-chat-thinking",
  chatAiMsg: "data-demo-chat-ai-msg",
  cursor: "data-demo-cursor",
  cursorRipple: "data-demo-cursor-ripple",
} as const;

export const OMITTED_V1 = ["feedback card (bottom-left)"] as const;

export const ACCESSIBILITY = {
  reducedMotion:
    "Static hero frame with headline and subcopy; no cursor, no loop.",
  decorative:
    'Demo container uses aria-hidden="true"; controls exposed separately in production.',
} as const;
