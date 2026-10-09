package dev.bennett.codexmeter;

/** Classifies observations, never attributes unexplained changes to a provider action. */
public final class UsageEventDetector {
    private UsageEventDetector() {}

    public static String detect(UsageSample before, UsageWindow after, long observedAt) {
        if (before == null || after == null || observedAt <= before.observedAtMillis
                || !after.showsResetCountdown()) return "";
        long nextReset = after.effectiveResetAtMillis(observedAt);
        if (nextReset <= observedAt || before.resetAtMillis <= 0L) return "";
        boolean same = UsageWindow.sameResetWindow(before.resetAtMillis, before.windowSeconds,
                nextReset, after.windowSeconds);
        if (before.usedPercent > 0 && nextReset > before.resetAtMillis
                && !same && observedAt >= before.resetAtMillis
                && before.windowSeconds == after.windowSeconds) return "reset_observed";
        if (before.usedPercent - after.usedPercent >= 2) {
            return !same && observedAt < before.resetAtMillis
                    ? "unexpected_reset" : "allowance_increase";
        }
        return "";
    }
}
