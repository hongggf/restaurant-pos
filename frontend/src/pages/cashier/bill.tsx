import { ArrowLeft, ReceiptText, WifiOff } from "lucide-react"
import { Link, useLocation, useNavigate, useParams } from "react-router"

import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert"
import { Button } from "@/components/ui/button"
import {
  Card,
  CardContent,
  CardHeader,
  CardTitle,
} from "@/components/ui/card"
import { Skeleton } from "@/components/ui/skeleton"
import { BillSummary } from "@/features/payments/components/bill-summary"
import { PaymentPanel } from "@/features/payments/components/payment-panel"
import { useBill } from "@/features/payments/hooks/use-payment"
import { useIsOffline } from "@/hooks/use-offline"

export function BillPage() {
  const { sessionId = "" } = useParams()
  const navigate = useNavigate()
  const location = useLocation() as { state?: { tableNumber?: string } }

  const { data: bill, isPending, isLoadingError, isRefetchError, error, refetch } = useBill(sessionId)
  const tableNumber = bill?.tableNumber ?? location.state?.tableNumber
  const isOffline = useIsOffline()

  return (
    <>

      {isOffline && (
        <Alert variant="destructive" className="mb-4 bg-destructive/10">
          <WifiOff className="size-4" />
          <AlertTitle>You are offline</AlertTitle>
          <AlertDescription>
            Showing cached data. New orders or updates may fail to save.
          </AlertDescription>
        </Alert>
      )}

      <div className="flex items-center gap-2">
        <Button size="icon" variant="ghost" asChild>
          <Link to={`/cashier/sessions/${sessionId}`} state={{ tableNumber }}>
            <ArrowLeft />
            <span className="sr-only">Back to session</span>
          </Link>
        </Button>
        <div>
          <h1 className="text-2xl font-semibold tracking-tight">Bill</h1>
          <p className="text-muted-foreground text-sm">
            {tableNumber ? `Table ${tableNumber} · ` : ""}
            All rounds in this session, one shared bill.
          </p>
        </div>
      </div>

            {isRefetchError && (
        <Alert variant="destructive" className="mb-4">
          <AlertTitle>Connection lost</AlertTitle>
          <AlertDescription>Reconnecting to the server...</AlertDescription>
        </Alert>
      )}

      {isPending ? (
        <div className="grid gap-4 lg:grid-cols-2">
          <Skeleton className="h-72 rounded-xl" />
          <Skeleton className="h-72 rounded-xl" />
        </div>
      ) : isLoadingError ? (
        <Alert variant="destructive">
          <AlertTitle>Couldn&apos;t load the bill</AlertTitle>
          <AlertDescription>
            <p>{error.message}</p>
            <Button
              variant="outline"
              size="sm"
              className="mt-2"
              onClick={() => refetch()}
            >
              Try again
            </Button>
          </AlertDescription>
        </Alert>
      ) : bill.sessionStatus === "CLOSED" ? (
        <Alert>
          <ReceiptText />
          <AlertTitle>This session is already paid</AlertTitle>
          <AlertDescription>
            <Button
              variant="outline"
              size="sm"
              className="mt-2"
              onClick={() =>
                navigate(`/cashier/sessions/${sessionId}/receipt`, {
                  state: { tableNumber },
                })
              }
            >
              View receipt
            </Button>
          </AlertDescription>
        </Alert>
      ) : (
        <div className="grid items-start gap-4 lg:grid-cols-2">
          <Card>
            <CardHeader>
              <CardTitle>Order summary</CardTitle>
            </CardHeader>
            <CardContent>
              <BillSummary bill={bill} />
            </CardContent>
          </Card>
          <Card>
            <CardHeader>
              <CardTitle>Payment</CardTitle>
            </CardHeader>
            <CardContent>
              <PaymentPanel
                sessionId={sessionId}
                bill={bill}
                onPaid={() =>
                  navigate(`/cashier/sessions/${sessionId}/receipt`, {
                    state: { tableNumber },
                  })
                }
              />
            </CardContent>
          </Card>
        </div>
      )}
    </>
  )
}
