# Spring Boot 4.0 Upgrade Plan

**Status:** paused
**Priority:** Medium
**Created:** 2026-02-13
**Target:** Q2 2026 (After current sprint)

## Goal

Upgrade the API module from Spring Boot 3.3.9 to Spring Boot 4.0.2+ to achieve native Java 25 support and re-enable all API tests.

## Context

### Current Situation
- **Main codebase:** Uses Spring Framework 6.2.15 directly (no Spring Boot)
- **API module:** Spring Boot 3.3.9 (just upgraded from 3.2.2)
- **Java version:** Java 25
- **Test status:** 11 tests disabled due to ASM/Java 25 incompatibility
  - `EmailServiceTest` (3 tests) - requires DI refactoring
  - `ProfileControllerTest` (8 tests) - Spring Framework 6.1.x ASM limitation

### Why Upgrade?
1. **Native Java 25 support** - Spring Framework 7.0 uses Java 24+ Class-File API (JEP 484)
2. **Re-enable API tests** - No more ASM bytecode compatibility issues
3. **Future-proof** - Latest stable release with long-term support
4. **Performance improvements** - Better GraalVM native image support, optimizations

### Alternatives Considered
1. ❌ **Downgrade to Java 21 LTS** - Goes against project's Java 25 adoption
2. ❌ **Wait for Spring Boot 3.4.x** - Still uses Spring Framework 6.2.x (ASM 9.8, not Class-File API)
3. ✅ **Upgrade to Spring Boot 4.0** - Best long-term solution

## Prerequisites

### Phase 0: Investigation (1-2 days)

**Tasks:**
- [ ] Audit Jackson usage in API module
  ```bash
  cd code/api
  grep -r "import com.fasterxml.jackson" .
  grep -r "@JsonProperty\|@JsonIgnore\|@JsonCreator\|@JsonValue" .
  grep -r "ObjectMapper" .
  ```
- [ ] Run deprecation scan
  ```bash
  cd code/api
  mvn compile 2>&1 | grep -i "deprecated"
  ```
- [ ] List all `@ConfigurationProperties` classes
  ```bash
  grep -r "@ConfigurationProperties" code/api/src
  ```
- [ ] Review Spring Security configuration
  ```bash
  find code/api/src -name "*SecurityConfig.java" -o -name "*Security*.java"
  ```
- [ ] Document current Spring Boot starters
  ```bash
  grep "spring-boot-starter" code/api/pom.xml
  ```

**Deliverables:**
- Investigation findings document
- List of required changes
- Risk assessment
- Final effort estimate

## Breaking Changes Checklist

### 🔴 High Impact

#### 1. Jackson 2.x → Jackson 3.x
**What changed:**
- Package reorganization (some classes moved)
- Removed/renamed annotations
- Stricter type handling
- Module consolidation
- `ObjectMapper` behavior changes

**Actions:**
- [ ] Inventory all Jackson annotations used
- [ ] Test JSON serialization/deserialization
- [ ] Update custom serializers/deserializers (if any)
- [ ] Review Jackson configuration in `application.yml`
- [ ] Update any `ObjectMapper` bean configurations

**Risk:** High if custom serialization logic exists, Low if using standard annotations

#### 2. Deprecated API Removals (88% of 2.x/3.x deprecations)
**Removed in 4.0:**
- `MockitoTestExecutionListener` → Use `@ExtendWith(MockitoExtension.class)`
- `WebSecurityConfigurerAdapter` → Already using `SecurityFilterChain` pattern ✅
- Configuration property binding to public fields

**Actions:**
- [ ] Replace `MockitoTestExecutionListener` if used
- [ ] Convert public fields in `@ConfigurationProperties` to private + getters/setters
- [ ] Fix any other deprecated API usage found in Phase 0

### 🟡 Medium Impact

#### 3. Modularization
**What changed:**
- New modular JAR structure: `org.springframework.boot.<module>`
- Legacy classpath available via `spring-boot-starter-classic`

**Decision needed:**
- **Option A:** Use `spring-boot-starter-classic` (easier, traditional classpath)
- **Option B:** Adopt modular structure (future-proof, more work)

**Recommended:** Start with Option A, migrate to Option B later

**Actions:**
- [ ] Add `spring-boot-starter-classic` to pom.xml (temporary)
- [ ] Test application starts and runs correctly
- [ ] Plan future migration to modular structure (separate plan)

#### 4. Starter Renames
**Actions:**
- [ ] Update renamed starters in pom.xml
- [ ] Remove deprecated starter references

**Note:** Old starters still work but are deprecated

### 🟢 Low Impact

#### 5. Servlet 6.1 Requirement
**Status:** ✅ Already compatible
- Current: Servlet API 6.1.0
- Required: Servlet 6.1+
- No action needed

#### 6. Undertow Removed
**Status:** ✅ Not applicable
- DDPoker uses Jetty, not Undertow
- No action needed

#### 7. JSpecify Nullability Annotations
**Status:** Optional enhancement
- Spring Boot 4.0 adds JSpecify annotations throughout
- Benefit: Better IDE null-safety warnings
- No breaking changes, purely additive

## Implementation Plan

### Phase 1: Preparation (Week 1)

**Day 1-2: Investigation**
- Complete Phase 0 tasks
- Document all findings
- Create detailed task list
- Get stakeholder approval

**Day 3-4: Environment Setup**
- Create new worktree: `spring-boot-4-upgrade`
- Update test data/fixtures if needed
- Prepare rollback plan

**Day 5: Dependency Review**
- Check all dependency compatibility with Spring Boot 4.0
- Identify any dependencies that need updating
- Document version changes needed

### Phase 2: Staged Upgrade (Week 2)

**Step 1: Upgrade to Spring Boot 3.5.8 (2-3 days)**
```xml
<spring-boot.version>3.5.8</spring-boot.version>
```

**Why this step:**
- Recommended by Spring Boot migration guide
- Catches 3.x → 3.5 breaking changes separately
- Reduces risk by breaking upgrade into smaller steps

**Actions:**
- [ ] Update pom.xml to 3.5.8
- [ ] Run full test suite
- [ ] Fix any 3.5-specific issues
- [ ] Verify application starts correctly
- [ ] Document any issues encountered

**Step 2: Upgrade to Spring Boot 4.0.2 (2-3 days)**
```xml
<spring-boot.version>4.0.2</spring-boot.version>
```

**Actions:**
- [ ] Update pom.xml to 4.0.2
- [ ] Add `spring-boot-starter-classic` dependency
- [ ] Remove ASM/ByteBuddy overrides (no longer needed)
- [ ] Update Spring Security test version if needed
- [ ] Run OpenRewrite migration recipe:
  ```bash
  mvn -U org.openrewrite.maven:rewrite-maven-plugin:run \
    -Drewrite.recipeArtifactCoordinates=org.openrewrite.recipe:rewrite-spring:LATEST \
    -Drewrite.activeRecipes=org.openrewrite.java.spring.boot4.UpgradeSpringBoot_4_0
  ```
- [ ] Review OpenRewrite changes
- [ ] Manual fixes for anything automated tools missed

### Phase 3: Fix Breaking Changes (Week 3)

**Jackson Migration:**
- [ ] Update package imports
- [ ] Fix annotation changes
- [ ] Update ObjectMapper configuration
- [ ] Test all API endpoints for correct JSON serialization
- [ ] Integration test all JSON request/response handling

**Test Fixes:**
- [ ] Re-enable `EmailServiceTest` (still need DI refactoring)
- [ ] Re-enable `ProfileControllerTest`
- [ ] Fix any test incompatibilities
- [ ] Replace `MockitoTestExecutionListener`
- [ ] Verify all tests pass

**Configuration:**
- [ ] Fix `@ConfigurationProperties` public field issues
- [ ] Update starter names
- [ ] Review `application.yml` for deprecated properties

### Phase 4: Testing & Validation (Week 3-4)

**Unit Tests:**
- [ ] All tests pass
- [ ] Code coverage maintained/improved
- [ ] No skipped tests

**Integration Tests:**
- [ ] API endpoints work correctly
- [ ] Authentication/authorization works
- [ ] JSON serialization/deserialization correct
- [ ] Database operations work

**Manual Testing:**
- [ ] Application starts without errors
- [ ] All API endpoints accessible
- [ ] Error handling works correctly
- [ ] Logging works correctly

**Performance:**
- [ ] Compare startup time (should be similar or better)
- [ ] API response times (should be similar or better)
- [ ] Memory usage (should be similar or better)

### Phase 5: Documentation & Deployment (Week 4)

**Documentation:**
- [ ] Update README with Spring Boot 4.0 requirement
- [ ] Document any configuration changes
- [ ] Update deployment guides
- [ ] Add migration notes to CHANGELOG

**Code Review:**
- [ ] Self-review all changes
- [ ] Create review handoff document
- [ ] Spawn review agent (Opus)
- [ ] Address review feedback

**Deployment:**
- [ ] Merge to main
- [ ] Deploy to staging
- [ ] Monitor for issues
- [ ] Deploy to production

## Rollback Plan

### If Issues Found During Testing
1. **Minor issues:** Fix and continue
2. **Major issues:**
   - Document issue
   - Revert to Spring Boot 3.5.8 or 3.3.9
   - Investigate and plan fix
   - Retry upgrade when ready

### Rollback Procedure
```bash
# If using worktree
git worktree remove ../DDPoker-spring-boot-4-upgrade
git branch -D spring-boot-4-upgrade

# Revert main if already merged
git revert <merge-commit-sha>
```

## Success Criteria

### Required (Must Have)
- ✅ All tests pass (0 skipped, 0 failures)
- ✅ Application starts without errors
- ✅ All API endpoints functional
- ✅ No regressions in functionality
- ✅ Code coverage maintained

### Desired (Nice to Have)
- ✅ Improved startup time
- ✅ Better null-safety warnings in IDE
- ✅ Modular JAR structure adopted (or planned)
- ✅ Documentation complete

## Risk Assessment

### High Risk Areas
1. **Jackson 3 migration** - Custom serialization logic
2. **Deprecated API usage** - Unknown scope until audit complete
3. **Third-party library compatibility** - May need dependency updates

### Mitigation Strategies
1. **Thorough investigation** - Complete Phase 0 before committing
2. **Staged upgrade** - 3.5.8 first, then 4.0.2
3. **Automated tools** - Use OpenRewrite for reliable migration
4. **Comprehensive testing** - Unit, integration, and manual tests
5. **Rollback plan** - Clear procedure if issues arise

## Dependencies

### Blocks
- None (can start anytime)

### Blocked By
- Current CI fix merged ✅ (Completed 2026-02-13)

### Related Plans
- None currently

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

### Internal Documentation
- `.claude/analysis/spring-boot-4-upgrade-analysis.md` - Detailed analysis

## Timeline Estimate

| Phase | Duration | Start After |
|-------|----------|-------------|
| Phase 0: Investigation | 1-2 days | Anytime |
| Phase 1: Preparation | 5 days | Phase 0 complete |
| Phase 2: Staged Upgrade | 4-6 days | Phase 1 complete |
| Phase 3: Fix Breaking Changes | 5-7 days | Phase 2 complete |
| Phase 4: Testing | 3-5 days | Phase 3 complete |
| Phase 5: Documentation & Deployment | 2-3 days | Phase 4 complete |

**Total Estimated Duration:** 3-4 weeks (15-23 working days)

**Calendar Time:** 4-6 weeks (accounting for reviews, discussions, unexpected issues)

## Notes

### Assumptions
- API module is relatively small and isolated
- Jackson usage is standard (no complex custom serialization)
- Limited use of deprecated Spring Boot APIs
- Current test suite is comprehensive

### Open Questions
- How extensively is Jackson used? (Audit needed)
- Are there any custom `ObjectMapper` configurations? (Audit needed)
- Are there any Spring Batch dependencies? (Unlikely based on codebase)
- Should we adopt modular structure now or later? (Recommend later)

### Future Considerations
- After Spring Boot 4.0 is stable, consider migrating from `spring-boot-starter-classic` to modular structure
- Refactor `EmailService` for proper dependency injection (separate task)
- Consider GraalVM native image compilation (separate investigation)

---

**Last Updated:** 2026-02-13
**Plan Owner:** Development Team
**Review Date:** Start of next sprint
**Status:** Ready for Phase 0 (Investigation)
