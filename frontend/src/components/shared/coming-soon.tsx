import { Construction } from "lucide-react"

export function ComingSoon({ title }: { title: string }) {
  return (
    <div className="flex flex-1 flex-col items-center justify-center gap-2 rounded-lg border border-dashed p-10 text-center">
      <Construction className="text-muted-foreground size-8" />
      <h1 className="text-lg font-semibold">{title}</h1>
      <p className="text-muted-foreground text-sm">
        This screen hasn&apos;t been built yet.
      </p>
    </div>
  )
}
