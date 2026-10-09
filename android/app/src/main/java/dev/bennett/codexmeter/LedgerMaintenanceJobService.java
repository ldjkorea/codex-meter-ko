package dev.bennett.codexmeter;

import android.app.job.JobParameters;
import android.app.job.JobService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class LedgerMaintenanceJobService extends JobService {
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final Object lifecycle = new Object();
    private JobParameters active;
    private long generation;
    @Override public boolean onStartJob(JobParameters params) {
        final long run;
        synchronized (lifecycle) { active=params; run=++generation; }
        worker.execute(() -> {
            boolean retry=false;
            try { UsageLedgerDatabase.load(this); }
            catch(Exception ignored) { retry=true; DiagnosticLog.warn(this,"history","ledger_close_failed"); }
            synchronized (lifecycle) {
                if(active!=params || generation!=run) return;
                active=null;
                if(!retry) UsageLedgerDatabase.scheduleMaintenance(this);
                jobFinished(params,retry);
            }
        });
        return true;
    }
    @Override public boolean onStopJob(JobParameters params) {
        synchronized (lifecycle) { if(active==params){active=null;generation++;} }
        return true;
    }
    @Override public void onDestroy(){
        synchronized (lifecycle) { active=null;generation++; }
        worker.shutdown();super.onDestroy();
    }
}
