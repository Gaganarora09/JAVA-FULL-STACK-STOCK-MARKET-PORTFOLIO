# PortfolioPro backend

Spring Boot REST API for an educational portfolio simulator. Orders are simulated against seeded demo prices. This project has no brokerage connection and does not execute real-money trades.

## Database profiles

The default profile uses an in-memory H2 database. Data is reset when the backend process stops.

The `mysql` profile is configured for the intended MySQL database, `portfolio_pro`. It requires a reachable MySQL server and a database user allowed to create/use that database. Configure credentials in the environment; do not commit them:

```powershell
$env:SPRING_PROFILES_ACTIVE = 'mysql'
$env:DB_URL = 'jdbc:mysql://localhost:3306/portfolio_pro?createDatabaseIfNotExist=true&serverTimezone=UTC'
$env:DB_USERNAME = 'root'
$env:DB_PASSWORD = 'your-local-database-password'
$env:PORTFOLIOPRO_JWT_SECRET = 'set-a-long-random-secret-value-here'
```

Then run the app from this directory with `mvn spring-boot:run` or launch `BackendApplication` from IntelliJ. The MySQL profile uses Hibernate `ddl-auto: update` for this learning project; use reviewed schema migrations before production deployment.

The project targets Java 21. `GET /api/health` is public and returns a small service status response.

The seeded stock prices are starting values for the simulator, not live market prices. A deterministic demo price move is recorded once each UTC day while the backend is running. No real-market data source is configured.

The public stock catalogue supports `GET /api/stocks`, `GET /api/stocks/{ticker}`, `GET /api/stocks/search?q=apple&sector=Technology`, and `GET /api/stocks/sectors`. Search terms match ticker or company name; sector matching ignores case.

`GET /api/stocks/{ticker}/fundamentals` returns sample EPS, P/E, market capitalization, and dividend yield for the seeded catalogue. These figures are illustrative demo data, not sourced financial statements or investment guidance.

`GET /api/stocks/{ticker}/technical-analysis?days=30` returns a generated 30-day simulated close-price series, its 20-day simple moving average, and 14-period RSI where enough data exists. These indicators use generated demo data and are for demonstrating the API only.

Authenticated users can retrieve their account details and simulated cash balance from `GET /api/users/me` with a Bearer token.

Authenticated users can retrieve daily portfolio valuations with `GET /api/portfolio/performance?range=week`, `month`, `year`, or `all`. The older `days` parameter is also supported and bounded to 1–365 days. A daily UTC snapshot is recorded after the simulated market update, and registration or trading refreshes that day's snapshot. The chart can only show snapshots recorded since account activity began. This history is simulated and is not a live performance record.

`GET /api/portfolio/analytics` also reports maximum drawdown and sample daily return volatility from up to 365 days of snapshots. Those fields remain `null` until enough observations exist; volatility is daily, unannualized, and descriptive of simulated history only.

## Postman smoke flow

1. Start the backend.
2. Import `postman/PortfolioPro.postman_collection.json` and `postman/PortfolioPro-local.postman_environment.json` into Postman.
3. Select the `PortfolioPro local` environment and set `password` to a test value with at least eight characters.
4. Run the collection in order. The registration request creates a fresh test user and saves its JWT for the authenticated requests. The sequence exercises the catalogue, portfolio, watchlist, simulated buy/sell, analytics, and trade history.

All requests use demo stocks and simulated trades. The environment file intentionally contains no reusable account password or token.
