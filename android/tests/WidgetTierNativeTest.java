package dev.bennett.codexmeter;

import android.app.Instrumentation;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.RemoteViews;
import java.io.File;
import java.io.FileOutputStream;

/** Optional isolated emulator harness: synthetic display values, no accounts/API/ledger writes. */
public final class WidgetTierNativeTest extends Instrumentation {
    private int checks;
    private void check(boolean value,String label) {
        checks++;
        if(!value)throw new AssertionError(label);
    }
    @Override public void onCreate(Bundle args) { super.onCreate(args); start(); }
    @Override public void onStart() {
        Bundle result=new Bundle();
        try {
            runOnMainSync(()-> {
                try { verify(); }
                catch(Exception e) { throw new RuntimeException(e); }
            });
            result.putString("stream","Native RemoteViews fixture checks: "+checks+" passed. Synthetic values; not Samsung launcher/live-account verification.\n");
            finish(-1,result);
        } catch(Throwable error) {
            result.putString("stream",android.util.Log.getStackTraceString(error));
            finish(0,result);
        }
    }
    private void verify() throws Exception {
        Context c=getTargetContext();
        int[] layouts={R.layout.widget_rings,R.layout.widget_rings_four,R.layout.widget_dials,
                R.layout.widget_dials_large,R.layout.widget_dials_max,R.layout.widget_rings_large,R.layout.widget_rings_max};
        for(int layout:layouts) {
            RemoteViews v=new RemoteViews(c.getPackageName(),layout);
            v.setViewVisibility(R.id.widget_tier_emblem,View.VISIBLE);
            View root=inflate(c,v,250,156);
            View first=root.findViewById(R.id.primary_section),center=root.findViewById(R.id.widget_tier_emblem),last=root.findViewById(R.id.secondary_section);
            check(first.getRight()<=center.getLeft(),"Center does not overlap primary");
            check(center.getRight()<=last.getLeft(),"Center does not overlap secondary");
            check(center.getWidth()>20&&center.getHeight()>20,"Center exceeds former corner crest");
            v.setViewVisibility(R.id.widget_tier_emblem,View.GONE);
            check(inflate(c,v,250,156).findViewById(R.id.widget_tier_emblem).getVisibility()==View.GONE,"OFF removes center");
        }
        for(int tier=0;tier<10;tier++) {
            for(int opacity:new int[]{56,88,100}) {
                Object drawable=Class.forName("androidx.appcompat.content.res.AppCompatResources")
                        .getMethod("getDrawable",Context.class,int.class)
                        .invoke(null,c,WidgetTierSurface.resource(tier,opacity));
                check(drawable!=null,"Production compat factory loads every tier/opacity surface");
            }
            for(int width:new int[]{110,250}) {
                // Exercise actual packaged rounded surfaces, crest art and scalable dial painter.
                RemoteViews v=new RemoteViews(c.getPackageName(),R.layout.widget_rings_four);
                v.setInt(android.R.id.background,"setBackgroundResource",WidgetTierSurface.resource(tier,100));
                v.setViewVisibility(R.id.widget_tier_emblem,View.VISIBLE);
                v.setImageViewResource(R.id.widget_tier_emblem,TierTheme.EMBLEMS[tier]);
                v.setViewVisibility(R.id.reset_time_four_graphic,View.GONE);
                v.setViewVisibility(R.id.reset_count_four_graphic,View.GONE);
                v.setImageViewBitmap(R.id.primary_four_graphic,WidgetGraphics.compactDial(c,35,R.drawable.ic_oui_calendar_week,WidgetTierPalette.accent(tier),WidgetGraphics.trackColor(true),WidgetGraphics.mainTextColor(true),"35%",1));
                v.setImageViewBitmap(R.id.secondary_four_graphic,WidgetGraphics.compactDial(c,65,R.drawable.ic_oui_alarm,WidgetTierPalette.accent(tier),WidgetGraphics.trackColor(true),WidgetGraphics.mainTextColor(true),"4일 13시간 후",1));
                View root=inflate(c,v,width,70);
                check(root.getBackground()!=null,"Packaged tier background inflates");
                View crest=root.findViewById(R.id.widget_tier_emblem);
                check(crest.getWidth()>20&&crest.getWidth()<root.getWidth(),"Narrow crest stays inside root");
                save(root,"tier-"+tier+"-"+width+".png");
            }
        }
        // Integrated production entry point, with a fresh signed-out target app.
        Bundle size=new Bundle();size.putInt("appWidgetMinWidth",110);size.putInt("appWidgetMinHeight",70);
        WidgetOptions options=WidgetOptions.defaults();
        for(int width:new int[]{110,250}) {
            size.putInt("appWidgetMinWidth",width);
            View draftOn=inflate(c,WidgetRenderer.buildPreview(c,777,options,size,true),width,70);
            check(draftOn.findViewById(R.id.widget_tier_emblem).getVisibility()==View.VISIBLE,"Integrated draft ON");
            View draftOff=inflate(c,WidgetRenderer.buildPreview(c,777,options,size,false),width,70);
            check(draftOff.findViewById(R.id.widget_tier_emblem).getVisibility()==View.GONE,"Integrated draft OFF");
            if(width==110)save(draftOn,"signed-out-integrated.png");
        }
    }
    private View inflate(Context c,RemoteViews remote,int width,int height) {
        View root=remote.apply(c,new FrameLayout(c));
        // Measure the actual layout in emulator density; the supplied sizes remain dp.
        float density=c.getResources().getDisplayMetrics().density;
        int w=Math.round(width*density),h=Math.round(height*density);
        root.measure(View.MeasureSpec.makeMeasureSpec(w,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(h,View.MeasureSpec.EXACTLY));
        root.layout(0,0,w,h);
        return root;
    }
    private void save(View root,String name) throws Exception {
        Bitmap bitmap=Bitmap.createBitmap(root.getWidth(),root.getHeight(),Bitmap.Config.ARGB_8888);
        root.draw(new Canvas(bitmap));
        File dir=new File(getTargetContext().getExternalFilesDir(null),"widget-tier-fixtures");
        if(!dir.isDirectory()&&!dir.mkdirs())throw new java.io.IOException("Fixture folder unavailable");
        try(FileOutputStream stream=new FileOutputStream(new File(dir,name))) {
            if(!bitmap.compress(Bitmap.CompressFormat.PNG,100,stream))throw new java.io.IOException("Capture failed");
        }
        bitmap.recycle();
    }
}
