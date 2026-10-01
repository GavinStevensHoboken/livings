# Livings API

Spring Boot backend for **Livings**, an iOS app that helps international students find housing in the
United States. It covers **private rentals**, where a student leases from a landlord, and **roommate
rentals**, where a student takes a room in a shared home or teams up with another student to rent together.

**Service area:** the 48 contiguous states and Washington, DC. Alaska, Hawaii, Puerto Rico and the other
US territories are rejected anywhere the API accepts an address or a state. The state code, the ZIP prefix
and the coordinates are all checked (see `geo/ServiceArea.java`).

## Tech stack

| | |
|---|---|
| Runtime | Java 21, Spring Boot 3.5 |
| API | Spring Web (REST/JSON), Bean Validation, RFC 9457 `problem+json` errors |
| Auth | Stateless JWT (Bearer) via Spring Security, BCrypt password hashing |
| Data | Spring Data JPA / Hibernate, Flyway migrations, PostgreSQL (prod), H2 (dev & tests) |
| Docs | OpenAPI 3 + Swagger UI (springdoc) |

## Run it

```bash
# Dev: no setup needed, uses a file-based H2 database in ./data
./mvnw spring-boot:run
# -> http://localhost:8080/swagger-ui.html

# Tests
./mvnw test

# Production-like: API + PostgreSQL 16
docker compose up --build
```

### Configuration (prod profile)

| Env var | Purpose |
|---|---|
| `SPRING_PROFILES_ACTIVE=prod` | Use PostgreSQL settings (set by the Dockerfile) |
| `DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD` | JDBC connection |
| `LIVINGS_JWT_SECRET` | **Required in prod.** Base64 HMAC key, ≥ 256 bits (`openssl rand -base64 48`) |
| `LIVINGS_JWT_TTL` | Token lifetime, ISO-8601 duration (default `PT24H`) |
| `LIVINGS_SWAGGER_ENABLED` | Expose Swagger UI / OpenAPI in prod (default `false`) |
| `PORT` | HTTP port (default `8080`) |

## Domain

- **User**: either `STUDENT` or `LANDLORD`. Nobody can register as an admin. A profile holds the home
  country, university and languages. Accounts with a `.edu` email get a flag that serves as a simple sign
  that the user is a student.
- **Listing**: has a category (`PRIVATE_RENTAL` or `ROOMMATE`), a room type, a property type,
  rent/deposit/utilities, lease terms, a gender preference, photos and amenities. It also has flags that
  matter to international students: `noSsnRequired`, `noCreditHistoryRequired`,
  `internationalGuarantorAccepted` and `virtualTourAvailable`. When a listing includes coordinates, it is
  linked automatically to the nearest university within 30 miles.
- **Roommate profile**: a "looking for a roommate" card with a budget range, preferred city/state,
  university, move-in date and lifestyle details (smoker, pets, sleep schedule, cleanliness). Search
  honors each profile's gender preference toward the person searching, and never shows users their own
  profile.
- **Favorites**: saved listings.
- **Conversations**: two-person threads with unread counts. A thread is either about a listing or a direct
  chat with a roommate match.
- **University**: seeded with 42 schools that enroll many international students (`V2__seed_universities.sql`).

## API overview (`/api/v1`)

Public endpoints don't need a token. All other endpoints need `Authorization: Bearer <accessToken>`.

| Method & path | Auth | Description |
|---|---|---|
| `POST /auth/register` | public | Create account → `{accessToken, expiresAt, user}` |
| `POST /auth/login` | public | Log in → same shape |
| `GET /users/me` · `PATCH /users/me` | ✔ | Own profile (partial update) |
| `GET /users/{id}` | public | Public profile (no email/phone) |
| `GET /states` | public | Supported states for pickers |
| `GET /universities?state=&q=` · `GET /universities/{id}` | public | University lookup |
| `GET /listings` | public | Search (filters below), paginated |
| `GET /listings/{id}` | public | Listing detail |
| `GET /listings/mine` | ✔ | My listings |
| `POST /listings` · `PUT /listings/{id}` | ✔ | Create / replace a listing |
| `PATCH /listings/{id}/status` | ✔ | `ACTIVE`, `PAUSED`, `RENTED` |
| `DELETE /listings/{id}` | ✔ | Archive (soft delete) |
| `GET /roommates` · `GET /roommates/{id}` | ✔ | Roommate search / detail |
| `GET /roommates/me` · `PUT /roommates/me` · `DELETE /roommates/me` | ✔ | Manage my roommate profile |
| `GET /favorites` · `PUT /favorites/{listingId}` · `DELETE /favorites/{listingId}` | ✔ | Saved listings |
| `POST /conversations` | ✔ | `{listingId \| recipientId, message}`: start a new thread or add to an existing one |
| `GET /conversations` | ✔ | Inbox with last message + unread count |
| `GET /conversations/{id}/messages` · `POST /conversations/{id}/messages` | ✔ | Read (marks read) / send |

### Listing search filters

`q, state, city, zipCode, category, roomType, propertyType, genderPreference, minRent, maxRent,
minBedrooms, minBathrooms, availableBy, maxLeaseMonths, furnished, petsAllowed, utilitiesIncluded,
noSsnRequired, noCreditHistoryRequired, internationalGuarantorAccepted, virtualTourAvailable,
amenities (repeatable, all must match)`

For a radius search, pass `universityId` **or** `latitude`+`longitude`, plus `radiusMiles` (default 10,
max 100). Results then include `distanceMiles`.

Paging: `page` (0-based), `size` (max 50), `sort=monthlyRent,asc` (sortable: `createdAt`, `monthlyRent`,
`availableFrom`, `bedrooms`). Every paged response is
`{content, page, size, totalElements, totalPages, last}`.

```bash
curl "localhost:8080/api/v1/listings?universityId=5&radiusMiles=3&maxRent=1600&noSsnRequired=true&sort=monthlyRent,asc"
```

## Notes for the iOS client

- Dates use ISO-8601. A `LocalDate` looks like `"2027-01-15"` and a timestamp like
  `"2026-10-01T12:00:00Z"`. Decode timestamps with `JSONDecoder.dateDecodingStrategy = .iso8601` and dates
  with a `yyyy-MM-dd` formatter.
- Responses leave out null fields, so model those fields as Swift optionals.
- Errors are always `application/problem+json`: `{type, title, status, detail, instance}`. Validation
  failures also include `errors: {field: message}`. A `401` means the token is missing or expired, and the
  user needs to log in again.
- Store the access token in the Keychain.
- You can generate a Swift client from `/v3/api-docs`, for example with Apple's `swift-openapi-generator`.

## Project layout

```
src/main/java/com/livings
├── config/        Security (JWT filter chain), OpenAPI
├── security/      JWT issue/parse, AuthUser principal
├── common/        Errors, paging, auditing base class
├── geo/           UsState (service area), ServiceArea validation, distance math
├── user/          Accounts, auth, profiles
├── university/    University reference data
├── listing/       Listings, search specifications
├── roommate/      Roommate profiles & matching
├── favorite/      Saved listings
└── messaging/     Conversations & messages
src/main/resources/db/migration   Flyway SQL (schema + university seed)
```

## Suggested next steps

- Photo upload through S3/GCS pre-signed URLs. Listings currently store photo URLs only.
- APNs push notifications for new messages (the hook point is marked in `MessagingService.append`),
  refresh tokens, and Sign in with Apple.
- Address geocoding (for example Apple Maps or Mapbox) so landlords don't have to send coordinates.
- Student verification (.edu email confirmation or I-20 upload), reporting and moderation, and an admin API.
- PostGIS for geo queries at scale.
