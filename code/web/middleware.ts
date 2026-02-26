/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import { NextRequest, NextResponse } from 'next/server'

const PUBLIC_PATHS = ['/', '/login', '/register', '/forgot', '/support', '/download', '/about', '/terms', '/online']

export function middleware(request: NextRequest) {
  const { pathname } = request.nextUrl

  // Allow public paths and static assets
  if (
    PUBLIC_PATHS.some((p) => pathname === p || pathname.startsWith(p + '/')) ||
    pathname.startsWith('/_next') ||
    pathname.startsWith('/images') ||
    pathname.startsWith('/api') ||
    pathname.startsWith('/favicon')
  ) {
    return NextResponse.next()
  }

  // Check for auth cookie
  const authToken = request.cookies.get('auth_token') || request.cookies.get('access_token')
  if (!authToken) {
    const loginUrl = new URL('/login', request.url)
    loginUrl.searchParams.set('returnUrl', pathname)
    return NextResponse.redirect(loginUrl)
  }

  return NextResponse.next()
}

export const config = {
  matcher: ['/((?!_next/static|_next/image|favicon.ico).*)'],
}
