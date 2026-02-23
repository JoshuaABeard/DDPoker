# CLAUDE.md

**IMPORTANT: Do not modify CLAUDE.md without explicit user consent.**

@../AGENTS.md

---

## Claude-Specific: Plans

See `.claude/guides/plan-protocol.md` for the full plan lifecycle: creation, approval via `ExitPlanMode`, and completion tracking with `TodoWrite`.

## Claude-Specific: Code Reviews

Reviews are fully automated: the dev agent creates a review handoff and spawns a review subagent (Opus).

When work is complete, create `.claude/reviews/BRANCH-NAME.md` (use `.claude/reviews/TEMPLATE.md`) and spawn a review subagent.

See `.claude/guides/review-protocol.md` for the full review process.

## Claude-Specific: Commit Attribution

Use the specific model name in the `Co-Authored-By` line:

```
Co-Authored-By: Claude Sonnet 4.6
```

or `Claude Opus 4.6`, `Claude Haiku 4.5`, etc. — whichever model did the work.
