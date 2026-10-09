package dev.bennett.codexmeter;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.lang.reflect.Field;
import java.util.Collections;
import org.json.JSONObject;

/** Actual production DAO on a real SQLite connection behind JVM platform fixtures. */
public final class LedgerDatabaseSelfTest {
    private static final long HOUR = 3_600_000L, DAY = 24 * HOUR;
    private static final long NOW = System.currentTimeMillis();
    private static int checks;
    private static void check(boolean value, String message) {
        checks++;
        if (!value) throw new AssertionError(message);
    }
    private static UsageSnapshot snapshot(long at, int used) {
        return new UsageSnapshot("pro", true, false, null,
                new UsageWindow(used, 604800, 0, (NOW + 5 * DAY) / 1000), at);
    }
    private static UsageLedgerDatabase owner() throws Exception {
        Field field = UsageLedgerDatabase.class.getDeclaredField("instance"); field.setAccessible(true);
        return (UsageLedgerDatabase) field.get(null);
    }
    private static void restart() throws Exception {
        owner().close();
        Field field = UsageLedgerDatabase.class.getDeclaredField("instance"); field.setAccessible(true); field.set(null, null);
    }
    private static long scalar(SQLiteDatabase db, String sql) {
        try (Cursor c = db.rawQuery(sql, null)) { return c.moveToFirst() ? c.getLong(0) : 0; }
    }
    private static void seed(SQLiteDatabase db, long at, int used, String meter) {
        db.execSQL("INSERT INTO observations(meter,plan,policy,observed_at,used,used_decimal,reset_at,window_seconds,source,manual) VALUES(?,?,?,?,?,?,?,?,?,?)",
                new Object[] {meter, "pro", meter + "|pro|604800", at, used, Integer.toString(used), at + 5 * DAY, 604800, "api_rounded", 0});
    }

    public static void main(String[] args) throws Exception {
        Context context = new Context();
        UsageHistoryRecorder.record(context, snapshot(NOW - 2 * HOUR, 10));
        String originalPrototype = context.getSharedPreferences("codex_meter_ledger_v1", 0).getString("ledger", "");
        UsageLedgerDatabase.Data data = UsageLedgerDatabase.load(context);
        check(data.records.size() == 1, "Preference history/prototype migrate once, duplicate timestamp excluded");
        check(data.records.get(0).source.equals("legacy"), "Imported confidence unknown");
        check(data.records.get(0).plan.equals("pro"), "Prefer known-plan prototype over unknown-plan chart history");
        check(UsageLedgerDatabase.load(context).records.size() == 1, "Idempotent migration");
        check(originalPrototype.equals(context.getSharedPreferences("codex_meter_ledger_v1", 0).getString("ledger", "")), "Original preference payload preserved");
        check(context.histories.get("weekly").samples.size() == 1, "Original chart history preserved");
        restart();
        check(UsageLedgerDatabase.load(context).records.size() == 1, "SQLite persists through helper restart");
        JSONObject raw = new JSONObject().put("rate_limit", new JSONObject().put("primary_window",
                new JSONObject().put("used_percent", 15.75).put("limit_window_seconds", 604800)
                        .put("reset_at", (NOW + 5 * DAY) / 1000).put("reset_after_seconds", 0)));
        int rounded = UsageWindow.fromJson(raw.getJSONObject("rate_limit").getJSONObject("primary_window")).usedPercent;
        check(UsageLedgerDatabase.record(context, snapshot(NOW - HOUR, rounded), raw.toString(), false), "New real observation saved");
        check(UsageLedgerDatabase.record(context, snapshot(NOW - HOUR, rounded), raw.toString(), true), "Same observation marked manual");
        data = UsageLedgerDatabase.load(context);
        check(data.records.size() == 2, "Manual/automatic dedup per meter timestamp");
        check(data.records.get(1).manual && Math.abs(data.records.get(1).used - 15.75) < 1e-9, "Manual flag and precision retained");
        check(UsageLedgerDatabase.manuallySaved(context, NOW - HOUR), "Manual request confirmation persisted");
        check(!UsageLedgerDatabase.record(context, snapshot(NOW - 3 * HOUR, 99), null, false), "Late response rejected");
        SQLiteDatabase db = owner().getWritableDatabase();
        check(scalar(db, "SELECT COUNT(*) FROM manual_requests") == 1, "Manual requests dedup");
        check(scalar(db, "SELECT COUNT(*) FROM metadata WHERE key='closed_through_seoul'") == 1, "Missed midnight close repaired on read");
        SQLiteDatabase.failNextWrite = true;
        check(!UsageLedgerDatabase.record(context, snapshot(NOW, 20), null, true), "Optional DB failure does not escape refresh observer");
        check(!UsageLedgerDatabase.manuallySaved(context, NOW), "Failed transaction never reports manual success");
        check(UsageLedgerDatabase.load(context).records.size() == 2, "Failed write rolls back data");
        SQLiteDatabase.silentRejectTable = "daily";
        check(!UsageLedgerDatabase.record(context, snapshot(NOW, 20), null, true), "Silent aggregate write rejection reports failure");
        check(!UsageLedgerDatabase.manuallySaved(context, NOW), "Silent rejection rolls back manual success marker");
        check(UsageLedgerDatabase.load(context).records.size() == 2, "Silent daily failure rolls back new raw observation");
        SQLiteDatabase.silentRejectTable = "observations";
        check(!UsageLedgerDatabase.record(context, snapshot(NOW, 20), null, false), "Silent nonduplicate raw rejection reports failure");
        SQLiteDatabase.failCommit = true;
        check(!UsageLedgerDatabase.record(context, snapshot(NOW, 20), null, true), "Commit failure cannot return saved=true");
        check(!UsageLedgerDatabase.manuallySaved(context, NOW), "Commit failure rolls back success marker");
        check(UsageLedgerDatabase.load(context).records.size() == 2, "Commit failure preserves prior data");
        restart(); data = UsageLedgerDatabase.load(context);
        check(data.records.get(1).manual, "Manual provenance survives restart");
        check(data.days.stream().mapToInt(day -> day.count).sum() == 2, "Repeated read does not duplicate daily aggregation");

        // Explicit user history deletion creates a tombstone even if old fixture preferences remain.
        UsageLedgerDatabase.clear(context);
        check(UsageLedgerDatabase.load(context).records.isEmpty(), "Clear does not resurrect old preferences");
        check(UsageLedgerDatabase.load(context).events.isEmpty(), "Clear removes event export data");
        check(!UsageLedgerDatabase.manuallySaved(context, NOW - HOUR), "Clear removes manual requests");
        check(LedgerMaintenanceScheduler.cancels > 0, "Clear cancels midnight job");
        UsageEventStore.recordConfirmedCreditUse(context, NOW - DAY);
        check(UsageLedgerDatabase.load(context).events.isEmpty(), "Deleted credit events cannot reimport from surviving preferences");
        int schedulesAfterClear = LedgerMaintenanceScheduler.schedules;
        UsageLedgerDatabase.scheduleMaintenance(context);
        check(LedgerMaintenanceScheduler.schedules == schedulesAfterClear, "Empty cleared ledger does not reschedule midnight job");

        check(!context.histories.isEmpty(), "DAO clear itself does not delete original preference histories");

        UsageEventStore.clear(context);
        long creditAt = System.currentTimeMillis() + 1;
        UsageEventStore.recordConfirmedCreditUse(context, creditAt);
        check(UsageLedgerDatabase.record(context, snapshot(NOW - 2 * HOUR, 50), null, false), "Credit scenario baseline");
        check(UsageLedgerDatabase.record(context, snapshot(creditAt + 1000, 20), null, false), "Credit-related gain saved");
        data = UsageLedgerDatabase.load(context);
        check(data.events.stream().anyMatch(event -> event.type.equals("credit_used") && event.origin.equals("confirmed")), "Confirmed operation imported independently");
        check(data.events.stream().anyMatch(event -> event.type.equals("CREDIT_CHANGE") && event.origin.equals("observed")), "Nearby gain classification preserves observational scope");
        check(data.days.stream().allMatch(day -> day.points == 0), "Credit gain is not negative consumption");
        UsageEventStore.clear(context);
        SQLiteDatabase.failNextWrite = true;
        UsageLedgerDatabase.clear(context);
        check(context.getSharedPreferences("codex_next_ledger_state",0).getString("clear_pending","").equals("1"), "Failed clear keeps persistent pending marker");
        restart();
        check(UsageLedgerDatabase.load(context).records.isEmpty(), "Pending clear retried before reopening prior account data");
        check(context.getSharedPreferences("codex_next_ledger_state",0).getString("clear_pending","").isEmpty(), "Pending marker removed only after clear commits");
        UsageLedgerDatabase.clear(context);

        db = owner().getWritableDatabase();
        seed(db, NOW - 110 * DAY, 1, "weekly"); seed(db, NOW - 100 * DAY, 2, "weekly");
        seed(db, NOW - 95 * DAY, 3, "weekly"); seed(db, NOW - 80 * DAY, 4, "weekly");
        seed(db, NOW - 110 * DAY, 1, "monthly"); seed(db, NOW - 94 * DAY, 2, "monthly");
        data = UsageLedgerDatabase.load(context);
        check(data.records.size() == 3, "90-day retention preserves one boundary anchor per meter");
        check(data.records.stream().filter(row -> row.meter.equals("weekly")).count() == 2, "Independent retention per meter");
        check(data.days.size() == 6, "Daily summaries sealed before raw pruning");
        int sealedCount = data.days.size();
        check(UsageLedgerDatabase.load(context).days.size() == sealedCount, "Pruned raw records do not erase older sealed days");
        String veryOld = LedgerAggregation.day(NOW).minusDays(751).toString();
        String retained = LedgerAggregation.day(NOW).minusDays(749).toString();
        for (String day : new String[] {veryOld, retained}) db.execSQL(
                "INSERT INTO daily(policy,day,points,observations,first_at,last_at,covered_ms,uncertain_count,boundaries,legacy,quality) VALUES(?,?,?,?,?,?,?,?,?,?,?)",
                new Object[] {"weekly|pro|604800", day, 7, 2, 1, 2, 1, 0, 0, 0, "partial"});
        data = UsageLedgerDatabase.load(context);
        check(data.days.stream().noneMatch(day -> day.date.equals(veryOld)), "Daily retention removes beyond 750 days");
        check(data.days.stream().anyMatch(day -> day.date.equals(retained) && day.points == 7), "More than two years of daily data retained");
        check(UsageLedgerDatabase.load(context).records.size() == 3, "Offline recovery produces no fake midnight samples");
        restart();
        check(UsageLedgerDatabase.load(context).days.stream().anyMatch(day -> day.date.equals(retained)), "Long-term days persist on reopen");

        // A brand new database has no migration-complete marker if any migration write fails.
        owner().close();
        Field field = UsageLedgerDatabase.class.getDeclaredField("instance"); field.setAccessible(true); field.set(null, null);
        System.setProperty("ledger.dbdir", System.getProperty("ledger.dbdir") + java.io.File.separator + "migration-failure");
        new java.io.File(System.getProperty("ledger.dbdir")).mkdirs();
        Context importing = new Context();
        UsageHistoryRecorder.record(importing, snapshot(NOW - HOUR, 10));
        UsageLedgerDatabase.load(new Context()); // creates schema without imported records
        db = owner().getWritableDatabase(); db.delete("metadata", null, null);
        field.set(null, null); // reopen with importing context
        db.close();
        // Schema already exists, so the injection occurs inside the production migration transaction.
        SQLiteDatabase.failNextWrite = true;
        boolean failed = false;
        try { UsageLedgerDatabase.load(importing); } catch (Exception expected) { failed = true; }
        check(failed, "Migration failure reported without erasing preference source");
        db = owner().getReadableDatabase();
        check(scalar(db, "SELECT COUNT(*) FROM metadata WHERE key='legacy_imported'") == 0, "Failed migration marker rolled back");
        check(UsageLedgerDatabase.load(importing).records.size() == 1, "Migration retry restores record exactly once");
        check(importing.histories.get("weekly").samples.size() == 1, "Migration failure preserves original records");
        owner().close();
        field.set(null, null);
        System.setProperty("ledger.dbdir", System.getProperty("ledger.dbdir") + java.io.File.separator + "first-api");
        new java.io.File(System.getProperty("ledger.dbdir")).mkdirs();
        Context firstApi = new Context();
        UsageHistoryRecorder.record(firstApi, snapshot(NOW - 2 * HOUR, 10));
        UsageHistoryRecorder.record(firstApi, snapshot(NOW - HOUR, rounded));
        check(UsageLedgerDatabase.record(firstApi, snapshot(NOW - HOUR, rounded), raw.toString(), true), "First API record and migration share transaction");
        data = UsageLedgerDatabase.load(firstApi);
        check(data.records.size() == 2, "First response not duplicated by old recorder migration");
        check(data.records.get(0).source.equals("legacy") && data.records.get(1).source.equals("api_precise"), "Current API response is not downgraded to legacy on first migration");
        check(Math.abs(data.records.get(1).used - 15.75) < 1e-9, "First API preserves precision despite prior preference recording");
        check(firstApi.histories.get("weekly").samples.size() == 2, "Concurrent migration path preserves all original preferences");

        java.util.concurrent.ExecutorService concurrent = java.util.concurrent.Executors.newFixedThreadPool(6);
        java.util.List<java.util.concurrent.Future<Boolean>> writes = new java.util.ArrayList<>();
        for (int i=0; i<12; i++) writes.add(concurrent.submit(() ->
                UsageLedgerDatabase.record(firstApi, snapshot(NOW, 20), null, true)));
        for (java.util.concurrent.Future<Boolean> write : writes) check(write.get(), "Concurrent duplicate accepted safely");
        concurrent.shutdown();
        check(UsageLedgerDatabase.load(firstApi).records.size() == 3, "Concurrent manual/automatic callbacks store one observation");
        restart();
        check(UsageLedgerDatabase.load(firstApi).records.size() == 3, "Concurrent committed data persists across reopening");
        owner().close();
        System.out.println("Production ledger DAO on SQLite JVM fixtures: " + checks + " assertions passed (not Android device proof).");
    }
}
