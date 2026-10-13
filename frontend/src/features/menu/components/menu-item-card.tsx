import {
  ImageOff,
  MoreHorizontal,
  Pencil,
  SlidersHorizontal,
  Trash2,
} from "lucide-react"

import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Card, CardContent } from "@/components/ui/card"
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu"
import { Switch } from "@/components/ui/switch"
import { resolveItemImage } from "../lib/food-image"
import { formatPrice } from "@/lib/format"
import type { MenuItem } from "../types"

interface MenuItemCardProps {
  item: MenuItem
  onEdit: (item: MenuItem) => void
  onDelete: (item: MenuItem) => void
  onModifiers: (item: MenuItem) => void
  onToggleAvailable: (item: MenuItem, available: boolean) => void
  togglePending: boolean
}

export function MenuItemCard({
  item,
  onEdit,
  onDelete,
  onModifiers,
  onToggleAvailable,
  togglePending,
}: MenuItemCardProps) {
  const image = resolveItemImage(item.nameEn, item.imageUrl, "card")

  return (
    <Card className="gap-3 overflow-hidden pt-0 pb-3">
      <div className="bg-muted relative aspect-[4/3]">
        {image ? (
          <img
            src={image}
            alt={item.nameEn}
            className="size-full object-cover"
            loading="lazy"
          />
        ) : (
          <div className="text-muted-foreground/50 flex size-full items-center justify-center">
            <ImageOff className="size-8" />
          </div>
        )}
        {!item.available && (
          <Badge
            variant="secondary"
            className="absolute bottom-2 left-2 shadow-sm"
          >
            Unavailable
          </Badge>
        )}
        <DropdownMenu>
          <DropdownMenuTrigger asChild>
            <Button
              variant="secondary"
              size="icon"
              className="absolute top-2 right-2 size-7 shadow-sm"
            >
              <MoreHorizontal />
              <span className="sr-only">Actions for {item.nameEn}</span>
            </Button>
          </DropdownMenuTrigger>
          <DropdownMenuContent align="end">
            <DropdownMenuItem onSelect={() => onEdit(item)}>
              <Pencil />
              Edit
            </DropdownMenuItem>
            <DropdownMenuItem onSelect={() => onModifiers(item)}>
              <SlidersHorizontal />
              Modifiers
            </DropdownMenuItem>
            <DropdownMenuItem
              variant="destructive"
              onSelect={() => onDelete(item)}
            >
              <Trash2 />
              Delete
            </DropdownMenuItem>
          </DropdownMenuContent>
        </DropdownMenu>
      </div>
      <CardContent className="flex items-start justify-between gap-2 px-3">
        <div className="min-w-0">
          <p className="truncate font-medium">{item.nameEn}</p>
          <p className="text-muted-foreground truncate text-xs">
            {item.categoryNameEn}
          </p>
          <p className="mt-1 text-sm font-semibold tabular-nums">
            {formatPrice(item.price, item.currencyCode)}
          </p>
        </div>
        <Switch
          checked={!!item.available}
          disabled={togglePending}
          onCheckedChange={(checked) => onToggleAvailable(item, checked)}
          aria-label={`Toggle availability for ${item.nameEn}`}
        />
      </CardContent>
    </Card>
  )
}
