import { useEffect, useState } from "react"
import {
  ChevronLeft,
  ChevronRight,
  Plus,
  UtensilsCrossed,
} from "lucide-react"

import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert"
import { Button } from "@/components/ui/button"
import {
  Empty,
  EmptyContent,
  EmptyDescription,
  EmptyHeader,
  EmptyMedia,
  EmptyTitle,
} from "@/components/ui/empty"
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select"
import { Skeleton } from "@/components/ui/skeleton"
import { DeleteMenuItemDialog } from "@/features/menu/components/delete-menu-item-dialog"
import { AttachModifiersDialog } from "@/features/modifiers/components/attach-modifiers-dialog"
import { MenuItemFormDialog } from "@/features/menu/components/menu-item-form-dialog"
import { MenuItemGrid } from "@/features/menu/components/menu-item-grid"
import {
  useCategoryOptions,
  useMenuItems,
  useUpdateMenuItem,
} from "@/features/menu/hooks/use-menu-items"
import type { MenuItem } from "@/features/menu/types"

const ALL = "all"

export function MenuItemsPage() {
  const [page, setPage] = useState(0)
  const [categoryId, setCategoryId] = useState(ALL)
  const [availability, setAvailability] = useState(ALL)

  const { data, isPending, isError, error, refetch } = useMenuItems({
    page,
    categoryId: categoryId === ALL ? undefined : categoryId,
    available: availability === ALL ? undefined : availability === "true",
  })
  const categories = useCategoryOptions()
  const update = useUpdateMenuItem()

  const [formOpen, setFormOpen] = useState(false)
  const [editing, setEditing] = useState<MenuItem | undefined>(undefined)
  const [deleting, setDeleting] = useState<MenuItem | null>(null)
  const [managingModifiers, setManagingModifiers] = useState<MenuItem | null>(
    null
  )
  const [togglingId, setTogglingId] = useState<string | null>(null)

  function openCreate() {
    setEditing(undefined)
    setFormOpen(true)
  }

  function openEdit(item: MenuItem) {
    setEditing(item)
    setFormOpen(true)
  }

  function toggleAvailable(item: MenuItem, available: boolean) {
    if (!item.id) return
    setTogglingId(item.id)
    update.mutate(
      {
        id: item.id,
        body: {
          nameEn: item.nameEn!,
          nameKm: item.nameKm!,
          descriptionEn: item.descriptionEn,
          descriptionKm: item.descriptionKm,
          price: item.price!,
          currencyCode: item.currencyCode!,
          categoryId: item.categoryId!,
          station: item.station ?? "KITCHEN",
          available,
        },
      },
      { onSettled: () => setTogglingId(null) }
    )
  }

  const items = data?.content ?? []
  const totalPages = data?.totalPages ?? 0
  const totalElements = data?.totalElements ?? 0
  const filtered = categoryId !== ALL || availability !== ALL

  useEffect(() => {
    if (data && page > 0 && page >= totalPages) {
      setPage(Math.max(0, totalPages - 1))
    }
  }, [data, page, totalPages])

  return (
    <>
      <div className="flex items-start justify-between gap-4">
        <div>
          <h1 className="text-2xl font-semibold tracking-tight">Menu Items</h1>
          <p className="text-muted-foreground text-sm">
            What customers see and order, grouped by category.
          </p>
        </div>
        <Button onClick={openCreate}>
          <Plus />
          New item
        </Button>
      </div>

      <div className="flex flex-wrap gap-2">
        <Select
          value={categoryId}
          onValueChange={(value) => {
            setCategoryId(value)
            setPage(0)
          }}
        >
          <SelectTrigger className="w-48">
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value={ALL}>All categories</SelectItem>
            {(categories.data ?? []).map((category) => (
              <SelectItem key={category.id} value={category.id!}>
                {category.nameEn}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
        <Select
          value={availability}
          onValueChange={(value) => {
            setAvailability(value)
            setPage(0)
          }}
        >
          <SelectTrigger className="w-40">
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value={ALL}>All items</SelectItem>
            <SelectItem value="true">Available</SelectItem>
            <SelectItem value="false">Unavailable</SelectItem>
          </SelectContent>
        </Select>
      </div>

      {isPending || (totalElements > 0 && items.length === 0) ? (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
          {Array.from({ length: 8 }, (_, i) => (
            <Skeleton key={i} className="aspect-[4/3] w-full rounded-xl" />
          ))}
        </div>
      ) : isError ? (
        <Alert variant="destructive">
          <AlertTitle>Couldn&apos;t load menu items</AlertTitle>
          <AlertDescription>
            <p>{error.message}</p>
            <Button
              variant="outline"
              size="sm"
              className="mt-2"
              onClick={() => refetch()}
            >
              Try again
            </Button>
          </AlertDescription>
        </Alert>
      ) : totalElements === 0 ? (
        <Empty className="border border-dashed">
          <EmptyHeader>
            <EmptyMedia variant="icon">
              <UtensilsCrossed />
            </EmptyMedia>
            <EmptyTitle>
              {filtered ? "Nothing matches these filters" : "No menu items yet"}
            </EmptyTitle>
            <EmptyDescription>
              {filtered
                ? "Try a different category or availability filter."
                : "Add your first dish or drink so customers can order."}
            </EmptyDescription>
          </EmptyHeader>
          {!filtered && (
            <EmptyContent>
              <Button onClick={openCreate}>
                <Plus />
                New item
              </Button>
            </EmptyContent>
          )}
        </Empty>
      ) : (
        <>
          <MenuItemGrid
            items={items}
            onEdit={openEdit}
            onDelete={setDeleting}
            onModifiers={setManagingModifiers}
            onToggleAvailable={toggleAvailable}
            togglePendingId={togglingId}
          />
          {totalPages > 1 && (
            <div className="flex items-center justify-between">
              <p className="text-muted-foreground text-sm">
                Page {page + 1} of {totalPages} · {totalElements} items
              </p>
              <div className="flex gap-2">
                <Button
                  variant="outline"
                  size="sm"
                  disabled={page === 0}
                  onClick={() => setPage((p) => p - 1)}
                >
                  <ChevronLeft />
                  Previous
                </Button>
                <Button
                  variant="outline"
                  size="sm"
                  disabled={page + 1 >= totalPages}
                  onClick={() => setPage((p) => p + 1)}
                >
                  Next
                  <ChevronRight />
                </Button>
              </div>
            </div>
          )}
        </>
      )}

      <MenuItemFormDialog
        open={formOpen}
        onOpenChange={setFormOpen}
        item={editing}
      />
      <DeleteMenuItemDialog
        item={deleting}
        onOpenChange={(open) => !open && setDeleting(null)}
      />
      <AttachModifiersDialog
        item={managingModifiers}
        onOpenChange={(open) => !open && setManagingModifiers(null)}
      />
    </>
  )
}
