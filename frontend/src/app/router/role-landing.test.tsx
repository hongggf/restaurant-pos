import { describe, it, expect, beforeEach, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { createMemoryRouter, RouterProvider } from 'react-router'
import { RoleLanding } from './role-landing'
import { clearToken, saveToken } from '@/lib/auth/token'

function createMockJwt(payload: Record<string, unknown>): string {
  const header = btoa(JSON.stringify({ alg: 'HS256', typ: 'JWT' }))
  const body = btoa(JSON.stringify(payload))
  return `${header}.${body}.mock-sig`
}

describe('<RoleLanding />', () => {
  beforeEach(() => {
    localStorage.clear()
    clearToken()
    vi.restoreAllMocks()
  })

  function renderLanding() {
    const router = createMemoryRouter(
      [
        { path: '/', element: <RoleLanding /> },
        { path: '/login', element: <div>Login Landing Page</div> },
        { path: '/admin', element: <div>Admin Landing Page</div> },
        { path: '/cashier', element: <div>Cashier Landing Page</div> },
        { path: '/kitchen', element: <div>Kitchen Landing Page</div> },
      ],
      { initialEntries: ['/'] }
    )
    return render(<RouterProvider router={router} />)
  }

  it('redirects unauthenticated users to /login', () => {
    renderLanding()

    expect(screen.getByText('Login Landing Page')).toBeInTheDocument()
  })

  it('redirects users with ADMIN role to /admin', () => {
    const token = createMockJwt({ sub: 'admin', roles: ['ROLE_ADMIN'] })
    saveToken(token, 60000)

    renderLanding()

    expect(screen.getByText('Admin Landing Page')).toBeInTheDocument()
  })

  it('redirects users with CASHIER role to /cashier', () => {
    const token = createMockJwt({ sub: 'cashier', roles: ['ROLE_CASHIER'] })
    saveToken(token, 60000)

    renderLanding()

    expect(screen.getByText('Cashier Landing Page')).toBeInTheDocument()
  })

  it('redirects users with CHEF role to /kitchen', () => {
    const token = createMockJwt({ sub: 'chef', roles: ['ROLE_CHEF'] })
    saveToken(token, 60000)

    renderLanding()

    expect(screen.getByText('Kitchen Landing Page')).toBeInTheDocument()
  })

  it('shows unauthorized screen and sign out button for unrecognized roles', async () => {
    const token = createMockJwt({ sub: 'unknown', roles: ['ROLE_GUEST'] })
    saveToken(token, 60000)

    const assignMock = vi.fn()
    Object.defineProperty(window, 'location', {
      value: { assign: assignMock },
      writable: true,
    })

    const user = userEvent.setup()
    renderLanding()

    expect(
      screen.getByText(/your account doesn't have access to any app\./i)
    ).toBeInTheDocument()

    const signOutBtn = screen.getByRole('button', { name: /sign out/i })
    await user.click(signOutBtn)

    expect(assignMock).toHaveBeenCalledWith('/login')
  })
})
