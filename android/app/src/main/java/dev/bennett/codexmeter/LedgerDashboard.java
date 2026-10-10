package dev.bennett.codexmeter;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.util.concurrent.ExecutorService;

/** Prominent read-only summary. Existing quota cards and dashboard preferences stay intact. */
final class LedgerDashboard {
    private LedgerDashboard() {}

    static State addOverview(AppCompatActivity activity, LinearLayout parent, ExecutorService worker,
            UsageSnapshot snapshot, boolean dark) {
        if (snapshot == null || !AppPreferences.showUsageOverview(activity)) return null;
        UsageWindow window;
        String meter;
        int label;
        if (snapshot.weekly != null && AppPreferences.showDashboardWeekly(activity)) {
            window = snapshot.weekly; meter = "weekly"; label = R.string.ui_weekly_158f3d;
        } else if (snapshot.monthly != null && AppPreferences.showDashboardMonthly(activity)) {
            window = snapshot.monthly; meter = "monthly"; label = R.string.ui_monthly_d31edb;
        } else if (snapshot.fiveHour != null && AppPreferences.showDashboardFiveHour(activity)) {
            window = snapshot.fiveHour; meter = "five_hour"; label = R.string.ui_5_hour_bc4288;
        } else {
            window=null;meter="additional";label=R.string.ui_additional_limit_e91f10;
            if(AppPreferences.showDashboardAdditionalLimits(activity))for(UsageLimit limit:snapshot.additionalLimits){
                window=limit.primary!=null?limit.primary:limit.secondary;if(window!=null)break;
            }
            if(window==null)return null;
        }
        State state = new State(activity, worker, snapshot, window, meter, label, dark);
        parent.addView(state.card); Ui.addSpacer(parent, 16);
        state.loadToday(); return state;
    }

    static final class State {
        final LinearLayout card;
        private final AppCompatActivity activity;
        private final ExecutorService worker;
        private final UsageSnapshot snapshot;
        private final UsageWindow window;
        private final boolean dark;
        private final String policy;
        private final TextView reset, fiveHourReset, quality, todayAmount, todayNote;
        private String day;

        State(AppCompatActivity activity, ExecutorService worker, UsageSnapshot snapshot,
                UsageWindow window, String meter, int label, boolean dark) {
            this.activity=activity; this.worker=worker; this.snapshot=snapshot; this.window=window; this.dark=dark;
            policy=meter+"|"+LedgerRecord.cleanPlan(snapshot.planType)+"|"+window.windowSeconds;
            card=Ui.card(activity,dark);
            card.setPadding(Ui.dp(activity,16),Ui.dp(activity,14),Ui.dp(activity,16),Ui.dp(activity,14));
            card.setBackground(LedgerUi.shape(activity,LedgerUi.tint(dark),24));
            TierTheme.frame(card,TierTheme.tier(activity),dark);
            card.addView(LedgerUi.heading(activity,activity.getString(meter.equals("weekly")?R.string.ui_ledger_home:label),dark));
            Ui.addSpacer(card,6);
            quality=LedgerUi.badge(activity,activity.getString(R.string.ui_ledger_last_value),dark,false);
            quality.setPadding(0,0,0,0);quality.setBackground(null);card.addView(quality); Ui.addSpacer(card,8);
            LedgerPeriods.Span span=LedgerPeriods.span(window.effectiveResetAtMillis(snapshot.fetchedAtMillis),window.windowSeconds,snapshot.fetchedAtMillis);
            card.addView(LedgerUi.caption(activity,span==null?activity.getString(R.string.next_missing_timeline):span.end<=System.currentTimeMillis()?activity.getString(R.string.ui_ledger_last_value)+" · "+V3Display.range(span):activity.getString(R.string.v3_window_range,activity.getString(label),V3Display.range(span)),dark));
            Ui.addSpacer(card,8);
            card.addView(LedgerUi.caption(activity,activity.getString(R.string.ui_ledger_remaining,
                    activity.getString(label)),dark));
            card.addView(LedgerUi.amount(activity,UsagePrecision.remaining(window),dark,36));
            card.addView(LedgerUi.caption(activity,activity.getString(R.string.hud_used,UsagePrecision.used(window)),dark));
            ProgressBar progress=new ProgressBar(activity,null,android.R.attr.progressBarStyleHorizontal);
            progress.setMax(100000);progress.setProgress((int)Math.round((100-window.preciseUsedPercent)*1000));
            progress.setProgressTintList(ColorStateList.valueOf(Ui.accent(activity,dark)));
            progress.setProgressBackgroundTintList(ColorStateList.valueOf(Ui.divider(dark)));
            card.addView(progress,new LinearLayout.LayoutParams(-1,Ui.dp(activity,8)));
            Ui.addSpacer(card,6);reset=LedgerUi.caption(activity,"",dark);card.addView(reset);
            long absolute=window.effectiveResetAtMillis(snapshot.fetchedAtMillis);
            card.addView(LedgerUi.caption(activity,absolute>0?V3Display.time(absolute):activity.getString(R.string.next_missing_timeline),dark));
            boolean separateFiveHour=window!=snapshot.fiveHour
                    &&snapshot.fiveHour!=null
                    &&AppPreferences.showDashboardFiveHour(activity);
            fiveHourReset=separateFiveHour?LedgerUi.caption(activity,"",dark):null;
            if(fiveHourReset!=null){Ui.addSpacer(card,8);card.addView(fiveHourReset);}
            Ui.addSpacer(card,10);
            card.addView(LedgerUi.caption(activity,activity.getString(R.string.ui_ledger_today_usage),dark));
            todayAmount=LedgerUi.amount(activity,"—",dark,26);card.addView(todayAmount);
            todayNote=LedgerUi.caption(activity,activity.getString(R.string.next_loading),dark);card.addView(todayNote);
            Ui.addSpacer(card,6);card.addView(LedgerUi.caption(activity,activity.getString(R.string.next_observed_at,V3Display.time(snapshot.fetchedAtMillis)),dark));
            Ui.addSpacer(card,10);
            Button details=LedgerUi.action(activity,activity.getString(R.string.v3_limit_details),false,dark,()->MatteNav.homeAction(activity,"limits"));
            Button save=LedgerUi.action(activity,activity.getString(R.string.ui_refresh_56e3ba),false,dark,()->{});
            save.setContentDescription(activity.getString(R.string.ui_refresh_56e3ba));
            save.setOnClickListener(v->((MainActivity)activity).refreshNow(save));save.setEnabled(!MainActivity.refreshInProgress());
            LinearLayout actions=Ui.horizontal(activity,android.view.Gravity.CENTER_VERTICAL);
            if(LedgerUi.stacked(activity)){
                actions.setOrientation(LinearLayout.VERTICAL);actions.addView(details,new LinearLayout.LayoutParams(-1,-2));
                Ui.addSpacer(actions,6);actions.addView(save,new LinearLayout.LayoutParams(-1,-2));
            }else{
                actions.addView(details,new LinearLayout.LayoutParams(0,-2,1));
                LinearLayout.LayoutParams refreshParams=new LinearLayout.LayoutParams(Ui.dp(activity,136),Ui.dp(activity,52));
                refreshParams.setMarginStart(Ui.dp(activity,8));actions.addView(save,refreshParams);
            }
            card.addView(actions);
            updateClock();
        }

        void updateClock() {
            long now=System.currentTimeMillis();
            boolean fresh=UsageInsights.freshness(snapshot.fetchedAtMillis,now,
                    RefreshScheduler.effectiveRefreshMinutes(activity))==UsageInsights.Freshness.FRESH;
            quality.setText(activity.getString(fresh?R.string.ui_ledger_last_value:R.string.ui_ledger_refresh));
            quality.setTextColor(fresh?LedgerUi.muted(dark):Ui.mainText(dark));
            long at=window.effectiveResetAtMillis(snapshot.fetchedAtMillis);
            reset.setText(window==snapshot.fiveHour
                    ? FiveHourResetDisplay.text(activity,window,snapshot.fetchedAtMillis,now,WidgetOptions.RESET_RELATIVE,false)
                    : at>now?activity.getString(R.string.next_time_to_reset,
                    UsageInsightDisplay.duration(activity,at-now)):activity.getString(R.string.next_missing_timeline));
            if(fiveHourReset!=null) fiveHourReset.setText(FiveHourResetDisplay.text(activity,
                    snapshot.fiveHour,snapshot.fetchedAtMillis,now,WidgetOptions.RESET_RELATIVE,false));
            if(day!=null&&!day.equals(LedgerAggregation.day(now).toString()))loadToday();
        }

        private int labelForPolicy(){return policy.startsWith("weekly|")?R.string.ui_weekly_158f3d:policy.startsWith("monthly|")?R.string.ui_monthly_d31edb:policy.startsWith("five_hour|")?R.string.ui_5_hour_bc4288:R.string.ui_additional_limit_e91f10;}

        private void loadToday() {
            day=LedgerAggregation.day(System.currentTimeMillis()).toString();
            final String requestedDay=day;
            worker.execute(()->{
                LedgerAggregation.Day today=null; boolean failed=false;
                try { today=LedgerPresentation.find(UsageLedgerDatabase.load(activity.getApplicationContext()).days,
                        policy,java.time.LocalDate.parse(requestedDay)); }
                catch(Exception ignored){failed=true;}
                final LedgerAggregation.Day result=today; final boolean error=failed;
                activity.runOnUiThread(()->{
                    if(activity.isDestroyed()||activity.isFinishing()||!requestedDay.equals(day))return;
                    todayAmount.setText(LedgerPresentation.measured(result)
                            ?activity.getString(R.string.ui_ledger_points,LedgerUi.number(result.points)):"—");
                    todayNote.setText(error?activity.getString(R.string.next_failed):result==null
                            ?activity.getString(R.string.ui_ledger_none):!LedgerPresentation.measured(result)
                            ?activity.getString(R.string.ui_ledger_comparison):activity.getString(R.string.ui_ledger_measured)
                            +" · "+activity.getString(result.uncertain>0?R.string.next_gap:R.string.next_partial)
                            +(result.legacy?" · "+activity.getString(R.string.next_legacy_tag):""));
                    todayNote.append(" · "+activity.getString(R.string.hud_limit_basis,activity.getString(labelForPolicy())));
                });
            });
        }
    }
}
