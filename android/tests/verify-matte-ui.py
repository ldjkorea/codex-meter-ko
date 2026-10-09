from publication_review import reviewed_widget_reset, reviewed_evolution
"""Matte phone UI contracts and WCAG contrast. Does not simulate native rendering."""
from pathlib import Path
import json,re,xml.etree.ElementTree as ET
import baseline_subprocess as subprocess
root=Path(__file__).resolve().parents[1];repo=root.parent
app=root/'app/src/main/java/dev/bennett/codexmeter'
a='{http://schemas.android.com/apk/res/android}'
def src(name):return (app/(name+'.java')).read_text(encoding='utf-8')
main,nav,analytics,home,settings,ui=map(src,['MainActivity','MatteNav','LedgerAnalyticsActivity','FunHome','RecordSettings','Ui'])
manifest=ET.parse(root/'app/src/main/AndroidManifest.xml').getroot()
activities={item.get(a+'name'):item for item in manifest.find('application').findall('activity')}
for target in ['MainActivity','LedgerAnalyticsActivity','RecordsActivity','DashboardReorderActivity','SettingsActivity','FunSettingsActivity','FunActivity','TestNotesActivity','MeterSettingsActivity','AboutActivity']:
    assert 'dev.bennett.codexmeter.'+target in activities,('Navigation target is not registered',target)
assert activities['dev.bennett.codexmeter.RecordsActivity'].get(a+'exported')=='false'
assert nav.index('R.string.matte_analysis')<nav.index('R.string.matte_home')<nav.index('R.string.matte_records')
assert 'MatteNav.install(this,1)' in main
assert 'FLAG_ACTIVITY_REORDER_TO_FRONT|Intent.FLAG_ACTIVITY_SINGLE_TOP' in nav
assert 'moveTaskToBack(true)' in nav and 'else open(activity,1)' in nav
assert 'matte_return' in nav and 'dialog.setOnDismissListener' in main
assert '!(context instanceof WidgetConfigActivity)' in ui and '!(context instanceof LockWidgetConfigActivity)' in ui
assert 'if(phoneSurface(context))return' in ui and 'if(phoneSurface(context)&&' in ui
assert 'screen=recordsRoot()?2:Math.max(0,Math.min(1,screen))' in analytics
assert 'protected boolean recordsRoot(){return true;}' in src('RecordsActivity')
assert 'home_scroll' in main and 'restoreHomePending' in main and 'ACTION_DOWN' in main
for state in ['selected_date','policy','period','screen','month','scroll','groups']:
    assert '"'+state+'"' in analytics,state
assert 'request!=loadGeneration' in analytics and '!requestedKey.equals(pageKey())' in analytics
assert 'SubscriptionStore.key(this,s)' in analytics and 'Ui.isDark(this)!=dark' in analytics
assert 'codex_subscription_cost' in main and 'codex_fun_settings' in main
assert 'button.setEnabled(true);button.setText(previousLabel)' in main
assert 'previousLabel.length()>0' in main # no clipped spinner text in compact icon control
assert main.index('LedgerDashboard.addOverview')<main.index('FunHome.add')
assert main.index('content.addView(crestSlot)')<main.index('LedgerDashboard.addOverview')
assert home.index('crestTarget.addView(tier)')<home.index('target.addView(quotation)')<home.index('target.addView(value)')
assert 'LiveCards.ai(' in home and 'FunStore.tone(' not in home
assert 'LiveCards.crest(' in home and 'TierStore.evaluate(' in home
assert 'SubscriptionValueUi.card' in home and 'FunStore.rule(' not in home
assert 'LiveUsageStore.calculate(' in home
assert 'screenWidthDp<420' in home and 'figures.setOrientation(LinearLayout.VERTICAL)' in src('SubscriptionValueUi')
assert 'setMaxLines' not in home and 'setEllipsize' not in home
assert 'SpicyAi.key(' in src('LiveCards') and 'R.string.live_ai_title' in src('LiveCards')
records=settings.split('static void add(',1)[1].split('static void hub(',1)[0]
assert records.count('toggle(a,')==3
assert 'MeterSettingsActivity.class' in records and '=group(' not in settings
assert 'TestNotesStore' not in settings and 'TestNotesActivity.class' not in settings
for page in ['appearance','refresh_usage','notifications','now_bar','updates','transfer','privacy','diagnostics']:
    assert '"'+page+'"' in settings,page
for setter in ['setShowDashboardFiveHour','setShowDashboardWeekly','setShowUsageOverview']:
    assert 'AppPreferences.'+setter+'(a,on)' in settings
assert 'SubscriptionUi.edit' in settings and 'R.string.ux_payment_history' in settings
assert 'clear);' in settings and 'details);' in settings
assert not any(x in home+settings+nav for x in ['HttpURLConnection','OkHttpClient','consumeBestAvailable(','new URL('])
layout=ET.parse(root/'app/src/main/res/layout/activity_oneui_dashboard.xml').getroot()
assert layout.tag=='LinearLayout' and layout.get(a+'orientation')=='vertical'
assert layout[0].get(a+'layout_weight')=='1'
assert layout[1].get(a+'id')=='@+id/matte_bottom' # fixed sibling of the scrolling area
assert 'setMinHeight(Ui.dp(activity,64))' in nav and 'setMinHeight(Ui.dp(a,48))' in settings
assert 'ViewCompat.setStateDescription' in nav and 'SwitchCompat toggle' in settings
resources=root/'app/src/main/res'
en={x.get('name'):''.join(x.itertext()) for x in ET.parse(resources/'values/matte_strings.xml').getroot()}
ko={x.get('name'):''.join(x.itertext()) for x in ET.parse(resources/'values-ko/matte_strings.xml').getroot()}
assert en.keys()==ko.keys() and all(en.values()) and all(ko.values())
assert en['matte_analysis']=='Analysis' and ko['matte_home']=='홈'
# Evaluate the colors actually returned by the production Ui methods, not a separate palette fixture.
def colors(method):
    body=ui.split('public static int '+method+'(',1)[1].split('\n    }',1)[0]
    return [tuple(map(int,m)) for m in re.findall(r'Color.rgb\((\d+),\s*(\d+),\s*(\d+)\)',body)]
def lum(c):
    v=[x/255 for x in c];v=[x/12.92 if x<=.04045 else ((x+.055)/1.055)**2.4 for x in v]
    return sum(x*y for x,y in zip(v,[.2126,.7152,.0722]))
def contrast(f,b):
    x,y=sorted([lum(f),lum(b)]);return (y+.05)/(x+.05)
secondary=colors('secondaryText');surfaces=[colors('background')[0],colors('cardColor')[2],colors('controlSurface')[0]]
assert colors('mainText')[0]==(245,245,245)
assert surfaces[0]==(13,14,16) and surfaces[1]==(23,25,28) and surfaces[2]==(32,34,38)
ratios={str(b):round(contrast(secondary[0],b),2) for b in surfaces}
assert all(x>=4.5 for x in ratios.values()),ratios
assert contrast(secondary[1],(242,243,245))>=4.5
# Stronger than UI snapshots: all computation, stores, network, device-surface code are unchanged from 2.8.4.
prefix='android/app/src/main/java/dev/bennett/codexmeter/'
paths=[prefix+name+'.java' for name in ['FunStore','TestNotesStore','SubscriptionStore','UsageApi','UsageParser','UsageLedgerDatabase','AccountSession','SecureTokenStore','AppPreferences','MeterVisibility','WidgetRenderer','WidgetOptions','NowBarManager','RefreshScheduler']]
paths+=subprocess.check_output(['git','ls-tree','-r','--name-only','e86c69c','android/shared/src','android/wear/src'],cwd=repo,text=True).splitlines()
for path in paths:
    old=subprocess.check_output(['git','show','e86c69c:'+path],cwd=repo).replace(b'\r\n',b'\n')
    old=reviewed_evolution(path,reviewed_widget_reset(path,old))
    assert (repo/path).read_bytes().replace(b'\r\n',b'\n')==old,path
result={'type':'source/resource/contrast checks, not native UI','protected_files':len(paths),'dark_secondary_contrast':ratios,'native_device_render':'not performed'}
(root/'build/matte-ui-verification').mkdir(parents=True,exist_ok=True)
(root/'build/matte-ui-verification/result.json').write_text(json.dumps(result,indent=2),encoding='utf-8')
print('Matte navigation/state, separate settings hub, payment-based value wiring, bilingual resources, protected core and WCAG contrast passed (source checks).')
print(json.dumps(result))
