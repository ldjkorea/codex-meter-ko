"""Calendar paint/data navigation and payment refresh wiring; not Android screen proof."""
from pathlib import Path
import xml.etree.ElementTree as ET
root=Path(__file__).resolve().parents[1]
app=root/'app/src/main/java/dev/bennett/codexmeter'
s=(app/'LedgerAnalyticsActivity.java').read_text(encoding='utf-8')
assert 'LedgerPresentation.find(data.days,policy,date)' in s
assert 'LedgerCalendar.emerald(day)' in s and 'new CalendarGlowDrawable(' in s
assert 'selectedDate=date;render();' in s and '()->detail(selectedDate)' in s
assert 'selectedDate=month.atDay(1);render();' in s and 'getString(R.string.next_day_counts,day.count,day.uncertain)' in s
assert 'text.setSelected(date.equals(selectedDate))' in s
assert 'calendar_activity_highlight' in s and 'text.setContentDescription' in s
main=(app/'MainActivity.java').read_text(encoding='utf-8')
assert 'getSharedPreferences("codex_subscription_cost",0).getAll().hashCode()' in main
assert 'protected void onResume()' in main and 'rebuild();' in main
for name in ['FunHome','FunActivity']:
 assert 'SubscriptionValueUi.card(' in (app/(name+'.java')).read_text(encoding='utf-8')
value=(app/'SubscriptionValueUi.java').read_text(encoding='utf-8')
assert 'SubscriptionStore.load(a,snapshot)' in value and 'SubscriptionValue.calculate(bill,span,percent,fresh,safe)' in value
ui=(app/'SubscriptionUi.java').read_text(encoding='utf-8')
assert 'SubscriptionStore.save(' in ui and 'dialog.dismiss();refresh.run();' in ui
paint=(app/'CalendarGlowDrawable.java').read_text(encoding='utf-8')
assert 'selected ? 2 : 1' in paint and 'today ?' in paint
assert all(x not in paint for x in ['Animator','Bitmap','setLayerType'])
def luminance(rgb):
 channels=[((x/255+0.055)/1.055)**2.4 if x/255>0.04045 else x/255/12.92 for x in rgb]
 return sum(x*w for x,w in zip(channels,[0.2126,0.7152,0.0722]))
contrast=(luminance((209,250,229))+0.05)/(luminance((8,118,83))+0.05)
assert contrast>=4.5,contrast
for locale in ['values','values-ko']:
 rows=ET.parse(root/'app/src/main/res'/locale/'calendar_strings.xml').getroot()
 assert len(rows)==1 and rows[0].attrib['name']=='calendar_activity_highlight'
 assert '13' not in ''.join(rows[0].itertext())
print(f'Calendar SQLite/date selection/month/detail/accessibility/payment refresh wiring passed; emerald text contrast {contrast:.2f}:1. Native UI not tested.')
