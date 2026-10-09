package dev.bennett.codexmeter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Server reset windows, independent of rolling dates and billing dates. No interpolation. */
public final class LedgerPeriods {
    private LedgerPeriods() {}
    public static final class Span {
        public final long start,end;
        Span(long start,long end){this.start=start;this.end=end;}
    }
    public static Span span(long reset,long seconds,long observed){
        if(reset<=0||seconds<=0||seconds>366L*86400)return null;
        long start=reset-seconds*1000L;
        return start>=0&&observed>=start&&observed<reset?new Span(start,reset):null;
    }
    public static LedgerRecord latest(List<LedgerRecord> input,String policy){
        LedgerRecord found=null;for(LedgerRecord row:input)if(row.policy().equals(policy)&&(found==null||row.at>found.at))found=row;return found;
    }
    public static Span previous(List<LedgerRecord> input,String policy,Span current){
        if(current==null)return null;Span best=null;
        for(LedgerRecord row:input)if(row.policy().equals(policy)){
            Span candidate=span(row.reset,row.seconds,row.at);
            if(candidate!=null&&candidate.end<=current.start&&(best==null||candidate.end>best.end))best=candidate;
        }return best;
    }
    public static final class Measurement {
        public double points;public long covered;public int observations,gaps;
        public boolean comparable;
    }
    public static Measurement measure(List<LedgerRecord> input,String policy,Span span,long until){
        Measurement out=new Measurement();if(span==null)return out;
        long end=Math.min(until,span.end);List<LedgerRecord> rows=new ArrayList<>();
        // Keep all policies of this meter so a plan change cannot be silently bridged.
        String meter=policy.split("\\|")[0];for(LedgerRecord row:input)if(row.meter.equals(meter))rows.add(row);
        rows.sort(Comparator.comparingLong(row->row.at));LedgerRecord before=null;
        for(LedgerRecord row:rows){
            if(row.policy().equals(policy)&&row.at>=span.start&&row.at<=end)out.observations++;
            if(before!=null&&before.at>=span.start&&row.at<=end&&row.policy().equals(policy)){
                LedgerAggregation.Change change=LedgerAggregation.classify(before,row,java.util.Collections.emptyList());
                if(change==LedgerAggregation.Change.NORMAL){out.points+=row.used-before.used;out.covered+=row.at-before.at;}
                else out.gaps++;
            }before=row;
        }
        long duration=end-span.start;
        out.comparable=duration>0&&out.gaps==0&&out.covered>=duration*.8&&out.observations>=3;
        return out;
    }
    public static Measurement[] matched(List<LedgerRecord> rows,String policy,Span current,long now){
        Span prior=previous(rows,policy,current);long elapsed=current==null?0:Math.max(0,Math.min(now,current.end)-current.start);
        Measurement a=measure(rows,policy,current,current==null?0:current.start+elapsed);
        Measurement b=measure(rows,policy,prior,prior==null?0:prior.start+elapsed);
        if(prior==null||current==null||prior.end-prior.start!=current.end-current.start){a.comparable=false;b.comparable=false;}
        return new Measurement[]{a,b};
    }
}
