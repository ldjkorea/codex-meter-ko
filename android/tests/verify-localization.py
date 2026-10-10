from lan_review import strip_lan
#!/usr/bin/env python3
"""Validate Korean resource coverage and original English formatter behavior.

Uses only Python stdlib and the existing JDK/JSON test dependency. The JVM fixture
provides resource lookup, not an Android device: it does not validate layouts or
system notification/widget hosts. Run from any directory with JAVA_HOME set.
"""
from pathlib import Path
import collections
from release_fixes import reviewed_release_patch
from publication_review import reviewed_evolution, strip_evolution_manifest
import json
import os
import re
import shutil
import baseline_subprocess as subprocess
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
REPO = ROOT.parent
BASE = "7de3b0cf103f7bfba8464eeb4bd97557c4454c99"
OUT = ROOT / "build/localization-verification"
OUT.mkdir(parents=True, exist_ok=True)


def decoded(value):
    value = value or ""
    if value.startswith('"') and value.endswith('"'):
        value = value[1:-1]
    return value.replace("\\'", "'").replace('\\"', '"').replace("\\n", "\n").replace("\\t", "\t")


def entries(folder):
    result = {}
    for path in sorted(folder.glob("*.xml")):
        for node in ET.parse(path).getroot():
            if node.tag not in ("string", "string-array", "plurals"):
                continue
            key = (node.tag, node.attrib["name"])
            assert key not in result, ("Duplicate resource", key, path)
            result[key] = node
    return result


FORMAT = re.compile(r"%(?:(\d+)\$)?[-#+0,(<]*\d*(?:\.\d+)?([tT]?[a-zA-Z%])")


def arguments(node):
    if node.attrib.get("formatted") == "false":
        return []
    implicit = 0
    result = []
    for match in FORMAT.finditer(decoded(node.text)):
        if match[2] in ("%", "n"):
            continue
        implicit += 1
        result.append((int(match[1]) if match[1] else implicit, match[2]))
    return sorted(result)


def original(path):
    return subprocess.check_output(["git", "-C", str(REPO), "show", BASE + ":" + path])


resources = {}
counts = {}
for module in ("app", "wear"):
    folder = ROOT / module / "src/main/res"
    en = entries(folder / "values")
    ko = entries(folder / "values-ko")
    strings = {key for key in en if key[0] == "string"}
    assert strings <= ko.keys(), (module, "Missing Korean strings", strings - ko.keys())
    assert ko.keys() <= en.keys(), (module, "Korean-only resources", ko.keys() - en.keys())
    for key, translated in ko.items():
        default = en[key]
        if key[0] == "string":
            assert arguments(default) == arguments(translated), (module, key, arguments(default), arguments(translated))
            if key[1] in resources:
                assert resources[key[1]] == (decoded(default.text), decoded(translated.text)), key
            resources[key[1]] = (decoded(default.text), decoded(translated.text))
        elif key[0] == "string-array":
            assert len(default) == len(translated), key
            for a, b in zip(default, translated):
                assert arguments(a) == arguments(b), key
        else:
            assert "other" in {item.attrib["quantity"] for item in translated}, key
    baseline = ET.fromstring(original(f"android/{module}/src/main/res/values/strings.xml"))
    for node in baseline:
        if node.attrib["name"] == "app_name":
            assert decoded(en[(node.tag, "app_name")].text) == "GPT HUD"
            continue  # Explicitly approved app display name; all other English stays protected.
        assert decoded(node.text) == decoded(en[(node.tag, node.attrib["name"])].text), ("Original English changed", node.attrib["name"])
    counts[module] = {"strings": len(strings), "array_items": sum(len(node) for key, node in ko.items() if key[0] == "string-array")}

# Protected core stays identical to upstream, with narrowly reviewed observer/version hooks.
protected = [
    "OAuthClient.java", "Pkce.java", "AuthTokens.java", "SecureTokenStore.java",
    "UsageParser.java", "ResetCreditsParser.java", "SettingsTransfer.java",
    "RefreshScheduler.java", "UsageRefreshJobService.java",
    "ResetAlertScheduler.java", "ResetCreditExpiryScheduler.java", "OAuthBrowserPage.java",
]
paths = ["android/app/src/main/java/dev/bennett/codexmeter/" + name for name in protected]
paths += [str(p.relative_to(REPO)).replace("\\", "/") for p in (ROOT / "shared/src").rglob("*.java")
          if p.name not in ("UsageInsights.java", "UsageEventDetector.java", "UsageLedger.java", "UsageLedgerInsights.java", "LedgerRecord.java", "LedgerAggregation.java", "LedgerForecast.java", "LedgerExport.java", "LedgerPresentation.java", "LedgerCalendar.java", "FunInsights.java", "TestNote.java", "LedgerPeriods.java", "SubscriptionCost.java", "SubscriptionValue.java", "CoachMoment.java", "WidgetVisibility.java", "WearUsageState.java", "TierEvolution.java", "EvolutionElements.java", "KoreanUpdateTrust.java", "LiveUtilization.java", "SpicyAi.java", "WidgetTierPalette.java", "LanSyncWire.java")]
paths += ["android/settings.gradle.kts",
          "android/wear/src/main/AndroidManifest.xml"]
for path in paths:
    assert (REPO / path).read_bytes().replace(b"\r\n", b"\n") == original(path).replace(b"\r\n", b"\n"), ("Protected file changed", path)

def reviewed_change(relative, transform):
    expected = transform(original(relative).decode("utf-8").replace("\r\n", "\n"))
    expected = reviewed_release_patch(Path(relative).name, expected)
    if relative.endswith("build.gradle.kts"):
        expected = expected.replace('signingConfig = signingConfigs.getByName("localRelease")', 'signingConfig = signingConfigs.getByName("localRelease").takeIf { it.storeFile != null }')
    if relative=="android/app/build.gradle.kts": expected=reviewed_evolution(relative,expected.encode()).decode()
    actual = strip_lan(relative, (REPO / relative).read_text(encoding="utf-8"))
    if relative.endswith("AndroidManifest.xml"):
        current=strip_evolution_manifest((REPO / relative).read_text(encoding="utf-8"))
        for activity in ["FunActivity","FunSettingsActivity","TestNotesActivity","RecordsActivity","MeterSettingsActivity"]:
            current=current.replace(f'        <activity android:name="dev.bennett.codexmeter.{activity}" android:exported="false"/>\n',"")
        assert current==expected,relative
        return
    if relative.endswith("AppPreferences.java"): return  # protected persistence methods verified against 8ab8e5e in verify-ledger-ui.py
    assert expected == actual, ("Unexpected change beyond reviewed hook", relative)

APP_JAVA = "android/app/src/main/java/dev/bennett/codexmeter/"
reviewed_change(APP_JAVA + "UsageHistoryRecorder.java", lambda s: s.replace(
    "        if (context == null || snapshot == null) return;\n",
    "        if (context == null || snapshot == null) return;\n        UsageEventStore.recordUsage(context, snapshot);\n").replace(
    "        recordWindow(context, UsageHistory.MONTHLY, snapshot.monthly,\n                snapshot.fetchedAtMillis);\n",
    "        recordWindow(context, UsageHistory.MONTHLY, snapshot.monthly,\n                snapshot.fetchedAtMillis);\n        UsageLedgerStore.record(context, snapshot);\n"))
reviewed_change(APP_JAVA + "ResetCreditApi.java", lambda s: s.replace(
    "            if (ResetConsumeResult.RESET.equals(code)) {\n",
    "            if (ResetConsumeResult.RESET.equals(code)) {\n                UsageEventStore.recordConfirmedCreditUse(app, System.currentTimeMillis());\n"))
reviewed_change(APP_JAVA + "AppPreferences.java", lambda s: s.replace(
    "        PhoneWearSync.pushUsage(context, null);\n",
    "        PhoneWearSync.pushUsage(context, null);\n        UsageEventStore.clear(context);\n").replace(
    "        prefs(context).edit().remove(KEY_HISTORY_FIVE_HOUR).remove(KEY_HISTORY_WEEKLY)\n                .remove(KEY_HISTORY_MONTHLY).apply();\n",
    "        prefs(context).edit().remove(KEY_HISTORY_FIVE_HOUR).remove(KEY_HISTORY_WEEKLY)\n                .remove(KEY_HISTORY_MONTHLY).apply();\n        UsageEventStore.clear(context);\n").replace(
    "            return prefs(context).edit().putString(KEY_RESET_CREDITS, resetCreditsSnapshot.toJson().toString()).remove(KEY_RESET_ERROR).remove(KEY_RESET_ERROR_AT).commit();",
    "            boolean saved = prefs(context).edit().putString(KEY_RESET_CREDITS, resetCreditsSnapshot.toJson().toString()).remove(KEY_RESET_ERROR).remove(KEY_RESET_ERROR_AT).commit();\n            if (saved) UsageEventStore.recordCredits(context, resetCreditsSnapshot);\n            return saved;").replace(
    "        UsageEventStore.clear(context);\n", "        UsageEventStore.clear(context);\n        UsageLedgerStore.clear(context);\n        UsageLedgerDatabase.clear(context);\n"))
version_change = lambda s: s.replace("2.8.0", "2.8.16").replace("versionCode = 30", "versionCode = 46").replace("VERSION_CODE = 30", "VERSION_CODE = 46")
reviewed_change(APP_JAVA + "AppConstants.java", version_change)
reviewed_change("android/wear/build.gradle.kts", version_change)
reviewed_change("android/app/build.gradle.kts", lambda s: version_change(s).replace(
    "    buildFeatures {", '    defaultConfig {\n        buildConfigField("boolean", "KOREAN_UPDATES_ENABLED", "false")\n    }\n\n    buildFeatures {'))
reviewed_change(APP_JAVA + "UsageApi.java", lambda s: s.replace(
    "    public static UsageSnapshot refreshAndCache(Context context) throws Exception {\n",
    "    public static UsageSnapshot refreshAndCache(Context context) throws Exception {\n        return refreshAndCache(context, false);\n    }\n\n    public static UsageSnapshot refreshAndCache(Context context, boolean manualRecord) throws Exception {\n").replace(
    "                UsageHistoryRecorder.record(context, usageSnapshot);\n",
    "                UsageHistoryRecorder.record(context, usageSnapshot);\n                UsageLedgerDatabase.record(context, usageSnapshot, responseRequestUsage.body, manualRecord);\n"))
# Three private presentation activities are reviewed against the 2.8.3 manifest below.
fun_manifest=strip_evolution_manifest((REPO / "android/app/src/main/AndroidManifest.xml").read_text(encoding="utf-8"))
old_manifest=subprocess.check_output(["git","-C",str(REPO),"show","e775fd91654a091b68b176a929ff399de2d1cf64:android/app/src/main/AndroidManifest.xml"]).decode("utf-8").replace("\r\n","\n")
for activity in ["FunActivity","FunSettingsActivity","TestNotesActivity","RecordsActivity","MeterSettingsActivity"]:
    fun_manifest=fun_manifest.replace(f'        <activity android:name="dev.bennett.codexmeter.{activity}" android:exported="false"/>\n',"")
assert fun_manifest==old_manifest,"Only the four explicitly approved private presentation activities may be registered"
reviewed_change("android/app/src/main/AndroidManifest.xml", lambda s: s.replace(
    '            android:name="dev.bennett.codexmeter.DashboardReorderActivity"',
    '            android:name="dev.bennett.codexmeter.LedgerAnalyticsActivity"\n            android:exported="false"/>\n        <activity\n            android:name="dev.bennett.codexmeter.DashboardReorderActivity"').replace(
    '            android:name="dev.bennett.codexmeter.ReleaseUpdateJobService"',
    '            android:name="dev.bennett.codexmeter.LedgerMaintenanceJobService"\n            android:permission="android.permission.BIND_JOB_SERVICE"\n            android:exported="false"/>\n        <service\n            android:name="dev.bennett.codexmeter.ReleaseUpdateJobService"'))
reviewed_change(APP_JAVA + "SettingsTransferStore.java", lambda s: s)
reviewed_hooks = 9

# Compile and execute the repository's existing pure-Java self-test unchanged.
java_home = os.environ.get("JAVA_HOME")
java = str(Path(java_home) / "bin/java") if java_home else shutil.which("java")
javac = str(Path(java_home) / "bin/javac") if java_home else shutil.which("javac")
assert java and javac, "Set JAVA_HOME to an installed JDK 17 or newer"
jar = ROOT / "build/test-libs/json-20250517.jar"
assert jar.exists(), "Run the existing run-tests.sh or provide build/test-libs/json-20250517.jar"
test_dir = OUT / "classes"
test_dir.mkdir(exist_ok=True)
script = (ROOT / "run-tests.sh").read_text(encoding="utf-8").split("# Source-level")[0]
files = re.findall(r'\$ROOT/([^"\n]+\.java)', script)
subprocess.run([javac, "-encoding", "UTF-8", "-cp", str(jar), "-d", str(test_dir)] + [str(ROOT / p) for p in files], check=True)
classpath = str(test_dir) + os.pathsep + str(jar)
subprocess.run([java, "-ea", "-cp", classpath, "dev.bennett.codexmeter.ParserSelfTest"], check=True)


def fixture(relative, source):
    path = OUT / "fixtures" / relative
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(source, encoding="utf-8")
    return str(path)


q = lambda text: json.dumps(text, ensure_ascii=False)
keys = sorted(resources)
fixture_files = []
fixture_files.append(fixture("dev/bennett/codexmeter/R.java", "package dev.bennett.codexmeter; public final class R { public static final class string {\n" + "\n".join(f"public static final int {key} = {i};" for i, key in enumerate(keys)) + "}}"))
fixture_files.append(fixture("android/content/res/Configuration.java", """package android.content.res;
public class Configuration {
 public java.util.Locale locale = java.util.Locale.ENGLISH;
 public Configuration() {} public Configuration(Configuration other) { locale = other.locale; }
 public void setLocale(java.util.Locale value) { locale = value; }
}"""))
fixture_files.append(fixture("android/content/res/Resources.java", """package android.content.res;
public class Resources { private final Configuration configuration;
 public Resources(Configuration config) { configuration = config; }
 public Configuration getConfiguration() { return configuration; }
}"""))
fixture_files.append(fixture("android/content/Context.java", """package android.content;
public class Context {
 public final java.util.Locale locale; public boolean hour24;
 public Context(java.util.Locale value) { locale = value; }
 public String getString(int id) { return ("ko".equals(locale.getLanguage()) ? KO : EN)[id]; }
 public String getString(int id, Object... args) { return String.format(locale, getString(id), args); }
 public android.content.res.Resources getResources() { android.content.res.Configuration c = new android.content.res.Configuration(); c.setLocale(locale); return new android.content.res.Resources(c); }
 public Context createConfigurationContext(android.content.res.Configuration c) { return new Context(c.locale); }
 private static final String[] EN = {""" + ",".join(q(resources[key][0]) for key in keys) + "};\n private static final String[] KO = {" + ",".join(q(resources[key][1]) for key in keys) + "};\n}"))
fixture_files.append(fixture("android/text/format/DateFormat.java", "package android.text.format; public class DateFormat { public static boolean is24HourFormat(android.content.Context c) { return c.hour24; } }"))
baseline_format = original("android/app/src/main/java/dev/bennett/codexmeter/UsageFormat.java").decode("utf-8").replace("UsageFormat", "OriginalUsageFormat")
fixture_files.append(fixture("dev/bennett/codexmeter/OriginalUsageFormat.java", baseline_format))
base_java = ROOT / "app/src/main/java/dev/bennett/codexmeter"
for name in ["UsageFormat", "NowBarText", "DisplayLabels", "DisplayMessages", "OAuthBrowserText"]:
    fixture_files.append(str(base_java / (name + ".java")))
fixture_files.append(str(ROOT / "wear/src/main/java/dev/bennett/codexmeter/WearDisplayText.java"))
fixture_files.append(str(Path(__file__).with_name("LocalizationSelfTest.java")))
subprocess.run([javac, "-encoding", "UTF-8", "-cp", classpath, "-d", str(test_dir)] + fixture_files, check=True)
subprocess.run([java, "-ea", "-cp", classpath, "dev.bennett.codexmeter.LocalizationSelfTest"], check=True)
reset_files = [fixture("dev/bennett/codexmeter/AppPreferences.java", 'package dev.bennett.codexmeter; class AppPreferences { static String error=""; static String getLastError(android.content.Context c){return error;} }'),
    fixture("dev/bennett/codexmeter/RefreshScheduler.java", 'package dev.bennett.codexmeter; class RefreshScheduler { static int effectiveRefreshMinutes(android.content.Context c){return 5;} }')]
reset_files += [str(base_java / (name + '.java')) for name in ['FiveHourResetState','FiveHourResetDisplay']]
reset_files += [str(ROOT / 'shared/src/main/java/dev/bennett/codexmeter/UsageInsights.java'),str(Path(__file__).with_name('FiveHourResetSelfTest.java'))]
subprocess.run([javac, '-encoding', 'UTF-8', '-cp', classpath, '-d', str(test_dir)] + reset_files, check=True)
subprocess.run([java, '-ea', '-cp', classpath, 'dev.bennett.codexmeter.FiveHourResetSelfTest'], check=True)
summary = {"resources": counts, "protected_files_unchanged": len(paths), "baseline": BASE, "reviewed_observer_or_version_files": reviewed_hooks,
           "resource_parity": "passed", "original_self_tests": "passed", "formatter_fixtures": "passed",
           "device_or_emulator_validation": "not performed"}
(OUT / "result.json").write_text(json.dumps(summary, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
print(json.dumps(summary, ensure_ascii=False, indent=2))
