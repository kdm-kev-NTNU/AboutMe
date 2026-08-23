import posthog from 'posthog-js'
import { hasAnalyticsConsent, isPosthogEnabled } from './posthog-consent'
import { isPosthogSdkInitialized } from './posthog-sdk'

const INTERNAL_SUPER_PROPERTY = 'is_internal'

let pendingDistinctId: string | null = null
let appliedDistinctId: string | null = null

function canApplyIdentity(): boolean {
  return isPosthogEnabled() && hasAnalyticsConsent() && isPosthogSdkInitialized()
}

/**
 * Stores the server-issued PostHog distinct id for an authenticated admin. Applies immediately
 * when analytics consent and SDK init are already active; otherwise deferred until activation.
 */
export function setOwnerIdentity(distinctId: string): void {
  const trimmed = distinctId.trim()
  if (!trimmed) return

  pendingDistinctId = trimmed
  applyPendingOwnerIdentity()
}

/** Idempotent: identify and register internal super-property when prerequisites are met. */
export function applyPendingOwnerIdentity(): void {
  if (!pendingDistinctId || !canApplyIdentity()) return
  if (appliedDistinctId === pendingDistinctId) return

  try {
    posthog.identify(pendingDistinctId, {
      is_internal: true,
      is_owner: true,
    })
    posthog.register({ [INTERNAL_SUPER_PROPERTY]: true })
    appliedDistinctId = pendingDistinctId
  } catch {
    // ignore identify failures
  }
}

/** Clears owner identity from PostHog. Call only on explicit logout — not on session expiry. */
export function revokeOwnerIdentity(): void {
  pendingDistinctId = null
  appliedDistinctId = null

  if (!isPosthogEnabled() || !isPosthogSdkInitialized()) return

  try {
    posthog.unregister(INTERNAL_SUPER_PROPERTY)
    posthog.reset()
  } catch {
    // ignore reset failures
  }
}

/** @internal Test-only reset of module state. */
export function __resetAnalyticsIdentityForTests(): void {
  pendingDistinctId = null
  appliedDistinctId = null
}
