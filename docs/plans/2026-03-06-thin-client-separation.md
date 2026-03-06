# Thin Client Separation Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Make the Java desktop client (`poker` module) a true thin client with no poker engine knowledge, no direct database access, and no server-side imports beyond embedded server bootstrap.

**Architecture:** Create protocol-layer DTOs in `pokergameprotocol` to replace `pokerengine.model` JPA entities. Migrate hand history and tournament results storage from client-side H2 to server-side persistence with REST endpoints. Replace direct Spring bean access with REST calls.

**Tech Stack:** Java 25, Spring Boot 3.5, JPA/Hibernate, H2, JUnit 5, Mockito, Jackson, JDK HttpClient

**Design Doc:** `docs/plans/2026-03-06-thin-client-separation-design.md`

---

## Task Dependency Graph

```
Task 1 (Protocol DTOs) ──────────────────────┐
Task 2 (GameConfigBuilder) ← depends on 1    │
Task 3 (Hand History Entities) ───────────────┤
Task 4 (Hand History REST) ← depends on 3    ├──→ Task 8 (Client Model Migration) ← depends on 1,2
Task 5 (Tournament Results REST) ─────────────┤    Task 9 (Client DB→REST) ← depends on 4,5
Task 6 (Auth Cleanup)                         │    Task 10 (Enforcer & Cleanup) ← depends on 6,8,9
Task 7 (Move Legacy Tests)                    │
```

Tasks 1, 3, 5, 6, 7 can run in parallel. Tasks 2, 4, 8, 9, 10 have sequential dependencies.

---

## Task 1: Protocol Model Records

Create client-safe record types in `pokergameprotocol` to replace `pokerengine.model` JPA entities.

**Files:**
- Create: `code/pokergameprotocol/src/main/java/com/donohoedigital/games/poker/protocol/dto/TournamentProfileData.java`
- Create: `code/pokergameprotocol/src/main/java/com/donohoedigital/games/poker/protocol/dto/BlindLevelData.java`
- Create: `code/pokergameprotocol/src/main/java/com/donohoedigital/games/poker/protocol/dto/OnlineProfileData.java`
- Create: `code/pokergameprotocol/src/main/java/com/donohoedigital/games/poker/protocol/dto/OnlineGameData.java`
- Create: `code/pokergameprotocol/src/main/java/com/donohoedigital/games/poker/protocol/dto/TournamentHistoryData.java`
- Test: `code/pokergameprotocol/src/test/java/com/donohoedigital/games/poker/protocol/dto/TournamentProfileDataTest.java`
- Test: `code/pokergameprotocol/src/test/java/com/donohoedigital/games/poker/protocol/dto/OnlineProfileDataTest.java`
- Test: `code/pokergameprotocol/src/test/java/com/donohoedigital/games/poker/protocol/dto/TournamentHistoryDataTest.java`

**Step 1: Write TournamentProfileData record**

This is the largest record — it replaces `TournamentProfile` for client use. Fields derived from the TournamentProfile getter analysis. Use `@JsonInclude(NON_NULL)` to match existing protocol DTO pattern.

```java
@JsonInclude(JsonInclude.Include.NON_NULL)
public record TournamentProfileData(
    String name,
    String description,
    String greeting,
    int maxPlayers,
    int maxOnlinePlayers,
    int seats,
    boolean fillComputer,
    int buyin,
    int buyinChips,
    List<BlindLevelData> blindLevels,
    boolean doubleAfterLastLevel,
    String defaultGameType,
    String levelAdvanceMode,  // "TIME" or "HANDS"
    int handsPerLevel,
    int defaultMinutesPerLevel,
    // Rebuys
    boolean rebuysEnabled,
    int rebuyCost,
    int rebuyChips,
    int rebuyChipCount,
    int maxRebuys,
    int lastRebuyLevel,
    String rebuyExpressionType,
    // Addons
    boolean addonsEnabled,
    int addonCost,
    int addonChips,
    int addonLevel,
    // Payout
    String payoutType,
    double payoutPercent,
    int prizePool,
    int numSpots,
    List<Double> spotAllocations,
    String allocationType,
    // House
    String houseCutType,
    double housePercent,
    int houseAmount,
    // Bounty
    boolean bountyEnabled,
    int bountyAmount,
    // Timeouts
    int timeoutSeconds,
    int timeoutPreflop,
    int timeoutFlop,
    int timeoutTurn,
    int timeoutRiver,
    int thinkBankSeconds,
    // Boot
    boolean bootSitout,
    int bootSitoutCount,
    boolean bootDisconnect,
    int bootDisconnectCount,
    // Online settings
    boolean allowDash,
    boolean allowAdvisor,
    int maxObservers,
    // Betting
    int maxRaises,
    boolean raiseCapIgnoredHeadsUp,
    // Late registration
    boolean lateRegEnabled,
    int lateRegUntilLevel,
    String lateRegChipMode,
    // Scheduled start
    boolean scheduledStartEnabled,
    Instant startTime,
    int minPlayersForStart,
    // Invite
    boolean inviteOnly,
    List<String> invitees,
    boolean observersPublic
) {}
```

**Step 2: Write BlindLevelData record**

```java
@JsonInclude(JsonInclude.Include.NON_NULL)
public record BlindLevelData(
    int level,
    int smallBlind,
    int bigBlind,
    int ante,
    int minutes,
    boolean isBreak,
    String gameType
) {}
```

**Step 3: Write OnlineProfileData record**

```java
@JsonInclude(JsonInclude.Include.NON_NULL)
public record OnlineProfileData(
    Long id,
    String name,
    String email,
    Instant createDate,
    boolean retired
) {}
```

**Step 4: Write OnlineGameData record**

```java
@JsonInclude(JsonInclude.Include.NON_NULL)
public record OnlineGameData(
    Long id,
    String url,
    String hostPlayer,
    String mode,
    String tournamentName,
    String hostingType,
    Instant startDate,
    Instant endDate
) {}
```

**Step 5: Write TournamentHistoryData record**

```java
@JsonInclude(JsonInclude.Include.NON_NULL)
public record TournamentHistoryData(
    Long gameId,
    int place,
    String tournamentName,
    String tournamentType,
    Instant startDate,
    Instant endDate,
    int buyin,
    int rebuys,
    int addons,
    int prize,
    int numPlayers,
    int numRemaining,
    boolean ended,
    int numChips
) {
    public int totalSpent() {
        return buyin + rebuys + addons;
    }

    public int net() {
        return prize - totalSpent();
    }
}
```

**Step 6: Write tests for JSON round-trip serialization**

Tests should verify Jackson serialization/deserialization round-trips for each record. Use the same ObjectMapper pattern as existing protocol tests:

```java
class TournamentProfileDataTest {
    private static final ObjectMapper MAPPER = new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Test
    void roundTrip_allFields_preservesData() throws Exception {
        var profile = new TournamentProfileData("Test", "Desc", null, 10, 10, 9,
            true, 100, 1000, List.of(new BlindLevelData(1, 5, 10, 0, 20, false, "NOLIMIT")),
            false, "NOLIMIT", "TIME", 0, 20,
            false, 0, 0, 0, 0, 0, null,
            false, 0, 0, 0,
            "PERCENT", 100.0, 0, 3, List.of(50.0, 30.0, 20.0), "PERCENT",
            null, 0.0, 0,
            false, 0,
            30, 0, 0, 0, 0, 60,
            true, 3, true, 3,
            true, true, 10,
            4, true,
            false, 0, null,
            false, null, 0,
            false, null, false);
        String json = MAPPER.writeValueAsString(profile);
        var deserialized = MAPPER.readValue(json, TournamentProfileData.class);
        assertThat(deserialized).isEqualTo(profile);
    }
}
```

Similar round-trip tests for `OnlineProfileData`, `OnlineGameData`, `TournamentHistoryData`.

**Step 7: Run tests**

Run: `cd code && mvn test -pl pokergameprotocol -Dtest="TournamentProfileDataTest,OnlineProfileDataTest,TournamentHistoryDataTest" -P fast`
Expected: All pass

**Step 8: Commit**

```bash
git add code/pokergameprotocol/src/
git commit -m "feat(pokergameprotocol): add client-safe model records

Add TournamentProfileData, BlindLevelData, OnlineProfileData,
OnlineGameData, TournamentHistoryData records for client use.
These replace direct pokerengine.model JPA entity imports."
```

---

## Task 2: GameConfigBuilder

Create a client-side converter from `TournamentProfileData` -> `GameConfig` in `pokergameprotocol`. This replaces the client's use of `TournamentProfileConverter` (which stays in `pokergameserver` for server-side entity conversion).

**Files:**
- Create: `code/pokergameprotocol/src/main/java/com/donohoedigital/games/poker/protocol/dto/GameConfigBuilder.java`
- Test: `code/pokergameprotocol/src/test/java/com/donohoedigital/games/poker/protocol/dto/GameConfigBuilderTest.java`
- Reference: `code/pokergameserver/src/main/java/com/donohoedigital/games/poker/gameserver/controller/TournamentProfileConverter.java` (logic to replicate)

**Step 1: Write failing test**

Test the core conversion — blind structure, rebuys, payouts. Reference `TournamentProfileConverter` for the exact mapping logic. The builder takes `TournamentProfileData` (protocol DTO) and produces `GameConfig` (protocol DTO) — no engine types involved.

```java
class GameConfigBuilderTest {
    @Test
    void convert_basicProfile_mapsAllFields() {
        var profile = new TournamentProfileData("Test Tournament", "A test", null,
            10, 10, 9, true, 100, 1000,
            List.of(
                new BlindLevelData(1, 5, 10, 0, 20, false, "NOLIMIT"),
                new BlindLevelData(2, 10, 20, 0, 20, false, "NOLIMIT"),
                new BlindLevelData(3, 0, 0, 0, 10, true, null)  // break
            ),
            false, "NOLIMIT", "TIME", 0, 20,
            false, 0, 0, 0, 0, 0, null,
            false, 0, 0, 0,
            "PERCENT", 100.0, 0, 3, List.of(50.0, 30.0, 20.0), "PERCENT",
            null, 0.0, 0,
            false, 0,
            30, 0, 0, 0, 0, 60,
            true, 3, true, 3,
            true, true, 10,
            4, true,
            false, 0, null,
            false, null, 0,
            false, null, false);

        GameConfig config = GameConfigBuilder.fromProfile(profile);

        assertThat(config.name()).isEqualTo("Test Tournament");
        assertThat(config.maxPlayers()).isEqualTo(10);
        assertThat(config.startingChips()).isEqualTo(1000);
        assertThat(config.blindStructure()).hasSize(3);
        assertThat(config.blindStructure().get(2).isBreak()).isTrue();
        assertThat(config.payout().spots()).isEqualTo(3);
    }
}
```

**Step 2: Implement GameConfigBuilder**

Static method `fromProfile(TournamentProfileData)` that maps each field group. Follow the same conversion logic as `TournamentProfileConverter.convert()` but sourcing from `TournamentProfileData` fields instead of `TournamentProfile` getters.

Also add `buildAiPlayers(TournamentProfileData, List<String> aiNames, int defaultSkillLevel)` for AI player list generation.

**Step 3: Run tests**

Run: `cd code && mvn test -pl pokergameprotocol -Dtest="GameConfigBuilderTest" -P fast`
Expected: All pass

**Step 4: Commit**

```bash
git add code/pokergameprotocol/src/
git commit -m "feat(pokergameprotocol): add GameConfigBuilder for client-side profile conversion"
```

---

## Task 3: Hand History JPA Entities

Create server-side persistence for hand history data. Three new JPA entities plus a repository and service layer.

**Files:**
- Create: `code/pokergameserver/src/main/java/com/donohoedigital/games/poker/gameserver/persistence/entity/HandHistoryEntity.java`
- Create: `code/pokergameserver/src/main/java/com/donohoedigital/games/poker/gameserver/persistence/entity/HandPlayerEntity.java`
- Create: `code/pokergameserver/src/main/java/com/donohoedigital/games/poker/gameserver/persistence/entity/HandActionEntity.java`
- Create: `code/pokergameserver/src/main/java/com/donohoedigital/games/poker/gameserver/persistence/repository/HandHistoryRepository.java`
- Create: `code/pokergameserver/src/main/java/com/donohoedigital/games/poker/gameserver/persistence/repository/HandPlayerRepository.java`
- Create: `code/pokergameserver/src/main/java/com/donohoedigital/games/poker/gameserver/persistence/repository/HandActionRepository.java`
- Create: `code/pokergameserver/src/main/java/com/donohoedigital/games/poker/gameserver/service/HandHistoryService.java`
- Modify: `code/pokergameserver/src/main/java/com/donohoedigital/games/poker/gameserver/ServerHand.java` (implement `storeHandHistory()`)
- Test: `code/pokergameserver/src/test/java/com/donohoedigital/games/poker/gameserver/service/HandHistoryServiceTest.java`

**Step 1: Write HandHistoryEntity**

Follow the `GameEventEntity` pattern. Key fields: handId (generated), gameId (FK to game_instances), tableId, handNumber, gameStyle, gameType, startDate, endDate, ante, smallBlind, bigBlind, communityCards (stored as JSON string via `@Column(columnDefinition = "TEXT")`), communityCardsDealt.

```java
@Entity
@Table(name = "hand_history", indexes = {
    @Index(name = "idx_hand_history_game_id", columnList = "game_id"),
    @Index(name = "idx_hand_history_game_hand", columnList = "game_id, hand_number")
})
public class HandHistoryEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "game_id", nullable = false, length = 64)
    private String gameId;

    @Column(name = "table_id")
    private int tableId;

    @Column(name = "hand_number")
    private int handNumber;

    // ... remaining fields matching design
}
```

**Step 2: Write HandPlayerEntity and HandActionEntity**

`HandPlayerEntity`: composite key (handId, playerId), seat, holeCards (JSON), chips, action bitflags, cardsExposed.

`HandActionEntity`: composite key (handId, playerId, sequence), round, actionType, amount, subAmount, allIn.

**Step 3: Write repositories**

Spring Data JPA interfaces:

```java
public interface HandHistoryRepository extends JpaRepository<HandHistoryEntity, Long> {
    Page<HandHistoryEntity> findByGameIdOrderByHandNumberDesc(String gameId, Pageable pageable);
    long countByGameId(String gameId);
    Optional<HandHistoryEntity> findByGameIdAndId(String gameId, Long handId);
    List<HandHistoryEntity> findByGameId(String gameId);
}
```

Similar for `HandPlayerRepository` and `HandActionRepository` with `findByHandId()` methods.

**Step 4: Write HandHistoryService**

Service that orchestrates storing and querying hand history. Methods:

- `storeHand(String gameId, ServerHand hand)` — persists all three entities
- `getHandCount(String gameId)` — delegates to repository count
- `getHandSummaries(String gameId, Pageable pageable)` — returns paginated summaries
- `getHandDetail(String gameId, Long handId)` — returns full hand with players and actions
- `getHandStats(String gameId)` — aggregated statistics query
- `getHandsForExport(String gameId)` — full hand data for export

**Step 5: Write tests for HandHistoryService**

Unit tests with mocked repositories. Verify:
- `storeHand()` calls all three repository `save()` methods
- `getHandSummaries()` delegates to repository with correct pagination
- `getHandDetail()` assembles entity + players + actions into complete result

**Step 6: Implement ServerHand.storeHandHistory()**

Replace the no-op with a call to `HandHistoryService.storeHand()`. The `ServerHand` already has all the data: `playerHands`, `community`, `history` (List<ServerHandAction>), blind amounts, hand number.

Inject `HandHistoryService` via the `ServerGameTable` or `ServerTournamentDirector` that creates ServerHand instances.

**Step 7: Run tests**

Run: `cd code && mvn test -pl pokergameserver -P fast`
Expected: All pass

**Step 8: Commit**

```bash
git add code/pokergameserver/src/
git commit -m "feat(pokergameserver): add hand history persistence

Add HandHistoryEntity, HandPlayerEntity, HandActionEntity JPA entities.
Add HandHistoryRepository interfaces and HandHistoryService.
Implement ServerHand.storeHandHistory() to persist hand data."
```

---

## Task 4: Hand History REST Endpoints

Expose hand history data via REST. New protocol DTOs for the response shapes, plus a new controller.

**Files:**
- Create: `code/pokergameprotocol/src/main/java/com/donohoedigital/games/poker/protocol/dto/HandSummaryData.java`
- Create: `code/pokergameprotocol/src/main/java/com/donohoedigital/games/poker/protocol/dto/HandDetailData.java`
- Create: `code/pokergameprotocol/src/main/java/com/donohoedigital/games/poker/protocol/dto/HandPlayerDetailData.java`
- Create: `code/pokergameprotocol/src/main/java/com/donohoedigital/games/poker/protocol/dto/HandActionDetailData.java`
- Create: `code/pokergameprotocol/src/main/java/com/donohoedigital/games/poker/protocol/dto/HandStatsData.java`
- Create: `code/pokergameprotocol/src/main/java/com/donohoedigital/games/poker/protocol/dto/HandExportData.java`
- Create: `code/pokergameserver/src/main/java/com/donohoedigital/games/poker/gameserver/controller/HandHistoryController.java`
- Test: `code/pokergameserver/src/test/java/com/donohoedigital/games/poker/gameserver/controller/HandHistoryControllerTest.java`

**Step 1: Write protocol DTOs**

```java
public record HandSummaryData(Long handId, int handNumber, int tableId,
    List<String> holeCards, List<String> communityCards, Instant startDate) {}

public record HandDetailData(Long handId, int handNumber, int tableId,
    String gameStyle, String gameType, Instant startDate, Instant endDate,
    int ante, int smallBlind, int bigBlind,
    List<String> communityCards, int communityCardsDealt,
    List<HandPlayerDetailData> players, List<HandActionDetailData> actions) {}

public record HandPlayerDetailData(int playerId, String playerName, int seatNumber,
    int startChips, int endChips, List<String> holeCards,
    int preflopActions, int flopActions, int turnActions, int riverActions,
    boolean cardsExposed) {}

public record HandActionDetailData(int playerId, int sequence, int round,
    String actionType, int amount, int subAmount, boolean allIn) {}

public record HandStatsData(String handClass, int count, double winPct, double losePct,
    double passPct, double avgBet, double avgChips,
    double flopPct, double turnPct, double riverPct, double showdownPct) {}

public record HandExportData(Long handId, int handNumber, String tournamentName,
    int tournamentId, String tableId, String gameStyle, String gameType,
    Instant startDate, Instant endDate,
    int ante, int smallBlind, int bigBlind, int buttonSeat,
    List<String> communityCards,
    List<HandPlayerDetailData> players,
    List<HandActionDetailData> actions) {}
```

**Step 2: Write HandHistoryController**

```java
@RestController
@RequestMapping("/api/v1/games/{gameId}/hands")
public class HandHistoryController {
    private final HandHistoryService handHistoryService;

    @GetMapping
    public Page<HandSummaryData> listHands(@PathVariable String gameId, Pageable pageable) { ... }

    @GetMapping("/count")
    public long countHands(@PathVariable String gameId) { ... }

    @GetMapping("/{handId}")
    public HandDetailData getHand(@PathVariable String gameId, @PathVariable Long handId) { ... }

    @GetMapping("/{handId}/html")
    public String getHandHtml(@PathVariable String gameId, @PathVariable Long handId,
        @RequestParam(defaultValue = "false") boolean showAll) { ... }

    @GetMapping("/stats")
    public List<HandStatsData> getStats(@PathVariable String gameId) { ... }

    @GetMapping("/export")
    public List<HandExportData> exportHands(@PathVariable String gameId) { ... }
}
```

**Step 3: Add entity-to-DTO mapping in HandHistoryService**

Add mapping methods to convert `HandHistoryEntity` + related entities into protocol DTOs.

**Step 4: Move hand HTML rendering server-side**

Move the hand formatting logic from `PokerDatabase.getHandAsHTML()` to a new server-side utility class `HandHtmlRenderer` in `pokergameserver`. This reads from entities and produces the same HTML output.

Reference: `code/poker/src/main/java/com/donohoedigital/games/poker/PokerDatabase.java` lines 1120-1340.

**Step 5: Move hand class calculation server-side**

Move `PokerDatabaseProcs.getHandClass()` logic to a utility in `pokergameserver` for use in statistics aggregation.

Reference: `code/poker/src/main/java/com/donohoedigital/games/poker/PokerDatabaseProcs.java`

**Step 6: Write controller tests**

Follow the `@WebMvcTest` + `@MockitoBean` pattern from existing `HistoryControllerTest`:

```java
@WebMvcTest
@Import({TestSecurityConfiguration.class, HandHistoryController.class})
class HandHistoryControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private HandHistoryService handHistoryService;

    @Test
    void listHands_gameExists_returnsPage() throws Exception {
        when(handHistoryService.getHandSummaries(eq("game1"), any()))
            .thenReturn(new PageImpl<>(List.of(
                new HandSummaryData(1L, 1, 0, List.of("Ac", "Kd"), List.of(), Instant.now())
            )));

        mockMvc.perform(get("/api/v1/games/game1/hands"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].handNumber").value(1));
    }
}
```

**Step 7: Run tests**

Run: `cd code && mvn test -pl pokergameprotocol,pokergameserver -P fast`
Expected: All pass

**Step 8: Commit**

```bash
git add code/pokergameprotocol/src/ code/pokergameserver/src/
git commit -m "feat(pokergameserver): add hand history REST endpoints

Add HandHistoryController with endpoints for hand listing, detail,
HTML rendering, statistics, and export. Move hand formatting and
hand class calculation server-side."
```

---

## Task 5: Tournament Results REST Endpoints

Add missing REST endpoints for tournament history management (delete, stats). The existing `/api/v1/history` GET endpoint already handles reads.

**Files:**
- Modify: `code/pokergameserver/src/main/java/com/donohoedigital/games/poker/gameserver/controller/HistoryController.java`
- Modify: `code/pokergameserver/src/main/java/com/donohoedigital/games/poker/gameserver/service/HistoryService.java` (or equivalent)
- Create: `code/pokergameprotocol/src/main/java/com/donohoedigital/games/poker/protocol/dto/OverallStatsData.java`
- Test: `code/pokergameserver/src/test/java/com/donohoedigital/games/poker/gameserver/controller/HistoryControllerTest.java` (extend)

**Step 1: Write OverallStatsData DTO**

```java
public record OverallStatsData(
    int totalTournaments,
    int totalWins,
    int totalPrize,
    int totalSpent,
    int netProfit,
    double avgFinish,
    double avgROI
) {}
```

**Step 2: Add endpoints to HistoryController**

```java
@GetMapping("/history/stats")
public OverallStatsData getOverallStats(@RequestParam String name) { ... }

@DeleteMapping("/history/{id}")
public ResponseEntity<Void> deleteHistory(@PathVariable Long id) { ... }

@DeleteMapping("/history")
public ResponseEntity<Void> deleteAllHistory(@RequestParam String name) { ... }
```

**Step 3: Write tests**

**Step 4: Run tests**

Run: `cd code && mvn test -pl pokergameserver -Dtest="HistoryControllerTest" -P fast`
Expected: All pass

**Step 5: Commit**

```bash
git add code/pokergameprotocol/src/ code/pokergameserver/src/
git commit -m "feat(pokergameserver): add tournament history delete and stats endpoints"
```

---

## Task 6: Auth Cleanup

Replace direct Spring bean access in `EmbeddedGameServer` with REST calls.

**Files:**
- Modify: `code/poker/src/main/java/com/donohoedigital/games/poker/server/EmbeddedGameServer.java`
- Test: `code/poker/src/test/java/com/donohoedigital/games/poker/server/EmbeddedGameServerTest.java` (if exists, or create)

**Step 1: Write failing test**

Test that `getLocalUserJwt()` returns a valid JWT without requiring Spring bean access. Mock the HTTP client to verify REST calls are made to `/api/v1/auth/register` and `/api/v1/auth/login`.

**Step 2: Modify EmbeddedGameServer.getJwtForProfile()**

Replace lines that use `context.getBean(AuthService.class)` and `context.getBean(JwtTokenProvider.class)` with HTTP calls:

```java
// Before:
AuthService authService = context.getBean(AuthService.class);
JwtTokenProvider jwtProvider = context.getBean(JwtTokenProvider.class);
long profileId = registerOrLogin(authService, username, password);
String jwt = jwtProvider.generateToken(username, profileId, false, true);

// After:
String jwt = registerOrLoginViaRest(username, password);
```

New method `registerOrLoginViaRest()`:
```java
private String registerOrLoginViaRest(String username, String password) {
    String email = username + "@local.ddpoker";
    String baseUrl = "http://localhost:" + port;
    ObjectMapper mapper = ...;

    // Try register first
    HttpRequest req = HttpRequest.newBuilder()
        .uri(URI.create(baseUrl + "/api/v1/auth/register"))
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(
            mapper.writeValueAsString(new RegisterRequest(username, password, email))))
        .build();
    HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
    LoginResponse loginResp = mapper.readValue(resp.body(), LoginResponse.class);

    if (loginResp.success()) {
        return loginResp.token();
    }

    // Fallback to login
    req = HttpRequest.newBuilder()
        .uri(URI.create(baseUrl + "/api/v1/auth/login"))
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(
            mapper.writeValueAsString(new LoginRequest(username, password, false))))
        .build();
    resp = http.send(req, HttpResponse.BodyHandlers.ofString());
    loginResp = mapper.readValue(resp.body(), LoginResponse.class);
    return loginResp.token();
}
```

**Step 3: Remove server-side imports**

Remove these imports from `EmbeddedGameServer.java`:
```java
// DELETE:
import com.donohoedigital.games.poker.gameserver.auth.JwtKeyManager;
import com.donohoedigital.games.poker.gameserver.auth.JwtTokenProvider;
import com.donohoedigital.games.poker.gameserver.service.AuthService;
```

Add protocol imports:
```java
import com.donohoedigital.games.poker.protocol.dto.RegisterRequest;
import com.donohoedigital.games.poker.protocol.dto.LoginRequest;
import com.donohoedigital.games.poker.protocol.dto.LoginResponse;
```

**Step 4: Run tests**

Run: `cd code && mvn test -pl poker -P fast`
Expected: All pass

**Step 5: Commit**

```bash
git add code/poker/src/
git commit -m "refactor(poker): replace direct auth bean access with REST calls

EmbeddedGameServer now uses /api/v1/auth/register and /login
endpoints instead of directly accessing AuthService and JwtTokenProvider."
```

---

## Task 7: Move Legacy Test Files

Move engine test files from `poker` to `pokerengine` where they belong.

**Files:**
- Move: `code/poker/src/test/java/com/donohoedigital/games/poker/HandInfoFasterTest.java` -> `code/pokerengine/src/test/java/com/donohoedigital/games/poker/engine/HandInfoFasterTest.java`
- Move: `code/poker/src/test/java/com/donohoedigital/games/poker/HandUtilsTest.java` -> `code/pokerengine/src/test/java/com/donohoedigital/games/poker/engine/HandUtilsTest.java`

**Step 1: Check for duplicates**

Check if `pokerengine/src/test/` already has these test files. If so, compare content and keep the more complete version.

**Step 2: Move files and update package declarations**

Change package from `com.donohoedigital.games.poker` to `com.donohoedigital.games.poker.engine`.

**Step 3: Run tests in both modules**

Run: `cd code && mvn test -pl pokerengine,poker -P fast`
Expected: All pass; no test count regression

**Step 4: Commit**

```bash
git add code/poker/src/test/ code/pokerengine/src/test/
git commit -m "refactor: move engine tests from poker to pokerengine module"
```

---

## Task 8: Client Model Migration

Replace all `pokerengine.model` imports in the `poker` module with protocol record types. This is the largest task — ~25 files in main src need their model imports changed.

**Files (abbreviated — see design doc Section 6 for full list):**
- Modify: `code/poker/src/main/java/com/donohoedigital/games/poker/TournamentProfileDialog.java`
- Modify: `code/poker/src/main/java/com/donohoedigital/games/poker/TournamentProfileFormatter.java`
- Modify: `code/poker/src/main/java/com/donohoedigital/games/poker/TournamentOptions.java`
- Modify: `code/poker/src/main/java/com/donohoedigital/games/poker/BlindQuickSetupDialog.java`
- Modify: `code/poker/src/main/java/com/donohoedigital/games/poker/PrizePoolDialog.java`
- Modify: `code/poker/src/main/java/com/donohoedigital/games/poker/PokerGame.java`
- Modify: `code/poker/src/main/java/com/donohoedigital/games/poker/online/OnlineConfiguration.java`
- Modify: `code/poker/src/main/java/com/donohoedigital/games/poker/online/FindGames.java`
- Modify: `code/poker/src/main/java/com/donohoedigital/games/poker/online/ListGames.java`
- Modify: `code/poker/src/main/java/com/donohoedigital/games/poker/PlayerProfile.java`
- Modify: `code/poker/src/main/java/com/donohoedigital/games/poker/PlayerProfileOptions.java`
- Modify: `code/poker/src/main/java/com/donohoedigital/games/poker/RestartTournament.java`
- Modify: `code/poker/src/main/java/com/donohoedigital/games/poker/TournamentSummaryPanel.java`
- Modify: `code/poker/src/main/java/com/donohoedigital/games/poker/StatisticsViewer.java`
- Modify: `code/poker/src/main/java/com/donohoedigital/games/poker/server/GameServerRestClient.java`
- Modify: All remaining files with `import com.donohoedigital.games.poker.model.*`

**Approach:** Work through files methodically. For each file:
1. Replace `import com.donohoedigital.games.poker.model.*` with specific protocol DTO imports
2. Replace `TournamentProfile` references with `TournamentProfileData`
3. Replace `OnlineProfile` with `OnlineProfileData`
4. Replace `OnlineGame` with `OnlineGameData`
5. Replace `TournamentHistory` with `TournamentHistoryData`
6. Update method signatures and field types accordingly
7. For `GameServerRestClient` — replace `TournamentProfileConverter` with `GameConfigBuilder`

**Key change in PokerGame.java:**
```java
// Before:
private TournamentProfile profile_;
// After:
private TournamentProfileData profile_;
```

**Key change in GameServerRestClient.java:**
```java
// Before:
import com.donohoedigital.games.poker.gameserver.controller.TournamentProfileConverter;
private final TournamentProfileConverter converter;
GameConfig config = converter.convert(profile);

// After:
import com.donohoedigital.games.poker.protocol.dto.GameConfigBuilder;
GameConfig config = GameConfigBuilder.fromProfile(profile);
```

**Profile save/load in tournament dialogs:**

Replace `DataMarshal` binary serialization with Jackson JSON:
```java
// Before:
TournamentProfile profile = (TournamentProfile) DataMarshal.demarshal(data);

// After:
ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
TournamentProfileData profile = mapper.readValue(jsonString, TournamentProfileData.class);
```

**Step 1: Start with core classes** — `PokerGame.java`, `TournamentProfileFormatter.java`

**Step 2: Update tournament UI classes** — dialog, options, blinds, payout, summary

**Step 3: Update online classes** — `OnlineConfiguration`, `FindGames`, `ListGames`

**Step 4: Update profile classes** — `PlayerProfile`, `PlayerProfileOptions`

**Step 5: Update `GameServerRestClient`** — swap converter

**Step 6: Run full poker module tests**

Run: `cd code && mvn test -pl poker -P fast`
Expected: All pass

**Step 7: Verify no model imports remain**

Run: `grep -r "import com.donohoedigital.games.poker.model" code/poker/src/main/java/`
Expected: Zero results

**Step 8: Commit**

```bash
git add code/poker/src/
git commit -m "refactor(poker): replace pokerengine.model imports with protocol records

All TournamentProfile, OnlineProfile, OnlineGame, TournamentHistory
references replaced with protocol DTOs. GameServerRestClient now uses
GameConfigBuilder instead of TournamentProfileConverter."
```

---

## Task 9: Client Database to REST Migration

Replace all `PokerDatabase` calls with `GameServerRestClient` REST calls. Delete the client-side database infrastructure.

**Files:**
- Delete: `code/poker/src/main/java/com/donohoedigital/games/poker/PokerDatabase.java`
- Delete: `code/poker/src/main/java/com/donohoedigital/games/poker/PokerDatabaseProcs.java`
- Delete: `code/poker/src/main/java/com/donohoedigital/games/poker/DatabaseQueryTableModel.java`
- Modify: `code/poker/src/main/java/com/donohoedigital/games/poker/HandHistoryPanel.java`
- Modify: `code/poker/src/main/java/com/donohoedigital/games/poker/HandHistoryDialog.java`
- Modify: `code/poker/src/main/java/com/donohoedigital/games/poker/StatisticsViewer.java`
- Modify: `code/poker/src/main/java/com/donohoedigital/games/poker/GameInfoDialog.java`
- Modify: `code/poker/src/main/java/com/donohoedigital/games/poker/HistoryExportDialog.java`
- Modify: `code/poker/src/main/java/com/donohoedigital/games/poker/PlayerProfile.java`
- Modify: `code/poker/src/main/java/com/donohoedigital/games/poker/server/GameServerRestClient.java`

**Step 1: Add hand history methods to GameServerRestClient**

```java
public long getHandCount(String gameId, String jwt) { ... }
public List<HandSummaryData> getHandSummaries(String gameId, String jwt, int page, int pageSize) { ... }
public HandDetailData getHandDetail(String gameId, String jwt, long handId) { ... }
public String getHandHtml(String gameId, String jwt, long handId, boolean showAll) { ... }
public List<HandStatsData> getHandStats(String gameId, String jwt) { ... }
public List<HandExportData> getHandsForExport(String gameId, String jwt) { ... }
public OverallStatsData getOverallStats(String jwt, String playerName) { ... }
public void deleteHistory(String jwt, long id) { ... }
public void deleteAllHistory(String jwt, String playerName) { ... }
```

**Step 2: Rewrite HandHistoryPanel**

Replace `PokerDatabase.getHandCount()` / `getHandIDs()` / `getHandAsHTML()` / `getHandListHTML()` calls with `GameServerRestClient` methods. The panel receives a `GameServerRestClient` instance and game context instead of SQL WHERE clauses.

Key change: The panel's constructor changes from `(Context, style, where, bindArray, pageSize)` to `(Context, style, gameId, jwt, port, pageSize)`.

**Step 3: Rewrite HandHistoryDialog**

Update to pass game context (gameId, jwt, port) to `HandHistoryPanel` instead of SQL parameters.

**Step 4: Rewrite StatisticsViewer**

- "Finishes" tab: calls `GameServerRestClient` for tournament history (already served by `/api/v1/history`)
- "By Hand" and "By Round" tabs: calls `GameServerRestClient.getHandStats()` for server-aggregated data
- Remove `DatabaseQueryTableModel` subclasses, replace with simple `AbstractTableModel` backed by DTOs

**Step 5: Rewrite GameInfoDialog**

Replace `PokerDatabase.storeTournament()` + `HandHistoryPanel` initialization with REST-backed panel.

**Step 6: Rewrite HistoryExportDialog**

Replace `getExportSummary()` / `getHandForExport()` with `GameServerRestClient.getHandsForExport()`.

**Step 7: Update PlayerProfile**

Replace `PokerDatabase.getTournamentHistory()`, `getOverallHistory()`, `deleteTournament()`, `deleteAllTournaments()` with REST calls.

**Step 8: Remove PokerDatabase lifecycle calls**

Search for `PokerDatabase.init()`, `PokerDatabase.shutdownDatabase()`, `PokerDatabase.delete()` and remove them.

**Step 9: Delete database files**

Delete `PokerDatabase.java`, `PokerDatabaseProcs.java`, `DatabaseQueryTableModel.java`.

**Step 10: Run tests**

Run: `cd code && mvn test -pl poker -P fast`
Expected: All pass

**Step 11: Verify no db imports remain**

Run: `grep -r "import com.donohoedigital.db" code/poker/src/main/java/`
Expected: Zero results

**Step 12: Commit**

```bash
git add code/poker/src/
git commit -m "refactor(poker): replace client database with REST API calls

Delete PokerDatabase, PokerDatabaseProcs, DatabaseQueryTableModel.
HandHistoryPanel, StatisticsViewer, GameInfoDialog, HistoryExportDialog,
PlayerProfile now use GameServerRestClient for all data access."
```

---

## Task 10: Enforcer & Dependency Cleanup

Final cleanup — tighten Maven Enforcer, remove unused dependencies, clean wildcard imports.

**Files:**
- Modify: `code/poker/pom.xml`
- Modify: `code/poker/src/main/java/com/donohoedigital/games/poker/online/Lobby.java`
- Modify: `code/poker/src/main/java/com/donohoedigital/games/poker/online/OnlineConfiguration.java`
- Modify: `code/poker/src/main/java/com/donohoedigital/games/poker/PokerShowdownPanel.java`

**Step 1: Update poker/pom.xml**

Remove dependencies:
```xml
<!-- DELETE these -->
<dependency>
    <groupId>com.donohoedigital</groupId>
    <artifactId>db</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-mail</artifactId>
</dependency>
```

Change Enforcer:
```xml
<!-- Change from: -->
<searchTransitive>false</searchTransitive>
<!-- To: -->
<searchTransitive>true</searchTransitive>
```

**Step 2: Clean wildcard imports**

In `Lobby.java`: remove `import com.donohoedigital.games.poker.server.*`

In `OnlineConfiguration.java`: remove `import com.donohoedigital.games.poker.gameserver.*` and `import com.donohoedigital.games.poker.server.*`, replace with explicit protocol imports.

In `PokerShowdownPanel.java`: remove `import com.donohoedigital.games.poker.server.*`, replace with explicit import if any actual class is used.

**Step 3: Full build verification**

Run: `cd code && mvn clean test`
Expected: Full build succeeds with enforcer checks passing

**Step 4: Verify no server imports remain in client main src**

Run these checks:
```bash
grep -r "import com.donohoedigital.games.poker.model" code/poker/src/main/java/
grep -r "import com.donohoedigital.games.poker.engine" code/poker/src/main/java/
grep -r "import com.donohoedigital.games.poker.core" code/poker/src/main/java/
grep -r "import com.donohoedigital.db" code/poker/src/main/java/
```
Expected: Zero results for all four

Acceptable remaining server imports (embedded server bootstrap only):
```bash
grep -r "import com.donohoedigital.games.poker.gameserver" code/poker/src/main/java/
# Should only appear in code/poker/src/main/java/.../server/EmbeddedServerConfig.java
```

**Step 5: Commit**

```bash
git add code/poker/
git commit -m "build(poker): enforce strict thin-client boundaries

Set searchTransitive=true on Maven Enforcer for pokerengine/pokergamecore.
Remove db, h2, spring-data-jpa, spring-mail dependencies.
Clean wildcard server imports from Lobby, OnlineConfiguration, PokerShowdownPanel."
```

---

## Final Verification

After all tasks complete:

1. `cd code && mvn clean verify` — full build with enforcer and coverage
2. `cd code && mvn test -P dev` — dev profile tests still work
3. Verify the desktop client launches: `java -jar poker/target/DDPokerCE-3.3.0.jar`
4. Start a practice game and verify hand history displays
5. Check statistics viewer works with REST-backed data
