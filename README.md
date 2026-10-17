# BajriX Multi-Seller Marketplace

A small full-stack marketplace where many sellers can offer the same product at
different prices, stock levels, and minimum order quantities. Buyers browse and
compare offers; sellers manage their own listings.

- **Backend:** Java 21, Spring Boot 3.5, Spring Data JPA + `JdbcTemplate`, PostgreSQL 16, Flyway, Maven.
- **Frontend:** React 19, TypeScript, Vite, TanStack Query, plain CSS.

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

   Open `http://localhost:5173`. Use the header **View as** switch to move between
   the buyer experience and the seller dashboard. In seller mode a dropdown picks
   which seller you are "logged in" as.

### Tests

```bash
cd backend/multi-seller
./mvnw test        # Windows: .\mvnw.cmd test
```

The integration and concurrency tests start a throwaway `postgres:16-alpine`
container with Testcontainers, so Docker must be running.

## Architecture

```
backend/multi-seller   Spring Boot app (Maven wrapper)
  src/main/java/...    controller -> service -> repository
  src/main/resources/db/migration   Flyway migrations (schema + seed)
frontend               Vite + React + TypeScript app
docker-compose.yml     Postgres 16 only (the apps run from source)
```

### Backend

- **Layering** is `controller -> service -> repository`. Business rules live only
  in services; controllers are thin and deal in DTOs (entities are never exposed).
- **Two data-access styles on purpose:**
  - Buyer reads are aggregate queries (lowest price + offer count per product)
    written once with `NamedParameterJdbcTemplate` and a shared SQL visibility
    predicate, which avoids N+1 lookups and keeps the SQL explicit.
  - Seller writes go through Spring Data JPA entities with `@Version`
    optimistic locking, so guarded updates follow the entity lifecycle.
- **Product vs. SellerListing.** `product` is the catalogue item; `seller_listing`
  links a seller to a product with price/stock/MOQ. `UNIQUE (seller_id, product_id)`
  makes duplicate listings impossible.
- **Stop selling is a soft delete** (`active = false`), and re-adding the same
  product reactivates the existing row instead of inserting a duplicate.
- **Search** uses PostgreSQL `pg_trgm` with a GIN index on the product name, plus
  indexes on category, brand, seller, product, and a partial index for buyable
  listings. Sort is limited to `name`/`price_asc`/`price_desc` — relevance ranking
  was deliberately left out (it needs a search engine to be worth the cost; see
  the scale notes). Offset pagination with a hard `size` cap (100) is used, and the
  scale notes explain why a production system would move to keyset pagination.
- **Errors** are returned in one shape via `@RestControllerAdvice`:
  `{ timestamp, status, code, message, fieldErrors[] }` with codes
  `BAD_REQUEST`, `VALIDATION_ERROR`, `FORBIDDEN`, `NOT_FOUND`, `CONFLICT`.
- **Mock auth:** the `X-Seller-Id` header is resolved into a `SellerContext`. Every
  `/api/seller/**` call is scoped to that seller id; the header is required and
  validated.
- **Concurrency:** `PATCH` requires the current `version`; a stale value returns
  `409 Conflict`, and the DB `@Version` column is the final guard.

### API

| Method | Path | Purpose |
| --- | --- | --- |
| GET | `/api/products` | Buyer product list: `q`, `category`, `brand`, `minPrice`, `maxPrice`, `inStockOnly`, `sort` (`name`/`price_asc`/`price_desc`, default `name`), `page`, `size`. |
| GET | `/api/products/{id}` | Product detail + buyable offers sorted by price. |
| GET | `/api/seller/listings` | The current seller's listings (includes inactive). |
| POST | `/api/seller/listings` | Add an existing product to the seller's catalogue (reactivates if previously stopped). |
| PATCH | `/api/seller/listings/{id}` | Update price/stock/MOQ (requires `version`). |
| DELETE | `/api/seller/listings/{id}` | Stop selling (soft delete). |
| GET | `/api/seller/products/search?q=` | Find catalogue products to list. |
| GET | `/api/sellers/mock` | Dev-only seller list for the frontend switcher. |

### Frontend

- **Routing** with React Router: `/` (catalogue), `/products/:id`, `/seller`.
- A typed API client wraps `fetch`, throws a rich `ApiError`, and attaches the
  `X-Seller-Id` header for seller calls.
- **Seller identity is a mock role switcher** in the header (Buyer / Seller +
  seller dropdown), persisted in `localStorage`. This stands in for real auth.
- The buyer **filters and page live in the URL**, so a filtered view is shareable
  and survives refresh; text inputs are debounced before they update the URL.
- The seller dashboard does **inline editing** with client-side validation that
  mirrors the backend, maps `400` field errors to inputs, and handles `409` by
  refreshing to the latest version with a toast. `PENDING`/`REJECTED` sellers see
  a banner and all write actions are disabled.

## Business rules

- A listing is visible to buyers only when the seller is `APPROVED`, the listing
  is `active`, and `stock >= min_order_qty`. Out-of-stock / below-MOQ listings are
  hidden; a product with no such offers shows as **Currently unavailable**.
- `PENDING`/`REJECTED` sellers can sign in (mock) and view their listings but get
  `403` on create/update/stop.
- Accessing another seller's listing returns `404` (existence is not leaked).
- Validation: `price > 0` (max 2 decimals), `stock >= 0`, `min_order_qty >= 1`,
  and `min_order_qty <= stock` when `stock > 0`.

## Assumptions

- Local development uses the non-secret Postgres credentials in `docker-compose.yml`.
- The backend allows browser requests from `http://localhost:5173` (override with `CORS_ALLOWED_ORIGIN`).
- Sellers add **existing catalogue products** to their catalogue (create a listing);
  creating brand-new products is an admin concern outside this brief.
- MOQ may exceed stock only when stock is `0`, because the listing is not buyable
  either way.
- Category and brand are free-text filters. A "list all categories" endpoint was
  avoided deliberately, since scanning a large catalogue for facets is exactly the
  kind of query the scale notes warn about.
- `PATCH` replaces price, stock, and MOQ together (no partial field patch).
- `GET /api/sellers/mock` is a development convenience, not a production endpoint.

## Not implemented

Intentionally out of scope per the brief: payments, checkout, RFQ flows, real
authentication/authorization, email/SMS, and any mobile app. Sellers cannot create
brand-new products, only list existing catalogue items.

## Future improvements

- Containerize the backend and frontend so the whole stack runs with one
  `docker compose up`.
- Add a facets/autocomplete endpoint (or search engine) to power category/brand
  dropdowns instead of free-text inputs.
- Seed-data admin tooling, and a small product-creation flow for admins.
- Paginate seller listings with keyset pagination and add bulk edit.

## Scale reconsiderations

For roughly 1M products, 100k sellers, and 10M listings the current approach would
need to change. Offset pagination degrades (`OFFSET` scans skipped rows) — use
**keyset/seek pagination** on an indexed sort key. Aggregate offer summaries would
be **precomputed or denormalized** (updated on write) or served from a materialized
view, instead of computed per request. Product search would move from `pg_trgm` on
the primary to a dedicated engine (**OpenSearch/Elasticsearch**) for relevance,
facets, and typo tolerance. Buyer reads would use **read replicas and caching**
(HTTP/CDN or Redis). Large tables (`seller_listing`) would be **partitioned**, and
indexes reviewed against real query plans. The mock `X-Seller-Id` auth would be
replaced with real identity and row-level authorization.
