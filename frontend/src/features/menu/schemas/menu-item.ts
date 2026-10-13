import { z } from "zod"

export const menuItemSchema = z.object({
  nameEn: z.string().min(1, "English name is required"),
  nameKm: z.string().min(1, "Khmer name is required"),
  descriptionEn: z.string(),
  descriptionKm: z.string(),
  price: z
    .number()
    .min(0, "Price can't be negative"),
  currencyCode: z.string().min(1, "Currency is required"),
  categoryId: z.string().min(1, "Category is required"),
  station: z.enum(["KITCHEN", "COUNTER"]),
  available: z.boolean(),
  imageUrl: z.string().optional().nullable(),
})

export type MenuItemValues = z.infer<typeof menuItemSchema>
