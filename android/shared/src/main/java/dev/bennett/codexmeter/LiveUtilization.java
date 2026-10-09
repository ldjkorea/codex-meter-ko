package dev.bennett.codexmeter;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Observed quota utilization expressed on one cycle. Never a token cost or a fabricated reset. */
public final class LiveUtilization {
    private static final double[] THRESHOLDS={10,20,30,45,60,75,85,92,97};
    public static final class Result {
        public final double percent,windowPoints;
        public final long start,end;
        public final int tier,windows;
        public final boolean partial,bonus,valid;
        Result(double percent,double windowPoints,long start,long end,int windows,boolean partial,boolean bonus,boolean valid){
            this.percent=percent;this.windowPoints=windowPoints;this.start=start;this.end=end;this.windows=windows;
            this.partial=partial;this.bonus=bonus;this.valid=valid;
            int rank=0;while(rank<9&&percent>=THRESHOLDS[rank])rank++;
            tier=valid?(bonus?9:rank):-1;
        }
        public BigDecimal amount(BigDecimal payment){return payment.multiply(BigDecimal.valueOf(percent)).divide(BigDecimal.valueOf(100),MathContext.DECIMAL64);}
    }
    private static final class Segment {
        LedgerRecord first,last;double points,chain;long chainStart;
        boolean credit;
        Segment(LedgerRecord row){first=last=row;points=row.used;chain=row.used;chainStart=row.reset-row.seconds*1000L;}
    }
    public static Result calculate(List<LedgerRecord> input,List<Long> credits,String policy,
            LedgerPeriods.Span current,long start,long end,long at) {
        if(current==null||start<0||end<=start||at<start||at>=end)
            return new Result(0,0,start,end,0,true,false,false);
        String meter=policy.split("\\|")[0];List<LedgerRecord> sorted=new ArrayList<>();
        for(LedgerRecord row:input)if(row.meter.equals(meter)&&row.at<=at)sorted.add(row);
        sorted.sort(Comparator.comparingLong(r->r.at));List<Segment> segments=new ArrayList<>();
        Segment segment=null;LedgerRecord before=null;boolean partial=false;double maximumCreditChain=0;
        for(LedgerRecord row:sorted) {
            if(before!=null&&row.at<=before.at)continue;
            if(!row.policy().equals(policy)||row.reset<=row.at) {
                if(row.at>=start){partial=true;segment=null;}
                before=row;continue;
            }
            LedgerAggregation.Change change=LedgerAggregation.classify(before,row,credits);
            // A global credit event near a scheduled rollover does not make that rollover a refill.
            if(change==LedgerAggregation.Change.CREDIT_CHANGE&&before!=null&&row.at>=before.reset
                    &&row.reset>before.reset)change=LedgerAggregation.Change.RESET;
            if(segment==null||before==null||!before.policy().equals(policy)) {
                segment=new Segment(row);segments.add(segment);
            } else if(change==LedgerAggregation.Change.NORMAL||change==LedgerAggregation.Change.GAP) {
                double delta=row.used-before.used;segment.points+=delta;segment.chain+=delta;segment.last=row;
                if(change==LedgerAggregation.Change.GAP&&row.at>=start)partial=true;
            } else if(change==LedgerAggregation.Change.CREDIT_CHANGE) {
                boolean same=UsageWindow.sameResetWindow(before.reset,before.seconds,row.reset,row.seconds);
                double chain=segment.chain;long chainStart=segment.chainStart;
                if(same){segment.points+=row.used;segment.chain+=row.used;segment.last=row;segment.credit=true;}
                else {segment=new Segment(row);segment.chain=chain+row.used;segment.chainStart=chainStart;segment.credit=true;segments.add(segment);}
            } else if(change==LedgerAggregation.Change.RESET) {
                segment=new Segment(row);segments.add(segment);
            } else {
                // An unexplained correction/new schedule is not proof of another consumed allowance.
                if(segment.last.at>=start){segments.remove(segment);partial=true;}
                segment=new Segment(row);segments.add(segment);
            }

            before=row;
        }
        double total=0,currentPoints=0;int windows=0;long earliest=Long.MAX_VALUE;boolean found=false,measured=false;
        for(Segment s:segments) {
            LedgerPeriods.Span span=LedgerPeriods.span(s.last.reset,s.last.seconds,s.last.at);
            if(span==null||span.start>=end||s.last.at<start)continue;
            earliest=Math.min(earliest,span.start);
            double contribution;
            if(span.start>=start){contribution=s.points;measured=true;if(s.credit&&s.chainStart>=start)maximumCreditChain=Math.max(maximumCreditChain,s.chain);}
            else {
                contribution=0;partial=true;
                LedgerRecord prior=null;
                for(LedgerRecord row:sorted)if(row.at>=s.first.at&&row.at<=s.last.at) {
                    if(prior!=null&&prior.at>=start&&row.policy().equals(policy)&&row.at<end
                            &&LedgerAggregation.classify(prior,row,credits)==LedgerAggregation.Change.NORMAL){
                        contribution+=row.used-prior.used;measured=true;
                    }
                    prior=row;
                }
            }
            total+=contribution;windows++;
            if(UsageWindow.sameResetWindow(s.last.reset,s.last.seconds,current.end,s.last.seconds)){
                currentPoints+=s.points;found=true;
            } else if(s.last.at<span.end-3600000)partial=true;
        }
        partial|=earliest>start;
        LedgerRecord last=LedgerPeriods.latest(sorted,policy);
        boolean valid=found&&measured&&last!=null&&last.at==at&&last.reset>at;
        double percent=last==null?0:total*(last.seconds*1000d)/(end-start);
        // Bonus needs observed consumption above one allowance and a confirmed counter reset.
        boolean bonus=maximumCreditChain>100.000000001;
        return new Result(percent,currentPoints,start,end,windows,partial,bonus,valid);
    }
}
