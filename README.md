# PortfolioPro

PortfolioPro is an educational stock portfolio simulator. It includes an Angular frontend, a Spring Boot REST API, and MySQL storage. Trades remain simulated; optional real end-of-day prices can be fetched from Massive.

## Run the full application

Install Docker Desktop, open this repository root in PowerShell, then run:

```powershell
docker compose up --build
```

On first start, Compose builds the frontend and backend images, starts MySQL, waits for its health check, and then starts the API and web app. Open:

- Frontend: <http://localhost:4200>
- API health: <http://localhost:8081/api/health>
- MySQL: `localhost:3306`, database `portfolio_pro`

The compose file has local-only development credentials so it can start without a separate MySQL installation. To enable real end-of-day data, copy `.env.example` to `.env`, add your Massive API key as `MASSIVE_API_KEY`, and restart Compose. Without a key the app continues to use clearly labeled demo prices. The free Massive plan provides end-of-day data and currently allows five API calls per minute; the refresh button may take about a minute for the starter catalogue. Do not use included database defaults outside local development.

MySQL data persists in the `portfolio_pro_mysql` Docker volume when containers stop. Stop the services with `Ctrl+C` or `docker compose down`. The frontend reaches the API through its Nginx `/api` proxy, so browser CORS configuration is not needed.

## Run without Docker

Start MySQL yourself and create/use a database named `portfolio_pro`. In one PowerShell window, set `SPRING_PROFILES_ACTIVE=mysql`, `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, and a long `PORTFOLIOPRO_JWT_SECRET`, then launch `BackendApplication` in IntelliJ. The backend targets Java 21. In another PowerShell window, run `npm ci` and `npm start` from `portfolio-frontend`; the Angular dev server proxies `/api` requests to `localhost:8081`.

## Included features

- JWT registration and login
- Stock catalogue, search, sectors, sample fundamentals, technical indicators, and optional Massive daily market prices/history
- Search the active U.S. stock directory by ticker or company name when Massive is configured; add a result to the catalogue with its daily price history, then use it in the watchlist and simulated trades
- Portfolio holdings, simulated buy/sell trades, trade history, and watchlists
- Portfolio allocation, P/L, daily history, concentration, volatility, and drawdown metrics

Fundamentals remain illustrative sample data. With Massive configured, the app refreshes daily closing prices after the US market close and retains up to one year of daily bars for the catalogue. Data availability and display rights depend on the provider plan. Prices are not real-time. Trades remain simulated. Performance and risk metrics need collected snapshots and are not forecasts.
