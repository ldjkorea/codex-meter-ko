package dev.bennett.codexmeter;

/** Read-only calendar styling from measured daily deltas, never cumulative quota or gaps. */
public final class LedgerCalendar {
    private LedgerCalendar() { }

    public static boolean emerald(LedgerAggregation.Day day) {
        return LedgerPresentation.measured(day) && day.policy.startsWith("weekly|")
                // Ignore sub-nanopoint floating subtraction noise at the exact boundary.
                && Double.isFinite(day.points) && day.points > 13d + 1e-9;
    }

    public static int intensity(LedgerAggregation.Day day) {
        if (!LedgerPresentation.measured(day) || !Double.isFinite(day.points) || day.points < 0) return 0;
        return (int) (35 + 90 * Math.min(1, day.points / 20d));
    }
}
