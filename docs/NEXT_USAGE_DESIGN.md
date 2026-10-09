> Historical prototype document. The detailed SQLite implementation is documented in [NEXT_SQLITE_USAGE_DESIGN.ko.md](NEXT_SQLITE_USAGE_DESIGN.ko.md). This file retains the earlier prototype record.

# Codex Meter NEXT usage intelligence design

Phase 1 and Phase 2 extend the existing Android usage-history flow with a separate local observation ledger and conservative quota forecasts. Phase 3 is a design proposal for opt-in Desktop token analytics; no Desktop collector, token import, network sync or token-derived quota estimate is implemented.

The original Korean release baseline is commit `d2e2cf1` on `feature/korean-usage-insights`. Implementation is isolated in this workspace on `feature/next-usage-ledger`. Version `2.8.1`, versionCode `31`, package identity and existing release signing key are preserved.

## Phase 1 observation ledger

Each successful usage refresh continues to save the existing snapshot, event observations and per-meter chart history. The existing recorder then calls an optional ledger observer. The network API, OAuth, PKCE, token encryption and refresh scheduling remain unchanged.

`codex_meter_ledger_v1` holds a versioned JSON payload under `ledger`. It contains only normalized plan labels, meter kind, observation timestamp, reported used percentage, window duration and effective reset timestamp. The source is `usage_api`. It contains no credentials, account IDs, conversation content, prompts, file paths, model attribution or reset-credit IDs.

Meters are `five_hour`, `weekly` and `monthly`. Equal timestamps for different meters are valid. Equal or older timestamps for the same meter cannot replace a previous value. Late responses cannot reorder observations across meters. A refresh produces one atomic ledger payload for the meters it actually reports; a missing meter is not invented as a zero observation.

The ledger retains at most 2,048 observations and removes observations older than 90 days on a subsequent append. The count bound can shorten the retained period. Each record remains an observation, including unchanged values; it is not a reconstructed request or session. Changes are derived from adjacent observations of the same meter:

| Change | Meaning |
| --- | --- |
| Baseline | First retained observation; no earlier change is inferred |
| No change | Same reported quota percentage in a comparable interval |
| Usage increase | Positive change in reported quota percentage points |
| Allowance increase | Reported used percentage dropped; cause unknown |
| Reset observed | A later reset timeline was observed after the previous boundary |
| Window changed | Timeline/window changed outside a comparable interval |
| Plan changed | Normalized API plan label changed |
| Gap | More than six hours between comparable observations |
| Missing reset | A comparable current timeline cannot be established |

An allowance increase can reflect corrections or policy changes. The app does not attribute it to Tibo, a promotion or a particular provider action. Confirmed reset-credit actions and credit-bank observations remain in the existing event history.

There is no automatic migration or backfill. Existing chart/event history keeps its original storage and semantics. The ledger starts at the next successful refresh after installing this build. Malformed or unknown-version payloads are preserved byte for byte and block ledger writes; they do not block the original snapshot/history save. A failed ledger commit is logged with a fixed diagnostic code and does not turn a successful refresh into a failure.

The existing explicit clear-history action and account snapshot-reset flow also clear the new ledger namespace. These hooks run only when those existing flows execute; development does not clear an installed app’s data. Clearing the ledger itself does not clear snapshot, auth or event namespaces. Account separation follows the existing sign-out/reset lifecycle; switching accounts through other paths has no physical-device proof in this work.

## Phase 2 conservative insights

The existing dashboard weekly insights, widgets, Wear data contracts, AOD and Now Bar calculations are unchanged. Supplemental native cards appear below the existing events in Usage history. They show retained count, newest 12 observations, change labels and analysis for each reported meter. English fallback and Korean resources have matching format arguments.

A forecast requires a fresh response that exactly matches the latest ledger observation. Only a contiguous segment within the latest 24 hours, in the same plan and current reset window, contributes. A reset, allowance decrease in used percentage, plan change, long gap, or single jump above 30 points starts a new segment. Invalid/missing reset information and unreadable storage withhold forecasts. A sufficient new segment can restore forecasts after a boundary.

| Requirement | Five-hour | Weekly/monthly |
| --- | --- | --- |
| Minimum observations | 3 | 3 |
| Minimum span | 10 minutes | 6 hours |
| Maximum adjacent gap for forecasts | 1 hour | 6 hours |
| Minimum observed increase | 2 percentage points | 2 percentage points |
| Maximum single increase | 30 percentage points | 30 percentage points |

These are conservative product guardrails, not statistical confidence estimates. Polling only observes interval endpoints; it cannot prove activity was continuous or identify the request that caused a change. No confidence percentage is fabricated.

For a qualifying segment, rate is `(last used - first used) / observed span`. Daily pace is that rate multiplied by one day. The budget is the observed remaining quota divided by time to reset, expressed per day and capped at the remaining quota for intervals shorter than a day. Projected exhaustion and remaining quota at reset assume that rate continues. All values are estimates based on the latest observation, not replacements for the API’s reported quota values. Forecasts never convert quota points into tokens, messages, currency or model usage.

## Phase 3 Desktop token analytics proposal

This phase is design only. Implement it later as a separately authorized vertical slice, beginning with one validated runtime/schema adapter and synthetic fixtures. Quota observations in Android and Desktop inference token counters are distinct datasets.

### Collection and privacy

Use an opt-in local collector with a user-selected allowlist of Codex session-log directories. Read logs without modifying them or opening the auth store. Decode supported metadata/events with a versioned adapter. Do not persist raw JSONL lines or emit prompt text, response text, tool inputs/outputs, repository names, paths, account identifiers or credentials. Unknown schemas produce an unsupported-source status.

Retain aggregate token records only. Use a local keyed identifier for session/event identity; the key remains on the desktop. The Windows file tailer must handle a locked active log using shared read access, incomplete final lines, truncation, rotation and replacement. Checkpoint offsets only after a complete record is validated and committed. Time-only data cannot be used as a unique event ID.

### Suggested aggregate schema

| Field | Contract |
| --- | --- |
| schema_version, source_adapter | Explicit format and adapter identity |
| session_key, event_key | Locally pseudonymized stable identity for deduplication |
| observed_at_utc, occurred_at_utc | Separate collector observation and source event time; missing event time stays unknown |
| model_id, reasoning_effort | Only source-reported validated metadata; unknown stays unknown |
| input_tokens, output_tokens | Nonnegative source counters with known cumulative/incremental semantics |
| cached_input_tokens | Subset of input when the source defines it that way; not added again |
| reasoning_output_tokens | Subset of output when documented by the adapter; not added again |
| total_tokens | Validate against defined counter semantics; preserve mismatches as data-quality findings |
| coverage_state | Complete, partial, unsupported, corrected, or uncertain |
| adapter_version | Reproducible interpretation of source metadata |

Some logs report cumulative totals and incremental totals together. Select one canonical form per adapter. For cumulative counters, use an ordered prior checkpoint from the same session/counter epoch. A decrease starts a new epoch or a correction investigation; never count a negative delta, clamp it into a fabricated zero, or re-add the full cumulative value. Replayed records and copied log files must not double count. Detect late and corrected data with deterministic upserts into a separate aggregate store.

### Analysis boundaries

Report daily totals by the user’s selected timezone, and separate model/effort values that were actually logged. Derive cache-hit fraction only when its denominator and cached subset are known. Preserve unknown counters rather than substituting zero. Explain incomplete coverage, collector downtime and missing logs alongside totals.

The collector cannot infer account-wide usage from one desktop. It cannot equate a Desktop token total with Android quota consumption or claim that a particular model caused a quota change. If cost estimates are later requested, use a versioned, date-effective price source and label them as API-equivalent estimates; subscription quota is not an API invoice.

### Optional Android handoff

Start with explicit user export/import of a small versioned aggregate file. No background sync or new network endpoint is needed for the first slice. Android treats it as a separate token dataset with provenance and a distinct retention policy. Reject oversized files, unsupported versions, invalid integer counters and duplicate event keys without touching existing quota/history data. The archive contains no signing key or desktop identity key.

### Acceptance criteria before Phase 3 implementation can ship

Use synthetic fixtures for duplicated/copied logs, cumulative-vs-incremental counters, cached/reasoning subsets, partial lines, rotation/truncation, clock skew, out-of-order corrections, schema changes and unknown models. Verify secret/content exclusion, deterministic replay, aggregate-store recovery, export/import idempotence and timezone day boundaries. Then obtain explicit device-flow evidence before claiming Desktop-to-Android operation. No collector or external service is exercised by Phase 1/2 tests.

## Validation boundaries

Pure JVM fixtures exercise production ledger, store and recorder code with in-memory preferences. They prove the tested calculation and storage contracts; they do not prove Android filesystem durability or Keystore operation. Gradle Release/lint and signature verification prove local artifacts. Actual installation, upgrade over an existing APK, OAuth, live usage/credit APIs, background jobs, clipping and Samsung/Wear system surfaces require physical-device validation.
