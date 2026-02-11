# UI Testing Setup Complete ✅

AssertJ Swing UI testing infrastructure has been successfully added to DD Poker!

## What Was Added

### 1. Dependencies (`code/poker/pom.xml`)
- **assertj-swing-junit** (v3.17.1) - Swing UI testing framework

### 2. Test Infrastructure (`code/poker/src/test/java/.../ui/`)
- **PokerUITestBase.java** - Base class for all UI tests
  - Handles application launch/shutdown
  - Provides Robot for UI automation
  - Screenshot utilities for debugging
  - Common helper methods

### 3. Example Tests
- **PokerStartMenuTest.java** - Main menu interaction tests
  - Window visibility checks
  - Button click automation
  - Navigation testing
  - Menu bar verification

- **PlayerProfileDialogTest.java** - Dialog interaction templates
  - Modal dialog handling
  - Text field input
  - Form validation examples
  - Custom matchers

### 4. Documentation
- **docs/UI-TESTING.md** - Comprehensive guide with:
  - How to write UI tests
  - Best practices
  - Debugging tips
  - CI/CD integration
  - Advanced topics

## Quick Start

### Run UI Tests
```bash
cd code

# Run all UI tests
mvn test -Dtest=*UI*Test

# Run specific test
mvn test -Dtest=PokerStartMenuTest

# Run with visible GUI (for debugging)
mvn test -Dtest=PokerStartMenuTest

# Run headless (CI mode)
mvn test -Dtest=*UI*Test -Djava.awt.headless=true
```

### Create Your First Test

1. **Create test class extending PokerUITestBase:**
```java
@Tag("ui")
@Tag("slow")
public class MyFeatureTest extends PokerUITestBase {

    @Test
    void should_DoSomething_When_Triggered() {
        // Click a button
        window.button("myButton").click();

        // Verify result
        window.label("status").requireText("Success");

        // Take screenshot
        takeScreenshot("my-feature");
    }
}
```

2. **Run your test:**
```bash
mvn test -Dtest=MyFeatureTest
```

3. **Debug with screenshots:**
- Screenshots saved to: `code/poker/target/screenshots/`
- Automatically named based on your takeScreenshot() calls

## Key Features

### Available Fixtures
- `window.button("name")` - Find and interact with buttons
- `window.textBox("name")` - Text fields
- `window.label("name")` - Labels
- `window.comboBox("name")` - Dropdowns
- `window.dialog()` - Modal dialogs

### Common Actions
- `.click()` - Click component
- `.enterText("text")` - Type in text field
- `.requireVisible()` - Assert visible
- `.requireText("expected")` - Assert text content

### Debugging
- `takeScreenshot("name")` - Capture UI state
- `waitFor(milliseconds)` - Pause execution
- `robot().waitForIdle()` - Wait for UI thread

## Next Steps

1. **Map Component Names**
   - Review actual UI components in poker application
   - Set meaningful names with `component.setName()`
   - Update test examples with correct names

2. **Add Specific Tests**
   - Practice mode workflow
   - Online game creation
   - Tournament setup
   - Settings dialogs

3. **CI Integration**
   - Tests are tagged with `@Tag("slow")` and `@Tag("ui")`
   - Can be excluded from fast test runs
   - Use xvfb-run on Linux for headless execution

## Documentation

See **docs/UI-TESTING.md** for:
- Complete API reference
- Best practices
- Advanced patterns
- Troubleshooting guide

## Examples in Code

- `PokerUITestBase.java` - Base class setup
- `PokerStartMenuTest.java` - Real test examples
- `PlayerProfileDialogTest.java` - Dialog test patterns

## Status

✅ Dependencies added
✅ Base infrastructure created
✅ Example tests written
✅ Documentation completed
✅ Compilation verified

**Ready to use!** Start writing UI tests to automate your poker application testing.
