package dev.bennett.codexmeter;

/** Original local plan illustrations, unrelated to a user's rank. No account identity in artwork. */
final class PlanArtwork {
    private PlanArtwork() {}
    static int image(String plan){
        switch(LedgerRecord.cleanPlan(plan)){
            case "plus":return R.drawable.plan_plus;
            case "pro":return R.drawable.plan_pro;
            case "team":case "business":return R.drawable.plan_business;
            case "enterprise":case "edu":return R.drawable.plan_enterprise;
            default:return R.drawable.plan_free;
        }
    }
}
