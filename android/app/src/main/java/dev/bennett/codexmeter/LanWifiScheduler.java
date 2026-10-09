package dev.bennett.codexmeter;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

/** Best-effort Wi-Fi reconnect sync; no extra quota requests, cellular transfer or foreground service. */
final class LanWifiScheduler {
    static final int JOB = 73420;
    private static boolean registered;
    private static long lastAttempt = -30000;
    private static Network lastWifi;
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private LanWifiScheduler() {}

    static boolean paired(Context c) {
        for (String key : c.getSharedPreferences("codex_lan_sync", 0).getAll().keySet())
            if (key.matches("[0-9a-f]{64}")) return true;
        return false;
    }

    static synchronized void install(Context c) {
        Context app = c.getApplicationContext();
        ensureScheduled(app);
        if (registered) return;
        ConnectivityManager cm = (ConnectivityManager) app.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return;
        try {
            cm.registerDefaultNetworkCallback(new ConnectivityManager.NetworkCallback() {
                @Override public void onCapabilitiesChanged(Network network, NetworkCapabilities capabilities) {
                    if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI))
                        MAIN.post(() -> {if(!network.equals(lastWifi)){lastWifi=network;reconnect(app);}});
                }
                @Override public void onLost(Network network) {
                    MAIN.post(() -> {if(network.equals(lastWifi))lastWifi=null;});
                }
            });
            registered = true;
        } catch (RuntimeException ignored) { /* Periodic persisted job remains the fallback. */ }
    }

    private static synchronized void reconnect(Context c) {
        if (SystemClock.elapsedRealtime() - lastAttempt < 30000) return;
        lastAttempt = SystemClock.elapsedRealtime();
        // The active network and account/pair are validated again by the existing bridge.
        if(paired(c))LanSync.schedule(c);
        UsageSnapshot snapshot=AppPreferences.loadSnapshot(c);
        if(AppPreferences.getAutomaticRefresh(c)&&(snapshot==null
                ||System.currentTimeMillis()-snapshot.fetchedAtMillis>=RefreshScheduler.effectiveRefreshMinutes(c)*60000L))
            RefreshScheduler.scheduleImmediate(c);
        else WidgetRenderer.updateAll(c);
    }

    static void ensureScheduled(Context c) {
        try {
            JobScheduler jobs = (JobScheduler) c.getSystemService(Context.JOB_SCHEDULER_SERVICE);
            if (jobs == null) return;
            if (!paired(c)) { jobs.cancel(JOB); return; }
            if (jobs.getPendingJob(JOB) != null) return;
            JobInfo.Builder job = new JobInfo.Builder(JOB, new ComponentName(c, LanWifiJobService.class))
                    .setPeriodic(15 * 60000L, 5 * 60000L).setPersisted(true);
            if (Build.VERSION.SDK_INT >= 28) {
                job.setRequiredNetwork(new NetworkRequest.Builder()
                        .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                        .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET).build());
            } else job.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY);
            jobs.schedule(job.build());
        } catch (RuntimeException ignored) { /* App resume and refresh keep their existing retries. */ }
    }
}
