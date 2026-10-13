import {
  createColumnHelper,
  flexRender,
  getCoreRowModel,
  useReactTable,
} from "@tanstack/react-table"
import { ExternalLink, MoreHorizontal, Pencil, QrCode, Trash2 } from "lucide-react"

import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu"
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table"
import { guestQrUrl } from "../lib/qr-url"
import type { DiningTable } from "../types"

const columnHelper = createColumnHelper<DiningTable>()

interface TablesTableProps {
  tables: DiningTable[]
  onShowQr: (table: DiningTable) => void
  onEdit: (table: DiningTable) => void
  onDelete: (table: DiningTable) => void
}

export function TablesTable({
  tables,
  onShowQr,
  onEdit,
  onDelete,
}: TablesTableProps) {
  const columns = [
    columnHelper.accessor("tableNumber", {
      header: "Table",
      cell: (info) => <span className="font-medium">{info.getValue()}</span>,
    }),
    columnHelper.accessor("active", {
      header: "Status",
      cell: (info) =>
        info.getValue() ? (
          <Badge variant="secondary">Active</Badge>
        ) : (
          <Badge variant="outline">Inactive</Badge>
        ),
    }),
    columnHelper.display({
      id: "qr",
      header: () => <span className="sr-only">QR code</span>,
      cell: ({ row }) => (
        <div className="flex items-center justify-end gap-2 text-right">
          {row.original.qrToken && (
            <Button variant="outline" size="sm" asChild>
              <a
                href={guestQrUrl(row.original.qrToken)}
                target="_blank"
                rel="noopener noreferrer"
              >
                <ExternalLink className="size-4" />
                Guest Menu
              </a>
            </Button>
          )}
          <Button
            variant="outline"
            size="sm"
            onClick={() => onShowQr(row.original)}
          >
            <QrCode className="size-4" />
            Show QR
          </Button>
        </div>
      ),
    }),
    columnHelper.display({
      id: "actions",
      header: () => <span className="sr-only">Actions</span>,
      cell: ({ row }) => (
        <DropdownMenu>
          <DropdownMenuTrigger asChild>
            <Button variant="ghost" size="icon" className="size-8">
              <MoreHorizontal />
              <span className="sr-only">
                Actions for table {row.original.tableNumber}
              </span>
            </Button>
          </DropdownMenuTrigger>
          <DropdownMenuContent align="end">
            <DropdownMenuItem onSelect={() => onEdit(row.original)}>
              <Pencil />
              Edit
            </DropdownMenuItem>
            <DropdownMenuItem
              variant="destructive"
              onSelect={() => onDelete(row.original)}
            >
              <Trash2 />
              Delete
            </DropdownMenuItem>
          </DropdownMenuContent>
        </DropdownMenu>
      ),
    }),
  ]

  const table = useReactTable({
    data: tables,
    columns,
    getCoreRowModel: getCoreRowModel(),
  })

  return (
    <div className="rounded-lg border">
      <Table>
        <TableHeader>
          {table.getHeaderGroups().map((headerGroup) => (
            <TableRow key={headerGroup.id}>
              {headerGroup.headers.map((header) => (
                <TableHead key={header.id}>
                  {flexRender(
                    header.column.columnDef.header,
                    header.getContext()
                  )}
                </TableHead>
              ))}
            </TableRow>
          ))}
        </TableHeader>
        <TableBody>
          {table.getRowModel().rows.map((row) => (
            <TableRow key={row.id}>
              {row.getVisibleCells().map((cell) => (
                <TableCell key={cell.id}>
                  {flexRender(cell.column.columnDef.cell, cell.getContext())}
                </TableCell>
              ))}
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </div>
  )
}
