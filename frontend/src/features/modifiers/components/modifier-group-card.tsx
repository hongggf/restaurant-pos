import { MoreHorizontal, Pencil, Trash2 } from "lucide-react"

import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import {
  Card,
  CardContent,
  CardFooter,
  CardHeader,
  CardTitle,
} from "@/components/ui/card"
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu"
import { Separator } from "@/components/ui/separator"
import { Switch } from "@/components/ui/switch"
import { formatPrice } from "@/lib/format"
import { assetUrl } from "@/lib/api/client"
import { choiceRule } from "../lib/choice-rule"
import type { ModifierGroup } from "../types"

const VISIBLE_OPTIONS = 6

interface ModifierGroupCardProps {
  group: ModifierGroup
  onEdit: (group: ModifierGroup) => void
  onDelete: (group: ModifierGroup) => void
  onToggleActive: (group: ModifierGroup, active: boolean) => void
  togglePending: boolean
}

export function ModifierGroupCard({
  group,
  onEdit,
  onDelete,
  onToggleActive,
  togglePending,
}: ModifierGroupCardProps) {
  const options = group.options ?? []
  const hidden = options.length - VISIBLE_OPTIONS

  return (
    <Card className="gap-4">
      <CardHeader className="gap-1">
        <div className="flex items-start justify-between gap-2">
          <div className="min-w-0">
            <CardTitle className="truncate">{group.nameEn}</CardTitle>
            <p className="text-muted-foreground truncate text-sm">
              {group.nameKm}
            </p>
          </div>
          <DropdownMenu>
            <DropdownMenuTrigger asChild>
              <Button variant="ghost" size="icon" className="size-7 shrink-0">
                <MoreHorizontal />
                <span className="sr-only">Actions for {group.nameEn}</span>
              </Button>
            </DropdownMenuTrigger>
            <DropdownMenuContent align="end">
              <DropdownMenuItem onSelect={() => onEdit(group)}>
                <Pencil />
                Edit
              </DropdownMenuItem>
              <DropdownMenuItem
                variant="destructive"
                onSelect={() => onDelete(group)}
              >
                <Trash2 />
                Delete
              </DropdownMenuItem>
            </DropdownMenuContent>
          </DropdownMenu>
        </div>
        <div className="flex flex-wrap gap-1.5">
          <Badge variant="outline">
            {choiceRule(group.minChoice, group.maxChoice)}
          </Badge>
          {!group.active && <Badge variant="secondary">Inactive</Badge>}
        </div>
      </CardHeader>
      <CardContent className="grow">
        <ul className="space-y-1.5 text-sm">
          {options.slice(0, VISIBLE_OPTIONS).map((option) => (
            <li
              key={option.id}
              className="flex items-center justify-between gap-2"
            >
              <div className="flex items-center gap-2 truncate">
                {option.imageUrl ? (
                  <img src={assetUrl(option.imageUrl) || ""} alt="" className="size-5 rounded object-cover border" />
                ) : (
                  <div className="size-5 rounded border bg-muted" />
                )}
                <span
                  className={
                    option.available === false
                      ? "text-muted-foreground truncate line-through"
                      : "truncate"
                  }
                >
                  {option.nameEn}
                </span>
              </div>
              <span className="text-muted-foreground tabular-nums shrink-0">
                {option.unitPrice
                  ? `+${formatPrice(option.unitPrice, "USD")}`
                  : "Free"}
              </span>
            </li>
          ))}
        </ul>
        {hidden > 0 && (
          <p className="text-muted-foreground mt-2 text-xs">
            +{hidden} more option{hidden > 1 ? "s" : ""}
          </p>
        )}
      </CardContent>
      <CardFooter className="flex-col items-stretch gap-3">
        <Separator />
        <div className="flex items-center justify-between">
          <p className="text-muted-foreground text-sm">
            {options.length} option{options.length === 1 ? "" : "s"}
          </p>
          <Switch
            checked={!!group.active}
            disabled={togglePending}
            onCheckedChange={(checked) => onToggleActive(group, checked)}
            aria-label={`Toggle active for ${group.nameEn}`}
          />
        </div>
      </CardFooter>
    </Card>
  )
}
