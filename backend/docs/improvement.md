# UI Decision Review — Malatang POS

**Date:** 2026-09-11
**Method:** Multi-agent review — 41 agents; 53 UI decisions audited across receipt, cashier terminal, kitchen, admin, and guest surfaces; 29 contested claims adversarially re-verified against the code (several overturned). All file:line references were read and confirmed.

**Scope note:** The cross-cutting consistency sweep (i18n coverage, money-formatting unification, design-token discipline) failed on an upstream API timeout and was not covered; everything else below is fully reviewed.

---

## Overall assessment

The system's UI decisions are strongest exactly where the money is: the guest cart disclosure model, the cash tender flow, and the preview-before-print receipt are all designed around real jobs and should stay as they are. The guest cart being "fine" is confirmed — its per-option pricing, live footer total, VAT line, and server-side idempotent cart are genuinely well-made.

What is genuinely broken is the **operational periphery**: corrections (single-item void, 86, table move) have no UI even where the API and render paths already exist; recovery (reprint after "Back to tables", 24h token expiry on a never-closed KDS, any refetch error on the kitchen board) either dead-ends or destroys the screen; and liveness (a frozen table board with no offline flag, silent order arrivals with no beep) lets the till confidently lie.

---

## 1. The receipt

### What we got right — keep

- **Preview-before-print is the correct decision.** `frontend/src/pages/cashier/receipt.tsx:53–121` shows a real preview with **change as the hero** (`text-3xl`, KHR alternative below); the cashier verifies against the guest's count before speaking. Beats POS systems that blind-fire the printer.
- **The PDF is a proper legal record** (`backend/.../feature/receipt/service/ReceiptPdfGenerator.java:125–144, :70–112`): itemised priced builds — base row, indented `+ N × option $x` per modifier, `= qty × unit` reconciliation line — plus VAT disclosure, dual-currency totals, change given, cashier name, unique stored receipt number (`R-yyyyMMdd-NNNN`), and an embedded Khmer font pipeline that degrades safely when the font is absent.
- **EN-only printed item names are consistent, not a bug** (verified by adversarial pass): the guest journey is EN-primary end to end (menu cards, cart, rounds all `nameEn`-only), so the paper matches what the guest confirmed on their phone. Khmer item names are a nice-to-have.

### Problems

1. 🔴 **Reprint is impossible** — *broken, high severity.*
   The receipt page is reachable **only** via the automatic post-payment navigation. "Back to tables" is a one-way door: the board filters to ACTIVE sessions and emits `sessionId=null` for closed tables (`frontend/src/pages/cashier/table-board.tsx:33–49`; `backend/.../feature/table/service/impl/CashierTableServiceImpl.java:45–47, 78–81`), and tapping an IDLE card calls `findOrCreateActiveSession` and mints a **new** session. `ReceiptController.getBySession` (`GET /sessions/{id}/receipt`) is dead code from the UI — recovery would require knowing the session UUID.

   **Fix:** extend `TableOverviewResponse` with `lastClosedSessionId` + `lastClosedSessionHasReceipt`; render a distinct secondary "View last receipt" chip on cards that have one, routing to `/cashier/sessions/{id}/receipt`, gated on `hasReceipt` so sessions auto-closed by `SessionCleanupJob` don't 404, with a "Closed 12:34" badge so a stale session isn't misread. While re-rendering receipts:
   - persist the KHR grand total on `Payment` at pay-time (the `toKhr` call already happens there) so a later rate edit doesn't rewrite the ៛ line on reprints;
   - add a `REPRINTED <ts> · COPY` footer driven by a `firstPrintedAt` field on `Receipt` — the record a guest keeps should never silently change value between prints.

2. 🟡 **Screen-vs-paper modifier gap** — *revisit.*
   The PDF itemises modifier prices, but `frontend/src/features/payments/components/bill-summary.tsx:30–47` shows one collapsed total per line — and once the session is CLOSED, `bill.tsx:63–81` redirects to the receipt, so the itemised on-screen view is gone; the PDF (new-tab dance) is the only breakdown. The cashier's dispute-defusing tool at the counter becomes the PDF viewer. The data is free: `OrderRoundLineResponse` already ships `basePrice`/`unitPrice`/per-selection prices (`schema.d.ts:798–824`).

   **Fix:** extract round-card's selection sub-row (`round-card.tsx:118–153`, name + extended price when > 0) into a shared component rendered under each BillSummary line, and include the `= {qty} × {unitPrice}` reconciliation row whenever quantity > 1 — otherwise printed sub-rows won't sum to `lineTotal` and the dispute is worse than before.

3. 🟡 **Print-path papercuts** — *revisit.*
   `use-receipt.ts:14–26` ignores `window.open` returning `null` (popup blocked → silent nothing); the sibling `table-qr-dialog.tsx:38–65` already has the guard — copy it (and give the QR print path's silent `if (!printWindow) return` the same toast). The receipt PDF page height is fixed A4 (841.89pt, `ReceiptPdfGenerator.java:48–49`) — an 80mm printer feeds ~297mm of blank roll per transaction; size the page from content.

4. ✅ Checked and fine: the PDF-failure toast is a clean client-side generic message; the 404 "no receipt" branch is unreachable through navigation. Leave the error copy alone.

---

## 2. Cashier terminal

### Keep as-is

- **The cash tender flow is the strongest part of the system** (`payment-panel.tsx:44–52, 128–146, 158–198`): change computed live as the cashier types, "Exact — $x" fill, inline (not toast-only) short-payment error, confirm dialog restating bill/received/close-consequence; a failed payment returns to the panel with inputs intact. Backend makes double-charge impossible via row lock + CLOSED-status check (`SessionPaymentServiceImpl.java:56–65`).
- **Mandatory typed reason gates the only pre-payment money-destroying action** (round cancel), and recorded voids surface everywhere they matter: cashier round card, kitchen ticket ("VOIDED — reason — do not cook"), and the guest's round view. Void fields (`voidedAt/voidedBy/voidReason`) are persisted, never deleted.
- **Session page:** pinned bottom action bar for the two workflow-ending actions; `tableNumber` threaded through router state so every screen titles itself "Table 7" before data lands; board cards self-label state in text so color never carries meaning alone.
- **Accidental table open is self-healing** (adversarially verified): round-less session derives IDLE, $0 payment rejected server-side, `findOrCreateActiveSession` idempotent, `SessionCleanupJob` auto-closes after 4h idle. No undo/Close-session affordance needed (one would strand guest tokens).
- **No menu search is fine** (verified): 13 items across 6 categories in the seed, no category paginates; search would be a full-stack feature for a menu that fits one screen.
- **Touch-target "broken" claims mostly refuted** — the round-panel trash sits ~200px from the stepper cluster, pre-send deletes are recoverable drafts, and the destructive action is already gated behind the reason dialog. (One remnant kept as a quick win: category pills, below.)
- **Partial tender / multi-tender:** self-correcting panel; split-check is a deliberate product ceiling worth a product decision, not a UI defect.
- 🟡 *Revisit (low):* reason dialog is free-text-only (backend accepts "x") — quick-pick reason chips would improve audit quality; round cards don't show QR-vs-staff source (the data model already encodes it).

### Problems

1. 🔴 **Single-item void is stranded** — *broken.*
   The most common mid-service correction ("one soup came back") has **no button anywhere**, despite the API (`PUT /rounds/{id}/lines/{lineId}/void` recomputes totals), the hook (`useVoidLine`, `use-session-rounds.ts:57–68` — **zero call sites**), and every render path (round card struck-through, kitchen ticket, bill summary) existing. Today the only option is cancel-and-rekey the whole round — which fires a duplicate full ticket at the kitchen and collapses a per-line void into a round cancel with no line-level reason.

   **Fix (nearly free):** rename the round-card Edit dropdown trigger (`round-card.tsx:254–271`, add to the single-edit branch at 244–252 too) to "Change" with "Modifiers…" and a destructive "Void item…" entry, wire `onVoidLine` to a second `ReasonDialog` ("Void 1× {name}? This can't be undone.") calling the already-typed `useVoidLine` payload; show it only while the round is SENT/READY, matching the backend constraint. No new dialog, no new API.

2. 🟡 **No table transfer, merge, or split path at all.**
   Greps for transfer/merge/split across both frontends and the whole backend: zero. Moving a guest mid-meal means cancelling rounds and retyping, destroying the round-level audit trail. Two compounding hazards: with only whole-round cancel wired, a move after one mixed round nukes kitchen-fired items; and an ACTIVE session whose rounds are all cancelled renders identical to a never-used IDLE table (`CashierTableServiceImpl.java:88–90`) while departed guests' QR tokens still resolve to it — a new party can be seated onto the zombie session.

   **Fix:** `PATCH /sessions/{id}/transfer` (targetTableId, optional roundIds) re-pointing ACTIVE rounds via the one-active-session-per-table guarantee, with "Move to table…" on the session header; merge is the same call from the second table. Interim: the line-void wiring downgrades the workaround to per-item. Independently, surface a rounds-less ACTIVE session as a distinct board state ("SEATED · no orders") so the zombie window is visible.

3. 🟡 **The till renders a frozen board as live indefinitely — no offline indicator anywhere.**
   Zero hits for `navigator.onLine`/online events in `frontend/src`; once any data exists, background refetch failures render the stale snapshot silently. The server is on EC2 (or cloud) while the till rides restaurant Wi-Fi — the realistic failure is Wi-Fi dropping mid-service while guests on 4G keep sending rounds. An "IDLE" tile may hold a seated table with live rounds, and the bill about to be collected may be missing the last round — the board lies with full confidence.

   **Fix:** global connectivity layer — window online/offline listeners + an "api unreachable" flag from consecutive query failures (`QueryCache.onError`), rendering a persistent non-dismissible "OFFLINE — data may be stale, last updated HH:MM" banner on board/session/bill while keeping last-good data visible but dimmed, with per-card stale badges.

---

## 3. Kitchen

1. 🔴 **Expired 24h token turns the mounted KDS into a dead-end error with no sign-in path.**
   `getToken()` returns null past expiry and `apiFetch` silently drops the Authorization header (`lib/api/client.ts:34–37, 53–58`); `RequireAuth` only checks token *presence* at route render (`require-auth.tsx:8–10`), so a screen mounted across midnight never redirects; the 5s poll then renders "Request failed (401)" with a "Try again" that re-runs the same failing query forever. The guest client already has the correct pattern (`guest-client.ts:17–18`). The KDS is wall-mounted and practically never closed: day two mid-dinner, the board dies while tickets pile up server-side unseen.

   **Fix:** staff-side 401 interceptor in `apiFetch` — on 401 with a token present, `clearToken()` and redirect to `/login` preserving the from-path (`require-auth.tsx:9` already supports this) so re-auth returns to the exact queue/cashier screen; show "Session expired — sign in again" wherever a mounted poll fails.

2. 🔴 **New orders arrive with no sound or visual cue** — *broken.*
   Silent 5s poll, no change detection, zero audio/notification handling anywhere in `frontend/src`, no WebSocket/SSE, and no kitchen printer in the backend — the queue is the system's **only** kitchen channel, and rounds can arrive from guests' phones with no staff involved. The 10m aging border is an escalation, not an arrival cue; new tickets append bottom-right, the least-attended slot. (Adversarially confirmed; the aging-border claim was even stronger than reported — the amber border never renders at all, `card.tsx:14` has no `border` class.)

   **Fix:** beep when the SENT list gains an id (unlock `AudioContext` lazily on the first "Mark ready" tap — browsers block autoplay; mute toggle persisted per device in localStorage; single short beep for multi-ticket batches), plus a one-shot animate-in highlight on cards whose `sentAt` landed since the last poll (`tw-animate-css` is already in package.json), and a flash of the "N tickets · oldest Xm" header on count change.

3. 🟡 **A refetch error replaces the live cook board while valid data sits in the cache.**
   In TanStack Query v5 a background-refetch failure **retains** data (`isRefetchError`), but the render ternary checks `isError` before the board branch (`queue.tsx:78–98`) — so during an outage the page shows "5 tickets · oldest 12m" in the header *and* "Couldn't load the cook queue" in the body. Same pattern degrades `session.tsx:82–88` and `bill.tsx:43–48`.

   **Fix:** gate the destructive alert on `isLoadingError` (error AND no data); when `isRefetchError`, keep rendering the last-known board with a slim amber "Reconnecting — times may be out of date" banner that self-clears on the next poll, and freeze/`~`-suffix the age badges since `now` keeps ticking against stale `sentAt`.

### Keep as-is

- Voided lines retained struck-through rather than silently dropped (the chef may already be cooking it), full-width `h-12` "Mark ready" button, per-ticket spinner, table/round-naming success toast.
- **FIFO grid is correct** (claim overturned): array is `sentAt`-asc and a row-major grid reads in Z-order, so reading order *is* FIFO; the proposed round-robin columns would have introduced an inversion. Keep the grid.

---

## 4. Guest cart — confirmed good ✅

Adversarial reviewers tried to refute "the cart is fine" and mostly failed. **Verified keep:**

- Per-option itemised prices with `= qty × unit` reconciliation (`guest-cart-line.tsx:62–86`), sticky-footer Add carrying the live build total, portion-budget counters for malatang-style builds, explained build-minimum, VAT (10%) line and dual-currency total above Send (`guest-item-dialog.tsx:186–397`, `cart.tsx:92–107`).
- Server-side per-device draft → Send is idempotent and fully recoverable from a dropped connection; spent-device 403 maps to a friendly "scan again" state (`SpentDeviceGuard.java:11–33`).
- **Closed-session dead-end: refuted.** `GuestSessionGuard` (`app/router/guest-session-guard.tsx:13–22`) wraps menu/cart/orders and shows "This session has ended — scan the table QR" — already handled, test-covered.
- **Send-finality: refuted as a problem.** Every path into the spent state dead-ends in an explicit rescan instruction at exactly the moment it's needed; need-time disclosure is the better design here.
- **Group-violation silent-disable: refuted.** `atMax` disables Plus with a "· full" counter so max violations can't occur; the only required group in the live menu sits at the top with a "Pick 1" label.

### Only label-level touch-ups (quick wins)

1. `orders.tsx:29–33, 69` — h1 → "Orders at this table" (sticky header already pins the table number, don't duplicate it); add caption "Everyone at your table shares one bill — pay at the counter"; fix empty state to "Rounds sent at this table will appear here" (current copy teaches the wrong ownership model).
2. `cart.tsx:119` — "Send order" → **"Send to kitchen"** (forward-discloses finality at the decision point without adding process copy to the conversion-critical spot).
3. `guest-menu-browser.tsx` (header area) — one static muted line: "Prices shown before VAT" — deliberately **without** a hardcoded percentage, since the menu API carries no `vatRate` and a "10%" label would go stale.
4. 🟡 **86'd items silently vanish instead of showing "Sold out"** — *revisit, trust-eroding.* The guest list endpoint hard-filters `available=true` (`GuestMenuController.java:46–52`); the string "sold out" appears nowhere in `frontend/src`; a whole empty category reads "Nothing available here" (staff phrasing leaking to guests). A regular can't tell sold-out from deleted from broken-phone.
   **Fix:** return all active items with the `available` flag (the detail endpoint already serves unavailable options); render unavailable cards dimmed, non-interactive, with a bilingual "Sold out / អស់" badge. Pair with the send path: mark cart lines whose item became unavailable between add and send ("No longer available" badge + prominent Remove) instead of rejecting the whole send with a raw English server string naming a dish the guest can no longer see.
5. **86 requires leaving the service screen for the admin backoffice** — availability is toggled only by the admin Switch; zero availability-write affordances in kitchen or cashier. During rush, guests keep ordering items that don't exist. **Fix:** one-tap "86 / Sold out" on kitchen ticket lines and cashier menu cards calling the existing PATCH, invalidating menu query keys so guests see it within seconds.

---

## 5. Admin

**Keep:** correct destructive hierarchy — recoverable availability Switch one-touch on the card face; Delete behind overflow + AlertDialog naming the actual consequence ("item disappears from menu; printed QR stops working"); QR regeneration destructive-styled with a when-would-you explanation; users cannot delete themselves (`menu-item-card.tsx:57–110`, `table-qr-dialog.tsx:96–118`, `delete-table-dialog.tsx:29–35`). All six working pages have direct sidebar links.

**Revisit:**

1. **Overview shows hardcoded `—` stats** (`overview.tsx:29`) on the post-login landing screen — wire from existing endpoints or delete the nav entry.
2. **No batch QR printing** — one dialog per table; the dialog's own copy ("Print this QR code and place it on the table") confirms in-app printing is the designed path, so opening N tables deserves a print-all.

---

## Ranked action list

| # | Change | Surface | Verdict | Cost |
|---|--------|---------|---------|------|
| 1 | Wire "Void item…" to the existing `useVoidLine` hook | cashier | broken | ~1h |
| 2 | Reprint: "View last receipt" chip on board → receipt route | receipt | broken | ~½ day + small backend |
| 3 | 401 interceptor → sign-in-and-return | kitchen/cashier | broken | ~2h |
| 4 | Kitchen beep + animate-in on new tickets | kitchen | broken | ~2h |
| 5 | `isLoadingError` gate so refetches don't wipe boards | kitchen/cashier/bill | broken | ~30m each |
| 6 | Offline/stale banner on till | cashier | revisit | ~½ day |
| 7 | Modifier breakdown on bill/receipt screen (shared sub-row component) | receipt | revisit | ~½ day |
| 8 | Sold-out badging + one-tap 86 from kitchen/cashier | guest/kitchen/cashier | revisit | ~1 day |
| 9 | Table transfer endpoint + "Move to table…" | cashier | revisit (product) | bigger |
| 10 | Quick wins: QR-ref row wrapping + maxLength 64; guest labels; "Send to kitchen"; "Prices shown before VAT"; popup-blocked guard; A4→content-height PDF; category pills 20px→~32px chips | mixed | — | <1h each |

---

## What we checked and found fine (coverage record)

- Guest cart core (pricing, footer, portion budgets, VAT/dual-currency, pencil-only-on-built-lines editing with prefill/merge-back).
- Guest closed-session handling (`GuestSessionGuard`), send-finality disclosure, group-violation gating, offline cart durability (only nit: "Is the API running?" toast wording is staff-register — fix at the copy layer).
- Kitchen board sequence (row-major Z-order is literally oldest-first).
- Cashier accidental-table-open self-healing; menu browsing without search at current menu size; touch-target claims beyond the pills; partial-tender self-correction; payment double-charge impossibility.
- Receipt error copy; EN-only paper consistent with EN-only guest journey; void history integrity (persisted + surfaced at all three layers; exclusion from the customer receipt is per spec "show what was charged" — the only real gaps are a post-close manager history view and an optional "N items voided" line).
- Admin navigation completeness.
- Spec-conformant happy paths generally: the adversarial pass overturned the loudest claims (touch-scale "broken", FIFO grid, menu search, "erased void audit"), which is why the remaining findings concentrate in corrections, recovery, and liveness.
