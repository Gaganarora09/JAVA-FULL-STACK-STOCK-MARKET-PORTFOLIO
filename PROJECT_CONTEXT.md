# PortfolioPro project context

This file is the handoff note for future work on this repository. Update it when major project decisions or run-state changes occur. Do not put passwords, API keys, JWTs, or other secrets here.

## Project goal

PortfolioPro is an educational stock portfolio and trading simulator. It has an Angular frontend and a Java 21 / Spring Boot REST backend, with MySQL as the intended persistent database. Trades are simulated; the app does not connect to a brokerage or place real-money orders.

Core project details:

- Backend package: `com.portfoliopro.backend`
- Database: `portfolio_pro`
- REST API prefix: `/api`
- Health endpoint: `GET /api/health`
- Backend port: `8081`
- Frontend port: `4200`
- Backend: `portfoliopro-backend/`
- Frontend: `portfolio-frontend/`
- Docker Compose file: `compose.yaml`
- Git remote: `https://github.com/Gaganarora09/JAVA-FULL-STACK-STOCK-MARKET-PORTFOLIO.git`

## Product features already in the project

- User registration and login with JWT authentication.
- Stock catalogue, search, sectors, fundamentals, technical indicators, and price history.
- Simulated buy/sell trades, holdings, cash balance, trade history, and watchlists.
- Portfolio analytics, allocation, P/L, risk metrics, and performance chart with week/month/year/all controls.
- When account history is too short, the performance chart can show explicitly labeled illustrative curves. These are not real past account results.
- Dashboard labels demo prices as simulated. Do not present them as live data.
- MySQL profile and Docker Compose setup exist. The default local backend profile is in-memory H2.

## Real market data work

The app has an optional Massive end-of-day data integration. When configured, the backend fetches up to one year of daily bars for the five seeded stocks (AAPL, MSFT, GOOGL, TSLA, JPM), stores closes in the existing stock daily price table, updates the stock's current price, and labels its source. It excludes the still-forming current-day bar. A scheduled refresh is configured on weekdays after the US market close. Prices are end-of-day, not real-time. Fundamentals remain sample data. Trades remain simulated.

Relevant endpoints:

- Public status: `GET /api/market-data/status`
- Authenticated manual update: `POST /api/market-data/refresh`
- Authenticated simulated cash top-up: `POST /api/users/me/demo-funds` (adds $100,000 in practice cash)

To enable Massive, copy `.env.example` to `.env`, set `MASSIVE_API_KEY` in `.env`, and restart the backend/Compose services. Never put the key in source control or in this context file. Without the key, demo prices continue to be used.

## Current state as of 2026-09-30

- The user reported they could not buy a stock. Their screenshot showed a `$0.00` balance and no positions. A $0 cash balance causes the simulated buy endpoint to reject a purchase.
- The user approved restarting the backend, including the acknowledged reset of its in-memory H2 database. The old account/trade data in that H2 instance was therefore cleared. The user must register a new account in the app; new accounts are initialized with $100,000 practice cash.
- Added an “Add $100,000 practice cash” button when cash is below $1,000, backed by `POST /api/users/me/demo-funds`.
- Added clearer messaging for insufficient funds and a frontend path back to login when the old in-memory account no longer exists.
- Rebuilt the backend jar and started it as a hidden Java 21 process on port 8081 (PID was 23672 at the time of restart; confirm before managing the process).
- Backend health responded `UP`; `/api/market-data/status` responded successfully and said `configured: false` because no provider key is configured.
- `GET /api/stocks` returned the five seeded symbols with `priceType: SIMULATED_DEMO` and `currentPrice` fields.
- Angular dev server on port 4200 responded HTTP 200. The user should refresh the browser and register a new account before testing a buy.
- Backend Java compilation/package and Angular production builds succeeded. Tests were not run.
- Docker Desktop/MySQL was unavailable during earlier work; current running backend uses H2 memory storage. Restarting it again clears its account data. Switch to persistent MySQL before relying on account data across restarts.
- Recent code changes are not committed/pushed yet. Earlier project work had been pushed to GitHub in commit `9918c82ae8fe3b9b44a11f37e3e264bf13ef70e4`; check Git status before making a new commit.
- Docker Desktop/MySQL remains unavailable; ports 8081 and 4200 are running, while MySQL port 3306 is not. The existing Compose setup still persists MySQL data in `portfolio_pro_mysql` once Docker is available.
- Massive key was provided by the user and saved only in ignored local `.env`. The live backend reports configured=true. Public search for `NVDA` returned Nvidia Corp from Massive, filtered to common stocks. The authenticated manual price refresh endpoint has not yet been run.
- Dynamic Massive ticker search and stock import are active in the running backend. Adding a result imports up to one year of EOD bars and makes the ticker available in the catalogue for watchlist and simulated trades. The user approved the backend restart and H2 reset; register a new account to use authenticated features. H2 remains in-memory, so another restart clears user and trade data.
- Angular production build and direct Java compilation succeeded; the staged Spring Boot jar is running. Tests were not run. The source changes and this context file were pushed to `main` in commit `dd9d39f` (follow-up context correction committed separately).

## First steps next time

1. Read this context and check `git status` before editing.
2. Check whether the frontend/backend are already running before restarting either one.
3. Ask whether a Massive API key has been added to `.env` only if live prices are the immediate goal; the key itself should not be shared in chat.
4. Have the user refresh `http://localhost:4200`, register again after the H2 reset, and test a one-share simulated buy. If the browser still shows an old view, hard-refresh it.
5. Prioritize making MySQL persistence easy to run so future backend restarts do not clear users, holdings, and trades.

## Working preferences from the conversation

- The user wants the project built through to usable, connected features and gets frustrated by stopping after small edits.
- Continue with concrete implementation and verification rather than repeatedly asking whether to proceed.
- Ask only for genuinely required external input (for example a provider API key); instruct the user to place secrets in local environment configuration instead of chat.
