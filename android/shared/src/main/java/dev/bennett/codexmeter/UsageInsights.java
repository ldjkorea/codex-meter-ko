package dev.bennett.codexmeter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/** Read-only analysis of authoritative usage values and existing local samples. */
public final class UsageInsights {
    public static final long DAY = TimeUnit.DAYS.toMillis(1);
    public static final long MINIMUM_SPAN = TimeUnit.HOURS.toMillis(6);
    public static final long MAXIMUM_GAP = TimeUnit.HOURS.toMillis(6);

    private UsageInsights() {}

    public enum Freshness { UNKNOWN, FRESH, STALE }
    public enum Pace { UNAVAILABLE, COMFORTABLE, ON_TRACK, CAUTION, FAST }

    public static long staleAfterMillis(int refreshMinutes) {
        return Math.max(TimeUnit.MINUTES.toMillis(15),
                TimeUnit.MINUTES.toMillis(Math.max(1L, refreshMinutes)) * 2L);
    }

    public static Freshness freshness(long successfulAt, long now, int refreshMinutes) {
        if (successfulAt <= 0L || successfulAt > now) return Freshness.UNKNOWN;
        return now - successfulAt <= staleAfterMillis(refreshMinutes)
                ? Freshness.FRESH : Freshness.STALE;
    }

    public static Weekly weekly(UsageWindow window, UsageHistory history,
            long observedAt, long now, int refreshMinutes) {
        if (window == null || window.windowSeconds <= 0L || !window.showsResetCountdown()
                || observedAt <= 0L || now < observedAt) return Weekly.unavailable();
        long reset = window.effectiveResetAtMillis(observedAt);
        if (reset <= now) return Weekly.unavailable();
        long remainingMillis = reset - now;
        double budget = window.remainingPercent() * (double) DAY / remainingMillis;
        budget = Math.min(window.remainingPercent(), budget);
        Weekly unavailableRate = new Weekly(true, false, budget, 0d, 0L, 0L,
                0d, reset, remainingMillis, Pace.UNAVAILABLE);
        if (freshness(observedAt, now, refreshMinutes) != Freshness.FRESH
                || history == null) return unavailableRate;

        List<UsageSample> recent = new ArrayList<>();
        for (UsageSample sample : history.samples) {
            if (sample.observedAtMillis < observedAt - DAY) continue;
            if (sample.observedAtMillis > observedAt || sample.observedAtMillis <= 0L) continue;
            if (UsageWindow.sameResetWindow(sample.resetAtMillis, sample.windowSeconds,
                    reset, window.windowSeconds)) recent.add(sample);
        }
        if (recent.size() < 3) return unavailableRate;
        UsageSample first = recent.get(0);
        UsageSample last = recent.get(recent.size() - 1);
        long span = last.observedAtMillis - first.observedAtMillis;
        if (span < MINIMUM_SPAN || observedAt - last.observedAtMillis > TimeUnit.MINUTES.toMillis(5)
                || last.usedPercent != window.usedPercent) return unavailableRate;
        for (int i = 1; i < recent.size(); i++) {
            UsageSample before = recent.get(i - 1);
            UsageSample after = recent.get(i);
            long gap = after.observedAtMillis - before.observedAtMillis;
            int change = after.usedPercent - before.usedPercent;
            // Reject reset/correction, out-of-order, sparse, or large single-jump histories.
            if (gap <= 0L || gap > MAXIMUM_GAP || change < 0 || change > 30)
                return unavailableRate;
        }
        int consumed = last.usedPercent - first.usedPercent;
        if (consumed < 2) return unavailableRate;
        double rate = consumed / (double) span;
        double daily = rate * DAY;
        double rawOffset = window.remainingPercent() / rate;
        if (!Double.isFinite(rawOffset) || rawOffset > Long.MAX_VALUE - observedAt)
            return unavailableRate;
        long exhaustion = observedAt + Math.round(rawOffset);
        double atReset = Math.max(0d, window.remainingPercent() - rate * (reset - observedAt));
        // Compare rates over the actual time left, including windows shorter than one day.
        double allowedDailyRate = window.remainingPercent() * (double) DAY / remainingMillis;
        double ratio = allowedDailyRate <= 0d ? Double.POSITIVE_INFINITY : daily / allowedDailyRate;
        Pace pace = ratio <= 0.8d ? Pace.COMFORTABLE : ratio <= 1.1d ? Pace.ON_TRACK
                : ratio <= 1.5d ? Pace.CAUTION : Pace.FAST;
        return new Weekly(true, true, budget, daily, span, exhaustion,
                atReset, reset, remainingMillis, pace);
    }

    public static final class Weekly {
        public final boolean available;
        public final boolean rateAvailable;
        public final double dailyBudget;
        public final double recentDailyUsage;
        public final long sampleSpanMillis;
        public final long exhaustionAtMillis;
        public final double remainingAtReset;
        public final long resetAtMillis;
        public final long remainingMillis;
        public final Pace pace;

        private Weekly(boolean available, boolean rateAvailable, double budget,
                double dailyUsage, long span, long exhaustion, double atReset,
                long reset, long remainingMillis, Pace pace) {
            this.available = available;
            this.rateAvailable = rateAvailable;
            this.dailyBudget = budget;
            this.recentDailyUsage = dailyUsage;
            this.sampleSpanMillis = span;
            this.exhaustionAtMillis = exhaustion;
            this.remainingAtReset = atReset;
            this.resetAtMillis = reset;
            this.remainingMillis = remainingMillis;
            this.pace = pace;
        }

        private static Weekly unavailable() {
            return new Weekly(false, false, 0d, 0d, 0L, 0L, 0d, 0L, 0L, Pace.UNAVAILABLE);
        }
    }
}
