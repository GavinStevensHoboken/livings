# Livings: Spring Boot Learning Path

Keep this file open in IntelliJ while you read. Tick a box by changing `[ ]` to `[x]`.
Read the folders in this order. Each step lists the files (in reading order), what to look for,
a hands-on exercise, and questions to check yourself.

---

## Phase 0: Read and understand the existing code

### ✅ Step 1: `university/` (simplest full slice)
- [x] `University.java` → `UniversityRepository.java` → `UniversityService.java` → `UniversityController.java` → `UniversityResponse.java`
- Concepts: entity, repository, JPQL `@Query`, dependency injection, DTO, Flyway seed data
- [ ] Optional exercise: move the search logic from the controller into `UniversityService` (strict layering)

### Step 2: `listing/` (full create/read/update/delete + search)
Files, in order:
- [ ] `Listing.java`: `@ManyToOne owner`, `@ElementCollection` (photos, amenities), `extends Timestamped`
- [ ] `ListingEnums.java`: category, room type, status...
- [ ] `ListingDtos.java`: request DTO with validation (`@NotBlank`, `@DecimalMin`...) + response DTO
- [ ] `ListingController.java`: POST / PUT / PATCH / DELETE, `@Valid`, `@AuthenticationPrincipal`, `@ResponseStatus`
- [ ] `ListingService.java` ⭐: business rules, `requireOwned`, `@Transactional`
- [ ] `ListingRepository.java`: derived query `findByOwnerIdAndStatusNot`
- [ ] `ListingMapper.java`: entity → DTO
- [ ] `ListingSearchCriteria.java` + `ListingSpecifications.java`: dynamic search (hardest, read last)
- Also open: `common/Timestamped.java`, `common/PageResponse.java`, `common/Pageables.java`

Look for:
1. `update()` never calls `save()`. Why does the change still reach the database? (dirty checking)
2. Who is `me` in `@AuthenticationPrincipal AuthUser me`? (set by the JWT filter)
3. Where does a 403 come from when editing someone else's listing? (`requireOwned`)
4. Why does DELETE set `status = ARCHIVED` instead of deleting the row? (soft delete)
5. Why `Specification` here, when `university/` used `@Query`?

Exercise:
- [ ] Debug mode, breakpoints on the first line inside `ListingService.create` and `apply`
- [ ] Swagger: register a `LANDLORD` → Authorize → `POST /api/v1/listings` (sample body below)
- [ ] Break it: `"state": "HI"`, remove `"title"`, edit with another user's token
- [ ] `GET /api/v1/listings?universityId=5&radiusMiles=2&maxRent=1500` with a breakpoint in `ListingSpecifications.search`

<details><summary>Sample listing JSON</summary>

```json
{
  "title": "Sunny room near Northeastern",
  "description": "Furnished room, 5 min walk to campus.",
  "category": "ROOMMATE",
  "roomType": "PRIVATE_ROOM",
  "propertyType": "APARTMENT",
  "streetAddress": "100 Huntington Ave",
  "city": "Boston",
  "state": "MA",
  "zipCode": "02115",
  "latitude": 42.342,
  "longitude": -71.085,
  "monthlyRent": 1450,
  "availableFrom": "2027-01-15",
  "minLeaseMonths": 4,
  "bedrooms": 3,
  "bathrooms": 1.5,
  "furnished": true,
  "noSsnRequired": true,
  "amenities": ["WIFI_INCLUDED", "IN_UNIT_LAUNDRY"]
}
```
</details>

Check yourself:
- [ ] Why does `update()` work without `save()`? What breaks if `@Transactional` is removed?
- [ ] Why is `ListingRequest` separate from `Listing`? Name a field a client must not set.
- [ ] Soft delete vs hard delete: trade-offs?
- [ ] When would you pick `Specification` over `@Query`?

### Step 3: `user/` + `security/` + `config/` (login and JWT)
- [ ] `user/Role.java`, `user/Gender.java`, `user/User.java`, `user/UserRepository.java`
- [ ] `user/UserDtos.java`: notice `passwordHash` is never in a response
- [ ] `user/AuthController.java` → `user/AuthService.java`: register / login, BCrypt hashing
- [ ] `security/JwtProperties.java` → `security/JwtService.java`: create and verify tokens
- [ ] `security/JwtAuthenticationFilter.java`: reads `Authorization: Bearer ...` on every request
- [ ] `security/AuthUser.java`: the logged-in user object
- [ ] `config/SecurityConfig.java` ⭐: which URLs are public, stateless sessions, filter order
- [ ] `user/UserService.java` + `user/UserController.java`: `/users/me`
- [ ] Exercise: paste your token into https://jwt.io and read its claims (never paste real production tokens)
- [ ] Exercise: breakpoint in `JwtAuthenticationFilter.doFilterInternal`, then call `GET /users/me`
- Check yourself: Why hash passwords? Why is JWT "stateless"? Difference between 401 and 403?

### Step 4: `common/` (cross-cutting code)
- [ ] `ApiException.java` → `GlobalExceptionHandler.java` ⭐: how exceptions become JSON errors
- [ ] `PageResponse.java`, `Pageables.java`, `Timestamped.java` (if not read yet)
- Check yourself: What does `@RestControllerAdvice` do? What is RFC 9457 "problem details"?

### Step 5: `favorite/` (small, a good review)
- [ ] `Favorite.java` → `FavoriteRepository.java` (`@Modifying` delete query) → `FavoriteController.java`
- Check yourself: Why is `PUT /favorites/{id}` idempotent? What does a unique constraint protect against?

### Step 6: `messaging/`
- [ ] `Conversation.java`, `Message.java`
- [ ] `ConversationRepository.java`, `MessageRepository.java` (bulk `update` with `@Modifying`)
- [ ] `MessagingDtos.java` → `MessagingService.java` → `MessagingController.java`
- Check yourself: Why return 404 instead of 403 for someone else's conversation? Where would Kafka fit? (see the TODO in `append`)

### Step 7: `roommate/`
- [ ] `RoommateProfile.java` (`@OneToOne`) → `RoommateProfileRepository.java`
- [ ] `RoommateDtos.java` → `RoommateService.java` (Specification with a `join`) → `RoommateController.java`
- Check yourself: How does "upsert" work in `upsert()`? How is the gender preference enforced?

### Step 8: `geo/` + database + tests
- [ ] `geo/UsState.java`, `geo/ServiceArea.java`, `geo/GeoUtils.java`
- [ ] `src/main/resources/db/migration/V1__init_schema.sql`: compare tables with the entities
- [ ] `src/test/java/.../geo/ServiceAreaTest.java`: unit tests, `@ParameterizedTest`
- [ ] `src/test/java/.../LivingsApiIntegrationTest.java`: `@SpringBootTest`, `MockMvc`, `jsonPath`
- [ ] Exercise: write one new test of your own, then run all tests

---

## Phase 1–5: Build new features (after Phase 0)

- [ ] **1. Exception handling**: `DataIntegrityViolationException` → 409, `MethodArgumentTypeMismatchException` → 400, trace ID in every error
- [ ] **2. Security extensions**: JSON body for 401/403, `@PreAuthorize` admin endpoints, refresh tokens, fail startup if the JWT secret is missing in prod
- [ ] **3. Rate limiting**: Bucket4j on login (429 + `Retry-After`) → Spring Cloud Gateway + Redis
- [ ] **4. Kafka**: `MessageSent` / `ListingCreated` events, notification consumer, transactional outbox, dead-letter topic
- [ ] **5. Kubernetes**: manifests (Deployment, Service, Ingress, HPA, probes) on kind/minikube → EKS (RDS, MSK, ElastiCache, ECR)

---

## Handy reference

- Run: right-click `LivingsApplication` → Run / Debug
- Swagger: http://localhost:8080/swagger-ui.html
- H2 console: http://localhost:8080/h2-console (JDBC URL `jdbc:h2:file:./data/livings`, user `sa`, empty password)
- See generated SQL: add to `application-dev.yml`:
  ```yaml
  logging:
    level:
      org.hibernate.SQL: DEBUG
      org.hibernate.orm.jdbc.bind: TRACE
  ```
- Debugger: F8 step over · F7 step into · Shift+F8 step out · F9 resume · Cmd+F8 toggle breakpoint
- Run tests: right-click `src/test/java` → Run 'All Tests'
