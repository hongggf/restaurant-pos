import { describe, it, expect } from 'vitest'
import { choiceRule } from './choice-rule'

describe('choiceRule', () => {
  it('returns "Optional" when no min or max choice is specified', () => {
    expect(choiceRule()).toBe('Optional')
    expect(choiceRule(0)).toBe('Optional')
    expect(choiceRule(0, undefined)).toBe('Optional')
  })

  it('returns "Pick at least {min}" when minChoice is set without maxChoice', () => {
    expect(choiceRule(1)).toBe('Pick at least 1')
    expect(choiceRule(3, undefined)).toBe('Pick at least 3')
  })

  it('returns "Pick {n}" when minChoice equals maxChoice', () => {
    expect(choiceRule(1, 1)).toBe('Pick 1')
    expect(choiceRule(2, 2)).toBe('Pick 2')
  })

  it('returns "Up to {max}" when minChoice is 0 and maxChoice is set', () => {
    expect(choiceRule(0, 3)).toBe('Up to 3')
    expect(choiceRule(undefined, 2)).toBe('Up to 2')
  })

  it('returns "Pick {min}–{max}" for a range of choices', () => {
    expect(choiceRule(1, 3)).toBe('Pick 1–3')
    expect(choiceRule(2, 5)).toBe('Pick 2–5')
  })
})
