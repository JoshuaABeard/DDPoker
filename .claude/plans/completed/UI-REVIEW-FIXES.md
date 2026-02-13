# UI Review Fixes Plan

**Status:** Planning
**Created:** 2026-02-12
**Review Source:** `.claude/reviews/ui-comprehensive-review.md`

## Summary

Systematic plan to address all findings from the comprehensive UI review. Organized by priority with estimated effort and implementation approach for each fix.

## Priority 1: Critical Security Fixes (BLOCKING)

### 1.1 Fix Open Redirect Vulnerability
**Issue:** C1 - LoginForm returnUrl not validated
**Files:**
- `code/web/components/auth/LoginForm.tsx` (line 29)
- `code/web/lib/auth/useRequireAuth.ts` (line 31)

**Fix:**
```typescript
// Validate returnUrl starts with / and not //
const raw = searchParams.get('returnUrl') || '/online'
const returnUrl = raw.startsWith('/') && !raw.startsWith('//') ? raw : '/online'
router.push(returnUrl)
```

**Effort:** 10 minutes
**Risk:** Low

---

### 1.2 Add Server-Side Admin Authorization
**Issue:** C2 - Admin routes only protected client-side
**Files:**
- `code/web/app/admin/layout.tsx`
- All `/api/admin/*` backend endpoints

**Fix:**
1. Verify backend enforces admin auth on all `/api/admin/*` endpoints
2. Add server-side auth check to admin layout
3. Consider middleware for admin route protection

**Effort:** 1-2 hours
**Risk:** Medium - needs backend verification

---

### 1.3 Verify Password Reset Security
**Issue:** C3 - Flagged as sending plaintext passwords
**Status:** ALREADY FIXED - we implemented temporary password generation
**Action:** Verify implementation and close issue

**Effort:** 5 minutes (verification only)

---

## Priority 2: High Priority Fixes (Pre-Launch)

### 2.1 Fix DataTable Dynamic Tailwind Classes
**Issue:** H3 - `text-${align}` will be purged
**File:** `code/web/components/data/DataTable.tsx` (lines 45, 67)

**Fix:**
```typescript
const alignClasses = {
  left: 'text-left',
  center: 'text-center',
  right: 'text-right'
}
className={`px-4 py-2 ${alignClasses[column.align || 'left']}`}
```

**Effort:** 15 minutes
**Risk:** Low

---

### 2.2 Add Keyboard Navigation to Dropdowns
**Issue:** H4 - Desktop dropdown menus not keyboard accessible
**File:** `code/web/components/layout/Navigation.tsx`

**Fix:**
1. Add `onKeyDown` handlers (Enter/Space to toggle, Escape to close, Arrow keys to navigate)
2. Add ARIA attributes (`role="menu"`, `aria-haspopup`, `aria-expanded`)
3. Manage focus properly

**Effort:** 2-3 hours
**Risk:** Medium - complex accessibility work

---

### 2.3 Add Mobile Sidebar Toggle
**Issue:** H5 - Mobile sidebar has no way to open
**File:** `code/web/components/layout/Sidebar.tsx`

**Fix:**
1. Add hamburger button for mobile
2. Add backdrop/overlay when open
3. Handle Escape key to close
4. Add touch gestures (optional)

**Effort:** 1-2 hours
**Risk:** Low

---

### 2.4 Remove Client-Side isAdmin Storage
**Issue:** H1 - User can manipulate isAdmin in localStorage
**Files:**
- `code/web/lib/auth/storage.ts`
- All auth components

**Fix:**
1. Remove isAdmin from localStorage/sessionStorage
2. Get isAdmin from JWT claims or API call
3. Re-verify admin status when needed

**Effort:** 1-2 hours
**Risk:** Medium - affects auth flow

---

### 2.5 Remove force-static from Client Components
**Issue:** H6 - Confusing mixed signals
**Files:**
- `code/web/app/admin/ban-list/page.tsx`
- Potentially others

**Fix:**
Remove `export const dynamic = 'force-static'` from all client components

**Effort:** 10 minutes
**Risk:** Low

---

### 2.6 Verify CSRF Protection
**Issue:** H7 - No visible CSRF protection
**File:** `code/web/lib/api.ts`

**Action:**
1. Verify backend uses SameSite cookies
2. Check if CSRF tokens are implemented
3. Document protection mechanism

**Effort:** 30 minutes (investigation)
**Risk:** Low (likely already protected)

---

### 2.7 Remove Unused retireAlias State
**Issue:** H2 - Dead code
**File:** `code/web/components/profile/AliasManagement.tsx` (line 22)

**Fix:**
Remove `retireAlias` and `setRetireAlias` entirely

**Effort:** 5 minutes
**Risk:** None

---

## Priority 3: Medium Priority Fixes (Short-Term)

### 3.1 Replace <a> with <Link> for Internal Navigation
**Issue:** M1 - Causes full page reloads
**Files:**
- `code/web/app/login/page.tsx` (lines 25, 30, 35)
- `code/web/components/auth/LoginForm.tsx` (line 98)

**Fix:**
Replace all `<a href="/...">` with `<Link href="/...">`

**Effort:** 15 minutes
**Risk:** None

---

### 3.2 Extract Shared SidebarLayout Component
**Issue:** M2 - ~150 lines of duplicated code
**Files:**
- `code/web/app/about/layout.tsx`
- `code/web/app/support/layout.tsx`
- `code/web/app/online/layout.tsx`

**Fix:**
1. Create `code/web/components/layout/SidebarLayout.tsx`
2. Accept `sections`, `title`, `variant` props
3. Update all three layouts to use it

**Effort:** 1 hour
**Risk:** Low

---

### 3.3 Improve Sidebar Active Detection
**Issue:** M3 - Hardcoded path list
**File:** `code/web/components/layout/Sidebar.tsx` (line 32)

**Fix:**
Add `exactMatch?: boolean` property to `SidebarItem` interface

**Effort:** 30 minutes
**Risk:** Low

---

### 3.4 Remove Dead Variant Logic
**Issue:** M4 - Variant prop has no effect
**File:** `code/web/components/layout/Sidebar.tsx` (lines 39-43)

**Fix:**
Either differentiate admin variant or remove `variant` prop

**Effort:** 15 minutes
**Risk:** Low

---

### 3.5 Fix PlayerList Keys
**Issue:** M5 - Potential duplicate key warnings
**File:** `code/web/components/online/PlayerList.tsx` (line 22)

**Fix:**
```typescript
key={`${player}-${index}`}
```

**Effort:** 5 minutes
**Risk:** None

---

### 3.6 Wire Up Password Change API
**Issue:** M6 - No API integration
**File:** `code/web/components/profile/PasswordChangeForm.tsx`

**Fix:**
Connect to `playerApi.changePassword()`

**Effort:** 15 minutes
**Risk:** Low

---

### 3.7 Replace Native Dialogs with Custom Modals
**Issue:** M7 - Inconsistent UX
**Files:**
- `code/web/components/profile/AliasManagement.tsx`
- `code/web/app/admin/ban-list/page.tsx`

**Fix:**
1. Create reusable Modal/Dialog component
2. Replace all `confirm()` and `alert()` calls

**Effort:** 2-3 hours
**Risk:** Low

---

### 3.8 Fix force-static on Dynamic Pages
**Issue:** M8 - Causes stale game data
**Files:** All game pages (available, current, completed, history, hosts, leaderboard, search)

**Fix:**
1. Remove `force-static` or change to `force-dynamic`
2. OR use ISR with `revalidate`
3. Document trade-offs

**Effort:** 30 minutes + testing
**Risk:** Medium - affects data freshness

---

### 3.9 Add ARIA Alerts to Messages
**Issue:** M9 - Screen readers don't announce messages
**Files:** LoginForm, PasswordChangeForm, AliasManagement, forgot page, ban-list

**Fix:**
Add `role="alert"` to errors, `role="status"` to success messages

**Effort:** 20 minutes
**Risk:** None

---

### 3.10 Add Suspense Boundaries for Pagination
**Issue:** M10 - May cause hydration issues
**File:** `code/web/components/data/Pagination.tsx`

**Fix:**
Wrap `Pagination` usages in `<Suspense>` boundaries at page level

**Effort:** 30 minutes
**Risk:** Low

---

### 3.11 Add TypeScript Interfaces for API Responses
**Issue:** M11 - Extensive use of `any` type
**Files:** Search page, admin pages, `code/web/lib/api.ts`

**Fix:**
1. Define proper interfaces for all API responses
2. Replace all `any` with typed interfaces

**Effort:** 2-3 hours
**Risk:** Low

---

## Priority 4: Low Priority (Progressive Improvements)

### 4.1 Use next/font for Google Fonts
**Issue:** L1 - Render-blocking font loading
**File:** `code/web/app/layout.tsx`

**Fix:**
```typescript
import { Delius } from 'next/font/google'
const delius = Delius({ weight: '400', subsets: ['latin'] })
```

**Effort:** 20 minutes
**Risk:** Low

---

### 4.2 Consolidate Footer Placement
**Issue:** L2 - Inconsistent footer inclusion
**Files:** Home, download, login, forgot pages

**Fix:**
Move Footer to root layout OR remove from individual pages

**Effort:** 15 minutes
**Risk:** None

---

### 4.3 Fix Button Inside Link
**Issue:** L3 - Invalid HTML
**File:** `code/web/app/page.tsx` (lines 77-80)

**Fix:**
Style the `<Link>` directly with button classes

**Effort:** 5 minutes
**Risk:** None

---

### 4.4 Remove Unused title Prop from Sidebar
**Issue:** L4 - Prop never rendered
**File:** `code/web/components/layout/Sidebar.tsx`

**Fix:**
Render the title or remove the prop

**Effort:** 5 minutes
**Risk:** None

---

### 4.5 Move Version to Config
**Issue:** L5 - Hardcoded version string
**File:** `code/web/app/download/page.tsx`

**Fix:**
Read version from environment variable or config file

**Effort:** 15 minutes
**Risk:** None

---

### 4.6 Replace Emoji with SVG Icons
**Issue:** L6 - Inconsistent rendering
**File:** `code/web/app/download/page.tsx`

**Fix:**
Use SVG icons or add `aria-hidden="true"` to emojis

**Effort:** 30 minutes
**Risk:** Low

---

### 4.7 Add keyField Prop to DataTable
**Issue:** L7 - Index as key is fragile
**File:** `code/web/components/data/DataTable.tsx`

**Fix:**
Add optional `keyField` prop for stable keys

**Effort:** 20 minutes
**Risk:** Low

---

### 4.8 Fix Nested Main Landmarks
**Issue:** L8 - Invalid HTML
**Files:** Root layout, section layouts

**Fix:**
Remove `<main>` from one layer (prefer section layouts)

**Effort:** 10 minutes
**Risk:** None

---

### 4.9 Plan CSS Migration from !important
**Issue:** L9 - Maintenance burden
**File:** `code/web/app/globals.css`

**Fix:**
Long-term: Migrate styled-jsx to CSS modules or Tailwind

**Effort:** 4-6 hours
**Risk:** Medium - large refactor

---

### 4.10 Standardize Color System
**Issue:** L10 - Inconsistent color usage
**Files:** Throughout codebase

**Fix:**
1. Define all theme colors in Tailwind config
2. Replace hardcoded colors with theme references
3. Apply Wood & Leather theme consistently

**Effort:** 2-3 hours
**Risk:** Low

---

## Implementation Phases

### Phase 1: Critical Security (Week 1, Day 1-2)
**Estimated Time:** 3-4 hours
- Fix open redirect (C1)
- Verify admin auth (C2)
- Verify password reset (C3)

**Deliverable:** All critical security issues resolved

---

### Phase 2: High Priority Pre-Launch (Week 1, Day 3-5)
**Estimated Time:** 8-10 hours
- Fix DataTable Tailwind classes (H3)
- Add keyboard navigation (H4)
- Add mobile sidebar toggle (H5)
- Remove client-side isAdmin (H1)
- Clean up force-static (H6)
- Verify CSRF (H7)
- Remove dead code (H2)

**Deliverable:** All high-priority accessibility and functionality issues resolved

---

### Phase 3: Medium Priority Improvements (Week 2)
**Estimated Time:** 12-15 hours
- Replace <a> with <Link> (M1)
- Extract shared layout (M2)
- Improve sidebar logic (M3, M4)
- Fix keys and API wiring (M5, M6)
- Replace native dialogs (M7)
- Fix force-static on dynamic pages (M8)
- Add ARIA alerts (M9)
- Add Suspense boundaries (M10)
- Add TypeScript interfaces (M11)

**Deliverable:** Improved code quality, UX, and maintainability

---

### Phase 4: Low Priority Polish (Week 3+)
**Estimated Time:** 6-8 hours
- next/font migration (L1)
- Footer consolidation (L2)
- HTML fixes (L3, L8)
- Sidebar cleanup (L4)
- Config improvements (L5)
- Icon updates (L6)
- DataTable enhancement (L7)
- Color system standardization (L10)

**Deliverable:** Professional polish and best practices

---

### Phase 5: Long-Term Refactoring (Future)
**Estimated Time:** 4-6 hours
- CSS migration away from !important (L9)

**Deliverable:** Reduced technical debt

---

## Testing Strategy

### Security Testing
- Manual testing of redirect validation
- Verify admin endpoints reject non-admin users
- Test CSRF protection

### Accessibility Testing
- Keyboard-only navigation testing
- Screen reader testing (NVDA/JAWS)
- ARIA attribute validation
- Color contrast checking

### Functional Testing
- Mobile responsive testing
- Cross-browser testing
- Form submission flows
- Data display/pagination

### Regression Testing
- Verify existing functionality still works
- Check all pages render correctly
- Test auth flows

---

## Success Criteria

### Phase 1 (Critical)
- [ ] No security vulnerabilities in auth flows
- [ ] Admin routes properly protected
- [ ] Password reset uses temporary passwords

### Phase 2 (High Priority)
- [ ] Keyboard navigation fully functional
- [ ] Mobile sidebar accessible
- [ ] DataTable columns align correctly
- [ ] isAdmin securely managed

### Phase 3 (Medium Priority)
- [ ] No full page reloads on internal navigation
- [ ] Clean, maintainable code (no duplication)
- [ ] Consistent UX (custom dialogs)
- [ ] Proper TypeScript typing

### Phase 4 (Low Priority)
- [ ] Lighthouse score > 90
- [ ] WCAG 2.1 AA compliance
- [ ] Consistent visual design
- [ ] Professional polish

---

## Risk Mitigation

### High-Risk Changes
- Admin auth refactor (C2)
- isAdmin storage changes (H1)
- force-static removal (M8)

**Mitigation:** Test thoroughly, have rollback plan, coordinate with backend

### Medium-Risk Changes
- Keyboard navigation (H4)
- Shared layout extraction (M2)

**Mitigation:** Incremental implementation, extensive testing

### Low-Risk Changes
- Most other fixes are low-risk

**Mitigation:** Standard code review and testing

---

## Dependencies

### Backend Coordination Required
- C2 (verify admin auth endpoints)
- H7 (verify CSRF protection)
- M6 (password change API)

### No External Dependencies
- All other fixes are frontend-only

---

## Notes

- **Strengths to Maintain:** Keep the good practices identified (S1-S10)
- **Documentation:** Update as fixes are implemented
- **Code Review:** Get review for each phase before merging
- **User Testing:** Get feedback on UX improvements (keyboard nav, mobile sidebar, dialogs)

---

## Appendix: Quick Wins (< 30 minutes each)

Can be done opportunistically:
- H2: Remove unused state (5 min)
- M1: Replace <a> with <Link> (15 min)
- M5: Fix PlayerList keys (5 min)
- M6: Wire up password API (15 min)
- M9: Add ARIA alerts (20 min)
- L3: Fix button in link (5 min)
- L4: Remove unused prop (5 min)
- L8: Fix nested main (10 min)

**Total Quick Wins Time:** ~1.5 hours for 8 fixes
