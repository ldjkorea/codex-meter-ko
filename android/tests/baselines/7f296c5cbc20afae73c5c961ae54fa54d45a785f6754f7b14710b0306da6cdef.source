package dev.bennett.codexmeter;

import android.content.Context;
import java.util.List;

/** One policy for phone quotas and widget previews/renders. Collection and stored data stay intact. */
final class MeterVisibility {
    private MeterVisibility(){}
    static List<String> resolve(Context context,String csv,String mode,UsageSnapshot snapshot){
        return WidgetVisibility.resolve(csv,mode,snapshot,AppPreferences.showDashboardFiveHour(context),
                AppPreferences.showDashboardWeekly(context),AppPreferences.showDashboardMonthly(context),AppPreferences.showDashboardResetCredits(context));
    }
    static boolean allowed(Context context,String key,UsageSnapshot snapshot){
        return WidgetVisibility.catalog(snapshot,AppPreferences.showDashboardFiveHour(context),
                AppPreferences.showDashboardWeekly(context),AppPreferences.showDashboardMonthly(context),AppPreferences.showDashboardResetCredits(context)).contains(key);
    }
    static UsageWindow five(Context context,UsageSnapshot snapshot){return snapshot!=null&&AppPreferences.showDashboardFiveHour(context)?snapshot.fiveHour:null;}
    static UsageWindow longWindow(Context context,UsageSnapshot snapshot){return snapshot!=null&&
            (snapshot.weekly!=null?AppPreferences.showDashboardWeekly(context):AppPreferences.showDashboardMonthly(context))?snapshot.longWindow():null;}
    static void changed(Context context){WidgetRenderer.updateAll(context);NowBarManager.applyPercentModeChange(context);dev.bennett.codexmeter.wear.PhoneWearSync.pushAll(context);}
}
