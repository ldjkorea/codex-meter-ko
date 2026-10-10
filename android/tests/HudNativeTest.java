package dev.bennett.codexmeter;

import android.app.*;
import android.content.*;
import android.content.res.Configuration;
import android.graphics.*;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.lang.reflect.*;
import java.time.*;
import java.util.*;

/** Dedicated owned AVD, synthetic UI data. No credentials, usage API, installed-widget reset or live ledger writes. */
public final class HudNativeTest extends Instrumentation {
    int checks;void check(boolean b,String why){checks++;if(!b)throw new AssertionError(why);}
    static Field field(Class<?> type,String name)throws Exception{Field f=type.getDeclaredField(name);f.setAccessible(true);return f;}
    @Override public void onCreate(Bundle b){super.onCreate(b);start();}
    @Override public void onStart(){Bundle result=new Bundle();Activity a=null;try{
        a=startActivitySync(new Intent(getTargetContext(),AboutActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));final Activity activity=a;
        Throwable[] error={null};runOnMainSync(()->{try{home(activity);}catch(Throwable e){error[0]=e;}});if(error[0]!=null)throw error[0];
        Activity records=startActivitySync(new Intent(getTargetContext(),RecordsActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));Thread.sleep(500);waitForIdleSync();
        runOnMainSync(()->{try{records(records);}catch(Throwable e){error[0]=e;}});if(error[0]!=null)throw error[0];
        Activity settings=startActivitySync(new Intent(getTargetContext(),MeterSettingsActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));waitForIdleSync();
        runOnMainSync(()->{try{View content=settings.findViewById(R.id.dashboard_content);check(text(content).contains(settings.getString(R.string.hud_data_settings)),"Data settings reachable without an account");save(settings,content,411,"settings.png");settings.finish();}catch(Throwable e){error[0]=e;}});if(error[0]!=null)throw error[0];
        Activity status=startActivitySync(new Intent(getTargetContext(),TaskStatusActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));waitForIdleSync();
        runOnMainSync(()->{try{View content=status.findViewById(R.id.dashboard_content);check(text(content).contains(status.getString(R.string.task_trial_off)),"Status OFF does not fabricate tasks");save(status,content,411,"status-off.png");status.finish();records.finish();activity.finish();}catch(Throwable e){error[0]=e;}});if(error[0]!=null)throw error[0];
        result.putString("stream","HUD native components: "+checks+" checks passed. Synthetic UI, 320/411dp, 1x/1.5x fonts; not Samsung/live account/Tailscale/launcher-host proof.\n");finish(-1,result);
    }catch(Throwable e){result.putString("stream",android.util.Log.getStackTraceString(e));finish(0,result);}}
    void home(Activity a)throws Exception{
        Class<?> compatible=Class.forName("androidx.appcompat.app.AppCompatActivity");
        Method crest=LiveCards.class.getDeclaredMethod("crest",compatible,boolean.class,LiveUtilization.Result.class,boolean.class);crest.setAccessible(true);
        Method overview=LedgerDashboard.class.getDeclaredMethod("addOverview",compatible,LinearLayout.class,java.util.concurrent.ExecutorService.class,UsageSnapshot.class,boolean.class);overview.setAccessible(true);
        Configuration original=new Configuration(a.getResources().getConfiguration());
        java.util.concurrent.ExecutorService worker=java.util.concurrent.Executors.newSingleThreadExecutor();
        try{for(int width:new int[]{320,411})for(boolean dark:new boolean[]{false,true}){
            Configuration config=new Configuration(original);config.setLocale(Locale.KOREAN);config.screenWidthDp=width;config.fontScale=width==320?1.5f:1;
            a.getResources().updateConfiguration(config,a.getResources().getDisplayMetrics());
            long now=System.currentTimeMillis();UsageSnapshot snapshot=new UsageSnapshot("plus",true,false,new UsageWindow(31.23456,18000,7200,(now+7200000L)/1000),new UsageWindow(13.42736,604800,259200,(now+259200000L)/1000),now);
            LinearLayout root=new LinearLayout(a);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(Ui.dp(a,16),0,Ui.dp(a,16),0);
            View tier=(View)crest.invoke(null,a,dark,new LiveUtilization.Result(98,13.42736,0,0,1,true,false,true),true);root.addView(tier);
            overview.invoke(null,a,root,worker,snapshot,dark);
            save(a,root,width,"home-"+width+"-"+dark+".png");String text=text(root);
            for(String required:new String[]{"CHALLENGER","86.573%","13.427%",a.getString(R.string.ui_ledger_today_usage),a.getString(R.string.v3_limit_details),a.getString(R.string.ui_refresh_56e3ba)})check(text.contains(required),"Home retains "+required);
            check(text.contains("5"),"Five-hour assistance remains");checkNoEllipsis(root);
        }}finally{worker.shutdown();a.getResources().updateConfiguration(original,a.getResources().getDisplayMetrics());}
        RemoteViews widget=new RemoteViews(a.getPackageName(),R.layout.task_status_widget);
        widget.setTextViewText(R.id.task_status_copy,"연결됨\n마지막 연결 · 12:34\n\nCodex · 합성 1\n작업 중 · 12:33");View v=widget.apply(a,null);save(a,v,280,"status-widget-synthetic.png");check(text(v).contains("12:34"),"Independent widget inflates with last contact");
    }
    void records(Activity a)throws Exception{
        long now=System.currentTimeMillis(),reset=now+3*86400000L;String policy="weekly|plus|604800";
        UsageLedgerDatabase.Data data=new UsageLedgerDatabase.Data();
        LocalDate today=LedgerAggregation.day(now);
        for(int i=0;i<4;i++){
            LedgerAggregation.Day day=new LedgerAggregation.Day(policy,today.minusDays(i).toString());day.points=i==1?0:i==2?13.5:2.137;day.count=2;day.coveredMillis=3600000;data.days.add(day);
        }
        data.records.add(new LedgerRecord("weekly","plus",now-3600000,11.29036,"11.29036",reset,604800,"api_precise",false));
        data.records.add(new LedgerRecord("weekly","plus",now,13.42736,"13.42736",reset,604800,"api_precise",false));
        Class<?> type=LedgerAnalyticsActivity.class;field(type,"data").set(a,data);field(type,"policy").set(a,policy);field(type,"screen").set(a,1);field(type,"period").set(a,7);
        field(type,"loadGeneration").setLong(a,field(type,"loadGeneration").getLong(a)+1);
        Method controls=type.getDeclaredMethod("controls");controls.setAccessible(true);controls.invoke(a);
        View content=a.findViewById(R.id.dashboard_content);save(a,content,411,"records-synthetic.png");String text=text(content);
        check(text.indexOf(a.getString(R.string.ui_ledger_total))<text.indexOf(a.getString(R.string.next_calendar)),"Analysis appears before calendar");
        check(text.lastIndexOf(a.getString(R.string.matte_settings))>text.indexOf(a.getString(R.string.next_calendar)),"Settings is below selected date");
        check(!text.contains(a.getString(R.string.next_events)),"Events moved out of records");
        View previous=findDescription(content,a.getString(R.string.next_previous_month));check(previous!=null,"Accessible previous-month arrow");
        YearMonth before=(YearMonth)field(type,"month").get(a);previous.performClick();check(field(type,"month").get(a).equals(before.minusMonths(1)),"Month arrow interaction");
        check(field(type,"period").getInt(a)==7,"Period selection remains after month move");
        View current=findDescription(content,a.getString(R.string.hud_current_month));current.performClick();check(field(type,"month").get(a).equals(YearMonth.from(today)),"Current month shortcut");
    }
    void checkNoEllipsis(View v){if(v instanceof TextView){TextView t=(TextView)v;if(t.getLayout()!=null)for(int i=0;i<t.getLayout().getLineCount();i++)check(t.getLayout().getEllipsisCount(i)==0,"No required text truncation");}if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++)checkNoEllipsis(((ViewGroup)v).getChildAt(i));}
    static String text(View v){StringBuilder s=new StringBuilder();if(v instanceof TextView)s.append(((TextView)v).getText()).append('\n');if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++)s.append(text(((ViewGroup)v).getChildAt(i)));return s.toString();}
    static View findDescription(View v,String value){if(value.contentEquals(v.getContentDescription()==null?"":v.getContentDescription()))return v;if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++){View found=findDescription(((ViewGroup)v).getChildAt(i),value);if(found!=null)return found;}return null;}
    static void save(Context c,View v,int dp,String name)throws Exception{
        int w=Ui.dp(c,dp);v.measure(View.MeasureSpec.makeMeasureSpec(w,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED));int h=Math.max(Ui.dp(c,120),v.getMeasuredHeight());
        v.layout(0,0,w,h);Bitmap b=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(b);canvas.drawColor(0xff0d0e10);v.draw(canvas);
        File dir=new File(c.getExternalFilesDir(null),"hud-fixtures");dir.mkdirs();try(FileOutputStream out=new FileOutputStream(new File(dir,name))){b.compress(Bitmap.CompressFormat.PNG,100,out);}b.recycle();
    }
}
