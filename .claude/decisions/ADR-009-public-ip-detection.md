# ADR-009: Client-Side Public IP Detection

**Status:** Accepted
**Date:** 2026-01
**Plan:** `.claude/plans/completed/PUBLIC-IP-DETECTION-PLAN.md`

## Decision
Client queries external IP detection services directly (ipify.org → icanhazip.com → checkip.amazonaws.com) with 5-minute cache, instead of server-side `request.getRemoteAddr()`.

## Alternatives Considered
- **Server-side detection** — Rejected. Client needs its own public IP for P2P game hosting after NAT translation.
- **Single service** — Rejected. Three-service fallback for reliability.

## Consequences
- Pure client-side solution, no server changes needed.
- Validates responses reject private IPs (192.168.x.x, 10.x.x.x, 127.x.x.x).
- Falls back to server method if all external services fail.
