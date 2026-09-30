# PortfolioPro

PortfolioPro is an educational stock portfolio simulator. It includes an Angular frontend, a Spring Boot REST API, and MySQL storage. All stock data and trades are simulated; no brokerage connection or live market data source is configured.

## Run the full application

Install Docker Desktop, open this repository root in PowerShell, then run:

```powershell
docker compose up --build
```

On first start, Compose builds the frontend and backend images, starts MySQL, waits for its health check, and then starts the API and web app. Open:

- Frontend: <http://localhost:4200>
- API health: <http://localhost:8081/api/health>
- MySQL: `localhost:3306`, database `portfolio_pro`

The compose file has local-only development credentials so it can start without a separate MySQL installation. To override them, copy `.env.example` to `.env`, edit the values, and restart Compose. Do not use the included defaults outside local development.

MySQL data persists in the `portfolio_pro_mysql` Docker volume when containers stop. Stop the services with `Ctrl+C` or `docker compose down`. The frontend reaches the API through its Nginx `/api` proxy, so browser CORS configuration is not needed.

## Run without Docker

Start MySQL yourself and create/use a database named `portfolio_pro`. In one PowerShell window, set `SPRING_PROFILES_ACTIVE=mysql`, `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, and a long `PORTFOLIOPRO_JWT_SECRET`, then launch `BackendApplication` in IntelliJ. The backend targets Java 21. In another PowerShell window, run `npm ci` and `npm start` from `portfolio-frontend`; the Angular dev server proxies `/api` requests to `localhost:8081`.

## Included features

- JWT registration and login
- Stock catalogue, search, sectors, sample fundamentals, and simulated technical indicators
- Portfolio holdings, simulated buy/sell trades, trade history, and watchlists
- Portfolio allocation, P/L, daily history, concentration, volatility, and drawdown metrics

Market values and analysis inputs are educational demo data, not live prices or investment advice. Performance and risk metrics need collected snapshots and are not forecasts.
