# .claude Directory

This directory is for agent/runtime tooling files, not project docs or tests.

## Kept here

- `CLAUDE.md` - Agent bootstrap instructions.
- `DDPOKER-OVERVIEW.md` - Compact architecture context for agent sessions.
- `SECURITY.md` - Commit-time privacy/security checklist for agent workflows.
- `hooks/` - Git hook scripts used by this repository setup.
- `settings*.json` - Local agent settings.

## Moved out of `.claude`

- Guides: `docs/guides/`
- Security architecture docs: `docs/security/`
- Test plans: `docs/testing/plans/`
- Scenario integration scripts: `tests/scenarios/`
- Legacy session summaries: `docs/archive/sessions/`

If a new artifact is part of product/testing documentation, place it under `docs/` or `tests/` instead of `.claude`.
