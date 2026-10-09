package dev.bennett.codexmeter;

import java.math.BigDecimal;
import java.util.List;

/** Local reactions to supported quota observations. Billing never participates. */
public final class CoachMoment {
    public enum Kind { NONE, RESET, NEAR_LIMIT, LOW, NORMAL, HIGH, BEST, INCREASED, DECREASED }
    private CoachMoment() { }
    public static Kind choose(List<LedgerRecord> rows,String policy,LedgerPeriods.Span span,
            BigDecimal current,boolean fresh,long now) {
        if(!fresh||current==null||span==null||!FunInsights.safeCurrent(rows,policy,span))return Kind.NONE;
        LedgerPeriods.Measurement measured=LedgerPeriods.measure(rows,policy,span,now);
        if(measured.observations<2)return Kind.NONE;
        double used=current.doubleValue();
        if(used>=95)return Kind.NEAR_LIMIT;
        if(now>=span.start&&now-span.start<=3600000L&&used<=5)return Kind.RESET;
        LedgerPeriods.Measurement[] match=LedgerPeriods.matched(rows,policy,span,now);
        if(match[0].comparable&&match[1].comparable) {
            if(match[0].points>=match[1].points+8)return Kind.INCREASED;
            if(match[1].points>=match[0].points+8)return Kind.DECREASED;
        }
        List<FunInsights.Window> past=FunInsights.completed(rows,policy,now);
        if(past.size()>=3) {
            double best=0;for(FunInsights.Window window:past)best=Math.max(best,window.used.doubleValue());
            if(used>best)return Kind.BEST;
        }
        return used<10?Kind.LOW:used>=75?Kind.HIGH:Kind.NORMAL;
    }
    public static int variant(String window,Kind kind,long now) {
        return Math.floorMod((window+"|"+kind+"|"+LedgerAggregation.day(now)).hashCode(),2);
    }
}
