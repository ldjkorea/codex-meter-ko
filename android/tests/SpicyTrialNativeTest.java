package dev.bennett.codexmeter;

import android.app.*;
import android.content.*;
import android.content.res.Configuration;
import android.graphics.*;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.util.*;

/** Synthetic Android components only, on a dedicated empty QA AVD. No account/API calls. */
public final class SpicyTrialNativeTest extends Instrumentation {
    int checks;void check(boolean b,String why){checks++;if(!b)throw new AssertionError(why);}
    @Override public void onCreate(Bundle b){super.onCreate(b);start();}
    @Override public void onStart(){Bundle result=new Bundle();Activity a=null;try{
        a=startActivitySync(new Intent(getTargetContext(),AboutActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));final Activity activity=a;
        Configuration original=new Configuration(a.getResources().getConfiguration()),ko=new Configuration(original);ko.setLocale(Locale.KOREAN);
        runOnMainSync(()->activity.getResources().updateConfiguration(ko,activity.getResources().getDisplayMetrics()));
        long now=System.currentTimeMillis();UsageSnapshot snapshot=new UsageSnapshot("pro",true,false,null,new UsageWindow(5,604800,259200,(now+3*86400000L)/1000),now);
        long begin=System.nanoTime();String first=SpicyQuotes.select(a,"synthetic-native-2817",snapshot,true,false,Double.NaN,now);long preparation=System.nanoTime()-begin;
        check(first!=null&&!first.isEmpty(),"Active Korean catalog");check(first.equals(SpicyQuotes.select(a,"synthetic-native-2817",snapshot,true,false,Double.NaN,now)),"Rebuild keeps actual persisted choice");
        Set<String> recent=new HashSet<>();for(int i=0;i<21;i++){SpicyQuotes.manual();String selected=SpicyQuotes.select(a,"synthetic-native-2817",snapshot,true,false,Double.NaN,now);check(recent.add(selected),"Actual Android 5% recent-20 selection");}
        long[] timing=new long[1000];for(int i=0;i<timing.length;i++){long at=System.nanoTime();SpicyQuotes.select(a,"synthetic-native-2817",snapshot,true,false,Double.NaN,now);timing[i]=System.nanoTime()-at;}Arrays.sort(timing);
        Throwable[] failure={null};runOnMainSync(()->{try{render(activity,ko,snapshot,first);}catch(Throwable e){failure[0]=e;}});if(failure[0]!=null)throw failure[0];
        Configuration en=new Configuration(original);en.setLocale(Locale.ENGLISH);runOnMainSync(()->activity.getResources().updateConfiguration(en,activity.getResources().getDisplayMetrics()));
        check(SpicyQuotes.select(a,"synthetic-native-2817",snapshot,true,false,Double.NaN,now)==null,"Original English fallback");
        check(!TaskStatusStore.enabled(a),"Trial OFF without login");check(TaskStatusStore.text(a,3).equals(a.getString(R.string.task_trial_off)),"OFF has no fabricated tasks");
        Intent info=new Intent(a,TaskStatusActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);Activity trial=startActivitySync(info);runOnMainSync(trial::finish);
        result.putString("stream","Native phrase/trial components: "+checks+" checks passed. First prepare+prefs="+preparation/1e6+"ms; hot selection p50="+timing[500]/1e6+"ms p95="+timing[950]/1e6+"ms. Synthetic emulator components, not Samsung/Remote/live account proof.\n");finish(-1,result);
    }catch(Throwable e){result.putString("stream",android.util.Log.getStackTraceString(e));finish(0,result);}finally{if(a!=null){Activity last=a;runOnMainSync(last::finish);}}}
    void render(Activity a,Configuration ko,UsageSnapshot snapshot,String quote)throws Exception{
        Class<?> compatible=Class.forName("androidx.appcompat.app.AppCompatActivity");java.lang.reflect.Method ai=LiveCards.class.getDeclaredMethod("ai",compatible,boolean.class,LiveUtilization.Result.class,UsageSnapshot.class,boolean.class,double.class,long.class,String.class);ai.setAccessible(true);
        for(int width:new int[]{320,411})for(boolean dark:new boolean[]{true,false}){Configuration config=new Configuration(ko);config.screenWidthDp=width;config.fontScale=width==320?1.5f:1;a.getResources().updateConfiguration(config,a.getResources().getDisplayMetrics());
            View card=(View)ai.invoke(null,a,dark,new LiveUtilization.Result(5,5,0,0,0,true,false,true),snapshot,true,Double.NaN,System.currentTimeMillis(),quote);
            save(a,card,width,240,"quote-"+width+"-"+dark+".png");TextView text=(TextView)((LinearLayout)card).getChildAt(((LinearLayout)card).getChildCount()-1);
            check(text.getLayout().getLineCount()>=1&&text.getLayout().getEllipsisCount(0)==0,"Korean quote wraps without ellipsis");check(text.getBottom()<=Ui.dp(a,240),"Card fits capture");}
        for(int count:new int[]{1,3}){int height=count==1?180:330;RemoteViews views=new RemoteViews(a.getPackageName(),R.layout.task_status_widget);StringBuilder text=new StringBuilder("마지막 연결 · 12:34");for(int i=0;i<count;i++)text.append("\n\nCodex · 합성 ").append(i+1).append("\n마지막 확인 상태: 작업 중");views.setTextViewText(R.id.task_status_copy,text);View widget=views.apply(a,null);save(a,widget,280,height,"task-"+count+"-synthetic.png");
            TextView copy=widget.findViewById(R.id.task_status_copy);check(copy.getLayout().getLineTop(copy.getLayout().getLineCount())<=copy.getHeight(),"Trial widget fixture not vertically clipped");}
    }
    static void save(Context c,View v,int w,int h,String name)throws Exception{int width=Ui.dp(c,w),height=Ui.dp(c,h);v.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(height,View.MeasureSpec.EXACTLY));v.layout(0,0,width,height);Bitmap b=Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(b);canvas.drawColor(name.contains("false")?0xfff2f3f5:0xff0d0e10);v.draw(canvas);File dir=new File(c.getExternalFilesDir(null),"spicy-trial-fixtures");dir.mkdirs();try(FileOutputStream stream=new FileOutputStream(new File(dir,name))){b.compress(Bitmap.CompressFormat.PNG,100,stream);}b.recycle();}
}
