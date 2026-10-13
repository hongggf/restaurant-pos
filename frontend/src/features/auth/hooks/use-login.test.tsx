import { describe, it, expect, beforeEach, vi } from 'vitest'
import { renderHook, waitFor } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { useLogin } from './use-login'
import * as loginApi from '../api/login'
import * as tokenStore from '@/lib/auth/token'

vi.mock('../api/login', () => ({
  login: vi.fn(),
}))

vi.mock('@/lib/auth/token', () => ({
  saveToken: vi.fn(),
}))

describe('useLogin', () => {
  let queryClient: QueryClient

  beforeEach(() => {
    vi.clearAllMocks()
    queryClient = new QueryClient({
      defaultOptions: {
        queries: { retry: false },
      },
    })
  })

  function createWrapper() {
    return ({ children }: { children: React.ReactNode }) => (
      <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
    )
  }

  it('saves token on successful login', async () => {
    vi.mocked(loginApi.login).mockResolvedValue({
      accessToken: 'test-token',
      expiresInMs: 3600000,
    })

    const { result } = renderHook(() => useLogin(), { wrapper: createWrapper() })
    result.current.mutate({ username: 'admin', password: 'password' })
    
    await waitFor(() => expect(result.current.isSuccess).toBe(true))
    
    expect(tokenStore.saveToken).toHaveBeenCalledWith('test-token', 3600000)
  })

  it('does not throw when token information is missing', async () => {
    vi.mocked(loginApi.login).mockResolvedValue({
      // missing accessToken and expiresInMs
    } as any)

    const { result } = renderHook(() => useLogin(), { wrapper: createWrapper() })
    result.current.mutate({ username: 'admin', password: 'password' })
    
    await waitFor(() => expect(result.current.isSuccess).toBe(true))
    
    expect(tokenStore.saveToken).not.toHaveBeenCalled()
  })
})
