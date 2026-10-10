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
            String issue=r.optString("Issue","");long issueAt=r.optLong("IssueAt",0);
            if(!(issue.equals("command_error")||issue.equals("tool_error"))||issueAt<=0||issueAt>now+300000){issue="";issueAt=0;}
            rows.put(new JSONObject().put("thread",thread).put("turn",turn).put("state",state).put("name",name(r.optString("Name",""))).put("issue",issue).put("issue_at",issueAt).put("at",at).put("seen",seen).put("seq",seq));}
        return new JSONObject().put("epoch",epoch).put("device",device).put("checked",checked).put("verified",data.getBoolean("verified")).put("rows",rows);
    }
    public static boolean newer(JSONObject prior,JSONObject next){
        if(prior==null)return true;
        if(next.optLong("checked")<prior.optLong("checked"))return false;
        if(!prior.optString("epoch").equals(next.optString("epoch")))return true;
        JSONArray left=prior.optJSONArray("rows"),right=next.optJSONArray("rows");
        long previous=left==null||left.length()==0?0:left.optJSONObject(0).optLong("seq");
        long incoming=right==null||right.length()==0?0:right.optJSONObject(0).optLong("seq");
        return incoming>=previous;
    }
    public static boolean connected(JSONObject data,long received,long now){
        long checked=data==null?0:data.optLong("checked");
        return received>0&&received<=now&&now-received<=120000&&checked>0&&checked<=now+300000&&Math.abs(now-checked)<=120000;
    }
    public static String name(String value){
        String text=value==null?"":value.replaceAll("[\\p{Cntrl}\\u2028\\u2029]"," ").replaceAll("\\s+"," ").trim();
        text=text.replaceAll("(?i)(bearer\\s+\\S+|sk-[a-z0-9_-]{12,}|eyJ[a-z0-9_-]+\\.[a-z0-9_-]+\\.[a-z0-9_-]+)","[redacted]");
        return text.codePointCount(0,text.length())>80?text.substring(0,text.offsetByCodePoints(0,80))+"…":text;
    }
}
