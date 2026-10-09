package dev.bennett.codexmeter;

import android.content.Context;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicLong;

/** Derived cache only. Ledger/auth/payment and legacy tier journals remain the source of truth. */
final class LiveUsageStore {
    private static final ExecutorService WORKER=Executors.newSingleThreadExecutor();
    private static final AtomicLong REVISION=new AtomicLong();
    private static volatile Cached cache;
    private static String requested="";
    private static final class Cached {
        final String signature;final LiveUtilization.Result result;
        Cached(String signature,LiveUtilization.Result result){this.signature=signature;this.result=result;}
    }
    static long revision(){return REVISION.get();}
    static void invalidate(){cache=null;synchronized(LiveUsageStore.class){requested="";}REVISION.incrementAndGet();}
    static String signature(Context c,UsageSnapshot s){return c.getSharedPreferences("secure_auth_v1",0).getString("blob","")+":"
            +(s==null?0:s.fetchedAtMillis)+":"+quotaStamp(s)+":"+c.getSharedPreferences("codex_subscription_cost",0).getAll().hashCode()
            +":"+LedgerAggregation.day(System.currentTimeMillis())+":"+c.getSharedPreferences("codex_meter_settings_v1",0).getString("usage_history_weekly","").hashCode()+":"+c.getSharedPreferences("codex_meter_settings_v1",0).getString("usage_history_monthly","").hashCode();}
    private static String quotaStamp(UsageSnapshot s){
        UsageWindow w=s==null?null:s.longWindow();
        return w==null?"":s.planType+":"+(s.weekly!=null?"weekly":"monthly")+":"+w.windowSeconds+":"+w.usedPercent+":"+w.effectiveResetAtMillis(s.fetchedAtMillis);
    }
    static LiveUtilization.Result calculate(Context c,UsageSnapshot s,UsageLedgerDatabase.Data data){
        UsageWindow w=s==null?null:s.longWindow();String meter=s!=null&&s.weekly!=null?"weekly":"monthly";
        String policy=w==null?"":meter+"|"+LedgerRecord.cleanPlan(s.planType)+"|"+w.windowSeconds;
        LedgerPeriods.Span span=w==null?null:LedgerPeriods.span(w.effectiveResetAtMillis(s.fetchedAtMillis),w.windowSeconds,s.fetchedAtMillis);
        long start=span==null?0:Math.max(0,span.end-w.windowSeconds*1000L*(meter.equals("weekly")?4:1));
        long end=span==null?0:span.end;
        SubscriptionCost bill=SubscriptionStore.load(c,s);
        if(bill!=null&&bill.active(LedgerAggregation.day(System.currentTimeMillis()))){
            start=bill.start.atStartOfDay(LedgerAggregation.ZONE).toInstant().toEpochMilli();
            end=bill.end.atStartOfDay(LedgerAggregation.ZONE).toInstant().toEpochMilli();
        }
        List<LedgerRecord> rows=new ArrayList<>(data.records);
        if(w!=null&&rows.stream().noneMatch(r->r.policy().equals(policy)&&r.at==s.fetchedAtMillis))
            rows.add(new LedgerRecord(meter,s.planType,s.fetchedAtMillis,w.usedPercent,Integer.toString(w.usedPercent),w.effectiveResetAtMillis(s.fetchedAtMillis),w.windowSeconds,"api_rounded",false));
        List<Long> credits=new ArrayList<>();for(UsageLedgerDatabase.Event e:data.events)if(e.type.equals("credit_used")&&e.origin.equals("confirmed"))credits.add(e.at);
        return LiveUtilization.calculate(rows,credits,policy,span,start,end,s==null?0:s.fetchedAtMillis);
    }
    static LiveUtilization.Result current(Context c){
        UsageSnapshot s=AppPreferences.loadSnapshot(c);Cached value=cache;
        if(value!=null&&value.signature.equals(signature(c,s)))return value.result;
        return calculate(c,s,new UsageLedgerDatabase.Data());
    }
    private static void publish(Context c,UsageSnapshot captured,LiveUtilization.Result result){
        synchronized(UsageApi.NETWORK_LOCK){
            UsageSnapshot current=AppPreferences.loadSnapshot(c);
            if(current==null||captured==null||current.fetchedAtMillis!=captured.fetchedAtMillis)return;
            String stamp=signature(c,current);Cached prior=cache;cache=new Cached(stamp,result);
            if(prior==null||!prior.signature.equals(stamp)||prior.result.tier!=result.tier||prior.result.percent!=result.percent)REVISION.incrementAndGet();
        }
    }
    static void schedule(Context c){
        if(c==null)return;Context app=c.getApplicationContext();UsageSnapshot s=AppPreferences.loadSnapshot(app);
        String identity=app.getSharedPreferences("secure_auth_v1",0).getString("blob","");
        if(s==null||identity.isEmpty()){cache=null;REVISION.incrementAndGet();return;}
        String stamp=signature(app,s);
        synchronized(LiveUsageStore.class){if(stamp.equals(requested))return;requested=stamp;}
        WORKER.execute(()->{try{
            UsageLedgerDatabase.Data data=UsageLedgerDatabase.load(app);LiveUtilization.Result result=calculate(app,s,data);
            synchronized(UsageApi.NETWORK_LOCK){
                if(!stamp.equals(signature(app,AppPreferences.loadSnapshot(app))))return;
                publish(app,s,result);
            }
            EvolutionWidget.updateAll(app);
        }catch(Exception e){synchronized(LiveUsageStore.class){if(stamp.equals(requested))requested="";}DiagnosticLog.error(app,"tier","live_utilization_deferred",e);}});
    }
}
