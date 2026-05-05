# Library Management System API

A production-grade **Spring Boot** REST API for managing a library — books, authors, members, loans, and fines. Built with JWT authentication, role-based access control, and full Swagger documentation.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 4.x |
| Security | Spring Security 7 + JWT (jjwt 0.12.6) |
| Persistence | Spring Data JPA + Hibernate |
| Database | H2 (dev) · MySQL · PostgreSQL |
| Migrations | Flyway |
| Documentation | SpringDoc OpenAPI (Swagger UI) |
| Build | Maven |
| Testing | JUnit 5 · Mockito · MockMvc |
| Utilities | Lombok |

---

## Features

- **Authentication** — JWT-based register/login; stateless sessions
- **Role-Based Access Control** — `ADMIN`, `LIBRARIAN`, `MEMBER`
- **Book Catalog** — full CRUD; search/filter by title, author, category, ISBN; availability tracking
- **Author Management** — create, update, delete, search authors
- **Category Management** — create, update, delete categories
- **User/Member Management** — register, update profile, activate/deactivate, assign roles
- **Loan Management** — issue books, return books, track due dates
- **Overdue Tracking** — scheduled status sync; paginated overdue query
- **Fine Calculation** — auto-calculated at return ($0.50/day, configurable); mark fine as paid
- **Inventory Tracking** — `totalCopies` / `availableCopies` per book; decrement on borrow, increment on return
- **Pagination & Sorting** — all list endpoints support `page`, `size`, `sort`
- **Global Exception Handling** — structured JSON errors with field-level validation messages
- **Swagger UI** — interactive API docs at `/swagger-ui.html`
- **Flyway Migrations** — versioned SQL schema with seed data

---

## Project Structure

```
src/main/java/com/mini_project/library/
├── config/           # OpenAPI / Swagger configuration
├── controller/       # REST controllers (Auth, Book, Author, Category, User, Loan)
├── dto/
│   ├── request/      # Validated input DTOs
│   └── response/     # Output DTOs (ApiResponse wrapper, entity responses)
├── entity/           # JPA entities
│   └── enums/        # ERole, LoanStatus
├── exception/        # Custom exceptions + GlobalExceptionHandler
├── repository/       # Spring Data JPA repositories
├── security/         # JWT provider, filter, UserDetailsService, SecurityConfig
└── service/          # Business logic (Auth, User, Book, Author, Category, Loan)

src/main/resources/
├── application.properties        # Shared config (JWT, app rules, logging)
├── application-dev.properties    # H2 + Flyway for local dev
├── application-prod.properties   # MySQL/PostgreSQL for production
└── db/migration/
    ├── V1__create_initial_schema.sql
    ├── V2__insert_default_roles.sql
    └── V3__insert_sample_data.sql

src/test/java/com/mini_project/library/
├── controller/BookControllerTest.java   # MockMvc web-layer tests
├── service/BookServiceTest.java         # Unit tests
└── service/LoanServiceTest.java         # Unit tests (fine calculation, borrow rules)
```

---

## Setup Instructions

### Prerequisites

- Java 17+
- Maven 3.8+
- (Optional) MySQL 8+ or PostgreSQL 14+ for production profile

### 1. Clone the repository

```bash
git clone https://github.com/<your-username>/library-management-system.git
cd library-management-system/library
```

### 2. Run with dev profile (H2, no external DB needed)

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

The app starts on **http://localhost:8088**.

### 3. Swagger UI

```
http://localhost:8088/swagger-ui.html
```

### 4. H2 Console (dev only)

```
http://localhost:8088/h2-console
JDBC URL : jdbc:h2:mem:librarydb
Username : sa
Password : (empty)
```

---

## Environment Variables

| Variable | Default | Description |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | `dev` | Active profile (`dev` or `prod`) |
| `JWT_SECRET` | (base64 dev key) | **Override in production** — base64-encoded secret ≥ 256 bits |
| `JWT_EXPIRATION_MS` | `86400000` | Token lifetime in ms (default: 24 hours) |
| `LOAN_DURATION_DAYS` | `14` | Days before a loan is overdue |
| `FINE_PER_DAY` | `0.50` | Fine amount per overdue day (USD) |
| `DB_HOST` | `localhost` | Database host (prod) |
| `DB_PORT` | `3306` | Database port (prod) |
| `DB_NAME` | `librarydb` | Database name (prod) |
| `DB_USERNAME` | — | Database username (prod, required) |
| `DB_PASSWORD` | — | Database password (prod, required) |

---

## Database Setup

### Dev (H2 — automatic)

No setup required. Flyway runs migrations automatically on startup.

### Production (MySQL)

```sql
CREATE DATABASE librarydb CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'libraryuser'@'%' IDENTIFIED BY 'strongpassword';
GRANT ALL PRIVILEGES ON librarydb.* TO 'libraryuser'@'%';
```

Set environment variables and run:

```bash
SPRING_PROFILES_ACTIVE=prod \
DB_USERNAME=libraryuser \
DB_PASSWORD=strongpassword \
JWT_SECRET=<your-base64-secret> \
./mvnw spring-boot:run
```

---

## Default Credentials (dev seed data)

| Username | Password | Roles |
|---|---|---|
| `admin` | `Admin@123` | ADMIN, LIBRARIAN |
| `librarian` | `Admin@123` | LIBRARIAN |

> Register new members via `POST /api/auth/register`.

---

## API Documentation

Full interactive docs at **`/swagger-ui.html`** after startup.

### Common Endpoints

#### Auth

| Method | Endpoint | Description | Auth |
|---|---|---|---|
| POST | `/api/auth/register` | Register as a member | Public |
| POST | `/api/auth/login` | Login, receive JWT | Public |

#### Books

| Method | Endpoint | Description | Roles |
|---|---|---|---|
| GET | `/api/books?title=&author=&category=&isbn=&availableOnly=` | Search books | All |
| GET | `/api/books/{id}` | Get book by ID | All |
| GET | `/api/books/isbn/{isbn}` | Get book by ISBN | All |
| POST | `/api/books` | Add a book | ADMIN, LIBRARIAN |
| PUT | `/api/books/{id}` | Update a book | ADMIN, LIBRARIAN |
| DELETE | `/api/books/{id}` | Delete a book | ADMIN, LIBRARIAN |

#### Loans

| Method | Endpoint | Description | Roles |
|---|---|---|---|
| POST | `/api/loans` | Issue a book | ADMIN, LIBRARIAN |
| PATCH | `/api/loans/{id}/return` | Return a book | ADMIN, LIBRARIAN |
| PATCH | `/api/loans/{id}/pay-fine` | Mark fine as paid | ADMIN, LIBRARIAN |
| GET | `/api/loans` | All loans (paginated) | ADMIN, LIBRARIAN |
| GET | `/api/loans/overdue` | Overdue loans | ADMIN, LIBRARIAN |
| GET | `/api/loans/user/{userId}` | Loans by user | All |
| GET | `/api/loans/status/{status}` | Filter by status | ADMIN, LIBRARIAN |
| POST | `/api/loans/sync-overdue` | Sync overdue statuses | ADMIN, LIBRARIAN |

#### Authors & Categories

| Method | Endpoint | Description | Roles |
|---|---|---|---|
| GET | `/api/authors` | List authors | All |
| GET | `/api/authors/search?name=` | Search by name | All |
| POST | `/api/authors` | Create author | ADMIN, LIBRARIAN |
| PUT | `/api/authors/{id}` | Update author | ADMIN, LIBRARIAN |
| DELETE | `/api/authors/{id}` | Delete author | ADMIN, LIBRARIAN |
| GET | `/api/categories` | List categories | All |
| POST | `/api/categories` | Create category | ADMIN, LIBRARIAN |

---

## How to Run Tests

```bash
# Run all tests
./mvnw test

# Run with test profile explicitly
./mvnw test -Dspring.profiles.active=test

# Run a specific test class
./mvnw test -Dtest=BookServiceTest
```

---

## Sample API Request

### 1. Login

```bash
curl -X POST http://localhost:8088/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"Admin@123"}'
```

### 2. Create a book (use token from step 1)

```bash
curl -X POST http://localhost:8088/api/books \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Clean Code",
    "isbn": "978-0132350884",
    "totalCopies": 3,
    "authorIds": [3],
    "categoryIds": [3]
  }'
```

### 3. Issue a book

```bash
curl -X POST http://localhost:8088/api/loans \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"bookId": 1, "userId": 3}'
```

### 4. Return a book

```bash
curl -X PATCH http://localhost:8088/api/loans/1/return \
  -H "Authorization: Bearer <token>"
```

---

## Future Improvements

- [ ] Email notifications for overdue loans
- [ ] Scheduled job to auto-sync overdue statuses daily
- [ ] Book reservation / hold queue
- [ ] PDF/CSV export for loan history
- [ ] Rate limiting and API key support
- [ ] Refresh token support
- [ ] Docker Compose setup with MySQL
- [ ] CI/CD pipeline (GitHub Actions)
- [ ] Integration test suite with Testcontainers
