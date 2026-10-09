package dev.bennett.codexmeter;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

/** Small separate observation log. Existing snapshot and history JSON remain unchanged. */
final class UsageEventStore {
    private static final String PREFS = "codex_meter_events_v1";
    private static final int MAX_EVENTS = 100;
    private static final Object LOCK = new Object();

    private UsageEventStore() {}

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    static void recordUsage(Context context, UsageSnapshot snapshot) {
        synchronized (LOCK) {
            try {
                JSONArray events = read(context);
                recordWindow(context, events, UsageHistory.FIVE_HOUR, snapshot.fiveHour,
                        snapshot.fetchedAtMillis);
                recordWindow(context, events, UsageHistory.WEEKLY, snapshot.weekly,
                        snapshot.fetchedAtMillis);
                save(context, events);
            } catch (Exception exception) {
                DiagnosticLog.warn(context, "history", "event_record_failed");
            }
        }
    }

    private static void recordWindow(Context context, JSONArray events, String kind,
            UsageWindow window, long at) throws Exception {
        List<UsageSample> samples = AppPreferences.loadUsageHistory(context, kind).samples;
        UsageSample previous = samples.isEmpty() ? null : samples.get(samples.size() - 1);
        String type = UsageEventDetector.detect(previous, window, at);
        if ("unexpected_reset".equals(type) || "allowance_increase".equals(type)) {
            for (int i = 0; i < events.length(); i++) {
                JSONObject event = events.optJSONObject(i);
                if (event != null && "credit_used".equals(event.optString("type"))
                        && at >= event.optLong("at")
                        && at - event.optLong("at") <= 300_000L) return;
            }
        }
        if (!type.isEmpty()) add(events, type, kind, at,
                kind + ":" + type + ":" + at);
    }

    static void recordConfirmedCreditUse(Context context, long at) {
        synchronized (LOCK) {
            try {
                JSONArray events = read(context);
                add(events, "credit_used", "", at, "credit_used:" + at);
                save(context, events);
            } catch (Exception exception) {
                DiagnosticLog.warn(context, "history", "credit_event_record_failed");
            }
        }
    }

    static void recordCredits(Context context, ResetCreditsSnapshot current) {
        synchronized (LOCK) {
            try {
                SharedPreferences preferences = prefs(context);
                String raw = preferences.getString("credit_observation", "");
                ResetCreditsSnapshot previous = raw.isEmpty() ? null
                        : ResetCreditsSnapshot.fromJson(new JSONObject(raw));
                if (previous != null && current.fetchedAtMillis <= previous.fetchedAtMillis) return;
                JSONArray events = read(context);
                if (previous != null) {
                    if (current.availableCount > previous.availableCount) {
                        add(events, "bank_increased", "", current.fetchedAtMillis,
                                "bank_increased:" + current.fetchedAtMillis);
                    }
                    for (RateLimitResetCredit old : previous.credits) {
                        if (!old.isAvailable() || old.id.isEmpty()) continue;
                        RateLimitResetCredit updated = null;
                        for (RateLimitResetCredit credit : current.credits) {
                            if (old.id.equals(credit.id)) updated = credit;
                        }
                        if (updated != null && "expired".equalsIgnoreCase(updated.status)) {
                            add(events, "credit_expired", "", current.fetchedAtMillis,
                                    "credit_expired:" + old.id);
                        }
                    }
                }
                JSONArray creditStates = new JSONArray();
                for (RateLimitResetCredit credit : current.credits) {
                    creditStates.put(new JSONObject().put("id", credit.id).put("status", credit.status));
                }
                JSONObject observation = new JSONObject().put("available_count", current.availableCount)
                        .put("fetched_at", current.fetchedAtMillis).put("credits", creditStates);
                preferences.edit().putString("credit_observation", observation.toString())
                        .putString("events", bounded(events).toString()).apply();
            } catch (Exception exception) {
                DiagnosticLog.warn(context, "history", "credit_observation_failed");
            }
        }
    }

    static List<Event> events(Context context) {
        synchronized (LOCK) {
            List<Event> result = new ArrayList<>();
            JSONArray stored = read(context);
            for (int i = 0; i < stored.length(); i++) {
                JSONObject object = stored.optJSONObject(i);
                if (object == null || object.optLong("at") <= 0L) continue;
                result.add(new Event(object.optString("type"), object.optString("kind"),
                        object.optLong("at")));
            }
            result.sort((a, b) -> Long.compare(b.atMillis, a.atMillis));
            return Collections.unmodifiableList(result);
        }
    }

    static boolean hasEvents(Context context) {
        return !events(context).isEmpty();
    }

    static void clear(Context context) {
        synchronized (LOCK) {
            prefs(context).edit().remove("events").remove("credit_observation").apply();
        }
    }

    private static JSONArray read(Context context) {
        try {
            return new JSONArray(prefs(context).getString("events", "[]"));
        } catch (Exception ignored) {
            return new JSONArray();
        }
    }

    private static void add(JSONArray events, String type, String kind, long at, String key)
            throws Exception {
        if (at <= 0L) return;
        for (int i = 0; i < events.length(); i++) {
            JSONObject existing = events.optJSONObject(i);
            if (existing != null && key.equals(existing.optString("key"))) return;
        }
        events.put(new JSONObject().put("type", type).put("kind", kind).put("at", at).put("key", key));
    }

    private static JSONArray bounded(JSONArray events) {
        JSONArray result = new JSONArray();
        for (int i = Math.max(0, events.length() - MAX_EVENTS); i < events.length(); i++) {
            JSONObject object = events.optJSONObject(i);
            if (object != null) result.put(object);
        }
        return result;
    }

    private static void save(Context context, JSONArray events) {
        prefs(context).edit().putString("events", bounded(events).toString()).apply();
    }

    static final class Event {
        final String type;
        final String kind;
        final long atMillis;

        Event(String type, String kind, long atMillis) {
            this.type = type;
            this.kind = kind;
            this.atMillis = atMillis;
        }
    }
}
