package dev.bennett.codexmeter;

import android.content.Context;
import android.app.job.JobParameters;
import java.util.concurrent.*;

/** Production account/job code with deterministic blocking fixtures, not device verification. */
public final class ReleaseStabilitySelfTest {
    private static int checks;
    private static void check(boolean ok, String message) {
        checks++; if(!ok) throw new AssertionError(message);
    }
    private static AuthTokens token(String account) {
        return new AuthTokens("fixture", "fixture", "", Long.MAX_VALUE, account, "");
    }
    private static void waiting(Future<?> future, String message) throws Exception {
        try { future.get(100, TimeUnit.MILLISECONDS); throw new AssertionError(message); }
        catch(TimeoutException expected) { checks++; }
    }
    public static void main(String[] args) throws Exception {
        Context context=new Context();
        ExecutorService threads=Executors.newFixedThreadPool(3);
        try {
            SecureTokenStore.current=token("A");AppPreferences.history=1;
            final CountDownLatch entered=new CountDownLatch(1),release=new CountDownLatch(1);
            Future<?> request=threads.submit(()->{
                synchronized(UsageApi.NETWORK_LOCK) {
                    entered.countDown();
                    try{release.await();}catch(InterruptedException e){throw new AssertionError(e);}
                    SecureTokenStore.current=token("A"); AppPreferences.history=2;
                }
            });
            check(entered.await(3,TimeUnit.SECONDS),"Simulated request owns real network lock");
            Future<AuthTokens> logout=threads.submit(()->AccountSession.signOut(context));
            waiting(logout,"Logout must wait until in-flight persistence completes");
            release.countDown();request.get(3,TimeUnit.SECONDS);
            check(logout.get(3,TimeUnit.SECONDS).accountId.equals("A"),"Revocation receives prior credentials");
            check(SecureTokenStore.current==null && AppPreferences.history==0,"No old credentials or rows resurrect after logout");
            AccountSession.install(context,token("A"),false);AppPreferences.history=5;
            AccountSession.install(context,token("A"),false);
            check(AppPreferences.history==5,"Same known account reauthentication preserves history");
            AccountSession.install(context,token("B"),false);
            check(AppPreferences.history==0,"Different account clears prior history");
            check(SecureTokenStore.historyWhenSaved==0,"Old history cleared before replacement credentials become visible");
            AppPreferences.history=4;AccountSession.install(context,token("B"),true);
            check(AppPreferences.history==0,"Authentication import retains existing explicit clear policy");
            AppPreferences.history=3;SecureTokenStore.current=token("");
            AccountSession.install(context,token(""),false);
            check(AppPreferences.history==0,"Unknown identity cannot mix accounts");
            final CountDownLatch clearEntered=new CountDownLatch(1),clearRelease=new CountDownLatch(1);
            request=threads.submit(()->{synchronized(UsageApi.NETWORK_LOCK){
                clearEntered.countDown();try{clearRelease.await();}catch(InterruptedException e){throw new AssertionError(e);}
                AppPreferences.history=9;
            }});
            check(clearEntered.await(3,TimeUnit.SECONDS),"Second request enters");
            Future<?> deletion=threads.submit(()->AccountSession.clearHistory(context));
            waiting(deletion,"Delete must not race accepted request");
            clearRelease.countDown();request.get(3,TimeUnit.SECONDS);deletion.get(3,TimeUnit.SECONDS);
            check(AppPreferences.history==0 && SecureTokenStore.current!=null,"History delete preserves authentication");

            LedgerMaintenanceJobService service=new LedgerMaintenanceJobService();
            JobParameters first=new JobParameters(),second=new JobParameters();
            service.onStartJob(first);
            check(UsageLedgerDatabase.entered.await(3,TimeUnit.SECONDS),"First maintenance run enters");
            service.onStopJob(first);service.onStartJob(second);
            UsageLedgerDatabase.release.countDown();
            check(service.finished.await(3,TimeUnit.SECONDS),"Replacement maintenance run completes");
            check(service.completed.size()==1 && service.completed.get(0)==second,"Stopped run cannot finish replacement job");
            check(UsageLedgerDatabase.schedules==1,"Stopped job cannot reschedule itself");
            service.onDestroy();
            System.out.println("Release account/lifecycle JVM fixtures: "+checks+" assertions passed (not device proof).");
        } finally { threads.shutdownNow(); }
    }
}
