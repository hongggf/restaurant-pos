import type { components } from "@/lib/api/schema"

export type DiningTable = components["schemas"]["TableResponse"]
export type DiningTablePage =
  components["schemas"]["PageResponseTableResponse"]
export type TableCreateRequest = components["schemas"]["TableCreateRequest"]
export type TableUpdateRequest = components["schemas"]["TableUpdateRequest"]
