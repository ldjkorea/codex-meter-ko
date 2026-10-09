package dev.bennett.codexmeter;

import android.content.Context;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import org.json.JSONObject;

/** Real production ledger/store/recorder on an in-memory preferences fixture. */
public final class UsageLedgerSelfTest {
    private static final long NOW = 1_800_000_000_000L;
    private static final long HOUR = TimeUnit.HOURS.toMillis(1);
    private static final long DAY = TimeUnit.DAYS.toMillis(1);
    private static final String WEEKLY = UsageHistory.WEEKLY;
    private static int checks;

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }

    private static UsageWindow window(int used, long reset) {
        return new UsageWindow(used, 604800L, 0L, reset / 1000L);
    }

    private static UsageSnapshot snapshot(int used, long at) {
        return new UsageSnapshot("pro", true, false, null, window(used, NOW + 4 * DAY),
                null, Collections.emptyList(), null, -1, at);
    }

    private static UsageLedger history(int... used) {
        UsageLedger ledger = UsageLedger.empty();
        for (int i = 0; i < used.length; i++) ledger = ledger.append(WEEKLY, "pro",
                window(used[i], NOW + 4 * DAY), NOW - (used.length - 1 - i) * 6 * HOUR);
        return ledger;
    }

    private static UsageLedgerInsights.Result analyze(UsageLedger ledger, int used) {
        return UsageLedgerInsights.analyze(ledger, WEEKLY, window(used, NOW + 4 * DAY), NOW, NOW, 30);
    }

    private static void rejected(JSONObject json, String message) {
        try { UsageLedger.fromJson(json); throw new AssertionError(message); }
        catch (org.json.JSONException expected) { checks++; }
    }

    public static void main(String[] args) throws Exception {
        testLedger(); testInsights(); testStore(); testRecorder();
        System.out.println("Usage ledger, insights, persistence and recorder assertions passed: " + checks);
    }

    private static void testLedger() throws Exception {
        UsageLedger ledger = history(10, 12, 14, 17, 20);
        check(ledger.observations.size() == 5, "every successful observation retained");
        check(ledger.append(WEEKLY, "pro", window(99, NOW + DAY), NOW) == ledger, "duplicate cannot replace");
        check(ledger.append(WEEKLY, "pro", window(99, NOW + DAY), NOW - 1) == ledger, "late response ignored");
        check(ledger.append("bogus", "pro", window(20, NOW + DAY), NOW + 1) == ledger, "unknown kind ignored");
        check(ledger.append(WEEKLY, "pro", null, NOW + 1) == ledger, "missing meter ignored");
        check(UsageLedger.fromJson(new JSONObject(ledger.toJson().toString())).toJson().toString()
                .equals(ledger.toJson().toString()), "round trip");
        UsageLedger.Observation before = ledger.latest(WEEKLY);
        UsageLedger next = ledger.append(WEEKLY, "pro", window(25, NOW + 4 * DAY), NOW + HOUR);
        check(UsageLedger.change(before, next.latest(WEEKLY)) == UsageLedger.Change.USAGE_INCREASE, "same-window increase");
        next = ledger.append(WEEKLY, "pro", window(15, NOW + 4 * DAY), NOW + HOUR);
        check(UsageLedger.change(before, next.latest(WEEKLY)) == UsageLedger.Change.ALLOWANCE_INCREASE, "decrease un-attributed");
        next = ledger.append(WEEKLY, "plus", window(25, NOW + 4 * DAY), NOW + HOUR);
        check(UsageLedger.change(before, next.latest(WEEKLY)) == UsageLedger.Change.PLAN_CHANGED, "plan boundary");
        next = ledger.append(WEEKLY, "pro", window(20, NOW + 4 * DAY), NOW + 7 * HOUR);
        check(UsageLedger.change(before, next.latest(WEEKLY)) == UsageLedger.Change.GAP, "long gap");
        next = ledger.append(WEEKLY, "pro", window(20, NOW + 4 * DAY), NOW + HOUR);
        check(UsageLedger.change(before, next.latest(WEEKLY)) == UsageLedger.Change.NO_CHANGE, "unchanged observation");
        next = ledger.append(WEEKLY, "pro", window(0, NOW + 11 * DAY), NOW + 4 * DAY);
        check(UsageLedger.change(before, next.latest(WEEKLY)) == UsageLedger.Change.RESET_OBSERVED, "reset boundary");
        next = ledger.append(WEEKLY, "pro", window(0, NOW + 5 * DAY), NOW + HOUR);
        check(UsageLedger.change(before, next.latest(WEEKLY)) == UsageLedger.Change.WINDOW_CHANGED, "early shifted reset");
        next = ledger.append(WEEKLY, "pro", new UsageWindow(25, 604800, 0, 0), NOW + HOUR);
        check(UsageLedger.change(before, next.latest(WEEKLY)) == UsageLedger.Change.MISSING_RESET, "missing reset");
        UsageLedger overflow = UsageLedger.empty().append(WEEKLY, "pro",
                new UsageWindow(25, 604800, 0, Long.MAX_VALUE), NOW);
        check(UsageLedger.fromJson(overflow.toJson()).latest(WEEKLY).resetAtMillis == 0L,
                "overflowing reset cannot poison ledger persistence");
        rejected(ledger.toJson().put("version", 2), "future schema rejected");
        rejected(ledger.toJson().put("version", 1.5), "fractional version rejected");
        JSONObject bad = ledger.toJson(); bad.getJSONArray("observations").getJSONObject(0).put("used_percent", 101);
        rejected(bad, "range validation");
        bad = ledger.toJson(); bad.getJSONArray("observations").getJSONObject(0).put("at", 1.2);
        rejected(bad, "fractional timestamp rejected");
        bad = ledger.toJson(); bad.getJSONArray("observations").getJSONObject(1).put("at", NOW - DAY);
        rejected(bad, "duplicate/out-of-order timestamp rejected");
        UsageLedger bounded = UsageLedger.empty();
        for (int i = 0; i < 2100; i++) bounded = bounded.append(WEEKLY, "pro", window(i % 100, NOW + 4 * DAY), NOW + i * 1000L);
        check(bounded.observations.size() == UsageLedger.MAX_OBSERVATIONS, "retention count");
        check(bounded.observations.get(0).atMillis == NOW + 52_000L, "retains newest");
        bounded = bounded.append(WEEKLY, "pro", window(1, NOW + 101 * DAY), NOW + 100 * DAY);
        check(bounded.observations.size() == 1, "retention age");
        check(ledger.observations.size() == 5, "immutable append");
        try { ledger.observations.clear(); throw new AssertionError("mutable list"); }
        catch (UnsupportedOperationException expected) { checks++; }
    }

    private static void testInsights() {
        UsageLedgerInsights.Result result = analyze(history(10, 12, 14, 17, 20), 20);
        check(result.status == UsageLedgerInsights.Status.READY, "24-hour reliable segment");
        check(result.observationCount == 5 && result.spanMillis == DAY && result.consumedPoints == 10, "sample evidence");
        check(Math.abs(result.recentDailyUsage - 10) < 0.00001, "rate points per day");
        check(Math.abs(result.dailyBudget - 20) < 0.00001, "daily allowance budget");
        check(result.exhaustionAtMillis == NOW + 8 * DAY && result.remainingAtReset == 40, "forecast");
        check(analyze(null, 20).status == UsageLedgerInsights.Status.UNREADABLE, "unreadable");
        check(analyze(UsageLedger.empty(), 20).status == UsageLedgerInsights.Status.EMPTY, "new install");
        check(analyze(history(20), 20).status == UsageLedgerInsights.Status.WARMUP, "one observation");
        check(analyze(history(20, 20, 21), 21).status == UsageLedgerInsights.Status.LOW_CHANGE, "quantization");
        check(analyze(history(0, 1, 50), 50).status == UsageLedgerInsights.Status.LARGE_JUMP, "large jump");
        check(analyze(history(10, 12, 14, 5, 6), 6).status == UsageLedgerInsights.Status.WARMUP, "correction segment");
        check(analyze(history(10, 12, 14), 15).status == UsageLedgerInsights.Status.MISMATCH, "snapshot mismatch");
        check(UsageLedgerInsights.analyze(history(10, 12, 14), WEEKLY, window(14, NOW + 4 * DAY),
                NOW, NOW + HOUR + 1, 30).status == UsageLedgerInsights.Status.STALE, "stale suppresses");
        check(UsageLedgerInsights.analyze(history(10, 12, 14), WEEKLY, new UsageWindow(14, 604800, 0, 0),
                NOW, NOW, 30).status == UsageLedgerInsights.Status.MISSING_RESET, "reset missing suppresses");
        UsageLedger plan = history(10, 12).append(WEEKLY, "plus", window(14, NOW + 4 * DAY), NOW + HOUR);
        check(UsageLedgerInsights.analyze(plan, WEEKLY, window(14, NOW + 4 * DAY), NOW + HOUR,
                NOW + HOUR, 30).status == UsageLedgerInsights.Status.WARMUP, "no cross-plan forecast");
        UsageLedger gap = history(10, 12, 14).append(WEEKLY, "pro", window(15, NOW + 4 * DAY), NOW + 7 * HOUR);
        check(UsageLedgerInsights.analyze(gap, WEEKLY, window(15, NOW + 4 * DAY), NOW + 7 * HOUR,
                NOW + 7 * HOUR, 30).status == UsageLedgerInsights.Status.GAP, "gap suppresses");
        UsageLedger five = UsageLedger.empty();
        UsageWindow w = null;
        for (int i = 0; i < 3; i++) {
            w = new UsageWindow(10 + i * 2, 18000L, 0, (NOW + HOUR) / 1000);
            five = five.append(UsageHistory.FIVE_HOUR, "pro", w, NOW - (2 - i) * 300_000L);
        }
        check(UsageLedgerInsights.analyze(five, UsageHistory.FIVE_HOUR, w, NOW, NOW, 30).status
                == UsageLedgerInsights.Status.READY, "five-hour short segment");
        UsageLedger monthly = UsageLedger.empty();
        for (int i = 0; i < 3; i++) {
            w = new UsageWindow(10 + i * 2, TimeUnit.DAYS.toSeconds(30), 0, (NOW + 10 * DAY) / 1000);
            monthly = monthly.append(UsageHistory.MONTHLY, "free", w, NOW - (2 - i) * 3 * HOUR);
        }
        check(UsageLedgerInsights.analyze(monthly, UsageHistory.MONTHLY, w, NOW, NOW, 30).status
                == UsageLedgerInsights.Status.READY, "monthly six-hour segment");
        check(UsageLedgerInsights.analyze(monthly, UsageHistory.MONTHLY, w, NOW, NOW - 1, 30).status
                == UsageLedgerInsights.Status.STALE, "clock reversal");
        check(UsageLedgerInsights.analyze(monthly, UsageHistory.MONTHLY, w, NOW, NOW + HOUR, 30).status
                == UsageLedgerInsights.Status.READY, "freshness inclusive boundary");
        check(UsageLedgerInsights.analyze(monthly, UsageHistory.MONTHLY, w, NOW, NOW + HOUR + 1, 30).status
                == UsageLedgerInsights.Status.STALE, "freshness just beyond boundary");
        UsageLedger reset = history(10, 12, 14).append(WEEKLY, "pro", window(0, NOW + 11 * DAY), NOW + 4 * DAY);
        check(UsageLedgerInsights.analyze(reset, WEEKLY, window(0, NOW + 11 * DAY), NOW + 4 * DAY,
                NOW + 4 * DAY, 30).status == UsageLedgerInsights.Status.WARMUP, "no cross-reset forecast");
        UsageLedger recovered = history(20, 22, 5, 7, 9, 11);
        check(analyze(recovered, 11).status == UsageLedgerInsights.Status.READY
                && analyze(recovered, 11).consumedPoints == 6, "new segment recovers after allowance change");
    }

    private static void testStore() throws Exception {
        Context context = new Context();
        UsageLedgerStore.record(context, snapshot(10, NOW));
        UsageLedgerStore.record(context, snapshot(99, NOW));
        check(UsageLedgerStore.load(context).latest(WEEKLY).usedPercent == 10, "store idempotence");
        String raw = context.getSharedPreferences("codex_meter_ledger_v1", 0).getString("ledger", "");
        context.failCommit = true;
        UsageLedgerStore.record(context, snapshot(15, NOW + HOUR));
        check(raw.equals(context.getSharedPreferences("codex_meter_ledger_v1", 0).getString("ledger", "")), "failed commit preserved");
        context.failCommit = false;
        for (String damaged : new String[] {"broken", "", "{\"version\":2}", "{\"version\":1}"}) {
            context.getSharedPreferences("codex_meter_ledger_v1", 0).edit().putString("ledger", damaged).apply();
            check(UsageLedgerStore.load(context) == null, "damaged unreadable");
            UsageLedgerStore.record(context, snapshot(15, NOW + HOUR));
            check(damaged.equals(context.getSharedPreferences("codex_meter_ledger_v1", 0).getString("ledger", "")), "damaged bytes preserved");
        }
        UsageLedgerStore.clear(context);
        check(UsageLedgerStore.load(context).observations.isEmpty(), "explicit clear");
        context.getSharedPreferences("codex_meter_settings_v1", 0).edit().putString("latest", "fixture").apply();
        UsageLedgerStore.clear(context);
        check("fixture".equals(context.getSharedPreferences("codex_meter_settings_v1", 0).getString("latest", "")),
                "ledger clear leaves existing preference namespace intact");
        Thread[] workers = new Thread[4];
        for (int i = 0; i < workers.length; i++) {
            workers[i] = new Thread(() -> UsageLedgerStore.record(context, snapshot(15, NOW + HOUR)));
            workers[i].start();
        }
        for (Thread worker : workers) worker.join();
        check(UsageLedgerStore.load(context).observations.size() == 1, "concurrent same response idempotent");
        context.failAccess = true;
        UsageLedgerStore.record(context, snapshot(15, NOW + HOUR));
        check(UsageLedgerStore.load(context) == null, "storage failure isolated");
    }

    private static void testRecorder() throws Exception {
        Context context = new Context();
        UsageHistoryRecorder.record(context, snapshot(10, NOW - HOUR));
        UsageHistoryRecorder.record(context, snapshot(5, NOW));
        check(context.histories.get(WEEKLY).samples.size() == 2, "existing history retained");
        check(UsageEventStore.events(context).size() == 1, "existing event recorded before history append");
        check(UsageLedgerStore.load(context).observations.size() == 2, "recorder ledger integration");
        context.getSharedPreferences("codex_meter_ledger_v1", 0).edit().putString("ledger", "broken").apply();
        UsageHistoryRecorder.record(context, snapshot(8, NOW + HOUR));
        check(context.histories.get(WEEKLY).samples.size() == 3, "ledger damage leaves history operational");
        check("broken".equals(context.getSharedPreferences("codex_meter_ledger_v1", 0).getString("ledger", "")), "recorder preserves damage");
    }
}
