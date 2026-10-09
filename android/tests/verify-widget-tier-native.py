"""Explicit opt-in native fixture runner. Refuses physical devices/existing target app.

Usage after Release compilation: python .../verify-widget-tier-native.py emulator-5554
Leaves installed fixture apps/captures intact. Never logs in, calls usage APIs or clears data.
"""
from pathlib import Path
from zipfile import ZipFile
import os,subprocess,sys

root=Path(__file__).resolve().parents[1]
serial=sys.argv[1]
assert serial.startswith('emulator-'), 'Only an isolated emulator is authorized for this harness'
sdk=Path(os.environ['ANDROID_HOME']);jdk=Path(os.environ['JAVA_HOME'])/'bin'
bt=sdk/'build-tools/36.0.0';adb=sdk/'platform-tools/adb.exe';jar=sdk/'platforms/android-36/android.jar'
out=root/'build/widget-tier-native';out.mkdir(parents=True,exist_ok=True)
test_package='dev.bennett.codexmeter.widgettier2813test'
run=lambda args:subprocess.run(list(map(str,args)),check=True)
shell=lambda *args:subprocess.check_output([str(adb),'-s',serial,'shell',*args],text=True,encoding='utf-8')
assert shell('getprop','ro.kernel.qemu').strip()=='1'
assert 'package:dev.bennett.codexmeter' not in shell('pm','list','packages','--user','0','dev.bennett.codexmeter').splitlines(), 'Preserve an existing emulator installation'
assert 'package:'+test_package not in shell('pm','list','packages','--user','0',test_package).splitlines(), 'Preserve previous isolated fixtures'
manifest=out/'AndroidManifest.xml'
manifest.write_text(f'''<manifest xmlns:android="http://schemas.android.com/apk/res/android" package="{test_package}">
<uses-sdk android:minSdkVersion="26" android:targetSdkVersion="36"/>
<application android:label="Codex Meter isolated widget QA" android:debuggable="true"/>
<instrumentation android:name="dev.bennett.codexmeter.WidgetTierNativeTest" android:targetPackage="dev.bennett.codexmeter"/>
</manifest>''',encoding='utf-8')
classes=out/'classes';classes.mkdir(exist_ok=True)
classpath=os.pathsep.join(map(str,[jar,
    root/'app/build/intermediates/javac/release/compileReleaseJavaWithJavac/classes',
    root/'app/build/intermediates/compile_r_class_jar/release/generateReleaseRFile/R.jar',
    root/'shared/build/classes/java/main']))
run([jdk/'javac.exe','-encoding','UTF-8','-source','17','-target','17','-cp',classpath,'-d',classes,Path(__file__).with_name('WidgetTierNativeTest.java')])
run([bt/'aapt2.exe','link','-I',jar,'--manifest',manifest,'-o',out/'base.apk'])
dex=out/'dex';dex.mkdir(exist_ok=True)
run([bt/'d8.bat','--lib',jar,'--min-api','26','--output',dex,*classes.rglob('*.class')])
with ZipFile(out/'base.apk','a') as z:z.write(dex/'classes.dex','classes.dex')
signing=root/'.local-signing'
run([bt/'apksigner.bat','sign','--ks',signing/'codex-meter-local.p12','--ks-key-alias','codexmeter',
     '--ks-pass','file:'+str(signing/'password'),
     '--out',out/'native-tests.apk',out/'base.apk'])
run([adb,'-s',serial,'install',root/'app/build/outputs/apk/release/app-release.apk'])
run([adb,'-s',serial,'install',out/'native-tests.apk'])
result=shell('am','instrument','-r','-w',test_package+'/dev.bennett.codexmeter.WidgetTierNativeTest')
(out/'RESULT.txt').write_text(result,encoding='utf-8');print(result)
assert 'INSTRUMENTATION_CODE: -1' in result and 'Native RemoteViews fixture checks:' in result
captures=out/'captures';captures.mkdir(exist_ok=True)
run([adb,'-s',serial,'pull','/sdcard/Android/data/dev.bennett.codexmeter/files/widget-tier-fixtures',captures])
assert len(list(captures.rglob('*.png')))==21
print('Android emulator native renderer: 21 synthetic captures. Not a Samsung launcher or authenticated usage test.')
