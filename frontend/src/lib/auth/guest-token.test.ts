import { describe, it, expect, beforeEach, vi } from 'vitest'
import {
  saveGuestSession,
  setGuestSpent,
  isGuestSpent,
  getGuestToken,
  getGuestSessionMeta,
  clearGuestSession,
} from './guest-token'

describe('guest-token management', () => {
  beforeEach(() => {
    localStorage.clear()
    vi.restoreAllMocks()
  })

  it('saves session and retrieves token when not expired', () => {
    saveGuestSession({
      accessToken: 'test-token-123',
      expiresInMs: 60000,
      sessionId: 'sess-abc',
      tableNumber: 'T-01',
    })

    expect(getGuestToken()).toBe('test-token-123')
    expect(isGuestSpent()).toBe(false)
    expect(getGuestSessionMeta()).toEqual({
      sessionId: 'sess-abc',
      tableNumber: 'T-01',
    })
  })

  it('returns null token and null meta when token is expired', () => {
    saveGuestSession({
      accessToken: 'test-token-123',
      expiresInMs: 1000,
      sessionId: 'sess-abc',
      tableNumber: 'T-01',
    })

    // Advance time past expiry
    vi.spyOn(Date, 'now').mockReturnValue(Date.now() + 5000)

    expect(getGuestToken()).toBeNull()
    expect(getGuestSessionMeta()).toBeNull()
  })

  it('manages guest spent state correctly', () => {
    expect(isGuestSpent()).toBe(false)

    setGuestSpent(true)
    expect(isGuestSpent()).toBe(true)

    setGuestSpent(false)
    expect(isGuestSpent()).toBe(false)
  })

  it('clears all guest session keys on clearGuestSession', () => {
    saveGuestSession({
      accessToken: 'test-token-123',
      expiresInMs: 60000,
      sessionId: 'sess-abc',
      tableNumber: 'T-01',
    })
    setGuestSpent(true)

    clearGuestSession()

    expect(getGuestToken()).toBeNull()
    expect(getGuestSessionMeta()).toBeNull()
    expect(isGuestSpent()).toBe(false)
  })
})
