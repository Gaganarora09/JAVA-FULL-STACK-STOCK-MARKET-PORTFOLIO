# PortfolioPro Angular frontend

Responsive Angular 21 dashboard for the simulated PortfolioPro backend.

## Run locally

1. Start the Spring Boot backend from `../portfoliopro-backend` (it uses port 8081).
2. From this directory, run `npm start`.
3. Open `http://localhost:4200` and register an account or sign in.

The Angular dev server proxies `/api` requests to `http://localhost:8081` through `proxy.conf.json`.
Change that target if you run the backend on another port.

The UI stores the short-lived bearer token in `sessionStorage` for this educational client. Stock prices are seeded demo values, not live market data, and all orders are simulated.

The dashboard loads account performance snapshots from the API and provides 1 week, 1 month, 1 year, and all-time views. History accumulates while the backend is running and recording daily simulated price movements and portfolio snapshots.

If fewer than two real account snapshots exist, the graph shows a clearly labeled illustrative curve anchored to the current account value. It is a visual demo only and is not saved or presented as actual account history.

Run `npm run build` to produce the production bundle and `npm test` for the component tests.
