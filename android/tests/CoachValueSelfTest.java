package dev.bennett.codexmeter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Exercises production calculation and coach catalog; fixtures do not render Android UI. */
public final class CoachValueSelfTest {
    private static int checks;
    private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    private static BigDecimal bd(String s){return new BigDecimal(s);}
    private static long at(String day){return LocalDate.parse(day).atStartOfDay(LedgerAggregation.ZONE).toInstant().toEpochMilli();}
    private static final long DAY=86400000L;
    private static LedgerRecord row(long observed,double used,long reset){return new LedgerRecord("weekly","pro",observed,used,Double.toString(used),reset,604800,"api_precise",false);}
    private static void rejects(Runnable run,String why){boolean threw=false;try{run.run();}catch(IllegalArgumentException e){threw=true;}check(threw,why);}
    public static void main(String[] args){
        long begin=at("2026-10-07");LedgerPeriods.Span span=LedgerPeriods.span(begin+7*DAY,604800,begin+DAY);
        SubscriptionCost bill=new SubscriptionCost("110000","KRW",LocalDate.parse("2026-10-01"),LocalDate.parse("2026-11-01"));
        SubscriptionValue.Result v=SubscriptionValue.calculate(bill,span,bd("65"),true,true);
        check(v.state==SubscriptionValue.State.READY,"Full coverage supports allocation");
        check(v.allocated.subtract(bd("24838.7096774193548387")).abs().compareTo(bd("0.00000001"))<0,"7/31 of actual tax-inclusive payment, not monthly bill");
        check(v.used.subtract(bd("16145.1612903225806452")).abs().compareTo(bd("0.00000001"))<0,"65% of allocated weekly payment");
        check(v.used.compareTo(bd("71500"))!=0,"Never multiply weekly percentage by whole monthly bill");
        check(SubscriptionValue.calculate(bill,span,bd("0"),true,true).used.signum()==0,"Observed zero remains zero");
        check(SubscriptionValue.calculate(bill,span,bd("100"),true,true).used.compareTo(v.allocated)==0,"100% equals this window allocation");
        check(SubscriptionValue.calculate(null,span,bd("65"),true,true).state==SubscriptionValue.State.NO_PAYMENT,"No assumed payment");
        check(SubscriptionValue.calculate(bill,span,bd("65"),false,true).state==SubscriptionValue.State.NEED_REFRESH,"Stale reading suspends amount");
        check(SubscriptionValue.calculate(bill,span,bd("65"),true,false).state==SubscriptionValue.State.UNKNOWN_WINDOW,"Early reset/correction boundary suspends amount");
        check(SubscriptionValue.calculate(bill,null,bd("65"),true,true).used==null,"Unknown reset is not a zero or estimate");
        check(SubscriptionValue.calculate(bill,span,bd("101"),true,true).used==null,"Invalid percentage rejected");
        check(SubscriptionValue.calculate(bill,span,bd("-1"),true,true).used==null,"Negative percentage rejected");
        SubscriptionCost partial=new SubscriptionCost("110000","KRW",LocalDate.parse("2026-10-08"),LocalDate.parse("2026-11-08"));
        check(SubscriptionValue.calculate(partial,span,bd("65"),true,true).state==SubscriptionValue.State.PERIOD_MISMATCH,"Partial window coverage never silently clips a full-window percentage");
        SubscriptionCost ending=new SubscriptionCost("110000","KRW",LocalDate.parse("2026-09-14"),LocalDate.parse("2026-10-13"));
        check(SubscriptionValue.calculate(ending,span,bd("65"),true,true).state==SubscriptionValue.State.PERIOD_MISMATCH,"Billing end excluded");
        SubscriptionCost zero=new SubscriptionCost("0","KRW",bill.start,bill.end);
        check(SubscriptionValue.calculate(zero,span,bd("65"),true,true).used.signum()==0,"Zero payment avoids division/fictional value");
        SubscriptionCost usd=new SubscriptionCost("22.35","USD",bill.start,bill.end);
        BigDecimal usdValue=SubscriptionValue.calculate(usd,span,bd("65.1234"),true,true).used;
        check(usdValue.signum()>0&&usdValue.compareTo(usd.amount)<0,"USD preserved without exchange-rate invention");
        LedgerPeriods.Span month=LedgerPeriods.span(at("2026-11-01"),31*86400L,at("2026-10-12"));
        check(SubscriptionValue.calculate(bill,month,bd("65"),true,true).used.compareTo(bd("71500"))==0,"Full monthly window aligned with bill uses monthly amount");
        rejects(()->new SubscriptionCost("110000","BAD",bill.start,bill.end),"Invalid currency");
        rejects(()->new SubscriptionCost("-1","KRW",bill.start,bill.end),"Negative payment");
        rejects(()->new SubscriptionCost("20","USD",bill.end,bill.start),"Reversed dates");
        int tier=FunInsights.tier(65);SubscriptionValue.calculate(usd,span,bd("65"),true,true);check(FunInsights.tier(65)==tier,"Money never affects tier");
        String policy="weekly|pro|604800";List<LedgerRecord> rows=new ArrayList<>();
        rows.add(row(begin,0,span.end));rows.add(row(begin+DAY,5,span.end));
        check(CoachMoment.choose(rows,policy,span,bd("5"),true,begin+DAY)==CoachMoment.Kind.LOW,"Low use");
        check(CoachMoment.choose(rows,policy,span,bd("50"),true,begin+DAY)==CoachMoment.Kind.NORMAL,"Normal use");
        check(CoachMoment.choose(rows,policy,span,bd("80"),true,begin+DAY)==CoachMoment.Kind.HIGH,"High use");
        check(CoachMoment.choose(rows,policy,span,bd("98"),true,begin+DAY)==CoachMoment.Kind.NEAR_LIMIT,"Near exhaustion");
        check(CoachMoment.choose(rows,policy,span,bd("5"),false,begin+DAY)==CoachMoment.Kind.NONE,"Stale coach makes no usage claim");
        check(CoachMoment.choose(List.of(rows.get(0)),policy,span,bd("5"),true,begin+DAY)==CoachMoment.Kind.NONE,"Single observation triggers data-shortage phrase");
        List<LedgerRecord> correction=List.of(row(begin,50,span.end),row(begin+DAY,5,span.end));
        check(CoachMoment.choose(correction,policy,span,bd("5"),true,begin+DAY)==CoachMoment.Kind.NONE,"Unexpected correction cannot imply rest");
        List<LedgerRecord> reset=List.of(row(begin,0,span.end),row(begin+600000,1,span.end));
        check(CoachMoment.choose(reset,policy,span,bd("1"),true,begin+600000)==CoachMoment.Kind.RESET,"New confirmed interval reaction");
        // Previous completed window and current: same elapsed-day comparison, not whole prior week.
        List<LedgerRecord> matched=new ArrayList<>();long prev=begin-7*DAY;
        matched.add(row(prev,0,begin));matched.add(row(prev+3600000L,3,begin));matched.add(row(prev+7200000L,5,begin));
        matched.add(row(begin,0,span.end));matched.add(row(begin+3600000L,10,span.end));matched.add(row(begin+7200000L,20,span.end));
        check(CoachMoment.choose(matched,policy,span,bd("20"),true,begin+7200000L)==CoachMoment.Kind.INCREASED,"Matched elapsed time increase");
        matched.set(1,row(prev+3600000L,30,begin));matched.set(2,row(prev+7200000L,50,begin));
        check(CoachMoment.choose(matched,policy,span,bd("20"),true,begin+7200000L)==CoachMoment.Kind.DECREASED,"Matched elapsed time decrease");
        matched.remove(1);matched.remove(1);
        check(CoachMoment.choose(matched,policy,span,bd("20"),true,begin+7200000L)==CoachMoment.Kind.NORMAL,"Incomplete prior window does not invent decrease");
        List<LedgerRecord> best=new ArrayList<>();
        for(int week=3;week>=1;week--){long start=begin-week*7*DAY,end=start+7*DAY;
            for(int i=0;i<28;i++)best.add(row(start+i*21600000L,i*1.0,end));
            best.add(row(end-60000,30,end));
        }
        best.add(row(begin,0,span.end));best.add(row(begin+DAY,50,span.end));
        check(FunInsights.completed(best,policy,begin+DAY).size()==3,"Three complete comparable prior windows");
        check(CoachMoment.choose(best,policy,span,bd("50"),true,begin+DAY)==CoachMoment.Kind.BEST,"Only supported personal record reaction");
        check(CoachMoment.choose(best,policy,span,bd("20"),true,begin+DAY)!=CoachMoment.Kind.BEST,"No invented personal record");
        for(String locale:new String[]{"ko","en"}){
            android.content.Context c=new android.content.Context(locale);
            for(FunInsights.Situation state:FunInsights.Situation.values())for(int variant=0;variant<2;variant++){
                String calm=FunCoach.text(c,"calm",3,state,variant),play=FunCoach.text(c,"playful",3,state,variant),spicy=FunCoach.text(c,"spicy",3,state,variant);
                check(!calm.equals(play)&&!play.equals(spicy)&&!calm.equals(spicy),"Distinct tones in "+locale+" "+state);
                check(FunCoach.text(c,"off",3,state,variant).isEmpty(),"Off produces no text");
                check(!play.contains("잠정 배지")&&!play.contains("증거가 부족"),"No developer-style shortage text");
            }
            for(CoachMoment.Kind kind:CoachMoment.Kind.values()){
                String calm=FunCoach.current(c,"calm",3,FunInsights.Situation.TIER,kind,"w",begin+DAY);
                String play=FunCoach.current(c,"playful",3,FunInsights.Situation.TIER,kind,"w",begin+DAY);
                String spicy=FunCoach.current(c,"spicy",3,FunInsights.Situation.TIER,kind,"w",begin+DAY);
                check(!calm.equals(play)&&!play.equals(spicy)&&!calm.equals(spicy),"Actual Home catalog tone differs "+kind);
                check(play.equals(FunCoach.current(c,"playful",3,FunInsights.Situation.TIER,kind,"w",begin+DAY+30000)),"Same-state refresh stable");
                check(FunCoach.current(c,"off",3,FunInsights.Situation.TIER,kind,"w",begin+DAY).isEmpty(),"Off across all moments");
            }
        }
        System.out.println("Production coach catalog / payment allocation: "+checks+" assertions passed (JVM fixtures, not native UI).");
    }
}
