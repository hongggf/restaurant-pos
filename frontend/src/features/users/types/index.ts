import type { components } from "@/lib/api/schema"

export type User = components["schemas"]["UserResponse"]
export type UserCreateRequest = components["schemas"]["UserCreateRequest"]
export type UserUpdateRequest = components["schemas"]["UserUpdateRequest"]
export type PasswordResetRequest =
  components["schemas"]["PasswordResetRequest"]
export type UserRole = "ADMIN" | "CASHIER" | "CHEF"
