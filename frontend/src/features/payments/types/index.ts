import type { components } from "@/lib/api/schema"

export type Bill = components["schemas"]["BillResponse"]
export type BillRound = components["schemas"]["OrderRoundResponse"]
export type PaymentRequest = components["schemas"]["PaymentRequest"]
export type Payment = components["schemas"]["PaymentResponse"]
export type PaymentMethod = NonNullable<PaymentRequest["method"]>
