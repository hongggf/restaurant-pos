import type { MenuItem } from "../types"
import { MenuItemCard } from "./menu-item-card"

interface MenuItemGridProps {
  items: MenuItem[]
  onEdit: (item: MenuItem) => void
  onDelete: (item: MenuItem) => void
  onModifiers: (item: MenuItem) => void
  onToggleAvailable: (item: MenuItem, available: boolean) => void
  togglePendingId: string | null
}

export function MenuItemGrid({
  items,
  onEdit,
  onDelete,
  onModifiers,
  onToggleAvailable,
  togglePendingId,
}: MenuItemGridProps) {
  return (
    <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
      {items.map((item) => (
        <MenuItemCard
          key={item.id}
          item={item}
          onEdit={onEdit}
          onDelete={onDelete}
          onModifiers={onModifiers}
          onToggleAvailable={onToggleAvailable}
          togglePending={togglePendingId === item.id}
        />
      ))}
    </div>
  )
}
