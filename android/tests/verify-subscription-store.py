"""Compile production payment store on JVM file-backed Android preference fixtures."""
from pathlib import Path
import os, subprocess

root=Path(__file__).resolve().parents[1]
out=root/'build/subscription-store-verification'
app=root/'app/src/main/java/dev/bennett/codexmeter'
fixtures={
'android/annotation/SuppressLint.java': 'package android.annotation; public @interface SuppressLint {String[] value();}',
'android/content/SharedPreferences.java': '''package android.content;
public interface SharedPreferences {String getString(String key,String fallback);Editor edit();
 interface Editor {Editor putString(String key,String value);boolean commit();}}''',
'android/content/Context.java': '''package android.content;
import java.nio.file.*;import org.json.JSONObject;
public class Context {
 public final Path directory;public static boolean failWrites;
 public Context(Path directory){this.directory=directory;}
 public SharedPreferences getSharedPreferences(String name,int mode){return new Prefs(directory.resolve(name+".json"));}
 static class Prefs implements SharedPreferences {
  final Path file;Prefs(Path file){this.file=file;}
  JSONObject read(){try{return new JSONObject(Files.readString(file));}catch(Exception ignored){return new JSONObject();}}
  public String getString(String key,String fallback){return read().optString(key,fallback);}
  public Editor edit(){JSONObject values=read();return new Editor(){
   public Editor putString(String key,String value){try{values.put(key,value);}catch(Exception e){throw new RuntimeException(e);}return this;}
   public boolean commit(){if(failWrites)return false;try{Files.writeString(file,values.toString());return true;}catch(Exception ignored){return false;}}
  };}
 }
}''',
'dev/bennett/codexmeter/SecureTokenStore.java': '''package dev.bennett.codexmeter;
class SecureTokenStore {static AuthTokens current;static AuthTokens load(android.content.Context context){return current;}}''',
'dev/bennett/codexmeter/AppPreferences.java': '''package dev.bennett.codexmeter;
class AppPreferences {static UsageSnapshot current;static UsageSnapshot loadSnapshot(android.content.Context context){return current;}}''',
'dev/bennett/codexmeter/UsageApi.java': 'package dev.bennett.codexmeter; class UsageApi {static final Object NETWORK_LOCK=new Object();}',
}
files=[]
for name,text in fixtures.items():
    path=out/'fixtures'/name;path.parent.mkdir(parents=True,exist_ok=True);path.write_text(text,encoding='utf-8');files.append(str(path))
files += [str(app/'SubscriptionStore.java'),str(app/'AuthTokens.java'),str(root/'shared/src/main/java/dev/bennett/codexmeter/SubscriptionCost.java'),str(Path(__file__).with_name('SubscriptionStoreSelfTest.java'))]
files += [str(app/name) for name in ['FunStore.java','TestNotesStore.java']]
files += [str(root/'shared/src/main/java/dev/bennett/codexmeter'/name) for name in ['FunInsights.java','TestNote.java']]
files += [str(Path(__file__).with_name(name)) for name in ['FunInsightsSelfTest.java','FunPersistenceSelfTest.java']]
classes=out/'classes';classes.mkdir(parents=True,exist_ok=True)
cp=os.pathsep.join(map(str,[classes,root/'build/insight-verification/classes',root/'build/test-libs/json-20250517.jar']))
jdk=Path(os.environ['JAVA_HOME'])/'bin'
subprocess.run([str(jdk/'javac'),'-encoding','UTF-8','-cp',cp,'-d',str(classes),*files],check=True)
subprocess.run([str(jdk/'java'),'-ea','-cp',cp,'dev.bennett.codexmeter.SubscriptionStoreSelfTest'],check=True)
for test in ['FunInsightsSelfTest','FunPersistenceSelfTest']:
    subprocess.run([str(jdk/'java'),'-ea','-cp',cp,'dev.bennett.codexmeter.'+test],check=True)
