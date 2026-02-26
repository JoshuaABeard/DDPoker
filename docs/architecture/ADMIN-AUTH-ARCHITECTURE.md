# Admin Authentication Architecture

## Current State

**Authentication Method:** Client-side (localStorage/sessionStorage)
**Admin Protection:** Client-side only

### Frontend Protection

The admin routes (`/admin/*`) are protected by:

1. **Admin Layout** (`code/web/app/admin/layout.tsx`):
   - Uses `useRequireAuth()` hook
   - Checks `user.isAdmin` client-side
   - Redirects non-admin users to `/login`

2. **Limitations:**
   - Protection happens **after** page HTML is delivered to client
   - JavaScript must execute before redirect occurs
   - No server-side middleware protection possible with localStorage auth

### Backend Protection

**Critical:** Backend Java servlets MUST enforce admin authorization independently.

The frontend client-side checks are **not** a security boundary - they only improve UX by preventing non-admin users from seeing admin UI.

**Backend endpoints must:**
- Verify session/authentication
- Check admin role/permissions
- Return 401/403 for unauthorized requests

### Security Model

```
┌─────────────────────────────────────────────┐
│ Frontend (Client-Side)                      │
│ ✓ UX improvement (prevent UI from showing)  │
│ ✗ NOT a security boundary                   │
└─────────────────────────────────────────────┘
                    │
                    ▼
┌─────────────────────────────────────────────┐
│ Backend (Server-Side)                       │
│ ✓ TRUE security boundary                    │
│ ✓ Must verify admin on ALL /api/admin/*     │
│ ✓ Must return 401/403 for unauthorized      │
└─────────────────────────────────────────────┘
```

## Recommendations

### Short Term (Current Architecture)

✅ **Frontend:**
- Client-side admin checks in place (`admin/layout.tsx`)
- Open redirect vulnerability fixed (`LoginForm.tsx`)

⚠️ **Backend:**
- **VERIFY** all Java servlet admin endpoints check for admin role
- Document which servlets/endpoints are admin-only

### Long Term (Architecture Improvement)

To enable true server-side admin protection in Next.js:

1. **Migrate to Cookie-Based Authentication:**
   - Use HTTP-only cookies for session tokens
   - Set cookies on successful login
   - Middleware can access cookies server-side

2. **Add Next.js Middleware:**
   ```typescript
   // middleware.ts
   export function middleware(request: NextRequest) {
     const token = request.cookies.get('session')

     if (request.nextUrl.pathname.startsWith('/admin')) {
       if (!token || !isAdmin(token)) {
         return NextResponse.redirect(new URL('/login', request.url))
       }
     }
   }
   ```

3. **Benefits:**
   - Admin pages never render for non-admin users
   - Protection happens before page generation
   - True server-side security boundary

## Verification Checklist

- [x] C1: Open redirect vulnerability fixed
- [ ] C2: Backend admin endpoints verified (requires Java code audit)
- [x] C3: Password reset security verified (secure token implementation)
- [ ] Document all admin-only servlet endpoints
- [ ] Consider cookie-based auth migration (future enhancement)

## Related

- **Backend Review:** `BACKEND-CODE-REVIEW.md` (archived)
- **Auth System Redesign:** `BACKEND-ARCHITECTURE-IMPROVEMENTS.md` (BE-BACKEND-3)
