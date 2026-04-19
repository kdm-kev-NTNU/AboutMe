import { describe, expect, it } from 'vitest'
import { cn } from '../utils'

describe('cn', () => {
  it('merges classes and resolves tailwind conflicts', () => {
    const classes = cn('px-2 py-1', 'px-4', false && 'hidden', ['text-sm'])

    expect(classes).toContain('px-4')
    expect(classes).toContain('py-1')
    expect(classes).toContain('text-sm')
    expect(classes).not.toContain('px-2')
  })
})
