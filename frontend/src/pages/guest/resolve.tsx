import { useEffect, useState } from "react"
import { QrCode } from "lucide-react"
import { useNavigate, useSearchParams } from "react-router"

import {
  Empty,
  EmptyDescription,
  EmptyHeader,
  EmptyMedia,
  EmptyTitle,
} from "@/components/ui/empty"
import { Spinner } from "@/components/ui/spinner"
import { resolveGuestSession } from "@/features/guest/api/session"
import { guestSessionStore } from "@/features/guest/lib/session-store"
import { ApiError } from "@/lib/api/client"
import { getGuestToken, saveGuestSession } from "@/lib/auth/guest-token"

export function GuestResolvePage() {
  const [params] = useSearchParams()
  const qr = params.get("qr")
  const navigate = useNavigate()
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (qr) {
      guestSessionStore.setResolving()
      resolveGuestSession(qr)
        .then((session) => {
          if (
            !session.accessToken ||
            !session.expiresInMs ||
            !session.sessionId ||
            !session.tableNumber
          ) {
            throw new Error("Malformed session response")
          }
          saveGuestSession({
            accessToken: session.accessToken,
            expiresInMs: session.expiresInMs,
            sessionId: session.sessionId,
            tableNumber: session.tableNumber,
          })
          guestSessionStore.setBrowsing({
            sessionId: session.sessionId,
            tableNumber: session.tableNumber,
          })
          navigate("/guest/menu", { replace: true })
        })
        .catch((err: unknown) => {
          guestSessionStore.setNoSession()
          setError(err instanceof ApiError ? err.message : "Couldn't start your order.")
        })
      return
    }
    if (getGuestToken()) {
      navigate("/guest/menu", { replace: true })
    }
  }, [qr, navigate])

  if (qr && !error) {
    return (
      <div className="flex flex-1 items-center justify-center py-12">
        <Spinner className="size-6" />
      </div>
    )
  }

  return (
    <Empty>
      <EmptyHeader>
        <EmptyMedia variant="icon">
          <QrCode />
        </EmptyMedia>
        <EmptyTitle>Scan to order</EmptyTitle>
        <EmptyDescription>
          {error ?? "Scan the QR code on your table to start ordering."}
        </EmptyDescription>
      </EmptyHeader>
    </Empty>
  )
}
