# Code Review Handoffs

This directory contains review handoff files for automated code reviews.

## Workflow

1. **Developer agent** creates `BRANCH-NAME.md` when feature is complete
2. **Developer agent** spawns Opus review agent automatically
3. **Review agent** reads handoff, reviews code, updates file with findings
4. **Developer agent** presents results to user

## File Naming

- `feature-<description>.md` - Feature reviews
- `fix-<issue>.md` - Bug fix reviews
- `refactor-<area>.md` - Refactoring reviews

## After Merge

Review files can be deleted after successful merge, or kept for historical reference.
