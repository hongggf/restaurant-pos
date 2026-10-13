import { ChefHat, LogOut } from "lucide-react"
import { NavLink, Outlet, useNavigate } from "react-router"

import { Avatar, AvatarFallback } from "@/components/ui/avatar"
import { Button } from "@/components/ui/button"
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu"
import { clearToken, getUsername } from "@/lib/auth/token"

export function CashierLayout() {
  const navigate = useNavigate()
  const username = getUsername() ?? "Cashier"

  function logout() {
    clearToken()
    navigate("/login", { replace: true })
  }

  return (
    <div className="flex min-h-svh flex-col">
      <header className="bg-background sticky top-0 z-10 flex h-14 shrink-0 items-center justify-between gap-2 border-b px-4">
        <NavLink to="/cashier" className="flex items-center gap-2">
          <div className="bg-primary text-primary-foreground flex size-8 items-center justify-center rounded-lg">
            <ChefHat className="size-4" />
          </div>
          <div className="grid text-left text-sm leading-tight">
            <span className="font-medium">Restaurant POS</span>
            <span className="text-muted-foreground text-xs">Cashier</span>
          </div>
        </NavLink>
        <DropdownMenu>
          <DropdownMenuTrigger asChild>
            <Button variant="ghost" className="gap-2 px-2">
              <Avatar className="size-8 rounded-lg">
                <AvatarFallback className="rounded-lg uppercase">
                  {username.slice(0, 2)}
                </AvatarFallback>
              </Avatar>
              <span className="hidden sm:inline">{username}</span>
            </Button>
          </DropdownMenuTrigger>
          <DropdownMenuContent align="end">
            <DropdownMenuLabel>{username}</DropdownMenuLabel>
            <DropdownMenuSeparator />
            <DropdownMenuItem onSelect={logout}>
              <LogOut />
              Sign out
            </DropdownMenuItem>
          </DropdownMenuContent>
        </DropdownMenu>
      </header>
      <main className="flex flex-1 flex-col gap-4 p-4 md:p-6">
        <Outlet />
      </main>
    </div>
  )
}
