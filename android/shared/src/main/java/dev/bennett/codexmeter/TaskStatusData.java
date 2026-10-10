package dev.bennett.codexmeter;

import org.json.JSONArray;
import org.json.JSONObject;

/** Strict metadata allow-list, not a transcript or an inferred project outcome. */
public final class TaskStatusData {
    private TaskStatusData() {}
    public static JSONObject clean(JSONObject data,long now)throws Exception {
        if(data==null)return new JSONObject().put("verified",false).put("rows",new JSONArray()).put("checked",0);
        String epoch=data.getString("epoch"),device=data.getString("device");
        if(!epoch.matches("[a-f0-9]{32}")||!device.matches("[a-f0-9]{64}")||!data.getString("source").equals("local_log_trial"))throw new IllegalArgumentException("Invalid observer");
        long checked=data.getLong("checked");if(checked<0||checked>now+300000)throw new IllegalArgumentException("Invalid check time");
        JSONArray input=data.getJSONArray("rows"),rows=new JSONArray();if(input.length()>3)throw new IllegalArgumentException("Too many tasks");
        long previous=Long.MAX_VALUE;
        for(int i=0;i<input.length();i++){JSONObject r=input.getJSONObject(i);String thread=r.getString("Thread"),turn=r.getString("Turn"),state=r.getString("State");
            long at=r.getLong("At"),seen=r.getLong("Seen"),seq=r.getLong("Sequence");
            if(!thread.matches("[a-f0-9-]{16,40}")||!turn.matches("[a-f0-9-]{16,40}")||!(state.equals("running")||state.equals("completed")||state.equals("interrupted")))throw new IllegalArgumentException("Invalid task");
            if(at<=0||at>now+300000||seen<at-300000||seen>now+300000||seq<=0||seq>=previous)throw new IllegalArgumentException("Invalid task ordering");previous=seq;
            rows.put(new JSONObject().put("thread",thread).put("turn",turn).put("state",state).put("at",at).put("seen",seen).put("seq",seq));}
        return new JSONObject().put("epoch",epoch).put("device",device).put("checked",checked).put("verified",data.getBoolean("verified")).put("rows",rows);
    }
    public static boolean newer(JSONObject prior,JSONObject next){
        if(prior==null||!prior.optString("epoch").equals(next.optString("epoch")))return true;
        return next.optLong("checked")>=prior.optLong("checked");
    }
}
