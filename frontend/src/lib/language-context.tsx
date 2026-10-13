import { createContext, useContext, useState, type ReactNode } from "react"

export type Language = "en" | "km"

interface LanguageContextType {
  language: Language
  setLanguage: (lang: Language) => void
  toggleLanguage: () => void
  t: (key: string, options?: string | { defaultValue?: string }) => string
}

const STORAGE_KEY = "malatang_guest_language"

const translations: Record<string, { en: string; km: string }> = {
  menu: { en: "Menu", km: "ម៉ឺនុយ" },
  cart: { en: "Cart", km: "កន្ត្រក" },
  yourCart: { en: "Your cart", km: "កន្ត្រករបស់អ្នក" },
  orderAhead: { en: "Order ahead", km: "កុម្ម៉ង់ទុកមុន" },
  table: { en: "Table", km: "តុ" },
  all: { en: "All", km: "ទាំងអស់" },
  pricesBeforeVat: {
    en: "Prices shown before VAT",
    km: "តម្លៃមិនទាន់បូកបញ្ចូលអាករ (VAT)",
  },
  noteForKitchen: { en: "Special instructions (optional)", km: "ចំណាំបន្ថែម (មិនទាមទារ)" },
  specialInstructions: { en: "Special instructions (optional)", km: "ចំណាំបន្ថែម (មិនទាមទារ)" },
  notePlaceholder: {
    en: "e.g. No onions, sauce on the side...",
    km: "ឧ. មិនដាក់ខ្ទឹមបារាំង, ទឹកជ្រលក់ដាក់ដោយឡែក...",
  },
  drinkNotePlaceholder: {
    en: "e.g. Less ice, no ice, cold can...",
    km: "ឧ. ទឹកកកតិច, អត់ទឹកកក, កំប៉ុងត្រជាក់...",
  },
  add: { en: "Add", km: "បន្ថែម" },
  update: { en: "Update", km: "កែប្រែ" },
  cartEmptyTitle: { en: "Your cart is empty", km: "កន្ត្រករបស់អ្នកទទេ" },
  cartEmptyDesc: {
    en: "Add items from the menu to get started.",
    km: "សូមជ្រើសរើសមុខម្ហូបពីម៉ឺនុយដើម្បីចាប់ផ្ដើម។",
  },
  browseMenu: { en: "Browse menu", km: "មើលម៉ឺនុយ" },
  sendOrder: { en: "Send order", km: "ផ្ញើការកុម្ម៉ង់" },
  orderAlreadySentTitle: {
    en: "Order already sent",
    km: "បានផ្ញើការកុម្ម៉ង់រួចរាល់",
  },
  orderAlreadySentDesc: {
    en: "Scan the table QR code again to start another round.",
    km: "ស្កេន QR កូដលើតុម្ដងទៀត ដើម្បីកុម្ម៉ង់ជុំបន្ទាប់។",
  },
  couldntLoadCart: {
    en: "Couldn't load your cart",
    km: "មិនអាចទាញយកកន្ត្រករបស់អ្នកបានទេ",
  },
  tryAgain: { en: "Try again", km: "ព្យាយាមម្ដងទៀត" },
  itemOptionsError: {
    en: "Couldn't load this item's options. Close and try again.",
    km: "មិនអាចទាញយកជម្រើសសម្រាប់មុខម្ហូបនេះបានទេ។ សូមបិទហើយព្យាយាមម្តងទៀត។",
  },
  nothingAvailable: {
    en: "Nothing available here",
    km: "មិនមានមុខម្ហូបនៅឡើយទេ",
  },
  noAvailableItems: {
    en: "No available menu items in this category.",
    km: "មិនមានមុខម្ហូបសម្រាប់ប្រភេទនេះនៅឡើយទេ។",
  },
  previous: { en: "Previous", km: "ថយក្រោយ" },
  next: { en: "Next", km: "បន្ទាប់" },
  subtotal: { en: "Subtotal", km: "សរុបរង" },
  vat: { en: "VAT", km: "អាករ (VAT)" },
  total: { en: "Total", km: "សរុប" },
  grandTotal: { en: "Grand total", km: "សរុបរួម" },
  sendToKitchen: { en: "Send to kitchen", km: "ផ្ញើទៅផ្ទះបាយ" },
  sending: { en: "Sending…", km: "កំពុងផ្ញើ…" },
  free: { en: "Free", km: "ឥតគិតថ្លៃ" },
  each: { en: "each", km: "ក្នុងមួយមុខ" },
  yourOrders: { en: "Your ordered items", km: "មុខម្ហូបដែលបានកុម្ម៉ង់" },
  orderSentTitle: { en: "Order sent", km: "បានផ្ញើការកុម្ម៉ង់" },
  orderSentDesc: {
    en: "Scan the table QR code again to order more.",
    km: "ស្កេន QR កូដលើតុម្ដងទៀត ដើម្បីកុម្ម៉ង់បន្ថែម។",
  },
  couldntLoadOrders: {
    en: "Couldn't load your orders",
    km: "មិនអាចទាញយកបញ្ជីកុម្ម៉ង់បានទេ",
  },
  nothingOrderedTitle: { en: "Nothing ordered yet", km: "មិនទាន់មានការកុម្ម៉ង់នៅឡើយទេ" },
  nothingOrderedDesc: {
    en: "Items you send to the kitchen will appear here.",
    km: "មុខម្ហូបដែលបានផ្ញើទៅផ្ទះបាយ នឹងបង្ហាញនៅទីនេះ។",
  },
  runningTotal: { en: "Running total", km: "សរុបបច្ចុប្បន្ន" },
  round: { en: "Round", km: "ជុំទី" },
  statusSent: { en: "Sent", km: "បានផ្ញើ" },
  statusCooking: { en: "Cooking", km: "កំពុងចម្អិន" },
  statusReady: { en: "Ready", km: "រួចរាល់" },
  statusCompleted: { en: "Completed", km: "បានបញ្ចប់" },
  statusCancelled: { en: "Cancelled", km: "បានបោះបង់" },
}

const LanguageContext = createContext<LanguageContextType>({
  language: "en",
  setLanguage: () => {},
  toggleLanguage: () => {},
  t: (key: string) => translations[key]?.en ?? key,
})

export function LanguageProvider({ children }: { children: ReactNode }) {
  const [language, setLanguageState] = useState<Language>(() => {
    if (typeof window !== "undefined") {
      try {
        const saved = localStorage.getItem(STORAGE_KEY)
        if (saved === "en" || saved === "km") {
          return saved
        }
      } catch {
        // ignore localStorage access issues in tests/sandboxes
      }
    }
    return "en"
  })

  const setLanguage = (lang: Language) => {
    setLanguageState(lang)
    if (typeof window !== "undefined") {
      try {
        localStorage.setItem(STORAGE_KEY, lang)
      } catch {
        // ignore
      }
    }
  }

  const toggleLanguage = () => {
    setLanguage(language === "en" ? "km" : "en")
  }

  const t = (key: string) => {
    const entry = translations[key]
    if (!entry) return key
    return entry[language] ?? entry.en ?? key
  }

  return (
    <LanguageContext.Provider
      value={{ language, setLanguage, toggleLanguage, t }}
    >
      {children}
    </LanguageContext.Provider>
  )
}

export function useLanguage() {
  return useContext(LanguageContext)
}
