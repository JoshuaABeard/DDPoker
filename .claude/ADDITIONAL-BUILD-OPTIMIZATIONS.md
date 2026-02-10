# Additional Build Optimization Opportunities

## Current Performance Analysis

**Current timings (incremental build with `-T 1C`):**
- **Total**: ~139 seconds
- **Slowest modules**:
  1. common: 29.7s (122 tests, core utilities)
  2. pokerserver: 29.3s (34 tests, Hibernate initialization)
  3. poker: 22.2s (integration tests, game engine)
  4. gameserver: 12.8s
  5. pokerwicket: 6.8s

## High-Impact Optimizations (Recommended)

### 1. Maven Daemon (mvnd) - 20-30% Faster
**Impact**: High | **Effort**: Low | **Estimated savings**: 15-30 seconds

Maven Daemon keeps a warm JVM running, eliminating startup overhead.

**Installation**:
```bash
# Download from https://github.com/apache/maven-mvnd/releases
# Or use package manager
scoop install mvnd  # Windows (Scoop)
brew install mvnd   # macOS
```

**Usage** (drop-in replacement for mvn):
```bash
mvnd clean test -T 1C  # Instead of mvn
mvnd test              # Incremental builds
```

**Benefits**:
- Persistent JVM (no startup cost)
- Smart class loading
- Better dependency caching
- Built-in parallelism

### 2. Increase Parallel Thread Count - 10-20% Faster
**Impact**: Medium | **Effort**: Very Low | **Estimated savings**: 10-20 seconds

Current: 2 threads per core (conservative)
Recommended: 4 threads per core (aggressive)

**Implementation**:
```xml
<!-- In code/pom.xml maven-surefire-plugin config -->
<threadCount>4</threadCount>  <!-- Change from 2 to 4 -->
```

Or use runtime override:
```bash
mvn test -T 1C -DthreadCount=4
```

**Trade-off**: Higher CPU usage, may cause resource contention on slower machines

### 3. Test Categorization with Smart Execution - 30-50% Faster (Development)
**Impact**: High (for dev builds) | **Effort**: Medium | **Estimated savings**: 30-60 seconds

Separate fast unit tests from slow integration tests.

**Implementation**:

1. **Tag slow tests**:
```java
@Tag("slow")
@Tag("integration")
class HibernateIntegrationTest {
    // Slow Hibernate tests
}
```

2. **Create development profile**:
```xml
<profile>
  <id>dev</id>
  <build>
    <plugins>
      <plugin>
        <artifactId>maven-surefire-plugin</artifactId>
        <configuration>
          <!-- Skip slow tests in dev builds -->
          <excludedGroups>slow</excludedGroups>
        </configuration>
      </plugin>
    </plugins>
  </build>
</profile>
```

3. **Usage**:
```bash
mvn test -T 1C -Pdev     # Fast: ~40-60s (unit tests only)
mvn test -T 1C           # Full: ~90s (all tests)
mvn verify -T 1C         # Complete: with coverage
```

### 4. Optimize Test Logging - 5-10% Faster
**Impact**: Low-Medium | **Effort**: Low | **Estimated savings**: 5-10 seconds

Reduce test output verbosity.

**Create** `code/src/test/resources/log4j2-test.xml`:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<Configuration status="WARN">
  <Appenders>
    <Console name="Console" target="SYSTEM_ERR">
      <PatternLayout pattern="%d{HH:mm:ss.SSS} [%-5level] %msg%n"/>
    </Console>
  </Appenders>
  <Loggers>
    <Root level="WARN">  <!-- Was INFO/DEBUG, now WARN -->
      <AppenderRef ref="Console"/>
    </Root>
    <!-- Only show errors during tests -->
    <Logger name="org.hibernate" level="ERROR"/>
    <Logger name="org.apache.wicket" level="ERROR"/>
    <Logger name="com.donohoedigital" level="WARN"/>
  </Loggers>
</Configuration>
```

### 5. Disable Unnecessary Plugins in Development - 5-10% Faster
**Impact**: Low | **Effort**: Low | **Estimated savings**: 5-10 seconds

Skip plugins not needed during development.

**Add to fast profile**:
```xml
<profile>
  <id>fast</id>
  <properties>
    <jacoco.skip>true</jacoco.skip>
    <maven.javadoc.skip>true</maven.javadoc.skip>
    <maven.source.skip>true</maven.source.skip>
    <checkstyle.skip>true</checkstyle.skip>
  </properties>
</profile>
```

## Medium-Impact Optimizations

### 6. Module-Specific Test Optimization
**Impact**: Medium | **Effort**: Medium | **Estimated savings**: Variable

**Target slowest modules:**

**common module (29.7s, 122 tests)**:
- Many tests involve file I/O with `@TempDir`
- Consider: `@TestInstance(Lifecycle.PER_CLASS)` to amortize setup
- Reuse test fixtures where safe

**pokerserver module (29.3s, 34 tests)**:
- Heavy Hibernate initialization (SessionFactory creation)
- **Optimization**: Share SessionFactory across test class
  ```java
  @TestInstance(Lifecycle.PER_CLASS)
  class HibernateTest {
      private static SessionFactory sessionFactory;

      @BeforeAll
      void setupSessionFactory() {
          // Create once for all tests
          sessionFactory = buildSessionFactory();
      }
  }
  ```

**poker module (22.2s)**:
- Many integration tests with game engine setup
- Already using `IntegrationTestBase` (good!)
- Consider parallel execution within test classes (methods)

### 7. Increase Surefire Fork Memory - 5% Faster
**Impact**: Low | **Effort**: Very Low

Current: 512MB per fork
Recommended: 1GB for test-heavy modules

**In code/pom.xml**:
```xml
<argLine>-Xmx1024m -XX:+UseParallelGC @{argLine}</argLine>
```

### 8. Enable JUnit Parallel Execution (Methods) - 10-20% Faster
**Impact**: Medium | **Effort**: Medium | **Risk**: Medium

Current: Parallel classes only
Possible: Parallel methods within classes

**Configuration**:
```xml
<parallel>classesAndMethods</parallel>
<threadCount>8</threadCount>
<perCoreThreadCount>true</perCoreThreadCount>
```

**Risk**: Requires all tests to be truly independent (no shared state within class)

### 9. Resource Filtering Optimization
**Impact**: Low | **Effort**: Low

Disable resource filtering if not using Maven property substitution.

**Check if needed**:
```bash
grep -r "\${" code/*/src/main/resources
```

If no results, disable filtering:
```xml
<resources>
  <resource>
    <directory>src/main/resources</directory>
    <filtering>false</filtering>  <!-- Explicitly disable -->
  </resource>
</resources>
```

## Advanced Optimizations

### 10. Maven Build Cache Extension - 40-60% Faster (Incremental)
**Impact**: High (incremental) | **Effort**: Medium

Cache build outputs to skip unchanged modules.

**Setup** (add to `code/.mvn/extensions.xml`):
```xml
<?xml version="1.0" encoding="UTF-8"?>
<extensions>
  <extension>
    <groupId>org.apache.maven.extensions</groupId>
    <artifactId>maven-build-cache-extension</artifactId>
    <version>1.2.0</version>
  </extension>
</extensions>
```

**Configuration** (create `code/.mvn/maven-build-cache-config.xml`):
```xml
<?xml version="1.0" encoding="UTF-8" ?>
<cache xmlns="http://maven.apache.org/BUILD-CACHE-CONFIG/1.0.0">
  <configuration>
    <enabled>true</enabled>
    <hashAlgorithm>SHA-256</hashAlgorithm>
    <remote enabled="false"/> <!-- Local cache only -->
  </configuration>
</cache>
```

**Benefits**: Skip compiling/testing unchanged modules

### 11. Test Result Caching - Skip Passing Tests
**Impact**: High (incremental) | **Effort**: Low

Surefire can skip tests that haven't changed.

**Configuration**:
```xml
<plugin>
  <artifactId>maven-surefire-plugin</artifactId>
  <configuration>
    <rerunFailingTestsCount>0</rerunFailingTestsCount>
    <skipAfterFailureCount>1</skipAfterFailureCount>
    <!-- Run only tests affected by code changes -->
    <runOrder>failedfirst</runOrder>
  </configuration>
</plugin>
```

### 12. JVM Startup Optimization
**Impact**: Low | **Effort**: Medium

Reduce JVM startup time with AppCDS (Application Class-Data Sharing).

**Generate class list**:
```bash
java -Xshare:off -XX:DumpLoadedClassList=classes.lst \
  -jar target/poker.jar
```

**Create shared archive**:
```bash
java -Xshare:dump -XX:SharedClassListFile=classes.lst \
  -XX:SharedArchiveFile=app-cds.jsa
```

**Use in tests**:
```xml
<argLine>-Xshare:on -XX:SharedArchiveFile=app-cds.jsa</argLine>
```

## Benchmarking Tools

### Measure Build Performance

**Maven profiler**:
```bash
mvn clean test -T 1C -Dprofile
# Generates .buildprofile.json
```

**Analyze with speedscope**:
```bash
npm install -g speedscope
speedscope .buildprofile.json
```

**Module timing**:
```bash
mvn test -T 1C | grep "SUCCESS \["
```

## Recommended Implementation Order

### Phase 1: Quick Wins (30 minutes, 25-40% improvement)
1. ✅ Install Maven Daemon (mvnd)
2. ✅ Increase parallel thread count to 4
3. ✅ Reduce test logging verbosity
4. ✅ Disable unnecessary plugins in fast profile

**Expected result**: 90s → 60-70s

### Phase 2: Test Organization (2 hours, additional 20-30%)
1. ✅ Tag slow/integration tests
2. ✅ Create dev profile (unit tests only)
3. ✅ Optimize common module test fixtures
4. ✅ Optimize pokerserver Hibernate initialization

**Expected result**: Dev builds 40-50s, Full builds 60-70s

### Phase 3: Advanced (4+ hours, additional 10-20%)
1. ✅ Enable Maven build cache
2. ✅ Parallel method execution (carefully)
3. ✅ Module-specific optimizations

**Expected result**: Dev builds 30-40s, Incremental 20-30s

## Current vs. Optimized Performance

| Build Type | Current | After Phase 1 | After Phase 2 | After Phase 3 |
|------------|---------|---------------|---------------|---------------|
| **Clean full** | 90s | 60-70s | 60-70s | 50-60s |
| **Incremental full** | 65s | 45-55s | 45-55s | 35-45s |
| **Dev (unit only)** | N/A | N/A | 35-45s | 25-35s |
| **Incremental dev** | N/A | N/A | 30-40s | 20-30s |
| **Single module** | 5-30s | 3-20s | 3-20s | 2-15s |

## Trade-offs

### More Parallelism
- ✅ **Pro**: Faster builds
- ⚠️ **Con**: Higher CPU/memory usage
- ⚠️ **Con**: May overwhelm slower machines

### Test Categorization
- ✅ **Pro**: Very fast dev builds
- ⚠️ **Con**: May miss integration issues
- ✅ **Mitigation**: Run full suite pre-commit/CI

### Maven Daemon
- ✅ **Pro**: Significant speedup
- ✅ **Pro**: No code changes needed
- ⚠️ **Con**: Additional tool to install
- ⚠️ **Con**: Memory overhead (persistent JVM)

### Build Cache
- ✅ **Pro**: Huge speedup for incremental builds
- ⚠️ **Con**: Cache invalidation complexity
- ⚠️ **Con**: Initial setup effort

## Commands Summary

```bash
# Current optimized build
mvn test -T 1C                          # 65s (incremental), 90s (clean)

# Phase 1: Maven daemon + more threads
mvnd test -T 1C -DthreadCount=4         # 45-55s estimated

# Phase 2: Dev profile (unit tests only)
mvnd test -T 1C -Pdev                   # 35-45s estimated

# Phase 3: With build cache (incremental)
mvnd test -T 1C -Pdev                   # 25-35s estimated

# Single module (fastest)
cd code/poker && mvnd test              # 3-8s estimated
```

## Monitoring Build Performance

### Track build times
```bash
# Add to .bashrc / .zshrc
alias mvn-time='time mvn "$@"'
alias mvnd-time='time mvnd "$@"'

# Usage
mvnd-time test -T 1C
```

### Build dashboard
```bash
# Install maven-profiler
mvn com.github.jcgay.maven.profiler:maven-profiler-plugin:0.1:profile
```

## Conclusion

**Immediate actions** (30 min, big impact):
1. Install mvnd: `scoop install mvnd` or `brew install mvnd`
2. Increase thread count: Use `-DthreadCount=4`
3. Use fast profile: `mvnd test -T 1C -Pfast`

**Expected outcome**: 90s → 50-60s (35-45% faster)

**Medium-term** (2-4 hours):
- Tag slow tests
- Optimize module-specific bottlenecks
- Enable build cache

**Expected outcome**: Dev builds 30-40s, full builds 50-60s

**Best practice**: Use `mvnd test -T 1C -Pdev` during development, `mvnd verify -T 1C` pre-commit.
