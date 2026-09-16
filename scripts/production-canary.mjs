#!/usr/bin/env node
/**
 * Production canary for the chat and voice features.
 *
 * Why this exists
 * ---------------
 * On 2026-09-16 live voice was broken in production for an unknown length of time while every
 * shallow signal reported healthy: `/actuator/health` returned UP, `/realtime/status` returned
 * `liveEnabled: true`, `/realtime/models` returned a model, and the SPA rendered normally. The
 * failure only surfaced when the backend actually tried to open an HTTPS connection to OpenAI.
 *
 * The lesson encoded here: an availability check that stops at the process boundary cannot detect
 * an egress fault. Each feature is therefore probed at the boundary that can actually break.
 *
 * The backend has two distinct outbound HTTP stacks, and they fail independently:
 *   - Apache HttpClient 5  (Spring AI chat + transcription)  -> exercised by CHAT_EGRESS
 *   - java.net.http.HttpClient (realtime session + PostHog)  -> exercised by VOICE_EGRESS
 * Covering one says nothing about the other, so both are probed on every run.
 *
 * Usage
 * -----
 *   node scripts/production-canary.mjs [--base-url https://example.com] [--json out.json]
 *                                      [--markdown out.md]
 *
 * Environment: CANARY_BASE_URL overrides the default base URL.
 * Exit code 0 when no check FAILED, 1 otherwise. INCONCLUSIVE never fails the run.
 */

const DEFAULT_BASE_URL = 'https://kevindmazali.me'

/** Outcome of a single check. INCONCLUSIVE means "could not determine", never an alert. */
const PASS = 'PASS'
const FAIL = 'FAIL'
const INCONCLUSIVE = 'INCONCLUSIVE'

/**
 * Realtime error codes that prove the backend completed an HTTPS round-trip to OpenAI.
 * Reaching OpenAI is the property under test, so an upstream rejection still counts as reachable.
 * Mirrors com.kevinmazali.portfolio.exception.RealtimeErrorCode.
 */
const REALTIME_CODES_PROVING_REACHABILITY = new Set([
  'OPENAI_REJECTED', // OpenAI parsed the request and refused it -> transport is healthy
  'OPENAI_SERVER_ERROR', // OpenAI answered with 5xx -> transport is healthy, upstream is not
])

/**
 * Realtime error codes that mean live voice is broken for real users, with the operator-facing
 * reason. Anything not listed here and not in REALTIME_CODES_PROVING_REACHABILITY is treated as
 * inconclusive rather than paging on an unknown code.
 */
const REALTIME_CODES_MEANING_BROKEN = new Map([
  [
    'OPENAI_UNREACHABLE',
    'Backend cannot open a connection to api.openai.com (egress/DNS/routing fault).',
  ],
  ['API_KEY_MISSING', 'OPENAI_API_KEY is not configured on the backend.'],
  ['REALTIME_DISABLED', 'PORTFOLIO_REALTIME_ENABLED is not true on the backend.'],
  ['VOICE_MODEL_NOT_CONFIGURED', 'No realtime voice model is configured or allow-listed.'],
  ['SESSION_CONFIG_FAILED', 'Backend could not build the Realtime session config.'],
  ['BUDGET_EXCEEDED', 'AI budget is exhausted, so voice is refusing new sessions.'],
  ['CIRCUIT_OPEN', 'Realtime circuit breaker is open after repeated upstream failures.'],
])

/**
 * A syntactically invalid SDP offer.
 *
 * VOICE_EGRESS deliberately does not negotiate a real WebRTC session: doing so needs a browser,
 * a microphone and a paid Realtime session, none of which belong in an unattended daily job.
 * Sending an offer OpenAI is guaranteed to refuse isolates exactly one property -- can the backend
 * talk to OpenAI at all -- because the backend maps an upstream refusal and a transport failure to
 * two different error codes. Costs nothing upstream and needs no credentials here.
 */
const INVALID_SDP_OFFER = 'v=0\r\n'

function parseArgs(argv) {
  const args = { baseUrl: process.env.CANARY_BASE_URL || DEFAULT_BASE_URL }
  for (let i = 0; i < argv.length; i += 1) {
    const arg = argv[i]
    if (arg === '--base-url') args.baseUrl = argv[(i += 1)]
    else if (arg === '--json') args.jsonPath = argv[(i += 1)]
    else if (arg === '--markdown') args.markdownPath = argv[(i += 1)]
    else throw new Error(`Unknown argument: ${arg}`)
  }
  if (!args.baseUrl) throw new Error('Base URL must not be empty')
  args.baseUrl = args.baseUrl.replace(/\/+$/, '')
  return args
}

const sleep = (ms) => new Promise((resolve) => setTimeout(resolve, ms))

/**
 * Performs a request, retrying only on transport errors so a flaky runner network cannot be
 * reported as a production outage. Upstream HTTP responses are never retried: they are the signal.
 */
async function request(url, { method = 'GET', headers = {}, body, timeoutMs = 30_000 } = {}) {
  const maxAttempts = 3
  let lastError
  for (let attempt = 1; attempt <= maxAttempts; attempt += 1) {
    try {
      const response = await fetch(url, {
        method,
        headers,
        body,
        signal: AbortSignal.timeout(timeoutMs),
        redirect: 'follow',
      })
      return { status: response.status, text: await response.text() }
    } catch (error) {
      lastError = error
      if (attempt < maxAttempts) await sleep(attempt * 2_000)
    }
  }
  return { transportError: lastError?.message ?? String(lastError) }
}

function parseJson(text) {
  try {
    return JSON.parse(text)
  } catch {
    return undefined
  }
}

/** Truncated single-line form of a response body, safe to paste into a GitHub issue. */
function excerpt(text, limit = 300) {
  if (!text) return '(empty body)'
  const flat = text.replace(/\s+/g, ' ').trim()
  return flat.length > limit ? `${flat.slice(0, limit)}...` : flat
}

const result = (status, summary, detail) => ({ status, summary, detail })

// --- Checks -------------------------------------------------------------------------------------
// Each check answers one question and owns its own pass/fail contract. A check never throws; an
// unexpected shape is reported as INCONCLUSIVE so unknown states do not masquerade as outages.

async function checkBackendHealth(baseUrl) {
  const response = await request(`${baseUrl}/api/actuator/health`, { timeoutMs: 20_000 })
  if (response.transportError) {
    return result(FAIL, 'Backend health endpoint is unreachable', response.transportError)
  }
  if (response.status !== 200) {
    return result(FAIL, `Backend health returned HTTP ${response.status}`, excerpt(response.text))
  }
  const body = parseJson(response.text)
  if (body?.status !== 'UP') {
    return result(FAIL, `Backend health status is ${body?.status ?? 'unparseable'}`, excerpt(response.text))
  }
  return result(PASS, 'Backend reports UP')
}

/** A non-empty model list is what the SPA gates its chat UI on. */
async function checkChatAvailability(baseUrl) {
  const response = await request(`${baseUrl}/api/chat/models`, { timeoutMs: 20_000 })
  if (response.transportError) {
    return result(FAIL, 'Chat model catalog is unreachable', response.transportError)
  }
  if (response.status !== 200) {
    return result(FAIL, `Chat model catalog returned HTTP ${response.status}`, excerpt(response.text))
  }
  const models = parseJson(response.text)
  if (!Array.isArray(models) || models.length === 0) {
    return result(FAIL, 'Chat model catalog is empty, so the SPA will offer no chat models', excerpt(response.text))
  }
  return result(PASS, `${models.length} chat model(s) offered`, models.map((m) => m?.id).join(', '))
}

/**
 * Proves the full chat path including the Apache HttpClient 5 egress to the model provider.
 * This is the only check that spends provider tokens; the prompt is kept minimal for that reason.
 */
async function checkChatEgress(baseUrl) {
  const response = await request(`${baseUrl}/api/ask`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', 'X-Chat-Language': 'en' },
    body: JSON.stringify({ question: 'Reply with exactly one word: ping' }),
    timeoutMs: 90_000,
  })
  if (response.transportError) {
    return result(FAIL, 'Chat endpoint is unreachable', response.transportError)
  }
  if (response.status === 429) {
    return result(INCONCLUSIVE, 'Chat is rate limited, so this run could not verify it', excerpt(response.text))
  }
  if (response.status !== 200) {
    return result(FAIL, `Chat answer failed with HTTP ${response.status}`, excerpt(response.text))
  }
  const answer = parseJson(response.text)?.answer
  if (typeof answer !== 'string' || answer.trim() === '') {
    return result(FAIL, 'Chat returned HTTP 200 with no answer text', excerpt(response.text))
  }
  return result(PASS, 'Chat answered a live question', `answer: ${excerpt(answer, 120)}`)
}

/** The two signals the SPA uses to decide whether to show the "Start live voice" button. */
async function checkVoiceAvailability(baseUrl) {
  const statusResponse = await request(`${baseUrl}/api/realtime/status`, { timeoutMs: 20_000 })
  if (statusResponse.transportError) {
    return result(FAIL, 'Realtime status endpoint is unreachable', statusResponse.transportError)
  }
  if (statusResponse.status !== 200) {
    return result(FAIL, `Realtime status returned HTTP ${statusResponse.status}`, excerpt(statusResponse.text))
  }
  if (parseJson(statusResponse.text)?.liveEnabled !== true) {
    return result(
      FAIL,
      'Realtime status reports liveEnabled=false, so the SPA hides live voice',
      excerpt(statusResponse.text),
    )
  }

  const modelsResponse = await request(`${baseUrl}/api/realtime/models`, { timeoutMs: 20_000 })
  if (modelsResponse.transportError) {
    return result(FAIL, 'Realtime model catalog is unreachable', modelsResponse.transportError)
  }
  const models = parseJson(modelsResponse.text)
  if (!Array.isArray(models) || models.length === 0) {
    return result(
      FAIL,
      'Realtime model catalog is empty, so the SPA hides live voice',
      excerpt(modelsResponse.text),
    )
  }
  return result(PASS, `Live voice advertised with ${models.length} model(s)`, models.map((m) => m?.id).join(', '))
}

/**
 * Proves the backend can reach the OpenAI Realtime API over the java.net.http.HttpClient stack.
 * See INVALID_SDP_OFFER for why an upstream rejection is the expected healthy outcome.
 */
async function checkVoiceEgress(baseUrl) {
  const response = await request(`${baseUrl}/api/realtime/session`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/sdp', 'X-Chat-Language': 'en' },
    body: INVALID_SDP_OFFER,
    timeoutMs: 60_000,
  })
  if (response.transportError) {
    return result(FAIL, 'Realtime session endpoint is unreachable', response.transportError)
  }
  if (response.status >= 200 && response.status < 300) {
    return result(PASS, 'OpenAI Realtime accepted a session, so voice egress works')
  }
  if (response.status === 429) {
    return result(
      INCONCLUSIVE,
      'Realtime session start is rate limited, so this run could not verify voice egress',
      excerpt(response.text),
    )
  }

  const code = parseJson(response.text)?.code
  if (REALTIME_CODES_PROVING_REACHABILITY.has(code)) {
    return result(
      PASS,
      'Backend reached OpenAI Realtime; the probe offer was rejected upstream as expected',
      `HTTP ${response.status}, code ${code}`,
    )
  }
  const brokenReason = REALTIME_CODES_MEANING_BROKEN.get(code)
  if (brokenReason) {
    return result(FAIL, `Live voice is broken: ${brokenReason}`, `HTTP ${response.status}, code ${code}`)
  }
  return result(
    INCONCLUSIVE,
    `Realtime session returned an unrecognised code, so voice egress is unverified`,
    `HTTP ${response.status}, body ${excerpt(response.text)}`,
  )
}

const CHECKS = [
  { id: 'BACKEND_HEALTH', feature: 'platform', title: 'Backend process health', run: checkBackendHealth },
  { id: 'CHAT_AVAILABILITY', feature: 'chat', title: 'Chat models advertised', run: checkChatAvailability },
  { id: 'CHAT_EGRESS', feature: 'chat', title: 'Chat answers a live question', run: checkChatEgress },
  { id: 'VOICE_AVAILABILITY', feature: 'voice', title: 'Live voice advertised', run: checkVoiceAvailability },
  { id: 'VOICE_EGRESS', feature: 'voice', title: 'Backend reaches OpenAI Realtime', run: checkVoiceEgress },
]

// --- Reporting ----------------------------------------------------------------------------------

const ICONS = { [PASS]: '✅', [FAIL]: '❌', [INCONCLUSIVE]: '⚠️' }

function buildMarkdown(report) {
  const lines = [
    `**Target:** ${report.baseUrl}`,
    `**Checked:** ${report.checkedAt}`,
    `**Result:** ${report.failed} failed, ${report.inconclusive} inconclusive, ${report.passed} passed`,
    '',
    '| | Check | Feature | Detail |',
    '| --- | --- | --- | --- |',
  ]
  for (const check of report.checks) {
    const detail = [check.summary, check.detail].filter(Boolean).join(' — ').replace(/\|/g, '\\|')
    lines.push(`| ${ICONS[check.status]} | ${check.title} | ${check.feature} | ${detail} |`)
  }
  if (report.failed > 0) {
    lines.push(
      '',
      'A failing check means the feature is broken for real visitors. `INCONCLUSIVE` means the run',
      'could not determine the state (usually rate limiting) and is not an outage.',
    )
  }
  return `${lines.join('\n')}\n`
}

async function main() {
  const args = parseArgs(process.argv.slice(2))
  const { writeFile, appendFile } = await import('node:fs/promises')

  console.log(`Production canary against ${args.baseUrl}\n`)
  const checks = []
  for (const check of CHECKS) {
    const outcome = await check.run(args.baseUrl)
    checks.push({ id: check.id, feature: check.feature, title: check.title, ...outcome })
    console.log(`${ICONS[outcome.status]} ${outcome.status.padEnd(12)} ${check.title}: ${outcome.summary}`)
    if (outcome.detail) console.log(`   ${outcome.detail}`)
  }

  const report = {
    baseUrl: args.baseUrl,
    checkedAt: new Date().toISOString(),
    passed: checks.filter((c) => c.status === PASS).length,
    failed: checks.filter((c) => c.status === FAIL).length,
    inconclusive: checks.filter((c) => c.status === INCONCLUSIVE).length,
    failedFeatures: [...new Set(checks.filter((c) => c.status === FAIL).map((c) => c.feature))].sort(),
    checks,
  }

  const markdown = buildMarkdown(report)
  if (args.jsonPath) await writeFile(args.jsonPath, `${JSON.stringify(report, null, 2)}\n`, 'utf8')
  if (args.markdownPath) await writeFile(args.markdownPath, markdown, 'utf8')
  if (process.env.GITHUB_STEP_SUMMARY) {
    await appendFile(process.env.GITHUB_STEP_SUMMARY, `## Production canary\n\n${markdown}`, 'utf8')
  }

  console.log(
    `\n${report.failed} failed, ${report.inconclusive} inconclusive, ${report.passed} passed` +
      (report.failed > 0 ? ` (broken: ${report.failedFeatures.join(', ')})` : ''),
  )
  process.exitCode = report.failed > 0 ? 1 : 0
}

main().catch((error) => {
  console.error(`Canary could not run: ${error?.stack ?? error}`)
  process.exitCode = 1
})
