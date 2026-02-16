# Changelog

All notable changes to DD Poker Community Edition will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### AI Algorithm Extraction (In Progress)
- Extracting V2 AI algorithm (~7,700 lines) from desktop client to server-compatible `pokergamecore` module
- V2 uses a sophisticated rule-based engine with 46 decision factors, opponent modeling, and hand range weighting
- V1 extraction complete; V2 extraction underway (Phases 1-4 of 8 complete)

### Test Coverage
- Continued expansion of test coverage across all modules
- Added comprehensive tests for utility classes, model DTOs, and online profile handling

### Fixed
- **Chat/Keyboard Shortcut Conflict** - Fixed critical UX bug where typing in chat during your turn could accidentally trigger game actions (fold, raise, all-in). Focus now remains in chat when typing, with a blinking keyboard indicator prompting users to click the table to activate shortcuts. Prevents accidental actions like typing "Red Sox" and triggering a Raise with the 'R' key.

### Changed
- Made online player limit configurable per tournament (2-120 players, default 60)
- Raised absolute maximum from 90 to 120 players to support larger tournaments

---

## [3.3.0-CommunityEdition] - 2026-02-09

A major release transforming DD Poker from a legacy proprietary application into a modern, fully open-source poker platform. Includes new gameplay features, a modern web interface, overhauled networking, native installers, and extensive architectural improvements.

### Gameplay & Tournament Features

- **Tournament Hosting Overhaul** - Significantly expanded online tournament capabilities:
  - Late registration with configurable chip mode
  - Bounty/knockout tournament support with elimination tracking and prize pool display
  - Hands-per-level advancement mode (alternative to time-based levels)
  - Blind level quick setup with 4 templates (Slow, Standard, Turbo, Hyper)
  - Standard payout presets (Top-Heavy, Standard, Flat)
  - Per-street action timeouts (separate limits for preflop/flop/turn/river)
  - Scheduled start times with minimum player requirements
  - Profile import/export for sharing tournament structures
  - Profile validation warnings displayed in UI
  - Raised limits: max players 30 → 90, max chips 50K → 1M, max observers 10 → 30, think bank 60 → 120s
  - Starting stack depth displayed in big blinds

- **Table Size Presets** - Replaced numeric spinner with intuitive radio buttons (Full Ring 10 / 6-Max / Heads-Up)

- **Configurable Chat Font Size** - User-adjustable chat text size (8-24pt), persists across sessions

- **Demo Mode Removed** - All features now available to everyone; removed 500+ lines of restriction code

### Web Interface (New)

- **Modern Website** - Replaced legacy Apache Wicket site with Next.js 14 + Spring Boot REST API
  - 9 REST controllers with JWT authentication (HttpOnly cookies) and role-based access control
  - 20+ pages: Home, About, Support, Download, Terms of Use
  - Online portal: game browser, player profiles, leaderboard
  - Admin panel: player search, ban management, server monitoring
  - Password reset with email verification tokens
  - Mobile-responsive with accessible navigation (ARIA, keyboard support)
  - TypeScript, Tailwind CSS v4

### Networking

- **UDP to TCP Conversion** - Migrated all point-to-point communication from UDP to TCP, removing ~2,500 lines of custom UDP reliability code. Improves Docker compatibility and simplifies the network stack. LAN game discovery still uses UDP multicast.

- **Public IP Detection Fix** - P2P game hosting now correctly detects public IP via external services (ipify.org, icanhazip.com) instead of returning private/local addresses behind NAT. Cached with 5-minute TTL.

### Distribution & Deployment

- **Native Installers** - Zero-dependency installers with bundled Java 25 runtime:
  - Windows: MSI installer (~98 MB) with Start Menu integration and uninstaller
  - macOS: DMG installer
  - Linux: DEB and RPM packages
  - GitHub Actions workflow for automated cross-platform builds

- **Docker Deployment** - Production-ready Docker Compose setup with multi-process entrypoint (poker server + REST API + H2 database). Includes Unraid app templates and SWAG reverse proxy configs.

- **Configurable Admin User** - Admin credentials via environment variables (`ADMIN_USERNAME`, `ADMIN_PASSWORD`) with auto-generation on first run

### Open Source Transition

- **License System Removed** - Deleted ~1,950 lines of proprietary licensing/activation code. Player identity now uses locally-generated UUID v4. No phone-home, no telemetry, no central registration.

- **File-Based Configuration** - Replaced Java Preferences API (Windows Registry) with portable JSON config files. Platform-specific paths with automatic backup and corruption recovery. Human-readable and container-friendly.

- **First-Time User Experience** - Guided setup wizard on first launch with three paths (Offline Practice, Online New Account, Link Existing Account). Progressive disclosure with real-time validation.

- **Community Rebranding** - Updated product name, dialogs, and documentation to "DD Poker Community Edition" with dual copyright (original author + community) under GPL-3.0.

### Security

- **Bcrypt Password Hashing** - Migrated from reversible DES encryption to bcrypt. Password reset generates new credentials instead of revealing existing ones.
- **Improved Deck Shuffling** - Replaced weak custom seed generator with `SecureRandom`; thread-isolated via `ThreadLocal`
- **Input Validation & Rate Limiting** - All servlet endpoints validated; rate limits on profile creation, game creation, and chat
- **Dangerous Chat Commands Removed** - Eliminated debug commands that could be abused
- **Server Authentication** - Added auth and authorization checks to game mutation endpoints

### Architecture & Internal

- **`pokergamecore` Module** - New module extracting pure game logic with zero Swing dependencies. Enables future server-hosted games and web clients. Includes HoldemHand, 11 logic classes, event system, and tournament clock. 99% test coverage.

- **AI Algorithm Extraction** - Extracted V1 AI algorithm (~1,100 lines) to `pokergamecore` with full behavioral parity. Created `PurePokerAI` and `AIContext` interfaces, `TournamentAI` with M-ratio strategy, and `ServerAIProvider` for server-side AI. Validates feasibility of intelligent server-hosted poker.

- **TournamentDirector Refactoring** - Extracted business logic into 8 testable classes (TableManager, HandOrchestrator, TournamentClock, OnlineCoordinator, ColorUpLogic, LevelTransitionLogic, DealingRules, ShowdownCalculator). Fixed bug where `selectNewTable()` could select removed tables.

- **Build Optimizations** - Three Maven profiles (`dev`, `fast`, `coverage`). Parallel test execution, incremental compilation. Full build ~69s, fast profile ~50s.

- **Coverage Enforcement** - Module-specific JaCoCo baselines prevent regression. 1,292+ tests across all modules.

- **Spring Boot Upgrade** - Upgraded from 3.3.9 to 3.5.8

- **Code Formatting** - Adopted Spotless for consistent auto-formatting

### Bug Fixes

- Public IP detection returning private addresses behind NAT/routers
- Tournament data column too small for large tournaments
- Cheat detection incorrectly triggering after rebuy decline
- `OnlineGame.hashCode()` inconsistency
- Multiple resource leaks (ResultSet, Statement, ApplicationContext, file streams)
- HoldemSimulator test failures in parallel CI execution
- Tab key not working for focus traversal in Help dialog
- Various HTML structure and accessibility issues in web interface

---

## [3.2.0-CommunityEdition] - 2026-01-15

Initial release of DD Poker as a community-maintained fork.

### Added
- Docker deployment support with docker-compose
- GitHub CI pipeline
- JUnit 5 and AssertJ test infrastructure
- Modern SMTP support for Gmail and other email providers

### Changed
- Upgraded to Java 25 (from Java 11)
- Modernized Maven build (3.9+)
- Updated all dependencies to current versions (Spring 6, Hibernate 6, Log4j2, Wicket 10, Jetty 12)
- Migrated from MySQL to H2 embedded database (no external database setup required)
- Code reformatted to consistent style
- Removed Install4j dependency (replaced by jpackage in 3.3.0)

### Fixed
- Java 17+ compatibility issues
- Build reproducibility problems
- Timezone handling in tests
- Various IntelliJ warnings and code quality issues

---

## Historical Releases (Pre-Community Edition)

DD Poker was originally developed by Doug Donohoe of Donohoe Digital LLC from 2003 to 2017. It was open-sourced on GitHub in August 2024. The 3.1.x releases below were made by the original author to modernize the codebase after open-sourcing.

### [3.1.6] - 2026-01-08
- Upgraded to Java 25
- Updated Install4j to v12
- Made Wicket website responsive
- Ongoing dependency updates (Spring 6.2, Hibernate 6.x, Jetty 12, Log4j2)
- Updated copyright to 2026

### [3.1.5] - 2025-05-18
- Upgraded to Java 21
- Updated Install4j to v11
- Added Dependabot for automated dependency updates
- Major dependency upgrades: Spring 6.1→6.2, Hibernate 6.6, Wicket 10, Jetty 12

### [3.1.4] - 2024-11-10
- Upgraded to Java 17 with deprecation fixes
- Set up GitHub CI action for automated testing
- Upgraded to Wicket 9
- Pointed Downloads page to GitHub releases

### [3.1.3] - 2024-11-09
- Upgraded to Java 11
- Incremental Wicket upgrades (1.5 → 6 → 7 → 8)

### [3.1.2] - 2024-10-20
- Upgraded to Log4j2 (from Log4j 1.x)
- Logging initialization improvements
- PokerStats tool restored to working condition
- Added HandInfo unit tests
- macOS `.ddpokerjoin` file association

### [3.1.1] - 2024-09-08
- Windows installer via Install4j
- Major dependency upgrade path: Spring 3.2 → 4.3 → 5.3, Hibernate 4.3 → 5.6
- OFL fonts for cross-platform consistency
- Fixed window centering, online tab spacing
- Removed old marketing emails and patch installer code

### [3.1] - 2024-08-29
- Initial open source release on GitHub
- Cross-platform installer using Install4j (macOS, Linux)
- Online server configuration UI (#1)
- Fixed macOS-only build issues (#2)
- Docker support and Ubuntu instructions

### Pre-3.1 (2003-2017)
DD Poker was a commercial Texas Hold'em poker simulator sold as retail and online software. It featured AI opponents, online multiplayer tournaments, and a poker calculator. The original codebase supported Java 1.4 through Java 8.

---

## Version Number Scheme

Starting with 3.2.0, DD Poker uses semantic versioning with a `-CommunityEdition` suffix:

- **Major.Minor.Patch-CommunityEdition** (e.g., 3.3.0-CommunityEdition)
- **Major**: Breaking changes or major new features
- **Minor**: New features, backward compatible
- **Patch**: Bug fixes, backward compatible
- **-CommunityEdition**: Denotes the community-maintained fork

---

## Links

- [Source Repository](https://github.com/donohoedigital/DDPoker)
- [Issue Tracker](https://github.com/donohoedigital/DDPoker/issues)
- [License (GPL-3.0)](LICENSE.txt)
