import { Navigate } from "react-router"

import { Button } from "@/components/ui/button"
import { useGuestSession } from "@/features/guest/hooks/use-guest-session"

export function GuestSessionGuard({ children }: { children: React.ReactNode }) {
  const session = useGuestSession()

  if (session.status === "no-session") {
    return <Navigate to="/guest" replace />
  }

  if (session.status === "closed") {
    return (
      <div className="flex flex-1 flex-col items-center justify-center gap-4 py-12 text-center">
        <p className="text-muted-foreground">This session has ended.</p>
        <Button variant="outline" asChild>
          <a href="/guest">Scan the table QR code to start a new order</a>
        </Button>
      </div>
    )
  }

  return <>{children}</>
}
