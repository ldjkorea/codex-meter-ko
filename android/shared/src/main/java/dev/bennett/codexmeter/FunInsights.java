package dev.bennett.codexmeter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Currency;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Interpretation only. Arbitrary game rules never mutate observations or imply API cost. */
public final class FunInsights {
    public static final int RULE_VERSION=1, TIER_VERSION=1;
    public static final long WEEK=604800, EDGE=3600000;
    private static final double[] BOUNDS={10,20,30,45,60,75,85,92,97};
    private FunInsights(){}
    public static final class Rule {
        public final BigDecimal fullValue; public final String currency; public final long seconds,at;
        public Rule(String value,String currency,long seconds,long at){
            fullValue=new BigDecimal(value);Currency.getInstance(currency);
            if(fullValue.signum()<=0||fullValue.compareTo(new BigDecimal("1000000000"))>0||fullValue.scale()>4||seconds<=0||seconds>366L*86400||at<=0)
                throw new IllegalArgumentException("Invalid play rule");
            this.currency=currency;this.seconds=seconds;this.at=at;
        }
    }
    public static int tier(double percent){
        if(!Double.isFinite(percent)||percent<0||percent>100)throw new IllegalArgumentException("Invalid utilization");
        for(int i=0;i<BOUNDS.length;i++)if(percent<BOUNDS[i])return i;return 9;
    }
    public static BigDecimal value(Rule rule,BigDecimal percent){
        if(percent.signum()<0||percent.compareTo(new BigDecimal("100"))>0)throw new IllegalArgumentException("Invalid utilization");
        return rule.fullValue.multiply(percent).divide(new BigDecimal("100"),Math.max(0,Currency.getInstance(rule.currency).getDefaultFractionDigits()),RoundingMode.HALF_UP);
    }
    public enum IndexState { OK, NO_BILLING, ZERO_CHARGE, CURRENCY, PERIOD, STALE, NO_RULE, BOUNDARY }
    public static boolean safeCurrent(List<LedgerRecord> records,String policy,LedgerPeriods.Span span){
        if(span==null)return false;LedgerRecord previous=null;List<LedgerRecord> ordered=new ArrayList<>(records);ordered.sort(Comparator.comparingLong(r->r.at));
        String meter=policy.split("\\|")[0];
        for(LedgerRecord row:ordered)if(row.meter.equals(meter)&&row.at>=span.start&&row.at<span.end){
            if(!row.policy().equals(policy)||row.reset!=span.end)return false;
            if(previous!=null&&row.used<previous.used)return false;previous=row;
        }return true;
    }
    public static final class Index {
        public final IndexState state;public final BigDecimal allocated,index;
        Index(IndexState state,BigDecimal allocated,BigDecimal index){this.state=state;this.allocated=allocated;this.index=index;}
    }
    public static Index index(Rule rule,BigDecimal percent,LedgerPeriods.Span span,SubscriptionCost bill,boolean fresh){
        if(rule==null||span==null||rule.seconds*1000!=span.end-span.start)return new Index(IndexState.NO_RULE,null,null);
        if(!fresh)return new Index(IndexState.STALE,null,null);
        if(bill==null)return new Index(IndexState.NO_BILLING,null,null);
        if(!bill.currency.equals(rule.currency))return new Index(IndexState.CURRENCY,null,null);
        long start=bill.start.atStartOfDay(LedgerAggregation.ZONE).toInstant().toEpochMilli(),end=bill.end.atStartOfDay(LedgerAggregation.ZONE).toInstant().toEpochMilli();
        // Entire quota window must be covered; never divide a whole-window value by a partial bill.
        if(start>span.start||end<span.end)return new Index(IndexState.PERIOD,null,null);
        if(bill.amount.signum()==0)return new Index(IndexState.ZERO_CHARGE,null,null);
        BigDecimal allocated=bill.amount.multiply(BigDecimal.valueOf(span.end-span.start)).divide(BigDecimal.valueOf(end-start),12,RoundingMode.HALF_UP);
        return new Index(IndexState.OK,allocated,value(rule,percent).multiply(new BigDecimal("100")).divide(allocated,1,RoundingMode.HALF_UP));
    }
    public static final class Window {
        public final String policy;public final LedgerPeriods.Span span;public final BigDecimal used;public final int observations;
        Window(String policy,LedgerPeriods.Span span,LedgerRecord last,int count){this.policy=policy;this.span=span;used=new BigDecimal(last.decimal);observations=count;}
        public String id(){return policy+"@"+span.end;}
    }
    public static List<Window> completed(List<LedgerRecord> records,String policy,long now){
        Map<Long,List<LedgerRecord>> groups=new LinkedHashMap<>();
        for(LedgerRecord row:records)if(row.policy().equals(policy)&&row.seconds==WEEK&&row.reset<=now&&row.reset>row.at&& !"legacy".equals(row.source))
            groups.computeIfAbsent(row.reset,ignored->new ArrayList<>()).add(row);
        List<Window> accepted=new ArrayList<>();
        for(List<LedgerRecord> rows:groups.values()){
            rows.sort(Comparator.comparingLong(r->r.at));LedgerRecord first=rows.get(0),last=rows.get(rows.size()-1);
            LedgerPeriods.Span span=LedgerPeriods.span(last.reset,last.seconds,last.at);if(span==null||rows.size()<3||first.at-span.start>EDGE||span.end-last.at>EDGE)continue;
            boolean safe=true;
            for(int i=1;i<rows.size();i++)if(LedgerAggregation.classify(rows.get(i-1),rows.get(i),java.util.Collections.emptyList())!=LedgerAggregation.Change.NORMAL)safe=false;
            for(LedgerRecord row:records){
                if(row.meter.equals("weekly")&&row.at>=span.start&&row.at<span.end&&(!row.policy().equals(policy)||row.reset!=last.reset))safe=false;
                // A subsequent early-reset overlapping this span disqualifies a nominal seven-day period.
                if(row.meter.equals("weekly")&&row.reset!=last.reset&&row.at>=last.at&&row.at<span.end)safe=false;
            }
            if(safe&&last.at-first.at>=(span.end-span.start)*.8)accepted.add(new Window(policy,span,last,rows.size()));
        }
        accepted.sort(Comparator.comparingLong(w->w.span.end));return accepted;
    }
    public static final class Rating {
        public final int tier,count;public final double average;public final boolean provisional;
        Rating(int tier,int count,double average,boolean provisional){this.tier=tier;this.count=count;this.average=average;this.provisional=provisional;}
    }
    public static Rating rating(List<Window> completed,BigDecimal current){
        if(!completed.isEmpty()){
            int count=Math.min(4,completed.size());double total=0;
            for(int i=completed.size()-count;i<completed.size();i++)total+=completed.get(i).used.doubleValue();
            double average=total/count;return new Rating(tier(average),count,average,false);
        }
        return current==null?new Rating(-1,0,Double.NaN,true):new Rating(tier(current.doubleValue()),0,current.doubleValue(),true);
    }
    public enum Situation { PLACING, PROVISIONAL, REST, NEAR_RESET, RAPID, UP, PROMOTED, TIER }
    public enum Style { UNKNOWN, FOCUS, STEADY, SPRINT, RESERVE }
    public static Style style(List<LedgerRecord> records,String policy,LedgerPeriods.Span span,long until){
        if(span==null)return Style.UNKNOWN;List<LedgerRecord> bounded=new ArrayList<>();
        // End is exclusive: a new reset observed exactly at end belongs to the next window.
        for(LedgerRecord row:records)if(row.at<span.end)bounded.add(row);
        LedgerPeriods.Measurement measurement=LedgerPeriods.measure(bounded,policy,span,until);
        if(!measurement.comparable||measurement.observations<12||Math.min(until,span.end)-span.start<3*86400000L)return Style.UNKNOWN;
        Map<java.time.LocalDate,Double> daily=new LinkedHashMap<>();List<LedgerRecord> rows=new ArrayList<>();
        for(LedgerRecord row:records)if(row.policy().equals(policy)&&row.at>=span.start&&row.at<=until&&row.at<span.end)rows.add(row);
        rows.sort(Comparator.comparingLong(row->row.at));long covered=0;double total=0,sprint=0;
        for(int i=1;i<rows.size();i++){LedgerRecord a=rows.get(i-1),b=rows.get(i);
            if(LedgerAggregation.classify(a,b,java.util.Collections.emptyList())==LedgerAggregation.Change.NORMAL&&LedgerAggregation.day(a.at).equals(LedgerAggregation.day(b.at))){double delta=b.used-a.used;total+=delta;covered+=b.at-a.at;daily.merge(LedgerAggregation.day(b.at),delta,Double::sum);if(a.at>=span.start+(span.end-span.start)*3/4)sprint+=delta;}}
        if(covered<(Math.min(until,span.end)-span.start)*.75||daily.size()<4)return Style.UNKNOWN;
        if(rows.get(rows.size()-1).used<30)return Style.RESERVE;
        if(total<=0)return Style.UNKNOWN;if(sprint/total>=.5)return Style.SPRINT;
        double largest=0;for(double value:daily.values())largest=Math.max(largest,value);return largest/total>=.6?Style.FOCUS:Style.STEADY;
    }
    public static Situation situation(Rating rating,BigDecimal current,long remaining,boolean fresh,boolean rapid){
        if(!fresh||rating.tier<0)return Situation.PLACING;
        if(rapid)return Situation.RAPID;
        if(current!=null&&current.doubleValue()<10&&!rating.provisional)return Situation.REST;
        if(remaining>0&&remaining<=86400000L&&current!=null&&current.doubleValue()<80)return Situation.NEAR_RESET;
        if(rating.provisional)return Situation.PROVISIONAL;
        if(current!=null&&current.doubleValue()>rating.average+8)return Situation.UP;return Situation.TIER;
    }
    /** Deterministic daily variant. Refresh and arbitrary money rules cannot reshuffle it. */
    public static int variant(String window,int tier,Situation state,long now){return Math.floorMod((window+"|"+tier+"|"+state+"|"+LedgerAggregation.day(now)).hashCode(),2);}
}
