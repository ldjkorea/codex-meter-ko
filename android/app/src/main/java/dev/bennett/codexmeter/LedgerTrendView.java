package dev.bennett.codexmeter;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.MotionEvent;
import android.view.View;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

/** Observed daily deltas only. A gap is drawn as a mark, never an interpolated zero bar. */
final class LedgerTrendView extends View {
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final boolean dark;
    private List<LedgerAggregation.Day> days=Collections.emptyList();
    private LocalDate start;
    private float plotLeft,plotRight;
    private LocalDate selected;
    private OnDateSelectedListener listener;
    interface OnDateSelectedListener { void onDateSelected(LocalDate date); }
    public LedgerTrendView(Context context){this(context,Ui.isDark(context));}
    LedgerTrendView(Context context,boolean dark){super(context);this.dark=dark;setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);setClickable(true);setFocusable(true);}
    void setOnDateSelectedListener(OnDateSelectedListener listener){this.listener=listener;}
    void setData(List<LedgerAggregation.Day> days,LocalDate start){
        this.days=days;this.start=start;selected=start.plusDays(Math.max(0,days.size()-1));StringBuilder description=new StringBuilder();
        for(int i=0;i<days.size();i++){LedgerAggregation.Day day=days.get(i);description.append(start.plusDays(i)).append(": ");
            description.append(!LedgerPresentation.measured(day)?getContext().getString(R.string.ui_ledger_comparison):getContext().getString(R.string.next_day_value,"",String.format(java.util.Locale.getDefault(),"%.1f",day.points),getContext().getString(day.uncertain>0?R.string.next_gap:R.string.next_partial))).append(". ");}
        setContentDescription(getContext().getString(R.string.v3_chart_access));invalidate();
    }
    @Override protected void onDraw(Canvas canvas){super.onDraw(canvas);if(days.isEmpty())return;
        float font=11*getResources().getDisplayMetrics().scaledDensity;
        paint.setTextSize(font);paint.setStyle(Paint.Style.FILL);
        float bottom=getHeight()-font*2.3f,top=font*1.6f;
        double maximum=1;for(LedgerAggregation.Day day:days)if(LedgerPresentation.measured(day))maximum=Math.max(maximum,day.points);
        maximum=maximum>5?Math.ceil(maximum/5)*5:Math.ceil(maximum);
        plotLeft=paint.measureText(LedgerUi.number(maximum))+Ui.dp(getContext(),12);plotRight=getWidth()-Ui.dp(getContext(),8);
        if(plotRight<=plotLeft||bottom<=top)return;
        for(int i=0;i<3;i++){
            float y=bottom-(bottom-top)*i/2;
            paint.setColor(Ui.divider(dark));canvas.drawLine(plotLeft,y,plotRight,y,paint);
            paint.setColor(LedgerUi.muted(dark));String value=LedgerUi.number(maximum*i/2);
            canvas.drawText(value,plotLeft-Ui.dp(getContext(),6)-paint.measureText(value),y+font*.3f,paint);
        }
        paint.setColor(LedgerUi.muted(dark));canvas.drawText("%p",plotLeft,top-font*.5f,paint);
        float slot=(plotRight-plotLeft)/days.size();
        for(int i=0;i<days.size();i++){LedgerAggregation.Day day=days.get(i);
            float center=plotLeft+(i+.5f)*slot,width=Math.min(slot*.65f,Ui.dp(getContext(),34));
            if(start.plusDays(i).equals(selected)){paint.setColor(LedgerUi.tint(dark));canvas.drawRect(center-slot/2,top,center+slot/2,bottom,paint);}
            paint.setColor(!LedgerPresentation.measured(day)?LedgerUi.muted(dark):Ui.accent(getContext(),dark));
            if(LedgerPresentation.measured(day)&&selected!=null&&!start.plusDays(i).equals(selected))paint.setAlpha(105);else paint.setAlpha(255);
            if(!LedgerPresentation.measured(day))canvas.drawLine(center-width/2,bottom-Ui.dp(getContext(),3),center+width/2,bottom-Ui.dp(getContext(),3),paint);
            else canvas.drawRoundRect(center-width/2,bottom-Math.max(Ui.dp(getContext(),2),(float)(day.points/maximum)*(bottom-top)),center+width/2,bottom,Ui.dp(getContext(),3),Ui.dp(getContext(),3),paint);
        }
        paint.setAlpha(255);paint.setColor(LedgerUi.muted(dark));
        java.time.format.DateTimeFormatter format=java.time.format.DateTimeFormatter.ofPattern("MM.dd",java.util.Locale.getDefault());
        canvas.drawText(start.format(format),plotLeft,getHeight()-font*.4f,paint);
        if(days.size()>1){String end=start.plusDays(days.size()-1).format(format);canvas.drawText(end,plotRight-paint.measureText(end),getHeight()-font*.4f,paint);}
    }
    @Override public boolean onTouchEvent(MotionEvent event){
        if(event.getAction()==MotionEvent.ACTION_UP&&start!=null&&!days.isEmpty()&&plotRight>plotLeft){
            int index=Math.max(0,Math.min(days.size()-1,(int)((event.getX()-plotLeft)/(plotRight-plotLeft)*days.size())));
            selected=start.plusDays(index);invalidate();performClick();return true;
        }
        return event.getAction()==MotionEvent.ACTION_DOWN||super.onTouchEvent(event);
    }
    @Override public void onInitializeAccessibilityNodeInfo(android.view.accessibility.AccessibilityNodeInfo info){
        super.onInitializeAccessibilityNodeInfo(info);info.setScrollable(true);info.addAction(android.view.accessibility.AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_FORWARD);info.addAction(android.view.accessibility.AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_BACKWARD);
    }
    @Override public boolean performAccessibilityAction(int action,android.os.Bundle args){
        if((action==android.view.accessibility.AccessibilityNodeInfo.ACTION_SCROLL_FORWARD||action==android.view.accessibility.AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD)&&start!=null&&!days.isEmpty()){
            int index=selected==null?0:(int)java.time.temporal.ChronoUnit.DAYS.between(start,selected);index=Math.max(0,Math.min(days.size()-1,index+(action==android.view.accessibility.AccessibilityNodeInfo.ACTION_SCROLL_FORWARD?1:-1)));
            selected=start.plusDays(index);invalidate();performClick();return true;
        }return super.performAccessibilityAction(action,args);
    }
    @Override public boolean performClick(){
        super.performClick();
        if(listener!=null&&start!=null&&!days.isEmpty())listener.onDateSelected(selected==null?start.plusDays(days.size()-1):selected);
        return true;
    }
}
