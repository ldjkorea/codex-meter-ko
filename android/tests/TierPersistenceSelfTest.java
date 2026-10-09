package dev.bennett.codexmeter;
import android.content.Context;
import java.nio.file.Files;
import java.util.*;
import org.json.JSONObject;
public final class TierPersistenceSelfTest {
    static int checks;static final String POLICY="weekly|pro|604800";
    static void check(boolean c,String m){checks++;if(!c)throw new AssertionError(m);}
    static List<FunInsights.Window> windows(long now,double... percentages){List<FunInsights.Window> r=new ArrayList<>();for(int i=0;i<percentages.length;i++){
        long end=now-(percentages.length-1-i)*TierEvolution.WEEK,start=end-TierEvolution.WEEK;
        LedgerRecord row=new LedgerRecord("weekly","pro",end-1000,percentages[i],Double.toString(percentages[i]),end,604800,"api_precise",false);
        r.add(new FunInsights.Window(POLICY,LedgerPeriods.span(end,604800,end-1000),row,40));}return r;}
    public static void main(String[] args)throws Exception {
        Context c=new Context(Files.createTempDirectory("evolution-store"));long now=System.currentTimeMillis()-10000;
        SecureTokenStore.current=new AuthTokens("fixture","fixture","",0,"A","");AppPreferences.current=new UsageSnapshot("pro",true,false,null,null,now);
        String key=SubscriptionStore.key(c,AppPreferences.current);List<FunInsights.Window> old=windows(now,75,80,85,90);
        // Real old store produces frozen recaps before the new journal exists.
        FunStore.saveRule(c,key,new FunInsights.Rule("100000","KRW",604800,now-8*TierEvolution.WEEK));FunStore.settle(c,key,old);
        String legacy=c.getSharedPreferences("codex_fun_v1",0).getString(key,"");
        TierStore.evaluate(c,key,POLICY,Collections.emptyList());TierEvolution.State s=TierStore.read(c,POLICY);
        check(s.count==4&&s.tier>=0,"Certified legacy weeks safely imported");
        check(legacy.equals(c.getSharedPreferences("codex_fun_v1",0).getString(key,"")),"Old recaps, tiers and rules byte preserved");
        JSONObject journal=TierStore.load(c,key).getJSONObject("policies").getJSONObject(POLICY);
        check(journal.getJSONArray("evaluations").length()==4,"Every completed window evaluated once");
        TierStore.evaluate(c,key,POLICY,old);check(TierStore.load(c,key).getJSONObject("policies").getJSONObject(POLICY).getJSONArray("evaluations").length()==4,"Migration and retry idempotent");
        LedgerRecord foreign=new LedgerRecord("weekly","plus",now+TierEvolution.WEEK-1000,100,"100",now+TierEvolution.WEEK,604800,"api_precise",false);
        TierStore.evaluate(c,key,POLICY,Collections.singletonList(new FunInsights.Window("weekly|plus|604800",LedgerPeriods.span(now+TierEvolution.WEEK,604800,foreign.at),foreign,40)));
        check(TierStore.read(c,POLICY).end==s.end,"Foreign policy input cannot enter the current journal");
        Context restarted=new Context(c.directory);check(TierStore.read(restarted,POLICY).tier==s.tier,"Process preference reopen persists tier");
        check(TierStore.read(c,"weekly|pro|302400").tier==-1,"Policy length isolated");
        AppPreferences.current=new UsageSnapshot("plus",true,false,null,null,now);check(TierStore.read(c,"weekly|plus|604800").tier==-1,"Changed plan does not borrow tier");
        AppPreferences.current=new UsageSnapshot("pro",true,false,null,null,now);SecureTokenStore.current=new AuthTokens("fixture","fixture","",0,"B","");
        check(TierStore.read(c,POLICY).tier==-1,"Changed account isolated");TierStore.evaluate(c,key,POLICY,old);
        check(TierStore.load(c,key).getJSONObject("policies").getJSONObject(POLICY).getJSONArray("evaluations").length()==4,"Stale account writer rejected");
        SecureTokenStore.current=null;check(TierStore.read(c,POLICY).tier==-1,"Logout hides journal");
        SecureTokenStore.current=new AuthTokens("fixture","fixture","",0,"A","");
        String before=c.getSharedPreferences("codex_tier_evolution_v2",0).getString(key,"");Context.failWrites=true;boolean failed=false;
        try{TierStore.evaluate(c,key,POLICY,windows(now+TierEvolution.WEEK,100));}catch(IllegalStateException e){failed=true;}Context.failWrites=false;
        check(failed&&before.equals(c.getSharedPreferences("codex_tier_evolution_v2",0).getString(key,"")),"Write failure preserves durable old tier and signals failure");
        TierStore.evaluate(c,key,POLICY,windows(now+TierEvolution.WEEK,100));check(TierStore.read(c,POLICY).end==now+TierEvolution.WEEK,"Successful retry commits next week");
        JSONObject malformed=TierStore.load(c,key);malformed.getJSONObject("policies").getJSONObject(POLICY).getJSONObject("state").put("tier",100);
        c.getSharedPreferences("codex_tier_evolution_v2",0).edit().putString(key,malformed.toString()).commit();
        check(TierStore.read(c,POLICY).tier==-1,"Structurally valid but out-of-range state safely hidden");
        c.getSharedPreferences("codex_tier_evolution_v2",0).edit().putString(key,"broken").commit();check(TierStore.read(c,POLICY).tier==-1,"Corrupt journal not a guessed tier");
        check(c.getSharedPreferences("codex_tier_evolution_v2",0).getString(key,"").equals("broken"),"Corrupt original journal never overwritten by read");
        System.out.println("Production tier journal on file-backed JVM preferences: "+checks+" assertions passed (not Android process evidence).");
    }
}
