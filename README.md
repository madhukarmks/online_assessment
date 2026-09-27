# Online Assessment Platform

This repository contains **Phase 1: the complete Spring Boot backend** for the Online Assessment Platform. The React frontend will be added as Phase 2 in the `frontend/` directory.

## Stack

- Java 21
- Spring Boot 3.5
- Spring Security + JWT
- Spring Data JPA / Hibernate
- MySQL 8+
- BCrypt
- Bean Validation
- Swagger/OpenAPI
- Lombok

## Backend setup

```bash
cd backend
mvn clean install
mvn spring-boot:run
```

See `backend/README.md` for all API, security, database and deployment instructions.

## Database

```sql
SOURCE database/schema.sql;
```

## Environment

Copy `.env.example` into your local environment configuration. Never commit production secrets.

Important variables:

```text
DB_URL
DB_USERNAME
DB_PASSWORD
JWT_SECRET
JWT_EXPIRATION_MS
FRONTEND_URL
PORT
DDL_AUTO
SEED_SAMPLE_DATA
```

## Local admin

```text
admin@onlineassessment.local
Admin@12345
```

Use only for local development and change the password before deployment.

## Swagger

`http://localhost:8080/swagger-ui.html`

## Phase 2

The `frontend/` directory will contain the React/Vite/Tailwind application integrated with these APIs.
