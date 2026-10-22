# ConfigVault Backend

Java 21 and Spring Boot backend for the ConfigVault project. The service is a
separate Maven project so it can evolve independently from other repository
components.

## Requirements

- JDK 21 or newer
- Maven 3.9 or newer
- PostgreSQL 16

## Run locally

Create a PostgreSQL database named `configvault`, then start the service:

```sh
export DATABASE_URL=jdbc:postgresql://localhost:5432/configvault
export DATABASE_USERNAME=configvault
export DATABASE_PASSWORD='your-local-database-password'
mvn spring-boot:run
```

Flyway applies the schema on startup. The application validates the schema
against Hibernate entities (`ddl-auto: validate`); it does not create or update
tables through Hibernate. The health endpoint is available at
`http://localhost:8080/actuator/health`.
Interactive API documentation is available at
`http://localhost:8080/swagger-ui/index.html`, with the OpenAPI JSON at
`http://localhost:8080/v3/api-docs`.

## Project, service, and environment API

The initial CRUD endpoints are available at `/api`:

| Method | Path | Purpose |
| --- | --- | --- |
| `POST` | `/api/projects` | Create a project |
| `GET` | `/api/projects` | List projects |
| `GET` | `/api/projects/{projectId}` | Get one project |
| `PUT` | `/api/projects/{projectId}` | Update a project |
| `DELETE` | `/api/projects/{projectId}` | Delete a project |
| `POST` | `/api/projects/{projectId}/services` | Create a service in a project |
| `GET` | `/api/projects/{projectId}/services` | List a project's services |
| `GET` | `/api/projects/{projectId}/services/{serviceId}` | Get one service |
| `PUT` | `/api/projects/{projectId}/services/{serviceId}` | Update a service |
| `DELETE` | `/api/projects/{projectId}/services/{serviceId}` | Delete a service |
| `POST` | `/api/projects/{projectId}/environments` | Create an environment |
| `GET` | `/api/projects/{projectId}/environments` | List a project's environments |
| `GET` | `/api/projects/{projectId}/environments/{environmentId}` | Get one environment |
| `PUT` | `/api/projects/{projectId}/environments/{environmentId}` | Update an environment |
| `DELETE` | `/api/projects/{projectId}/environments/{environmentId}` | Delete an environment |
| `POST` | `/api/projects/{projectId}/environments/{environmentId}/services` | Assign a service to an environment |
| `GET` | `/api/projects/{projectId}/environments/{environmentId}/services` | List services assigned to an environment |
| `DELETE` | `/api/projects/{projectId}/environments/{environmentId}/services/{serviceId}` | Remove a service assignment |

For example, create a project, a service, an environment, and assign the service:

```sh
curl -i -X POST http://localhost:8080/api/projects \
  -H 'Content-Type: application/json' \
  -d '{"name":"commerce","description":"Commerce application"}'

curl -i -X POST http://localhost:8080/api/projects/PROJECT_UUID/services \
  -H 'Content-Type: application/json' \
  -d '{"name":"payments","description":"Payment processing"}'

curl -i -X POST http://localhost:8080/api/projects/PROJECT_UUID/environments \
  -H 'Content-Type: application/json' \
  -d '{"name":"production"}'

curl -i -X POST http://localhost:8080/api/projects/PROJECT_UUID/environments/ENVIRONMENT_UUID/services \
  -H 'Content-Type: application/json' \
  -d '{"serviceId":"SERVICE_UUID"}'
```

Creation responds with `201 Created` and a `Location` header. Invalid input
returns `400`, missing resources return `404`, and uniqueness conflicts return
`409`. Deleting a project also removes its dependent environments and config
data and services through the database's cascading foreign keys. A service can
only be assigned to an environment in the same project. Config entries and API
tokens now support service/environment scoping in the database; their API
workflows are not implemented yet.

These endpoints currently have no authentication or authorization and are
intended only for local development. Config CRUD, encryption, token
authentication, and audit logging still need to be implemented before using the
service to manage secrets.
