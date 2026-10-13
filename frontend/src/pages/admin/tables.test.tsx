import { describe, it, expect, beforeEach, vi } from "vitest"
import { render, screen, waitFor } from "@testing-library/react"
import { QueryClient, QueryClientProvider } from "@tanstack/react-query"
import { TablesPage } from "./tables"
import * as tablesApi from "@/features/tables/api/tables"
import type { DiningTable } from "@/features/tables/types"

vi.mock("@/features/tables/api/tables", () => ({
  listTables: vi.fn(),
  createTable: vi.fn(),
  updateTable: vi.fn(),
  deleteTable: vi.fn(),
  regenerateQrToken: vi.fn(),
}))

vi.mock("sonner", () => ({
  toast: {
    success: vi.fn(),
    error: vi.fn(),
  },
}))

describe("TablesPage", () => {
  let queryClient: QueryClient

  beforeEach(() => {
    vi.clearAllMocks()
    queryClient = new QueryClient({
      defaultOptions: {
        queries: { retry: false },
      },
    })
  })

  function renderPage() {
    return render(
      <QueryClientProvider client={queryClient}>
        <TablesPage />
      </QueryClientProvider>
    )
  }

  const makeTable = (num: number): DiningTable => ({
    id: `table-${num}`,
    tableNumber: `T${num}`,
    active: true,
    qrToken: `token-${num}`,
  })

  it("shows empty state when there are 0 tables", async () => {
    vi.mocked(tablesApi.listTables).mockResolvedValue({
      content: [],
      page: 0,
      size: 10,
      totalElements: 0,
      totalPages: 0,
    })

    renderPage()

    await waitFor(() => {
      expect(screen.getByText("No tables yet")).toBeInTheDocument()
    })
  })

  it("navigates back to page 0 when the only item on the last page is deleted", async () => {
    const tenTables = Array.from({ length: 10 }, (_, i) => makeTable(i + 1))
    const eleventhTable = makeTable(11)

    // Initially on page 0 with 11 tables total (2 pages)
    vi.mocked(tablesApi.listTables).mockImplementation(async ({ page = 0 }) => {
      if (page === 0) {
        return {
          content: tenTables,
          page: 0,
          size: 10,
          totalElements: 11,
          totalPages: 2,
        }
      }
      if (page === 1) {
        return {
          content: [eleventhTable],
          page: 1,
          size: 10,
          totalElements: 11,
          totalPages: 2,
        }
      }
      return {
        content: [],
        page,
        size: 10,
        totalElements: 11,
        totalPages: 2,
      }
    })

    renderPage()

    // Verify page 0 shows first 10 tables and pagination
    await waitFor(() => {
      expect(screen.getByText("T1")).toBeInTheDocument()
      expect(screen.getByText(/Page 1 of 2/)).toBeInTheDocument()
    })

    // Click "Next" to go to page 2 (page index 1)
    const nextBtn = screen.getByRole("button", { name: /Next/i })
    await waitFor(() => expect(nextBtn).toBeEnabled())
    nextBtn.click()

    // Verify page 1 shows table 11
    await waitFor(() => {
      expect(screen.getByText("T11")).toBeInTheDocument()
      expect(screen.getByText(/Page 2 of 2/)).toBeInTheDocument()
    })

    // Now simulate deleting table 11:
    // When invalidated and page 1 is refetched, backend returns empty content and totalPages = 1
    vi.mocked(tablesApi.listTables).mockImplementation(async ({ page = 0 }) => {
      if (page === 0) {
        return {
          content: tenTables,
          page: 0,
          size: 10,
          totalElements: 10,
          totalPages: 1,
        }
      }
      // Out of bounds page 1
      return {
        content: [],
        page: 1,
        size: 10,
        totalElements: 10,
        totalPages: 1,
      }
    })

    // Trigger query refetch as delete does
    queryClient.invalidateQueries({ queryKey: ["tables"] })

    // It should automatically recover and take user back to page 0 with 10 tables!
    await waitFor(() => {
      expect(screen.getByText("T1")).toBeInTheDocument()
      expect(screen.getByText("T10")).toBeInTheDocument()
      expect(screen.queryByText("No tables yet")).not.toBeInTheDocument()
    })
  })
})
