# Phase 4: Admin Portal Implementation

## Context

Phase 4 of the Website Modernization Plan adds the admin section to the Next.js frontend. The Spring Boot REST API admin endpoints were implemented in Phase 1. The Next.js static pages and auth infrastructure were completed in Phase 2-3. This phase connects admin UI pages to the backend admin API.

**Current state:**
- ✅ Phase 1: Spring Boot REST API with admin endpoints (`AdminController.java`)
- ✅ Phase 2: Next.js project setup with static pages
- ✅ Phase 3: Authentication infrastructure and online portal pages
- ❌ Phase 4: Admin pages exist as placeholders (TODO functions)

**Goal:** Complete admin section with route protection, profile/registration search, ban list management, and RSS feed verification.

## Scope

**In Scope:**
- Admin route protection (redirect non-admin users)
- Admin home dashboard
- Online profile search page
- Registration search page
- Ban list management (view, add, remove bans)
- RSS feed verification

**Out of Scope:**
- Backend admin API changes (already implemented in Phase 1)
- New admin features beyond the existing Wicket pages
- Admin statistics/analytics (not in current Wicket site)

## Admin Pages Overview

From the plan, we need to implement 4 admin pages:

1. **`/admin` (page.tsx)** - Admin home dashboard
2. **`/admin/online-profile-search` (page.tsx)** - Search online profiles
3. **`/admin/reg-search` (page.tsx)** - Search registrations
4. **`/admin/ban-list` (page.tsx)** - Manage banned keys (view, add, remove)

## Implementation Approach

### Strategy: Bottom-Up Integration

**Step 1: Admin Route Protection** - Protect all `/admin/*` routes
**Step 2: Admin API Client** - Extend `lib/api.ts` with admin endpoints
**Step 3: Admin Home Dashboard** - Simple landing page with links
**Step 4: Profile Search** - Search online profiles with filters
**Step 5: Registration Search** - Search player registrations
**Step 6: Ban List Management** - CRUD for banned keys
**Step 7: RSS Feed Verification** - Test RSS endpoints work

### Key Technical Decisions

1. **Route Protection**: Use middleware or layout-level auth check in `/admin/layout.tsx`
2. **Admin Check**: JWT token contains `admin: true` claim (set by backend)
3. **Error Handling**: Graceful degradation - redirect to login if not authenticated/authorized
4. **Data Tables**: Reuse existing `DataTable` component from online portal pages
5. **Ban Dates**: Use date inputs for ban expiration (optional field)

## Critical Files

### New Files to Create

**1. `code/web/app/admin/layout.tsx`** - Admin section layout with auth protection
```tsx
'use client'
// Wraps all /admin/* pages
// Checks if user.isAdmin, redirects to /login if not
// Shows admin navigation sidebar
```

**2. `code/web/app/admin/page.tsx`** - Admin home dashboard
```tsx
// Links to all admin tools
// Simple card-based layout
```

**3. `code/web/app/admin/online-profile-search/page.tsx`** - Profile search
```tsx
// Search form: name, email filters
// Results table with profile data
// Pagination support
```

**4. `code/web/app/admin/reg-search/page.tsx`** - Registration search
```tsx
// Search form: name, email, date range
// Results table with registration details
// Pagination support
```

**5. `code/web/app/admin/ban-list/page.tsx`** - Ban management
```tsx
// List of banned keys
// Add ban form (key, reason, expiration date)
// Remove ban button per entry
// Pagination support
```

### Existing Files to Update

**6. `code/web/lib/api.ts`** - Add admin API client functions
```typescript
export const adminApi = {
  searchProfiles: async (filters) => GET /api/admin/profiles
  searchRegistrations: async (filters) => GET /api/admin/registrations
  getBans: async (page, pageSize) => GET /api/admin/bans
  addBan: async (banData) => POST /api/admin/bans
  removeBan: async (banId) => DELETE /api/admin/bans/{id}
}
```

**7. `code/web/lib/types.ts`** - Add admin-specific types
```typescript
interface OnlineProfile { ... }
interface Registration { ... }
interface BannedKey { ... }
interface BanRequest { ... }
```

**8. `code/web/lib/auth.ts`** (optional) - Add admin check helper
```typescript
export function useRequireAdmin() {
  // Hook that redirects if not admin
}
```

## Implementation Steps

### Step 1: Admin Route Protection

**Goal:** Ensure only admin users can access `/admin/*` pages

**Create `code/web/app/admin/layout.tsx`:**
```tsx
'use client'

import { useRequireAuth } from '@/lib/auth/useRequireAuth'
import { redirect } from 'next/navigation'
import { useEffect } from 'react'

export default function AdminLayout({ children }) {
  const { user, isLoading } = useRequireAuth()

  useEffect(() => {
    if (!isLoading && (!user || !user.isAdmin)) {
      redirect('/login')
    }
  }, [user, isLoading])

  if (isLoading) {
    return <div>Loading...</div>
  }

  if (!user || !user.isAdmin) {
    return null // Will redirect
  }

  return (
    <div className="admin-layout">
      <aside className="admin-nav">
        {/* Admin navigation sidebar */}
        <nav>
          <a href="/admin">Home</a>
          <a href="/admin/online-profile-search">Profile Search</a>
          <a href="/admin/reg-search">Registration Search</a>
          <a href="/admin/ban-list">Ban List</a>
        </nav>
      </aside>
      <main className="admin-content">
        {children}
      </main>
    </div>
  )
}
```

### Step 2: Extend API Client

**Update `code/web/lib/api.ts`:**
```typescript
export const adminApi = {
  searchProfiles: async (
    filters?: { name?: string; email?: string; page?: number; pageSize?: number }
  ) => {
    const params = new URLSearchParams()
    if (filters?.name) params.append('name', filters.name)
    if (filters?.email) params.append('email', filters.email)
    if (filters?.page !== undefined) params.append('page', filters.page.toString())
    if (filters?.pageSize) params.append('pageSize', filters.pageSize.toString())

    const response = await apiFetch<any>(`/api/admin/profiles?${params}`)
    return response.data
  },

  searchRegistrations: async (
    filters?: { name?: string; email?: string; from?: string; to?: string; page?: number }
  ) => {
    const params = new URLSearchParams()
    if (filters?.name) params.append('name', filters.name)
    if (filters?.email) params.append('email', filters.email)
    if (filters?.from) params.append('from', filters.from)
    if (filters?.to) params.append('to', filters.to)
    if (filters?.page !== undefined) params.append('page', filters.page.toString())

    const response = await apiFetch<any>(`/api/admin/registrations?${params}`)
    return response.data
  },

  getBans: async (page = 0, pageSize = 50) => {
    const response = await apiFetch<any>(`/api/admin/bans?page=${page}&pageSize=${pageSize}`)
    return response.data
  },

  addBan: async (banData: { key: string; reason?: string; expiresAt?: string }) => {
    const response = await apiFetch<any>('/api/admin/bans', {
      method: 'POST',
      body: JSON.stringify(banData),
    })
    return response.data
  },

  removeBan: async (banId: number) => {
    await apiFetch<void>(`/api/admin/bans/${banId}`, {
      method: 'DELETE',
    })
  },
}
```

### Step 3: Admin Home Dashboard

**Create `code/web/app/admin/page.tsx`:**
```tsx
import Link from 'next/link'

export const metadata = {
  title: 'Admin - DD Poker',
  description: 'Administration tools',
}

export default function AdminPage() {
  return (
    <div className="container mx-auto px-4 py-8">
      <h1 className="text-3xl font-bold mb-6">Administration</h1>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        <Link href="/admin/online-profile-search">
          <div className="p-6 bg-white rounded-lg shadow hover:shadow-lg">
            <h2 className="text-xl font-bold mb-2">Online Profile Search</h2>
            <p className="text-gray-600">Search and view online player profiles</p>
          </div>
        </Link>

        <Link href="/admin/reg-search">
          <div className="p-6 bg-white rounded-lg shadow hover:shadow-lg">
            <h2 className="text-xl font-bold mb-2">Registration Search</h2>
            <p className="text-gray-600">Search player registrations</p>
          </div>
        </Link>

        <Link href="/admin/ban-list">
          <div className="p-6 bg-white rounded-lg shadow hover:shadow-lg">
            <h2 className="text-xl font-bold mb-2">Ban List</h2>
            <p className="text-gray-600">Manage banned keys and players</p>
          </div>
        </Link>
      </div>
    </div>
  )
}
```

### Step 4-6: Implement Search and Ban Pages

*(Similar pattern to online portal pages: server component with async data fetch, DataTable display, pagination)*

### Step 7: RSS Feed Verification

**Goal:** Test that RSS feeds work via `/api/rss` endpoints

**Manual verification steps:**
1. Open `/api/rss/games` in browser - should return valid XML
2. Validate RSS XML structure
3. Check feed readers can parse it

## Verification Steps

After implementation:

1. **Admin Protection**
   - [ ] Non-admin user accessing `/admin` redirects to login
   - [ ] Non-logged-in user accessing `/admin` redirects to login
   - [ ] Admin user can access all admin pages

2. **Profile Search**
   - [ ] Search by name returns results
   - [ ] Search by email returns results
   - [ ] Pagination works
   - [ ] Empty search shows all profiles

3. **Registration Search**
   - [ ] Search filters work
   - [ ] Date range filtering works
   - [ ] Results display correctly

4. **Ban List**
   - [ ] View all bans with pagination
   - [ ] Add new ban (with and without expiration)
   - [ ] Remove ban (confirmation dialog)
   - [ ] Ban list updates after add/remove

5. **RSS Feeds**
   - [ ] `/api/rss/games` returns valid XML
   - [ ] Feed validates in RSS validator
   - [ ] Feed readers can parse it

## Known Limitations

1. **Backend API Assumptions**: This plan assumes Phase 1 admin endpoints are fully functional. If not, those need to be implemented first.

2. **Admin User Creation**: Need at least one admin user in database for testing. Check if backend has admin user seeding.

3. **RSS Feed Format**: Assumes backend `RssController` matches expected XML format.

## Implementation Timeline

**Week 1: Foundation (2-3 days)**
- Admin layout and route protection
- Extend API client
- Admin home dashboard

**Week 2: Search Pages (2-3 days)**
- Profile search page
- Registration search page

**Week 3: Ban Management (2-3 days)**
- Ban list view
- Add/remove ban functionality
- RSS verification

## Dependencies

- ✅ Phase 1 complete (Admin API endpoints exist)
- ✅ Phase 2 complete (Next.js setup)
- ✅ Phase 3 complete (Auth infrastructure)
- ⚠️ Admin user exists in database (required for testing)

## Success Criteria

- All 4 admin pages functional
- Route protection prevents non-admin access
- Profile and registration search work with filters
- Ban list CRUD operations work
- RSS feeds validate
- Build passes with zero errors
- Code reviewed and approved
