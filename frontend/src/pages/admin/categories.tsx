import { useEffect, useState } from "react"
import { ChevronLeft, ChevronRight, Plus, Tags } from "lucide-react"

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
import { CategoriesTable } from "@/features/categories/components/categories-table"
import { CategoryFormDialog } from "@/features/categories/components/category-form-dialog"
import { DeleteCategoryDialog } from "@/features/categories/components/delete-category-dialog"
import { useCategories } from "@/features/categories/hooks/use-categories"
import type { Category } from "@/features/categories/types"

export function CategoriesPage() {
  const [page, setPage] = useState(0)
  const { data, isPending, isError, error, refetch } = useCategories(page)

  const [formOpen, setFormOpen] = useState(false)
  const [editing, setEditing] = useState<Category | undefined>(undefined)
  const [deleting, setDeleting] = useState<Category | null>(null)

  function openCreate() {
    setEditing(undefined)
    setFormOpen(true)
  }

  function openEdit(category: Category) {
    setEditing(category)
    setFormOpen(true)
  }

  const categories = data?.content ?? []
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
          <h1 className="text-2xl font-semibold tracking-tight">Categories</h1>
          <p className="text-muted-foreground text-sm">
            Menu sections shown to customers and the cashier.
          </p>
        </div>
        {(totalElements > 0 || page > 0) && (
          <Button onClick={openCreate}>
            <Plus />
            New category
          </Button>
        )}
      </div>

      {isPending || (totalElements > 0 && categories.length === 0) ? (
        <div className="space-y-2">
          <Skeleton className="h-10 w-full" />
          {Array.from({ length: 5 }, (_, i) => (
            <Skeleton key={i} className="h-12 w-full" />
          ))}
        </div>
      ) : isError ? (
        <Alert variant="destructive">
          <AlertTitle>Couldn&apos;t load categories</AlertTitle>
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
              <Tags />
            </EmptyMedia>
            <EmptyTitle>No categories yet</EmptyTitle>
            <EmptyDescription>
              Categories group your menu — start with something like Drinks,
              Soups, or Rice.
            </EmptyDescription>
          </EmptyHeader>
          <EmptyContent>
            <Button onClick={openCreate}>
              <Plus />
              New category
            </Button>
          </EmptyContent>
        </Empty>
      ) : (
        <>
          <CategoriesTable
            categories={categories}
            onEdit={openEdit}
            onDelete={setDeleting}
          />
          {totalPages > 1 && (
            <div className="flex items-center justify-between">
              <p className="text-muted-foreground text-sm">
                Page {page + 1} of {totalPages} · {totalElements} categories
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

      <CategoryFormDialog
        open={formOpen}
        onOpenChange={setFormOpen}
        category={editing}
      />
      <DeleteCategoryDialog
        category={deleting}
        onOpenChange={(open) => !open && setDeleting(null)}
      />
    </>
  )
}
