package dev.bennett.codexmeter;

import java.time.LocalDate;
import java.util.*;

/** Behavioral fixtures, not native Android layout/device evidence. */
public final class UiV3SelfTest {
    private static int checks;
    private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    private static void invalid(Runnable task,String why){try{task.run();throw new AssertionError(why);}catch(IllegalArgumentException expected){checks++;}}
    private static LedgerRecord row(String plan,long at,double used,long end){return new LedgerRecord("weekly",plan,at,used,Double.toString(used),end,604800,"api_precise",false);}
    public static void main(String[] args)throws Exception{
        long start=LocalDate.of(2026,10,7).atStartOfDay(LedgerAggregation.ZONE).toInstant().toEpochMilli();long week=604800000L;
        LedgerPeriods.Span span=LedgerPeriods.span(start+week,604800,start+3600000);
        check(span.start==start&&span.end==start+week,"Server window derives exact start/end");
        check(LedgerPeriods.span(0,604800,start)==null,"Unknown reset cannot fabricate dates");
        check(LedgerPeriods.span(start+week,0,start)==null,"Unknown length cannot fabricate start");
        check(LedgerPeriods.span(start+week,604800,start+week)==null,"Observation exactly at reset is not in old window");
        check(LedgerPeriods.span(start+2592000000L,2592000,start)!=null,"Monthly period independent from weekly");
        check(LedgerPeriods.span(start+18000000,18000,start)!=null,"Five-hour period remains independent");
        String policy="weekly|pro|604800";List<LedgerRecord> rows=new ArrayList<>();
        for(int i=0;i<=12;i++){rows.add(row("pro",start-week+i*7200000L,i,start));rows.add(row("pro",start+i*7200000L,i*2,start+week));}
        LedgerPeriods.Measurement[] pair=LedgerPeriods.matched(rows,policy,span,start+86400000L);
        check(pair[0].comparable&&pair[1].comparable,"Comparable same-elapsed observations have adequate coverage");
        check(pair[0].points==24&&pair[1].points==12,"Compare matching elapsed interval, not complete previous week");
        check(pair[0].covered==86400000L,"Measured coverage is actual intervals");
        List<LedgerRecord> sparse=Arrays.asList(row("pro",start+1000,1,start+week),row("pro",start+86400000L,60,start+week));
        check(!LedgerPeriods.measure(sparse,policy,span,start+86400000L).comparable,"Large gaps suppress comparison");
        check(LedgerPeriods.measure(sparse,policy,span,start+86400000L).points==0,"Gap increase never becomes observed window spending");
        List<LedgerRecord> corrected=Arrays.asList(row("pro",start,30,start+week),row("pro",start+3600000,10,start+week),row("pro",start+7200000,15,start+week));
        check(LedgerPeriods.measure(corrected,policy,span,start+7200000).points==5,"Correction cannot create negative usage");
        check(!LedgerPeriods.measure(corrected,policy,span,start+7200000).comparable,"Correction suppresses comparison");
        List<LedgerRecord> policyChange=Arrays.asList(row("pro",start,10,start+week),row("plus",start+3600000,20,start+week),row("pro",start+7200000,40,start+week));
        check(LedgerPeriods.measure(policyChange,policy,span,start+7200000).points==0,"Plan transition cannot bridge usage deltas");
        LedgerPeriods.Span early=LedgerPeriods.span(start+week/2,604800,start);
        check(LedgerPeriods.previous(rows,policy,early)==null,"Overlapping early reset has no comparable previous window");
        check(LedgerPeriods.measure(Collections.emptyList(),policy,span,start+1).observations==0,"No synthetic observations");

        LocalDate today=LocalDate.of(2026,10,8);SubscriptionCost cost=new SubscriptionCost("30000","KRW",today,today.plusMonths(1));
        check(cost.amount.toPlainString().equals("30000")&&cost.currency.equals("KRW"),"Exact amount and currency retained");
        check(cost.active(today)&&!cost.active(cost.end),"Billing end exclusive and independent from reset");
        check(!cost.monetaryComparisonAvailable(),"Quota does not establish monetary benefit");
        check(new SubscriptionCost("0","USD",today,today.plusDays(1)).amount.signum()==0,"Valid zero-cost charge");
        invalid(()->new SubscriptionCost("-1","KRW",today,today.plusDays(1)),"Negative amount");
        invalid(()->new SubscriptionCost("NaN","USD",today,today.plusDays(1)),"Non-finite amount");
        invalid(()->new SubscriptionCost("3","ZZZ",today,today.plusDays(1)),"Invalid currency");
        invalid(()->new SubscriptionCost("3","KRW",today,today),"Invalid dates");
        invalid(()->new SubscriptionCost("1000000001","KRW",today,today.plusDays(1)),"Invalid amount upper bound");

        UsageWindow five=new UsageWindow(25,18000,0,(start+18000000)/1000),weekly=new UsageWindow(40,604800,0,(start+week)/1000);
        UsageSnapshot both=new UsageSnapshot("pro",true,false,five,weekly,start);
        UsageSnapshot weeklyOnly=new UsageSnapshot("pro",true,false,null,weekly,start);
        String saved="five_hour,weekly,next_reset,reset_credits";
        check(WidgetVisibility.resolve(saved,"both",both,true,true,true,true).contains("five_hour"),"ON plus actual five-hour data shows meter");
        check(!WidgetVisibility.resolve(saved,"both",both,false,true,true,true).contains("five_hour"),"OFF plus actual data fully hides meter");
        check(!WidgetVisibility.resolve(saved,"both",null,false,true,true,false).contains("five_hour"),"OFF no data cannot render unknown five-hour label");
        check(WidgetVisibility.resolve(saved,"both",weeklyOnly,false,true,true,false).get(0).equals("weekly"),"Weekly becomes primary without blank slot");
        check(!WidgetVisibility.resolve(saved,"both",weeklyOnly,true,true,true,false).contains("five_hour"),"ON but API does not offer five-hour meter hides it");
        check(WidgetVisibility.resolve(saved,"both",both,true,true,true,true).contains("five_hour"),"Re-enable restores saved choice");
        check(WidgetVisibility.resolve(saved,"both",both,false,false,false,false).isEmpty(),"All disabled does not restore arbitrary meters");
        check(WidgetVisibility.resolve("none","both",both,true,true,true,true).isEmpty(),"Explicit empty selection survives default fallback");
        check(WidgetVisibility.resolve("five_hour","five_hour",both,false,true,true,false).equals(Collections.singletonList("weekly")),"Pinned hidden five-hour selection uses allowed weekly fallback");
        check(WidgetVisibility.resolve("","both",weeklyOnly,false,true,true,false).contains("weekly"),"Legacy missing preference migrates without unavailable five-hour");
        WidgetOptions options=WidgetOptions.defaults().withVisibleMeters(saved);WidgetOptions restored=options.withVisibleMeters(WidgetMeters.effectiveVisibleCsv(options.visibleMeters,options.metricMode));
        check(restored.visibleMeters.equals(saved),"Persisted order round-trip retained across restart");
        check(restored.opacity==options.opacity&&restored.accent.equals(options.accent)&&restored.layout.equals(options.layout),"Design/color/opacity preserved");
        check(WidgetOptions.normalizeTapAction("use_reset").equals("open_app"),"Legacy reset tap becomes harmless open-app action");
        dev.bennett.codexmeter.wear.WearUsageState payload=new dev.bennett.codexmeter.wear.WearUsageState(both,start,"phone",true,false);
        check(payload.snapshot.fiveHour==five&&payload.displaySnapshot().fiveHour==null,"Wear hides only its view, retains raw quota");
        check(payload.displaySnapshot().weekly==weekly,"Wear weekly data preserved");
        dev.bennett.codexmeter.wear.WearUsageState decoded=dev.bennett.codexmeter.wear.WearUsageState.fromJson(payload.toJson());
        check(!decoded.showFiveHour&&decoded.snapshot.fiveHour!=null,"Visibility and raw data both survive wire round-trip");
        org.json.JSONObject older=payload.toJson();older.remove("show_five_hour");
        check(dev.bennett.codexmeter.wear.WearUsageState.fromJson(older).showFiveHour,"Old Wear messages default to existing visible policy");
        for(String style:Arrays.asList("auto","dials","bars"))for(int height:new int[]{60,130,250}){
            List<String> visible=WidgetVisibility.resolve("five_hour,weekly","both",both,false,true,true,false);
            String visual=WidgetMeters.resolveHomeVisualStyle(style,WidgetMeters.singleUsageMetric(visible),2,2,height,250);
            List<String> capped=WidgetMeters.cap(visible,WidgetMeters.slotCapacity(visual,height));
            check(capped.size()==1&&capped.get(0).equals("weekly"),"Resize/style cannot reintroduce disabled five-hour: "+style+height);
            check(restored.visibleMeters.equals(saved),"Resize filtering does not rewrite saved selection");
        }
        System.out.println("UI v3 periods, subscription boundaries and widget visibility: "+checks+" assertions passed (not device proof).");
    }
}
