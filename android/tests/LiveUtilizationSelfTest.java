package dev.bennett.codexmeter;
import java.util.*;
import java.math.BigDecimal;
public final class LiveUtilizationSelfTest {
    static int checks;static final long W=604800000L,S=1800000000000L;
    static final String P="weekly|pro|604800";
    static void check(boolean v,String m){checks++;if(!v)throw new AssertionError(m);}
    static void near(double expected,double actual,String m){check(Math.abs(expected-actual)<.000001,m+": "+actual);}
    static LedgerRecord row(long at,double value,long reset){return new LedgerRecord("weekly","pro",at,value,Double.toString(value),reset,604800,"api_precise",false);}
    static LiveUtilization.Result calc(List<LedgerRecord> rows,List<Long> credits,long start,long end){LedgerRecord last=rows.get(rows.size()-1);return LiveUtilization.calculate(rows,credits,last.policy(),LedgerPeriods.span(last.reset,last.seconds,last.at),start,end,last.at);}
    public static void main(String[] args){
        List<LedgerRecord> rows=new ArrayList<>();rows.add(row(S+1000,50,S+W));
        LiveUtilization.Result v=calc(rows,List.of(),S-3*W,S+W);near(12.5,v.percent,"Weekly half is eighth of a four-week reference");near(50,v.windowPoints,"Actual current counter");check(v.valid&&v.partial&&v.tier==1,"Immediate Bronze before completed week");
        near(12500,v.amount(new BigDecimal("100000")).doubleValue(),"Fee proportional to cycle utilization");
        rows.add(rows.get(0));near(12.5,calc(rows,List.of(),S-3*W,S+W).percent,"Duplicates not counted");
        rows=List.of(row(S+1000,0,S+W));v=calc(rows,List.of(),S-3*W,S+W);check(v.tier==0&&v.percent==0,"Observed zero is immediate Iron");
        rows=new ArrayList<>();for(int i=0;i<4;i++)rows.add(row(S+i*W+W-1000,100,S+(i+1)*W));
        v=calc(rows,List.of(),S,S+4*W);near(100,v.percent,"Four regular allowances");check(v.tier==9&&!v.bonus,"Full cycle Challenger");
        rows=List.of(row(S+1000,100,S+W),row(S+2000,0,S+W),row(S+3000,100,S+W));
        v=calc(rows,List.of(S+1500),S-3*W,S+W);near(50,v.percent,"Confirmed refill preserves consumption");near(200,v.windowPoints,"Actual two allowances");check(v.bonus&&v.tier==9,"Observed excess refill top tier");
        v=calc(rows,List.of(),S-3*W,S+W);near(25,v.percent,"Unconfirmed correction never doubles");check(!v.bonus,"No invented coupon cause");
        rows=List.of(row(S+1000,5,S+W),row(S+2000,0,S+W),row(S+3000,5,S+W));v=calc(rows,List.of(S+1500),S-3*W,S+W);near(2.5,v.percent,"Small refill sums only actual usage");check(!v.bonus&&v.tier==0,"Coupon alone not top tier");
        rows=List.of(row(S+1000,50,S+W),row(S+2000,60,S+W));v=calc(rows,List.of(S+1500),S-3*W,S+W);near(15,v.percent,"Credit timestamp with no reset does not duplicate");check(!v.bonus,"Unaffected counter no bonus");
        rows=List.of(row(S+86400000L,90,S+W),row(S+2*86400000L,10,S+W+86400000L));v=calc(rows,List.of(),S-3*W,S+W);near(2.5,v.percent,"Unexplained early boundary conservatively replaces segment");check(v.partial&&!v.bonus,"Early boundary uncertain");
        rows=List.of(row(S+10000,100,S+W),row(S+20000,0,S+W+10000),row(S+30000,100,S+W+10000));v=calc(rows,List.of(S+15000),S-3*W,S+W);check(v.bonus,"Drifting reset timestamps still preserve confirmed refill");
        rows=List.of(row(S+1000,50,S+W),row(S+2000,60,S+W));v=calc(rows,List.of(),S+1500,S+30*86400000L);near(0,v.percent,"No invented billing-boundary allocation");check(v.partial&&!v.valid&&v.tier<0,"Unknown boundary is missing, not a zero-usage verdict");
        rows=List.of(row(S+1000,50,S+W),row(S+2000,60,S+W),row(S+3000,70,S+W));v=calc(rows,List.of(),S+1500,S+1500+30*86400000L);near(10*7d/30,v.percent,"Only deltas entirely inside billing cycle");
        rows=List.of(row(S+1000,50,S+W));v=calc(rows,List.of(),S,S+30*86400000L);near(50*7d/30,v.percent,"Actual thirty-day billing normalization");
        rows=List.of(row(S+1000,10,S+W),row(S+86400000L,60,S+W));v=calc(rows,List.of(),S-3*W,S+W);near(15,v.percent,"Quota counter survives observation gap");check(v.partial,"Gap flagged rather than allocated to a day");
        LedgerRecord monthly=new LedgerRecord("monthly","pro",S+1000,50,"50",S+30*86400000L,30*86400,"api_precise",false);v=calc(List.of(monthly),List.of(),S,monthly.reset);near(50,v.percent,"Monthly-only account");check(v.valid,"No five-hour needed");
        LedgerPeriods.Span span=LedgerPeriods.span(S+W,604800,S+1000);check(span.start==S&&span.end==S+W,"Server reset anchors week precisely");
        check(LedgerPeriods.span(S+W,604800,S+W)==null,"Expired week not current");check(!LiveUtilization.calculate(List.of(),List.of(),P,null,S,S+W,S+1000).valid,"Missing reset suppresses tier");
        check(!LiveUtilization.calculate(List.of(row(S+1000,50,S+W)),List.of(),P,span,S,S+W,S+W).valid,"Period expired invalid");
        for(int p=0;p<=100;p++){v=calc(List.of(row(S+1000,p,S+W)),List.of(),S-3*W,S+W);near(p/4d,v.percent,"Every current quota percentage");check(v.valid&&v.tier>=0,"No completion gating");}
        for(String state:List.of("missing","bonus","empty","sprint","active","idle","zero","partial","steady")){
            double d=state.equals("sprint")?14:state.equals("active")?6:state.equals("zero")?0:state.equals("partial")?Double.NaN:1;
            double w=state.equals("empty")?100:state.equals("idle")?0:50;
            for(int i=0;i<3;i++)check(SpicyAi.key(d,w,!state.equals("missing"),state.equals("bonus"),state.equals("idle")?1000:2*86400000L,i*14400000L).equals("live_ai_"+state+"_"+i),"Spicy state "+state);
        }
        check(SpicyAi.key(0,0,true,false,2*86400000L,0).equals(SpicyAi.key(0,0,true,false,2*86400000L,1000)),"Stable wording per four-hour block");
        check(SpicyAi.key(Double.NaN,0,false,false,1000,0).startsWith("live_ai_missing"),"Missing does not insult imaginary consumption");
        rows=List.of(row(S+W-1000,100,S+W),row(S+W+1000,10,S+2*W));v=calc(rows,List.of(S+W),S-2*W,S+2*W);near(27.5,v.percent,"Scheduled rollover remains regular near global credit event");check(!v.bonus,"No bonus from unrelated credit at scheduled reset");
        rows=List.of(row(S+86400000L,100,S+W),row(S+2*86400000L,0,S+W+86400000L),row(S+2*86400000L+1000,1,S+W+86400000L));v=calc(rows,List.of(S+2*86400000L),S+86400000L,S+86400000L+4*W);check(!v.bonus,"Pre-billing usage does not manufacture top-tier refill");
        System.out.println("Immediate cycle utilization/reset/billing/one-voice AI: "+checks+" assertions passed.");
    }
}
