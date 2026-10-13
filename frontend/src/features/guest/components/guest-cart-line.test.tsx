import { describe, it, expect, beforeEach, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { GuestCartLine } from './guest-cart-line'
import type { CartLine } from '../types'

const mockUpdateMutate = vi.fn()
const mockRemoveMutate = vi.fn()
let mockUpdatePending = false
let mockRemovePending = false

vi.mock('../hooks/use-guest-cart', () => ({
  useUpdateCartLine: () => ({
    mutate: mockUpdateMutate,
    isPending: mockUpdatePending,
  }),
  useRemoveCartLine: () => ({
    mutate: mockRemoveMutate,
    isPending: mockRemovePending,
  }),
}))

describe('<GuestCartLine />', () => {
  const baseLine: CartLine = {
    id: 'line-1',
    menuItemId: 'item-1',
    nameEn: 'Spicy Beef Noodle',
    nameKm: 'មីសាច់គោហឹរ',
    basePrice: 4.0,
    unitPrice: 6.5,
    quantity: 2,
    lineTotal: 13.0,
    remark: 'Extra spicy please',
    selections: [
      {
        modifierOptionId: 'mod-1',
        nameEn: 'Boiled Egg',
        unitPrice: 1.0,
        quantity: 2,
      },
      {
        modifierOptionId: 'mod-2',
        nameEn: 'Cilantro',
        unitPrice: 0.5,
        quantity: 1,
      },
    ],
  }

  beforeEach(() => {
    vi.clearAllMocks()
    mockUpdatePending = false
    mockRemovePending = false
  })

  it('renders item name, quantity, line total, selections, and remarks', () => {
    render(<GuestCartLine line={baseLine} />)

    expect(screen.getByText('Spicy Beef Noodle')).toBeInTheDocument()
    expect(screen.getByText('2')).toBeInTheDocument()
    expect(screen.getByText('$13.00')).toBeInTheDocument()
    expect(screen.getByText('2× Boiled Egg ($1.00 ea)')).toBeInTheDocument()
    expect(screen.getByText('Cilantro')).toBeInTheDocument()
    expect(screen.getByText('“Extra spicy please”')).toBeInTheDocument()
  })

  it('prices each option row by its own extended total', () => {
    render(<GuestCartLine line={baseLine} />)

    expect(screen.getByText('$2.00')).toBeInTheDocument() // 2 × 1.00 egg
    expect(screen.getByText('$0.50')).toBeInTheDocument() // 1 × 0.50 cilantro
  })

  it('offers an edit button on a built line', () => {
    render(<GuestCartLine line={baseLine} />)

    expect(screen.getByRole('button', { name: /edit line/i })).toBeInTheDocument()
  })

  it('omits the edit button on a line with nothing to configure', () => {
    const drink: CartLine = {
      id: 'line-3',
      menuItemId: 'item-3',
      nameEn: 'Coca-Cola Classic',
      unitPrice: 1.0,
      quantity: 1,
      lineTotal: 1.0,
    }

    render(<GuestCartLine line={drink} />)

    expect(screen.queryByRole('button', { name: /edit line/i })).not.toBeInTheDocument()
    expect(screen.getByRole('button', { name: /remove line/i })).toBeInTheDocument()
  })

  it('reconciles per-bowl option prices against the line total above quantity one', () => {
    render(<GuestCartLine line={baseLine} />)

    expect(screen.getByText('$6.50 each × 2')).toBeInTheDocument()
  })

  it('omits the reconciliation row at quantity one', () => {
    render(<GuestCartLine line={{ ...baseLine, quantity: 1 }} />)

    expect(screen.queryByText(/each ×/)).not.toBeInTheDocument()
  })

  it('labels a zero-priced option Free rather than $0.00', () => {
    const freeOption: CartLine = {
      ...baseLine,
      selections: [
        { modifierOptionId: 'mod-3', nameEn: 'Dry Malatang', unitPrice: 0, quantity: 1 },
      ],
    }

    render(<GuestCartLine line={freeOption} />)

    expect(screen.getByText('Free')).toBeInTheDocument()
    expect(screen.queryByText('$0.00')).not.toBeInTheDocument()
  })

  it('renders correctly without selections and remarks', () => {
    const minimalLine: CartLine = {
      id: 'line-2',
      menuItemId: 'item-2',
      nameEn: 'Steamed Rice',
      unitPrice: 1.0,
      quantity: 1,
      lineTotal: 1.0,
    }

    render(<GuestCartLine line={minimalLine} />)

    expect(screen.getByText('Steamed Rice')).toBeInTheDocument()
    expect(screen.getByText('$1.00')).toBeInTheDocument()
    expect(screen.queryByText(/“/)).not.toBeInTheDocument()
    // With nothing stacked on top, a base row would just restate the total.
    expect(screen.queryByText('Base price')).not.toBeInTheDocument()
  })

  it('calls updateLine when increasing quantity', async () => {
    const user = userEvent.setup()
    render(<GuestCartLine line={baseLine} />)

    const increaseBtn = screen.getByRole('button', { name: /increase quantity/i })
    await user.click(increaseBtn)

    expect(mockUpdateMutate).toHaveBeenCalledTimes(1)
    expect(mockUpdateMutate).toHaveBeenCalledWith({
      lineId: 'line-1',
      body: {
        quantity: 3,
        remark: 'Extra spicy please',
        selections: [
          { modifierOptionId: 'mod-1', quantity: 2 },
          { modifierOptionId: 'mod-2', quantity: 1 },
        ],
      },
    })
  })

  it('calls updateLine when decreasing quantity from greater than 1', async () => {
    const user = userEvent.setup()
    render(<GuestCartLine line={baseLine} />)

    const decreaseBtn = screen.getByRole('button', { name: /decrease quantity/i })
    await user.click(decreaseBtn)

    expect(mockUpdateMutate).toHaveBeenCalledTimes(1)
    expect(mockUpdateMutate).toHaveBeenCalledWith({
      lineId: 'line-1',
      body: {
        quantity: 1,
        remark: 'Extra spicy please',
        selections: [
          { modifierOptionId: 'mod-1', quantity: 2 },
          { modifierOptionId: 'mod-2', quantity: 1 },
        ],
      },
    })
  })

  it('disables decrease button when quantity is 1', () => {
    const singleQtyLine: CartLine = {
      ...baseLine,
      quantity: 1,
    }

    render(<GuestCartLine line={singleQtyLine} />)

    const decreaseBtn = screen.getByRole('button', { name: /decrease quantity/i })
    expect(decreaseBtn).toBeDisabled()
  })

  it('calls removeLine when delete button is clicked', async () => {
    const user = userEvent.setup()
    render(<GuestCartLine line={baseLine} />)

    const removeBtn = screen.getByRole('button', { name: /remove line/i })
    await user.click(removeBtn)

    expect(mockRemoveMutate).toHaveBeenCalledTimes(1)
    expect(mockRemoveMutate).toHaveBeenCalledWith('line-1')
  })

  it('disables all action buttons when disabled prop is true', () => {
    render(<GuestCartLine line={baseLine} disabled />)

    expect(screen.getByRole('button', { name: /decrease quantity/i })).toBeDisabled()
    expect(screen.getByRole('button', { name: /increase quantity/i })).toBeDisabled()
    expect(screen.getByRole('button', { name: /remove line/i })).toBeDisabled()
    expect(screen.getByRole('button', { name: /edit line/i })).toBeDisabled()
  })

  it('disables all action buttons when update or remove mutation is pending', () => {
    mockUpdatePending = true
    render(<GuestCartLine line={baseLine} />)

    expect(screen.getByRole('button', { name: /decrease quantity/i })).toBeDisabled()
    expect(screen.getByRole('button', { name: /increase quantity/i })).toBeDisabled()
    expect(screen.getByRole('button', { name: /remove line/i })).toBeDisabled()
  })
})
