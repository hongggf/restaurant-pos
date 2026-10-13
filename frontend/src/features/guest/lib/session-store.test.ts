import { describe, it, expect, beforeEach, vi } from 'vitest'
import { guestSessionStore } from './session-store'
import { ApiError } from '@/lib/api/client'

describe('guestSessionStore', () => {
  beforeEach(() => {
    localStorage.clear()
    guestSessionStore.setNoSession()
    vi.restoreAllMocks()
  })

  it('notifies subscribers when state changes', () => {
    const listener = vi.fn()
    const unsubscribe = guestSessionStore.subscribe(listener)

    guestSessionStore.setBrowsing({ sessionId: 'session-1', tableNumber: '05' })
    expect(listener).toHaveBeenCalledTimes(1)
    expect(guestSessionStore.getState()).toEqual({
      status: 'browsing',
      sessionId: 'session-1',
      tableNumber: '05',
    })

    unsubscribe()
    guestSessionStore.setResolving()
    expect(listener).toHaveBeenCalledTimes(1)
  })

  it('transitions to resolving state', () => {
    guestSessionStore.setResolving()
    expect(guestSessionStore.getState()).toEqual({ status: 'resolving' })
  })

  it('marks state as spent when browsing', () => {
    guestSessionStore.setBrowsing({ sessionId: 'session-1', tableNumber: '05' })
    guestSessionStore.markSpent()

    expect(guestSessionStore.getState()).toEqual({
      status: 'spent',
      sessionId: 'session-1',
      tableNumber: '05',
    })
  })

  it('handles 401 Unauthorized API error by clearing session and setting no-session', () => {
    guestSessionStore.setBrowsing({ sessionId: 'session-1', tableNumber: '05' })
    guestSessionStore.handleApiError(new ApiError('Unauthorized', 401))

    expect(guestSessionStore.getState()).toEqual({ status: 'no-session' })
  })

  it('handles 410 Gone API error by clearing session and setting closed state', () => {
    guestSessionStore.setBrowsing({ sessionId: 'session-1', tableNumber: '05' })
    guestSessionStore.handleApiError(new ApiError('Session closed', 410))

    expect(guestSessionStore.getState()).toEqual({ status: 'closed' })
  })

  it('handles 403 Forbidden API error by transitioning to spent state', () => {
    guestSessionStore.setBrowsing({ sessionId: 'session-1', tableNumber: '05' })
    guestSessionStore.handleApiError(new ApiError('Device spent', 403))

    expect(guestSessionStore.getState()).toEqual({
      status: 'spent',
      sessionId: 'session-1',
      tableNumber: '05',
    })
  })
})
