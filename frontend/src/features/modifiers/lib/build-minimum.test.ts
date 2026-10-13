import { describe, it, expect } from 'vitest'

import {
  BUILD_MINIMUM,
  buildMinimumProgress,
  formatGroupList,
} from './build-minimum'
import type { AttachedModifierGroup, ModifierOption } from '../types'

/** Real seeded prices: Meat 0.90, Meat Ball 0.30, Veggie 0.30, Noodles 0.70. */
function group(nameEn: string, options: [string, number][]): AttachedModifierGroup {
  return {
    sortOrder: 0,
    group: {
      nameEn,
      options: options.map(([id, unitPrice]) => ({ id, nameEn: id, unitPrice })),
    },
  } as AttachedModifierGroup
}

const GROUPS: AttachedModifierGroup[] = [
  group('Flavor', [['dry', 0], ['sichuan', 0]]),
  group('Meat', [['beef', 0.9], ['chicken', 0.9]]),
  group('Meat Ball', [['fishball', 0.3], ['crab', 0.3]]),
  group('Veggie', [['broccoli', 0.3], ['tofu', 0.3]]),
  group('Noodles & Rice', [['noodles', 0.7], ['rice', 0.7]]),
  group('Extra Love Add-Ons', [
    ['tea', 1.58],
    ['apple-tea', 1.58],
  ]),
  // Real seeded add-ons. Note "Full Steamed Rice" also exists in the counted
  // Noodles & Rice group at the same price — matching is by group, not by name.
  group('Choice of Adds-On', [
    ['addon-full-rice', 0.7],
    ['addon-half-rice', 0.35],
    ['addon-dumplings', 1.5],
  ]),
]

function pick(...entries: [string, number][]) {
  return Object.fromEntries(
    entries.map(([id, quantity]) => [
      id,
      { option: { id } as ModifierOption, quantity },
    ])
  )
}

describe('buildMinimumProgress', () => {
  it('does not apply to an item with none of the counted groups', () => {
    const result = buildMinimumProgress([group('Choice of Adds-On', [['rice', 0.7]])], {})
    expect(result.applies).toBe(false)
    expect(result.shortfall).toBe(0)
  })

  it('requires the full minimum when nothing is selected', () => {
    const result = buildMinimumProgress(GROUPS, {})
    expect(result.applies).toBe(true)
    expect(result.total).toBe(0)
    expect(result.shortfall).toBe(BUILD_MINIMUM)
  })

  it('ignores flavour and add-ons, which do not count toward the minimum', () => {
    const result = buildMinimumProgress(GROUPS, pick(['dry', 1], ['tea', 1]))
    expect(result.total).toBe(0)
    expect(result.shortfall).toBe(BUILD_MINIMUM)
  })

  it('excludes Extra Love Add-Ons and Choice of Adds-On entirely', () => {
    // $1.58 + $1.58 + $1.50 + $0.70 = $5.36 of add-ons, and still nothing
    // counts: none of it is meat, meatball, veggie, noodles or rice.
    const result = buildMinimumProgress(
      GROUPS,
      pick(
        ['dry', 1],
        ['tea', 1],
        ['apple-tea', 1],
        ['addon-dumplings', 1],
        ['addon-full-rice', 1]
      )
    )
    expect(result.applies).toBe(true)
    expect(result.total).toBe(0)
    expect(result.shortfall).toBe(BUILD_MINIMUM)
  })

  it('blocks a build whose order value clears $3 only because of add-ons', () => {
    // $2.80 of real food + $1.58 tea = $4.38 on the bill, but only $2.80
    // counts, so the guest is still 20 cents short of a valid bowl.
    const result = buildMinimumProgress(
      GROUPS,
      pick(['noodles', 4], ['tea', 1])
    )
    expect(result.total).toBeCloseTo(2.8, 2)
    expect(result.shortfall).toBeCloseTo(0.2, 2)
  })

  it('counts Full Steamed Rice from Noodles & Rice but not the identically named add-on', () => {
    const counted = buildMinimumProgress(GROUPS, pick(['rice', 1]))
    const notCounted = buildMinimumProgress(GROUPS, pick(['addon-full-rice', 1]))

    expect(counted.total).toBeCloseTo(0.7, 2)
    expect(notCounted.total).toBe(0)
  })

  it('counts option quantity, not just the number of options', () => {
    const result = buildMinimumProgress(GROUPS, pick(['beef', 2]))
    expect(result.total).toBeCloseTo(1.8, 2)
    expect(result.shortfall).toBeCloseTo(1.2, 2)
  })

  it('reports the remaining shortfall part-way through a build', () => {
    const result = buildMinimumProgress(GROUPS, pick(['beef', 1], ['broccoli', 1]))
    expect(result.total).toBeCloseTo(1.2, 2)
    expect(result.shortfall).toBeCloseTo(1.8, 2)
  })

  // Each of these sums to exactly $3.00 but lands on 2.9999999999999996 in
  // binary floating point — a naive `total >= 3` would block a paying customer.
  it.each([
    ['0.70x3 + 0.90x1', pick(['noodles', 3], ['beef', 1])],
    ['0.90x1 + 0.70x3', pick(['beef', 1], ['rice', 3])],
    ['0.30x3 + 0.70x3', pick(['broccoli', 3], ['noodles', 3])],
    ['0.90x2 + 0.30x4', pick(['beef', 2], ['fishball', 4])],
  ])('treats exactly $3.00 as satisfied (%s)', (_label, selected) => {
    const result = buildMinimumProgress(GROUPS, selected)
    expect(result.shortfall).toBe(0)
  })

  it('is satisfied above the minimum', () => {
    const result = buildMinimumProgress(GROUPS, pick(['beef', 4]))
    expect(result.total).toBeCloseTo(3.6, 2)
    expect(result.shortfall).toBe(0)
  })

  it('still blocks just below the minimum', () => {
    // 0.70 x 4 = 2.80, the closest a noodles-only build gets to $3.00.
    const result = buildMinimumProgress(GROUPS, pick(['noodles', 4]))
    expect(result.total).toBeCloseTo(2.8, 2)
    expect(result.shortfall).toBeCloseTo(0.2, 2)
  })

  it('matches group names case-insensitively', () => {
    const result = buildMinimumProgress(
      [group('MEAT', [['beef', 0.9]])],
      pick(['beef', 1])
    )
    expect(result.applies).toBe(true)
    expect(result.total).toBeCloseTo(0.9, 2)
  })
})

describe('formatGroupList', () => {
  it('renders the counted groups as a disjunction', () => {
    expect(formatGroupList(['Meat', 'Meat Ball', 'Veggie', 'Noodles & Rice'])).toBe(
      'Meat, Meat Ball, Veggie, or Noodles & Rice'
    )
  })
})
