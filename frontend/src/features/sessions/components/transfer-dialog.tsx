import { useState } from "react"
import { useNavigate } from "react-router"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog"
import { Button } from "@/components/ui/button"
import { useTablesOverview } from "../hooks/use-tables-overview"
import { useTransferSession } from "../hooks/use-transfer-session"

export function TransferDialog({
  sessionId,
  open,
  onOpenChange,
}: {
  sessionId: string
  open: boolean
  onOpenChange: (open: boolean) => void
}) {
  const { data: tables } = useTablesOverview()
  const transfer = useTransferSession()
  const navigate = useNavigate()
  const [selectedTableId, setSelectedTableId] = useState<string | null>(null)

  const idleTables = tables?.filter((t) => t.state === "IDLE") ?? []

  function handleTransfer() {
    if (!selectedTableId) return
    transfer.mutate(
      { sessionId, targetTableId: selectedTableId },
      {
        onSuccess: () => {
          onOpenChange(false)
          // The session ID stays the same, but we might want to update the URL state
          // Navigating without sessionId change just updates state? We can just go back to board
          navigate("/cashier")
        },
      }
    )
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Move to another table</DialogTitle>
          <DialogDescription>
            Select an idle table to transfer this session to.
          </DialogDescription>
        </DialogHeader>
        <div className="grid grid-cols-4 gap-2 py-4">
          {idleTables.length === 0 ? (
            <p className="col-span-4 text-center text-sm text-muted-foreground">
              No idle tables available.
            </p>
          ) : (
            idleTables.map((t) => (
              <Button
                key={t.tableId}
                variant={selectedTableId === t.tableId ? "default" : "outline"}
                onClick={() => setSelectedTableId(t.tableId ?? null)}
              >
                {t.tableNumber}
              </Button>
            ))
          )}
        </div>
        <DialogFooter>
          <Button
            variant="outline"
            onClick={() => onOpenChange(false)}
            disabled={transfer.isPending}
          >
            Cancel
          </Button>
          <Button
            onClick={handleTransfer}
            disabled={!selectedTableId || transfer.isPending}
          >
            {transfer.isPending ? "Moving..." : "Move session"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
