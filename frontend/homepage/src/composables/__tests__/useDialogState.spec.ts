import { beforeEach, describe, expect, it } from 'vitest'
import { useDialogState } from '../useDialogState'

describe('useDialogState', () => {
  beforeEach(() => {
    const state = useDialogState()
    state.setWelcomeDialogOpen(false)
    state.setEducationDialogOpen(false)
    state.setInfoDialogOpen(false)
  })

  it('updates and exposes all dialog flags', () => {
    const state = useDialogState()

    state.setWelcomeDialogOpen(true)
    state.setEducationDialogOpen(true)
    state.setInfoDialogOpen(true)

    expect(state.isWelcomeDialogOpen.value).toBe(true)
    expect(state.isEducationDialogOpen.value).toBe(true)
    expect(state.isInfoDialogOpen.value).toBe(true)
  })

  it('shares the same global state across invocations', () => {
    const first = useDialogState()
    const second = useDialogState()

    first.setWelcomeDialogOpen(true)
    expect(second.isWelcomeDialogOpen.value).toBe(true)
  })
})
