# AI Usage

## Tools used

- An AI coding assistant (opencode CLI) & and Codex CLI driving the implementation.
- Framework scaffolding from the standard Spring Initializr and Vite templates.

## What I primarily used it for

- Scaffolding the monorepo: the Spring Boot project, `docker-compose.yml`,
  Flyway migrations, and the Vite + React + TypeScript app.
- Boilerplate that follows an established pattern: DTOs/records, controllers,
  repositories, the global error handler, and the React components and API client.
- Drafting tests (service unit tests, Testcontainers integration tests, and the
  concurrency test) and the first pass of this README.

Everything was reviewed and adjusted. I deliberately kept the design boring and
explainable so I can defend every line in the follow-up discussion.

## Examples where an AI suggestion was changed or rejected

1. **A JPA `Product` entity with associations was removed.** The first draft
   modelled `SellerListing` with a `@ManyToOne Product` (and a managed `Product`
   entity). I rejected that: the buyer endpoints are aggregate SQL queries
   (lowest price + offer count per product), and a managed association would add
   lazy-loading/N+1 risk and pull the entity graph into code that reads better as
   explicit SQL. I replaced it with a plain `product_id` column plus lightweight
   `ProductRef`/`ProductDetailRow` records returned from `JdbcTemplate`, keeping
   JPA only for the seller write path.

2. **Testcontainers had to be pinned.** The generated dependency used Testcontainers
   1.21.3, which negotiates Docker API version 1.32; Docker Engine 29 rejects that
   ("minimum supported API version is 1.44"). Rather than a workaround, I pinned
   `testcontainers.version` to 1.21.4, which fixes the negotiation.

3. **Lombok was rejected.** AI tooling suggested Lombok to cut boilerplate. The
   brief favours simple, explainable code and the models have little repetitive
   code, so I kept plain Java.

4. **A frontend URL-sync bug was corrected.** The AI-written filter effect always
   stripped the `page` query parameter, which would silently reset a shared
   `?page=2` link on load. I changed it to only reset the page when a filter
   actually changes.

5. **A relevance sort was removed as overkill.** The AI's plan included a `RELEVANCE`
   sort using Postgres `similarity()`. Reviewing it against the brief, the assignment
   only asks for search/filters/sorting, and `similarity()` in an `ORDER BY` can't use
   the trigram index — so the fanciest feature was the least scalable part of the
   search. I dropped it and kept `name`/`price_asc`/`price_desc`.

6. **Login keeps a server-side identity instead of a client-chosen id.** The first
   auth design was a `X-Seller-Id` header the frontend could set freely, which meant
   a logged-in seller could still send another seller's id. I replaced it with a
   `POST /api/auth/login` that checks the seeded email/password and returns an opaque
   token; the seller endpoints resolve that token to the account id, so a seller can
   only ever act as themselves (a cross-seller write is a `404`). I kept it basic on
   purpose — no Spring Security, an in-memory session store, and plain-text demo
   passwords — and documented those as demo-only rather than production auth.
