"""Verify actual palette code and additive layouts; does not claim native UI rendering."""
from pathlib import Path
import os,re,subprocess,xml.etree.ElementTree as ET

repo=Path(__file__).resolve().parents[2]
android=repo/'android';app=android/'app/src/main/java/dev/bennett/codexmeter'
out=android/'build/widget-tier-center-verification';out.mkdir(parents=True,exist_ok=True)
jdk=Path(os.environ['JAVA_HOME'])/'bin'
policy=android/'shared/src/main/java/dev/bennett/codexmeter/WidgetTierPalette.java'
subprocess.run([str(jdk/'javac'),'-encoding','UTF-8','-d',str(out),str(policy),str(Path(__file__).with_name('WidgetTierPaletteSelfTest.java'))],check=True)
subprocess.run([str(jdk/'java'),'-ea','-cp',str(out),'dev.bennett.codexmeter.WidgetTierPaletteSelfTest'],check=True)
ns='{http://schemas.android.com/apk/res/android}'
for name in ['rings','rings_four','dials','dials_large','dials_max','rings_large','rings_max']:
    xml=ET.parse(android/f'app/src/main/res/layout/widget_{name}.xml').getroot()
    parent=next(p for p in xml.iter() if any(c.get(ns+'id')=='@+id/widget_tier_emblem' for c in p))
    ids=[c.get(ns+'id') for c in parent]
    assert ids.index('@+id/primary_section')<ids.index('@+id/widget_tier_emblem')<ids.index('@+id/secondary_section'),name
    emblem=next(c for c in parent if c.get(ns+'id')=='@+id/widget_tier_emblem')
    assert parent.get(ns+'orientation')=='horizontal'
    assert emblem.get(ns+'layout_width')=='0dp' and emblem.get(ns+'layout_weight')=='1.2'
    assert emblem.get(ns+'visibility')=='gone' and emblem.get(ns+'contentDescription')=='@string/fun_tier'
palette=policy.read_text(encoding='utf-8')
color=lambda key:[int(x,16) for x in re.findall('0x([0-9a-f]+)',re.search(key+r' = \{([^}]+)',palette).group(1))]
for tier in range(10):
    for opacity in [56,88,100]:
        xml=ET.parse(android/f'app/src/main/res/drawable/widget_tier_{tier}_{opacity}.xml').getroot()
        gradient=xml.find('gradient');alpha=int(opacity*2.55+.5)
        assert gradient.get(ns+'startColor')==f'#{alpha:02x}{color("TOP")[tier]&0xffffff:06x}'
        assert gradient.get(ns+'endColor')==f'#{alpha:02x}{color("BOTTOM")[tier]&0xffffff:06x}'
        assert xml.find('corners').get(ns+'radius')=='28dp'
        assert xml.find('stroke').get(ns+'width')=='1dp'
surface=(app/'WidgetTierSurface.java').read_text(encoding='utf-8')
assert re.findall(r'R.drawable.widget_tier_(\d+)_(\d+)',surface)==[(str(t),str(o)) for t in range(10) for o in [56,88,100]]
renderer=(app/'WidgetRenderer.java').read_text(encoding='utf-8')
config=(app/'WidgetConfigActivity.java').read_text(encoding='utf-8')
assert 'WidgetTierSurface.resource(tier,widgetOptions.opacity)' in renderer
assert 'WidgetTierSurface.resource(tier,options.opacity)' in config
assert 'resolveSlots(context, widgetOptions, widgetStateFrom, str, bundle)' in renderer, 'Narrow rendering must not add extra selected metrics'
assert 'WidgetCrest.bind(context,remoteViews,crest)' in renderer and 'resolveGraphicTier(context,options,size),crest' in renderer
assert 'tierSurface || chooseDark(context, widgetOptions)' in renderer
assert 'setContentDescription(graphicIds[index],slot.label+": "+slot.valueText)' in renderer
for p in [policy,app/'WidgetTierSurface.java']:
    text=p.read_text(encoding='utf-8')
    assert not any(s in text for s in ['UsageApi','HttpURLConnection','SharedPreferences','commit()','UsageLedgerDatabase'])
print('Seven central crest layouts, 30 rounded opacity resources, draft/launcher shared theme and unchanged slot selection passed. Native screen proof not performed.')
