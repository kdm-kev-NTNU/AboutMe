import { beforeEach, describe, expect, it, vi } from 'vitest'

describe('cookie-settings-state', () => {
  beforeEach(() => {
    vi.resetModules()
  })

  it('opens and closes cookie settings', async () => {
    const { cookieSettingsOpen, openCookieSettings, closeCookieSettings } = await import(
      '../cookie-settings-state'
    )

    expect(cookieSettingsOpen.value).toBe(false)
    openCookieSettings()
    expect(cookieSettingsOpen.value).toBe(true)
    closeCookieSettings()
    expect(cookieSettingsOpen.value).toBe(false)
  })
})
