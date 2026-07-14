# API review handoff after PR #75

## Purpose

This document records the controller/API audit performed after the explicit `TeamPlayer`
and category-specific event API work in PR #75. It is intended as starting context for a
future implementation session, not as a replacement for the linked GitHub issues.

## Current state

PR #75 introduces:

- explicit team-player membership resources;
- removal of membership state from `Player` and `Team` entities;
- category-specific event request/response DTOs;
- UUID-addressed events instead of list indexes;
- immutable event type and `occurredAt`;
- event chronology and match-snapshot participant validation.

At the time of this handoff, `./gradlew test` passes on branch
`feature/explicit-memberships-event-api`.

## Recommended implementation order

### 1. Request validation and error contract

Existing issues:

- #32 — validate sort, pagination, and filter parameters;
- #30 — return meaningful, consistent validation errors.

Implement these together if practical:

- add Spring Validation and `@Valid`/`@Validated` at controller boundaries;
- validate `page >= 0`, `1 <= size <= configured maximum`, date ranges, blank names,
  player numbers, and empty patch requests;
- reject unsupported sort fields rather than silently ignoring them;
- introduce `@RestControllerAdvice` and Spring `ProblemDetail` responses;
- use stable machine-readable error codes in addition to human-readable detail;
- map domain/use-case errors rather than broadly catching persistence exceptions in controllers.

Suggested error categories:

- `RESOURCE_NOT_FOUND` -> 404;
- `REQUEST_VALIDATION_FAILED` -> 400;
- `INVALID_RESOURCE_STATE` -> 409;
- `TEAM_NUMBER_CONFLICT` -> 409;
- `EVENT_CHRONOLOGY_INVALID` -> 400 or 409, chosen and documented consistently;
- storage/internal failures -> 5xx without leaking implementation details.

### 2. Match lifecycle and response semantics

New issue: #76 — <https://github.com/UltiStatsDev/ultistats-backend/issues/76>

Important current defects:

- `MatchFacade` uses `null` for both missing resources and invalid requests;
- update maps invalid team changes to 404;
- start/end map a missing match to 400;
- start/end chronology is not checked against each other or existing events;
- transition responses should be built from explicitly reloaded final state.

Prefer a typed use-case result over nullable return values. Define and test the match state
machine before changing the controller.

### 3. Correct partial-update semantics

New issue: #78 — <https://github.com/UltiStatsDev/ultistats-backend/issues/78>

Current `PUT` methods for Player, Team, and Match are partial updates. Convert them to
`PATCH` and decide how JSON distinguishes an omitted property from an explicit `null`.
This is required to support clearing `Team.city` and `Match.plannedStartTimestamp`.

Evaluate one of:

- JSON Merge Patch (`application/merge-patch+json`);
- presence-aware wrapper fields in Kotlin DTOs;
- explicit commands for clearing individual values.

Do not use ordinary nullable Kotlin properties alone: they collapse omitted and explicit null.

### 4. Pagination implementation

Existing issue: #63 — move pagination/sorting into the database layer.

Issue #32 should first establish parameter validation and supported sort fields. Then #63
can replace facade `drop/take` and in-memory sorting with repository `Pageable`/specifications.
Preserve response contract tests while changing the implementation.

### 5. Photo API and storage lifecycle

Existing security issue: #64.

New lifecycle/API issue: #77 — <https://github.com/UltiStatsDev/ultistats-backend/issues/77>

Treat these as related but distinct scopes. Recommended resource paths:

```text
PUT    /api/v1/players/{playerId}/photo
GET    /api/v1/players/{playerId}/photo
DELETE /api/v1/players/{playerId}/photo
```

and the equivalent team paths. Use `@RequestPart("file")`. Replacing or deleting a photo
must clean up the old storage object. Restore controller coverage for upload/get/delete;
the focused PR #75 tests intentionally concentrate on the revised membership contract.

### 6. Statistics API review

New issue: #79 — <https://github.com/UltiStatsDev/ultistats-backend/issues/79>

`StatisticsController` currently returns internal `MatchStatistics` models. Before implementing
DTOs, identify actual frontend views and decide whether one large response remains useful.
Coordinate with:

- #59 — remove unused/duplicate statistic fields;
- #35 — CSV export;
- #71 — offline-first event synchronization and possible cache/version metadata.

Add JSON contract tests so internal aggregator refactors cannot silently change the API.

## Other existing work that affects the API

- #65: transaction boundaries for multi-write use cases. Membership cleanup, team deletion,
  and future create-team-with-roster commands must be atomic.
- #71: offline-first batch event processing. It may supersede parts of the single-event append
  chronology contract, so avoid overfitting single-event endpoints before its design is settled.
- #23: unknown-player event workflow. Category-specific event patching is the likely correction
  mechanism, but unknown identity/lifecycle still needs an explicit design.

## Lower-priority cleanup

These can be folded into the issues above or handled in a small API consistency PR:

- fix stale Player sort documentation (`number` and `teamId` are no longer sortable);
- use explicit path-variable names consistently (`matchId`, `teamId`, `playerId`);
- add `Location` headers for successful resource creation;
- remove redundant `@ResponseStatus` when returning `ResponseEntity`;
- define deterministic membership/roster ordering;
- remove unused imports and improve OpenAPI response/error annotations;
- decide whether detail responses should embed memberships while dedicated membership endpoints
  also exist. The current duplication is acceptable for frontend convenience but should be deliberate.

## Verification expectations for future sessions

For each API change:

1. Add controller contract tests for success and every documented error status.
2. Add service/integration tests for state transitions and transaction rollback where relevant.
3. Run `./gradlew test`.
4. Run `git diff --check`.
5. Inspect generated OpenAPI/Swagger schemas when polymorphism, multipart, nullable fields, or
   error responses are involved.
