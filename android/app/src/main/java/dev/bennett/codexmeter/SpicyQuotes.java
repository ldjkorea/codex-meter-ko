package dev.bennett.codexmeter;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.SystemClock;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.json.JSONArray;
import org.json.JSONObject;

/** Loaded and selected only on FunHome's existing worker. English uses the original resource path. */
final class SpicyQuotes {
    private static final Map<String,int[]> GROUPS=new HashMap<>();
    private static final List<String> TEXT=new ArrayList<>();
    private static final Random RANDOM=new Random();
    private static long generation=System.nanoTime(),away;
    
    private static boolean loaded;
    static synchronized void foreground(){if(away>0&&SystemClock.elapsedRealtime()-away>=30000)generation++;away=0;}
    static synchronized void background(boolean rotation){if(!rotation)away=SystemClock.elapsedRealtime();}
    static synchronized void manual(){generation++;}
    private static synchronized long request(){return generation;}
    static synchronized String select(Context c,String account,UsageSnapshot snapshot,boolean fresh,boolean bonus,double daily,long now) {try{
        if(!c.getResources().getConfiguration().getLocales().get(0).getLanguage().equals("ko"))return null;
        if(!loaded){try(BufferedReader reader=new BufferedReader(new InputStreamReader(c.getAssets().open("spicy-ko.txt"),StandardCharsets.UTF_8))){
            String line,group="";List<Integer> ids=new ArrayList<>();
            while((line=reader.readLine())!=null){if(line.isEmpty())continue;
                if(line.startsWith("[")){if(!group.isEmpty())put(group,ids);group=line.substring(1,line.length()-1);ids.clear();}
                else{ids.add(TEXT.size());TEXT.add(line);}}
            put(group,ids);loaded=true;}}
        UsageWindow w=snapshot.longWindow();
        String group=SpicyRotation.group(w==null?Double.NaN:w.usedPercent,daily,fresh,bonus,w==null?0:w.effectiveResetAtMillis(snapshot.fetchedAtMillis)-now,w==null?0:w.windowSeconds,AppPreferences.getLastError(c)!=null&&!AppPreferences.getLastError(c).isEmpty());
        SharedPreferences prefs=c.getSharedPreferences("spicy_quote_ids",0);String raw=prefs.getString(account,"");SpicyRotation.State state=new SpicyRotation.State();
        try{JSONObject j=new JSONObject(raw);state.current=j.optInt("id",-1);state.group=j.optString("group","");state.generation=j.optLong("generation",Long.MIN_VALUE);
            JSONArray ids=j.optJSONArray("recent");if(ids!=null)for(int i=Math.max(0,ids.length()-20);i<ids.length();i++)state.recent[state.size++]=ids.getInt(i);}catch(Exception ignored){}
        int priorId=state.current;long priorGeneration=state.generation;
        int id=state.choose(group,GROUPS.get(group),request(),RANDOM);if(id<0)return null;
        JSONArray recent=new JSONArray();for(int i=0;i<state.size;i++)recent.put(state.recent[i]);
        if(id!=priorId||priorGeneration!=state.generation)prefs.edit().putString(account,new JSONObject().put("id",id).put("group",group).put("generation",state.generation).put("recent",recent).toString()).apply();return TEXT.get(id);
        }catch(Exception ignored){if(!loaded){TEXT.clear();GROUPS.clear();}return null;}
    }
    private static void put(String group,List<Integer> list){int[] ids=new int[list.size()];for(int i=0;i<ids.length;i++)ids[i]=list.get(i);GROUPS.put(group,ids);}
}
