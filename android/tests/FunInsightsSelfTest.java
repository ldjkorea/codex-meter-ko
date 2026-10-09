package dev.bennett.codexmeter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public final class FunInsightsSelfTest {
    private static int checks;
    private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    private static LedgerRecord row(long at,double used,long end,String plan){return new LedgerRecord("weekly",plan,at,used,Double.toString(used),end,604800,"api_precise",false);}
    private static List<LedgerRecord> pattern(long start,double[] daily){
        List<LedgerRecord> rows=new ArrayList<>();double used=0;
        for(int h=0;h<=168;h+=3){double value=used;if(h>0){used+=daily[(h-1)/24]*3/24;value=used;}long at=start+h*3600000L;if(h==168)at-=60000;
            rows.add(row(at,value,start+FunInsights.WEEK*1000,"pro"));}return rows;
    }
    public static void main(String[] args)throws Exception{
        long start=LocalDate.of(2026,10,7).atStartOfDay(LedgerAggregation.ZONE).toInstant().toEpochMilli(),week=FunInsights.WEEK*1000;
        FunInsights.Rule rule=new FunInsights.Rule("100000","KRW",604800,start);
        check(FunInsights.value(rule,new BigDecimal("42")).toPlainString().equals("42000"),"Explicit arbitrary 42-percent play value");
        check(FunInsights.value(rule,new BigDecimal("42.125")).toPlainString().equals("42125"),"Precise percentage retained");
        LedgerPeriods.Span span=LedgerPeriods.span(start+week,604800,start+1);
        SubscriptionCost bill=new SubscriptionCost("120000","KRW",LocalDate.of(2026,10,1),LocalDate.of(2026,10,31));
        FunInsights.Index index=FunInsights.index(rule,new BigDecimal("42"),span,bill,true);
        check(index.state==FunInsights.IndexState.OK&&index.index.toPlainString().equals("150.0"),"Same full seven-day charge produces 150 percent, not elapsed-hour inflation");
        check(index.allocated.compareTo(new BigDecimal("28000"))==0,"Actual 30-day billing period allocation");
        check(FunInsights.index(rule,new BigDecimal("42"),span,null,true).state==FunInsights.IndexState.NO_BILLING,"Missing bill with value still available");
        check(FunInsights.index(rule,new BigDecimal("42"),span,new SubscriptionCost("0","KRW",bill.start,bill.end),true).state==FunInsights.IndexState.ZERO_CHARGE,"Zero never infinite");
        check(FunInsights.index(rule,new BigDecimal("42"),span,new SubscriptionCost("120","USD",bill.start,bill.end),true).state==FunInsights.IndexState.CURRENCY,"No fabricated FX");
        check(FunInsights.index(rule,new BigDecimal("42"),span,new SubscriptionCost("120000","KRW",LocalDate.of(2026,10,10),bill.end),true).state==FunInsights.IndexState.PERIOD,"Whole usage cannot compare to partial bill");
        check(FunInsights.index(rule,new BigDecimal("42"),span,bill,false).state==FunInsights.IndexState.STALE,"Stale cannot imply current index");
        check(FunInsights.index(new FunInsights.Rule("100000","KRW",2592000,start),new BigDecimal("42"),span,bill,true).state==FunInsights.IndexState.NO_RULE,"Monthly rule not applied to week");
        double[] edges={0,9.999,10,19.999,20,29.999,30,44.999,45,59.999,60,74.999,75,84.999,85,91.999,92,96.999,97,100};
        for(int i=0;i<edges.length;i++)check(FunInsights.tier(edges[i])==i/2,"Tier boundary "+edges[i]);
        check(FunInsights.rating(new ArrayList<>(),null).tier==-1,"Missing does not mean Iron");
        check(FunInsights.rating(new ArrayList<>(),new BigDecimal("42")).provisional,"Current-only rating is provisional");
        List<LedgerRecord> rows=new ArrayList<>();
        for(int w=0;w<5;w++)for(int h=0;h<=168;h+=3){long at=start+w*week+h*3600000L;if(h==168)at-=60000;rows.add(row(at,(w+1)*15.0*h/168,start+(w+1)*week,"pro"));}
        List<FunInsights.Window> done=FunInsights.completed(rows,"weekly|pro|604800",start+5*week);
        check(done.size()==5,"Only well-observed normal completed windows accepted");
        FunInsights.Rating rating=FunInsights.rating(done,BigDecimal.ZERO);
        check(rating.count==4&&!rating.provisional&&rating.tier==4,"Last four completed windows hold tier after reset");
        check(FunInsights.rating(done,new BigDecimal("99")).tier==rating.tier,"Current challenge never replaces recorded tier");
        int before=rating.tier;FunInsights.value(new FunInsights.Rule("999999","KRW",604800,start),new BigDecimal("99"));
        check(FunInsights.rating(done,BigDecimal.ZERO).tier==before,"Money rule independent from tier");
        check(FunInsights.completed(rows,"weekly|plus|604800",start+5*week).isEmpty(),"Plan isolation");
        List<LedgerRecord> gap=new ArrayList<>(rows.subList(0,57));gap.removeIf(r->r.at>start+3600000*12L&&r.at<start+3600000*48L);
        check(FunInsights.completed(gap,"weekly|pro|604800",start+week).isEmpty(),"Gap disqualifies recorded rating");
        List<LedgerRecord> correction=new ArrayList<>(rows.subList(0,57));correction.set(20,row(correction.get(20).at,0,start+week,"pro"));
        check(FunInsights.completed(correction,"weekly|pro|604800",start+week).isEmpty(),"Correction cannot count as consumed value");
        List<LedgerRecord> early=new ArrayList<>(rows.subList(0,57));early.add(row(start+week-30000,3,start+week+10000,"pro"));
        check(FunInsights.completed(early,"weekly|pro|604800",start+week+20000).isEmpty(),"Early overlapping reset cannot be full week");
        check(!FunInsights.safeCurrent(early,"weekly|pro|604800",span),"Early boundary blocks whole-period index");
        check(!FunInsights.safeCurrent(correction,"weekly|pro|604800",span),"Correction blocks period index");
        check(FunInsights.safeCurrent(rows.subList(0,57),"weekly|pro|604800",span),"Normal observed window safe for index");
        check(FunInsights.style(rows,"weekly|pro|604800",span,span.end)==FunInsights.Style.RESERVE,"Observed low utilization reserve pattern");
        check(FunInsights.style(pattern(start,new double[]{5,5,50,5,5,5,5}),"weekly|pro|604800",span,span.end)==FunInsights.Style.FOCUS,"Concentrated observed daily pattern");
        check(FunInsights.style(pattern(start,new double[]{10,10,10,10,10,10,10}),"weekly|pro|604800",span,span.end)==FunInsights.Style.STEADY,"Steady observed pattern");
        check(FunInsights.style(pattern(start,new double[]{2,2,2,2,2,35,35}),"weekly|pro|604800",span,span.end)==FunInsights.Style.SPRINT,"Late observed pattern");
        check(FunInsights.style(correction,"weekly|pro|604800",span,span.end)==FunInsights.Style.UNKNOWN,"Correction cannot imply a style");
        check(FunInsights.style(new ArrayList<>(),"weekly|pro|604800",span,span.end)==FunInsights.Style.UNKNOWN,"No forced style with sparse observations");
        check(FunInsights.completed(rows,"weekly|pro|604800",start+week-1).isEmpty(),"In-progress never sealed");
        long now=start+5*week+3600000;
        check(FunInsights.situation(rating,BigDecimal.ZERO,week-3600000,true,false)==FunInsights.Situation.REST,"Reset/quiet window preserves past coach context");
        check(FunInsights.situation(rating,new BigDecimal("80"),week,true,true)==FunInsights.Situation.RAPID,"Burst state uses observed acceleration");
        check(FunInsights.situation(rating,new BigDecimal("80"),week,false,true)==FunInsights.Situation.PLACING,"Poor quality never numerically coached");
        check(FunInsights.variant("window",4,FunInsights.Situation.TIER,now)==FunInsights.variant("window",4,FunInsights.Situation.TIER,now+1000),"Refresh does not randomly reshuffle coach");
        TestNote note=new TestNote("test-id","hello","analysis/2","2.8.4","34-release",now,now,1,0,true);
        check(TestNote.from(note.json()).draft&&TestNote.from(note.json()).text.equals("hello"),"Draft and metadata round trip");
        String clean=TestNote.safeExport("Bearer secret-value\naccess_token=secret\naccount_id=private\nC:\\Users\\someone\\file");
        check(!clean.contains("secret")&&!clean.contains("private")&&!clean.contains("someone"),"Sensitive manual content filtered in export");
        check(note.json().keySet().stream().noneMatch(k->k.contains("account")||k.contains("token")||k.contains("path")),"No auto-sensitive metadata");
        System.out.println("Fun value/period/tier/coach/note calculations: "+checks+" assertions passed.");
    }
}
