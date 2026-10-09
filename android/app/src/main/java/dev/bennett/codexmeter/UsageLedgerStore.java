package dev.bennett.codexmeter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONObject;

/** Optional observer: failures never invalidate a successfully cached usage response. */
final class UsageLedgerStore {
    private static final String PREFS = "codex_meter_ledger_v1";
    private static final String KEY = "ledger";
    private static final Object LOCK = new Object();

    private UsageLedgerStore() {}

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    // null means unreadable. Never replace damaged or newer-format data with an empty ledger.
    private static UsageLedger read(Context context) {
        String raw = prefs(context).getString(KEY, null);
        if (raw == null) return UsageLedger.empty();
        try { return UsageLedger.fromJson(new JSONObject(raw)); }
        catch (Exception ignored) { return null; }
    }

    static UsageLedger load(Context context) {
        synchronized (LOCK) {
            try { return read(context); }
            catch (Exception ignored) { return null; }
        }
    }

    @SuppressLint("ApplySharedPref") // Runs on the refresh worker; detect and isolate disk write failure.
    static void record(Context context, UsageSnapshot snapshot) {
        if (context == null || snapshot == null || snapshot.fetchedAtMillis <= 0L) return;
        synchronized (LOCK) {
            try {
                UsageLedger current = read(context);
                if (current == null) {
                    DiagnosticLog.warn(context, "history", "ledger_unreadable_preserved");
                    return;
                }
                long at = snapshot.fetchedAtMillis;
                UsageLedger next = current.append(UsageHistory.FIVE_HOUR, snapshot.planType, snapshot.fiveHour, at)
                        .append(UsageHistory.WEEKLY, snapshot.planType, snapshot.weekly, at)
                        .append(UsageHistory.MONTHLY, snapshot.planType, snapshot.monthly, at);
                if (next != current && !prefs(context).edit().putString(KEY, next.toJson().toString()).commit())
                    DiagnosticLog.warn(context, "history", "ledger_save_failed");
            } catch (Exception ignored) {
                DiagnosticLog.warn(context, "history", "ledger_record_failed");
            }
        }
    }

    static boolean hasObservations(Context context) {
        UsageLedger ledger = load(context);
        return ledger == null || !ledger.observations.isEmpty();
    }

    /** Called only by the existing explicit clear-history and account-reset flows. */
    static void clear(Context context) {
        synchronized (LOCK) { prefs(context).edit().remove(KEY).apply(); }
    }
}
