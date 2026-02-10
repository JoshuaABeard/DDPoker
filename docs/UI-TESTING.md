# DD Poker UI Testing Guide

## Overview

DD Poker uses **AssertJ Swing** for automated UI testing of the Swing-based desktop application. This allows us to programmatically interact with the application just like a user would - clicking buttons, entering text, and verifying UI state.

## Why UI Testing?

- **Catch visual regressions**: Ensure UI changes don't break existing functionality
- **Automated workflows**: Test complete user journeys without manual clicking
- **Confidence in refactoring**: Safely refactor UI code knowing tests will catch issues
- **Documentation**: Tests serve as executable documentation of how the UI works

## Architecture

```
PokerUITestBase (Base class)
├── Robot management
├── Application lifecycle (setup/teardown)
├── Screenshot utilities
└── Common helpers

Specific Test Classes
├── PokerStartMenuTest (Main menu interactions)
├── PlayerProfileDialogTest (Dialog interactions)
└── [Your custom tests]
```

## Running UI Tests

### Run all UI tests:
```bash
cd code
mvn test -Dtest=*UI*Test
```

### Run a specific test:
```bash
mvn test -Dtest=PokerStartMenuTest
```

### Run without UI (headless mode):
```bash
mvn test -Dtest=*UI*Test -Djava.awt.headless=true
```

### Skip UI tests (they're tagged as "slow"):
```bash
mvn test -Dgroups='!ui'
```

## Writing Your First UI Test

### 1. Extend PokerUITestBase

```java
@Tag("ui")
@Tag("slow")
public class MyFeatureTest extends PokerUITestBase {
    // Tests go here
}
```

### 2. Use the `window` fixture

The base class provides a `window` fixture that represents the main application frame:

```java
@Test
void should_DoSomething_When_ConditionMet() {
    // Click a button
    window.button("myButtonName").click();

    // Enter text
    window.textBox("myTextField").enterText("Hello");

    // Verify label
    window.label("statusLabel").requireText("Success!");

    // Take screenshot for debugging
    takeScreenshot("my-feature-test");
}
```

## Key Concepts

### Finding Components

AssertJ Swing provides several ways to find components:

```java
// By name (set via component.setName())
window.button("practice")

// By text
window.button(JButtonMatcher.withText("Practice"))

// By type (finds first matching type)
window.textBox()

// Custom matcher
window.button(new GenericTypeMatcher<JButton>(JButton.class) {
    @Override
    protected boolean isMatching(JButton button) {
        return "Play".equals(button.getText());
    }
})
```

### Component Fixtures

Each component type has its own fixture with specific methods:

- **JButtonFixture**: `click()`, `requireEnabled()`, `requireText()`
- **JTextComponentFixture**: `enterText()`, `requireText()`, `deleteText()`
- **JLabelFixture**: `requireText()`, `requireVisible()`
- **JComboBoxFixture**: `selectItem()`, `requireSelection()`
- **DialogFixture**: `requireModal()`, `close()`

### Dialogs

```java
// Find dialog by title
DialogFixture dialog = window.dialog("Settings");

// Or use a custom matcher
JDialog dialog = robot().finder().find(new GenericTypeMatcher<JDialog>(JDialog.class) {
    @Override
    protected boolean isMatching(JDialog dialog) {
        return dialog.getTitle().contains("Settings");
    }
});
DialogFixture fixture = new DialogFixture(robot(), dialog);
```

### Waiting & Timing

```java
// Wait for robot to be idle (recommended before assertions)
robot().waitForIdle();

// Custom wait
waitFor(1000); // milliseconds

// Wait for component to appear
robot().finder().findAll(matcher).timeout(5000);
```

### Screenshots

Useful for debugging test failures:

```java
takeScreenshot("test-name-description");
// Saves to: target/screenshots/test-name-description.png
```

## Best Practices

### 1. Set Component Names

In production code, set meaningful names for testability:

```java
JButton button = new JButton("Practice");
button.setName("practiceButton");
```

### 2. Use BDD-style Test Names

```java
@Test
void should_ShowErrorDialog_When_InvalidCredentials() { }
```

### 3. Tag UI Tests

```java
@Tag("ui")      // Identifies as UI test
@Tag("slow")    // Excludes from fast test runs
```

### 4. Use Page Objects

For complex screens, create page objects:

```java
public class StartMenuPage {
    private FrameFixture window;

    public StartMenuPage(FrameFixture window) {
        this.window = window;
    }

    public void clickPractice() {
        window.button("practice").click();
    }

    public void clickOnline() {
        window.button("online").click();
    }
}
```

### 5. Handle Asynchronous Operations

```java
// Wait for background operation
robot().waitForIdle();

// Or poll for condition
robot().finder().find(myMatcher).timeout(5000);
```

## Debugging Failed Tests

### 1. Enable Screenshots

Add `takeScreenshot()` calls at key points:

```java
@Test
void myTest() {
    takeScreenshot("before-action");
    window.button("test").click();
    takeScreenshot("after-action");
}
```

### 2. Print Component Hierarchy

```java
import org.assertj.swing.core.BasicComponentPrinter;

BasicComponentPrinter.printComponents(System.out, window.target());
```

### 3. Increase Timeouts

```java
robot().settings().delayBetweenEvents(100);
robot().settings().eventPostingDelay(50);
```

### 4. Run in Non-Headless Mode

Remove `-Djava.awt.headless=true` to see the UI

## Common Issues & Solutions

### Issue: Test Can't Find Component

**Solution 1**: Set component name explicitly
```java
component.setName("myComponent");
```

**Solution 2**: Use custom matcher
```java
window.button(new GenericTypeMatcher<JButton>(JButton.class) {
    protected boolean isMatching(JButton button) {
        return "Text".equals(button.getText());
    }
})
```

### Issue: Test Times Out

**Solution**: Add explicit waits
```java
robot().waitForIdle();
waitFor(500);
```

### Issue: Focus Issues

**Solution**: Explicitly request focus
```java
window.focus();
window.button("test").focus().click();
```

### Issue: Modal Dialog Blocks Test

**Solution**: Use DialogFixture
```java
DialogFixture dialog = window.dialog();
dialog.button("OK").click();
```

## Integration with CI/CD

### GitHub Actions Example:

```yaml
- name: Run UI Tests
  run: |
    cd code
    xvfb-run mvn test -Dtest=*UI*Test
  # xvfb-run provides virtual display on Linux
```

### Windows CI:

```yaml
- name: Run UI Tests
  run: |
    cd code
    mvn test -Dtest=*UI*Test
  # Windows has display by default
```

## Advanced Topics

### Custom Matchers

Create reusable matchers for common patterns:

```java
public class PokerMatchers {
    public static GenericTypeMatcher<JButton> buttonWithIcon(String iconName) {
        return new GenericTypeMatcher<JButton>(JButton.class) {
            protected boolean isMatching(JButton button) {
                Icon icon = button.getIcon();
                return icon != null && icon.toString().contains(iconName);
            }
        };
    }
}
```

### Testing Poker-Specific Components

For custom Swing components (like poker tables, cards):

```java
@Test
void should_DisplayCards_When_DealingHand() {
    // Find custom poker table component
    Container table = robot().finder().find(new GenericTypeMatcher<Container>(Container.class) {
        protected boolean isMatching(Container c) {
            return c.getClass().getName().contains("PokerTable");
        }
    });

    // Verify cards are rendered
    assertThat(table.getComponents()).hasSizeGreaterThan(0);
}
```

### Simulating Keyboard Input

```java
// Type text
window.textBox("name").pressAndReleaseKeys(KeyEvent.VK_H, KeyEvent.VK_I);

// Keyboard shortcuts
window.pressKey(KeyEvent.VK_CONTROL).pressAndReleaseKey(KeyEvent.VK_S).releaseKey(KeyEvent.VK_CONTROL);
```

### Mouse Operations

```java
// Right-click
window.button("test").rightClick();

// Double-click
window.button("test").doubleClick();

// Drag and drop
window.button("card1").drag();
window.button("dropZone").drop();
```

## Resources

- [AssertJ Swing Documentation](https://assertj.github.io/doc/#assertj-swing)
- [AssertJ Swing Javadoc](https://joel-costigliola.github.io/assertj/swing/api/)
- [Swing Testing Best Practices](https://github.com/joel-costigliola/assertj-swing/wiki)

## Examples in Codebase

- `PokerUITestBase.java` - Base class with setup/teardown
- `PokerStartMenuTest.java` - Main menu interactions
- `PlayerProfileDialogTest.java` - Dialog interactions (templates)

## Contributing

When adding UI tests:

1. Extend `PokerUITestBase`
2. Tag with `@Tag("ui")` and `@Tag("slow")`
3. Use BDD-style naming: `should_X_When_Y()`
4. Add screenshots for debugging
5. Document any custom matchers or helpers

---

**Happy Testing!** 🎰🃏
