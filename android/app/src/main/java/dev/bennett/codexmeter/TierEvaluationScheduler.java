package dev.bennett.codexmeter;
import android.content.Context;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
/** Coalesces local catch-up after the existing successful refresh path, never calls an API. */
final class TierEvaluationScheduler {
    private static final ExecutorService WORKER=Executors.newSingleThreadExecutor();
    private static final AtomicBoolean QUEUED=new AtomicBoolean();
    static void afterRefresh(Context c){if(!QUEUED.compareAndSet(false,true))return;Context app=c.getApplicationContext();
        WORKER.execute(()->{try{UsageSnapshot s=AppPreferences.loadSnapshot(app);String key=SubscriptionStore.key(app,s);String policy=TierStore.policy(app);
            if(key!=null&&!policy.isEmpty()){UsageLedgerDatabase.Data data=UsageLedgerDatabase.load(app);TierStore.evaluate(app,key,policy,FunInsights.completed(data.records,policy,System.currentTimeMillis()));}
        }catch(Exception e){DiagnosticLog.error(app,"tier","evaluation_deferred",e);}finally{QUEUED.set(false);}});}
}
