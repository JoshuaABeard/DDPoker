# Code Review Protocol

Reviews are fully automated: the dev agent spawns a review agent (Opus).

## Developer Agent: Request Review

When work is complete:

1. **Create review handoff** at `.claude/reviews/BRANCH-NAME.md`
   - Use `.claude/reviews/TEMPLATE.md` as the starting point
   - Fill in: summary, plan reference, files changed, privacy check, verification results, context/decisions
   - Include the worktree absolute path

2. **Spawn review agent:**
   - Use Task tool with `subagent_type: "general-purpose"`, `model: "opus"`
   - Task prompt **must** include: "Read `.claude/CLAUDE.md` for project standards, then read the handoff file at `.claude/reviews/BRANCH-NAME.md` and perform the review using the checklist in `.claude/guides/review-protocol.md`."

3. **Present results:** Show user the review findings from the updated handoff file

## Review Agent: Perform Review

When spawned:

1. **Read** the handoff file and the plan (if referenced)
2. **Navigate to worktree** using the path from the handoff file
3. **Run verification:** Execute tests (`mvn test`), check coverage, run build
4. **Check against standards:**
   - Tests pass, coverage >= 65%, build clean (zero warnings)
   - No scope creep — changes match the plan, nothing extra
   - No over-engineering — minimum code for the problem
   - No private info — IPs, credentials, personal data (see `SECURITY.md`)
   - No security vulnerabilities — OWASP top 10 basics
   - Implementation matches plan: correct approach, all steps completed, deviations documented
5. **Update handoff file** with findings:
   - Status: `APPROVED` | `NOTES` | `CHANGES REQUIRED`
   - Findings: Specific issues with `file:line` references
   - Blockers: Required changes (if any)
6. **Return to dev agent:** Summary of review status

## Handling Review Feedback

When a review comes back `CHANGES REQUIRED`:

1. **Fix in the same worktree** — Don't create a new branch
2. **Commit fixes** as additional commits (don't amend/rebase review history)
3. **Re-run verification** — Tests, coverage, build
4. **Update the handoff file** — Note what was fixed, reset status to awaiting review
5. **Spawn a new review agent** — Same process as the original review
