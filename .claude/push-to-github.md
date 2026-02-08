# GitHub Push Guide

Your repository is configured with two remotes:
- **origin**: https://github.com/dougdonohoe/ddpoker.git (upstream/original)
- **github**: https://github.com/JoshuaABeard/DDPoker.git (your fork)

## Quick Commands

### Push current changes to your GitHub:
```bash
git add .
git commit -m "Your commit message"
git push github main
```

### Pull latest changes from upstream (original repo):
```bash
git fetch origin
git merge origin/main
```

## Automated Push Script

You can ask Claude to commit and push changes by saying:
- "Push these changes to GitHub"
- "Commit and push with message: [your message]"
- "Update GitHub repository"

Claude will automatically stage, commit, and push your changes to the `github` remote.
