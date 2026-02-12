# .claude Directory Structure

## Root Files

| File | Purpose |
|------|---------|
| `CLAUDE.md` | AI agent instructions and behavioral guidelines |
| `DDPOKER-OVERVIEW.md` | Full project architecture, tech stack, module structure |
| `SECURITY.md` | Privacy and security checklist for commits |
| `README.md` | This file (directory guide) |
| `settings.local.json` | Local configuration settings |

## `/guides` — Reference Documentation

Read on demand. Referenced from CLAUDE.md when relevant.

| File | When to Read |
|------|-------------|
| `testing-guide.md` | Writing tests — practices, frameworks, coverage |
| `plan-protocol.md` | Creating or managing implementation plans |
| `worktree-workflow.md` | Creating worktrees, merging, cleanup |
| `review-protocol.md` | Requesting or performing code reviews |

## `/plans` — Implementation Plans

- **Active plans** — Current work in progress
- **`/plans/completed`** — Archived completed plans

## `/reviews` — Code Review Handoffs

- **`TEMPLATE.md`** — Starting template for review requests
- **Active reviews** — Named `BRANCH-NAME.md`
- **`/reviews/completed`** — Archived completed reviews
