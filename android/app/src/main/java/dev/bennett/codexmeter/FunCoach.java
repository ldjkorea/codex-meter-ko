package dev.bennett.codexmeter;

/** Local phrase catalog. No model/API call or resource reflection. */
final class FunCoach {
    private FunCoach(){}
    private static final int[][][] TIERS={
        {{R.string.fun_coach_calm_0_0,R.string.fun_coach_calm_0_1},{R.string.fun_coach_calm_1_0,R.string.fun_coach_calm_1_1},{R.string.fun_coach_calm_2_0,R.string.fun_coach_calm_2_1},{R.string.fun_coach_calm_3_0,R.string.fun_coach_calm_3_1},{R.string.fun_coach_calm_4_0,R.string.fun_coach_calm_4_1},{R.string.fun_coach_calm_5_0,R.string.fun_coach_calm_5_1},{R.string.fun_coach_calm_6_0,R.string.fun_coach_calm_6_1},{R.string.fun_coach_calm_7_0,R.string.fun_coach_calm_7_1},{R.string.fun_coach_calm_8_0,R.string.fun_coach_calm_8_1},{R.string.fun_coach_calm_9_0,R.string.fun_coach_calm_9_1}},
        {{R.string.fun_coach_playful_0_0,R.string.fun_coach_playful_0_1},{R.string.fun_coach_playful_1_0,R.string.fun_coach_playful_1_1},{R.string.fun_coach_playful_2_0,R.string.fun_coach_playful_2_1},{R.string.fun_coach_playful_3_0,R.string.fun_coach_playful_3_1},{R.string.fun_coach_playful_4_0,R.string.fun_coach_playful_4_1},{R.string.fun_coach_playful_5_0,R.string.fun_coach_playful_5_1},{R.string.fun_coach_playful_6_0,R.string.fun_coach_playful_6_1},{R.string.fun_coach_playful_7_0,R.string.fun_coach_playful_7_1},{R.string.fun_coach_playful_8_0,R.string.fun_coach_playful_8_1},{R.string.fun_coach_playful_9_0,R.string.fun_coach_playful_9_1}},
        {{R.string.fun_coach_spicy_0_0,R.string.fun_coach_spicy_0_1},{R.string.fun_coach_spicy_1_0,R.string.fun_coach_spicy_1_1},{R.string.fun_coach_spicy_2_0,R.string.fun_coach_spicy_2_1},{R.string.fun_coach_spicy_3_0,R.string.fun_coach_spicy_3_1},{R.string.fun_coach_spicy_4_0,R.string.fun_coach_spicy_4_1},{R.string.fun_coach_spicy_5_0,R.string.fun_coach_spicy_5_1},{R.string.fun_coach_spicy_6_0,R.string.fun_coach_spicy_6_1},{R.string.fun_coach_spicy_7_0,R.string.fun_coach_spicy_7_1},{R.string.fun_coach_spicy_8_0,R.string.fun_coach_spicy_8_1},{R.string.fun_coach_spicy_9_0,R.string.fun_coach_spicy_9_1}},
    };
    private static final int[][][] STATES={
        {{R.string.fun_coach_calm_placing_0,R.string.fun_coach_calm_placing_1},{R.string.fun_coach_calm_provisional_0,R.string.fun_coach_calm_provisional_1},{R.string.fun_coach_calm_rest_0,R.string.fun_coach_calm_rest_1},{R.string.fun_coach_calm_near_reset_0,R.string.fun_coach_calm_near_reset_1},{R.string.fun_coach_calm_rapid_0,R.string.fun_coach_calm_rapid_1},{R.string.fun_coach_calm_up_0,R.string.fun_coach_calm_up_1},{R.string.fun_coach_calm_promoted_0,R.string.fun_coach_calm_promoted_1}},
        {{R.string.fun_coach_playful_placing_0,R.string.fun_coach_playful_placing_1},{R.string.fun_coach_playful_provisional_0,R.string.fun_coach_playful_provisional_1},{R.string.fun_coach_playful_rest_0,R.string.fun_coach_playful_rest_1},{R.string.fun_coach_playful_near_reset_0,R.string.fun_coach_playful_near_reset_1},{R.string.fun_coach_playful_rapid_0,R.string.fun_coach_playful_rapid_1},{R.string.fun_coach_playful_up_0,R.string.fun_coach_playful_up_1},{R.string.fun_coach_playful_promoted_0,R.string.fun_coach_playful_promoted_1}},
        {{R.string.fun_coach_spicy_placing_0,R.string.fun_coach_spicy_placing_1},{R.string.fun_coach_spicy_provisional_0,R.string.fun_coach_spicy_provisional_1},{R.string.fun_coach_spicy_rest_0,R.string.fun_coach_spicy_rest_1},{R.string.fun_coach_spicy_near_reset_0,R.string.fun_coach_spicy_near_reset_1},{R.string.fun_coach_spicy_rapid_0,R.string.fun_coach_spicy_rapid_1},{R.string.fun_coach_spicy_up_0,R.string.fun_coach_spicy_up_1},{R.string.fun_coach_spicy_promoted_0,R.string.fun_coach_spicy_promoted_1}},
    };
    private static final int[][][] MOMENTS={
        {{R.string.ux_coach_calm_reset_0,R.string.ux_coach_calm_reset_1},{R.string.ux_coach_calm_near_limit_0,R.string.ux_coach_calm_near_limit_1},{R.string.ux_coach_calm_low_0,R.string.ux_coach_calm_low_1},{R.string.ux_coach_calm_normal_0,R.string.ux_coach_calm_normal_1},{R.string.ux_coach_calm_high_0,R.string.ux_coach_calm_high_1},{R.string.ux_coach_calm_best_0,R.string.ux_coach_calm_best_1},{R.string.ux_coach_calm_increased_0,R.string.ux_coach_calm_increased_1},{R.string.ux_coach_calm_decreased_0,R.string.ux_coach_calm_decreased_1}},
        {{R.string.ux_coach_playful_reset_0,R.string.ux_coach_playful_reset_1},{R.string.ux_coach_playful_near_limit_0,R.string.ux_coach_playful_near_limit_1},{R.string.ux_coach_playful_low_0,R.string.ux_coach_playful_low_1},{R.string.ux_coach_playful_normal_0,R.string.ux_coach_playful_normal_1},{R.string.ux_coach_playful_high_0,R.string.ux_coach_playful_high_1},{R.string.ux_coach_playful_best_0,R.string.ux_coach_playful_best_1},{R.string.ux_coach_playful_increased_0,R.string.ux_coach_playful_increased_1},{R.string.ux_coach_playful_decreased_0,R.string.ux_coach_playful_decreased_1}},
        {{R.string.ux_coach_spicy_reset_0,R.string.ux_coach_spicy_reset_1},{R.string.ux_coach_spicy_near_limit_0,R.string.ux_coach_spicy_near_limit_1},{R.string.ux_coach_spicy_low_0,R.string.ux_coach_spicy_low_1},{R.string.ux_coach_spicy_normal_0,R.string.ux_coach_spicy_normal_1},{R.string.ux_coach_spicy_high_0,R.string.ux_coach_spicy_high_1},{R.string.ux_coach_spicy_best_0,R.string.ux_coach_spicy_best_1},{R.string.ux_coach_spicy_increased_0,R.string.ux_coach_spicy_increased_1},{R.string.ux_coach_spicy_decreased_0,R.string.ux_coach_spicy_decreased_1}},
    };
    static String current(android.content.Context context,String tone,int tier,FunInsights.Situation situation,CoachMoment.Kind kind,String window,long now){
        if("off".equals(tone))return "";
        if(kind==CoachMoment.Kind.NONE)return text(context,tone,tier,FunInsights.Situation.PLACING,FunInsights.variant(window,tier,FunInsights.Situation.PLACING,now));
        if(situation==FunInsights.Situation.PROMOTED||situation==FunInsights.Situation.RAPID||situation==FunInsights.Situation.NEAR_RESET)
            return text(context,tone,tier,situation,FunInsights.variant(window,tier,situation,now));
        int t="calm".equals(tone)?0:"spicy".equals(tone)?2:1;
        return context.getString(MOMENTS[t][kind.ordinal()-1][CoachMoment.variant(window,kind,now)]);
    }
    static String text(android.content.Context context,String tone,int tier,FunInsights.Situation situation,int variant){
        if("off".equals(tone))return "";
        int t="calm".equals(tone)?0:"spicy".equals(tone)?2:1;
        int v=Math.floorMod(variant,2);
        if(situation==FunInsights.Situation.TIER&&tier>=0)return context.getString(TIERS[t][Math.min(9,tier)][v]);
        int s;switch(situation){case PROVISIONAL:s=1;break;case REST:s=2;break;case NEAR_RESET:s=3;break;case RAPID:s=4;break;case UP:s=5;break;case PROMOTED:s=6;break;default:s=0;}
        return context.getString(STATES[t][s][v]);
    }
}
