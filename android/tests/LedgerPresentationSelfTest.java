package dev.bennett.codexmeter;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;

public final class LedgerPresentationSelfTest {
    private static int checks;
    private static void check(boolean value, String message) {
        checks++; if (!value) throw new AssertionError(message);
    }
    public static void main(String[] args) {
        LocalDate today = LocalDate.of(2026, 10, 8);
        String weekly = "weekly|pro|604800", five = "five_hour|pro|18000";
        LedgerAggregation.Day baseline = new LedgerAggregation.Day(weekly, today.toString());
        baseline.count=1;
        check(!LedgerPresentation.measured(null), "Missing date has no usage value");
        check(!LedgerPresentation.measured(baseline), "Single baseline must display dash, not zero");
        baseline.count=2;baseline.uncertain=1;
        check(!LedgerPresentation.measured(baseline), "Cross-midnight/gap observations alone are not measured daily usage");
        LedgerAggregation.Day zero = new LedgerAggregation.Day(weekly, today.minusDays(1).toString());
        zero.count=2;zero.coveredMillis=60000;
        check(LedgerPresentation.measured(zero), "Observed unchanged interval may show an actual zero increase");
        LedgerAggregation.Day used = new LedgerAggregation.Day(weekly, today.minusDays(2).toString());
        used.coveredMillis=60000;used.points=3.75;used.count=2;used.uncertain=1;
        check(LedgerPresentation.measured(used), "Known increase remains visible with an uncertainty badge");
        LedgerAggregation.Day other = new LedgerAggregation.Day(five, today.minusDays(2).toString());
        other.coveredMillis=60000;other.points=90;
        java.util.List<LedgerAggregation.Day> days=Arrays.asList(baseline,zero,used,other);
        LedgerPresentation.Period period=LedgerPresentation.period(days,weekly,today,7);
        check(Math.abs(period.points-3.75)<1e-9, "Never add percentages from different allowances");
        check(period.recordedDays==3 && period.measuredDays==2, "Separate recording coverage from comparable measurements");
        period=LedgerPresentation.period(days,five,today,30);
        check(period.points==90 && period.measuredDays==1, "Independent alternate allowance period");
        period=LedgerPresentation.period(Collections.emptyList(),weekly,today,7);
        check(period.measuredDays==0, "No measured dates means period amount must remain unavailable");
        check(LedgerPresentation.find(days,weekly,today)==baseline, "Policy/date lookup exact");
        check(LedgerPresentation.find(days,weekly,today.minusDays(7))==null, "Missing date is never fabricated");
        long at=1_000_000;
        LedgerRecord a=new LedgerRecord("five_hour","pro",at,20,"20",at+18000000,18000,"api_rounded",false);
        LedgerRecord b=new LedgerRecord("weekly","pro",at,40,"40",at+604800000,604800,"api_rounded",false);
        check(LedgerPresentation.latest(Arrays.asList(a,b))==b, "Prefer weekly among equally recent observations");
        LedgerRecord month=new LedgerRecord("monthly","free",at+1,10,"10",at+2592000000L,2592000,"api_rounded",false);
        check(LedgerPresentation.latest(Arrays.asList(a,b,month))==month, "Newest plan without five-hour limit remains selectable");
        check(LedgerPresentation.latest(Collections.emptyList())==null, "Empty ledger has no synthetic latest value");
        System.out.println("Ledger presentation safety: "+checks+" assertions passed.");
    }
}
