import { describe, expect, it } from 'vitest'
import { apiErrorMessage, formatAdminHttpError, parseApiError } from '@/lib/api-error'

describe('parseApiError', () => {
  it('returns undefined for non-objects and missing error string', () => {
    expect(parseApiError(null)).toBeUndefined()
    expect(parseApiError({})).toBeUndefined()
    expect(parseApiError({ code: 'X' })).toBeUndefined()
  })

  it('parses minimal ApiError', () => {
    expect(parseApiError({ error: 'oops' })).toEqual({ error: 'oops' })
  })

  it('parses extended ApiError with violations', () => {
    expect(
      parseApiError({
        error: 'Validation failed',
        code: 'VALIDATION_FAILED',
        traceId: 'abc',
        timestamp: '2026-01-01T00:00:00Z',
        violations: [{ field: 'name', message: 'must not be blank' }],
      }),
    ).toEqual({
      error: 'Validation failed',
      code: 'VALIDATION_FAILED',
      traceId: 'abc',
      timestamp: '2026-01-01T00:00:00Z',
      violations: [{ field: 'name', message: 'must not be blank' }],
    })
  })
})

describe('apiErrorMessage', () => {
  it('appends violation messages', () => {
    expect(
      apiErrorMessage({
        error: 'Validation failed',
        violations: [{ field: 'q', message: 'too long' }],
      }),
    ).toBe('Validation failed (too long)')
  })
})

describe('formatAdminHttpError', () => {
  it('maps 401 without body', () => {
    expect(formatAdminHttpError(401, {})).toBe('Ikke autorisert (logg inn som admin)')
  })

  it('prefers ApiError on 403', () => {
    expect(
      formatAdminHttpError(403, { error: 'Access denied', code: 'FORBIDDEN' }),
    ).toBe('Access denied')
  })
})
