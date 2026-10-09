package dev.bennett.codexmeter;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Local SQLite ledger. Original preferences remain the compatibility source for old surfaces. */
public final class UsageLedgerDatabase extends SQLiteOpenHelper {
    public static final String NAME = "codex_usage_ledger.db";
    public static final int VERSION = 1;
    public static final int RAW_DAYS = 90;
    public static final int DAILY_DAYS = 750;
    static final String CREATE_OBSERVATIONS = "CREATE TABLE observations (id INTEGER PRIMARY KEY, meter TEXT NOT NULL, plan TEXT NOT NULL, policy TEXT NOT NULL, observed_at INTEGER NOT NULL CHECK(observed_at>0), used REAL NOT NULL CHECK(used>=0 AND used<=100), used_decimal TEXT NOT NULL, reset_at INTEGER NOT NULL CHECK(reset_at>=0), window_seconds INTEGER NOT NULL CHECK(window_seconds>0), source TEXT NOT NULL, manual INTEGER NOT NULL DEFAULT 0 CHECK(manual IN (0,1)), UNIQUE(meter,observed_at))";
    static final String CREATE_DAYS = "CREATE TABLE daily (policy TEXT NOT NULL, day TEXT NOT NULL, points REAL NOT NULL CHECK(points>=0), observations INTEGER NOT NULL, first_at INTEGER NOT NULL, last_at INTEGER NOT NULL, covered_ms INTEGER NOT NULL, uncertain_count INTEGER NOT NULL, boundaries INTEGER NOT NULL, legacy INTEGER NOT NULL, quality TEXT NOT NULL, PRIMARY KEY(policy,day))";
    static final String CREATE_INTERVALS = "CREATE TABLE uncertain_intervals (policy TEXT NOT NULL, start_at INTEGER NOT NULL, end_at INTEGER NOT NULL, points REAL NOT NULL CHECK(points>=0), reason TEXT NOT NULL, PRIMARY KEY(policy,start_at,end_at))";
    static final String CREATE_EVENTS = "CREATE TABLE events (event_key TEXT PRIMARY KEY, type TEXT NOT NULL, at INTEGER NOT NULL, meter TEXT NOT NULL, origin TEXT NOT NULL)";
    static final String CREATE_REQUESTS = "CREATE TABLE manual_requests (observed_at INTEGER PRIMARY KEY, requested_at INTEGER NOT NULL)";
    static final String CREATE_META = "CREATE TABLE metadata (key TEXT PRIMARY KEY, value TEXT NOT NULL)";
    static final String CREATE_INDEX = "CREATE INDEX observations_time ON observations(observed_at)";
    static final String PRUNE_RAW = "DELETE FROM observations WHERE observed_at < ? AND id NOT IN (SELECT id FROM observations o WHERE observed_at=(SELECT MAX(observed_at) FROM observations p WHERE p.meter=o.meter AND p.observed_at < ?))";
    private static final Object LOCK = new Object();
    private static UsageLedgerDatabase instance;
    private static boolean clearPending;
    private final Context app;

    private UsageLedgerDatabase(Context context) {
        super(context, NAME, null, VERSION); app = context.getApplicationContext();
    }
    private static UsageLedgerDatabase helper(Context context) {
        if (instance == null) instance = new UsageLedgerDatabase(context.getApplicationContext());
        return instance;
    }
    @Override public void onCreate(SQLiteDatabase db) {
        for (String sql : new String[] {CREATE_OBSERVATIONS, CREATE_DAYS, CREATE_INTERVALS,
                CREATE_EVENTS, CREATE_REQUESTS, CREATE_META, CREATE_INDEX}) db.execSQL(sql);
    }
    @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        throw new IllegalStateException("Unsupported ledger version; existing database preserved");
    }

    /** This optional observer must never throw into the successful usage API pipeline. */
    static boolean record(Context context, UsageSnapshot snapshot, String body, boolean manual) {
        boolean saved = false;
        synchronized (LOCK) {
            try {
                UsageLedgerDatabase owner = helper(context);
                SQLiteDatabase db = owner.getWritableDatabase();
                owner.finishPendingClear(db);
                db.beginTransaction();
                try {
                    owner.migrate(db, snapshot.fetchedAtMillis);
                    List<LedgerRecord> capture = LedgerCapture.fromSnapshot(snapshot, body, manual);
                    int accepted = 0;
                    for (LedgerRecord row : capture) {
                        long latest = scalar(db, "SELECT COALESCE(MAX(observed_at),0) FROM observations WHERE meter=?", row.meter);
                        if (row.at < latest) continue;
                        insert(db, row);
                        if (manual) {
                            ContentValues flag = new ContentValues(); flag.put("manual", 1);
                            db.update("observations", flag, "meter=? AND observed_at=?", new String[] {row.meter, Long.toString(row.at)});
                        }
                        accepted++;
                    }
                    owner.importCredits(db);
                    if (manual && accepted > 0) {
                        ContentValues request = new ContentValues(); request.put("observed_at", snapshot.fetchedAtMillis);
                        request.put("requested_at", System.currentTimeMillis());
                        put(db,"manual_requests",request,SQLiteDatabase.CONFLICT_IGNORE,"observed_at");
                    }
                    owner.rollup(db, System.currentTimeMillis());
                    db.setTransactionSuccessful(); saved = accepted > 0;
                } finally { db.endTransaction(); }
            } catch (Exception failure) {
                saved = false;
                DiagnosticLog.warn(context, "history", "sqlite_ledger_record_failed");
            }
            if (saved) LedgerMaintenanceScheduler.schedule(context);
        }
        return saved;
    }

    static void scheduleMaintenance(Context context) {
        synchronized (LOCK) {
            try {
                UsageLedgerDatabase owner = helper(context);
                SQLiteDatabase db = owner.getWritableDatabase();
                owner.finishPendingClear(db);
                if (scalar(db, "SELECT COUNT(*) FROM observations WHERE observed_at>?", "0") > 0)
                    LedgerMaintenanceScheduler.schedule(context);
            } catch (Exception ignored) { DiagnosticLog.warn(context,"history","ledger_schedule_failed"); }
        }
    }

    public static Data load(Context context) throws Exception {
        synchronized (LOCK) {
            UsageLedgerDatabase owner = helper(context);
            SQLiteDatabase db = owner.getWritableDatabase();
            owner.finishPendingClear(db);
            db.beginTransaction();
            try {
                owner.migrate(db); owner.importCredits(db); owner.rollup(db, System.currentTimeMillis());
                Data result = readData(db);
                db.setTransactionSuccessful(); return result;
            } finally { db.endTransaction(); }
        }
    }

    static boolean manuallySaved(Context context, long at) {
        synchronized (LOCK) {
            try {
                UsageLedgerDatabase owner = helper(context);
                SQLiteDatabase db = owner.getWritableDatabase();
                owner.finishPendingClear(db);
                return scalar(db,"SELECT COUNT(*) FROM manual_requests WHERE observed_at=?", Long.toString(at)) > 0;
            }
            catch (Exception ignored) { return false; }
        }
    }

    /** Existing explicit clear-history/sign-out policy: clear all ledger data, retain migration tombstone. */
    static void clear(Context context) {
        synchronized (LOCK) {
            clearPending = true;
            try {
                if (!context.getSharedPreferences("codex_next_ledger_state", Context.MODE_PRIVATE)
                        .edit().putString("clear_pending", "1").commit())
                    throw new IllegalStateException("Cannot persist pending clear");
                UsageLedgerDatabase owner = helper(context);
                owner.finishPendingClear(owner.getWritableDatabase());
            } catch (Exception ignored) { DiagnosticLog.warn(context, "history", "sqlite_ledger_clear_failed"); }
            LedgerMaintenanceScheduler.cancel(context);
        }
    }

    /** Retry a failed explicit clear before admitting another account's data or exposing old rows. */
    private void finishPendingClear(SQLiteDatabase db) {
        android.content.SharedPreferences state = app.getSharedPreferences("codex_next_ledger_state", Context.MODE_PRIVATE);
        if (!clearPending && !"1".equals(state.getString("clear_pending", ""))) return;
        db.beginTransaction();
        try {
            for (String table : new String[] {"observations", "daily", "uncertain_intervals", "events", "manual_requests", "metadata"})
                db.delete(table, null, null);
            meta(db, "legacy_imported", "cleared");
            meta(db, "credits_cleared_through", Long.toString(System.currentTimeMillis()));
            db.setTransactionSuccessful();
        } finally { db.endTransaction(); }
        if (!state.edit().remove("clear_pending").commit()) throw new IllegalStateException("Cannot finish pending clear");
        clearPending = false;
    }

    private void migrate(SQLiteDatabase db) {
        migrate(db, 0L);
    }
    private void migrate(SQLiteDatabase db, long currentObservation) {
        if (scalar(db, "SELECT COUNT(*) FROM metadata WHERE key=?", "legacy_imported") > 0) return;
        UsageLedger prototype = UsageLedgerStore.load(app);
        if (prototype != null) {
            for (UsageLedger.Observation row : prototype.observations) {
                // The old recorder has already seen this API response. Capture it below at full precision.
                if (row.atMillis == currentObservation) continue;
                insert(db, new LedgerRecord(row.kind, row.plan, row.atMillis, row.usedPercent,
                        Integer.toString(row.usedPercent), row.resetAtMillis, row.windowSeconds, "legacy", false));
            }
        }
        for (String kind : new String[] {UsageHistory.FIVE_HOUR, UsageHistory.WEEKLY, UsageHistory.MONTHLY}) {
            for (UsageSample sample : AppPreferences.loadUsageHistory(app, kind).samples) {
                if (sample.observedAtMillis == currentObservation) continue;
                try {
                    insert(db, new LedgerRecord(kind, "unknown", sample.observedAtMillis, sample.usedPercent,
                            Integer.toString(sample.usedPercent), sample.resetAtMillis, sample.windowSeconds, "legacy", false));
                } catch (IllegalArgumentException ignored) { }
            }
        }
        meta(db, "legacy_imported", prototype == null ? "history_imported_prototype_unreadable" : "complete");
    }

    private void importCredits(SQLiteDatabase db) {
        long clearedThrough = 0;
        try (Cursor c = db.rawQuery("SELECT value FROM metadata WHERE key='credits_cleared_through'", null)) {
            if (c.moveToFirst()) clearedThrough = Long.parseLong(c.getString(0));
        }
        for (UsageEventStore.Event event : UsageEventStore.events(app)) {
            if (event.atMillis <= clearedThrough) continue;
            if (!"credit_used".equals(event.type) && !"credit_expired".equals(event.type)
                    && !"bank_increased".equals(event.type)) continue;
            ContentValues value = new ContentValues(); value.put("event_key", event.type + ":" + event.atMillis);
            value.put("type", event.type); value.put("at", event.atMillis); value.put("meter", ""); value.put("origin", "confirmed");
            put(db,"events",value,SQLiteDatabase.CONFLICT_IGNORE,"event_key");
        }
    }

    private static void insert(SQLiteDatabase db, LedgerRecord row) {
        ContentValues value = new ContentValues(); value.put("meter", row.meter); value.put("plan", row.plan);
        value.put("policy", row.policy()); value.put("observed_at", row.at); value.put("used", row.used);
        value.put("used_decimal", row.decimal); value.put("reset_at", row.reset); value.put("window_seconds", row.seconds);
        value.put("source", row.source); value.put("manual", row.manual ? 1 : 0);
        put(db,"observations",value,SQLiteDatabase.CONFLICT_IGNORE,"meter","observed_at");
    }

    private void rollup(SQLiteDatabase db, long now) {
        List<LedgerRecord> rows = observations(db);
        List<Long> credits = new ArrayList<>();
        try (Cursor c = db.rawQuery("SELECT at FROM events WHERE type='credit_used' ORDER BY at", null)) {
            while (c.moveToNext()) credits.add(c.getLong(0));
        }
        LedgerAggregation.Result aggregate = LedgerAggregation.aggregate(rows, credits);
        String rawDay = LedgerAggregation.day(now).minusDays(RAW_DAYS).toString();
        long rawCutoff = LocalDate.parse(rawDay).atStartOfDay(LedgerAggregation.ZONE).toInstant().toEpochMilli();
        // Preserve sealed older daily rows; only the raw-supported horizon is recomputed.
        db.delete("daily", "day>=?", new String[] {rawDay});
        for (LedgerAggregation.Day day : aggregate.days.values()) {
            ContentValues v = new ContentValues(); v.put("policy", day.policy); v.put("day", day.date);
            v.put("points", day.points); v.put("observations", day.count); v.put("first_at", day.first);
            v.put("last_at", day.last); v.put("covered_ms", day.coveredMillis); v.put("uncertain_count", day.uncertain);
            v.put("boundaries", day.boundaries); v.put("legacy", day.legacy ? 1 : 0); v.put("quality", day.quality());
            put(db,"daily",v,day.date.compareTo(rawDay) >= 0
                    ? SQLiteDatabase.CONFLICT_REPLACE : SQLiteDatabase.CONFLICT_IGNORE,"policy","day");
        }
        db.delete("uncertain_intervals", "end_at>=?", new String[] {Long.toString(rawCutoff)});
        for (LedgerAggregation.Interval interval : aggregate.uncertain) {
            ContentValues v = new ContentValues(); v.put("policy", interval.policy); v.put("start_at", interval.start);
            v.put("end_at", interval.end); v.put("points", interval.points); v.put("reason", interval.reason);
            put(db,"uncertain_intervals",v,SQLiteDatabase.CONFLICT_IGNORE,"policy","start_at","end_at");
        }
        for (LedgerAggregation.Event event : aggregate.events) {
            if (event.change == LedgerAggregation.Change.NORMAL || event.change == LedgerAggregation.Change.BASELINE) continue;
            ContentValues v = new ContentValues(); v.put("event_key", "observation:" + event.record.meter + ":" + event.record.at);
            v.put("type", event.change.name()); v.put("at", event.record.at); v.put("meter", event.record.meter); v.put("origin", "observed");
            put(db,"events",v,SQLiteDatabase.CONFLICT_REPLACE,"event_key");
        }
        db.execSQL(PRUNE_RAW, new Object[] {rawCutoff,rawCutoff});
        String dailyCutoff = LedgerAggregation.day(now).minusDays(DAILY_DAYS).toString();
        long longCutoff = LocalDate.parse(dailyCutoff).atStartOfDay(LedgerAggregation.ZONE).toInstant().toEpochMilli();
        db.delete("daily", "day<?", new String[] {dailyCutoff});
        db.delete("events", "at<?", new String[] {Long.toString(longCutoff)});
        db.delete("uncertain_intervals", "end_at<?", new String[] {Long.toString(longCutoff)});
        db.delete("manual_requests", "observed_at<?", new String[] {Long.toString(longCutoff)});
        meta(db, "closed_through_seoul", LedgerAggregation.day(now).minusDays(1).toString());
    }

    private static List<LedgerRecord> observations(SQLiteDatabase db) {
        List<LedgerRecord> rows = new ArrayList<>();
        try (Cursor c = db.rawQuery("SELECT meter,plan,observed_at,used,used_decimal,reset_at,window_seconds,source,manual FROM observations ORDER BY meter,observed_at", null)) {
            while (c.moveToNext()) rows.add(new LedgerRecord(c.getString(0),c.getString(1),c.getLong(2),c.getDouble(3),
                    c.getString(4),c.getLong(5),c.getLong(6),c.getString(7),c.getInt(8)==1));
        }
        return rows;
    }
    private static Data readData(SQLiteDatabase db) {
        Data data = new Data(); data.records.addAll(observations(db));
        try (Cursor c = db.rawQuery("SELECT policy,day,points,observations,first_at,last_at,covered_ms,uncertain_count,boundaries,legacy FROM daily ORDER BY day,policy", null)) {
            while (c.moveToNext()) {
                LedgerAggregation.Day day = new LedgerAggregation.Day(c.getString(0),c.getString(1));
                day.points=c.getDouble(2); day.count=c.getInt(3); day.first=c.getLong(4); day.last=c.getLong(5);
                day.coveredMillis=c.getLong(6); day.uncertain=c.getInt(7); day.boundaries=c.getInt(8); day.legacy=c.getInt(9)==1;
                data.days.add(day);
            }
        }
        try (Cursor c = db.rawQuery("SELECT policy,start_at,end_at,points,reason FROM uncertain_intervals ORDER BY end_at", null)) {
            while(c.moveToNext()) data.uncertain.add(new LedgerAggregation.Interval(c.getString(0),c.getLong(1),c.getLong(2),c.getDouble(3),c.getString(4)));
        }
        try (Cursor c = db.rawQuery("SELECT type,at,meter,origin FROM events ORDER BY at", null)) {
            while(c.moveToNext()) data.events.add(new Event(c.getString(0),c.getLong(1),c.getString(2),c.getString(3)));
        }
        return data;
    }
    private static long scalar(SQLiteDatabase db, String query, String argument) {
        try (Cursor c=db.rawQuery(query,new String[]{argument})) { return c.moveToFirst()?c.getLong(0):0L; }
    }
    private static void meta(SQLiteDatabase db, String key, String value) {
        ContentValues v = new ContentValues(); v.put("key",key); v.put("value",value);
        put(db,"metadata",v,SQLiteDatabase.CONFLICT_REPLACE,"key");
    }
    /** Android insertWithOnConflict can return -1 for write errors. Only an existing IGNORE key is benign. */
    private static void put(SQLiteDatabase db,String table,ContentValues values,int conflict,String... keys) {
        if(db.insertWithOnConflict(table,null,values,conflict)!=-1L)return;
        if(conflict==SQLiteDatabase.CONFLICT_IGNORE) {
            StringBuilder where=new StringBuilder();String[] arguments=new String[keys.length];
            for(int i=0;i<keys.length;i++) {
                if(i>0)where.append(" AND ");where.append(keys[i]).append("=?");
                arguments[i]=String.valueOf(values.get(keys[i]));
            }
            try(Cursor existing=db.rawQuery("SELECT 1 FROM "+table+" WHERE "+where,arguments)) {
                if(existing.moveToFirst())return;
            }
        }
        throw new IllegalStateException("Ledger write rejected");
    }
    public static final class Data {
        public final List<LedgerRecord> records = new ArrayList<>();
        public final List<LedgerAggregation.Day> days = new ArrayList<>();
        public final List<LedgerAggregation.Interval> uncertain = new ArrayList<>();
        public final List<Event> events = new ArrayList<>();
    }
    public static final class Event {
        public final String type,meter,origin; public final long at;
        Event(String type,long at,String meter,String origin){this.type=type;this.at=at;this.meter=meter;this.origin=origin;}
    }
}
