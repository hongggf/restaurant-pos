import { describe, it, expect, beforeEach, vi } from 'vitest'
import { saveToken, getToken, clearToken, getRoles, getUsername } from './token'

describe('staff auth token', () => {
  beforeEach(() => {
    localStorage.clear()
    vi.restoreAllMocks()
  })

  function createMockJwt(payload: Record<string, unknown>): string {
    const header = btoa(JSON.stringify({ alg: 'HS256', typ: 'JWT' }))
    const body = btoa(JSON.stringify(payload))
    const sig = 'mock-signature'
    return `${header}.${body}.${sig}`
  }

  it('saves and retrieves token when not expired', () => {
    const token = createMockJwt({ sub: 'admin', roles: ['ROLE_ADMIN'] })
    saveToken(token, 60000)

    expect(getToken()).toBe(token)
    expect(getUsername()).toBe('admin')
    expect(getRoles()).toEqual(['ADMIN'])
  })

  it('returns null and empty roles when token is expired', () => {
    const token = createMockJwt({ sub: 'admin', roles: ['ROLE_ADMIN'] })
    saveToken(token, 1000)

    vi.spyOn(Date, 'now').mockReturnValue(Date.now() + 5000)

    expect(getToken()).toBeNull()
    expect(getUsername()).toBeNull()
    expect(getRoles()).toEqual([])
  })

  it('correctly normalizes roles from authorities and scope claims', () => {
    const token = createMockJwt({
      sub: 'cashier1',
      authorities: ['ROLE_CASHIER'],
      scope: 'ADMIN',
    })
    saveToken(token, 60000)

    const roles = getRoles()
    expect(roles).toContain('CASHIER')
    expect(roles).toContain('ADMIN')
  })

  it('clears token on clearToken', () => {
    const token = createMockJwt({ sub: 'admin' })
    saveToken(token, 60000)
    clearToken()

    expect(getToken()).toBeNull()
    expect(getUsername()).toBeNull()
  })
})
