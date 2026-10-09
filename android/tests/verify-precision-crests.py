"""Production artwork wiring, bounded resources, update discovery and unchanged data contracts.

Optional APK arguments verify the exact ten approved resource payloads after packaging.
These checks do not claim Android screen rendering or PackageInstaller execution.
"""
from pathlib import Path
import hashlib, json, os, re, subprocess, sys
from zipfile import ZipFile

root = Path(__file__).resolve().parents[2]
app = root/'android/app/src/main/java/dev/bennett/codexmeter'
manifest = json.loads((root/'docs/precision-crests/ASSETS.json').read_text(encoding='utf-8'))
assets = manifest['assets']
assert len(assets) == 10 and [x['tier'] for x in assets] == list(range(10))
assert [x['name'] for x in assets] == ['Iron','Bronze','Silver','Gold','Platinum','Emerald','Diamond','Master','Grandmaster','Challenger']
assert len({x['sha256'] for x in assets}) == 10
assert sum(x['bytes'] for x in assets) < 4*1024*1024
for a in assets:
    raw = (root/a['path']).read_bytes()
    assert hashlib.sha256(raw).hexdigest() == a['sha256']
    assert len(raw) == a['bytes'] and raw[:4] == b'RIFF' and raw[8:12] == b'WEBP'
    assert raw[12:16] == b'VP8X' and raw[20] & 0x10, 'Actual alpha channel required'
    assert int.from_bytes(raw[24:27],'little')+1 == 512
    assert int.from_bytes(raw[27:30],'little')+1 == 512
    assert a['decodedArgbBytes'] == 1048576
theme = (app/'TierTheme.java').read_text(encoding='utf-8')
mapping = re.search(r'EMBLEMS=\{([^}]+)', theme).group(1)
assert re.findall(r'R.drawable.([a-z0-9_]+)', mapping) == [a['resource'] for a in assets]
assert 'R.drawable.evo_' not in theme and 'R.drawable.badge_' not in theme
assert 'frame.addView(badge' not in theme and 'ScaleType.FIT_CENTER' in theme
companion = (app/'TierCompanionView.java').read_text(encoding='utf-8')
assert 'setImageResource(TierTheme.EMBLEMS' in companion
assert 'ANIMATOR_DURATION_SCALE' in companion and 'onDetachedFromWindow' in companion
for name in ['FunHome','FunActivity']:
    source = (app/(name+'.java')).read_text(encoding='utf-8')
    assert 'new TierCompanionView(' in source and 'TierTheme.companion(' in source
widget = (app/'EvolutionWidget.java').read_text(encoding='utf-8')
assert 'TierTheme.EMBLEMS' in widget and 'Bitmap.createBitmap(144,144' in widget
main = (app/'MainActivity.java').read_text(encoding='utf-8')
assert 'GitHubRelease available=UpdatePreferences.availableUpdate(this);' in main
assert 'content.addView(buildUpdateCard(available))' in main
assert '+getSharedPreferences("codex_meter_updates_v1",0).getAll().hashCode()' in main
assert 'AppConstants.ACTION_RELEASES_UPDATED' in main
assert 'ReleaseUpdateScheduler.ensureScheduled(this)' in main
assert 'KOREAN_UPDATES_ENABLED", "true"' in (root/'android/app/build.gradle.kts').read_text()
# Existing authenticated/data/tier/updater settings code is compared to the actual 2.8.8 release.
protected = ['UsageApi','UsageParser','UsageLedgerDatabase','AccountSession','SecureTokenStore',
             'AppPreferences','SubscriptionStore','FunStore','TierStore','TierEvaluationScheduler',
             'UpdatePreferences','UpdateInstaller','ReleaseUpdateClient','ReleaseUpdateScheduler',
             'NowBarManager','MeterVisibility','WidgetOptions']
for name in protected:
    relative = f'android/app/src/main/java/dev/bennett/codexmeter/{name}.java'
    old = subprocess.check_output(['git','show','567ee1a:'+relative],cwd=root).replace(b'\r\n',b'\n')
    assert (root/relative).read_bytes().replace(b'\r\n',b'\n') == old, name
relative = 'android/shared/src/main/java/dev/bennett/codexmeter/TierEvolution.java'
assert (root/relative).read_bytes().replace(b'\r\n',b'\n') == subprocess.check_output(['git','show','567ee1a:'+relative],cwd=root).replace(b'\r\n',b'\n')
for argument in sys.argv[1:]:
    sdk = Path(os.environ.get('ANDROID_HOME') or os.environ['ANDROID_SDK_ROOT'])
    aapt = sdk/'build-tools/36.0.0'/('aapt2.exe' if os.name == 'nt' else 'aapt2')
    table = subprocess.check_output([str(aapt),'dump','resources',argument],text=True,encoding='utf-8').replace('\r\n','\n')
    with ZipFile(argument) as apk:
        for a in assets:
            # Release optimization shortens ZIP filenames; resolve the actual named resource table.
            matches = re.findall(r'resource [^\n]+ drawable/'+re.escape(a['resource'])+r'\s*\n\s*\(nodpi\) \(file\) ([^\s]+)',table)
            assert len(matches) == 1, (a['name'], matches)
            assert matches[0].startswith('res/') and matches[0].endswith('.webp')
            assert hashlib.sha256(apk.read(matches[0])).hexdigest() == a['sha256'], a['name']
print(f'10 transparent 512px crests, mapping/home/analysis/widget paths, cached update banner, 18 protected contracts and {len(sys.argv)-1} packaged APKs passed. Device UI/install not tested.')
