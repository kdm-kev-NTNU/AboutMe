import { applyCsrfHeader } from '@/utils/csrf'

/**
 * Orval fetch mutator: prefixes relative URLs with the configured API prefix, sends cookies and CSRF
 * for mutating admin requests, returns `{ data, status, headers }`.
 */
export const customFetch = async <T>(url: string, init?: RequestInit): Promise<T> => {
  const path = url.startsWith('http') ? url : `/api${url}`

  const headers = new Headers(init?.headers)
  applyCsrfHeader(headers, init?.method)

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
