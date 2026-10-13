# 🍽️ Restaurant POS & QR System

[![Java](https://img.shields.io/badge/Java-21%20LTS-ED8B00?logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5-6DB33F?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-19-61DAFB?logo=react&logoColor=black)](https://react.dev/)
[![TypeScript](https://img.shields.io/badge/TypeScript-5.x-3178C6?logo=typescript&logoColor=white)](https://www.typescriptlang.org/)
[![Vite](https://img.shields.io/badge/Vite-6.x-646CFF?logo=vite&logoColor=white)](https://vitejs.dev/)
[![Tailwind CSS](https://img.shields.io/badge/Tailwind%20CSS-v4-06B6D4?logo=tailwindcss&logoColor=white)](https://tailwindcss.com/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-17-4169E1?logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?logo=docker&logoColor=white)](https://www.docker.com/)

A modern, full-stack Restaurant Management and Point of Sale (POS) system engineered for high-throughput dine-in and quick-service operations. The system features a zero-install QR customer self-ordering client (optimized for DIY Malatang & customizable menu items), a live cashier terminal with real-time table tracking and order queues, and a comprehensive administrative backoffice.

---

## 📑 Table of Contents

- [Key Features](#-key-features)
  - [1. 📱 Customer QR-at-Table Ordering](#1--customer-qr-at-table-ordering)
  - [2. 🖥️ Cashier & Till Terminal](#2-️-cashier--till-terminal)
  - [3. 👨‍🍳 Kitchen Display](#3--kitchen-display)
  - [4. ⚙️ Admin Backoffice](#4-️-admin-backoffice)
  - [5. 🛡️ Architectural Highlights](#5-️-architectural-highlights)
- [Tech Stack](#-tech-stack)
- [Project Structure](#-project-structure)
- [Getting Started](#-getting-started)
  - [Prerequisites](#prerequisites)
  - [Method 1: Docker Compose (Recommended)](#method-1-docker-compose-recommended)
  - [Method 2: Manual Local Development](#method-2-manual-local-development)
  - [Database Seeding](#database-seeding)
- [Default Credentials & Access Points](#-default-credentials--access-points)
- [Environment Configuration](#-environment-configuration)
- [Ordering & Session Lifecycle](#-ordering--session-lifecycle)
- [Production Deployment & CI/CD](#-production-deployment--cicd)
- [Documentation & Specifications](#-documentation--specifications)

---

## 🌟 Key Features

### 1. 📱 Customer QR-at-Table Ordering
- **No App or Registration Required**: Diners scan a per-table QR code to instantly start ordering.
- **Multi-Device Support with Private Draft Carts**: Multiple guests at the same table share one bill and active session while holding separate, private draft carts on their phones (no edit collisions).
- **One-Send-Per-Scan Model**: Once an order round is submitted, the device token is spent to prevent double-submitting. Re-scanning mints a new turn on the same active table session.
- **Bilingual Interface**: Full localization in English and Khmer (`_en` and `_km`).
- **Composable Modifiers**: Rich item customization with min/max selection rules (broths, meat, veggies, toppings, spiciness levels, add-ons).
- **Dual-Currency Cart**: Automatic pricing recalculations with VAT handling and real-time USD/KHR currency conversion.

### 2. 🖥️ Cashier & Till Terminal
- **Visual Table Status Board**: Polled overview with color-coded states (mixed rounds roll up most-urgent-wins: any NEW/COOKING beats any READY beats SERVED):
  - ⬜ **IDLE**: Table empty.
  - 🟨 **WAITING**: Any live round `NEW` or `COOKING` (kitchen working).
  - 🟦 **AT PASS**: All rounds cooked and at least one `READY` (food at the pass — go run it).
  - 🟩 **READY TO PAY**: All rounds `SERVED` with an outstanding balance.
- **Station Routing (KITCHEN vs COUNTER)**: Every menu item carries a station tag, snapshotted onto each order line. `KITCHEN` lines feed the KDS; `COUNTER` lines (drinks, desserts) are flagged for the cashier's counter queue instead of the cooks.
- **Round & Line Item Controls**: Cancel rounds with mandatory audit reasons, or void/edit specific line items. Voids and edits are **rejected with 409 once the kitchen has started cooking** (or the round is READY/SERVED); rounds with unserved KITCHEN lines cannot be cancelled.
- **Walk-In / Manual Ordering**: Staff can open sessions and submit rounds directly for phone-less customers.
- **Bill Calculation & Settlement**: Dual-currency settlement (USD & KHR) with support for:
  - **Cash**: Computes exact change across currencies.
  - **KHQR**: Direct QR payment verification workflows.
- **Bilingual Printable Receipts**: Instant receipt payload generation upon payment confirmation.

### 3. 👨‍🍳 Kitchen Display
- **Dedicated Cook Queue**: A `CHEF` account lands on `/kitchen`, a polled board of every round with `KITCHEN` lines across all tables, oldest first. `COUNTER` lines (drinks/desserts) never reach the kitchen — they go to the cashier's counter queue.
- **Persisted Cooking State**: "Start Cooking" and per-line statuses are saved to the database (not tablet-local state), so a screen refresh or tablet reboot never loses ticket state. The cashier table board mirrors them live.
- **Line-Level Progress & Bumping**: Cooks strike out individual dishes as finished; once all KITCHEN lines are done, "Mark Ready" moves the ticket to the expedite column, and "Bump" clears it after the food is run.
- **Ticket Ageing**: Tickets pass 10 minutes to amber and 20 minutes to red so a backed-up pass is visible at a glance.
- **Narrow Permissions**: The chef's entire API surface is `KitchenController` — read the queue, start cooking, mark lines ready, bump. Cancel, void, payments and the table board stay with the cashier.

### 4. ⚙️ Admin Backoffice
- **Category & Menu Management**: Create, reorder, toggle availability, upload dish photos, and set bilingual descriptions.
- **Modifier Groups**: Attach modular modifier groups to menu items with enforceability rules (`minChoice`, `maxChoice`, unit pricing).
- **Table Management**: Setup floor layouts, manage table numbers, and generate/download table QR tokens.
- **User & Role Management**: Provision staff accounts with granular role-based permissions (`ADMIN`, `CASHIER`, `CHEF`).

### 5. 🛡️ Architectural Highlights
- **Immutable Order Rounds vs. Mutable Carts**: Carts exist as mutable drafts; submitting copies them into immutable `OrderRoundLineItem` snapshots preserving historical pricing and modifier selections.
- **Server-Authoritative Pricing**: All item prices, modifier deltas, tax rates, and exchange rates are computed exclusively on the backend.
- **Pessimistic Session Locking**: Prevents race conditions and double-submits during cart sends and payment confirmations.

---

## 💻 Tech Stack

### Backend
| Layer | Technology | Details |
|---|---|---|
| **Runtime & Language** | Java 21 LTS | Records, sealed classes, pattern matching |
| **Framework** | Spring Boot 3.5.x | Spring Web, Spring Data JPA, Spring Security |
| **Database** | PostgreSQL 17 | Managed with Flyway database migrations (`db/migration`) |
| **Authentication** | Spring Security + JWT | Token-based auth with RBAC (`ADMIN`, `CASHIER`, `GUEST`) |
| **Mapping & Helpers** | MapStruct + Lombok | High-performance, compile-time object mapping |
| **API Documentation** | SpringDoc OpenAPI 2.x | Interactive Swagger UI at `/swagger-ui.html` |

### Frontend
| Layer | Technology | Details |
|---|---|---|
| **Framework & Build** | React 19 + TypeScript | Vite 6 build tool with HMR |
| **Styling & UI** | Tailwind CSS v4 + Radix UI | shadcn/ui component architecture, Geist font |
| **Routing** | React Router v8 | Guarded route trees (Admin, Cashier, Kitchen, Guest sessions) |
| **State & Data Fetching** | TanStack Query v5 | Auto-caching, optimistic UI, background polling |
| **Tables & Forms** | TanStack Table + React Hook Form | Schema-driven form validation with Zod |
| **Utilities** | Lucide React, qrcode.react, Sonner | Icons, QR generation, toast notifications |

### Infrastructure & DevOps
- **Containerization**: Multi-stage `Dockerfile` builds for backend (Eclipse Temurin JRE) and frontend (Nginx Alpine).
- **Orchestration**: `docker-compose.yml` linking Postgres, Spring Boot backend, and Nginx frontend.
- **Reverse Proxy**: Nginx handling SSL termination (Let's Encrypt), SPA routing, and API reverse proxying.
- **CI/CD**: GitHub Actions deploying to AWS EC2 over SSH on pushes to `main`.

---

## 📂 Project Structure

```text
restaurant/
├── .github/workflows/          # GitHub Actions CI/CD workflows
│   └── deploy.yml              # Automated EC2 deployment pipeline
├── backend/                    # Spring Boot 3.5 backend application
│   ├── docs/                   # Engineering specs & architecture references
│   │   ├── CASHIER_SPEC.md     # Cashier screen & round lifecycle spec
│   │   ├── CUSTOMER_ORDERING_SPEC.md # QR customer ordering spec
│   │   ├── PLAN.md             # Development milestones & dependency roadmap
│   │   └── CLAUDE.md           # Backend coding standards & guidelines
│   ├── scripts/
│   │   └── seed.sh             # Shell script to seed categories, items & modifiers
│   ├── src/main/java/com/vichovong/restaurant_pos/
│   │   ├── common/             # Base entities, API envelopes & global exceptions
│   │   ├── config/             # Security, Web MVC, and OpenAPI configurations
│   │   ├── security/           # JWT providers, filters & auth handlers
│   │   └── feature/            # Modular domain features:
│   │       ├── auth/ & user/   # User authentication and management
│   │       ├── currency/       # USD/KHR exchange rates engine
│   │       ├── menu/           # Categories and menu items
│   │       ├── modifier/       # Modifier groups and options
│   │       ├── table/          # Dining tables and active sessions
│   │       ├── cart/           # Per-device draft carts & pricing service
│   │       ├── order/          # Order rounds and immutable line snapshots
│   │       ├── payment/        # Payment processing and settlement
│   │       └── receipt/        # Receipt formatting and generation
│   ├── src/main/resources/
│   │   ├── application.yml     # Application configuration
│   │   └── db/migration/       # Flyway SQL schema migrations (V1 to V10)
│   ├── Dockerfile              # Multi-stage Java 21 build
│   └── pom.xml                 # Maven dependencies
├── frontend/                   # React 19 + TypeScript SPA
│   ├── src/
│   │   ├── app/                # Router, route guards, and providers
│   │   ├── components/         # Shared shadcn/ui components
│   │   ├── features/           # Feature-specific state, API hooks & modals
│   │   ├── layouts/            # AdminLayout, CashierLayout, GuestLayout
│   │   └── pages/              # Admin, Cashier, Kitchen, Guest, and Auth pages
│   ├── Dockerfile              # Multi-stage Node build with Nginx
│   ├── nginx.conf              # Production Nginx reverse proxy configuration
│   └── package.json
├── docker-compose.yml          # Root multi-container orchestration
├── .env.example                # Environment template
└── README.md                   # Project documentation
```

---

## 🚀 Getting Started

### Prerequisites

Ensure you have the following installed on your machine:
- **Docker** & **Docker Compose** (recommended for quick setup), or:
- **Java 21 JDK** & **Maven**
- **Node.js 20+** & **npm**
- **PostgreSQL 17**

---

### Method 1: Docker Compose (Recommended)

1. **Clone the repository:**
   ```bash
   git clone https://github.com/vichevong1-coder/resturant.git
   cd resturant
   ```

2. **Prepare Environment Variables:**
   ```bash
   cp .env.example .env
   ```
   *(Update values in `.env` if necessary; see [Environment Configuration](#-environment-configuration).)*

3. **Build and Launch Services:**
   ```bash
   docker compose up -d --build
   ```

4. **Verify Running Containers:**
   ```bash
   docker compose ps
   ```
   You should see `restaurant-pos-db`, `restaurant-pos-backend`, and `restaurant-pos-frontend` active.

---

### Method 2: Manual Local Development

#### 1. Start the Database
Start a local PostgreSQL 17 instance on port `5432` with a database named `restaurant_pos`, user `pos_user`, and password `pos_password` (or run PostgreSQL via Docker):
```bash
docker run --name restaurant-pos-db -e POSTGRES_DB=restaurant_pos -e POSTGRES_USER=pos_user -e POSTGRES_PASSWORD=pos_password -p 5432:5432 -d postgres:17-alpine
```

#### 2. Start the Backend
```bash
cd backend
./mvnw clean spring-boot:run
```
The backend starts on `http://localhost:8080`. Flyway will automatically execute database migrations on startup.

#### 3. Start the Frontend
In a new terminal:
```bash
cd frontend
npm install
npm run dev
```
The frontend dev server starts on `http://localhost:5173` with Vite's proxy automatically forwarding `/api` and `/uploads` to `http://localhost:8080`.

---

### Database Seeding

To populate the system with a complete set of categories, menu items, modifier groups, and sample dining tables (DIY Malatang test dataset):

```bash
# Make sure the backend is running on localhost:8080
chmod +x ./backend/scripts/seed.sh
./backend/scripts/seed.sh
```

---

## 🔑 Default Credentials & Access Points

| Service / Interface | URL | Credentials / Notes |
| **Frontend Web App** | `http://localhost:5173` *(dev)* or `http://localhost` *(Docker / prod)* | Redirects to role landing / login |
| **Admin & Cashier Login** | `/login` | **Username:** `admin`<br>**Password:** `Admin@123` *(dev default)* |
| **Customer QR Ordering** | `/guest?token=<qrToken>` | Token generated automatically per table |
| **Backend REST API** | `http://localhost:8080/api/v1` | Public and Authenticated routes |
| **Swagger / OpenAPI UI** | `http://localhost:8080/swagger-ui.html` | Interactive API documentation & testing |

---

## ⚙️ Environment Configuration

Configuration is managed via the root `.env` file:

| Variable | Default Value | Description |
|---|---|---|
| `POSTGRES_DB` | `restaurant_pos` | PostgreSQL database name |
| `POSTGRES_USER` | `pos_user` | Database master user |
| `POSTGRES_PASSWORD` | `pos_password` | Database password *(change in production)* |
| `JWT_SECRET` | *(required in prod)* | 256-bit signing key for JWT auth tokens |
| `UPLOAD_DIR` | `uploads` | Directory for uploaded menu images |
| `TAX_RATE` | `0.10` | Default VAT tax rate applied to carts (10%) |
| `RESTAURANT_NAME` | `Malantang Restaurant` | Name printed on receipts |
| `ADMIN_USERNAME` | `admin` | Initial seeded administrator username |
| `ADMIN_EMAIL` | `admin@restaurant-pos.local` | Initial administrator email |
| `ADMIN_PASSWORD` | `Admin@123` | Initial administrator password |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:*,http://127.0.0.1:*` | Allowed browser origins for CORS |

---

## 🔄 Ordering & Session Lifecycle

```
 ┌────────────────┐
 │ Customer Scans │ ──► POST /api/v1/guest/sessions (qrToken)
 │ Table QR Code  │     - Mints GUEST JWT with (sessionId, deviceId)
 └────────────────┘
         │
         ▼
 ┌────────────────┐
 │  Drafting Cart │ ──► PUT /api/v1/guest/cart/items (Private to deviceId)
 │   & Modifiers  │     - Live pricing re-derived on server
 └────────────────┘
         │
         ▼
 ┌────────────────┐
 │   Send Round   │ ──► POST /api/v1/guest/cart/send
 │ (Submit Order) │     - Atomic snapshot to OrderRound (status: SENT)
 └────────────────┘     - Device becomes SPENT (Prevents double submission)
         │
         ▼
 ┌────────────────┐
 │ Kitchen Queue  │ ──► GET /api/v1/kitchen/rounds (KITCHEN lines only)
 │ & Table Board  │     PUT /kitchen/rounds/{id}/start-cooking → COOKING
 │                │     PUT /kitchen/rounds/{id}/lines/{lineId}/status → READY
 │                │     PUT /kitchen/rounds/{id}/ready (all lines done)
 │                │     PUT /kitchen/rounds/{id}/bump → SERVED
 └────────────────┘
         │
         ▼
 ┌────────────────┐
 │    Checkout    │ ──► POST /api/v1/sessions/{id}/payments (CASH / KHQR)
 │   & Receipts   │     - Marks Rounds PAID (fulfillment unaffected —
 └────────────────┘       kitchen may still be cooking)
                          - Closes Session, returns bilingual receipt payload
```

---

## 🚢 Production Deployment & Hosting

- **Laptop Hosting via Cloudflare Tunnels**: This system is self-hosted locally on a roaming Ubuntu laptop. It uses Cloudflare Tunnels (`cloudflared` in `docker-compose.yml`) to securely expose the application to the internet (or custom domain) without opening router ports or requiring a static IP.
- **SSL / HTTPS**: SSL certificates and HTTPS termination are handled automatically by Cloudflare. The local `nginx.conf` is configured to run plain HTTP, which Cloudflare encrypts securely.
- **Deploying Updates**: Because the host is a roaming laptop, the old GitHub Actions automated SSH deployment has been disabled. To deploy new code, simply pull the latest `main` branch on the host machine and run `docker compose up -d --build`.
- **Database Port**: The PostgreSQL database exposes port `5435` instead of the default `5432` to intentionally prevent collisions with other local AI and microservice projects running on the same laptop.

---

## 📚 Documentation & Specifications

Detailed architectural plans and feature specifications are located in `backend/docs/`:
- [backend/docs/CUSTOMER_ORDERING_SPEC.md](backend/docs/CUSTOMER_ORDERING_SPEC.md): Full specification of customer QR scanning, session creation, private draft carts, and one-send-per-device rules.
- [backend/docs/CASHIER_SPEC.md](backend/docs/CASHIER_SPEC.md): Detailed specification of the cashier screen, table status board, round void/cancel logic, and payment settlement.
- [backend/docs/PLAN.md](backend/docs/PLAN.md): Complete phase-by-phase development roadmap and dependency graph.
- [backend/docs/CLAUDE.md](backend/docs/CLAUDE.md): Backend coding standards, package structures, and convention guide.

---

## 📄 License

This project is proprietary and intended for restaurant operations. All rights reserved.
