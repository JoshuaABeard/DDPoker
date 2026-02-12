# ADR-005: jpackage for Cross-Platform Installers

**Status:** Accepted
**Date:** 2026-01
**Plan:** `.claude/plans/completed/WINDOWS-INSTALLER-COMPLETE.md`, `.claude/plans/completed/MAC-LINUX-INSTALLERS.md`

## Decision
Use jpackage for Windows MSI, macOS DMG, and Linux DEB/RPM installers. GitHub Actions matrix build across 3 platforms.

## Alternatives Considered
- **GraalVM Native Image** — Too complex for Swing reflection.
- **Launch4j** — Windows-only.
- **Install4j** — Commercial cost.
- **Code signing certificate** — Rejected ($75-174/year). SmartScreen bypass is one click.

## Consequences
- MSI version must be numeric-only (`3.3.0` not `3.3.0-community`).
- OS-activated Maven profiles for platform-specific builds.
- RPM requires explicit profile activation (`-Pinstaller-linux-rpm`).
- Custom `LargeFileDownloadServlet` needed — Jetty ResourceHandler has ~42MB file size limit.
