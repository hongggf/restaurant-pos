import { z } from "zod"

export const categorySchema = z.object({
  nameEn: z.string().min(1, "English name is required"),
  nameKm: z.string().min(1, "Khmer name is required"),
  description: z.string(),
  sortOrder: z
    .number("Sort order must be a number")
    .int("Sort order must be a whole number")
    .min(0, "Sort order can't be negative"),
  active: z.boolean(),
  allowNotes: z.boolean(),
})

export type CategoryValues = z.infer<typeof categorySchema>
