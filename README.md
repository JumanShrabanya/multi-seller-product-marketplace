# BajriX Multi-Seller Marketplace

Many sellers can offer the same product at different prices, stock, and minimum
order quantities. Buyers browse and compare offers; sellers manage only their own
listings.

**Stack:** Java 21 · Spring Boot 3.5 (JPA + `JdbcTemplate`) · PostgreSQL 16 ·
Flyway · Maven — React 19 · TypeScript · Vite · TanStack Query · plain CSS.

## How to run

Prerequisites: Docker Desktop, Java 21, and Node.js 20+.

1. **Database**

   ```bash
   docker compose up -d postgres
   ```

2. **Backend** (listens on `http://localhost:8080`)

   ```bash
   cd backend/multi-seller
   ./mvnw spring-boot:run        # Windows: .\mvnw.cmd spring-boot:run
   ```

   Flyway creates the schema and seeds sample data on first start. Health check:
   `http://localhost:8080/actuator/health`.

3. **Frontend** (listens on `http://localhost:5173`)

   ```bash
   cd frontend
   npm install
   npm run dev
   ```

   Open `http://localhost:5173`, click **Sign in**, and use one of the seeded
   accounts below (buyers browse and compare; sellers manage only their own
   listings).

### Demo accounts

| Role | Email | Password |
| --- | --- | --- |
| Seller (approved) | `buildright@example.com` | `buildright123` |
| Seller (approved) | `metro@example.com` | `metro123` |
| Seller (approved) | `rajasthan@example.com` | `rajasthan123` |
| Seller (pending) | `newhorizon@example.com` | `newhorizon123` |
| Seller (rejected) | `clearance@example.com` | `clearance123` |
| Buyer | `priya@example.com` | `priya123` |
| Buyer | `amit@example.com` | `amit123` |

Signing in with a wrong password returns `401` and the login box shows an error.

### Tests

```bash
cd backend/multi-seller
./mvnw test        # Windows: .\mvnw.cmd test
```

The integration and concurrency tests start a throwaway `postgres:16-alpine`
container with Testcontainers, so Docker must be running.

## Architecture

```
backend/multi-seller   Spring Boot API (controller -> service -> repository)
frontend               React + TypeScript app
docker-compose.yml     Postgres 16 (the apps run from source)
```

- **Layering** is `controller -> service -> repository`. Business rules live in
  services; controllers are thin and expose DTOs, never entities.
- **Two data-access styles on purpose:** buyer reads are aggregate SQL
  (`NamedParameterJdbcTemplate` — lowest price + offer count in one query, with a
  shared visibility predicate, so no N+1); seller writes use JPA entities with
  `@Version`.
- **`product` vs. `seller_listing`:** the catalogue item vs. a seller's offer.
  `UNIQUE (seller_id, product_id)` prevents duplicate listings.
- **Stop selling is a soft delete** (`active = false`); re-adding the same product
  reactivates the existing row.
- **Search:** `pg_trgm` GIN index on product name plus category/brand/seller
  indexes. Sort is `name`/`price_asc`/`price_desc`; offset pagination capped at 100.
- **Login (demo-grade, no Spring Security):** `POST /api/auth/login` matches a
  seeded email/password and returns an opaque token. Seller calls send
  `Authorization: Bearer <token>`, which the server resolves to the seller id, so a
  seller can only act as themselves (cross-seller access → `404`). Sessions are
  in-memory and passwords are plain text — deliberate for a demo.
- **Errors** share one shape: `{ timestamp, status, code, message, fieldErrors[] }`
  with codes `BAD_REQUEST`, `VALIDATION_ERROR`, `UNAUTHORIZED`, `FORBIDDEN`,
  `NOT_FOUND`, `CONFLICT`.
- **Concurrency:** `PATCH` needs the current `version`; a stale value → `409`,
  backed by the DB `@Version` column.

### API

| Method | Path | Purpose |
| --- | --- | --- |
| POST | `/api/auth/login` | Log in with `{ email, password }`; returns token + identity, or `401`. |
| GET | `/api/products` | Buyer list: `q`, `category`, `brand`, `minPrice`, `maxPrice`, `inStockOnly`, `sort`, `page`, `size`. |
| GET | `/api/products/{id}` | Product detail + buyable offers sorted by price. |
| GET | `/api/seller/listings` | Current seller's listings (includes inactive). |
| POST | `/api/seller/listings` | Add an existing product (reactivates if previously stopped). |
| PATCH | `/api/seller/listings/{id}` | Update price/stock/MOQ (requires `version`). |
| DELETE | `/api/seller/listings/{id}` | Stop selling (soft delete). |
| GET | `/api/seller/products/search?q=` | Find catalogue products to list. |

Seller endpoints require the `Authorization: Bearer <token>` header.

### Frontend

- Routes: `/` (catalogue), `/products/:id`, `/login`, `/seller`.
- A typed `fetch` client throws a rich `ApiError` and attaches the Bearer token for
  seller calls.
- Login stores the account (token, role, name, seller status) in `localStorage`; the
  header shows the signed-in user with sign-out, and `/seller` redirects to `/login`
  unless a seller is signed in.
- Buyer filters and page live in the URL (shareable, survive refresh); text inputs
  are debounced.
- The seller dashboard does inline editing with client-side validation that mirrors
  the backend, maps `400` field errors to inputs, and refreshes on `409`;
  `PENDING`/`REJECTED` sellers see a banner and all writes are disabled.

## Business rules

- A listing is visible to buyers only when the seller is `APPROVED`, the listing is
  `active`, and `stock >= min_order_qty`; otherwise the product shows
  **Currently unavailable**.
- `PENDING`/`REJECTED` sellers can sign in and view their listings but get `403` on
  create/update/stop.
- Another seller's listing returns `404` (existence is not leaked).
- Validation: `price > 0` (max 2 decimals), `stock >= 0`, `min_order_qty >= 1`, and
  `min_order_qty <= stock` when `stock > 0`.

## Assumptions

- Local Postgres credentials come from `docker-compose.yml` (non-secret).
- CORS allows `http://localhost:5173` (override with `CORS_ALLOWED_ORIGIN`).
- Sellers list **existing catalogue products**; creating new products is an admin
  concern outside the brief.
- MOQ may exceed stock only when `stock` is `0` (the listing is not buyable anyway).
- Category and brand are free-text filters — no facets endpoint, on purpose.
- `PATCH` replaces price, stock, and MOQ together (no partial patch).
- Login is demo-only: seeded plain-text passwords and in-memory sessions.

## Not implemented

Per the brief: payments, checkout, RFQ, production-grade auth (Spring Security,
password hashing, persistent sessions), email/SMS, and any mobile app. Sellers
cannot create brand-new products.

## Future improvements

- One-command `docker compose up` for the whole stack.
- A facets/autocomplete endpoint for category/brand.
- Admin seed tooling and a product-creation flow.
- Keyset pagination and bulk edit for seller listings.

## Scale reconsiderations

At roughly 1M products / 100k sellers / 10M listings: offset pagination degrades, so
move to **keyset pagination**; precompute or denormalize offer summaries (or use a
materialized view) instead of aggregating per request; move search to a dedicated
engine (**OpenSearch/Elasticsearch**) for relevance and facets; add **read replicas
and caching**; **partition** `seller_listing`; review indexes against real query
plans. The in-memory session store and plain-text passwords would be replaced with
real identity (hashed credentials, persistent sessions, row-level authorization).
