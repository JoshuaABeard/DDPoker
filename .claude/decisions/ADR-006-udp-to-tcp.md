# ADR-006: TCP for Point-to-Point, UDP for LAN Discovery Only

**Status:** In Progress
**Date:** 2026-02
**Plan:** `.claude/plans/UDP-TO-TCP-CONVERSION.md`

## Decision
Convert all point-to-point UDP communication to TCP. Keep UDP multicast only for LAN game discovery.

## Alternatives Considered
- **Keep UDP with reliability layer** — Rejected. 1,500+ lines of custom ACK/resend/fragmentation code that TCP handles natively.
- **WebSocket** — Rejected. Unnecessary complexity for desktop-to-desktop communication.

## Consequences
- Game IDs change from `u-` prefix to `n-` prefix. Saved `u-` prefix treated as `n-` on load.
- Docker ports 11886/11889 change from UDP to TCP.
- Eliminates custom reliability layer (ACKs, resends, MTU discovery, session IDs, fragmentation).
- 4-phase migration: P2P → Chat → Cleanup → Testing.
