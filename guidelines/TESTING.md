# Testing Best Practices - Test-Driven Development

## Core Philosophy

**Write tests BEFORE implementation whenever possible.**

Tests aren't just verification - they're design tools that force you to think about:
- What should this code do?
- What are the edge cases?
- What's the simplest interface?

## Test-First Workflow

### 1. Understand the Requirement
Before writing ANY code:
- What's the expected behavior?
- What are the inputs and outputs?
- What can go wrong?

### 2. Write the Test
```csharp
[Fact]
public void NewFeature_WithValidInput_ReturnsExpectedResult()
{
    // Arrange - setup
    var sut = new MyClass();
    
    // Act - invoke
    var result = sut.NewFeature("valid input");
    
    // Assert - verify
    result.Should().Be("expected output");
}
```

**The test will fail** - that's expected! You haven't implemented it yet.

### 3. Implement Minimum Code
Write the **simplest** code that makes the test pass:
- No extra features
- No "what if" scenarios not covered by tests
- Just enough to go green

### 4. Refactor (If Needed)
Now that tests pass:
- Simplify the implementation
- Remove duplication
- Improve naming
- **Tests must still pass**

### 5. Repeat
For the next requirement, go back to step 1.

## Test Types by Purpose

### Property-Based Tests (FSCheck)
**When**: Testing invariants, security properties, edge cases

**Pattern**:
```csharp
[Property(MaxTest = 500)]
public Property SystemInvariant_AlwaysHolds(ValidInput input)
{
    var result = SystemUnderTest(input.Get);
    return (invariant_condition).ToProperty()
        .Label($"Failed for: {input.Get}");
}
```

**Use for**:
- XSS prevention (no `<script>` tags ever)
- Routing consistency (query always routes to exactly one agent)
- Input validation (malformed input never crashes)

### Example-Based Tests (Theory + InlineData)
**When**: Documenting specific expected behaviors

**Pattern**:
```csharp
[Theory]
[InlineData("input1", "expected1")]
[InlineData("input2", "expected2")]
public void Feature_WithKnownInput_ProducesKnownOutput(string input, string expected)
{
    var result = FeatureUnderTest(input);
    result.Should().Be(expected);
}
```

**Use for**:
- Concrete examples from requirements
- Regression tests (bugs found in production)
- Known edge cases

### Component Tests (bUnit)
**When**: Testing Blazor UI logic

**Pattern**:
```csharp
[Fact]
public void Component_RendersExpectedMarkup()
{
    using var ctx = new TestContext();
    var cut = ctx.RenderComponent<MyComponent>(parameters => parameters
        .Add(p => p.InputProp, "test value"));
    
    cut.Markup.Should().Contain("expected output");
}
```

### Integration Tests
**When**: Verifying real external dependencies work

**Pattern**:
```csharp
[Fact]
public async Task McpProvider_WithRealServer_LoadsTools()
{
    // This actually starts the MCP server process
    var provider = new FetchMcpProvider(...);
    await provider.InitializeAsync(ct);
    
    provider.IsConnected.Should().BeTrue();
    provider.Tools.Should().NotBeEmpty();
}
```

**Note**: These are slower, require configuration (API keys, etc.)

### E2E Tests (Playwright)
**When**: Critical user flows only

**Pattern**:
```csharp
[Test]
public async Task User_CanSendMessage_AndReceiveResponse()
{
    await Page.GotoAsync("http://localhost:5076");
    await Page.FillAsync("textarea", "test query");
    await Page.ClickAsync("button:has-text('Send')");
    await Expect(Page.Locator(".message.assistant")).ToBeVisibleAsync();
}
```

**Note**: Slowest, most brittle. Use sparingly for smoke tests.

## TDD Anti-Patterns

### ❌ Writing Tests After Implementation
**Why bad**: You'll write tests that just confirm what you did, not what's correct

**Fix**: Write tests first, even if it feels slower initially

### ❌ Testing Implementation Details
```csharp
// BAD - testing private methods
var result = UseReflection(obj, "PrivateMethod");

// GOOD - testing public behavior
var result = obj.PublicMethod();
```

**Why bad**: Refactoring breaks tests even when behavior doesn't change

**Fix**: Test public interfaces only

### ❌ One Giant Test for Everything
```csharp
// BAD
[Fact]
public void TestEverything() { /* 200 lines */ }

// GOOD
[Fact] public void Feature_Scenario1() { }
[Fact] public void Feature_Scenario2() { }
[Fact] public void Feature_EdgeCase() { }
```

**Why bad**: When it fails, you don't know what broke

**Fix**: One assertion per test (or closely related assertions)

### ❌ Tests Depend on Each Other
```csharp
// BAD - test 2 assumes test 1 ran first
[Fact] public void Test1_CreateData() { }
[Fact] public void Test2_UsesDataFromTest1() { }

// GOOD - each test is independent
[Fact] public void Test1() { /* setup, act, assert */ }
[Fact] public void Test2() { /* own setup, act, assert */ }
```

**Why bad**: Test order shouldn't matter, tests run in parallel in CI

**Fix**: Each test creates its own test data

### ❌ Mocking Everything
```csharp
// BAD - over-mocking makes tests brittle
var mock1 = new Mock<IDep1>();
var mock2 = new Mock<IDep2>();
var mock3 = new Mock<IDep3>();
// ... 10 more mocks

// GOOD - use real objects when simple
var dep1 = new RealDep1();  // Simple, no side effects
var mock2 = new Mock<IExternalApi>();  // Only mock expensive/external stuff
```

**Why bad**: Tests break when implementation changes, even if behavior is correct

**Fix**: Mock only external dependencies (APIs, databases, file I/O)

## Test Coverage Targets

- **Overall**: 65% minimum (enforced in CI)
- **Services**: 80%+ (business logic)
- **MCP Providers**: 85%+ (integration points)
- **Models**: 95%+ (simple, testable)
- **UI Components**: 25-30% (bUnit for critical formatting)

**Not aiming for 100%**: Diminishing returns. Focus on high-value paths.

## When NOT to Write Tests First

**Spiking/Exploring**: Investigating how something works
- Write throwaway code to learn
- Once you understand, **delete it** and write tests + proper implementation

**Truly Trivial Code**: Simple DTOs, pass-through methods
- `public string Name { get; set; }` doesn't need a test
- Use judgment

**Proof-of-Concept**: Showing feasibility before commitment
- Get it working, show stakeholders
- If approved, **rewrite with tests**

## Fast Feedback Loop

### Run Relevant Tests Frequently
```powershell
# Watch mode - re-run on file save
dotnet watch test --project tests/PersonalAssistant.UnitTests

# Run specific test
dotnet test --filter "FullyQualifiedName~MyFeature_Scenario"

# Run category
dotnet test --filter "FullyQualifiedName~PropertyTesting"
```

### Test Execution Time Targets
- **Unit tests**: <30 seconds total (377 tests)
- **Per unit test**: <100ms average
- **Integration tests**: <3 minutes
- **E2E tests**: <10 minutes

If tests are slow, people won't run them.

## Red-Green-Refactor Cycle

```
RED:    Write a failing test
  ↓
GREEN:  Make it pass (minimal code)
  ↓
REFACTOR: Clean up (tests still green)
  ↓
Repeat
```

**Key**: Never refactor without green tests. Never add features without tests.

## Test Naming Convention

```csharp
[MethodName]_[Scenario]_[ExpectedBehavior]

// Examples:
ClassifyQuery_WithMathQuestion_ReturnsSimpleAgent
FormatMessage_WithScriptTag_RemovesScriptTag
InitializeAsync_WithMissingApiKey_ThrowsInvalidOperationException
```

**Why**: Test name should explain what went wrong when it fails.

## Mocking Best Practices

### Use Moq for Interface Dependencies
```csharp
var mockLogger = new Mock<ILogger<MyClass>>();
var mockConfig = new Mock<IConfiguration>();
mockConfig.Setup(c => c["Key"]).Returns("value");

var sut = new MyClass(mockLogger.Object, mockConfig.Object);
```

### Verify Interactions When Important
```csharp
// Act
await service.DoWorkAsync();

// Assert
mockRepository.Verify(r => r.SaveAsync(It.IsAny<Data>()), Times.Once);
```

### Don't Over-Verify
```csharp
// BAD - brittle, implementation-focused
mockLogger.Verify(l => l.LogInformation(...), Times.Exactly(3));

// GOOD - behavior-focused
result.Should().NotBeNull();  // Verify outcome, not how it logged
```

## FluentAssertions Style

Use FluentAssertions for readable assertions:

```csharp
// Instead of Assert.Equal
result.Should().Be(expected);

// Instead of Assert.True
result.Should().BeTrue();

// Collections
results.Should().HaveCount(5);
results.Should().Contain(x => x.Name == "test");

// Exceptions
act.Should().Throw<InvalidOperationException>()
    .WithMessage("*API key*");  // Wildcard matching
```

## Resources

- **xUnit Docs**: https://xunit.net/
- **FluentAssertions**: https://fluentassertions.com/
- **Moq**: https://github.com/moq/moq4
- **FSCheck**: https://fscheck.github.io/FsCheck/
- **bUnit**: https://bunit.dev/

## Related Documentation

- [Test Suite Overview](../docs/testing/TEST_SUITE.md) - Project test organization
- [Testing Strategy](../docs/testing/TESTING_STRATEGY.md) - Project-specific approach
- [Unit Tests README](../tests/PersonalAssistant.UnitTests/README.md) - How to run tests
