from publication_review import reviewed_widget_reset, reviewed_evolution
"""Wiring and asset checks. These are source checks, not Android screen rendering."""
from pathlib import Path
import baseline_subprocess as subprocess
import xml.etree.ElementTree as ET
root=Path(__file__).resolve().parents[1]
repo=root.parent
app=root/'app/src/main/java/dev/bennett/codexmeter'
def source(name):return (app/name).read_text(encoding='utf-8')
analytics=source('LedgerAnalyticsActivity.java')
assert 'R.string.ui_ledger_trends)},screen' in analytics and 'screen=recordsRoot()?2:Math.max(0,Math.min(1,screen))' in analytics
assert 'RecordsActivity' in source('MatteNav.java') and 'recordsRoot(){return true;}' in source('RecordsActivity.java')
assert 'screen==3' in analytics and 'FunActivity.class' in analytics
assert 'private void addValue' not in analytics
assert 'scrollTo(0,0)' not in analytics
for name in ['MainActivity.java','LedgerAnalyticsActivity.java','FunActivity.java']:
    text=source(name)
    assert 'NotesUi.prepare(' in text and 'NotesUi.flush(' in text and 'NotesUi.close(' in text,name
assert 'FunActivity.class' in source('SubscriptionUi.java')
assert 'menu.removeItem(ADD);menu.removeItem(LIST)' in source('NotesUi.java')
assert 'menu.add(' not in source('NotesUi.java')
assert 'TestNotesActivity.class' not in source('RecordSettings.java')
assert 'fun_test_mode' not in source('FunSettingsActivity.java')
assert 'MAIN.postDelayed(debounce,250)' in source('NotesUi.java')
assert 'WORKER.execute(' in source('NotesUi.java')
assert 'LiveCards.crest(' in source('FunActivity.java') and 'TierStore.evaluate(' in source('FunActivity.java')
assert 'getIdentifier' not in source('FunCoach.java')
new=['FunActivity.java','FunSettingsActivity.java','FunCoach.java','FunStore.java','NotesUi.java','TestNotesActivity.java','TestNotesStore.java']
for name in new:
    text=source(name)
    assert not any(term in text for term in ['HttpURLConnection','new URL(','OkHttpClient','consumeBestAvailable(','startActivityForResult(new Intent(this,Login']),name
# Usage collectors, database, account boundary, widget rendering and Wear source remain byte-identical.
paths=['android/app/src/main/java/dev/bennett/codexmeter/'+name for name in ['UsageApi.java','UsageParser.java','UsageLedgerDatabase.java','AccountSession.java','RefreshScheduler.java','SecureTokenStore.java','MeterVisibility.java','WidgetRenderer.java']]
paths+=subprocess.check_output(['git','ls-tree','-r','--name-only','e775fd9','android/wear/src'],cwd=repo,text=True).splitlines()
for path in paths:
    old=subprocess.check_output(['git','show','e775fd9:'+path],cwd=repo)
    old=reviewed_evolution(path,reviewed_widget_reset(path,old))
    assert (repo/path).read_bytes()==old or (repo/path).read_bytes().replace(b'\r\n',b'\n')==old.replace(b'\r\n',b'\n'),path
names=['iron','bronze','silver','gold','platinum','emerald','diamond','master','grandmaster','challenger']
geometry=set()
a='{http://schemas.android.com/apk/res/android}'
for name in names:
    vector=ET.parse(root/f'app/src/main/res/drawable/badge_{name}.xml').getroot()
    assert vector.tag=='vector'
    paths=tuple(item.get(a+'pathData') for item in vector.findall('path'))
    assert len(paths)>4
    geometry.add(paths)
    assert (repo/f'docs/badges-2.8.4/{name}.svg').exists()
assert len(geometry)==10,'Each badge must differ by geometry, not just color'
values={item.attrib['name']:item.text for item in ET.parse(root/'app/src/main/res/values/fun_strings.xml').getroot()}
ko={item.attrib['name']:item.text for item in ET.parse(root/'app/src/main/res/values-ko/fun_strings.xml').getroot()}
assert values.keys()==ko.keys()
assert all('coach_'+tone+'_' in source('FunCoach.java') for tone in ['calm','playful','spicy'])
assert len([key for key in values if key.startswith(('fun_coach_calm_','fun_coach_playful_','fun_coach_spicy_'))])==102
print('Fun navigation, draft lifecycle wiring, guarded core/Wear preservation, 10 distinct badge vectors and bilingual coach resources passed (source checks).')

