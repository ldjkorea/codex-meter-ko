# Android Remote model selector bug report draft

Status: draft for Tibo. Not submitted. Checked on 2026-10-08 (Asia/Seoul).

## Message to Tibo

Hi Tibo — I’m seeing a model selector issue in Android Remote.

GPT-6.1 Sol appears in the model list, but after I select it and set reasoning to High, the composer still shows **GPT-6 Luna · Low**. It also happens after the task has finished, so it does not seem limited to changing settings during an active response. I can change the model and reasoning level on the connected Windows desktop.

There is a useful discrepancy: the local turn metadata for my latest development prompt shows **`gpt-6.1-sol` with `effort: high`**, while the Android label I reported stays at Luna · Low. That makes a stale display or synchronization issue plausible, but we have not captured an Android selection followed by a before/after pair of new turns. We cannot yet tell whether every Android selection is persisted correctly.

**Reproduction steps**

1. Open an existing local Codex thread in Remote on Android, connected to a Windows host.
2. Wait until the current response finishes.
3. Open the model selector. GPT-6.1 Sol is listed.
4. Select GPT-6.1 Sol and High reasoning.
5. Return to the composer. The label still shows GPT-6 Luna · Low.
6. Submit a small new prompt and compare its host-side model/effort metadata with the Android label. The latest prompt we inspected has Sol/High metadata; an instrumented before/after selection test is still needed.

Expected: the selected model and reasoning level persist and are reflected in the composer and the next prompt’s execution configuration. If changing them is unsupported for this Remote thread, the UI should state that clearly.

Actual: the Android label does not reflect the requested selection. Desktop selection works according to my reproduction report. The inspected latest prompt has Sol/High execution metadata.

**Environment**

- Client: Android, ChatGPT app, Remote.
- Android device model, Android OS version and ChatGPT app version: not yet confirmed.
- Connected host: local Windows machine.
- Installed desktop package: `OpenAI.Codex`, version `26.1002.7124.0`.
- Session runtime: Codex Desktop, CLI `0.162.0-alpha.2`.
- Thread: `01a11a6c-e368-7c80-8a7f-7f4e97f5eef7`.
- Inspected prompt turn: `01a11a6c-e829-7f82-8004-4fd6bec58add`.
- Execution configuration: `model: gpt-6.1-sol`, `effort: high`.
- Model list includes GPT-6.1 Sol; failure also reported while idle.

Could you confirm whether Android Remote supports changing model and reasoning for an existing local thread, and check whether the composer state and the next turn’s configuration can get out of sync?

## Evidence and limits

The Android symptoms and successful desktop selection above are the user’s report. The desktop package version and the current thread’s local `turn_context` values were read directly on the host. No credentials, full conversation transcript or source directory path are included in the draft. The agent did not change any model setting, edit Codex settings files, restart the app or submit feedback.

The latest prompt’s metadata confirms the Sol/High configuration delivered to this turn. It does not prove the Android picker caused that configuration, prove Android selection works generally, or expose the underlying inference routing. Automated approval-review sessions use their own `codex-auto-review` model and were excluded from the finding.

Official references confirm that [GPT-6.1 Sol supports High](https://developers.openai.com/api/docs/models/gpt-6.1-sol) and describe [Remote control of host-side Codex work](https://developers.openai.com/blog/mastering-codex-remote-for-engineering). The pages inspected do not establish the exact Android existing-thread model-selection contract. Support status for that specific UI action remains unconfirmed.

For a definitive follow-up, capture the idle composer’s before/after selection labels and inspect the metadata of a new prompt sent after each selection. Use a short harmless prompt; response prose claiming its own model is not verification.

## New detailed development prompt verification

The next user prompt requesting the complete Usage Ledger/Insights implementation was also inspected on the host. Turn `01a11a82-8e38-7901-b7c7-cb3084223682` has `model: gpt-6.1-sol` and `effort: high` in its `turn_context` at `2026-10-08T07:55:26.238Z` (and the later context checkpoint at `08:16:30.106Z`). This confirms the delivered configuration for the new prompt. The agent still did not operate the Android selector or change this thread's settings, so the cause/persistence of the Android selector action remains unverified.
