# Copyright and Licensing Guide

**How to properly attribute original work and community contributions in DD Poker**

## Overview

DD Poker is a GPL-3.0 licensed project originally created by Doug Donohoe (2003-2026). This community fork adds substantial new features and modifications (February 2026 onwards) while respecting the original author's copyright and maintaining license compatibility.

## Legal Framework

### Source Code License: GPL-3.0

**Key Requirements:**
- All source code must be licensed under GPL-3.0
- Modified files must carry prominent modification notices (GPL-3.0 Section 5a)
- You hold copyright on your original contributions
- Your contributions must be GPL-3.0 licensed for compatibility
- Original copyright notices must remain intact

### Creative Assets License: CC BY-NC-ND 4.0

**What's covered:**
- "DD Poker" and "Donohoe Digital" names and logos
- Images, graphics, documentation
- Website content and marketing materials

**Key points:**
- Always include the CC license notice in file headers
- These assets remain Doug Donohoe's copyright
- Cannot create derivatives or use commercially without permission

## Copyright Ownership Principles

### You Own Copyright When:
1. You write completely new files from scratch
2. You substantially modify existing code (>30% new implementation)
3. You refactor/rewrite algorithms with new logic
4. You create new architecture or infrastructure

### Original Copyright Remains When:
1. You make minor bug fixes (<10 lines changed)
2. You move code between files without modification
3. You update dependencies or configuration only
4. You add comments or documentation to existing code

### Dual Copyright (Both) When:
1. You significantly refactor existing code with new implementation
2. You extract code from UI to server with substantial changes
3. You enhance existing algorithms with new logic
4. You're unsure — err on the side of dual copyright

## Standard Copyright Headers

### Template 1: Original Code (Unmodified or Minor Changes)

Use this for files you haven't substantially modified:

```java
/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Source Code
 * Copyright (c) 2003-2026 Doug Donohoe
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * For the full License text, please see the LICENSE.txt file
 * in the root directory of this project.
 *
 * The "DD Poker" and "Donohoe Digital" names and logos, as well as any images,
 * graphics, text, and documentation found in this repository (including but not
 * limited to written documentation, website content, and marketing materials)
 * are licensed under the Creative Commons Attribution-NonCommercial-NoDerivatives
 * 4.0 International License (CC BY-NC-ND 4.0). You may not use these assets
 * without explicit written permission for any uses not covered by this License.
 * For the full License text, please see the LICENSE-CREATIVE-COMMONS.txt file
 * in the root directory of this project.
 *
 * For inquiries regarding commercial licensing of this source code or
 * the use of names, logos, images, text, or other assets, please contact
 * doug [at] donohoe [dot] info.
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */
```

### Template 2: Substantially Modified Code (Dual Copyright)

Use this for files you've significantly refactored or enhanced:

```java
/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Source Code
 * Copyright (c) 2003-2026 Doug Donohoe
 * Copyright (c) 2026 Joshua Beard and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * For the full License text, please see the LICENSE.txt file
 * in the root directory of this project.
 *
 * The "DD Poker" and "Donohoe Digital" names and logos, as well as any images,
 * graphics, text, and documentation found in this repository (including but not
 * limited to written documentation, website content, and marketing materials)
 * are licensed under the Creative Commons Attribution-NonCommercial-NoDerivatives
 * 4.0 International License (CC BY-NC-ND 4.0). You may not use these assets
 * without explicit written permission for any uses not covered by this License.
 * For the full License text, please see the LICENSE-CREATIVE-COMMONS.txt file
 * in the root directory of this project.
 *
 * For inquiries regarding commercial licensing of this source code or
 * the use of names, logos, images, text, or other assets, please contact
 * doug [at] donohoe [dot] info.
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */
```

### Template 3: Completely New Files (Community Creation)

Use this for entirely new files you created:

```java
/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Source Code
 * Copyright (c) 2026 Joshua Beard and contributors
 *
 * This file is part of DD Poker, originally created by Doug Donohoe.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * For the full License text, please see the LICENSE.txt file
 * in the root directory of this project.
 *
 * The "DD Poker" and "Donohoe Digital" names and logos, as well as any images,
 * graphics, text, and documentation found in this repository (including but not
 * limited to written documentation, website content, and marketing materials)
 * are licensed under the Creative Commons Attribution-NonCommercial-NoDerivatives
 * 4.0 International License (CC BY-NC-ND 4.0). You may not use these assets
 * without explicit written permission for any uses not covered by this License.
 * For the full License text, please see the LICENSE-CREATIVE-COMMONS.txt file
 * in the root directory of this project.
 *
 * For inquiries regarding commercial licensing of this source code or
 * the use of names, logos, images, text, or other assets, please contact
 * doug [at] donohoe [dot] info.
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */
```

## Community Fork Contributions

### Major Contributions (February 2026 onwards)

These are substantial original works that warrant community copyright:

**Infrastructure & Architecture:**
- `code/api/` - Complete REST API module (Spring Boot controllers, services, DTOs)
- `code/pokergamecore/` - Server-side game engine extracted and refactored from client
- Server-hosted game logic (converted from P2P client-side)
- Networking API and protocol improvements

**Build & Deployment:**
- Docker deployment configuration and orchestration
- Maven build system improvements and optimization
- jpackage-based installer infrastructure
- GitHub Actions CI/CD pipelines

**Features:**
- File-based JSON configuration system (replaced Java Preferences)
- Bcrypt password hashing (replaced plaintext)
- Admin panel with environment-based authentication
- TCP conversion (replaced UDP for chat/discovery)
- Public IP detection service

**Testing:**
- Comprehensive test coverage additions (65% project-wide)
- Test infrastructure and utilities
- Playwright E2E testing framework

**Documentation:**
- `.claude/` documentation structure
- Development guides and ADRs
- Deployment and configuration documentation

## File-by-File Decision Guide

### Completely New (Community Copyright)

**When the file didn't exist in Doug's original release:**

Examples:
- `code/api/src/main/java/com/donohoedigital/poker/api/**/*` - New REST API
- `code/common/src/main/java/com/donohoedigital/config/FileBasedConfigStore.java` - New config system
- `code/common/src/main/java/com/donohoedigital/config/JsonConfigPersistence.java` - New config persistence
- `code/pokerserver/src/main/java/com/donohoedigital/games/poker/server/AdminAuthService.java` - New admin features
- `docker/**/*` - Docker deployment files
- `.github/workflows/**/*` - CI/CD configurations
- `.claude/**/*` - Documentation and guides

**Header:** Template 3 (Community copyright with DD Poker acknowledgment)

### Substantially Modified (Dual Copyright)

**When you've significantly refactored or rewritten Doug's code:**

Indicators:
- Extracted from client UI to server module with architectural changes
- Rewrote algorithm implementation while keeping concept
- Added significant new functionality to existing class (>30% new code)
- Changed data structures or control flow substantially

Examples:
- Files in `pokergamecore/` extracted from `poker/` with server-side refactoring
- AI evaluation code moved to server with new context handling
- Configuration classes enhanced with new backup/recovery logic

**Header:** Template 2 (Dual copyright)

### Original (Doug's Copyright Only)

**When you've made minor or no changes:**

Examples:
- Core poker hand evaluation algorithms (unchanged)
- Card, Deck, Hand model classes (minor changes only)
- UI components from original codebase (bug fixes only)
- Utilities and helper classes (unchanged)
- Third-party code (Zookitec, MersenneTwister, etc.)

**Header:** Template 1 (Original copyright)

## Special Cases

### Third-Party Code

Files with explicit third-party licenses must retain their original headers:

- `com.zookitec.layout.**` - Zookitec Explicit Layout (original license)
- `MersenneTwisterFast.java` - Original license
- `RandomGUID.java` - Original license
- `Base64.java` - Original license

**Never modify these headers** - they have their own license terms.

### Test Files

Apply the same rules as production code:
- New test files for new features → Community copyright
- Tests for original code → Original copyright
- Comprehensive test suites for refactored code → Dual copyright

### Configuration and Data Files

- XML/JSON config files from original → Original copyright
- New Docker configs, CI configs → Community copyright
- Modified build files (pom.xml with significant changes) → Dual copyright

## Commit Messages

When committing copyright header updates, use this format:

```
docs: Update copyright headers for [module/area]

- Template 3 (community): New REST API controllers
- Template 2 (dual): Refactored game engine classes
- Template 1 (original): Unchanged core poker logic

Ensures proper attribution for Doug Donohoe's original work
and community contributions per GPL-3.0 requirements.

Co-Authored-By: Claude Sonnet 4.5
```

## Attribution Best Practices

### Do:
✅ Keep Doug Donohoe's copyright on all original code
✅ Add your copyright when you create substantial new work
✅ Use dual copyright when refactoring existing code significantly
✅ Include the CC BY-NC-ND 4.0 notice in all source file headers
✅ Maintain "not affiliated with or endorsed by" disclaimers
✅ Credit Doug Donohoe as the original creator in README and docs

### Don't:
❌ Remove or modify Doug's copyright notices
❌ Claim copyright on minor modifications or bug fixes
❌ Use Doug's name to imply endorsement
❌ Change license terms (must remain GPL-3.0)
❌ Remove CC BY-NC-ND notices from headers
❌ Forget to include GPL-3.0 boilerplate in new files

## README Attribution Section

Ensure your README.md includes clear attribution:

```markdown
## Copyright and Licenses

### Source Code

**Original DD Poker code:**
Copyright (c) 2003-2026 Doug Donohoe

**Community fork modifications:**
Copyright (c) 2026 Joshua Beard and contributors

All source code is licensed under **GPL-3.0**. See [LICENSE.txt](LICENSE.txt).

### Creative Assets

The "DD Poker" and "Donohoe Digital" names, logos, and creative assets:
Copyright (c) 2003-2026 Doug Donohoe

Licensed under **CC BY-NC-ND 4.0**. See [LICENSE-CREATIVE-COMMONS.txt](LICENSE-CREATIVE-COMMONS.txt).

### Disclaimer

This community fork is not affiliated with, endorsed by, or sponsored by
Donohoe Digital LLC or Doug Donohoe.
```

## When in Doubt

**If you're unsure which template to use:**

1. Check git history: `git log --follow <file>` - Was it in Doug's original release?
2. Compare implementations: Is >30% of the current code your new implementation?
3. Consider architecture: Did you restructure how it works, or just move it?
4. Err on the side of dual copyright - It's better to over-credit than under-credit
5. Ask: "Could this file exist without Doug's original work?"
   - No → Dual copyright (Template 2)
   - Yes → Community copyright (Template 3)

## Legal Disclaimer

This guide provides general guidance on copyright practices for GPL-3.0 licensed projects. It is not legal advice. For specific legal questions about copyright, licensing, or commercial use, consult a qualified attorney.

## References

- [GNU GPL v3.0 Full Text](https://www.gnu.org/licenses/gpl-3.0.html)
- [CC BY-NC-ND 4.0 Full Text](https://creativecommons.org/licenses/by-nc-nd/4.0/)
- [GPL-3.0 Section 5: Conveying Modified Source Versions](https://www.gnu.org/licenses/gpl-3.0.html#section5)
- [Original DD Poker Repository](https://github.com/dougdonohoe/ddpoker)

---

**Last Updated:** 2026-02-15
