package dev.bennett.codexmeter;

import android.content.Context;
import java.time.Instant;
import java.util.Locale;
import java.util.TimeZone;

/** Server fixtures and deterministic clocks, not a logged-in device test. */
public final class FiveHourResetSelfTest {
    private static int checks;
    private static void check(boolean value) { checks++; if (!value) throw new AssertionError("5-hour check " + checks); }
    private static UsageSnapshot parse(String plan, String fields, long observed) throws Exception {
        return UsageParser.parse("{\"plan_type\":\""+plan+"\",\"rate_limit\":{\"primary_window\":{\"used_percent\":40,\"limit_window_seconds\":18000"+fields+"},\"secondary_window\":{\"used_percent\":15,\"limit_window_seconds\":604800,\"reset_after_seconds\":500000}}}",observed);
    }
    public static void main(String[] args) throws Exception {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"));
        long observed=Instant.parse("2026-10-08T14:59:00Z").toEpochMilli(); // 23:59 KST
        for (String plan:new String[]{"plus","pro"}) {
            UsageSnapshot snapshot=parse(plan,",\"reset_after_seconds\":9240",observed);
            UsageWindow window=snapshot.fiveHour;
            check(window!=null && snapshot.weekly!=null);
            check(FiveHourResetState.at(window,observed,observed).remainingMillis==9240000);
            check(FiveHourResetState.at(window,observed,observed+60000).remainingMillis==9180000);
            check(FiveHourResetState.at(window,observed,observed+9240000).kind==FiveHourResetState.Kind.EXPIRED);
            Context ko=new Context(Locale.KOREAN);
            check(FiveHourResetDisplay.text(ko,window,observed,observed,"relative",false).equals("5시간 한도 초기화까지 2시간 34분"));
            check(FiveHourResetDisplay.text(ko,window,observed,observed+60000,"relative",false).contains("2시간 33분"));
            check(FiveHourResetDisplay.text(ko,window,observed,observed+20*60000,"relative",false).contains("마지막 관측 기준"));
            check(FiveHourResetDisplay.text(ko,window,observed,observed+9240000,"relative",false).contains("시각 경과"));
            check(FiveHourResetDisplay.text(ko,window,observed,observed,"hidden",false).isEmpty());
            check(FiveHourResetDisplay.text(ko,window,observed,observed,"absolute",true).contains("내일"));
            check(FiveHourResetDisplay.text(new Context(Locale.ENGLISH),window,observed,observed,"relative",false).contains("2h 34m"));
            AppPreferences.error="fixture";
            check(FiveHourResetDisplay.text(ko,window,observed,observed,"relative",false).contains("조회 실패"));
            AppPreferences.error="";
            check(FiveHourResetDisplay.text(ko,window,observed,observed,"relative",false).indexOf("조회 실패")<0);
            UsageWindow missing=parse(plan,"",observed).fiveHour;
            check(FiveHourResetState.at(missing,observed,observed).kind==FiveHourResetState.Kind.NO_RESET);
            check(FiveHourResetDisplay.text(ko,missing,observed,observed,"relative",false).contains("시각 미제공"));
            check(FiveHourResetDisplay.text(ko,null,observed,observed,"relative",false).equals("5시간 사용량 데이터 미제공"));
            UsageWindow absolute=parse(plan,",\"reset_at\":"+(observed/1000+1080)+",\"reset_after_seconds\":9999",observed).fiveHour;
            check(FiveHourResetState.at(absolute,observed,observed).remainingMillis==1080000);
            check(FiveHourResetDisplay.text(ko,absolute,observed,observed,"relative",false).endsWith("18분"));
            check(FiveHourResetState.at(absolute,observed,observed+300000).remainingMillis==780000); // delayed delivery
            check(FiveHourResetState.at(new UsageWindow(0,18000,600,0),0,observed).kind==FiveHourResetState.Kind.NO_RESET);
            check(FiveHourResetState.at(new UsageWindow(0,18000,Long.MAX_VALUE,0),observed,observed).kind==FiveHourResetState.Kind.NO_RESET);
            UsageWindow updated=parse(plan,",\"reset_after_seconds\":18000",observed+60000).fiveHour;
            check(FiveHourResetState.at(updated,observed+60000,observed+60000).remainingMillis==18000000);
        }
        System.out.println("Plus/Pro 5-hour reset parser, localization and clock fixtures: "+checks+" assertions passed (not Android device proof).");
    }
}
