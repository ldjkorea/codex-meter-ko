package dev.bennett.codexmeter;

import android.app.*;
import android.appwidget.*;
import android.content.*;
import android.graphics.*;
import android.os.*;
import android.view.View;
import android.widget.RemoteViews;
import java.util.List;

/** Additive home-only provider; original widget ids/options and Samsung surfaces are untouched. */
public final class EvolutionWidget extends AppWidgetProvider {
    static android.content.SharedPreferences prefs(Context c){return c.getSharedPreferences("codex_evolution_widgets",0);}
    static String selected(Context c,int id){return prefs(c).getString(id+".elements",EvolutionElements.DEFAULT);}
    public void onUpdate(Context c,AppWidgetManager m,int[] ids){for(int id:ids)m.updateAppWidget(id,render(c,id,m.getAppWidgetOptions(id)));schedule(c);}
    public void onAppWidgetOptionsChanged(Context c,AppWidgetManager m,int id,Bundle options){m.updateAppWidget(id,render(c,id,options));schedule(c);}
    public void onReceive(Context c,Intent i){super.onReceive(c,i);if("dev.bennett.codexmeter.EVOLUTION_TICK".equals(i.getAction()))updateAll(c);}
    static void updateAll(Context c){WidgetCrest.updateExistingIfChanged(c);TaskStatusWidget.update(c);try{AppWidgetManager m=AppWidgetManager.getInstance(c);int[] ids=m.getAppWidgetIds(new ComponentName(c,EvolutionWidget.class));
        for(int id:ids)m.updateAppWidget(id,render(c,id,m.getAppWidgetOptions(id)));if(ids.length>0)schedule(c);}catch(RuntimeException e){DiagnosticLog.error(c,"widget","evolution_update_failed",e);}}
    static RemoteViews render(Context c,int id,Bundle options){
        return render(c,id,options,selected(c,id),prefs(c).getBoolean(id+".theme",true),prefs(c).getInt(id+".opacity",88));
    }
    static RemoteViews render(Context c,int id,Bundle options,String selection,boolean themed,int opacity){
        return render(c,id,options,selection,themed,opacity,AppPreferences.loadSnapshot(c));
    }
    static RemoteViews render(Context c,int id,Bundle options,String selection,boolean themed,int opacity,UsageSnapshot s){
        long now=System.currentTimeMillis();
        boolean five=AppPreferences.showDashboardFiveHour(c)&&s!=null&&s.fiveHour!=null;
        boolean week=AppPreferences.showDashboardWeekly(c)&&s!=null&&s.weekly!=null;
        List<String> elements=EvolutionElements.visible(selection,five,week);
        int width=Math.max(120,Math.min(1000,options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,300))),columns=EvolutionElements.columns(elements.size(),width);
        int height=Math.max(140,options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT,180));
        float font=Math.max(1,c.getResources().getConfiguration().fontScale);
        if(elements.size()<=3&&width>=240)columns=Math.max(1,elements.size());
        RemoteViews root=new RemoteViews(c.getPackageName(),R.layout.evolution_widget);root.removeAllViews(R.id.evolution_rows);
        root.setInt(R.id.evolution_background,"setBackgroundColor",Color.argb(Math.round(Math.max(0,Math.min(100,opacity))*2.55f),23,25,28));
        root.setViewVisibility(R.id.evolution_frame,themed?View.VISIBLE:View.GONE);
        if(themed){Bitmap frame=Bitmap.createBitmap(Math.max(240,width*2),240,Bitmap.Config.ARGB_8888);TierTheme.Frame shape=new TierTheme.Frame(TierTheme.tier(c),true,false);shape.setBounds(0,0,frame.getWidth(),frame.getHeight());shape.draw(new Canvas(frame));root.setImageViewBitmap(R.id.evolution_frame,frame);}
        root.setViewVisibility(R.id.evolution_empty,elements.isEmpty()?View.VISIBLE:View.GONE);
        Intent edit=new Intent(c,EvolutionWidgetConfigActivity.class).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,id).setData(android.net.Uri.parse("codexmeter://evolution/edit/"+id));
        root.setOnClickPendingIntent(R.id.evolution_edit,PendingIntent.getActivity(c,id,edit,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE));
        root.setOnClickPendingIntent(R.id.evolution_background,PendingIntent.getActivity(c,0,new Intent(c,MainActivity.class),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE));
        int needed=72+((elements.size()+columns-1)/columns)*Math.round(88*font);
        boolean compact=height<needed;
        if(compact){int capacity=Math.max(1,(height-72)/Math.round(64*font))*columns;
            if(elements.size()>capacity){elements=elements.subList(0,capacity);root.setTextViewText(R.id.evolution_edit,c.getString(R.string.task_named_evo_compact));}
        }
        RemoteViews row=null;int tint=themed?TierTheme.accent(c,true):0xffeeeeee;
        for(int i=0;i<elements.size();i++){
            if(i%columns==0){row=new RemoteViews(c.getPackageName(),R.layout.evolution_row);}
            String key=elements.get(i);RemoteViews item=new RemoteViews(c.getPackageName(),compact?R.layout.evolution_item_compact:R.layout.evolution_item);
            item.setTextColor(R.id.evolution_value,tint);item.setTextColor(R.id.evolution_clock,tint);
            item.setTextViewTextSize(R.id.evolution_value,android.util.TypedValue.COMPLEX_UNIT_SP,compact?20:elements.size()==1?36:elements.size()==2?28:22);
            if(compact)item.setTextViewTextSize(R.id.evolution_clock,android.util.TypedValue.COMPLEX_UNIT_SP,14);
            boolean fresh=s!=null&&AppPreferences.getLastError(c).isEmpty()&&UsageInsights.freshness(s.fetchedAtMillis,now,RefreshScheduler.effectiveRefreshMinutes(c))==UsageInsights.Freshness.FRESH;
            if(key.equals("tier")){
                int tier=TierTheme.tier(c);item.setTextViewText(R.id.evolution_label,c.getString(tier<0?R.string.fun_placing:TierPresentation.NAMES[tier]));
                item.setViewVisibility(R.id.evolution_emblem,View.VISIBLE);item.setViewVisibility(R.id.evolution_value,View.GONE);
                android.graphics.drawable.Drawable d=androidx.appcompat.content.res.AppCompatResources.getDrawable(c,TierTheme.EMBLEMS[Math.max(0,tier)]);Bitmap b=Bitmap.createBitmap(144,144,Bitmap.Config.ARGB_8888);d.setBounds(0,0,144,144);d.setAlpha(tier<0?100:255);d.draw(new Canvas(b));item.setImageViewBitmap(R.id.evolution_emblem,b);
                item.setContentDescription(R.id.evolution_emblem,c.getString(tier<0?R.string.fun_placing:TierPresentation.NAMES[tier]));
            }else{
                boolean isFive=key.startsWith("five");UsageWindow w=isFive?s.fiveHour:s.weekly;
                boolean reset=key.endsWith("reset");long at=w.effectiveResetAtMillis(s.fetchedAtMillis);
                item.setTextViewText(R.id.evolution_label,c.getString(isFive?R.string.matte_five_hour:R.string.ux_weekly)+(reset?" · "+c.getString(R.string.evo_reset):""));
                if(reset&&fresh&&at>now){item.setViewVisibility(R.id.evolution_value,View.GONE);item.setViewVisibility(R.id.evolution_clock,View.VISIBLE);
                    item.setChronometerCountDown(R.id.evolution_clock,true);item.setChronometer(R.id.evolution_clock,SystemClock.elapsedRealtime()+at-now,"%s",true);
                }else item.setTextViewText(R.id.evolution_value,!fresh?c.getString(AppPreferences.getLastError(c).isEmpty()?R.string.evo_stale:R.string.pub_five_error_short):reset?c.getString(at<=0?R.string.evo_unknown_reset:R.string.evo_refresh_needed):Integer.toString(100-w.usedPercent)+"%");
            }
            row.addView(R.id.evolution_columns,item);
            if(i%columns==columns-1||i==elements.size()-1)root.addView(R.id.evolution_rows,row);
        }return root;
    }
    private static void schedule(Context c){
        AppWidgetManager m=AppWidgetManager.getInstance(c);if(m.getAppWidgetIds(new ComponentName(c,EvolutionWidget.class)).length==0)return;
        long now=System.currentTimeMillis(),next=now+15*60000;UsageSnapshot s=AppPreferences.loadSnapshot(c);
        if(s!=null)for(UsageWindow w:new UsageWindow[]{s.fiveHour,s.weekly})if(w!=null){long at=w.effectiveResetAtMillis(s.fetchedAtMillis);if(at>now)next=Math.min(next,at);}
        PendingIntent tick=PendingIntent.getBroadcast(c,0,new Intent(c,EvolutionWidget.class).setAction("dev.bennett.codexmeter.EVOLUTION_TICK"),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        ((AlarmManager)c.getSystemService(Context.ALARM_SERVICE)).setAndAllowWhileIdle(AlarmManager.RTC,next,tick);
    }
}
