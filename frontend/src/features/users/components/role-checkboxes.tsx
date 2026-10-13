import { Checkbox } from "@/components/ui/checkbox"
import { FieldLabel } from "@/components/ui/field"
import type { UserRole } from "../types"

const ROLES: { value: UserRole; label: string; hint: string }[] = [
  { value: "CASHIER", label: "Cashier", hint: "Table board, orders, payments" },
  { value: "CHEF", label: "Chef", hint: "Kitchen queue only" },
  { value: "ADMIN", label: "Admin", hint: "Full back-office access" },
]

interface RoleCheckboxesProps {
  value: UserRole[]
  onChange: (roles: UserRole[]) => void
  /** Role values that can't be unchecked (e.g. your own ADMIN role). */
  locked?: UserRole[]
}

export function RoleCheckboxes({
  value,
  onChange,
  locked = [],
}: RoleCheckboxesProps) {
  function toggle(role: UserRole, checked: boolean) {
    onChange(checked ? [...value, role] : value.filter((r) => r !== role))
  }

  return (
    <div className="flex flex-col gap-3">
      {ROLES.map((role) => (
        <div key={role.value} className="flex items-start gap-3">
          <Checkbox
            id={`role-${role.value}`}
            checked={value.includes(role.value)}
            disabled={locked.includes(role.value)}
            onCheckedChange={(checked) => toggle(role.value, checked === true)}
          />
          <div className="grid gap-0.5">
            <FieldLabel htmlFor={`role-${role.value}`}>{role.label}</FieldLabel>
            <p className="text-muted-foreground text-xs">{role.hint}</p>
          </div>
        </div>
      ))}
    </div>
  )
}
