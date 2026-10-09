package dev.bennett.codexmeter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/** Separate local observations; quota percentage points are never token counts. */
public final class UsageLedger {
    public static final int VERSION = 1;
    public static final int MAX_OBSERVATIONS = 2048;
    public static final long RETENTION_MILLIS = TimeUnit.DAYS.toMillis(90);
    public final List<Observation> observations;

    private UsageLedger(List<Observation> observations) {
        this.observations = Collections.unmodifiableList(new ArrayList<>(observations));
    }

    public static UsageLedger empty() { return new UsageLedger(Collections.emptyList()); }

    public Observation latest(String kind) {
        for (int i = observations.size() - 1; i >= 0; i--) {
            Observation observation = observations.get(i);
            if (observation.kind.equals(kind)) return observation;
        }
        return null;
    }

    public List<Observation> forKind(String kind) {
        List<Observation> result = new ArrayList<>();
        for (Observation observation : observations) {
            if (observation.kind.equals(kind)) result.add(observation);
        }
        return Collections.unmodifiableList(result);
    }

    /** Equal or older responses cannot rewrite a previously observed value. */
    public UsageLedger append(String kind, String plan, UsageWindow window, long at) {
        if (!knownKind(kind) || window == null || at <= 0L || window.windowSeconds <= 0L
                || window.windowSeconds > TimeUnit.DAYS.toSeconds(366)) return this;
        Observation previous = latest(kind);
        if (previous != null && at <= previous.atMillis) return this;
        // A clock rollback or a late response must not reorder the ledger across meters.
        if (!observations.isEmpty() && at < observations.get(observations.size() - 1).atMillis)
            return this;
        List<Observation> next = new ArrayList<>();
        long oldest = Math.max(0L, at - RETENTION_MILLIS);
        for (Observation observation : observations) {
            if (observation.atMillis >= oldest) next.add(observation);
        }
        next.add(new Observation(kind, safePlan(plan), at, window.usedPercent,
                window.windowSeconds, Math.max(0L, window.effectiveResetAtMillis(at))));
        if (next.size() > MAX_OBSERVATIONS)
            next = new ArrayList<>(next.subList(next.size() - MAX_OBSERVATIONS, next.size()));
        return new UsageLedger(next);
    }

    public JSONObject toJson() throws JSONException {
        JSONArray entries = new JSONArray();
        for (Observation observation : observations) {
            entries.put(new JSONObject().put("kind", observation.kind).put("plan", observation.plan)
                    .put("at", observation.atMillis).put("used_percent", observation.usedPercent)
                    .put("window_seconds", observation.windowSeconds)
                    .put("reset_at", observation.resetAtMillis));
        }
        return new JSONObject().put("version", VERSION).put("source", "usage_api")
                .put("observations", entries);
    }

    /** Reject an unreadable/future schema so the store can preserve the original bytes. */
    public static UsageLedger fromJson(JSONObject json) throws JSONException {
        if (json == null || exactLong(json, "version") != VERSION
                || !"usage_api".equals(json.getString("source")))
            throw new JSONException("Unsupported ledger schema");
        JSONArray entries = json.getJSONArray("observations");
        if (entries.length() > MAX_OBSERVATIONS) throw new JSONException("Oversized ledger");
        List<Observation> result = new ArrayList<>();
        long lastAt = 0L;
        java.util.Map<String, Long> latest = new java.util.HashMap<>();
        for (int i = 0; i < entries.length(); i++) {
            JSONObject entry = entries.getJSONObject(i);
            String kind = entry.getString("kind");
            String plan = entry.getString("plan");
            long at = exactLong(entry, "at");
            long used = exactLong(entry, "used_percent");
            long seconds = exactLong(entry, "window_seconds");
            long reset = exactLong(entry, "reset_at");
            if (!knownKind(kind) || !plan.equals(safePlan(plan)) || at <= 0L || at < lastAt
                    || at <= latest.getOrDefault(kind, 0L) || used < 0L || used > 100L
                    || seconds <= 0L || seconds > TimeUnit.DAYS.toSeconds(366) || reset < 0L)
                throw new JSONException("Invalid ledger observation");
            result.add(new Observation(kind, plan, at, (int) used, seconds, reset));
            latest.put(kind, at);
            lastAt = at;
        }
        return new UsageLedger(result);
    }

    private static long exactLong(JSONObject json, String key) throws JSONException {
        Object raw = json.get(key);
        if (!(raw instanceof Byte || raw instanceof Short || raw instanceof Integer
                || raw instanceof Long)) throw new JSONException("Expected integer " + key);
        return ((Number) raw).longValue();
    }

    private static boolean knownKind(String kind) {
        return UsageHistory.FIVE_HOUR.equals(kind) || UsageHistory.WEEKLY.equals(kind)
                || UsageHistory.MONTHLY.equals(kind);
    }

    private static String safePlan(String value) {
        String plan = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        return plan.matches("[a-z0-9_-]{1,40}") ? plan : "unknown";
    }

    public enum Change { BASELINE, NO_CHANGE, USAGE_INCREASE, ALLOWANCE_INCREASE,
        RESET_OBSERVED, WINDOW_CHANGED, PLAN_CHANGED, GAP, MISSING_RESET }

    public static Change change(Observation before, Observation after) {
        if (before == null) return Change.BASELINE;
        if (!before.plan.equals(after.plan)) return Change.PLAN_CHANGED;
        if (before.resetAtMillis <= before.atMillis || after.resetAtMillis <= after.atMillis)
            return Change.MISSING_RESET;
        if (!UsageWindow.sameResetWindow(before.resetAtMillis, before.windowSeconds,
                after.resetAtMillis, after.windowSeconds)) {
            return before.windowSeconds == after.windowSeconds && after.atMillis >= before.resetAtMillis
                    && after.resetAtMillis > before.resetAtMillis
                    ? Change.RESET_OBSERVED : Change.WINDOW_CHANGED;
        }
        if (after.atMillis >= before.resetAtMillis) return Change.WINDOW_CHANGED;
        if (after.usedPercent < before.usedPercent) return Change.ALLOWANCE_INCREASE;
        if (after.atMillis - before.atMillis > UsageInsights.MAXIMUM_GAP) return Change.GAP;
        return after.usedPercent == before.usedPercent ? Change.NO_CHANGE : Change.USAGE_INCREASE;
    }

    public static final class Observation {
        public final String kind;
        public final String plan;
        public final long atMillis;
        public final int usedPercent;
        public final long windowSeconds;
        public final long resetAtMillis;

        private Observation(String kind, String plan, long at, int used, long seconds, long reset) {
            this.kind = kind; this.plan = plan; this.atMillis = at; this.usedPercent = used;
            this.windowSeconds = seconds; this.resetAtMillis = reset;
        }
    }
}
