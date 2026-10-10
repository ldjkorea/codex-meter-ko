from pathlib import Path
import os,subprocess,xml.etree.ElementTree as ET
root=Path(__file__).resolve().parents[1];app=root/'app/src/main/java/dev/bennett/codexmeter';shared=root/'shared/src/main/java/dev/bennett/codexmeter';out=root/'build/spicy-trial';out.mkdir(parents=True,exist_ok=True)
jar=root/'build/test-libs/json-20250517.jar';jdk=Path(os.environ['JAVA_HOME'])/'bin';cp=str(out)+os.pathsep+str(jar)
subprocess.run([str(jdk/'javac'),'-encoding','UTF-8','-cp',str(jar),'-d',str(out),str(shared/'SpicyRotation.java'),str(shared/'TaskStatusData.java'),str(Path(__file__).with_name('SpicyTrialSelfTest.java'))],check=True)
subprocess.run([str(jdk/'java'),'-ea','-cp',cp,'dev.bennett.codexmeter.SpicyTrialSelfTest',str(root/'app/src/main/assets/spicy-ko.txt')],check=True)
home=(app/'FunHome.java').read_text();cards=(app/'LiveCards.java').read_text();quotes=(app/'SpicyQuotes.java').read_text();main=(app/'MainActivity.java').read_text();lan=(app/'LanSync.java').read_text()
assert home.index('WORKER.execute')<home.index('SpicyQuotes.select')<home.index('home.render')
assert 'now,quote)' in home and 'selected==null?' in cards
assert 'getLanguage().equals("ko")' in quotes and 'if(!loaded)' in quotes and '.apply()' in quotes and '.commit()' not in quotes
assert 'isChangingConfigurations()' in main and 'SpicyQuotes.manual()' in main and 'SpicyQuotes.foreground()' in main
assert 'stamp.equals(LiveUsageStore.signature' in home and 'target.isAttachedToWindow()' in home
assert '!TaskStatusStore.enabled(app)||!BUSY.compareAndSet' in lan and '"task-status"' in lan and 'paired.equals(code(app,a))' in lan
assert 'UsageApi.refresh' not in (app/'TaskStatusActivity.java').read_text()
assert 'new ComponentName(c,TaskStatusWidget.class)' in (app/'TaskStatusWidget.java').read_text()
for filename in ['TaskStatusActivity.java','TaskStatusWidget.java','TaskStatusStore.java']:
 assert 'approve' not in (app/filename).read_text().lower() and 'ProcessBuilder' not in (app/filename).read_text()
en={e.get('name'):e.text for e in ET.parse(root/'app/src/main/res/values/task_trial.xml').getroot()};ko={e.get('name'):e.text for e in ET.parse(root/'app/src/main/res/values-ko/task_trial.xml').getroot()};assert en.keys()==ko.keys() and all(en.values()) and all(ko.values())
print('Actual home/worker/cache wiring, English fallback, snapshot guard, independent OFF-default provider and bilingual trial strings passed (source checks).')
