package dev.bennett.codexmeter;

import java.time.LocalDate;
import java.util.List;

/** Real aggregation output drives styling; no Android rendering claim. */
public final class CalendarSelfTest {
    private static int checks;
    private static final long HOUR=3600000L;
    private static final long START=LocalDate.of(2026,10,8).atStartOfDay(LedgerAggregation.ZONE).toInstant().toEpochMilli();
    private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    private static LedgerRecord row(String meter,long at,double used,long reset){
        return new LedgerRecord(meter,"pro",at,used,Double.toString(used),reset,604800,"api_precise",false);
    }
    private static LedgerAggregation.Day day(String meter,long a,long b,double x,double y){
        return LedgerAggregation.aggregate(List.of(row(meter,a,x,START+7*24*HOUR),row(meter,b,y,START+7*24*HOUR)),List.of()).days.values().stream().reduce((p,q)->q).orElseThrow();
    }
    public static void main(String[] args){
        check(!LedgerCalendar.emerald(null)&&LedgerCalendar.intensity(null)==0,"No data is not zero or emerald");
        LedgerAggregation.Day baseline=LedgerAggregation.aggregate(List.of(row("weekly",START+HOUR,90,START+7*24*HOUR)),List.of()).days.values().iterator().next();
        check(!LedgerCalendar.emerald(baseline)&&LedgerCalendar.intensity(baseline)==0,"Cumulative 90% with one reading is not daily use");
        LedgerAggregation.Day zero=day("weekly",START+HOUR,START+2*HOUR,40,40);
        check(LedgerCalendar.intensity(zero)>0&&!LedgerCalendar.emerald(zero),"Measured zero differs from missing data");
        check(!LedgerCalendar.emerald(day("weekly",START+HOUR,START+2*HOUR,0,13)),"Exactly 13 points is not above threshold");
        check(!LedgerCalendar.emerald(day("weekly",START+HOUR,START+2*HOUR,13.1,26.1)),"Floating subtraction noise cannot turn exact 13 into a highlight");
        check(LedgerCalendar.emerald(day("weekly",START+HOUR,START+2*HOUR,0,13.000001)),"Precise increment above boundary qualifies");
        check(LedgerCalendar.emerald(day("weekly",START+HOUR,START+2*HOUR,40,54)),"Daily delta, not cumulative reading");
        check(!LedgerCalendar.emerald(day("five_hour",START+HOUR,START+2*HOUR,0,90)),"Five-hour percentages do not count as weekly use");
        check(!LedgerCalendar.emerald(day("monthly",START+HOUR,START+2*HOUR,0,90)),"Monthly percentages remain independent");
        check(!LedgerCalendar.emerald(day("weekly",START+23*HOUR,START+25*HOUR,0,40)),"Midnight delta is not invented as daily use");
        check(!LedgerCalendar.emerald(day("weekly",START+HOUR,START+HOUR+LedgerAggregation.MAX_GAP+1,0,40)),"Long gap is not measured daily use");
        check(!LedgerCalendar.emerald(day("weekly",START+HOUR,START+2*HOUR,90,2)),"Correction is not consumption");
        LedgerAggregation.Result reset=LedgerAggregation.aggregate(List.of(row("weekly",START+HOUR,90,START+2*HOUR),row("weekly",START+3*HOUR,20,START+7*24*HOUR)),List.of());
        check(reset.days.values().stream().noneMatch(LedgerCalendar::emerald),"Reset does not invent a daily delta");
        LedgerAggregation.Day high=day("weekly",START+HOUR,START+2*HOUR,0,20);
        check(LedgerCalendar.intensity(zero)<LedgerCalendar.intensity(high)&&LedgerCalendar.intensity(high)==125,"Normal heat intensity increases and remains bounded");
        high.points=Double.NaN;check(!LedgerCalendar.emerald(high)&&LedgerCalendar.intensity(high)==0,"Invalid numeric data cannot light a cell");
        System.out.println("Calendar measured-delta/boundary/isolation checks: "+checks+" assertions passed.");
    }
}
