# Restaurant POS — Build Plan

> Phase-by-phase build plan. Conventions, tech stack, and project structure live in `CLAUDE.md`.
> When starting a phase, implement it against the conventions in CLAUDE.md.

> **Scope update (July 2026):** the product is QR-at-table customer ordering + a cashier screen.
> Phases 4–7 below are superseded/reshaped by two specs — build from the specs, not from the
> original phase text (kept for history):
>
> - `docs/CUSTOMER_ORDERING_SPEC.md` — Part A: modifiers (replaces Phase 4, ✅ built);
>   Part B: tables/sessions, draft cart, order rounds (steps C2–C4, migrations V5–V7, ✅ built —
>   per-device carts / one send per device added in V10); promo banners (C5) still pending.
> - `docs/CASHIER_SPEC.md` — table status board, order queue, void/cancel, cashier manual
>   ordering, payment & close-out (reshapes Phases 6–7; ✅ built, migration V8).
>
> Phases 0A–3 are built; receipts (Phase 8) are in progress (migration V9). Phases 9–11
> (audit, reports, settings) still apply on top. Phase 5's discount-promotion engine is
> deferred (promo *banners* moved to the customer spec).

---

## Build Phases

### Phase 0A — Minimum Foundation

**Goal:** Bare minimum scaffolding before any feature.

**Build:**
- `RestaurantPosApplication.java`
- `BaseEntity.java` — `@MappedSuperclass` with UUID `id`, `createdAt`, `updatedAt` (auto-managed via `@PrePersist` / `@PreUpdate`)
- `ApiException.java` — extends `RuntimeException`, carries `HttpStatus`
- `ApiResponse.java` — standardized wrapper: `success`, `message`, `data`, `timestamp` (needed by GlobalExceptionHandler and every controller from Phase 1 onward)
- `GlobalExceptionHandler.java` — `@RestControllerAdvice`, returns `ApiResponse`
- `application.yml` — datasource, JPA, Flyway, server port config
- `docker-compose.yml` — PostgreSQL 17 service

**Depends on:** Nothing

---

### Phase 1 — User Management

**Goal:** Authentication, authorization, user CRUD.

**Entities:**
- `User` — username, email, password (BCrypt), enabled, roles (ManyToMany)
- `Role` — name (enum: `ADMIN`, `CASHIER`)

**Features:**
- `POST /api/v1/auth/login` — returns JWT access token (public)
- `POST /api/v1/auth/register` — admin-only user creation, guarded with `@PreAuthorize("hasRole('ADMIN')")`
- `GET/PUT/DELETE /api/v1/users/{id}` — user CRUD (admin-only)
- `PUT /api/v1/users/{id}/password` — password reset
- JWT filter chain: `JwtAuthFilter` validates token on every request
- `CustomUserDetailsService` loads user by username for Spring Security
- `SecurityConfig` — permit ONLY `/api/v1/auth/login` and the Swagger/OpenAPI paths; authenticate everything else. Do NOT permit `/api/v1/auth/**` wholesale — `register` must stay admin-only.

**Migration:** `V1__create_user_role_tables.sql`
- `users` table
- `roles` table
- `user_roles` join table
- Seed default ADMIN user — username `admin`, password `Admin@123` (BCrypt hash in migration; change in production)

**Depends on:** Phase 0A

---

### Phase 2 — Currency Management

**Goal:** USD/KHR support with configurable exchange rates.

**Entities:**
- `Currency` — code (`USD`, `KHR`), name, symbol, is_default
- `ExchangeRate` — from_currency, to_currency, rate, effective_date

**Features:**
- `GET /api/v1/currencies` — list currencies
- `GET/POST/PUT /api/v1/exchange-rates` — manage exchange rates
- Service method: `convert(BigDecimal amount, String from, String to)` — used by Order and Payment modules later

**Migration:** `V2__create_currency_exchange_rate_table.sql`
- `currencies` table
- `exchange_rates` table
- Seed USD and KHR with default rate (1 USD = 4100 KHR or current)

**Depends on:** Phase 1 (auth required for admin endpoints)

---

### Phase 3 — Menu Management

**Goal:** Full menu CRUD with categories, items, bilingual support.

**Common addition:** `PageResponse.java` — generic paginated response wrapper with `content`, `page`, `size`, `totalElements`, `totalPages`.

**Entities:**
- `Category` — nameEn, nameKm, description, sortOrder, active
- `MenuItem` — nameEn, nameKm, descriptionEn, descriptionKm, price, currency, imageUrl, available, category (ManyToOne)

**Features:**
- `CRUD /api/v1/categories` — with pagination
- `CRUD /api/v1/menu-items` — with pagination, filter by category, filter by availability
- `POST /api/v1/menu-items/{id}/image` — image upload (store on filesystem or cloud)
- Bilingual: all name/description fields stored in both `_en` and `_km` columns

**Migration:** `V3__create_menu_category_item_tables.sql`
- `categories` table
- `menu_items` table (FK to `categories`)

**Depends on:** Phase 2 (menu items reference currency)

---

### Phase 4 — Modifier Management

> **Superseded — build from `docs/CUSTOMER_ORDERING_SPEC.md` Part A.** Key differences from the
> text below: `minChoice`/`maxChoice` instead of `required`/`multiSelect` (`minChoice >= 1` ⇒
> required), options carry `unitPrice`/`imageUrl`/`packSize`/`available`, join table is
> `menu_item_modifier_groups` with per-item `sortOrder`.

**Goal:** Configurable modifiers (size, temperature, sugar, ice) linked to menu items.

**Entities:**
- `ModifierGroup` — name (e.g. "Size", "Sugar Level"), required (boolean), multiSelect (boolean)
- `ModifierOption` — name (e.g. "Large", "50%"), priceAdjustment, modifierGroup (ManyToOne)
- `MenuItemModifier` — join entity linking MenuItem to ModifierGroup (ManyToMany with extra columns if needed)

**Features:**
- `CRUD /api/v1/modifier-groups` — with nested options
- `POST /api/v1/menu-items/{id}/modifiers` — attach modifier groups to a menu item
- When creating an order item, the client selects from available modifier options

**Migration:** `V4__create_modifier_tables.sql`
- `modifier_groups` table
- `modifier_options` table (FK to `modifier_groups`)
- `menu_item_modifiers` join table (FK to `menu_items` and `modifier_groups`)

**Depends on:** Phase 3 (modifiers attach to menu items)

---

### Phase 5 — Promotion Management

> **Deferred.** Display-only promo *banners* moved to `docs/CUSTOMER_ORDERING_SPEC.md` (step C5).
> The discount engine below is postponed until after the customer + cashier modules ship.

**Goal:** Flexible promotions that the order module can apply.

**Entities:**
- `Promotion` — name, description, promotionType (enum), discountValue, discountUnit (PERCENTAGE or FIXED), startDate, endDate, active, minOrderAmount, applicableItems (ManyToMany to MenuItem, nullable for store-wide)
- `PromotionType` — enum: `BUY_ONE_GET_ONE`, `HAPPY_HOUR`, `STUDENT_DISCOUNT`, `PERCENTAGE_OFF`, `FIXED_AMOUNT_OFF`

**Features:**
- `CRUD /api/v1/promotions`
- `PUT /api/v1/promotions/{id}/activate` — toggle active
- `GET /api/v1/promotions/active` — list currently active promotions
- Validation: date range checks, discount value bounds

**Migration:** `V5__create_promotion_tables.sql`
- `promotions` table
- `promotion_menu_items` join table (FK to `menu_items`)

**Depends on:** Phase 3 (promotions reference menu items)

---

### Phase 6 — POS & Order Processing

> **Superseded.** Replaced by tables/sessions, draft cart, and immutable order rounds
> (`docs/CUSTOMER_ORDERING_SPEC.md` Part B, steps C2–C4) plus the cashier workflow — status
> board, FIFO round queue, void-not-delete, manual ordering (`docs/CASHIER_SPEC.md` §1–§5).
> The `Order`/`OrderItem` entities below do not get built; `OrderRound` + snapshot line
> items take their place.

**Goal:** Core order flow — the heart of the POS.

**Entities:**
- `Order` — orderNumber (auto-generated), orderStatus, subtotal, discountAmount, taxAmount, totalAmount, currency, notes, createdBy (FK to User)
- `OrderItem` — order (ManyToOne), menuItem (ManyToOne), quantity, unitPrice, totalPrice, notes
- `OrderItemModifier` — orderItem (ManyToOne), modifierOption (ManyToOne), priceAdjustment
- `OrderStatus` — enum: `PENDING`, `PREPARING`, `COMPLETED`, `CANCELLED`

**Features:**
- `POST /api/v1/orders` — create order with items and modifiers
- `PUT /api/v1/orders/{id}/items` — add/remove items
- `PUT /api/v1/orders/{id}/status` — update status
- `GET /api/v1/orders` — list with filters (status, date range, pagination)
- `OrderPricingService` — calculates item prices + modifier adjustments + promotion discounts + currency conversion; recomputes totals on every cart change

**Business Logic:**
1. For each `OrderItem`: `unitPrice = menuItem.price + sum(selectedModifiers.priceAdjustment)`
2. `subtotal = sum(orderItems.quantity * unitPrice)`
3. Apply promotions: `discountAmount` computed based on active promotions
4. `totalAmount = subtotal - discountAmount + taxAmount`
5. Currency conversion via `CurrencyService.convert()` if order currency differs

**Migration:** `V6__create_order_tables.sql`
- `orders` table (FK to `users`)
- `order_items` table (FK to `orders`, `menu_items`)
- `order_item_modifiers` table (FK to `order_items`, `modifier_options`)

**Depends on:** Phase 3 + Phase 4 + Phase 5 + Phase 2

---

### Phase 7 — Payment Processing

> **Reshaped by `docs/CASHIER_SPEC.md` §6.** Payments attach to a `TableSession` (the whole
> visit), not a single order. v1 methods: `CASH` (change in USD + KHR) and `QR` (manual
> verification, no KHQR integration — reference note only). Single payment per session in v1;
> split payments and card deferred. Payment confirm closes the session and completes its rounds.

**Goal:** Accept payments against orders.

**Entities:**
- `Payment` — order (ManyToOne), paymentMethod (enum), amount, currency, amountInOrderCurrency, changeAmount, referenceNumber, paidAt
- `PaymentMethod` — enum: `CASH`, `KHQR`, `CARD`

**Features:**
- `POST /api/v1/orders/{orderId}/payments` — record a payment
- `GET /api/v1/payments` — list payments with filters
- `CashPaymentService` — validates amount >= order total, calculates change
- `KhqrPaymentService` — generates KHQR code/payload, records reference
- `CardPaymentService` — placeholder for card terminal integration
- On full payment: auto-update order status to `COMPLETED`
- Support split payments (multiple payments per order until total covered)

**Migration:** `V7__create_payment_tables.sql`
- `payments` table (FK to `orders`)

**Depends on:** Phase 6

---

### Phase 8 — Receipt Management

**Goal:** Generate, print, and download receipts.

**Entities:**
- `Receipt` — order (OneToOne), receiptNumber, generatedAt, pdfUrl

**Features:**
- `POST /api/v1/orders/{orderId}/receipt` — generate receipt
- `GET /api/v1/receipts/{id}` — get receipt metadata
- `GET /api/v1/receipts/{id}/pdf` — download PDF
- `ReceiptPdfGenerator` — uses iText/OpenPDF to create receipt PDF with:
    - Restaurant name/logo
    - Order items with modifiers
    - Subtotal, discounts, tax, total
    - Payment method and change
    - Bilingual content (EN/KM)
    - QR code (optional)

**Migration:** `V8__create_receipt_tables.sql`
- `receipts` table (FK to `orders`)

**Depends on:** Phase 6 + Phase 7

---

### Phase 9 — Audit Logging

**Goal:** Track all important system activities.

**Entities:**
- `AuditLog` — action (enum), entityType, entityId, userId, oldValue (JSON), newValue (JSON), ipAddress, timestamp

**Features:**
- `AuditEventListener` — Spring `@EventListener` or `@EntityListeners` that auto-captures:
    - Login events (no logout event — stateless JWT has no server-side logout)
    - User CRUD operations
    - Menu item changes (especially price changes)
    - Order status changes
    - Payment events
- `GET /api/v1/audit-logs` — admin-only, with filters (action, entity, user, date range)
- `AuditLogService` — provides `log(action, entity, oldVal, newVal)` method callable from any service

**Migration:** `V9__create_audit_log_table.sql`
- `audit_logs` table (FK to `users`)

**Depends on:** All prior phases (wraps existing write operations)

---

### Phase 10 — Reporting & Analytics

**Goal:** Sales and performance reports.

**No new entities.** Query-only module against existing `orders`, `order_items`, `payments` tables.

**Features:**
- `GET /api/v1/reports/sales/daily?date=YYYY-MM-DD`
- `GET /api/v1/reports/sales/weekly?startDate=...`
- `GET /api/v1/reports/sales/monthly?month=YYYY-MM`
- `GET /api/v1/reports/sales?startDate=...&endDate=...` — custom range
- `GET /api/v1/reports/products/performance` — top sellers, revenue per item
- `GET /api/v1/reports/dashboard` — summary: today's revenue, order count, avg order value, top items

**Services:**
- `SalesReportService` — aggregates revenue, order count, payment method breakdown
- `ProductPerformanceService` — item-level sales data
- `DashboardService` — compiles summary stats

**No migration required.**

**Depends on:** Phase 6 + Phase 7

---

### Phase 11 — Administration & Polish

**Goal:** System settings, localization, API docs, final config.

**Entities:**
- `SystemSetting` — key (unique), value, description, updatable

**Features:**
- `CRUD /api/v1/settings` — admin-only system settings
- Settings examples: `restaurant.name`, `restaurant.address`, `tax.rate`, `receipt.footer`, `default.currency`
- `OpenApiConfig` — Swagger UI at `/swagger-ui.html`, API docs at `/v3/api-docs`
- `WebConfig` — CORS configuration
- `LocaleConfig` — `MessageSource` bean loading `messages_en.properties` and `messages_km.properties`, locale resolver
- i18n: error messages, validation messages, receipt text

**Migration:** `V10__create_system_setting_table.sql`
- `system_settings` table
- Seed default settings

**Depends on:** Everything else

---

## Dependency Graph

```text
Phase 0A (Foundation)
  └─► Phase 1 (Users)
       └─► Phase 2 (Currency)
            └─► Phase 3 (Menu)
                 ├─► Phase 4 (Modifiers) ──┐
                 └─► Phase 5 (Promotions) ─┤
                                           ▼
                              Phase 6 (Orders) ◄── Phase 2
                                   │
                                   ▼
                              Phase 7 (Payments)
                                   │
                     ┌─────────────┼─────────────┐
                     ▼             ▼             ▼
                Phase 8       Phase 9       Phase 10
               (Receipts)  (Audit Logs)   (Reports)
                     │             │             │
                     └─────────────┼─────────────┘
                                   ▼
                         Phase 11 (Admin & i18n)
```
