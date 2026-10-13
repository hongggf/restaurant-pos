import { describe, it, expect, beforeEach, vi } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router'
import { toast } from 'sonner'
import { LoginForm } from './login-form'

const mockLoginMutate = vi.fn()
let mockIsPending = false
let mockIsError = false
let mockError: { message: string } | null = null

const mockNavigate = vi.fn()
let mockLocationState: unknown = null

vi.mock('../hooks/use-login', () => ({
  useLogin: () => ({
    mutate: mockLoginMutate,
    isPending: mockIsPending,
    isError: mockIsError,
    error: mockError,
  }),
}))

vi.mock('sonner', () => ({
  toast: {
    success: vi.fn(),
    error: vi.fn(),
  },
}))

vi.mock('react-router', async () => {
  const actual = await vi.importActual<typeof import('react-router')>('react-router')
  return {
    ...actual,
    useNavigate: () => mockNavigate,
    useLocation: () => ({
      pathname: '/login',
      search: '',
      hash: '',
      state: mockLocationState,
      key: 'test',
    }),
  }
})

describe('<LoginForm />', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mockIsPending = false
    mockIsError = false
    mockError = null
    mockLocationState = null
  })

  function renderLoginForm() {
    return render(
      <MemoryRouter>
        <LoginForm />
      </MemoryRouter>
    )
  }

  it('renders title, description, form fields, and submit button', () => {
    renderLoginForm()

    expect(screen.getByText('Restaurant POS')).toBeInTheDocument()
    expect(screen.getByText('Staff sign in')).toBeInTheDocument()
    expect(screen.getByLabelText(/username/i)).toBeInTheDocument()
    expect(screen.getByLabelText(/^password$/i)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /^sign in$/i })).toBeInTheDocument()
  })

  it('displays validation errors when submitting with empty fields', async () => {
    const user = userEvent.setup()
    renderLoginForm()

    const submitBtn = screen.getByRole('button', { name: /^sign in$/i })
    await user.click(submitBtn)

    await waitFor(() => {
      expect(screen.getByText('Username is required')).toBeInTheDocument()
      expect(screen.getByText('Password is required')).toBeInTheDocument()
    })

    expect(mockLoginMutate).not.toHaveBeenCalled()
  })

  it('submits form with entered username and password when valid', async () => {
    const user = userEvent.setup()
    renderLoginForm()

    const usernameInput = screen.getByLabelText(/username/i)
    const passwordInput = screen.getByLabelText(/^password$/i)
    const submitBtn = screen.getByRole('button', { name: /^sign in$/i })

    await user.type(usernameInput, 'admin_user')
    await user.type(passwordInput, 'secret123')
    await user.click(submitBtn)

    await waitFor(() => {
      expect(mockLoginMutate).toHaveBeenCalledTimes(1)
      expect(mockLoginMutate).toHaveBeenCalledWith(
        {
          username: 'admin_user',
          password: 'secret123',
        },
        expect.objectContaining({
          onSuccess: expect.any(Function),
        })
      )
    })
  })

  it('toggles password visibility without losing the typed value', async () => {
    const user = userEvent.setup()
    renderLoginForm()

    const passwordInput = screen.getByLabelText(/^password$/i)
    await user.type(passwordInput, 'secret123')
    expect(passwordInput).toHaveAttribute('type', 'password')

    await user.click(screen.getByRole('button', { name: /show password/i }))
    expect(passwordInput).toHaveAttribute('type', 'text')
    expect(passwordInput).toHaveValue('secret123')

    await user.click(screen.getByRole('button', { name: /hide password/i }))
    expect(passwordInput).toHaveAttribute('type', 'password')
    expect(passwordInput).toHaveValue('secret123')
  })

  it('does not submit the form when the visibility toggle is clicked', async () => {
    const user = userEvent.setup()
    renderLoginForm()

    await user.click(screen.getByRole('button', { name: /show password/i }))

    expect(mockLoginMutate).not.toHaveBeenCalled()
  })

  it('renders server error message when login fails with error', () => {
    mockIsError = true
    mockError = { message: 'Invalid username or password' }

    renderLoginForm()

    expect(screen.getByText('Invalid username or password')).toBeInTheDocument()
  })

  it('shows loading state and disables submit button while pending', () => {
    mockIsPending = true

    renderLoginForm()

    const submitBtn = screen.getByRole('button', { name: /signing in…/i })
    expect(submitBtn).toBeInTheDocument()
    expect(submitBtn).toBeDisabled()
  })

  it('handles successful login by navigating to "/" by default and showing toast', async () => {
    const user = userEvent.setup()
    mockLoginMutate.mockImplementation((_values, options) => {
      options?.onSuccess?.()
    })

    renderLoginForm()

    await user.type(screen.getByLabelText(/username/i), 'admin')
    await user.type(screen.getByLabelText(/^password$/i), 'pass')
    await user.click(screen.getByRole('button', { name: /^sign in$/i }))

    await waitFor(() => {
      expect(toast.success).toHaveBeenCalledWith('Signed in')
      expect(mockNavigate).toHaveBeenCalledWith('/', { replace: true })
    })
  })

  it('handles successful login by navigating to previous "from" path when provided in location state', async () => {
    mockLocationState = { from: { pathname: '/cashier/tables' } }
    const user = userEvent.setup()
    mockLoginMutate.mockImplementation((_values, options) => {
      options?.onSuccess?.()
    })

    renderLoginForm()

    await user.type(screen.getByLabelText(/username/i), 'cashier1')
    await user.type(screen.getByLabelText(/^password$/i), 'pass123')
    await user.click(screen.getByRole('button', { name: /^sign in$/i }))

    await waitFor(() => {
      expect(toast.success).toHaveBeenCalledWith('Signed in')
      expect(mockNavigate).toHaveBeenCalledWith('/cashier/tables', { replace: true })
    })
  })
})
