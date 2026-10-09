package dev.bennett.codexmeter;

/** Pure presentation state. The cached observation anchors relative server resets. */
final class FiveHourResetState {
    enum Kind { NO_WINDOW, NO_RESET, EXPIRED, COUNTDOWN }
    final Kind kind;
    final long resetAtMillis, remainingMillis;
    private FiveHourResetState(Kind kind, long at, long remaining) {
        this.kind = kind; this.resetAtMillis = at; this.remainingMillis = remaining;
    }
    static FiveHourResetState at(UsageWindow window, long observedAt, long now) {
        if (window == null) return new FiveHourResetState(Kind.NO_WINDOW, 0, 0);
        long at = window.effectiveResetAtMillis(observedAt);
        if (!window.showsResetCountdown() || at <= 0)
            return new FiveHourResetState(Kind.NO_RESET, 0, 0);
        if (at <= now) return new FiveHourResetState(Kind.EXPIRED, at, 0);
        return new FiveHourResetState(Kind.COUNTDOWN, at, at - now);
    }
}
