from pathlib import Path
from zipfile import ZipFile
import os,subprocess,sys
root=Path(__file__).resolve().parents[1];serial=sys.argv[1];assert serial.startswith('emulator-')
sdk=Path(os.environ['ANDROID_HOME']);jdk=Path(os.environ['JAVA_HOME'])/'bin';bt=sdk/'build-tools/36.0.0';adb=sdk/'platform-tools/adb.exe';jar=sdk/'platforms/android-36/android.jar';out=root/'build/spicy-native';out.mkdir(parents=True,exist_ok=True)
run=lambda args:subprocess.run(list(map(str,args)),check=True)
shell=lambda *args:subprocess.check_output([str(adb),'-s',serial,'shell',*args],text=True,encoding='utf-8')
assert shell('getprop','ro.kernel.qemu').strip()=='1' and shell('getprop','ro.boot.qemu.avd_name').strip()=='benefit-perf-api35'
test_package='dev.bennett.codexmeter.spicy2817test';package='dev.bennett.codexmeter';installed=shell('pm','list','packages',package).splitlines()
reuse='--reuse-owned-fixture' in sys.argv
if reuse:assert 'package:'+test_package in installed and 'versionName=2.8.17' in shell('dumpsys','package',package)
else:assert 'package:'+package not in installed and 'package:'+test_package not in installed,'Preserve any existing emulator app'
manifest=out/'AndroidManifest.xml';manifest.write_text(f'<manifest xmlns:android="http://schemas.android.com/apk/res/android" package="{test_package}"><uses-sdk android:minSdkVersion="26" android:targetSdkVersion="36"/><application android:label="GPT HUD isolated 2.8.17 QA" android:debuggable="true"/><instrumentation android:name="dev.bennett.codexmeter.SpicyTrialNativeTest" android:targetPackage="{package}"/></manifest>',encoding='utf-8')
classes=out/'classes';classes.mkdir(exist_ok=True);classpath=os.pathsep.join(map(str,[jar,root/'app/build/intermediates/javac/release/compileReleaseJavaWithJavac/classes',root/'app/build/intermediates/compile_r_class_jar/release/generateReleaseRFile/R.jar',root/'shared/build/classes/java/main']))
run([jdk/'javac.exe','-encoding','UTF-8','-source','17','-target','17','-cp',classpath,'-d',classes,Path(__file__).with_name('SpicyTrialNativeTest.java')]);run([bt/'aapt2.exe','link','-I',jar,'--manifest',manifest,'-o',out/'base.apk']);dex=out/'dex';dex.mkdir(exist_ok=True)
run([bt/'d8.bat','--lib',jar,'--min-api','26','--output',dex,*classes.rglob('*.class')])
with ZipFile(out/'base.apk','a') as z:z.write(dex/'classes.dex','classes.dex')
signing=root/'.local-signing';run([bt/'apksigner.bat','sign','--ks',signing/'codex-meter-local.p12','--ks-key-alias','codexmeter','--ks-pass','file:'+str(signing/'password'),'--out',out/'tests.apk',out/'base.apk'])
run([adb,'-s',serial,'install',*(['-r'] if reuse else []),root/'app/build/outputs/apk/release/app-release.apk']);run([adb,'-s',serial,'install',*(['-r'] if reuse else []),out/'tests.apk'])
result=shell('am','instrument','-r','-w',test_package+'/dev.bennett.codexmeter.SpicyTrialNativeTest');(out/'RESULT.txt').write_text(result,encoding='utf-8');print(result);assert 'INSTRUMENTATION_CODE: -1' in result and 'checks passed' in result
run([adb,'-s',serial,'pull','/sdcard/Android/data/'+package+'/files/spicy-trial-fixtures',out/'captures'])
