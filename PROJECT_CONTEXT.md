# PortfolioPro project context

This is the handoff note for future work on this repository. Read it before editing. Update it after major implementation or runtime changes. Do not put passwords, API keys, JWTs, or other secrets here.

## Project goal

PortfolioPro is an educational stock portfolio and paper-trading simulator for a third-year CSE project. It has an Angular frontend and a Java 21 / Spring Boot REST backend. Trades are simulated only; the app does not connect to a brokerage or place real-money orders.

Core details:

- Backend package: `com.portfoliopro.backend`
- Backend directory: `portfoliopro-backend/`
- Frontend directory: `portfolio-frontend/`
- Database name: `portfolio_pro`
- REST prefix: `/api`
- Backend port: `8081`
- Frontend port: `4200`
- Docker Compose: `compose.yaml`
- Git remote: `https://github.com/Gaganarora09/JAVA-FULL-STACK-STOCK-MARKET-PORTFOLIO.git`

## Stack and architecture

- Frontend: Angular 21 standalone component application, TypeScript, RxJS, template-driven forms.
- Current frontend shell: shared dashboard state remains in `portfolio-frontend/src/app/app.ts` and `app.html`, with routed page components defined in `app.routes.ts`.
- Backend: Java 21, Spring Boot 3.2.5, Spring Web, Spring Data JPA, Bean Validation, Spring Security.
- Auth: BCrypt password hashing, stateless JWT authentication, Angular auth interceptor using `sessionStorage`.
- Entities: User, Portfolio, Holding, Stock, Trade, WatchlistEntry, StockDailyPrice, PortfolioSnapshot.
- API responses use DTOs rather than directly exposing JPA entities.
- Default backend profile: in-memory H2, temporary and reset on restart.
- Persistent profile: PostgreSQL through `application-postgres.yml`, Flyway migration, and Docker Compose.
- Obsolete MySQL profile was removed; do not reintroduce MySQL unless explicitly required.

## Implemented product functionality

- Registration and login with JWT.
- BCrypt password hashing and protected user-specific endpoints.
- User-specific portfolios, holdings, watchlists, and trade history.
- Simulated buy/sell trades with BigDecimal monetary values.
- Weighted-average holding cost basis.
- Realized and unrealized P/L.
- Pessimistic user row locking during trade execution.
- Backend trade idempotency via optional UUID `idempotencyKey`.
- Frontend generates `crypto.randomUUID()` for every submitted trade.
- Validation for ticker, trade side, positive quantity, maximum quantity, available cash, and available holdings.
- Portfolio summary, allocation, concentration, volatility, drawdown, and recorded snapshots.
- Activity area with All/Buys/Sells filters and client-side pagination.
- Trade activity displays asset, side, shares, execution price, realized P/L, and date.
- Trade ticket shows selected price, estimated order value, available cash, owned shares, and readiness messages.
- Buy/Sell buttons are disabled client-side when cash/holding/quantity checks clearly fail; backend remains authoritative.
- Dashboard performance chart now shows only persisted backend snapshots. Synthetic illustrative curves were removed.
- Empty performance state clearly explains that history begins after persisted valuations are recorded.
- Stock catalogue search and watchlist add/remove.
- Catalogue rows can be selected to update the selected ticker's fundamentals, technical indicators, and price history.
- Existing stock technical-analysis endpoint is rendered as a daily closing-price history chart with source and dates.
- Optional Massive end-of-day integration, ticker search, ticker import, refresh status, and demo fallback.
- Seeded stocks: AAPL, MSFT, GOOGL, TSLA, JPM.
- Demo market data is explicitly labeled; fundamentals remain sample/demo values.
- Practice cash endpoint: `POST /api/users/me/demo-funds`.

## Important API groups

- Auth: `POST /api/auth/register`, `POST /api/auth/login`
- Health: `GET /api/health`
- Stocks: `/api/stocks`, `/api/stocks/search`, `/api/stocks/{ticker}`, fundamentals, technical analysis, import
- Market data: `GET /api/market-data/status`, `POST /api/market-data/refresh`
- User: `GET /api/users/me`, `POST /api/users/me/demo-funds`
- Portfolio: `GET /api/portfolio/me`, `/analytics`, `/performance`
- Trades: `POST /api/trades`, `GET /api/trades`
- Watchlist: `GET /api/watchlist`, `POST /api/watchlist/{ticker}`, `DELETE /api/watchlist/{ticker}`

## Persistence status

- `compose.yaml` now starts PostgreSQL 17 on `127.0.0.1:5432` and persists data in Docker volume `portfolio_pro_postgres`.
- Compose backend uses `SPRING_PROFILES_ACTIVE=postgres` and waits for the PostgreSQL health check.
- `application-postgres.yml` enables Flyway, validates the schema with Hibernate `ddl-auto: validate`, uses UTC Hibernate JDBC timezone, validates migrations, and disables Flyway clean.
- Migration: `portfoliopro-backend/src/main/resources/db/migration/V1__create_portfolio_schema.sql`.
- Default H2 profile explicitly disables Flyway and uses `ddl-auto: update` for quick local/test startup.
- Docker Desktop/PostgreSQL was unavailable during the latest work. Persistent mode has not been runtime-verified here.
- Do not rely on the currently running H2 account across a backend restart; it will be erased.

## Current runtime and validation state as of 2026-10-01

- Frontend was confirmed responsive at `http://localhost:4200` with HTTP 200.
- Backend was confirmed healthy at `http://localhost:8081/api/health` with `{"status":"UP","service":"portfoliopro-backend"}`.
- The user viewed the updated dashboard and Activity section in the browser. The screen showed persisted account data, a holding, five trade rows, and All/Buys/Sells controls.
- Angular production builds passed after the latest changes with no warnings.
- Added Angular route definitions for the dashboard views (`/overview`, `/holdings`, `/watchlist`, `/activity`, `/performance`, `/stock/:ticker`), split the dashboard into routed page components, and kept the sidebar navigation working through Angular router links while preserving the existing dashboard functionality.
- Stock links from holdings and watchlist now open the stock-detail route, the dashboard breadcrumb reflects the active route, and stock route parameters are normalized to uppercase.
- Stock-detail pages preserve the ticker requested by the route while dashboard data refreshes, including tickers that are not yet in the user's catalogue.
- Stock-detail analysis clears stale metrics while loading and shows explicit loading/error status when fundamentals or technical history cannot be fetched.
- Stock-detail analysis errors can be retried inline, and starting a new analysis request clears stale global errors.
- Technical-history and performance charts now use responsive SVG sizing, constrained endpoint labels, and visible single-snapshot points without inventing history.
- Backend trade integration coverage now also verifies persisted cash, market value, total account value, unrealized P/L, quantity, and average cost after a buy/sell sequence.
- Frontend coverage now verifies persisted trade activity filtering and client-side pagination.
- `git diff --check` passed during the latest work.
- Backend Maven tests were not run because `mvn` and `mvnw.cmd` are unavailable in the environment.
- Docker/PostgreSQL integration was not run because Docker is unavailable.
- The worktree contains uncommitted changes from the persistence, trading, frontend dashboard, migration, and documentation work. Check `git status` before editing; do not discard unrelated changes.

## Remaining gaps / next recommended work

1. Refactor routed pages away from inheriting the full `App` shell into cleaner shared state/services when the feature set stabilizes.
2. Expand the stock-detail/research page with additional exchange/company metadata where supported.
3. Add backend unit tests for TradeService, cost basis, realized P/L, analytics formulas, and market-data provider fallback.
4. Add frontend tests for trade readiness, idempotency payloads, activity filtering/pagination, and performance empty states.
5. Run Maven tests and PostgreSQL Compose verification once Maven/Docker are available.
6. Verify Flyway V1 against a fresh PostgreSQL database and fix any dialect/schema mismatch discovered at startup.
7. Improve trade-history API with server-side pagination if the dataset grows; current activity pagination is client-side over the returned list.
8. Consider a provider abstraction separating Massive and demo market-data implementations.
9. Add OpenAPI/Swagger if it can be introduced without unnecessary dependency complexity.
10. Commit the accumulated changes only after reviewing the complete diff and running available validation.

## First steps in the next session

1. Read this file and run `git status --short`.
2. Check ports 4200 and 8081 before restarting anything.
3. Do not restart the H2 backend if the current local account/trades need to be preserved.
4. If persistence is the next priority, start Docker Desktop and run `docker compose up --build`; verify `/api/health`, registration, trade persistence, and restart behavior.
5. Otherwise continue with routed Angular feature components and tests, preserving the existing visual design.
6. Never request or record the Massive API key in chat or this file; use the ignored local `.env` only.

## Working preferences

- Continue with concrete implementation and verification rather than repeatedly asking whether to proceed.
- Preserve working functionality and the existing visual identity.
- Explain important architectural and algorithmic decisions in straightforward language suitable for a third-year CSE student.
- Ask the user only for genuinely required external input, such as starting Docker or providing a local environment value without sharing the secret itself.
