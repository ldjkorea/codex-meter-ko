package dev.bennett.codexmeter;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.RemoteViews;

/** Home widget ornament only. Does not modify quota selections, auth, or tier evidence. */
final class WidgetCrest {
    private static String marker;
    private static Bitmap cached;
    private static int cachedTier = -2;
    static SharedPreferences prefs(Context c) { return c.getSharedPreferences("codex_widget_crests",0); }
    static boolean enabled(Context c,int id) { return prefs(c).getBoolean(id+".enabled",true); }
    static boolean save(Context c,int id,boolean value) { return prefs(c).edit().putBoolean(id+".enabled",value).commit(); }
    static void deleted(Context c,int id) { prefs(c).edit().remove(id+".enabled").apply(); }
    static void restored(Context c,int oldId,int newId) {
        if(oldId==newId)return;
        if(save(c,newId,enabled(c,oldId)))deleted(c,oldId);
    }
    static void bind(Context c,RemoteViews v,int id) { bind(c,v,enabled(c,id)); }
    static synchronized void bind(Context c,RemoteViews v,boolean show) {
        v.setViewVisibility(R.id.widget_tier_emblem,show?View.VISIBLE:View.GONE);
        if(!show)return;
        int tier=TierTheme.tier(c);
        if(cached==null||cachedTier!=tier) {
            Bitmap image=Bitmap.createBitmap(256,256,Bitmap.Config.ARGB_8888);
            Drawable d=androidx.appcompat.content.res.AppCompatResources.getDrawable(c,TierTheme.EMBLEMS[Math.max(0,tier)]);
            d.setBounds(0,0,256,256);d.setAlpha(tier<0?100:255);d.draw(new Canvas(image));
            // RemoteViews can still reference the previous bitmap; do not recycle it here.
            cached=image;cachedTier=tier;
        }
        v.setImageViewBitmap(R.id.widget_tier_emblem,cached);
        v.setContentDescription(R.id.widget_tier_emblem,c.getString(tier<0?R.string.fun_placing:TierPresentation.NAMES[tier]));
    }
    static synchronized void updateExistingIfChanged(Context c) {
        String current=c.getSharedPreferences("codex_tier_evolution_v2",0).getString("revision","")+"|"
                +c.getSharedPreferences("secure_auth_v1",0).getString("blob","")+"|"+LiveUsageStore.revision();
        if(current.equals(marker))return;
        try {
            AppWidgetManager m=AppWidgetManager.getInstance(c);
            for(int id:m.getAppWidgetIds(new ComponentName(c,CodexUsageWidget.class)))WidgetRenderer.update(c,m,id);
            marker=current;
        }catch(RuntimeException e){DiagnosticLog.error(c,"widget","home_crest_update_failed",e);}
    }
}
