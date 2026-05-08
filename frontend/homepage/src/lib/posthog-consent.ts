import posthog from 'posthog-js'
import { initializePosthogSdk, isPosthogSdkInitialized } from './posthog-sdk'

export const PRIVACY_POLICY_VERSION = '2026-05-08'

/** Legacy key (v1: single `analytics` boolean). Still read for migration. */
export const CONSENT_RECORD_KEY_V1 = 'aboutme_cookie_consent_v1'

/** Current consent storage key (granular categories). */
export const CONSENT_RECORD_KEY = 'aboutme_cookie_consent_v2'

/** @deprecated Use CONSENT_RECORD_KEY; kept for tests and imports. */
export const COOKIE_CONSENT_RECORD_KEY = CONSENT_RECORD_KEY

const LEGACY_POSTHOG_FLAG = 'posthog_tracking_consent'

export type ConsentSource =
  | 'banner_accept_all'
  | 'banner_necessary_only'
  | 'banner_reject'
  | 'settings'
  | 'legacy_migration'

export type GranularConsentFlags = {
  pageviews: boolean
  sessionRecording: boolean
  errorTracking: boolean
  featureFlags: boolean
}

export type CookieConsentRecord = GranularConsentFlags & {
  dismissed: boolean
  policyVersion: string
  updatedAt: string
  source: ConsentSource
}

type CookieConsentRecordV1 = {
  dismissed: boolean
  analytics: boolean
  policyVersion: string
  updatedAt: string
  source: ConsentSource | string
}

type TrackingConsent = 'granted' | 'denied' | null

let activationHandler: (() => void) | null = null

type PosthogEnv = {
  key: string
  host: string
  enabled: boolean
}

let testEnvOverride: Partial<PosthogEnv> | null = null

export function __setPosthogTestEnv(env: Partial<PosthogEnv> | null): void {
  testEnvOverride = env
}

function getPosthogEnv(): PosthogEnv {
  const env = import.meta.env ?? {}
  const key = (testEnvOverride?.key ?? env.VITE_POSTHOG_KEY ?? '').trim()
  const host =
    (testEnvOverride?.host ?? env.VITE_POSTHOG_HOST ?? '').trim() || 'https://eu.i.posthog.com'
  const enabled =
    testEnvOverride?.enabled ?? (env.VITE_POSTHOG_ENABLED === 'true' && Boolean(key))

  return { key, host, enabled }
}

export function isPosthogEnabled(): boolean {
  return getPosthogEnv().enabled
}

function anyOptionalConsent(record: CookieConsentRecord): boolean {
  return (
    record.pageviews ||
    record.sessionRecording ||
    record.errorTracking ||
    record.featureFlags
  )
}

export function hasAnalyticsConsent(): boolean {
  const record = getConsentRecord()
  return record?.dismissed === true && anyOptionalConsent(record)
}

export function hasPageviewConsent(): boolean {
  const record = getConsentRecord()
  return record?.dismissed === true && record.pageviews === true
}

export function hasSessionRecordingConsent(): boolean {
  const record = getConsentRecord()
  return record?.dismissed === true && record.sessionRecording === true
}

export function hasErrorTrackingConsent(): boolean {
  const record = getConsentRecord()
  return record?.dismissed === true && record.errorTracking === true
}

export function hasFeatureFlagConsent(): boolean {
  const record = getConsentRecord()
  return record?.dismissed === true && record.featureFlags === true
}

export function getConsentRecord(): CookieConsentRecord | null {
  const v2 = readConsentRecordV2()
  if (v2) return v2

  const fromV1 = readAndMigrateV1()
  if (fromV1) return fromV1

  return migrateLegacyConsent()
}

export function isCookieBannerDismissed(): boolean {
  return getConsentRecord()?.dismissed === true
}

export function registerPosthogActivationHandler(handler: () => void): void {
  activationHandler = handler
}

export function applyStoredTrackingConsent(): void {
  if (!isPosthogEnabled()) return

  const record = getConsentRecord()
  if (!record?.dismissed) return

  syncPosthogWithRecord(record)
}

export function grantAllCookies(source: ConsentSource = 'banner_accept_all'): void {
  persistGranularDecision(
    {
      pageviews: true,
      sessionRecording: true,
      errorTracking: true,
      featureFlags: true,
    },
    source,
  )
  syncPosthogWithRecord(getConsentRecord()!)
}

export function grantNecessaryCookiesOnly(source: ConsentSource = 'banner_necessary_only'): void {
  persistGranularDecision(
    {
      pageviews: false,
      sessionRecording: false,
      errorTracking: false,
      featureFlags: false,
    },
    source,
  )
  deactivatePosthogAnalytics()
}

export function rejectOptionalCookies(source: ConsentSource = 'banner_reject'): void {
  persistGranularDecision(
    {
      pageviews: false,
      sessionRecording: false,
      errorTracking: false,
      featureFlags: false,
    },
    source,
  )
  deactivatePosthogAnalytics()
}

export function saveGranularConsent(
  flags: GranularConsentFlags,
  source: ConsentSource = 'settings',
): void {
  persistGranularDecision(flags, source)
  syncPosthogWithRecord(getConsentRecord()!)
}

/** @deprecated Use saveGranularConsent; retained for transitional callers if any. */
export function saveAnalyticsConsent(
  analytics: boolean,
  source: ConsentSource = 'settings',
): void {
  saveGranularConsent(
    {
      pageviews: analytics,
      sessionRecording: analytics,
      errorTracking: analytics,
      featureFlags: analytics,
    },
    source,
  )
}

function syncPosthogWithRecord(record: CookieConsentRecord): void {
  const env = getPosthogEnv()
  if (!env.enabled) return

  if (!anyOptionalConsent(record)) {
    deactivatePosthogAnalytics()
    return
  }

  /** Pass true when user opted out of session recording (maps to PostHog `disable_session_recording`). */
  const disableSessionRecordingAtInit = !record.sessionRecording
  if (
    !initializePosthogSdk({
      key: env.key,
      host: env.host,
      disableSessionRecording: disableSessionRecordingAtInit,
    })
  )
    return

  posthog.opt_in_capturing()

  if (record.sessionRecording) {
    try {
      posthog.startSessionRecording?.()
    } catch {
      // ignore if recorder unavailable
    }
  } else {
    try {
      posthog.stopSessionRecording?.()
    } catch {
      // ignore
    }
  }

  activationHandler?.()
}

function deactivatePosthogAnalytics(): void {
  if (!isPosthogEnabled()) return
  if (!isPosthogSdkInitialized()) return
  try {
    posthog.stopSessionRecording?.()
  } catch {
    // ignore if recorder unavailable
  }
  posthog.opt_out_capturing()
  posthog.reset()
}

function persistGranularDecision(flags: GranularConsentFlags, source: ConsentSource): void {
  const now = new Date().toISOString()
  const record: CookieConsentRecord = {
    dismissed: true,
    ...flags,
    policyVersion: PRIVACY_POLICY_VERSION,
    updatedAt: now,
    source,
  }
  writeConsentRecord(record)
  writeLegacyConsentFlag(anyOptionalConsent(record) ? 'granted' : 'denied')
}

function readConsentRecordV2(): CookieConsentRecord | null {
  try {
    const raw = localStorage.getItem(CONSENT_RECORD_KEY)
    if (!raw) return null
    const parsed = JSON.parse(raw) as unknown
    if (!isCookieConsentRecord(parsed)) return null
    return parsed
  } catch {
    return null
  }
}

function readAndMigrateV1(): CookieConsentRecord | null {
  try {
    const raw = localStorage.getItem(CONSENT_RECORD_KEY_V1)
    if (!raw) return null
    const parsed = JSON.parse(raw) as unknown
    if (!isV1Record(parsed)) return null

    const all = parsed.analytics
    const record: CookieConsentRecord = {
      dismissed: parsed.dismissed,
      pageviews: all,
      sessionRecording: all,
      errorTracking: all,
      featureFlags: all,
      policyVersion: PRIVACY_POLICY_VERSION,
      updatedAt: new Date().toISOString(),
      source: 'legacy_migration',
    }
    writeConsentRecord(record)
    writeLegacyConsentFlag(all ? 'granted' : 'denied')
    return record
  } catch {
    return null
  }
}

function isV1Record(parsed: unknown): parsed is CookieConsentRecordV1 {
  if (!parsed || typeof parsed !== 'object') return false
  const p = parsed as Record<string, unknown>
  return (
    typeof p.dismissed === 'boolean' &&
    typeof p.analytics === 'boolean' &&
    typeof p.policyVersion === 'string' &&
    typeof p.updatedAt === 'string' &&
    typeof p.source === 'string'
  )
}

function isCookieConsentRecord(parsed: unknown): parsed is CookieConsentRecord {
  if (!parsed || typeof parsed !== 'object') return false
  const p = parsed as Record<string, unknown>
  return (
    typeof p.dismissed === 'boolean' &&
    typeof p.pageviews === 'boolean' &&
    typeof p.sessionRecording === 'boolean' &&
    typeof p.errorTracking === 'boolean' &&
    typeof p.featureFlags === 'boolean' &&
    typeof p.policyVersion === 'string' &&
    typeof p.updatedAt === 'string' &&
    typeof p.source === 'string'
  )
}

function writeConsentRecord(record: CookieConsentRecord): void {
  localStorage.setItem(CONSENT_RECORD_KEY, JSON.stringify(record))
}

function readLegacyPosthogConsent(): TrackingConsent {
  try {
    const status = posthog.get_explicit_consent_status?.()
    if (status === 'granted' || status === 'denied') return status
  } catch {
    // ignore
  }

  const storedFlag = localStorage.getItem(LEGACY_POSTHOG_FLAG)
  if (storedFlag === 'granted' || storedFlag === 'denied') return storedFlag

  return null
}

function writeLegacyConsentFlag(consent: Exclude<TrackingConsent, null>): void {
  localStorage.setItem(LEGACY_POSTHOG_FLAG, consent)
}

function migrateLegacyConsent(): CookieConsentRecord | null {
  const legacy = readLegacyPosthogConsent()
  if (!legacy) return null

  const all = legacy === 'granted'
  const now = new Date().toISOString()
  const record: CookieConsentRecord = {
    dismissed: true,
    pageviews: all,
    sessionRecording: all,
    errorTracking: all,
    featureFlags: all,
    policyVersion: PRIVACY_POLICY_VERSION,
    updatedAt: now,
    source: 'legacy_migration',
  }
  writeConsentRecord(record)
  return record
}
