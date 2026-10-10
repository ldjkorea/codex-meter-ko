package dev.bennett.codexmeter;

import android.app.*;import android.appwidget.*;import android.content.*;import android.content.res.Configuration;import android.graphics.*;import android.os.*;import android.view.*;import android.widget.*;import org.json.*;import java.io.*;import java.util.*;

/** Owned emulator host, synthetic state only. Never changes auth, ledger or real user widget IDs. */
public final class NamedWidgetsNativeTest extends Instrumentation {
    int checks;void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    public void onCreate(Bundle b){super.onCreate(b);start();}
    public void onStart(){Bundle result=new Bundle();Activity a=null;try{
        a=startActivitySync(new Intent(getTargetContext(),AboutActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));final Activity activity=a;Throwable[] error={null};
        runOnMainSync(()->{try{widgets(activity);}catch(Throwable e){error[0]=e;}});if(error[0]!=null)throw error[0];
        Activity edit=startActivitySync(new Intent(getTargetContext(),EvolutionWidgetConfigActivity.class).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,991).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));waitForIdleSync();
        runOnMainSync(()->{try{check(edit.findViewById(R.id.widget_settings_content)!=null,"Real evolution configuration opens without AppCompat RemoteViews exception");edit.finish();activity.finish();}catch(Throwable e){error[0]=e;}});if(error[0]!=null)throw error[0];
        result.putString("stream","Named widgets: "+checks+" checks passed. Real emulator AppWidgetHost/resize metadata, synthetic tasks and quota, no Samsung/Tailscale/Doze proof.\n");finish(-1,result);
    }catch(Throwable e){result.putString("stream",android.util.Log.getStackTraceString(e));finish(0,result);}}
    void widgets(Activity activity)throws Exception{
        Context c=activity.getApplicationContext();Configuration original=new Configuration(c.getResources().getConfiguration()),ko=new Configuration(original);ko.setLocale(Locale.KOREAN);c.getResources().updateConfiguration(ko,c.getResources().getDisplayMetrics());
        AppWidgetManager manager=AppWidgetManager.getInstance(c);AppWidgetHost host=new AppWidgetHost(c,2820);List<Integer> allocated=new ArrayList<>();
        try{host.startListening();long now=System.currentTimeMillis();JSONObject data=new JSONObject().put("checked",now).put("verified",true).put("rows",new JSONArray());
            String[] names={"GPT HUD · 작업 이름과 위젯 개선","임대관리 · 화면 오류 수정","웨딩 관리 · 예약 검증"},states={"running","completed","interrupted"};
            for(int i=0;i<3;i++)data.getJSONArray("rows").put(new JSONObject().put("thread","a".repeat(32)).put("name",names[i]).put("state",states[i]).put("at",now).put("issue",i==0?"command_error":""));
            ComponentName task=new ComponentName(c,TaskStatusWidget.class);int id=host.allocateAppWidgetId();allocated.add(id);check(manager.bindAppWidgetIdIfAllowed(id,task),"Dedicated QA host can bind work widget");
            AppWidgetProviderInfo info=manager.getAppWidgetInfo(id);check(info.resizeMode==3&&info.minResizeWidth<info.minWidth,"Work widget advertises both resize directions and smaller minimum");
            for(int[] size:new int[][]{{180,180},{300,240},{360,450}}){
                RemoteViews remote=TaskStatusWidget.render(c,size[0],size[1],5,data,now,true);AppWidgetHostView view=host.createView(c,id,info);view.updateAppWidget(remote);save(c,view,size[0],size[1],"work-"+size[0]+"x"+size[1]+".png");
                check(text(view).contains(names[0]),"Host displays actual task title");check(text(view).contains(c.getString(R.string.task_named_command_error)),"Host distinguishes tool error from task failure");
                if(size[1]>400)check(text(view).contains(names[2]),"Expanded widget renders three independent tasks");
                Bundle options=size(size[0],size[1]);manager.updateAppWidgetOptions(id,options);new TaskStatusWidget().onAppWidgetOptionsChanged(c,manager,id,options);check(manager.getAppWidgetOptions(id).getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)==size[0],"Existing widget ID keeps updated size");
            }
            RemoteViews offline=TaskStatusWidget.render(c,300,260,-1,data,now-180000,true);View old=offline.apply(c,null);check(text(old).contains(c.getString(R.string.task_trial_disconnected))&&text(old).contains(c.getString(R.string.task_trial_last_state)),"Disconnected state retains last known title without fake failure");
            ComponentName evolution=new ComponentName(c,EvolutionWidget.class);int eid=host.allocateAppWidgetId();allocated.add(eid);check(manager.bindAppWidgetIdIfAllowed(eid,evolution),"Separate evolution widget provider remains bindable");
            UsageSnapshot quota=new UsageSnapshot("plus",true,false,new UsageWindow(25,18000,7200,(now+7200000)/1000),new UsageWindow(27,604800,259200,(now+259200000)/1000),now);
            for(int[] s:new int[][]{{180,160},{280,180},{360,320}}){
                RemoteViews remote=EvolutionWidget.render(c,eid,size(s[0],s[1]),"weekly,tier,weekly_reset",true,88,quota);AppWidgetHostView view=host.createView(c,eid,manager.getAppWidgetInfo(eid));view.updateAppWidget(remote);save(c,view,s[0],s[1],"evolution-"+s[0]+"x"+s[1]+".png");
                check(text(view).contains("73%"),"Evolution never hides all quota content in a small widget");check(!text(view).contains("5시간"),"Hidden five-hour choice stays hidden");
            }
        }finally{for(int id:allocated)host.deleteAppWidgetId(id);host.stopListening();c.getResources().updateConfiguration(original,c.getResources().getDisplayMetrics());}
    }
    static Bundle size(int w,int h){Bundle b=new Bundle();b.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,w);b.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH,w);b.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT,h);b.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT,h);return b;}
    static String text(View v){StringBuilder b=new StringBuilder();if(v instanceof TextView)b.append(((TextView)v).getText()).append('\n');if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++)b.append(text(((ViewGroup)v).getChildAt(i)));return b.toString();}
    static void save(Context c,View v,int width,int height,String name)throws Exception{int w=Ui.dp(c,width),h=Ui.dp(c,height);v.measure(View.MeasureSpec.makeMeasureSpec(w,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(h,View.MeasureSpec.EXACTLY));v.layout(0,0,w,h);Bitmap b=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);v.draw(new Canvas(b));File dir=new File(c.getExternalFilesDir(null),"named-widget-fixtures");dir.mkdirs();try(FileOutputStream stream=new FileOutputStream(new File(dir,name))){b.compress(Bitmap.CompressFormat.PNG,100,stream);}b.recycle();}
}
