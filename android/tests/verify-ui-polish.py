"""Production Wi-Fi scheduler, plan aliases and offline catalog on JVM platform fixtures.

These fixtures verify decisions, not Android connectivity dispatch or Samsung UI.
"""
from pathlib import Path
import os,json,re,subprocess,xml.etree.ElementTree as ET
repo=Path(__file__).resolve().parents[2];root=repo/'android';app=root/'app/src/main/java/dev/bennett/codexmeter'
out=root/'build/polish-verification';out.mkdir(parents=True,exist_ok=True)
fixtures={
'android/content/Context.java':'''package android.content;public class Context {public static final String CONNECTIVITY_SERVICE="net",JOB_SCHEDULER_SERVICE="jobs";public final android.app.job.JobScheduler jobs=new android.app.job.JobScheduler();public final android.net.ConnectivityManager net=new android.net.ConnectivityManager();public final SharedPreferences prefs=new SharedPreferences();public Context getApplicationContext(){return this;}public Object getSystemService(String name){return name.equals("net")?net:jobs;}public SharedPreferences getSharedPreferences(String n,int m){return prefs;}public android.content.res.Resources getResources(){return new android.content.res.Resources();}public android.content.res.AssetManager getAssets(){return new android.content.res.AssetManager();}}''',
'android/content/SharedPreferences.java':'package android.content;public class SharedPreferences {public java.util.Map<String,Object> values=new java.util.HashMap<>();public java.util.Map<String,Object> getAll(){return values;}}',
'android/content/ComponentName.java':'package android.content;public class ComponentName {public ComponentName(Context c,Class<?> t){}}',
'android/content/res/Resources.java':'package android.content.res;public class Resources {public Configuration getConfiguration(){return new Configuration();}public static class Configuration {public Locales getLocales(){return new Locales();}}public static class Locales {public java.util.Locale get(int i){return java.util.Locale.KOREAN;}}}',
'android/content/res/AssetManager.java':'package android.content.res;public class AssetManager {public java.io.InputStream open(String n)throws Exception{return new java.io.FileInputStream(System.getProperty("assets")+"/"+n);}}',
'android/net/Network.java':'package android.net;public class Network {}',
'android/net/NetworkCapabilities.java':'package android.net;public class NetworkCapabilities {public static final int TRANSPORT_WIFI=1,NET_CAPABILITY_INTERNET=12;public int type;public NetworkCapabilities(int t){type=t;}public boolean hasTransport(int t){return type==t;}}',
'android/net/NetworkRequest.java':'package android.net;public class NetworkRequest {public int type;public static class Builder {int type;public Builder addTransportType(int t){type=t;return this;}public Builder addCapability(int c){return this;}public NetworkRequest build(){NetworkRequest r=new NetworkRequest();r.type=type;return r;}}}',
'android/net/ConnectivityManager.java':'package android.net;public class ConnectivityManager {public int count;public NetworkCallback callback;public void registerDefaultNetworkCallback(NetworkCallback c){callback=c;count++;}public static class NetworkCallback {public void onCapabilitiesChanged(Network n,NetworkCapabilities c){}public void onLost(Network n){}}}',
'android/os/Build.java':'package android.os;public class Build {public static class VERSION {public static int SDK_INT=28;}}',
'android/os/Looper.java':'package android.os;public class Looper {public static Looper getMainLooper(){return new Looper();}}',
'android/os/Handler.java':'package android.os;public class Handler {public Handler(Looper l){}public void post(Runnable r){r.run();}}',
'android/os/SystemClock.java':'package android.os;public class SystemClock {public static long now=1000;public static long elapsedRealtime(){return now;}}',
'android/app/job/JobScheduler.java':'package android.app.job;public class JobScheduler {public JobInfo pending;public int count;public void cancel(int id){pending=null;}public JobInfo getPendingJob(int id){return pending;}public int schedule(JobInfo j){count++;pending=j;return 1;}}',
'android/app/job/JobInfo.java':'package android.app.job;public class JobInfo {public static final int NETWORK_TYPE_ANY=1;public long interval,flex;public boolean persisted;public int network;public android.net.NetworkRequest request;public static class Builder {JobInfo j=new JobInfo();public Builder(int id,android.content.ComponentName c){}public Builder setPeriodic(long p,long f){j.interval=p;j.flex=f;return this;}public Builder setPersisted(boolean p){j.persisted=p;return this;}public Builder setRequiredNetwork(android.net.NetworkRequest r){j.request=r;return this;}public Builder setRequiredNetworkType(int n){j.network=n;return this;}public JobInfo build(){return j;}}}',
'dev/bennett/codexmeter/LanWifiJobService.java':'package dev.bennett.codexmeter;class LanWifiJobService {}',
'dev/bennett/codexmeter/LanSync.java':'package dev.bennett.codexmeter;class LanSync {static int count;static void schedule(android.content.Context c){count++;}}',
'dev/bennett/codexmeter/WidgetRenderer.java':'package dev.bennett.codexmeter;class WidgetRenderer {static int count;static void updateAll(android.content.Context c){count++;}}',
'dev/bennett/codexmeter/AppPreferences.java':'package dev.bennett.codexmeter;class AppPreferences {static boolean automatic=true;static UsageSnapshot value;static boolean getAutomaticRefresh(android.content.Context c){return automatic;}static UsageSnapshot loadSnapshot(android.content.Context c){return value;}}',
'dev/bennett/codexmeter/UsageSnapshot.java':'package dev.bennett.codexmeter;class UsageSnapshot {long fetchedAtMillis;UsageSnapshot(long at){fetchedAtMillis=at;}}',
'dev/bennett/codexmeter/RefreshScheduler.java':'package dev.bennett.codexmeter;class RefreshScheduler {static int count;static int effectiveRefreshMinutes(android.content.Context c){return 5;}static void scheduleImmediate(android.content.Context c){count++;}}',
'dev/bennett/codexmeter/R.java':'package dev.bennett.codexmeter;class R {static class drawable {static int subscription_free=1,subscription_go=2,subscription_plus=3,subscription_prolite=4,subscription_pro=5;}}',
'dev/bennett/codexmeter/PolishSelfTest.java':'''package dev.bennett.codexmeter;
public class PolishSelfTest {static int n;static void check(boolean b,String s){n++;if(!b)throw new AssertionError(s);}static void wifi(android.content.Context c,long at,int transport){android.os.SystemClock.now=at;c.net.callback.onCapabilitiesChanged(new android.net.Network(),new android.net.NetworkCapabilities(transport));}
 public static void main(String[] args){android.content.Context c=new android.content.Context();LanWifiScheduler.install(c);check(c.net.count==1,"One callback");check(c.jobs.pending==null,"Unpaired no job");wifi(c,1000,1);check(LanSync.count==0,"Unpaired no transfer");check(RefreshScheduler.count==1,"Missing quota schedules existing refresh");wifi(c,1001,1);check(RefreshScheduler.count==1,"Repeated caps throttled");AppPreferences.value=new UsageSnapshot(System.currentTimeMillis());wifi(c,32001,1);check(WidgetRenderer.count==1&&RefreshScheduler.count==1,"Fresh quota repaint only");c.prefs.values.put("a".repeat(64),"synthetic-sealed-pair");LanWifiScheduler.install(c);check(c.net.count==1,"No callback leak on pairing");check(c.jobs.pending.persisted&&c.jobs.pending.interval==900000&&c.jobs.pending.flex==300000,"Bounded persisted periodic job");check(c.jobs.pending.request.type==1,"Metered Wi-Fi supported; cellular excluded");wifi(c,64001,1);check(LanSync.count==1,"Paired reconnect sync");wifi(c,96001,0);check(LanSync.count==1,"Cellular ignored");AppPreferences.automatic=false;AppPreferences.value=null;wifi(c,96001,1);check(LanSync.count==2&&RefreshScheduler.count==1,"Automatic usage OFF respected");c.prefs.values.clear();LanWifiScheduler.ensureScheduled(c);check(c.jobs.pending==null,"Disconnect cancels job");android.os.Build.VERSION.SDK_INT=26;c.prefs.values.put("b".repeat(64),"synthetic");LanWifiScheduler.ensureScheduled(c);check(c.jobs.pending.request==null&&c.jobs.pending.network==1,"API26 fallback network gate");LanWifiScheduler.ensureScheduled(c);check(c.jobs.count==2,"No duplicate job on repeated ensure");
 String[][] plans={{"free","Free"},{"go","Go"},{"plus","Plus"},{"prolite","Pro Lite"},{"pro_lite","Pro Lite"},{"pro-lite","Pro Lite"},{"pro lite","Pro Lite"},{"pro5x","Pro Lite"},{"PRO_5X","Pro Lite"},{"pro","Pro"},{"pro20x","Pro"}};for(String[] p:plans){check(PlanArtwork.image(p[0])>0,"Art "+p[0]);check(PlanArtwork.label(p[0]).equals(p[1]),"Label "+p[0]);}check(PlanArtwork.image(null)==0&&PlanArtwork.image("business")==0&&PlanArtwork.image("enterprise")==0,"No fabricated Free fallback");
 java.util.List<ReleaseCatalog.Entry> all=ReleaseCatalog.all(c);check(all.size()==48,"All 48 versions offline");check(all.get(0).version.equals("2.8.19")&&all.get(47).version.equals("1.0.0"),"Newest to oldest");check(ReleaseCatalog.notes(c,"v2.8.17-beta","fallback").contains("600"),"Beta installed notes mapping");check(ReleaseCatalog.notes(c,"9.0.0","fallback").equals("fallback"),"Unknown release preserved");for(ReleaseCatalog.Entry e:all)check(!e.notes.isBlank()&&!e.title.isBlank(),"Complete editorial entry "+e.version);System.out.println("Production Wi-Fi decisions, subscription aliases, offline release catalog: "+n+" assertions passed (JVM fixtures, not Android dispatch proof).");}}
'''}
files=[]
for name,body in fixtures.items():
 p=out/'fixtures'/name;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(body,encoding='utf-8');files.append(p)
files += [app/(n+'.java') for n in ['LanWifiScheduler','PlanArtwork','ReleaseCatalog']]
jar=root/'build/test-libs/json-20250517.jar';classes=out/'classes';classes.mkdir(exist_ok=True);jdk=Path(os.environ['JAVA_HOME'])/'bin'
subprocess.run([str(jdk/'javac'),'-encoding','UTF-8','-cp',str(jar),'-d',str(classes),*map(str,files)],check=True)
subprocess.run([str(jdk/'java'),'-ea','-Dassets='+str(root/'app/src/main/assets'),'-cp',str(classes)+os.pathsep+str(jar),'dev.bennett.codexmeter.PolishSelfTest'],check=True)
catalog=json.loads((root/'app/src/main/assets/release-history.json').read_text(encoding='utf-8'))
assert len({e['version'] for e in catalog})==48
historic=set(re.findall(r'^#{1,2} (\d+\.\d+\.\d+)',(repo/'CHANGELOG.md').read_text(encoding='utf-8'),re.M));assert historic.issubset({e['version'] for e in catalog})
for entry in catalog:
 assert entry['ko']['changes'] and entry['en']['changes']
 for value in entry['ko']['changes']:assert not re.search(r'승급.*\d+%',value),'Tier thresholds stay out of product copy'
settings=(app/'RecordSettings.java').read_text(encoding='utf-8');assert 'WhatsNewActivity.class' in settings and 'ReleaseHistoryActivity.class' in settings
assert 'LanWifiScheduler.install(this)' in (app/'CodexMeterApplication.java').read_text()
assert 'LanWifiScheduler.ensureScheduled(context)' in (app/'BootReceiver.java').read_text()
assert 'LanWifiScheduler.install(a)' in (app/'LanSyncUi.java').read_text()
job=(app/'LanWifiJobService.java').read_text();assert 'if (active == params)' in job and 'active = null' in job
prefix='android/app/src/main/java/dev/bennett/codexmeter/'
for n in ['UsageApi','UsageParser','UsageLedgerDatabase','AppPreferences','SecureTokenStore','AccountSession','SubscriptionStore','FunStore','TierStore','WidgetOptions','WidgetRenderer','ResetCreditActivity']:
 assert (app/(n+'.java')).read_bytes().replace(b'\r\n',b'\n')==subprocess.check_output(['git','show','e36b8e5:'+prefix+n+'.java'],cwd=repo).replace(b'\r\n',b'\n'),n
for folder in ['values','values-ko']:
 strings={x.get('name'):x.text for x in ET.parse(root/f'app/src/main/res/{folder}/polish_strings.xml').getroot()}
 assert strings['polish_tibo_quote'] and '2026.04.18' in strings['polish_tibo_source']
assert 'TiboQuote.add(activity,target,dark)' in (app/'FunHome.java').read_text()
assert 'HttpURLConnection' not in (app/'TiboQuote.java').read_text(),'Quote stays offline; no X polling'
relative='android/shared/src/main/java/dev/bennett/codexmeter/WidgetMeters.java'
assert (repo/relative).read_bytes().replace(b'\r\n',b'\n')==subprocess.check_output(['git','show','e36b8e5:'+relative],cwd=repo).replace(b'\r\n',b'\n')
print('Hidden-menu preservation, full version coverage, bilingual dated quote, private job lifecycle and 13 unchanged data/auth/widget contracts passed.')
