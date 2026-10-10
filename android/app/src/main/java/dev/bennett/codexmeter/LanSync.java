package dev.bennett.codexmeter;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import org.json.JSONArray;
import org.json.JSONObject;

/** Optional same-LAN bridge. Mobile ledger/billing remain the canonical tier source. */
@android.annotation.SuppressLint("ApplySharedPref") // Pair/unpair must confirm durable state before reporting success.
final class LanSync {
    private static final String PREFS="codex_lan_sync",ALIAS="codex_meter_lan_pair_v1";
    private static final AtomicBoolean BUSY=new AtomicBoolean();
    private static final java.util.concurrent.ExecutorService WORKER=Executors.newSingleThreadExecutor();
    interface Done{void finish(boolean ok);}
    static String account(Context c)throws Exception{AuthTokens t=SecureTokenStore.load(c);return t==null||t.accountId.isEmpty()?null:LanSyncWire.hash(t.accountId.getBytes(StandardCharsets.UTF_8));}
    private static SecretKey key()throws Exception{
        KeyStore store=KeyStore.getInstance("AndroidKeyStore");store.load(null);java.security.Key key=store.getKey(ALIAS,null);if(key instanceof SecretKey)return(SecretKey)key;
        KeyGenerator generator=KeyGenerator.getInstance("AES","AndroidKeyStore");generator.init(new KeyGenParameterSpec.Builder(ALIAS,KeyProperties.PURPOSE_ENCRYPT|KeyProperties.PURPOSE_DECRYPT).setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build());return generator.generateKey();
    }
    static void pair(Context c,String code)throws Exception{
        new LanSyncWire.Pair(code);synchronized(UsageApi.NETWORK_LOCK){String account=account(c);if(account==null)throw new IllegalStateException("Sign in first");
            Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.ENCRYPT_MODE,key());
            JSONObject sealed=new JSONObject().put("iv",Base64.getEncoder().encodeToString(cipher.getIV())).put("ct",Base64.getEncoder().encodeToString(cipher.doFinal(code.trim().getBytes(StandardCharsets.UTF_8))));
            if(!c.getSharedPreferences(PREFS,0).edit().putString(account,sealed.toString()).commit())throw new IllegalStateException("Pairing save failed");}
    }
    private static String code(Context c,String account)throws Exception{
        String raw=c.getSharedPreferences(PREFS,0).getString(account,"");if(raw.isEmpty())return null;JSONObject sealed=new JSONObject(raw);
        Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.DECRYPT_MODE,key(),new GCMParameterSpec(128,Base64.getDecoder().decode(sealed.getString("iv"))));
        return new String(cipher.doFinal(Base64.getDecoder().decode(sealed.getString("ct"))),StandardCharsets.UTF_8);
    }
    static void disconnect(Context c)throws Exception{synchronized(UsageApi.NETWORK_LOCK){String a=account(c);if(a!=null&&!c.getSharedPreferences(PREFS,0).edit().remove(a).remove(a+"_last").commit())throw new IllegalStateException("Disconnect save failed");}}
    static long last(Context c){try{String a=account(c);return a==null?0:c.getSharedPreferences(PREFS,0).getLong(a+"_last",0);}catch(Exception ignored){return 0;}}
    static boolean taskRemote(Context c){try{String a=account(c);String paired=a==null?null:code(c,a);return paired!=null&&new LanSyncWire.Pair(paired).tailscale();}catch(Exception ignored){return false;}}
    static void schedule(Context c){schedule(c,null);}
    static void schedule(Context c,Done done){Context app=c.getApplicationContext();if(!BUSY.compareAndSet(false,true)){if(done!=null)done.finish(false);return;}
        WORKER.execute(()->{boolean success=false;try{
            ConnectivityManager cm=(ConnectivityManager)app.getSystemService(Context.CONNECTIVITY_SERVICE);NetworkCapabilities nc=cm==null?null:cm.getNetworkCapabilities(cm.getActiveNetwork());
            if(nc==null||(!nc.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)&&!nc.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)))return;
            String a,paired;long floor;
            synchronized(UsageApi.NETWORK_LOCK){a=account(app);if(a==null)return;paired=code(app,a);if(paired==null)return;floor=UsageLedgerDatabase.lanFloor(app);}
            LanSyncWire.Pair target=new LanSyncWire.Pair(paired);JSONObject reply=LanSyncWire.exchange(target,a,new JSONObject().put("op","pull").put("floor",floor).put("task_status",TaskStatusStore.enabled(app,a)));
            JSONArray incoming=reply.getJSONArray("rows");if(incoming.length()>100000)throw new IllegalArgumentException("Too many observations");List<LedgerRecord> rows=new ArrayList<>();
            for(int i=0;i<incoming.length();i++){JSONObject r=incoming.getJSONObject(i);double used=r.getDouble("Used");String decimal=r.optString("Decimal",Double.toString(used));if(decimal.equals("null")||decimal.isEmpty())decimal=Double.toString(used);
                String source=r.optString("Source","api_precise");if(source.equals("null")||source.isEmpty())source="api_precise";
                rows.add(new LedgerRecord(r.getString("Meter"),r.getString("Plan"),r.getLong("At"),used,decimal,r.getLong("Reset"),r.getLong("Seconds"),source,r.optBoolean("Manual",false)));}
            JSONObject payload;
            synchronized(UsageApi.NETWORK_LOCK){if(!a.equals(account(app))||!paired.equals(code(app,a)))return;
                UsageLedgerDatabase.mergeLan(app,rows,floor);UsageSnapshot snapshot=AppPreferences.loadSnapshot(app);UsageLedgerDatabase.Data data=UsageLedgerDatabase.load(app);
                LiveUsageStore.invalidate();LiveUtilization.Result live=LiveUsageStore.calculate(app,snapshot,data);payload=payload(data,snapshot,live,floor);}
            JSONObject ack=LanSyncWire.exchange(target,a,payload);if(!ack.getBoolean("ok"))throw new IllegalStateException("Not saved");
            synchronized(UsageApi.NETWORK_LOCK){if(!a.equals(account(app))||!paired.equals(code(app,a))||floor!=UsageLedgerDatabase.lanFloor(app))return;
                if(TaskStatusStore.enabled(app,a))try{TaskStatusStore.save(app,a,reply.optJSONObject("tasks"));}catch(Exception ignored){}
                success=app.getSharedPreferences(PREFS,0).edit().putLong(a+"_last",System.currentTimeMillis()).commit();LiveUsageStore.schedule(app);}
        }catch(Exception ignored){DiagnosticLog.warn(app,"history","lan_sync_deferred");}
        finally{BUSY.set(false);if(done!=null)done.finish(success);}});
    }
    static void taskStatus(Context c,Done done){Context app=c.getApplicationContext();if(!TaskStatusStore.enabled(app)||!BUSY.compareAndSet(false,true)){if(done!=null)done.finish(false);return;}
        WORKER.execute(()->{boolean ok=false;try{ConnectivityManager cm=(ConnectivityManager)app.getSystemService(Context.CONNECTIVITY_SERVICE);NetworkCapabilities nc=cm==null?null:cm.getNetworkCapabilities(cm.getActiveNetwork());
            String a,paired;synchronized(UsageApi.NETWORK_LOCK){a=account(app);if(!TaskStatusStore.enabled(app,a))return;paired=code(app,a);if(paired==null)return;}
            LanSyncWire.Pair target=new LanSyncWire.Pair(paired);
            if(nc==null||!target.taskNetworkAllowed(nc.hasTransport(NetworkCapabilities.TRANSPORT_WIFI),nc.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET),nc.hasTransport(NetworkCapabilities.TRANSPORT_VPN)))return;
            JSONObject response=LanSyncWire.exchange(target,a,new JSONObject().put("op","task-status"));
            synchronized(UsageApi.NETWORK_LOCK){if(!a.equals(account(app))||!paired.equals(code(app,a))||!TaskStatusStore.enabled(app,a))return;TaskStatusStore.save(app,a,response.optJSONObject("tasks"));ok=true;}
        }catch(Exception ignored){}finally{BUSY.set(false);if(done!=null)done.finish(ok);}});
    }
    private static JSONObject payload(UsageLedgerDatabase.Data data,UsageSnapshot snapshot,LiveUtilization.Result live,long floor)throws Exception{
        JSONArray rows=new JSONArray(),days=new JSONArray(),events=new JSONArray();for(LedgerRecord r:data.records)if(r.at>floor)rows.put(new JSONObject().put("At",r.at).put("Reset",r.reset).put("Seconds",r.seconds).put("Meter",r.meter).put("Plan",r.plan).put("Used",r.used).put("Decimal",r.decimal).put("Source",r.source).put("Manual",r.manual));
        for(LedgerAggregation.Day d:data.days)days.put(new JSONObject().put("policy",d.policy).put("day",d.date).put("points",d.points).put("observations",d.count).put("first",d.first).put("last",d.last).put("covered_ms",d.coveredMillis).put("uncertain",d.uncertain).put("boundaries",d.boundaries).put("legacy",d.legacy));
        for(UsageLedgerDatabase.Event e:data.events)events.put(new JSONObject().put("type",e.type).put("at",e.at).put("meter",e.meter).put("origin",e.origin));
        UsageWindow main=snapshot==null?null:snapshot.longWindow();boolean fresh=snapshot!=null&&snapshot.fetchedAtMillis<=System.currentTimeMillis()&&System.currentTimeMillis()-snapshot.fetchedAtMillis<=15*60000&&main!=null&&main.effectiveResetAtMillis(snapshot.fetchedAtMillis)>System.currentTimeMillis();
        String policy=main==null?"":(snapshot.weekly!=null?"weekly":"monthly")+"|"+LedgerRecord.cleanPlan(snapshot.planType)+"|"+main.windowSeconds;
        Object today=JSONObject.NULL;for(LedgerAggregation.Day d:data.days)if(d.policy.equals(policy)&&d.date.equals(LedgerAggregation.day(System.currentTimeMillis()).toString())&&LedgerPresentation.measured(d))today=d.points;
        return new JSONObject().put("op","push").put("floor",floor).put("rows",rows).put("days",days).put("events",events).put("policy",policy).put("today",today).put("tier",fresh?live.tier:-1).put("percent",live.percent)
            .put("observed",snapshot==null?0:snapshot.fetchedAtMillis).put("reset",main==null?0:main.effectiveResetAtMillis(snapshot.fetchedAtMillis)).put("cycle_start",live.start).put("cycle_end",live.end);
    }
}
