package dev.bennett.codexmeter;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.json.JSONObject;

/** Separate details screen. Queries and exports never run on the UI thread. */
public class LedgerAnalyticsActivity extends AppCompatActivity {
    // Survive rotation/finish: accepted manual saves and SAF exports finish in order.
    private static final ExecutorService worker=Executors.newSingleThreadExecutor();
    private LinearLayout content,body;
    private boolean dark,busy;
    private UsageLedgerDatabase.Data data;
    private String policy="";
    private int period=-1,screen=0;
    private int savedScroll;private boolean restoreScroll;
    private final java.util.Set<String> expandedGroups=new java.util.HashSet<>();
    private String loadedKey="";private long loadGeneration;
    private LocalDate selectedDate=LedgerAggregation.day(System.currentTimeMillis());
    private YearMonth month=YearMonth.from(LedgerAggregation.day(System.currentTimeMillis()));
    private static final int EXPORT_JSON=8310,EXPORT_CSV=8311;
    @Override protected void onCreate(Bundle state){
        Ui.applySelectedTheme(this);super.onCreate(state);dark=Ui.isDark(this);
        content=Ui.installPage(this,getString(settingsRoot()?R.string.matte_settings:recordsRoot()?R.string.matte_records:R.string.next_title),settingsRoot()).content;
        if(!settingsRoot())MatteNav.install(this,recordsRoot()?2:0);
        ((androidx.swiperefreshlayout.widget.SwipeRefreshLayout)findViewById(R.id.dashboard_refresh)).setEnabled(false);
        if(!recordsRoot()&&!settingsRoot()){startActivity(new Intent(this,RecordsActivity.class).putExtra("screen",getIntent().getIntExtra("screen",0)).putExtra("manual_record",getIntent().getBooleanExtra("manual_record",false)));finish();return;}
        screen=getIntent().getIntExtra("screen",0);
        if(state==null&&getIntent().getStringExtra("selected_date")!=null)try{selectedDate=LocalDate.parse(getIntent().getStringExtra("selected_date"));}catch(java.time.format.DateTimeParseException ignored){}
        if(state!=null){selectedDate=LocalDate.parse(state.getString("selected_date",selectedDate.toString()));policy=state.getString("policy","");period=state.getInt("period",7);screen=state.getInt("screen",0);month=YearMonth.parse(state.getString("month",month.toString()));}
        if(screen==3){startActivity(new Intent(this,FunActivity.class));finish();return;}
        if(!recordsRoot()&&!settingsRoot()&&screen==2){MatteNav.open(this,2);finish();return;}
        screen=Math.max(0,Math.min(1,screen));
        if(state!=null){savedScroll=state.getInt("scroll");restoreScroll=true;java.util.ArrayList<String> groups=state.getStringArrayList("groups");if(groups!=null)expandedGroups.addAll(groups);}
        if(state==null&&getIntent().getBooleanExtra("manual_record",false))manual();else reload();
    }
    @Override protected void onSaveInstanceState(Bundle state){super.onSaveInstanceState(state);state.putString("selected_date",selectedDate.toString());state.putString("policy",policy);state.putInt("period",period);state.putInt("screen",screen);state.putStringArrayList("groups",new java.util.ArrayList<>(expandedGroups));state.putString("month",month.toString());View scroll=findViewById(R.id.dashboard_scroll);if(scroll!=null)state.putInt("scroll",scroll.getScrollY());}
    @Override protected void onResume(){super.onResume();if(Ui.isDark(this)!=dark){recreate();return;}if(data!=null&&!busy){String key=pageKey();if(!key.equals(loadedKey))reload();else if(recordsRoot()||settingsRoot())controls();}}
    @Override protected void onPause(){View scroll=findViewById(R.id.dashboard_scroll);if(scroll!=null){savedScroll=scroll.getScrollY();restoreScroll=true;}NotesUi.flush(this);super.onPause();}
    @Override protected void onDestroy(){NotesUi.close(this);super.onDestroy();}
    @Override public boolean onSupportNavigateUp(){finish();return true;}
    private String pageKey(){UsageSnapshot s=AppPreferences.loadSnapshot(this);return SubscriptionStore.key(this,s)+":"+(s==null?0:s.fetchedAtMillis)+":"+getSharedPreferences("codex_meter_settings_v1",0).getAll().hashCode();}
    protected boolean recordsRoot(){return false;}
    protected boolean settingsRoot(){return false;}
    private void reload(){
        final long request=++loadGeneration;final String requestedKey=pageKey();
        if(data!=null&&!restoreScroll){savedScroll=((androidx.core.widget.NestedScrollView)findViewById(R.id.dashboard_scroll)).getScrollY();restoreScroll=true;}
        content.removeAllViews();if(settingsRoot())RecordSettings.hub(this,content,dark,this::picker,this::confirmClear,()->Toast.makeText(this,R.string.next_loading,Toast.LENGTH_SHORT).show());else content.addView(Ui.text(this,getString(R.string.next_loading),14,Ui.secondaryText(dark)));
        worker.execute(()->{
            try{UsageLedgerDatabase.Data loaded=UsageLedgerDatabase.load(getApplicationContext());
                UsageLedgerDatabase.scheduleMaintenance(getApplicationContext());
                runOnUiThread(()->{if(isDestroyed()||isFinishing()||request!=loadGeneration)return;if(!requestedKey.equals(pageKey())){reload();return;}data=loaded;loadedKey=requestedKey;controls();});
            }catch(Exception ignored){runOnUiThread(()->{if(isDestroyed()||isFinishing()||request!=loadGeneration)return;if(!requestedKey.equals(pageKey())){reload();return;}content.removeAllViews();if(settingsRoot())RecordSettings.hub(this,content,dark,this::picker,this::confirmClear,()->Toast.makeText(this,R.string.next_failed,Toast.LENGTH_LONG).show());content.addView(Ui.text(this,getString(R.string.next_failed),14,Ui.mainText(dark)));content.addView(LedgerUi.action(this,getString(R.string.ui_ledger_recovery),true,dark,this::reload));});}
        });
    }
    @Override public boolean onCreateOptionsMenu(Menu menu){
        menu.add(0,8320,0,R.string.ui_ledger_save);
        menu.add(0,EXPORT_CSV,1,R.string.next_export_csv);
        menu.add(0,EXPORT_JSON,2,R.string.next_export_json);
        if(!settingsRoot())menu.add(0,8321,3,R.string.ui_ledger_help);
        menu.add(0,8323,4,R.string.next_spike_option).setCheckable(true);
        menu.add(0,8322,5,R.string.ui_clear_local_history_ab1b10);
        NotesUi.menu(this,menu,"analysis/"+screen);
        return true;
    }
    @Override public boolean onPrepareOptionsMenu(Menu menu){
        NotesUi.prepare(this,menu);
        for(int id:new int[]{8320,EXPORT_CSV,EXPORT_JSON,8322})menu.findItem(id).setVisible(false);
        menu.findItem(8323).setVisible(!recordsRoot()&&!settingsRoot());
        boolean records=data!=null&&(!data.records.isEmpty()||!data.days.isEmpty()||!data.events.isEmpty());
        menu.findItem(8320).setEnabled(!busy);
        menu.findItem(EXPORT_CSV).setEnabled(records);menu.findItem(EXPORT_JSON).setEnabled(records);
        menu.findItem(8322).setEnabled(records);menu.findItem(8323).setChecked(spikeEnabled());
        return super.onPrepareOptionsMenu(menu);
    }
    @Override public boolean onOptionsItemSelected(MenuItem item){
        if(NotesUi.select(this,item,"analysis/"+screen))return true;
        switch(item.getItemId()){
            case 8320:manual();return true;
            case EXPORT_CSV:picker(false);return true;
            case EXPORT_JSON:picker(true);return true;
            case 8321:help();return true;
            case 8323:getSharedPreferences("codex_next_ui",MODE_PRIVATE).edit().putBoolean("spike",!spikeEnabled()).apply();invalidateOptionsMenu();render();return true;
            case 8322:confirmClear();return true;
            default:return super.onOptionsItemSelected(item);
        }
    }
    private void help(){
        new AlertDialog.Builder(this).setTitle(R.string.ui_ledger_help)
                .setMessage(getString(R.string.next_note)+"\n\n"+getString(R.string.next_gap_note)
                        +"\n\n"+getString(R.string.ledger_warmup)+"\n\n"+getString(R.string.next_export_note))
                .setPositiveButton(R.string.ui_done_e9b450,null).show();
    }
    private void confirmClear(){
        new AlertDialog.Builder(this).setTitle(R.string.ui_clear_usage_history_03c461)
                .setMessage(R.string.ui_this_removes_every_locally_stored_usage_sample_your_lat_e3f515)
                .setNegativeButton(R.string.ui_cancel_77dfd2,null).setPositiveButton(R.string.ui_clear_719ea3,(dialog,which)->{
                    worker.execute(()->{AccountSession.clearHistory(getApplicationContext());LiveUsageStore.invalidate();WidgetRenderer.updateAll(getApplicationContext());runOnUiThread(()->{if(!isDestroyed())reload();});});
                }).show();
    }
    private void controls(){
        invalidateOptionsMenu();content.removeAllViews();
        if(settingsRoot()) {
            RecordSettings.hub(this,content,dark,this::picker,this::confirmClear,this::advancedData);
            restorePosition();return;
        }
        Map<String,String> options=new LinkedHashMap<>();
        for(LedgerAggregation.Day day:data.days)if(visiblePolicy(day.policy))options.put(day.policy,label(day.policy));
        for(LedgerRecord record:data.records)if(visiblePolicy(record.policy()))options.put(record.policy(),label(record.policy()));
        if(options.isEmpty()&&recordsRoot()){
            policy="";body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);content.addView(body);body.addView(LedgerUi.caption(this,getString(R.string.next_empty),dark));addRecords(LedgerAggregation.day(System.currentTimeMillis()));RecordSettings.add(this,body,dark);restorePosition();return;
        }
        if(options.isEmpty()){
            navigation();LinearLayout empty=Ui.card(this,dark);empty.addView(LedgerUi.heading(this,getString(R.string.ui_ledger_collecting),dark));
            Ui.addSpacer(empty,12);empty.addView(LedgerUi.caption(this,getString(R.string.next_empty),dark));Ui.addSpacer(empty,20);
            Button save=LedgerUi.action(this,getString(R.string.ui_ledger_save),true,dark,this::manual);save.setEnabled(!busy);empty.addView(save);content.addView(empty);return;
        }
        List<String> keys=new ArrayList<>(options.keySet());
        if(!keys.contains(policy)){
            List<LedgerRecord> visible=new ArrayList<>();for(LedgerRecord row:data.records)if(visiblePolicy(row.policy()))visible.add(row);LedgerRecord best=LedgerPresentation.latest(visible);policy=best==null?keys.get(keys.size()-1):best.policy();
        }
        content.addView(LedgerUi.caption(this,getString(R.string.ui_ledger_choose),dark));Ui.addSpacer(content,6);
        Spinner meters=Ui.spinner(this,options.values().toArray(new String[0]),dark);meters.setSelection(keys.indexOf(policy));content.addView(meters);
        Ui.addSpacer(content,14);
        navigation();
        body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);content.addView(body);
        meters.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
            public void onItemSelected(AdapterView<?> parent,View view,int position,long id){if(!policy.equals(keys.get(position))){savedScroll=0;restoreScroll=true;}policy=keys.get(position);render();}
            public void onNothingSelected(AdapterView<?> parent){}
        });render();
    }
    private boolean visiblePolicy(String value){
        if(value.startsWith("five_hour|"))return AppPreferences.showDashboardFiveHour(this);
        if(value.startsWith("weekly|"))return AppPreferences.showDashboardWeekly(this);
        if(value.startsWith("monthly|"))return AppPreferences.showDashboardMonthly(this);
        return AppPreferences.showDashboardAdditionalLimits(this);
    }
    private void navigation(){if(!settingsRoot()){content.addView(LedgerUi.tabs(this,new String[]{getString(R.string.ui_ledger_overview),getString(R.string.ui_ledger_trends)},screen,dark,index->{savedScroll=0;restoreScroll=true;screen=index;controls();}));Ui.addSpacer(content,18);}}

    private boolean spikeEnabled(){return getSharedPreferences("codex_next_ui",MODE_PRIVATE).getBoolean("spike",false);}
    private void render(){
        if(body==null||data==null)return;body.removeAllViews();
        LocalDate today=LedgerAggregation.day(System.currentTimeMillis());
        getIntent().putExtra("selected_date",selectedDate.toString());
        if(screen==0)addOverview(today);else addTrends(today);
        addRecords(today);RecordSettings.add(this,body,dark);
        restorePosition();
    }
    private void restorePosition(){
        View scroll=findViewById(R.id.dashboard_scroll);if(scroll!=null){if(!restoreScroll)savedScroll=scroll.getScrollY();scroll.post(()->{if(!isDestroyed()){scroll.scrollTo(0,savedScroll);restoreScroll=false;}});}
    }
    private void addOverview(LocalDate today){
        LedgerForecast.Result forecast=LedgerForecast.analyze(data.records,policy,System.currentTimeMillis(),RefreshScheduler.effectiveRefreshMinutes(this));
        LinearLayout hero=Ui.card(this,dark);hero.setBackground(LedgerUi.shape(this,LedgerUi.tint(dark),28));
        hero.addView(LedgerUi.badge(this,getString(R.string.ui_ledger_last_value),dark,false));Ui.addSpacer(hero,12);
        hero.addView(LedgerUi.caption(this,getString(R.string.ui_ledger_remaining,label(policy)),dark));
        hero.addView(LedgerUi.amount(this,forecast.latest==null?"—":number(forecast.latest.remaining())+"%",dark,42));
        if(forecast.latest!=null){
            hero.addView(LedgerUi.caption(this,getString(R.string.next_observed_at,time(forecast.latest.at)),dark));
            if(forecast.latest.reset>System.currentTimeMillis())hero.addView(LedgerUi.caption(this,getString(R.string.next_time_to_reset,
                    UsageInsightDisplay.duration(this,forecast.latest.reset-System.currentTimeMillis())),dark));
        }
        Ui.addSpacer(hero,16);Button save=LedgerUi.action(this,getString(R.string.ui_ledger_save),true,dark,this::manual);save.setEnabled(!busy);hero.addView(save);
        body.addView(hero);Ui.addSpacer(body,12);
        body.addView(LedgerUi.pair(this,dayTile(today,R.string.ui_ledger_today_usage),dayTile(today.minusDays(1),R.string.next_yesterday)));
        Ui.addSpacer(body,8);TextView help=LedgerUi.caption(this,getString(R.string.ui_ledger_measured)+" · "+getString(R.string.ui_ledger_help),dark);
        help.setPadding(0,Ui.dp(this,12),0,Ui.dp(this,12));help.setOnClickListener(view->help());body.addView(help);
        addForecast(forecast);
    }
    private LinearLayout dayTile(LocalDate date,int title){
        LedgerAggregation.Day day=find(date);
        String note=day==null?getString(R.string.ui_ledger_none):!LedgerPresentation.measured(day)?getString(R.string.ui_ledger_comparison)
                :getString(day.uncertain>0?R.string.next_gap:day.points==0?R.string.v3_zero:R.string.next_partial)+" · "+getString(R.string.ui_ledger_count,day.count);
        if(day!=null&&day.legacy)note+=" · "+getString(R.string.next_legacy_tag);
        LinearLayout tile=LedgerUi.tile(this,getString(title),dayAmount(day),note,dark);tile.setOnClickListener(view->detail(date));return tile;
    }
    private String dayAmount(LedgerAggregation.Day day){return LedgerPresentation.measured(day)?getString(R.string.ui_ledger_points,number(day.points)):"—";}
    private void periodTabs(){
        int[] lengths={-1,-2,7,30};int selected=period==-1?0:period==-2?1:period==7?2:3;
        String[] labels={getString(R.string.v3_this_window),getString(R.string.v3_previous_window),getString(R.string.next_weekly),getString(R.string.next_monthly)};
        if(getResources().getConfiguration().screenWidthDp<420){
            body.addView(LedgerUi.tabs(this,new String[]{labels[0],labels[1]},selected<2?selected:-1,dark,index->{period=lengths[index];render();}));Ui.addSpacer(body,6);
            body.addView(LedgerUi.tabs(this,new String[]{labels[2],labels[3]},selected>=2?selected-2:-1,dark,index->{period=lengths[index+2];render();}));
        }else body.addView(LedgerUi.tabs(this,labels,selected,dark,index->{period=lengths[index];render();}));
        Ui.addSpacer(body,12);
    }
    private void addTrends(LocalDate today){
        periodTabs();
        if(period<0){addServerWindow();return;}
        LedgerPresentation.Period total=LedgerPresentation.period(data.days,policy,today,period);
        LinearLayout trendCard=Ui.card(this,dark);
        trendCard.addView(LedgerUi.caption(this,getString(R.string.ui_ledger_total),dark));
        trendCard.addView(LedgerUi.amount(this,total.measuredDays==0?"—":getString(R.string.ui_ledger_points,number(total.points)),dark,32));
        trendCard.addView(LedgerUi.caption(this,getString(R.string.ui_ledger_coverage,total.recordedDays,period)+" · "+getString(R.string.next_partial),dark));Ui.addSpacer(trendCard,12);
        List<LedgerAggregation.Day> chart=new ArrayList<>();for(int i=period-1;i>=0;i--)chart.add(find(today.minusDays(i)));
        LedgerTrendView trend=new LedgerTrendView(this,dark);trend.setData(chart,today.minusDays(period-1));TextView selection=LedgerUi.caption(this,dayText(today,today.toString()),dark);
        trend.setOnDateSelectedListener(date->{selection.setText(dayText(date,date.toString()));selection.setContentDescription(selection.getText());selection.announceForAccessibility(selection.getText());});
        int chartHeight=Math.round(190+Math.max(0,getResources().getConfiguration().fontScale-1)*40);
        if(period==30){
            android.widget.HorizontalScrollView scroller=new android.widget.HorizontalScrollView(this);
            scroller.setHorizontalScrollBarEnabled(true);scroller.addView(trend,new android.widget.FrameLayout.LayoutParams(Ui.dp(this,1200),Ui.dp(this,chartHeight)));
            trendCard.addView(scroller,new LinearLayout.LayoutParams(-1,Ui.dp(this,chartHeight)));scroller.post(()->scroller.fullScroll(View.FOCUS_RIGHT));
        }else trendCard.addView(trend,new LinearLayout.LayoutParams(-1,Ui.dp(this,chartHeight)));
        Ui.addSpacer(trendCard,8);
        trendCard.addView(selection);Ui.addSpacer(trendCard,8);
        trendCard.addView(LedgerUi.caption(this,getString(R.string.ui_ledger_chart_note),dark));Ui.addSpacer(trendCard,12);
        trendCard.addView(LedgerUi.action(this,getString(R.string.ui_ledger_date_list),false,dark,()->{MatteNav.open(this,2);}));body.addView(trendCard);
        advancedButton();
    }
    private void advancedButton(){body.addView(LedgerUi.action(this,getString(R.string.v3_advanced_charts),false,dark,()->startActivity(new Intent(this,UsageHistoryActivity.class).putExtra("advanced",true))));}
    private void addServerWindow(){
        LedgerRecord latest=LedgerPeriods.latest(data.records,policy);
        LedgerPeriods.Span current=latest==null?null:LedgerPeriods.span(latest.reset,latest.seconds,latest.at);
        LedgerPeriods.Span chosen=period==-1?current:LedgerPeriods.previous(data.records,policy,current);
        long now=System.currentTimeMillis();LedgerPeriods.Measurement result=LedgerPeriods.measure(data.records,policy,chosen,now);
        LinearLayout card=Ui.card(this,dark);card.addView(LedgerUi.heading(this,getString(period==-1?R.string.v3_this_window:R.string.v3_previous_window),dark));
        Ui.addSpacer(card,8);card.addView(LedgerUi.caption(this,chosen==null?getString(R.string.v3_window_missing):V3Display.range(chosen),dark));Ui.addSpacer(card,16);
        LedgerRecord observed=null;
        if(chosen!=null)for(LedgerRecord row:data.records)if(row.policy().equals(policy)&&row.at>=chosen.start&&row.at<chosen.end
                &&UsageWindow.sameResetWindow(row.reset,row.seconds,chosen.end,row.seconds)&&(observed==null||row.at>observed.at))observed=row;
        LiveUtilization.Result cumulative=null;
        if(observed!=null){List<Long> credits=new ArrayList<>();for(UsageLedgerDatabase.Event event:data.events)
            if(event.type.equals("credit_used")&&event.origin.equals("confirmed"))credits.add(event.at);
            cumulative=LiveUtilization.calculate(data.records,credits,policy,chosen,chosen.start,chosen.end,observed.at);}
        card.addView(LedgerUi.amount(this,cumulative==null||!cumulative.valid?"—":number(cumulative.windowPoints)+"%",dark,32));
        card.addView(LedgerUi.caption(this,getString(R.string.live_window_total),dark));
        if(observed!=null)card.addView(LedgerUi.caption(this,getString(R.string.next_observed_at,time(observed.at)),dark));
        if(result.covered>0)card.addView(LedgerUi.caption(this,getString(R.string.live_window_delta,number(result.points)),dark));
        card.addView(LedgerUi.caption(this,getString(R.string.v3_window_measured,result.observations,UsageInsightDisplay.duration(this,result.covered)),dark));
        LedgerPeriods.Measurement[] comparison=LedgerPeriods.matched(data.records,policy,current,now);
        Ui.addSpacer(card,16);card.addView(LedgerUi.caption(this,comparison[0].comparable&&comparison[1].comparable
                ?getString(R.string.v3_matched,number(comparison[0].points),number(comparison[1].points)):getString(R.string.v3_compare_missing),dark));
        if(current!=null&&current.end<=now)card.addView(LedgerUi.badge(this,getString(R.string.ui_ledger_refresh),dark,true));
        body.addView(card);Ui.addSpacer(body,16);advancedButton();
    }
    private void addRecords(LocalDate today){
        addCalendar(today);
        LinearLayout selected=Ui.card(this,dark);selected.addView(LedgerUi.heading(this,selectedDate.toString(),dark));Ui.addSpacer(selected,10);
        selected.addView(LedgerUi.amount(this,dayAmount(find(selectedDate)),dark,28));selected.addView(LedgerUi.caption(this,dayText(selectedDate,""),dark));Ui.addSpacer(selected,12);
        body.addView(selected);Ui.addSpacer(body,16);
    }
    private void expandable(String title,java.util.function.Consumer<LinearLayout> fill){
        LinearLayout detail=new LinearLayout(this);detail.setOrientation(LinearLayout.VERTICAL);detail.setVisibility(View.GONE);fill.accept(detail);
        Button toggle=LedgerUi.action(this,title+" ▾",false,dark,()->detail.setVisibility(detail.getVisibility()==View.VISIBLE?View.GONE:View.VISIBLE));
        body.addView(toggle);body.addView(detail);Ui.addSpacer(body,10);
    }
    private void advancedData(){
        LinearLayout detail=new LinearLayout(this);detail.setOrientation(LinearLayout.VERTICAL);
        detail.setPadding(Ui.dp(this,16),Ui.dp(this,12),Ui.dp(this,16),Ui.dp(this,12));
        android.widget.ScrollView scroll=new android.widget.ScrollView(this);scroll.addView(detail);
        Map<String,String> choices=new LinkedHashMap<>();
        for(LedgerRecord row:data.records)if(visiblePolicy(row.policy()))choices.put(row.policy(),label(row.policy()));
        for(LedgerAggregation.Day day:data.days)if(visiblePolicy(day.policy))choices.put(day.policy,label(day.policy));
        java.util.List<String> keys=new ArrayList<>(choices.keySet());
        if(!keys.isEmpty()){
            if(!keys.contains(policy))policy=keys.get(keys.size()-1);
            Spinner meter=Ui.spinner(this,choices.values().toArray(new String[0]),dark);meter.setSelection(keys.indexOf(policy));detail.addView(meter);
            LinearLayout rows=new LinearLayout(this);rows.setOrientation(LinearLayout.VERTICAL);detail.addView(rows);
            meter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
                public void onItemSelected(AdapterView<?> parent,View view,int position,long id){policy=keys.get(position);rows.removeAllViews();
                    rows.addView(LedgerUi.heading(LedgerAnalyticsActivity.this,getString(R.string.next_intervals),dark));addUncertain(rows);
                    rows.addView(LedgerUi.action(LedgerAnalyticsActivity.this,getString(R.string.v3_observations),false,dark,()->detail(selectedDate)));
                    rows.addView(LedgerUi.heading(LedgerAnalyticsActivity.this,getString(R.string.next_events),dark));addEvents(rows);
                }
                public void onNothingSelected(AdapterView<?> parent) { }
            });
        }else detail.addView(LedgerUi.caption(this,getString(R.string.ui_ledger_none),dark));
        new AlertDialog.Builder(this).setTitle(R.string.ux_data_details).setView(scroll).setPositiveButton(R.string.ui_done_e9b450,null).show();
    }
    private void exportChoice(){new AlertDialog.Builder(this).setTitle(R.string.ui_ledger_export)
            .setItems(new String[]{getString(R.string.next_export_csv),getString(R.string.next_export_json)},(dialog,which)->picker(which==1)).show();}
    private LedgerAggregation.Day find(LocalDate date){return LedgerPresentation.find(data.days,policy,date);}
    private String dayText(LocalDate date,String title){LedgerAggregation.Day day=find(date);
        if(day==null)return title+" · "+getString(R.string.ui_ledger_none);
        if(!LedgerPresentation.measured(day))return title+" · "+getString(R.string.ui_ledger_comparison);
        String quality=getString(day.uncertain>0?R.string.next_gap:day.points==0?R.string.v3_zero:R.string.next_partial);
        if(day.legacy)quality+=" · "+getString(R.string.next_legacy_tag);
        return getString(R.string.next_day_value,title,number(day.points),quality);}
    private void addForecast(LedgerForecast.Result result){
        LedgerUi.section(body,getString(R.string.ui_ledger_forecast),dark);LinearLayout card=Ui.card(this,dark);
        int status=result.ready?(result.risk?R.string.ui_ledger_risk:R.string.ui_ledger_safe):"stale".equals(result.reason)
                ?R.string.ui_ledger_refresh:"reset".equals(result.reason)?R.string.ui_ledger_reset_unknown:R.string.ui_ledger_collecting;
        card.addView(LedgerUi.badge(this,getString(status),dark,result.risk||"stale".equals(result.reason)));Ui.addSpacer(card,14);
        card.addView(LedgerUi.caption(this,getString(R.string.ui_ledger_estimate)+" · "+getString(R.string.ui_ledger_rate_label),dark));
        card.addView(LedgerUi.amount(this,result.ready?getString(R.string.ui_ledger_per_day,number(result.dailyRate)):"—",dark,28));
        if(result.budgetAvailable){Ui.addSpacer(card,12);card.addView(LedgerUi.caption(this,getString(R.string.ui_ledger_budget_label),dark));
            card.addView(LedgerUi.amount(this,getString(R.string.ui_ledger_per_day,number(result.budget)),dark,24));}
        Ui.addSpacer(card,12);
        if(result.ready){
            card.addView(LedgerUi.caption(this,result.risk?getString(R.string.insight_depletion,time(result.exhaustion))
                    :getString(R.string.insight_remaining_at_reset,number(result.atReset)),dark));
            if(spikeEnabled()&&result.spike)card.addView(LedgerUi.caption(this,getString(R.string.next_spike,number(result.acceleration)),dark));
            Ui.addSpacer(card,8);card.addView(LedgerUi.caption(this,getString(R.string.next_forecast_note),dark));
        }else card.addView(LedgerUi.caption(this,getString("stale".equals(result.reason)?R.string.ledger_stale:
                "reset".equals(result.reason)?R.string.ledger_missing_reset:R.string.ui_ledger_warmup),dark));
        body.addView(card);Ui.addSpacer(body,12);
    }
    private void addCalendar(LocalDate today){
        LedgerUi.section(body,getString(R.string.next_calendar),dark);
        LinearLayout navigation=Ui.horizontal(this,0);
        Button previous=LedgerUi.action(this,"‹",false,dark,()->{}),next=LedgerUi.action(this,"›",false,dark,()->{});
        previous.setContentDescription(getString(R.string.next_previous_month));next.setContentDescription(getString(R.string.next_next_month));
        Button caption=LedgerUi.action(this,month.atDay(1).format(DateTimeFormatter.ofPattern(getString(R.string.hud_month_pattern),Locale.getDefault())),false,dark,()->{month=YearMonth.from(today);selectedDate=today;render();});
        caption.setContentDescription(getString(R.string.hud_current_month));
        previous.setEnabled(month.isAfter(YearMonth.from(today.minusDays(UsageLedgerDatabase.DAILY_DAYS))));
        next.setEnabled(month.isBefore(YearMonth.from(today)));
        previous.setOnClickListener(v->{month=month.minusMonths(1);selectedDate=month.atDay(1);render();});next.setOnClickListener(v->{month=month.plusMonths(1);selectedDate=month.atDay(1);render();});
        navigation.addView(previous,new LinearLayout.LayoutParams(Ui.dp(this,48),-2));navigation.addView(caption,new LinearLayout.LayoutParams(0,-2,1));navigation.addView(next,new LinearLayout.LayoutParams(Ui.dp(this,48),-2));body.addView(navigation);
        GridLayout grid=new GridLayout(this);grid.setColumnCount(7);
        LocalDate first=month.atDay(1),start=first.minusDays(first.getDayOfWeek().getValue()-1);
        for(int i=0;i<7;i++){TextView header=Ui.text(this,start.plusDays(i).format(DateTimeFormatter.ofPattern("E",Locale.getDefault())),11,Ui.secondaryText(dark));header.setGravity(Gravity.CENTER);cell(grid,header);}
        for(int i=0;i<42;i++){LocalDate date=start.plusDays(i);LedgerAggregation.Day day=find(date);
            TextView text=Ui.text(this,YearMonth.from(date).equals(month)?Integer.toString(date.getDayOfMonth()):"",13,Ui.mainText(dark));text.setGravity(Gravity.CENTER);
            if(YearMonth.from(date).equals(month)){
                text.setContentDescription(getString(R.string.next_calendar_cell,date.toString(),dayText(date,"")));
                boolean emerald=LedgerCalendar.emerald(day);
                int accent=Ui.accent(this,dark);
                if(emerald){
                    text.setBackground(new CalendarGlowDrawable(getResources().getDisplayMetrics().density,date.equals(selectedDate),date.equals(today),dark));
                    text.setTextColor(0xFFD1FAE5);
                    text.setTypeface(Ui.mediumTypeface(this));
                    text.setContentDescription(text.getContentDescription()+" · "+getString(R.string.calendar_activity_highlight));
                }else{
                    android.graphics.drawable.GradientDrawable background=new android.graphics.drawable.GradientDrawable();background.setCornerRadius(Ui.dp(this,8));
                    int fill=LedgerCalendar.intensity(day)>0?Color.argb(LedgerCalendar.intensity(day),Color.red(accent),Color.green(accent),Color.blue(accent)):Ui.cardColor(this,dark);
                    background.setColor(fill);if(date.equals(selectedDate))background.setStroke(Ui.dp(this,2),accent);else if(date.equals(today))background.setStroke(Ui.dp(this,1),LedgerUi.muted(dark));
                    text.setBackground(background);
                }
                text.setSelected(date.equals(selectedDate));if(date.equals(today))text.setTypeface(Ui.mediumTypeface(this));
                text.setOnClickListener(v->{selectedDate=date;render();});
            }
            cell(grid,text);
        }
        android.widget.HorizontalScrollView calendarScroll=new android.widget.HorizontalScrollView(this);calendarScroll.setFillViewport(true);
        calendarScroll.addView(grid,new android.widget.FrameLayout.LayoutParams(Ui.dp(this,350),-2));body.addView(calendarScroll);
        Ui.addSpacer(body,10);body.addView(LedgerUi.caption(this,getString(R.string.ui_ledger_calendar_note),dark));Ui.addSpacer(body,12);
    }
    private void cell(GridLayout grid,TextView cell){GridLayout.LayoutParams p=new GridLayout.LayoutParams();p.width=0;p.height=Ui.dp(this,48);p.columnSpec=GridLayout.spec(GridLayout.UNDEFINED,1f);p.setMargins(1,1,1,1);grid.addView(cell,p);}
    private void detail(LocalDate date){
        StringBuilder text=new StringBuilder(dayText(date,date.toString()));LedgerAggregation.Day day=find(date);
        if(day!=null)text.append("\n").append(getString(R.string.next_day_counts,day.count,day.uncertain));
        int shown=0;
        for(LedgerRecord r:data.records)if(r.policy().equals(policy)&&LedgerAggregation.day(r.at).equals(date)){
            if(shown++>=100)break;text.append("\n\n").append(getString(R.string.next_row,time(r.at),number(r.used),number(r.remaining()),
                    getString(r.manual?R.string.next_manual_tag:"legacy".equals(r.source)?R.string.next_legacy_tag:R.string.next_auto_tag)));
        }
        if(shown==0)text.append("\n\n").append(getString(R.string.next_gap_note));
        if(shown>100)text.append("\n\n").append(getString(R.string.next_detail_limit));
        new AlertDialog.Builder(this).setTitle(getString(R.string.next_detail)).setMessage(text).setPositiveButton(R.string.ui_done_e9b450,null).show();
    }
    private void addWindows(){
        List<LedgerAggregation.WindowTotal> windows=new ArrayList<>();
        for(LedgerAggregation.WindowTotal window:LedgerAggregation.aggregate(data.records,java.util.Collections.emptyList()).windows)
            if(window.policy.equals(policy))windows.add(window);
        body.addView(Ui.separator(this,getString(R.string.next_window_totals)));LinearLayout card=Ui.card(this,dark);
        for(int i=Math.max(0,windows.size()-5);i<windows.size();i++) {
            LedgerAggregation.WindowTotal window=windows.get(i);
            card.addView(Ui.text(this,getString(R.string.next_window_value,time(window.reset),number(window.points)),13,Ui.mainText(dark)));
        }
        if(windows.isEmpty())card.addView(Ui.text(this,getString(R.string.next_gap),13,Ui.secondaryText(dark)));
        body.addView(card);
    }
    private void addEvents(LinearLayout target){
        LinearLayout card=Ui.card(this,dark);
        String meter=policy.split("\\|")[0];int shown=0;
        for(int i=data.events.size()-1;i>=0&&shown<20;i--){UsageLedgerDatabase.Event event=data.events.get(i);
            if(!event.meter.isEmpty()&&!meter.equals(event.meter))continue;shown++;
            card.addView(Ui.text(this,time(event.at)+" · "+eventLabel(event.type),13,Ui.mainText(dark)));}
        if(shown==0)card.addView(LedgerUi.caption(this,getString(R.string.ui_ledger_no_events),dark));target.addView(card);
    }
    private String eventLabel(String type){switch(type){
        case "RESET":return getString(R.string.next_reset);case "EARLY_WINDOW":return getString(R.string.next_window);
        case "CREDIT_CHANGE":return getString(R.string.next_credit_change);case "CORRECTION":return getString(R.string.next_correction);
        case "POLICY_CHANGE":return getString(R.string.next_policy);case "GAP":return getString(R.string.next_gap);
        case "MISSING_TIMELINE":return getString(R.string.next_missing_timeline);case "credit_used":return getString(R.string.insight_event_credit_used);
        case "credit_expired":return getString(R.string.insight_event_credit_expired);case "bank_increased":return getString(R.string.insight_event_bank_increased);
        default:return getString(R.string.next_window);}}
    private void addUncertain(LinearLayout target){
        LinearLayout card=Ui.card(this,dark);
        card.addView(Ui.text(this,getString(R.string.next_gap_note),12,Ui.secondaryText(dark)));int shown=0;
        for(int i=data.uncertain.size()-1;i>=0&&shown<12;i--){LedgerAggregation.Interval interval=data.uncertain.get(i);if(!interval.policy.equals(policy))continue;shown++;
            card.addView(Ui.text(this,getString(R.string.next_interval,time(interval.start),time(interval.end),number(interval.points),
                    getString("date_boundary".equals(interval.reason)?R.string.next_date_boundary:R.string.next_gap)),13,Ui.mainText(dark)));}
        if(shown==0)card.addView(LedgerUi.caption(this,getString(R.string.ui_ledger_no_intervals),dark));target.addView(card);
    }
    private void manual(){
        if(busy)return;busy=true;content.removeAllViews();content.addView(Ui.text(this,getString(R.string.refreshing),14,Ui.mainText(dark)));
        worker.execute(()->{int message;
            try{UsageSnapshot snapshot=UsageApi.refreshAndCache(getApplicationContext(),true);
                message=UsageLedgerDatabase.manuallySaved(getApplicationContext(),snapshot.fetchedAtMillis)?R.string.next_manual_saved:R.string.next_manual_db_failed;
                try{RefreshScheduler.scheduleAtNextReset(getApplicationContext(),snapshot);WidgetRenderer.updateAll(getApplicationContext());}catch(Exception ignored){}
            }catch(Exception ignored){message=R.string.next_manual_failed;}
            final int result=message;runOnUiThread(()->{if(isDestroyed()||isFinishing())return;busy=false;Toast.makeText(this,result,Toast.LENGTH_LONG).show();reload();});
        });
    }
    private void picker(boolean json){
        try{Intent create=new Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE)
                .setType(json?"application/json":"text/csv").putExtra(Intent.EXTRA_TITLE,"CodexMeter-usage-"+LedgerAggregation.day(System.currentTimeMillis())+(json?".json":".csv"));
            startActivityForResult(create,json?EXPORT_JSON:EXPORT_CSV);
        }catch(Exception ignored){Toast.makeText(this,R.string.next_export_failed,Toast.LENGTH_LONG).show();}
    }
    @Override protected void onActivityResult(int request,int result,Intent intent){
        super.onActivityResult(request,result,intent);
        if(result!=RESULT_OK||intent==null||intent.getData()==null||request!=EXPORT_JSON&&request!=EXPORT_CSV)return;
        android.net.Uri destination=intent.getData();
        worker.execute(()->{int message=R.string.next_export_failed;
            try{UsageLedgerDatabase.Data snapshot=UsageLedgerDatabase.load(getApplicationContext());List<JSONObject> events=new ArrayList<>();
                for(UsageLedgerDatabase.Event event:snapshot.events)events.add(new JSONObject().put("type",event.type).put("at_utc",LedgerExport.utc(event.at)).put("meter",event.meter).put("origin",event.origin));
                String export=request==EXPORT_JSON?LedgerExport.json(snapshot.records,snapshot.days,snapshot.uncertain,events):LedgerExport.csv(snapshot.records,snapshot.days,snapshot.uncertain,events);
                try(OutputStream output=getContentResolver().openOutputStream(destination,"wt")){if(output==null)throw new IllegalStateException("Export destination unavailable");output.write(export.getBytes(StandardCharsets.UTF_8));}
                message=R.string.next_export_done;
            }catch(Exception ignored){}
            final int feedback=message;runOnUiThread(()->{if(!isDestroyed())Toast.makeText(this,feedback,Toast.LENGTH_LONG).show();});
        });
    }
    private String label(String key){String[] parts=key.split("\\|");String meter=parts[0];String name;
        switch(meter){case "five_hour":name=getString(R.string.ui_5_hour_bc4288);break;case "weekly":name=getString(R.string.ui_weekly_158f3d);break;case "monthly":name=getString(R.string.ui_monthly_d31edb);break;
            default:name=getString(R.string.next_additional,meter.substring(11,Math.min(meter.length(),19))+" · "+(meter.endsWith("secondary")?"2":"1"));}
        return name+" · "+parts[1]+" · "+UsageInsightDisplay.duration(this,Long.parseLong(parts[2])*1000L);
    }
    private static String number(double value){return UsagePrecision.number(value);}
    private String time(long at){return at<=0?getString(R.string.next_missing_timeline):Instant.ofEpochMilli(at).atZone(LedgerAggregation.ZONE).format(DateTimeFormatter.ofPattern("MM-dd HH:mm",Locale.getDefault()));}
}
