export function formatPrice(price?: number, currencyCode?: string) {
  if (price == null) return ""
  try {
    return new Intl.NumberFormat("en-US", {
      style: "currency",
      currency: currencyCode || "USD",
    }).format(price)
  } catch {
    return `${price} ${currencyCode ?? ""}`
  }
}

/** 0.1 -> "10%". VAT rates come off the API as a fraction, not a percentage. */
export function formatPercent(rate?: number) {
  if (rate == null) return ""
  return new Intl.NumberFormat("en-US", {
    style: "percent",
    maximumFractionDigits: 2,
  }).format(rate)
}
