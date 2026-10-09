package dev.bennett.codexmeter;
import android.content.Context;
import java.lang.reflect.Field;
import java.util.*;

/** Production DAO + real SQLite behind existing JVM Android fixtures. No installed app data. */
public final class LedgerLanMergeSelfTest {
    static int checks;
    static void check(boolean value,String note){checks++;if(!value)throw new AssertionError(note);}
    static void restart()throws Exception{Field field=UsageLedgerDatabase.class.getDeclaredField("instance");field.setAccessible(true);UsageLedgerDatabase owner=(UsageLedgerDatabase)field.get(null);owner.close();field.set(null,null);}
    public static void main(String[] args)throws Exception{
        Context c=new Context();long now=System.currentTimeMillis(),hour=3600000L,end=now+3*86400000L;
        UsageSnapshot snapshot=new UsageSnapshot("pro",true,false,null,new UsageWindow(20,604800,0,end/1000),now);
        check(UsageLedgerDatabase.record(c,snapshot,null,false),"initial mobile observation");long floor=UsageLedgerDatabase.lanFloor(c);check(floor==0,"fresh ledger has no deletion floor");
        LedgerRecord pc=new LedgerRecord("weekly","pro",now-hour,15.125,"15.125",end/1000*1000,604800,"api_precise",false);
        UsageLedgerDatabase.mergeLan(c,Arrays.asList(pc,pc),floor);UsageLedgerDatabase.Data data=UsageLedgerDatabase.load(c);
        check(data.records.size()==2,"out-of-order real PC observation merged once");check(data.records.get(0).used==15.125&&data.records.get(0).decimal.equals("15.125"),"precision preserved");
        check(data.days.size()==1&&Math.abs(data.days.get(0).points-4.875)<1e-9,"daily aggregation recalculated from shared observations");
        UsageLedgerDatabase.mergeLan(c,Collections.singletonList(pc),floor);check(UsageLedgerDatabase.load(c).records.size()==2,"retry idempotence");restart();check(UsageLedgerDatabase.load(c).records.size()==2,"merged records persist on restart");
        UsageLedgerDatabase.mergeLan(c,Collections.singletonList(new LedgerRecord("weekly","pro",now-91L*86400000,1,"1",end,604800,"api_precise",false)),floor);
        check(UsageLedgerDatabase.load(c).records.size()==2,"old raw observation cannot rewrite sealed daily archive");
        UsageLedgerDatabase.clear(c);long cleared=UsageLedgerDatabase.lanFloor(c);check(cleared>=now,"durable clear watermark");
        UsageLedgerDatabase.mergeLan(c,Collections.singletonList(pc),cleared);check(UsageLedgerDatabase.load(c).records.isEmpty(),"peer cannot resurrect deleted history");
        boolean rejected=false;try{UsageLedgerDatabase.mergeLan(c,Collections.singletonList(pc),floor);}catch(IllegalStateException expected){rejected=true;}check(rejected,"inflight stale merge rejected after clear");
        restart();check(UsageLedgerDatabase.lanFloor(c)==cleared,"clear watermark persists on restart");
        System.out.println("LAN production SQLite merge: "+checks+" assertions passed (JVM platform fixtures, not physical Android).");
    }
}
