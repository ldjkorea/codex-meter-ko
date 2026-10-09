package dev.bennett.codexmeter;

import android.content.Context;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONObject;

/** Actual app stores on durable JVM fixtures. Does not claim Android lifecycle/UI proof. */
public final class FunPersistenceSelfTest {
    private static int checks;
    private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    public static void main(String[] args)throws Exception{
        Context c=new Context(Files.createTempDirectory("fun-prefs"));long now=System.currentTimeMillis();
        TestNote note=new TestNote("note-1","my draft","home","2.8.4","34",now,now,1,0,true);
        check(TestNotesStore.save(c,note),"Draft disk commit");Context reopened=new Context(c.directory);
        check(TestNotesStore.load(reopened).get(0).text.equals("my draft"),"Restart from disk");
        check(TestNotesStore.setEnabled(c,true)&&TestNotesStore.enabled(reopened),"Test mode ON");
        check(TestNotesStore.setEnabled(c,false)&&TestNotesStore.load(reopened).size()==1,"OFF hides entry without deleting notes");
        check(TestNotesStore.setEnabled(c,true)&&TestNotesStore.load(reopened).size()==1,"ON restores notes");
        TestNote edited=new TestNote("note-1","edited","home","2.8.4","34",now,now+1,2,1,false);
        check(TestNotesStore.save(c,edited)&&TestNotesStore.load(c).get(0).status==1,"Edit and verify status");
        TestNotesStore.save(c,note);check(TestNotesStore.load(c).get(0).text.equals("edited"),"Delayed older autosave cannot overwrite newer content");
        Context.failWrites=true;check(!TestNotesStore.save(c,new TestNote("other","fail","value","2.8.4","34",now,now,3,0,true)),"Write failure not success");Context.failWrites=false;
        check(TestNotesStore.load(reopened).size()==1,"Disk failure preserves old notes");
        check(TestNotesStore.export(c,true).contains("edited")&&TestNotesStore.export(c,false).contains("VERIFY"),"Export unresolved notes");
        TestNote done=new TestNote("note-1","edited","home","2.8.4","34",now,now+2,3,2,false);TestNotesStore.save(c,done);
        check(!TestNotesStore.export(c,true).contains("edited"),"Resolved excluded from unresolved export");
        check(TestNotesStore.delete(c,"note-1")&&!TestNotesStore.save(c,edited)&&TestNotesStore.load(c).isEmpty(),"Tombstone blocks pending save resurrection");
        TestNotesStore.save(c,new TestNote("new","preserved across account","home","2.8.4","34",now,now,1,0,false));
        SecureTokenStore.current=null;AppPreferences.current=null;
        check(TestNotesStore.load(c).size()==1,"Device notes stay on logout/account change");
        UsageSnapshot snapshot=new UsageSnapshot("pro",true,false,null,null,now);AppPreferences.current=snapshot;
        SecureTokenStore.current=new AuthTokens("fixture","fixture","",now,"A","");String key=SubscriptionStore.key(c,snapshot);
        SubscriptionCost bill=new SubscriptionCost("120000","KRW",LocalDate.of(2026,10,1),LocalDate.of(2026,11,1));
        check(SubscriptionStore.save(c,snapshot,key,bill),"Payment saved");
        check(SubscriptionStore.history(c,snapshot).length()==1,"Initial billing history");
        check(SubscriptionStore.save(c,snapshot,key,bill)&&SubscriptionStore.history(c,snapshot).length()==1,"Identical payment save does not duplicate revision");
        check(SubscriptionStore.save(c,snapshot,key,new SubscriptionCost("130000","KRW",bill.start,bill.end))&&SubscriptionStore.history(c,snapshot).length()==2,"Editing retains original payment");
        long start=now-8*86400000L,week=604800000L;
        check(FunStore.saveRule(c,key,new FunInsights.Rule("100000","KRW",604800,start)),"Play rule history saved");
        List<LedgerRecord> rows=new ArrayList<>();for(int h=0;h<=168;h+=3){long at=start+h*3600000L;if(h==168)at-=60000;rows.add(new LedgerRecord("weekly","pro",at,60.0*h/168,Double.toString(60.0*h/168),start+week,604800,"api_precise",false));}
        List<FunInsights.Window> windows=FunInsights.completed(rows,"weekly|pro|604800",now);check(windows.size()==1,"Test has comparable window");
        check(FunStore.settle(c,key,windows,(tier,tone,variant)->"Frozen coach at settlement"),"Recap committed");JSONObject initial=FunStore.load(reopened,key);
        check(initial.getJSONArray("settlements").length()==1&&initial.getJSONArray("achievements").length()==1,"First recap and achievement");
        check(initial.getJSONArray("settlements").getJSONObject(0).getString("coach_text").equals("Frozen coach at settlement"),"Actual settlement coach frozen");
        check(!FunStore.rating(initial,"weekly|pro|604800",new ArrayList<>(),java.math.BigDecimal.ZERO).provisional&&FunStore.rating(initial,"weekly|pro|604800",new ArrayList<>(),java.math.BigDecimal.ZERO).tier==5,"Recorded tier persists after raw retention/reset");
        check(FunStore.rating(initial,"monthly|pro|2592000",new ArrayList<>(),java.math.BigDecimal.ZERO).provisional,"Cached weekly rating never borrowed by different meter");
        check(initial.getDouble("best")==60,"Personal best retained independently of raw observations");
        String sealed=initial.getJSONArray("settlements").getJSONObject(0).toString();
        check(FunStore.settle(c,key,windows)&&FunStore.load(c,key).getJSONArray("settlements").length()==1,"No duplicate recap after retry/restart");
        check(FunStore.saveRule(c,key,new FunInsights.Rule("200000","KRW",604800,now)),"Change rule append");
        FunStore.settle(c,key,windows);check(FunStore.load(c,key).getJSONArray("settlements").getJSONObject(0).toString().equals(sealed),"New rule does not rewrite sealed recap");
        check(FunStore.tone(c,"off")&&FunStore.tone(reopened).equals("off"),"Coach OFF persists");
        Context.failWrites=true;check(!FunStore.saveRule(c,key,new FunInsights.Rule("300000","KRW",604800,now+1)),"Play write failure propagated");Context.failWrites=false;
        Context.failWrites=true;check(!FunStore.tone(c,"spicy"),"Tone write failure propagated");Context.failWrites=false;
        check(FunStore.tone(reopened).equals("off"),"Failed tone write retains old tone");
        List<Thread> writers=new ArrayList<>();for(int i=0;i<8;i++){final int number=i;Thread worker=new Thread(()->{if(!TestNotesStore.save(c,new TestNote("parallel-"+number,"note "+number,"home","2.8.4","34",now,now,10+number,0,false)))throw new AssertionError("Concurrent write failed");});writers.add(worker);worker.start();}for(Thread worker:writers)worker.join();
        check(TestNotesStore.load(reopened).size()==9,"Concurrent notes serialized without lost updates");
        Context legacy=new Context(Files.createTempDirectory("legacy-billing"));
        legacy.getSharedPreferences("codex_subscription_cost",0).edit().putString(key,new JSONObject().put("amount","100000").put("currency","KRW").put("start",bill.start.toString()).put("end",bill.end.toString()).toString()).commit();
        check(SubscriptionStore.history(legacy,snapshot).length()==1,"Old single billing record readable before migration");
        check(SubscriptionStore.save(legacy,snapshot,key,bill)&&SubscriptionStore.history(legacy,snapshot).length()==2,"Legacy original retained alongside new revision");
        check(SubscriptionStore.save(legacy,snapshot,key,bill)&&SubscriptionStore.history(legacy,snapshot).length()==2,"Legacy migration idempotent");
        SecureTokenStore.current=new AuthTokens("fixture","fixture","",now,"B","");
        check(!FunStore.saveRule(c,key,new FunInsights.Rule("400000","KRW",604800,now+2)),"Account switch rejects stale setting write");
        check(TestNotesStore.load(c).size()==9,"Plan/payment edits do not clear notes");
        check(TestNotesStore.clear(c)&&!TestNotesStore.save(c,note),"Explicit notes clear never recreates deleted notes");
        c.getSharedPreferences(TestNotesStore.PREFS,0).edit().putString("document","corrupt").commit();
        check(!TestNotesStore.setEnabled(c,false),"Corrupt note store never reset by toggle");
        check(c.getSharedPreferences(TestNotesStore.PREFS,0).getString("document","").equals("corrupt"),"Original corrupt text preserved");
        System.out.println("Production note/payment/play/recap stores on JVM file fixtures: "+checks+" assertions passed.");
    }
}
