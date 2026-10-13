import { useEffect, useState } from "react"
import { ChevronLeft, ChevronRight, LayoutGrid, Plus } from "lucide-react"

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
import { DeleteTableDialog } from "@/features/tables/components/delete-table-dialog"
import { TableFormDialog } from "@/features/tables/components/table-form-dialog"
import { TableQrDialog } from "@/features/tables/components/table-qr-dialog"
import { TablesTable } from "@/features/tables/components/tables-table"
import { useTables } from "@/features/tables/hooks/use-tables"
import type { DiningTable } from "@/features/tables/types"

export function TablesPage() {
  const [page, setPage] = useState(0)
  const { data, isPending, isError, error, refetch } = useTables(page)

  const [formOpen, setFormOpen] = useState(false)
  const [editing, setEditing] = useState<DiningTable | undefined>(undefined)
  const [deleting, setDeleting] = useState<DiningTable | null>(null)
  const [qrTableId, setQrTableId] = useState<string | null>(null)

  function openCreate() {
    setEditing(undefined)
    setFormOpen(true)
  }

  function openEdit(table: DiningTable) {
    setEditing(table)
    setFormOpen(true)
  }

  const tables = data?.content ?? []
  const totalPages = data?.totalPages ?? 0
  const totalElements = data?.totalElements ?? 0

  useEffect(() => {
    if (data && page > 0 && page >= totalPages) {
      setPage(Math.max(0, totalPages - 1))
    }
  }, [data, page, totalPages])

  // Resolve from fresh list data so the QR dialog updates after regeneration.
  const qrTable = tables.find((t) => t.id === qrTableId) ?? null

  return (
    <>
      <div className="flex items-start justify-between gap-4">
        <div>
          <h1 className="text-2xl font-semibold tracking-tight">Tables</h1>
          <p className="text-muted-foreground text-sm">
            Physical tables and the QR codes customers scan to order.
          </p>
        </div>
        {(totalElements > 0 || page > 0) && (
          <Button onClick={openCreate}>
            <Plus />
            New table
          </Button>
        )}
      </div>

      {isPending || (totalElements > 0 && tables.length === 0) ? (
        <div className="space-y-2">
          <Skeleton className="h-10 w-full" />
          {Array.from({ length: 5 }, (_, i) => (
            <Skeleton key={i} className="h-12 w-full" />
          ))}
        </div>
      ) : isError ? (
        <Alert variant="destructive">
          <AlertTitle>Couldn&apos;t load tables</AlertTitle>
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
              <LayoutGrid />
            </EmptyMedia>
            <EmptyTitle>No tables yet</EmptyTitle>
            <EmptyDescription>
              Each table gets a permanent QR code that customers scan to
              order. Add your first table to get started.
            </EmptyDescription>
          </EmptyHeader>
          <EmptyContent>
            <Button onClick={openCreate}>
              <Plus />
              New table
            </Button>
          </EmptyContent>
        </Empty>
      ) : (
        <>
          <TablesTable
            tables={tables}
            onShowQr={(table) => setQrTableId(table.id ?? null)}
            onEdit={openEdit}
            onDelete={setDeleting}
          />
          {totalPages > 1 && (
            <div className="flex items-center justify-between">
              <p className="text-muted-foreground text-sm">
                Page {page + 1} of {totalPages} · {totalElements} tables
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

      <TableFormDialog
        open={formOpen}
        onOpenChange={setFormOpen}
        table={editing}
      />
      <DeleteTableDialog
        table={deleting}
        onOpenChange={(open) => !open && setDeleting(null)}
      />
      <TableQrDialog
        table={qrTable}
        onOpenChange={(open) => !open && setQrTableId(null)}
      />
    </>
  )
}
