# Phase 2: Next.js Project Setup & Static Pages - Detailed Breakdown

**Status:** In Progress
**Started:** 2026-02-12
**Estimated Files:** ~45 TypeScript/TSX files, ~200 images

---

## Summary

Phase 2 focuses on creating the Next.js frontend structure, porting all static/content pages from Wicket, and setting up the foundation for dynamic features in Phase 3.

**Completed:**
- ✅ Next.js 14 initialized with TypeScript, Tailwind CSS v4
- ✅ Design system configured (colors, typography, spacing)
- ✅ Navigation structure analyzed

**Remaining Work:**
1. Layout components (3 files)
2. Static page content (20+ pages)
3. Image assets (~200 files)
4. API infrastructure (3 files)

---

## 1. Layout Components (3 files)

### 1.1 Root Layout (`app/layout.tsx`)
**Priority:** HIGH - Required for all pages
**Dependencies:** None
**Complexity:** Low

**Tasks:**
- Update default Next.js layout
- Add Navigation component
- Add Footer component
- Configure metadata (title, description)
- Set up font loading (Arial fallback)

**Template:**
```tsx
import Navigation from '@/components/layout/Navigation'
import Footer from '@/components/layout/Footer'
import './globals.css'

export const metadata = {
  title: 'DD Poker - Texas Hold\'em Tournament Poker',
  description: 'Free Texas Hold\'em poker game...'
}

export default function RootLayout({ children }) {
  return (
    <html lang="en">
      <body>
        <Navigation />
        <main>{children}</main>
        <Footer />
      </body>
    </html>
  )
}
```

### 1.2 Navigation Component (`components/layout/Navigation.tsx`)
**Priority:** HIGH - Required for all pages
**Dependencies:** navData structure
**Complexity:** Medium (responsive hamburger menu)

**Features:**
- Top navigation bar with logo/title
- Menu items from navData.js structure
- Dropdown menus for About, Support, Online, Admin
- Mobile responsive hamburger menu
- Conditional admin menu (only if logged in as admin - Phase 3)
- Current page highlighting

**Navigation Structure:**
```
Home | About ▼ | Download | Support ▼ | Online ▼ | [Admin ▼]
```

**Dropdowns:**
- About: Overview, Practice, Online, Analysis, Poker Clock, Screenshots, FAQ (7 items)
- Support: Overview, Self Help, Password Help (3 items)
- Online: Portal, Leaderboard, Current, Completed, History, Search, Hosts, My Profile (8 items)
- Admin: Admin, Profile Search, Reg Search, Ban List (4 items) - conditional

**Mobile Behavior:**
- Hamburger icon (☰) on mobile
- Slide-out or dropdown menu
- Touch-friendly tap targets

### 1.3 Footer Component (`components/layout/Footer.tsx`)
**Priority:** MEDIUM
**Dependencies:** None
**Complexity:** Low

**Content:**
```
Copyright © 2003-2026 Doug Donohoe. All Rights Reserved.
```

**Styling:**
- Centered text
- Small font (85%)
- Subtle gray color
- Top border or separator

---

## 2. Static Pages (20 files)

All static pages follow similar structure:
- Next.js page component (`page.tsx`)
- Markdown/HTML content from Wicket templates
- Simple layout (heading + content)

### 2.1 Home Page (`app/page.tsx`)
**Priority:** HIGH
**Content Source:** `pokerwicket/.../pages/Home.html`
**Complexity:** Low

**Content:**
- Welcome message
- Feature highlights
- Download CTA
- Screenshots/images

### 2.2 About Section (7 pages)

#### 2.2.1 About Overview (`app/about/page.tsx`)
**Content Source:** `About.html`
**Features:**
- Product overview
- Feature summary
- Navigation to sub-pages

#### 2.2.2 Practice Mode (`app/about/practice/page.tsx`)
**Content Source:** `AboutPractice.html`
**Features:**
- Practice mode description
- AI opponents info
- Single-player features

#### 2.2.3 Online Play (`app/about/online/page.tsx`)
**Content Source:** `AboutOnline.html`
**Features:**
- Online multiplayer description
- Game hosting info
- Peer-to-peer details

#### 2.2.4 Analysis Tools (`app/about/analysis/page.tsx`)
**Content Source:** `AboutAnalysis.html`
**Features:**
- Hand replay features
- Statistics tracking
- Analysis tools

#### 2.2.5 Poker Clock (`app/about/pokerclock/page.tsx`)
**Content Source:** `AboutPokerClock.html`
**Features:**
- Tournament clock description
- Blind structure editor
- Timer features

#### 2.2.6 Screenshots (`app/about/screenshots/page.tsx`)
**Content Source:** `AboutScreenshots.html`
**Features:**
- Image gallery
- Feature screenshots
- UI demonstrations

#### 2.2.7 FAQ (`app/about/faq/page.tsx`)
**Content Source:** `AboutFaq.html`
**Features:**
- Common questions
- Troubleshooting
- Links to support

### 2.3 Download Page (`app/download/page.tsx`)
**Priority:** HIGH
**Content Source:** `Download.html`
**Complexity:** Medium

**Features:**
- Download buttons/links (via API endpoint)
- System requirements
- Installation instructions
- Version information
- File size information

**Integration:**
- May need to call `/api/downloads/` to get available files
- Or hardcode download links for Phase 2

### 2.4 Support Section (3 pages)

#### 2.4.1 Support Overview (`app/support/page.tsx`)
**Content Source:** `Support.html`
**Features:**
- Support options overview
- Contact information (email)
- Links to self-help resources

#### 2.4.2 Self Help (`app/support/selfhelp/page.tsx`)
**Content Source:** `SupportSelfHelp.html`
**Features:**
- Troubleshooting guides
- Common issues
- Manual/documentation links

#### 2.4.3 Password Help (`app/support/passwords/page.tsx`)
**Content Source:** `SupportPasswords.html`
**Features:**
- Password reset instructions
- Account recovery info
- Security information

### 2.5 Terms of Use (`app/terms/page.tsx`)
**Priority:** MEDIUM
**Content Source:** `Terms.html` or license files
**Complexity:** Low

**Features:**
- License information (GPL)
- Terms of service
- Privacy policy (if applicable)

### 2.6 Online Portal Placeholders (Phase 3 - Basic Structure Only)

Create placeholder pages with "Coming Soon" or basic layout:
- `app/online/page.tsx` - Portal home
- `app/leaderboard/page.tsx` - Leaderboard placeholder
- `app/current/page.tsx` - Current games
- `app/completed/page.tsx` - Completed games
- `app/history/page.tsx` - History placeholder
- `app/search/page.tsx` - Search placeholder
- `app/hosts/page.tsx` - Hosts placeholder
- `app/myprofile/page.tsx` - Profile placeholder

**Note:** These will be fully implemented in Phase 3

---

## 3. Image Assets (~200 files)

### 3.1 Copy Task
**Priority:** MEDIUM (can be done in parallel)
**Complexity:** Low (simple file copy)

**Source:** `code/pokerwicket/src/main/webapp/images/`
**Destination:** `code/web/public/images/`

**Command:**
```bash
# From DDPoker-feature-website-modernization/code/
cp -r ../DDPoker/code/pokerwicket/src/main/webapp/images/* web/public/images/
```

**Image Types:**
- Screenshots (game UI, tables, etc.)
- Feature images
- Icons
- Logo/branding
- UI elements

**Verification:**
- Check image references in HTML templates
- Ensure all images are copied
- Update image paths in components (`/images/...` → `/images/...`)

### 3.2 Favicon & Metadata
**Source:** `pokerwicket/src/main/webapp/favicon.ico`
**Destination:** `code/web/app/favicon.ico` (replace default)

---

## 4. API Infrastructure (3 files)

### 4.1 API Client Library (`lib/api.ts`)
**Priority:** MEDIUM (needed for Phase 3, setup in Phase 2)
**Dependencies:** None
**Complexity:** Medium

**Purpose:**
- Centralized API communication
- JWT token handling (from cookies)
- Type-safe API calls
- Error handling

**Functions:**
```typescript
export const api = {
  auth: {
    login(username, password, rememberMe): Promise<AuthResponse>
    logout(): Promise<void>
    getCurrentUser(): Promise<AuthResponse>
  },
  games: {
    list(params): Promise<GameListResponse>
    get(id): Promise<OnlineGame>
    // ... more in Phase 3
  }
  // ... other endpoints
}
```

**Features:**
- Automatic JWT cookie handling
- Base URL configuration (env var)
- Response parsing
- Error handling

### 4.2 TypeScript Types (`lib/types.ts`)
**Priority:** MEDIUM
**Dependencies:** API DTOs from Spring Boot
**Complexity:** Low

**Purpose:**
- Type definitions matching Spring Boot DTOs
- Frontend-only types
- Shared interfaces

**Types to Define:**
```typescript
export interface AuthResponse {
  success: boolean
  message: string
  username?: string
  admin?: boolean
}

export interface OnlineGame {
  id: number
  url: string
  hostPlayer: string
  createDate: string
  // ... more fields
}

export interface GameListResponse {
  games: OnlineGame[]
  total: number
  page: number
  pageSize: number
}

// ... more types
```

### 4.3 API Configuration (`lib/config.ts`)
**Priority:** LOW
**Dependencies:** None
**Complexity:** Low

**Purpose:**
- Environment-based configuration
- API base URL
- Feature flags (if needed)

**Content:**
```typescript
export const config = {
  apiBaseUrl: process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080/api',
  isDev: process.env.NODE_ENV === 'development'
}
```

---

## 5. Content Extraction Strategy

### Approach Options

**Option A: Manual Port (Accurate but Slow)**
- Read each Wicket `.html` template
- Extract content manually
- Rewrite as React components
- Maintain exact content

**Option B: Semi-Automated (Faster, May Need Cleanup)**
- Use agent to read Wicket templates
- Extract content sections
- Generate React components
- Manual verification

**Option C: Skeleton First (Fastest for Phase 2)**
- Create page structure with placeholder content
- Add real content progressively
- Focus on layout correctness first

**Recommendation:** Option C for Phase 2
- Get structure working
- Verify navigation, layout
- Port actual content incrementally or in Phase 2.5

---

## 6. Implementation Order (Recommended)

### Priority 1: Core Structure (Required for verification)
1. Root Layout (`app/layout.tsx`)
2. Footer Component
3. Navigation Component (basic version)
4. Home Page (with real content)

**Checkpoint:** `npm run dev` - Can navigate, see home page

### Priority 2: Static Pages (Bulk of content)
5. About pages (7 pages)
6. Download page
7. Support pages (3 pages)
8. Terms page

**Checkpoint:** All static content accessible, navigation works

### Priority 3: Assets
9. Copy images (~200 files)
10. Update image references in pages
11. Replace favicon

**Checkpoint:** Images load correctly

### Priority 4: API Foundation (Prep for Phase 3)
12. API client library (`lib/api.ts`)
13. TypeScript types (`lib/types.ts`)
14. API config (`lib/config.ts`)

**Checkpoint:** API client ready for Phase 3

### Priority 5: Online Placeholders (Structure only)
15. Create placeholder pages for online portal
16. Basic "Coming in Phase 3" messages

**Checkpoint:** Phase 2 complete, ready for Phase 3

---

## 7. Testing Checklist

### Manual Testing
- [ ] `npm run dev` - Dev server runs
- [ ] Navigation renders on all pages
- [ ] Footer renders on all pages
- [ ] Mobile hamburger menu works
- [ ] All static pages render
- [ ] All images load
- [ ] Links work correctly
- [ ] Styling matches Wicket (approximately)
- [ ] Responsive design works (mobile, tablet, desktop)

### Visual Regression
- [ ] Compare each page with Wicket equivalent
- [ ] Check color scheme matches
- [ ] Verify typography is consistent
- [ ] Confirm spacing looks correct

---

## 8. File Count Summary

| Category | Files | Estimated Lines |
|----------|-------|-----------------|
| Layout Components | 3 | ~200 |
| Static Pages | 20 | ~1,500 |
| Placeholder Pages | 8 | ~200 |
| API Infrastructure | 3 | ~400 |
| Image Assets | ~200 | N/A |
| **Total** | **~234** | **~2,300** |

---

## 9. Risks & Considerations

### Content Accuracy
- Wicket templates may have dynamic content mixed with static
- Need to identify what's truly static vs. what needs API data
- May need to defer some content to Phase 3

### Image Copyright
- Verify all images are GPL-compatible or owner-created
- Check Creative Commons license requirements

### Responsive Design
- Original Wicket site may not be fully responsive
- May need to improve mobile experience

### Performance
- 200+ images may need optimization
- Consider lazy loading for images
- May need Next.js Image component optimizations

---

## 10. Next Steps (After Review)

1. **User Decision:**
   - Approve approach?
   - Adjust priorities?
   - Add/remove pages?

2. **Implementation:**
   - Start with Priority 1 (Core Structure)
   - Checkpoint and verify
   - Continue through priorities
   - Test at each checkpoint

3. **Documentation:**
   - Update status document as pages complete
   - Note any content deferred to Phase 3
   - Track image references for verification

---

## Appendix: Wicket Template Locations

Reference for content extraction:

```
code/pokerwicket/src/main/java/com/donohoedigital/poker/wicket/pages/
├── Home.java / Home.html
├── About.java / About.html
├── about/
│   ├── AboutPractice.java / AboutPractice.html
│   ├── AboutOnline.java / AboutOnline.html
│   ├── AboutAnalysis.java / AboutAnalysis.html
│   ├── AboutPokerClock.java / AboutPokerClock.html
│   ├── AboutScreenshots.java / AboutScreenshots.html
│   └── AboutFaq.java / AboutFaq.html
├── Download.java / Download.html
├── support/
│   ├── Support.java / Support.html
│   ├── SupportSelfHelp.java / SupportSelfHelp.html
│   └── SupportPasswords.java / SupportPasswords.html
└── Terms.java / Terms.html
```
