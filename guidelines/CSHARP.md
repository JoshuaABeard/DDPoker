# C# Language & Code Quality Guidelines

## Code Quality Standards

### Warnings Policy
- **All compiler warnings must be resolved** before committing code
- Treat warnings as errors - they often indicate real issues
- If a warning truly cannot be resolved (rare), document why before suppressing
- Use `#pragma warning disable` only as a last resort, with `#pragma warning restore`

### Build Standards
- Builds should complete with zero warnings
- Run `dotnet build` and verify clean output before committing
- Address nullable reference warnings - they prevent null reference exceptions
- Fix all code analysis warnings (CA rules) when possible

## Language Feature Guidelines

### Avoid Newer C# Features (Tooling Compatibility)

Avoid C# language features that VS Code's C# extension doesn't fully support, even if they compile fine:

#### ❌ Avoid These Features:
- **Raw string literals** (`"""..."""`) - Use `@"..."` verbatim strings instead
- **Collection expressions** (`[item1, item2]`) - Use `new[] { item1, item2 }` or `new List<T> { }` instead
- **Primary constructors** for classes - Use traditional constructors
- **Spread operator** (`[.. items]`) - Use `.ToArray()` or `.ToList()` instead

#### ✅ Use These Instead:
```csharp
// Verbatim strings for multi-line text
public string Instructions => @"Line 1
Line 2
Line 3";

// Traditional array/list initialization
Arguments = new[] { "-y", "package-name" },

// Traditional constructors
public class MyService
{
    private readonly ILogger _logger;
    
    public MyService(ILogger<MyService> logger)
    {
        _logger = logger;
    }
}
```

#### Why?
1. **IDE Support**: VS Code C# extension shows false errors for these features
2. **Tooling**: Analyzers and formatters work better with traditional syntax
3. **Readability**: Team members may not be familiar with newest syntax

Note: These features compile fine with `dotnet build` - the limitation is IDE tooling, not the compiler.

## Logging Standards

Use structured logging with emoji prefixes for quick visual scanning:

```csharp
_logger.LogInformation("🔍 Searching for {Query}...", query);
_logger.LogInformation("✅ Found {Count} results", results.Count);
_logger.LogWarning("⚠️ Rate limited, retrying in {Seconds}s", waitTime);
_logger.LogError(ex, "❌ Failed to connect: {Message}", ex.Message);
```

## Dependency Injection Patterns

### Service Lifetimes
- **Singleton**: MCP providers, agent services (expensive to create, share connections)
- **Scoped**: Per-request services, database contexts
- **Transient**: Lightweight stateless services

### Registration Order
Register in `Program.cs` in dependency order:
```csharp
// 1. Infrastructure (providers)
builder.Services.AddSingleton<FetchMcpProvider>();

// 2. Registries/Managers
builder.Services.AddSingleton<McpProviderRegistry>();

// 3. High-level services
builder.Services.AddSingleton<ResearchAgentService>();
```
