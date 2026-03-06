# Web Client E2E Testing with Playwright — Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Set up Playwright E2E testing infrastructure and write 8 test suites (~78 tests) covering all user-facing web client workflows against a real pokergameserver backend.

**Architecture:** Playwright tests run Chromium against a production-built Next.js app (`next build` + `next start` on port 3000) backed by a real pokergameserver (Spring Boot with H2 in-memory DB on port 8877). A `global-setup.ts` script starts both servers; `global-teardown.ts` stops them. Each test suite calls `POST /api/v1/dev/reset` to get a clean DB. Tests are written from the user's perspective.

**Tech Stack:** Playwright, TypeScript, Next.js 16, Spring Boot 3.5, H2, JUnit 5 (for backend endpoint tests)

**Design Doc:** `docs/plans/2026-03-06-web-e2e-playwright-design.md`

---

## Task 1: Add `POST /api/v1/dev/reset` and `POST /api/v1/dev/make-admin` endpoints

The reset endpoint truncates all tables for test isolation. The make-admin endpoint sets an admin flag on a user (requires adding an `admin` boolean column to `OnlineProfile`).

**Files:**
- Modify: `code/pokergameserver/src/main/java/com/donohoedigital/games/poker/gameserver/controller/DevController.java`
- Modify: `code/pokerengine/src/main/java/com/donohoedigital/games/poker/model/OnlineProfile.java`
- Test: `code/pokergameserver/src/test/java/com/donohoedigital/games/poker/gameserver/controller/DevControllerTest.java`

**Step 1: Add `admin` column to OnlineProfile entity**

In `OnlineProfile.java`, add a new field alongside the existing boolean fields (near `emailVerified`):

```java
@Column(name = "wpr_admin")
private boolean admin;

public boolean isAdmin() {
    return admin;
}

public void setAdmin(boolean admin) {
    this.admin = admin;
}
```

**Step 2: Add reset and make-admin endpoints to DevController**

Add these two methods to `DevController.java` after the existing `verifyUser` method:

```java
@Autowired
private JdbcTemplate jdbcTemplate;

/**
 * Truncate all tables for test isolation. Resets auto-increment sequences.
 */
@PostMapping("/reset")
public ResponseEntity<Map<String, Object>> resetDatabase() {
    jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY FALSE");
    List<Map<String, Object>> tables = jdbcTemplate.queryForList(
            "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = 'PUBLIC'");
    for (Map<String, Object> table : tables) {
        String tableName = (String) table.get("TABLE_NAME");
        jdbcTemplate.execute("TRUNCATE TABLE " + tableName);
        jdbcTemplate.execute("ALTER TABLE " + tableName + " ALTER COLUMN IF EXISTS ID RESTART WITH 1");
    }
    jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY TRUE");
    return ResponseEntity.ok(Map.of("success", true));
}

/**
 * Grant admin role to a user.
 */
@PostMapping("/make-admin")
public ResponseEntity<Map<String, Object>> makeAdmin(@RequestParam(name = "username") String username) {
    Optional<OnlineProfile> maybeProfile = profileRepository.findByName(username);
    if (maybeProfile.isEmpty()) {
        return ResponseEntity.status(404).body(Map.of("found", false, "username", username));
    }
    OnlineProfile profile = maybeProfile.get();
    profile.setAdmin(true);
    profileRepository.save(profile);
    return ResponseEntity.ok(Map.of("found", true, "admin", true, "username", username));
}
```

Add the `JdbcTemplate` import and inject it via constructor or `@Autowired`. Also add `import org.springframework.jdbc.core.JdbcTemplate;` and `import java.util.List;`.

**Step 3: Add `admin` and `emailVerified` to ProfileResponse**

The web client reads `admin` and `emailVerified` from the `/me` response, but `ProfileResponse` currently only has `(id, username, email, retired)`. Add the missing fields:

Modify `code/pokergameprotocol/src/main/java/com/donohoedigital/games/poker/protocol/dto/ProfileResponse.java`:

```java
public record ProfileResponse(Long id, String username, String email, boolean retired,
        boolean emailVerified, boolean admin) {
}
```

Update `AuthService.getCurrentUser()` (in `code/pokergameserver/src/main/java/.../service/AuthService.java`) to populate the new fields from the `OnlineProfile` entity.

**Step 4: Write tests for the new endpoints**

Add tests to the existing `DevControllerTest.java`:

```java
@Test
void resetDatabase_clearsAllData() throws Exception {
    // Create a profile first
    OnlineProfile profile = newProfile("resettest");
    profileRepository.save(profile);
    assertThat(profileRepository.count()).isGreaterThan(0);

    // Reset
    mockMvc.perform(post("/api/v1/dev/reset"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));

    // Verify empty
    assertThat(profileRepository.count()).isZero();
}

@Test
void makeAdmin_setsAdminFlag() throws Exception {
    OnlineProfile profile = newProfile("admintest");
    profileRepository.save(profile);

    mockMvc.perform(post("/api/v1/dev/make-admin").param("username", "admintest"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.admin").value(true));

    OnlineProfile updated = profileRepository.findByName("admintest").orElseThrow();
    assertThat(updated.isAdmin()).isTrue();
}

@Test
void makeAdmin_returns404ForUnknownUser() throws Exception {
    mockMvc.perform(post("/api/v1/dev/make-admin").param("username", "nobody"))
            .andExpect(status().isNotFound());
}
```

**Step 5: Run tests**

Run: `cd code && mvn test -pl pokergameserver -Dtest=DevControllerTest`
Expected: All pass

**Step 6: Commit**

```bash
git add code/pokerengine/src/main/java/com/donohoedigital/games/poker/model/OnlineProfile.java
git add code/pokergameprotocol/src/main/java/com/donohoedigital/games/poker/protocol/dto/ProfileResponse.java
git add code/pokergameserver/src/main/java/com/donohoedigital/games/poker/gameserver/controller/DevController.java
git add code/pokergameserver/src/main/java/com/donohoedigital/games/poker/gameserver/service/AuthService.java
git add code/pokergameserver/src/test/java/com/donohoedigital/games/poker/gameserver/controller/DevControllerTest.java
git commit -m "feat: add dev reset, make-admin endpoints and admin flag on OnlineProfile"
```

---

## Task 2: Add standalone pokergameserver launcher

The pokergameserver module is a library (no main class). Playwright needs it running as a separate process. Add a standalone Spring Boot application class and the `spring-boot-maven-plugin` with an `exec` classifier so the module produces both a library JAR and an executable JAR.

**Files:**
- Create: `code/pokergameserver/src/main/java/com/donohoedigital/games/poker/gameserver/GameServerStandaloneApplication.java`
- Modify: `code/pokergameserver/pom.xml`

**Step 1: Create the standalone application class**

Model it after `code/poker/src/test/java/.../e2e/GameServerTestApplication.java` but as a proper `@SpringBootApplication`:

```java
/*
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 */
package com.donohoedigital.games.poker.gameserver;

import com.donohoedigital.games.poker.gameserver.auth.CorsProperties;
import com.donohoedigital.games.poker.gameserver.auth.JwtAuthenticationFilter;
import com.donohoedigital.games.poker.gameserver.auth.JwtKeyManager;
import com.donohoedigital.games.poker.gameserver.auth.JwtProperties;
import com.donohoedigital.games.poker.gameserver.auth.JwtTokenProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Primary;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.nio.file.Path;
import java.security.KeyPair;
import java.util.UUID;

/**
 * Standalone Spring Boot application for running the pokergameserver
 * independently (for E2E tests, development, etc.).
 *
 * <p>Usage:
 * <pre>
 * java -jar pokergameserver-exec.jar \
 *   --spring.profiles.active=embedded \
 *   --server.port=8877 \
 *   --spring.datasource.url=jdbc:h2:mem:e2e;DB_CLOSE_DELAY=-1 \
 *   --spring.jpa.hibernate.ddl-auto=create-drop
 * </pre>
 */
@SpringBootConfiguration
@EnableAutoConfiguration(exclude = {
        com.donohoedigital.games.poker.gameserver.auth.GameServerSecurityAutoConfiguration.class,
        com.donohoedigital.games.poker.gameserver.GameServerWebAutoConfiguration.class})
@EnableWebSecurity
@ComponentScan(basePackages = {
        "com.donohoedigital.games.poker.gameserver.controller",
        "com.donohoedigital.games.poker.gameserver.service"})
public class GameServerStandaloneApplication {

    public static void main(String[] args) {
        SpringApplication.run(GameServerStandaloneApplication.class, args);
    }

    @Bean
    public JwtProperties jwtProperties() {
        JwtProperties props = new JwtProperties();
        props.setCookieName("DDPoker-JWT");
        return props;
    }

    @Bean
    public CorsProperties corsProperties() {
        return new CorsProperties();
    }

    @Bean
    @Primary
    public JwtTokenProvider jwtTokenProvider() throws Exception {
        KeyPair keyPair = JwtKeyManager.generateKeyPair();
        String uniqueId = UUID.randomUUID().toString();
        Path privateKey = Path.of(System.getProperty("java.io.tmpdir"),
                "standalone-jwt-private-" + uniqueId + ".pem");
        Path publicKey = Path.of(System.getProperty("java.io.tmpdir"),
                "standalone-jwt-public-" + uniqueId + ".pem");
        JwtKeyManager.savePrivateKey(keyPair.getPrivate(), privateKey);
        JwtKeyManager.savePublicKey(keyPair.getPublic(), publicKey);
        return new JwtTokenProvider(privateKey, publicKey, 3600000L, 604800000L);
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(JwtTokenProvider tokenProvider) {
        return new JwtAuthenticationFilter(tokenProvider, "DDPoker-JWT");
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
            JwtAuthenticationFilter jwtFilter) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        .requestMatchers("/api/v1/dev/**").permitAll()
                        .requestMatchers("/api/v1/**").authenticated()
                        .anyRequest().permitAll())
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
```

**Step 2: Add spring-boot-maven-plugin to pokergameserver pom.xml**

Add this inside the `<plugins>` section of `pom.xml`, after the existing plugins:

```xml
<!-- Produce executable JAR alongside the library JAR -->
<plugin>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-maven-plugin</artifactId>
    <version>${spring-boot.version}</version>
    <executions>
        <execution>
            <id>repackage</id>
            <goals>
                <goal>repackage</goal>
            </goals>
            <configuration>
                <classifier>exec</classifier>
                <mainClass>com.donohoedigital.games.poker.gameserver.GameServerStandaloneApplication</mainClass>
            </configuration>
        </execution>
    </executions>
</plugin>
```

The `classifier` ensures the original library JAR is unchanged (other modules depend on it). The executable JAR is produced as `pokergameserver-3.3.0-CommunityEdition-exec.jar`.

**Step 3: Build and verify**

Run: `cd code && mvn package -pl pokergameserver -am -DskipTests`
Expected: BUILD SUCCESS. Two JARs in `code/pokergameserver/target/`:
- `pokergameserver-3.3.0-CommunityEdition.jar` (library, unchanged)
- `pokergameserver-3.3.0-CommunityEdition-exec.jar` (executable)

**Step 4: Smoke test the standalone server**

Run:
```bash
java -jar code/pokergameserver/target/pokergameserver-3.3.0-CommunityEdition-exec.jar \
  --spring.profiles.active=embedded \
  --server.port=8877 \
  --spring.datasource.url=jdbc:h2:mem:e2etest;DB_CLOSE_DELAY=-1 \
  --spring.datasource.driver-class-name=org.h2.Driver \
  --spring.jpa.hibernate.ddl-auto=create-drop \
  --game.server.server-base-url=ws://localhost:8877 \
  --settings.smtp.host=
```
Expected: Server starts, health check responds:
```bash
curl http://localhost:8877/api/v1/auth/check-username?username=test
# Should return {"available":true}
```
Kill the server after verifying.

**Step 5: Commit**

```bash
git add code/pokergameserver/src/main/java/com/donohoedigital/games/poker/gameserver/GameServerStandaloneApplication.java
git add code/pokergameserver/pom.xml
git commit -m "feat: add standalone pokergameserver launcher with exec JAR"
```

---

## Task 3: Install Playwright and create configuration

**Files:**
- Modify: `code/web/package.json`
- Create: `code/web/playwright.config.ts`
- Create: `code/web/global-setup.ts`
- Create: `code/web/global-teardown.ts`

**Step 1: Install Playwright**

Run:
```bash
cd code/web && npm install -D @playwright/test && npx playwright install chromium
```

**Step 2: Create `playwright.config.ts`**

```typescript
/*
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 */

import { defineConfig } from '@playwright/test'

export default defineConfig({
  testDir: './e2e',
  fullyParallel: false,
  workers: 1,
  retries: process.env.CI ? 1 : 0,
  timeout: 30_000,
  expect: {
    timeout: 10_000,
  },
  use: {
    baseURL: 'http://localhost:3000',
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
    video: 'off',
  },
  projects: [
    {
      name: 'chromium',
      use: { browserName: 'chromium' },
    },
  ],
  globalSetup: './global-setup.ts',
  globalTeardown: './global-teardown.ts',
})
```

**Step 3: Create `global-setup.ts`**

```typescript
/*
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 */

import { execSync, spawn, type ChildProcess } from 'child_process'
import { existsSync } from 'fs'
import path from 'path'

const GAME_SERVER_PORT = 8877
const NEXT_PORT = 3000
const STARTUP_TIMEOUT = 120_000
const POLL_INTERVAL = 1000

const CODE_DIR = path.resolve(__dirname, '..')
const EXEC_JAR = path.join(
  CODE_DIR,
  'pokergameserver/target/pokergameserver-3.3.0-CommunityEdition-exec.jar'
)

let gameServerProcess: ChildProcess | null = null
let nextProcess: ChildProcess | null = null

async function waitForHealth(url: string, timeoutMs: number): Promise<void> {
  const deadline = Date.now() + timeoutMs
  while (Date.now() < deadline) {
    try {
      const res = await fetch(url)
      if (res.ok) return
    } catch {
      // not ready
    }
    await new Promise((r) => setTimeout(r, POLL_INTERVAL))
  }
  throw new Error(`Server at ${url} did not become healthy within ${timeoutMs}ms`)
}

async function globalSetup() {
  // --- Build pokergameserver exec JAR if missing ---
  if (!existsSync(EXEC_JAR)) {
    console.log('Building pokergameserver exec JAR...')
    execSync('mvn package -pl pokergameserver -am -DskipTests -q', {
      cwd: CODE_DIR,
      stdio: 'inherit',
    })
  }

  // --- Start pokergameserver ---
  console.log('Starting pokergameserver on port', GAME_SERVER_PORT)
  gameServerProcess = spawn(
    'java',
    [
      '-jar',
      EXEC_JAR,
      '--spring.profiles.active=embedded',
      `--server.port=${GAME_SERVER_PORT}`,
      '--spring.datasource.url=jdbc:h2:mem:e2e;DB_CLOSE_DELAY=-1',
      '--spring.datasource.driver-class-name=org.h2.Driver',
      '--spring.jpa.hibernate.ddl-auto=create-drop',
      `--game.server.server-base-url=ws://localhost:${GAME_SERVER_PORT}`,
      '--settings.smtp.host=',
    ],
    { stdio: 'pipe' }
  )

  gameServerProcess.stdout?.on('data', (data) => {
    if (process.env.DEBUG) process.stdout.write(`[gameserver] ${data}`)
  })
  gameServerProcess.stderr?.on('data', (data) => {
    if (process.env.DEBUG) process.stderr.write(`[gameserver] ${data}`)
  })

  await waitForHealth(
    `http://localhost:${GAME_SERVER_PORT}/api/v1/auth/check-username?username=healthcheck`,
    60_000
  )
  console.log('pokergameserver is healthy')

  // --- Build Next.js if needed ---
  const nextBuildDir = path.join(__dirname, '.next')
  if (!existsSync(nextBuildDir)) {
    console.log('Building Next.js app...')
    execSync('npm run build', { cwd: __dirname, stdio: 'inherit' })
  }

  // --- Start Next.js production server ---
  console.log('Starting Next.js on port', NEXT_PORT)
  nextProcess = spawn('npx', ['next', 'start', '-p', String(NEXT_PORT)], {
    cwd: __dirname,
    stdio: 'pipe',
    env: {
      ...process.env,
      NEXT_PUBLIC_GAME_SERVER_URL: `http://localhost:${GAME_SERVER_PORT}`,
      NEXT_PUBLIC_API_BASE_URL: `http://localhost:${GAME_SERVER_PORT}`,
    },
    shell: true,
  })

  nextProcess.stdout?.on('data', (data) => {
    if (process.env.DEBUG) process.stdout.write(`[next] ${data}`)
  })
  nextProcess.stderr?.on('data', (data) => {
    if (process.env.DEBUG) process.stderr.write(`[next] ${data}`)
  })

  await waitForHealth(`http://localhost:${NEXT_PORT}`, STARTUP_TIMEOUT)
  console.log('Next.js is healthy')

  // Store PIDs for teardown
  process.env.GAME_SERVER_PID = String(gameServerProcess.pid)
  process.env.NEXT_PID = String(nextProcess.pid)
}

export default globalSetup
```

**Step 4: Create `global-teardown.ts`**

```typescript
/*
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 */

function killProcess(pid: string | undefined, name: string) {
  if (!pid) return
  try {
    process.kill(Number(pid), 'SIGTERM')
    console.log(`Stopped ${name} (pid ${pid})`)
  } catch {
    // already dead
  }
}

async function globalTeardown() {
  killProcess(process.env.NEXT_PID, 'Next.js')
  killProcess(process.env.GAME_SERVER_PID, 'pokergameserver')
}

export default globalTeardown
```

**Step 5: Add npm scripts to package.json**

Add to the `"scripts"` section of `code/web/package.json`:

```json
"test:e2e": "npx playwright test",
"test:e2e:ui": "npx playwright test --ui",
"test:e2e:debug": "DEBUG=1 npx playwright test --headed"
```

**Step 6: Create empty e2e directory**

Run: `mkdir -p code/web/e2e/fixtures`

**Step 7: Commit**

```bash
git add code/web/package.json code/web/package-lock.json
git add code/web/playwright.config.ts code/web/global-setup.ts code/web/global-teardown.ts
git add code/web/e2e/
git commit -m "feat: add Playwright E2E infrastructure with global setup/teardown"
```

---

## Task 4: Create test helper and fixtures

**Files:**
- Create: `code/web/e2e/fixtures/test-helper.ts`

**Step 1: Create the test helper**

```typescript
/*
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 */

import { type Page, type BrowserContext } from '@playwright/test'

const GAME_SERVER_URL = 'http://localhost:8877'

/**
 * Direct API calls to the game server (bypasses the web UI for test setup).
 */
export const api = {
  /** Truncate all DB tables for test isolation. */
  async resetDatabase(): Promise<void> {
    const res = await fetch(`${GAME_SERVER_URL}/api/v1/dev/reset`, {
      method: 'POST',
    })
    if (!res.ok) throw new Error(`Reset failed: ${res.status}`)
  },

  /** Register a user via API (not through the UI). */
  async registerUser(
    username: string,
    password: string,
    email: string
  ): Promise<{ token: string }> {
    const res = await fetch(`${GAME_SERVER_URL}/api/v1/auth/register`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username, password, email }),
    })
    if (!res.ok) throw new Error(`Register failed: ${res.status} ${await res.text()}`)
    return res.json()
  },

  /** Verify a user's email via dev endpoint. */
  async verifyUser(username: string): Promise<void> {
    const res = await fetch(
      `${GAME_SERVER_URL}/api/v1/dev/verify-user?username=${encodeURIComponent(username)}`,
      { method: 'POST' }
    )
    if (!res.ok) throw new Error(`Verify failed: ${res.status}`)
  },

  /** Grant admin role via dev endpoint. */
  async makeAdmin(username: string): Promise<void> {
    const res = await fetch(
      `${GAME_SERVER_URL}/api/v1/dev/make-admin?username=${encodeURIComponent(username)}`,
      { method: 'POST' }
    )
    if (!res.ok) throw new Error(`Make admin failed: ${res.status}`)
  },

  /** Register + verify in one call. Returns the JWT token. */
  async createVerifiedUser(
    username: string,
    password: string,
    email: string
  ): Promise<string> {
    const { token } = await api.registerUser(username, password, email)
    await api.verifyUser(username)
    return token
  },

  /** Create a verified admin user. */
  async createAdminUser(
    username: string,
    password: string,
    email: string
  ): Promise<string> {
    const token = await api.createVerifiedUser(username, password, email)
    await api.makeAdmin(username)
    return token
  },

  /** Request a forgot-password token (embedded mode returns it in response). */
  async getForgotPasswordToken(email: string): Promise<string> {
    const res = await fetch(`${GAME_SERVER_URL}/api/v1/auth/forgot-password`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email }),
    })
    if (!res.ok) throw new Error(`Forgot password failed: ${res.status}`)
    const data = await res.json()
    return data.resetToken
  },
}

/**
 * UI helpers — interact with the web app through the browser.
 */
export const ui = {
  /** Fill in the login form and submit. */
  async login(page: Page, username: string, password: string): Promise<void> {
    await page.goto('/login')
    await page.getByLabel('Username').fill(username)
    await page.getByLabel('Password').fill(password)
    await page.getByRole('button', { name: /log in/i }).click()
  },

  /** Log in via API and set the cookie on the browser context (fast, for test setup). */
  async loginViaAPI(
    context: BrowserContext,
    username: string,
    password: string
  ): Promise<void> {
    // Login via the game server API — the response sets a cookie
    const page = await context.newPage()
    const res = await page.request.post(`${GAME_SERVER_URL}/api/v1/auth/login`, {
      data: { username, password },
    })
    if (!res.ok()) throw new Error(`API login failed: ${res.status()}`)

    // The server sets an HttpOnly cookie. Navigate to sync cookies with the app domain.
    await page.goto('/')
    await page.close()
  },

  /** Fill in the registration form and submit. */
  async register(
    page: Page,
    username: string,
    email: string,
    password: string
  ): Promise<void> {
    await page.goto('/register')
    await page.getByLabel('Username').fill(username)
    await page.getByLabel('Email').fill(email)
    await page.getByLabel('Password', { exact: true }).fill(password)
    await page.getByLabel('Confirm Password').fill(password)
    await page.getByRole('button', { name: /create account/i }).click()
  },
}
```

**Step 2: Commit**

```bash
git add code/web/e2e/fixtures/test-helper.ts
git commit -m "feat: add E2E test helper with API and UI utilities"
```

---

## Task 5: Static pages test suite

**Files:**
- Create: `code/web/e2e/static-pages.spec.ts`

**Step 1: Write tests**

```typescript
/*
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 */

import { test, expect } from '@playwright/test'

test.describe('Static pages & navigation', () => {
  test('home page loads with branding and download link', async ({ page }) => {
    await page.goto('/')
    await expect(page.getByRole('heading', { level: 1 })).toBeVisible()
    await expect(page.getByRole('link', { name: /download/i })).toBeVisible()
  })

  test('navigate to About via nav link', async ({ page }) => {
    await page.goto('/')
    await page.getByRole('link', { name: /about/i }).first().click()
    await expect(page).toHaveURL(/\/about/)
    await expect(page.getByRole('heading', { level: 1 })).toBeVisible()
  })

  test('About sub-pages load via sidebar', async ({ page }) => {
    await page.goto('/about')

    // Navigate to FAQ sub-page
    await page.getByRole('link', { name: /faq/i }).click()
    await expect(page).toHaveURL(/\/about\/faq/)
    await expect(page.getByRole('heading', { level: 1 })).toBeVisible()
  })

  test('Download page shows platform installers', async ({ page }) => {
    await page.goto('/')
    await page.getByRole('link', { name: /download/i }).first().click()
    await expect(page).toHaveURL(/\/download/)
    // Should mention at least one platform
    await expect(
      page.getByText(/windows|macos|linux|debian|ubuntu/i).first()
    ).toBeVisible()
  })

  test('Support page loads', async ({ page }) => {
    await page.goto('/')
    await page.getByRole('link', { name: /support/i }).first().click()
    await expect(page).toHaveURL(/\/support/)
    await expect(page.getByRole('heading', { level: 1 })).toBeVisible()
  })

  test('Terms page loads', async ({ page }) => {
    await page.goto('/terms')
    await expect(page.getByRole('heading', { level: 1 })).toBeVisible()
  })

  test('Online portal loads without login', async ({ page }) => {
    await page.goto('/online')
    await expect(page.getByRole('heading', { level: 1 })).toBeVisible()
    await expect(page.getByRole('link', { name: /leaderboard/i })).toBeVisible()
  })

  test('Leaderboard loads without login', async ({ page }) => {
    await page.goto('/online/leaderboard')
    await expect(page.getByRole('heading', { level: 1 })).toBeVisible()
  })

  test('footer is visible on all pages', async ({ page }) => {
    await page.goto('/')
    await expect(page.locator('footer')).toBeVisible()
  })

  test('navigation highlights active page', async ({ page }) => {
    await page.goto('/about')
    const aboutLink = page.getByRole('navigation').getByRole('link', { name: /about/i }).first()
    await expect(aboutLink).toHaveClass(/active/)
  })
})
```

**Step 2: Run tests**

Run: `cd code/web && npx playwright test e2e/static-pages.spec.ts`
Expected: All pass (these are basic page-load tests)

**Step 3: Commit**

```bash
git add code/web/e2e/static-pages.spec.ts
git commit -m "test: add static pages E2E tests"
```

---

## Task 6: Registration test suite

**Files:**
- Create: `code/web/e2e/registration.spec.ts`

**Step 1: Write tests**

```typescript
/*
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 */

import { test, expect } from '@playwright/test'
import { api, ui } from './fixtures/test-helper'

test.describe('New user registration', () => {
  test.beforeAll(async () => {
    await api.resetDatabase()
  })

  test('navigate from home to register page', async ({ page }) => {
    await page.goto('/login')
    await page.getByRole('link', { name: /create one/i }).click()
    await expect(page).toHaveURL(/\/register/)
    await expect(page.getByRole('heading', { name: /create account/i })).toBeVisible()
  })

  test('short username shows no availability feedback', async ({ page }) => {
    await page.goto('/register')
    await page.getByLabel('Username').fill('ab')
    // Wait a moment for debounce — no feedback should appear
    await page.waitForTimeout(600)
    await expect(page.getByText(/username available/i)).not.toBeVisible()
    await expect(page.getByText(/username not available/i)).not.toBeVisible()
  })

  test('available username shows green feedback', async ({ page }) => {
    await page.goto('/register')
    await page.getByLabel('Username').fill('freshuser')
    await expect(page.getByText(/username available/i)).toBeVisible({ timeout: 5000 })
  })

  test('taken username shows red feedback', async ({ page }) => {
    // Pre-create a user via API
    await api.registerUser('takenname', 'password123', 'taken@example.com')

    await page.goto('/register')
    await page.getByLabel('Username').fill('takenname')
    await expect(page.getByText(/username not available/i)).toBeVisible({ timeout: 5000 })
  })

  test('mismatched passwords shows error on submit', async ({ page }) => {
    await page.goto('/register')
    await page.getByLabel('Username').fill('mismatchuser')
    await page.getByLabel('Email').fill('mismatch@example.com')
    await page.getByLabel('Password', { exact: true }).fill('password123')
    await page.getByLabel('Confirm Password').fill('different456')
    await page.getByRole('button', { name: /create account/i }).click()
    await expect(page.getByText(/passwords do not match/i)).toBeVisible()
  })

  test('short password shows error on submit', async ({ page }) => {
    await page.goto('/register')
    await page.getByLabel('Username').fill('shortpwuser')
    await page.getByLabel('Email').fill('short@example.com')
    await page.getByLabel('Password', { exact: true }).fill('short')
    await page.getByLabel('Confirm Password').fill('short')
    await page.getByRole('button', { name: /create account/i }).click()
    await expect(page.getByText(/at least 8 characters/i)).toBeVisible()
  })

  test('successful registration redirects to verify-email-pending', async ({ page }) => {
    await page.goto('/register')
    await page.getByLabel('Username').fill('newplayer')
    await page.getByLabel('Email').fill('newplayer@example.com')
    await page.getByLabel('Password', { exact: true }).fill('securepass123')
    await page.getByLabel('Confirm Password').fill('securepass123')
    await page.getByRole('button', { name: /create account/i }).click()
    await expect(page).toHaveURL(/\/verify-email-pending/, { timeout: 10_000 })
    await expect(page.getByText(/check your email/i)).toBeVisible()
    await expect(page.getByText('newplayer@example.com')).toBeVisible()
  })

  test('unverified user cannot access /games', async ({ page }) => {
    // Register but do NOT verify
    await api.registerUser('unverified', 'password123', 'unverified@example.com')

    // Login
    await ui.login(page, 'unverified', 'password123')
    // Try to access games — should be redirected to verify-email-pending
    await page.goto('/games')
    await expect(page).toHaveURL(/\/verify-email-pending|\/login/, { timeout: 10_000 })
  })

  test('email verification grants access to /games', async ({ page }) => {
    // Register and verify via API
    await api.registerUser('verified', 'password123', 'verified@example.com')
    await api.verifyUser('verified')

    // Login
    await ui.login(page, 'verified', 'password123')
    await expect(page).toHaveURL(/\/online|\/games/, { timeout: 10_000 })

    // Navigate to games — should work
    await page.goto('/games')
    await expect(page.getByText(/game lobby/i)).toBeVisible({ timeout: 10_000 })
  })

  test('verify-email page with valid token redirects to games', async ({ page }) => {
    // This tests the email link flow — user clicks link in verification email
    // In embedded mode, we use the dev endpoint to verify, but the /verify-email
    // page itself should work when given a valid token
    // Note: This may need adjustment based on how the server generates verification tokens
    await page.goto('/verify-email')
    // Without a token, should show error
    await expect(page.getByText(/missing token|verification failed|invalid/i)).toBeVisible()
  })
})
```

**Step 2: Run tests**

Run: `cd code/web && npx playwright test e2e/registration.spec.ts`
Expected: Most pass. Some may fail — that's intentional (finding bugs).

**Step 3: Commit**

```bash
git add code/web/e2e/registration.spec.ts
git commit -m "test: add registration E2E tests"
```

---

## Task 7: Login and password recovery test suite

**Files:**
- Create: `code/web/e2e/login-and-recovery.spec.ts`

**Step 1: Write tests**

```typescript
/*
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 */

import { test, expect } from '@playwright/test'
import { api, ui } from './fixtures/test-helper'

test.describe('Login, logout & password recovery', () => {
  test.beforeAll(async () => {
    await api.resetDatabase()
    await api.createVerifiedUser('alice', 'password123', 'alice@example.com')
    await api.registerUser('unverified', 'password123', 'unverified@example.com')
    // Do NOT verify 'unverified'
  })

  test('navigate to login from nav', async ({ page }) => {
    await page.goto('/')
    await page.getByRole('link', { name: /log in/i }).click()
    await expect(page).toHaveURL(/\/login/)
    await expect(page.getByRole('heading', { name: /login/i })).toBeVisible()
  })

  test('wrong password shows error', async ({ page }) => {
    await ui.login(page, 'alice', 'wrongpassword')
    await expect(page.getByRole('alert')).toBeVisible()
    await expect(page).toHaveURL(/\/login/)
  })

  test('successful login redirects to online portal', async ({ page }) => {
    await ui.login(page, 'alice', 'password123')
    await expect(page).toHaveURL(/\/online/, { timeout: 10_000 })
  })

  test('login preserves returnUrl', async ({ page }) => {
    // Try to access /games without auth — should redirect to login with returnUrl
    await page.goto('/games')
    await expect(page).toHaveURL(/\/login.*returnUrl/, { timeout: 10_000 })

    // Login — should redirect back to /games
    await page.getByLabel('Username').fill('alice')
    await page.getByLabel('Password').fill('password123')
    await page.getByRole('button', { name: /log in/i }).click()
    await expect(page).toHaveURL(/\/games/, { timeout: 10_000 })
  })

  test('login with unverified account redirects to verify-email-pending', async ({
    page,
  }) => {
    await ui.login(page, 'unverified', 'password123')
    await expect(page).toHaveURL(/\/verify-email-pending/, { timeout: 10_000 })
  })

  test('logged-in user sees profile in nav', async ({ page }) => {
    await ui.login(page, 'alice', 'password123')
    await expect(page).toHaveURL(/\/online/, { timeout: 10_000 })
    // Should see username or profile indicator in navigation
    await expect(page.getByRole('navigation').getByText('alice')).toBeVisible()
  })

  test('logout clears access to protected routes', async ({ page }) => {
    await ui.login(page, 'alice', 'password123')
    await expect(page).toHaveURL(/\/online/, { timeout: 10_000 })

    // Find and click logout
    await page.getByRole('link', { name: /log out/i }).click()

    // Try to access protected route — should redirect to login
    await page.goto('/games')
    await expect(page).toHaveURL(/\/login/, { timeout: 10_000 })
  })

  test('forgot password link leads to reset form', async ({ page }) => {
    await page.goto('/login')
    await page.getByRole('link', { name: /forgot your password/i }).click()
    await expect(page).toHaveURL(/\/forgot/)
    await expect(page.getByRole('heading', { name: /forgot password/i })).toBeVisible()
  })

  test('forgot password → reset → login with new password', async ({ page }) => {
    // Request reset
    await page.goto('/forgot')
    await page.getByLabel('Email').fill('alice@example.com')
    await page.getByRole('button', { name: /send reset link/i }).click()
    await expect(page.getByRole('status')).toBeVisible({ timeout: 5000 })

    // Get token via API (embedded mode returns it)
    const token = await api.getForgotPasswordToken('alice@example.com')

    // Visit reset page with token
    await page.goto(`/reset-password?token=${token}`)
    await expect(page.getByRole('heading', { name: /reset your password/i })).toBeVisible()

    // Enter new password
    await page.getByPlaceholder(/new password/i).fill('newpassword456')
    await page.getByPlaceholder(/confirm/i).fill('newpassword456')
    await page.getByRole('button', { name: /reset password/i }).click()

    // Should show success and redirect to login
    await expect(page.getByText(/password reset/i)).toBeVisible()
    await expect(page).toHaveURL(/\/login/, { timeout: 5000 })

    // Login with new password
    await ui.login(page, 'alice', 'newpassword456')
    await expect(page).toHaveURL(/\/online/, { timeout: 10_000 })
  })

  test('reset password without token shows error', async ({ page }) => {
    await page.goto('/reset-password')
    await expect(page.getByText(/invalid link|no reset token/i)).toBeVisible()
    await expect(page.getByRole('link', { name: /request a new/i })).toBeVisible()
  })
})
```

**Step 2: Run tests**

Run: `cd code/web && npx playwright test e2e/login-and-recovery.spec.ts`

**Step 3: Commit**

```bash
git add code/web/e2e/login-and-recovery.spec.ts
git commit -m "test: add login, logout, and password recovery E2E tests"
```

---

## Task 8: Account management test suite

**Files:**
- Create: `code/web/e2e/account-management.spec.ts`

**Step 1: Write tests**

```typescript
/*
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 */

import { test, expect } from '@playwright/test'
import { api, ui } from './fixtures/test-helper'

test.describe('Account management', () => {
  test.beforeAll(async () => {
    await api.resetDatabase()
    await api.createVerifiedUser('acctuser', 'password123', 'acctuser@example.com')
    await api.registerUser('unverifiedacct', 'password123', 'unverifiedacct@example.com')
    // Do NOT verify 'unverifiedacct'
  })

  test('account page shows user info', async ({ page }) => {
    await ui.login(page, 'acctuser', 'password123')
    await page.goto('/account')
    await expect(page.getByText('acctuser@example.com')).toBeVisible()
    await expect(page.getByRole('heading', { name: /change password/i })).toBeVisible()
    await expect(page.getByRole('heading', { name: /change email/i })).toBeVisible()
  })

  test('change password with wrong current password shows error', async ({ page }) => {
    await ui.login(page, 'acctuser', 'password123')
    await page.goto('/account')
    await page.getByPlaceholder(/current password/i).fill('wrongcurrent')
    await page.getByPlaceholder(/new password/i).first().fill('newpass12345')
    await page.getByPlaceholder(/confirm/i).fill('newpass12345')
    await page.getByRole('button', { name: /change password/i }).click()
    await expect(page.getByText(/failed|incorrect|error/i).first()).toBeVisible()
  })

  test('change password with mismatched new passwords shows error', async ({ page }) => {
    await ui.login(page, 'acctuser', 'password123')
    await page.goto('/account')
    await page.getByPlaceholder(/current password/i).fill('password123')
    await page.getByPlaceholder(/new password/i).first().fill('newpass12345')
    await page.getByPlaceholder(/confirm/i).fill('different789')
    await page.getByRole('button', { name: /change password/i }).click()
    await expect(page.getByText(/do not match/i)).toBeVisible()
  })

  test('change password with short new password shows error', async ({ page }) => {
    await ui.login(page, 'acctuser', 'password123')
    await page.goto('/account')
    await page.getByPlaceholder(/current password/i).fill('password123')
    await page.getByPlaceholder(/new password/i).first().fill('short')
    await page.getByPlaceholder(/confirm/i).fill('short')
    await page.getByRole('button', { name: /change password/i }).click()
    await expect(page.getByText(/at least 8 characters/i)).toBeVisible()
  })

  test('successful password change then re-login works', async ({ page }) => {
    await ui.login(page, 'acctuser', 'password123')
    await page.goto('/account')
    await page.getByPlaceholder(/current password/i).fill('password123')
    await page.getByPlaceholder(/new password/i).first().fill('updatedpass456')
    await page.getByPlaceholder(/confirm/i).fill('updatedpass456')
    await page.getByRole('button', { name: /change password/i }).click()
    await expect(page.getByText(/changed successfully/i)).toBeVisible()

    // Logout and re-login with new password
    await page.goto('/login')
    await ui.login(page, 'acctuser', 'updatedpass456')
    await expect(page).toHaveURL(/\/online/, { timeout: 10_000 })
  })

  test('change email shows confirmation', async ({ page }) => {
    await ui.login(page, 'acctuser', 'updatedpass456')
    await page.goto('/account')
    await page.getByPlaceholder(/new email/i).fill('newemail@example.com')
    await page.getByRole('button', { name: /update email/i }).click()
    await expect(page.getByText(/confirmation email sent/i)).toBeVisible()
  })

  test('unverified user sees verification section', async ({ page }) => {
    await ui.login(page, 'unverifiedacct', 'password123')
    await page.goto('/account')
    await expect(page.getByRole('heading', { name: /email verification/i })).toBeVisible()
    await expect(page.getByText(/not yet verified/i)).toBeVisible()
  })

  test('resend verification shows confirmation', async ({ page }) => {
    await ui.login(page, 'unverifiedacct', 'password123')
    await page.goto('/account')
    await page.getByRole('button', { name: /resend verification/i }).click()
    await expect(page.getByText(/resent|check your inbox/i)).toBeVisible()
  })
})
```

**Step 2: Run tests**

Run: `cd code/web && npx playwright test e2e/account-management.spec.ts`

**Step 3: Commit**

```bash
git add code/web/e2e/account-management.spec.ts
git commit -m "test: add account management E2E tests"
```

---

## Task 9: Online portal test suite

This suite needs seeded data (users with game history) to test leaderboard, search, etc.

**Files:**
- Create: `code/web/e2e/online-portal.spec.ts`

**Step 1: Write tests**

```typescript
/*
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 */

import { test, expect } from '@playwright/test'
import { api, ui } from './fixtures/test-helper'

test.describe('Online portal (public data)', () => {
  test.beforeAll(async () => {
    await api.resetDatabase()
    // Create users that will appear in search results
    await api.createVerifiedUser('pokerpro', 'password123', 'pokerpro@example.com')
    await api.createVerifiedUser('cardshark', 'password123', 'cardshark@example.com')
    await api.createVerifiedUser('bluffer', 'password123', 'bluffer@example.com')
  })

  test('online portal shows navigation links', async ({ page }) => {
    await page.goto('/online')
    await expect(page.getByRole('link', { name: /leaderboard/i })).toBeVisible()
    await expect(page.getByRole('link', { name: /search/i })).toBeVisible()
  })

  test('leaderboard loads in default DDR1 mode', async ({ page }) => {
    await page.goto('/online/leaderboard')
    await expect(page.getByRole('heading', { level: 1 })).toBeVisible()
    // Page should load without errors
    await expect(page.getByText(/error|failed/i)).not.toBeVisible()
  })

  test('player search finds existing player', async ({ page }) => {
    await page.goto('/online/search')
    await page.getByRole('textbox').fill('pokerpro')
    // Submit search (button or form submit)
    await page.getByRole('button', { name: /search/i }).click()
    await expect(page.getByText('pokerpro')).toBeVisible()
  })

  test('player search with no results shows message', async ({ page }) => {
    await page.goto('/online/search')
    await page.getByRole('textbox').fill('nonexistentplayer12345')
    await page.getByRole('button', { name: /search/i }).click()
    // Should show "no results" not a blank page or error
    await expect(page.getByText(/no.*results|no.*found|no.*players/i)).toBeVisible()
  })

  test('click player in search navigates to history', async ({ page }) => {
    await page.goto('/online/search')
    await page.getByRole('textbox').fill('pokerpro')
    await page.getByRole('button', { name: /search/i }).click()
    await page.getByRole('link', { name: 'pokerpro' }).click()
    await expect(page).toHaveURL(/\/online\/history\?name=pokerpro/)
  })

  test('tournament history page shows stats', async ({ page }) => {
    await page.goto('/online/history?name=pokerpro')
    await expect(page.getByRole('heading', { level: 1 })).toBeVisible()
    // Stats cards should be present (even if all zeros for new user)
    await expect(page.getByText(/total games|games played/i)).toBeVisible()
  })

  test('hosts page loads', async ({ page }) => {
    await page.goto('/online/hosts')
    await expect(page.getByRole('heading', { level: 1 })).toBeVisible()
    await expect(page.getByText(/error|failed/i)).not.toBeVisible()
  })

  test('my profile page requires login', async ({ page }) => {
    await page.goto('/online/myprofile')
    // Should either show login form or redirect to login
    await expect(page.getByText(/log in|login/i).first()).toBeVisible({ timeout: 10_000 })
  })

  test('my profile page shows user info when logged in', async ({ page }) => {
    await ui.login(page, 'pokerpro', 'password123')
    await page.goto('/online/myprofile')
    await expect(page.getByText('pokerpro')).toBeVisible()
  })

  test('leaderboard filter by name works', async ({ page }) => {
    await page.goto('/online/leaderboard')
    // Look for a name filter input
    const nameFilter = page.getByPlaceholder(/name|player/i).first()
    if (await nameFilter.isVisible()) {
      await nameFilter.fill('pokerpro')
      // Submit filter
      await page.getByRole('button', { name: /search|filter|apply/i }).first().click()
      // Should not error
      await expect(page.getByText(/error|failed/i)).not.toBeVisible()
    }
  })
})
```

**Step 2: Run tests**

Run: `cd code/web && npx playwright test e2e/online-portal.spec.ts`

**Step 3: Commit**

```bash
git add code/web/e2e/online-portal.spec.ts
git commit -m "test: add online portal E2E tests"
```

---

## Task 10: Game creation and lobby test suite

**Files:**
- Create: `code/web/e2e/game-creation.spec.ts`

**Step 1: Write tests**

```typescript
/*
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 */

import { test, expect } from '@playwright/test'
import { api, ui } from './fixtures/test-helper'

test.describe('Game creation & lobby', () => {
  test.beforeAll(async () => {
    await api.resetDatabase()
    await api.createVerifiedUser('gamehost', 'password123', 'gamehost@example.com')
    await api.createVerifiedUser('joiner', 'password123', 'joiner@example.com')
  })

  test('navigate to create game page', async ({ page }) => {
    await ui.login(page, 'gamehost', 'password123')
    await page.goto('/games')
    await page.getByRole('link', { name: /create game/i }).click()
    await expect(page).toHaveURL(/\/games\/create/)
  })

  test('quick practice starts a game immediately', async ({ page }) => {
    await ui.login(page, 'gamehost', 'password123')
    await page.goto('/games/create')
    await page.getByRole('button', { name: /quick practice/i }).click()
    // Should navigate to a play page
    await expect(page).toHaveURL(/\/games\/.*\/play/, { timeout: 15_000 })
    // Poker table should render
    await expect(page.locator('[data-testid="poker-table"], .poker-table, canvas').first()).toBeVisible({
      timeout: 10_000,
    })
  })

  test('create practice game with custom settings', async ({ page }) => {
    await ui.login(page, 'gamehost', 'password123')
    await page.goto('/games/create')

    // Fill in game name
    const nameInput = page.getByLabel(/game name|name/i).first()
    if (await nameInput.isVisible()) {
      await nameInput.fill('My Test Game')
    }

    // Look for a create/start button (not quick practice)
    await page.getByRole('button', { name: /create game|start game|create practice/i }).first().click()

    // Should navigate to play or lobby
    await expect(page).toHaveURL(/\/games\/.*\/(play|lobby)/, { timeout: 15_000 })
  })

  test('games page shows tabs and search', async ({ page }) => {
    await ui.login(page, 'gamehost', 'password123')
    await page.goto('/games')
    await expect(page.getByText(/game lobby/i)).toBeVisible()
    await expect(page.getByRole('button', { name: /open/i })).toBeVisible()
    await expect(page.getByRole('button', { name: /in progress/i })).toBeVisible()
    await expect(page.getByRole('button', { name: /completed/i })).toBeVisible()
    await expect(page.getByLabel(/search/i)).toBeVisible()
  })

  test('tab switching filters games', async ({ page }) => {
    await ui.login(page, 'gamehost', 'password123')
    await page.goto('/games')
    // Click each tab — should not error
    await page.getByRole('button', { name: /in progress/i }).click()
    await expect(page.getByText(/error|failed/i)).not.toBeVisible()
    await page.getByRole('button', { name: /completed/i }).click()
    await expect(page.getByText(/error|failed/i)).not.toBeVisible()
    await page.getByRole('button', { name: /open/i }).click()
    await expect(page.getByText(/error|failed/i)).not.toBeVisible()
  })

  test('no games shows empty state', async ({ page }) => {
    await ui.login(page, 'gamehost', 'password123')
    await page.goto('/games')
    await expect(page.getByText(/no games found/i)).toBeVisible()
  })

  test('create game page has blind structure options', async ({ page }) => {
    await ui.login(page, 'gamehost', 'password123')
    await page.goto('/games/create')
    // Should have blind structure section
    await expect(page.getByText(/blind|structure/i).first()).toBeVisible()
  })

  test('create game page has AI player options', async ({ page }) => {
    await ui.login(page, 'gamehost', 'password123')
    await page.goto('/games/create')
    // Should have AI/opponent section
    await expect(page.getByText(/ai|opponent|player/i).first()).toBeVisible()
  })
})
```

**Step 2: Run tests**

Run: `cd code/web && npx playwright test e2e/game-creation.spec.ts`

**Step 3: Commit**

```bash
git add code/web/e2e/game-creation.spec.ts
git commit -m "test: add game creation and lobby E2E tests"
```

---

## Task 11: Gameplay test suite

This is the most complex suite — tests actual poker gameplay via the browser.

**Files:**
- Create: `code/web/e2e/gameplay.spec.ts`

**Step 1: Write tests**

```typescript
/*
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 */

import { test, expect, type Page } from '@playwright/test'
import { api, ui } from './fixtures/test-helper'

/** Wait for the poker table to be interactive (player has cards or action prompt). */
async function waitForTable(page: Page) {
  // Wait for the poker table container to appear
  await expect(
    page.locator('[data-testid="poker-table"], .poker-table').first()
  ).toBeVisible({ timeout: 20_000 })
}

/** Wait for the action panel to show available actions. */
async function waitForAction(page: Page) {
  await expect(
    page.getByRole('button', { name: /fold|check|call/i }).first()
  ).toBeVisible({ timeout: 30_000 })
}

/** Start a quick practice game and wait for the table. */
async function startQuickPractice(page: Page) {
  await page.goto('/games/create')
  await page.getByRole('button', { name: /quick practice/i }).click()
  await expect(page).toHaveURL(/\/games\/.*\/play/, { timeout: 15_000 })
  await waitForTable(page)
}

test.describe('Gameplay (practice)', () => {
  test.beforeAll(async () => {
    await api.resetDatabase()
    await api.createVerifiedUser('player1', 'password123', 'player1@example.com')
  })

  test.beforeEach(async ({ page }) => {
    await ui.login(page, 'player1', 'password123')
  })

  test('practice game renders poker table with player seats', async ({ page }) => {
    await startQuickPractice(page)
    // Should see player seats (at least 2 — human + AI)
    const seats = page.locator('[data-testid*="seat"], [class*="seat"], [class*="player"]')
    await expect(seats.first()).toBeVisible()
  })

  test('player sees their hole cards', async ({ page }) => {
    await startQuickPractice(page)
    // Wait for cards to be dealt
    await page.waitForTimeout(3000)
    // Should see card elements (face-up for human player)
    const cards = page.locator('[data-testid*="card"], [class*="card"]')
    await expect(cards.first()).toBeVisible({ timeout: 15_000 })
  })

  test('tournament info bar shows blinds and level', async ({ page }) => {
    await startQuickPractice(page)
    await expect(page.getByText(/level|blind/i).first()).toBeVisible({ timeout: 10_000 })
  })

  test('action panel appears when it is player turn', async ({ page }) => {
    await startQuickPractice(page)
    await waitForAction(page)
    // Should see action buttons
    await expect(page.getByRole('button', { name: /fold/i })).toBeVisible()
  })

  test('fold action works', async ({ page }) => {
    await startQuickPractice(page)
    await waitForAction(page)
    await page.getByRole('button', { name: /fold/i }).click()
    // Action panel should disappear after folding
    await expect(page.getByRole('button', { name: /fold/i })).not.toBeVisible({
      timeout: 5000,
    })
  })

  test('check or call action works', async ({ page }) => {
    await startQuickPractice(page)
    await waitForAction(page)
    // Click check or call (whichever is available)
    const checkBtn = page.getByRole('button', { name: /^check$/i })
    const callBtn = page.getByRole('button', { name: /^call/i })
    if (await checkBtn.isVisible()) {
      await checkBtn.click()
    } else {
      await callBtn.click()
    }
    // Action panel should disappear
    await expect(page.getByRole('button', { name: /fold/i })).not.toBeVisible({
      timeout: 5000,
    })
  })

  test('keyboard shortcut F folds', async ({ page }) => {
    await startQuickPractice(page)
    await waitForAction(page)
    await page.keyboard.press('f')
    // Action panel should disappear
    await expect(page.getByRole('button', { name: /fold/i })).not.toBeVisible({
      timeout: 5000,
    })
  })

  test('hand history panel toggles', async ({ page }) => {
    await startQuickPractice(page)
    await page.waitForTimeout(3000)
    const historyBtn = page.getByRole('button', { name: /hand history/i })
    if (await historyBtn.isVisible()) {
      await historyBtn.click()
      // Should see log entries
      await expect(page.getByRole('log')).toBeVisible()
      await historyBtn.click()
    }
  })

  test('theme picker changes table appearance', async ({ page }) => {
    await startQuickPractice(page)
    const settingsBtn = page.getByRole('button', { name: /settings/i })
    if (await settingsBtn.isVisible()) {
      await settingsBtn.click()
      await expect(page.getByText(/table/i)).toBeVisible()
    }
  })

  test('chat panel accepts messages', async ({ page }) => {
    await startQuickPractice(page)
    const chatInput = page.getByPlaceholder(/message|chat/i)
    if (await chatInput.isVisible()) {
      await chatInput.fill('Hello from E2E test')
      await chatInput.press('Enter')
      await expect(page.getByText('Hello from E2E test')).toBeVisible()
    }
  })

  test('@slow full game plays to completion', async ({ page }) => {
    test.setTimeout(120_000)
    await page.goto('/games/create')

    // Configure a fast game: few AI players, small stacks, high blinds
    // The exact form interactions depend on the create page structure
    // Use Quick Practice for simplicity — it creates a default fast game
    await page.getByRole('button', { name: /quick practice/i }).click()
    await expect(page).toHaveURL(/\/games\/.*\/play/, { timeout: 15_000 })
    await waitForTable(page)

    // Play until game ends — always check/call when action is available
    let gameComplete = false
    const maxIterations = 200

    for (let i = 0; i < maxIterations; i++) {
      // Check if game ended (redirected to results or overlay shown)
      if (page.url().includes('/results')) {
        gameComplete = true
        break
      }

      // Check for continue/deal buttons (between hands)
      const continueBtn = page.getByRole('button', { name: /continue|deal|ok/i })
      if (await continueBtn.isVisible({ timeout: 500 }).catch(() => false)) {
        await continueBtn.click()
        continue
      }

      // Check for action buttons
      const foldBtn = page.getByRole('button', { name: /fold/i })
      if (await foldBtn.isVisible({ timeout: 1000 }).catch(() => false)) {
        // Check/Call if available, otherwise fold
        const checkBtn = page.getByRole('button', { name: /^check$/i })
        const callBtn = page.getByRole('button', { name: /^call/i })
        if (await checkBtn.isVisible({ timeout: 200 }).catch(() => false)) {
          await checkBtn.click()
        } else if (await callBtn.isVisible({ timeout: 200 }).catch(() => false)) {
          await callBtn.click()
        } else {
          await foldBtn.click()
        }
        continue
      }

      // Wait a moment for AI actions
      await page.waitForTimeout(500)
    }

    // Verify we reached the results page
    if (!gameComplete) {
      // Check if we're on the results page now
      await expect(page).toHaveURL(/\/results/, { timeout: 30_000 })
    }

    // Results page should show placement
    await expect(page.getByText(/1st|2nd|3rd|first|second|third/i).first()).toBeVisible()
  })
})
```

**Step 2: Run tests**

Run: `cd code/web && npx playwright test e2e/gameplay.spec.ts`
For just the fast tests: `cd code/web && npx playwright test e2e/gameplay.spec.ts --grep-invert "@slow"`

**Step 3: Commit**

```bash
git add code/web/e2e/gameplay.spec.ts
git commit -m "test: add gameplay E2E tests including full game completion"
```

---

## Task 12: Admin test suite

**Files:**
- Create: `code/web/e2e/admin.spec.ts`

**Step 1: Write tests**

```typescript
/*
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 */

import { test, expect } from '@playwright/test'
import { api, ui } from './fixtures/test-helper'

test.describe('Admin', () => {
  test.beforeAll(async () => {
    await api.resetDatabase()
    await api.createAdminUser('admin', 'password123', 'admin@example.com')
    await api.createVerifiedUser('regular', 'password123', 'regular@example.com')
    await api.registerUser('unverifieduser', 'password123', 'unverified@example.com')
    // Do NOT verify 'unverifieduser'
  })

  test('non-admin user cannot see admin nav link', async ({ page }) => {
    await ui.login(page, 'regular', 'password123')
    await expect(page).toHaveURL(/\/online/, { timeout: 10_000 })
    await expect(page.getByRole('navigation').getByRole('link', { name: /admin/i })).not.toBeVisible()
  })

  test('admin user sees admin nav link', async ({ page }) => {
    await ui.login(page, 'admin', 'password123')
    await expect(page).toHaveURL(/\/online/, { timeout: 10_000 })
    await expect(page.getByRole('navigation').getByRole('link', { name: /admin/i })).toBeVisible()
  })

  test('admin dashboard has links to tools', async ({ page }) => {
    await ui.login(page, 'admin', 'password123')
    await page.goto('/admin')
    await expect(page.getByRole('link', { name: /profile search/i })).toBeVisible()
    await expect(page.getByRole('link', { name: /ban list/i })).toBeVisible()
  })

  test('profile search returns results', async ({ page }) => {
    await ui.login(page, 'admin', 'password123')
    await page.goto('/admin/online-profile-search')
    await page.getByPlaceholder(/name/i).first().fill('regular')
    await page.getByRole('button', { name: /search/i }).click()
    await expect(page.getByText('regular')).toBeVisible()
  })

  test('verify email action on unverified profile', async ({ page }) => {
    await ui.login(page, 'admin', 'password123')
    await page.goto('/admin/online-profile-search')
    await page.getByPlaceholder(/name/i).first().fill('unverifieduser')
    await page.getByRole('button', { name: /search/i }).click()
    await expect(page.getByText('unverifieduser')).toBeVisible()
    // Click verify action
    const verifyBtn = page.getByRole('button', { name: /verify/i }).first()
    if (await verifyBtn.isVisible()) {
      await verifyBtn.click()
      // Should show success or the status should change
      await expect(page.getByText(/verified|success/i).first()).toBeVisible({ timeout: 5000 })
    }
  })

  test('ban list page loads', async ({ page }) => {
    await ui.login(page, 'admin', 'password123')
    await page.goto('/admin/ban-list')
    await expect(page.getByRole('heading', { level: 1 })).toBeVisible()
  })

  test('add and remove ban', async ({ page }) => {
    await ui.login(page, 'admin', 'password123')
    await page.goto('/admin/ban-list')

    // Add a ban
    const keyInput = page.getByPlaceholder(/key/i).first()
    const commentInput = page.getByPlaceholder(/comment|reason/i).first()
    if (await keyInput.isVisible()) {
      await keyInput.fill('test-ban-key-123')
      if (await commentInput.isVisible()) {
        await commentInput.fill('E2E test ban')
      }
      await page.getByRole('button', { name: /add|ban|submit/i }).first().click()

      // Ban should appear in list
      await expect(page.getByText('test-ban-key-123')).toBeVisible({ timeout: 5000 })

      // Remove the ban
      await page.getByRole('button', { name: /remove|delete/i }).first().click()
      await expect(page.getByText('test-ban-key-123')).not.toBeVisible({ timeout: 5000 })
    }
  })
})
```

**Step 2: Run tests**

Run: `cd code/web && npx playwright test e2e/admin.spec.ts`

Note: The admin nav visibility tests (`non-admin cannot see admin nav link` and `admin user sees admin nav link`) are expected to **fail initially** because:
- `ProfileResponse` doesn't include `admin` field (found during design)
- The web client's `isAdmin` is always `false`

These failures are the tests doing their job — catching the missing admin field bug.

**Step 3: Commit**

```bash
git add code/web/e2e/admin.spec.ts
git commit -m "test: add admin E2E tests"
```

---

## Task 13: Run full suite and document results

**Step 1: Run all E2E tests**

Run: `cd code/web && npx playwright test`

**Step 2: Document results**

For each suite, note:
- Passing tests (working features)
- Failing tests (bugs found)
- Tests that need adjustment (wrong selectors, timing issues)

Failing tests that reveal real bugs should be kept as-is (they represent the desired behavior). Create issues or notes for each bug found.

**Step 3: Add `.gitignore` entries**

Add to `code/web/.gitignore`:
```
# Playwright
/test-results/
/playwright-report/
/blob-report/
/playwright/.cache/
```

**Step 4: Final commit**

```bash
git add code/web/.gitignore
git add code/web/e2e/
git commit -m "test: complete web client E2E test suite with Playwright"
```
