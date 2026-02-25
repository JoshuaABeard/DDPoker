# Control API Observability Additions

Date: 2026-02-24
Status: completed

## Goal

Add missing control-server API observability needed to unblock strict scenario promotion for:

- multi-table visibility (`API-MT-01`)
- durable navigation confirmation (`API-NAV-01`)
- UI observability hooks for dashboard/prefs assertions (`API-UI-01`)
- rebuy/add-on economics assertions and durable rebuy outcome confirmation

## Implementation Steps

1. Add a shared navigation status tracker and expose it in `/state` plus `/navigate/status`. ✅
2. Extend `PokerDirector` with control-observability snapshot support; implement in `WebSocketTournamentDirector` with multi-table summaries derived from remote tables + latest server state snapshot. ✅
3. Extend `/state` payload with:
   - navigation status ✅
   - remote tournament/multi-table summary ✅
   - rebuy/add-on economics/profile limits ✅
4. Add `/ui/state` endpoint with frame/dialog/phase observability for UI-oriented tests. ✅
5. Add durable rebuy outcome signaling in director snapshot (`OFFERED`, `ACCEPT_SENT`, `DECLINE_SENT`, `APPLIED`) and surface it as `tournament.rebuyOutcome` in `/state`. ✅
6. Run targeted verification with scenario scripts and quick direct API checks. ✅

## Delivered

- New endpoints:
  - `GET /navigate/status`
  - `GET /ui/state`
- Navigation observability:
  - request-id based durable navigation state in `/navigate/status`
  - mirrored in `/state.navigation`
- Tournament observability:
  - director snapshot in `/state.director` and `/state.tournament.remote`
  - remote table/player/chip summary fields for multi-table awareness
- Rebuy observability:
  - `tournament.rebuyOutcome` with state machine + sequence counters + timestamps
  - explicit accept/decline/applied lifecycle signals independent of immediate chip/economics visibility
- Scenario integration:
  - `test-navigate.sh` now waits on `/navigate/status`
  - `test-rebuy-dialog.sh` now asserts `tournament.rebuyOutcome` transitions
  - `test-multi-table.sh` now consumes remote snapshot fields
- Docs:
  - `.claude/guides/desktop-client-testing.md` updated with new endpoints and `tournament.rebuyOutcome` schema

## Validation

- `mvn -pl poker -P dev -Dtest=GameControlServerTest,WebSocketTournamentDirectorTest test -f code/pom.xml`
- `test-navigate.sh --skip-build`
- `test-multi-table.sh --skip-build`
- `test-rebuy-dialog.sh --skip-build`
- direct `curl`/`api` checks for `/navigate/status`, `/ui/state`, and `/state.tournament.rebuyOutcome`
