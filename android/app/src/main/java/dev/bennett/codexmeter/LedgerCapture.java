package dev.bennett.codexmeter;

import java.util.ArrayList;
import java.util.List;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.json.JSONArray;
import org.json.JSONObject;

/** Allowlisted extraction from the existing response; the raw response is never stored. */
public final class LedgerCapture {
    private LedgerCapture() {}
    public static List<LedgerRecord> fromSnapshot(UsageSnapshot snapshot, String body, boolean manual) {
        List<LedgerRecord> records = new ArrayList<>();
        JSONObject root = null;
        try { if (body != null) root = new JSONObject(body); } catch (Exception ignored) { }
        JSONObject limits = root == null ? null : root.optJSONObject("rate_limit");
        add(records, "five_hour", snapshot, snapshot.fiveHour, match(limits, snapshot.fiveHour), manual);
        add(records, "weekly", snapshot, snapshot.weekly, match(limits, snapshot.weekly), manual);
        add(records, "monthly", snapshot, snapshot.monthly, match(limits, snapshot.monthly), manual);
        if (limits != null) {
            JSONObject five = match(limits, snapshot.fiveHour);
            JSONObject weekly = match(limits, snapshot.weekly);
            JSONObject monthly = match(limits, snapshot.monthly);
            for (String slot : new String[] {"primary", "secondary"}) {
                JSONObject raw = limits.optJSONObject(slot + "_window");
                if (raw != null && raw != five && raw != weekly && raw != monthly)
                    add(records, "additional:main:" + slot, snapshot, UsageWindow.fromJson(raw), raw, manual);
            }
        }
        JSONArray extra = root == null ? null : root.optJSONArray("additional_rate_limits");
        if (extra != null) {
            java.util.Map<String, Integer> occurrences = new java.util.HashMap<>();
            for (int i = 0; i < Math.min(128, extra.length()); i++) {
                JSONObject object = extra.optJSONObject(i);
                if (object == null) continue;
                String identity = object.optString("limit_id", "");
                if (identity.isEmpty()) identity = object.optString("metered_feature", "");
                if (identity.isEmpty()) identity = object.optString("limit_name", "");
                if (identity.isEmpty()) identity = "unidentified_" + i;
                String key = digest(identity);
                int occurrence = occurrences.getOrDefault(key, 0);
                occurrences.put(key, occurrence + 1);
                String meter = "additional:" + key + ":" + occurrence;
                JSONObject rate = object.optJSONObject("rate_limit");
                if (rate == null) rate = object;
                for (String slot : new String[] {"primary", "secondary"}) {
                    JSONObject raw = rate.optJSONObject(slot + "_window");
                    add(records, meter + ":" + slot, snapshot, UsageWindow.fromJson(raw), raw, manual);
                }
            }
        } else {
            for (UsageLimit limit : snapshot.additionalLimits) {
                String meter = "additional:" + digest(limit.id);
                add(records, meter + ":primary", snapshot, limit.primary, null, manual);
                add(records, meter + ":secondary", snapshot, limit.secondary, null, manual);
            }
        }
        return records;
    }
    private static JSONObject match(JSONObject rate, UsageWindow window) {
        if (rate == null || window == null) return null;
        for (String slot : new String[] {"primary_window", "secondary_window"}) {
            JSONObject raw = rate.optJSONObject(slot);
            UsageWindow candidate = UsageWindow.fromJson(raw);
            if (candidate != null && candidate.windowSeconds == window.windowSeconds
                    && candidate.usedPercent == window.usedPercent
                    && candidate.resetAtEpochSeconds == window.resetAtEpochSeconds
                    && candidate.resetAfterSeconds == window.resetAfterSeconds) return raw;
        }
        return null;
    }
    private static void add(List<LedgerRecord> records, String meter, UsageSnapshot snapshot,
            UsageWindow window, JSONObject raw, boolean manual) {
        if (window == null) return;
        try {
            double used = raw == null ? window.usedPercent : raw.optDouble("used_percent", Double.NaN);
            String decimal = raw == null ? Integer.toString(window.usedPercent) : raw.get("used_percent").toString();
            records.add(new LedgerRecord(meter, snapshot.planType, snapshot.fetchedAtMillis, used, decimal,
                    Math.max(0, window.effectiveResetAtMillis(snapshot.fetchedAtMillis)), window.windowSeconds,
                    raw == null ? "api_rounded" : "api_precise", manual));
        } catch (Exception ignored) { // Invalid optional ledger values cannot invalidate the original snapshot.
        }
    }
    private static String digest(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder text = new StringBuilder();
            for (int i = 0; i < 12; i++) text.append(String.format(java.util.Locale.ROOT, "%02x", digest[i] & 255));
            return text.toString();
        } catch (Exception ex) { throw new IllegalStateException(ex); }
    }
}
