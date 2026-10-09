package dev.bennett.codexmeter;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.widget.NestedScrollView;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import org.json.JSONArray;
import org.json.JSONObject;

/** Independent value playground. Reads actual quota/ledger, writes only interpretation history. */
public final class FunActivity extends AppCompatActivity {
    private static final ExecutorService WORKER=Executors.newSingleThreadExecutor();
    private static final int[] TIERS={R.string.fun_tier_0,R.string.fun_tier_1,R.string.fun_tier_2,R.string.fun_tier_3,R.string.fun_tier_4,R.string.fun_tier_5,R.string.fun_tier_6,R.string.fun_tier_7,R.string.fun_tier_8,R.string.fun_tier_9};
    private static final int[] BADGES={R.drawable.badge_iron,R.drawable.badge_bronze,R.drawable.badge_silver,R.drawable.badge_gold,R.drawable.badge_platinum,R.drawable.badge_emerald,R.drawable.badge_diamond,R.drawable.badge_master,R.drawable.badge_grandmaster,R.drawable.badge_challenger};
    private LinearLayout content;private boolean dark;private int scroll;private long generation;
    @Override protected void onCreate(Bundle state){Ui.applySelectedTheme(this);super.onCreate(state);dark=Ui.isDark(this);content=Ui.installPage(this,getString(R.string.fun_title),true).content;if(state!=null)scroll=state.getInt("scroll");}
    @Override protected void onResume(){super.onResume();reload();}
    @Override protected void onPause(){scroll=((NestedScrollView)findViewById(R.id.dashboard_scroll)).getScrollY();NotesUi.flush(this);super.onPause();}
    @Override protected void onDestroy(){NotesUi.close(this);super.onDestroy();}
    @Override protected void onSaveInstanceState(Bundle state){super.onSaveInstanceState(state);state.putInt("scroll",((NestedScrollView)findViewById(R.id.dashboard_scroll)).getScrollY());}
    @Override public boolean onSupportNavigateUp(){finish();return true;}
    @Override public boolean onCreateOptionsMenu(Menu menu){menu.add(0,8600,0,R.string.fun_settings);NotesUi.menu(this,menu,"value");return true;}
    @Override public boolean onPrepareOptionsMenu(Menu menu){NotesUi.prepare(this,menu);return super.onPrepareOptionsMenu(menu);}
    @Override public boolean onOptionsItemSelected(MenuItem item){if(NotesUi.select(this,item,"value"))return true;if(item.getItemId()==8600){startActivity(new Intent(this,FunSettingsActivity.class));return true;}return super.onOptionsItemSelected(item);}
    private void reload(){
        long request=++generation;UsageSnapshot snapshot=AppPreferences.loadSnapshot(this);String key=SubscriptionStore.key(this,snapshot);
        String stamp=LiveUsageStore.signature(this,snapshot);
        if(key==null){content.removeAllViews();content.addView(LedgerUi.tile(this,getString(R.string.fun_placing),"—",getString(R.string.v3_signin_note),dark));return;}
        WORKER.execute(()->{try{
            UsageLedgerDatabase.Data data=UsageLedgerDatabase.load(getApplicationContext());
            UsageWindow window=snapshot.weekly!=null?snapshot.weekly:snapshot.monthly;
            String meter=snapshot.weekly!=null?"weekly":"monthly";
            String policy=window==null?"":meter+"|"+LedgerRecord.cleanPlan(snapshot.planType)+"|"+window.windowSeconds;
            List<FunInsights.Window> completed=FunInsights.completed(data.records,policy,System.currentTimeMillis());
            if(!FunStore.settle(getApplicationContext(),key,completed,(tier,tone,variant)->FunCoach.text(getApplicationContext(),tone,tier,FunInsights.Situation.TIER,variant)))throw new IllegalStateException("Save failed");
            TierStore.evaluate(getApplicationContext(),key,policy,completed);
            JSONObject doc=FunStore.load(getApplicationContext(),key);
            LiveUtilization.Result live=LiveUsageStore.calculate(getApplicationContext(),snapshot,data);
            double daily=LiveCards.daily(data,policy,System.currentTimeMillis());
            runOnUiThread(()->{if(isDestroyed()||isFinishing()||request!=generation)return;if(!key.equals(SubscriptionStore.key(this,AppPreferences.loadSnapshot(this)))||!stamp.equals(LiveUsageStore.signature(this,AppPreferences.loadSnapshot(this)))){reload();return;}
                try{render(snapshot,window,policy,data.records,completed,doc,live,daily);}catch(Exception ignored){error();}});
        }catch(Exception ignored){runOnUiThread(()->{if(!isDestroyed()&&request==generation)error();});}});
    }
    private void error(){content.removeAllViews();content.addView(LedgerUi.caption(this,getString(R.string.fun_load_failed),dark));content.addView(LedgerUi.action(this,getString(R.string.ui_ledger_recovery),true,dark,this::reload));}
    private void render(UsageSnapshot snapshot,UsageWindow window,String policy,List<LedgerRecord> records,List<FunInsights.Window> completed,JSONObject doc,LiveUtilization.Result live,double daily)throws Exception{
        content.removeAllViews();long now=System.currentTimeMillis();
        BigDecimal current=window==null?null:BigDecimal.valueOf(window.usedPercent);
        LedgerRecord precise=LedgerPeriods.latest(records,policy);
        if(precise!=null&&precise.at==snapshot.fetchedAtMillis&&window!=null&&precise.reset==window.effectiveResetAtMillis(snapshot.fetchedAtMillis))current=new BigDecimal(precise.decimal);
        LedgerPeriods.Span span=window==null?null:LedgerPeriods.span(window.effectiveResetAtMillis(snapshot.fetchedAtMillis),window.windowSeconds,snapshot.fetchedAtMillis);
        boolean fresh=span!=null&&now<span.end&&UsageInsights.freshness(snapshot.fetchedAtMillis,now,RefreshScheduler.effectiveRefreshMinutes(this))==UsageInsights.Freshness.FRESH;
        content.addView(LiveCards.crest(this,dark,live,fresh));
        content.addView(LiveCards.ai(this,dark,live,snapshot,fresh,daily,now));
        content.addView(SubscriptionValueUi.card(this,dark,snapshot,live,fresh));Ui.addSpacer(content,16);
        if(span!=null){LedgerPeriods.Measurement[] match=LedgerPeriods.matched(records,policy,span,now);
            LinearLayout compare=Ui.card(this,dark);compare.addView(LedgerUi.caption(this,match[0].comparable&&match[1].comparable?getString(R.string.v3_matched,num(match[0].points),num(match[1].points)):getString(R.string.v3_compare_missing),dark));Ui.addSpacer(content,16);content.addView(compare);}
        Ui.addSpacer(content,16);SubscriptionCost bill=SubscriptionStore.load(this,snapshot);
        LinearLayout settings=Ui.card(this,dark);settings.addView(LedgerUi.caption(this,bill==null?getString(R.string.v3_payment_missing):getString(R.string.v3_user_payment,money(bill.currency,bill.amount)),dark));

        settings.addView(LedgerUi.action(this,getString(R.string.fun_settings),false,dark,()->startActivity(new Intent(this,FunSettingsActivity.class))));content.addView(settings);
        TierPresentation.history(this,content,policy,dark);addRecaps(doc,completed);((NestedScrollView)findViewById(R.id.dashboard_scroll)).post(()->((NestedScrollView)findViewById(R.id.dashboard_scroll)).scrollTo(0,scroll));
    }
    private void addRecaps(JSONObject doc,List<FunInsights.Window> completed)throws Exception{
        LedgerUi.section(content,getString(R.string.evo_legacy_history),dark);JSONArray rows=doc.getJSONArray("settlements");
        if(rows.length()==0)content.addView(LedgerUi.caption(this,getString(R.string.fun_no_recaps),dark));
        for(int i=rows.length()-1;i>=Math.max(0,rows.length()-3);i--){JSONObject row=rows.getJSONObject(i);LinearLayout card=Ui.card(this,dark);TierTheme.frame(card,row.getInt("tier"),dark);
            card.addView(LedgerUi.heading(this,V3Display.range(row.getLong("start"),row.getLong("end")),dark));card.addView(LedgerUi.amount(this,row.getString("used")+"% · "+getString(TIERS[row.getInt("tier")]),dark,23));
            card.addView(LedgerUi.caption(this,getString(R.string.fun_recap_quality),dark));
            if(!row.isNull("previous_used")&&row.has("previous_used"))card.addView(LedgerUi.caption(this,getString(R.string.fun_recap_change,num(new BigDecimal(row.getString("used")).subtract(new BigDecimal(row.getString("previous_used"))).doubleValue())),dark));
            if(!FunStore.tone(this).equals("off")&&!row.optString("coach_text").isEmpty())card.addView(LedgerUi.caption(this,row.getString("coach_text"),dark));
            Ui.addSpacer(content,10);content.addView(card);}
        if(doc.has("best"))content.addView(LedgerUi.caption(this,getString(R.string.fun_personal_best,num(doc.getDouble("best"))),dark));
        JSONArray achievements=doc.getJSONArray("achievements");if(achievements.length()>0){StringBuilder text=new StringBuilder();for(int i=0;i<achievements.length();i++){if(i>0)text.append(" · ");text.append(getString(achievements.getString(i).equals("first_rise")?R.string.fun_first_rise:R.string.fun_first_recap));}content.addView(LedgerUi.caption(this,text.toString(),dark));}
    }
    static int indexMessage(FunInsights.IndexState state){switch(state){case BOUNDARY:return R.string.fun_index_boundary;case ZERO_CHARGE:return R.string.fun_index_zero_charge;case CURRENCY:return R.string.fun_index_currency;case PERIOD:return R.string.fun_index_period;case STALE:return R.string.fun_index_stale;case NO_RULE:return R.string.fun_index_no_rule;default:return R.string.fun_index_missing;}}
    private static int styleId(FunInsights.Style style){switch(style){case FOCUS:return R.string.fun_style_focus;case SPRINT:return R.string.fun_style_sprint;case RESERVE:return R.string.fun_style_reserve;default:return R.string.fun_style_steady;}}
    private String coach(String tone,int tier,FunInsights.Situation state,String window,long now){return FunCoach.text(this,tone,tier,state,FunInsights.variant(window,tier,state,now));}
    static String money(String currency,BigDecimal amount){java.text.NumberFormat format=java.text.NumberFormat.getNumberInstance();format.setMaximumFractionDigits(Math.max(0,java.util.Currency.getInstance(currency).getDefaultFractionDigits()));return currency+" "+format.format(amount);}
    private static String num(double value){return LedgerUi.number(value);}
    private void help(int title,int message){new AlertDialog.Builder(this).setTitle(title).setMessage(message).setPositiveButton(R.string.ui_done_e9b450,null).show();}
}
