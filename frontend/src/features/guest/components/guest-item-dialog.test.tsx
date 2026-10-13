import { describe, it, expect, beforeEach, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { GuestItemDialog } from './guest-item-dialog'
import { LanguageProvider } from '@/lib/language-context'
import type { GuestMenuItemDetail, MenuItem } from '../types'
import type { GuestSessionState } from '../lib/session-store'

const mockAddLineMutate = vi.fn()
const mockUpdateLineMutate = vi.fn()
let mockAddLinePending = false
const mockUpdateLinePending = false
let mockDetailPending = false
let mockDetailError = false
let mockDetailData: GuestMenuItemDetail | null = null
let mockSessionState: GuestSessionState = {
  status: 'browsing',
  sessionId: 'sess-1',
  tableNumber: 'Table 1',
}

vi.mock('../hooks/use-guest-menu', () => ({
  useGuestMenuItemDetail: () => ({
    data: mockDetailData,
    isPending: mockDetailPending,
    isError: mockDetailError,
  }),
}))

vi.mock('../hooks/use-guest-cart', () => ({
  useAddCartLine: () => ({
    mutate: mockAddLineMutate,
    isPending: mockAddLinePending,
  }),
  useUpdateCartLine: () => ({
    mutate: mockUpdateLineMutate,
    isPending: mockUpdateLinePending,
  }),
}))

vi.mock('../hooks/use-guest-session', () => ({
  useGuestSession: () => mockSessionState,
}))

describe('<GuestItemDialog />', () => {
  const mockItem: MenuItem = {
    id: 'item-1',
    nameEn: 'Beef Malatang',
    nameKm: 'ស៊ុបម៉ាឡាថាំង',
    price: 5.0,
    currencyCode: 'USD',
    available: true,
  }

  const defaultDetail: GuestMenuItemDetail = {
    item: {
      id: 'item-1',
      nameEn: 'Beef Malatang',
      price: 5.0,
    },
    modifierGroups: [
      {
        sortOrder: 1,
        group: {
          id: 'group-spiciness',
          nameEn: 'Spice Level',
          minChoice: 1,
          maxChoice: 1,
          active: true,
          options: [
            { id: 'opt-mild', nameEn: 'Mild Spice', unitPrice: 0, available: true },
            { id: 'opt-hot', nameEn: 'Hot Spice', unitPrice: 0.5, available: true },
          ],
        },
      },
      {
        sortOrder: 2,
        group: {
          id: 'group-toppings',
          nameEn: 'Extra Toppings',
          minChoice: 0,
          maxChoice: 3,
          active: true,
          options: [
            { id: 'opt-egg', nameEn: 'Quail Egg', unitPrice: 1.0, available: true },
            { id: 'opt-beef', nameEn: 'Extra Beef', unitPrice: 2.0, available: true },
          ],
        },
      },
    ],
  }

  const mockOnOpenChange = vi.fn()

  beforeEach(() => {
    vi.clearAllMocks()
    mockAddLinePending = false
    mockDetailPending = false
    mockDetailError = false
    mockDetailData = defaultDetail
    mockSessionState = {
      status: 'browsing',
      sessionId: 'sess-1',
      tableNumber: 'Table 1',
    }
  })

  it('renders item title, price, Khmer name, and modifier groups', () => {
    render(<GuestItemDialog item={mockItem} onOpenChange={mockOnOpenChange} />)

    expect(screen.getByRole('heading', { name: 'Beef Malatang' })).toBeInTheDocument()
    expect(screen.getByText(/\$5\.00 · ស៊ុបម៉ាឡាថាំង/)).toBeInTheDocument()
    expect(screen.getByText('Spice Level')).toBeInTheDocument()
    expect(screen.getByText('Extra Toppings')).toBeInTheDocument()
    expect(screen.getByText('Mild Spice')).toBeInTheDocument()
    expect(screen.getByText('Hot Spice')).toBeInTheDocument()
    expect(screen.getByText('Quail Egg')).toBeInTheDocument()
    expect(screen.getByText('Extra Beef')).toBeInTheDocument()
  })

  it('renders Khmer title on top and English name on bottom when language is Khmer', () => {
    localStorage.setItem('malatang_guest_language', 'km')
    render(
      <LanguageProvider>
        <GuestItemDialog item={mockItem} onOpenChange={mockOnOpenChange} />
      </LanguageProvider>
    )

    expect(screen.getByRole('heading', { name: 'ស៊ុបម៉ាឡាថាំង' })).toBeInTheDocument()
    expect(screen.getByText(/\$5\.00 · Beef Malatang/)).toBeInTheDocument()
    localStorage.removeItem('malatang_guest_language')
  })

  it('shows loading spinner when menu item detail is pending', () => {
    mockDetailPending = true
    mockDetailData = null

    render(<GuestItemDialog item={mockItem} onOpenChange={mockOnOpenChange} />)

    expect(screen.queryByText('Spice Level')).not.toBeInTheDocument()
  })

  it('shows error state when detail loading fails', () => {
    mockDetailError = true
    mockDetailData = null

    render(<GuestItemDialog item={mockItem} onOpenChange={mockOnOpenChange} />)

    expect(
      screen.getByText(/couldn't load this item's options\. close and try again\./i)
    ).toBeInTheDocument()
  })

  it('disables add button until required single-choice modifier is selected', async () => {
    const user = userEvent.setup()
    render(<GuestItemDialog item={mockItem} onOpenChange={mockOnOpenChange} />)

    const addButton = screen.getByRole('button', { name: /add · \$5\.00/i })
    expect(addButton).toBeDisabled()

    // Select Mild Spice (single choice)
    const mildOption = screen.getByText('Mild Spice')
    await user.click(mildOption)

    expect(addButton).not.toBeDisabled()
  })

  it('allows selecting multi-choice toppings and calculates updated unit price', async () => {
    const user = userEvent.setup()
    render(<GuestItemDialog item={mockItem} onOpenChange={mockOnOpenChange} />)

    // Select required Spice Level
    await user.click(screen.getByText('Mild Spice'))

    // Add Quail Egg (+$1.00)
    const moreEggBtn = screen.getByRole('button', { name: /more quail egg/i })
    await user.click(moreEggBtn)

    // Price becomes $5.00 (base) + $1.00 (egg) = $6.00
    expect(screen.getByRole('button', { name: /add · \$6\.00/i })).toBeInTheDocument()

    // Add another Quail Egg (+$1.00)
    await user.click(moreEggBtn)
    expect(screen.getByRole('button', { name: /add · \$7\.00/i })).toBeInTheDocument()

    // Decrease Quail Egg by 1
    const fewerEggBtn = screen.getByRole('button', { name: /fewer quail egg/i })
    await user.click(fewerEggBtn)
    expect(screen.getByRole('button', { name: /add · \$6\.00/i })).toBeInTheDocument()
  })

  it('allows adjusting item quantity and multiplies the total price', async () => {
    const user = userEvent.setup()
    render(<GuestItemDialog item={mockItem} onOpenChange={mockOnOpenChange} />)

    // Select required Spice Level
    await user.click(screen.getByText('Mild Spice'))

    // Increase item quantity
    const increaseQtyBtn = screen.getByRole('button', { name: /increase quantity/i })
    await user.click(increaseQtyBtn)

    // $5.00 * 2 = $10.00
    expect(screen.getByRole('button', { name: /add · \$10\.00/i })).toBeInTheDocument()

    // Decrease item quantity
    const decreaseQtyBtn = screen.getByRole('button', { name: /decrease quantity/i })
    await user.click(decreaseQtyBtn)
    expect(screen.getByRole('button', { name: /add · \$5\.00/i })).toBeInTheDocument()

    // Cannot decrease below 1
    expect(decreaseQtyBtn).toBeDisabled()
  })

  it('submits cart line with remark and selections, closing dialog on success', async () => {
    const user = userEvent.setup()
    mockAddLineMutate.mockImplementation((_payload, options) => {
      options?.onSuccess?.()
    })

    render(<GuestItemDialog item={mockItem} onOpenChange={mockOnOpenChange} />)

    // Select required Hot Spice (+$0.50)
    await user.click(screen.getByText('Hot Spice'))

    // Add Extra Beef (+$2.00)
    await user.click(screen.getByRole('button', { name: /more extra beef/i }))

    // Type remark
    const remarkInput = screen.getByLabelText(/special instructions/i)
    await user.type(remarkInput, 'No scallions')

    // Click Add button ($5.00 + $0.50 + $2.00 = $7.50)
    const addBtn = screen.getByRole('button', { name: /add · \$7\.50/i })
    await user.click(addBtn)

    expect(mockAddLineMutate).toHaveBeenCalledTimes(1)
    expect(mockAddLineMutate).toHaveBeenCalledWith(
      {
        menuItemId: 'item-1',
        quantity: 1,
        remark: 'No scallions',
        selections: [
          { modifierOptionId: 'opt-hot', quantity: 1 },
          { modifierOptionId: 'opt-beef', quantity: 1 },
        ],
      },
      expect.objectContaining({
        onSuccess: expect.any(Function),
      })
    )

    expect(mockOnOpenChange).toHaveBeenCalledWith(false)
  })

  it('hides special instructions when category does not allow notes', () => {
    const itemWithoutNotes: MenuItem = {
      ...mockItem,
      categoryAllowNotes: false,
    }
    render(<GuestItemDialog item={itemWithoutNotes} onOpenChange={mockOnOpenChange} />)
    expect(screen.queryByLabelText(/special instructions/i)).not.toBeInTheDocument()
  })

  it('shows beverage placeholder for drinks with notes enabled', () => {
    const drinkItem: MenuItem = {
      ...mockItem,
      nameEn: 'Coca-Cola Classic',
      categoryNameEn: 'I Love Soft Drinks',
      categoryAllowNotes: true,
    }
    render(<GuestItemDialog item={drinkItem} onOpenChange={mockOnOpenChange} />)
    const input = screen.getByLabelText(/special instructions/i)
    expect(input).toHaveAttribute('placeholder', expect.stringMatching(/less ice|cold can/i))
  })

  it('disables add button when session status is spent', async () => {
    mockSessionState = {
      status: 'spent',
      sessionId: 'sess-1',
      tableNumber: 'Table 1',
    }

    const user = userEvent.setup()
    render(<GuestItemDialog item={mockItem} onOpenChange={mockOnOpenChange} />)

    await user.click(screen.getByText('Mild Spice'))

    const addBtn = screen.getByRole('button', { name: /add · \$5\.00/i })
    expect(addBtn).toBeDisabled()
  })
})
