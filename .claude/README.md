# .claude Directory

This directory is for agent/runtime tooling files, not project docs or tests.

## Kept here

- `CLAUDE.md` - Agent bootstrap instructions.
- `SECURITY.md` - Commit-time privacy/security checklist for agent workflows.
- `hooks/` - Claude runtime hook scripts.
- `settings.json` - Shared repository-level Claude settings.
- `settings.local.json` - Local-only machine settings (ignored by git).

## Git Hooks

- Repository git hooks live in `.githooks/`.
- `.claude/hooks/pre-commit` and `.claude/hooks/post-commit` are lightweight compatibility shims for existing local `core.hooksPath` setups.

## Moved out of `.claude`

- Guides: `docs/guides/`
- Security architecture docs: `docs/security/`
- Test plans: `docs/testing/plans/`
- Scenario integration scripts: `tests/scenarios/`
- Legacy session summaries: `docs/archive/sessions/`
- Architecture overview: `docs/architecture/DDPOKER-OVERVIEW.md`

If a new artifact is part of product/testing documentation, place it under `docs/` or `tests/` instead of `.claude`.
