"""Run the production home crest helper on isolated JVM preferences/RemoteViews fixtures.
Not launcher rendering, Samsung device proof, or Android process lifecycle proof.
"""
from pathlib import Path
import os, subprocess, xml.etree.ElementTree as ET, re
repo=Path(__file__).resolve().parents[2]
app=repo/'android/app/src/main/java/dev/bennett/codexmeter'
out=repo/'android/build/home-crest-verification';out.mkdir(parents=True,exist_ok=True)
fixtures={
'androidx/appcompat/content/res/AppCompatResources.java':'package androidx.appcompat.content.res;public class AppCompatResources {public static android.graphics.drawable.Drawable getDrawable(android.content.Context c,int id){return c.getDrawable(id);}}',
'android/content/SharedPreferences.java':'''package android.content; public interface SharedPreferences {boolean getBoolean(String k,boolean d);String getString(String k,String d);Editor edit();interface Editor {Editor putBoolean(String k,boolean v);Editor remove(String k);boolean commit();void apply();}}''',
'android/content/Context.java':'''package android.content; import java.util.*;import java.nio.file.*;
public class Context {public final Path dir;public Context(Path p){dir=p;}public SharedPreferences getSharedPreferences(String name,int mode){return new Pref(dir.resolve(name+".properties"));}public android.graphics.drawable.Drawable getDrawable(int id){return new android.graphics.drawable.Drawable(id);}public String getString(int id){return "label"+id;}
static class Pref implements SharedPreferences {final Path p;final Properties values=new Properties();Pref(Path f){p=f;try{if(Files.exists(p))values.load(Files.newInputStream(p));}catch(Exception e){throw new RuntimeException(e);}}public boolean getBoolean(String k,boolean d){return Boolean.parseBoolean(values.getProperty(k,""+d));}public String getString(String k,String d){return values.getProperty(k,d);}public Editor edit(){return new Editor(){public Editor putBoolean(String k,boolean v){values.setProperty(k,""+v);return this;}public Editor remove(String k){values.remove(k);return this;}public boolean commit(){try{Files.createDirectories(p.getParent());try(var s=Files.newOutputStream(p)){values.store(s,"");}return true;}catch(Exception e){return false;}}public void apply(){commit();}};}}}''',
'android/content/ComponentName.java':'package android.content;public class ComponentName {public ComponentName(Context c,Class<?> t){}}',
'android/appwidget/AppWidgetManager.java':'''package android.appwidget;public class AppWidgetManager {static final AppWidgetManager m=new AppWidgetManager();public static AppWidgetManager getInstance(android.content.Context c){return m;}public int[] getAppWidgetIds(android.content.ComponentName n){return new int[]{10,11};}}''',
'android/graphics/Bitmap.java':'package android.graphics;public class Bitmap {public int alpha,resource;public enum Config {ARGB_8888}public static Bitmap createBitmap(int w,int h,Config c){if(w!=96||h!=96)throw new AssertionError();return new Bitmap();}}',
'android/graphics/Canvas.java':'package android.graphics;public class Canvas {public Bitmap image;public Canvas(Bitmap b){image=b;}}',
'android/graphics/drawable/Drawable.java':'package android.graphics.drawable;public class Drawable {int resource,alpha;public Drawable(int id){resource=id;}public void setBounds(int a,int b,int c,int d){}public void setAlpha(int v){alpha=v;}public void draw(android.graphics.Canvas c){c.image.alpha=alpha;c.image.resource=resource;}}',
'android/view/View.java':'package android.view;public class View {public static final int VISIBLE=0,GONE=8;}',
'android/widget/RemoteViews.java':'package android.widget;public class RemoteViews {public int visibility;public String description;public android.graphics.Bitmap image;public void setViewVisibility(int id,int v){visibility=v;}public void setImageViewBitmap(int id,android.graphics.Bitmap b){image=b;}public void setContentDescription(int id,String s){description=s;}}',
'dev/bennett/codexmeter/R.java':'package dev.bennett.codexmeter;class R {static class id {static int widget_tier_emblem=1;}static class string {static int fun_placing=99;}}',
'dev/bennett/codexmeter/LiveUsageStore.java':'package dev.bennett.codexmeter;class LiveUsageStore {static long value;static long revision(){return value;}}',
'dev/bennett/codexmeter/TierTheme.java':'package dev.bennett.codexmeter;class TierTheme {static int rank=-1;static int[] EMBLEMS={0,1,2,3,4,5,6,7,8,9};static int tier(android.content.Context c){return rank;}}',
'dev/bennett/codexmeter/TierPresentation.java':'package dev.bennett.codexmeter;class TierPresentation {static int[] NAMES={10,11,12,13,14,15,16,17,18,19};}',
'dev/bennett/codexmeter/CodexUsageWidget.java':'package dev.bennett.codexmeter;class CodexUsageWidget {}',
'dev/bennett/codexmeter/WidgetRenderer.java':'package dev.bennett.codexmeter;class WidgetRenderer {static int calls;static void update(android.content.Context c,android.appwidget.AppWidgetManager m,int id){calls++;}}',
'dev/bennett/codexmeter/DiagnosticLog.java':'package dev.bennett.codexmeter;class DiagnosticLog {static void error(android.content.Context c,String s,String t,Exception e){throw new AssertionError(e);}}',
'dev/bennett/codexmeter/HomeCrestTest.java':'''package dev.bennett.codexmeter;import android.content.Context;import android.widget.RemoteViews;import java.nio.file.*;
public class HomeCrestTest {static int checks;static void check(boolean v){checks++;if(!v)throw new AssertionError(checks);}public static void main(String[] a)throws Exception {Context c=new Context(Path.of(a[0]));check(WidgetCrest.enabled(c,10));RemoteViews v=new RemoteViews();WidgetCrest.bind(c,v,10);check(v.visibility==0&&v.image.resource==0&&v.image.alpha==100&&v.description.equals("label99"));
for(int tier=0;tier<10;tier++){TierTheme.rank=tier;WidgetCrest.bind(c,v,10);check(v.image.resource==tier&&v.image.alpha==255);check(v.description.equals("label"+(10+tier)));}
check(WidgetCrest.save(c,10,false));WidgetCrest.bind(c,v,10);check(v.visibility==8);check(!WidgetCrest.enabled(new Context(c.dir),10));check(WidgetCrest.enabled(c,11));WidgetCrest.bind(c,v,true);check(v.visibility==0);check(!WidgetCrest.enabled(c,10));WidgetCrest.bind(c,v,false);check(v.visibility==8);WidgetCrest.restored(c,10,20);check(!WidgetCrest.enabled(new Context(c.dir),20));check(WidgetCrest.enabled(c,10));WidgetCrest.restored(c,20,20);check(!WidgetCrest.enabled(c,20));WidgetCrest.deleted(c,20);check(WidgetCrest.enabled(c,20));check(WidgetCrest.save(c,10,true));check(WidgetCrest.enabled(new Context(c.dir),10));WidgetCrest.updateExistingIfChanged(c);check(WidgetRenderer.calls==2);WidgetCrest.updateExistingIfChanged(c);check(WidgetRenderer.calls==2);Files.writeString(c.dir.resolve("secure_auth_v1.properties"),"blob=changed");WidgetCrest.updateExistingIfChanged(c);check(WidgetRenderer.calls==4);LiveUsageStore.value++;WidgetCrest.updateExistingIfChanged(c);check(WidgetRenderer.calls==6);WidgetCrest.updateExistingIfChanged(c);check(WidgetRenderer.calls==6);Path blocked=c.dir.resolve("blocked");Files.writeString(blocked,"existing");check(!WidgetCrest.save(new Context(blocked),50,false));System.out.println("Production home crest preferences/render/cache/restore: "+checks+" assertions passed (JVM fixtures).");}}
'''
}
files=[]
for name,source in fixtures.items():
    p=out/'fixtures'/name;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(source,encoding='utf-8');files.append(str(p))
bin=Path(os.environ['JAVA_HOME'])/'bin'
subprocess.run([str(bin/'javac'),'-encoding','UTF-8','-d',str(out),str(app/'WidgetCrest.java')]+files,check=True)
import uuid
subprocess.run([str(bin/'java'),'-ea','-cp',str(out),'dev.bennett.codexmeter.HomeCrestTest',str(out/('preferences-'+uuid.uuid4().hex))],check=True)
layouts=['battery_list','compact','detailed','dials','dials_large','dials_max','micro','rings','rings_four','rings_large','rings_max']
ns='{http://schemas.android.com/apk/res/android}'
for name in layouts:
    path='android/app/src/main/res/layout/widget_'+name+'.xml'
    text=(repo/path).read_text(encoding='utf-8');xml=ET.fromstring(text)
    emblems=[x for x in xml.iter() if x.get(ns+'id')=='@+id/widget_tier_emblem'];assert len(emblems)==1
    assert emblems[0].get(ns+'scaleType')=='fitCenter' and emblems[0].get(ns+'visibility')=='gone'
    stripped=re.sub(r'<ImageView android:id="@\+id/widget_tier_emblem"[^>]+/>\n    ','',text)
    original=subprocess.check_output(['git','show','d10688f:'+path],cwd=repo).decode('utf-8').replace('\r\n','\n')
    assert stripped==original,(name,'Existing layout changed beyond the additive crest')
renderer=(app/'WidgetRenderer.java').read_text(encoding='utf-8');config=(app/'WidgetConfigActivity.java').read_text(encoding='utf-8')
assert 'WidgetCrest.bind(context,remoteViews,i);' in renderer
assert 'this.widgetSize,crestSwitch.isChecked()' in config
assert 'crestDraft' in config and 'WidgetCrest.save(this,appWidgetId,crestSwitch.isChecked())' in config
assert 'WidgetCrest.updateExistingIfChanged(c)' in (app/'EvolutionWidget.java').read_text(encoding='utf-8')
print('All 11 existing home layouts preserved; draft preview/save/rotation and async tier repaint wiring passed. Native launcher UI not tested.')
