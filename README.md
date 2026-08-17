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

## 2. Create the `.env` file

The app reads its configuration from a `.env` file in the root directory (`backend/.env`) via Spring's `spring.config.import`. This file is **git-ignored**, so you must create it yourself — it is never committed.

Create a `.env` file in the root directory and add the required environment variables:

```properties
# Database
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/DB_NAME
SPRING_DATASOURCE_USERNAME=POSTGRES_USER
SPRING_DATASOURCE_PASSWORD=POSTGRES_PASSWORD

# JWT
SECURITY_JWT_SECRET_KEY=YOUR_LONG_RANDOM_SECRET
SECURITY_JWT_EXPIRATION_TIME=3600000
SECURITY_JWT_REFRESH_EXPIRATION_TIME=604800000
# Optional, defaults to 15
SECURITY_VERIFICATION_EXPIRATION_MINUTES=15

# OAuth2 — 42
OAUTH_42_CLIENT_ID=YOUR_42_CLIENT_ID
OAUTH_42_CLIENT_SECRET=YOUR_42_CLIENT_SECRET
OAUTH_42_REDIRECT_URI=http://localhost:8080/api/auth/42/callback
# Optional, default to 42's public endpoints
OAUTH_42_AUTHORIZE_URL=https://api.intra.42.fr/oauth/authorize
OAUTH_42_TOKEN_URL=https://api.intra.42.fr/oauth/token
OAUTH_42_ME_URL=https://api.intra.42.fr/v2/me

# OAuth2 — Google
OAUTH_GOOGLE_CLIENT_ID=YOUR_GOOGLE_CLIENT_ID
OAUTH_GOOGLE_CLIENT_SECRET=YOUR_GOOGLE_CLIENT_SECRET
OAUTH_GOOGLE_REDIRECT_URI=http://localhost:8080/api/auth/google/callback
# Optional, default to Google's public endpoints
OAUTH_GOOGLE_AUTHORIZE_URL=https://accounts.google.com/o/oauth2/v2/auth
OAUTH_GOOGLE_TOKEN_URL=https://oauth2.googleapis.com/token
OAUTH_GOOGLE_USERINFO_URL=https://www.googleapis.com/oauth2/v3/userinfo

# Email (used for verification codes, password reset, etc.)
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=YOUR_EMAIL_ADDRESS
MAIL_PASSWORD=YOUR_EMAIL_APP_PASSWORD

# Storage
IMG_STORAGE_PATH=./data/images
```

Replace the placeholders with your own values:

- `DB_NAME` — the database you created in step 1 (e.g. `filmexa_db`)
- `POSTGRES_USER` / `POSTGRES_PASSWORD` — your PostgreSQL credentials
- `SECURITY_JWT_SECRET_KEY` — a long random string used to sign JWTs (keep this secret)
- `SECURITY_JWT_EXPIRATION_TIME` / `SECURITY_JWT_REFRESH_EXPIRATION_TIME` — token lifetimes in milliseconds
- `OAUTH_42_CLIENT_ID` / `OAUTH_42_CLIENT_SECRET` — from your app registered at https://profile.intra.42.fr/oauth/applications, with `OAUTH_42_REDIRECT_URI` set to match exactly
- `OAUTH_GOOGLE_CLIENT_ID` / `OAUTH_GOOGLE_CLIENT_SECRET` — from an OAuth 2.0 Client ID created at https://console.cloud.google.com/apis/credentials, with `OAUTH_GOOGLE_REDIRECT_URI` added as an authorized redirect URI there (must match exactly, no trailing whitespace)
- `MAIL_USERNAME` / `MAIL_PASSWORD` — SMTP credentials used to send verification/reset emails (for Gmail, use an [app password](https://myaccount.google.com/apppasswords), not your regular password)
- `IMG_STORAGE_PATH` — local folder where uploaded avatars are stored (created automatically if missing)

The `OAUTH_42_*_URL` and `OAUTH_GOOGLE_*_URL` variables are optional — they already default to each provider's real endpoints in `application.properties`, so you only need to set the client ID/secret/redirect URI to get OAuth login working.

## 3. Run the project

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

## 4. Access the API

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
