package dev.bennett.codexmeter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/** Conservative forecasts over a contiguous observed segment of one quota window. */
public final class UsageLedgerInsights {
    private UsageLedgerInsights() {}

    public enum Status { READY, EMPTY, UNREADABLE, STALE, MISSING_RESET, MISMATCH,
        WARMUP, LOW_CHANGE, GAP, LARGE_JUMP }

    public static Result analyze(UsageLedger ledger, String kind, UsageWindow window,
            long observedAt, long now, int refreshMinutes) {
        if (ledger == null) return unavailable(Status.UNREADABLE);
        if (window == null || window.windowSeconds <= 0L || !window.showsResetCountdown()
                || window.effectiveResetAtMillis(observedAt) <= now)
            return unavailable(Status.MISSING_RESET);
        if (UsageInsights.freshness(observedAt, now, refreshMinutes) != UsageInsights.Freshness.FRESH)
            return unavailable(Status.STALE);
        List<UsageLedger.Observation> all = ledger.forKind(kind);
        if (all.isEmpty()) return unavailable(Status.EMPTY);
        UsageLedger.Observation last = all.get(all.size() - 1);
        long reset = window.effectiveResetAtMillis(observedAt);
        if (last.atMillis != observedAt || last.usedPercent != window.usedPercent
                || !UsageWindow.sameResetWindow(last.resetAtMillis, last.windowSeconds,
                        reset, window.windowSeconds)) return unavailable(Status.MISMATCH);
        List<UsageLedger.Observation> segment = new ArrayList<>();
        UsageLedger.Observation previous = null;
        Status boundary = Status.WARMUP;
        for (UsageLedger.Observation observation : all) {
            if (observation.atMillis < observedAt - UsageInsights.DAY) continue;
            if (!observation.plan.equals(last.plan)
                    || !UsageWindow.sameResetWindow(observation.resetAtMillis, observation.windowSeconds,
                            reset, window.windowSeconds)) {
                segment.clear();
                previous = null;
                boundary = Status.WARMUP;
                continue;
            }
            if (previous != null) {
                UsageLedger.Change change = UsageLedger.change(previous, observation);
                long maximumGap = UsageHistory.FIVE_HOUR.equals(kind)
                        ? TimeUnit.HOURS.toMillis(1) : UsageInsights.MAXIMUM_GAP;
                if (observation.atMillis - previous.atMillis > maximumGap
                        && (change == UsageLedger.Change.USAGE_INCREASE || change == UsageLedger.Change.NO_CHANGE)) {
                    segment.clear();
                    boundary = Status.GAP;
                } else if (change != UsageLedger.Change.USAGE_INCREASE && change != UsageLedger.Change.NO_CHANGE) {
                    segment.clear();
                    boundary = change == UsageLedger.Change.GAP ? Status.GAP : Status.WARMUP;
                } else if (observation.usedPercent - previous.usedPercent > 30) {
                    segment.clear();
                    boundary = Status.LARGE_JUMP;
                }
            }
            segment.add(observation);
            previous = observation;
        }
        UsageLedger.Observation first = segment.get(0);
        long span = last.atMillis - first.atMillis;
        long minimumSpan = UsageHistory.FIVE_HOUR.equals(kind)
                ? TimeUnit.MINUTES.toMillis(10) : UsageInsights.MINIMUM_SPAN;
        if (segment.size() < 3 || span < minimumSpan) return unavailable(boundary);
        int consumed = last.usedPercent - first.usedPercent;
        if (consumed < 2) return unavailable(Status.LOW_CHANGE);
        double rate = consumed / (double) span;
        long remainingMillis = reset - observedAt;
        double offset = window.remainingPercent() / rate;
        if (!Double.isFinite(offset) || offset > Long.MAX_VALUE - observedAt)
            return unavailable(Status.WARMUP);
        double budget = Math.min(window.remainingPercent(),
                window.remainingPercent() * (double) UsageInsights.DAY / remainingMillis);
        return new Result(Status.READY, segment.size(), span, consumed,
                rate * UsageInsights.DAY, budget,
                observedAt + Math.round(offset), reset,
                Math.max(0d, window.remainingPercent() - rate * remainingMillis));
    }

    private static Result unavailable(Status status) {
        return new Result(status, 0, 0L, 0, 0d, 0d, 0L, 0L, 0d);
    }

    public static final class Result {
        public final Status status;
        public final int observationCount;
        public final long spanMillis;
        public final int consumedPoints;
        public final double recentDailyUsage;
        public final double dailyBudget;
        public final long exhaustionAtMillis;
        public final long resetAtMillis;
        public final double remainingAtReset;

        private Result(Status status, int count, long span, int consumed, double daily,
                double budget, long exhaustion, long reset, double remaining) {
            this.status = status; this.observationCount = count; this.spanMillis = span;
            this.consumedPoints = consumed; this.recentDailyUsage = daily; this.dailyBudget = budget;
            this.exhaustionAtMillis = exhaustion; this.resetAtMillis = reset;
            this.remainingAtReset = remaining;
        }
    }
}
