import { describe, it, expect, beforeEach } from 'vitest'
import { render, screen } from '@testing-library/react'
import { createMemoryRouter, RouterProvider } from 'react-router'
import { GuestSessionGuard } from './guest-session-guard'
import { guestSessionStore } from '@/features/guest/lib/session-store'
import { ApiError } from '@/lib/api/client'

describe('<GuestSessionGuard />', () => {
  beforeEach(() => {
    localStorage.clear()
    guestSessionStore.setNoSession()
  })

  function renderGuard() {
    const router = createMemoryRouter(
      [
        {
          path: '/guest',
          element: <div>Guest Resolve Landing</div>,
        },
        {
          path: '/guest/menu',
          element: (
            <GuestSessionGuard>
              <div>Protected Guest Menu Content</div>
            </GuestSessionGuard>
          ),
        },
      ],
      { initialEntries: ['/guest/menu'] }
    )

    return render(<RouterProvider router={router} />)
  }

  it('redirects to /guest when session status is "no-session"', () => {
    guestSessionStore.setNoSession()

    renderGuard()

    expect(screen.getByText('Guest Resolve Landing')).toBeInTheDocument()
    expect(screen.queryByText('Protected Guest Menu Content')).not.toBeInTheDocument()
  })

  it('renders closed session message and QR code prompt when status is "closed"', () => {
    guestSessionStore.handleApiError(new ApiError('Session closed', 410))

    renderGuard()

    expect(screen.getByText('This session has ended.')).toBeInTheDocument()
    expect(
      screen.getByRole('link', { name: /scan the table qr code to start a new order/i })
    ).toHaveAttribute('href', '/guest')
    expect(screen.queryByText('Protected Guest Menu Content')).not.toBeInTheDocument()
  })

  it('renders children when session status is "browsing"', () => {
    guestSessionStore.setBrowsing({ sessionId: 'session-123', tableNumber: 'Table 4' })

    renderGuard()

    expect(screen.getByText('Protected Guest Menu Content')).toBeInTheDocument()
    expect(screen.queryByText('Guest Resolve Landing')).not.toBeInTheDocument()
    expect(screen.queryByText('This session has ended.')).not.toBeInTheDocument()
  })

  it('renders children when session status is "spent"', () => {
    guestSessionStore.setBrowsing({ sessionId: 'session-123', tableNumber: 'Table 4' })
    guestSessionStore.markSpent()

    renderGuard()

    expect(screen.getByText('Protected Guest Menu Content')).toBeInTheDocument()
    expect(screen.queryByText('Guest Resolve Landing')).not.toBeInTheDocument()
    expect(screen.queryByText('This session has ended.')).not.toBeInTheDocument()
  })

  it('renders children when session status is "resolving"', () => {
    guestSessionStore.setResolving()

    renderGuard()

    expect(screen.getByText('Protected Guest Menu Content')).toBeInTheDocument()
    expect(screen.queryByText('Guest Resolve Landing')).not.toBeInTheDocument()
    expect(screen.queryByText('This session has ended.')).not.toBeInTheDocument()
  })
})
