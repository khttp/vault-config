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

This is only the backend foundation: the schema captures the initial project,
environment, version, token, and audit-log model. Authentication, authorization,
encryption, and API workflows still need to be implemented before this service
is suitable for handling secrets.
