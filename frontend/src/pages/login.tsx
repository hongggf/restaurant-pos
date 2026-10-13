import { Navigate } from "react-router"

import { LoginForm } from "@/features/auth/components/login-form"
import { getToken } from "@/lib/auth/token"

export function LoginPage() {
  if (getToken()) {
    return <Navigate to="/" replace />
  }

  return (
    <div className="bg-muted flex min-h-svh w-full items-center justify-center p-6 md:p-10">
      <div className="w-full max-w-sm">
        <LoginForm />
      </div>
    </div>
  )
}
