# Comprehensive UI Review Request

**Scope:** All frontend UI assets in the Next.js web application
**Requested:** 2026-02-12 Evening
**Purpose:** Full code review of all UI components, layouts, and pages

## Summary

Review all frontend UI assets for code quality, consistency, accessibility, and best practices. This includes all components, layouts, pages, and styling.

## Files to Review

### Layout Components
- [ ] code/web/components/layout/Navigation.tsx - Top navigation bar
- [ ] code/web/components/layout/Sidebar.tsx - Reusable sidebar component
- [ ] code/web/components/layout/Footer.tsx - Footer component

### Data Components
- [ ] code/web/components/data/DataTable.tsx - Generic table component
- [ ] code/web/components/data/Pagination.tsx - Pagination component

### Filter Components
- [ ] code/web/components/filters/FilterForm.tsx - Reusable filter form

### Online Components
- [ ] code/web/components/online/PlayerLink.tsx - Player name link
- [ ] code/web/components/online/PlayerList.tsx - Comma-separated player links
- [ ] code/web/components/online/HighlightText.tsx - Search term highlighting

### Auth Components
- [ ] code/web/components/auth/LoginForm.tsx - Login form
- [ ] code/web/components/auth/CurrentProfile.tsx - User profile display

### Profile Components
- [ ] code/web/components/profile/PasswordChangeForm.tsx - Password change form
- [ ] code/web/components/profile/AliasManagement.tsx - Alias management

### Page Layouts
- [ ] code/web/app/layout.tsx - Root layout
- [ ] code/web/app/about/layout.tsx - About section layout
- [ ] code/web/app/support/layout.tsx - Support section layout
- [ ] code/web/app/online/layout.tsx - Online portal layout
- [ ] code/web/app/admin/layout.tsx - Admin section layout

### Pages
- [ ] code/web/app/page.tsx - Home page
- [ ] code/web/app/download/page.tsx - Download page
- [ ] code/web/app/login/page.tsx - Login page
- [ ] code/web/app/forgot/page.tsx - Forgot password page
- [ ] code/web/app/online/available/page.tsx - Available games
- [ ] code/web/app/online/current/page.tsx - Current games
- [ ] code/web/app/online/completed/page.tsx - Completed games
- [ ] code/web/app/online/history/page.tsx - Tournament history
- [ ] code/web/app/online/hosts/page.tsx - Host list
- [ ] code/web/app/online/leaderboard/page.tsx - Leaderboard
- [ ] code/web/app/online/search/page.tsx - Player search
- [ ] code/web/app/online/myprofile/page.tsx - My profile
- [ ] code/web/app/admin/ban-list/page.tsx - Ban list
- [ ] code/web/app/admin/layout.tsx - Admin layout
- [ ] code/web/app/admin/online-profile-search/page.tsx - Profile search

### Styling & Data
- [ ] code/web/app/globals.css - Global styles and overrides
- [ ] code/web/lib/sidebarData.ts - Sidebar navigation data
- [ ] code/web/lib/navData.ts - Top navigation data

**Total Files:** ~40 UI-related files

## Review Criteria

### 1. Code Quality
- Clean, readable code
- Proper TypeScript typing
- No unused imports or variables
- Consistent naming conventions
- Proper component organization

### 2. React Best Practices
- Proper use of hooks
- No unnecessary re-renders
- Appropriate use of client/server components
- Proper error boundaries

### 3. Accessibility
- Semantic HTML
- ARIA labels where needed
- Keyboard navigation support
- Screen reader compatibility
- Color contrast ratios

### 4. Performance
- Lazy loading where appropriate
- Optimized images
- Minimal bundle size
- No unnecessary client-side JavaScript

### 5. Styling Consistency
- Consistent color scheme (Wood & Leather theme)
- Proper spacing and typography
- Responsive design
- No conflicting CSS

### 6. Security
- No XSS vulnerabilities
- Proper input sanitization
- Secure authentication flows
- No sensitive data exposure

### 7. User Experience
- Clear navigation
- Intuitive interactions
- Helpful error messages
- Loading states
- Empty states

### 8. Documentation
- Component prop documentation
- Complex logic explained
- File headers present

## Known Issues/Limitations

- CSS uses !important overrides due to styled-jsx not applying correctly
- Some pages have TODO placeholders for API integration
- Tests not written for frontend components
- Email functionality requires SMTP configuration

## Expected Outcome

A comprehensive review identifying:
- Critical issues (blocking)
- High priority issues (should fix)
- Medium priority issues (nice to have)
- Low priority issues (optional improvements)
- Strengths and good practices

---

## Review Results

**Reviewer:** Claude Opus 4.6
**Date:** 2026-02-12
**Files Reviewed:** 40 (all listed above, plus referenced sub-components LeaderboardFilter.tsx, AdminSearchForm.tsx, and auth infrastructure)

---

### CRITICAL Issues

#### C1. Open Redirect Vulnerability in LoginForm returnUrl
**File:** `code/web/components/auth/LoginForm.tsx` (line 29)
**File:** `code/web/lib/auth/useRequireAuth.ts` (line 31)

The `returnUrl` parameter from the query string is passed directly to `router.push()` without validation. An attacker could craft a URL like `/login?returnUrl=https://evil.com` to redirect users after login to a malicious site.

```tsx
// LoginForm.tsx line 29
const returnUrl = searchParams.get('returnUrl') || '/online'
router.push(returnUrl)  // No validation - could be an external URL
```

**Fix:** Validate that `returnUrl` starts with `/` and does not contain `//` (to prevent protocol-relative URLs like `//evil.com`):
```tsx
const raw = searchParams.get('returnUrl') || '/online'
const returnUrl = raw.startsWith('/') && !raw.startsWith('//') ? raw : '/online'
```

#### C2. Client-Side Admin Authorization Only
**File:** `code/web/app/admin/layout.tsx` (lines 19-35)

Admin access is guarded only on the client side. The admin pages under `/admin/online-profile-search/page.tsx` are server components that call `adminApi.searchProfiles()` directly at build/request time with no server-side auth check. If a user navigates directly to the admin API endpoints or if the page renders server-side, admin data could leak. The `AdminLayout` guard is purely client-side and can be bypassed.

**Fix:** The backend API must enforce admin authorization on all `/api/admin/*` endpoints via JWT/session validation. If it already does, this is mitigated at the API layer. Confirm backend enforcement exists. Additionally, admin server-component pages should verify auth server-side (e.g., via cookies/headers) rather than relying solely on client-side redirect.

#### C3. Forgot Password Sends Plaintext Password
**File:** `code/web/app/forgot/page.tsx` (lines 53-56, 92)
**File:** `code/web/lib/api.ts` (lines 126-132)

The forgot password flow says "we'll send your password to the email address" (line 55) and the button says "Send Password" (line 92). This implies the system stores and sends plaintext passwords rather than using a password reset token/link flow. This is a severe security anti-pattern.

**Fix:** If the backend truly sends the raw password, this is a critical backend issue (passwords should be hashed). At minimum, the UI should be redesigned to use a "Send Password Reset Link" flow. If the backend already sends a reset link, the UI copy is misleading and should be corrected.

---

### HIGH Priority Issues

#### H1. `isAdmin` Stored in Client-Side localStorage
**File:** `code/web/lib/auth/storage.ts` (lines 46-62)

The `isAdmin` boolean is stored in localStorage/sessionStorage. A user could modify this value via DevTools to make the UI think they are an admin, revealing admin navigation items and admin UI. While this should not grant actual API access (if backend enforces auth), it exposes admin UI patterns and endpoints.

**Fix:** Do not store `isAdmin` client-side or treat it as untrusted. Re-verify admin status via API call when accessing admin routes, or include it in the JWT claims verified server-side.

#### H2. Unused `retireAlias` State Variable
**File:** `code/web/components/profile/AliasManagement.tsx` (line 22)

```tsx
const [retireAlias, setRetireAlias] = useState<string | null>(null)
```

`retireAlias` is set to `null` on line 47 but never read. `setRetireAlias` is never called with a non-null value. This is dead code.

**Fix:** Remove the `retireAlias` state variable and its setter.

#### H3. DataTable Dynamic Tailwind Classes Will Not Work
**File:** `code/web/components/data/DataTable.tsx` (lines 45, 67)

```tsx
className={`px-4 py-2 border border-white text-${column.align || 'left'}`}
```

Tailwind CSS purges unused classes at build time. Dynamically constructed class names like `text-left`, `text-center`, `text-right` built via string interpolation will NOT be included in the production CSS bundle because Tailwind's content scanner cannot detect them.

**Fix:** Use a mapping object:
```tsx
const alignClass = { left: 'text-left', center: 'text-center', right: 'text-right' }
className={`px-4 py-2 border border-white ${alignClass[column.align || 'left']}`}
```

#### H4. Keyboard Navigation Inaccessible for Desktop Dropdown Menus
**File:** `code/web/components/layout/Navigation.tsx` (lines 108-113)

Desktop nav items with dropdowns prevent navigation via `e.preventDefault()` on click, and there is no keyboard support. Users cannot tab through dropdown items, use arrow keys, or press Escape to close. The dropdown only opens on click and only closes on outside click.

**Fix:** Add `onKeyDown` handler supporting Enter/Space to toggle, Escape to close, Arrow keys to navigate items. Add `role="menu"`, `role="menuitem"`, and `aria-haspopup="true"` attributes.

#### H5. Mobile Sidebar Has No Toggle Button
**File:** `code/web/components/layout/Sidebar.tsx` (lines 148-167)

The sidebar has mobile CSS with `transform: translateX(-100%)` and a `mobile-open` class, and there is a `.mobile-sidebar-toggle` style defined (line 150-152), but there is no actual toggle button rendered in the component markup. On mobile, the sidebar slides off-screen and there is no way for users to open it.

**Fix:** Add a visible toggle button for mobile viewports that controls the `mobileOpen` state. Also add an overlay/backdrop when the sidebar is open on mobile, and handle Escape key to close.

#### H6. `force-static` on Client-Side Page (ban-list)
**File:** `code/web/app/admin/ban-list/page.tsx` (line 2)

```tsx
export const dynamic = 'force-static'
```

This page is marked `'use client'` (line 1) AND `force-static` (line 2). The `force-static` directive is for server components. On a client component, this creates a confusing mixed signal. Since this page fetches data via `useEffect` (client-side), the `force-static` likely has no effect but is misleading.

**Fix:** Remove `export const dynamic = 'force-static'` from this client component.

#### H7. No CSRF Protection Visible
**File:** `code/web/lib/api.ts` (lines 28-73)

All API calls use `credentials: 'include'` (cookie-based auth) but no CSRF token is included in requests. If the backend relies solely on cookies for auth, the application may be vulnerable to CSRF attacks on state-changing operations (login, logout, ban management, password change).

**Fix:** Confirm the backend implements CSRF protection (e.g., SameSite cookie attribute, CSRF tokens, or checking Origin/Referer headers). If using `SameSite=Strict` or `SameSite=Lax` cookies, this may be adequately mitigated.

---

### MEDIUM Priority Issues

#### M1. Inconsistent Use of `<a>` vs `<Link>` for Internal Navigation
**Files:**
- `code/web/app/login/page.tsx` (lines 25, 30, 35): Uses `<a href="/forgot">` instead of Next.js `<Link>`
- `code/web/components/auth/LoginForm.tsx` (line 98): Uses `<a href="/forgot">` instead of `<Link>`

Using native `<a>` tags for internal routes causes full page reloads instead of client-side navigation, degrading performance and user experience.

**Fix:** Replace `<a href="/forgot">` with `<Link href="/forgot">` (and similarly for other internal links on login page).

#### M2. Duplicated Layout Pattern Across About/Support/Online Layouts
**Files:**
- `code/web/app/about/layout.tsx`
- `code/web/app/support/layout.tsx`
- `code/web/app/online/layout.tsx`

These three files are nearly identical (same CSS, same structure), differing only in sidebar data, class name prefixes, and title. This is ~150 lines of duplicated code.

**Fix:** Extract a shared `SidebarLayout` component that accepts `sections`, `title`, and `variant` props, and use it in all three layouts.

#### M3. Sidebar `isActive` Contains Hardcoded Path List
**File:** `code/web/components/layout/Sidebar.tsx` (line 32)

```tsx
if (normalizedLink === '/online' || normalizedLink === '/admin' || normalizedLink === '/' || normalizedLink === '/about' || normalizedLink === '/support') {
  return normalizedPathname === normalizedLink
}
```

This hardcodes route roots. If new sections are added, this list must be manually updated. A forgotten entry would cause incorrect active states.

**Fix:** Consider deriving this from the sidebar data itself, or add an `exactMatch` property to `SidebarItem` to let the data drive the behavior.

#### M4. Sidebar Variant Logic Is Dead Code
**File:** `code/web/components/layout/Sidebar.tsx` (lines 39-43)

```tsx
const bgGradient = variant === 'admin'
  ? 'linear-gradient(180deg, #57534e 0%, #292524 100%)'
  : 'linear-gradient(180deg, #57534e 0%, #292524 100%)'
const accentColor = variant === 'admin' ? '#d97706' : '#d97706'
```

Both branches of both ternaries return the same value. The `variant` prop has no visual effect.

**Fix:** Either differentiate the admin variant visually or remove the `variant` prop entirely.

#### M5. `PlayerList` Uses Player Name as Key (Potential Duplicates)
**File:** `code/web/components/online/PlayerList.tsx` (line 22)

```tsx
{players.map((player, index) => (
  <span key={player}>
```

If the `players` array contains duplicate names (which is unlikely but possible in edge cases), React will warn about duplicate keys.

**Fix:** Use the index as part of the key: `key={`${player}-${index}`}`

#### M6. Password Change Form Has No API Integration
**File:** `code/web/components/profile/PasswordChangeForm.tsx` (lines 34-38)

The `try` block has a TODO comment and no actual API call. It always reports success regardless. There is a `playerApi.changePassword()` method available in `api.ts` but it is not wired up.

**Fix:** Uncomment and connect to `playerApi.changePassword(currentPassword, newPassword)`.

#### M7. `AliasManagement` Uses `confirm()` and `alert()` Dialogs
**Files:**
- `code/web/components/profile/AliasManagement.tsx` (line 27)
- `code/web/app/admin/ban-list/page.tsx` (lines 79, 96, 103)

Native `confirm()` and `alert()` dialogs are non-customizable, block the main thread, and are inconsistent with the application's design language.

**Fix:** Replace with a custom modal/dialog component that matches the Wood & Leather theme. Not urgent, but would improve UX consistency.

#### M8. `force-static` May Cause Stale Data on Game Pages
**Files:**
- `code/web/app/online/available/page.tsx` (line 6)
- `code/web/app/online/current/page.tsx` (line 6)
- `code/web/app/online/completed/page.tsx` (line 6)
- `code/web/app/online/history/page.tsx` (line 6)
- `code/web/app/online/hosts/page.tsx` (line 6)
- `code/web/app/online/leaderboard/page.tsx` (line 6)
- `code/web/app/online/search/page.tsx` (line 6)
- `code/web/app/admin/online-profile-search/page.tsx` (line 6)

`export const dynamic = 'force-static'` causes these pages to be rendered at build time only. Game data (available, current, completed games) is inherently dynamic. Users will see stale data until the next rebuild.

**Fix:** For pages showing live game data, use `force-dynamic` or remove the directive. If ISR is desired, use `revalidate` instead. The current setting is likely a build workaround (the API may not be available at build time), but it should be documented as a known trade-off.

#### M9. Missing `role="alert"` on Error/Success Messages
**Files:**
- `code/web/components/auth/LoginForm.tsx` (line 39)
- `code/web/components/profile/PasswordChangeForm.tsx` (line 57)
- `code/web/components/profile/AliasManagement.tsx` (line 58)
- `code/web/app/forgot/page.tsx` (line 76)
- `code/web/app/admin/ban-list/page.tsx` (line 239)

Error and success messages appear dynamically but lack `role="alert"` or `aria-live="polite"`, so screen readers may not announce them.

**Fix:** Add `role="alert"` to error messages and `role="status"` to success messages.

#### M10. Pagination Component Missing Suspense Boundary
**File:** `code/web/components/data/Pagination.tsx` (line 20)

The component calls `useSearchParams()` which requires a Suspense boundary in Next.js App Router. Without it, the entire page may fail to render during static generation or show a hydration mismatch.

**Fix:** Wrap usages of `Pagination` in `<Suspense>` boundaries at the page level, or wrap the component itself.

#### M11. `SearchPage` Uses `any` Type for API Response Mapping
**File:** `code/web/app/online/search/page.tsx` (line 40)

```tsx
const mapped = data.map((p: any) => ({
```

Also in `code/web/app/admin/online-profile-search/page.tsx` (line 45), `code/web/lib/api.ts` (lines 266, 276, 304, 332, 338, 380, 387, 396-398, 409).

Extensive use of `any` in API response types undermines TypeScript's type safety benefits.

**Fix:** Define proper TypeScript interfaces for all API responses and use them consistently.

---

### LOW Priority Issues

#### L1. Google Fonts Loaded via `<link>` in `<head>` Instead of `next/font`
**File:** `code/web/app/layout.tsx` (lines 37-42)

Loading Google Fonts via `<link>` tags causes a render-blocking request and potential CLS (Cumulative Layout Shift). Next.js provides `next/font/google` for automatic self-hosting and optimization.

**Fix:** Use `next/font/google` to load the Delius font:
```tsx
import { Delius } from 'next/font/google'
const delius = Delius({ weight: '400', subsets: ['latin'] })
```

#### L2. Footer Not Consistently Placed
**Files:** Home page (`code/web/app/page.tsx` line 86), Download page (`code/web/app/download/page.tsx` line 189), Login page (`code/web/app/login/page.tsx` line 51), Forgot page (`code/web/app/forgot/page.tsx` line 121)

Some pages render `<Footer />` explicitly within their content. The About, Support, and Online layouts include Footer in the layout. The root layout does NOT include Footer. This creates inconsistency -- standalone pages must remember to include Footer.

**Fix:** Consider adding Footer to the root layout so it appears on all pages automatically, and remove per-page Footer imports.

#### L3. Home Page Has `<button>` Inside `<Link>`
**File:** `code/web/app/page.tsx` (lines 77-80)

```tsx
<Link href="/download">
  <button className="...">Download DD Poker Community Edition</button>
</Link>
```

Nesting interactive elements (`<button>` inside `<a>`) is invalid HTML and can cause unpredictable behavior with assistive technologies.

**Fix:** Style the `<Link>` directly as a button using Tailwind classes, or use `<Link>` with `className` matching the button style.

#### L4. Navigation `title` Prop on Sidebar Is Unused
**File:** `code/web/components/layout/Sidebar.tsx` (line 23)

The `title` prop is accepted but never rendered in the component markup. All layout files pass `title` (e.g., `title="About"`, `title="Online Portal"`), but it appears nowhere.

**Fix:** Either render the title in the sidebar (e.g., as a heading) or remove the prop.

#### L5. Hardcoded Version String on Download Page
**File:** `code/web/app/download/page.tsx` (line 16)

```tsx
const version = '3.3.0'
```

The version is hardcoded. When a new version ships, this file must be manually updated.

**Fix:** Consider reading the version from an environment variable or a shared config file.

#### L6. Download Page Uses Emoji in Headings
**File:** `code/web/app/download/page.tsx` (lines 29, 61-62, 97, 150)

Emojis in headings (windows icon, apple, penguin, package) may not render consistently across all platforms and can confuse screen readers.

**Fix:** Consider using SVG icons or font icons instead. Alternatively, add `aria-hidden="true"` to emoji-containing spans and provide separate screen reader text.

#### L7. Missing `key` Stability in DataTable Rows
**File:** `code/web/components/data/DataTable.tsx` (line 63)

```tsx
<tr key={index} className={...}>
```

Using array index as key is fragile if the data list is reordered or items are added/removed. For paginated data that does not reorder within a page, this is acceptable but not ideal.

**Fix:** If data items have unique identifiers (e.g., `id`), prefer those as keys. This could be supported by adding a `keyField` prop to `DataTable`.

#### L8. Missing `<main>` Landmark Nesting Issue
**File:** `code/web/app/layout.tsx` (line 47)

The root layout wraps children in `<main id="content">`. But the About/Support/Online layouts also wrap their content area in `<main>`, creating nested `<main>` landmarks, which is invalid HTML and confusing for screen readers.

**Fix:** Remove `<main>` from section layouts (use `<div>` instead) or remove `<main>` from root layout and let section layouts define their own.

#### L9. CSS `!important` Overrides in globals.css
**File:** `code/web/app/globals.css` (lines 155-209)

Heavy use of `!important` (20+ instances) to override styled-jsx component styles. This is documented as a known issue (styled-jsx specificity conflict).

**Assessment:** This is acceptable as a pragmatic workaround given the constraint. However, it creates a maintenance burden. Long-term, consider migrating styled-jsx styles in Navigation.tsx and Sidebar.tsx to CSS modules or Tailwind utility classes, which would eliminate the specificity conflict and all `!important` overrides.

#### L10. Inconsistent Color Palette Between Components
Throughout the codebase, colors are referenced in three different ways:
1. CSS custom properties: `var(--color-poker-green)` (Footer, Home page, Forgot page)
2. Tailwind utilities: `text-green-600`, `bg-gray-700` (most components)
3. Hardcoded hex values: `#d97706`, `#451a03` (Navigation, Sidebar, globals.css)

The "Wood & Leather" theme (brown gradients, amber accents) is applied consistently in Navigation and Sidebar but NOT in most page content, which uses standard gray/green/white Tailwind colors. The DataTable, FilterForm, Pagination, and all form components use a generic gray/green theme that does not match the Wood & Leather sidebar/nav.

**Fix:** This is a cosmetic consistency issue. Consider defining theme colors as Tailwind theme extensions and using them consistently across all components.

---

### Strengths and Good Practices

**S1. Well-Structured Component Architecture**
The codebase demonstrates a clean separation of concerns: layout components, data display components, filter components, and page-specific components. The `DataTable`/`Pagination` pattern is reusable and consistently applied across all data pages.

**S2. Proper Server/Client Component Split**
Server components are used for data-fetching pages (available, current, completed, history, hosts, leaderboard, search, profile-search). Client components are used only where needed (auth, forms, interactive UI). This is correct Next.js App Router usage and minimizes client-side JavaScript.

**S3. Good Error Handling Patterns**
All data-fetching functions include try/catch blocks with console.error logging and graceful fallbacks (empty arrays, zero counts). Error states are rendered with clear messaging. Form components show inline error/success messages.

**S4. Proper Form Accessibility**
All form inputs have associated `<label>` elements with matching `htmlFor`/`id` attributes (LoginForm, PasswordChangeForm, FilterForm, AdminSearchForm, ban-list form). Forms use proper `<form>` elements with `onSubmit` handlers, and submit buttons have `type="submit"`.

**S5. HighlightText Regex Injection Prevention**
`code/web/components/online/HighlightText.tsx` (line 18) properly escapes special regex characters in the search term before using it in a `RegExp` constructor, preventing regex injection.

**S6. Auth Context Pattern**
The `AuthProvider`/`useAuth`/`useRequireAuth` pattern is well-structured:
- Context properly throws if used outside provider
- Login returns success boolean for conditional redirect
- Logout clears state even if API call fails
- Session validation occurs on mount via `checkAuthStatus`
- `useCallback` used properly to prevent unnecessary re-renders

**S7. Consistent File Headers**
Every file has a consistent copyright/description header following the project convention.

**S8. Good Loading and Empty States**
Admin layout shows loading spinner during auth check. MyProfile page shows loading state and unauthenticated fallback with embedded LoginForm. DataTable shows configurable empty messages. Pagination hides when there's only one page.

**S9. Proper URL Encoding**
`PlayerLink` properly uses `encodeURIComponent(playerName)` for URL construction (line 17), preventing URL injection. Same pattern used in MyProfile page (line 100) and API `removeBan` (line 421).

**S10. Cookie-Based Auth with HttpOnly JWT**
The API client uses `credentials: 'include'` (line 43 of api.ts), indicating cookies are sent automatically. Combined with the comment about "JWT in HttpOnly cookie," this suggests tokens are not accessible to JavaScript, which is a good security practice for preventing XSS token theft.

---

### Summary Statistics

| Severity | Count | Status |
|----------|-------|--------|
| CRITICAL | 3     | Must fix before deployment |
| HIGH     | 7     | Should fix soon |
| MEDIUM   | 11    | Should fix when convenient |
| LOW      | 10    | Optional improvements |
| Strengths | 10   | Good practices to maintain |

### Recommended Priority Order

1. **Immediate:** C1 (open redirect), C3 (plaintext passwords - verify backend)
2. **Before launch:** C2 (server-side admin auth), H3 (broken Tailwind alignment), H4 (keyboard nav), H7 (CSRF)
3. **Short-term:** H1 (client-side isAdmin), H5 (mobile sidebar), H6 (force-static on client), M8 (stale data), M1 (Link vs a)
4. **Medium-term:** M2 (layout dedup), M6 (password API), M7 (native dialogs), M9 (ARIA alerts), M10 (Suspense), M11 (any types)
5. **Long-term:** L1-L10 (progressive improvements)

### Files Reviewed Checklist

- [x] code/web/components/layout/Navigation.tsx
- [x] code/web/components/layout/Sidebar.tsx
- [x] code/web/components/layout/Footer.tsx
- [x] code/web/components/data/DataTable.tsx
- [x] code/web/components/data/Pagination.tsx
- [x] code/web/components/filters/FilterForm.tsx
- [x] code/web/components/online/PlayerLink.tsx
- [x] code/web/components/online/PlayerList.tsx
- [x] code/web/components/online/HighlightText.tsx
- [x] code/web/components/auth/LoginForm.tsx
- [x] code/web/components/auth/CurrentProfile.tsx
- [x] code/web/components/profile/PasswordChangeForm.tsx
- [x] code/web/components/profile/AliasManagement.tsx
- [x] code/web/app/layout.tsx
- [x] code/web/app/about/layout.tsx
- [x] code/web/app/support/layout.tsx
- [x] code/web/app/online/layout.tsx
- [x] code/web/app/admin/layout.tsx
- [x] code/web/app/page.tsx
- [x] code/web/app/download/page.tsx
- [x] code/web/app/login/page.tsx
- [x] code/web/app/forgot/page.tsx
- [x] code/web/app/online/available/page.tsx
- [x] code/web/app/online/current/page.tsx
- [x] code/web/app/online/completed/page.tsx
- [x] code/web/app/online/history/page.tsx
- [x] code/web/app/online/hosts/page.tsx
- [x] code/web/app/online/leaderboard/page.tsx
- [x] code/web/app/online/search/page.tsx
- [x] code/web/app/online/myprofile/page.tsx
- [x] code/web/app/admin/ban-list/page.tsx
- [x] code/web/app/admin/online-profile-search/page.tsx
- [x] code/web/app/globals.css
- [x] code/web/lib/sidebarData.ts
- [x] code/web/lib/navData.ts
- [x] code/web/app/online/leaderboard/LeaderboardFilter.tsx (bonus)
- [x] code/web/app/admin/online-profile-search/AdminSearchForm.tsx (bonus)
- [x] code/web/lib/auth/AuthContext.tsx (bonus - security review)
- [x] code/web/lib/auth/useAuth.ts (bonus - security review)
- [x] code/web/lib/auth/useRequireAuth.ts (bonus - security review)
- [x] code/web/lib/auth/storage.ts (bonus - security review)
- [x] code/web/lib/api.ts (bonus - security review)
