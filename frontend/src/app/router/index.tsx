import { lazy, Suspense } from "react"
import { createBrowserRouter, Navigate } from "react-router"

import { Spinner } from "@/components/ui/spinner"
import { AdminLayout } from "@/layouts/admin-layout"
import { CashierLayout } from "@/layouts/cashier-layout"
import { KitchenLayout } from "@/layouts/kitchen-layout"
import { GuestLayout } from "@/layouts/guest-layout"
import { GuestSessionGuard } from "./guest-session-guard"
import { RequireAuth } from "./require-auth"
import { RoleLanding } from "./role-landing"

const CategoriesPage = lazy(() => import("@/pages/admin/categories").then(m => ({ default: m.CategoriesPage })))
const MenuItemsPage = lazy(() => import("@/pages/admin/menu-items").then(m => ({ default: m.MenuItemsPage })))
const ModifierGroupsPage = lazy(() => import("@/pages/admin/modifier-groups").then(m => ({ default: m.ModifierGroupsPage })))
const OverviewPage = lazy(() => import("@/pages/admin/overview").then(m => ({ default: m.OverviewPage })))
const TablesPage = lazy(() => import("@/pages/admin/tables").then(m => ({ default: m.TablesPage })))
const UsersPage = lazy(() => import("@/pages/admin/users").then(m => ({ default: m.UsersPage })))

const BillPage = lazy(() => import("@/pages/cashier/bill").then(m => ({ default: m.BillPage })))
const ManualOrderPage = lazy(() => import("@/pages/cashier/manual-order").then(m => ({ default: m.ManualOrderPage })))
const ReceiptPage = lazy(() => import("@/pages/cashier/receipt").then(m => ({ default: m.ReceiptPage })))
const SessionPage = lazy(() => import("@/pages/cashier/session").then(m => ({ default: m.SessionPage })))
const TableBoardPage = lazy(() => import("@/pages/cashier/table-board").then(m => ({ default: m.TableBoardPage })))

const KitchenQueuePage = lazy(() => import("@/pages/kitchen/queue").then(m => ({ default: m.KitchenQueuePage })))

const GuestCartPage = lazy(() => import("@/pages/guest/cart").then(m => ({ default: m.GuestCartPage })))
const GuestMenuPage = lazy(() => import("@/pages/guest/menu").then(m => ({ default: m.GuestMenuPage })))
const GuestOrdersPage = lazy(() => import("@/pages/guest/orders").then(m => ({ default: m.GuestOrdersPage })))
const GuestResolvePage = lazy(() => import("@/pages/guest/resolve").then(m => ({ default: m.GuestResolvePage })))
const LoginPage = lazy(() => import("@/pages/login").then(m => ({ default: m.LoginPage })))

function withSuspense(Component: React.ComponentType) {
  return (
    <Suspense
      fallback={
        <div className="flex flex-1 items-center justify-center p-8">
          <Spinner className="size-6 text-muted-foreground" />
        </div>
      }
    >
      <Component />
    </Suspense>
  )
}

export const router = createBrowserRouter([
  { path: "/login", element: withSuspense(LoginPage) },
  { path: "/", element: <RoleLanding /> },
  {
    path: "/guest",
    element: <GuestLayout />,
    children: [
      { index: true, element: withSuspense(GuestResolvePage) },
      {
        path: "menu",
        element: (
          <GuestSessionGuard>
            {withSuspense(GuestMenuPage)}
          </GuestSessionGuard>
        ),
      },
      {
        path: "cart",
        element: (
          <GuestSessionGuard>
            {withSuspense(GuestCartPage)}
          </GuestSessionGuard>
        ),
      },
      {
        path: "orders",
        element: (
          <GuestSessionGuard>
            {withSuspense(GuestOrdersPage)}
          </GuestSessionGuard>
        ),
      },
    ],
  },
  {
    element: <RequireAuth roles={["ADMIN"]} />,
    children: [
      {
        path: "/admin",
        element: <AdminLayout />,
        children: [
          { index: true, element: withSuspense(OverviewPage) },
          { path: "categories", element: withSuspense(CategoriesPage) },
          { path: "menu-items", element: withSuspense(MenuItemsPage) },
          { path: "modifier-groups", element: withSuspense(ModifierGroupsPage) },
          { path: "promotions", element: withSuspense(lazy(() => import("@/pages/admin/promotions").then(m => ({ default: m.PromotionsPage })))) },
          { path: "tables", element: withSuspense(TablesPage) },
          { path: "users", element: withSuspense(UsersPage) },
        ],
      },
    ],
  },
  {
    // Admins can run the till too, e.g. covering a shift.
    element: <RequireAuth roles={["CASHIER", "ADMIN"]} />,
    children: [
      {
        path: "/cashier",
        element: <CashierLayout />,
        children: [
          { 
            path: "", 
            element: withSuspense(TableBoardPage),
            children: [
              { path: "sessions/:sessionId", element: withSuspense(SessionPage) },
            ]
          },
          { path: "sessions/:sessionId/order", element: withSuspense(ManualOrderPage) },
          { path: "sessions/:sessionId/bill", element: withSuspense(BillPage) },
          { path: "sessions/:sessionId/receipt", element: withSuspense(ReceiptPage) },
        ],
      },
    ],
  },
  {
    // Admins can work the pass too, e.g. checking on a backed-up queue.
    element: <RequireAuth roles={["CHEF", "ADMIN"]} />,
    children: [
      {
        path: "/kitchen",
        element: <KitchenLayout />,
        children: [{ index: true, element: withSuspense(KitchenQueuePage) }],
      },
    ],
  },
  { path: "*", element: <Navigate to="/" replace /> },
])
