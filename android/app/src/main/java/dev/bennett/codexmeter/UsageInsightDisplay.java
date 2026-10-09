package dev.bennett.codexmeter;

import android.content.Context;
import android.text.format.DateFormat;
import android.widget.LinearLayout;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/** Strings and small supplemental rows using the existing card and text styling. */
final class UsageInsightDisplay {
    private UsageInsightDisplay() {}

    static String freshness(Context context, UsageSnapshot snapshot) {
        long now = System.currentTimeMillis();
        UsageInsights.Freshness state = UsageInsights.freshness(snapshot.fetchedAtMillis, now,
                normalRefreshMinutes(context, snapshot, now));
        if (state == UsageInsights.Freshness.UNKNOWN)
            return context.getString(R.string.insight_freshness_unknown);
        String age = duration(context, now - snapshot.fetchedAtMillis);
        if (state == UsageInsights.Freshness.STALE)
            return context.getString(R.string.insight_stale, age);
        return now - snapshot.fetchedAtMillis < TimeUnit.MINUTES.toMillis(1)
                ? context.getString(R.string.insight_fresh_now)
                : context.getString(R.string.insight_fresh, age);
    }

    static LinearLayout addWeeklyRows(Context context, LinearLayout card, UsageSnapshot snapshot,
            boolean dark) {
        LinearLayout rows = new LinearLayout(context);
        rows.setOrientation(LinearLayout.VERTICAL);
        rows.setPadding(Ui.dp(context, 20), 0, Ui.dp(context, 20), Ui.dp(context, 14));
        updateWeeklyRows(context, rows, snapshot, dark);
        card.addView(rows, new LinearLayout.LayoutParams(-1, -2));
        return rows;
    }

    static void updateWeeklyRows(Context context, LinearLayout rows, UsageSnapshot snapshot,
            boolean dark) {
        rows.removeAllViews();
        long now = System.currentTimeMillis();
        UsageInsights.Weekly insight = UsageInsights.weekly(snapshot.weekly,
                AppPreferences.loadUsageHistory(context, UsageHistory.WEEKLY),
                snapshot.fetchedAtMillis, now, normalRefreshMinutes(context, snapshot, now));
        if (snapshot.weekly.remainingPercent() == 0) {
            rows.addView(Ui.text(context, context.getString(R.string.insight_exhausted),
                    12, Ui.secondaryText(dark)));
        } else if (!insight.available) {
            rows.addView(Ui.text(context, context.getString(R.string.insight_unavailable),
                    12, Ui.secondaryText(dark)));
        } else {
            rows.addView(Ui.text(context, context.getString(insight.remainingMillis < UsageInsights.DAY
                    ? R.string.insight_budget_until_reset : R.string.insight_budget,
                    decimal(insight.dailyBudget)), 13, Ui.mainText(dark)));
            int status = paceResource(insight.pace);
            rows.addView(Ui.text(context, context.getString(R.string.insight_pace,
                    context.getString(status)), 13, Ui.mainText(dark)));
            if (insight.rateAvailable) {
                rows.addView(Ui.text(context, context.getString(R.string.insight_recent_rate,
                        duration(context, insight.sampleSpanMillis), decimal(insight.recentDailyUsage)),
                        12, Ui.secondaryText(dark)));
                String forecast = insight.exhaustionAtMillis < insight.resetAtMillis
                        ? context.getString(R.string.insight_depletion,
                                timestamp(context, insight.exhaustionAtMillis))
                        : context.getString(R.string.insight_remaining_at_reset,
                                decimal(insight.remainingAtReset));
                rows.addView(Ui.text(context, forecast, 12, Ui.secondaryText(dark)));
            } else {
                rows.addView(Ui.text(context, context.getString(R.string.insight_prediction_unavailable),
                        12, Ui.secondaryText(dark)));
            }
        }
    }

    static void addEventHistory(Context context, LinearLayout content, boolean dark) {
        content.addView(Ui.separator(context, context.getString(R.string.insight_events_title)));
        LinearLayout card = Ui.card(context, dark);
        card.addView(Ui.text(context, context.getString(R.string.insight_events_note),
                12, Ui.secondaryText(dark)));
        List<UsageEventStore.Event> events = UsageEventStore.events(context);
        if (events.isEmpty()) {
            card.addView(Ui.text(context, context.getString(R.string.insight_events_empty),
                    13, Ui.secondaryText(dark)));
        }
        for (UsageEventStore.Event event : events) {
            Ui.addSpacer(card, 8);
            String label;
            if ("credit_used".equals(event.type)) label = context.getString(R.string.insight_event_credit_used);
            else if ("bank_increased".equals(event.type)) label = context.getString(R.string.insight_event_bank_increased);
            else if ("credit_expired".equals(event.type)) label = context.getString(R.string.insight_event_credit_expired);
            else {
                String window = context.getString(UsageHistory.WEEKLY.equals(event.kind)
                        ? R.string.ui_weekly_158f3d : R.string.ui_5_hour_bc4288);
                int id = "reset_observed".equals(event.type) ? R.string.insight_event_reset
                        : "unexpected_reset".equals(event.type) ? R.string.insight_event_unexpected
                        : R.string.insight_event_allowance;
                label = context.getString(id, window);
            }
            card.addView(Ui.text(context, context.getString(R.string.insight_event_row,
                    timestamp(context, event.atMillis), label), 13, Ui.mainText(dark)));
        }
        content.addView(card);
        Ui.addSpacer(content, 20);
    }

    private static int paceResource(UsageInsights.Pace pace) {
        switch (pace) {
            case COMFORTABLE: return R.string.insight_pace_comfortable;
            case ON_TRACK: return R.string.insight_pace_on_track;
            case CAUTION: return R.string.insight_pace_caution;
            case FAST: return R.string.insight_pace_fast;
            default: return R.string.insight_pace_unavailable;
        }
    }

    static int normalRefreshMinutes(Context context, UsageSnapshot snapshot, long now) {
        if (!AppPreferences.getAutomaticRefresh(context)) return AppPreferences.getRefreshMinutes(context);
        return AdaptiveRefreshPolicy.chooseMinutes(snapshot, RefreshEngagement.score(context, now),
                Calendar.getInstance().get(Calendar.HOUR_OF_DAY), 0, now);
    }

    static String decimal(double value) {
        return String.format(Locale.getDefault(), "%.1f", value);
    }

    static String timestamp(Context context, long at) {
        String pattern = DateFormat.getBestDateTimePattern(Locale.getDefault(),
                DateFormat.is24HourFormat(context) ? "MMMdHm" : "MMMdhm");
        return new SimpleDateFormat(pattern, Locale.getDefault()).format(new Date(at));
    }

    static String duration(Context context, long millis) {
        long minutes = Math.max(0L, TimeUnit.MILLISECONDS.toMinutes(millis));
        long hours = minutes / 60L;
        return hours > 0L ? context.getString(R.string.insight_duration_hours, hours, minutes % 60L)
                : context.getString(R.string.insight_duration_minutes, minutes);
    }
}
