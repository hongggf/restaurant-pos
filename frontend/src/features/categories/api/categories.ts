import { apiFetch } from "@/lib/api/client"
import type {
  Category,
  CategoryCreateRequest,
  CategoryPage,
  CategoryUpdateRequest,
} from "../types"

export function listCategories(params: { page: number; size: number }) {
  const query = new URLSearchParams({
    page: String(params.page),
    size: String(params.size),
    sort: "sortOrder,asc",
  })
  return apiFetch<CategoryPage>(`/categories?${query}`)
}

export function createCategory(body: CategoryCreateRequest) {
  return apiFetch<Category>("/categories", {
    method: "POST",
    body: JSON.stringify(body),
  })
}

export function updateCategory(id: string, body: CategoryUpdateRequest) {
  return apiFetch<Category>(`/categories/${id}`, {
    method: "PUT",
    body: JSON.stringify(body),
  })
}

export function deleteCategory(id: string) {
  return apiFetch<void>(`/categories/${id}`, { method: "DELETE" })
}
