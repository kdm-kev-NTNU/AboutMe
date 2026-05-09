import type { ApiError } from '@/api/generated/portfolio'

function isRecord(data: unknown): data is Record<string, unknown> {
  return data !== null && typeof data === 'object'
}

/** Parse backend {@link ApiError} JSON; returns undefined if body is not a recognizable error envelope. */
export function parseApiError(data: unknown): ApiError | undefined {
  if (!isRecord(data)) return undefined
  const err = data.error
  if (typeof err !== 'string' || err.length === 0) return undefined
  const out: ApiError = { error: err }
  if (typeof data.code === 'string' && data.code.length > 0) out.code = data.code
  if (typeof data.traceId === 'string') out.traceId = data.traceId
  if (typeof data.timestamp === 'string') out.timestamp = data.timestamp
  if (Array.isArray(data.violations)) {
    const violations = data.violations.filter(
      (v): v is { field: string; message: string } =>
        isRecord(v) && typeof v.field === 'string' && typeof v.message === 'string',
    )
    if (violations.length > 0) out.violations = violations
  }
  return out
}

/** Human-readable message for UI, including validation violation summaries when present. */
export function apiErrorMessage(data: unknown): string | undefined {
  const p = parseApiError(data)
  if (!p) return undefined
  if (p.violations?.length) {
    const parts = p.violations.map((v) => v.message || v.field)
    return `${p.error} (${parts.join(', ')})`
  }
  return p.error
}

/** Shared formatter for admin HTTP helpers (Norwegian defaults). */
export function formatAdminHttpError(status: number, data: unknown): string {
  if (status === 401) return 'Ikke autorisert (logg inn som admin)'
  if (status === 403) {
    const fromApi = apiErrorMessage(data)
    if (fromApi) return fromApi
    if (isRecord(data)) {
      if (typeof data.detail === 'string' && data.detail) return data.detail
      if (typeof data.message === 'string' && data.message) return data.message
    }
    return 'Ikke tillatt (403) — f.eks. synk deaktivert på serveren'
  }
  const fromApi = apiErrorMessage(data)
  if (fromApi) return fromApi
  if (isRecord(data)) {
    if (typeof data.message === 'string' && data.message) return data.message
    if (typeof data.detail === 'string' && data.detail) return data.detail
  }
  return `Feilet (${status})`
}
