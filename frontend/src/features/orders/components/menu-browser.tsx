import { useState, useMemo } from "react"
import { ImageOff, UtensilsCrossed, Ban, Search } from "lucide-react"

import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import {
  Empty,
  EmptyDescription,
  EmptyHeader,
  EmptyMedia,
  EmptyTitle,
} from "@/components/ui/empty"
import { Skeleton } from "@/components/ui/skeleton"
import {
  useCategoryOptions,
  useMenuItems,
  useUpdateMenuItemAvailability,
} from "@/features/menu/hooks/use-menu-items"
import { resolveItemImage } from "@/features/menu/lib/food-image"
import type { MenuItem } from "@/features/menu/types"
import { formatPrice } from "@/lib/format"
import { cn } from "@/lib/utils"

interface MenuBrowserProps {
  onPick: (item: MenuItem) => void
}

export function MenuBrowser({ onPick }: MenuBrowserProps) {
  const [categoryId, setCategoryId] = useState<string | undefined>(undefined)
  const [searchQuery, setSearchQuery] = useState("")

  const categories = useCategoryOptions()
  // Use a large page size to fetch all items for the category, enabling local search.
  const { data, isPending, isError, error, refetch } = useMenuItems({
    page: 0,
    categoryId,
  })
  
  // We override the default hook's size to 1000 in the query below, but since we can't easily 
  // change the hook without affecting other parts, we will just use the hook as is and filter. 
  // Actually, we should probably add a local filter to the items we have.
  const updateAvailability = useUpdateMenuItemAvailability()

  const allItems = data?.content ?? []
  
  const items = useMemo(() => {
    if (!searchQuery.trim()) return allItems
    const query = searchQuery.toLowerCase()
    return allItems.filter(item => 
      item.nameEn?.toLowerCase().includes(query)
    )
  }, [allItems, searchQuery])

  function pickCategory(id: string | undefined) {
    setCategoryId(id)
  }

  return (
    <div className="flex min-w-0 flex-1 flex-col gap-3 h-full">
      <div className="flex items-center gap-3 shrink-0">
        <div className="relative flex-1">
          <Search className="absolute left-2.5 top-2.5 h-4 w-4 text-muted-foreground" />
          <Input
            type="search"
            placeholder="Search menu..."
            className="pl-8 bg-background"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
          />
        </div>
      </div>
      
      <div className="flex gap-2 overflow-x-auto pb-1 shrink-0 scrollbar-none">
        <Badge
          asChild
          variant={categoryId === undefined ? "default" : "outline"}
          className="h-9 px-4 text-sm cursor-pointer whitespace-nowrap"
        >
          <button type="button" onClick={() => pickCategory(undefined)}>
            All
          </button>
        </Badge>
        {categories.data?.map((category) => (
          <Badge
            key={category.id}
            asChild
            variant={categoryId === category.id ? "default" : "outline"}
            className="h-9 px-4 text-sm cursor-pointer whitespace-nowrap"
          >
            <button type="button" onClick={() => pickCategory(category.id)}>
              {category.nameEn}
            </button>
          </Badge>
        ))}
      </div>

      <div className="flex-1 overflow-y-auto pb-20 pr-2">
        {isPending ? (
          <div className="grid grid-cols-3 gap-2 sm:grid-cols-4 xl:grid-cols-5">
            {Array.from({ length: 15 }, (_, i) => (
              <Skeleton key={i} className="aspect-square rounded-xl" />
            ))}
          </div>
        ) : isError ? (
          <Alert variant="destructive">
            <AlertTitle>Couldn&apos;t load the menu</AlertTitle>
            <AlertDescription>
              <p>{error.message}</p>
              <Button
                variant="outline"
                size="sm"
                className="mt-2"
                onClick={() => refetch()}
              >
                Try again
              </Button>
            </AlertDescription>
          </Alert>
        ) : items.length === 0 ? (
          <Empty className="border border-dashed h-full">
            <EmptyHeader>
              <EmptyMedia variant="icon">
                <UtensilsCrossed />
              </EmptyMedia>
              <EmptyTitle>Nothing found</EmptyTitle>
              <EmptyDescription>
                Try adjusting your search or category filter.
              </EmptyDescription>
            </EmptyHeader>
          </Empty>
        ) : (
          <div className="grid grid-cols-3 gap-2 sm:grid-cols-4 xl:grid-cols-5">
            {items.map((item) => {
              const image = resolveItemImage(item.nameEn, item.imageUrl, "card")
              return (
                <div
                  key={item.id}
                  className={cn(
                    "bg-card hover:border-primary/50 flex flex-col overflow-hidden rounded-xl border text-left relative",
                    "transition-colors",
                    !item.available && "opacity-50 cursor-not-allowed"
                  )}
                >
                  <button
                    type="button"
                    onClick={() => onPick(item)}
                    disabled={!item.available}
                    className="absolute inset-0 z-0 text-left"
                  >
                    <span className="sr-only">Select {item.nameEn}</span>
                  </button>
                  <div className="bg-muted relative aspect-[4/3] w-full pointer-events-none">
                    {image ? (
                      <img
                        src={image}
                        alt={item.nameEn}
                        className="size-full object-cover"
                        loading="lazy"
                      />
                    ) : (
                      <div className="text-muted-foreground/50 flex size-full items-center justify-center">
                        <ImageOff className="size-6" />
                      </div>
                    )}
                    {!item.available && (
                      <div className="absolute inset-0 flex items-center justify-center bg-background/20 backdrop-blur-[1px]">
                        <Badge variant="destructive" className="font-semibold pointer-events-none scale-75">Sold out</Badge>
                      </div>
                    )}
                  </div>
                  <div className="flex flex-1 flex-col gap-0 p-2 pointer-events-none">
                    <span className="line-clamp-2 text-xs font-medium leading-tight">
                      {item.nameEn}
                    </span>
                    <span className="text-muted-foreground mt-auto text-[11px] tabular-nums font-medium">
                      {formatPrice(item.price, item.currencyCode)}
                    </span>
                  </div>
                  {item.available && (
                    <Button
                      variant="secondary"
                      size="icon"
                      className="absolute top-1 right-1 size-6 shadow-md z-10 text-destructive bg-background/80 hover:bg-background/90 rounded-full"
                      onClick={() => updateAvailability.mutate({ id: item.id!, available: false })}
                      disabled={updateAvailability.isPending}
                      title="86 / Sold out"
                    >
                      <Ban className="size-3" />
                    </Button>
                  )}
                </div>
              )
            })}
          </div>
        )}
      </div>
    </div>
  )
}
