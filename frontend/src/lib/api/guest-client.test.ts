import { describe, it, expect, beforeEach, vi } from 'vitest'
import { guestApiFetch } from './guest-client'
import { saveGuestSession, clearGuestSession } from '@/lib/auth/guest-token'
import { guestSessionStore } from '@/features/guest/lib/session-store'
import * as clientModule from './client'
import { ApiError } from './client'

describe('guestApiFetch', () => {
  beforeEach(() => {
    localStorage.clear()
    clearGuestSession()
    guestSessionStore.setNoSession()
    vi.restoreAllMocks()
  })

  it('attaches guest authorization token and returns response data', async () => {
    saveGuestSession({
      accessToken: 'test-guest-jwt-token',
      expiresInMs: 60000,
      sessionId: 'sess-123',
      tableNumber: 'Table 5',
    })
    const mockData = { id: 'order-1', items: [] }

    const apiFetchSpy = vi.spyOn(clientModule, 'apiFetch').mockResolvedValue(mockData)

    const result = await guestApiFetch('/guest/cart')

    expect(apiFetchSpy).toHaveBeenCalledWith(
      '/guest/cart',
      expect.objectContaining({
        headers: expect.any(Headers),
      })
    )

    const passedHeaders = apiFetchSpy.mock.calls[0][1]?.headers as Headers
    expect(passedHeaders.get('Authorization')).toBe('Bearer test-guest-jwt-token')
    expect(result).toEqual(mockData)
  })

  it('calls apiFetch without guest Authorization header when no guest token exists', async () => {
    const apiFetchSpy = vi.spyOn(clientModule, 'apiFetch').mockResolvedValue({ ok: true })

    await guestApiFetch('/guest/session')

    const passedHeaders = apiFetchSpy.mock.calls[0][1]?.headers as Headers
    expect(passedHeaders.get('Authorization')).toBeNull()
  })

  it('handles ApiError by delegating to guestSessionStore.handleApiError and re-throwing', async () => {
    const apiError = new ApiError('Session expired', 410)
    vi.spyOn(clientModule, 'apiFetch').mockRejectedValue(apiError)
    const handleApiErrorSpy = vi.spyOn(guestSessionStore, 'handleApiError')

    await expect(guestApiFetch('/guest/cart')).rejects.toThrow(apiError)
    expect(handleApiErrorSpy).toHaveBeenCalledWith(apiError)
  })

  it('re-throws non-ApiError without calling guestSessionStore.handleApiError', async () => {
    const genericError = new Error('Unexpected crash')
    vi.spyOn(clientModule, 'apiFetch').mockRejectedValue(genericError)
    const handleApiErrorSpy = vi.spyOn(guestSessionStore, 'handleApiError')

    await expect(guestApiFetch('/guest/cart')).rejects.toThrow(genericError)
    expect(handleApiErrorSpy).not.toHaveBeenCalled()
  })
})
