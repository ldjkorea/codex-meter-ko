package dev.bennett.codexmeter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/** Presentation only: at most three decimals, with no invented trailing precision. */
public final class UsagePrecision {
    private UsagePrecision() {}
    public static String number(double value) {
        if (!Double.isFinite(value)) return "—";
        BigDecimal rounded = BigDecimal.valueOf(value).setScale(3, RoundingMode.HALF_UP);
        DecimalFormat format = new DecimalFormat("0.###", DecimalFormatSymbols.getInstance(Locale.getDefault()));
        format.setRoundingMode(RoundingMode.HALF_UP);
        format.setGroupingUsed(false);
        return format.format(rounded);
    }
    public static String used(UsageWindow window) { return number(window.preciseUsedPercent) + "%"; }
    public static String remaining(UsageWindow window) { return number(100.0 - window.preciseUsedPercent) + "%"; }
}
