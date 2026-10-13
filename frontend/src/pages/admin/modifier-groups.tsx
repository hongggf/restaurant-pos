import { useEffect, useState } from "react"
import {
  ChevronLeft,
  ChevronRight,
  Plus,
  SlidersHorizontal,
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
import { Skeleton } from "@/components/ui/skeleton"
import { DeleteModifierGroupDialog } from "@/features/modifiers/components/delete-modifier-group-dialog"
import { ModifierGroupCard } from "@/features/modifiers/components/modifier-group-card"
import { ModifierGroupFormDialog } from "@/features/modifiers/components/modifier-group-form-dialog"
import {
  useModifierGroups,
  useUpdateModifierGroup,
} from "@/features/modifiers/hooks/use-modifier-groups"
import type { ModifierGroup } from "@/features/modifiers/types"

export function ModifierGroupsPage() {
  const [page, setPage] = useState(0)
  const { data, isPending, isError, error, refetch } = useModifierGroups(page)
  const update = useUpdateModifierGroup()

  const [formOpen, setFormOpen] = useState(false)
  const [editing, setEditing] = useState<ModifierGroup | undefined>(undefined)
  const [deleting, setDeleting] = useState<ModifierGroup | null>(null)
  const [togglingId, setTogglingId] = useState<string | null>(null)

  function openCreate() {
    setEditing(undefined)
    setFormOpen(true)
  }

  function openEdit(group: ModifierGroup) {
    setEditing(group)
    setFormOpen(true)
  }

  function toggleActive(group: ModifierGroup, active: boolean) {
    if (!group.id) return
    setTogglingId(group.id)
    update.mutate(
      {
        id: group.id,
        body: {
          nameEn: group.nameEn!,
          nameKm: group.nameKm!,
          minChoice: group.minChoice ?? 0,
          maxChoice: group.maxChoice,
          active,
          options: (group.options ?? []).map((option, index) => ({
            id: option.id,
            nameEn: option.nameEn!,
            nameKm: option.nameKm!,
            imageUrl: option.imageUrl,
            unitPrice: option.unitPrice ?? 0,
            packSize: option.packSize,
            available: option.available,
            sortOrder: option.sortOrder ?? index,
          })),
        },
      },
      { onSettled: () => setTogglingId(null) }
    )
  }

  const groups = data?.content ?? []
  const totalPages = data?.totalPages ?? 0
  const totalElements = data?.totalElements ?? 0

  useEffect(() => {
    if (data && page > 0 && page >= totalPages) {
      setPage(Math.max(0, totalPages - 1))
    }
  }, [data, page, totalPages])

  return (
    <>
      <div className="flex items-start justify-between gap-4">
        <div>
          <h1 className="text-2xl font-semibold tracking-tight">
            Modifier Groups
          </h1>
          <p className="text-muted-foreground text-sm">
            Reusable choice sets — sugar level, size, toppings — attached to
            menu items.
          </p>
        </div>
        <Button onClick={openCreate}>
          <Plus />
          New group
        </Button>
      </div>

      {isPending || (totalElements > 0 && groups.length === 0) ? (
        <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
          {Array.from({ length: 6 }, (_, i) => (
            <Skeleton key={i} className="h-64 w-full rounded-xl" />
          ))}
        </div>
      ) : isError ? (
        <Alert variant="destructive">
          <AlertTitle>Couldn&apos;t load modifier groups</AlertTitle>
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
              <SlidersHorizontal />
            </EmptyMedia>
            <EmptyTitle>No modifier groups yet</EmptyTitle>
            <EmptyDescription>
              Create choice sets like sugar level or size, then attach them to
              menu items.
            </EmptyDescription>
          </EmptyHeader>
          <EmptyContent>
            <Button onClick={openCreate}>
              <Plus />
              New group
            </Button>
          </EmptyContent>
        </Empty>
      ) : (
        <>
          <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
            {groups.map((group) => (
              <ModifierGroupCard
                key={group.id}
                group={group}
                onEdit={openEdit}
                onDelete={setDeleting}
                onToggleActive={toggleActive}
                togglePending={togglingId === group.id}
              />
            ))}
          </div>
          {totalPages > 1 && (
            <div className="flex items-center justify-between">
              <p className="text-muted-foreground text-sm">
                Page {page + 1} of {totalPages} · {totalElements} groups
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

      <ModifierGroupFormDialog
        open={formOpen}
        onOpenChange={setFormOpen}
        group={editing}
      />
      <DeleteModifierGroupDialog
        group={deleting}
        onOpenChange={(open) => !open && setDeleting(null)}
      />
    </>
  )
}
