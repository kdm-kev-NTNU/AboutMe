import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

describe('cloudinary helpers', () => {
  beforeEach(() => {
    vi.resetModules()
  })

  afterEach(() => {
    vi.unstubAllEnvs()
  })

  it('returns empty string when cloud name is missing', async () => {
    vi.stubEnv('VITE_CLOUDINARY_CLOUD_NAME', '')
    const { buildCloudinaryImageUrl } = await import('../cloudinary')

    expect(buildCloudinaryImageUrl('sample')).toBe('')
  })

  it('returns empty string when publicId is missing', async () => {
    vi.stubEnv('VITE_CLOUDINARY_CLOUD_NAME', 'demo')
    const { buildCloudinaryImageUrl } = await import('../cloudinary')

    expect(buildCloudinaryImageUrl('')).toBe('')
  })

  it('uses default transforms for image url generation', async () => {
    vi.stubEnv('VITE_CLOUDINARY_CLOUD_NAME', 'demo')
    const { buildCloudinaryImageUrl } = await import('../cloudinary')

    expect(buildCloudinaryImageUrl('portfolio/avatar')).toBe(
      'https://res.cloudinary.com/demo/image/upload/f_auto,q_auto/portfolio/avatar',
    )
  })

  it('returns empty srcset when widths list is empty', async () => {
    vi.stubEnv('VITE_CLOUDINARY_CLOUD_NAME', 'demo')
    const { buildCloudinarySrcSet } = await import('../cloudinary')

    expect(buildCloudinarySrcSet('portfolio/avatar', [])).toBe('')
  })

  it('builds srcset entries with extra transforms and width tokens', async () => {
    vi.stubEnv('VITE_CLOUDINARY_CLOUD_NAME', 'demo')
    const { buildCloudinarySrcSet } = await import('../cloudinary')

    expect(buildCloudinarySrcSet('portfolio/avatar', [320, 640], ['c_fill', 'g_auto'])).toBe(
      'https://res.cloudinary.com/demo/image/upload/f_auto,q_auto,c_fill,g_auto,w_320/portfolio/avatar 320w, ' +
        'https://res.cloudinary.com/demo/image/upload/f_auto,q_auto,c_fill,g_auto,w_640/portfolio/avatar 640w',
    )
  })
})
