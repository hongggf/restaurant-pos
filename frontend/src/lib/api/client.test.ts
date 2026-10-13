import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest'
import { apiFetch, ApiError, assetUrl } from './client'
import { clearToken, saveToken } from '@/lib/auth/token'

describe('api client', () => {
  const originalFetch = globalThis.fetch

  beforeEach(() => {
    localStorage.clear()
    clearToken()
    vi.restoreAllMocks()
  })

  afterEach(() => {
    globalThis.fetch = originalFetch
  })

  describe('apiFetch', () => {
    it('fetches successfully and returns unwrapped envelope data', async () => {
      const mockData = { id: 1, name: 'Item 1' }
      const mockResponse = {
        ok: true,
        status: 200,
        json: async () => ({
          success: true,
          message: 'OK',
          data: mockData,
          timestamp: '2026-08-23T00:00:00Z',
        }),
      }
      globalThis.fetch = vi.fn().mockResolvedValue(mockResponse as unknown as Response)

      const result = await apiFetch<typeof mockData>('/items')

      expect(globalThis.fetch).toHaveBeenCalledWith(
        '/api/v1/items',
        expect.objectContaining({
          headers: expect.any(Headers),
        })
      )
      expect(result).toEqual(mockData)
    })

    it('sets Content-Type to application/json when body is provided and not FormData', async () => {
      const mockResponse = {
        ok: true,
        status: 200,
        json: async () => ({ success: true, data: { ok: true } }),
      }
      globalThis.fetch = vi.fn().mockResolvedValue(mockResponse as unknown as Response)

      await apiFetch('/items', {
        method: 'POST',
        body: JSON.stringify({ name: 'New Item' }),
      })

      const calledOptions = (globalThis.fetch as ReturnType<typeof vi.fn>).mock.calls[0][1]
      const headers = calledOptions.headers as Headers
      expect(headers.get('Content-Type')).toBe('application/json')
    })

    it('does not set Content-Type to application/json when body is FormData', async () => {
      const formData = new FormData()
      formData.append('file', new Blob(['test']), 'test.txt')

      const mockResponse = {
        ok: true,
        status: 200,
        json: async () => ({ success: true, data: { uploaded: true } }),
      }
      globalThis.fetch = vi.fn().mockResolvedValue(mockResponse as unknown as Response)

      await apiFetch('/upload', {
        method: 'POST',
        body: formData,
      })

      const calledOptions = (globalThis.fetch as ReturnType<typeof vi.fn>).mock.calls[0][1]
      const headers = calledOptions.headers as Headers
      expect(headers.get('Content-Type')).toBeNull()
    })

    it('attaches Authorization header when auth token exists in storage', async () => {
      const header = btoa(JSON.stringify({ alg: 'HS256', typ: 'JWT' }))
      const body = btoa(JSON.stringify({ sub: 'admin', roles: ['ROLE_ADMIN'] }))
      const token = `${header}.${body}.sig`
      saveToken(token, 60000)

      const mockResponse = {
        ok: true,
        status: 200,
        json: async () => ({ success: true, data: { auth: true } }),
      }
      globalThis.fetch = vi.fn().mockResolvedValue(mockResponse as unknown as Response)

      await apiFetch('/protected')

      const calledOptions = (globalThis.fetch as ReturnType<typeof vi.fn>).mock.calls[0][1]
      const headers = calledOptions.headers as Headers
      expect(headers.get('Authorization')).toBe(`Bearer ${token}`)
    })

    it('preserves existing Authorization header if explicitly passed', async () => {
      saveToken('local-token', 60000)

      const mockResponse = {
        ok: true,
        status: 200,
        json: async () => ({ success: true, data: {} }),
      }
      globalThis.fetch = vi.fn().mockResolvedValue(mockResponse as unknown as Response)

      await apiFetch('/custom-auth', {
        headers: { Authorization: 'Bearer custom-token' },
      })

      const calledOptions = (globalThis.fetch as ReturnType<typeof vi.fn>).mock.calls[0][1]
      const headers = calledOptions.headers as Headers
      expect(headers.get('Authorization')).toBe('Bearer custom-token')
    })

    it('throws ApiError with message and status on unsuccessful envelope', async () => {
      const mockResponse = {
        ok: false,
        status: 400,
        json: async () => ({
          success: false,
          message: 'Invalid table ID',
        }),
      }
      globalThis.fetch = vi.fn().mockResolvedValue(mockResponse as unknown as Response)

      await expect(apiFetch('/items')).rejects.toThrow(ApiError)
      await expect(apiFetch('/items')).rejects.toMatchObject({
        message: 'Invalid table ID',
        status: 400,
      })
    })

    it('throws ApiError with fallback status text when body is not JSON', async () => {
      const mockResponse = {
        ok: false,
        status: 502,
        json: async () => {
          throw new Error('Bad gateway HTML')
        },
      }
      globalThis.fetch = vi.fn().mockResolvedValue(mockResponse as unknown as Response)

      await expect(apiFetch('/items')).rejects.toMatchObject({
        message: 'Request failed (502)',
        status: 502,
      })
    })

    it('throws ApiError with status 0 on network connection failure', async () => {
      globalThis.fetch = vi.fn().mockRejectedValue(new Error('Network offline'))

      await expect(apiFetch('/items')).rejects.toMatchObject({
        message: 'Cannot reach the server. Is the API running?',
        status: 0,
      })
    })
  })

  describe('assetUrl', () => {
    it('returns null for null or undefined input', () => {
      expect(assetUrl(null)).toBeNull()
      expect(assetUrl(undefined)).toBeNull()
      expect(assetUrl('')).toBeNull()
    })

    it('returns absolute HTTP/HTTPS URLs as-is', () => {
      expect(assetUrl('http://example.com/pic.png')).toBe('http://example.com/pic.png')
      expect(assetUrl('https://images.unsplash.com/photo-1')).toBe('https://images.unsplash.com/photo-1')
    })

    it('returns /food-images/... local static assets as-is', () => {
      expect(assetUrl('/food-images/ramen.jpg')).toBe('/food-images/ramen.jpg')
    })

    it('prepends BASE_URL to backend-relative uploads', () => {
      expect(assetUrl('/uploads/menu-item-123.jpg')).toBe('/uploads/menu-item-123.jpg')
    })
  })
})
