package dev.bennett.codexmeter;

import android.app.AlertDialog;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.json.JSONObject;

/** Home-only presentation over the unchanged 2.8.4 interpretation engine and stores. */
final class FunHome {
    private static final ExecutorService WORKER=Executors.newSingleThreadExecutor();
    private static final int[] TIERS={R.string.fun_tier_0,R.string.fun_tier_1,R.string.fun_tier_2,R.string.fun_tier_3,R.string.fun_tier_4,R.string.fun_tier_5,R.string.fun_tier_6,R.string.fun_tier_7,R.string.fun_tier_8,R.string.fun_tier_9};
    private static final int[] BADGES={R.drawable.badge_iron,R.drawable.badge_bronze,R.drawable.badge_silver,R.drawable.badge_gold,R.drawable.badge_platinum,R.drawable.badge_emerald,R.drawable.badge_diamond,R.drawable.badge_master,R.drawable.badge_grandmaster,R.drawable.badge_challenger};
    private final AppCompatActivity activity;private final LinearLayout target;private final boolean dark;private final Runnable finished;
    private FunHome(AppCompatActivity activity,LinearLayout target,boolean dark,Runnable finished){this.activity=activity;this.target=target;this.dark=dark;this.finished=finished;}
    static void add(AppCompatActivity activity,LinearLayout parent,boolean dark,Runnable finished){
        LinearLayout target=new LinearLayout(activity);target.setOrientation(LinearLayout.VERTICAL);parent.addView(target);
        FunHome home=new FunHome(activity,target,dark,finished);target.addView(LedgerUi.caption(activity,activity.getString(R.string.next_loading),dark));
        UsageSnapshot snapshot=AppPreferences.loadSnapshot(activity);String key=SubscriptionStore.key(activity,snapshot);
        if(key==null){target.removeAllViews();target.addView(LedgerUi.caption(activity,activity.getString(R.string.fun_placing),dark));return;}
        WORKER.execute(()->{try{
            UsageLedgerDatabase.Data data=UsageLedgerDatabase.load(activity.getApplicationContext());UsageWindow window=snapshot.longWindow();
            String policy=(snapshot.weekly!=null?"weekly":"monthly")+"|"+LedgerRecord.cleanPlan(snapshot.planType)+"|"+(window==null?0:window.windowSeconds);
            List<FunInsights.Window> completed=FunInsights.completed(data.records,policy,System.currentTimeMillis());
            if(!key.equals(SubscriptionStore.key(activity.getApplicationContext(),AppPreferences.loadSnapshot(activity.getApplicationContext()))))return;
            if(!FunStore.settle(activity.getApplicationContext(),key,completed,(tier,tone,variant)->FunCoach.text(activity.getApplicationContext(),tone,tier,FunInsights.Situation.TIER,variant)))throw new IllegalStateException();
            JSONObject doc=FunStore.load(activity.getApplicationContext(),key);
            activity.runOnUiThread(()->{if(activity.isDestroyed()||activity.isFinishing()||!target.isAttachedToWindow()||!key.equals(SubscriptionStore.key(activity,AppPreferences.loadSnapshot(activity))))return;
                try{home.render(snapshot,window,policy,data.records,completed,doc);}catch(Exception ignored){home.error();}});
        }catch(Exception ignored){activity.runOnUiThread(()->{if(!activity.isDestroyed()&&target.isAttachedToWindow())home.error();});}});
    }
    private void error(){target.removeAllViews();target.addView(LedgerUi.caption(activity,getString(R.string.fun_load_failed),dark));finished.run();}
    private String getString(int id,Object... args){return activity.getString(id,args);}
    private void render(UsageSnapshot snapshot,UsageWindow window,String policy,List<LedgerRecord> records,List<FunInsights.Window> completed,JSONObject doc)throws Exception{
        target.removeAllViews();long now=System.currentTimeMillis();
        BigDecimal current=window==null?null:BigDecimal.valueOf(window.usedPercent);
        LedgerRecord precise=LedgerPeriods.latest(records,policy);
        if(precise!=null&&precise.at==snapshot.fetchedAtMillis&&window!=null&&precise.reset==window.effectiveResetAtMillis(snapshot.fetchedAtMillis))current=new BigDecimal(precise.decimal);
        LedgerPeriods.Span span=window==null?null:LedgerPeriods.span(window.effectiveResetAtMillis(snapshot.fetchedAtMillis),window.windowSeconds,snapshot.fetchedAtMillis);
        boolean fresh=span!=null&&now<span.end&&UsageInsights.freshness(snapshot.fetchedAtMillis,now,RefreshScheduler.effectiveRefreshMinutes(activity))==UsageInsights.Freshness.FRESH;
        LinearLayout value=SubscriptionValueUi.card(activity,dark,snapshot,current,span,fresh,FunInsights.safeCurrent(records,policy,span));
        FunInsights.Rating rating=FunStore.rating(doc,policy,completed,current);
        LinearLayout tier=Ui.card(activity,dark);tier.addView(LedgerUi.heading(activity,getString(R.string.fun_tier),dark));Ui.addSpacer(tier,12);
        LinearLayout row=new LinearLayout(activity);row.setOrientation(stacked()?LinearLayout.VERTICAL:LinearLayout.HORIZONTAL);row.setGravity(Gravity.CENTER_VERTICAL);
        ImageView badge=new ImageView(activity);badge.setImageResource(rating.tier<0?R.drawable.badge_iron:BADGES[rating.tier]);badge.setAlpha(rating.tier<0?.45f:1f);badge.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(Ui.dp(activity,128),Ui.dp(activity,128));if(stacked())bp.gravity=Gravity.CENTER_HORIZONTAL;else bp.setMarginEnd(Ui.dp(activity,16));row.addView(badge,bp);
        LinearLayout copy=new LinearLayout(activity);copy.setOrientation(LinearLayout.VERTICAL);copy.addView(LedgerUi.amount(activity,rating.tier<0?getString(R.string.fun_placing):getString(TIERS[rating.tier]),dark,27));
        copy.addView(LedgerUi.caption(activity,rating.tier<0?getString(R.string.fun_placing):rating.provisional?getString(R.string.fun_provisional):getString(R.string.fun_recorded,rating.count,num(rating.average)),dark));
        if(current!=null)copy.addView(LedgerUi.caption(activity,getString(R.string.fun_challenge,num(current.doubleValue())),dark));
        if(!completed.isEmpty()){FunInsights.Window last=completed.get(completed.size()-1);FunInsights.Style style=FunInsights.style(records,policy,last.span,last.span.end);if(style!=FunInsights.Style.UNKNOWN)copy.addView(LedgerUi.caption(activity,getString(R.string.fun_style_period,getString(styleId(style))),dark));}
        row.addView(copy,new LinearLayout.LayoutParams(stacked()?-1:0,-2,stacked()?0:1));tier.addView(row);
        String tone=FunStore.tone(activity);boolean rapid=false;
        if(precise!=null){LedgerForecast.Result forecast=LedgerForecast.analyze(records,policy,now,RefreshScheduler.effectiveRefreshMinutes(activity));rapid=forecast.spike;}
        FunInsights.Situation situation=FunInsights.situation(rating,current,span==null?0:span.end-now,fresh,rapid);
        if(fresh&&completed.size()>1&&completed.get(completed.size()-1).span.end>=now-86400000L&&rating.tier>FunInsights.rating(completed.subList(0,completed.size()-1),null).tier)situation=FunInsights.Situation.PROMOTED;
        LinearLayout quotation=new LinearLayout(activity);quotation.setOrientation(LinearLayout.VERTICAL);
        quotation.setPadding(Ui.dp(activity,10),Ui.dp(activity,22),Ui.dp(activity,10),Ui.dp(activity,22));
        if(!tone.equals("off")){quotation.addView(LedgerUi.caption(activity,getString(R.string.fun_coach),dark));Ui.addSpacer(quotation,10);
            TextView quote=LedgerUi.heading(activity,"“"+FunCoach.current(activity,tone,rating.tier,situation,CoachMoment.choose(records,policy,span,current,fresh,now),span==null?policy:policy+span.end,now)+"”",dark);
            quote.setTextSize(21);quotation.addView(quote);}
        Ui.addSpacer(tier,10);tier.addView(LedgerUi.action(activity,getString(R.string.fun_why),false,dark,()->new AlertDialog.Builder(activity).setTitle(R.string.fun_why).setMessage(getString(R.string.fun_tier_help)+"\n\n"+getString(R.string.fun_coach_help)).setPositiveButton(R.string.ui_done_e9b450,null).show()));target.addView(tier);
        if(!tone.equals("off"))target.addView(quotation);target.addView(value);Ui.addSpacer(target,16);
        finished.run();
    }
    private boolean stacked(){return LedgerUi.stacked(activity)||activity.getResources().getConfiguration().screenWidthDp<420;}
    private static String num(double value){return LedgerUi.number(value);}
    private static int styleId(FunInsights.Style style){switch(style){case FOCUS:return R.string.fun_style_focus;case SPRINT:return R.string.fun_style_sprint;case RESERVE:return R.string.fun_style_reserve;default:return R.string.fun_style_steady;}}
}
