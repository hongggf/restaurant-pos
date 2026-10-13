import { describe, it, expect, beforeEach } from 'vitest'
import { render, screen } from '@testing-library/react'
import { createMemoryRouter, RouterProvider, useLocation } from 'react-router'
import { RequireAuth } from './require-auth'
import { clearToken, saveToken, type Role } from '@/lib/auth/token'

function createMockJwt(payload: Record<string, unknown>): string {
  const header = btoa(JSON.stringify({ alg: 'HS256', typ: 'JWT' }))
  const body = btoa(JSON.stringify(payload))
  return `${header}.${body}.mock-sig`
}

function LocationDisplay() {
  const location = useLocation()
  return (
    <div>
      <span>LoginPage</span>
      <span data-testid="from-state">
        {(location.state as { from?: { pathname: string } } | null)?.from?.pathname ?? 'none'}
      </span>
    </div>
  )
}

describe('<RequireAuth />', () => {
  beforeEach(() => {
    localStorage.clear()
    clearToken()
  })

  function renderWithRouter({
    roles,
    initialEntry = '/admin/users',
  }: {
    roles?: Role[]
    initialEntry?: string
  } = {}) {
    const router = createMemoryRouter(
      [
        {
          path: '/login',
          element: <LocationDisplay />,
        },
        {
          path: '/',
          element: <div>Role Landing Root</div>,
        },
        {
          element: <RequireAuth roles={roles} />,
          children: [
            {
              path: '/admin/users',
              element: <div>Protected Admin Content</div>,
            },
            {
              path: '/cashier/till',
              element: <div>Protected Cashier Content</div>,
            },
            {
              path: '/common',
              element: <div>Protected Common Content</div>,
            },
          ],
        },
      ],
      { initialEntries: [initialEntry] }
    )

    return render(<RouterProvider router={router} />)
  }

  it('redirects unauthenticated users to /login and saves location state in "from"', () => {
    renderWithRouter({ roles: ['ADMIN'], initialEntry: '/admin/users' })

    expect(screen.getByText('LoginPage')).toBeInTheDocument()
    expect(screen.getByTestId('from-state')).toHaveTextContent('/admin/users')
    expect(screen.queryByText('Protected Admin Content')).not.toBeInTheDocument()
  })

  it('redirects to /login when token has expired', () => {
    const token = createMockJwt({ sub: 'admin', roles: ['ROLE_ADMIN'] })
    saveToken(token, -1000) // expired

    renderWithRouter({ roles: ['ADMIN'], initialEntry: '/admin/users' })

    expect(screen.getByText('LoginPage')).toBeInTheDocument()
    expect(screen.queryByText('Protected Admin Content')).not.toBeInTheDocument()
  })

  it('renders protected outlet when user is authenticated and no specific roles are required', () => {
    const token = createMockJwt({ sub: 'user1', roles: ['ROLE_CASHIER'] })
    saveToken(token, 60000)

    renderWithRouter({ initialEntry: '/common' })

    expect(screen.getByText('Protected Common Content')).toBeInTheDocument()
  })

  it('allows access when user has the exact required role', () => {
    const token = createMockJwt({ sub: 'admin', roles: ['ROLE_ADMIN'] })
    saveToken(token, 60000)

    renderWithRouter({ roles: ['ADMIN'], initialEntry: '/admin/users' })

    expect(screen.getByText('Protected Admin Content')).toBeInTheDocument()
  })

  it('allows access when user has one of several authorized roles', () => {
    const token = createMockJwt({ sub: 'cashier', roles: ['ROLE_CASHIER'] })
    saveToken(token, 60000)

    renderWithRouter({
      roles: ['CASHIER', 'ADMIN'],
      initialEntry: '/cashier/till',
    })

    expect(screen.getByText('Protected Cashier Content')).toBeInTheDocument()
  })

  it('redirects to "/" when authenticated user lacks the required role', () => {
    const token = createMockJwt({ sub: 'chef', roles: ['ROLE_CHEF'] })
    saveToken(token, 60000)

    renderWithRouter({ roles: ['ADMIN'], initialEntry: '/admin/users' })

    expect(screen.getByText('Role Landing Root')).toBeInTheDocument()
    expect(screen.queryByText('Protected Admin Content')).not.toBeInTheDocument()
  })

  it('redirects to "/" when user has unknown role that does not match required roles', () => {
    const token = createMockJwt({ sub: 'guest', roles: ['UNKNOWN_ROLE'] })
    saveToken(token, 60000)

    renderWithRouter({ roles: ['ADMIN', 'CASHIER'], initialEntry: '/admin/users' })

    expect(screen.getByText('Role Landing Root')).toBeInTheDocument()
    expect(screen.queryByText('Protected Admin Content')).not.toBeInTheDocument()
  })
})
