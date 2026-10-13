import { Navigate } from "react-router"

import { Button } from "@/components/ui/button"
import { clearToken, getRoles, getToken } from "@/lib/auth/token"

export function RoleLanding() {
  if (!getToken()) {
    return <Navigate to="/login" replace />
  }

  const roles = getRoles()
  if (roles.includes("ADMIN")) {
    return <Navigate to="/admin" replace />
  }
  if (roles.includes("CASHIER")) {
    return <Navigate to="/cashier" replace />
  }
  if (roles.includes("CHEF")) {
    return <Navigate to="/kitchen" replace />
  }

  // Token without a recognized role — nothing to show but the door.
  return (
    <div className="bg-muted flex min-h-svh flex-col items-center justify-center gap-4 p-6">
      <p className="text-muted-foreground text-center">
        Your account doesn&apos;t have access to any app.
      </p>
      <Button
        variant="outline"
        onClick={() => {
          clearToken()
          window.location.assign("/login")
        }}
      >
        Sign out
      </Button>
    </div>
  )
}
