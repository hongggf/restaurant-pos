import { z } from "zod"

const roles = z
  .array(z.enum(["ADMIN", "CASHIER", "CHEF"]))
  .min(1, "Select at least one role")

export const createUserSchema = z.object({
  username: z.string().min(1, "Username is required"),
  email: z.email("Enter a valid email"),
  password: z.string().min(8, "Password must be at least 8 characters"),
  roles,
})

export const editUserSchema = z.object({
  email: z.email("Enter a valid email"),
  enabled: z.boolean(),
  roles,
})

export const resetPasswordSchema = z.object({
  newPassword: z.string().min(8, "Password must be at least 8 characters"),
})

export type CreateUserValues = z.infer<typeof createUserSchema>
export type EditUserValues = z.infer<typeof editUserSchema>
export type ResetPasswordValues = z.infer<typeof resetPasswordSchema>
