package dev.bennett.codexmeter;

import android.content.Context;
import java.nio.file.Files;
import java.time.LocalDate;

/** Production store with file-backed preference fixtures; not Android runtime evidence. */
public final class SubscriptionStoreSelfTest {
    private static int checks;
    private static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    private static AuthTokens account(String id){return new AuthTokens("fixture","fixture","",0,id,"");}
    public static void main(String[] args)throws Exception{
        Context context=new Context(Files.createTempDirectory("subscription-fixture"));
        UsageSnapshot pro=new UsageSnapshot("pro",true,false,null,null,1);
        UsageSnapshot plus=new UsageSnapshot("plus",true,false,null,null,2);
        SecureTokenStore.current=account("A");AppPreferences.current=pro;
        SubscriptionCost cost=new SubscriptionCost("30000.00","KRW",LocalDate.of(2026,10,8),LocalDate.of(2026,11,8));
        String key=SubscriptionStore.key(context,pro);
        check(key.matches("[a-f0-9]{64}"),"Account ID is not written as preference key");
        check(SubscriptionStore.load(context,pro)==null,"No assumed payment before entry");
        check(SubscriptionStore.save(context,pro,key,cost),"Commit confirms payment save");
        Context restarted=new Context(context.directory);
        SubscriptionCost loaded=SubscriptionStore.load(restarted,pro);
        check(loaded!=null&&loaded.amount.toPlainString().equals("30000.00")&&loaded.end.equals(cost.end),"New context reloads amount/currency/dates from disk");
        check(SubscriptionStore.load(restarted,plus)==null,"Plan-scoped payment cannot leak into other plan");
        AppPreferences.current=plus;
        check(!SubscriptionStore.save(context,pro,key,cost),"Plan changed during edit cannot save stale payment");
        AppPreferences.current=pro;SecureTokenStore.current=account("B");
        check(SubscriptionStore.load(context,pro)==null,"Other account does not read account A payment");
        check(!SubscriptionStore.save(context,pro,key,cost),"Account changed during edit rejects save");
        SecureTokenStore.current=null;
        check(SubscriptionStore.key(context,pro)==null&&SubscriptionStore.load(context,pro)==null,"Logout exposes no payment");
        SecureTokenStore.current=account("A");
        check(SubscriptionStore.load(restarted,pro)!=null,"Returning account retains its payment");
        SubscriptionCost revised=new SubscriptionCost("129.99","USD",LocalDate.of(2026,10,7),LocalDate.of(2026,11,7));
        check(SubscriptionStore.save(context,pro,key,revised),"User-entered tax-inclusive payment can be edited");
        SubscriptionCost saved=SubscriptionStore.load(new Context(context.directory),pro);
        check(saved.amount.toPlainString().equals("129.99")&&saved.currency.equals("USD")&&saved.start.equals(revised.start)&&saved.end.equals(revised.end),"Tax-inclusive amount, currency and edited dates survive restart without added tax or FX");
        check(SubscriptionStore.history(restarted,pro).length()==2,"Previous payment preserved when new amount and currency are entered");
        Context.failWrites=true;
        check(!SubscriptionStore.save(context,pro,key,cost),"Disk write failure cannot claim successful save");
        Context.failWrites=false;
        context.getSharedPreferences("codex_subscription_cost",0).edit().putString(key,"invalid-json").commit();
        check(SubscriptionStore.load(restarted,pro)==null,"Corrupt payment yields unknown instead of fabricated price");
        check(restarted.getSharedPreferences("codex_subscription_cost",0).getString(key,"").equals("invalid-json"),"Failed read never overwrites original preference");
        System.out.println("Subscription persistence/account/plan/write-failure fixtures: "+checks+" assertions passed.");
    }
}
