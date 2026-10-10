package dev.bennett.codexmeter;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;

/** Optional widget-only best-effort refresh. Android may defer it, particularly in Doze. */
public final class TaskStatusJobService extends JobService {
    private static final int JOB=73421;
    private JobParameters active;
    static void schedule(Context c) {
        try {
            JobScheduler jobs=(JobScheduler)c.getSystemService(Context.JOB_SCHEDULER_SERVICE);
            if(jobs==null)return;
            boolean widget=AppWidgetManager.getInstance(c).getAppWidgetIds(new ComponentName(c,TaskStatusWidget.class)).length>0;
            if(!widget||!TaskStatusStore.enabled(c)){jobs.cancel(JOB);return;}
            if(jobs.getPendingJob(JOB)!=null)return;
            jobs.schedule(new JobInfo.Builder(JOB,new ComponentName(c,TaskStatusJobService.class))
                    .setPeriodic(15*60000L,5*60000L).setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                    .setPersisted(true).build());
        } catch(RuntimeException ignored) { /* Foreground refresh is still available. */ }
    }
    @Override public boolean onStartJob(JobParameters params) {
        active=params;
        TaskStatusWidget.update(this);
        LanSync.taskStatus(this,ok->new android.os.Handler(getMainLooper()).post(()->{
            if(active!=params)return;
            active=null;TaskStatusWidget.update(this);schedule(this);jobFinished(params,false);
        }));
        return true;
    }
    @Override public boolean onStopJob(JobParameters params){if(active==params)active=null;return false;}
    @Override public void onDestroy(){active=null;super.onDestroy();}
}
