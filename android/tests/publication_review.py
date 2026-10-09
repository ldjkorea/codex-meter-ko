"""Narrow 2.8.7 changes approved for existing whole-file source guards.
Original fixtures retain their independent hashes. No source guard is skipped.
"""
PATCHES = [('                    WidgetGraphics.dial(primaryProgress, iAccentColor, iTrackColor, iMainTextColor,\n', '                    WidgetGraphics.dial(context, primaryProgress, iAccentColor, iTrackColor, iMainTextColor,\n'), ('                    WidgetGraphics.dial(secondaryProgress, iAccentColor, iTrackColor, iMainTextColor,\n', '                    WidgetGraphics.dial(context, secondaryProgress, iAccentColor, iTrackColor, iMainTextColor,\n'), ('            String strReset = UsageFormat.reset(context, MeterVisibility.five(context,usageSnapshotLoadSnapshot),\n                    widgetOptions.resetMode, usageSnapshotLoadSnapshot.fetchedAtMillis,\n                    jCurrentTimeMillis);\n', '            String strReset = AppPreferences.showDashboardFiveHour(context)\n                    ? FiveHourResetDisplay.text(context,usageSnapshotLoadSnapshot.fiveHour,\n                    usageSnapshotLoadSnapshot.fetchedAtMillis,jCurrentTimeMillis,widgetOptions.resetMode,true) : "";\n'), ('            String strShortReset = WidgetRenderer.shortReset(context,\n                    MeterVisibility.five(context,usageSnapshotLoadSnapshot), widgetOptions.resetMode,\n                    usageSnapshotLoadSnapshot.fetchedAtMillis, jCurrentTimeMillis);\n', '            String strShortReset = AppPreferences.showDashboardFiveHour(context)\n                    ? FiveHourResetDisplay.text(context,usageSnapshotLoadSnapshot.fiveHour,\n                    usageSnapshotLoadSnapshot.fetchedAtMillis,jCurrentTimeMillis,widgetOptions.resetMode,true) : "";\n')]
def reviewed_widget_reset(path, raw):
    if not path.endswith('/WidgetRenderer.java'): return raw
    text=raw.decode('utf-8').replace('\r\n','\n')
    for before,after in PATCHES:
        assert text.count(before)==1, 'Publication patch no longer matches historical source'
        text=text.replace(before,after)
    return text.encode('utf-8')

def reviewed_evolution(path,raw):
    import json
    from pathlib import Path
    patches=json.loads(Path(__file__).with_name('evolution-reviewed-patches.json').read_text(encoding='utf-8'))
    if path not in patches:return raw
    text=raw.decode('utf-8').replace('\r\n','\n')
    for before,after in patches.get(path,[]):
        assert text.count(before)==1,(path,'Reviewed evolution patch mismatch',before[:90])
        text=text.replace(before,after)
    return text.encode('utf-8')

def strip_evolution_manifest(text):
    import re
    pattern=r'<activity android:name="dev.bennett.codexmeter.EvolutionWidgetConfigActivity"[\s\S]*?</receiver>\n    '
    assert len(re.findall(pattern,text))==1,'Additive evolution manifest registration missing'
    return re.sub(pattern,'',text)
