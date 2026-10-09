package dev.bennett.codexmeter;

import java.time.Instant;
import java.util.Locale;
import org.json.JSONException;
import org.json.JSONObject;

/** API quota observations only. No auth identity, request body or conversation content. */
public final class LedgerRecord {
    public final String meter, plan, source, decimal;
    public final long at, reset, seconds;
    public final double used;
    public final boolean manual;

    public LedgerRecord(String meter, String plan, long at, double used, String decimal,
            long reset, long seconds, String source, boolean manual) {
        if (meter == null || !meter.matches("[a-z0-9_:.-]{1,120}") || at <= 0
                || !Double.isFinite(used) || used < 0 || used > 100 || seconds <= 0
                || seconds > 31_622_400L || reset < 0)
            throw new IllegalArgumentException("Invalid quota observation");
        this.meter = meter; this.plan = cleanPlan(plan); this.at = at; this.used = used;
        String numeric = decimal == null ? Double.toString(used) : decimal;
        try {
            java.math.BigDecimal value = new java.math.BigDecimal(numeric);
            if (value.signum() < 0 || value.compareTo(java.math.BigDecimal.valueOf(100)) > 0
                    || numeric.length() > 80 || Double.compare(value.doubleValue(), used) != 0)
                throw new IllegalArgumentException("Invalid precision");
        } catch (NumberFormatException ex) { throw new IllegalArgumentException("Invalid precision"); }
        this.decimal = numeric; this.reset = reset; this.seconds = seconds;
        this.source = "api_precise".equals(source) ? source
                : "api_rounded".equals(source) ? source : "legacy";
        this.manual = manual;
    }

    public String policy() { return meter + "|" + plan + "|" + seconds; }
    public double remaining() { return 100d - used; }
    public static String cleanPlan(String value) {
        String s = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        return s.matches("[a-z0-9_-]{1,40}") ? s : "unknown";
    }
    public JSONObject toJson() throws JSONException {
        return new JSONObject().put("meter", meter).put("plan", plan).put("policy", policy())
                .put("observed_at_utc", Instant.ofEpochMilli(at).toString())
                .put("used_percent", used).put("used_percent_decimal", decimal)
                .put("remaining_percent", remaining()).put("reset_at_utc",
                        reset > 0 ? Instant.ofEpochMilli(reset).toString() : JSONObject.NULL)
                .put("window_seconds", seconds).put("source", source).put("manual", manual)
                .put("freshness_at_capture", "legacy".equals(source) ? "unknown" : "fresh")
                .put("precision", "api_precise".equals(source) ? "server_decimal" : "rounded");
    }
}
