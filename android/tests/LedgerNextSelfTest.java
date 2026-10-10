package dev.bennett.codexmeter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import org.json.JSONArray;
import org.json.JSONObject;

/** Production calculation/capture/export tests. Persistence is tested separately against SQLite. */
public final class LedgerNextSelfTest {
    private static final long HOUR = 3_600_000L;
    private static final long DAY = 24 * HOUR;
    private static final long START = Instant.parse("2026-10-08T00:00:00Z").toEpochMilli();
    private static final long RESET = START + 5 * DAY;
    private static int checks;

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
    private static void close(double actual, double expected, String message) {
        check(Math.abs(actual - expected) < 1e-8, message + ": " + actual + " != " + expected);
    }
    private static LedgerRecord record(String meter, String plan, long at, double used, long reset, long seconds) {
        return new LedgerRecord(meter, plan, at, used, Double.toString(used), reset, seconds, "api_precise", false);
    }
    private static LedgerRecord weekly(long at, double used) {
        return record("weekly", "pro", at, used, RESET, 604800);
    }
    private static LedgerAggregation.Result aggregate(LedgerRecord... rows) {
        return LedgerAggregation.aggregate(Arrays.asList(rows), Collections.emptyList());
    }
    private static double total(LedgerAggregation.Result result) {
        double sum = 0;
        for (LedgerAggregation.Day day : result.days.values()) sum += day.points;
        return sum;
    }
    private static LedgerAggregation.Change change(LedgerRecord a, LedgerRecord b) {
        return LedgerAggregation.classify(a, b, Collections.emptyList());
    }
    private static List<LedgerRecord> series(String meter, long now, long step, double... used) {
        List<LedgerRecord> rows = new ArrayList<>();
        for (int i = 0; i < used.length; i++) rows.add(record(meter, "pro", now - (used.length - 1 - i) * step,
                used[i], now + (meter.equals("five_hour") ? HOUR : 4 * DAY), meter.equals("five_hour") ? 18000 : 604800));
        return rows;
    }

    public static void main(String[] args) throws Exception {
        dailyAndBoundaries();
        precisionAndCapture();
        forecasts();
        exports();
        propertyCases();
        System.out.println("Ledger NEXT calculation, capture, forecast and export: " + checks + " assertions passed.");
    }

    private static void dailyAndBoundaries() {
        check(LedgerAggregation.day(Instant.parse("2026-10-07T14:59:59Z").toEpochMilli()).toString().equals("2026-10-07"), "Before Seoul midnight");
        check(LedgerAggregation.day(Instant.parse("2026-10-07T15:00:00Z").toEpochMilli()).toString().equals("2026-10-08"), "At Seoul midnight");
        LedgerRecord a = weekly(START, 10.125), b = weekly(START + HOUR, 13.875);
        LedgerAggregation.Result daily = aggregate(a, b, b);
        close(total(daily), 3.75, "Precise delta and duplicate observation");
        check(daily.days.values().iterator().next().count == 2, "Duplicate does not count as observation");
        check(daily.days.values().iterator().next().coveredMillis == HOUR, "Observed span");
        check(daily.days.values().iterator().next().quality().equals("partial"), "Sparse endpoints are not complete day");
        long midnight = START + 15 * HOUR;
        LedgerAggregation.Result crossed = aggregate(weekly(midnight - HOUR, 20), weekly(midnight + HOUR, 25));
        check(crossed.days.size() == 2, "Two actual observed days");
        close(total(crossed), 0, "No fabricated midnight allocation");
        close(crossed.uncertain.get(0).points, 5, "Cross-day change preserved");
        check(crossed.uncertain.get(0).reason.equals("date_boundary"), "Boundary reason");
        LedgerAggregation.Result gap = aggregate(a, weekly(START + 7 * HOUR, 30));
        close(total(gap), 0, "Long unobserved interval excluded from daily totals");
        check(gap.uncertain.size() == 1 && gap.days.values().iterator().next().quality().equals("gap"), "Gap quality");
        check(aggregate(a).days.size() == 1, "Offline recovery invents no observations");
        for (String meter : new String[] {"five_hour", "weekly", "monthly"}) {
            long seconds = meter.equals("five_hour") ? 18000 : meter.equals("weekly") ? 604800 : 2592000;
            LedgerRecord before = record(meter, "pro", START, 90, START + HOUR, seconds);
            LedgerRecord after = record(meter, "pro", START + 2 * HOUR, 2, START + HOUR + seconds * 1000, seconds);
            check(change(before, after) == LedgerAggregation.Change.RESET, meter + " scheduled reset");
            close(total(aggregate(before, after)), 0, "No reset subtraction " + meter);
        }
        LedgerRecord early = weekly(START + HOUR, 2);
        early = record("weekly", "pro", early.at, early.used, RESET + DAY, 604800);
        check(change(a, early) == LedgerAggregation.Change.EARLY_WINDOW, "Early weekly window, unknown cause");
        LedgerRecord lower = weekly(START + HOUR, 5);
        check(change(a, lower) == LedgerAggregation.Change.CORRECTION, "Allowance gain does not imply Reset Credit");
        check(LedgerAggregation.classify(a, lower, Arrays.asList(lower.at - 1000)) == LedgerAggregation.Change.CREDIT_CHANGE, "Confirmed credit operation near gain");
        check(LedgerAggregation.classify(a, lower, Arrays.asList(a.at - 1)) == LedgerAggregation.Change.CORRECTION, "Old credit not causal proof");
        LedgerRecord policy = record("weekly", "plus", START + HOUR, 30, RESET, 604800);
        check(change(a, policy) == LedgerAggregation.Change.POLICY_CHANGE, "Plan change");
        LedgerAggregation.Result returnPlan = aggregate(a, policy, weekly(START + 2 * HOUR, 40), weekly(START + 3 * HOUR, 42));
        close(total(returnPlan), 2, "Plan returning does not bridge incompatible interval");
        check(returnPlan.windows.size() == 3, "Window segments preserve plan boundaries");
        close(returnPlan.windows.get(2).points, 2, "Window total after plan return");
        check(change(a, record("weekly", "pro", START + HOUR, 20, 0, 604800)) == LedgerAggregation.Change.MISSING_TIMELINE, "Missing reset");
        check(change(a, record("weekly", "pro", START + HOUR, 20, RESET, 2592000)) == LedgerAggregation.Change.POLICY_CHANGE, "Duration/policy change");
        LedgerAggregation.Result separate = aggregate(a, b, record("monthly", "pro", START, 50, RESET, 2592000),
                record("monthly", "pro", START + HOUR, 52, RESET, 2592000));
        check(separate.days.size() == 2, "Meters never combined");
        close(separate.days.get(a.policy() + "@2026-10-08").points, 3.75, "Independent weekly total");
        LedgerRecord legacy = new LedgerRecord("weekly", "pro", START, 10, "10", RESET, 604800, "legacy", false);
        check(aggregate(legacy, b).days.values().iterator().next().legacy, "Migrated records retain legacy confidence");
    }

    private static void precisionAndCapture() throws Exception {
        JSONObject window = new JSONObject().put("used_percent", 12.345678901).put("limit_window_seconds", 604800)
                .put("reset_at", RESET / 1000).put("reset_after_seconds", 0);
        JSONObject root = new JSONObject().put("rate_limit", new JSONObject().put("primary_window", window))
                .put("access_token", "SENSITIVE_FIXTURE_TOKEN").put("conversation", "PRIVATE_FIXTURE_PROMPT");
        UsageSnapshot snapshot = new UsageSnapshot("pro", true, false, null, UsageWindow.fromJson(window), START);
        List<LedgerRecord> rows = LedgerCapture.fromSnapshot(snapshot, root.toString(), true);
        check(rows.size() == 1 && rows.get(0).meter.equals("weekly"), "Weekly-only account, no invented five-hour");
        close(rows.get(0).used, 12.345678901, "High precision from raw response");
        check(rows.get(0).manual && rows.get(0).source.equals("api_precise"), "Manual provenance");
        check(rows.get(0).decimal.equals("12.345678901"), "Original numeric decimal preserved");
        check(snapshot.weekly.usedPercent == 12, "Original integer display preserved");
        check(LedgerCapture.fromSnapshot(snapshot, "invalid body", false).get(0).source.equals("api_precise"), "Snapshot-owned precision survives missing raw body");
        close(LedgerCapture.fromSnapshot(snapshot, "invalid body", false).get(0).used,12.345678901,"No unrelated ledger precision lookup");
        root.getJSONObject("rate_limit").put("secondary_window", new JSONObject(window.toString()).put("limit_window_seconds", 7200));
        check(LedgerCapture.fromSnapshot(snapshot, root.toString(), false).size() == 2, "Unclassified main window kept as independent additional meter");
        root.getJSONObject("rate_limit").remove("secondary_window");
        JSONArray extra = new JSONArray();
        extra.put(new JSONObject().put("limit_name", "Feature A").put("rate_limit", new JSONObject().put("primary_window", window)));
        extra.put(new JSONObject().put("limit_name", "Feature A").put("rate_limit", new JSONObject().put("primary_window", window)));
        extra.put(new JSONObject().put("limit_name", "Feature B").put("rate_limit", new JSONObject().put("secondary_window", window)));
        root.put("additional_rate_limits", extra);
        rows = LedgerCapture.fromSnapshot(snapshot, root.toString(), false);
        check(rows.size() == 4, "All independent additional windows");
        check(!rows.get(1).meter.equals(rows.get(2).meter), "Duplicate identities disambiguated");
        check(!rows.get(1).meter.contains("Feature"), "Additional labels not persisted in ID");
        extra.remove(2); extra.remove(1);
        extra.put(new JSONObject().put("limit_name", "invalid").put("primary_window", new JSONObject(window.toString()).put("used_percent", 101)));
        check(LedgerCapture.fromSnapshot(snapshot, root.toString(), false).size() == 2, "Out-of-range optional raw value ignored");
        for (double invalid : new double[] {-1, 101, Double.NaN, Double.POSITIVE_INFINITY}) {
            boolean rejected = false;
            try { weekly(START, invalid); } catch (IllegalArgumentException expected) { rejected = true; }
            check(rejected, "Invalid used value rejected");
        }
        boolean rejected = false;
        try { new LedgerRecord("weekly", "pro", START, 5, "99", RESET, 604800, "api_precise", false); }
        catch (IllegalArgumentException expected) { rejected = true; }
        check(rejected, "Precision text must agree with numeric value");
    }

    private static void forecasts() {
        long now = START + 12 * HOUR;
        String policy = weekly(now, 10).policy();
        List<LedgerRecord> stable = series("weekly", now, 3 * HOUR, 10, 13, 16, 19, 22);
        LedgerForecast.Result result = LedgerForecast.analyze(stable, policy, now, 15);
        check(result.ready, "Enough fresh observations");
        close(result.dailyRate, 24, "Recent rate pp/day");
        close(result.budget, 19.5, "Allowance/day through reset");
        check(result.risk && result.exhaustion < result.latest.reset, "Exhaustion risk before reset");
        close(result.atReset, 0, "Remaining bounded to zero");
        LedgerForecast.Result baseline = LedgerForecast.analyze(stable.subList(4,5),policy,now,15);
        check(baseline.budgetAvailable && !baseline.ready,"Current daily allowance can be shown without inventing a forecast");
        check(!LedgerForecast.analyze(stable,policy,now+2*HOUR,15).budgetAvailable,"Stale observation cannot show current daily allowance");
        check(!LedgerForecast.analyze(stable.subList(2, 4), policy, stable.get(3).at, 15).ready, "Too few observations");
        check(!LedgerForecast.analyze(stable, policy, now + 2 * HOUR, 15).ready, "Stale observation disables forecast");
        check(!LedgerForecast.analyze(series("weekly", now, HOUR, 1, 2, 3), policy, now, 15).ready, "Too little span");
        check(!LedgerForecast.analyze(series("weekly", now, 3 * HOUR, 1, 1.1, 1.2), policy, now, 15).ready, "Too small change");
        check(!LedgerForecast.analyze(series("weekly", now, 3 * HOUR, 10, 13, 2), policy, now, 15).ready, "Allowance correction resets forecast");
        check(!LedgerForecast.analyze(series("weekly", now, 3 * HOUR, 10, 13, 50), policy, now, 15).ready, "Large jump unstable");
        List<LedgerRecord> withGap = series("weekly", now, 7 * HOUR, 10, 20, 30);
        check(!LedgerForecast.analyze(withGap, policy, now, 15).ready, "Offline gap suppresses forecast");
        List<LedgerRecord> policyReturn = new ArrayList<>(stable);
        policyReturn.add(record("weekly", "plus", now - HOUR, 20, RESET, 604800));
        check(!LedgerForecast.analyze(policyReturn, policy, now, 15).ready, "No bridging temporary plan change");
        List<LedgerRecord> spike = series("weekly", now, 3 * HOUR, 10, 11, 12, 13, 14, 18, 22, 26);
        result = LedgerForecast.analyze(spike, policy, now, 15);
        check(result.ready && result.spike && result.acceleration >= 2, "Observed acceleration versus prior portion");
        check(!LedgerForecast.analyze(stable, policy, now, 15).spike, "Steady usage not spike");
        List<LedgerRecord> five = series("five_hour", now, 600000, 1, 2, 3);
        check(LedgerForecast.analyze(five, five.get(0).policy(), now, 15).ready, "Five-hour shorter minimum span");
        List<LedgerRecord> shortAdditional=Arrays.asList(
                record("additional:main:primary","pro",now-1200000,1,now+HOUR,7200),
                record("additional:main:primary","pro",now-600000,2,now+HOUR,7200),
                record("additional:main:primary","pro",now,3,now+HOUR,7200));
        check(LedgerForecast.analyze(shortAdditional,shortAdditional.get(0).policy(),now,15).ready,"Short additional window uses duration guard, no five-hour assumption");
        result = LedgerForecast.analyze(series("weekly", now, 3 * HOUR, 1, 2, 3), policy, now, 15);
        check(result.ready && !result.risk && result.atReset > 0, "Safe rate remaining prediction");
    }

    private static void exports() throws Exception {
        long midnight = START + 15 * HOUR;
        List<LedgerRecord> rows = Arrays.asList(weekly(midnight - HOUR, 10), weekly(midnight + HOUR, 12));
        LedgerAggregation.Result aggregate = LedgerAggregation.aggregate(rows, Collections.emptyList());
        List<LedgerAggregation.Day> days = new ArrayList<>(aggregate.days.values());
        JSONObject event = new JSONObject().put("type", "=EXAMPLE,\"quoted\"").put("at_utc", LedgerExport.utc(START))
                .put("meter", "weekly").put("origin", "observed").put("access_token", "SENSITIVE_FIXTURE_TOKEN");
        String json = LedgerExport.json(rows, days, aggregate.uncertain, Arrays.asList(event));
        JSONObject decoded = new JSONObject(json);
        check(decoded.getJSONArray("observations").length() == 2, "JSON observations roundtrip");
        check(decoded.getJSONArray("daily").length() == 2, "JSON actual days");
        check(decoded.getJSONArray("uncertain_intervals").length() == 1, "JSON unallocated change");
        check(decoded.getString("unit").equals("quota_percentage_points_not_tokens"), "Export explains unit");
        check(!json.contains("SENSITIVE_FIXTURE_TOKEN") && !json.contains("access_token"), "Sensitive event extras excluded");
        String csv = LedgerExport.csv(rows, days, aggregate.uncertain, Arrays.asList(event));
        check(csv.contains("'="), "CSV formula neutralized");
        check(csv.contains("\"\"quoted\"\""), "CSV quotation escaping");
        String[] lines = csv.split("\r\n");
        check(lines.length == 7, "CSV all record categories");
        for (int i = 1; i < lines.length; i++) {
            // Parse quoting, including commas inside event fields.
            int columns = 1; boolean quoted = false;
            for (int j = 0; j < lines[i].length(); j++) {
                char c = lines[i].charAt(j);
                if (c == '"') {
                    if (quoted && j + 1 < lines[i].length() && lines[i].charAt(j + 1) == '"') j++;
                    else quoted = !quoted;
                } else if (c == ',' && !quoted) columns++;
            }
            check(!quoted && columns == 17, "CSV 17 fields on each row");
        }
    }

    private static void propertyCases() {
        Random random = new Random(8201);
        for (int run = 0; run < 150; run++) {
            List<LedgerRecord> rows = new ArrayList<>();
            double first = random.nextDouble() * 10, used = first;
            for (int i = 0; i < 10; i++) {
                rows.add(weekly(START + i * 600000L, used));
                if (i < 9) used += random.nextDouble();
            }
            Collections.shuffle(rows, random);
            rows.add(rows.get(0));
            LedgerAggregation.Result result = LedgerAggregation.aggregate(rows, Collections.emptyList());
            close(total(result), used - first, "Sorted/deduplicated conservation");
            check(result.days.values().iterator().next().count == 10, "Property observation count");
            check(result.uncertain.isEmpty(), "Property no invented uncertainty");
        }
    }
}
