#!/usr/bin/env python3
"""Display-only scope and measurement presentation checks. No device/rendering claim."""
from pathlib import Path
import os
import baseline_subprocess as subprocess
from ui_fixes import dashboard_patch

root=Path(__file__).resolve().parents[1]
repo=root.parent
app=root/'app/src/main/java/dev/bennett/codexmeter'
base='8ab8e5ef40cb58b8cb32d4527efbff38ffbc5055'
def original(relative):
    return subprocess.check_output(['git','-C',str(repo),'show',base+':'+relative]).decode('utf-8').replace('\r\n','\n')
prefix='android/app/src/main/java/dev/bennett/codexmeter/'
# Main presentation and refresh wiring intentionally changed in 2.8.3.

def method(source, declaration):
    start=source.index(declaration)
    opening=source.index('{',start)
    depth=0
    for i in range(opening,len(source)):
        if source[i]=='{':depth+=1
        elif source[i]=='}':
            depth-=1
            if depth==0:return source[start:i+1]
    raise AssertionError('Unterminated method '+declaration)

analytics=(app/'LedgerAnalyticsActivity.java').read_text(encoding='utf-8')
prior=original(prefix+'LedgerAnalyticsActivity.java')
for declaration in ['private void manual()', 'private void picker(boolean json)',
                    '@Override protected void onActivityResult(int request,int result,Intent intent)']:
    assert method(analytics,declaration)==method(prior,declaration), ('Storage/API/export method changed',declaration)
assert 'private static final ExecutorService worker' in analytics and 'worker.shutdown' not in analytics
assert 'AccountSession.clearHistory(getApplicationContext())' in analytics
for name in ['UsageApi.java','UsageLedgerDatabase.java','AccountSession.java','SettingsActivity.java',
             'OAuthService.java','SettingsTransferStore.java','RefreshScheduler.java',
             'UsageHistoryRecorder.java','SecureTokenStore.java','UsageParser.java']:
    actual=(app/name).read_text(encoding='utf-8')
    if name=='SettingsActivity.java':
        link='            findPreference("settings_fun").setOnPreferenceClickListener(preference -> {\n                Ui.startSecondaryActivity(requireActivity(), FunSettingsActivity.class);\n                return true;\n            });\n'
        assert actual.count(link)==1
        actual=actual.replace(link,'')
        dispatch='        // Legacy root links use the same consolidated hub; existing preference subpages stay intact.\n        if (PAGE_ROOT.equals(normalizePage(getIntent().getStringExtra(EXTRA_PAGE)))) {\n            startActivity(new Intent(this, MeterSettingsActivity.class));\n            finish();\n            return;\n        }\n'
        assert actual.count(dispatch)==1
        actual=actual.replace(dispatch,'')
    assert actual==original(prefix+name),('Non-UI core changed',name)
for relative in [
                 'android/shared/src/main/java/dev/bennett/codexmeter/LedgerAggregation.java',
                 'android/shared/src/main/java/dev/bennett/codexmeter/LedgerForecast.java']:
    assert (repo/relative).read_text(encoding='utf-8')==original(relative),relative

for filename,declarations in {
 'MainActivity.java':['public void signOut()', 'public void confirmSignOut()', 'public void startOrContinueSignIn()'],
 'AppPreferences.java':['public static void clearSnapshot(Context context)', 'public static void clearUsageHistory(Context context)']
}.items():
    for declaration in declarations:
        assert method((app/filename).read_text(encoding='utf-8'),declaration)==method(original(prefix+filename),declaration),(filename,declaration)

jdk=Path(os.environ['JAVA_HOME'])/'bin'
classes=root/'build/insight-verification/classes'
jar=root/'build/test-libs/json-20250517.jar'
classpath=str(classes)+os.pathsep+str(jar)
subprocess.run([str(jdk/'javac'),'-encoding','UTF-8','-cp',classpath,'-d',str(classes),
    str(root/'shared/src/main/java/dev/bennett/codexmeter/LedgerPresentation.java'),
    str(Path(__file__).with_name('LedgerPresentationSelfTest.java'))],check=True)
subprocess.run([str(jdk/'java'),'-ea','-cp',classpath,'dev.bennett.codexmeter.LedgerPresentationSelfTest'],check=True)
print('Auth/account/database, unchanged analytics manual/API/export paths and display-only calculation guards passed.')
