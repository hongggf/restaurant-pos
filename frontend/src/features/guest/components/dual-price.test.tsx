import { describe, it, expect } from 'vitest'
import { render, screen } from '@testing-library/react'
import { DualPrice } from './dual-price'

describe('<DualPrice />', () => {
  it('renders USD price only when khr is not provided', () => {
    render(<DualPrice usd={10.5} />)
    expect(screen.getByText(/\$10\.50/)).toBeInTheDocument()
  })

  it('renders both USD and KHR prices when khr is provided', () => {
    const { container } = render(<DualPrice usd={5} khr={20500} />)
    expect(container.textContent).toContain('$5.00')
    expect(container.textContent).toContain('20,500')
  })
})
