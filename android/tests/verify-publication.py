#!/usr/bin/env python3
"""Offline font/provenance checks for the public source and optional final APKs."""
from pathlib import Path
from zipfile import ZipFile, is_zipfile
import hashlib, io, json, sys, xml.etree.ElementTree as ET

root = Path(__file__).resolve().parents[2]
manifest = json.loads((root/'licenses/PRETENDARD_PROVENANCE.json').read_text(encoding='utf-8'))
sha = lambda data: hashlib.sha256(data).hexdigest()
for font in manifest['fonts']:
    assert sha((root/font['path']).read_bytes()) == font['sha256'], font['path']
assert sha((root/'licenses/PRETENDARD_OFL.txt').read_bytes()) == manifest['license']['sha256']
for patch in manifest['patched_dependencies']:
    path = root/patch['path']
    assert sha(path.read_bytes()) == patch['patched_aar_sha256'], str(path)
    with ZipFile(path) as z:
        assert sha(z.read(patch['replaced_entry'])) == patch['replacement_sha256']
        assert sha(z.read('META-INF/licenses/PRETENDARD_OFL.txt')) == manifest['license']['sha256']
for module in ['app','wear']:
    assets = root/f'android/{module}/src/main/assets/licenses'
    assert (assets/'PRETENDARD_OFL.txt').read_bytes() == (root/'licenses/PRETENDARD_OFL.txt').read_bytes()
    assert (assets/'CODEX-METER-MIT.txt').read_bytes() == (root/'LICENSE').read_bytes()
for folder in ['values','values-night']:
    styles = (root/f'android/app/src/main/res/{folder}/styles.xml').read_text(encoding='utf-8')
    assert '<item name="android:fontFamily">@font/pretendard_family</item>' in styles
for p in (root/'android/app/src/main/res').rglob('*.xml'):
    assert 'fontFamily="sec"' not in p.read_text(encoding='utf-8'), p
family = ET.parse(root/'android/app/src/main/res/font/pretendard_family.xml').getroot()
ns = '{http://schemas.android.com/apk/res/android}'
assert [n.get(ns+'fontWeight') for n in family] == ['400','500','600','700']
for name in ['WidgetOptions','AppPreferences','UsageParser','UsageApi','OAuthClient','SecureTokenStore','UsageLedgerDatabase','NowBarManager']:
    # The complete regression suites protect these original storage/auth contracts.
    assert (root/f'android/app/src/main/java/dev/bennett/codexmeter/{name}.java').exists()
forbidden = {p['original_font_sha256'] for p in manifest['patched_dependencies']}
archives = 0
def inspect_archive(raw, label):
    global archives
    archives += 1
    with ZipFile(io.BytesIO(raw)) as z:
        for n in z.namelist():
            if n.endswith('/'): continue
            data=z.read(n)
            assert sha(data) not in forbidden, label+'!'+n
            if n.lower().endswith(('.aar','.jar','.zip')) and is_zipfile(io.BytesIO(data)):
                inspect_archive(data,label+'!'+n)
            if n.lower().endswith(('.otf','.ttf')):
                assert data[:4] in [b'OTTO',b'\x00\x01\x00\x00',b'ttcf'], label+'!'+n
for path in (root/'android/vendor').rglob('*'):
    if path.is_file() and path.suffix in ['.aar','.jar']:
        inspect_archive(path.read_bytes(),str(path.relative_to(root)))
for arg in sys.argv[1:]:
    path=Path(arg)
    inspect_archive(path.read_bytes(),path.name)
    with ZipFile(path) as z:
        assert sha(z.read('assets/licenses/PRETENDARD_OFL.txt')) == manifest['license']['sha256']
        assert z.read('assets/licenses/CODEX-METER-MIT.txt') == (root/'LICENSE').read_bytes()
        packed_fonts={sha(z.read(n)) for n in z.namelist() if n.lower().endswith(('.ttf','.otf'))}
        if 'Wear' not in path.name:
            assert {font['sha256'] for font in manifest['fonts']} <= packed_fonts
        assert not packed_fonts.intersection(forbidden)
print(json.dumps({'official_font_hashes':'passed','weight_family':'400/500/600/700',
    'patched_dependencies':2,'forbidden_font_hashes_absent':'passed','archives_scanned':archives,
    'apk_count':len(sys.argv)-1,'native_device_render':'not performed'}))
