# Spring Boot 3.5.8 Upgrade - Follow-up Items

**Date:** 2026-02-13
**Related Review:** `.claude/reviews/spring-boot-4-upgrade-to-3-5-8.md`
**Status:** Tracked for future work

These items were identified during code review as low-priority improvements that should NOT block the Spring Boot 3.5.8 upgrade PR. They are documented here for future tracking.

---

## Issue 1: Migrate @MockBean to @MockitoBean (Spring Boot 4.0 Prep)

**Priority:** Low
**Target:** Spring Boot 4.0 upgrade
**Effort:** Small (15 minutes)

**Description:**

`@MockBean` is deprecated in Spring Boot 3.4+ and will be removed in Spring Boot 4.0. The API module uses `@MockBean` in ProfileControllerTest.

**Current Code:**
```java
// File: code/api/src/test/java/com/donohoedigital/poker/api/controller/ProfileControllerTest.java
// Lines 61, 64

@MockBean
private OnlineProfileService profileService;

@MockBean
private EmailService emailService;
```

**Required Changes for Spring Boot 4.0:**
```java
// Import changes:
// OLD: import org.springframework.boot.test.mock.mockito.MockBean;
// NEW: import org.springframework.test.context.bean.override.mockito.MockitoBean;

@MockitoBean
private OnlineProfileService profileService;

@MockitoBean
private EmailService emailService;
```

**Deprecation Warning:**
```
[WARNING] [removal] MockBean in org.springframework.boot.test.mock.mockito has been deprecated
and marked for removal
```

**References:**
- [Spring Boot Issue #39860](https://github.com/spring-projects/spring-boot/issues/39860) - Deprecate @MockBean and @SpyBean
- [Spring Boot 4.0 Migration Guide](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Migration-Guide)

**Action Items:**
- [ ] When upgrading to Spring Boot 4.0, update imports from `org.springframework.boot.test.mock.mockito.MockBean` to `org.springframework.test.context.bean.override.mockito.MockitoBean`
- [ ] Change `@MockBean` annotations to `@MockitoBean`
- [ ] Verify tests still pass

---

## Issue 2: Resolve Jackson Version Mix Across Modules

**Priority:** Low
**Target:** Next dependency cleanup sprint
**Effort:** Medium (1-2 hours)

**Description:**

The project has mixed Jackson versions across modules, which is technically unsupported by the Jackson project:
- `jackson-databind:2.18.2` from `common` module
- `jackson-datatype-jdk8:2.19.4`, `jackson-datatype-jsr310:2.19.4`, `jackson-module-parameter-names:2.19.4` from Spring Boot 3.5.8's `spring-boot-starter-json`

While Jackson minor versions within the 2.x line are generally compatible (and tests pass), mixing minor versions is not officially supported.

**Root Cause:**

The `common` module declares:
```xml
<!-- File: code/common/pom.xml, line 111 -->
<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-databind</artifactId>
    <version>2.18.2</version>
</dependency>
```

Spring Boot 3.5.8 provides Jackson 2.19.4, but `common` module's explicit declaration takes precedence for transitive dependencies.

**Recommended Solution:**

**Option A (Preferred):** Remove explicit Jackson version from `common` module and let Spring Boot manage it:
```xml
<!-- code/common/pom.xml -->
<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-databind</artifactId>
    <!-- Version managed by Spring Boot -->
</dependency>
```

**Option B:** Use Jackson BOM to ensure consistent versions:
```xml
<!-- code/pom.xml (parent) -->
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>com.fasterxml.jackson</groupId>
            <artifactId>jackson-bom</artifactId>
            <version>2.19.4</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

**Testing Required:**
- [ ] Verify `common` module unit tests pass with Jackson 2.19.4
- [ ] Run full project test suite (all 19 modules)
- [ ] Check for any Jackson serialization/deserialization behavior changes

**References:**
- [Jackson Releases](https://github.com/FasterXML/jackson/wiki/Jackson-Releases)
- [Spring Boot Dependency Versions](https://docs.spring.io/spring-boot/appendix/dependency-versions.html)

---

## Issue 3: Align Spring Framework Versions Across Modules

**Priority:** Low (Informational)
**Target:** Next Spring Framework upgrade
**Effort:** Small (30 minutes)

**Description:**

The parent POM declares `spring.version=6.2.15`, while Spring Boot 3.5.8 ships with Spring Framework 6.2.14. This causes a mix:
- `spring-context:6.2.15`, `spring-jdbc:6.2.15`, `spring-orm:6.2.15` (from parent-managed modules)
- `spring-webmvc:6.2.14`, `spring-core:6.2.14`, `spring-aop:6.2.14` (from Spring Boot 3.5.8)

This works because 6.2.15 is a patch on top of 6.2.14, but mixing patch versions is not ideal.

**Current Configuration:**
```xml
<!-- File: code/pom.xml (parent), line 72 -->
<spring.version>6.2.15</spring.version>
```

**Recommended Solution:**

Remove explicit Spring version from parent POM and let Spring Boot manage it entirely:
```xml
<!-- code/pom.xml (parent) -->
<!-- Remove or comment out: -->
<!-- <spring.version>6.2.15</spring.version> -->
```

This will use Spring Framework 6.2.14 (from Spring Boot 3.5.8) consistently across all modules.

**Alternative:**

If specific Spring Framework version is required, update to match Spring Boot:
```xml
<spring.version>6.2.14</spring.version>
```

**Testing Required:**
- [ ] Run full project test suite (all 19 modules)
- [ ] Verify no regressions in desktop client, server, or API

**Note:** This is pre-existing and not introduced by this PR. It's informational and low risk.

---

## Issue 4: Document Why spring-security-test Version is Hardcoded ✅ **COMPLETED**

**Priority:** Low
**Status:** ✅ **Addressed**
**Effort:** Trivial (5 minutes)

**Description:**

The `spring-security-test` version was hardcoded (6.5.7) rather than managed by Spring Boot's dependency management. This creates a maintenance burden when upgrading Spring Boot.

**Resolution:**

Added explanatory comment in `code/api/pom.xml`:
```xml
<dependency>
    <groupId>org.springframework.security</groupId>
    <artifactId>spring-security-test</artifactId>
    <!-- Hardcoded version: parent POM doesn't provide dependency management for spring-security-test.
         Must match Spring Security version used by Spring Boot 3.5.8 (currently 6.5.7).
         Update this when upgrading Spring Boot. -->
    <version>6.5.7</version>
    <scope>test</scope>
</dependency>
```

**Why Hardcoded:**

The parent POM (`code/pom.xml`) does not include Spring Boot's dependency management BOM, so `spring-security-test` version is not automatically managed. Attempting to remove the version results in:
```
ERROR: 'dependencies.dependency.version' for org.springframework.security:spring-security-test:jar is missing
```

**Future Improvement:**

Consider adding Spring Boot BOM to parent POM for centralized dependency management:
```xml
<!-- code/pom.xml (parent) -->
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-dependencies</artifactId>
            <version>3.5.8</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

**Note:** This would affect dependency resolution across all 19 modules and requires comprehensive testing.

---

## Summary

| Issue | Priority | Target | Effort | Status |
|-------|----------|--------|--------|--------|
| #1: @MockBean → @MockitoBean | Low | Spring Boot 4.0 | Small | 🟡 Tracked |
| #2: Jackson Version Mix | Low | Next cleanup | Medium | 🟡 Tracked |
| #3: Spring Framework Versions | Low | Next upgrade | Small | 🟡 Tracked |
| #4: spring-security-test Comment | Low | Immediate | Trivial | ✅ Completed |

**All items are documented and tracked for future work.**

---

## Recommendation

These items should NOT block the Spring Boot 3.5.8 upgrade PR. They are:
- Pre-existing issues not introduced by this upgrade (#2, #3)
- Forward-looking improvements for future upgrades (#1)
- Already addressed with documentation (#4)

The upgrade is ready to merge.
