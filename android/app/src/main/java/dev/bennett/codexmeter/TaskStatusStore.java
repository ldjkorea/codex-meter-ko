package dev.bennett.codexmeter;

import android.content.Context;
import org.json.JSONObject;

final class TaskStatusStore {
    private static final String PREFS="codex_task_trial";
    static boolean enabled(Context c){try{return enabled(c,LanSync.account(c));}catch(Exception ignored){return false;}}
    static boolean enabled(Context c,String account){return account!=null&&c.getSharedPreferences(PREFS,0).getBoolean(account+"_on",false);}
    static void enable(Context c,boolean on)throws Exception{String a=LanSync.account(c);if(a==null)throw new IllegalStateException();c.getSharedPreferences(PREFS,0).edit().putBoolean(a+"_on",on).apply();TaskStatusWidget.update(c);TaskStatusJobService.schedule(c);}
    static void save(Context c,String a,JSONObject incoming)throws Exception{
        if(!enabled(c,a))return;JSONObject clean=TaskStatusData.clean(incoming,System.currentTimeMillis());
        JSONObject prior=read(c,a);if(!TaskStatusData.newer(prior,clean))return;
        String previous=widgetText(c,3);
        c.getSharedPreferences(PREFS,0).edit().putString(a,clean.toString()).putLong(a+"_received",System.currentTimeMillis()).apply();
        if(!previous.equals(widgetText(c,3)))TaskStatusWidget.update(c);
    }
    private static JSONObject read(Context c,String a){try{return new JSONObject(c.getSharedPreferences(PREFS,0).getString(a,""));}catch(Exception ignored){return null;}}
    static String widgetText(Context c,int maximum){return text(c,maximum);}
    static String homeText(Context c){
        try{String a=LanSync.account(c);JSONObject data=a==null?null:read(c,a);
            long received=a==null?0:c.getSharedPreferences(PREFS,0).getLong(a+"_received",0),now=System.currentTimeMillis();
            if(!TaskStatusData.connected(data,received,now))return c.getString(R.string.hud_task_row,c.getString(R.string.task_trial_disconnected));
            int running=0;org.json.JSONArray rows=data.optJSONArray("rows");if(rows!=null)for(int i=0;i<rows.length();i++)if(rows.optJSONObject(i).optString("state").equals("running"))running++;
            return c.getString(R.string.hud_task_row,running>0?c.getString(R.string.hud_running_count,running):c.getString(R.string.task_trial_connected));
        }catch(Exception ignored){return c.getString(R.string.hud_task_row,c.getString(R.string.task_trial_unverified));}
    }
    static String text(Context c,int maximum){try{String a=LanSync.account(c);if(!enabled(c,a))return c.getString(R.string.task_trial_off);
        JSONObject data=read(c,a);long received=c.getSharedPreferences(PREFS,0).getLong(a+"_received",0),now=System.currentTimeMillis();
        boolean connected=TaskStatusData.connected(data,received,now);
        StringBuilder out=new StringBuilder(c.getString(connected?R.string.task_trial_connected:R.string.task_trial_disconnected));
        if(received>0)out.append("\n").append(c.getString(R.string.task_trial_last,android.text.format.DateFormat.getTimeFormat(c).format(new java.util.Date(received))));
        if(data==null||!data.optBoolean("verified"))out.append("\n\n").append(c.getString(R.string.task_trial_unverified));
        if(data!=null){org.json.JSONArray rows=data.optJSONArray("rows");if(rows!=null)for(int i=0;i<Math.min(maximum,rows.length());i++){JSONObject r=rows.getJSONObject(i);String state=r.getString("state");
            out.append("\n\nCodex · ").append(r.getString("thread").substring(0,6)).append("\n");out.append(c.getString(R.string.task_trial_last_state)).append(" ");
            out.append(c.getString(state.equals("running")?R.string.task_trial_running:state.equals("completed")?R.string.task_trial_completed:R.string.task_trial_interrupted));
            out.append(" · ").append(android.text.format.DateFormat.getTimeFormat(c).format(new java.util.Date(r.getLong("at"))));}}
        return out.toString();}catch(Exception ignored){return c.getString(R.string.task_trial_unverified);}}
}
