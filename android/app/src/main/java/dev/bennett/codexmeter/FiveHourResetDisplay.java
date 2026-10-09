package dev.bennett.codexmeter;

import android.content.Context;

/** No polling: labels use the current cached server observation and existing refresh state. */
final class FiveHourResetDisplay {
    private FiveHourResetDisplay() {}
    static String text(Context context, UsageWindow window, long observedAt, long now,
            String mode, boolean compact) {
        if (WidgetOptions.RESET_HIDDEN.equals(mode)) return "";
        FiveHourResetState state = FiveHourResetState.at(window, observedAt, now);
        String text;
        switch (state.kind) {
            case NO_WINDOW: text = context.getString(compact ? R.string.pub_five_no_window_short : R.string.pub_five_no_window); break;
            case NO_RESET: text = context.getString(compact ? R.string.pub_five_no_reset_short : R.string.pub_five_hour_reset_unknown); break;
            case EXPIRED: text = context.getString(compact ? R.string.pub_five_expired_short : R.string.pub_five_expired); break;
            default:
                String relative = UsageFormat.compactDuration(context, state.remainingMillis);
                String value = WidgetOptions.RESET_RELATIVE.equals(mode) ? relative
                        : "both".equals(mode) ? UsageFormat.absolute(context, state.resetAtMillis, now) + " (" + relative + ")"
                        : UsageFormat.absolute(context, state.resetAtMillis, now);
                text = compact ? value : context.getString(R.string.pub_five_hour_countdown, value);
        }
        if (!AppPreferences.getLastError(context).isEmpty())
            return text + " · " + context.getString(compact ? R.string.pub_five_error_short : R.string.pub_five_error);
        if (UsageInsights.freshness(observedAt, now, RefreshScheduler.effectiveRefreshMinutes(context)) != UsageInsights.Freshness.FRESH)
            return text + " · " + context.getString(compact ? R.string.pub_five_stale_short : R.string.pub_five_hour_reset_stale);
        return text;
    }
}
