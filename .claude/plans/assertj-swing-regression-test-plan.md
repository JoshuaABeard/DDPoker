# AssertJ Swing Regression Test Plan

## Context

DD Poker has 120 tests across the project but only **2 UI test classes** (with just 6 real test methods) out of **217 Swing UI components**. The existing `PokerUITestBase` + AssertJ Swing infrastructure was added recently (commit `682824b`) but remains skeletal. The `PlayerProfileDialogTest` has 2 of 3 tests `@Disabled` as templates.

This plan creates a comprehensive UI regression test suite organized with the Page Object pattern, prioritizing the core user flows: application launch, start menu navigation, first-time wizard, player profile management, and tournament setup.

## Scope

- **Primary focus:** UI/Swing tests using AssertJ Swing
- **Deliverables:** Gap analysis roadmap + implemented tests (highest priority first)
- **Organization:** Tagged tests for selective execution, Page Object pattern

---

## Phase 1: Foundation - Test Infrastructure

### 1a. Custom Matchers

**New file:** `code/poker/src/test/java/com/donohoedigital/games/poker/ui/matchers/PokerMatchers.java`

Reusable matchers for DD Poker's custom Swing components:
- `ddImageButtonNamed(String name)` - finds `DDImageButton` by name (buttons use image icons, not text)
- `internalDialogWithTitle(String titleContains)` - finds `JInternalFrame` (the app uses `InternalDialog`, not `JDialog`)
- `ddLabelWithText(String textContains)` - finds `DDLabel` by text content

### 1b. Test Profile Helper

**New file:** `code/poker/src/test/java/com/donohoedigital/games/poker/ui/TestProfileHelper.java`

Utility for test setup/teardown:
- `ensureDefaultProfileExists(String name)` - creates a profile so FirstTimeWizard doesn't block non-wizard tests
- `clearAllProfiles()` - removes profiles so wizard tests start clean
- `clearWizardPreferences()` - resets `ftue/wizard_completed` prefs to force wizard display

### 1c. Enhance `PokerUITestBase`

**Modify:** `code/poker/src/test/java/com/donohoedigital/games/poker/ui/PokerUITestBase.java`

Add helper methods:
- `findInternalDialog(String titleContains)` - locates InternalDialog (JInternalFrame) by title
- `waitForCondition(BooleanSupplier, timeoutMs, description)` - polling wait with timeout
- `printComponentHierarchy()` - debug helper to dump component tree

### 1d. Page Objects

**New directory:** `code/poker/src/test/java/com/donohoedigital/games/poker/ui/pages/`

| Page Object | Wraps | Key Methods |
|---|---|---|
| `StartMenuPage` | Main start menu | `clickPractice()`, `clickOnline()`, `clickOptions()`, `clickCalc()`, `requireAllBigButtonsPresent()`, `getProfileLabelText()` |
| `TournamentOptionsPage` | Practice mode setup | `clickStart()`, `clickCancel()`, `isStartEnabled()` |
| `OnlineMenuPage` | Online submenu | `clickLobby()`, `clickCancel()`, button assertions |
| `FirstTimeWizardPage` | FTUE wizard dialog | `enterPlayerName()`, `clickNext()`, `clickBack()`, `clickFinish()`, `getCurrentStepTitle()` |
| `GamePrefsPage` | Options screen | `clickOkay()`, `clickCancel()`, `clickReset()` |

Page objects receive `FrameFixture` + `Robot` as constructor args. Navigation methods return `void` or the next page object. Wait logic is encapsulated inside page objects.

---

## Phase 2: Start Menu Tests

**Modify:** `code/poker/src/test/java/com/donohoedigital/games/poker/ui/PokerStartMenuTest.java`

Keep existing 5 tests, add:
- `should_ShowAllBigButtons_When_OnMainMenu()` - verify "practice", "analysis", "pokerclock", "online" via page object
- `should_ShowAllControlButtons_When_OnMainMenu()` - verify "exit", "calc", "options", "support", "help", "register"
- `should_ShowProfileSummary_When_ProfileExists()` - verify profile label text contains player name
- `should_NavigateToAnalysis_When_AnalysisButtonClicked()`
- `should_NavigateToPokerClock_When_PokerClockButtonClicked()`
- `should_NavigateToOptions_When_OptionsButtonClicked()`
- `should_NavigateToCalculator_When_CalcButtonClicked()`

**New file:** `code/poker/src/test/java/com/donohoedigital/games/poker/ui/StartMenuNavigationTest.java`

Round-trip navigation tests:
- `should_ReturnToStartMenu_When_CancelFromTournamentOptions()`
- `should_ReturnToStartMenu_When_CancelFromOnlineMenu()`
- `should_ReturnToStartMenu_When_CancelFromGamePrefs()`

Button names come from `gamedef.xml` StartMenu phase params (e.g., `practice.phase=TournamentOptions` -> button name is `"practice"`).

---

## Phase 3: Dialog & Wizard Tests

**New file:** `code/poker/src/test/java/com/donohoedigital/games/poker/ui/FirstTimeWizardUITest.java`

Tags: `@Tag("ui")`, `@Tag("slow")`, `@Tag("wizard")`
Setup: `clearAllProfiles()` + `clearWizardPreferences()` so wizard appears on launch

Tests:
- `should_ShowWizard_When_NoProfileExists()`
- `should_ShowPlayModeStep_When_WizardOpens()`
- `should_EnableNextButton_When_PlayModeSelected()`
- `should_ShowProfileStep_When_OfflineModeSelectedAndNextClicked()`
- `should_ShowValidationError_When_EmptyNameAndNextClicked()`
- `should_CreateProfile_When_WizardCompleted()`
- `should_ReturnToPreviousStep_When_BackClicked()`

**Modify:** `code/poker/src/test/java/com/donohoedigital/games/poker/ui/PlayerProfileDialogUITest.java`

Replace the disabled templates with working tests:
- `should_OpenProfileDialog_When_ProfileButtonClicked()`
- `should_ShowCurrentPlayerName_When_DialogOpens()`
- `should_CloseDialog_When_CancelClicked()`
- `should_UpdateProfileName_When_OkClickedWithNewName()`

**New file:** `code/poker/src/test/java/com/donohoedigital/games/poker/ui/GamePrefsUITest.java`

- `should_ShowGamePrefsScreen_When_OptionsClicked()`
- `should_ReturnToStartMenu_When_CancelClicked()`
- `should_ReturnToStartMenu_When_OkClicked()`

---

## Phase 4: Tournament Setup & Game Flow

**New file:** `code/poker/src/test/java/com/donohoedigital/games/poker/ui/TournamentOptionsUITest.java`

Tags: `@Tag("ui")`, `@Tag("slow")`, `@Tag("tournament")`

- `should_ShowTournamentOptions_When_PracticeClicked()`
- `should_ShowButtons_When_TournamentOptionsVisible()` - verify "okaystart", "loadgame", "cancelprev"
- `should_ReturnToStartMenu_When_CancelClicked()`
- `should_StartGame_When_StartButtonClicked()` - select default profile, click start, verify poker table visible

**New file:** `code/poker/src/test/java/com/donohoedigital/games/poker/ui/PokerTableUITest.java`

Tags: `@Tag("ui")`, `@Tag("slow")`, `@Tag("gameplay")`

- `should_ShowPokerTable_When_GameStarted()`
- `should_ShowDashboardPanel_When_GameStarted()`
- `should_ReturnToStartMenu_When_QuitGameConfirmed()`

---

## Phase 5: Online Menu & Tools

**New file:** `code/poker/src/test/java/com/donohoedigital/games/poker/ui/OnlineMenuUITest.java`

- `should_ShowOnlineMenu_When_OnlineClicked()`
- `should_ShowAllButtons_When_OnlineMenuVisible()` - "lobby", "hostonline", "joinonline", "cancelprev"
- `should_ReturnToStartMenu_When_CancelClicked()`

**New file:** `code/poker/src/test/java/com/donohoedigital/games/poker/ui/SimulatorDialogUITest.java`

- `should_OpenSimulator_When_CalcClicked()`
- `should_CloseSimulator_When_CloseButtonClicked()`

---

## Tag Strategy

| Tag | Purpose | Run Command |
|---|---|---|
| `@Tag("ui")` | All UI tests | `mvn test -Dgroups=ui -pl poker` |
| `@Tag("slow")` | Exclude from fast runs | `mvn test -Dgroups='!slow'` |
| `@Tag("smoke")` | Quick sanity (~5 tests) | `mvn test -Dgroups=smoke -pl poker` |
| `@Tag("wizard")` | FirstTimeWizard only | `mvn test -Dgroups=wizard -pl poker` |
| `@Tag("dialog")` | Dialog tests only | `mvn test -Dgroups=dialog -pl poker` |
| `@Tag("tournament")` | Tournament setup tests | `mvn test -Dgroups=tournament -pl poker` |
| `@Tag("gameplay")` | In-game table tests | `mvn test -Dgroups=gameplay -pl poker` |
| `@Tag("online")` | Online menu tests | `mvn test -Dgroups=online -pl poker` |

---

## Key Technical Notes

1. **Button naming:** Buttons are `DDImageButton` named via `GameButton.getName()`. The StartMenu uses params like `practice.phase=TournamentOptions` which yields button name `"practice"`. `window.button("practice")` works.

2. **Dialogs are JInternalFrame, not JDialog:** The app uses `InternalDialog` backed by `JInternalFrame`. Standard `window.dialog()` won't work. Use `robot().finder().find(internalDialogWithTitle(...))`.

3. **Profile check blocks startup:** `PokerStartMenu.InitLabel.paintComponent()` triggers `profileCheck()` on first paint. If no profile exists, the FirstTimeWizard opens modally. Non-wizard tests must pre-create a profile via `TestProfileHelper`.

4. **Async phase transitions:** Button clicks trigger `SwingUtilities.invokeLater()`. Always call `robot().waitForIdle()` after clicks before assertions.

5. **gamedef.xml reference:** `code/poker/src/main/resources/config/poker/gamedef.xml` defines all phase names, button names, and dialog parameters.

---

## Files to Modify

| File | Action |
|---|---|
| `code/poker/src/test/java/.../ui/PokerUITestBase.java` | Add InternalDialog finder, waitForCondition, printComponentHierarchy |
| `code/poker/src/test/java/.../ui/PokerStartMenuTest.java` | Add ~7 new test methods |
| `code/poker/src/test/java/.../ui/PlayerProfileDialogTest.java` | Replace disabled templates with working tests |

## Files to Create

| File | Purpose |
|---|---|
| `.../ui/matchers/PokerMatchers.java` | Custom AssertJ Swing matchers for DD components |
| `.../ui/TestProfileHelper.java` | Profile/prefs setup utility for test isolation |
| `.../ui/pages/StartMenuPage.java` | Page object for start menu |
| `.../ui/pages/TournamentOptionsPage.java` | Page object for tournament options |
| `.../ui/pages/OnlineMenuPage.java` | Page object for online menu |
| `.../ui/pages/FirstTimeWizardPage.java` | Page object for FTUE wizard |
| `.../ui/pages/GamePrefsPage.java` | Page object for game preferences |
| `.../ui/StartMenuNavigationTest.java` | Round-trip navigation tests |
| `.../ui/FirstTimeWizardUITest.java` | Wizard flow tests |
| `.../ui/GamePrefsUITest.java` | Game preferences tests |
| `.../ui/TournamentOptionsUITest.java` | Tournament setup tests |
| `.../ui/PokerTableUITest.java` | In-game table tests |
| `.../ui/OnlineMenuUITest.java` | Online menu tests |
| `.../ui/SimulatorDialogUITest.java` | Calculator/simulator tests |

All new files under `code/poker/src/test/java/com/donohoedigital/games/poker/ui/`

---

## Verification

1. `mvn test -Dgroups=ui -pl poker` - all UI tests pass
2. `mvn test -Dgroups=smoke -pl poker` - smoke subset passes quickly
3. `mvn test -pl poker` - existing tests unaffected
4. `mvn verify -Pcoverage -pl poker` - coverage baseline maintained or improved
5. Manual review: screenshots in `target/screenshots/` show expected UI state at each test step
