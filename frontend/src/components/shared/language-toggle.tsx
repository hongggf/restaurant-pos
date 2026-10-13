import { useLanguage } from "@/lib/language-context"
import { cn } from "@/lib/utils"

export function LanguageToggle({ className }: { className?: string }) {
  const { language, setLanguage } = useLanguage()

  return (
    <div
      role="radiogroup"
      aria-label="Language selection"
      className={cn(
        "bg-muted/80 inline-flex items-center rounded-full p-0.5 text-xs font-semibold select-none border border-border/60",
        className
      )}
    >
      <button
        type="button"
        role="radio"
        aria-checked={language === "en"}
        onClick={() => setLanguage("en")}
        className={cn(
          "rounded-full px-2.5 py-1 transition-all cursor-pointer",
          language === "en"
            ? "bg-background text-foreground shadow-xs font-medium"
            : "text-muted-foreground hover:text-foreground"
        )}
      >
        EN
      </button>
      <button
        type="button"
        role="radio"
        aria-checked={language === "km"}
        onClick={() => setLanguage("km")}
        className={cn(
          "rounded-full px-2.5 py-1 transition-all cursor-pointer font-khmer",
          language === "km"
            ? "bg-background text-foreground shadow-xs font-medium"
            : "text-muted-foreground hover:text-foreground"
        )}
      >
        ខ្មែរ
      </button>
    </div>
  )
}
