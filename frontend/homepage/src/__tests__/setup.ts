import { vi } from 'vitest'

class TestIntersectionObserver implements IntersectionObserver {
  readonly root = null
  readonly rootMargin = '0px'
  readonly scrollMargin = '0px'
  readonly thresholds: ReadonlyArray<number> = []

  disconnect(): void {}

  observe(): void {}

  takeRecords(): IntersectionObserverEntry[] {
    return []
  }

  unobserve(): void {}
}

vi.stubGlobal('IntersectionObserver', TestIntersectionObserver)
