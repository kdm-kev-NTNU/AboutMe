import posthog from 'posthog-js'
import { initializePosthogSdk, isPosthogSdkInitialized } from './posthog-sdk'

export const PRIVACY_POLICY_VERSION = '2026-04-22'

export const CONSENT_RECORD_KEY = 'aboutme_cookie_consent_v1'
export const COOKIE_CONSENT_RECORD_KEY = CONSENT_RECORD_KEY
const LEGACY_POSTHOG_FLAG = 'posthog_tracking_consent'

export type ConsentSource =
  | 'banner_accept_all'
  | 'banner_necessary_only'
  | 'banner_reject'
  | 'settings'
  | 'legacy_migration'

export type CookieConsentRecord = {
  dismissed: boolean
  analytics: boolean
  policyVersion: string
  updatedAt: string
  source: ConsentSource
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

export function getConsentRecord(): CookieConsentRecord | null {
  return readConsentRecord() ?? migrateLegacyConsent()
}

export function isCookieBannerDismissed(): boolean {
  return getConsentRecord()?.dismissed === true
}

export function hasAnalyticsConsent(): boolean {
  return getConsentRecord()?.analytics === true
}

export function registerPosthogActivationHandler(handler: () => void): void {
  activationHandler = handler
}

export function applyStoredTrackingConsent(): void {
  if (!isPosthogEnabled()) return

  const record = getConsentRecord()
  if (!record?.dismissed) return

  if (record.analytics) {
    activatePosthogAnalytics()
  } else {
    deactivatePosthogAnalytics()
  }
}

export function grantAllCookies(source: ConsentSource = 'banner_accept_all'): void {
  persistDecision(true, source)
  activatePosthogAnalytics()
}

export function grantNecessaryCookiesOnly(source: ConsentSource = 'banner_necessary_only'): void {
  persistDecision(false, source)
  deactivatePosthogAnalytics()
}

export function rejectOptionalCookies(source: ConsentSource = 'banner_reject'): void {
  persistDecision(false, source)
  deactivatePosthogAnalytics()
}

export function saveAnalyticsConsent(
  analytics: boolean,
  source: ConsentSource = 'settings',
): void {
  persistDecision(analytics, source)
  if (analytics) {
    activatePosthogAnalytics()
  } else {
    deactivatePosthogAnalytics()
  }
}

function activatePosthogAnalytics(): void {
  const env = getPosthogEnv()
  if (!env.enabled) return
  if (!initializePosthogSdk({ key: env.key, host: env.host })) return
  posthog.opt_in_capturing()
  try {
    posthog.startSessionRecording?.()
  } catch {
    // ignore if recorder unavailable
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

function persistDecision(analytics: boolean, source: ConsentSource): void {
  const now = new Date().toISOString()
  const record: CookieConsentRecord = {
    dismissed: true,
    analytics,
    policyVersion: PRIVACY_POLICY_VERSION,
    updatedAt: now,
    source,
  }
  writeConsentRecord(record)
  writeLegacyConsentFlag(analytics ? 'granted' : 'denied')
}

function readConsentRecord(): CookieConsentRecord | null {
  try {
    const raw = localStorage.getItem(CONSENT_RECORD_KEY)
    if (!raw) return null
    const parsed = JSON.parse(raw) as CookieConsentRecord
    if (
      typeof parsed.dismissed === 'boolean' &&
      typeof parsed.analytics === 'boolean' &&
      typeof parsed.policyVersion === 'string' &&
      typeof parsed.updatedAt === 'string' &&
      typeof parsed.source === 'string'
    ) {
      return parsed
    }
  } catch {
    // ignore parse errors and treat as no record
  }
  return null
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

  const now = new Date().toISOString()
  const record: CookieConsentRecord = {
    dismissed: true,
    analytics: legacy === 'granted',
    policyVersion: PRIVACY_POLICY_VERSION,
    updatedAt: now,
    source: 'legacy_migration',
  }
  writeConsentRecord(record)
  return record
}
