# BajriX Multi-Seller Marketplace

## How to run

Prerequisites: Docker Desktop running, Java 21, and Node.js 20+.

1. Start PostgreSQL: `docker compose up -d postgres`.
2. Start the backend: `cd backend/multi-seller; .\mvnw.cmd spring-boot:run`.
3. Start the frontend: `cd frontend; npm install; npm run dev`.

The backend health check is available at `http://localhost:8080/actuator/health` and the frontend dev server runs at `http://localhost:5173`.

## Architecture decisions

- The generated Spring application remains in `backend/multi-seller`; this keeps the Maven wrapper with the backend source.
- PostgreSQL is the system database. Flyway owns schema changes, beginning with an empty baseline migration in Module 1.
- The database uses `seller`, `product`, and `seller_listing` tables. `seller_listing` is unique per seller/product, which makes stop-selling a soft state instead of a row deletion.
- PostgreSQL's `pg_trgm` extension provides the GIN index used for product-name search. Category, brand, seller, product, and buyable-listing access paths have dedicated indexes.
- Buyer product reads use `NamedParameterJdbcTemplate` with a shared SQL visibility predicate. This keeps the aggregate product-list query explicit and avoids N+1 offer-summary queries.
- Seller writes use Spring Data JPA entities with `@Version` optimistic locking, because guarded updates are easier to reason about through the entity lifecycle. Each side uses the tool that fits: JPA for writes, explicit SQL for the aggregate reads.
- Mock authentication resolves the `X-Seller-Id` header into a `SellerContext` with an MVC `HandlerMethodArgumentResolver`; every `/api/seller/**` endpoint requires and validates the header.
- Paginated endpoints share one generic `PageResponse<T>` shape (`content, page, size, totalElements, totalPages`).
- Query strings are escaped before use in `LIKE`, so a user-typed `%` or `_` is treated literally.
- Automated tests: fast unit tests for the service rules and Testcontainers-backed integration tests for the real SQL visibility rules, pagination, and optimistic locking. Testcontainers is pinned to 1.21.4 because 1.21.3 negotiates a Docker API version that Docker Engine 29 rejects (minimum 1.44).
- The React app uses Vite, TypeScript, and React Query. UI styling will stay deliberately lightweight unless a later module needs more.

## Tests

Run `cd backend/multi-seller; .\mvnw.cmd test`. The integration tests start a throwaway `postgres:16-alpine` container via Testcontainers, so Docker must be running. They cover:

- Service unit tests: pagination/price/sort validation, MOQ rules, duplicate handling, seller status rules, and ownership enforcement.
- Buyer API integration tests: seller/listing visibility, search (including literal `%`/`_`), category/brand/price filters, sorting, and pagination.
- Seller API integration tests: create, duplicate 409, stop/re-add reactivation, stale-version 409, cross-seller 404, and non-approved 403.
- A concurrency test that races two updates on one listing and asserts exactly one succeeds while the other fails with the optimistic-locking exception.

## Assumptions

- Local development uses the non-secret PostgreSQL credentials in `docker-compose.yml`.
- The backend accepts browser requests from `http://localhost:5173`; override `CORS_ALLOWED_ORIGIN` if needed.
- Java 21 is intentional: it is the project baseline specified for the take-home.
- MOQ must not exceed stock when stock is positive. For zero stock, a retained MOQ is allowed because the listing is not buyable either way.
- Buyer product pagination uses offset pagination with `page` starting at zero and a maximum `size` of 100. `minPrice` and `maxPrice` filter a product's lowest visible offer.
- Seller `PATCH /api/seller/listings/{id}` replaces price, stock, and MOQ together and requires the current `version`; a stale `version` returns 409. There is no partial field-level patch.
- `POST /api/seller/listings` re-lists a previously stopped product by reactivating the same row; posting a product that is already active returns 409.
- Seller write endpoints return 403 for `PENDING`/`REJECTED` sellers, and read/ownership checks return 404 (not 403) for another seller's listing so existence is not leaked.
- `GET /api/sellers/mock` is a development convenience for the frontend seller switcher and is not intended for production.

## Not implemented

Real authentication and the marketplace screens are introduced in later modules.

## Future improvements

Containerize the backend/frontend after their application behavior is complete.

## Scale reconsiderations

For 1M products, 100k sellers, and 10M listings: use keyset pagination, denormalized or materialized offer summaries, a dedicated search engine such as OpenSearch, read replicas and caching for buyer reads, table partitioning where appropriate, and regular index review.
