package dev.bennett.codexmeter;

/** Original local plan illustrations, unrelated to a user's rank. No account identity in artwork. */
final class PlanArtwork {
    private PlanArtwork() {}
    static String key(String plan){
        String key=plan==null?"":plan.trim().toLowerCase(java.util.Locale.ROOT).replaceAll("[ _-]", "");
        return key.equals("pro5x")?"prolite":key.equals("pro20x")?"pro":key;
    }
    static String label(String plan){
        switch(key(plan)){
            case "free":return "Free";
            case "go":return "Go";
            case "plus":return "Plus";
            case "prolite":return "Pro Lite";
            case "pro":return "Pro";
            default:return plan==null?"—":plan;
        }
    }
    static int image(String plan){
        switch(key(plan)){
            case "free":return R.drawable.subscription_free;
            case "go":return R.drawable.subscription_go;
            case "plus":return R.drawable.subscription_plus;
            case "prolite":return R.drawable.subscription_prolite;
            case "pro":return R.drawable.subscription_pro;
            default:return 0;
        }
    }
}
