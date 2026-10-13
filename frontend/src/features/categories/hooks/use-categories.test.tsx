import { describe, it, expect, beforeEach, vi } from 'vitest'
import { renderHook, waitFor } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { toast } from 'sonner'
import { useCategories, useCreateCategory, useUpdateCategory, useDeleteCategory } from './use-categories'
import * as categoriesApi from '../api/categories'
import { ApiError } from '@/lib/api/client'
import type { Category } from '../types'

vi.mock('sonner', () => ({
  toast: {
    success: vi.fn(),
    error: vi.fn(),
  },
}))

vi.mock('../api/categories', () => ({
  listCategories: vi.fn(),
  createCategory: vi.fn(),
  updateCategory: vi.fn(),
  deleteCategory: vi.fn(),
}))

describe('use-categories', () => {
  let queryClient: QueryClient

  beforeEach(() => {
    vi.clearAllMocks()
    queryClient = new QueryClient({
      defaultOptions: {
        queries: { retry: false },
      },
    })
  })

  function createWrapper() {
    return ({ children }: { children: React.ReactNode }) => (
      <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
    )
  }

  const mockCategory: Category = {
    id: '1',
    nameEn: 'Drinks',
    nameKm: 'ភេសជ្ជៈ',
    description: 'Refreshing drinks',
    sortOrder: 1,
    active: true,
    allowNotes: true,
  }

  describe('useCategories', () => {
    it('fetches categories', async () => {
      vi.mocked(categoriesApi.listCategories).mockResolvedValue({
        content: [mockCategory],
        page: 0, size: 10, totalElements: 1, totalPages: 1,
      })

      const { result } = renderHook(() => useCategories(0), { wrapper: createWrapper() })
      
      await waitFor(() => expect(result.current.isSuccess).toBe(true))
      expect(result.current.data?.content).toEqual([mockCategory])
      expect(categoriesApi.listCategories).toHaveBeenCalledWith({ page: 0, size: 10 })
    })
  })

  describe('useCreateCategory', () => {
    it('creates a category and invalidates queries on success', async () => {
      vi.mocked(categoriesApi.createCategory).mockResolvedValue(mockCategory)

      const { result } = renderHook(() => useCreateCategory(), { wrapper: createWrapper() })
      result.current.mutate({ nameEn: 'Drinks', nameKm: 'ភេសជ្ជៈ', sortOrder: 1, active: true, allowNotes: true })
      
      await waitFor(() => expect(result.current.isSuccess).toBe(true))
      
      expect(toast.success).toHaveBeenCalledWith('Category "Drinks" created')
      expect(categoriesApi.createCategory).toHaveBeenCalled()
    })

    it('shows error toast on failure', async () => {
      vi.mocked(categoriesApi.createCategory).mockRejectedValue(new ApiError('Failed to create', 400))

      const { result } = renderHook(() => useCreateCategory(), { wrapper: createWrapper() })
      result.current.mutate({ nameEn: 'Drinks', nameKm: 'ភេសជ្ជៈ', sortOrder: 1, active: true, allowNotes: true })
      
      await waitFor(() => expect(result.current.isError).toBe(true))
      
      expect(toast.error).toHaveBeenCalledWith('Failed to create')
    })
  })

  describe('useUpdateCategory', () => {
    it('updates a category and shows success toast', async () => {
      vi.mocked(categoriesApi.updateCategory).mockResolvedValue(mockCategory)

      const { result } = renderHook(() => useUpdateCategory(), { wrapper: createWrapper() })
      result.current.mutate({ id: '1', nameEn: 'Drinks', nameKm: 'ភេសជ្ជៈ', sortOrder: 1, active: true, allowNotes: true })
      
      await waitFor(() => expect(result.current.isSuccess).toBe(true))
      
      expect(toast.success).toHaveBeenCalledWith('Category "Drinks" updated')
      expect(categoriesApi.updateCategory).toHaveBeenCalledWith('1', expect.any(Object))
    })
  })

  describe('useDeleteCategory', () => {
    it('deletes a category and shows success toast', async () => {
      vi.mocked(categoriesApi.deleteCategory).mockResolvedValue(undefined)

      const { result } = renderHook(() => useDeleteCategory(), { wrapper: createWrapper() })
      result.current.mutate('1')
      
      await waitFor(() => expect(result.current.isSuccess).toBe(true))
      
      expect(toast.success).toHaveBeenCalledWith('Category deleted')
      expect(categoriesApi.deleteCategory).toHaveBeenCalledWith('1', expect.anything())
    })
  })
})
