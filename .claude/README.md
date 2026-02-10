# .claude Directory Structure

## Root Files (7 files - Claude-relevant only)

### Core Documentation
- **BACKLOG.md** - Current work backlog and priorities
- **CLAUDE.md** - AI assistant instructions and guidelines
- **README.md** - This file (directory structure guide)
- **SECURITY.md** - Security guidelines and requirements
- **settings.local.json** - Local configuration settings

### Technical Documentation
- **DDPOKER-OVERVIEW.md** - System architecture overview
- **TESTING.md** - TDD practices and testing guidelines

## Subdirectories

### `/plans` - Active Plans
Current implementation plans and work in progress (5 files):
- **CHAT-TCP-CONVERSION.md** - TCP chat conversion plan
- **OMAHA-IMPLEMENTATION-PLAN.md** - Omaha poker variant implementation
- **POKER-GAME-TEST-PROGRESS.md** - Poker game testing (30% complete)
- **SERVER-CODE-REVIEW.md** - Server code review and improvements
- **WINDOWS-INSTALLER-PLAN.md** - Windows installer implementation

### `/plans/completed` - Completed Plans
Finished plans with their status/completion documents (22 files):
- Test implementation plans and completions (11 consolidated docs)
- Feature implementations (FTUE wizard, licensing removal, file-based config)
- Infrastructure improvements (public IP detection, integration tests)
- Historical status snapshots and reports

### `/tech` - Technical Documentation (Reserved)
Directory reserved for future technical documentation and specifications.

## Organization Guidelines

1. **Active work** goes in `/plans`
2. **Completed work** moves to `/plans/completed` with status merged into plan doc where possible
3. **Core project files and documentation** stay in root (Claude-relevant only)
4. **Operational guides** belong in repo root or `/docker/` directory, not in `.claude/`

## External Documentation

Operational and deployment guides have been moved to more appropriate locations:
- **Build Guide:** `/BUILD.md` (repo root)
- **Local Development:** `/LOCAL-DEVELOPMENT.md` (repo root)
- **Docker Deployment:** `/docker/DEPLOYMENT.md`
- **Email Configuration:** `/docker/EMAIL-CONFIGURATION.md`
