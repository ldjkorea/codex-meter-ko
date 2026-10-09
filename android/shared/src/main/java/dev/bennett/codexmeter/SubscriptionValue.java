package dev.bennett.codexmeter;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Allocates the user's charge to one server window; never estimates API cost or savings. */
public final class SubscriptionValue {
    private SubscriptionValue() { }
    public enum State { READY, NO_PAYMENT, NEED_REFRESH, PERIOD_MISMATCH, UNKNOWN_WINDOW }
    public static final class Result {
        public final State state;
        public final BigDecimal allocated, used;
        Result(State state, BigDecimal allocated, BigDecimal used) {
            this.state=state;this.allocated=allocated;this.used=used;
        }
    }
    public static Result calculate(SubscriptionCost bill, LedgerPeriods.Span span,
            BigDecimal percent, boolean fresh, boolean safe) {
        if(bill==null)return new Result(State.NO_PAYMENT,null,null);
        if(span==null||percent==null||percent.signum()<0||percent.compareTo(new BigDecimal("100"))>0||!safe)
            return new Result(State.UNKNOWN_WINDOW,null,null);
        if(!fresh)return new Result(State.NEED_REFRESH,null,null);
        long start=bill.start.atStartOfDay(LedgerAggregation.ZONE).toInstant().toEpochMilli();
        long end=bill.end.atStartOfDay(LedgerAggregation.ZONE).toInstant().toEpochMilli();
        if(start>span.start||end<span.end||span.end<=span.start)
            return new Result(State.PERIOD_MISMATCH,null,null);
        // Keep intermediate precision. Round only when formatting the final monetary amounts.
        BigDecimal allocated=bill.amount.multiply(BigDecimal.valueOf(span.end-span.start))
                .divide(BigDecimal.valueOf(end-start),16,RoundingMode.HALF_UP);
        return new Result(State.READY,allocated,allocated.multiply(percent).divide(new BigDecimal("100")));
    }
}
