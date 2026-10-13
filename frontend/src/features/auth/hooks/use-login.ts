import { useMutation } from "@tanstack/react-query"

import { saveToken } from "@/lib/auth/token"
import { login } from "../api/login"

export function useLogin() {
  return useMutation({
    mutationFn: login,
    onSuccess: (data) => {
      if (data.accessToken && data.expiresInMs) {
        saveToken(data.accessToken, data.expiresInMs)
      }
    },
  })
}
