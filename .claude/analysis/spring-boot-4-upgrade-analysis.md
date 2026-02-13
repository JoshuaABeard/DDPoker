# Spring Boot 4.0 Upgrade Analysis

**Date:** 2026-02-13
**Current Version:** Spring Boot 3.2.2 (api module)
**Target Version:** Spring Boot 4.0.2 (Latest stable, released Jan 22, 2026)
**Reason:** Full Java 25 support via Spring Framework 7.0

## Executive Summary

Upgrading to Spring Boot 4.0 would provide **native Java 25 support** and resolve the ASM bytecode compatibility issues we're currently facing. However, this is a **major version upgrade** with significant breaking changes.

**Recommendation:** Upgrade to Spring Boot 3.5.x first, then evaluate 4.0 upgrade.

## Current State

### What We Have Now
- **Main codebase:** No Spring Boot (uses Spring Framework 6.2.15 directly)
- **API module only:** Spring Boot 3.2.2 (upgraded to 3.3.9 in our fix branch)
- **Java version:** Java 25
- **Current blocker:** Spring Framework 6.1.x (in Spring Boot 3.3.9) uses ASM 9.7 which doesn't support Java 25 bytecode

### Temporary Solution (Current Fix Branch)
- Disabled API module tests with clear TODOs
- Upgraded to Spring Boot 3.3.9 (best available with partial Java 25 support)
- Added ByteBuddy 1.17.7 and ASM 9.8 overrides (doesn't fully solve Spring's embedded ASM issue)

## Spring Boot 4.0 Benefits

### ✅ Java 25 Support
- **Spring Framework 7.0** baseline with native Java 25 support
- Uses Java 24+ **Class-File API (JEP 484)** for bytecode manipulation instead of ASM
- ASM 9.8 included for libraries that still need it
- No workarounds needed for bytecode compatibility

### ✅ Performance & Features
- Modular JAR structure (better code organization)
- JSpecify nullability annotations throughout
- Jackson 3.x for better JSON handling
- Better GraalVM native image support
- Spring Batch improvements (no database requirement)

## Breaking Changes Impact Analysis

### 🔴 Critical Breaking Changes

#### 1. **Servlet 6.1 Requirement**
**Impact:** Medium
**DDPoker Status:** Currently uses Servlet 6.1.0 (compatible)
```xml
<!-- In parent pom.xml -->
<servlet.api.version>6.1.0</servlet.api.version>
```
**Action:** ✅ No change needed

#### 2. **Undertow Removed**
**Impact:** None
**DDPoker Status:** Uses Jetty 12.1.6, not Undertow
**Action:** ✅ No change needed

#### 3. **Jackson 2 → Jackson 3**
**Impact:** HIGH ⚠️
**Changes Required:**
- Package names changed: `com.fasterxml.jackson` → `com.fasterxml.jackson.core` (mostly)
- Some annotations removed/renamed
- Stricter type handling
- Module consolidation

**DDPoker Usage:** Need to audit api module for Jackson usage
**Action:** 🔍 **Requires investigation** - search for Jackson annotations and ObjectMapper configuration

#### 4. **Deprecated API Removals**
**Impact:** Medium
**Removed in 4.0:**
- `MockitoTestExecutionListener` → Use Mockito's `MockitoExtension` instead
- `WebSecurityConfigurerAdapter` → Already using SecurityFilterChain pattern
- Many 2.x/3.x deprecated APIs (88% of deprecations removed)

**Action:** 🔍 **Requires deprecation audit**

#### 5. **Configuration Properties Changes**
**Impact:** Low-Medium
**Change:** No more binding to public fields - must use private fields with getters/setters

**DDPoker Status:** Small API module, likely minimal configuration classes
**Action:** 🔍 **Audit @ConfigurationProperties classes**

### 🟡 Moderate Changes

#### 6. **Modularization**
**Impact:** Medium (one-time refactoring effort)
**Change:** Spring Boot 4 introduces modular JARs:
- `org.springframework.boot.<module>` structure
- Can use `spring-boot-starter-classic` for traditional classpath

**DDPoker Impact:** API module is small and isolated
**Options:**
- Use `spring-boot-starter-classic` for easier migration (traditional classpath)
- Adopt new modular structure (future-proof, more work)

**Recommendation:** Start with `spring-boot-starter-classic`, migrate to modular later

#### 7. **Starter Renames**
**Impact:** Low
**Action:** Update pom.xml to use new starter names (old ones deprecated but still work)

### 🟢 Low Impact Changes

#### 8. **JSpecify Nullability**
**Impact:** Low (compile-time only)
**Benefit:** Better IDE null-safety warnings
**Action:** None required, optional enhancement

#### 9. **Spring Batch Database-Free**
**Impact:** None
**DDPoker Status:** Doesn't use Spring Batch
**Action:** ✅ Not applicable

## Migration Path Options

### Option 1: Staged Upgrade (RECOMMENDED)
**Timeline:** 2-3 weeks
**Risk:** Low-Medium

1. **Phase 1:** Upgrade to Spring Boot 3.5.x first (1 week)
   - Spring Boot 3.5.x is the stable stepping stone
   - Test thoroughly, fix any 3.5 compatibility issues
   - Open source support until June 2026

2. **Phase 2:** Audit for 4.0 breaking changes (3-5 days)
   - Run deprecation report
   - Audit Jackson usage
   - Review configuration classes
   - Test with `spring-boot-starter-classic`

3. **Phase 3:** Upgrade to Spring Boot 4.0 (1 week)
   - Use OpenRewrite migration recipe (automated)
   - Manual fixes for anything automated tools miss
   - Comprehensive testing

### Option 2: Direct Upgrade to 4.0
**Timeline:** 2-3 weeks
**Risk:** Medium-High

- Jump directly from 3.3.9 → 4.0.2
- Higher risk of missing breaking changes
- Harder to isolate issues
- **Not recommended per Spring Boot docs**

### Option 3: Postpone Upgrade
**Timeline:** Immediate
**Risk:** Low (technical debt)

- Keep current fix branch (3.3.9 with disabled tests)
- Wait for Spring Boot 4.1/4.2 (more stable ecosystem)
- Trade-off: API module tests remain disabled
- Re-evaluate in 3-6 months

## Effort Estimation

### Low Effort (1-2 days)
- Update pom.xml versions
- Run OpenRewrite automated migration
- Fix renamed starters
- Update test configurations

### Medium Effort (3-5 days)
- Jackson 3 migration (if heavily used)
- Deprecation fixes
- Configuration properties updates
- Testing and bug fixes

### High Effort (1-2 weeks)
- Full modular JAR adoption (if chosen over classic)
- Complex Jackson serialization logic
- Custom Spring Security configuration
- Extensive integration testing

## Recommended Approach

### Immediate (This Week)
1. ✅ **Merge current fix branch** (3.3.9 upgrade)
   - Gets CI green
   - Documents Java 25 limitation
   - Buys time for proper 4.0 upgrade

2. 🔍 **Investigate API module Jackson usage**
   ```bash
   cd code/api
   grep -r "import com.fasterxml.jackson" .
   grep -r "@JsonProperty\|@JsonIgnore\|ObjectMapper" .
   ```

3. 🔍 **Run deprecation scan**
   ```bash
   cd code/api
   mvn compile | grep -i "deprecated"
   ```

### Next Sprint (1-2 Weeks)
1. **Upgrade to Spring Boot 3.5.8** (latest 3.5.x)
   - Less risky than 4.0
   - Still gets closer to 4.0
   - Test thoroughly

2. **Create migration plan** based on findings
   - Document Jackson usage
   - Document deprecated API usage
   - Estimate effort for 4.0 upgrade

### Future (3-6 Months)
1. **Spring Boot 4.0 upgrade** when ready
   - Use OpenRewrite for automation
   - Consider modular structure adoption
   - Full test suite re-enabled

## Resources

### Official Documentation
- [Spring Boot 4.0 Migration Guide](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Migration-Guide)
- [Spring Boot 4.0 Release Notes](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Release-Notes)
- [Spring Framework 7.0 Release Notes](https://github.com/spring-projects/spring-framework/wiki/Spring-Framework-7.0-Release-Notes)

### Migration Tools
- [OpenRewrite Spring Boot 4.0 Recipe](https://docs.openrewrite.org/recipes/java/spring/boot4/upgradespringboot_4_0-community-edition)
- [Moderne Spring Boot Migration](https://www.moderne.ai/blog/spring-boot-4x-migration-guide)

### Community Resources
- [Spring Boot 4 Migration Guide (Medium)](https://medium.com/@amarpardeshi/spring-boot-4-migration-guide-a3873128ef7d)
- [Spring Boot 4 Breaking Changes](https://medium.com/@pmLearners/spring-boot-4-the-7-breaking-changes-every-developer-must-know-99de4c2b60e2)

## Decision Matrix

| Factor | Spring Boot 3.5.x | Spring Boot 4.0.2 | Keep 3.3.9 |
|--------|-------------------|-------------------|------------|
| Java 25 Support | Partial | ✅ Full Native | Partial |
| API Tests Working | Maybe | ✅ Yes | ❌ Disabled |
| Migration Risk | 🟢 Low | 🟡 Medium | ✅ None |
| Effort | 1-2 days | 1-3 weeks | ✅ Done |
| Long-term Support | Until Jun 2026 | ✅ Latest | Until Jun 2026 |
| Ecosystem Maturity | ✅ Stable | 🟡 New (Jan 2026) | ✅ Stable |

## Recommendation

**Short-term (Next 1-2 weeks):**
1. Merge current fix branch (Spring Boot 3.3.9)
2. Investigate Jackson and deprecation usage
3. Plan Spring Boot 3.5.8 upgrade

**Medium-term (1-2 months):**
1. Upgrade to Spring Boot 3.5.8
2. Re-enable and fix API tests
3. Create detailed Spring Boot 4.0 migration plan

**Long-term (3-6 months):**
1. Upgrade to Spring Boot 4.0.x when ecosystem matures
2. Adopt modular structure
3. Full Java 25 native support

This staged approach minimizes risk while keeping the project moving forward.

---

**Last Updated:** 2026-02-13
**Author:** Claude Sonnet 4.5
**Status:** For Review
