from pathlib import Path
import os,subprocess
root=Path(__file__).resolve().parents[1]
app=root/'app/src/main/java/dev/bennett/codexmeter'
classes=root/'build/insight-verification/classes'
jar=root/'build/test-libs/json-20250517.jar'
jdk=Path(os.environ['JAVA_HOME'])/'bin'
sources=[root/'shared/src/main/java/dev/bennett/codexmeter'/name for name in ['LedgerPeriods.java','SubscriptionCost.java','WidgetVisibility.java']]
sources += [app/'WidgetOptions.java',Path(__file__).with_name('UiV3SelfTest.java')]
cp=str(classes)+os.pathsep+str(root/'build/tests')+os.pathsep+str(jar)
subprocess.run([str(jdk/'javac'),'-encoding','UTF-8','-cp',cp,'-d',str(classes),*map(str,sources)],check=True)
subprocess.run([str(jdk/'java'),'-ea','-cp',cp,'dev.bennett.codexmeter.UiV3SelfTest'],check=True)
for name in ['WidgetRenderer.java','SamsungLockWidgetSupport.java','LockWidgetConfigActivity.java']:
    assert 'MeterVisibility.resolve(' in (app/name).read_text(encoding='utf-8'),name
assert 'consumeBestAvailable' not in (app/'ResetCreditActivity.java').read_text(encoding='utf-8')
assert 'useReset' not in (app/'ResetNotificationManager.java').read_text(encoding='utf-8')
assert 'AppPreferences.showUsageOverview(activity)' in (app/'LedgerDashboard.java').read_text(encoding='utf-8')
assert 'showDashboardUsageHistory(activity)' not in (app/'LedgerDashboard.java').read_text(encoding='utf-8')
assert 'compareAndSet(false,true)' in (app/'MainActivity.java').read_text(encoding='utf-8')
print('Phone/lock preview policy, reset-action removal and refresh/overview wiring checks passed.')
wear=root/'wear/src/main/java/dev/bennett/codexmeter'
assert 'fiveHour == null' in (wear/'DualUsageComplicationService.java').read_text(encoding='utf-8')
assert 'showFiveHourDisplay(this)' in (wear/'FiveHourComplicationService.java').read_text(encoding='utf-8')
print('Wear dual text and dedicated-provider visibility wiring checks passed.')
