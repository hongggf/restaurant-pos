import { describe, it, expect } from 'vitest'
import { formatPrice, formatPercent } from './format'

describe('formatPrice', () => {
  it('returns empty string when price is null or undefined', () => {
    expect(formatPrice(undefined)).toBe('')
    expect(formatPrice(undefined, 'USD')).toBe('')
  })

  it('formats valid price with default USD currency', () => {
    const formatted = formatPrice(12.5)
    expect(formatted).toContain('12.50')
    expect(formatted).toContain('$')
  })

  it('formats valid price with custom currency (KHR)', () => {
    const formatted = formatPrice(40000, 'KHR')
    expect(formatted).toBeDefined()
    expect(formatted.length).toBeGreaterThan(0)
  })

  it('formats zero correctly', () => {
    const formatted = formatPrice(0, 'USD')
    expect(formatted).toContain('0.00')
    expect(formatted).toContain('$')
  })

  it('falls back to string interpolation when Intl formatting fails', () => {
    // Some environments or bad currency codes could throw.
    const formatted = formatPrice(100, 'INVALID_CURRENCY_CODE')
    expect(formatted).toBe('100 INVALID_CURRENCY_CODE')
  })
})

describe('formatPercent', () => {
  it('returns empty string when rate is null or undefined', () => {
    expect(formatPercent(undefined)).toBe('')
  })

  it('formats fraction as percentage', () => {
    const formatted = formatPercent(0.1)
    // could contain non-breaking spaces or similar, just check it contains 10%
    expect(formatted.replace(/\s/g, '')).toContain('10%')
  })

  it('handles maximum fraction digits', () => {
    const formatted = formatPercent(0.12345)
    expect(formatted.replace(/\s/g, '')).toContain('12.35%')
  })
})
