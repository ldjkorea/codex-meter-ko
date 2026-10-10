package dev.bennett.codexmeter;

import java.util.Locale;
import org.json.*;

/** Real parser/serialization/formatter and metadata ordering, using synthetic responses only. */
public final class HudSelfTest {
    static int count;
    static void check(boolean ok,String why){count++;if(!ok)throw new AssertionError(why);}
    public static void main(String[] args)throws Exception {
        Locale.setDefault(Locale.KOREAN);
        double[] input={13,13.42736,13.42786,0,100,99.9996,.0004,.0006};
        String[] used={"13%","13.427%","13.428%","0%","100%","100%","0%","0.001%"};
        String[] remaining={"87%","86.573%","86.572%","100%","0%","0%","100%","99.999%"};
        for(int i=0;i<input.length;i++){
            JSONObject response=new JSONObject().put("plan_type","plus").put("rate_limit",new JSONObject().put("secondary_window",new JSONObject().put("used_percent",input[i]).put("limit_window_seconds",604800).put("reset_at",1792000000L)));
            UsageSnapshot s=UsageParser.parse(response.toString(),1791500000000L);UsageWindow w=s.weekly;
            check(s.fiveHour==null,"No phantom five-hour window");
            check(w.usedPercent==(int)Math.round(input[i]),"Legacy evaluation is unchanged");
            check(UsagePrecision.used(w).equals(used[i]),"Used rounding "+input[i]);
            check(UsagePrecision.remaining(w).equals(remaining[i]),"Remaining from raw value "+input[i]);
            UsageSnapshot restored=UsageSnapshot.fromJson(s.toJson());
            check(restored.weekly.preciseUsedPercent==input[i],"Cache/Wear serialization preserves raw precision");
            check(s.toJson().getJSONObject("weekly").getInt("used_percent")==w.usedPercent,"Old readers retain integer contract");
            LedgerRecord record=LedgerCapture.fromSnapshot(s,response.toString(),true).get(0);
            check(record.used==input[i]&&record.manual,"Actual ledger capture with precise source");
        }
        UsageWindow old=UsageWindow.fromJson(new JSONObject().put("used_percent",13).put("limit_window_seconds",604800));
        check(UsagePrecision.used(old).equals("13%")&&!old.toJson().has("used_percent_precise"),"No invented legacy decimals");
        check(UsageWindow.fromJson(new JSONObject().put("used_percent",13).put("used_percent_precise",99.999).put("limit_window_seconds",604800)).preciseUsedPercent==13,"Conflicting precision cannot override legacy value");
        check(UsagePrecision.number(Double.NaN).equals("—"),"Unknown is not zero");
        long now=System.currentTimeMillis();JSONObject snapshot=new JSONObject().put("epoch","a".repeat(32)).put("device","b".repeat(64)).put("source","local_log_trial").put("verified",true).put("checked",now).put("rows",new JSONArray().put(row("running",2,now)));
        JSONObject clean=TaskStatusData.clean(snapshot,now);
        check(TaskStatusData.connected(clean,now,now),"Connected heartbeat");
        check(!TaskStatusData.connected(clean,now,now+120001),"Stale without fabricated failure");
        check(!TaskStatusData.connected(clean,now+1,now),"Future receive time rejected");
        JSONObject reversed=new JSONObject(clean.toString()).put("checked",now+1);reversed.getJSONArray("rows").getJSONObject(0).put("seq",1);
        check(!TaskStatusData.newer(clean,reversed),"Older turn event cannot replace newer running turn");
        JSONObject restart=new JSONObject(clean.toString()).put("epoch","c".repeat(32)).put("checked",now+1);restart.getJSONArray("rows").getJSONObject(0).put("seq",1);
        check(TaskStatusData.newer(clean,restart),"New collector epoch recovers with current snapshot");
        check(!TaskStatusData.newer(restart,clean),"Previous collector cannot overwrite restart");
        check(!TaskStatusData.newer(clean,TaskStatusData.clean(null,now)),"Missing snapshot does not erase last known state");
        for(String state:new String[]{"running","completed","interrupted"}){snapshot.put("rows",new JSONArray().put(row(state,3,now)));check(TaskStatusData.clean(snapshot,now).getJSONArray("rows").getJSONObject(0).getString("state").equals(state),"Observed "+state);}
        snapshot.put("rows",new JSONArray().put(row("running",4,now)).put(row("completed",3,now)).put(row("interrupted",2,now)));
        check(TaskStatusData.clean(snapshot,now).getJSONArray("rows").length()==3,"Concurrent threads remain separate");
        System.out.println("HUD precision/parser/serialization/ledger/connection ordering: "+count+" assertions passed (synthetic).");
    }
    static JSONObject row(String state,long sequence,long at)throws Exception{return new JSONObject().put("Thread",Long.toHexString(sequence)+"d".repeat(31)).put("Turn","e".repeat(32)).put("State",state).put("At",at).put("Seen",at).put("Sequence",sequence);}
}
