import { Navigate, Outlet, useLocation } from "react-router"

import { getRoles, getToken, type Role } from "@/lib/auth/token"

export function RequireAuth({ roles }: { roles?: Role[] }) {
  const location = useLocation()

  if (!getToken()) {
    return <Navigate to="/login" replace state={{ from: location }} />
  }
  if (roles && !getRoles().some((role) => roles.includes(role))) {
    return <Navigate to="/" replace />
  }
  return <Outlet />
}
