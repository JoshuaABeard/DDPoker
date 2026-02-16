# Milestone 2: Game API, Authentication & Database Persistence — Detailed Plan

**Status:** DRAFT
**Created:** 2026-02-16
**Parent Plan:** `.claude/plans/SERVER-HOSTED-GAME-ENGINE.md`
**Depends On:** `.claude/plans/M1-SERVER-GAME-ENGINE.md` (complete, pending merge)

---

## Context

M1 created the `pokergameserver` module — a Spring Boot auto-configuration library that runs complete poker tournaments server-side. It has GameInstanceManager, ServerTournamentDirector, all game objects, and an in-memory event store. 24 production classes, 243 tests.

**What M1 provides:**
- `GameInstanceManager` — creates, tracks, and cleans up game instances
- `GameInstance` — single game lifecycle (create → wait → play → complete)
- `ServerTournamentDirector` — drives game loop via TournamentEngine
- `GameEventStore` — append-only in-memory event log
- `GameStateProjection` — per-player state views (card hiding security)
- Spring Boot auto-configuration (`@AutoConfiguration`)

**What M2 adds:**
- **JWT authentication (RS256)** — asymmetric JWT auth protecting endpoints, cross-server token validation
- **Profile management API** — registration, update, password change, retire
- **Game management REST API** — CRUD and lifecycle at `/api/v1/games/...`
- **GameConfig model** — clean server-side tournament configuration (replaces legacy TournamentProfile for server use)
- **Ban system** — profile ID and email-based bans
- **Database persistence** — H2 + Spring Data JPA for event store, game instances, and profiles

**Key architecture decisions from discussion:**
1. Game endpoints live in `pokergameserver` as auto-configured controllers (not the existing `api` module)
2. `/api/v1/` prefix — existing `api` module is throwaway
3. Spring Data JPA for persistence (not existing DAO pattern)
4. Asymmetric JWT (RS256) — WAN server signs with private key, game servers validate with public key
5. Profile CRUD included — registration, update, password change, retire
6. Ban system uses profile ID and email (not legacy keys or IP)

---

## Prerequisites

| # | Prerequisite | Status | Notes |
|---|-------------|--------|-------|
| M1 | Server Game Engine Foundation | Complete (pending merge) | 24 classes, 243 tests in `feature-m1-server-game-engine` worktree |

---

## Existing Infrastructure to Adapt

The `api` module has working JWT/security code that we'll adapt (not copy verbatim) for `pokergameserver`:

| Component | Existing Location | Adapting To |
|-----------|-------------------|-------------|
| JWT token generation | `api/.../JwtTokenProvider.java` | `pokergameserver/.../auth/JwtTokenProvider.java` |
| JWT auth filter | `api/.../JwtAuthFilter.java` | `pokergameserver/.../auth/JwtAuthFilter.java` |
| Security config | `api/.../SecurityConfig.java` | `pokergameserver/.../auth/GameServerSecurityAutoConfiguration.java` |
| BCrypt hashing | `pokerserver/.../PasswordHashingServiceImpl.java` | Direct BCrypt usage in auth service |
| OnlineProfile entity | `pokerengine/.../OnlineProfile.java` | Spring Data JPA repository (read-only for auth) |

**Key differences from existing implementation:**
- Support both cookie-based JWT (web) AND `Authorization: Bearer` header (desktop/API clients)
- Auto-configuration pattern (conditional on web environment)
- Spring Data JPA repositories (not EntityManager DAOs)
- Ban check by profile ID (not legacy key-based — keys were removed from the system)
- CORS configured same pattern as existing api module (configurable allowed origins)
- No admin role from PropertyConfig (game ownership replaces admin for game operations)

---

## Phase 2.1: Dependencies & Auto-Configuration

### 2.1a. New dependencies for `pokergameserver/pom.xml`

```xml
<!-- REST controllers + embedded Tomcat -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
    <optional>true</optional>  <!-- Host app brings this in -->
</dependency>

<!-- Spring Security for JWT auth -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
    <optional>true</optional>
</dependency>

<!-- Spring Data JPA + Hibernate -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
    <optional>true</optional>
</dependency>

<!-- H2 database (runtime) -->
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>runtime</scope>
    <optional>true</optional>
</dependency>

<!-- JWT library (same version as existing api module: 0.12.5) -->
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <scope>runtime</scope>
</dependency>

<!-- BCrypt for password validation -->
<dependency>
    <groupId>org.mindrot</groupId>
    <artifactId>jbcrypt</artifactId>
</dependency>

<!-- Test: Spring Security test support -->
<dependency>
    <groupId>org.springframework.security</groupId>
    <artifactId>spring-security-test</artifactId>
    <scope>test</scope>
</dependency>
```

**Why `<optional>true</optional>`:** This is a Spring Boot auto-configuration library. Optional means the host app must explicitly bring in the starters it wants. If it includes `spring-boot-starter-web`, the REST controllers auto-configure. If not, the module still works as a pure game engine (M1 functionality only).

### 2.1b. Split auto-configuration

M1 has a single `GameServerAutoConfiguration`. Split into:

| Auto-Configuration | Condition | Provides |
|-------------------|-----------|----------|
| `GameServerAutoConfiguration` | Always (existing M1) | `GameInstanceManager` |
| `GameServerWebAutoConfiguration` | `@ConditionalOnWebApplication` | REST controllers, security, auth |
| `GameServerPersistenceAutoConfiguration` | `@ConditionalOnClass(DataSource.class)` | JPA repositories, `DatabaseGameEventStore` |

Register all three in `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`.

### 2.1c. Entity scanning

```java
@AutoConfiguration
@ConditionalOnClass(DataSource.class)
@EnableJpaRepositories(basePackages = "com.donohoedigital.games.poker.gameserver.persistence.repository")
@EntityScan(basePackages = {
    "com.donohoedigital.games.poker.gameserver.persistence.entity",
    "com.donohoedigital.games.poker.model"  // OnlineProfile for auth
})
public class GameServerPersistenceAutoConfiguration {
    // ...
}
```

---

## Phase 2.2: JWT Authentication

Adapt the existing `api` module's JWT implementation for `pokergameserver`, with these changes:
- Support both cookie AND `Authorization: Bearer` header
- Auto-configuration pattern (not hard-wired)
- Use Spring Data JPA for profile lookup (not EntityManager DAO)

### 2.2a. JwtTokenProvider — Asymmetric JWT (RS256)

**File:** `pokergameserver/.../auth/JwtTokenProvider.java`

**Key design:** Uses **asymmetric RSA keys (RS256)** instead of HMAC so that tokens issued by the WAN server can be validated by any game server (standalone or embedded) without sharing a secret.

**Two operating modes:**
- **Issuing mode** (WAN server): Has RSA private key. Can generate AND validate tokens.
- **Validation-only mode** (embedded game server): Has RSA public key only. Can validate tokens but NOT generate them. Used by community-hosted game servers that trust the WAN server's identity.

```java
@Component
public class JwtTokenProvider {
    private final PrivateKey privateKey;   // Null in validation-only mode
    private final PublicKey publicKey;     // Always present
    private final long expiration;
    private final long rememberMeExpiration;

    // Issuing mode: generate + validate
    public String generateToken(String username, Long profileId, boolean rememberMe) {
        if (privateKey == null) {
            throw new IllegalStateException("Cannot generate tokens in validation-only mode");
        }
        return Jwts.builder()
            .subject(username)
            .claim("profileId", profileId)
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() +
                (rememberMe ? rememberMeExpiration : expiration)))
            .signWith(privateKey)
            .compact();
    }

    // Validation: works in both modes (uses public key)
    public boolean validateToken(String token) { ... }
    public String getUsernameFromToken(String token) { ... }
    public Long getProfileIdFromToken(String token) { ... }
}
```

**Key management:**
- On first startup, if no keys exist, auto-generate RSA 2048-bit key pair
- Private key stored at `game.server.jwt.private-key-path` (default: `{WORK}/jwt-private.pem`)
- Public key stored at `game.server.jwt.public-key-path` (default: `{WORK}/jwt-public.pem`)
- Embedded game servers only need the public key file
- PEM format for standard interoperability

**Claims:**
```json
{
  "sub": "PlayerName",
  "profileId": 42,
  "iat": 1708000000,
  "exp": 1708086400
}
```

**Auth flow across hosting modes:**
```
WAN Server (has private key):
  Player logs in → POST /api/v1/auth/login → JWT signed with private key

Server-hosted game (same server, has private key):
  Player presents JWT → validated with public key → identity confirmed

Community-hosted game (embedded, has public key only):
  Player presents WAN-issued JWT → validated with public key → identity confirmed
  Host registered game with WAN server, so WAN server provided its public key

Practice mode (embedded, localhost):
  No JWT needed → auto-authenticated as local user
```

### 2.2b. JwtAuthFilter

**File:** `pokergameserver/.../auth/JwtAuthFilter.java`

Extends `OncePerRequestFilter`. Checks for JWT in two places:
1. `Authorization: Bearer {token}` header (desktop/API clients)
2. `ddpoker-token` cookie (web browsers)

Header takes priority over cookie if both present.

On valid token: sets `UsernamePasswordAuthenticationToken` in SecurityContext with `ROLE_USER` authority and a custom principal object containing both username and profileId.

### 2.2c. GameServerSecurityAutoConfiguration

**File:** `pokergameserver/.../auth/GameServerSecurityAutoConfiguration.java`

```java
@AutoConfiguration
@ConditionalOnWebApplication
@ConditionalOnClass(SecurityFilterChain.class)
public class GameServerSecurityAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public JwtTokenProvider jwtTokenProvider() { ... }

    @Bean
    @ConditionalOnMissingBean
    public JwtAuthFilter jwtAuthFilter(JwtTokenProvider tokenProvider) { ... }

    @Bean
    public SecurityFilterChain gameServerSecurityFilterChain(HttpSecurity http, JwtAuthFilter authFilter) {
        http.csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Public
                .requestMatchers("/api/v1/auth/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/games/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/profiles").permitAll()  // Registration
                // Protected
                .requestMatchers("/api/v1/games/**").authenticated()
                .requestMatchers("/api/v1/profiles/**").authenticated()
                .anyRequest().permitAll())
            .addFilterBefore(authFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        // Same pattern as existing api module SecurityConfig
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList(allowedOrigins.split(",")));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }
}

// CORS origins configurable via property (same as existing api module)
// game.server.cors.allowed-origins=http://localhost:3000,http://localhost:8080
```

**Security rules:**
- `GET /api/v1/games` and `GET /api/v1/games/{id}` — public (browse/view)
- All mutating game operations — authenticated
- Auth endpoints — public
- Owner-specific operations (start, pause, cancel) — enforced at service layer via `profileId` from JWT

**`@ConditionalOnMissingBean`** on token provider and filter allows the host app to override with its own implementation (e.g., simplified auth for embedded desktop).

### 2.2d. Auth Controller

**File:** `pokergameserver/.../controller/AuthController.java`

```
POST /api/v1/auth/login     — Validate username/password, return JWT
POST /api/v1/auth/logout    — Clear JWT cookie
GET  /api/v1/auth/me        — Return current user info from JWT
```

**Login flow:**
1. Accept `LoginRequest` (username, password, rememberMe)
2. Look up `OnlineProfile` via `OnlineProfileRepository.findByName(username)`
3. Check profile exists and not retired
4. Check profile not banned via `BannedProfileRepository.findByProfileId(profile.getId())`
5. Validate password with `BCrypt.checkpw(password, profile.getPasswordHash())`
6. Generate JWT with username + profileId
7. Set HttpOnly cookie (for web) AND return token in response body (for API clients)
8. Return `AuthResponse` with success, username, profileId

**Response format:**
```json
{
  "success": true,
  "username": "Alice",
  "profileId": 42,
  "token": "eyJhbG..."  // Also set as HttpOnly cookie
}
```

### 2.2e. OnlineProfileRepository

**File:** `pokergameserver/.../persistence/repository/OnlineProfileRepository.java`

```java
public interface OnlineProfileRepository extends JpaRepository<OnlineProfile, Long> {
    Optional<OnlineProfile> findByName(String name);
    Optional<OnlineProfile> findByEmail(String email);
    boolean existsByName(String name);
    boolean existsByEmail(String email);
}
```

Used for both auth lookup (login) and profile CRUD operations (Phase 2.3-extra).

### 2.2f-extra. Ban System (Profile ID and Email)

The legacy `BannedKey` system used string keys (email, username, license key). Keys have been removed from the system. Refactor to a cleaner ban model supporting two ban types:

| Ban Type | Use Case |
|----------|----------|
| **Profile ID** | Ban a specific account |
| **Email** | Ban all accounts sharing an email (catches alt accounts) |

**File:** `pokergameserver/.../persistence/entity/BanEntity.java`

```java
@Entity
@Table(name = "bans", indexes = {
    @Index(name = "idx_ban_profile_id", columnList = "profile_id"),
    @Index(name = "idx_ban_email", columnList = "email")
})
public class BanEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ban_type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private BanType banType;           // PROFILE or EMAIL

    @Column(name = "profile_id")
    private Long profileId;            // Set when banType=PROFILE

    @Column(name = "email")
    private String email;              // Set when banType=EMAIL

    @Column(name = "until", nullable = false)
    private LocalDate until;

    @Column(name = "reason", length = 255)
    private String reason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public enum BanType { PROFILE, EMAIL }

    @PrePersist
    private void onInsert() {
        if (createdAt == null) createdAt = Instant.now();
        if (until == null) until = LocalDate.of(2099, 12, 31);
    }

    public boolean isActive() {
        return !LocalDate.now().isAfter(until);
    }
}
```

**File:** `pokergameserver/.../persistence/repository/BanRepository.java`

```java
public interface BanRepository extends JpaRepository<BanEntity, Long> {
    List<BanEntity> findByProfileId(Long profileId);
    List<BanEntity> findByEmail(String email);
}
```

**File:** `pokergameserver/.../service/BanService.java`

```java
@Service
public class BanService {
    private final BanRepository banRepository;

    /**
     * Check if a login attempt should be blocked.
     * Checks profile ID and email against active bans.
     */
    public Optional<BanEntity> checkBan(Long profileId, String email) {
        return Stream.concat(
                banRepository.findByProfileId(profileId).stream(),
                banRepository.findByEmail(email).stream()
            )
            .filter(BanEntity::isActive)
            .findFirst();
    }
}
```

**Integration in AuthController login flow:** After finding the profile, call `banService.checkBan(profile.getId(), profile.getEmail())`. If any active ban found, return login failure with "This account has been banned".

### 2.2g. Auth & CORS properties

```java
@ConfigurationProperties(prefix = "game.server.jwt")
public record JwtProperties(
    String privateKeyPath,     // Path to RSA private key PEM (null = validation-only mode)
    String publicKeyPath,      // Path to RSA public key PEM (required)
    long expiration,           // Default: 86400000 (24h)
    long rememberMeExpiration  // Default: 2592000000 (30 days)
) {}
```

**Configuration examples:**

```properties
# WAN server (issuing mode — has both keys)
game.server.jwt.private-key-path=/data/poker/jwt-private.pem
game.server.jwt.public-key-path=/data/poker/jwt-public.pem

# Embedded game server (validation-only mode — public key only)
game.server.jwt.public-key-path=/data/poker/jwt-public.pem
# private-key-path not set → validation-only mode

# Practice mode (no auth needed)
game.server.auth.mode=local
```

CORS configured via `game.server.cors.allowed-origins` property (same pattern as existing `api` module's `cors.allowed-origins`). Defaults to `http://localhost:3000,http://localhost:8080`.

---

## Phase 2.3: Game Management REST API

### 2.3a. GameController

**File:** `pokergameserver/.../controller/GameController.java`

All endpoints under `/api/v1/games/`.

**Game CRUD:**
```
POST   /api/v1/games                    — Create new game
GET    /api/v1/games                    — List games (filter by status, hostingType)
GET    /api/v1/games/{gameId}           — Get game details
DELETE /api/v1/games/{gameId}           — Cancel game (owner only)
```

**Game lifecycle:**
```
POST   /api/v1/games/{gameId}/join      — Join a game
POST   /api/v1/games/{gameId}/leave     — Leave a game
POST   /api/v1/games/{gameId}/start     — Start the game (owner only)
POST   /api/v1/games/{gameId}/pause     — Pause (owner only)
POST   /api/v1/games/{gameId}/resume    — Resume (owner only)
```

**Game data:**
```
GET    /api/v1/games/{gameId}/players   — List players in game
GET    /api/v1/games/{gameId}/events    — Get game event log (with ?since=sequenceNum)
```

### 2.3b. Request DTOs

**CreateGameRequest:**
```java
public record CreateGameRequest(
    GameConfig config,                 // Clean server-side tournament configuration
    boolean isPrivate,
    String password                    // null if not private
) {}
```

Note: `GameConfig` already includes `name` and `description`, so the request wraps it with privacy settings only.

**JoinGameRequest:**
```java
public record JoinGameRequest(
    String password                    // null if game is not private
) {}
```

### 2.3c. Response DTOs

**GameResponse:**
```java
public record GameResponse(
    String gameId,
    String status,                     // GameInstanceState name
    String hostingType,                // "SERVER" or "COMMUNITY"
    OwnerInfo owner,
    int playerCount,
    GameConfig config,                 // Full game configuration
    Instant createdAt,
    Instant startedAt,
    Instant completedAt
) {
    public record OwnerInfo(long profileId, String name) {}
}
```

**GameListResponse:**
```java
public record GameListResponse(
    List<GameSummary> games,
    int totalCount
) {
    public record GameSummary(
        String gameId,
        String name,
        String status,
        String hostingType,
        String ownerName,
        int playerCount,
        int maxPlayers,
        Instant createdAt
    ) {}
}
```

**PlayerListResponse:**
```java
public record PlayerListResponse(
    List<PlayerInfo> players
) {
    public record PlayerInfo(
        long profileId,
        String name,
        boolean isAI,
        boolean connected,
        int skillLevel              // Only for AI players
    ) {}
}
```

**GameEventListResponse:**
```java
public record GameEventListResponse(
    String gameId,
    List<EventInfo> events,
    long lastSequenceNumber
) {
    public record EventInfo(
        long sequenceNumber,
        String eventType,
        Instant timestamp,
        Object eventData            // Serialized GameEvent
    ) {}
}
```

### 2.3d. GameService

**File:** `pokergameserver/.../service/GameService.java`

Orchestrates between REST controllers, `GameInstanceManager` (in-memory), and database persistence.

```java
@Service
public class GameService {
    private final GameInstanceManager instanceManager;
    private final GameInstanceRepository instanceRepo;

    public GameResponse createGame(CreateGameRequest request, long ownerProfileId, String ownerName) {
        // 1. Validate GameConfig
        request.config().validate();

        // 2. Create in-memory game instance (GameConfig passed directly — no TournamentProfile)
        GameInstance instance = instanceManager.createGame(ownerProfileId, request.config());
        instance.transitionToWaitingForPlayers();

        // 3. Persist to database (GameConfig stored as JSON in profileData)
        instanceRepo.save(GameInstanceEntity.fromInstance(instance));

        // 4. Return response
        return GameResponse.from(instance);
    }

    public GameResponse startGame(String gameId, long requesterId) {
        instanceManager.startGame(gameId, requesterId);
        GameInstance instance = instanceManager.getGame(gameId);

        // Update database status
        instanceRepo.updateStatus(gameId, instance.getState().name(), Instant.now());

        return GameResponse.from(instance);
    }

    // ... join, leave, pause, resume, cancel follow same pattern
}
```

### 2.3e. Error handling

**File:** `pokergameserver/.../controller/GameServerExceptionHandler.java`

```java
@RestControllerAdvice
public class GameServerExceptionHandler {

    @ExceptionHandler(GameServerException.class)
    public ResponseEntity<ErrorResponse> handleGameServerException(GameServerException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(new ErrorResponse(e.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(IllegalStateException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(new ErrorResponse(e.getMessage()));
    }
}
```

---

## Phase 2.3-extra: Profile Management API

Profile CRUD endpoints for player registration, profile updates, and password management.

### 2.3f. ProfileController

**File:** `pokergameserver/.../controller/ProfileController.java`

```
POST   /api/v1/profiles              — Register new profile (public)
GET    /api/v1/profiles/me            — Get own profile (authenticated)
GET    /api/v1/profiles/{id}          — Get profile by ID (public info only)
PUT    /api/v1/profiles/me            — Update own profile (authenticated)
PUT    /api/v1/profiles/me/password   — Change password (authenticated)
DELETE /api/v1/profiles/me            — Retire profile (authenticated, soft delete)
```

### 2.3g. Profile DTOs

**CreateProfileRequest:**
```java
public record CreateProfileRequest(
    @NotBlank String name,             // Unique, 3-32 chars
    @NotBlank @Email String email,
    @NotBlank String password          // Min 6 chars
) {}
```

**ProfileResponse:**
```java
public record ProfileResponse(
    long id,
    String name,
    String email,
    String uuid,
    boolean retired,
    Instant createdAt,
    Instant modifiedAt
) {
    public static ProfileResponse from(OnlineProfile profile) { ... }

    // Public view — omits email for other users
    public static ProfileResponse publicView(OnlineProfile profile) { ... }
}
```

**UpdateProfileRequest:**
```java
public record UpdateProfileRequest(
    @Email String email                // Only email is updatable (name is identity)
) {}
```

**ChangePasswordRequest:**
```java
public record ChangePasswordRequest(
    @NotBlank String currentPassword,
    @NotBlank String newPassword       // Min 6 chars
) {}
```

### 2.3h. ProfileService

**File:** `pokergameserver/.../service/ProfileService.java`

```java
@Service
public class ProfileService {
    private final OnlineProfileRepository profileRepo;

    public OnlineProfile register(CreateProfileRequest request) {
        // 1. Validate name uniqueness
        if (profileRepo.findByName(request.name()).isPresent()) {
            throw new GameServerException("Profile name already taken");
        }
        // 2. Validate email format
        // 3. Hash password with BCrypt
        // 4. Generate UUID
        // 5. Create and save OnlineProfile
        // 6. Return created profile
    }

    public OnlineProfile getProfile(Long id) { ... }

    public OnlineProfile updateProfile(Long id, UpdateProfileRequest request) { ... }

    public void changePassword(Long id, ChangePasswordRequest request) {
        // 1. Verify current password
        // 2. Hash new password
        // 3. Update profile
    }

    public void retireProfile(Long id) {
        // Soft delete — set isRetired = true
    }
}
```

---

## Phase 2.4: GameConfig — Clean Server-Side Tournament Configuration

The legacy `TournamentProfile` class uses `DMTypedHashMap` internally and is deeply tied to DD Poker's `BaseProfile`/`ConfigManager` framework. Rather than wrangle with that, we create a **clean `GameConfig` model** that IS the server's tournament configuration — used as both the REST API contract and the internal model.

**Key decision:** The server never touches `TournamentProfile`. When the desktop client is adapted (M4), it converts its internal `TournamentProfile` to `GameConfig` for API calls. The existing desktop UI stays the same — only the data layer underneath changes.

### 2.4a. GameConfig

**File:** `pokergameserver/.../GameConfig.java`

This record is the canonical tournament configuration for the server. It serves triple duty:
1. **REST API contract** — JSON request/response body for game creation
2. **Internal model** — `GameInstance` and `ServerTournamentContext` consume it directly
3. **Database storage** — serialized as JSON in `GameInstanceEntity.profileData`

```java
public record GameConfig(
    // ── Display ──
    String name,
    String description,
    String greeting,                   // Greeting message (supports ${name} variable)

    // ── Player & Table ──
    int maxPlayers,                    // 2-5625
    int maxOnlinePlayers,              // Default: 90, max: 120
    boolean fillComputer,              // Fill empty seats with AI (default: true)

    // ── Buy-in ──
    int buyIn,                         // Cost in dollars (1-1,000,000)
    int startingChips,                 // Chips per buy-in (1-1,000,000)

    // ── Blind Structure ──
    List<BlindLevel> blindStructure,   // Ordered list of levels (0-indexed)
    boolean doubleAfterLastLevel,      // Auto-double blinds beyond last level (default: true)
    String defaultGameType,            // Default for levels: "NOLIMIT_HOLDEM" etc.

    // ── Level Advancement ──
    LevelAdvanceMode levelAdvanceMode, // TIME or HANDS (default: TIME)
    int handsPerLevel,                 // 1-100, used when mode=HANDS (default: 10)
    int defaultMinutesPerLevel,        // Default duration for levels

    // ── Rebuy ──
    RebuyConfig rebuys,

    // ── Add-on ──
    AddonConfig addons,

    // ── Payout Structure ──
    PayoutConfig payout,

    // ── House Take ──
    HouseConfig house,

    // ── Bounty ──
    BountyConfig bounty,

    // ── Timeouts ──
    TimeoutConfig timeouts,

    // ── Boot Configuration ──
    BootConfig boot,

    // ── Late Registration ──
    LateRegistrationConfig lateRegistration,

    // ── Scheduled Start ──
    ScheduledStartConfig scheduledStart,

    // ── Invite / Observer ──
    InviteConfig invite,

    // ── Betting Rules ──
    BettingConfig betting,

    // ── Game Options ──
    boolean onlineActivatedOnly,       // Require activated profiles (default: true)
    boolean allowDash,                 // Allow dashboard during play (default: false)
    boolean allowAdvisor,              // Allow AI advisor during play (default: false)

    // ── AI Players ──
    List<AIPlayerConfig> aiPlayers
) {
    // ── Nested Records ──

    public record BlindLevel(
        int smallBlind,
        int bigBlind,
        int ante,                      // -1 indicates break level
        int minutes,                   // Duration (or break duration)
        boolean isBreak,
        String gameType                // "NOLIMIT_HOLDEM", "POTLIMIT_HOLDEM", "LIMIT_HOLDEM"
    ) {}

    public record RebuyConfig(
        boolean enabled,
        int cost,                      // Cost per rebuy in dollars
        int chips,                     // Chips per rebuy
        int chipCount,                 // Rebuy chip count override (default: startingChips)
        int maxRebuys,                 // Max rebuys per player (0-99)
        int lastLevel,                 // Last level allowing rebuys (0-indexed)
        String expressionType          // "LESS_THAN" or "LESS_THAN_OR_EQUAL"
    ) {}

    public record AddonConfig(
        boolean enabled,
        int cost,
        int chips,
        int level                      // Level at which addon is offered
    ) {}

    public record PayoutConfig(
        String type,                   // "SPOTS", "PERCENT", "SATELLITE"
        int spots,                     // Number of paid positions (for SPOTS, 1-560)
        int percent,                   // Percent of players paid (for PERCENT, 1-100)
        int prizePool,                 // Total prize pool in dollars
        String allocationType,         // "AUTO", "FIXED", "PERCENT", "SATELLITE"
        List<Double> spotAllocations   // Per-position payouts (% or $ depending on allocationType)
    ) {}

    public record HouseConfig(
        String cutType,                // "AMOUNT" or "PERCENT"
        int percent,                   // 0-25%
        int amount                     // Fixed dollar amount (0-9999)
    ) {}

    public record BountyConfig(
        boolean enabled,
        int amount                     // $ per knockout (0-10000)
    ) {}

    public record TimeoutConfig(
        int defaultSeconds,            // General timeout (5-120, default: 30)
        int preflopSeconds,            // 0 = use default
        int flopSeconds,               // 0 = use default
        int turnSeconds,               // 0 = use default
        int riverSeconds,              // 0 = use default
        int thinkBankSeconds           // Bank time per player (0-120, default: 15)
    ) {}

    public record BootConfig(
        boolean bootSitout,            // Boot players who sit out
        int bootSitoutCount,           // Hands before boot (5-100, default: 25)
        boolean bootDisconnect,        // Boot disconnected players (default: true)
        int bootDisconnectCount        // Hands before boot (5-100, default: 10)
    ) {}

    public record LateRegistrationConfig(
        boolean enabled,
        int untilLevel,                // Last level for late reg (1-40)
        String chipMode                // "STARTING" or "AVERAGE"
    ) {}

    public record ScheduledStartConfig(
        boolean enabled,
        Instant startTime,             // Scheduled start time
        int minPlayers                 // Minimum players before start (2-120)
    ) {}

    public record InviteConfig(
        boolean inviteOnly,            // Restrict to invited players
        List<String> invitees,         // Invited player names
        boolean observersPublic        // Allow public observation
    ) {}

    public record BettingConfig(
        int maxRaises,                 // Max raises per round (0 = unlimited)
        boolean raiseCapIgnoredHeadsUp // Ignore raise cap heads-up (default: true)
    ) {}

    public record AIPlayerConfig(
        String name,
        int skillLevel                 // 1-7 (maps to TournamentAI/V1/V2)
    ) {}
}
```

**Validation:** Use Bean Validation (`@Valid`) annotations plus a custom `validate()` method:
- `maxPlayers`: 2-5625
- `startingChips`: 1-1,000,000
- `blindStructure`: non-empty, at least 1 non-break level
- `aiPlayers[].skillLevel`: 1-7
- Timeouts: 0-120 seconds
- Boot counts: 5-100 hands

**Default values:** Records don't support defaults directly. Provide a `GameConfig.withDefaults()` factory method or use Jackson `@JsonSetter(nulls = Nulls.SKIP)` with default instances for optional nested configs. Nested configs that are null in the JSON request use sensible defaults (e.g., rebuys disabled, no bounties, 30s timeout).

### 2.4b. M1 Modification: GameInstance Accepts GameConfig

**Modify:** `GameInstance.java` — replace `TournamentProfile` parameter with `GameConfig`.

Currently M1's `GameInstance.create()` takes a `TournamentProfile` and `start()` extracts arrays from it to build `ServerTournamentContext`. With `GameConfig`, this extraction becomes trivial since `GameConfig` already has the data in clean record form:

```java
// Before (M1):
GameInstance.create(gameId, ownerProfileId, tournamentProfile, properties);
// start() extracts: profile.getSmallBlind(i), profile.getBigBlind(i), etc.

// After (M2):
GameInstance.create(gameId, ownerProfileId, gameConfig, properties);
// start() reads: config.blindStructure().get(i).smallBlind(), etc.
```

The `ServerTournamentContext` constructor stays the same (it takes arrays). The extraction logic just reads from `GameConfig` instead of `TournamentProfile`.

**Also modify:** `GameInstanceManager.createGame()` to accept `GameConfig` instead of `TournamentProfile`.

### 2.4c. Example JSON

```json
{
  "name": "Friday Night Poker",
  "description": "Weekly tournament",
  "maxPlayers": 10,
  "buyIn": 1000,
  "startingChips": 5000,
  "blindStructure": [
    { "smallBlind": 25, "bigBlind": 50, "ante": 0, "minutes": 20, "isBreak": false, "gameType": "NOLIMIT_HOLDEM" },
    { "smallBlind": 50, "bigBlind": 100, "ante": 10, "minutes": 20, "isBreak": false, "gameType": "NOLIMIT_HOLDEM" },
    { "smallBlind": 0, "bigBlind": 0, "ante": 0, "minutes": 5, "isBreak": true, "gameType": null },
    { "smallBlind": 100, "bigBlind": 200, "ante": 25, "minutes": 20, "isBreak": false, "gameType": "NOLIMIT_HOLDEM" }
  ],
  "doubleAfterLastLevel": true,
  "rebuys": { "enabled": true, "cost": 1000, "chips": 5000, "maxRebuys": 3, "lastLevel": 2 },
  "addons": { "enabled": true, "cost": 1000, "chips": 3000, "level": 3 },
  "levelAdvanceMode": "TIME",
  "handsPerLevel": 10,
  "timeoutSeconds": 30,
  "aiPlayers": [
    { "name": "Bot-Easy", "skillLevel": 2 },
    { "name": "Bot-Medium", "skillLevel": 4 },
    { "name": "Bot-Hard", "skillLevel": 6 }
  ]
}
```

---

## Phase 2.5: Database Persistence

### 2.5a. GameInstanceEntity

**File:** `pokergameserver/.../persistence/entity/GameInstanceEntity.java`

```java
@Entity
@Table(name = "game_instances")
public class GameInstanceEntity {

    @Id
    @Column(name = "game_id", length = 64)
    private String gameId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "status", nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    private GameInstanceState status;

    @Column(name = "owner_profile_id", nullable = false)
    private Long ownerProfileId;

    @Column(name = "owner_name", nullable = false)
    private String ownerName;

    @Column(name = "profile_data", nullable = false, columnDefinition = "TEXT")
    private String profileData;           // GameConfig as JSON

    @Column(name = "max_players", nullable = false)
    private int maxPlayers;

    @Column(name = "player_count", nullable = false)
    private int playerCount;

    @Column(name = "hosting_type", nullable = false, length = 20)
    private String hostingType;           // "SERVER" or "COMMUNITY"

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    // Factory method from GameInstance
    public static GameInstanceEntity fromInstance(GameInstance instance, String name) { ... }
}
```

### 2.5b. GameEventEntity

**File:** `pokergameserver/.../persistence/entity/GameEventEntity.java`

```java
@Entity
@Table(name = "game_events", indexes = {
    @Index(name = "idx_game_events_game_id", columnList = "game_id"),
    @Index(name = "idx_game_events_sequence", columnList = "game_id, sequence_number", unique = true)
})
public class GameEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "game_id", nullable = false, length = 64)
    private String gameId;

    @Column(name = "sequence_number", nullable = false)
    private long sequenceNumber;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @Column(name = "event_data", nullable = false, columnDefinition = "TEXT")
    private String eventData;             // JSON serialization of GameEvent

    @Column(name = "timestamp", nullable = false)
    private Instant timestamp;
}
```

### 2.5c. Spring Data JPA Repositories

```java
public interface GameInstanceRepository extends JpaRepository<GameInstanceEntity, String> {
    List<GameInstanceEntity> findByStatus(GameInstanceState status);
    List<GameInstanceEntity> findByHostingType(String hostingType);
    List<GameInstanceEntity> findByOwnerProfileId(Long ownerProfileId);

    @Modifying
    @Query("UPDATE GameInstanceEntity g SET g.status = :status, g.startedAt = :startedAt WHERE g.gameId = :gameId")
    void updateStatusWithStartTime(String gameId, GameInstanceState status, Instant startedAt);

    @Modifying
    @Query("UPDATE GameInstanceEntity g SET g.status = :status, g.completedAt = :completedAt WHERE g.gameId = :gameId")
    void updateStatusWithCompletionTime(String gameId, GameInstanceState status, Instant completedAt);

    @Modifying
    @Query("UPDATE GameInstanceEntity g SET g.status = :status WHERE g.gameId = :gameId")
    void updateStatus(String gameId, GameInstanceState status);

    @Modifying
    @Query("UPDATE GameInstanceEntity g SET g.playerCount = :count WHERE g.gameId = :gameId")
    void updatePlayerCount(String gameId, int count);
}

public interface GameEventRepository extends JpaRepository<GameEventEntity, Long> {
    List<GameEventEntity> findByGameIdOrderBySequenceNumberAsc(String gameId);
    List<GameEventEntity> findByGameIdAndSequenceNumberGreaterThanOrderBySequenceNumberAsc(
        String gameId, long afterSequence);
    long countByGameId(String gameId);
}
```

### 2.5d. Extract GameEventStore Interface

**Refactoring:** The M1 `GameEventStore` is a concrete in-memory class. Extract an interface so database-backed and in-memory implementations can coexist.

```java
// Interface (extracted from current class)
public interface GameEventStore {
    void append(GameEvent event);
    List<StoredEvent> getEvents();
    List<StoredEvent> getEventsSince(long afterSequence);
    String getGameId();
    long getCurrentSequenceNumber();
    void clear();
}

// Rename existing M1 class
public class InMemoryGameEventStore implements GameEventStore { ... }

// New database-backed implementation
public class DatabaseGameEventStore implements GameEventStore { ... }
```

### 2.5e. DatabaseGameEventStore

**File:** `pokergameserver/.../persistence/DatabaseGameEventStore.java`

```java
public class DatabaseGameEventStore implements GameEventStore {
    private final String gameId;
    private final GameEventRepository repository;
    private final ObjectMapper objectMapper;
    private final AtomicLong sequenceNumber;

    public DatabaseGameEventStore(String gameId, GameEventRepository repository, ObjectMapper objectMapper) {
        this.gameId = gameId;
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.sequenceNumber = new AtomicLong(
            repository.countByGameId(gameId)  // Resume sequence from existing events
        );
    }

    @Override
    public void append(GameEvent event) {
        long seq = sequenceNumber.incrementAndGet();
        GameEventEntity entity = new GameEventEntity();
        entity.setGameId(gameId);
        entity.setSequenceNumber(seq);
        entity.setEventType(event.getClass().getSimpleName());
        entity.setEventData(objectMapper.writeValueAsString(event));
        entity.setTimestamp(Instant.now());
        repository.save(entity);
    }

    @Override
    public List<StoredEvent> getEvents() {
        return repository.findByGameIdOrderBySequenceNumberAsc(gameId).stream()
            .map(this::toStoredEvent)
            .toList();
    }

    @Override
    public List<StoredEvent> getEventsSince(long afterSequence) {
        return repository.findByGameIdAndSequenceNumberGreaterThanOrderBySequenceNumberAsc(gameId, afterSequence)
            .stream()
            .map(this::toStoredEvent)
            .toList();
    }
}
```

### 2.5f. GameEventStore Factory

**File:** `pokergameserver/.../persistence/GameEventStoreFactory.java`

Auto-configured bean that creates the appropriate event store implementation.

```java
@Component
public class GameEventStoreFactory {
    private final GameEventRepository repository;  // null if no DataSource
    private final ObjectMapper objectMapper;

    public GameEventStore create(String gameId) {
        if (repository != null) {
            return new DatabaseGameEventStore(gameId, repository, objectMapper);
        }
        return new InMemoryGameEventStore(gameId);
    }
}
```

**Integration:** `GameInstance` currently creates `GameEventStore` directly in `start()`. Change to accept a `GameEventStoreFactory` and use it to create the appropriate store. This is a small modification to M1 code.

### 2.5g. Persistence auto-configuration

```java
@AutoConfiguration
@ConditionalOnClass(DataSource.class)
@EnableJpaRepositories(basePackages = "com.donohoedigital.games.poker.gameserver.persistence.repository")
@EntityScan(basePackages = {
    "com.donohoedigital.games.poker.gameserver.persistence.entity",
    "com.donohoedigital.games.poker.model"
})
public class GameServerPersistenceAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public GameEventStoreFactory gameEventStoreFactory(
            @Autowired(required = false) GameEventRepository repository,
            ObjectMapper objectMapper) {
        return new GameEventStoreFactory(repository, objectMapper);
    }
}
```

---

## Phase 2.6: Tests

All phases use TDD — tests written before implementation.

### 2.6a. Auth Tests

**JwtTokenProviderTest:**
- Token generation includes username and profileId claims (RS256)
- Token validation succeeds for valid tokens (public key only)
- Token validation fails for expired tokens
- Token validation fails for tampered tokens
- Username and profileId extracted correctly
- Remember-me uses longer expiration
- Validation-only mode: validate succeeds, generate throws IllegalStateException
- Auto-generated RSA 2048-bit key pair on first startup

**JwtAuthFilterTest:**
- Extracts JWT from Authorization Bearer header
- Extracts JWT from ddpoker-token cookie
- Header takes priority over cookie
- Valid token sets SecurityContext with username and profileId
- Invalid token does not set SecurityContext
- Missing token passes through without authentication

**AuthControllerTest (MockMvc):**
- Login with valid credentials returns JWT and sets cookie
- Login with invalid password returns 200 with success=false
- Login with non-existent user returns 200 with success=false
- Login with retired profile returns 200 with success=false
- Logout clears cookie
- Me endpoint returns user info when authenticated
- Me endpoint returns not-authenticated when no token

### 2.6b. Controller Tests

**GameControllerTest (MockMvc):**
- Create game with valid profile returns 201 with game details
- Create game without auth returns 401
- List games returns public game list
- Get game details returns full game info
- Join game adds player to game
- Start game as owner succeeds
- Start game as non-owner returns 403
- Pause/resume game lifecycle
- Cancel game removes it
- Leave game removes player
- Get players returns player list
- Get events returns event log with sequence numbers
- Get events with `?since=N` returns only newer events

### 2.6c. Persistence Tests

**GameInstanceRepositoryTest:**
- Save and find game instance by ID
- Find by status returns matching games
- Update status methods work correctly
- Update player count works

**GameEventRepositoryTest:**
- Save and find events by game ID
- Events ordered by sequence number
- Find events since sequence number filters correctly

**DatabaseGameEventStoreTest:**
- Append persists to database
- getEvents() returns all events in order
- getEventsSince() returns events after specified sequence
- Sequence numbers auto-increment correctly
- Event data serialized/deserialized as JSON
- New store resumes sequence from existing events

### 2.6d. GameConfig & Ban Tests

**GameConfigTest:**
- JSON serialization produces expected format
- JSON deserialization handles all fields
- Validation rejects invalid values (maxPlayers < 2, empty blind structure, etc.)
- Default values for optional fields
- Blind structure with breaks serializes correctly
- AI player config serializes correctly

**BanServiceTest:**
- Profile ID ban blocks login
- Email ban blocks login for all accounts with that email
- Expired ban does not block login
- No ban allows login
- Multiple ban types checked (profile ID + email)

### 2.6e. Profile Tests

**ProfileControllerTest (MockMvc):**
- Register with valid data returns 201 with profile
- Register with duplicate name returns 409
- Register with duplicate email returns 409
- Register with missing fields returns 400
- Get own profile returns full profile info
- Get other profile returns public info only (no email)
- Update own email succeeds
- Change password with correct current password succeeds
- Change password with wrong current password returns 403
- Retire profile soft-deletes (profile still exists, flagged retired)
- Retired profile cannot login

**ProfileServiceTest:**
- Register creates profile with hashed password
- Register generates UUID
- getProfile returns profile by ID
- updateProfile updates email
- changePassword verifies current password before updating
- retireProfile sets retired flag

### 2.6f. Integration Test

**GameLifecycleIntegrationTest** (full Spring Boot test):
1. POST `/api/v1/profiles` → register new profile
2. POST `/api/v1/auth/login` → get JWT
3. POST `/api/v1/games` with JWT → create game
4. GET `/api/v1/games` → verify game listed
5. POST `/api/v1/games/{id}/join` → add player
6. POST `/api/v1/games/{id}/start` → start game (AI players fill rest)
7. GET `/api/v1/games/{id}` → verify IN_PROGRESS
8. GET `/api/v1/games/{id}/events` → verify events logged to DB
9. Wait for game completion (AI-only)
10. GET `/api/v1/games/{id}` → verify COMPLETED
11. Verify `game_events` table has complete event log

---

## Package Structure

```
pokergameserver/src/main/java/com/donohoedigital/games/poker/gameserver/
├── (existing M1 classes — GameInstance, GameInstanceManager, etc.)
├── GameConfig.java                      (Clean server-side tournament config — replaces TournamentProfile)
├── auth/
│   ├── JwtTokenProvider.java
│   ├── JwtAuthFilter.java
│   ├── JwtProperties.java
│   ├── GameServerSecurityAutoConfiguration.java
│   └── AuthenticatedUser.java           (Principal record: username + profileId)
├── controller/
│   ├── AuthController.java
│   ├── GameController.java
│   ├── ProfileController.java
│   └── GameServerExceptionHandler.java
├── dto/
│   ├── LoginRequest.java
│   ├── AuthResponse.java
│   ├── CreateGameRequest.java
│   ├── JoinGameRequest.java
│   ├── GameResponse.java
│   ├── GameListResponse.java
│   ├── PlayerListResponse.java
│   ├── GameEventListResponse.java
│   ├── CreateProfileRequest.java
│   ├── ProfileResponse.java
│   ├── UpdateProfileRequest.java
│   ├── ChangePasswordRequest.java
│   └── ErrorResponse.java
├── persistence/
│   ├── entity/
│   │   ├── GameInstanceEntity.java
│   │   ├── GameEventEntity.java
│   │   └── BanEntity.java
│   ├── repository/
│   │   ├── OnlineProfileRepository.java
│   │   ├── BanRepository.java
│   │   ├── GameInstanceRepository.java
│   │   └── GameEventRepository.java
│   ├── DatabaseGameEventStore.java
│   ├── GameEventStoreFactory.java
│   └── GameServerPersistenceAutoConfiguration.java
├── service/
│   ├── GameService.java
│   ├── ProfileService.java
│   └── BanService.java
└── GameServerWebAutoConfiguration.java
```

---

## Files Summary

### New Files (38)

| File | Description |
|------|-------------|
| **Game Config (1)** | |
| `GameConfig.java` | Clean server-side tournament configuration (replaces TournamentProfile) |
| **Auth (5)** | |
| `auth/JwtTokenProvider.java` | JWT generation/validation with RS256 asymmetric keys |
| `auth/JwtAuthFilter.java` | Cookie + Bearer header extraction |
| `auth/JwtProperties.java` | JWT + CORS configuration properties |
| `auth/GameServerSecurityAutoConfiguration.java` | Security filter chain + CORS auto-config |
| `auth/AuthenticatedUser.java` | Principal record (username, profileId) |
| **Controllers (4)** | |
| `controller/AuthController.java` | Login/logout/me endpoints |
| `controller/GameController.java` | Game CRUD and lifecycle |
| `controller/ProfileController.java` | Profile registration, update, password, retire |
| `controller/GameServerExceptionHandler.java` | REST error handling |
| **DTOs (13)** | |
| `dto/LoginRequest.java` | Login credentials |
| `dto/AuthResponse.java` | Auth response |
| `dto/CreateGameRequest.java` | Wraps GameConfig + privacy settings |
| `dto/JoinGameRequest.java` | Join game payload |
| `dto/GameResponse.java` | Single game details (includes GameConfig) |
| `dto/GameListResponse.java` | Game listing |
| `dto/PlayerListResponse.java` | Players in a game |
| `dto/GameEventListResponse.java` | Event log |
| `dto/CreateProfileRequest.java` | Registration fields (name, email, password) |
| `dto/ProfileResponse.java` | Profile response (full + public view) |
| `dto/UpdateProfileRequest.java` | Profile update fields |
| `dto/ChangePasswordRequest.java` | Password change (current + new) |
| `dto/ErrorResponse.java` | Error envelope |
| **Persistence (8)** | |
| `persistence/entity/GameInstanceEntity.java` | JPA entity for game instances |
| `persistence/entity/GameEventEntity.java` | JPA entity for game events |
| `persistence/entity/BanEntity.java` | Ban tracking (profile ID, email) |
| `persistence/repository/OnlineProfileRepository.java` | Profile lookup (auth + CRUD) |
| `persistence/repository/BanRepository.java` | Ban lookup by profile ID, email |
| `persistence/repository/GameInstanceRepository.java` | Game instance CRUD |
| `persistence/repository/GameEventRepository.java` | Event log CRUD |
| `persistence/DatabaseGameEventStore.java` | DB-backed event store |
| **Service (3)** | |
| `service/GameService.java` | Game orchestration layer |
| `service/ProfileService.java` | Profile registration, update, password, retire |
| `service/BanService.java` | Ban checking across profile ID + email |
| **Auto-config (3)** | |
| `GameServerWebAutoConfiguration.java` | Web auto-config (controllers) |
| `persistence/GameServerPersistenceAutoConfiguration.java` | Persistence auto-config |
| `persistence/GameEventStoreFactory.java` | Event store factory |

### Modified Files (6)

| File | Change |
|------|--------|
| `pokergameserver/pom.xml` | Add web, security, data-jpa, JJWT, jBCrypt, H2 dependencies |
| `GameServerAutoConfiguration.java` | Register GameEventStoreFactory bean |
| `GameEventStore.java` | Extract to interface |
| `GameInstance.java` | Accept `GameConfig` instead of `TournamentProfile`; accept `GameEventStoreFactory` |
| `GameInstanceManager.java` | Accept `GameConfig` instead of `TournamentProfile` |
| `META-INF/spring/...AutoConfiguration.imports` | Register new auto-configurations |

### New Test Files (~14)

| File | Tests |
|------|-------|
| `GameConfigTest.java` | JSON serialization, validation, defaults |
| `auth/JwtTokenProviderTest.java` | Token generation/validation (RS256, issuing/validation-only modes) |
| `auth/JwtAuthFilterTest.java` | Filter extraction logic |
| `controller/AuthControllerTest.java` | Auth endpoint MockMvc tests (incl. ban check) |
| `controller/GameControllerTest.java` | Game endpoint MockMvc tests |
| `controller/ProfileControllerTest.java` | Profile CRUD MockMvc tests |
| `persistence/GameInstanceRepositoryTest.java` | Repository CRUD |
| `persistence/GameEventRepositoryTest.java` | Event repository |
| `persistence/DatabaseGameEventStoreTest.java` | DB event store |
| `persistence/BanRepositoryTest.java` | Ban lookup by profile ID, email |
| `service/BanServiceTest.java` | Ban checking logic (profile ID + email) |
| `service/ProfileServiceTest.java` | Profile registration, update, password, retire |
| `GameLifecycleIntegrationTest.java` | Full end-to-end |

---

## Development Methodology: Test-Driven Development

**Tests are written BEFORE implementation for all steps.** Each step follows the red-green-refactor cycle:

1. **Red:** Write failing tests that define the expected behavior
2. **Green:** Write the minimum implementation to make tests pass
3. **Refactor:** Clean up while keeping tests green

### TDD Order by Step

The implementation order is designed so each step's tests can be written and run independently:

| Order | Step | TDD Approach |
|-------|------|-------------|
| 1 | Dependencies + auto-config | No tests — Maven config and auto-config wiring only |
| 2 | **GameConfig** (2.4a) | Write GameConfigTest first — JSON serialization round-trip, validation constraints, default values for optional nested configs |
| 3 | **GameConfig → M1 integration** (2.4b) | Modify existing M1 tests to use GameConfig instead of TournamentProfile. Tests should pass with new parameter type. |
| 4 | **JPA entities** (2.5a-b) | Write repository tests first with `@DataJpaTest` — save/find/query for GameInstanceEntity, GameEventEntity, BanEntity |
| 5 | **GameEventStore interface** (2.5d) | Rename existing tests, add interface compliance tests that both InMemory and Database implementations must pass |
| 6 | **DatabaseGameEventStore** (2.5e) | Write DatabaseGameEventStoreTest first — append persists to DB, getEvents returns in order, getEventsSince filters, sequence resume |
| 7 | **JwtTokenProvider** (2.2a) | Write JwtTokenProviderTest first — **key TDD target**. Test issuing mode (generate + validate), validation-only mode (validate only, generate throws), expired tokens, tampered tokens, claim extraction |
| 8 | **JwtAuthFilter** (2.2b) | Write JwtAuthFilterTest first — Bearer header extraction, cookie extraction, header priority, SecurityContext population |
| 9 | **BanService** (2.2f-extra) | Write BanServiceTest first — profile ID ban, email ban, expired ban, no ban |
| 10 | **AuthController** (2.2d) | Write AuthControllerTest first (MockMvc) — login success, wrong password, banned user, retired profile, logout clears cookie, me endpoint |
| 11 | **ProfileService** (2.3-extra) | Write ProfileServiceTest first — registration with BCrypt, duplicate name/email rejection, update, password change, retire |
| 12 | **ProfileController** (2.3-extra) | Write ProfileControllerTest first (MockMvc) — all CRUD endpoints with auth |
| 13 | **GameService** (2.3d) | Write GameServiceTest first — create persists to DB, start updates status, join/leave update player count |
| 14 | **GameController** (2.3a) | Write GameControllerTest first (MockMvc) — all endpoints with auth and owner checks |
| 15 | **Integration test** (2.6f) | Full end-to-end: register → login → create game → join → start → play → complete |

### TDD Notes for JWT Auth

JwtTokenProvider is the most critical auth component. TDD by building up incrementally:

1. **Test key generation:** Auto-generate RSA key pair, verify both keys present
2. **Test issuing mode:** Generate token, validate returns true, extract claims match
3. **Test validation-only mode:** Load only public key, validate WAN-issued token succeeds, generate throws IllegalStateException
4. **Test expiration:** Token with past expiry fails validation
5. **Test tampered token:** Modified token fails validation
6. **Test remember-me:** Longer expiration used when rememberMe=true

### TDD Notes for GameConfig

GameConfig has many fields. TDD by testing in layers:

1. **Test minimal config:** Only required fields, verify defaults for optional nested configs
2. **Test full config:** All fields populated, JSON round-trip preserves values
3. **Test blind structure:** Multiple levels including breaks, verify serialization
4. **Test validation:** Each constraint (maxPlayers range, empty blinds, skill levels)
5. **Test default factory:** `GameConfig.withDefaults()` produces valid config

---

## Implementation Order

| Order | Step | Description | Depends On | Est. Effort |
|-------|------|-------------|------------|-------------|
| 1 | **2.1** | Dependencies + auto-config split | None | S |
| 2 | **2.4** | GameConfig model + M1 modifications (GameInstance/Manager accept GameConfig) | Step 1 | M |
| 3 | **2.5a-c** | JPA entities + repositories (including BanEntity) | Step 1 | M |
| 4 | **2.5d-f** | GameEventStore interface + DatabaseGameEventStore | Step 3 | M |
| 5 | **2.2a-c** | JWT auth (RS256 token provider, filter, security config, CORS) | Step 1 | L |
| 6 | **2.2d-g** | Auth controller + ban service + auth properties | Steps 3, 5 | M |
| 7 | **2.3-extra** | Profile CRUD (ProfileController, ProfileService, DTOs) | Steps 3, 5 | M |
| 8 | **2.3d** | GameService | Steps 2, 3, 4 | M |
| 9 | **2.3a-c,e** | GameController + DTOs + error handling | Steps 6, 8 | L |
| 10 | **2.6f** | Integration test | All above | M |

Steps 2 and 3 can be built in parallel (GameConfig and JPA entities have no dependencies on each other).
Steps 4 and 5 can be built in parallel (DatabaseGameEventStore and JWT auth have no dependencies on each other).
Steps 7 and 8 can be built in parallel (profile CRUD and game service have no dependencies on each other).

**Critical path:** 1 → 3 → 5 → 6 → 9 → integration test

---

## Risks

| Risk | Impact | Mitigation |
|------|--------|------------|
| `@EntityScan` conflicts with host app's JPA config | LOW | Spring Boot merges entity scan base packages. Test with both standalone and embedded configurations. |
| `spring-boot-starter-web` as optional doesn't auto-configure in tests | LOW | Test configuration explicitly includes web starter. |
| OnlineProfile JPA mapping differences between existing persistence-pokerserver.xml and Spring Data auto-config | MEDIUM | Verify OnlineProfile entity annotations are sufficient for Spring Data (they should be — standard JPA annotations). Test repository queries against existing H2 schema. |
| GameEvent JSON serialization — GameEvent subclasses may not serialize cleanly | MEDIUM | Add Jackson type information annotations. Test each event type round-trips through JSON. |
| RSA key management across deployment modes | MEDIUM | Auto-generate key pair on first startup. Clear documentation for distributing public key to embedded game servers. |
| GameConfig field additions over time | LOW | Records are forward-compatible with Jackson (unknown fields ignored by default). New fields get default values. |

---

## Design Note: Offline/LAN Play Without WAN Profile

The legacy DD Poker client supports three distinct play modes with very different auth requirements:

| Legacy Mode | Auth | Identity | Ban Check |
|-------------|------|----------|-----------|
| **Practice** | None | Local PlayerProfile or just a name | No |
| **LAN/P2P** | None — GUID only | Player name + hostname + IP | No |
| **WAN (Online)** | UUID + ValidateProfile | OnlineProfile (server DB) | Yes |

LAN games have **zero authentication** — players join with just a display name via UDP multicast discovery and direct TCP connection. No profile creation, no password, no server involvement.

### How M2's Architecture Supports This

The `pokergameserver` module is a library embedded in different host apps. M2's auth layer is designed to be **pluggable via `@ConditionalOnMissingBean`** — the host app can override any auth component.

**Three auth modes mapped to hosting scenarios:**

| Auth Mode | Config | Who Provides JWT | Use Case |
|-----------|--------|-----------------|----------|
| `full` | Private + public key | This server (WAN) | Server-hosted online games |
| `validate-only` | Public key only | WAN server (remote) | Community-hosted games |
| `local` | No keys | N/A — no JWT | Practice, LAN, self-hosted without WAN |

**In `local` mode (deferred to M4 implementation):**
- The host app provides its own `SecurityFilterChain` that permits all requests (or uses a simple `X-Player-Name` header filter)
- No `JwtTokenProvider` needed — the auto-configured one backs off
- Player identity is ephemeral — an in-memory ID + display name, not tied to an OnlineProfile
- No ban checking (no central authority)
- The desktop client can still track local stats in its `PlayerProfile` files

**Why this works without M2 changes:**
- `AuthenticatedUser` (the principal record) carries `username + profileId` — mode-agnostic
- In local mode, `profileId` is a generated transient ID (not a database FK)
- All game engine internals (`GameInstance`, `ServerPlayer`, `GameService`) work with this generic identity, never with JWT claims or OnlineProfile directly
- `GameEventStore`, `GameConfig`, and the full game lifecycle work identically regardless of auth mode

**What M4 adds for LAN support:**
- `LocalAuthProvider` — replaces JWT auth with name-based identification
- LAN discovery (multicast) and direct-connect networking
- Desktop client configures `game.server.auth.mode=local` when hosting LAN games

**No M2 action items** — the conditional bean architecture already supports this. This note documents the intent so M4 implementation can build on it.

---

## Verification

```bash
# After Phase 2.1 (dependencies):
cd code && mvn compile -pl pokergameserver

# After Phase 2.2-2.5 (all implementation):
cd code && mvn test -pl pokergameserver
cd code && mvn test -P dev   # Full regression (ensure M1 tests still pass)

# After integration test:
cd code && mvn verify -pl pokergameserver  # With coverage

# Enforcer check (no Swing dependencies):
cd code && mvn verify -pl pokergameserver  # Enforcer plugin validates
```

---

## Relationship to Next Milestones

This plan creates the **REST API and persistence layer**. It does NOT include:
- WebSocket protocol (Milestone 3) — real-time gameplay communication
- Desktop client adaptation (Milestone 4) — embedded server in Swing client
- Web client (Milestone 5) — Next.js game UI
- Game discovery unification (Milestone 6)

After M2, the game server has:
- Asymmetric JWT auth (RS256) — cross-server token validation
- Profile registration, update, password change, retire
- Ban system (profile ID + email)
- Authenticated REST API for game management
- Clean GameConfig model (replaces legacy TournamentProfile on server)
- Database-persisted event store and game instances
- Complete game lifecycle via API (create → join → start → play → complete)
- Foundation for WebSocket integration (M3 adds real-time play on top of this)
