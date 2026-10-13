import { describe, it, expect } from 'vitest'
import { loginSchema } from './auth/schemas/login'
import { tableSchema } from './tables/schemas/table'
import { categorySchema } from './categories/schemas/category'
import { menuItemSchema } from './menu/schemas/menu-item'
import { modifierGroupSchema } from './modifiers/schemas/modifier-group'

describe('Form Validation Schemas', () => {
  describe('modifierGroupSchema', () => {
    // Columns the seeder never fills come back from the API as null, not
    // undefined. These fields have no input, so a rejection here used to
    // surface as a submit button that did nothing at all.
    const asLoadedFromApi = {
      nameEn: 'Noodles & Rice',
      nameKm: 'Noodles & Rice',
      minChoice: 0,
      maxChoice: 5,
      active: true,
      options: [
        {
          id: 'opt-1',
          nameEn: 'Mee Chiet Noodles',
          nameKm: 'Mee Chiet Noodles',
          unitPrice: 0.7,
          available: true,
          imageUrl: '/food-images/mee-chiet-noodles.jpg',
          packSize: null,
        },
      ],
    }

    it('accepts a group loaded from the API with a null packSize', () => {
      const result = modifierGroupSchema.safeParse(asLoadedFromApi)
      expect(result.success).toBe(true)
    })

    it('accepts a null imageUrl as well', () => {
      const result = modifierGroupSchema.safeParse({
        ...asLoadedFromApi,
        options: [{ ...asLoadedFromApi.options[0], imageUrl: null }],
      })
      expect(result.success).toBe(true)
    })

    it('still rejects an option with no name', () => {
      const result = modifierGroupSchema.safeParse({
        ...asLoadedFromApi,
        options: [{ ...asLoadedFromApi.options[0], nameEn: '' }],
      })
      expect(result.success).toBe(false)
    })
  })

  describe('loginSchema', () => {
    it('accepts valid credentials', () => {
      const result = loginSchema.safeParse({
        username: 'admin',
        password: 'password123',
      })
      expect(result.success).toBe(true)
    })

    it('rejects empty username or password', () => {
      const result = loginSchema.safeParse({ username: '', password: '' })
      expect(result.success).toBe(false)
    })
  })

  describe('tableSchema', () => {
    it('validates correct table data', () => {
      const result = tableSchema.safeParse({
        tableNumber: 'Table 01',
        active: true,
      })
      expect(result.success).toBe(true)
    })

    it('rejects empty table number', () => {
      const result = tableSchema.safeParse({
        tableNumber: '',
        active: true,
      })
      expect(result.success).toBe(false)
    })
  })

  describe('categorySchema', () => {
    it('validates correct category data', () => {
      const result = categorySchema.safeParse({
        nameEn: 'Broth',
        nameKm: 'ទឹកស៊ុប',
        description: 'Flavorful base',
        sortOrder: 1,
        active: true,
        allowNotes: true,
      })
      expect(result.success).toBe(true)
    })

    it('rejects negative sort order', () => {
      const result = categorySchema.safeParse({
        nameEn: 'Broth',
        nameKm: 'ទឹកស៊ុប',
        description: '',
        sortOrder: -1,
        active: true,
        allowNotes: true,
      })
      expect(result.success).toBe(false)
    })
  })

  describe('menuItemSchema', () => {
    it('validates correct menuItem data', () => {
      const result = menuItemSchema.safeParse({
        nameEn: 'Beef Slice',
        nameKm: 'សាច់គោបន្ទះ',
        descriptionEn: 'Fresh sliced beef',
        descriptionKm: 'សាច់គោស្រស់',
        price: 3.5,
        currencyCode: 'USD',
        categoryId: 'cat-123',
        available: true,
        station: 'KITCHEN',
      })
      expect(result.success).toBe(true)
    })

    it('rejects negative price', () => {
      const result = menuItemSchema.safeParse({
        nameEn: 'Beef Slice',
        nameKm: 'សាច់គោបន្ទះ',
        descriptionEn: '',
        descriptionKm: '',
        price: -1,
        currencyCode: 'USD',
        categoryId: 'cat-123',
        available: true,
        station: 'KITCHEN',
      })
      expect(result.success).toBe(false)
    })
  })
})
