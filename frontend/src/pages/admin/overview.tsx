import { useState } from "react"
import { Printer, LayoutGrid, DollarSign, Ticket, Ban } from "lucide-react"
import { QRCodeCanvas } from "qrcode.react"
import { createRoot } from "react-dom/client"
import { Link } from "react-router"

import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { Skeleton } from "@/components/ui/skeleton"
import { useOverviewStats } from "@/features/reports/hooks/use-overview-stats"
import { formatPrice } from "@/lib/format"
import { listTables } from "@/features/tables/api/tables"
import { guestQrUrl } from "@/features/tables/lib/qr-url"

export function OverviewPage() {
  const { data: stats, isLoading } = useOverviewStats()
  const [isPrinting, setIsPrinting] = useState(false)

  async function handlePrintAllQrCodes() {
    setIsPrinting(true)
    try {
      const page = await listTables({ page: 0, size: 500 })
      const tables = page.content || []

      if (!tables.length) {
        alert("No tables found to print.")
        return
      }

      const printWindow = window.open("", "_blank", "width=800,height=600")
      if (!printWindow) return

      printWindow.document.write(`
        <!DOCTYPE html>
        <html>
        <head>
          <title>Print QR Codes</title>
          <style>
            @media print {
              body { margin: 0; padding: 0; }
              .page-break { page-break-after: always; }
            }
            body { font-family: system-ui, sans-serif; }
            .grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 2rem; padding: 2rem; }
            .card { text-align: center; padding: 24px; border: 1px dashed #ccc; border-radius: 8px; page-break-inside: avoid; }
            .card h1 { font-size: 28px; margin: 0 0 4px; }
            .card p { margin: 0 0 16px; color: #555; }
            .card canvas { width: 240px !important; height: 240px !important; margin: 0 auto; }
          </style>
        </head>
        <body>
          <div id="root"></div>
        </body>
        </html>
      `)
      printWindow.document.close()

      const rootElement = printWindow.document.getElementById("root")
      if (rootElement) {
        const root = createRoot(rootElement)
        root.render(
          <div className="grid">
            {tables.map(table => table.qrToken ? (
              <div key={table.id} className="card">
                <h1>Table {table.tableNumber}</h1>
                <p>Scan to order</p>
                <QRCodeCanvas
                  value={guestQrUrl(table.qrToken)}
                  size={240}
                  marginSize={2}
                />
              </div>
            ) : null)}
          </div>
        )

        setTimeout(() => {
          printWindow.print()
          printWindow.close()
        }, 800)
      } else {
        printWindow.onload = () => {
          const rootElement = printWindow.document.getElementById("root")
          if (rootElement) {
            const root = createRoot(rootElement)
            root.render(
              <div className="grid">
                {tables.map(table => table.qrToken ? (
                  <div key={table.id} className="card">
                    <h1>Table {table.tableNumber}</h1>
                    <p>Scan to order</p>
                    <QRCodeCanvas
                      value={guestQrUrl(table.qrToken)}
                      size={240}
                      marginSize={2}
                    />
                  </div>
                ) : null)}
              </div>
            )

            setTimeout(() => {
              printWindow.print()
              printWindow.close()
            }, 800)
          }
        }
      }
    } catch (e) {
      console.error(e)
      alert("Failed to print QR codes.")
    } finally {
      setIsPrinting(false)
    }
  }

  const statCards = [
    {
      label: "Today's Revenue",
      value: stats ? formatPrice(stats.todayRevenue) : "—",
      hint: "Cashier closed sessions today",
      icon: <DollarSign className="h-4 w-4 text-muted-foreground" />,
    },
    {
      label: "Active Tables",
      value: stats?.activeTables ?? "—",
      hint: "Tables currently seated",
      icon: <LayoutGrid className="h-4 w-4 text-muted-foreground" />,
    },
    {
      label: "Open Tickets",
      value: stats?.openTickets ?? "—",
      hint: "Sent or Ready order rounds",
      icon: <Ticket className="h-4 w-4 text-muted-foreground" />,
    },
    {
      label: "Monthly Revenue",
      value: stats ? formatPrice(stats.monthlyRevenue) : "—",
      hint: "This month to date",
      icon: <DollarSign className="h-4 w-4 text-muted-foreground" />,
    },
  ]

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-semibold tracking-tight">Overview</h1>
        <p className="text-sm text-muted-foreground">
          A quick look at your restaurant&apos;s operation.
        </p>
      </div>

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        {statCards.map((stat) => (
          <Card key={stat.label}>
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium">
                {stat.label}
              </CardTitle>
              {stat.icon}
            </CardHeader>
            <CardContent>
              {isLoading ? (
                <Skeleton className="h-8 w-20" />
              ) : (
                <div className="text-2xl font-bold tabular-nums">
                  {stat.value}
                </div>
              )}
              <p className="text-xs text-muted-foreground mt-1">{stat.hint}</p>
            </CardContent>
          </Card>
        ))}
      </div>

      <div className="grid gap-4 md:grid-cols-2">
        <Card>
          <CardHeader>
            <CardTitle>Top Sellers</CardTitle>
            <CardDescription>
              Most ordered items this month (excluding voids)
            </CardDescription>
          </CardHeader>
          <CardContent>
            {isLoading ? (
              <div className="space-y-2">
                <Skeleton className="h-4 w-full" />
                <Skeleton className="h-4 w-full" />
                <Skeleton className="h-4 w-full" />
              </div>
            ) : stats?.topSellers.length ? (
              <div className="space-y-4">
                {stats.topSellers.map((item, idx) => (
                  <div
                    key={idx}
                    className="flex items-center justify-between border-b pb-2 last:border-0 last:pb-0"
                  >
                    <span className="font-medium">{item.itemName}</span>
                    <span className="text-sm text-muted-foreground">
                      {item.count} ordered
                    </span>
                  </div>
                ))}
              </div>
            ) : (
              <p className="text-sm text-muted-foreground">
                No orders this month yet.
              </p>
            )}
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>Quick Actions & Info</CardTitle>
            <CardDescription>Shortcuts and active status</CardDescription>
          </CardHeader>
          <CardContent className="space-y-6">
            <div className="space-y-4">
              <Button
                variant="outline"
                className="w-full justify-start"
                onClick={handlePrintAllQrCodes}
                disabled={isPrinting}
              >
                <Printer className="mr-2 h-4 w-4" />
                {isPrinting ? "Generating..." : "Print all QR codes"}
              </Button>
              <Button asChild variant="outline" className="w-full justify-start">
                <Link to="/admin/promotions">
                  <Ticket className="mr-2 h-4 w-4" />
                  Manage Promo
                </Link>
              </Button>
            </div>

            <div className="space-y-3 pt-4 border-t">
              <div className="flex justify-between items-center text-sm">
                <span className="text-muted-foreground">Active Promo</span>
                {isLoading ? (
                  <Skeleton className="h-4 w-24" />
                ) : (
                  <span className="font-medium truncate max-w-[200px]">
                    {stats?.activePromoTitle || "None"}
                  </span>
                )}
              </div>
              <div className="flex justify-between items-center text-sm">
                <span className="text-muted-foreground">Monthly Voids</span>
                {isLoading ? (
                  <Skeleton className="h-4 w-12" />
                ) : (
                  <span className="font-medium flex items-center">
                    {stats?.monthlyVoids} <Ban className="ml-1 h-3 w-3 text-destructive" />
                  </span>
                )}
              </div>
            </div>
          </CardContent>
        </Card>
      </div>
    </div>
  )
}
