# ConfigVault Frontend

Local React dashboard for the ConfigVault API.

## Run

Start the backend on port 8080, then:

```sh
npm install
npm run dev
```

Open `http://localhost:5173`. Vite proxies API, health, and OpenAPI requests to
`http://localhost:8080`. Build the static frontend with `npm run build`.

The dashboard can create projects, environments, and services; assign services
to environments; manage config entries; inspect version metadata; and restore
older versions. Secret values remain masked. To update a secret, enter a new
replacement value; the existing secret is never loaded into the browser.

The backend is currently unauthenticated. Use this dashboard only with a local
backend and do not expose it to a network.
