package dev.bennett.codexmeter;

/** One local spicy voice. Stable within a four-hour block; no inference calls or personal text. */
public final class SpicyAi {
    private SpicyAi() {}
    public static String key(double daily,double weekly,boolean fresh,boolean bonus,long remaining,long now){
        String state;
        if(!fresh||!Double.isFinite(weekly))state="missing";
        else if(bonus)state="bonus";
        else if(weekly>=95)state="empty";
        else if(Double.isFinite(daily)&&daily>=13)state="sprint";
        else if(Double.isFinite(daily)&&daily>=5)state="active";
        else if(weekly<10&&remaining>0&&remaining<=86400000L)state="idle";
        else if(Double.isFinite(daily)&&daily==0)state="zero";
        else if(!Double.isFinite(daily))state="partial";
        else state="steady";
        return "live_ai_"+state+"_"+Math.floorMod(now/14400000L,3);
    }
}
