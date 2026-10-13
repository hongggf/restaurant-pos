import { ReceiptText } from "lucide-react"
import { Link } from "react-router"

import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert"
import { Button } from "@/components/ui/button"
import {
  Empty,
  EmptyDescription,
  EmptyHeader,
  EmptyMedia,
  EmptyTitle,
} from "@/components/ui/empty"
import { Separator } from "@/components/ui/separator"
import { Skeleton } from "@/components/ui/skeleton"
import { DualPrice } from "@/features/guest/components/dual-price"
import { GuestRoundCard } from "@/features/guest/components/guest-round-card"
import { useGuestOrders } from "@/features/guest/hooks/use-guest-orders"
import { useGuestSession } from "@/features/guest/hooks/use-guest-session"

import { useLanguage } from "@/lib/language-context"

export function GuestOrdersPage() {
  const { t } = useLanguage()
  const session = useGuestSession()
  const spent = session.status === "spent"
  const { data, isPending, isError, error, refetch } = useGuestOrders()

  const rounds = data?.rounds ?? []

  return (
    <>
      <div className="flex items-center justify-between">
        <h1 className="text-lg font-semibold">{t("yourOrders")}</h1>
        <Button variant="outline" size="sm" asChild>
          <Link to="/guest/menu">{t("menu")}</Link>
        </Button>
      </div>

      {spent && (
        <Alert>
          <AlertTitle>{t("orderSentTitle")}</AlertTitle>
          <AlertDescription>
            {t("orderSentDesc")}
          </AlertDescription>
        </Alert>
      )}

      {isPending ? (
        <div className="space-y-3">
          {Array.from({ length: 2 }, (_, i) => (
            <Skeleton key={i} className="h-32 rounded-lg" />
          ))}
        </div>
      ) : isError ? (
        <Alert variant="destructive">
          <AlertTitle>{t("couldntLoadOrders")}</AlertTitle>
          <AlertDescription>
            <p>{error.message}</p>
            <Button variant="outline" size="sm" className="mt-2" onClick={() => refetch()}>
              {t("tryAgain")}
            </Button>
          </AlertDescription>
        </Alert>
      ) : rounds.length === 0 ? (
        <Empty className="border border-dashed">
          <EmptyHeader>
            <EmptyMedia variant="icon">
              <ReceiptText />
            </EmptyMedia>
            <EmptyTitle>{t("nothingOrderedTitle")}</EmptyTitle>
            <EmptyDescription>
              {t("nothingOrderedDesc")}
            </EmptyDescription>
          </EmptyHeader>
        </Empty>
      ) : (
        <>
          <div className="space-y-3">
            {rounds.map((round) => (
              <GuestRoundCard key={round.id} round={round} />
            ))}
          </div>

          <Separator />

          <div className="flex items-center justify-between">
            <span className="text-muted-foreground text-sm">{t("runningTotal")}</span>
            <DualPrice
              usd={data?.runningGrandTotal}
              khr={data?.runningGrandTotalKhr}
              className="font-semibold"
            />
          </div>
        </>
      )}
    </>
  )
}
