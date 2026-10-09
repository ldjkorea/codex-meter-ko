package dev.bennett.codexmeter;

import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

/** Immediate utilization presentation; historical settlements stay in their existing journal. */
final class LiveCards {
    private LiveCards() {}
    static LinearLayout crest(AppCompatActivity a,boolean dark,LiveUtilization.Result live,boolean fresh){
        int rank=live.valid&&fresh?live.tier:-1;
        LinearLayout card=Ui.card(a,dark);TierTheme.frame(card,rank,dark);
        boolean stacked=LedgerUi.stacked(a)||a.getResources().getConfiguration().screenWidthDp<420;
        LinearLayout row=new LinearLayout(a);row.setOrientation(stacked?LinearLayout.VERTICAL:LinearLayout.HORIZONTAL);row.setGravity(Gravity.CENTER_VERTICAL);
        TierCompanionView badge=new TierCompanionView(a,rank);badge.setAlpha(rank<0?.45f:1f);badge.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(Ui.dp(a,160),Ui.dp(a,160));
        if(stacked)bp.gravity=Gravity.CENTER_HORIZONTAL;else bp.setMarginEnd(Ui.dp(a,16));row.addView(TierTheme.companion(badge,rank),bp);
        LinearLayout copy=new LinearLayout(a);copy.setOrientation(LinearLayout.VERTICAL);
        copy.addView(LedgerUi.amount(a,a.getString(rank<0?R.string.fun_placing:TierPresentation.NAMES[rank]),dark,27));
        if(rank>=0){copy.addView(LedgerUi.caption(a,a.getString(R.string.live_utilization,LedgerUi.number(live.percent)),dark));
            copy.addView(LedgerUi.caption(a,a.getString(live.bonus?R.string.live_bonus:live.partial?R.string.live_partial:R.string.live_current),dark));}
        else copy.addView(LedgerUi.caption(a,a.getString(fresh?R.string.live_need_coverage:R.string.ui_ledger_refresh),dark));
        row.addView(copy,new LinearLayout.LayoutParams(stacked?-1:0,-2,stacked?0:1));card.addView(row);return card;
    }
    static LinearLayout ai(AppCompatActivity a,boolean dark,LiveUtilization.Result live,UsageSnapshot snapshot,boolean fresh,double daily,long now){
        UsageWindow window=snapshot.longWindow();
        LinearLayout card=new LinearLayout(a);card.setOrientation(LinearLayout.VERTICAL);card.setPadding(Ui.dp(a,10),Ui.dp(a,22),Ui.dp(a,10),Ui.dp(a,22));
        card.addView(LedgerUi.caption(a,a.getString(R.string.live_ai_title),dark));Ui.addSpacer(card,10);
        String key=SpicyAi.key(daily,window==null?Double.NaN:window.usedPercent,fresh,live.bonus,window==null?0:window.effectiveResetAtMillis(snapshot.fetchedAtMillis)-now,now);
        int id=resource(key);
        TextView quote=LedgerUi.heading(a,"“"+a.getString(id==0?R.string.live_ai_missing_0:id)+"”",dark);quote.setTextSize(21);card.addView(quote);return card;
    }
    private static int resource(String key){
        switch(key){
            case "live_ai_missing_0":return R.string.live_ai_missing_0;
            case "live_ai_missing_1":return R.string.live_ai_missing_1;
            case "live_ai_missing_2":return R.string.live_ai_missing_2;
            case "live_ai_bonus_0":return R.string.live_ai_bonus_0;
            case "live_ai_bonus_1":return R.string.live_ai_bonus_1;
            case "live_ai_bonus_2":return R.string.live_ai_bonus_2;
            case "live_ai_empty_0":return R.string.live_ai_empty_0;
            case "live_ai_empty_1":return R.string.live_ai_empty_1;
            case "live_ai_empty_2":return R.string.live_ai_empty_2;
            case "live_ai_sprint_0":return R.string.live_ai_sprint_0;
            case "live_ai_sprint_1":return R.string.live_ai_sprint_1;
            case "live_ai_sprint_2":return R.string.live_ai_sprint_2;
            case "live_ai_active_0":return R.string.live_ai_active_0;
            case "live_ai_active_1":return R.string.live_ai_active_1;
            case "live_ai_active_2":return R.string.live_ai_active_2;
            case "live_ai_idle_0":return R.string.live_ai_idle_0;
            case "live_ai_idle_1":return R.string.live_ai_idle_1;
            case "live_ai_idle_2":return R.string.live_ai_idle_2;
            case "live_ai_zero_0":return R.string.live_ai_zero_0;
            case "live_ai_zero_1":return R.string.live_ai_zero_1;
            case "live_ai_zero_2":return R.string.live_ai_zero_2;
            case "live_ai_partial_0":return R.string.live_ai_partial_0;
            case "live_ai_partial_1":return R.string.live_ai_partial_1;
            case "live_ai_partial_2":return R.string.live_ai_partial_2;
            case "live_ai_steady_0":return R.string.live_ai_steady_0;
            case "live_ai_steady_1":return R.string.live_ai_steady_1;
            case "live_ai_steady_2":return R.string.live_ai_steady_2;
            default:return R.string.live_ai_missing_0;
        }
    }
    static double daily(UsageLedgerDatabase.Data data,String policy,long now){
        LedgerAggregation.Day day=LedgerPresentation.find(data.days,policy,LedgerAggregation.day(now));
        return day!=null&&(day.coveredMillis>0||day.points>0)?day.points:Double.NaN;
    }
}
