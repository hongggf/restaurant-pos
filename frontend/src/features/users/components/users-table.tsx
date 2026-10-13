import {
  createColumnHelper,
  flexRender,
  getCoreRowModel,
  useReactTable,
} from "@tanstack/react-table"
import { KeyRound, MoreHorizontal, Pencil, Trash2 } from "lucide-react"

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
import type { User } from "../types"

const columnHelper = createColumnHelper<User>()

interface UsersTableProps {
  users: User[]
  /** Username of the logged-in admin; their row gets self-protection. */
  currentUsername: string | null
  onEdit: (user: User) => void
  onResetPassword: (user: User) => void
  onDelete: (user: User) => void
}

export function UsersTable({
  users,
  currentUsername,
  onEdit,
  onResetPassword,
  onDelete,
}: UsersTableProps) {
  const columns = [
    columnHelper.accessor("username", {
      header: "Username",
      cell: (info) => (
        <span className="flex items-center gap-2 font-medium">
          {info.getValue()}
          {info.getValue() === currentUsername && (
            <Badge variant="outline">You</Badge>
          )}
        </span>
      ),
    }),
    columnHelper.accessor("email", {
      header: "Email",
      cell: (info) => (
        <span className="text-muted-foreground">{info.getValue()}</span>
      ),
    }),
    columnHelper.accessor("roles", {
      header: "Roles",
      cell: (info) => (
        <span className="flex gap-1">
          {(info.getValue() ?? []).map((role) => (
            <Badge
              key={role}
              variant={role === "ADMIN" ? "default" : "secondary"}
            >
              {role}
            </Badge>
          ))}
        </span>
      ),
    }),
    columnHelper.accessor("enabled", {
      header: "Status",
      cell: (info) =>
        info.getValue() ? (
          <Badge variant="secondary">Enabled</Badge>
        ) : (
          <Badge variant="outline">Disabled</Badge>
        ),
    }),
    columnHelper.display({
      id: "actions",
      header: () => <span className="sr-only">Actions</span>,
      cell: ({ row }) => {
        const isSelf = row.original.username === currentUsername
        return (
          <DropdownMenu>
            <DropdownMenuTrigger asChild>
              <Button variant="ghost" size="icon" className="size-8">
                <MoreHorizontal />
                <span className="sr-only">
                  Actions for {row.original.username}
                </span>
              </Button>
            </DropdownMenuTrigger>
            <DropdownMenuContent align="end">
              <DropdownMenuItem onSelect={() => onEdit(row.original)}>
                <Pencil />
                Edit
              </DropdownMenuItem>
              <DropdownMenuItem
                onSelect={() => onResetPassword(row.original)}
              >
                <KeyRound />
                Reset password
              </DropdownMenuItem>
              <DropdownMenuItem
                variant="destructive"
                disabled={isSelf}
                onSelect={() => onDelete(row.original)}
              >
                <Trash2 />
                Delete
              </DropdownMenuItem>
            </DropdownMenuContent>
          </DropdownMenu>
        )
      },
    }),
  ]

  const table = useReactTable({
    data: users,
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
