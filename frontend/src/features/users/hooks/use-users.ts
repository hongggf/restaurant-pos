import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { toast } from "sonner"

import type { ApiError } from "@/lib/api/client"
import {
  createUser,
  deleteUser,
  listUsers,
  resetPassword,
  updateUser,
} from "../api/users"
import type { PasswordResetRequest, UserUpdateRequest } from "../types"

export function useUsers() {
  return useQuery({
    queryKey: ["users", "list"],
    queryFn: listUsers,
  })
}

function useInvalidateUsers() {
  const queryClient = useQueryClient()
  return () => queryClient.invalidateQueries({ queryKey: ["users"] })
}

export function useCreateUser() {
  const invalidate = useInvalidateUsers()
  return useMutation({
    mutationFn: createUser,
    onSuccess: (data) => {
      invalidate()
      toast.success(`User "${data.username}" created`)
    },
    onError: (error: ApiError) => toast.error(error.message),
  })
}

export function useUpdateUser() {
  const invalidate = useInvalidateUsers()
  return useMutation({
    mutationFn: ({ id, ...body }: UserUpdateRequest & { id: string }) =>
      updateUser(id, body),
    onSuccess: (data) => {
      invalidate()
      toast.success(`User "${data.username}" updated`)
    },
    onError: (error: ApiError) => toast.error(error.message),
  })
}

export function useResetPassword() {
  return useMutation({
    mutationFn: ({ id, ...body }: PasswordResetRequest & { id: string }) =>
      resetPassword(id, body),
    onSuccess: () => toast.success("Password updated"),
    onError: (error: ApiError) => toast.error(error.message),
  })
}

export function useDeleteUser() {
  const invalidate = useInvalidateUsers()
  return useMutation({
    mutationFn: deleteUser,
    onSuccess: () => {
      invalidate()
      toast.success("User deleted")
    },
    onError: (error: ApiError) => toast.error(error.message),
  })
}
