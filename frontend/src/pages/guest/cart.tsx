import { ShoppingCart } from "lucide-react"
import { Link, useNavigate } from "react-router"

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
import { Spinner } from "@/components/ui/spinner"
import { DualPrice } from "@/features/guest/components/dual-price"
import { GuestCartLine } from "@/features/guest/components/guest-cart-line"
import { useGuestCart, useSendCart } from "@/features/guest/hooks/use-guest-cart"
import { useGuestSession } from "@/features/guest/hooks/use-guest-session"
import { formatPercent, formatPrice } from "@/lib/format"
import { useLanguage } from "@/lib/language-context"

export function GuestCartPage() {
  const { t } = useLanguage()
  const navigate = useNavigate()
  const session = useGuestSession()
  const spent = session.status === "spent"
  const { data: cart, isPending, isError, error, refetch } = useGuestCart()
  const sendCart = useSendCart()

  const lines = cart?.lines ?? []
  // Rate is a fraction off the API and may be absent; fall back to a bare label.
  const vatLabel = cart?.vatRate != null ? `${t("vat")} (${formatPercent(cart.vatRate)})` : t("vat")

  return (
    <>
      <div className="flex items-center justify-between">
        <h1 className="text-lg font-semibold">{t("yourCart")}</h1>
        <Button variant="outline" size="sm" asChild>
          <Link to="/guest/menu">{t("menu")}</Link>
        </Button>
      </div>

      {spent && (
        <Alert>
          <AlertTitle>{t("orderAlreadySentTitle")}</AlertTitle>
          <AlertDescription>
            {t("orderAlreadySentDesc")}
          </AlertDescription>
        </Alert>
      )}

      {isPending ? (
        <div className="space-y-3">
          {Array.from({ length: 3 }, (_, i) => (
            <Skeleton key={i} className="h-16 rounded-lg" />
          ))}
        </div>
      ) : isError ? (
        <Alert variant="destructive">
          <AlertTitle>{t("couldntLoadCart")}</AlertTitle>
          <AlertDescription>
            <p>{error.message}</p>
            <Button variant="outline" size="sm" className="mt-2" onClick={() => refetch()}>
              {t("tryAgain")}
            </Button>
          </AlertDescription>
        </Alert>
      ) : lines.length === 0 ? (
        <Empty className="border border-dashed">
          <EmptyHeader>
            <EmptyMedia variant="icon">
              <ShoppingCart />
            </EmptyMedia>
            <EmptyTitle>{t("cartEmptyTitle")}</EmptyTitle>
            <EmptyDescription>
              {t("cartEmptyDesc")}
            </EmptyDescription>
          </EmptyHeader>
        </Empty>
      ) : (
        <>
          <ul className="space-y-3">
            {lines.map((line) => (
              <GuestCartLine
                key={line.id}
                line={line}
                disabled={spent}
                currencyCode={cart?.currencyCode}
              />
            ))}
          </ul>

          <Separator />

          <div className="space-y-1 text-sm">
            <div className="text-muted-foreground flex items-baseline justify-between">
              <span>{t("subtotal")}</span>
              <span className="tabular-nums">{formatPrice(cart?.subtotal)}</span>
            </div>
            <div className="text-muted-foreground flex items-baseline justify-between">
              <span>{vatLabel}</span>
              <span className="tabular-nums">{formatPrice(cart?.vatAmount)}</span>
            </div>
            <div className="flex items-baseline justify-between font-semibold">
              <span>{t("total")}</span>
              <DualPrice usd={cart?.grandTotal} khr={cart?.grandTotalKhr} />
            </div>
          </div>

          <Button
            className="w-full"
            disabled={spent || sendCart.isPending}
            onClick={() =>
              sendCart.mutate(undefined, {
                onSuccess: () => navigate("/guest/orders"),
              })
            }
          >
            {sendCart.isPending && <Spinner />}
            {sendCart.isPending ? t("sending") : t("sendToKitchen")}
          </Button>
        </>
      )}
    </>
  )
}
