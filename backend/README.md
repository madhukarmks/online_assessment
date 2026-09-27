# Online Assessment Platform — Backend

Spring Boot 3.5 / Java 21 backend for the Online Assessment Platform.

## Features

- JWT authentication with BCrypt
- ADMIN and STUDENT roles
- Student registration/login/profile/password change
- Admin dashboard and student management
- Assessment CRUD and publish/unpublish
- MCQ single, MCQ multiple and True/False questions
- Server-authoritative timed attempts
- Resume active attempt after browser refresh
- Server-side answer validation
- Mark for review
- Server-side evaluation and negative marking
- Result and question-wise analysis
- Student history/dashboard analytics
- Admin result filtering and detailed results
- Assessment leaderboard
- CORS configuration
- Validation and global error handling
- Swagger/OpenAPI
- MySQL 8+
- Optional sample Java Fundamentals assessment

## Requirements

- Java 21
- Maven 3.9+
- MySQL 8+

## Database

Create the database using:

```sql
SOURCE ../database/schema.sql;
```

Or use the default JDBC URL, which includes `createDatabaseIfNotExist=true`.

## Environment variables

```text
DB_URL=jdbc:mysql://localhost:3306/online_assessment?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
DB_USERNAME=root
DB_PASSWORD=your-password
JWT_SECRET=at-least-32-characters-random-secret
JWT_EXPIRATION_MS=86400000
FRONTEND_URL=http://localhost:5173
PORT=8080
DDL_AUTO=update
SEED_SAMPLE_DATA=true
```

## Run

From `backend/`:

```bash
mvn clean install
mvn spring-boot:run
```

Or:

```bash
mvn clean package
java -jar target/online-assessment-backend-1.0.0.jar
```

## Profiles

Development:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

Production:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=prod
```

Production uses `DDL_AUTO=validate` and disables sample data.

## Default local admin

The development profile seeds the admin from environment variables:

```text
ADMIN_EMAIL=admin@onlineassessment.local
ADMIN_PASSWORD=Admin@12345
SEED_ADMIN=true
```

The password is BCrypt-hashed. Production disables admin/sample seeding by default; configure an explicit secure admin bootstrap process before enabling it in any shared environment.

## Sample data

Set:

```text
SEED_SAMPLE_DATA=true
```

The application creates a published `Java Fundamentals` assessment with 8 questions covering OOP, collections, inheritance, exceptions, multithreading and Java 8 functional interfaces.

## Swagger

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

Use the `Authorization` header as:

```text
Bearer <JWT>
```

## Main API groups

### Authentication

```text
POST /api/auth/register
POST /api/auth/login
GET  /api/auth/me
PUT  /api/auth/me
PUT  /api/auth/change-password
POST /api/auth/logout
```

### Admin

```text
GET    /api/admin/dashboard
GET    /api/admin/students?search=
PUT    /api/admin/students/{id}/active?value=true
GET    /api/admin/assessments
POST   /api/admin/assessments
GET    /api/admin/assessments/{id}
PUT    /api/admin/assessments/{id}
DELETE /api/admin/assessments/{id}
GET    /api/admin/assessments/{id}/questions
POST   /api/admin/assessments/{id}/questions
PUT    /api/admin/questions/{id}
DELETE /api/admin/questions/{id}
GET    /api/admin/results
GET    /api/admin/results/{id}
```

### Student

```text
GET  /api/student/dashboard
GET  /api/student/assessments
GET  /api/student/assessments/{id}
POST /api/student/assessments/{id}/start
GET  /api/student/attempts/history
GET  /api/student/attempts/{id}
POST /api/student/attempts/{id}/answers
POST /api/student/attempts/{id}/submit
GET  /api/student/results
GET  /api/student/results/{id}
```

### Leaderboard

```text
GET /api/assessments/{id}/leaderboard
```

## Security/business rules

- Students cannot access `/api/admin/**`.
- Admins cannot access `/api/student/**`.
- Students can only access their own attempts/results.
- Correct options are never returned in active assessment payloads.
- Scores are calculated only by the backend.
- Answer option IDs are validated against the current question.
- Submitted attempts cannot be changed.
- Late answers/submissions are rejected.
- Attempt end time is calculated by the server and capped by the assessment availability end time.
- Starting an assessment while an active attempt already exists resumes that attempt.
- Attempt limits are enforced server-side.

## Docker

Build:

```bash
docker build -t online-assessment-backend ./backend
```

Run with environment variables for the database, JWT and frontend origin.

## New features in upd_ph1
- Fixed repeated admin/student 500s caused by lazy JPA relationships being accessed after repository transactions. Read operations that map related entities now run inside read-only transactions.
- Camera + microphone permission gate on assessment attempts. Production deployments must use HTTPS (localhost is also allowed by browsers).
- Coding questions with starter code, enabled languages, hidden test cases, and compiler execution.
- Compiler endpoint uses configurable Judge0 CE. Set `JUDGE0_URL` and optional `JUDGE0_API_KEY` in the backend environment.
- Supported starter language IDs: C 50, C++ 54, Java 62, Python 3 71, JavaScript 63.
- Admin can create/edit/delete coding questions from the existing Question Management page and select enabled languages.
