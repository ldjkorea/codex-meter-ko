package dev.bennett.codexmeter;

import android.app.job.JobParameters;
import android.app.job.JobService;

/** Bounded, opt-in fallback when Android has stopped the foreground app process. */
public final class LanWifiJobService extends JobService {
    private JobParameters active;
    @Override public boolean onStartJob(JobParameters params) {
        active = params;
        LanSync.schedule(this, ok -> new android.os.Handler(getMainLooper()).post(() -> {
            if (active == params) { active = null; jobFinished(params, false); }
        }));
        return true;
    }
    @Override public boolean onStopJob(JobParameters params) {
        if (active == params) active = null;
        return true;
    }
}
