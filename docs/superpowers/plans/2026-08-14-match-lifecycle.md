# Match Lifecycle Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Enforce the approved match lifecycle and timestamp rules for match and event mutations, return stable HTTP errors, and serialize competing commands on the same match.

**Architecture:** Add a pure `MatchLifecyclePolicy` that evaluates an already-loaded aggregate and returns typed decisions. Transactional services obtain a pessimistic write lock for the match, invoke the policy, and return typed command results. Facades translate domain objects to response DTOs, while controllers map command results to HTTP responses and RFC 9457 `ProblemDetail` bodies.

**Tech Stack:** Kotlin 1.9.25, Java 21, Spring Boot 3.5.7, Spring MVC, Spring Data JPA, PostgreSQL/H2, JUnit 5, MockMvc, Gradle.

## Global Constraints

- Preserve the approved lifecycle matrix:
  - match details can be changed only in `PLANNED`;
  - start is allowed only in `PLANNED`;
  - finish is allowed only in `IN_PROGRESS`;
  - event creation is allowed only in `IN_PROGRESS`;
  - event participant correction and deletion are allowed in `IN_PROGRESS` and `FINISHED`;
  - no transition back to an earlier state.
- Preserve the approved timestamp rules and allow equality at every boundary.
- Do not compare client timestamps with the server wall clock.
- Do not change event type or `occurredAt` through the event patch API.
- Keep existing routes and keep `PUT /api/v1/matches/{id}` partial until issue #78.
- Do not implement event-sequence rules in issue #76 and do not add an empty `EventSequencePolicy` abstraction.
- Policies must not access repositories, services, HTTP types, or Spring transaction state.
- A policy check and its mutation must run in one transaction while holding the match row lock.
- No database migration is required.
- Preserve unrelated local files `AGENTS.md`, `mcpserver.log`, and `openapi.json`.
- Follow TDD: observe every new test fail for the intended reason before adding the corresponding implementation.

---

## Task 1: Introduce the pure lifecycle policy

**Files:**

- Create: `src/main/kotlin/com/github/mihanizzm/ultistats/validation/match/MatchProblem.kt`
- Create: `src/main/kotlin/com/github/mihanizzm/ultistats/validation/match/MatchLifecycleDecision.kt`
- Create: `src/main/kotlin/com/github/mihanizzm/ultistats/validation/match/MatchLifecyclePolicy.kt`
- Create: `src/test/kotlin/com/github/mihanizzm/ultistats/validation/match/MatchLifecyclePolicyTest.kt`

- [ ] **Step 1: Write table-driven state-transition tests.**

  Cover update, start, finish, event create, event edit, and event delete for all three statuses. Assert both the decision subtype and the stable problem code. Use a test factory that builds a detached `Match`; do not start Spring.

  Representative test shape:

  ```kotlin
  @ParameterizedTest
  @MethodSource("eventCreationStates")
  fun `создание события разрешено только во время матча`(
      match: Match,
      expected: MatchLifecycleDecision,
  ) {
      assertThat(policy.validateEventCreation(match, EVENT_AT)).isEqualTo(expected)
  }
  ```

  Expected state failures:

  | Command | Current status | Problem code |
  |---|---|---|
  | update match | `IN_PROGRESS`, `FINISHED` | `MATCH_UPDATE_LOCKED` |
  | start | `IN_PROGRESS` | `MATCH_ALREADY_STARTED` |
  | start | `FINISHED` | `MATCH_ALREADY_FINISHED` |
  | finish | `PLANNED` | `MATCH_NOT_STARTED` |
  | finish | `FINISHED` | `MATCH_ALREADY_FINISHED` |
  | create event | `PLANNED`, `FINISHED` | `MATCH_NOT_IN_PROGRESS` |
  | edit/delete event | `PLANNED` | `MATCH_NOT_IN_PROGRESS` |

- [ ] **Step 2: Run the policy test and confirm it fails because the policy types do not exist.**

  Run:

  ```bash
  ./gradlew test --tests "com.github.mihanizzm.ultistats.validation.match.MatchLifecyclePolicyTest" --console=plain
  ```

  Expected: Kotlin test compilation fails on the new policy symbols, not on an unrelated existing test.

- [ ] **Step 3: Add typed policy decisions and problem data.**

  Use the following public shapes:

  ```kotlin
  enum class MatchProblemCode {
      INVALID_REQUEST,
      RESOURCE_NOT_FOUND,
      MATCH_NOT_IN_PROGRESS,
      MATCH_ALREADY_STARTED,
      MATCH_NOT_STARTED,
      MATCH_ALREADY_FINISHED,
      MATCH_UPDATE_LOCKED,
      START_AFTER_FIRST_EVENT,
      END_BEFORE_START,
      END_BEFORE_LAST_EVENT,
      EVENT_BEFORE_START,
      EVENT_OUT_OF_ORDER,
  }

  data class MatchProblem(
      val code: MatchProblemCode,
      val title: String,
      val detail: String,
      val currentStatus: MatchStatus? = null,
  )

  sealed interface MatchLifecycleDecision {
      data object Allowed : MatchLifecycleDecision
      data class InvalidState(val problem: MatchProblem) : MatchLifecycleDecision
      data class Conflict(val problem: MatchProblem) : MatchLifecycleDecision
  }
  ```

  `InvalidState` represents a command that is not legal in the current lifecycle status. `Conflict` represents a legal command whose timestamp conflicts with persisted match data.

- [ ] **Step 4: Implement the stateless policy.**

  Expose these methods:

  ```kotlin
  @Component
  class MatchLifecyclePolicy {
      fun validateUpdate(match: Match): MatchLifecycleDecision
      fun validateStart(match: Match, startedAt: Instant): MatchLifecycleDecision
      fun validateFinish(match: Match, endedAt: Instant): MatchLifecycleDecision
      fun validateEventCreation(match: Match, occurredAt: Instant): MatchLifecycleDecision
      fun validateEventUpdate(match: Match): MatchLifecycleDecision
      fun validateEventDeletion(match: Match): MatchLifecycleDecision
  }
  ```

  Use active events already present in `match.events`. `validateStart` compares with the earliest event, `validateFinish` with the latest event, and `validateEventCreation` with `startedAt` and the latest event. Use strict `<` comparisons so equal timestamps are accepted.

- [ ] **Step 5: Add boundary tests.**

  Assert:

  - start before or equal to the earliest active event is allowed; later start returns `START_AFTER_FIRST_EVENT`;
  - finish before start returns `END_BEFORE_START`;
  - finish before the latest active event returns `END_BEFORE_LAST_EVENT`;
  - an event before start returns `EVENT_BEFORE_START`;
  - an event before the latest active event returns `EVENT_OUT_OF_ORDER`;
  - equality with start/latest event is allowed;
  - planned start time and wall-clock time do not affect a decision.

- [ ] **Step 6: Run the policy test and commit.**

  ```bash
  ./gradlew test --tests "com.github.mihanizzm.ultistats.validation.match.MatchLifecyclePolicyTest" --console=plain
  git add src/main/kotlin/com/github/mihanizzm/ultistats/validation/match src/test/kotlin/com/github/mihanizzm/ultistats/validation/match
  git commit -m "Add match lifecycle policy"
  ```

  Expected: the policy suite passes without loading Spring.

---

## Task 2: Lock match commands and return typed service results

**Files:**

- Create: `src/main/kotlin/com/github/mihanizzm/ultistats/service/result/MatchCommandResult.kt`
- Modify: `src/main/kotlin/com/github/mihanizzm/ultistats/repository/jpa/SpringDataMatchRepository.kt`
- Modify: `src/main/kotlin/com/github/mihanizzm/ultistats/service/MatchService.kt`
- Modify: `src/main/kotlin/com/github/mihanizzm/ultistats/service/MatchServiceImpl.kt`
- Modify: `src/main/kotlin/com/github/mihanizzm/ultistats/facade/MatchFacade.kt`
- Modify: `src/test/kotlin/com/github/mihanizzm/ultistats/RelationalModelIntegrationTest.kt`

- [ ] **Step 1: Add failing production-path service tests.**

  Extend `RelationalModelIntegrationTest` with scenarios that use the real Spring Data repositories:

  - update succeeds while planned and returns the re-read match;
  - update after start returns `InvalidState(MATCH_UPDATE_LOCKED)` and changes nothing;
  - start succeeds once, persists the exact client timestamp, and a repeat start returns `InvalidState`;
  - finish of a planned match returns `InvalidState(MATCH_NOT_STARTED)`;
  - finish before the latest persisted event returns `Conflict(END_BEFORE_LAST_EVENT)`;
  - finish succeeds at the latest event timestamp and returns a `FINISHED` match;
  - a missing match returns `NotFound` for update, start, and finish.

  Assert the persisted value after each failure, not only the result object.

- [ ] **Step 2: Run the new integration tests and confirm lifecycle assertions fail.**

  ```bash
  ./gradlew test --tests "com.github.mihanizzm.ultistats.RelationalModelIntegrationTest" --console=plain
  ```

  Expected: failures show that the current Boolean/`Unit` service contract cannot represent the approved outcomes.

- [ ] **Step 3: Add the typed match result.**

  ```kotlin
  sealed interface MatchCommandResult<out T> {
      data class Success<T>(val value: T) : MatchCommandResult<T>
      data object NotFound : MatchCommandResult<Nothing>
      data class InvalidRequest(val problem: MatchProblem) : MatchCommandResult<Nothing>
      data class InvalidState(val problem: MatchProblem) : MatchCommandResult<Nothing>
      data class Conflict(val problem: MatchProblem) : MatchCommandResult<Nothing>
  }
  ```

  Build invalid team-selection failures as `MatchProblem(INVALID_REQUEST, ...)`; this generic code is not a lifecycle-state violation.

- [ ] **Step 4: Add a pessimistic-write repository query.**

  Add one explicit query so the lock semantics do not depend on a derived method name:

  ```kotlin
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select m from Match m where m.id = :id and m.deletedAt is null")
  fun findByIdForUpdate(@Param("id") id: UUID): Match?
  ```

- [ ] **Step 5: Change match mutations to commands over locked current state.**

  Change the service API to merge updates only after loading the current row:

  ```kotlin
  fun update(
      matchId: UUID,
      teamIds: List<UUID>?,
      plannedStartTimestamp: Instant?,
  ): MatchCommandResult<Match>

  fun startMatch(matchId: UUID, timestamp: Instant): MatchCommandResult<Match>
  fun endMatch(matchId: UUID, timestamp: Instant): MatchCommandResult<Match>

  @Transactional(Transactional.TxType.MANDATORY)
  fun getForUpdate(matchId: UUID): Match?
  ```

  Each public mutation must:

  1. start a transaction;
  2. load the active match using `findByIdForUpdate`;
  3. hydrate teams, participants, and active events;
  4. invoke the matching policy method;
  5. return a typed rejection without writing, or persist the mutation;
  6. re-read/hydrate the final aggregate and return `Success`.

  The `MANDATORY` propagation on `getForUpdate` prevents callers from accidentally obtaining a row lock that is released before they use the aggregate.

- [ ] **Step 6: Run integration and existing match tests, then commit.**

  Before running the tests, adapt `MatchFacade` to the new service signatures without changing HTTP semantics yet: convert `Success` to the existing response and every typed rejection to `null`. Task 3 replaces this temporary nullable adapter with the complete typed facade mapping. This keeps the repository compiling at the Task 2 commit boundary.

  ```bash
  ./gradlew test --tests "com.github.mihanizzm.ultistats.RelationalModelIntegrationTest" --tests "com.github.mihanizzm.ultistats.controller.MatchControllerTest" --console=plain
  git add src/main/kotlin/com/github/mihanizzm/ultistats/repository/jpa/SpringDataMatchRepository.kt src/main/kotlin/com/github/mihanizzm/ultistats/service src/main/kotlin/com/github/mihanizzm/ultistats/facade/MatchFacade.kt src/test/kotlin/com/github/mihanizzm/ultistats/RelationalModelIntegrationTest.kt
  git commit -m "Enforce lifecycle in match service"
  ```

  Expected: the new service tests pass; existing controller tests may still require the HTTP mapping changes in Task 3, but must compile.

---

## Task 3: Expose exact match HTTP semantics and ProblemDetail bodies

**Files:**

- Create: `src/main/kotlin/com/github/mihanizzm/ultistats/controller/ApiProblemDetails.kt`
- Modify: `src/main/kotlin/com/github/mihanizzm/ultistats/facade/MatchFacade.kt`
- Modify: `src/main/kotlin/com/github/mihanizzm/ultistats/controller/MatchController.kt`
- Modify: `src/test/kotlin/com/github/mihanizzm/ultistats/controller/MatchControllerTest.kt`

- [ ] **Step 1: Write failing MockMvc contract tests.**

  Cover the existing endpoints exactly:

  - `PUT /api/v1/matches/{id}`: `200`, invalid team selection `400`, missing match `404`, locked match `409`;
  - `POST /api/v1/matches/{id}/start`: `200`, missing match `404`, repeated/finished match `409`;
  - `POST /api/v1/matches/{id}/end`: success `200`, missing match `404`, planned/repeated finish `409`, and timestamp conflict `409`.

  For every `409`, assert `application/problem+json` and all fields:

  ```kotlin
  andExpect(status().isConflict)
  andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
  andExpect(jsonPath("$.status").value(409))
  andExpect(jsonPath("$.code").value("MATCH_UPDATE_LOCKED"))
  andExpect(jsonPath("$.currentStatus").value("IN_PROGRESS"))
  andExpect(jsonPath("$.instance").value("/api/v1/matches/$matchId"))
  ```

  For timestamp conflicts, assert `code` and absence of `currentStatus`. For invalid team selections, assert `400` and `code=INVALID_REQUEST`. For missing matches, assert a ProblemDetail body with `404`, `code=RESOURCE_NOT_FOUND`, and no `currentStatus`.

- [ ] **Step 2: Run only the match controller suite and observe failures.**

  ```bash
  ./gradlew test --tests "com.github.mihanizzm.ultistats.controller.MatchControllerTest" --console=plain
  ```

  Expected: current `400`/`404` branches and empty bodies fail the new assertions.

- [ ] **Step 3: Map service results in `MatchFacade`.**

  Return `MatchCommandResult<MatchResponse>` from create, update, start, and finish. Validate exactly two distinct, existing teams before the service mutation and return `InvalidRequest` on malformed selection. Map `Success<Match>` to a response using the final match returned by the service; do not reuse the match loaded before the mutation.

- [ ] **Step 4: Add one shared ProblemDetail builder and controller mappings.**

  Build `ProblemDetail` with Spring's API:

  ```kotlin
  fun MatchProblem.toProblemDetail(status: HttpStatus, instance: URI): ProblemDetail =
      ProblemDetail.forStatusAndDetail(status, detail).apply {
          title = this@toProblemDetail.title
          this.instance = instance
          setProperty("code", code.name)
          currentStatus?.let { setProperty("currentStatus", it.name) }
      }
  ```

  Map every result through the same `MatchProblem` builder:

  - `Success` to `200` (`201` for create);
  - `NotFound` to `404`;
  - `InvalidRequest` to `400`;
  - `InvalidState` and `Conflict` to `409`.

  Build the `NotFound` ProblemDetail in the controller with `code=RESOURCE_NOT_FOUND` and a detail naming only the requested resource type and ID.
  Change match mutation endpoint return types to `ResponseEntity<*>` so the same method can return either `MatchResponse` or `ProblemDetail`.

  Construct `instance` from the actual request path, including `/start` or `/end` for those commands.

- [ ] **Step 5: Run the match controller and service-path tests, then commit.**

  ```bash
  ./gradlew test --tests "com.github.mihanizzm.ultistats.controller.MatchControllerTest" --tests "com.github.mihanizzm.ultistats.RelationalModelIntegrationTest" --console=plain
  git add src/main/kotlin/com/github/mihanizzm/ultistats/controller src/main/kotlin/com/github/mihanizzm/ultistats/facade/MatchFacade.kt src/test/kotlin/com/github/mihanizzm/ultistats/controller/MatchControllerTest.kt
  git commit -m "Expose match lifecycle HTTP errors"
  ```

  Expected: exact status/body assertions pass and successful start/end responses contain the persisted timestamps.

---

## Task 4: Apply the lifecycle policy to event writes

**Files:**

- Create: `src/main/kotlin/com/github/mihanizzm/ultistats/service/result/EventCommandResult.kt`
- Modify: `src/main/kotlin/com/github/mihanizzm/ultistats/service/EventService.kt`
- Modify: `src/main/kotlin/com/github/mihanizzm/ultistats/service/EventServiceImpl.kt`
- Modify: `src/main/kotlin/com/github/mihanizzm/ultistats/facade/EventFacade.kt`
- Modify: `src/main/kotlin/com/github/mihanizzm/ultistats/controller/EventController.kt`
- Modify: `src/test/kotlin/com/github/mihanizzm/ultistats/controller/EventControllerTest.kt`
- Modify: `src/test/kotlin/com/github/mihanizzm/ultistats/service/EventServiceImplTest.kt`
- Modify: `src/test/kotlin/com/github/mihanizzm/ultistats/MatchAbstractTest.kt`
- Modify: `src/test/kotlin/com/github/mihanizzm/ultistats/controller/StatisticsControllerTest.kt`
- Modify: `src/test/kotlin/com/github/mihanizzm/ultistats/RelationalModelIntegrationTest.kt`

- [ ] **Step 1: Add failing event API lifecycle tests.**

  In `EventControllerTest`, add scenarios proving:

  - valid event creation in `PLANNED` returns `409 MATCH_NOT_IN_PROGRESS`;
  - valid event creation in `IN_PROGRESS` returns `201`;
  - event before `startedAt` returns `409 EVENT_BEFORE_START`;
  - event before the latest active event returns `409 EVENT_OUT_OF_ORDER`;
  - event creation in `FINISHED` returns `409 MATCH_NOT_IN_PROGRESS`;
  - patch and delete in `PLANNED` return `409 MATCH_NOT_IN_PROGRESS`;
  - patch and delete in `FINISHED` remain successful;
  - missing match/event still returns `404`;
  - invalid event shape still returns `400`;
  - unsupported participant patch still returns `405`.

  Assert `currentStatus` only for lifecycle-state errors.

- [ ] **Step 2: Run the event controller suite and observe the lifecycle tests fail.**

  ```bash
  ./gradlew test --tests "com.github.mihanizzm.ultistats.controller.EventControllerTest" --console=plain
  ```

  Expected: the planned/finished creation cases reveal the current missing lifecycle validation.

- [ ] **Step 3: Add a typed event command result.**

  ```kotlin
  sealed interface EventCommandResult {
      data class Success(val event: StoredEvent) : EventCommandResult
      data object Deleted : EventCommandResult
      data object NotFound : EventCommandResult
      data class InvalidState(val problem: MatchProblem) : EventCommandResult
      data class Conflict(val problem: MatchProblem) : EventCommandResult
  }
  ```

  Change only event mutation methods to return this result. Keep read methods unchanged.

- [ ] **Step 4: Lock, validate, and mutate within each event transaction.**

  For create, update, and remove:

  1. enter the existing transaction;
  2. call `matchService.getForUpdate(matchId)`;
  3. return `NotFound` if the active match is absent;
  4. invoke `validateEventCreation`, `validateEventUpdate`, or `validateEventDeletion`;
  5. return the typed rejection or perform the write;
  6. recalculate score before the transaction commits.

  Remove lifecycle/time-order `require` checks superseded by the policy. Keep request/event-shape validation in `EventFactory` and keep participant membership validation where it currently belongs.

- [ ] **Step 5: Map event results through facade and controller.**

  Extend `EventResult` with `InvalidState` and `Conflict`, each carrying `MatchProblem`. Return the same ProblemDetail format used by match commands. Keep `BadRequest` for malformed event requests and `MethodNotAllowed` for event kinds whose participants cannot be changed.

  Change mutation endpoint return types to `ResponseEntity<*>` so one method can return either its success DTO or `ProblemDetail`. Map event/match absence to `404 RESOURCE_NOT_FOUND`, malformed event input to `400 INVALID_REQUEST`, and lifecycle/time failures to their policy codes. Keep `405` for the existing unsupported patch behavior.

- [ ] **Step 6: Migrate existing test fixtures to the new invariant.**

  Start fixture matches at a deterministic instant at or before their first event before tests that create events. Do not weaken the new production check to preserve old tests.

  In particular:

  - start the default match in `EventControllerTest` before tests that expect `201`;
  - start service/statistics fixtures before calling `EventService.create`;
  - unwrap `EventCommandResult.Success` in direct service tests;
  - create separate planned and finished matches for lifecycle rejection tests so tests do not depend on execution order.

- [ ] **Step 7: Run affected suites and commit.**

  ```bash
  ./gradlew test --tests "com.github.mihanizzm.ultistats.controller.EventControllerTest" --tests "com.github.mihanizzm.ultistats.service.EventServiceImplTest" --tests "com.github.mihanizzm.ultistats.controller.StatisticsControllerTest" --tests "com.github.mihanizzm.ultistats.RelationalModelIntegrationTest" --console=plain
  git add src/main/kotlin/com/github/mihanizzm/ultistats/service src/main/kotlin/com/github/mihanizzm/ultistats/facade/EventFacade.kt src/main/kotlin/com/github/mihanizzm/ultistats/controller/EventController.kt src/test/kotlin/com/github/mihanizzm/ultistats
  git commit -m "Enforce lifecycle for event mutations"
  ```

  Expected: all event and statistics tests pass under the new `IN_PROGRESS` creation invariant.

---

## Task 5: Verify serialization of concurrent commands

**Files:**

- Create: `src/test/kotlin/com/github/mihanizzm/ultistats/service/MatchLifecycleConcurrencyTest.kt`

- [ ] **Step 1: Write a concurrent-start integration test.**

  Use `@SpringBootTest`, no test-level transaction, two executor threads, and a start gate. Both threads call `matchService.startMatch` for the same planned match. Collect both results after the futures finish.

  ```kotlin
  val gate = CountDownLatch(1)
  val futures = listOf(FIRST_START, SECOND_START).map { timestamp ->
      executor.submit<MatchCommandResult<Match>> {
          gate.await()
          matchService.startMatch(matchId, timestamp)
      }
  }
  gate.countDown()
  ```

  Assert exactly one `Success`, exactly one `InvalidState(MATCH_ALREADY_STARTED)`, and that the persisted `startedAt` equals the successful command timestamp.

- [ ] **Step 2: Run the test before the lock implementation is considered complete.**

  ```bash
  ./gradlew test --tests "com.github.mihanizzm.ultistats.service.MatchLifecycleConcurrencyTest" --console=plain
  ```

  Expected: H2 serializes the two transactions through the JPA pessimistic lock. Production PostgreSQL behavior is verified separately by simultaneous remote requests in Task 7. Do not replace the database lock with JVM synchronization.

- [ ] **Step 3: Add a concurrent finish-versus-event test.**

  Race an event creation and match finish for one in-progress match. Assert both observed outcomes correspond to one serialized order:

  - finish wins: finish succeeds and event creation returns `MATCH_NOT_IN_PROGRESS`; or
  - event wins: event succeeds and finish either uses a timestamp at/after it or returns `END_BEFORE_LAST_EVENT`.

  Never accept a finished match containing an event later than `endedAt`.

- [ ] **Step 4: Repeat the H2 test and run the same class against a clean PostgreSQL 16 container.**

  ```bash
  ./gradlew test --tests "com.github.mihanizzm.ultistats.service.MatchLifecycleConcurrencyTest" --rerun-tasks --console=plain
  docker container inspect ultistats-issue76-postgres
  ```

  The `docker container inspect` command must report that the exact task-owned name does not exist. Do not remove or reuse a pre-existing container with that name. After that check, start the disposable database:

  ```bash
  docker run -d --name ultistats-issue76-postgres -p 127.0.0.1:55476:5432 -e POSTGRES_DB=ultistats -e POSTGRES_USER=ultistats -e POSTGRES_PASSWORD=ultistats postgres:16-alpine
  docker exec ultistats-issue76-postgres pg_isready -U ultistats -d ultistats
  SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:55476/ultistats SPRING_DATASOURCE_USERNAME=ultistats SPRING_DATASOURCE_PASSWORD=ultistats SPRING_DATASOURCE_DRIVER_CLASS_NAME=org.postgresql.Driver SPRING_JPA_PROPERTIES_HIBERNATE_DIALECT=org.hibernate.dialect.PostgreSQLDialect SPRING_FLYWAY_ENABLED=true SPRING_SQL_INIT_MODE=never ./gradlew test --tests "com.github.mihanizzm.ultistats.service.MatchLifecycleConcurrencyTest" --rerun-tasks --console=plain
  ```

  Expected: Flyway creates a clean PostgreSQL schema and both concurrency tests pass against actual PostgreSQL row locks.

  Whether the test passes or fails, remove only the exact task-owned container and verify it is gone:

  ```bash
  docker rm -f ultistats-issue76-postgres
  docker container inspect ultistats-issue76-postgres
  ```

  Expected final inspect result: the container does not exist.

- [ ] **Step 5: Commit the concurrency tests.**

  ```bash
  git add src/test/kotlin/com/github/mihanizzm/ultistats/service/MatchLifecycleConcurrencyTest.kt
  git commit -m "Test serialized match lifecycle commands"
  ```

  Expected: no stale-write outcome and no invalid finished aggregate.

---

## Task 6: Run full local verification and self-review

**Files:**

- Review all files changed since `538bcb6`
- Update only tests or implementation files where verification reveals an issue

- [ ] **Step 1: Run formatting/static compilation and the complete test suite.**

  ```bash
  ./gradlew clean test build --console=plain
  ```

  Expected: `BUILD SUCCESSFUL` with every test passing.

- [ ] **Step 2: Inspect the complete diff and generated repository state.**

  ```bash
  git diff --check
  git status --short
  git diff 538bcb6...HEAD
  ```

  Verify:

  - no controller contains lifecycle rules;
  - no policy accesses a repository or service;
  - every event write locks the match before checking state;
  - every match mutation checks and writes in one transaction;
  - no event-sequence validation was introduced;
  - `PUT` remains partial;
  - every `409` has stable `code`; lifecycle-state errors also have `currentStatus`;
  - equality boundaries are covered;
  - no wall-clock check exists;
  - unrelated untracked files remain untouched.

- [ ] **Step 3: Run the required read-only self-review workflow.**

  Invoke the repository's `self-code-review` skill, address every confirmed issue, and rerun the smallest relevant test plus the full suite after any fix.

- [ ] **Step 4: Commit any review fixes.**

  ```bash
  git add src/main/kotlin src/test/kotlin
  git commit -m "Address match lifecycle self-review"
  ```

  Skip this commit when self-review finds no actionable issue.

---

## Task 7: Smoke-test the remote stand and clean up precisely

**Files:**

- No repository files should be changed by this task

- [ ] **Step 1: Confirm the stand and deployment correspond to this branch.**

  Use the task-specific environment variable `ULTISTATS_ISSUE76_STAND_URL`. If no stand URL or branch deployment is available, report the remote smoke test as skipped; do not guess a URL and do not test an unrelated deployment.

- [ ] **Step 2: Create an isolated data set and record every returned ID.**

  Use a unique prefix produced by `date -u +codex-issue76-%Y%m%dT%H%M%SZ`. Create exactly two teams and one match through the public API. Save both team IDs, the match ID, and the two unknown participant IDs returned for each team. Do not select or modify pre-existing records.

- [ ] **Step 3: Exercise the lifecycle through the public API.**

  With deterministic client timestamps, verify in order:

  1. valid event creation before start returns `409 MATCH_NOT_IN_PROGRESS`;
  2. two simultaneous start requests with different timestamps return exactly one `200` and one `409 MATCH_ALREADY_STARTED`;
  3. the re-read match contains the timestamp from the successful start response;
  4. match update returns `409 MATCH_UPDATE_LOCKED`;
  5. event creation using one of the returned unknown participants returns `201`;
  6. an earlier event returns `409 EVENT_OUT_OF_ORDER`;
  7. finish before the latest event returns `409 END_BEFORE_LAST_EVENT`;
  8. finish at the latest event time returns `200` and `FINISHED`;
  9. new event creation returns `409 MATCH_NOT_IN_PROGRESS`;
  10. patching the existing event in `FINISHED` returns `200`;
  11. deleting that event in `FINISHED` returns `204`.

  Capture response bodies and status codes in the terminal output; do not write credentials or authorization headers to repository files.

- [ ] **Step 4: Clean up only the recorded resources in reverse dependency order.**

  In a `finally`-style cleanup sequence, delete the recorded match first and then the two recorded teams. Do not use list-wide deletion, name-only matching, wildcard IDs, or database access.

- [ ] **Step 5: Verify cleanup.**

  GET each recorded match/team ID and require `404`. If deletion fails, report the exact surviving IDs to the user; do not touch any other data in an attempt to compensate.

- [ ] **Step 6: Perform final verification and prepare the PR.**

  ```bash
  ./gradlew test --console=plain
  git diff --check
  git status --short --branch
  ```

  Confirm the branch contains only intended commits and the three unrelated local files remain untracked. Then push `agent/issue-76-match-lifecycle` and open a ready-for-review PR linked to issue #76 with the lifecycle matrix, test evidence, concurrency behavior, and remote smoke/cleanup result in the description.
