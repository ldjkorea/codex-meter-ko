package dev.bennett.codexmeter.wear;

import dev.bennett.codexmeter.UsageSnapshot;
import org.json.JSONException;
import org.json.JSONObject;

public final class WearUsageState {
    public final boolean signedIn;
    public final String sourceNode;
    public final UsageSnapshot snapshot;
    public final long updatedAtMillis;
    public final boolean showFiveHour;

    public WearUsageState(UsageSnapshot snapshot, long updatedAtMillis, String sourceNode) {
        this(snapshot, updatedAtMillis, sourceNode, snapshot != null);
    }

    public WearUsageState(UsageSnapshot snapshot, long updatedAtMillis, String sourceNode,
            boolean signedIn) {
        this(snapshot,updatedAtMillis,sourceNode,signedIn,true);
    }
    public WearUsageState(UsageSnapshot snapshot,long updatedAtMillis,String sourceNode,
            boolean signedIn,boolean showFiveHour){
        this.snapshot = snapshot;
        this.updatedAtMillis = Math.max(0L, updatedAtMillis);
        this.sourceNode = WearSettingsState.SOURCE_WEAR.equals(sourceNode)
                ? WearSettingsState.SOURCE_WEAR : WearSettingsState.SOURCE_PHONE;
        this.signedIn = signedIn;
        this.showFiveHour=showFiveHour;
    }

    /** View copy only. The raw snapshot is serialized/stored intact. */
    public UsageSnapshot displaySnapshot(){
        if(snapshot==null||showFiveHour)return snapshot;
        return new UsageSnapshot(snapshot.planType,snapshot.allowed,snapshot.limitReached,null,
                snapshot.weekly,snapshot.monthly,snapshot.additionalLimits,snapshot.usageCredits,
                snapshot.resetCreditsAvailable,snapshot.fetchedAtMillis);
    }

    public JSONObject toJson() throws JSONException {
        JSONObject json = new JSONObject();
        if (snapshot != null) {
            json.put("usage", snapshot.toJson());
        }
        json.put("signed_in", signedIn);
        json.put("show_five_hour",showFiveHour);
        json.put("updated_at_millis", updatedAtMillis);
        json.put("source_node", sourceNode);
        return json;
    }

    public static WearUsageState fromJson(JSONObject json) {
        if (json == null) return null;
        UsageSnapshot snapshot = UsageSnapshot.fromJson(json.optJSONObject("usage"));
        return new WearUsageState(
                snapshot,
                json.optLong("updated_at_millis", 0L),
                json.optString("source_node", WearSettingsState.SOURCE_PHONE),
                json.has("signed_in") ? json.optBoolean("signed_in", false) : snapshot != null,
                json.optBoolean("show_five_hour",true));
    }
}
