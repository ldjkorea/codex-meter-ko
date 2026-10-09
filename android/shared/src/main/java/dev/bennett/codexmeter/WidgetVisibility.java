package dev.bennett.codexmeter;

import java.util.ArrayList;
import java.util.List;

/** Non-destructive display filter. Saved choices survive disabling/re-enabling a meter. */
public final class WidgetVisibility {
    public static final String NONE="none";
    private WidgetVisibility(){}
    public static List<String> catalog(UsageSnapshot snapshot,boolean five,boolean weekly,boolean monthly,boolean credits){
        List<String> keys=new ArrayList<>();
        if(weekly&&(snapshot==null||snapshot.weekly!=null)||monthly&&snapshot!=null&&snapshot.weekly==null&&snapshot.monthly!=null)keys.add(WidgetMeters.WEEKLY);
        if(five&&snapshot!=null&&snapshot.fiveHour!=null)keys.add(WidgetMeters.FIVE_HOUR);
        if(!keys.isEmpty())keys.add(WidgetMeters.NEXT_RESET);
        if(credits)keys.add(WidgetMeters.RESET_CREDITS);return keys;
    }
    public static List<String> resolve(String saved,String mode,UsageSnapshot snapshot,boolean five,boolean weekly,boolean monthly,boolean credits){
        List<String> available=catalog(snapshot,five,weekly,monthly,credits);
        if(NONE.equals(saved))return new ArrayList<>();
        String effective=WidgetMeters.effectiveVisibleCsv(saved,mode);
        List<String> selected=WidgetMeters.resolveVisible(effective,available);
        // A temporarily disabled or unsupported pinned quota may fall back to an allowed quota.
        // Never alter the saved list and never restore an explicitly empty widget.
        if(selected.isEmpty())for(String key:available)if(WidgetMeters.WEEKLY.equals(key)||WidgetMeters.FIVE_HOUR.equals(key)){selected.add(key);break;}
        return selected;
    }
}
