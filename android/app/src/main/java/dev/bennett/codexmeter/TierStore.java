package dev.bennett.codexmeter;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

/** Separate versioned journal: old fun settlements and rating are never rewritten. */
@android.annotation.SuppressLint("ApplySharedPref")
final class TierStore {
    private static final String PREFS="codex_tier_evolution_v2";
    static JSONObject load(Context c,String key)throws Exception {
        String raw=c.getSharedPreferences(PREFS,0).getString(key,"");
        JSONObject doc=raw.isEmpty()?new JSONObject().put("schema",2).put("policies",new JSONObject()):new JSONObject(raw);
        if(doc.getInt("schema")!=2)throw new IllegalStateException("Unsupported tier journal");return doc;
    }
    static void evaluate(Context c,String key,String policy,List<FunInsights.Window> windows)throws Exception {
        synchronized(UsageApi.NETWORK_LOCK){
            if(!key.equals(SubscriptionStore.key(c,AppPreferences.loadSnapshot(c))))return;
            JSONObject doc=load(c,key),policies=doc.getJSONObject("policies");
            JSONObject journal=policies.optJSONObject(policy);
            if(journal==null){journal=new JSONObject().put("inputs",new JSONArray()).put("evaluations",new JSONArray());policies.put(policy,journal);}
            JSONArray inputs=journal.getJSONArray("inputs"),evals=journal.getJSONArray("evaluations");
            // Import only previously certified, complete windows. Keep all legacy recaps intact.
            if(!journal.optBoolean("legacy_imported")){
                JSONArray legacy=FunStore.load(c,key).getJSONArray("settlements");
                for(int i=0;i<legacy.length();i++){JSONObject w=legacy.getJSONObject(i);
                    if(w.getString("id").equals(policy+"@"+w.getLong("end"))&&"near_boundary_observations_no_interpolation".equals(w.optString("quality")))
                        add(inputs,new TierEvolution.Evidence(policy,w.getLong("end"),Double.parseDouble(w.getString("used")),w.getInt("observations")));}
                journal.put("legacy_imported",true);
            }
            for(FunInsights.Window w:windows)if(w.policy.equals(policy))add(inputs,new TierEvolution.Evidence(w.policy,w.span.end,w.used.doubleValue(),w.observations));
            List<TierEvolution.Evidence> evidence=new ArrayList<>();
            for(int i=0;i<inputs.length();i++){JSONObject e=inputs.getJSONObject(i);evidence.add(new TierEvolution.Evidence(policy,e.getLong("end"),e.getDouble("used"),e.getInt("observations")));}
            evidence.sort(java.util.Comparator.comparingLong(e->e.end));
            TierEvolution.State state=state(journal.optJSONObject("state"));boolean changed=false;
            for(TierEvolution.Evidence e:evidence){TierEvolution.State next=TierEvolution.evaluate(state,evidence,policy,e.end);if(next==state)continue;
                List<Long> selected=new ArrayList<>();for(TierEvolution.Evidence v:evidence)if(v.end<=e.end&&v.end>e.end-11*TierEvolution.WEEK)selected.add(v.end);
                JSONArray ids=new JSONArray();for(int i=Math.max(0,selected.size()-8);i<selected.size();i++)ids.put(selected.get(i));
                evals.put(json(next).put("at",System.currentTimeMillis()).put("window",policy+"@"+e.end).put("evidence",ids).put("rule_version",TierEvolution.VERSION));state=next;changed=true;}
            journal.put("state",json(state));
            android.content.SharedPreferences p=c.getSharedPreferences(PREFS,0);String before=p.getString(key,""),revision=p.getString("revision","");
            if(!doc.toString().equals(before)&&!p.edit().putString(key,doc.toString()).putString("revision",java.util.UUID.randomUUID().toString()).commit()){
                p.edit().putString(key,before).putString("revision",revision).commit();throw new IllegalStateException("Tier journal write failed");}
            if(changed)EvolutionWidget.updateAll(c);
        }
    }
    private static void add(JSONArray list,TierEvolution.Evidence e)throws Exception {
        for(int i=0;i<list.length();i++)if(list.getJSONObject(i).getLong("end")==e.end)return;
        list.put(new JSONObject().put("end",e.end).put("used",e.used).put("observations",e.observations));
    }
    static TierEvolution.State read(Context c,String policy){try{String key=SubscriptionStore.key(c,AppPreferences.loadSnapshot(c));if(key==null)return TierEvolution.State.empty();
        JSONObject j=load(c,key).getJSONObject("policies").optJSONObject(policy);return state(j==null?null:j.optJSONObject("state"));}catch(Exception ignored){return TierEvolution.State.empty();}}
    static String policy(Context c){UsageSnapshot s=AppPreferences.loadSnapshot(c);return s==null||s.weekly==null?"": "weekly|"+LedgerRecord.cleanPlan(s.planType)+"|"+s.weekly.windowSeconds;}
    static FunInsights.Rating rating(Context c,String policy){TierEvolution.State s=read(c,policy);return new FunInsights.Rating(s.tier,s.count,s.score,s.provisional());}
    private static TierEvolution.State state(JSONObject j)throws Exception{return j==null?TierEvolution.State.empty():new TierEvolution.State(j.getInt("tier"),j.getInt("previous"),j.getInt("count"),j.getInt("low_streak"),j.getLong("end"),j.getDouble("score"));}
    private static JSONObject json(TierEvolution.State s)throws Exception{return new JSONObject().put("tier",s.tier).put("previous",s.previous).put("count",s.count).put("low_streak",s.lowStreak).put("end",s.end).put("score",s.score);}
}
