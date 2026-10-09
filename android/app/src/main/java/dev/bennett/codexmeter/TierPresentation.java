package dev.bennett.codexmeter;
import android.content.Context;
import android.widget.LinearLayout;
final class TierPresentation {
    static final int[] NAMES={R.string.fun_tier_0,R.string.fun_tier_1,R.string.fun_tier_2,R.string.fun_tier_3,R.string.fun_tier_4,R.string.fun_tier_5,R.string.fun_tier_6,R.string.fun_tier_7,R.string.fun_tier_8,R.string.fun_tier_9};
    static String note(Context c,String policy){TierEvolution.State s=TierStore.read(c,policy);
        return c.getString(s.tier<0?R.string.evo_placing:s.provisional()?R.string.fun_provisional:R.string.evo_stable);}
    static void details(Context c,LinearLayout card,String policy,LedgerPeriods.Span span,boolean dark){
        TierEvolution.State s=TierStore.read(c,policy);
        if(s.previous>=0){String previous=c.getString(NAMES[s.previous]);String outcome=c.getString(s.tier>s.previous?R.string.evo_promoted:s.tier<s.previous?R.string.evo_demoted:R.string.evo_held);
            card.addView(LedgerUi.caption(c,c.getString(R.string.evo_result,previous,outcome),dark));}
        if(span!=null&&policy.startsWith("weekly|"))card.addView(LedgerUi.caption(c,c.getString(R.string.evo_next,V3Display.range(span.start,span.end)),dark));
    }
    static void history(Context c,LinearLayout parent,String policy,boolean dark){try{
        String key=SubscriptionStore.key(c,AppPreferences.loadSnapshot(c));if(key==null)return;
        org.json.JSONObject journal=TierStore.load(c,key).getJSONObject("policies").optJSONObject(policy);if(journal==null)return;
        org.json.JSONArray rows=journal.getJSONArray("evaluations");LedgerUi.section(parent,c.getString(R.string.evo_history),dark);
        for(int i=rows.length()-1;i>=Math.max(0,rows.length()-3);i--){org.json.JSONObject r=rows.getJSONObject(i);int tier=r.getInt("tier"),previous=r.getInt("previous");LinearLayout card=Ui.card(c,dark);TierTheme.frame(card,tier,dark);
            card.addView(LedgerUi.caption(c,V3Display.range(r.getLong("end")-TierEvolution.WEEK,r.getLong("end")),dark));
            card.addView(LedgerUi.heading(c,tier<0?c.getString(R.string.fun_placing):c.getString(NAMES[tier]),dark));
            if(previous>=0)card.addView(LedgerUi.caption(c,c.getString(R.string.evo_result,c.getString(NAMES[previous]),c.getString(tier>previous?R.string.evo_promoted:tier<previous?R.string.evo_demoted:R.string.evo_held)),dark));
            parent.addView(card);Ui.addSpacer(parent,10);
        }
    }catch(Exception ignored){parent.addView(LedgerUi.caption(c,c.getString(R.string.fun_load_failed),dark));}}
}
