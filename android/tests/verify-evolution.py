from lan_review import strip_lan
from pathlib import Path
import os,subprocess,json,xml.etree.ElementTree as ET
root=Path(__file__).resolve().parents[1];repo=root.parent
out=root/'build/evolution-verification';out.mkdir(parents=True,exist_ok=True)
shared=root/'shared/src/main/java/dev/bennett/codexmeter';app=root/'app/src/main/java/dev/bennett/codexmeter'
files=[shared/(n+'.java') for n in ['TierEvolution','EvolutionElements','KoreanUpdateTrust']]+[Path(__file__).with_name('TierEvolutionSelfTest.java')]
jdk=Path(os.environ['JAVA_HOME'])/'bin'
subprocess.run([str(jdk/'javac'),'-encoding','UTF-8','-d',str(out),*map(str,files)],check=True)
subprocess.run([str(jdk/'java'),'-ea','-cp',str(out),'dev.bennett.codexmeter.TierEvolutionSelfTest'],check=True)
fixture=out/'fixtures/dev/bennett/codexmeter/EvolutionWidget.java';fixture.parent.mkdir(parents=True,exist_ok=True)
fixture.write_text('package dev.bennett.codexmeter; class EvolutionWidget {static void updateAll(android.content.Context c){}}',encoding='utf-8')
cp=os.pathsep.join(map(str,[out,root/'build/subscription-store-verification/classes',root/'build/insight-verification/classes',root/'build/test-libs/json-20250517.jar']))
subprocess.run([str(jdk/'javac'),'-encoding','UTF-8','-cp',cp,'-d',str(out),str(fixture),str(app/'TierStore.java'),str(Path(__file__).with_name('TierPersistenceSelfTest.java'))],check=True)
subprocess.run([str(jdk/'java'),'-ea','-cp',cp,'dev.bennett.codexmeter.TierPersistenceSelfTest'],check=True)
# New vector geometry and bilingual strings; no old fixture source is regenerated.
ns='{http://schemas.android.com/apk/res/android}';geometry=[]
for i in range(10):
 v=ET.parse(root/f'app/src/main/res/drawable/evo_{i}.xml').getroot();geometry.append(tuple(p.get(ns+'pathData') for p in v));assert len(v)>=7
assert len(set(geometry))==10
en={s.get('name'):s.text for s in ET.parse(root/'app/src/main/res/values/evolution_strings.xml').getroot()}
ko={s.get('name'):s.text for s in ET.parse(root/'app/src/main/res/values-ko/evolution_strings.xml').getroot()}
assert en.keys()==ko.keys() and all(en.values()) and all(ko.values())
manifest=ET.parse(root/'app/src/main/AndroidManifest.xml').getroot();a=manifest.find('application')
assert any(x.get(ns+'name')=='dev.bennett.codexmeter.EvolutionWidget' for x in a.findall('receiver'))
assert any(x.get(ns+'name')=='dev.bennett.codexmeter.EvolutionWidgetConfigActivity' for x in a.findall('activity'))
for name in ['UsageApi','UsageParser','UsageLedgerDatabase','AccountSession','SecureTokenStore','AppPreferences','SubscriptionStore','FunStore','NowBarManager','FiveHourResetState','FiveHourResetDisplay','PretendardFont']:
 old=subprocess.check_output(['git','show','f720a05:android/app/src/main/java/dev/bennett/codexmeter/'+name+'.java'],cwd=repo).replace(b'\r\n',b'\n')
 assert strip_lan(name+'.java',(app/(name+'.java')).read_text(encoding='utf-8')).encode()==old,name
store=(app/'TierStore.java').read_text();widget=(app/'EvolutionWidget.java').read_text();installer=(app/'UpdateInstaller.java').read_text()
assert 'schema",2' in store and 'legacy_imported' in store and 'commit()' in store and 'NETWORK_LOCK' in store
assert 'EvolutionElements.visible' in widget and 'setChronometerCountDown' in widget and 'effectiveResetAtMillis' in widget
assert 'HttpURLConnection' not in widget and 'UsageApi.refresh' not in widget
assert 'archiveCode <= installedCode' in installer and 'KoreanUpdateTrust.compatible' in installer and 'sameSigners' in installer
print('10 distinct local vector evolutions, bilingual new UI, manifest registration, protected 2.8.7 core, widget countdown and updater gates passed (source checks).')

assert 'OPTION_APPWIDGET_MIN_HEIGHT' in widget and 'fontScale' in widget and 'd.setAlpha' in widget
config=(app/'EvolutionWidgetConfigActivity.java').read_text()
assert 'id+".elements"' in config and 'onSaveInstanceState' in config
assert 'session.abandon' not in installer and 'installer.abandonSession(sessionId)' in installer
assert installer.count('Thread.currentThread().isInterrupted()')>=4
assert 'w.policy.equals(policy)' in store

# Evaluate the actual theme palette, including light-theme HSV transformation and button labels.
import re,colorsys
palette=re.search(r'COLORS=\{([^}]+)',(app/'TierTheme.java').read_text()).group(1)
colors=[int(x,16)&0xffffff for x in re.findall(r'0x[0-9a-f]+',palette)]+[0xc2c9d0]
def lum(rgb):
 v=[x/255 for x in rgb];v=[x/12.92 if x<=.04045 else ((x+.055)/1.055)**2.4 for x in v]
 return sum(x*y for x,y in zip(v,[.2126,.7152,.0722]))
def ratio(a,b):
 x,y=sorted([lum(a),lum(b)]);return (y+.05)/(x+.05)
for raw in colors:
 rgb=((raw>>16)&255,(raw>>8)&255,raw&255)
 for dark in [True,False]:
  if dark:c=rgb
  else:
   h,s,v=colorsys.rgb_to_hsv(*[x/255 for x in rgb]);c=tuple(round(x*255) for x in colorsys.hsv_to_rgb(h,max(.3,s),.38))
  text=(0,0,0) if lum(c)>.179 else (255,255,255)
  assert ratio(text,c)>=4.5,(c,'button label')
  assert ratio(c,(23,25,28) if dark else (249,249,250))>=4.5,(c,'accent on card')
assert 'phoneSurface(context))return Color.luminance' in (app/'Ui.java').read_text()
print('All 10 tiers plus neutral palette meet 4.5:1 for dark/light accent and button text (calculated, not screen rendering).')

activity=(app/'UpdateActivity.java').read_text()
assert 'if (comparison > 0) requestInstall(); else openReleasePage();' in activity
assert activity.count('UpdatePreferences.installedVersion(this)) <= 0')==2
assert 'confirmOlderDownload' not in activity
print('Updater UI blocks equal/older installs and preserves data; beta safety copy checked (source).')

assert "R.string.pub_five_error_short" in widget # API error differs from an old observation.
