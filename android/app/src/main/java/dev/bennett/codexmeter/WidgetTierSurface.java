package dev.bennett.codexmeter;

/** RemoteViews-compatible rounded tier surfaces, shared by preview and launcher. */
final class WidgetTierSurface {
    private WidgetTierSurface() {}
    private static final int[][] BACKGROUNDS = {
        {R.drawable.widget_tier_0_56, R.drawable.widget_tier_0_88, R.drawable.widget_tier_0_100},
        {R.drawable.widget_tier_1_56, R.drawable.widget_tier_1_88, R.drawable.widget_tier_1_100},
        {R.drawable.widget_tier_2_56, R.drawable.widget_tier_2_88, R.drawable.widget_tier_2_100},
        {R.drawable.widget_tier_3_56, R.drawable.widget_tier_3_88, R.drawable.widget_tier_3_100},
        {R.drawable.widget_tier_4_56, R.drawable.widget_tier_4_88, R.drawable.widget_tier_4_100},
        {R.drawable.widget_tier_5_56, R.drawable.widget_tier_5_88, R.drawable.widget_tier_5_100},
        {R.drawable.widget_tier_6_56, R.drawable.widget_tier_6_88, R.drawable.widget_tier_6_100},
        {R.drawable.widget_tier_7_56, R.drawable.widget_tier_7_88, R.drawable.widget_tier_7_100},
        {R.drawable.widget_tier_8_56, R.drawable.widget_tier_8_88, R.drawable.widget_tier_8_100},
        {R.drawable.widget_tier_9_56, R.drawable.widget_tier_9_88, R.drawable.widget_tier_9_100}
    };
    static int resource(int tier,int opacity) {
        if(tier<0||tier>=BACKGROUNDS.length||opacity<=0)return R.drawable.widget_bg_transparent;
        int band=WidgetTierPalette.opacity(opacity);
        return BACKGROUNDS[tier][band==56?0:band==88?1:2];
    }
}
