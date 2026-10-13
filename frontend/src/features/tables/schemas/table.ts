import { z } from "zod"

export const tableSchema = z.object({
  tableNumber: z.string().min(1, "Table number is required"),
  active: z.boolean(),
})

export type TableValues = z.infer<typeof tableSchema>
