# Account Management Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Implement full account lifecycle management — email verification, embedded server localhost restriction, unified auth API, account lockout, and client UX flows for both the web client and Java thin client.

**Architecture:** All authoritative auth lives in `pokergameserver` (`/api/v1/auth/*`). The legacy `api` module auth layer is deleted outright. Both clients (web + desktop) call the game server API. Email verification gates WAN features; practice mode (embedded server) remains accessible to unverified users.

**Tech Stack:** Java 25, Spring Boot 3.5.8, Spring Security, Spring Boot Mail, JPA/H2, JUnit 5, Next.js (TypeScript), Jest/RTL.

**Design doc:** `docs/plans/2026-03-03-account-management-design.md`

---

## Test commands reference

```bash
# Backend — run a single test class
mvn test -pl pokergameserver -Dtest=AuthServiceTest -P dev

# Backend — run all pokergameserver tests
mvn test -pl pokergameserver -P dev

# Backend — run pokerengine tests
mvn test -pl pokerengine -P dev

# Web client — run all tests
cd code/web && npm test -- --watchAll=false

# Web client — run single test file
cd code/web && npm test -- --testPathPattern=api.test --watchAll=false
```

---

## Task 1: OnlineProfile entity — new fields

**Files:**
- Modify: `code/pokerengine/src/main/java/com/donohoedigital/games/poker/model/OnlineProfile.java`
- Modify test: `code/pokergameserver/src/test/java/com/donohoedigital/games/poker/gameserver/persistence/repository/` — create `OnlineProfileRepositoryTest.java`

**Step 1: Add fields to OnlineProfile**

Add these fields to the `OnlineProfile` entity class after the existing fields:

```java
@Column(name = "email_verified", nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
private boolean emailVerified = false;

@Column(name = "email_verification_token", unique = true)
private String emailVerificationToken;

@Column(name = "email_verification_token_expiry")
private Long emailVerificationTokenExpiry; // epoch ms

@Column(name = "pending_email")
private String pendingEmail;

@Column(name = "failed_login_attempts", nullable = false, columnDefinition = "INTEGER DEFAULT 0")
private int failedLoginAttempts = 0;

@Column(name = "locked_until")
private Long lockedUntil; // epoch ms, null = not locked

@Column(name = "lockout_count", nullable = false, columnDefinition = "INTEGER DEFAULT 0")
private int lockoutCount = 0;
```

Add getters and setters for all seven fields using the same style as the existing ones in the file.

**Step 2: Run the existing pokerengine tests to confirm entity compiles**

```bash
mvn test -pl pokerengine -P dev
```

Expected: BUILD SUCCESS (all existing tests pass, new fields have no test yet).

**Step 3: Commit**

```bash
git add code/pokerengine/src/main/java/com/donohoedigital/games/poker/model/OnlineProfile.java
git commit -m "feat(pokerengine): add email verification and lockout fields to OnlineProfile"
```

---

## Task 2: OnlineProfileRepository — new queries

**Files:**
- Modify: `code/pokergameserver/src/main/java/com/donohoedigital/games/poker/gameserver/persistence/repository/OnlineProfileRepository.java`
- Create: `code/pokergameserver/src/test/java/com/donohoedigital/games/poker/gameserver/persistence/repository/OnlineProfileRepositoryTest.java`

**Step 1: Write the failing test**

```java
package com.donohoedigital.games.poker.gameserver.persistence.repository;

import com.donohoedigital.games.poker.gameserver.persistence.TestJpaConfiguration;
import com.donohoedigital.games.poker.model.OnlineProfile;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(TestJpaConfiguration.class)
class OnlineProfileRepositoryTest {

    @Autowired OnlineProfileRepository repo;

    @Test
    void findByEmailVerificationToken_returnsProfile() {
        OnlineProfile p = new OnlineProfile();
        p.setName("alice");
        p.setEmail("alice@example.com");
        p.setEmailVerificationToken("tok123");
        p.setEmailVerificationTokenExpiry(System.currentTimeMillis() + 86400000L);
        repo.save(p);

        Optional<OnlineProfile> found = repo.findByEmailVerificationToken("tok123");
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("alice");
    }

    @Test
    void findByEmailVerificationToken_missingToken_returnsEmpty() {
        assertThat(repo.findByEmailVerificationToken("no-such-token")).isEmpty();
    }

    @Test
    void findByPendingEmail_returnsProfile() {
        OnlineProfile p = new OnlineProfile();
        p.setName("bob");
        p.setEmail("bob@example.com");
        p.setPendingEmail("bob-new@example.com");
        repo.save(p);

        Optional<OnlineProfile> found = repo.findByPendingEmail("bob-new@example.com");
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("bob");
    }
}
```

**Step 2: Run to confirm it fails**

```bash
mvn test -pl pokergameserver -Dtest=OnlineProfileRepositoryTest -P dev
```

Expected: FAIL — `findByEmailVerificationToken` / `findByPendingEmail` methods do not exist.

**Step 3: Add query methods to OnlineProfileRepository**

```java
Optional<OnlineProfile> findByEmailVerificationToken(String token);
Optional<OnlineProfile> findByPendingEmail(String pendingEmail);
```

Spring Data JPA derives both queries from the method names — no `@Query` annotation needed.

**Step 4: Run tests to verify they pass**

```bash
mvn test -pl pokergameserver -Dtest=OnlineProfileRepositoryTest -P dev
```

Expected: PASS.

**Step 5: Commit**

```bash
git add code/pokergameserver/src/main/java/com/donohoedigital/games/poker/gameserver/persistence/repository/OnlineProfileRepository.java
git add code/pokergameserver/src/test/java/com/donohoedigital/games/poker/gameserver/persistence/repository/OnlineProfileRepositoryTest.java
git commit -m "feat(pokergameserver): add verification token and pending email queries to OnlineProfileRepository"
```

---

## Task 3: Email infrastructure — Spring Boot Mail

**Files:**
- Modify: `code/pokergameserver/pom.xml`
- Create: `code/pokergameserver/src/main/resources/application.properties`

**Step 1: Add spring-boot-starter-mail to pokergameserver pom.xml**

Add inside `<dependencies>`:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-mail</artifactId>
    <optional>true</optional>
</dependency>
```

**Step 2: Create application.properties mapping existing Docker env vars to Spring Mail**

Create `code/pokergameserver/src/main/resources/application.properties`:

```properties
# Email — maps existing Docker SMTP_* env vars to Spring Boot Mail
# When SMTP_HOST is not set, mail sending is disabled (see EmailService)
spring.mail.host=${SMTP_HOST:}
spring.mail.port=${SMTP_PORT:587}
spring.mail.username=${SMTP_USER:}
spring.mail.password=${SMTP_PASSWORD:}
spring.mail.properties.mail.smtp.auth=${SMTP_AUTH:false}
spring.mail.properties.mail.smtp.starttls.enable=${SMTP_STARTTLS_ENABLE:true}

# From address and base URL for email links
app.email.from=${SMTP_FROM:noreply@ddpoker.com}
app.base-url=${SERVER_HOST:localhost}
```

**Step 3: Run pokergameserver tests to confirm nothing is broken**

```bash
mvn test -pl pokergameserver -P dev
```

Expected: BUILD SUCCESS.

**Step 4: Commit**

```bash
git add code/pokergameserver/pom.xml code/pokergameserver/src/main/resources/application.properties
git commit -m "feat(pokergameserver): add Spring Boot Mail dependency and SMTP configuration"
```

---

## Task 4: EmailService

**Files:**
- Create: `code/pokergameserver/src/main/java/com/donohoedigital/games/poker/gameserver/service/EmailService.java`
- Create: `code/pokergameserver/src/test/java/com/donohoedigital/games/poker/gameserver/service/EmailServiceTest.java`

**Step 1: Write the failing test**

```java
package com.donohoedigital.games.poker.gameserver.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock JavaMailSender mailSender;

    EmailService emailService;

    @BeforeEach
    void setUp() {
        emailService = new EmailService(mailSender, "noreply@ddpoker.com", "https://poker.example.com");
    }

    @Test
    void sendVerificationEmail_sendsToCorrectAddress() {
        emailService.sendVerificationEmail("user@example.com", "alice", "tok123");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        SimpleMailMessage msg = captor.getValue();

        assertThat(msg.getTo()).containsExactly("user@example.com");
        assertThat(msg.getSubject()).isEqualTo("Verify your DD Poker account");
        assertThat(msg.getText()).contains("https://poker.example.com/verify-email?token=tok123");
        assertThat(msg.getText()).contains("7 days");
    }

    @Test
    void sendPasswordResetEmail_sendsToCorrectAddress() {
        emailService.sendPasswordResetEmail("user@example.com", "alice", "reset-tok");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        SimpleMailMessage msg = captor.getValue();

        assertThat(msg.getTo()).containsExactly("user@example.com");
        assertThat(msg.getSubject()).isEqualTo("Reset your DD Poker password");
        assertThat(msg.getText()).contains("https://poker.example.com/reset-password?token=reset-tok");
        assertThat(msg.getText()).contains("1 hour");
    }

    @Test
    void sendEmailChangeConfirmation_sendsToNewAddress() {
        emailService.sendEmailChangeConfirmation("new@example.com", "alice", "change-tok");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        SimpleMailMessage msg = captor.getValue();

        assertThat(msg.getTo()).containsExactly("new@example.com");
        assertThat(msg.getSubject()).isEqualTo("Confirm your new DD Poker email address");
        assertThat(msg.getText()).contains("https://poker.example.com/verify-email?token=change-tok");
    }
}
```

**Step 2: Run to confirm fail**

```bash
mvn test -pl pokergameserver -Dtest=EmailServiceTest -P dev
```

Expected: FAIL — `EmailService` class does not exist.

**Step 3: Implement EmailService**

```java
package com.donohoedigital.games.poker.gameserver.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger LOG = LogManager.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final String fromAddress;
    private final String baseUrl;

    public EmailService(JavaMailSender mailSender,
                        @Value("${app.email.from:noreply@ddpoker.com}") String fromAddress,
                        @Value("${app.base-url:localhost}") String baseUrl) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
        this.baseUrl = baseUrl;
    }

    public void sendVerificationEmail(String toEmail, String username, String token) {
        String link = baseUrl + "/verify-email?token=" + token;
        String body = "Hi " + username + ",\n\n"
                + "Please verify your DD Poker account by clicking the link below:\n\n"
                + link + "\n\n"
                + "This link expires in 7 days.\n\n"
                + "If you did not create this account, you can ignore this email.\n\n"
                + "— The DD Poker Team";
        send(toEmail, "Verify your DD Poker account", body);
    }

    public void sendPasswordResetEmail(String toEmail, String username, String token) {
        String link = baseUrl + "/reset-password?token=" + token;
        String body = "Hi " + username + ",\n\n"
                + "A password reset was requested for your DD Poker account.\n\n"
                + link + "\n\n"
                + "This link expires in 1 hour. If you did not request a reset, ignore this email.\n\n"
                + "— The DD Poker Team";
        send(toEmail, "Reset your DD Poker password", body);
    }

    public void sendEmailChangeConfirmation(String toEmail, String username, String token) {
        String link = baseUrl + "/verify-email?token=" + token;
        String body = "Hi " + username + ",\n\n"
                + "Please confirm your new DD Poker email address by clicking the link below:\n\n"
                + link + "\n\n"
                + "This link expires in 7 days. If you did not request this change, ignore this email.\n\n"
                + "— The DD Poker Team";
        send(toEmail, "Confirm your new DD Poker email address", body);
    }

    private void send(String to, String subject, String body) {
        if (mailSender == null) {
            LOG.info("SMTP not configured — email to {} subject '{}' suppressed. Body:\n{}", to, subject, body);
            return;
        }
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(fromAddress);
            msg.setTo(to);
            msg.setSubject(subject);
            msg.setText(body);
            mailSender.send(msg);
        } catch (Exception e) {
            LOG.error("Failed to send email to {} subject '{}': {}", to, subject, e.getMessage());
        }
    }
}
```

**Note:** `JavaMailSender` is only available when `spring.mail.host` is configured. The `EmailService` bean will fail to create if `spring-boot-starter-mail` auto-configuration is disabled. Add `@ConditionalOnBean(JavaMailSender.class)` or handle the null case via `@Autowired(required = false)`. The simplest approach: mark `mailSender` `@Autowired(required = false)` and null-check in `send()`.

Update the constructor signature:

```java
public EmailService(@Autowired(required = false) JavaMailSender mailSender,
                    @Value("${app.email.from:noreply@ddpoker.com}") String fromAddress,
                    @Value("${app.base-url:localhost}") String baseUrl) {
```

**Step 4: Run to verify pass**

```bash
mvn test -pl pokergameserver -Dtest=EmailServiceTest -P dev
```

Expected: PASS.

**Step 5: Commit**

```bash
git add code/pokergameserver/src/main/java/com/donohoedigital/games/poker/gameserver/service/EmailService.java
git add code/pokergameserver/src/test/java/com/donohoedigital/games/poker/gameserver/service/EmailServiceTest.java
git commit -m "feat(pokergameserver): add EmailService for verification, reset, and email change emails"
```

---

## Task 5: JwtTokenProvider — emailVerified claim + LoginResponse update

**Files:**
- Modify: `code/pokergameserver/src/main/java/com/donohoedigital/games/poker/gameserver/auth/JwtTokenProvider.java`
- Modify: `code/pokergameserver/src/main/java/com/donohoedigital/games/poker/gameserver/dto/LoginResponse.java`
- Modify test: `code/pokergameserver/src/test/java/com/donohoedigital/games/poker/gameserver/auth/JwtTokenProviderTest.java`

**Step 1: Update LoginResponse to carry emailVerified and email**

Open `LoginResponse.java` and add two fields alongside the existing ones:

```java
private boolean emailVerified;
private String email;
```

Add constructor parameter and/or builder support matching the existing style.

**Step 2: Write a failing test in JwtTokenProviderTest**

Add to the existing test class:

```java
@Test
void generateToken_encodesEmailVerifiedClaim() {
    OnlineProfile profile = new OnlineProfile();
    profile.setName("alice");
    profile.setEmailVerified(true);
    // use whatever method already exists to generate a login token
    String token = provider.generateLoginToken(profile);

    Claims claims = provider.parseToken(token);
    assertThat(claims.get("emailVerified", Boolean.class)).isTrue();
}

@Test
void generateToken_emailNotVerified_encodedAsFalse() {
    OnlineProfile profile = new OnlineProfile();
    profile.setName("bob");
    profile.setEmailVerified(false);
    String token = provider.generateLoginToken(profile);

    Claims claims = provider.parseToken(token);
    assertThat(claims.get("emailVerified", Boolean.class)).isFalse();
}
```

Look at the existing test to find the correct method names for `generateLoginToken` and `parseToken`.

**Step 3: Run to confirm fail**

```bash
mvn test -pl pokergameserver -Dtest=JwtTokenProviderTest -P dev
```

Expected: FAIL — `emailVerified` claim is not present.

**Step 4: Add emailVerified claim to JwtTokenProvider**

Find the method that builds the login JWT (look for `Jwts.builder()` with claims). Add:

```java
.claim("emailVerified", profile.isEmailVerified())
```

**Step 5: Update JwtAuthenticationFilter to read the claim**

In `JwtAuthenticationFilter.java`, find where the `UsernamePasswordAuthenticationToken` or authentication principal is built from JWT claims. Store `emailVerified` in the authentication details or as a granted authority so it can be read by the `EmailVerificationFilter` later without a DB call.

The simplest approach: store the raw JWT claims in the `Authentication.getDetails()`.

**Step 6: Run tests**

```bash
mvn test -pl pokergameserver -Dtest=JwtTokenProviderTest,JwtAuthenticationFilterTest -P dev
```

Expected: PASS.

**Step 7: Commit**

```bash
git add code/pokergameserver/src/main/java/com/donohoedigital/games/poker/gameserver/auth/JwtTokenProvider.java
git add code/pokergameserver/src/main/java/com/donohoedigital/games/poker/gameserver/auth/JwtAuthenticationFilter.java
git add code/pokergameserver/src/main/java/com/donohoedigital/games/poker/gameserver/dto/LoginResponse.java
git commit -m "feat(pokergameserver): add emailVerified JWT claim and LoginResponse field"
```

---

## Task 6: EmailVerificationFilter

**Files:**
- Create: `code/pokergameserver/src/main/java/com/donohoedigital/games/poker/gameserver/auth/EmailVerificationFilter.java`
- Modify: `code/pokergameserver/src/main/java/com/donohoedigital/games/poker/gameserver/auth/GameServerSecurityAutoConfiguration.java`
- Create: `code/pokergameserver/src/test/java/com/donohoedigital/games/poker/gameserver/auth/EmailVerificationFilterTest.java`

**Step 1: Write the failing test**

```java
package com.donohoedigital.games.poker.gameserver.auth;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class EmailVerificationFilterTest {

    EmailVerificationFilter filter = new EmailVerificationFilter();

    @Test
    void unverifiedUser_accessingGame_returns403() throws Exception {
        setAuth(false);
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/v1/games");
        MockHttpServletResponse res = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilterInternal(req, res, chain);

        assertThat(res.getStatus()).isEqualTo(403);
        assertThat(res.getContentAsString()).contains("EMAIL_NOT_VERIFIED");
    }

    @Test
    void unverifiedUser_accessingVerifyEndpoint_passes() throws Exception {
        setAuth(false);
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/v1/auth/verify-email");
        MockHttpServletResponse res = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilterInternal(req, res, chain);

        // chain was invoked — not blocked
        assertThat(chain.getRequest()).isNotNull();
        assertThat(res.getStatus()).isEqualTo(200);
    }

    @Test
    void verifiedUser_accessingGame_passes() throws Exception {
        setAuth(true);
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/v1/games");
        MockHttpServletResponse res = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilterInternal(req, res, chain);

        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    void unauthenticatedRequest_passes() throws Exception {
        SecurityContextHolder.clearContext();
        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        MockHttpServletResponse res = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilterInternal(req, res, chain);

        assertThat(chain.getRequest()).isNotNull();
    }

    private void setAuth(boolean emailVerified) {
        // Store emailVerified in Authentication details as a Map
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "alice", null, Collections.emptyList());
        auth.setDetails(Map.of("emailVerified", emailVerified));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
```

**Step 2: Run to confirm fail**

```bash
mvn test -pl pokergameserver -Dtest=EmailVerificationFilterTest -P dev
```

Expected: FAIL — `EmailVerificationFilter` does not exist.

**Step 3: Implement EmailVerificationFilter**

```java
package com.donohoedigital.games.poker.gameserver.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

public class EmailVerificationFilter extends OncePerRequestFilter {

    private static final Set<String> EXEMPT_PATHS = Set.of(
            "/api/v1/auth/register",
            "/api/v1/auth/login",
            "/api/v1/auth/logout",
            "/api/v1/auth/verify-email",
            "/api/v1/auth/resend-verification",
            "/api/v1/auth/forgot-password",
            "/api/v1/auth/reset-password",
            "/api/v1/auth/check-username"
    );

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated() || EXEMPT_PATHS.contains(request.getRequestURI())) {
            chain.doFilter(request, response);
            return;
        }

        if (auth.getDetails() instanceof Map<?, ?> details) {
            Object verified = details.get("emailVerified");
            if (Boolean.FALSE.equals(verified)) {
                response.setStatus(403);
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                objectMapper.writeValue(response.getWriter(),
                        Map.of("error", "EMAIL_NOT_VERIFIED",
                               "message", "Please verify your email address before accessing this feature."));
                return;
            }
        }

        chain.doFilter(request, response);
    }
}
```

**Step 4: Register the filter in GameServerSecurityAutoConfiguration**

Find where the Spring Security filter chain is configured. Add `EmailVerificationFilter` after `JwtAuthenticationFilter`:

```java
.addFilterAfter(new EmailVerificationFilter(), JwtAuthenticationFilter.class)
```

**Step 5: Run tests**

```bash
mvn test -pl pokergameserver -Dtest=EmailVerificationFilterTest -P dev
```

Expected: PASS.

**Step 6: Run full module tests to check nothing broke**

```bash
mvn test -pl pokergameserver -P dev
```

Expected: BUILD SUCCESS.

**Step 7: Commit**

```bash
git add code/pokergameserver/src/main/java/com/donohoedigital/games/poker/gameserver/auth/EmailVerificationFilter.java
git add code/pokergameserver/src/main/java/com/donohoedigital/games/poker/gameserver/auth/GameServerSecurityAutoConfiguration.java
git add code/pokergameserver/src/test/java/com/donohoedigital/games/poker/gameserver/auth/EmailVerificationFilterTest.java
git commit -m "feat(pokergameserver): add EmailVerificationFilter gating WAN features for unverified accounts"
```

---

## Task 7: AuthService — login lockout

**Files:**
- Modify: `code/pokergameserver/src/main/java/com/donohoedigital/games/poker/gameserver/service/AuthService.java`
- Modify: `code/pokergameserver/src/test/java/com/donohoedigital/games/poker/gameserver/service/AuthServiceTest.java`
- Create: `code/pokergameserver/src/main/java/com/donohoedigital/games/poker/gameserver/dto/LockoutException.java` (or add to existing exception handling)

**Step 1: Write failing tests in AuthServiceTest**

Add to the existing `AuthServiceTest` class:

```java
@Test
void login_afterFiveFailures_throwsAccountLockedException() {
    // Register a user first
    authService.register("lockme", "lockme@test.com", "Password1!");
    // Fail 5 times
    for (int i = 0; i < 5; i++) {
        assertThrows(RestAuthException.class,
                () -> authService.login("lockme", "wrongpassword"));
    }
    // 6th attempt should throw account locked
    RestAuthException ex = assertThrows(RestAuthException.class,
            () -> authService.login("lockme", "wrongpassword"));
    assertThat(ex.getMessage()).contains("locked");
    assertThat(ex.getRetryAfterSeconds()).isGreaterThan(0);
}

@Test
void login_successAfterFailures_resetCounter() {
    authService.register("alice2", "alice2@test.com", "Password1!");
    for (int i = 0; i < 3; i++) {
        assertThrows(Exception.class, () -> authService.login("alice2", "wrong"));
    }
    // Correct password resets counter
    assertDoesNotThrow(() -> authService.login("alice2", "Password1!"));
    // Now fail again — counter was reset, so not locked yet
    assertThrows(RestAuthException.class, () -> authService.login("alice2", "wrong"));
    OnlineProfile p = profileRepo.findByName("alice2").orElseThrow();
    assertThat(p.getFailedLoginAttempts()).isEqualTo(1);
}
```

**Step 2: Run to confirm fail**

```bash
mvn test -pl pokergameserver -Dtest=AuthServiceTest -P dev
```

Expected: FAIL — lockout logic doesn't exist yet.

**Step 3: Implement lockout logic in AuthService.login()**

In the existing `login()` method, before validating the password:

```java
// Check lockout
if (profile.getLockedUntil() != null && profile.getLockedUntil() > System.currentTimeMillis()) {
    long retryAfterSeconds = (profile.getLockedUntil() - System.currentTimeMillis()) / 1000;
    throw new RestAuthException("Account is locked. Try again in " + retryAfterSeconds + "s.",
                                423, retryAfterSeconds);
}
```

After a failed password check:

```java
int attempts = profile.getFailedLoginAttempts() + 1;
profile.setFailedLoginAttempts(attempts);

if (attempts >= 5) {
    profile.setLockoutCount(profile.getLockoutCount() + 1);
    long lockDurationMs = lockDurationFor(profile.getLockoutCount());
    profile.setLockedUntil(System.currentTimeMillis() + lockDurationMs);
    profile.setFailedLoginAttempts(0);
}
profileRepo.save(profile);
```

Add a helper:

```java
private long lockDurationFor(int lockoutCount) {
    return switch (lockoutCount) {
        case 1 -> 5 * 60 * 1000L;    // 5 min
        case 2 -> 15 * 60 * 1000L;   // 15 min
        case 3 -> 60 * 60 * 1000L;   // 1 hour
        default -> Long.MAX_VALUE;   // requires admin unlock
    };
}
```

After successful password validation, reset:

```java
profile.setFailedLoginAttempts(0);
profile.setLockoutCount(0);
```

> **Note:** Per design doc Section 12, **both** `failedLoginAttempts` AND `lockoutCount` reset to 0 on successful login. Resetting `lockoutCount` on success means the progressive delay restarts from 5 min if the user later accumulates failures again. The earlier comment saying "do NOT reset lockoutCount" was incorrect.

Add `retryAfterSeconds` to `RestAuthException` if not already there (check the class — if it only has a `String message`, add a `long retryAfterSeconds` field and a two-arg constructor).

**Step 4: Run tests**

```bash
mvn test -pl pokergameserver -Dtest=AuthServiceTest -P dev
```

Expected: PASS.

**Step 5: Commit**

```bash
git add code/pokergameserver/src/main/java/com/donohoedigital/games/poker/gameserver/service/AuthService.java
git commit -m "feat(pokergameserver): add progressive login lockout to AuthService"
```

---

## Task 8: AuthService — register with email verification

**Files:**
- Modify: `code/pokergameserver/src/main/java/com/donohoedigital/games/poker/gameserver/service/AuthService.java`
- Modify: `code/pokergameserver/src/test/java/com/donohoedigital/games/poker/gameserver/service/AuthServiceTest.java`

**Step 1: Write failing tests**

```java
@Test
void register_setsEmailVerifiedFalseAndGeneratesToken() {
    authService.register("newuser", "new@example.com", "Password1!");

    OnlineProfile p = profileRepo.findByName("newuser").orElseThrow();
    assertThat(p.isEmailVerified()).isFalse();
    assertThat(p.getEmailVerificationToken()).isNotBlank();
    assertThat(p.getEmailVerificationTokenExpiry())
            .isGreaterThan(System.currentTimeMillis() + 6 * 24 * 60 * 60 * 1000L); // > 6 days from now
}

@Test
void register_passwordTooShort_throwsException() {
    assertThrows(RestAuthException.class,
            () -> authService.register("user2", "u@example.com", "short"));
}

@Test
void register_passwordTooLong_throwsException() {
    String longPass = "a".repeat(129);
    assertThrows(RestAuthException.class,
            () -> authService.register("user3", "u3@example.com", longPass));
}
```

**Step 2: Run to confirm fail**

```bash
mvn test -pl pokergameserver -Dtest=AuthServiceTest -P dev
```

Expected: FAIL on the new tests.

**Step 3: Update AuthService.register()**

At the start of `register()`, add password strength validation:

```java
validatePasswordStrength(password);
```

```java
private void validatePasswordStrength(String password) {
    if (password == null || password.length() < 8) {
        throw new RestAuthException("Password must be at least 8 characters.", 400);
    }
    if (password.length() > 128) {
        throw new RestAuthException("Password must not exceed 128 characters.", 400);
    }
}
```

After creating the profile, generate a verification token and set it:

```java
String verificationToken = generateSecureToken(); // 32 bytes Base64URL — already exists for password reset
profile.setEmailVerified(false);
profile.setEmailVerificationToken(verificationToken);
profile.setEmailVerificationTokenExpiry(System.currentTimeMillis() + 7L * 24 * 60 * 60 * 1000);
profileRepo.save(profile);

emailService.sendVerificationEmail(profile.getEmail(), profile.getName(), verificationToken);
```

The `generateSecureToken()` method already exists in `AuthService` for password reset tokens — reuse it.

**Step 4: Update AuthService.login() to include email in response**

The `login()` method returns a `LoginResponse`. Add `email` and `emailVerified` fields to the response:

```java
return new LoginResponse(token, profile.getId(), profile.getName(),
                         profile.isEmailVerified(), profile.getEmail());
```

Update `LoginResponse` constructor/builder to match.

**Step 5: Run tests**

```bash
mvn test -pl pokergameserver -Dtest=AuthServiceTest -P dev
```

Expected: PASS.

**Step 6: Commit**

```bash
git add code/pokergameserver/src/main/java/com/donohoedigital/games/poker/gameserver/service/AuthService.java
git commit -m "feat(pokergameserver): send verification email on register, add password strength validation"
```

**Implementation note:** Password strength validation (too short / too long) returns `LoginResponse(false, ...)` rather than throwing an exception. This matches the pattern used by all other `register()` validation failures (banned email, duplicate username, duplicate email) and keeps the controller layer simple — all failures are uniform `LoginResponse` values.

---

## Task 9: AuthService — verifyEmail + resendVerification

**Files:**
- Modify: `code/pokergameserver/src/main/java/com/donohoedigital/games/poker/gameserver/service/AuthService.java`
- Modify: `code/pokergameserver/src/test/java/com/donohoedigital/games/poker/gameserver/service/AuthServiceTest.java`

**Step 1: Write failing tests**

```java
@Test
void verifyEmail_validToken_marksVerifiedAndClearsToken() {
    authService.register("vuser", "v@example.com", "Password1!");
    OnlineProfile p = profileRepo.findByName("vuser").orElseThrow();
    String token = p.getEmailVerificationToken();

    authService.verifyEmail(token);

    OnlineProfile updated = profileRepo.findByName("vuser").orElseThrow();
    assertThat(updated.isEmailVerified()).isTrue();
    assertThat(updated.getEmailVerificationToken()).isNull();
    assertThat(updated.getEmailVerificationTokenExpiry()).isNull();
}

@Test
void verifyEmail_invalidToken_throwsException() {
    assertThrows(RestAuthException.class, () -> authService.verifyEmail("bad-token"));
}

@Test
void verifyEmail_expiredToken_throwsException() {
    authService.register("expuser", "exp@example.com", "Password1!");
    OnlineProfile p = profileRepo.findByName("expuser").orElseThrow();
    p.setEmailVerificationTokenExpiry(System.currentTimeMillis() - 1000); // expired
    profileRepo.save(p);

    assertThrows(RestAuthException.class,
            () -> authService.verifyEmail(p.getEmailVerificationToken()));
}

@Test
void resendVerification_replacesToken() {
    authService.register("ruser", "r@example.com", "Password1!");
    OnlineProfile p = profileRepo.findByName("ruser").orElseThrow();
    String original = p.getEmailVerificationToken();

    authService.resendVerification("ruser");

    OnlineProfile updated = profileRepo.findByName("ruser").orElseThrow();
    assertThat(updated.getEmailVerificationToken()).isNotEqualTo(original);
    assertThat(updated.isEmailVerified()).isFalse();
}

@Test
void resendVerification_alreadyVerified_throwsException() {
    authService.register("vuser2", "v2@example.com", "Password1!");
    OnlineProfile p = profileRepo.findByName("vuser2").orElseThrow();
    p.setEmailVerified(true);
    profileRepo.save(p);

    assertThrows(RestAuthException.class, () -> authService.resendVerification("vuser2"));
}
```

**Step 2: Run to confirm fail**

```bash
mvn test -pl pokergameserver -Dtest=AuthServiceTest -P dev
```

**Step 3: Implement in AuthService**

```java
public LoginResponse verifyEmail(String token) {
    OnlineProfile profile = profileRepo.findByEmailVerificationToken(token)
            .orElseThrow(() -> new RestAuthException("Invalid or expired verification token.", 400));

    if (profile.getEmailVerificationTokenExpiry() == null
            || profile.getEmailVerificationTokenExpiry() < System.currentTimeMillis()) {
        throw new RestAuthException("Verification token has expired. Please request a new one.", 400);
    }

    // If this was an email change confirmation, swap emails
    if (profile.getPendingEmail() != null) {
        profile.setEmail(profile.getPendingEmail());
        profile.setPendingEmail(null);
    }

    profile.setEmailVerified(true);
    profile.setEmailVerificationToken(null);
    profile.setEmailVerificationTokenExpiry(null);
    profileRepo.save(profile);

    // Issue a fresh JWT with emailVerified=true
    String newToken = jwtTokenProvider.generateLoginToken(profile);
    return new LoginResponse(newToken, profile.getId(), profile.getName(), true, profile.getEmail());
}

public void resendVerification(String username) {
    OnlineProfile profile = profileRepo.findByName(username)
            .orElseThrow(() -> new RestAuthException("Profile not found.", 404));

    if (profile.isEmailVerified()) {
        throw new RestAuthException("Email is already verified.", 400);
    }

    // Rate limit: check last token issue time (reuse expiry field — new token always sets 7-day expiry)
    // If expiry is > 6 days 23h 55m from now, was issued < 5 min ago
    long fiveMinAgo = System.currentTimeMillis() - 5 * 60 * 1000;
    long sevenDaysFromNow = System.currentTimeMillis() + 7L * 24 * 60 * 60 * 1000;
    if (profile.getEmailVerificationTokenExpiry() != null
            && profile.getEmailVerificationTokenExpiry() > sevenDaysFromNow - 5 * 60 * 1000) {
        throw new RestAuthException("Please wait before requesting another verification email.", 429);
    }

    String token = generateSecureToken();
    profile.setEmailVerificationToken(token);
    profile.setEmailVerificationTokenExpiry(sevenDaysFromNow);
    profileRepo.save(profile);

    emailService.sendVerificationEmail(profile.getEmail(), profile.getName(), token);
}
```

**Step 4: Run tests**

```bash
mvn test -pl pokergameserver -Dtest=AuthServiceTest -P dev
```

Expected: PASS.

**Step 5: Commit**

```bash
git add code/pokergameserver/src/main/java/com/donohoedigital/games/poker/gameserver/service/AuthService.java
git commit -m "feat(pokergameserver): add verifyEmail and resendVerification to AuthService"
```

---

## Task 10: AuthService — email change

**Files:**
- Modify: `code/pokergameserver/src/main/java/com/donohoedigital/games/poker/gameserver/service/AuthService.java`
- Modify test: same `AuthServiceTest.java`

**Step 1: Write failing tests**

```java
@Test
void requestEmailChange_storesPendingEmailAndSendsEmail() {
    authService.register("euser", "euser@example.com", "Password1!");
    OnlineProfile p = profileRepo.findByName("euser").orElseThrow();
    p.setEmailVerified(true);
    profileRepo.save(p);

    authService.requestEmailChange("euser", "newemail@example.com");

    OnlineProfile updated = profileRepo.findByName("euser").orElseThrow();
    assertThat(updated.getPendingEmail()).isEqualTo("newemail@example.com");
    assertThat(updated.getEmailVerificationToken()).isNotBlank();
    // Old email unchanged
    assertThat(updated.getEmail()).isEqualTo("euser@example.com");
}

@Test
void confirmEmailChange_swapsEmailAndClearsFields() {
    authService.register("euser2", "euser2@example.com", "Password1!");
    OnlineProfile p = profileRepo.findByName("euser2").orElseThrow();
    p.setEmailVerified(true);
    profileRepo.save(p);
    authService.requestEmailChange("euser2", "new2@example.com");

    OnlineProfile withToken = profileRepo.findByName("euser2").orElseThrow();
    authService.verifyEmail(withToken.getEmailVerificationToken()); // reuses verifyEmail

    OnlineProfile confirmed = profileRepo.findByName("euser2").orElseThrow();
    assertThat(confirmed.getEmail()).isEqualTo("new2@example.com");
    assertThat(confirmed.getPendingEmail()).isNull();
    assertThat(confirmed.isEmailVerified()).isTrue();
}
```

**Step 2: Implement requestEmailChange**

```java
public void requestEmailChange(String username, String newEmail) {
    OnlineProfile profile = profileRepo.findByName(username)
            .orElseThrow(() -> new RestAuthException("Profile not found.", 404));

    if (profileRepo.findByEmail(newEmail).isPresent()) {
        // Generic message to prevent email enumeration
        throw new RestAuthException("That email address is not available.", 409);
    }

    String token = generateSecureToken();
    profile.setPendingEmail(newEmail);
    profile.setEmailVerificationToken(token);
    profile.setEmailVerificationTokenExpiry(System.currentTimeMillis() + 7L * 24 * 60 * 60 * 1000);
    profileRepo.save(profile);

    emailService.sendEmailChangeConfirmation(newEmail, profile.getName(), token);
}
```

Note: `verifyEmail()` already handles the `pendingEmail` swap (implemented in Task 9). No additional method needed.

**Step 3: Add `findByEmail` to OnlineProfileRepository if not present**

```java
Optional<OnlineProfile> findByEmail(String email);
```

**Step 4: Run tests**

```bash
mvn test -pl pokergameserver -Dtest=AuthServiceTest -P dev
```

Expected: PASS.

**Step 5: Commit**

```bash
git add code/pokergameserver/src/main/java/com/donohoedigital/games/poker/gameserver/service/AuthService.java
git add code/pokergameserver/src/main/java/com/donohoedigital/games/poker/gameserver/persistence/repository/OnlineProfileRepository.java
git commit -m "feat(pokergameserver): add requestEmailChange to AuthService"
```

---

## Task 11: AuthController — new endpoints

**Files:**
- Modify: `code/pokergameserver/src/main/java/com/donohoedigital/games/poker/gameserver/controller/AuthController.java`
- Create new DTOs: `code/pokergameserver/src/main/java/com/donohoedigital/games/poker/gameserver/dto/ChangeEmailRequest.java`, `CheckUsernameResponse.java`
- Modify: `code/pokergameserver/src/test/java/com/donohoedigital/games/poker/gameserver/controller/AuthControllerTest.java`

**Step 1: Create new DTOs**

`ChangeEmailRequest.java`:
```java
public record ChangeEmailRequest(@NotBlank String email) {}
```

`CheckUsernameResponse.java`:
```java
public record CheckUsernameResponse(boolean available) {}
```

**Step 2: Write failing controller tests**

Add to `AuthControllerTest`:

```java
@Test
void verifyEmail_validToken_returns200WithFreshJwt() throws Exception {
    // Register and get the verification token
    mockMvc.perform(post("/api/v1/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""{"username":"vtest","email":"vtest@example.com","password":"Password1!"}"""))
            .andExpect(status().isOk());

    // Fetch the token from the DB (via test repo)
    String token = profileRepo.findByName("vtest").orElseThrow().getEmailVerificationToken();

    mockMvc.perform(get("/api/v1/auth/verify-email").param("token", token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.emailVerified").value(true));
}

@Test
void verifyEmail_badToken_returns400() throws Exception {
    mockMvc.perform(get("/api/v1/auth/verify-email").param("token", "not-a-token"))
            .andExpect(status().isBadRequest());
}

@Test
void checkUsername_available_returnsTrue() throws Exception {
    mockMvc.perform(get("/api/v1/auth/check-username").param("username", "brandnew"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.available").value(true));
}

@Test
void checkUsername_taken_returnsFalse() throws Exception {
    authService.register("taken", "taken@example.com", "Password1!");
    mockMvc.perform(get("/api/v1/auth/check-username").param("username", "taken"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.available").value(false));
}

@Test
void resendVerification_unverifiedUser_returns200() throws Exception {
    // Register (sets emailVerified=false)
    String jwt = registerAndGetJwt("resend1", "resend1@example.com");
    // Force token expiry so rate limit doesn't block
    forceTokenExpiry("resend1");

    mockMvc.perform(post("/api/v1/auth/resend-verification")
            .header("Authorization", "Bearer " + jwt))
            .andExpect(status().isOk());
}

@Test
void changeEmail_authenticated_returns200() throws Exception {
    String jwt = registerVerifyAndGetJwt("cemail1", "cemail1@example.com");
    mockMvc.perform(put("/api/v1/auth/email")
            .header("Authorization", "Bearer " + jwt)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""{"email":"cemail1new@example.com"}"""))
            .andExpect(status().isOk());
}
```

**Step 3: Run to confirm fail**

```bash
mvn test -pl pokergameserver -Dtest=AuthControllerTest -P dev
```

**Step 4: Add endpoints to AuthController**

```java
@GetMapping("/verify-email")
public ResponseEntity<LoginResponse> verifyEmail(@RequestParam String token) {
    return ResponseEntity.ok(authService.verifyEmail(token));
}

@PostMapping("/resend-verification")
public ResponseEntity<Void> resendVerification(Authentication auth) {
    authService.resendVerification(auth.getName());
    return ResponseEntity.ok().build();
}

@GetMapping("/check-username")
public ResponseEntity<CheckUsernameResponse> checkUsername(@RequestParam String username) {
    boolean available = profileRepo.findByName(username).isEmpty();
    return ResponseEntity.ok(new CheckUsernameResponse(available));
}

@PutMapping("/email")
public ResponseEntity<Void> changeEmail(@RequestBody @Valid ChangeEmailRequest request,
                                         Authentication auth) {
    authService.requestEmailChange(auth.getName(), request.email());
    return ResponseEntity.ok().build();
}
```

**Step 5: Run tests**

```bash
mvn test -pl pokergameserver -Dtest=AuthControllerTest -P dev
```

Expected: PASS.

**Step 6: Commit**

```bash
git add code/pokergameserver/src/main/java/com/donohoedigital/games/poker/gameserver/controller/AuthController.java
git add code/pokergameserver/src/main/java/com/donohoedigital/games/poker/gameserver/dto/
git commit -m "feat(pokergameserver): add verify-email, resend-verification, check-username, and change-email endpoints"
```

---

## Task 12: CORS update for web browser access

**Files:**
- Modify: `code/pokergameserver/src/main/java/com/donohoedigital/games/poker/gameserver/auth/CorsProperties.java`
- Modify: `code/pokergameserver/src/main/resources/application.properties`

**Step 1: Update CORS configuration**

In `application.properties`, add (check what `CorsProperties` uses):

```properties
# Add SERVER_HOST to allowed CORS origins for browser-based web client
game.server.cors.allowed-origins=${SERVER_HOST:http://localhost:3000},http://localhost:3000,http://localhost:8080
```

Read `CorsProperties.java` to confirm the exact property key and update accordingly. The pattern is: allow `SERVER_HOST` as a CORS origin so the web client can call the game server API from a browser.

**Step 2: Verify in CorsPropertiesTest**

Add a test asserting the SERVER_HOST value is read and included in allowed origins. Reference `CorsPropertiesTest.java` for the existing test pattern.

**Step 3: Commit**

```bash
git add code/pokergameserver/src/main/resources/application.properties
git commit -m "feat(pokergameserver): include SERVER_HOST in CORS allowed origins for web client access"
```

---

## Task 13: Embedded server — localhost binding

**Files:**
- Modify: `code/poker/src/main/java/com/donohoedigital/games/poker/server/EmbeddedGameServer.java`
- Modify test: find or create a test for `EmbeddedGameServer` startup configuration

**Step 1: Read EmbeddedGameServer.java**

Open the file and find where the Spring Boot application is started with a specific port. Look for `SpringApplication.run()`, `SpringApplicationBuilder`, or properties being set before boot.

**Step 2: Set server.address=127.0.0.1 in embedded mode**

In the method that starts the embedded server with a specific port, add:

```java
properties.put("server.address", "127.0.0.1");
```

This can be done via `SpringApplication.setDefaultProperties()` or `SpringApplicationBuilder.properties()` before `.run()`. The `embedded` Spring profile should already be active — confirm this and add the address property alongside the port:

```java
Map<String, Object> props = new HashMap<>();
props.put("server.port", port);
props.put("server.address", "127.0.0.1");
app.setDefaultProperties(props);
```

**Step 3: Verify the change is minimal and correct**

The `start()` no-argument method (random port) should not be changed — it already doesn't bind explicitly. Only the `start(int port)` overload needs updating.

**Step 4: Commit**

```bash
git add code/poker/src/main/java/com/donohoedigital/games/poker/server/EmbeddedGameServer.java
git commit -m "fix(poker): bind embedded game server to 127.0.0.1 only, preventing external access"
```

---

## Task 14: Delete legacy auth from api module

**Files — delete entirely:**
- `code/api/src/main/java/com/donohoedigital/poker/api/controller/AuthController.java`
- `code/api/src/main/java/com/donohoedigital/poker/api/controller/ProfileController.java`
- `code/api/src/main/java/com/donohoedigital/poker/api/service/EmailService.java`
- `code/api/src/main/java/com/donohoedigital/poker/api/dto/AuthResponse.java`
- `code/api/src/main/java/com/donohoedigital/poker/api/dto/ForgotPasswordRequest.java`
- `code/api/src/main/java/com/donohoedigital/poker/api/dto/LoginRequest.java`
- `code/api/src/main/java/com/donohoedigital/poker/api/security/JwtAuthFilter.java`
- `code/api/src/main/java/com/donohoedigital/poker/api/security/JwtTokenProvider.java`

**Files — update:**
- `code/api/src/main/java/com/donohoedigital/poker/api/config/SecurityConfig.java` — remove all auth endpoint config; keep only admin/public routes
- `code/api/pom.xml` — remove JWT/BCrypt dependencies no longer needed

**Step 1: Delete the files**

```bash
git rm code/api/src/main/java/com/donohoedigital/poker/api/controller/AuthController.java
git rm code/api/src/main/java/com/donohoedigital/poker/api/controller/ProfileController.java
git rm code/api/src/main/java/com/donohoedigital/poker/api/service/EmailService.java
git rm code/api/src/main/java/com/donohoedigital/poker/api/dto/AuthResponse.java
git rm code/api/src/main/java/com/donohoedigital/poker/api/dto/ForgotPasswordRequest.java
git rm code/api/src/main/java/com/donohoedigital/poker/api/dto/LoginRequest.java
git rm code/api/src/main/java/com/donohoedigital/poker/api/security/JwtAuthFilter.java
git rm code/api/src/main/java/com/donohoedigital/poker/api/security/JwtTokenProvider.java
```

**Step 2: Also delete corresponding test files**

Find and delete test files for the above classes (search in `code/api/src/test/`).

**Step 3: Fix SecurityConfig.java**

Remove references to deleted classes. The api module security should now only protect `/api/admin/**` endpoints with admin credentials. All other routes are public.

**Step 4: Build api module to confirm no compilation errors**

```bash
mvn test -pl api -P dev
```

Expected: BUILD SUCCESS.

**Step 5: Commit**

```bash
git commit -m "feat(api): delete legacy auth layer — AuthController, ProfileController, EmailService, JWT security"
```

---

## Task 15: Web client — api.ts consolidation

**Files:**
- Modify: `code/web/lib/api.ts`
- Modify: `code/web/lib/auth/AuthContext.tsx`
- Modify: `code/web/lib/auth/useAuth.ts`
- Modify test: `code/web/lib/__tests__/api.test.ts`

**Step 1: Read current api.ts**

Open `code/web/lib/api.ts` and identify all calls to `/api/auth/*` and `/api/profile/*`. Map each to its game server equivalent:

| Old (legacy) | New (game server) |
|---|---|
| `POST /api/auth/login` | `POST /api/v1/auth/login` |
| `POST /api/auth/logout` | `POST /api/v1/auth/logout` |
| `GET /api/auth/me` | `GET /api/v1/auth/me` |
| `POST /api/profile/forgot-password` | `POST /api/v1/auth/forgot-password` |
| `PUT /api/profile/password` | `PUT /api/v1/auth/change-password` |

**Step 2: Add new functions for new endpoints**

```typescript
export const authApi = {
  // ... existing methods updated to /api/v1/auth/...
  verifyEmail: (token: string) =>
    fetch(`${GAME_SERVER_URL}/api/v1/auth/verify-email?token=${token}`, { credentials: 'include' }),
  resendVerification: () =>
    fetch(`${GAME_SERVER_URL}/api/v1/auth/resend-verification`, { method: 'POST', credentials: 'include' }),
  checkUsername: (username: string) =>
    fetch(`${GAME_SERVER_URL}/api/v1/auth/check-username?username=${encodeURIComponent(username)}`, { credentials: 'include' }),
  changeEmail: (email: string) =>
    fetch(`${GAME_SERVER_URL}/api/v1/auth/email`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email }),
      credentials: 'include',
    }),
};
```

Note: `GAME_SERVER_URL` is the game server base URL. The web client needs to know the game server's address. In `code/web/lib/config.ts`, add:

```typescript
export const GAME_SERVER_URL = process.env.NEXT_PUBLIC_GAME_SERVER_URL ?? 'http://localhost:8877';
```

And in `.env.example` or `docker-compose.yml`, add `NEXT_PUBLIC_GAME_SERVER_URL=http://poker.yourdomain.com:8877`.

**Step 3: Update AuthContext to carry emailVerified**

In `AuthContext.tsx`, add `emailVerified: boolean` to the auth state. Set it from the login response. Update `useAuth.ts` to expose `emailVerified`.

**Step 4: Update api.test.ts**

Update tests to assert calls go to `/api/v1/auth/*` instead of `/api/auth/*`.

**Step 5: Run web tests**

```bash
cd code/web && npm test -- --watchAll=false
```

Expected: PASS.

**Step 6: Commit**

```bash
git add code/web/lib/api.ts code/web/lib/auth/ code/web/lib/config.ts
git commit -m "feat(web): consolidate auth to game server API, add emailVerified to auth context"
```

---

## Task 16: Web client — new auth pages

**Files — create:**
- `code/web/app/verify-email-pending/page.tsx`
- `code/web/app/verify-email/page.tsx`
- `code/web/app/reset-password/page.tsx`

**Step 1: Create verify-email-pending page**

This is the verification wall — shown after registration and on login when `emailVerified=false`.

```tsx
// code/web/app/verify-email-pending/page.tsx
'use client';
import { useState } from 'react';
import { useAuth } from '@/lib/auth/useAuth';
import { authApi } from '@/lib/api';

export default function VerifyEmailPendingPage() {
  const { user } = useAuth();
  const [status, setStatus] = useState<'idle' | 'sent' | 'error'>('idle');

  const resend = async () => {
    try {
      await authApi.resendVerification();
      setStatus('sent');
    } catch {
      setStatus('error');
    }
  };

  return (
    <main>
      <h1>Check your email</h1>
      <p>We sent a verification link to <strong>{user?.email}</strong>.</p>
      <p>Click the link in that email to activate your account. It expires in 7 days.</p>
      {status === 'sent' && <p>Email resent — check your inbox.</p>}
      {status === 'error' && <p>Could not resend — please try again in a few minutes.</p>}
      <button onClick={resend} disabled={status === 'sent'}>Resend verification email</button>
    </main>
  );
}
```

**Step 2: Create verify-email page**

This is the landing page for the link in the verification email.

```tsx
// code/web/app/verify-email/page.tsx
'use client';
import { useEffect, useState } from 'react';
import { useRouter, useSearchParams } from 'next/navigation';
import { authApi } from '@/lib/api';

export default function VerifyEmailPage() {
  const params = useSearchParams();
  const router = useRouter();
  const token = params.get('token');
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!token) { setError('Missing token.'); return; }
    authApi.verifyEmail(token).then(async res => {
      if (res.ok) {
        router.replace('/games');
      } else {
        const body = await res.json();
        setError(body.message ?? 'Invalid or expired link.');
      }
    });
  }, [token]);

  if (error) return (
    <main>
      <h1>Verification failed</h1>
      <p>{error}</p>
      <a href="/verify-email-pending">Request a new link</a>
    </main>
  );

  return <main><p>Verifying…</p></main>;
}
```

**Step 3: Create reset-password page**

```tsx
// code/web/app/reset-password/page.tsx
'use client';
import { useState } from 'react';
import { useSearchParams, useRouter } from 'next/navigation';
import { authApi } from '@/lib/api';

export default function ResetPasswordPage() {
  const params = useSearchParams();
  const router = useRouter();
  const token = params.get('token') ?? '';
  const [password, setPassword] = useState('');
  const [confirm, setConfirm] = useState('');
  const [error, setError] = useState('');
  const [done, setDone] = useState(false);

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (password !== confirm) { setError('Passwords do not match.'); return; }
    if (password.length < 8) { setError('Password must be at least 8 characters.'); return; }
    const res = await authApi.resetPassword(token, password);
    if (res.ok) { setDone(true); setTimeout(() => router.replace('/login'), 2000); }
    else { const b = await res.json(); setError(b.message ?? 'Reset failed.'); }
  };

  if (done) return <main><p>Password reset. Redirecting to login…</p></main>;

  return (
    <main>
      <h1>Reset your password</h1>
      <form onSubmit={submit}>
        <input type="password" placeholder="New password (min 8 chars)" value={password} onChange={e => setPassword(e.target.value)} required />
        <input type="password" placeholder="Confirm password" value={confirm} onChange={e => setConfirm(e.target.value)} required />
        {error && <p>{error}</p>}
        <button type="submit">Reset password</button>
      </form>
    </main>
  );
}
```

**Step 4: Commit**

```bash
git add code/web/app/verify-email-pending/ code/web/app/verify-email/ code/web/app/reset-password/
git commit -m "feat(web): add verify-email-pending, verify-email, and reset-password pages"
```

---

## Task 17: Web client — login, register, and account settings updates

**Files:**
- Modify: `code/web/app/login/page.tsx`
- Modify: `code/web/app/register/page.tsx`
- Modify: `code/web/app/forgot/page.tsx`
- Modify: `code/web/components/auth/LoginForm.tsx`
- Modify: `code/web/components/profile/PasswordChangeForm.tsx`
- Create: `code/web/app/account/page.tsx`
- Modify: `code/web/app/games/layout.tsx` (route guard)

**Step 1: Update LoginForm to add Remember Me and Forgot Password link**

In `LoginForm.tsx`, add below the password field:
```tsx
<label><input type="checkbox" checked={rememberMe} onChange={e => setRememberMe(e.target.checked)} /> Remember me</label>
<a href="/forgot">Forgot password?</a>
```

Pass `rememberMe` to the login API call body: `{ username, password, rememberMe }`.

**Step 2: Update login page — redirect to verify-email-pending if unverified**

After successful login, check `user.emailVerified`. If false and the user tried to access a game route, redirect to `/verify-email-pending`.

**Step 3: Update register page — username availability check**

Add a debounced check on the username field that calls `authApi.checkUsername()` and shows an inline "Username is available ✓" or "Username is taken" message. Debounce 400ms.

After successful registration, redirect to `/verify-email-pending` instead of `/games`.

**Step 4: Update forgot page**

Update to call `authApi.forgotPassword(email)` (takes email, not username). The existing page may take username — update the form field label and API call.

**Step 5: Create account settings page**

`code/web/app/account/page.tsx` — accessible to authenticated users (including unverified):
- Change password form (using `PasswordChangeForm` component)
- Change email form (new email input → calls `authApi.changeEmail()`)
- "Resend verification email" section (shown only if `!emailVerified`)

**Step 6: Add route guard to games layout**

In `code/web/app/games/layout.tsx`, check `emailVerified` from auth context. If false, redirect to `/verify-email-pending`:

```tsx
const { user, isLoading } = useAuth();
if (!isLoading && user && !user.emailVerified) redirect('/verify-email-pending');
if (!isLoading && !user) redirect('/login');
```

**Step 7: Run web tests**

```bash
cd code/web && npm test -- --watchAll=false
```

Fix any broken tests (LoginForm test may need updating for new checkbox).

**Step 8: Commit**

```bash
git add code/web/app/login/ code/web/app/register/ code/web/app/forgot/ code/web/app/account/ code/web/app/games/layout.tsx code/web/components/auth/ code/web/components/profile/
git commit -m "feat(web): add remember me, username check, email verification flow, and account settings page"
```

---

## Task 18: Admin panel — account management

**Files:**
- Modify: `code/api/src/main/java/com/donohoedigital/poker/api/controller/AdminController.java`
- Modify: `code/web/app/admin/online-profile-search/page.tsx` and `AdminSearchForm.tsx`
- Create: `code/web/app/admin/accounts/page.tsx`

**Step 1: Add admin endpoints to AdminController**

The existing `AdminController` has profile search. Add:

```java
@PostMapping("/profiles/{id}/verify")
public ResponseEntity<Void> manuallyVerify(@PathVariable Long id) {
    OnlineProfile p = profileRepo.findById(id).orElseThrow();
    p.setEmailVerified(true);
    p.setEmailVerificationToken(null);
    p.setEmailVerificationTokenExpiry(null);
    profileRepo.save(p);
    return ResponseEntity.ok().build();
}

@PostMapping("/profiles/{id}/unlock")
public ResponseEntity<Void> unlockAccount(@PathVariable Long id) {
    OnlineProfile p = profileRepo.findById(id).orElseThrow();
    p.setLockedUntil(null);
    p.setFailedLoginAttempts(0);
    p.setLockoutCount(0);
    profileRepo.save(p);
    return ResponseEntity.ok().build();
}

@PostMapping("/profiles/{id}/resend-verification")
public ResponseEntity<Void> resendVerification(@PathVariable Long id) {
    OnlineProfile p = profileRepo.findById(id).orElseThrow();
    if (p.isEmailVerified()) return ResponseEntity.badRequest().build();
    String token = generateSecureToken();
    p.setEmailVerificationToken(token);
    p.setEmailVerificationTokenExpiry(System.currentTimeMillis() + 7L * 24 * 60 * 60 * 1000);
    profileRepo.save(p);
    emailService.sendVerificationEmail(p.getEmail(), p.getName(), token);
    return ResponseEntity.ok().build();
}
```

Note: `AdminController` needs `EmailService` injected — add a constructor parameter.

**Step 2: Update admin profile search to show verification and lock status**

In `AdminSearchForm.tsx` and the search results, display:
- `emailVerified` badge (green ✓ / red ✗)
- Lock status (`lockedUntil` — "Locked until HH:MM" or "Not locked")
- Action buttons: **Verify**, **Unlock**, **Resend verification**

**Step 3: Run api tests**

```bash
mvn test -pl api -P dev
```

Expected: PASS.

**Step 4: Commit**

```bash
git add code/api/src/main/java/com/donohoedigital/poker/api/controller/AdminController.java
git add code/web/app/admin/
git commit -m "feat: add admin account management — verify, unlock, resend verification"
```

---

## Task 19: Desktop client — RestAuthClient new methods

**Files:**
- Modify: `code/poker/src/main/java/com/donohoedigital/games/poker/online/RestAuthClient.java`

**Step 1: Read RestAuthClient.java**

Confirm existing methods: `login()`, `register()`, `getCurrentUser()`, `changePassword()`, `forgotPassword()`, `resetPassword()`, `updateProfile()`, `logout()`.

Note the response parsing pattern — it uses `ObjectMapper` and returns custom response objects.

**Step 2: Update LoginResponse handling**

The login (and register) response now includes `emailVerified` and `email`. Create an `AuthResult` record (or update the existing one) to carry these:

```java
public record AuthResult(String token, long profileId, String username, boolean emailVerified, String email) {}
```

Update `login()` and `register()` to parse and return `AuthResult`.

**Step 3: Add new methods**

```java
public void verifyEmail(String serverUrl, String token) throws RestAuthException {
    // GET /api/v1/auth/verify-email?token=...
    // Updates cachedJwt_ with the new token from response
}

public void resendVerification(String serverUrl) throws RestAuthException {
    // POST /api/v1/auth/resend-verification (authenticated)
}

public boolean checkUsername(String serverUrl, String username) throws RestAuthException {
    // GET /api/v1/auth/check-username?username=...
    // Returns the 'available' boolean
}

public void requestEmailChange(String serverUrl, String newEmail) throws RestAuthException {
    // PUT /api/v1/auth/email
}
```

**Step 4: Cache emailVerified per session**

Add `private boolean cachedEmailVerified_` field. Set on login/register from `AuthResult.emailVerified()`. Expose as `isEmailVerified()`. Update to `true` after `verifyEmail()` succeeds (new JWT will have the updated claim).

**Step 5: Commit**

```bash
git add code/poker/src/main/java/com/donohoedigital/games/poker/online/RestAuthClient.java
git commit -m "feat(poker): add verifyEmail, resendVerification, checkUsername, and changeEmail to RestAuthClient"
```

---

## Task 20: Desktop client — first-run wizard and startup screen

**Files:**
- Create: `code/poker/src/main/java/com/donohoedigital/games/poker/ui/FirstRunWizard.java`
- Create: `code/poker/src/main/java/com/donohoedigital/games/poker/ui/StartupScreen.java`
- Modify: the main startup flow — search for the class that calls `PokerMain` or the initial window setup. Look for `AppStartup`, `PokerMain`, or `GameStartup` in `code/poker/src/main/java/com/donohoedigital/games/poker/`.

**Step 1: Find the startup entry point**

Run:
```bash
grep -r "ProfileManager\|getProfiles\|getLocalProfiles\|profile.*list" code/poker/src/main/java/ --include="*.java" -l
```

This finds where profiles are loaded at startup.

**Step 2: Implement FirstRunWizard as a Swing JDialog**

The wizard has two panels:
1. **Welcome** — "Play Locally" / "Play Online" buttons
2. **Local profile creation** — name field → creates local profile → done
3. **Online profile creation** — Host field (blank, placeholder `poker.yourdomain.com`) + Port field (default `8877`) → Register/Login tabs → done

Key implementation notes:
- Use `JTabbedPane` for Register/Login tabs on the online step
- Register tab: username, email, password fields → calls `RestAuthClient.register()`
- Login tab: username, password → calls `RestAuthClient.login()`
- On success, save the profile (server URL, username) to the local profile store
- If `emailVerified=false`, show a follow-up message dialog: "A verification email has been sent to [email]. Verify your email to access online features."

**Step 3: Implement StartupScreen**

Shown when the last-used profile is an online profile. A minimal Swing dialog or panel:
- "Welcome back, **[username]**" label
- Server hostname shown below
- **[Practice]** button — closes screen, enters main app unauthenticated for that profile
- **[Sign In]** button — shows password field → calls `RestAuthClient.login()` → if success, enters main app authenticated
- **"Switch / New Profile"** hyperlink-style button → opens profile picker

**Step 4: Hook into startup**

In the startup entry point found in Step 1:
- If no profiles: show `FirstRunWizard`
- If profiles exist and last profile is online: show `StartupScreen`
- If last profile is local: proceed directly to main menu

**Step 5: Commit**

```bash
git add code/poker/src/main/java/com/donohoedigital/games/poker/ui/FirstRunWizard.java
git add code/poker/src/main/java/com/donohoedigital/games/poker/ui/StartupScreen.java
git commit -m "feat(poker): add first-run wizard and startup screen for profile-based authentication"
```

---

## Task 21: Desktop client — profile picker and Email Verification dialog

**Files:**
- Create: `code/poker/src/main/java/com/donohoedigital/games/poker/ui/ProfilePickerDialog.java`
- Create: `code/poker/src/main/java/com/donohoedigital/games/poker/ui/EmailVerificationDialog.java`

**Step 1: Implement ProfilePickerDialog**

A `JDialog` listing all profiles:
- Each row: profile name, type badge (Local / Online + hostname)
- Online profiles show a lock icon (🔒) if unauthenticated (no JWT cached), checkmark if authenticated
- **[+ New Profile]** button at bottom → launches `FirstRunWizard` in profile-addition mode (skips the welcome step, goes straight to Local/Online choice)
- Clicking an authenticated online profile → closes dialog, switches profile
- Clicking an unauthenticated online profile → shows mini Practice/Sign In choice inline or as a sub-dialog
- Clicking a local profile → closes dialog, switches profile

**Step 2: Implement EmailVerificationDialog**

A `JDialog` shown when an unverified user attempts an online feature:

```java
// Title: "Email Verification Required"
// Body: "Online features require a verified email address."
//       "A verification link was sent to: alice@example.com"
// [Resend Email]   [Close]
```

On **Resend Email** click:
- Call `RestAuthClient.resendVerification(serverUrl)` on a background thread (not EDT)
- On success: change button label to "Email Sent ✓", disable button
- On rate-limit error: show "Please wait before requesting another email."
- On other error: show "Failed to send. Please try again."

**Step 3: Wire EmailVerificationDialog into WAN feature entry points**

Search for where WAN features start (lobby join, game hosting, leaderboard fetch, etc.). Pattern to find:

```bash
grep -r "RestAuthClient\|serverUrl\|getServerUrl\|connectToServer" code/poker/src/main/java/ --include="*.java" -l
```

At each online feature entry point, before making the WAN call, check:

```java
if (!RestAuthClient.getInstance().isEmailVerified()) {
    new EmailVerificationDialog(parentFrame, serverUrl, email).setVisible(true);
    return; // Don't proceed
}
```

**Step 4: Commit**

```bash
git add code/poker/src/main/java/com/donohoedigital/games/poker/ui/ProfilePickerDialog.java
git add code/poker/src/main/java/com/donohoedigital/games/poker/ui/EmailVerificationDialog.java
git commit -m "feat(poker): add ProfilePickerDialog and EmailVerificationDialog with resend support"
```

---

## Task 22: Desktop client — Remember Me

**Files:**
- Modify: `code/poker/src/main/java/com/donohoedigital/games/poker/online/RestAuthClient.java`
- Modify: `code/poker/src/main/java/com/donohoedigital/games/poker/ui/StartupScreen.java`

**Step 1: Update StartupScreen Sign In form to add Remember Me checkbox**

```java
JCheckBox rememberMeBox = new JCheckBox("Remember me on this device");
```

Pass the checkbox value to the login flow.

**Step 2: Persist JWT to disk when Remember Me is checked**

In `RestAuthClient`, after a successful login with `rememberMe=true`:

```java
private static final String JWT_FILE = "auth.token";

public void persistJwt(String profileName, String jwt) {
    try {
        Path dir = getProfileDir(profileName); // same dir as profile data
        Path file = dir.resolve(JWT_FILE);
        Files.writeString(file, jwt, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        // Restrict to owner-only read (best effort on Windows)
        File f = file.toFile();
        f.setReadable(false, false);
        f.setReadable(true, true);
        f.setWritable(false, false);
        f.setWritable(true, true);
    } catch (IOException e) {
        LOG.warn("Could not persist JWT for profile {}: {}", profileName, e.getMessage());
    }
}

public Optional<String> loadPersistedJwt(String profileName) {
    try {
        Path file = getProfileDir(profileName).resolve(JWT_FILE);
        if (!Files.exists(file)) return Optional.empty();
        String jwt = Files.readString(file).trim();
        // Basic sanity: JWTs are 3-part base64
        if (jwt.split("\\.").length != 3) return Optional.empty();
        return Optional.of(jwt);
    } catch (IOException e) {
        return Optional.empty();
    }
}

public void clearPersistedJwt(String profileName) {
    try {
        Files.deleteIfExists(getProfileDir(profileName).resolve(JWT_FILE));
    } catch (IOException ignored) {}
}
```

**Step 3: Check for persisted JWT at startup**

In `StartupScreen` (or the startup hook), before showing the Sign In prompt:

```java
Optional<String> saved = RestAuthClient.getInstance().loadPersistedJwt(profileName);
if (saved.isPresent()) {
    // Validate by calling /api/v1/auth/me
    try {
        RestAuthClient.getInstance().setCachedJwt(saved.get());
        RestAuthClient.getInstance().getCurrentUser(serverUrl); // throws if expired
        // JWT valid — silently authenticated, go to main menu
        proceed();
        return;
    } catch (RestAuthException e) {
        RestAuthClient.getInstance().clearPersistedJwt(profileName);
        // Fall through to normal Sign In prompt
    }
}
```

**Step 4: Clear persisted JWT on logout**

In the logout flow, call `clearPersistedJwt(profileName)`.

**Step 5: Pass rememberMe to login API call**

Update `RestAuthClient.login()` to include `"rememberMe": true` in the JSON body when the flag is set. The game server already handles this by issuing a 30-day JWT.

Update `JwtTokenProvider` in `pokergameserver` to check for a `rememberMe` claim in the login request and set the appropriate expiry (30 days vs 24h).

**Step 6: Commit**

```bash
git add code/poker/src/main/java/com/donohoedigital/games/poker/online/RestAuthClient.java
git add code/poker/src/main/java/com/donohoedigital/games/poker/ui/StartupScreen.java
git commit -m "feat(poker): add Remember Me — persist JWT to profile directory for silent re-auth on startup"
```

---

## Task 23: Final integration test and coverage check

**Step 1: Run full build**

```bash
cd code && mvn test -P dev
```

Expected: BUILD SUCCESS, all tests pass.

**Step 2: Run coverage check for pokergameserver**

```bash
cd code && mvn verify -P coverage -pl pokergameserver
```

Check that line coverage is at or above the existing 77% threshold. If coverage dropped, add targeted tests for uncovered branches in `AuthService`, `EmailVerificationFilter`, or `EmailService`.

**Step 3: Run web tests**

```bash
cd code/web && npm test -- --watchAll=false --coverage
```

Review any failing tests and fix them.

**Step 4: Final commit if any fixes were needed**

```bash
git add -A
git commit -m "test: improve coverage for account management feature"
```

---

## Deferred items (do not implement)

- Username rename (deferred — see design doc §15)
- Account deletion (deferred — use `retired` flag for now)
- Concurrent session management / sign-out-all-devices (deferred)
- Lockout count 24h rolling reset (deferred — admin can unlock manually; scheduled cleanup can be added later)
