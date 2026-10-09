package dev.bennett.codexmeter;

import org.json.JSONObject;

/** Device-owned note, no automatically attached account, credentials, path or conversation. */
public final class TestNote {
    public final String id,text,screen,version,build;
    public final long created,updated,revision;public final int status;public final boolean draft;
    public TestNote(String id,String text,String screen,String version,String build,long created,long updated,long revision,int status,boolean draft){
        if(id==null||!id.matches("[a-zA-Z0-9-]{1,80}")||text==null||text.length()>32000||screen==null||!screen.matches("[a-zA-Z0-9_/-]{1,60}")||created<=0||updated<created||revision<1||status<0||status>2)
            throw new IllegalArgumentException("Invalid note");
        this.id=id;this.text=text;this.screen=screen;this.version=version;this.build=build;this.created=created;this.updated=updated;this.revision=revision;this.status=status;this.draft=draft;
    }
    public JSONObject json() throws Exception{return new JSONObject().put("id",id).put("text",text).put("screen",screen).put("version",version).put("build",build).put("created",created).put("updated",updated).put("revision",revision).put("status",status).put("draft",draft);}
    public static TestNote from(JSONObject row)throws Exception{return new TestNote(row.getString("id"),row.getString("text"),row.getString("screen"),row.getString("version"),row.getString("build"),row.getLong("created"),row.getLong("updated"),row.getLong("revision"),row.getInt("status"),row.getBoolean("draft"));}
    public static String safeExport(String text){return text.replaceAll("(?i)(Bearer\\s+)[A-Za-z0-9._~-]+","$1[REDACTED]")
        .replaceAll("\\bsk-[A-Za-z0-9_-]{12,}","[REDACTED]")
        .replaceAll("\\beyJ[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+","[REDACTED]")
        .replaceAll("(?i)(access_token|refresh_token|account_id|api_key)\\s*[:=]\\s*[^\\s,;]+","$1=[REDACTED]")
        .replaceAll("(?i)[A-Z]:\\\\[^\\r\\n]+","[LOCAL_PATH]");}
}
