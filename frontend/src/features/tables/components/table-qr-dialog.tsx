import { useRef } from "react"
import { QRCodeCanvas } from "qrcode.react"
import { Printer, RefreshCw } from "lucide-react"

import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
  AlertDialogTrigger,
} from "@/components/ui/alert-dialog"
import { Button } from "@/components/ui/button"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog"
import { useRegenerateQrToken } from "../hooks/use-tables"
import { guestQrUrl } from "../lib/qr-url"
import type { DiningTable } from "../types"

interface TableQrDialogProps {
  table: DiningTable | null
  onOpenChange: (open: boolean) => void
}

export function TableQrDialog({ table, onOpenChange }: TableQrDialogProps) {
  const canvasRef = useRef<HTMLCanvasElement>(null)
  const regenerate = useRegenerateQrToken()

  function handlePrint() {
    const canvas = canvasRef.current
    if (!canvas || !table) return
    const printWindow = window.open("", "_blank", "width=480,height=640")
    if (!printWindow) return
    printWindow.document.write(`<!doctype html>
<html>
<head>
<title>Table ${table.tableNumber} — QR code</title>
<style>
  body { margin: 0; display: flex; align-items: center; justify-content: center; min-height: 100vh; font-family: system-ui, sans-serif; }
  .card { text-align: center; padding: 24px; }
  .card h1 { font-size: 28px; margin: 0 0 4px; }
  .card p { margin: 0 0 16px; color: #555; }
  .card img { width: 280px; height: 280px; }
</style>
</head>
<body>
  <div class="card">
    <h1>Table ${table.tableNumber}</h1>
    <p>Scan to order</p>
    <img src="${canvas.toDataURL("image/png")}" alt="QR code" />
  </div>
  <script>window.onload = () => { window.print(); window.close(); }</script>
</body>
</html>`)
    printWindow.document.close()
  }

  return (
    <Dialog open={!!table} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-sm">
        <DialogHeader>
          <DialogTitle>Table {table?.tableNumber}</DialogTitle>
          <DialogDescription>
            Print this QR code and place it on the table. It stays valid until
            you regenerate it.
          </DialogDescription>
        </DialogHeader>
        <div className="flex justify-center rounded-lg border bg-white p-6">
          {table?.qrToken && (
            <QRCodeCanvas
              ref={canvasRef}
              value={guestQrUrl(table.qrToken)}
              size={240}
              marginSize={2}
            />
          )}
        </div>
        <DialogFooter className="sm:justify-between">
          <AlertDialog>
            <AlertDialogTrigger asChild>
              <Button variant="outline" disabled={regenerate.isPending}>
                <RefreshCw />
                {regenerate.isPending ? "Regenerating…" : "Regenerate"}
              </Button>
            </AlertDialogTrigger>
            <AlertDialogContent>
              <AlertDialogHeader>
                <AlertDialogTitle>
                  Regenerate QR code for table {table?.tableNumber}?
                </AlertDialogTitle>
                <AlertDialogDescription>
                  All printed QR codes for this table will stop working
                  immediately. Only do this if the current code leaked or a
                  printed card went missing — you&apos;ll need to print and
                  place a new one.
                </AlertDialogDescription>
              </AlertDialogHeader>
              <AlertDialogFooter>
                <AlertDialogCancel>Cancel</AlertDialogCancel>
                <AlertDialogAction
                  className="bg-destructive text-white hover:bg-destructive/90"
                  onClick={() => {
                    if (table?.id) regenerate.mutate(table.id)
                  }}
                >
                  Regenerate
                </AlertDialogAction>
              </AlertDialogFooter>
            </AlertDialogContent>
          </AlertDialog>
          <Button onClick={handlePrint}>
            <Printer />
            Print
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
