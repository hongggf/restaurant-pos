import { Outlet, useLocation } from "react-router"

import {
  Breadcrumb,
  BreadcrumbItem,
  BreadcrumbLink,
  BreadcrumbList,
  BreadcrumbPage,
  BreadcrumbSeparator,
} from "@/components/ui/breadcrumb"
import { Separator } from "@/components/ui/separator"
import {
  SidebarInset,
  SidebarProvider,
  SidebarTrigger,
} from "@/components/ui/sidebar"
import { AdminSidebar } from "./admin-sidebar"

const sectionTitles: Record<string, string> = {
  categories: "Categories",
  "menu-items": "Menu Items",
  "modifier-groups": "Modifier Groups",
  tables: "Tables",
  users: "Users",
}

export function AdminLayout() {
  const { pathname } = useLocation()
  const section = sectionTitles[pathname.split("/")[2] ?? ""]

  return (
    <SidebarProvider>
      <AdminSidebar />
      <SidebarInset>
        <header className="flex h-14 shrink-0 items-center gap-2 border-b px-4">
          <SidebarTrigger className="-ml-1" />
          <Separator orientation="vertical" className="mr-2 !h-4" />
          <Breadcrumb>
            <BreadcrumbList>
              <BreadcrumbItem>
                {section ? (
                  <BreadcrumbLink href="/admin">Admin</BreadcrumbLink>
                ) : (
                  <BreadcrumbPage>Overview</BreadcrumbPage>
                )}
              </BreadcrumbItem>
              {section && (
                <>
                  <BreadcrumbSeparator />
                  <BreadcrumbItem>
                    <BreadcrumbPage>{section}</BreadcrumbPage>
                  </BreadcrumbItem>
                </>
              )}
            </BreadcrumbList>
          </Breadcrumb>
        </header>
        <div className="flex flex-1 flex-col gap-4 p-4 md:p-6">
          <Outlet />
        </div>
      </SidebarInset>
    </SidebarProvider>
  )
}
