package dev.bennett.codexmeter;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.io.File;
import java.io.FileOutputStream;
import java.util.concurrent.Executors;

/** Synthetic native production-component checks on a fresh isolated emulator, no login/API. */
public final class UiPolishNativeTest extends Instrumentation {
    private int checks;
    private void check(boolean ok,String label){checks++;if(!ok)throw new AssertionError(label);}
    @Override public void onCreate(Bundle args){super.onCreate(args);start();}
    @Override public void onStart(){
        Bundle result=new Bundle();Activity a=null;
        try{
            Intent intent=new Intent(getTargetContext(),AboutActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            a=startActivitySync(intent);final Activity activity=a;
            final Throwable[] failure={null};
            runOnMainSync(()->{try{verify(activity);}catch(Throwable e){failure[0]=e;}});
            if(failure[0]!=null)throw failure[0];
            result.putString("stream","Native polish component checks: "+checks+" passed; synthetic fixtures, not Samsung launcher or live-account proof.\n");finish(-1,result);
        }catch(Throwable error){result.putString("stream",android.util.Log.getStackTraceString(error));finish(0,result);}
        finally{if(a!=null){final Activity close=a;runOnMainSync(close::finish);}}
    }
    private void verify(Activity a)throws Exception{
        Configuration original=new Configuration(a.getResources().getConfiguration());
        Configuration ko=new Configuration(original);ko.setLocale(java.util.Locale.KOREAN);a.getResources().updateConfiguration(ko,a.getResources().getDisplayMetrics());
        java.util.List<ReleaseCatalog.Entry> releases=ReleaseCatalog.all(a);
        check(releases.size()==44,"All historic releases bundled");check(releases.get(43).version.equals("1.0.0"),"Earliest release retained");
        check(ReleaseCatalog.notes(a,"2.8.15-beta","missing").contains("삼족오"),"Installed version notes local and Korean");
        check(releases.stream().anyMatch(e->e.version.equals("2.8.11")&&e.notes.contains("Windows")),"Desktop widget history present");
        String[] plans={"free","go","plus","pro_lite","pro20x"};int prior=0;
        LinearLayout planRow=new LinearLayout(a);planRow.setOrientation(LinearLayout.VERTICAL);
        for(String plan:plans){int resource=PlanArtwork.image(plan);check(resource!=0&&resource!=prior,"Distinct plan resource "+plan);prior=resource;
            LinearLayout row=new LinearLayout(a);android.widget.ImageView picture=new android.widget.ImageView(a);picture.setImageResource(resource);picture.setScaleType(android.widget.ImageView.ScaleType.FIT_CENTER);
            row.addView(picture,new LinearLayout.LayoutParams(dp(a,80),dp(a,80)));TextView label=Ui.text(a,PlanArtwork.label(plan),21,Ui.mainText(true));row.addView(label);planRow.addView(row);}
        check(PlanArtwork.image("pro5x")==PlanArtwork.image("pro-lite"),"Pro Lite aliases");check(PlanArtwork.image("enterprise")==0,"Unsupported plan never pretends to be Free");
        android.app.job.JobScheduler jobs=(android.app.job.JobScheduler)a.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        android.content.SharedPreferences pairs=a.getSharedPreferences("codex_lan_sync",0);
        check(pairs.getAll().isEmpty()&&jobs.getPendingJob(LanWifiScheduler.JOB)==null,"Fresh isolated unpaired state");
        String fixtureKey="aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
        try{
            check(pairs.edit().putString(fixtureKey,"isolated-invalid-pair-no-account").commit(),"Synthetic opt-in marker");
            LanWifiScheduler.ensureScheduled(a);android.app.job.JobInfo pending=jobs.getPendingJob(LanWifiScheduler.JOB);
            check(pending!=null&&pending.isPersisted(),"Actual Android persisted job accepted");
            check(pending.getIntervalMillis()==900000&&pending.getRequiredNetwork().hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI),"Actual Wi-Fi constraint and period");
        }finally{pairs.edit().remove(fixtureKey).commit();LanWifiScheduler.ensureScheduled(a);}
        check(jobs.getPendingJob(LanWifiScheduler.JOB)==null,"Unpaired job cancelled without user data");
        save(a,planRow,360,450,"subscriptions.png");
        Class<?> compatible=Class.forName("androidx.appcompat.app.AppCompatActivity");
        java.lang.reflect.Method crest=LiveCards.class.getDeclaredMethod("crest",compatible,boolean.class,LiveUtilization.Result.class,boolean.class);crest.setAccessible(true);
        java.lang.reflect.Constructor<?> constructor=Class.forName("dev.bennett.codexmeter.LedgerDashboard$State").getDeclaredConstructor(compatible,java.util.concurrent.ExecutorService.class,UsageSnapshot.class,UsageWindow.class,String.class,int.class,boolean.class);constructor.setAccessible(true);
        java.lang.reflect.Field cardField=Class.forName("dev.bennett.codexmeter.LedgerDashboard$State").getDeclaredField("card");cardField.setAccessible(true);
        for(boolean dark:new boolean[]{true,false})for(int width:new int[]{411,320}){
            Configuration config=new Configuration(ko);config.screenWidthDp=width;config.fontScale=width==320?1.4f:1f;a.getResources().updateConfiguration(config,a.getResources().getDisplayMetrics());
            long now=System.currentTimeMillis(),end=now+4*86400000L;UsageWindow window=new UsageWindow(65,604800,345600,end/1000);
            UsageSnapshot snapshot=new UsageSnapshot("pro_lite",true,false,null,window,now);
            LiveUtilization.Result live=new LiveUtilization.Result(75,65,end-4*604800000L,end,1,true,false,true);
            LinearLayout home=new LinearLayout(a);home.setOrientation(LinearLayout.VERTICAL);home.setPadding(dp(a,16),dp(a,12),dp(a,16),dp(a,12));
            home.addView((View)crest.invoke(null,a,dark,live,true));Ui.addSpacer(home,10);
            java.util.concurrent.ExecutorService worker=Executors.newSingleThreadExecutor();
            Object state=constructor.newInstance(a,worker,snapshot,window,"weekly",R.string.ui_weekly_158f3d,dark);
            View quota=(View)cardField.get(state);home.addView(quota);measure(a,home,width,700);
            check(!labels(quota).contains("5시간 사용량"),"Unavailable five-hour line omitted");
            check(quota.getWidth()<=dp(a,width-32),"No horizontal overflow");
            if(width==411)check(quota.getBottom()<=dp(a,630),"Crest and quota fit normal first viewport");
            save(a,home,width,700,"home-"+width+"-"+(dark?"dark":"light")+".png");worker.shutdown();
        }
        a.getResources().updateConfiguration(ko,a.getResources().getDisplayMetrics());
        TextView notes=ReleaseNotesUi.create(a,"## 보기 좋은 업데이트\n\n- 긴 한국어 업데이트 내용이 다음 줄로 넘어갈 때도 같은 시작 위치를 유지하도록 정리했습니다.\n- **티어 휘장**과 위젯 설정을 보존합니다.\n",true);
        android.text.Spanned text=(android.text.Spanned)notes.getText();check(text.getSpans(0,text.length(),android.text.style.BulletSpan.class).length==2,"Actual Android bullet spans");
        check(text.getSpans(0,text.length(),android.text.style.LeadingMarginSpan.Standard.class).length==2,"Wrapped bullet indentation");save(a,notes,320,240,"release-notes.png");
        a.getResources().updateConfiguration(original,a.getResources().getDisplayMetrics());
    }
    private static int dp(Context c,int value){return Ui.dp(c,value);}
    private static String labels(View v){StringBuilder out=new StringBuilder();if(v instanceof TextView)out.append(((TextView)v).getText());if(v instanceof android.view.ViewGroup){android.view.ViewGroup group=(android.view.ViewGroup)v;for(int i=0;i<group.getChildCount();i++)out.append(labels(group.getChildAt(i)));}return out.toString();}
    private static void measure(Context c,View v,int w,int h){v.measure(View.MeasureSpec.makeMeasureSpec(dp(c,w),View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(dp(c,h),View.MeasureSpec.EXACTLY));v.layout(0,0,dp(c,w),dp(c,h));}
    private static void save(Context c,View view,int width,int height,String name)throws Exception{
        measure(c,view,width,height);Bitmap image=Bitmap.createBitmap(dp(c,width),dp(c,height),Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(image);canvas.drawColor(0xff17191c);view.draw(canvas);
        File directory=new File(c.getExternalFilesDir(null),"polish-fixtures");if(!directory.exists()&&!directory.mkdirs())throw new IllegalStateException("Capture directory");
        try(FileOutputStream out=new FileOutputStream(new File(directory,name))){image.compress(Bitmap.CompressFormat.PNG,100,out);}image.recycle();
    }
}
