package dev.bennett.codexmeter;

import java.time.Instant;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

/** Versioned aggregate/observation export. Callers supply only allowlisted event metadata. */
public final class LedgerExport {
    private LedgerExport() {}
    public static String json(List<LedgerRecord> records,List<LedgerAggregation.Day> days,
            List<LedgerAggregation.Interval> uncertain,List<JSONObject> events) throws Exception {
        JSONArray observations=new JSONArray(),daily=new JSONArray(),intervals=new JSONArray(),eventArray=new JSONArray();
        for(LedgerRecord row:records)observations.put(row.toJson());
        for(LedgerAggregation.Day day:days)daily.put(new JSONObject().put("policy",day.policy).put("day_seoul",day.date)
                .put("observed_consumption_pp",day.points).put("observation_count",day.count)
                .put("quality",day.quality()).put("uncertain_intervals",day.uncertain).put("legacy",day.legacy)
                .put("first_observed_utc",utc(day.first)).put("last_observed_utc",utc(day.last))
                .put("covered_millis",day.coveredMillis).put("boundary_count",day.boundaries));
        for(LedgerAggregation.Interval interval:uncertain)intervals.put(new JSONObject().put("policy",interval.policy)
                .put("start_utc",utc(interval.start)).put("end_utc",utc(interval.end))
                .put("unallocated_change_pp",interval.points).put("reason",interval.reason));
        for(JSONObject event:events)eventArray.put(new JSONObject().put("type",event.getString("type"))
                .put("at_utc",event.getString("at_utc")).put("meter",event.getString("meter"))
                .put("origin",event.getString("origin")));
        return new JSONObject().put("schema_version",1).put("day_timezone","Asia/Seoul")
                .put("unit","quota_percentage_points_not_tokens").put("observations",observations)
                .put("daily",daily).put("uncertain_intervals",intervals).put("events",eventArray).toString(2);
    }
    public static String csv(List<LedgerRecord> records,List<LedgerAggregation.Day> days,
            List<LedgerAggregation.Interval> uncertain,List<JSONObject> events) throws Exception {
        StringBuilder out=new StringBuilder("record_type,policy,meter,plan,observed_at_utc,day_seoul,used_percent,remaining_percent,observed_consumption_pp,unallocated_change_pp,start_utc,end_utc,quality,source,manual,used_percent_decimal,event_type\r\n");
        for(LedgerRecord row:records)line(out,"observation",row.policy(),row.meter,row.plan,utc(row.at),"",row.used,row.remaining(),"","","","",
                "legacy".equals(row.source)?"unknown":"fresh_at_capture",row.source,row.manual,row.decimal,"");
        for(LedgerAggregation.Day day:days)line(out,"daily",day.policy,"","","",day.date,"","",day.points,"","","",day.quality(),day.legacy?"legacy_or_mixed":"api","","","");
        for(LedgerAggregation.Interval interval:uncertain)line(out,"uncertain",interval.policy,"","","","","","","",interval.points,utc(interval.start),utc(interval.end),interval.reason,"","","","");
        for(JSONObject event:events)line(out,"event","",event.getString("meter"),"",event.getString("at_utc"),"","","","","","","",event.getString("origin"),"","","",event.getString("type"));
        return out.toString();
    }
    private static void line(StringBuilder out,Object... values){
        for(int i=0;i<values.length;i++){
            if(i>0)out.append(',');String value=String.valueOf(values[i]);
            // Safe even if a future label introduces CSV spreadsheet formula syntax.
            if(value.startsWith("=")||value.startsWith("+")||value.startsWith("-")||value.startsWith("@"))value="'"+value;
            out.append('"').append(value.replace("\"","\"\"")).append('"');
        }
        out.append("\r\n");
    }
    public static String utc(long at){return Instant.ofEpochMilli(at).toString();}
}
