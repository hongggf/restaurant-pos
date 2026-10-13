import { apiFetch } from "@/lib/api/client"
import type { components } from "@/lib/api/schema"

export type LoginRequest = components["schemas"]["LoginRequest"]
export type LoginResponse = components["schemas"]["LoginResponse"]

export function login(credentials: LoginRequest) {
  return apiFetch<LoginResponse>("/auth/login", {
    method: "POST",
    body: JSON.stringify(credentials),
  })
}
