import type { components } from "@/lib/api/schema"

export type Category = components["schemas"]["CategoryResponse"]
export type CategoryPage =
  components["schemas"]["PageResponseCategoryResponse"]
export type CategoryCreateRequest =
  components["schemas"]["CategoryCreateRequest"]
export type CategoryUpdateRequest =
  components["schemas"]["CategoryUpdateRequest"]
