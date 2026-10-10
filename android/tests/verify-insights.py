#!/usr/bin/env python3
"""Run pure derived-analysis tests and JVM event-storage fixtures. No device proof."""
from pathlib import Path
import os
import shutil
import subprocess
import xml.etree.ElementTree as ET

root = Path(__file__).resolve().parents[1]
out = root / 'build/insight-verification'
out.mkdir(parents=True, exist_ok=True)
java_home = os.environ.get('JAVA_HOME')
javac = str(Path(java_home) / 'bin/javac') if java_home else shutil.which('javac')
java = str(Path(java_home) / 'bin/java') if java_home else shutil.which('java')
jar = root / 'build/test-libs/json-20250517.jar'
assert jar.exists(), 'Run the existing self-tests first to obtain their JSON dependency.'

fixtures = {
    'android/annotation/SuppressLint.java': '''package android.annotation;
public @interface SuppressLint { String[] value(); }''',
    'android/content/SharedPreferences.java': '''package android.content;
public interface SharedPreferences {
 String getString(String key, String fallback);
 Editor edit();
 interface Editor { Editor putString(String key, String value); Editor remove(String key); void apply(); boolean commit(); }
}''',
    'android/content/Context.java': '''package android.content;
import java.util.*;
import dev.bennett.codexmeter.*;
public class Context {
 public static final int MODE_PRIVATE = 0;
 public Context getApplicationContext() { return this; }
 public boolean failCommit;
 public boolean failAccess;
 public final Map<String,UsageHistory> histories = new HashMap<>();
 private final Map<String,SharedPreferences> stores = new HashMap<>();
 public SharedPreferences getSharedPreferences(String name, int mode) {
  if (failAccess) throw new IllegalStateException("fixture access failure");
  return stores.computeIfAbsent(name, key -> new Preferences());
 }
 private class Preferences implements SharedPreferences {
  final Map<String,String> values = new HashMap<>();
  public String getString(String key, String fallback) { return values.getOrDefault(key, fallback); }
  public Editor edit() { return new Editor() {
   final Map<String,String> pending = new HashMap<>();
   public Editor putString(String key, String value) { pending.put(key,value); return this; }
   public Editor remove(String key) { pending.put(key,null); return this; }
   public void apply() { for (Map.Entry<String,String> e : pending.entrySet()) {
    if (e.getValue() == null) values.remove(e.getKey()); else values.put(e.getKey(),e.getValue());
   } }
   public boolean commit() { if (failCommit) return false; apply(); return true; }
  }; }
 }
}''',
    'dev/bennett/codexmeter/AppPreferences.java': '''package dev.bennett.codexmeter;
public class AppPreferences {
 public static UsageHistory loadUsageHistory(android.content.Context c, String kind) {
  return c.histories.getOrDefault(kind, UsageHistory.empty(kind));
 }
 public static boolean saveUsageHistory(android.content.Context c, UsageHistory history) {
  c.histories.put(history.kind, history); return true;
 }
}''',
    'dev/bennett/codexmeter/DiagnosticLog.java': '''package dev.bennett.codexmeter;
public class DiagnosticLog { static void warn(android.content.Context c, String tag, String message) {} }''',
}
files = []
for name, text in fixtures.items():
    path = out / 'fixtures' / name
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(text, encoding='utf-8')
    files.append(str(path))
shared = root / 'shared/src/main/java/dev/bennett/codexmeter'
app = root / 'app/src/main/java/dev/bennett/codexmeter'
files += [str(shared / f'{name}.java') for name in ['UsageWindow', 'UsageSample', 'UsageHistory',
          'UsageSnapshot', 'UsageLimit', 'UsageCredits', 'UsageInsights', 'UsageEventDetector',
          'UsageLedger', 'UsageLedgerInsights', 'LedgerRecord', 'LedgerAggregation', 'LedgerForecast', 'LedgerExport']]
files += [str(app / f'{name}.java') for name in ['RateLimitResetCredit', 'ResetCreditsSnapshot',
          'UsageEventStore', 'UsageLedgerStore', 'UsageHistoryRecorder', 'LedgerCapture']]
files.append(str(Path(__file__).with_name('UsageInsightsSelfTest.java')))
files.append(str(Path(__file__).with_name('UsageLedgerSelfTest.java')))
files.append(str(Path(__file__).with_name('LedgerNextSelfTest.java')))
classes = out / 'classes'
classes.mkdir(exist_ok=True)
subprocess.run([javac, '-encoding', 'UTF-8', '-cp', str(jar), '-d', str(classes)] + files, check=True)
subprocess.run([java, '-ea', '-cp', str(classes) + os.pathsep + str(jar),
                'dev.bennett.codexmeter.UsageInsightsSelfTest'], check=True)
subprocess.run([java, '-ea', '-cp', str(classes) + os.pathsep + str(jar),
                'dev.bennett.codexmeter.UsageLedgerSelfTest'], check=True)

subprocess.run([java, '-ea', '-cp', str(classes) + os.pathsep + str(jar),
                'dev.bennett.codexmeter.LedgerNextSelfTest'], check=True)

# Production integration guards: preserve core requests, existing JSON, release identity and key.
assert not list(app.glob('ChatPro*.java')), 'Fixed policy tracker must not ship.'
for directory in [app, shared, root / 'app/src/main/res']:
    for path in directory.rglob('*'):
        if path.suffix in ('.java', '.xml'):
            assert 'chat_pro' not in path.read_text(encoding='utf-8').lower(), path
for name, needle in [
        ('UsageHistoryRecorder', 'UsageEventStore.recordUsage(context, snapshot)'),
        ('ResetCreditApi', 'UsageEventStore.recordConfirmedCreditUse(app'),
        ('AppPreferences', 'if (saved) UsageEventStore.recordCredits'),
        ('ReleaseUpdateClient', 'if (!BuildConfig.KOREAN_UPDATES_ENABLED)'),
        ('UpdateInstaller', 'if (!BuildConfig.KOREAN_UPDATES_ENABLED)'),
        ('UpdatePreferences', 'if (!BuildConfig.KOREAN_UPDATES_ENABLED) return Collections.emptyList()')]:
    assert needle in (app / f'{name}.java').read_text(encoding='utf-8'), name
build = (root / 'app/build.gradle.kts').read_text(encoding='utf-8')
assert 'applicationId = "dev.bennett.codexmeter"' in build
assert 'versionCode = 48' in build and 'versionName = "2.8.18"' in build
assert 'buildConfigField("boolean", "KOREAN_UPDATES_ENABLED", "true")' in build
key_file = root / '.local-signing/codex-meter-local.p12'
if os.environ.get('CODEX_METER_REQUIRE_SIGNED_RELEASE') == '1':
    assert key_file.is_file(), 'Do not generate a replacement key.'
elif not key_file.exists():
    guard = subprocess.run([os.environ.get('BASH_EXE', 'bash'), str(root / 'build.sh')], capture_output=True, text=True)
    assert guard.returncode != 0 and 'refusing to create a replacement key' in guard.stderr
    assert not key_file.exists(), 'Unsigned validation must never generate signing keys.'
    assert 'takeIf { it.storeFile != null }' in build
build_script = (root / 'build.sh').read_text(encoding='utf-8')
assert 'refusing to create a replacement key' in build_script
assert '-genkeypair' not in build_script and 'openssl rand' not in build_script
# Resource extraction must preserve the existing picker order and English copy.
arrays = ET.parse(root / 'app/src/main/res/values/widget_labels.xml').getroot()
for name, expected in [('widget_metric_labels', ['Both windows', '5-hour only', 'Weekly only']),
                       ('widget_style_labels', ['Adaptive by size', 'Dials', 'Progress bars'])]:
    node = next(n for n in arrays if n.attrib.get('name') == name)
    assert [item.text for item in node] == expected
assert 'OAuthBrowserText.render' in (app / 'OAuthService.java').read_text(encoding='utf-8')
assert 'OAuthBrowserPage.render' in (app / 'OAuthBrowserText.java').read_text(encoding='utf-8')
print('Pro removal, event observer wiring, updater gates and release identity checks passed.')
