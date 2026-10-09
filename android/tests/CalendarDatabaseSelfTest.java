package dev.bennett.codexmeter;

import android.content.Context;
import java.lang.reflect.Field;
import java.time.LocalDate;
import org.json.JSONObject;

/** Production capture -> SQLite -> daily read -> calendar styling, in isolated SQLite fixtures. */
public final class CalendarDatabaseSelfTest {
    private static int checks;
    private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    private static final long HOUR=3600000L;
    private static final long START=LedgerAggregation.day(System.currentTimeMillis()).minusDays(1).atStartOfDay(LedgerAggregation.ZONE).toInstant().toEpochMilli();
    private static final long RESET=(START+7*24*HOUR)/1000;
    private static boolean record(Context c,long at,double used,boolean manual)throws Exception{
        UsageSnapshot snapshot=new UsageSnapshot("pro",true,false,null,new UsageWindow((int)used,604800,0,RESET),at);
        String body=new JSONObject().put("rate_limit",new JSONObject().put("primary_window",new JSONObject().put("used_percent",used).put("limit_window_seconds",604800).put("reset_at",RESET))).toString();
        return UsageLedgerDatabase.record(c,snapshot,body,manual);
    }
    public static void main(String[] args)throws Exception{
        Context context=new Context();LocalDate date=LedgerAggregation.day(START);String policy="weekly|pro|604800";
        check(record(context,START+10*HOUR,13.1,false),"Initial real observation stored");
        check(!LedgerCalendar.emerald(LedgerPresentation.find(UsageLedgerDatabase.load(context).days,policy,date)),"Single persisted reading has no measured daily delta");
        check(record(context,START+11*HOUR,26.1,false),"Second precise observation stored");
        check(!LedgerCalendar.emerald(LedgerPresentation.find(UsageLedgerDatabase.load(context).days,policy,date)),"Exact 13 points survives SQLite without accidental glow");
        check(record(context,START+12*HOUR,26.2,false),"Additional measured use stored");
        LedgerAggregation.Day day=LedgerPresentation.find(UsageLedgerDatabase.load(context).days,policy,date);
        check(LedgerCalendar.emerald(day)&&Math.abs(day.points-13.1)<1e-8,"Actual SQLite daily rollup lights the calendar");
        check(record(context,START+12*HOUR,26.2,true),"Manual save of same observation accepted");
        UsageLedgerDatabase.Data data=UsageLedgerDatabase.load(context);
        check(data.records.size()==3&&LedgerPresentation.find(data.days,policy,date).count==3,"Manual/automatic dedup leaves daily consumption unchanged");
        Field field=UsageLedgerDatabase.class.getDeclaredField("instance");field.setAccessible(true);
        ((UsageLedgerDatabase)field.get(null)).close();field.set(null,null);
        data=UsageLedgerDatabase.load(context);
        day=LedgerPresentation.find(data.days,policy,date);
        check(LedgerCalendar.emerald(day)&&day.count==3,"Database reopen preserves actual calendar records and highlight");
        check(LedgerPresentation.find(data.days,policy,date.minusDays(1))==null,"No synthetic record created for empty date");
        check(!LedgerCalendar.emerald(LedgerPresentation.find(data.days,"monthly|pro|604800",date)),"Other quota cannot borrow weekly cell data");
        System.out.println("Production SQLite-to-calendar integration: "+checks+" assertions passed (not Android runtime).");
    }
}
