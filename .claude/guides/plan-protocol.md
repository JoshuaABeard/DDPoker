# Plan Protocol

## When to Create a Plan

**Always create a plan for:**
- New features (multi-file changes)
- Significant refactoring
- Complex bug fixes requiring investigation
- Anything spanning > 3 files or > 200 lines

**Skip plans for:**
- Trivial bug fixes (one-liners)
- Documentation-only changes
- Configuration tweaks
- Dependency updates only

**If unsure:** Propose a plan. User can say "just do it" if too simple.

## Plan Status

Every plan must include a status line at the top:

```
Status: draft | active | paused | completed
```

- **draft** — Plan created, not yet approved or started. **User must approve before moving to active.**
- **active** — Currently being worked on
- **paused** — Work stopped, will resume later
- **completed** — Work finished, ready to archive

## Plan Lifecycle

1. **Create** — Store as `.claude/plans/FEATURE-NAME.md` (UPPER-CASE with hyphens) with `Status: draft`
2. **Start** — Set `Status: active` when work begins
3. **Work** — Update as progress is made: check off completed steps, note decisions, record deviations
4. **Complete** — Set `Status: completed`, document a summary of changes made
5. **Archive** — After user approval, move to `.claude/plans/completed/` right before the final commit
