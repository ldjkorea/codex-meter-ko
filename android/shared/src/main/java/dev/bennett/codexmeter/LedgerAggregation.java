package dev.bennett.codexmeter;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Seoul calendar days. Never interpolate a midnight observation or distribute a cross-day delta. */
public final class LedgerAggregation {
    public static final ZoneId ZONE = ZoneId.of("Asia/Seoul");
    public static final long MAX_GAP = UsageInsights.MAXIMUM_GAP;
    private LedgerAggregation() {}
    public static LocalDate day(long at) { return Instant.ofEpochMilli(at).atZone(ZONE).toLocalDate(); }

    public enum Change { BASELINE, NORMAL, RESET, EARLY_WINDOW, CREDIT_CHANGE, CORRECTION,
        POLICY_CHANGE, GAP, MISSING_TIMELINE }

    public static Change classify(LedgerRecord before, LedgerRecord after, List<Long> credits) {
        if (before == null) return Change.BASELINE;
        if (!before.policy().equals(after.policy())) return Change.POLICY_CHANGE;
        if (after.at <= before.at || before.reset <= before.at || after.reset <= after.at)
            return Change.MISSING_TIMELINE;
        boolean same = UsageWindow.sameResetWindow(before.reset, before.seconds, after.reset, after.seconds);
        if (!same || after.used < before.used) {
            // Confirmation concerns the operation. Its per-meter causal scope is not assumed.
            for (long credit : credits) {
                if (credit > before.at && credit <= after.at && after.at - credit <= 300_000L)
                    return Change.CREDIT_CHANGE;
            }
            if (!same) return after.at >= before.reset && after.reset > before.reset
                    ? Change.RESET : Change.EARLY_WINDOW;
            return Change.CORRECTION;
        }
        if (after.at >= before.reset) return Change.EARLY_WINDOW;
        return after.at - before.at > MAX_GAP ? Change.GAP : Change.NORMAL;
    }

    public static Result aggregate(List<LedgerRecord> input, List<Long> credits) {
        List<LedgerRecord> records = new ArrayList<>(input);
        records.sort(Comparator.comparing((LedgerRecord r) -> r.meter).thenComparingLong(r -> r.at));
        Result result = new Result();
        Map<String, LedgerRecord> latest = new LinkedHashMap<>();
        Map<String, WindowTotal> windows = new LinkedHashMap<>();
        for (LedgerRecord current : records) {
            LedgerRecord before = latest.get(current.meter);
            if (before != null && current.at <= before.at) continue;
            latest.put(current.meter, current);
            String date = day(current.at).toString();
            String key = current.policy() + "@" + date;
            Day total = result.days.computeIfAbsent(key, ignored -> new Day(current.policy(), date));
            total.count++; total.first = Math.min(total.first, current.at); total.last = Math.max(total.last, current.at);
            if ("legacy".equals(current.source)) total.legacy = true;
            Change change = classify(before, current, credits);
            result.events.add(new Event(current, change));
            WindowTotal window = windows.get(current.meter);
            if (current.reset > current.at) {
                if (window == null || change != Change.NORMAL && change != Change.GAP) {
                    window = new WindowTotal(current.policy(), current.reset, current.at);
                    windows.put(current.meter, window);
                    result.windows.add(window);
                }
                window.count++;
                if (before != null && change == Change.NORMAL) window.points += current.used - before.used;
                if (before != null && change == Change.GAP) window.uncertainPoints += current.used - before.used;
            } else windows.remove(current.meter);
            if (before == null || change != Change.NORMAL && change != Change.GAP) {
                if (change != Change.BASELINE) total.boundaries++;
                continue;
            }
            double delta = current.used - before.used;
            boolean crossDay = !day(before.at).equals(day(current.at));
            if (change == Change.NORMAL && !crossDay) {
                total.points += delta;
                total.coveredMillis += current.at - before.at;
            } else {
                total.uncertain++;
                result.uncertain.add(new Interval(current.policy(), before.at, current.at, delta,
                        crossDay ? "date_boundary" : "measurement_gap"));
            }
        }
        return result;
    }

    public static final class Day {
        public final String policy, date;
        public int count, uncertain, boundaries;
        public double points;
        public long first = Long.MAX_VALUE, last, coveredMillis;
        public boolean legacy;
        public Day(String policy, String date) { this.policy = policy; this.date = date; }
        public String quality() { return uncertain > 0 ? "gap" : "partial"; }
    }
    public static final class Interval {
        public final String policy, reason;
        public final long start, end;
        public final double points;
        public Interval(String policy, long start, long end, double points, String reason) {
            this.policy = policy; this.start = start; this.end = end; this.points = points; this.reason = reason;
        }
    }
    public static final class Event {
        public final LedgerRecord record;
        public final Change change;
        Event(LedgerRecord record, Change change) { this.record = record; this.change = change; }
    }
    public static final class WindowTotal {
        public final String policy;
        public final long reset, first;
        public int count;
        public double points, uncertainPoints;
        WindowTotal(String policy, long reset, long first) {
            this.policy = policy; this.reset = reset; this.first = first;
        }
    }
    public static final class Result {
        public final Map<String, Day> days = new LinkedHashMap<>();
        public final List<Interval> uncertain = new ArrayList<>();
        public final List<Event> events = new ArrayList<>();
        public final List<WindowTotal> windows = new ArrayList<>();
    }
}
