import { z } from "zod"

const optionSchema = z.object({
  id: z.string().optional(),
  nameEn: z.string().min(1, "English name is required"),
  nameKm: z.string().min(1, "Khmer name is required"),
  unitPrice: z
    .number("Price must be a number")
    .min(0, "Price can't be negative"),
  available: z.boolean(),
  // The API returns null, not undefined, for an unset column — .optional()
  // alone rejects null and fails validation on a field the form never renders.
  imageUrl: z.string().nullish(),
  packSize: z.string().nullish(),
})

export const modifierGroupSchema = z
  .object({
    nameEn: z.string().min(1, "English name is required"),
    nameKm: z.string().min(1, "Khmer name is required"),
    minChoice: z
      .number("Min choices must be a number")
      .int("Min choices must be a whole number")
      .min(0, "Min choices can't be negative"),
    // Left empty, max is unlimited.
    maxChoice: z
      .number("Max choices must be a number")
      .int("Max choices must be a whole number")
      .min(1, "Max choices must be at least 1")
      .optional(),
    active: z.boolean(),
    options: z.array(optionSchema).min(1, "Add at least one option"),
  })
  .refine(
    (value) =>
      value.maxChoice === undefined || value.maxChoice >= value.minChoice,
    {
      path: ["maxChoice"],
      message: "Max choices can't be less than min choices",
    }
  )

export type ModifierGroupValues = z.infer<typeof modifierGroupSchema>
