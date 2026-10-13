import { ChefHat } from "lucide-react"
import { Outlet } from "react-router"

import { LanguageToggle } from "@/components/shared/language-toggle"
import { useGuestSession } from "@/features/guest/hooks/use-guest-session"
import { useLanguage } from "@/lib/language-context"

export function GuestLayout() {
  const session = useGuestSession()
  const { t, language } = useLanguage()
  const tableNumber =
    session.status === "browsing" || session.status === "spent"
      ? session.tableNumber
      : null

  return (
    <div className="bg-muted/30 flex min-h-svh flex-col">
      <header className="bg-background sticky top-0 z-10 flex h-14 shrink-0 items-center justify-between border-b px-4">
        <div className="flex items-center gap-2">
          <div className="bg-primary text-primary-foreground flex size-8 items-center justify-center rounded-lg">
            <ChefHat className="size-4" />
          </div>
          <div className="grid text-left text-sm leading-tight">
            <span className="font-medium">Restaurant POS</span>
            <span className="text-muted-foreground text-xs">
              {tableNumber
                ? `${language === "km" ? "តុ" : "Table"} ${tableNumber}`
                : t("orderAhead")}
            </span>
          </div>
        </div>

        <LanguageToggle />
      </header>
      <main className="mx-auto flex w-full max-w-md flex-1 flex-col gap-4 p-4">
        <Outlet />
      </main>
    </div>
  )
}
