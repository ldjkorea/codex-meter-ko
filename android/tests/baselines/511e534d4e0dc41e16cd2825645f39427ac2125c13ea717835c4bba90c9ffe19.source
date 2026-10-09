package dev.bennett.codexmeter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Extends the existing freshness/span guardrails using precise ledger values. */
public final class LedgerForecast {
    private LedgerForecast() {}
    public static Result analyze(List<LedgerRecord> input, String policy, long now, int refreshMinutes) {
        List<LedgerRecord> rows=new ArrayList<>();
        for(LedgerRecord r:input) if(r.policy().equals(policy))rows.add(r);
        rows.sort(Comparator.comparingLong(r->r.at));
        Result result=new Result();
        if(rows.isEmpty())return result;
        LedgerRecord last=rows.get(rows.size()-1); result.latest=last;
        long policyBoundary=0;
        for(LedgerRecord row:input)if(row.meter.equals(last.meter)&&!row.policy().equals(policy)){
            if(row.at>last.at){result.reason="stale";return result;}
            policyBoundary=Math.max(policyBoundary,row.at);
        }
        if("legacy".equals(last.source)||UsageInsights.freshness(last.at,now,refreshMinutes)!=UsageInsights.Freshness.FRESH){result.reason="stale";return result;}
        if(last.reset<=now){result.reason="reset";return result;}
        // Allowance/time is arithmetic from a fresh observation; it does not require a rate forecast.
        result.budgetAvailable=true;
        result.budget=Math.min(last.remaining(),last.remaining()*UsageInsights.DAY/(last.reset-last.at));
        List<LedgerRecord> segment=new ArrayList<>();
        LedgerRecord previous=null;
        for(LedgerRecord row:rows){
            if(row.at<last.at-UsageInsights.DAY||row.at<=policyBoundary)continue;
            if(!UsageWindow.sameResetWindow(row.reset,row.seconds,last.reset,last.seconds)){
                segment.clear();previous=null;continue;
            }
            long maxGap=row.seconds<=28_800L?3_600_000L:UsageInsights.MAXIMUM_GAP;
            if(previous!=null&&(row.at<=previous.at||row.at-previous.at>maxGap
                    ||row.at>=previous.reset||row.used<previous.used||row.used-previous.used>30))segment.clear();
            segment.add(row);previous=row;
        }
        if(segment.size()<3)return result;
        LedgerRecord first=segment.get(0);
        long span=last.at-first.at;
        long minimum=last.seconds<=28_800L?600_000L:UsageInsights.MINIMUM_SPAN;
        double delta=last.used-first.used;
        if(span<minimum||delta<2)return result;
        result.ready=true;result.span=span;result.count=segment.size();
        result.dailyRate=delta*UsageInsights.DAY/span;
        double offset=last.remaining()*span/delta;
        if(!Double.isFinite(offset)||offset>Long.MAX_VALUE-last.at){result.ready=false;return result;}
        result.exhaustion=last.at+Math.round(offset);
        result.atReset=Math.max(0,last.remaining()-delta*(last.reset-last.at)/span);
        result.risk=result.exhaustion<last.reset;
        // Compare two observed portions, not project costs. No push notifications are emitted.
        int split=segment.size()/2;
        if(split>=3&&segment.size()-split>=3){
            LedgerRecord a=segment.get(0),b=segment.get(split-1),c=segment.get(split),d=last;
            long oldSpan=b.at-a.at,newSpan=d.at-c.at;
            if(oldSpan>=minimum&&newSpan>=minimum&&b.used-a.used>=2&&d.used-c.used>=4){
                double oldRate=(b.used-a.used)/oldSpan,newRate=(d.used-c.used)/newSpan;
                result.acceleration=newRate/oldRate; result.spike=result.acceleration>=2;
            }
        }
        return result;
    }
    public static final class Result {
        public boolean ready,risk,spike,budgetAvailable; public String reason="warmup";
        public LedgerRecord latest; public double dailyRate,budget,atReset,acceleration;
        public long span,exhaustion;public int count;
    }
}
