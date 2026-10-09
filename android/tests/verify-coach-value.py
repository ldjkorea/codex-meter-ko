from publication_review import reviewed_widget_reset
"""Production money/coach tests with only resource/context fixtures, plus navigation safety checks."""
from pathlib import Path
import os, json
import baseline_subprocess as subprocess, xml.etree.ElementTree as ET
root=Path(__file__).resolve().parents[1]; app=root/'app/src/main/java/dev/bennett/codexmeter'
out=root/'build/coach-value-verification';out.mkdir(parents=True,exist_ok=True)
res=root/'app/src/main/res';en={};ko={}
for file in ['fun_strings.xml','ux286_strings.xml']:
 for locale,d in [('values',en),('values-ko',ko)]:
  for x in ET.parse(res/locale/file).getroot():d[x.attrib['name']]=''.join(x.itertext()).replace("\\'", "'")
assert en.keys()==ko.keys()
keys=sorted(en); generated='package dev.bennett.codexmeter; public final class R { public static final class string {'
for i,k in enumerate(keys):generated+=f' public static final int {k}={i};'
generated+='}}'
context='package android.content; public class Context {private final String[] labels; public Context(String locale){ labels=locale.equals("ko")?new String[]{'+','.join(json.dumps(ko[k],ensure_ascii=False) for k in keys)+'}:new String[]{'+','.join(json.dumps(en[k],ensure_ascii=False) for k in keys)+'}; } public String getString(int id){return labels[id];}}'
files=[]
for name,text in [('dev/bennett/codexmeter/R.java',generated),('android/content/Context.java',context)]:
 p=out/'fixtures'/name;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(text,encoding='utf-8');files.append(str(p))
shared=root/'shared/src/main/java/dev/bennett/codexmeter'
files += [str(shared/(n+'.java')) for n in ['SubscriptionCost','SubscriptionValue','CoachMoment','FunInsights']]
files += [str(app/'FunCoach.java'),str(Path(__file__).with_name('CoachValueSelfTest.java'))]
classes=out/'classes';classes.mkdir(exist_ok=True)
cp=os.pathsep.join(map(str,[classes,root/'build/insight-verification/classes',root/'build/test-libs/json-20250517.jar']))
jdk=Path(os.environ['JAVA_HOME'])/'bin'
subprocess.run([str(jdk/'javac'),'-encoding','UTF-8','-cp',cp,'-d',str(classes),*files],check=True)
subprocess.run([str(jdk/'java'),'-ea','-cp',cp,'dev.bennett.codexmeter.CoachValueSelfTest'],check=True)
settings=(app/'RecordSettings.java').read_text(encoding='utf-8')
records=settings.split('static void add(',1)[1].split('static void hub(',1)[0]
assert records.count('toggle(a,')==3 and 'MeterSettingsActivity.class' in records
assert not any(x in settings for x in ['TestNotes','fun_test_mode','fun_rule_amount','saveRule('])
for n in ['FunHome','FunActivity']:
 s=(app/(n+'.java')).read_text(encoding='utf-8');assert 'FunStore.rule(' not in s and 'FunInsights.index(' not in s
notes=(app/'NotesUi.java').read_text(encoding='utf-8')
assert 'menu.add(' not in notes and 'static boolean select' in notes and 'return false;' in notes
assert 'TestNotesStore.save(context,note)' in notes # archives/draft writer preserved
analytics=(app/'LedgerAnalyticsActivity.java').read_text(encoding='utf-8')
record_body=analytics.split('private void addRecords(',1)[1].split('private void expandable(',1)[0]
assert 'addEvents' in record_body and 'addUncertain' not in record_body and 'exportChoice' not in record_body
assert 'if(settingsRoot())' in analytics and 'RecordSettings.hub' in analytics
assert 'this::picker,this::confirmClear,this::advancedData' in analytics
assert 'restorePosition();return;' in analytics
for n in ['SubscriptionStore','TestNotesStore','FunStore','MeterVisibility','WidgetRenderer','AccountSession','SecureTokenStore']:
 relative='android/app/src/main/java/dev/bennett/codexmeter/'+n+'.java'
 baseline=subprocess.check_output(['git','show','9de3b6c:'+relative],cwd=root.parent).replace(b'\r\n',b'\n')
 baseline=reviewed_widget_reset(relative,baseline)
 assert (root.parent/relative).read_bytes().replace(b'\r\n',b'\n')==baseline,n
print('Settings/records/data wiring, removal of test entry points and byte-preserved account/payment/note stores passed (source checks).')
