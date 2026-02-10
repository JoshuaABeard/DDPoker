# .claude Directory Structure

## Root Files

### Core Documentation
- **BACKLOG.md** - Current work backlog and priorities
- **CLAUDE.md** - AI assistant instructions and guidelines
- **SECURITY.md** - Security guidelines and requirements
- **settings.local.json** - Local configuration settings

### Technical Documentation
- **TESTING.md** - TDD practices and testing guidelines
- **DDPOKER-OVERVIEW.md** - System architecture overview

## Subdirectories

### `/plans` - Active Plans
Current implementation plans and work in progress (6 files):
- **OMAHA-IMPLEMENTATION-PLAN.md** - Omaha poker variant implementation
- **SERVER-CODE-REVIEW.md** - Server code review and improvements
- **WINDOWS-INSTALLER-PLAN.md** - Windows installer implementation
- **CHAT-TCP-CONVERSION.md** - TCP chat conversion plan
- **FILE-BASED-CONFIG-PLAN.md** - File-based configuration implementation
- **POKER-GAME-TEST-PROGRESS.md** - Poker game testing (30% complete)

### `/plans/completed` - Completed Plans
Finished plans with their status/completion documents (21 files):
- Test implementation plans and completions (11 consolidated docs)
- Feature implementation records (FTUE wizard, licensing removal)
- Infrastructure improvements (public IP detection, integration tests)
- Historical status snapshots and reports

### `/tech` - Technical Documentation (Reserved)
Directory reserved for future technical documentation and specifications.

## Organization Guidelines

1. **Active work** goes in `/plans`
2. **Completed work** moves to `/plans/completed` with status merged into plan doc where possible
3. **Core project files and documentation** stay in root
4. **Future technical specs** can go in `/tech` if needed
