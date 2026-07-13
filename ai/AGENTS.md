# ultistats-backend

Kotlin + Spring Boot 3 backend for Ultimate Frisbee stats.

## Quick start

```bash
docker-compose up -d                              # app + PostgreSQL
./gradlew bootRun                                  # dev (needs PG on localhost:5432)
```

API at `http://localhost:8080/api/v1/...`, Swagger at `/swagger-ui.html`.

## PostgreSQL-only

No more in-memory profile. The app requires PostgreSQL. Env vars: `DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD`.

`docker-compose.yml` runs both the app and postgres:16-alpine with a health check.

## Tests

```bash
./gradlew test
```

Tests use **H2 in PostgreSQL compatibility mode** (`MODE=PostgreSQL`). Flyway is disabled in tests; schema is created via `src/test/resources/schema.sql`. `application.yaml` in test resources overrides the main one entirely.

All tests are `@SpringBootTest` (full context, no slicing). Controller tests use `@AutoConfigureMockMvc`. Service tests extend `MatchAbstractTest` which clears all data in `@BeforeEach` and sets up fixed-UUID teams/players/match.

## Architecture

`Controller` → `Facade` → `Service` → `SpringData*Repository` (JPA directly — no repository interface layer anymore).

Pagination, filtering, and sorting are **in-memory** in facades using `drop`/`take` + `SortingUtils`. Sort format: `field:direction` (e.g. `plannedStartTimestamp:desc`). Sortable match fields: `plannedStartTimestamp`, `startedAt`, `endedAt`, `status`.

Models are JPA `@Entity` classes directly (`Match`, `Team`, `Player`). JSONB columns (`events`, `team_scores`) use custom JPA `@Convert` converters. `Team.playerIds` is `@Transient` — loaded separately.

Static files: `/uploads/**` from `file:uploads/`. All entity IDs are `UUID`.

## Build

- Java 21, Kotlin 1.9.25, Spring Boot 3.5.7, Gradle 8.14.3
- No CI/CD configured (`.github` absent).
- Docker multi-stage build: `gradle:8.14-jdk21` → `eclipse-temurin:21-jre-alpine`.
