# Phase 3 Unit Testing Plan: gamecommon Module

**Status:** SUPERSEDED by `SERVER-HOSTED-GAME-ENGINE.md` (2026-02-15)

> **Note:** Target classes (Territory, GamePiece, EngineMessage, GameState, GamePhase, SaveDetails) are
> board-game framework abstractions, old binary protocol, or old save system — all bypassed or replaced
> by the server hosting plan. Testing effort is better directed at the new `pokergameserver` module
> and `pokergamecore` tests.

## Context

Phase 1 and Phase 2 of the unit testing plan have successfully added comprehensive tests for poker engine primitives (`pokerengine` module: 65% coverage, 240 tests) and core poker logic (`poker` module: 21% coverage, 47 tests). Phase 3 continues this momentum by targeting the `gamecommon` module - the shared game infrastructure layer that both client and server depend on.

The `gamecommon` module currently has **46 source files with only 1 test file (GameConfigUtilsTest) and 0% coverage threshold**. This phase will create **7 new test files** covering the core data structures for game state management, player representation, territory/board management, and game messaging. These classes are pure data structures and business logic with **minimal Swing dependencies**, making them ideal testing targets.

The goal is to raise coverage from **0% → 15%** while establishing comprehensive test coverage for classes that are critical to both single-player and multiplayer game functionality.

---

## Target Classes (Priority Ordered)

Based on code analysis, these classes will be tested in priority order:

### Very High Priority
1. **GameState.java** (793 lines) - Core game persistence and state container
2. **Territory.java** (1,466 lines) - Game board entity (largest class in module)

### High Priority
3. **GamePlayer.java** (505 lines) - Player representation

### Medium Priority
4. **GamePieceContainerImpl.java** (210 lines) - Game piece management (testable in isolation)
5. **EngineMessage.java** (341 lines) - Game messaging protocol
6. **GamePhase.java** (218 lines) - Phase configuration

### Low Priority
7. **SaveDetails.java** (208 lines) - Save strategy configuration (simple data class, quick coverage wins)

### Skip
- **Territories.java** - Container class, test through Territory integration tests
- **SaveFile.java** - Interface only (2 lines), test through GameState implementation

---

## Implementation Plan

### Test File 1: GameStateTest.java
**Path**: `code/gamecommon/src/test/java/com/donohoedigital/games/config/GameStateTest.java`

**Source**: `code/gamecommon/src/main/java/com/donohoedigital/games/config/GameState.java`

**Key Testing Areas**:
1. **Constructors** (5 variations):
   - File-based: `GameState(File, boolean)` with/without header loading
   - Byte array: `GameState(byte[])`
   - New save: `GameState(String gameName, String name, String desc, String dir)`
   - Online game: `GameState(String gameName, File, String onlineID, String name, String desc)`
   - Multi-game: `GameState(String gameName, String dir)`

2. **File Operations**:
   - `getFile()`, `getFileNumber()`, `isFileInSaveDirectory()`
   - `delete()` - verify file removal
   - `resetFileSaveDir()` - file relocation
   - `isOnlineGame()`, `setOnlineGameID()`

3. **Metadata**:
   - `getGameName()`, `getName()`, `setName()`
   - `getDescription()`, `setDescription()`
   - `lastModified()` timestamp
   - `getSaveDetails()`, `setSaveDetails()`

4. **Entry Management**:
   - `addEntry()` - add game state entries
   - `removeEntry()` - remove by type
   - `peekEntry()` - access without removal
   - `initForSave()` / `initForLoad()` - mode switching

5. **Static Methods**:
   - `setDelegate()` / `getDelegate()` - delegate management
   - `getSaveFileList()` - enumerate saved games

**Testing Challenges**:
- **File I/O**: Use temp directories for test files (`@TempDir` JUnit annotation)
- **Obfuscation**: The Hide class is used in write/read cycles - may need to mock or use real implementation
- **Static delegate**: Use `@BeforeEach` to reset `GameStateDelegate` to avoid test pollution
- **Entry tokenization**: Verify entry line format (type + data + ENTRY_ENDLINE)

**Test Count Estimate**: 15-20 tests

---

### Test File 2: TerritoryTest.java
**Path**: `code/gamecommon/src/test/java/com/donohoedigital/games/config/TerritoryTest.java`

**Source**: `code/gamecommon/src/main/java/com/donohoedigital/games/config/Territory.java`

**Key Testing Areas**:
1. **Constructors**:
   - Basic: `Territory(Integer id, String sName, String sArea, String sType)`
   - Verify ID, name, area, type initialization

2. **ID and Naming**:
   - `getID()`, `setID()` - unique identifier
   - `getName()`, `setName()` - territory name
   - `getMapDisplayName()`, `getDisplayName()` - formatted names

3. **Type Management**:
   - `getType()`, `setType()`, `resetType()` - territory classification
   - `getCategory()`, `getArea()`, `setArea()` - grouping

4. **Type Checks**:
   - `isEdge()`, `isDecoration()`, `setDecoration()`
   - `isWater()`, `setWater()`, `isLand()`, `setLand()`
   - `isIsland()` - land with no adjacent land

5. **Adjacency**:
   - `getAdjacentTerritories()` - list of neighbors
   - `isAdjacent(Territory)` - basic adjacency check
   - `isAdjacent(GamePlayer, int)` - player-filtered adjacency
   - Test wrap-around borders

6. **GamePieceContainer Delegation**:
   - `setGamePlayer()`, `getGamePlayer()` - owner
   - `addGamePiece()`, `removeGamePiece()` - piece management
   - `getNumPieces()`, `getGamePieces()` - queries
   - `hasNonOwnerGamePiece()`, `hasMovedPieces()`

7. **Serialization**:
   - `addGameStateEntry()` - save to game state
   - `loadFromGameStateEntry()` - restore from saved data

8. **Dirty State**:
   - `setDirty()`, `isDirty()` - change tracking
   - `setAllDirty()` - static batch update

9. **User Data**:
   - `setUserData()`, `getUserData()` - generic storage
   - `getUserInt()`, `setUserInt()` - typed access
   - `setUserFlag()`, `isUserFlag()` - boolean flags

**Testing Challenges**:
- **AWT dependencies**: GeneralPath, Rectangle2D - use real implementations (they work without UI)
- **Static state**: `Territory.setTerritories()`, `Territory.setAreas()` must be set up in `@BeforeEach`
- **GamePieceContainerImpl**: Test delegation behavior, may need mock GamePiece objects
- **Complex initialization**: Focus on simple constructor, defer XML-based constructor
- **Path calculations**: Skip `createPath()`, `getScaledPath()` tests - too GUI-dependent

**Test Count Estimate**: 20-25 tests

---

### Test File 3: GamePlayerTest.java
**Path**: `code/gamecommon/src/test/java/com/donohoedigital/games/config/GamePlayerTest.java`

**Source**: `code/gamecommon/src/main/java/com/donohoedigital/games/config/GamePlayer.java`

**Key Testing Areas**:
1. **Constructors**:
   - Empty: `GamePlayer()` - for deserialization
   - With params: `GamePlayer(int id, String name)`

2. **ID Management**:
   - `getID()`, `getObjectID()` - player identifier
   - `isHost()` - verify HOST_ID (0) detection

3. **State Management**:
   - `isEliminated()`, `setEliminated()` - elimination status
   - `isCurrentGamePlayer()`, `setCurrentGamePlayer()` - turn tracking
   - `isObserver()`, `setObserver()` - observer mode
   - `isComputer()` - AI detection (based on GameAI)
   - `isDirty()`, `setDirty()` - change tracking

4. **Properties**:
   - `getName()`, `setName()` - player name
   - `getGameAI()`, `setGameAI()` - AI assignment (can be null)
   - `getLastPoll()`, `setLastPoll()` - online game heartbeat

5. **Info Map** (TypedHashMap):
   - `putInfo()`, `getInfo()`, `removeInfo()` - custom data storage
   - Verify order preservation

6. **Serialization**:
   - `addGameStateEntry()` - save to game state
   - `loadFromGameStateEntry()` - restore from saved data

7. **PropertyChangeListener**:
   - `addPropertyChangeListener()`, `removePropertyChangeListener()`
   - Verify listener firing when name/state changes
   - Test with property name filter

**Testing Challenges**:
- **SwingPropertyChangeSupport**: The class uses SwingUtilities.invokeLater() for thread safety
  - **Mitigation**: Use CountDownLatch or Awaitility to wait for async events in tests
  - Alternative: Set listeners and verify they were called (don't validate threading)
- **GameAI dependency**: May need mock or stub GameAI for isComputer() tests
- **Static HOST_ID**: Verify special behavior for player ID 0

**Test Count Estimate**: 12-15 tests

---

### Test File 4: GamePieceContainerImplTest.java
**Path**: `code/gamecommon/src/test/java/com/donohoedigital/games/config/GamePieceContainerImplTest.java`

**Source**: `code/gamecommon/src/main/java/com/donohoedigital/games/config/GamePieceContainerImpl.java`

**Key Testing Areas**:
1. **Constructor**:
   - `GamePieceContainerImpl(GamePieceContainer implFor)` - verify delegation setup

2. **Info Delegation**:
   - `getName()`, `getDisplayName()`, `getObjectID()` - forward to implFor

3. **Owner Management**:
   - `getGamePlayer()`, `setGamePlayer()` - owner assignment

4. **Piece Management**:
   - `addGamePiece()` - add piece (assert piece.getContainer() == null)
   - `removeGamePiece()` - remove piece
   - `getNumPieces()` - count
   - `getGamePieces()` - get all pieces

5. **Queries**:
   - `getGamePiece(int type, GamePlayer owner)` - find by type/owner
   - `hasNonOwnerGamePiece()` - detect foreign pieces
   - `hasMovedPieces()` - movement tracking

6. **Utility**:
   - `getMap()` - synchronized access to internal map
   - `equals()` - identity comparison

**Testing Challenges**:
- **GamePiece dependency**: Need mock/stub GamePiece with:
  - `getContainer()`, `setContainer()` for add/remove contract
  - `getType()`, `getGamePlayer()` for queries
  - `hasMoved()` for movement tracking
  - `transferAllTokensTo()` for duplicate handling
- **Synchronization**: Use Collections.synchronizedSortedMap, test thread safety (optional)
- **Assertion on add**: Verify adding piece with existing container fails (assertion error in dev mode)

**Test Count Estimate**: 10-12 tests

---

### Test File 5: EngineMessageTest.java
**Path**: `code/gamecommon/src/test/java/com/donohoedigital/games/comms/EngineMessageTest.java`

**Source**: `code/gamecommon/src/main/java/com/donohoedigital/games/comms/EngineMessage.java`

**Key Testing Areas**:
1. **Constructors** (6 variations):
   - Empty: `EngineMessage()`
   - Basic: `EngineMessage(String gameID, int fromPlayerID, int cat)`
   - With string: `EngineMessage(String gameID, int fromPlayerID, int cat, String data)`
   - With bytes: `EngineMessage(String gameID, int fromPlayerID, int cat, byte[] data)`
   - With file: `EngineMessage(String gameID, int fromPlayerID, int cat, File data)`
   - With files: `EngineMessage(String gameID, int fromPlayerID, int cat, File[] data)`

2. **Game/Player Info**:
   - `getGameID()`, `setGameID()` - game identifier
   - `getFromPlayerID()`, `setFromPlayerID()` - sender ID
   - Test special player IDs: PLAYER_NOTDEFINED (-1), PLAYER_SERVER (-2), PLAYER_GROUP (-3)

3. **Sequence Tracking**:
   - `getSeqID()`, `setSeqID()` - message ordering

4. **Category Validation**:
   - Test all 16 message category constants (CAT_SERVER_QUERY, CAT_NEW_GAME, etc.)
   - `getDebugCat()` - verify category string mapping for all 22 cases

5. **Debugging Methods**:
   - `getDebugInfoShort()` - compact message info
   - `getDebugInfo()` - standard message info
   - `getDebugInfoLong()` - detailed message info
   - `getDebugFrom()` - sender info

**Testing Challenges**:
- **Parent class DDMessage**: Inherited behavior from base module
  - Test basic construction, but defer deep data/parameter testing to parent tests
- **File data handling**: Constructors with File/File[] - use temp files (`@TempDir`)
- **@DataCoder annotation**: Serialization framework - may need MsgState context for full testing

**Test Count Estimate**: 12-15 tests

---

### Test File 6: GamePhaseTest.java
**Path**: `code/gamecommon/src/test/java/com/donohoedigital/games/config/GamePhaseTest.java`

**Source**: `code/gamecommon/src/main/java/com/donohoedigital/games/config/GamePhase.java`

**Key Testing Areas**:
1. **XML Construction**:
   - Create JDOM2 Element fixtures for testing
   - `GamePhase(Element phase, Namespace ns, String attrErrorDesc)`

2. **Configuration Access**:
   - `getName()` - phase name
   - `getClassName()` - implementation class name
   - `getClassObject()` - class loading (test valid and invalid class names)

3. **Flags**:
   - `isCached()`, `isHistory()`, `isTransient()`, `isWindow()`
   - `getWindowName()` - for window phases

4. **Inheritance**:
   - Test phase extending another phase (extends attribute)
   - Verify parent property merging
   - Test circular inheritance detection (should fail)

5. **Utility**:
   - `getButtonNameFromParam(String)` - parameter name parsing
   - `toString()` - readable representation

**Testing Challenges**:
- **XML fixtures**: Create helper method to build JDOM2 Element from string
  ```java
  private Element buildPhaseElement(String xml) throws Exception {
      SAXBuilder builder = new SAXBuilder();
      Document doc = builder.build(new StringReader("<root>" + xml + "</root>"));
      return doc.getRootElement().getChildren().get(0);
  }
  ```
- **Static allPhases_**: Use `@BeforeEach` to set up GamePhases for inheritance resolution
- **Class loading**: Test with real classes (e.g., String.class) and fake class names
- **Namespace**: Use JDOM2 Namespace.NO_NAMESPACE for simple tests

**Test Count Estimate**: 8-10 tests

---

### Test File 7: SaveDetailsTest.java
**Path**: `code/gamecommon/src/test/java/com/donohoedigital/games/config/SaveDetailsTest.java`

**Source**: `code/gamecommon/src/main/java/com/donohoedigital/games/config/SaveDetails.java`

**Key Testing Areas**:
1. **Constructors**:
   - Empty: `SaveDetails()`
   - With initial value: `SaveDetails(int initialValue)` - sets all save flags to value

2. **Constants Validation**:
   - SAVE_ALL (1), SAVE_DIRTY (2), SAVE_NONE (3)
   - TERRITORY_OWNER_UNITS (1), TERRITORY_ALL_UNITS (2)

3. **Save Strategy Flags** (10 properties):
   - `getSaveGameHashData()`, `setSaveGameHashData()`
   - `getSaveGameSubclassData()`, `setSaveGameSubclassData()`
   - `getSavePlayers()`, `setSavePlayers()`
   - `getSaveAI()`, `setSaveAI()`
   - `getSaveObservers()`, `setSaveObservers()`
   - `getSaveCurrentPhase()`, `setSaveCurrentPhase()`
   - `getSaveTerritories()`, `setSaveTerritories()`
   - `getTerritoriesDirtyType()`, `setTerritoriesDirtyType()`
   - `getTerritoriesUnitOwnerID()`, `setTerritoriesUnitOwnerID()`
   - `getSaveCustom()`, `setSaveCustom()`

4. **Custom Info**:
   - `getCustomInfo()`, `setCustomInfo()` - arbitrary DataMarshal object storage

5. **Serialization**:
   - `marshal()` / `demarshal()` - round-trip test
   - Verify all properties preserved after marshal/demarshal cycle

**Testing Challenges**:
- **DataMarshal framework**: marshal() returns TokenizedList, demarshal() reads from it
  - Test basic round-trip without deep framework knowledge
- **Custom info**: Use simple DataMarshal implementation or null for basic tests

**Test Count Estimate**: 8-10 tests

---

## Testing Patterns and Utilities

### Common Test Setup
Based on `GameConfigUtilsTest` and Phase 1 tests (`CardTest`, `HandTest`), follow these patterns:

1. **Test naming**: `should_<Expected>_When_<Condition>()`
2. **Assertions**: Use AssertJ (`assertThat()`) for fluent assertions
3. **Test organization**: Group tests with section comments (`// ========== Constructor Tests ==========`)
4. **No setup overkill**: Only use `@BeforeEach` when truly needed (e.g., static state reset)

### Reusable Test Utilities

**For file-based tests** (GameState, EngineMessage):
```java
@TempDir
Path tempDir;

private File createTempFile(String name) {
    return tempDir.resolve(name).toFile();
}
```

**For XML-based tests** (GamePhase):
```java
private Element buildElement(String xml) throws Exception {
    SAXBuilder builder = new SAXBuilder();
    Document doc = builder.build(new StringReader("<root>" + xml + "</root>"));
    return doc.getRootElement().getChildren().get(0);
}
```

**For mock GamePiece** (GamePieceContainerImpl, Territory):
```java
private static class TestGamePiece implements GamePiece {
    private GamePieceContainer container;
    private final int type;
    private final GamePlayer owner;

    TestGamePiece(int type, GamePlayer owner) {
        this.type = type;
        this.owner = owner;
    }

    @Override public int getType() { return type; }
    @Override public GamePlayer getGamePlayer() { return owner; }
    @Override public GamePieceContainer getContainer() { return container; }
    @Override public void setContainer(GamePieceContainer c) { container = c; }
    // ... other required methods with minimal implementations
}
```

---

## Implementation Strategy

### Execution Order (Phased Approach)

**Phase 3a: Simple Data Classes** (Quick Coverage Wins)
1. SaveDetailsTest (8-10 tests) - simple getters/setters + serialization
2. GamePieceContainerImplTest (10-12 tests) - delegation pattern, requires mock GamePiece

**Phase 3b: Core Entity Classes** (High Value)
3. GamePlayerTest (12-15 tests) - player representation, PropertyChangeListener complexity
4. TerritoryTest (20-25 tests) - largest class, GamePieceContainer delegation, adjacency logic

**Phase 3c: State Management** (Critical Path)
5. GameStateTest (15-20 tests) - state persistence, file I/O, entry management

**Phase 3d: Configuration & Messaging** (Integration Foundation)
6. EngineMessageTest (12-15 tests) - message protocol, DDMessage parent
7. GamePhaseTest (8-10 tests) - XML configuration, requires fixtures

**Estimated Total**: 85-107 new tests across 7 test files

### Coverage Calculation

Current state: 46 source files, 1 test (GameConfigUtilsTest)

Target classes total lines: 793 + 1,466 + 505 + 210 + 341 + 218 + 208 = **3,741 lines**

Estimated test coverage on target classes: 60-70% (based on Phase 1/2 patterns)

Module-wide coverage impact: (3,741 * 0.65) / (total module LOC) ≈ **15-20%**

This should comfortably exceed the 15% target threshold.

---

## Critical Files Reference

### Source Files
- `code/gamecommon/src/main/java/com/donohoedigital/games/config/GameState.java`
- `code/gamecommon/src/main/java/com/donohoedigital/games/config/GamePlayer.java`
- `code/gamecommon/src/main/java/com/donohoedigital/games/config/Territory.java`
- `code/gamecommon/src/main/java/com/donohoedigital/games/config/GamePhase.java`
- `code/gamecommon/src/main/java/com/donohoedigital/games/config/GamePieceContainerImpl.java`
- `code/gamecommon/src/main/java/com/donohoedigital/games/config/SaveDetails.java`
- `code/gamecommon/src/main/java/com/donohoedigital/games/comms/EngineMessage.java`

### Test Files to Create
- `code/gamecommon/src/test/java/com/donohoedigital/games/config/GameStateTest.java`
- `code/gamecommon/src/test/java/com/donohoedigital/games/config/GamePlayerTest.java`
- `code/gamecommon/src/test/java/com/donohoedigital/games/config/TerritoryTest.java`
- `code/gamecommon/src/test/java/com/donohoedigital/games/config/GamePhaseTest.java`
- `code/gamecommon/src/test/java/com/donohoedigital/games/config/GamePieceContainerImplTest.java`
- `code/gamecommon/src/test/java/com/donohoedigital/games/config/SaveDetailsTest.java`
- `code/gamecommon/src/test/java/com/donohoedigital/games/comms/EngineMessageTest.java`

### Existing Test for Reference
- `code/gamecommon/src/test/java/com/donohoedigital/games/config/GameConfigUtilsTest.java`

### POM Configuration
- `code/gamecommon/pom.xml` - update JaCoCo coverage threshold from 0% to 15% after test implementation

---

## Verification Plan

After implementing all test files:

1. **Run tests locally**:
   ```bash
   cd code
   mvn test -pl gamecommon -P dev
   ```
   - Verify all ~90+ new tests pass
   - Check for no test failures or flakiness

2. **Check coverage**:
   ```bash
   mvn verify -pl gamecommon -P coverage
   ```
   - Verify coverage report shows >=15% for gamecommon module
   - Inspect `code/gamecommon/target/site/jacoco/index.html`

3. **Update POM threshold**:
   - Edit `code/gamecommon/pom.xml`
   - Change JaCoCo `<limit>` from `<minimum>0.00</minimum>` to `<minimum>0.15</minimum>`

4. **Run full build**:
   ```bash
   mvn clean verify -P coverage
   ```
   - Verify all modules build successfully
   - Verify aggregate coverage includes gamecommon contribution

5. **CI validation**:
   - Commit and push to trigger CI
   - Verify GitHub Actions build passes with new tests

6. **Update plan status**:
   - Mark Phase 3 as COMPLETE in `UNIT-TESTING-PLAN.md`
   - Document final test count and coverage percentage
   - Archive this plan to `.claude/plans/completed/`

---

## Success Criteria

- 7 new test files created
- 85-107 new tests passing (no flakiness)
- gamecommon module coverage >=15% (verified by JaCoCo)
- JaCoCo threshold updated in pom.xml
- CI build passes
- No new warnings or errors introduced
- All tests follow established naming and organization patterns
- Plan marked complete in UNIT-TESTING-PLAN.md
