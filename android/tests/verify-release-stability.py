#!/usr/bin/env python3
"""Run production account/job concurrency logic on JVM platform fixtures."""
from pathlib import Path
import os
import baseline_subprocess as subprocess
from release_fixes import reviewed_release_patch, PATCHES
from ui_fixes import dashboard_patch

root = Path(__file__).resolve().parents[1]
repo = root.parent
out = root / 'build/release-stability-verification'
out.mkdir(parents=True, exist_ok=True)
app = root / 'app/src/main/java/dev/bennett/codexmeter'
baseline = 'c3b27b7586836b53279a1bea9b7d9a71371cafcc'
for name in PATCHES:
    if name in ("LedgerAnalyticsActivity.java","MainActivity.java","AppPreferences.java","UsageHistoryActivity.java"): continue  # method-level guards in verify-ledger-ui.py
    original = subprocess.check_output(['git', '-C', str(repo), 'show',
        baseline + ':android/app/src/main/java/dev/bennett/codexmeter/' + name]).decode('utf-8').replace('\r\n', '\n')
    expected=reviewed_release_patch(name, original)
    if name == 'MainActivity.java': expected=dashboard_patch(expected)
    actual=(app / name).read_text(encoding='utf-8')
    if name=='SettingsActivity.java':
        link='            findPreference("settings_fun").setOnPreferenceClickListener(preference -> {\n                Ui.startSecondaryActivity(requireActivity(), FunSettingsActivity.class);\n                return true;\n            });\n'
        assert actual.count(link)==1
        actual=actual.replace(link,'')
        dispatch='        // Legacy root links use the same consolidated hub; existing preference subpages stay intact.\n        if (PAGE_ROOT.equals(normalizePage(getIntent().getStringExtra(EXTRA_PAGE)))) {\n            startActivity(new Intent(this, MeterSettingsActivity.class));\n            finish();\n            return;\n        }\n'
        assert actual.count(dispatch)==1
        actual=actual.replace(dispatch,'')
    assert actual == expected, name
assert 'worker.shutdown' not in (app / 'LedgerAnalyticsActivity.java').read_text(encoding='utf-8')
assert 'private static final ExecutorService worker' in (app / 'LedgerAnalyticsActivity.java').read_text(encoding='utf-8')

fixtures = {
'android/content/Context.java': 'package android.content; public class Context {}',
'android/app/job/JobParameters.java': 'package android.app.job; public class JobParameters {}',
'android/app/job/JobService.java': '''package android.app.job;
public abstract class JobService extends android.content.Context {
 public final java.util.List<JobParameters> completed=new java.util.ArrayList<>();
 public final java.util.concurrent.CountDownLatch finished=new java.util.concurrent.CountDownLatch(1);
 public abstract boolean onStartJob(JobParameters p);public abstract boolean onStopJob(JobParameters p);
 public void jobFinished(JobParameters p,boolean retry){completed.add(p);finished.countDown();}
 public void onDestroy(){}
}''',
'dev/bennett/codexmeter/UsageApi.java': 'package dev.bennett.codexmeter; class UsageApi {static final Object NETWORK_LOCK=new Object();}',
'dev/bennett/codexmeter/SecureTokenStore.java': '''package dev.bennett.codexmeter;
class SecureTokenStore {
 static AuthTokens current;static int historyWhenSaved;
 static AuthTokens load(android.content.Context c){return current;}
 static void save(android.content.Context c,AuthTokens t){historyWhenSaved=AppPreferences.history;current=t;}
 static void clear(android.content.Context c){current=null;}
}''',
'dev/bennett/codexmeter/AppPreferences.java': '''package dev.bennett.codexmeter;
class AppPreferences {
 static int history;
 static void clearSnapshot(android.content.Context c){history=0;}
 static void clearUsageHistory(android.content.Context c){history=0;}
 static void setOAuthPending(android.content.Context c,boolean b,String s){}
}''',
'dev/bennett/codexmeter/UsageLedgerDatabase.java': '''package dev.bennett.codexmeter;
class UsageLedgerDatabase {
 static final java.util.concurrent.CountDownLatch entered=new java.util.concurrent.CountDownLatch(1),
 release=new java.util.concurrent.CountDownLatch(1);
 static int loads,schedules;
 static void load(android.content.Context c) throws Exception {
  if(++loads==1){entered.countDown();release.await();}
 }
 static void scheduleMaintenance(android.content.Context c){schedules++;}
}''',
'dev/bennett/codexmeter/DiagnosticLog.java': 'package dev.bennett.codexmeter; class DiagnosticLog {static void warn(android.content.Context c,String a,String b){}}',
}
for name in ['RefreshScheduler','ResetAlertScheduler','WidgetRenderer']:
    method='updateAll' if name=='WidgetRenderer' else 'cancelAll'
    fixtures[f'dev/bennett/codexmeter/{name}.java'] = f'package dev.bennett.codexmeter; class {name} {{static void {method}(android.content.Context c){{}}}}'
files=[]
for name, text in fixtures.items():
    path=out/'fixtures'/name
    path.parent.mkdir(parents=True,exist_ok=True)
    path.write_text(text,encoding='utf-8')
    files.append(str(path))
files += [str(app/'AccountSession.java'),str(app/'LedgerMaintenanceJobService.java'),
          str(app/'AuthTokens.java'),
          str(Path(__file__).with_name('ReleaseStabilitySelfTest.java'))]
jdk=Path(os.environ['JAVA_HOME'])/'bin'
jar=root/'build/test-libs/json-20250517.jar'
classes=out/'classes'
classes.mkdir(exist_ok=True)
subprocess.run([str(jdk/'javac'),'-encoding','UTF-8','-cp',str(jar),'-d',str(classes)]+files,check=True)
subprocess.run([str(jdk/'java'),'-ea','-cp',str(classes)+os.pathsep+str(jar),
               'dev.bennett.codexmeter.ReleaseStabilitySelfTest'],check=True)
print('Exact final audit patches and lifecycle persistence source guards passed.')
