# ConfigVault Backend

Java 21 and Spring Boot backend for ConfigVault, organized as a feature-oriented
modular monolith.

## Requirements

- JDK 21 or newer
- Maven 3.9 or newer
- PostgreSQL 16 or newer
- OpenSSL for generating the local encryption key

## Run locally

The recommended full-stack setup uses Docker Compose from the repository root;
see the root `README.md`. To run only the backend locally, create a PostgreSQL
database and user, then from this directory run:

```sh
export DATABASE_URL=jdbc:postgresql://localhost:5432/configvault
export DATABASE_USERNAME=configvault
export DATABASE_PASSWORD='your-local-database-password'
if [ ! -f .env ]; then
  umask 077
  printf 'CONFIGVAULT_ENCRYPTION_KEY=%s\n' "$(openssl rand -base64 32)" > .env
fi
set -a
. ./.env
set +a
mvn spring-boot:run
```

Keep `.env` private and back up the encryption key securely. Values encrypted
with this key cannot be decrypted if it is lost. Keep the same key across
restarts; the `.env` file is ignored by Git.

Flyway applies database migrations at startup. Hibernate validates the schema
but does not create or modify it (`ddl-auto: validate`). The local login
defaults to `admin` / `admin`; set `CONFIGVAULT_ADMIN_USERNAME` and
`CONFIGVAULT_ADMIN_PASSWORD` to override these credentials. Change the default
password before exposing the application beyond a trusted local machine.

Health endpoint:
`http://localhost:8080/actuator/health`. OpenAPI UI:
`http://localhost:8080/swagger-ui/index.html`; JSON:
`http://localhost:8080/v3/api-docs`.

## API

The API currently supports CRUD for projects, services, and environments,
assigning services to environments, and configuration entries scoped to an
assigned project/service/environment combination.

### Configuration endpoints

| Method | Path | Purpose |
| --- | --- | --- |
| `POST` | `/api/projects/{projectId}/environments/{environmentId}/services/{serviceId}/config` | Create an entry |
| `GET` | `/api/projects/{projectId}/environments/{environmentId}/services/{serviceId}/config` | List entries |
| `GET` | `/api/projects/{projectId}/environments/{environmentId}/services/{serviceId}/config/{key}` | Get an entry |
| `PUT` | `/api/projects/{projectId}/environments/{environmentId}/services/{serviceId}/config/{key}` | Add a new version |
| `GET` | `/api/projects/{projectId}/environments/{environmentId}/services/{serviceId}/config/{key}/versions` | List version metadata |
| `POST` | `/api/projects/{projectId}/environments/{environmentId}/services/{serviceId}/config/{key}/rollback` | Restore an older version as a new version |

Example:

```sh
curl -i -X POST \
  http://localhost:8080/api/projects/PROJECT_UUID/environments/ENVIRONMENT_UUID/services/SERVICE_UUID/config \
  -H 'Content-Type: application/json' \
  -d '{"key":"PAYMENT_API_KEY","value":"replace-me","secret":true}'
```

Values—secret and non-secret—are encrypted using AES-256-GCM before storage.
Secret values are masked as `********` in management API responses. Each update
adds an immutable version; rollback copies the selected historical value into a
new version. Audit records track writes and rollbacks without storing values.

## Security status

The management API requires a login session, and state-changing requests require
a CSRF token. The `admin` / `admin` default is only suitable for local
development; configure a strong password before any network exposure. Service
token creation/validation, service-facing config retrieval, and audited secret
reveal are not implemented yet.

## Tests

```sh
mvn test
```
