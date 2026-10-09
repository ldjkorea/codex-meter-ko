package dev.bennett.codexmeter;

import android.content.Context;

/** Android resource labels for the unchanged shared meter/history models. */
final class DisplayLabels {
    private DisplayLabels() {}

    static String meter(Context context, String key, UsageSnapshot snapshot, boolean compact) {
        switch (key == null ? "" : key) {
            case WidgetMeters.FIVE_HOUR: return compact ? context.getString(R.string.ui_5h_798876) : context.getString(R.string.ui_codex_5_hours_76704a);
            case WidgetMeters.NEXT_RESET: return compact ? context.getString(R.string.ui_reset_44c57a) : context.getString(R.string.ui_next_reset_d8c7dd);
            case WidgetMeters.RESET_CREDITS: return compact ? context.getString(R.string.ui_credits_bfac50) : context.getString(R.string.ui_reset_credits_ef7c08);
            case WidgetMeters.WEEKLY:
                return WidgetMeters.weeklyMeterIsMonthly(snapshot)
                        ? (compact ? context.getString(R.string.ui_mo_91e885) : context.getString(R.string.ui_codex_monthly_f5270e))
                        : (compact ? context.getString(R.string.ui_wk_0acc1b) : context.getString(R.string.ui_codex_weekly_8de3b2));
            default:
                String label = compact ? WidgetMeters.shortLabel(key, snapshot)
                        : WidgetMeters.configLabel(key, snapshot);
                if (label.endsWith(" \u00b7 5 hours")) return context.getString(R.string.ui_1_s_5_hours_0f04c8, label.substring(0, label.length() - 10));
                if (label.endsWith(" \u00b7 Weekly")) return context.getString(R.string.ui_1_s_weekly_5ee323, label.substring(0, label.length() - 9));
                if (label.endsWith(" 5h")) return context.getString(R.string.ui_1_s_5h_861026, label.substring(0, label.length() - 3));
                if (label.endsWith(" W")) return context.getString(R.string.ui_1_s_w_379121, label.substring(0, label.length() - 2));
                return label;
        }
    }

    static String history(Context context, String key) {
        switch (key == null ? "" : key) {
            case HistorySections.GUIDE: return context.getString(R.string.ui_how_to_read_the_charts_37a76f);
            case HistorySections.WINDOW_LIST: return context.getString(R.string.ui_previous_window_list_31b6ac);
            case HistorySections.INSIGHT_PACE: return context.getString(R.string.ui_pace_vs_typical_69a481);
            case HistorySections.INSIGHT_EXHAUSTION: return context.getString(R.string.ui_projected_exhaustion_9f0e66);
            case HistorySections.INSIGHT_AVERAGE: return context.getString(R.string.ui_average_completed_window_945040);
            case HistorySections.INSIGHT_PEAK: return context.getString(R.string.ui_peak_burn_rate_5a19a3);
            case HistorySections.VALUE_ESTIMATES: return context.getString(R.string.ui_value_estimates_56a867);
            default: return HistorySections.label(key);
        }
    }
}
