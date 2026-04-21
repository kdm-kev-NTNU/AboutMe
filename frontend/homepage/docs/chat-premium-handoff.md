# Chat Premium Handoff Spec

Design basis: `02_Redesign_Premium` from [AboutMe Chat Redesign - Modern Premium](https://www.figma.com/design/0Dihi1iBaCX9FyftZDpaKn).

## 1) Tokens (Tailwind-oriented)

- Spacing scale: `p-2`, `p-3`, `p-4`, `p-6`, `p-8`, `gap-2`, `gap-3`, `gap-4`, `gap-6`
- Radius scale: `rounded-xl` (12), `rounded-2xl` (16), `rounded-3xl` (24), `rounded-full`
- Surface shadows:
  - `shadow-sm` for subtle cards
  - `shadow-lg shadow-blue-900/10` for elevated panels
  - `shadow-xl shadow-blue-500/25` for primary CTA emphasis
- Typography:
  - Title: `text-3xl font-bold`
  - Section label: `text-xs font-semibold uppercase tracking-wide`
  - Message body: `text-sm leading-relaxed`
  - CTA label: `text-sm font-semibold`
- Core palette:
  - Page bg: `from-slate-100 via-blue-50 to-slate-100`
  - Premium panel bg: `bg-white/85` + `backdrop-blur-xl`
  - Border default: `border-blue-100/70`
  - Border focus/active: `border-blue-300/70`
  - Text primary: `text-slate-800`
  - Text secondary: `text-slate-500`
  - Primary action: `from-blue-600 to-blue-700`
  - Primary hover: `from-blue-700 to-blue-800`

## 2) Component states

- Input:
  - default: `bg-white/85 border-blue-100/70 text-slate-700`
  - focus-within: `focus-within:border-blue-300/70 focus-within:shadow-lg focus-within:shadow-blue-500/20`
  - disabled: `disabled:opacity-50 disabled:cursor-not-allowed`
- Send button:
  - default: blue gradient + subtle shadow
  - hover: slightly darker gradient + lift `-translate-y-0.5`
  - loading/disabled: reduced opacity and no lift
- Assistant bubble:
  - default: `bg-white/95 border-blue-100/80 text-slate-700`
  - emphasis/new: stronger shadow, keep high contrast body text
- User bubble:
  - default: blue gradient + white text
  - hover/active: stronger shadow `shadow-blue-500/35`
- Provider toggle/model select:
  - container: `bg-slate-100/80 border-blue-100/70`
  - selected pill: `bg-white text-blue-700`
  - unselected: `text-slate-500`
  - disabled: `opacity-40`

## 3) File mapping

- `src/views/ChatView.vue`
  - Apply premium shell hierarchy for page, header row, controls row, and composer row.
- `src/views/MessagesArea.vue`
  - Apply premium message panel surface and bubble state treatments.

## 4) Known intentional deviations

- Keep existing API/state behavior untouched (model/provider selection, loading flow, language switching).
- Restrict changes to visual system + spacing rhythm so feature behavior remains stable.
