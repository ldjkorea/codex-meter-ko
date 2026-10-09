package dev.bennett.codexmeter;

import android.content.Context;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

/** Separate device-local preferences. Every write checks disk commit and preserves corrupt data. */
// commit() is intentional: report durable write failures instead of claiming apply() succeeded.
@android.annotation.SuppressLint("ApplySharedPref")
final class TestNotesStore {
    static final String PREFS="codex_test_notes_v1";private static final Object LOCK=new Object();
    private TestNotesStore(){}
    private static JSONObject root(Context context)throws Exception{
        String text=context.getSharedPreferences(PREFS,0).getString("document","");
        JSONObject doc=text.isEmpty()?new JSONObject().put("schema",1).put("test_mode",false).put("notes",new JSONArray()):new JSONObject(text);
        if(doc.getInt("schema")!=1)throw new IllegalStateException("Unsupported notes schema");return doc;
    }
    static List<TestNote> load(Context context)throws Exception{synchronized(LOCK){JSONArray rows=root(context).getJSONArray("notes");List<TestNote> out=new ArrayList<>();for(int i=0;i<rows.length();i++)out.add(TestNote.from(rows.getJSONObject(i)));return out;}}
    static boolean enabled(Context context){synchronized(LOCK){try{return root(context).getBoolean("test_mode");}catch(Exception ignored){return false;}}}
    private static boolean commit(Context c,JSONObject doc){android.content.SharedPreferences prefs=c.getSharedPreferences(PREFS,0);String previous=prefs.getString("document","");boolean saved=prefs.edit().putString("document",doc.toString()).commit();if(!saved)prefs.edit().putString("document",previous).commit();return saved;}
    static boolean setEnabled(Context c,boolean enabled){synchronized(LOCK){try{return commit(c,root(c).put("test_mode",enabled));}catch(Exception ignored){return false;}}}
    static boolean save(Context c,TestNote note){synchronized(LOCK){try{
        JSONObject doc=root(c);JSONArray deleted=doc.optJSONArray("deleted");if(deleted!=null)for(int i=0;i<deleted.length();i++)if(deleted.getString(i).equals(note.id))return false;
        JSONArray old=doc.getJSONArray("notes"),next=new JSONArray();boolean found=false;
        for(int i=0;i<old.length();i++){TestNote item=TestNote.from(old.getJSONObject(i));if(item.id.equals(note.id)){found=true;if(item.revision>note.revision)return false;next.put(note.json());}else next.put(item.json());}
        if(!found)next.put(note.json());return commit(c,doc.put("notes",next));
    }catch(Exception ignored){return false;}}}
    static boolean delete(Context c,String id){synchronized(LOCK){try{JSONObject doc=root(c);JSONArray rows=doc.getJSONArray("notes"),next=new JSONArray(),deleted=doc.optJSONArray("deleted");if(deleted==null)deleted=new JSONArray();deleted.put(id);for(int i=0;i<rows.length();i++)if(!rows.getJSONObject(i).getString("id").equals(id))next.put(rows.getJSONObject(i));return commit(c,doc.put("notes",next).put("deleted",deleted));}catch(Exception ignored){return false;}}}
    static boolean clear(Context c){synchronized(LOCK){try{JSONObject doc=root(c);JSONArray deleted=doc.optJSONArray("deleted");if(deleted==null)deleted=new JSONArray();JSONArray rows=doc.getJSONArray("notes");for(int i=0;i<rows.length();i++)deleted.put(rows.getJSONObject(i).getString("id"));return commit(c,doc.put("notes",new JSONArray()).put("deleted",deleted));}catch(Exception ignored){return false;}}}
    static String export(Context c,boolean json)throws Exception{
        List<TestNote> notes=load(c);JSONArray rows=new JSONArray();StringBuilder md=new StringBuilder("# Codex Meter test notes\n\n");
        for(TestNote note:notes)if(note.status!=2){JSONObject row=note.json();row.put("text",TestNote.safeExport(note.text));rows.put(row);
            md.append("## [").append(note.version).append(" / ").append(note.screen).append(" / ").append(note.status==0?"OPEN":"VERIFY").append("]\n")
                .append(TestNote.safeExport(note.text)).append("\n\nCreated UTC: ").append(java.time.Instant.ofEpochMilli(note.created)).append("\nUpdated UTC: ").append(java.time.Instant.ofEpochMilli(note.updated)).append(note.draft?"\nDraft":"").append("\n\n");}
        return json?new JSONObject().put("schema",1).put("scope","device").put("notes",rows).toString(2):md.toString();
    }
}
