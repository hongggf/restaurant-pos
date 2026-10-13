import { formatPrice } from "@/lib/format"

const khrFormatter = new Intl.NumberFormat("en-US", {
  style: "currency",
  currency: "KHR",
  maximumFractionDigits: 0,
})

interface DualPriceProps {
  usd?: number
  khr?: number
  className?: string
}

export function DualPrice({ usd, khr, className }: DualPriceProps) {
  return (
    <span className={className}>
      {formatPrice(usd, "USD")}
      {khr != null && (
        <span className="text-muted-foreground text-xs"> · {khrFormatter.format(khr)}</span>
      )}
    </span>
  )
}
