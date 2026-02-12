# Worktree Workflow

## Creating a Worktree

```bash
# From the main worktree root
git worktree add -b feature-<description> ../DDPoker-feature-<description>
```

**Branch naming:** `feature-*`, `fix-*`, `refactor-*`
**Worktree directory naming:** `DDPoker-feature-*`, `DDPoker-fix-*`, `DDPoker-refactor-*`

## Development Workflow

1. Pull main from remote
2. Create worktree from main and work there
3. Commit and test normally
4. When complete, STOP and request code review (no PRs)
5. After approval: rebase, squash merge to main, push, clean up

## Merging Back

**Always rebase before merging** to keep main clean and avoid untested conflict resolutions:

```bash
# 1. Rebase worktree branch onto latest main
cd <worktree-path>
git fetch origin
git rebase origin/main

# 2. If conflicts occur, resolve them in the worktree
#    After resolving: git rebase --continue
#    Re-run tests to verify: mvn test -P dev (from code/)

# 3. Squash merge to main (should be clean after rebase)
cd <main-worktree>
git checkout main
git merge --squash <branch>
git commit
```

**If rebase conflicts are complex:** Stop and ask. Don't force-resolve conflicts you don't understand.

## Cleanup

```bash
git worktree remove <path>
git branch -d <branch>
```

## What Goes Where

| Change Type | Where |
|-------------|-------|
| Code or tests | Always a worktree |
| Small (< 10 lines) `.claude/` files, plans, .gitignore, README typos | Main is OK |
| Unsure | Use a worktree |
