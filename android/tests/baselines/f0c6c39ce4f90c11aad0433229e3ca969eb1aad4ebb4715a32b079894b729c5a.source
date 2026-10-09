package dev.bennett.codexmeter;

import android.content.Context;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Locale;
import org.json.JSONArray;
import org.json.JSONObject;

/** Billing preference only: isolated by account/plan, no auth bytes or ledger mutation. */
final class SubscriptionStore {
    private SubscriptionStore(){}
    static String key(Context context,UsageSnapshot snapshot){
        AuthTokens tokens=SecureTokenStore.load(context);
        if(tokens==null||tokens.accountId.isEmpty()||snapshot==null)return null;
        try{byte[] hash=java.security.MessageDigest.getInstance("SHA-256").digest(
                (tokens.accountId+"|"+LedgerRecord.cleanPlan(snapshot.planType)).getBytes(StandardCharsets.UTF_8));
            StringBuilder key=new StringBuilder();for(byte value:hash)key.append(String.format(Locale.ROOT,"%02x",value&255));return key.toString();
        }catch(Exception ignored){return null;}
    }
    static SubscriptionCost load(Context context,UsageSnapshot snapshot){
        String key=key(context,snapshot);if(key==null)return null;
        try{JSONObject row=new JSONObject(context.getSharedPreferences("codex_subscription_cost",0).getString(key,""));
            return new SubscriptionCost(row.getString("amount"),row.getString("currency"),LocalDate.parse(row.getString("start")),LocalDate.parse(row.getString("end")));
        }catch(Exception ignored){return null;}
    }
    static boolean save(Context context,UsageSnapshot snapshot,String expectedKey,SubscriptionCost cost){
        synchronized(UsageApi.NETWORK_LOCK){
            if(expectedKey==null||!expectedKey.equals(key(context,AppPreferences.loadSnapshot(context))))return false;
            try{JSONObject row=new JSONObject().put("amount",cost.amount.toPlainString()).put("currency",cost.currency)
                    .put("start",cost.start.toString()).put("end",cost.end.toString());
                String historyText=context.getSharedPreferences("codex_subscription_cost",0).getString(expectedKey+"_history","");
                JSONArray history=historyText.isEmpty()?new JSONArray():new JSONArray(historyText);
                String previous=context.getSharedPreferences("codex_subscription_cost",0).getString(expectedKey,"");
                if(history.length()==0&&!previous.isEmpty())history.put(new JSONObject(previous).put("saved_at",0));
                JSONObject revision=new JSONObject(row.toString()).put("saved_at",System.currentTimeMillis());
                if(history.length()==0||!same(history.getJSONObject(history.length()-1),row))history.put(revision);
                android.content.SharedPreferences prefs=context.getSharedPreferences("codex_subscription_cost",0);
                boolean saved=prefs.edit().putString(expectedKey,row.toString()).putString(expectedKey+"_history",history.toString()).commit();
                if(!saved)prefs.edit().putString(expectedKey,previous).putString(expectedKey+"_history",historyText).commit();
                return saved;
            }catch(Exception ignored){return false;}
        }
    }
    private static boolean same(JSONObject a,JSONObject b)throws Exception{return a.getString("amount").equals(b.getString("amount"))&&a.getString("currency").equals(b.getString("currency"))&&a.getString("start").equals(b.getString("start"))&&a.getString("end").equals(b.getString("end"));}
    static JSONArray history(Context context,UsageSnapshot snapshot)throws Exception{
        synchronized(UsageApi.NETWORK_LOCK){String key=key(context,snapshot);if(key==null)return new JSONArray();
            String text=context.getSharedPreferences("codex_subscription_cost",0).getString(key+"_history","");
            if(!text.isEmpty())return new JSONArray(text);
            String previous=context.getSharedPreferences("codex_subscription_cost",0).getString(key,"");
            return previous.isEmpty()?new JSONArray():new JSONArray().put(new JSONObject(previous).put("saved_at",0));}
    }
}
