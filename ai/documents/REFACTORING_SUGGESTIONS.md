# Предложения по рефакторингу

Все пункты из трёх волн анализа — без пересечений.

---

## Высокий приоритет

### 1. Immutable collections вместо MutableList (Match.kt)
**Почему:** `MutableList` создаёт лишние копии при каждой мутации, проблематично для concurrent access  
**Исправление:** использовать `List<Event>`, `Map<UUID, TeamScore>` для атомарных изменений

### 2. Выделение бизнес-логики пересчёта счёта в отдельный слой (TeamScoreCalculator)
**Почему:** `recalculateTeamScores()` смешивает данные с логикой — трудно тестировать и расширять  
**Исправление:** создать Calculator интерфейс, делегировать к нему вычисления через dependency injection

### 3. Defensive null-checks вместо RuntimeException (Match.kt:80-84)
**Почему:** `if (teamScore == null)` с последующим throw нечитабелен  
**Исправление:** добавить `require(teamScores.any { it.teamId == scoringTeamId })` в начале метода

### 4. Удаление лишних чтений БД в `EventServiceImpl`
**Где:** `EventServiceImpl.kt:22,35,48`  
**Почему:** После каждого изменения событий цепочка `update` → `recalculateDiskHolder` → **`getOrThrow`** даёт лишний запрос diskHolderId (повторяется 3 раза).  
**Исправление:** вернуть `diskHolderId` из `recalculateDiskHolder(...)` или обновлять объект match внутри него.

### 5. N+1 запросы при сборке связанных данных в фасадах
**Где:** `MatchFacade.kt:36,54-56`, `PlayerFacade.kt:39,57-59,82`, `TeamFacade.kt:36,98,134`  
**Почему:** Для каждого Match/Player/Team выполняется отдельный запрос к связанным сущностям — O(n) запросов.  
**Исправление:** загрузить связанные данные одним набором до цикла.

### 6. Дублирование утилитарного метода `asJsonContent()` в конвертерах
**Где:** `EventListJsonConverter.kt:29-40`, `TeamScoreListJsonConverter.kt:25-36`  
**Почему:** Идентичная логика размножена в двух местах.  
**Исправление:** вынести в общий `JsonbUtils.asJsonContent(String)`.

### 7. Неявная валидация с возвратом null в `EventFactory`
**Где:** `EventFactory.kt:17-27,38`  
**Почему:** `createFromRequest` возвращает null при некорректных данных — теряется причина отказа.  
**Исправление:** выбрасывать `IllegalArgumentException` с описанием проблемы, обрабатывать в `EventFacade`.

### 8. Отсутствует глобальный обработчик исключений (`@ControllerAdvice`)
**Где:** Нет файла-advice  
**Почему:** `EntityNotFoundException`, `IllegalArgumentException` возвращают 500 со stack trace вместо чистого 404/400.  
**Исправление:** добавить `@RestControllerAdvice`.

### 9. `countFiltered` загружает все строки в память вместо COUNT-запроса
**Где:** `MatchServiceImpl.kt:43-44`  
**Почему:** `findAllFiltered(filter).size.toLong()` тащит всё из БД вместе с jsonb-колонками.  
**Исправление:** JPQL COUNT-метод в `SpringDataMatchRepository`.

### 10. Внедрение `LocalFileStorageService` вместо интерфейса
**Где:** `PlayerFacade.kt:24`, `TeamFacade.kt:24`  
**Почему:** Фасады завязаны на конкретную реализацию, а не на `FileStorageService`. Смена storage требует правки каждого фасада.  
**Исправление:** заменить тип поля на `FileStorageService`.

### 11. Фото не удаляются с диска при удалении сущности
**Где:** `PlayerFacade.kt:129-141` (`delete`), `TeamFacade.kt:118-129` (`delete`), `PlayerFacade.kt:157-163` (`deletePhotoUrl`), `TeamFacade.kt:187-193` (`deletePhotoUrl`)  
**Почему:** Обновляется `photoUrl = null` в БД, но файл на диске не удаляется.  
**Исправление:** вызывать `localFileStorageService.delete(existingPhotoUrl)` перед занулением.

### 12. `PrettyStringConverter` — пустой `@Component`
**Где:** `utils/PrettyStringConverter.kt`  
**Почему:** Класс в контексте Spring, но без единого метода — только TODO-комментарии.  
**Исправление:** удалить или реализовать запланированные методы.

---

## Средний приоритет

### 13. Добавить KDoc для публичных интерфейсов (StatisticsAggregator.kt)
**Почему:** отсутствие `@param`/`@throws` затрудняет понимание контракта  
**Исправление:** задокументировать предусловия, постусловия и побочные эффекты

### 14. Value Object для статуса матча вместо computed property
**Почему:** `status` вычисляется каждый раз через `when` — логика размазана  
**Исправление:** сделать `MatchStatusValueObject`, принимающий `startedAt`/`endedAt`, возвращающий статус + reason

### 15. Отсутствие @Transactional для групповых изменений
**Где:** `PlayerFacade.kt:76-105`, `TeamFacade.kt:82,94-124`, `EventServiceImpl.kt:16-49`  
**Почему:** Нет гарантии атомарности при изменении нескольких entities.  
**Исправление:** обернуть методы фасадов/сервисов `@Transactional`.

### 16. Лишнее двойное чтение в `TeamFacade.getPhotoUrl`
**Где:** `TeamFacade.kt:183-184`  
**Почему:** `teamService.get(teamId)` вызывается дважды подряд.
**Исправление:** сохранить результат первого вызова в переменную.

### 17. `@EnableScheduling` без `@Scheduled`-методов
**Где:** `UltistatsApplication.kt:8`  
**Почему:** Планировщик запущен, потребляет поток, но ничего не делает.  
**Исправление:** удалить до появления реальной задачи.

### 18. Мутация статических тестовых данных в `MatchAbstractTest`
**Где:** `MatchAbstractTest.kt:39-147`, `@BeforeEach` очищает `MATCH.events`  
**Почему:** Статическое поле мутируется между тестовыми классами — потенциально flaky тесты.  
**Исправление:** создавать свежий `Match` в `@BeforeEach`.

### 19. Дублирование маппинга `teamIds → teamScore` в Response-классах
**Где:** `MatchResponse.kt:20-34`, `MatchListItemResponse.kt:18-31`  
**Почему:** Идентичный код обхода `match.teamIds` с поиском счёта.  
**Исправление:** вынести в extension `Match.toTeamScoresMap(): Map<UUID, Int>`.

### 20. `LocalFileStorageService.upload()` использует force-unwrap `file!!`
**Где:** `LocalFileStorageService.kt:23`  
**Почему:** NPE без сообщения при null.  
**Исправление:** `requireNotNull(file) { "File must not be null" }`.

### 21. Небезопасная nullable-сигнатура `delete` и `getUrl`
**Где:** `FileStorageService.kt:6-9`, `LocalFileStorageService.kt:34,42`  
**Почему:** `delete(key: String?)` — `root.resolve(null)` вызовет `InvalidPathException`.  
**Исправление:** сменить параметры на `String`.

---

## Низкий приоритет

### 22. Добавить @JvmStatic к companion object Player.kt
**Почему:** для вызова из Java требуется создавать инстанс  
**Исправление:** использовать `@JvmStatic`

### 23. Идентичная структура `getAllPaged` для трёх фасадов
**Где:** `MatchFacade.kt:40-60`, `PlayerFacade.kt:52-61`, `TeamFacade.kt:48-57`  
**Почему:** Паттерн `filter → applySorting → drop/take → map` повторён 3 раза.  
**Исправление:** абстрагировать через extension-функцию `List<T>.paged(...)`.

### 24. Неявный fallback на default-сортировку при неизвестном поле
**Где:** `SortingUtils.kt:39-42`  
**Почему:** `buildComparator` возвращает null → список без сортировки, без предупреждения.  
**Исправление:** логировать или бросать исключение.

### 25. Некорректная обработка результата при `startMatch`/`endMatch`
**Где:** `MatchFacade.kt:103-122`  
**Почему:** При `false` от сервиса фасад возвращает null → контроллер отдаёт 404 вместо 400.  
**Исправление:** использовать `EventResult`-подобный sealed class или разнести коды ответа.

### 26. Отсутствие проверки уникальности номера игрока
**Где:** `PlayerFacade.kt:71-127`  
**Почему:** Номер может дублироваться в рамках одной команды.  
**Исправление:** `existsWithNumberInTeam()` или unique constraint `(team_id, number)`.

### 27. `SortParam.parse` не валидирует лишние части после второго двоеточия
**Где:** `SortParam.kt:12-13`  
**Почему:** `"lastName:desc:extra"` игнорирует `:extra` без ошибки.  
**Исправление:** `require(parts.size <= 2)`.

### 28. `Verifiable` — неиспользуемый тестовый утилитарный интерфейс
**Где:** `Verifiable.kt`  
**Почему:** Интерфейс объявлен, но не используется ни в одном тесте.  
**Исправление:** удалить.

### 29. Статистика всегда пересчитывается с нуля
**Где:** `StatisticsServiceImpl.kt:34-41`  
**Почему:** Каждый запрос `GET /statistics` проходит по всем событиям заново, хотя для неизменившегося матча результат тот же.  
**Исправление:** `@Cacheable(matchId)` + `@CacheEvict` при изменении событий.
