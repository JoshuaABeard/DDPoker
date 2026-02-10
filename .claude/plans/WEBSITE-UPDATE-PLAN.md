# Website Update Plan - DD Poker Community Edition

## Context

The DD Poker website (served via Apache Wicket) was originally built for the commercial/donation-ware product. It contains dead pages (Forums, Store), outdated references (version 3.0 instead of 3.3.0-community), and content that doesn't reflect the community fork's improvements. The original website content is CC BY-NC-ND 4.0 licensed, so we'll write **all-new original content** rather than modifying existing text. We'll also write **new CSS from scratch** using modern techniques while preserving the same visual look and feel. The GPL3-licensed Wicket framework stays — it explicitly allows modification.

**Goals:**
- Brand as "DD Poker Community Edition" throughout
- Remove dead pages (Forums, Store/Donate)
- Write new content highlighting community edition improvements
- **Prominently credit Doug Donohoe** as original creator on every page (footer) and dedicated About section
- Link to ddpoker.com for the original project
- Reference GitHub repo and Docker deployment prominently
- Update version references to 3.3.0-community

---

## 1. Navigation Restructure

**File:** `code/pokerwicket/src/main/webapp/navData.js`

Remove `store` and `forums` entries. Updated nav:

```
Home | About | Download | Support | Online | Admin
```

- **Removed:** `store` (Donate) entry, `forums` entry
- **Kept:** Home, About (with sub-pages), Download, Support, Online, Admin (all unchanged structurally)

---

## 2. Page Changes (by page)

### 2a. Home Page — Complete content rewrite
**Files:**
- `code/pokerwicket/src/main/java/com/donohoedigital/games/poker/wicket/pages/home/HomeHome.html`
- `code/pokerwicket/src/main/java/com/donohoedigital/games/poker/wicket/pages/home/HomeHome.java` (check for Wicket component refs)

**New content direction:**
- Title: "DD Poker Community Edition"
- Hero description: Free, open-source Texas Hold'em simulator — community maintained fork
- Feature highlights (bullet list): AI opponents, online play, hand analysis, poker clock, tournament design
- "What's New in Community Edition" section: license removal, file-based config, FTUE wizard, Docker support, Java 25+
- Single CTA: Download button (no donate button)
- Links: GitHub repo, ddpoker.com (original)

### 2b. About Page — Complete content rewrite
**File:** `code/pokerwicket/src/main/java/com/donohoedigital/games/poker/wicket/pages/about/AboutHome.html`

**New content direction:**
- **Dedicated "Origins" section**: Full credit to Doug Donohoe as the creator. Mention the project history (released 2004, evolved through multiple versions, open-sourced August 2024). Link prominently to ddpoker.com. Acknowledge that this community edition builds on his work.
- **"Special Thanks" section**: Explicitly thank Doug Donohoe for creating DD Poker and for open-sourcing the project, enabling the community to carry it forward.
- Remove the personal photo of Doug and Greg (CC BY-NC-ND content — cannot be reused)
- **"Community Edition" section**: What the community fork brings — why it exists, what's improved
- **"Get Involved" section**: Links to community GitHub repo (source, issues, contributions) and Docker deployment for self-hosting
- Keep the 5 feature boxes (Practice, Online, Analysis, Poker Clock, In-Game Help) but rewrite the descriptions as new original content
- Update the "In Game Help" link (currently points to static.ddpoker.com) — point to GitHub docs or remove
- Sub-pages (Practice, Online, Analysis, etc.) — review and rewrite as needed (separate tasks)

### 2c. Download Page — Content rewrite
**Files:**
- `code/pokerwicket/src/main/java/com/donohoedigital/games/poker/wicket/pages/download/DownloadHome.html`
- `code/pokerwicket/src/main/java/com/donohoedigital/games/poker/wicket/pages/download/DownloadHome.java` (has Wicket `version` and `serverAddress` labels)

**Changes:**
- Title: "Download DD Poker Community Edition"
- Version label already dynamically injected via Wicket — verify it shows "3.3.0-community"
- Replace "What's New?" link (currently points to static.ddpoker.com) with link to GitHub CHANGELOG or releases page
- Keep the JAR download section and first-time setup instructions (rewrite text as original)
- Mention the FTUE wizard that guides new users
- **Add "Run Your Own Server" section**: Brief instructions/link to Docker deployment docs for self-hosting the online portal
- **Add GitHub link**: Source code, contributing, reporting issues

### 2d. Forums Page — Remove
**Files to potentially remove or redirect:**
- `code/pokerwicket/src/main/java/com/donohoedigital/games/poker/wicket/pages/forums/ForumsHome.html`
- `code/pokerwicket/src/main/java/com/donohoedigital/games/poker/wicket/pages/forums/ForumsHome.java`
- Any related Java classes (ForumsTopNavigation, etc.)

**Approach:** Keep the page files but update content to redirect to GitHub Discussions or just show a brief message pointing to GitHub. This avoids breaking any bookmarked URLs. Alternatively, remove from nav but keep as a fallback page.

### 2e. Store/Donate Pages — Remove from nav
**Files:**
- `code/pokerwicket/src/main/java/com/donohoedigital/games/poker/wicket/pages/store/StoreHome.html`
- `code/pokerwicket/src/main/java/com/donohoedigital/games/poker/wicket/pages/store/Donate.html`
- `code/pokerwicket/src/main/java/com/donohoedigital/games/poker/wicket/pages/store/ThankYou.html`
- `code/pokerwicket/src/main/java/com/donohoedigital/games/poker/wicket/pages/store/Upgrade.html`
- Related Java files

**Approach:** Remove from navigation. Update page content to say "This page is no longer active" for bookmark fallback. Remove Donate button/image from Home page.

### 2f. Support Page — Content rewrite
**File:** `code/pokerwicket/src/main/java/com/donohoedigital/games/poker/wicket/pages/support/SupportHome.html`

**Changes:**
- Rewrite intro paragraph (currently references "Donohoe Digital's goal")
- Remove callout about DD Poker 1.x/2.x upgrades (no longer relevant)
- Keep links to self-help, password help, and online supplement sub-pages
- Add link to GitHub Issues for bug reports

### 2g. BasePokerPage / Header — Update title
**File:** `code/pokerwicket/src/main/java/com/donohoedigital/games/poker/wicket/pages/BasePokerPage.html`

- No structural changes needed (nav is JS-driven)
- May need to update any hardcoded "DD Poker" references in footer component

### 2h. Page Titles — Update across all modified pages
- Change `<title>` tags to include "Community Edition" where appropriate
- e.g., "DD Poker Community Edition - Texas Hold'em" for home, "About - DD Poker Community Edition" for about, etc.

---

## 3. About Sub-Pages (Lower Priority)

These pages need review and rewriting but are lower priority:
- `AboutPractice.html` — Rewrite feature descriptions
- `AboutOnline.html` — Rewrite, mention self-hosted server capability
- `AboutAnalysis.html` — Rewrite
- `AboutPokerClock.html` — Rewrite
- `AboutCompetition.html` — Rewrite (currently `skipInDocMode`)
- `AboutScreenshots.html` — Review, may need new screenshots
- `AboutFaq.html` — Rewrite with community-relevant FAQs

---

## 4. Java Changes

### 4a. Check DownloadHome.java version label
- Verify the Wicket `version` label dynamically pulls 3.3.0-community
- **File:** `code/pokerwicket/src/main/java/com/donohoedigital/games/poker/wicket/pages/download/DownloadHome.java`

### 4b. Footer component — Full refresh
**Files:**
- `code/pokerwicket/src/main/java/com/donohoedigital/games/poker/wicket/panels/CopyrightFooter.html`
- `code/pokerwicket/src/main/java/com/donohoedigital/games/poker/wicket/panels/CopyrightFooter.java`

**Current footer:** Donohoe Digital logo image + copyright + trademark notice

**New footer (text only, no logo image):**
```
DD Poker Community Edition
Originally created by Doug Donohoe · ddpoker.com
DD Poker™ is a trademark of Donohoe Digital LLC
```
- "DD Poker Community Edition" links to the community GitHub repo
- "ddpoker.com" links to https://www.ddpoker.com
- Remove the `logosmall.gif` image (CC BY-NC-ND content)
- Remove Doug's GitHub link from footer (avoid confusion with community repo)
- Keep the dynamic year via Wicket `StringLabel` (use in copyright or omit if cleaner)
- Keep trademark notice (legally required)

### 4c. No Java class deletions
- Keep all Java page classes to avoid breaking URL routes
- Just update the HTML content

---

## 5. Implementation Order

1. **styles.css** — Write new stylesheet from scratch (foundational — everything depends on this)
2. **nav.css** — Write new nav stylesheet from scratch
3. **CopyrightFooter.html/.java** — New footer component
4. **navData.js** — Remove forums and store entries
5. **HomeHome.html** — New home page content (highest impact)
6. **AboutHome.html** — New about page with community focus
7. **DownloadHome.html** — Updated download page
8. **SupportHome.html** — Updated support page
9. **ForumsHome.html** — Update to redirect/notice
10. **StoreHome.html, Donate.html, etc.** — Update to inactive notices
11. **Page titles** — Update `<title>` tags across all modified pages
12. **About sub-pages** — Rewrite each (can be done incrementally)

---

## 6. Key Files to Modify

| File | Change |
|------|--------|
| `code/pokerwicket/src/main/webapp/navData.js` | Remove forums, store entries |
| `code/pokerwicket/.../pages/home/HomeHome.html` | Complete rewrite |
| `code/pokerwicket/.../pages/about/AboutHome.html` | Complete rewrite |
| `code/pokerwicket/.../pages/download/DownloadHome.html` | Content rewrite |
| `code/pokerwicket/.../pages/support/SupportHome.html` | Content rewrite |
| `code/pokerwicket/.../pages/forums/ForumsHome.html` | Redirect notice |
| `code/pokerwicket/.../pages/store/StoreHome.html` | Inactive notice |
| `code/pokerwicket/.../pages/store/Donate.html` | Inactive notice |
| `code/pokerwicket/src/main/webapp/styles.css` | Complete rewrite (modern CSS) |
| `code/pokerwicket/src/main/webapp/nav.css` | Complete rewrite (modern CSS) |
| `code/pokerwicket/.../panels/CopyrightFooter.html` | New footer content |
| `code/pokerwicket/.../panels/CopyrightFooter.java` | Update footer component |
| Various page `.html` files | Update `<title>` tags |

---

## 7. CSS Rewrite — New Stylesheets

Write new `styles.css` and `nav.css` from scratch using modern CSS, preserving the same visual appearance.

**Files:**
- `code/pokerwicket/src/main/webapp/styles.css` (~1370 lines → rewrite)
- `code/pokerwicket/src/main/webapp/nav.css` (~348 lines → rewrite)

**Approach — same look, modern code:**
- Replace YUI reset with modern CSS reset (`box-sizing: border-box`, minimal reset)
- Use CSS custom properties (`:root` variables) for colors, fonts, spacing
- Replace float-based layouts with Flexbox/Grid where appropriate
- Consolidate repetitive media queries
- Preserve the same color scheme: white background, green titles (#336600), blue links, dark header with pattern
- Preserve the same typography: Arial/sans-serif body, Delius cursive header
- Preserve same responsive breakpoint (768px)
- **Critical:** Keep all existing CSS class names used in HTML templates for compatibility
- Update footer styles for new text-only footer layout

**New CSS structure:**
```
styles.css:
  1. Modern CSS Reset
  2. Custom Properties (:root variables for colors, fonts, spacing)
  3. Global Typography & Links
  4. Layout (#content, .media, floats → flex)
  5. Components (forms, tables, buttons, callouts, pagination)
  6. Page-specific (home, about, download, support, online/tournament)
  7. Footer

nav.css:
  1. Header / sticky bar
  2. Logo
  3. Main navigation (desktop)
  4. Secondary navigation
  5. Mobile navigation (hamburger, submenu)
```

---

## 8. What We're NOT Changing

- Online section pages (functional, stay as-is)
- Admin section (stays as-is)
- Wicket Java framework code
- Docker/deployment configuration
- nav.js (navigation behavior logic — only navData.js content changes)
- Original CC BY-NC-ND images (removing, not modifying)

---

## 9. Edge Cases & Risks

**CSS class compatibility:** New CSS must cover all class names across ~55 HTML templates (not just pages we rewrite). Audit all templates before writing CSS.

**CC BY-NC-ND image assets referenced in CSS/HTML:**
- `images/spade.jpg` — used as `li` bullet icon globally → replace with CSS/Unicode ♠
- `images/titlebar-background-pattern.png` — header background → replace with CSS gradient or new pattern
- `images/icon-*.jpg` — About page feature icons → use text-only or Unicode
- `images/websitemenu-mirror.png` — Home page screenshot → omit or take new screenshot
- `images/logosmall.gif` — footer logo → already removing (text-only footer)
- `images/donate.jpg`, `images/download-button.jpg` — action buttons → omit donate, create new download button or use CSS

**Wicket binding:** `wicket:id` attributes in HTML must match Java component definitions. Key risk: `DownloadHome` (has `version`, `serverAddress` labels).

**`wicket:link` paths:** Internal links like `<wicket:link><a href="../about/AboutHome.html">` are resolved by Wicket — keep relative paths correct.

**`static.ddpoker.com` links:** Multiple pages reference external help docs on Doug's server. Replace with GitHub links.

**Donate form:** Ensure deactivated Donate page doesn't have an active PayPal form.

---

## 10. Verification

1. Build the project: `mvn clean package -pl code/pokerwicket -am` (or full build)
2. Run locally via Docker or embedded Jetty and verify:
   - **Visual parity:** Site looks the same as before (same colors, layout, typography)
   - **Responsive:** Check both desktop and mobile (768px breakpoint)
   - **Footer:** New footer shows on all pages with correct links
   - Home page shows new Community Edition content
   - Navigation no longer shows Forums or Donate
   - About page credits original author with Special Thanks, highlights community edition
   - Download page shows correct version (3.3.0-community)
   - Forums URL still works (shows redirect/notice)
   - Store URLs still work (show inactive notice)
   - All existing Online/Admin pages still function and look correct
   - Online section tables, forms, pagination all render properly with new CSS
3. Check no broken internal links
