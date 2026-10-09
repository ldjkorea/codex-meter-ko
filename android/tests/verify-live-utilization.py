"""Run the production immediate engine and check its active UI integration. No device proof."""
from pathlib import Path
import os,subprocess,xml.etree.ElementTree as ET,json,hashlib,re,sys
repo=Path(__file__).resolve().parents[2];root=repo/'android';shared=root/'shared/src/main/java/dev/bennett/codexmeter';app=root/'app/src/main/java/dev/bennett/codexmeter'
out=root/'build/live-verification';out.mkdir(parents=True,exist_ok=True)
cp=os.pathsep.join(map(str,[out,root/'build/insight-verification/classes',root/'build/test-libs/json-20250517.jar']))
jdk=Path(os.environ['JAVA_HOME'])/'bin'
subprocess.run([str(jdk/'javac'),'-encoding','UTF-8','-cp',cp,'-d',str(out),str(shared/'LedgerPeriods.java'),str(shared/'LiveUtilization.java'),str(shared/'SpicyAi.java'),str(Path(__file__).with_name('LiveUtilizationSelfTest.java'))],check=True)
subprocess.run([str(jdk/'java'),'-ea','-cp',cp,'dev.bennett.codexmeter.LiveUtilizationSelfTest'],check=True)
en={x.get('name'):x.text for x in ET.parse(root/'app/src/main/res/values/live_strings.xml').getroot()};ko={x.get('name'):x.text for x in ET.parse(root/'app/src/main/res/values-ko/live_strings.xml').getroot()}
assert en.keys()==ko.keys() and len([x for x in en if x.startswith('live_ai_') and x!='live_ai_title'])==27
for state in ['missing','bonus','empty','sprint','active','idle','zero','partial','steady']:
 for i in range(3):assert all(d[f'live_ai_{state}_{i}'] for d in [en,ko])
assert ko['live_ai_title']=='AI의 한마디'
home=(app/'FunHome.java').read_text(encoding='utf-8');main=(app/'MainActivity.java').read_text(encoding='utf-8');settings=(app/'RecordSettings.java').read_text(encoding='utf-8');cards=(app/'LiveCards.java').read_text(encoding='utf-8')
assert main.index('content.addView(crestSlot)')<main.index('LedgerDashboard.addOverview')
assert 'restoreHomeScroll,crestSlot' in main and 'crestTarget.addView(tier)' in home
assert 'LiveUtilization.Result live' in home and 'LiveCards.crest(' in home
assert 'LiveUsageStore.calculate(' in home and 'LiveCards.daily(' in home
for s in [home,(app/'FunActivity.java').read_text(encoding='utf-8'),settings]:
 assert 'R.string.fun_why' not in s and 'R.string.fun_tone' not in s and 'R.string.fun_tier)' not in s
assert 'R.string.ux_value_method' not in settings and 'PlanArtwork.image(' in settings
assert 'day.coveredMillis>0||day.points>0' in cards and 'Double.NaN' in cards
assert 'LiveUsageStore.invalidate();WidgetRenderer.updateAll' in (app/'LedgerAnalyticsActivity.java').read_text(encoding='utf-8')
assert 'LiveUsageStore.schedule(context)' in (app/'WidgetRenderer.java').read_text(encoding='utf-8')
assert 'LiveUsageStore.current(c)' in (app/'TierTheme.java').read_text(encoding='utf-8')
assets=json.loads((repo/'docs/plan-artwork/ASSETS.json').read_text(encoding='utf-8'))['assets']
assert len(assets)==5 and len({a['sha256'] for a in assets})==5
for a in assets:
 raw=(repo/a['path']).read_bytes();assert hashlib.sha256(raw).hexdigest()==a['sha256'] and len(raw)==a['bytes']
 assert raw[12:16]==b'VP8X' and raw[20]&0x10 and int.from_bytes(raw[24:27],'little')+1==256 and int.from_bytes(raw[27:30],'little')+1==256
assert sum(a['bytes'] for a in assets)<250000
if len(sys.argv)>1:
 from zipfile import ZipFile
 sdk=Path(os.environ['ANDROID_HOME']);table=subprocess.check_output([str(sdk/'build-tools/36.0.0/aapt2.exe'),'dump','resources',sys.argv[1]],text=True,encoding='utf-8')
 with ZipFile(sys.argv[1]) as apk:
  for a in assets:
   matches=re.findall(r'resource [^\n]+ drawable/'+a['resource']+r'\s*\n\s*\(nodpi\) \(file\) ([^\s]+)',table);assert len(matches)==1
   assert hashlib.sha256(apk.read(matches[0])).hexdigest()==a['sha256']
print('Immediate crest ordering, fixed AI voice, removed explanation/tone controls, payment/clear/widget wiring, bilingual copy and five original packaged plan assets passed (source checks, no native UI proof).')
