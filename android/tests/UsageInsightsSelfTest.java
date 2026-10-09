package dev.bennett.codexmeter;

import android.content.Context;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/** Deterministic derived-analysis and event-store tests, independent of live accounts. */
public final class UsageInsightsSelfTest {
    private static final long NOW = 1_800_000_000_000L;
    private static final long HOUR = TimeUnit.HOURS.toMillis(1);
    private static final long DAY = UsageInsights.DAY;
    private static final long WEEK_SECONDS = 604_800L;
    private static int checks;

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }

    private static void near(double expected, double actual, String message) {
        check(Math.abs(expected - actual) < 0.00001d, message + ": " + actual);
    }

    private static UsageWindow window(int used, long reset) {
        return new UsageWindow(used, WEEK_SECONDS, 0L, reset / 1000L);
    }

    private static UsageHistory history(long reset, int... values) {
        List<UsageSample> samples = new ArrayList<>();
        for (int i = 0; i < values.length; i++) {
            samples.add(new UsageSample(NOW - (values.length - 1 - i) * 6L * HOUR,
                    values[i], reset, WEEK_SECONDS));
        }
        return new UsageHistory(UsageHistory.WEEKLY, samples);
    }

    private static UsageInsights.Weekly analyze(int used, int days, int... values) {
        long reset = NOW + days * DAY;
        return UsageInsights.weekly(window(used, reset), history(reset, values), NOW, NOW, 30);
    }

    public static void main(String[] args) throws Exception {
        testFreshness();
        testWeekly();
        testEvents();
        testStore();
        System.out.println("Usage insight and event-store assertions passed: " + checks);
    }

    private static void testFreshness() {
        check(UsageInsights.freshness(0, NOW, 30) == UsageInsights.Freshness.UNKNOWN, "missing update");
        check(UsageInsights.freshness(NOW + 1, NOW, 30) == UsageInsights.Freshness.UNKNOWN, "clock reversal");
        check(UsageInsights.freshness(NOW - HOUR, NOW, 30) == UsageInsights.Freshness.FRESH, "inclusive boundary");
        check(UsageInsights.freshness(NOW - HOUR - 1, NOW, 30) == UsageInsights.Freshness.STALE, "stale boundary");
        check(UsageInsights.freshness(NOW - 15 * 60_000L, NOW, 5) == UsageInsights.Freshness.FRESH, "minimum tolerance");
        check(UsageInsights.freshness(NOW - 15 * 60_000L - 1, NOW, 5) == UsageInsights.Freshness.STALE, "minimum stale");
        check(UsageInsights.staleAfterMillis(120) == 4 * HOUR, "adaptive overnight interval");
        check(UsageInsights.staleAfterMillis(-1) == 15 * 60_000L, "invalid interval");
    }

    private static void testWeekly() {
        UsageInsights.Weekly comfortable = analyze(20, 4, 10, 12, 14, 17, 20);
        check(comfortable.available && comfortable.rateAvailable, "reliable 24-hour history");
        near(20d, comfortable.dailyBudget, "remaining 80 percent over 4 days");
        near(10d, comfortable.recentDailyUsage, "24-hour rate");
        check(comfortable.sampleSpanMillis == DAY, "actual window duration");
        check(comfortable.pace == UsageInsights.Pace.COMFORTABLE, "comfortable");
        check(comfortable.exhaustionAtMillis == NOW + 8 * DAY, "linear exhaustion");
        near(40d, comfortable.remainingAtReset, "projected reset balance");
        check(analyze(20, 4, 0, 5, 10, 15, 20).pace == UsageInsights.Pace.ON_TRACK, "on track");
        check(analyze(50, 3, 30, 35, 40, 45, 50).pace == UsageInsights.Pace.CAUTION, "caution");
        UsageInsights.Weekly fast = analyze(60, 3, 30, 37, 45, 52, 60);
        check(fast.pace == UsageInsights.Pace.FAST, "fast");
        check(fast.exhaustionAtMillis < fast.resetAtMillis, "depletion before reset");
        near(0d, fast.remainingAtReset, "remaining bounded at zero");
        check(!analyze(20, 4, 20).rateAvailable, "one observation");
        check(!analyze(20, 4, 10, 20).rateAvailable, "only two observations");
        check(!analyze(20, 4, 20, 20, 20).rateAvailable, "flat history");
        check(!analyze(20, 4, 19, 19, 20).rateAvailable, "one point jitter");
        check(!analyze(20, 4, 10, 25, 20).rateAvailable, "allowance correction");
        check(!analyze(60, 3, 10, 12, 15, 17, 60).rateAvailable, "single jump outlier");
        long reset = NOW + 4 * DAY;
        check(!UsageInsights.weekly(window(20, reset), history(reset, 10, 12, 14, 17, 20),
                NOW, NOW + HOUR + 1, 30).rateAvailable, "stale forecasts suppressed");
        check(!UsageInsights.weekly(null, null, NOW, NOW, 30).available, "missing weekly value");
        check(!UsageInsights.weekly(window(20, NOW), null, NOW, NOW, 30).available, "expired reset");
        check(!UsageInsights.weekly(new UsageWindow(20, WEEK_SECONDS, 0, 0), null,
                NOW, NOW, 30).available, "missing reset");
        check(!UsageInsights.weekly(window(20, reset), null, NOW, NOW - 1, 30).available, "future fetch time");
        UsageHistory sparse = new UsageHistory(UsageHistory.WEEKLY, Arrays.asList(
                new UsageSample(NOW - DAY, 10, reset, WEEK_SECONDS),
                new UsageSample(NOW - 12 * HOUR, 15, reset, WEEK_SECONDS),
                new UsageSample(NOW, 20, reset, WEEK_SECONDS)));
        check(!UsageInsights.weekly(window(20, reset), sparse, NOW, NOW, 30).rateAvailable, "unobserved long gaps");
        check(!UsageInsights.weekly(window(20, reset), history(reset - DAY, 10, 15, 20),
                NOW, NOW, 30).rateAvailable, "reset boundary isolation");
        near(80d, UsageInsights.weekly(window(20, NOW + HOUR), null, NOW, NOW, 30).dailyBudget,
                "less than one day uses remaining allowance cap");
        UsageWindow relative = new UsageWindow(20, WEEK_SECONDS, 4 * 86_400L, 0);
        near(20d, UsageInsights.weekly(relative, null, NOW, NOW, 30).dailyBudget, "relative reset");
        UsageInsights.Weekly exhausted = analyze(100, 1, 70, 77, 85, 92, 100);
        near(0d, exhausted.dailyBudget, "exhausted budget");
        long shortReset = NOW + HOUR;
        UsageInsights.Weekly shortWindow = UsageInsights.weekly(window(60, shortReset),
                history(shortReset, 30, 37, 45, 52, 60), NOW, NOW, 30);
        check(shortWindow.pace == UsageInsights.Pace.COMFORTABLE,
                "short reset uses the actual allowed rate, not a whole-day consumption comparison");
        check(shortWindow.exhaustionAtMillis > shortWindow.resetAtMillis,
                "short reset forecast consistent with comfortable pace");

        for (int used = 0; used <= 100; used++) {
            for (int days = 1; days <= 7; days++) {
                UsageInsights.Weekly result = UsageInsights.weekly(window(used, NOW + days * DAY),
                        null, NOW, NOW, 30);
                check(result.available && !result.rateAvailable, "budget without fabricated forecast");
                check(Double.isFinite(result.dailyBudget) && result.dailyBudget >= 0
                        && result.dailyBudget <= 100 - used, "bounded daily budget");
                check(result.remainingMillis == days * DAY, "reset time preserved");
            }
        }
    }

    private static void testEvents() {
        UsageSample before = new UsageSample(NOW - HOUR, 70, NOW, WEEK_SECONDS);
        check("reset_observed".equals(UsageEventDetector.detect(before, window(0, NOW + 7 * DAY), NOW)),
                "scheduled boundary observed");
        before = new UsageSample(NOW - HOUR, 70, NOW + DAY, WEEK_SECONDS);
        check("unexpected_reset".equals(UsageEventDetector.detect(before, window(0, NOW + 7 * DAY), NOW)),
                "early boundary does not claim global reset");
        check("allowance_increase".equals(UsageEventDetector.detect(before, window(60, NOW + DAY), NOW)),
                "same-window allowance increase");
        check(UsageEventDetector.detect(before, window(69, NOW + DAY), NOW).isEmpty(), "one point ignored");
        check(UsageEventDetector.detect(before, window(75, NOW + DAY), NOW).isEmpty(), "normal consumption");
        check(UsageEventDetector.detect(null, window(0, NOW + DAY), NOW).isEmpty(), "first observation");
        check(UsageEventDetector.detect(before, window(0, NOW + DAY), NOW - HOUR).isEmpty(), "duplicate fetch");
        check(UsageEventDetector.detect(before, new UsageWindow(0, WEEK_SECONDS, 0, 0), NOW).isEmpty(),
                "missing timeline does not fabricate reset");
    }

    private static void testStore() {
        Context context = new Context();
        check(!UsageEventStore.hasEvents(context), "empty store");
        context.histories.put(UsageHistory.WEEKLY, new UsageHistory(UsageHistory.WEEKLY,
                Collections.singletonList(new UsageSample(NOW - HOUR, 70, NOW, WEEK_SECONDS))));
        UsageSnapshot snapshot = new UsageSnapshot("pro", true, false, null,
                window(0, NOW + 7 * DAY), NOW);
        UsageEventStore.recordUsage(context, snapshot);
        UsageEventStore.recordUsage(context, snapshot);
        check(UsageEventStore.events(context).size() == 1, "same observation deduplicated");
        check("reset_observed".equals(UsageEventStore.events(context).get(0).type), "usage hook event");
        UsageEventStore.clear(context);
        UsageEventStore.recordConfirmedCreditUse(context, NOW - 1000);
        context.histories.put(UsageHistory.WEEKLY, new UsageHistory(UsageHistory.WEEKLY,
                Collections.singletonList(new UsageSample(NOW - HOUR, 70, NOW + DAY, WEEK_SECONDS))));
        UsageEventStore.recordUsage(context, snapshot);
        check(UsageEventStore.events(context).size() == 1, "known credit use not labeled unexpected");
        check("credit_used".equals(UsageEventStore.events(context).get(0).type), "confirmed use recorded");
        UsageEventStore.clear(context);
        RateLimitResetCredit available = new RateLimitResetCredit("test-credit", "weekly", "available",
                NOW - DAY, NOW + DAY, "private title", "private description");
        UsageEventStore.recordCredits(context, new ResetCreditsSnapshot(1, Collections.singletonList(available), NOW));
        check(!UsageEventStore.hasEvents(context), "initial balance not a grant event");
        UsageEventStore.recordCredits(context, ResetCreditsSnapshot.summary(2, NOW + 1000));
        check("bank_increased".equals(UsageEventStore.events(context).get(0).type), "bank count observed increase");
        UsageEventStore.recordCredits(context, ResetCreditsSnapshot.summary(3, NOW));
        check(UsageEventStore.events(context).size() == 1, "old credits ignored");
        UsageEventStore.clear(context);
        UsageEventStore.recordCredits(context, new ResetCreditsSnapshot(1, Collections.singletonList(available), NOW));
        RateLimitResetCredit expired = new RateLimitResetCredit("test-credit", "weekly", "expired", 0, 0, "", "");
        UsageEventStore.recordCredits(context, new ResetCreditsSnapshot(0, Collections.singletonList(expired), NOW + DAY));
        check("credit_expired".equals(UsageEventStore.events(context).get(0).type), "explicit expiry status");
        UsageEventStore.clear(context);
        UsageEventStore.recordCredits(context, new ResetCreditsSnapshot(1, Collections.singletonList(available), NOW));
        UsageEventStore.recordCredits(context, ResetCreditsSnapshot.summary(0, NOW + DAY));
        check(!UsageEventStore.hasEvents(context), "disappearance is not proof of expiry or use");
        String observation = context.getSharedPreferences("codex_meter_events_v1", 0)
                .getString("credit_observation", "");
        check(!observation.contains("private"), "only aggregate and id/status metadata saved");
        for (int i = 1; i <= 120; i++) UsageEventStore.recordConfirmedCreditUse(context, NOW + i);
        List<UsageEventStore.Event> events = UsageEventStore.events(context);
        check(events.size() == 100, "bounded event storage");
        check(events.get(0).atMillis == NOW + 120 && events.get(99).atMillis == NOW + 21, "newest first");
        UsageEventStore.clear(context);
        check(!UsageEventStore.hasEvents(context), "logout/history clear hook");
    }
}
