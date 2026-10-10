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
    if path in patches:
        text=raw.decode('utf-8').replace('\r\n','\n')
        for before,after in patches[path]:
            assert text.count(before)==1,(path,'Reviewed evolution patch mismatch',before[:90])
            text=text.replace(before,after)
        raw=text.encode('utf-8')
    # Explicitly approved 2.8.16 Wear launcher/name patch. Other bytes remain protected.
    prefix='android/wear/src/main/res/'
    if path in [prefix+'mipmap-anydpi/ic_launcher.xml',prefix+'mipmap-anydpi-v33/ic_launcher.xml']:
        return raw.replace(b'@drawable/codex_meter_adaptive_bg',b'@drawable/gpt_hud_launcher_background').replace(b'@drawable/codex_meter_adaptive_fg',b'@drawable/gpt_hud_launcher_foreground')
    if path==prefix+'drawable/ic_launcher_monochrome.xml':
        return b'<?xml version="1.0" encoding="utf-8"?>\n<bitmap xmlns:android="http://schemas.android.com/apk/res/android" android:gravity="fill" android:src="@drawable/gpt_hud_challenger_monochrome"/>\n'
    if path in [prefix+'values/strings.xml',prefix+'values-ko/strings.xml']:
        return raw.replace(b"Codex Meter",b"GPT HUD")
    return raw

def strip_evolution_manifest(text):
    import re
    trial='        <activity android:name="dev.bennett.codexmeter.TaskStatusActivity" android:exported="false"/>\n        <receiver android:name="dev.bennett.codexmeter.TaskStatusWidget" android:exported="false" android:label="@string/task_trial_widget">\n            <intent-filter><action android:name="android.appwidget.action.APPWIDGET_UPDATE"/></intent-filter>\n            <meta-data android:name="android.appwidget.provider" android:resource="@xml/task_status_widget_info"/>\n        </receiver>\n'
    assert text.count(trial)==1,'Trial activity/widget remain private, independent and read-only'
    text=text.replace(trial,'')
    service='        <service android:name="dev.bennett.codexmeter.LanWifiJobService"\n            android:permission="android.permission.BIND_JOB_SERVICE" android:exported="false"/>\n'
    assert text.count(service)==1,'Wi-Fi job must remain private and require BIND_JOB_SERVICE'
    text=text.replace(service,'')
    activity='        <activity android:name="dev.bennett.codexmeter.WhatsNewActivity" android:exported="false"/>\n'
    assert text.count(activity)==1,'Installed-version notes must stay private'
    text=text.replace(activity,'')
    pattern=r'<activity android:name="dev.bennett.codexmeter.EvolutionWidgetConfigActivity"[\s\S]*?</receiver>\n    '
    assert len(re.findall(pattern,text))==1,'Additive evolution manifest registration missing'
    return re.sub(pattern,'',text)
