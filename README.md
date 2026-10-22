# ConfigVault

ConfigVault is a local configuration-management project. The repository
contains a Java/Spring Boot API, a React dashboard, and PostgreSQL storage.

## Run the full stack with Docker

Requirements: Docker Engine/Desktop with the Compose plugin, and OpenSSL.

From the repository root:

```sh
cp .env.example .env
```

Edit `.env` and set a strong `POSTGRES_PASSWORD`. Generate a persistent
database password and encryption key with:

```sh
openssl rand -hex 24
openssl rand -base64 32
```

Use the hex output for `POSTGRES_PASSWORD` and the base64 output for
`CONFIGVAULT_ENCRYPTION_KEY` in `.env`. Keep `.env` private and back it up
securely. Do not rotate or lose the encryption key without a migration plan:
configuration values encrypted with it will no longer be readable.

Build and start the database, backend, and frontend:

```sh
docker compose up --build
```

Open the dashboard at `http://localhost:3000`. Swagger UI is available at
`http://localhost:3000/swagger-ui/index.html`, and the proxied backend health
check is at `http://localhost:3000/actuator/health`.

Sign in with `admin` / `admin` for local development. Set
`CONFIGVAULT_ADMIN_USERNAME` and `CONFIGVAULT_ADMIN_PASSWORD` in `.env` to
choose different credentials. Change the default password before exposing
ConfigVault beyond your local machine.

PostgreSQL and the backend are only reachable inside the Compose network; only
the frontend port is published to the host. Postgres data is stored in the
`postgres-data` named volume.

Stop the stack with `Ctrl+C`, or run `docker compose down`. To also permanently
delete the local database data, run `docker compose down -v`.

> This is a local development application. The default login is intentionally
> simple; change it before exposing the service to a network.
