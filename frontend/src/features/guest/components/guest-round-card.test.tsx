import { describe, it, expect } from 'vitest'
import { render, screen } from '@testing-library/react'
import { GuestRoundCard } from './guest-round-card'
import type { OrderRound } from '../types'

describe('<GuestRoundCard />', () => {
  const mockRound: OrderRound = {
    id: 'round-1',
    roundNumber: 1,
    fulfillmentStatus: 'NEW',
    paymentStatus: 'UNPAID',
    subtotal: 10,
    vatRate: 0.1,
    vatAmount: 1,
    grandTotal: 11,
    sentAt: new Date().toISOString(),
    lines: [
      {
        id: 'line-1',
        menuItemId: 'item-1',
        nameEn: 'Beef Ramen',
        nameKm: 'មីសាច់គោ',
        basePrice: 8,
        unitPrice: 10,
        quantity: 1,
        lineTotal: 10,
        remark: 'Extra broth',
        voided: false,
        selections: [
          {
            modifierOptionId: 'mod-1',
            nameEn: 'Boiled Egg',
            nameKm: 'ពងមាន់',
            unitPrice: 2,
            quantity: 1,
          },
        ],
      },
    ],
  }

  it('renders round number, status badge, items, and totals', () => {
    render(<GuestRoundCard round={mockRound} />)

    expect(screen.getByText('Round #1')).toBeInTheDocument()
    expect(screen.getByText('SENT')).toBeInTheDocument()
    expect(screen.getByText(/1× Beef Ramen/)).toBeInTheDocument()
    expect(screen.getByText(/Boiled Egg/)).toBeInTheDocument()
    expect(screen.getByText(/“Extra broth”/)).toBeInTheDocument()
    expect(screen.getByText(/Subtotal/)).toBeInTheDocument()
    expect(screen.getByText(/\$11\.00/)).toBeInTheDocument()
  })

  it('renders CANCELLED status without footer totals', () => {
    const cancelledRound: OrderRound = {
      ...mockRound,
      fulfillmentStatus: 'CANCELLED',
      paymentStatus: 'UNPAID',
    }

    render(<GuestRoundCard round={cancelledRound} />)

    expect(screen.getByText('CANCELLED')).toBeInTheDocument()
    expect(screen.queryByText(/Subtotal/)).not.toBeInTheDocument()
  })
})
