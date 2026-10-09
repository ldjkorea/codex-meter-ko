package dev.bennett.codexmeter;

import android.content.Context;
import android.text.format.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/* JADX INFO: loaded from: classes.dex */
public final class UsageFormat {
    private UsageFormat() {
    }

    public static String planLabel(String str) {
        if (str == null || str.trim().isEmpty()) {
            return "";
        }
        String normalized = str.trim().toLowerCase(Locale.US).replace("_", "").replace("-", "");
        switch (normalized) {
            case "free": return "Free";
            case "go": return "Go";
            case "plus": return "Plus";
            case "prolite":
            case "pro5x": return "Pro 5x";
            case "pro":
            case "pro20x": return "Pro 20x";
            default: return "";
        }
    }

    public static String percent(Context context, UsageWindow usageWindow, String str, boolean z) {
        if (usageWindow == null) {
            return z ? "—" : context.getString(R.string.ui_unavailable_2c9c1f);
        }
        boolean zEquals = WidgetOptions.DISPLAY_USED.equals(str);
        int iRemainingPercent = zEquals ? usageWindow.usedPercent : usageWindow.remainingPercent();
        if (z) {
            return iRemainingPercent + "%";
        }
        return context.getString(R.string.ui_1_d_2_s_60d41b, iRemainingPercent, zEquals ? context.getString(R.string.ui_used_192a56) : context.getString(R.string.ui_left_12c0f1));
    }

    public static String reset(Context context, UsageWindow usageWindow, String str, long j) {
        return reset(context, usageWindow, str, j, j);
    }

    public static String reset(Context context, UsageWindow usageWindow, String str,
            long observedAtMillis, long nowMillis) {
        if (usageWindow == null || WidgetOptions.RESET_HIDDEN.equals(str)
                || !usageWindow.showsResetCountdown()) {
            return "";
        }
        long jResetAtMillis = usageWindow.effectiveResetAtMillis(observedAtMillis);
        if (jResetAtMillis <= 0) {
            return context.getString(R.string.ui_reset_time_unavailable_5c3030);
        }
        String strAbsolute = absolute(context, jResetAtMillis, nowMillis);
        String strRelative = relative(context, jResetAtMillis, nowMillis);
        if (WidgetOptions.RESET_RELATIVE.equals(str)) {
            return context.getString(R.string.ui_resets_1_s_0805de, strRelative);
        }
        return "both".equals(str) ? context.getString(R.string.ui_resets_1_s_2_s_a5167d, strAbsolute, strRelative) : context.getString(R.string.ui_resets_1_s_0805de, strAbsolute);
    }

    public static String estimatedRemaining(Context context, UsagePace.Assessment assessment) {
        if (assessment == null || !assessment.available) {
            return "";
        }
        if (assessment.estimatedRemainingMillis <= 0L) {
            return context.getString(R.string.ui_est_depleted_f7fa5c);
        }
        return context.getString(R.string.ui_est_1_s_683002, compactDuration(context, assessment.estimatedRemainingMillis));
    }

    static String compactDuration(Context context, long millis) {
        long minutes = Math.max(1L, TimeUnit.MILLISECONDS.toMinutes(Math.max(0L, millis)));
        long days = minutes / 1440L;
        long hours = (minutes % 1440L) / 60L;
        long remainingMinutes = minutes % 60L;
        if (days > 0L) {
            return context.getString(R.string.ui_1_dd_2_dh_8cf3c5, days, hours);
        }
        if (hours > 0L) {
            return context.getString(R.string.ui_1_dh_2_dm_3f8a1a, hours, remainingMinutes);
        }
        return context.getString(R.string.ui_1_dm_2be7be, minutes);
    }

    public static String absolute(Context context, long j, long j2) {
        String str;
        boolean zIs24HourFormat = DateFormat.is24HourFormat(context);
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(j);
        Calendar calendar2 = Calendar.getInstance();
        calendar2.setTimeInMillis(j2);
        Calendar calendar3 = (Calendar) calendar2.clone();
        calendar3.add(6, 1);
        if (sameDay(calendar, calendar2)) {
            str = zIs24HourFormat ? context.getString(R.string.ui_today_at_hh_mm_6a2a8f) : context.getString(R.string.ui_today_at_h_mm_a_56b3f9);
        } else if (sameDay(calendar, calendar3)) {
            str = zIs24HourFormat ? context.getString(R.string.ui_tomorrow_at_hh_mm_95ce8f) : context.getString(R.string.ui_tomorrow_at_h_mm_a_236109);
        } else {
            str = zIs24HourFormat ? context.getString(R.string.ui_eee_mmm_d_at_hh_mm_cea9db) : context.getString(R.string.ui_eee_mmm_d_at_h_mm_a_a8c9a9);
        }
        return new SimpleDateFormat(str, Locale.getDefault()).format(new Date(j));
    }

    private static boolean sameDay(Calendar calendar, Calendar calendar2) {
        return calendar.get(0) == calendar2.get(0) && calendar.get(1) == calendar2.get(1) && calendar.get(6) == calendar2.get(6);
    }

    public static String relative(Context context, long j, long j2) {
        long minutes = TimeUnit.MILLISECONDS.toMinutes(Math.max(0L, j - j2));
        long j3 = minutes / 1440;
        long j4 = (minutes % 1440) / 60;
        long j5 = minutes % 60;
        if (j3 > 0) {
            return context.getString(R.string.ui_in_1_dd_2_dh_d56e90, j3, j4);
        }
        if (j4 > 0) {
            return context.getString(R.string.ui_in_1_dh_2_dm_ec1004, j4, j5);
        }
        return minutes > 0 ? context.getString(R.string.ui_in_1_dm_69ca6b, minutes) : context.getString(R.string.ui_now_c9bc84);
    }

    public static String updated(Context context, long j, long j2) {
        if (j <= 0) {
            return context.getString(R.string.ui_not_updated_yet_95c08f);
        }
        long jMax = Math.max(0L, TimeUnit.MILLISECONDS.toMinutes(j2 - j));
        if (jMax < 1) {
            return context.getString(R.string.ui_updated_just_now_61c8f7);
        }
        if (jMax < 60) {
            return context.getString(R.string.ui_updated_1_dm_ago_d83cbc, jMax);
        }
        long j3 = jMax / 60;
        return j3 < 24 ? context.getString(R.string.ui_updated_1_dh_ago_b10c58, j3) : context.getString(R.string.ui_updated_1_dd_ago_a7c2b1, j3 / 24);
    }
}
