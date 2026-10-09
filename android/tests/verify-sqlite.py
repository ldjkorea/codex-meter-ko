#!/usr/bin/env python3
"""Execute production ledger DAO/migration against real SQLite via JVM Android API fixtures.

The bridge uses Python stdlib SQLite, not an Android device. It exercises production
Java transaction boundaries and queries; OS scheduling/Android SQLite integration
still need device validation. It never opens an installed application's database.
"""
from pathlib import Path
import os
import shutil
import subprocess
import sys

root = Path(__file__).resolve().parents[1]
out = root / 'build/sqlite-verification'
out.mkdir(parents=True, exist_ok=True)
base = root / 'build/insight-verification/fixtures'
jar = root / 'build/test-libs/json-20250517.jar'
assert (base / 'android/content/Context.java').exists(), 'Run verify-insights.py first.'
java_home = os.environ.get('JAVA_HOME')
javac = str(Path(java_home) / 'bin/javac') if java_home else shutil.which('javac')
java = str(Path(java_home) / 'bin/java') if java_home else shutil.which('java')

bridge = out / 'sqlite_bridge.py'
bridge.write_text('''import json, sqlite3, sys
db=sqlite3.connect(sys.argv[1], isolation_level=None)
for line in sys.stdin:
 try:
  request=json.loads(line)
  cursor=db.execute(request['sql'],request.get('args',[]))
  rows=cursor.fetchall() if cursor.description else []
  result={'rows':rows,'id':cursor.lastrowid or 0,'count':max(0,cursor.rowcount)}
 except Exception as error:
  result={'error':str(error)}
 print(json.dumps(result),flush=True)
db.close()
''', encoding='utf-8')

fixtures = {
    'android/content/ContentValues.java': '''package android.content;
import java.util.*;
public class ContentValues extends LinkedHashMap<String,Object> {
 public void put(String k,String v){super.put(k,v);}
 public void put(String k,Integer v){super.put(k,v);}
 public void put(String k,Long v){super.put(k,v);}
 public void put(String k,Double v){super.put(k,v);}
}''',
    'android/database/Cursor.java': '''package android.database;
import org.json.*;
public class Cursor implements AutoCloseable {
 private final JSONArray rows;private int index=-1;
 public Cursor(JSONArray rows){this.rows=rows;}
 public boolean moveToNext(){return ++index<rows.length();}
 public boolean moveToFirst(){index=0;return rows.length()>0;}
 private Object get(int column){return rows.getJSONArray(index).get(column);}
 public long getLong(int c){Object v=get(c);return v instanceof Number?((Number)v).longValue():Long.parseLong(v.toString());}
 public int getInt(int c){return (int)getLong(c);}
 public double getDouble(int c){Object v=get(c);return v instanceof Number?((Number)v).doubleValue():Double.parseDouble(v.toString());}
 public String getString(int c){return get(c).toString();}
 public void close(){}
}''',
    'android/database/sqlite/SQLiteDatabase.java': '''package android.database.sqlite;
import android.content.ContentValues;
import android.database.Cursor;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.json.*;
public class SQLiteDatabase implements AutoCloseable {
 public static final int CONFLICT_IGNORE=4,CONFLICT_REPLACE=5;
 public static boolean failNextWrite;
 public static boolean failCommit;
 public static String silentRejectTable;
 private final Process process;private final BufferedReader input;private final BufferedWriter output;
 private boolean transaction,successful;
 public SQLiteDatabase(String name){try{
  process=new ProcessBuilder(System.getProperty("ledger.python"),"-u",System.getProperty("ledger.bridge"),
    System.getProperty("ledger.dbdir")+File.separator+name).redirectError(ProcessBuilder.Redirect.INHERIT).start();
  input=new BufferedReader(new InputStreamReader(process.getInputStream(),StandardCharsets.UTF_8));
  output=new BufferedWriter(new OutputStreamWriter(process.getOutputStream(),StandardCharsets.UTF_8));
 }catch(IOException ex){throw new IllegalStateException(ex);}}
 private synchronized JSONObject execute(String sql,Object[] args){try{
  if(failNextWrite&&!sql.startsWith("SELECT")&&!sql.equals("BEGIN")&&!sql.equals("ROLLBACK")){
   failNextWrite=false;throw new IllegalStateException("Injected SQLite write failure");
  }
  output.write(new JSONObject().put("sql",sql).put("args",new JSONArray(args==null?new Object[0]:args)).toString());
  output.newLine();output.flush();String line=input.readLine();
  if(line==null)throw new IllegalStateException("SQLite bridge ended");
  JSONObject result=new JSONObject(line);if(result.has("error"))throw new IllegalStateException(result.getString("error"));
  return result;
 }catch(IOException ex){throw new IllegalStateException(ex);}}
 public void execSQL(String sql){execute(sql,null);}
 public void execSQL(String sql,Object[] args){execute(sql,args);}
 public Cursor rawQuery(String sql,String[] args){return new Cursor(execute(sql,args).getJSONArray("rows"));}
 public void beginTransaction(){if(transaction)throw new IllegalStateException("Nested transaction");execSQL("BEGIN");transaction=true;successful=false;}
 public void setTransactionSuccessful(){successful=true;}
 public void endTransaction(){
  if(successful&&failCommit){failCommit=false;execSQL("ROLLBACK");transaction=false;throw new IllegalStateException("Injected commit failure");}
  execSQL(successful?"COMMIT":"ROLLBACK");transaction=false;
 }
 public long insertWithOnConflict(String table,String nullColumn,ContentValues values,int conflict){
  if(table.equals(silentRejectTable)){silentRejectTable=null;return -1;}
  String columns=String.join(",",values.keySet());String marks=String.join(",",Collections.nCopies(values.size(),"?"));
  JSONObject result=execute("INSERT OR "+(conflict==CONFLICT_IGNORE?"IGNORE":"REPLACE")+" INTO "+table+"("+columns+") VALUES("+marks+")",values.values().toArray());
  return result.getInt("count")==0?-1:result.getLong("id");
 }
 public int delete(String table,String where,String[] args){return execute("DELETE FROM "+table+(where==null?"":" WHERE "+where),args).getInt("count");}
 public int update(String table,ContentValues values,String where,String[] args){
  List<String> sets=new ArrayList<>();for(String key:values.keySet())sets.add(key+"=?");
  List<Object> parameters=new ArrayList<>(values.values());if(args!=null)parameters.addAll(Arrays.asList(args));
  return execute("UPDATE "+table+" SET "+String.join(",",sets)+" WHERE "+where,parameters.toArray()).getInt("count");
 }
 public void close(){try{output.close();input.close();process.waitFor();}catch(Exception ex){throw new IllegalStateException(ex);}}
}''',
    'android/database/sqlite/SQLiteOpenHelper.java': '''package android.database.sqlite;
import android.content.Context;
import android.database.Cursor;
public abstract class SQLiteOpenHelper {
 private SQLiteDatabase db;private final String name;
 protected SQLiteOpenHelper(Context context,String name,Object factory,int version){this.name=name;}
 public synchronized SQLiteDatabase getWritableDatabase(){
  if(db==null){db=new SQLiteDatabase(name);try(Cursor c=db.rawQuery("SELECT COUNT(*) FROM sqlite_master WHERE type='table' AND name='observations'",null)){
   if(c.moveToFirst()&&c.getInt(0)==0)onCreate(db);
  }}return db;
 }
 public SQLiteDatabase getReadableDatabase(){return getWritableDatabase();}
 public abstract void onCreate(SQLiteDatabase db);
 public abstract void onUpgrade(SQLiteDatabase db,int oldVersion,int newVersion);
 public void close(){if(db!=null)db.close();db=null;}
}''',
    'dev/bennett/codexmeter/LedgerMaintenanceScheduler.java': '''package dev.bennett.codexmeter;
final class LedgerMaintenanceScheduler {
 static int schedules,cancels;
 static void schedule(android.content.Context c){schedules++;}
 static void cancel(android.content.Context c){cancels++;}
}''',
}
files = [str(path) for path in base.rglob('*.java')]
for name, text in fixtures.items():
    path = out / 'fixtures' / name
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(text, encoding='utf-8')
    files.append(str(path))
shared = root / 'shared/src/main/java/dev/bennett/codexmeter'
app = root / 'app/src/main/java/dev/bennett/codexmeter'
files += [str(shared / f'{name}.java') for name in ['UsageWindow', 'UsageSample', 'UsageHistory',
          'UsageSnapshot', 'UsageLimit', 'UsageCredits', 'UsageInsights', 'UsageEventDetector',
          'UsageLedger', 'UsageLedgerInsights', 'LedgerRecord', 'LedgerAggregation', 'LedgerPresentation', 'LedgerCalendar']]
files += [str(app / f'{name}.java') for name in ['RateLimitResetCredit', 'ResetCreditsSnapshot',
          'UsageEventStore', 'UsageLedgerStore', 'UsageHistoryRecorder', 'LedgerCapture', 'UsageLedgerDatabase']]
files.append(str(Path(__file__).with_name('LedgerDatabaseSelfTest.java')))
classes = out / 'classes'
classes.mkdir(exist_ok=True)
subprocess.run([javac, '-encoding', 'UTF-8', '-cp', str(jar), '-d', str(classes)] + files, check=True)
# Each run gets its own new fixture directory, preserving all previous fixture evidence.
import tempfile
dbdir = tempfile.mkdtemp(prefix='ledger-fixture-', dir=str(out))
subprocess.run([java, '-ea', '-Dledger.python=' + sys.executable, '-Dledger.bridge=' + str(bridge),
                '-Dledger.dbdir=' + dbdir, '-cp', str(classes) + os.pathsep + str(jar),
                'dev.bennett.codexmeter.LedgerDatabaseSelfTest'], check=True)

# Independent data fixtures prove that the displayed calendar reads persisted daily rows.
for test in ['CalendarSelfTest','CalendarDatabaseSelfTest']:
    subprocess.run([javac,'-encoding','UTF-8','-cp',str(classes)+os.pathsep+str(jar),'-d',str(classes),str(Path(__file__).with_name(test+'.java'))],check=True)
    calendar_dir=tempfile.mkdtemp(prefix='calendar-fixture-',dir=str(out))
    subprocess.run([java,'-ea','-Dledger.python='+sys.executable,'-Dledger.bridge='+str(bridge),'-Dledger.dbdir='+calendar_dir,'-cp',str(classes)+os.pathsep+str(jar),'dev.bennett.codexmeter.'+test],check=True)
