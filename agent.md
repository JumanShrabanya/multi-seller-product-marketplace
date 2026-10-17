# agent.md — BajriX Multi-Seller Marketplace

Take-home project. Build a small, working, well-reasoned marketplace. The author must be able to explain every line, so keep code simple, readable, and consistent.

## Stack & Layout (monorepo)

```
/
├── backend/            # Java 21, Spring Boot 3, Maven, PostgreSQL, Flyway
├── frontend/           # React + Vite + TypeScript, Tailwind (or MUI), React Query
├── docker-compose.yml  # postgres (+ optionally backend/frontend)
├── README.md
├── AI_USAGE.md
└── agent.md
```

## Working Rules

1. **One module at a time.** Finish, run, and test a module before starting the next. Stop and summarize after each module (what was built, how to verify, what's next).
2. Don't add features beyond the brief (no payments, checkout, RFQ, real auth, email/SMS).
3. Keep it small: no speculative abstractions, no unused dependencies.
4. After each module: update README notes (decisions/assumptions)
5. Prefer boring, explainable choices over clever ones.

## Domain Model

- **Seller**: `id, name, city, status (APPROVED | PENDING | REJECTED)`
- **Product**: `id, name, brand, category, unit (e.g. "50 kg bag"), description` — the catalogue item, seller-independent.
- **SellerListing**: `id, product_id, seller_id, price (NUMERIC(12,2)), stock (int), min_order_qty (int), active (bool), version (bigint), created_at, updated_at`
  - `UNIQUE (seller_id, product_id)` → no duplicate listings.
  - CHECKs: `price > 0`, `stock >= 0`, `min_order_qty >= 1`.
  - Index on `(product_id)` and a partial index for buyable listings (`WHERE active AND stock > 0`).
  - Index on `(seller_id)`.
  - Trigram/GIN or full-text index on `product.name` for search; index on `category`, `brand`.

## Business Rules (document in README)

- Buyers only see listings where: seller `APPROVED` AND listing `active` AND `stock >= min_order_qty`. Out-of-stock listings are hidden from the offer list (product shows "Currently unavailable" if no offers).
- Seller status: `PENDING`/`REJECTED` sellers cannot appear to buyers; they can still log in (mock) and see their own listings, but cannot create/modify listings unless `APPROVED` (reject with 403).
- "Stop selling" = soft delete (`active=false`), not row deletion. Re-adding reactivates the existing row instead of creating a duplicate.
- Validation: price > 0 (max 2 decimals), stock >= 0, MOQ >= 1, MOQ should not exceed stock when stock > 0 (decide and document).
- Authorization: seller identity comes from the `X-Seller-Id` header (mock). Every seller query/update is scoped by `seller_id`; accessing another seller's listing returns 404 (don't leak existence).
- Concurrency: optimistic locking with `@Version` on `SellerListing`; client sends `version` on update; stale update → 409 Conflict with a clear message.

## API Conventions

- REST, JSON, base path `/api`. Buyer: `/api/products...`. Seller: `/api/seller/listings...`.
- DTOs for requests/responses — never expose entities.
- Bean Validation on DTOs; one `@RestControllerAdvice` returning a consistent error shape: `{ timestamp, status, code, message, fieldErrors[] }`.
- Layering: `controller → service → repository`. Business rules live in services only.
- **Pagination everywhere** for lists (page/size with a max size cap). Prefer keyset pagination for the product list if time permits; otherwise offset with a documented note.
- Avoid N+1 queries: fetch offer summaries (min price, seller count) in a single aggregated query, not per product.
- Search: `q` (name/brand), `category`, `brand`, `minPrice`/`maxPrice`, `inStockOnly`, `sort` (price asc/desc, name, relevance).

## Modules (build in this order)

### Module 1 — Foundation
- Scaffold `backend/` (Spring Boot, Web, Validation, JPA, PostgreSQL, Flyway, Lombok optional, Testcontainers) and `frontend/` (Vite React TS).
- `docker-compose.yml` with Postgres. Health endpoint. CORS for the frontend dev origin.
- **Done when:** both apps start, backend connects to DB, Flyway runs an empty baseline.

### Module 2 — Database & Seed Data
- Flyway migrations for the schema + constraints + indexes above.
- Seed migration (realistic construction products: cement, TMT steel, bricks, sand, paint, tiles, pipes, etc.) demonstrating: multiple sellers per product, different prices/stock/MOQ, sellers in all three statuses, an inactive listing, an out-of-stock listing, a product with zero buyable offers.
- **Done when:** a fresh `docker compose up` + app start yields a fully seeded DB.

### Module 3 — Backend: Buyer APIs
- `GET /api/products` (search, filters, sort, pagination; each item includes lowest price, offer count).
- `GET /api/products/{id}` (product details + list of buyable seller offers sorted by price, with seller name/city, price, stock, MOQ).
- Enforce the visibility rules in a single place (query/specification), reused by both endpoints.
- **Done when:** endpoints work against seed data; hidden sellers/listings never leak.

### Module 4 — Backend: Seller APIs
- Mock auth filter/resolver reading `X-Seller-Id`; load seller, expose status.
- `GET /api/seller/listings` (own, paginated, includes inactive)
- `POST /api/seller/listings` (add existing product to catalogue; duplicate → 409 or reactivate)
- `PATCH /api/seller/listings/{id}` (price, stock, MOQ; requires `version`)
- `DELETE /api/seller/listings/{id}` (soft stop-selling)
- `GET /api/seller/products/search?q=` (find catalogue products to list)
- `GET /api/sellers/mock` (dev-only list for the frontend seller selector)
- Optimistic locking + global error handling.
- **Done when:** a seller cannot read or modify another seller's data; invalid input and stale versions return correct errors.

### Module 5 — Backend Tests
Only meaningful tests, no coverage chasing:
- Service tests: validation rules, duplicate handling, status rules, ownership enforcement.
- Integration test (Testcontainers Postgres): buyer visibility rules, search/filter, pagination.
- Concurrency test: two concurrent updates to one listing → one succeeds, one gets 409.
- **Done when:** `mvn test` passes cleanly.

### Module 6 — Frontend: Foundation
- Routing (`/`, `/products/:id`, `/seller`), API client (typed), React Query, layout with header and a **role switcher** (Buyer / Seller + seller dropdown; selected seller id stored in state/localStorage and sent as `X-Seller-Id`).
- Shared UI: loading, empty, and error states; toasts.

### Module 7 — Frontend: Buyer Experience
- Product listing page: debounced search, category/brand/price filters, sort, pagination; URL-synced query params; product cards show lowest price + "N sellers".
- Product detail page: offers table/cards comparing sellers (price, stock, MOQ, city); highlight the best price; clear "unavailable" state.

### Module 8 — Frontend: Seller Dashboard
- Own listings table with status badges (active/inactive, low/out of stock).
- Inline edit for price/stock/MOQ with client-side validation mirroring backend rules; handle 409 stale-version (prompt to refresh) and 4xx field errors.
- "Add product" flow (search catalogue → set price/stock/MOQ), "Stop selling" with confirmation.
- Banner for `PENDING`/`REJECTED` sellers explaining restrictions; disable write actions.

### Module 9 — Polish & Docs
- Fix UX rough edges (responsive layout, a11y basics, consistent errors).
- `README.md` with the six required sections: how to run, architecture decisions, assumptions, not implemented, future improvements, **scale reconsiderations** (1M products / 100k sellers / 10M listings: keyset pagination, precomputed/denormalized offer summaries or materialized view, dedicated search engine like OpenSearch/Elasticsearch, read replicas, caching, partitioning, index review).
- `AI_USAGE.md`: tools used, what for, and one concrete example of an AI suggestion that was changed/rejected (fill in honestly with the real example from the build).

## Definition of Done (overall)
- `docker compose up` + one command each for backend/frontend runs everything.
- Seed data demonstrates all required scenarios.
- All tests pass; no secrets or dead code committed.
- README and AI_USAGE.md complete and truthful.
