import { useState } from "react"
import { Plus } from "lucide-react"
import { Button } from "@/components/ui/button"
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { Switch } from "@/components/ui/switch"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog"
import { Field, FieldLabel } from "@/components/ui/field"
import { usePromos, useCreatePromo, useUpdatePromo, useDeletePromo } from "@/features/promo/hooks"
import type { GuestPromo, GuestPromoDto } from "@/features/promo/api/promotions"
import { assetUrl } from "@/lib/api/client"
import { resolveItemImage } from "@/features/menu/lib/food-image"

function PromoFormDialog({ open, onOpenChange, promo }: { open: boolean, onOpenChange: (o: boolean) => void, promo?: GuestPromo }) {
  const [title, setTitle] = useState(promo?.title ?? "")
  const [active, setActive] = useState(promo?.active ?? false)
  const [imageFile, setImageFile] = useState<File | undefined>()
  const [imageUrl] = useState(promo?.imageUrl ?? "")

  const create = useCreatePromo()
  const update = useUpdatePromo()

  const isPending = create.isPending || update.isPending

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    const dto: GuestPromoDto = { title, description: "", active, imageUrl }
    if (promo) {
      update.mutate({ id: promo.id, dto, image: imageFile }, { onSuccess: () => onOpenChange(false) })
    } else {
      create.mutate({ dto, image: imageFile }, { onSuccess: () => onOpenChange(false) })
    }
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>{promo ? "Edit Promotion" : "New Promotion"}</DialogTitle>
          <DialogDescription>
            {promo ? "Update this promotion" : "Create a new promotion popup"}
          </DialogDescription>
        </DialogHeader>
        <form onSubmit={handleSubmit} className="space-y-4">
          <Field>
            <FieldLabel>Title</FieldLabel>
            <Input value={title} onChange={(e) => setTitle(e.target.value)} required />
          </Field>
          <Field>
            <FieldLabel htmlFor="promo-image">Image</FieldLabel>
            <div className="flex items-center gap-4">
              {imageUrl && !imageFile && (
                <img src={assetUrl(resolveItemImage(undefined, imageUrl, "card") || imageUrl) || ""} alt="" className="h-24 w-24 object-cover rounded" />
              )}
              {imageFile && (
                <div className="bg-muted h-24 w-24 rounded-md flex flex-col items-center justify-center text-xs text-muted-foreground p-2">
                  <span className="font-semibold text-foreground text-center line-clamp-2">{imageFile.name}</span>
                  <span className="mt-1">Ready</span>
                </div>
              )}
              <div className="flex-1">
                <Input id="promo-image" type="file" accept="image/*" className={(imageUrl || imageFile) ? "hidden" : ""} onChange={(e) => setImageFile(e.target.files?.[0])} />
                {(imageUrl || imageFile) && (
                  <Button type="button" variant="outline" size="sm" asChild>
                    <label htmlFor="promo-image" className="cursor-pointer">Change Image</label>
                  </Button>
                )}
              </div>
            </div>
          </Field>
          <Field orientation="horizontal">
            <Switch checked={active} onCheckedChange={setActive} />
            <FieldLabel>Active</FieldLabel>
          </Field>
          <DialogFooter>
            <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>Cancel</Button>
            <Button type="submit" disabled={isPending}>Save</Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}

export function PromotionsPage() {
  const { data: promos = [], isLoading } = usePromos()
  const [dialogOpen, setDialogOpen] = useState(false)
  const [editingPromo, setEditingPromo] = useState<GuestPromo | undefined>()
  const deletePromo = useDeletePromo()

  if (isLoading) return <div>Loading...</div>

  return (
    <div className="space-y-6 max-w-4xl">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold tracking-tight">Promotions</h1>
          <p className="text-muted-foreground">Manage guest popups</p>
        </div>
        <Button onClick={() => { setEditingPromo(undefined); setDialogOpen(true) }}>
          <Plus className="mr-2 size-4" /> Add Promo
        </Button>
      </div>
      <div className="grid gap-4 sm:grid-cols-2">
        {promos.map(promo => (
          <Card key={promo.id}>
            {promo.imageUrl && (
              <img src={assetUrl(resolveItemImage(undefined, promo.imageUrl, "card") || promo.imageUrl) || ""} className="h-40 w-full object-cover rounded-t-lg" alt="" />
            )}
            <CardHeader>
              <CardTitle>{promo.title}</CardTitle>
              <CardDescription>
                {promo.active ? <span className="text-green-600 font-bold">Active</span> : "Inactive"}
              </CardDescription>
            </CardHeader>
            <CardContent>
              <div className="mt-4 flex gap-2">
                <Button variant="outline" size="sm" onClick={() => { setEditingPromo(promo); setDialogOpen(true) }}>Edit</Button>
                <Button variant="destructive" size="sm" onClick={() => deletePromo.mutate(promo.id)}>Delete</Button>
              </div>
            </CardContent>
          </Card>
        ))}
      </div>
      {dialogOpen && (
        <PromoFormDialog open={dialogOpen} onOpenChange={setDialogOpen} promo={editingPromo} />
      )}
    </div>
  )
}
