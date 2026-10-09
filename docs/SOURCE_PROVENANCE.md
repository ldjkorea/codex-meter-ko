# Source provenance

Upstream: [BenItBuhner/Codex-Meter](https://github.com/BenItBuhner/Codex-Meter).
Original MIT LICENSE and Copyright (c) 2026 Bennett are preserved byte for byte.

Korean baseline: `6a2efd5ab46c256c55e6b3bfb12ca338c90075b2`, 2.8.6 / 36.
First public version: phone/Wear 2.8.7 / 37, tag `v2.8.7-beta`.

This separate repository begins with an audited snapshot, without private development Git history, signing material, personal delivery reports or runtime data. Package IDs, authentication/API, storage/SQLite schema, payment/tier calculations and device integration contracts remain the baseline's. Changes are presentation fonts, 5-hour reset presentation and clock updates, version identifiers, public documentation, unsigned build handling, license assets and an official checked Gradle wrapper.

Necessary historical test responses are stored as hash-verified source fixtures. Four explicit widget presentation changes are reviewed in `android/tests/publication_review.py`; those guards still require exact equality for all other code. Missing or altered baseline fixtures fail closed. New tests use synthetic server data and deterministic clocks.

Two bundled dependency font entries were replaced with unmodified official Pretendard 1.3.9. Dependency class files and other existing entries remain identical. Added dependency license files are documented in `licenses/PRETENDARD_PROVENANCE.json` and `docs/FONT_REPLACEMENT.md`. Original font binaries and original AAR versions are excluded from public Git history.
