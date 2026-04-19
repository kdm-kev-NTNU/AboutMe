const AUTH_KEY = 'auth'

function readBasicToken(): string | null {
  if (typeof sessionStorage === 'undefined') return null
  try {
    const raw = sessionStorage.getItem(AUTH_KEY)
    if (!raw) return null
    const parsed = JSON.parse(raw) as { basicToken?: string | null }
    return parsed.basicToken ?? null
  } catch {
    return null
  }
}

// Vite dev proxy forwards /api/* to the Spring Boot port; production builds use the same prefix behind the host.
const API_PREFIX = '/api'

/**
 * Orval fetch mutator: prefixes `/api`, merges Basic auth from sessionStorage, returns `{ data, status, headers }`.
 * Generated clients in `api/generated/` import this instead of raw `fetch` so admin routes receive Authorization.
 */
export const customFetch = async <T>(url: string, init?: RequestInit): Promise<T> => {
  const path = url.startsWith('http') ? url : `${API_PREFIX}${url}`

  const headers = new Headers(init?.headers)
  const basic = readBasicToken()
  if (basic) {
    headers.set('Authorization', `Basic ${basic}`)
  }

  const res = await fetch(path, {
    ...init,
    headers,
    credentials: 'include',
  })

  let data: unknown
  const ct = res.headers.get('Content-Type') ?? ''
  if (res.status === 204 || res.status === 205) {
    data = undefined
  } else if (ct.includes('application/json')) {
    try {
      data = await res.json()
    } catch {
      data = undefined
    }
  } else {
    data = await res.text()
  }

  return {
    data,
    status: res.status,
    headers: res.headers,
  } as T
}
