package dev.bennett.codexmeter;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import java.time.Instant;

/** Local-only best-effort daily close. It neither refreshes the API nor requires exact alarms. */
final class LedgerMaintenanceScheduler {
    private static final int JOB_ID = 73120;
    private LedgerMaintenanceScheduler() {}
    static void schedule(Context context) {
        try {
            long now = System.currentTimeMillis();
            long next = Instant.ofEpochMilli(now).atZone(LedgerAggregation.ZONE).toLocalDate()
                    .plusDays(1).atStartOfDay(LedgerAggregation.ZONE).toInstant().toEpochMilli() + 60_000L;
            JobScheduler scheduler = context.getSystemService(JobScheduler.class);
            if (scheduler != null) scheduler.schedule(new JobInfo.Builder(JOB_ID,
                    new ComponentName(context, LedgerMaintenanceJobService.class))
                    .setMinimumLatency(Math.max(60_000L,next-now)).setOverrideDeadline(Math.max(60_000L,next-now)+900_000L)
                    .setPersisted(true).build());
        } catch (Exception ignored) { DiagnosticLog.warn(context,"history","ledger_close_schedule_failed"); }
    }
    static void cancel(Context context) {
        try { JobScheduler scheduler=context.getSystemService(JobScheduler.class); if(scheduler!=null)scheduler.cancel(JOB_ID); }
        catch(Exception ignored) { }
    }
}
