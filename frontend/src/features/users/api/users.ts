import { apiFetch } from "@/lib/api/client"
import type {
  PasswordResetRequest,
  User,
  UserCreateRequest,
  UserUpdateRequest,
} from "../types"

export function listUsers() {
  return apiFetch<User[]>("/users")
}

// User creation lives on the auth controller, not /users.
export function createUser(body: UserCreateRequest) {
  return apiFetch<User>("/auth/register", {
    method: "POST",
    body: JSON.stringify(body),
  })
}

export function updateUser(id: string, body: UserUpdateRequest) {
  return apiFetch<User>(`/users/${id}`, {
    method: "PUT",
    body: JSON.stringify(body),
  })
}

export function resetPassword(id: string, body: PasswordResetRequest) {
  return apiFetch<void>(`/users/${id}/password`, {
    method: "PUT",
    body: JSON.stringify(body),
  })
}

export function deleteUser(id: string) {
  return apiFetch<void>(`/users/${id}`, { method: "DELETE" })
}
