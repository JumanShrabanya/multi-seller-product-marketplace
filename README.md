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
- The React app uses Vite, TypeScript, and React Query. UI styling will stay deliberately lightweight unless a later module needs more.

## Assumptions

- Local development uses the non-secret PostgreSQL credentials in `docker-compose.yml`.
- The backend accepts browser requests from `http://localhost:5173`; override `CORS_ALLOWED_ORIGIN` if needed.
- Java 21 is intentional: it is the project baseline specified for the take-home.
- MOQ must not exceed stock when stock is positive. For zero stock, a retained MOQ is allowed because the listing is not buyable either way.

## Not implemented

Domain tables, APIs, authentication mock, and marketplace screens are introduced in later modules.

## Future improvements

Containerize the backend/frontend after their application behavior is complete.

## Scale reconsiderations

For 1M products, 100k sellers, and 10M listings: use keyset pagination, denormalized or materialized offer summaries, a dedicated search engine such as OpenSearch, read replicas and caching for buyer reads, table partitioning where appropriate, and regular index review.
