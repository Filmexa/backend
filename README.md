# Filmexa Backend

## Requirements

- **Java 17** (JDK)
- **Maven** (or use the bundled `./mvnw` wrapper — no install needed)
- **PostgreSQL** (running locally on port `5432`)

## 1. Create the database first

The app connects to a PostgreSQL database named `filmexa_db` using the `postgres` user. Create it **before** running the project — the app creates/updates tables automatically (`spring.jpa.hibernate.ddl-auto=update`), but it does **not** create the database itself.

Using `psql`:

```bash
# Connect as the postgres user
psql -U postgres

# Then, inside the psql prompt:
CREATE DATABASE filmexa_db;
\q
```

Or as a one-liner:

```bash
createdb -U postgres filmexa_db
```

The default connection settings (in `src/main/resources/application.properties`) are:

| Property   | Value                                          |
| ---------- | ---------------------------------------------- |
| URL        | `jdbc:postgresql://localhost:5432/filmexa_db`  |
| Username   | `postgres`                                     |
| Password   | `postgres`                                      |

If your PostgreSQL credentials differ, update these values in `application.properties`.

## 2. Run the project

From the `backend/` directory:

```bash
# Build and start the application
./mvnw spring-boot:run
```

The server starts on **http://localhost:8080** (override with the `PORT` environment variable).

### Other useful commands

```bash
./mvnw clean package     # Build the WAR into target/
./mvnw test              # Run the tests
```

## 3. Access the API

The app uses **HTTP Basic authentication**. The default in-memory user is:

- **Username:** `admin`
- **Password:** `admin123`

(Configurable via `spring.security.user.name` / `spring.security.user.password`.)

### Swagger UI

API docs are public (no auth required):

- Swagger UI: http://localhost:8080/swagger-ui.html

### Example endpoint

All `/api/**` routes require authentication:

## Project structure

```
src/main/java/com/filmexa/stream/
├── config/         # Security & OpenAPI configuration
├── controllers/    # REST controllers
├── dto/            # Data transfer objects
├── entities/       # JPA entities
├── enums/          # Enums (e.g. Role)
├── repo/           # Spring Data repositories
└── services/       # Business logic
```
