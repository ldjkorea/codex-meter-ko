package dev.bennett.codexmeter;

import android.content.Context;
import java.util.Locale;
import java.util.Objects;

/** JVM resource fixtures; no claim of real Android layout/host coverage. */
public final class LocalizationSelfTest {
    private static int checks;

    private static void equal(String expected, String actual) {
        checks++;
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError("Expected <" + expected + "> but got <" + actual + ">");
        }
    }

    public static void main(String[] args) {
        Context en = new Context(Locale.ENGLISH);
        Context ko = new Context(Locale.KOREAN);
        long now = 1_800_000_000_000L;
        Locale.setDefault(Locale.ENGLISH);
        for (long offset : new long[] {-1000, 0, 1, 59_999, 60_000, 3_600_000, 8_100_000, 86_400_000, 604_800_000}) {
            equal(OriginalUsageFormat.relative(now + offset, now), UsageFormat.relative(en, now + offset, now));
            equal(OriginalUsageFormat.compactDuration(offset), UsageFormat.compactDuration(en, offset));
            equal(OriginalUsageFormat.updated(now - offset, now), UsageFormat.updated(en, now - offset, now));
            for (boolean hour24 : new boolean[] {false, true}) {
                en.hour24 = hour24;
                equal(OriginalUsageFormat.absolute(en, now + offset, now), UsageFormat.absolute(en, now + offset, now));
            }
            for (int used : new int[] {0, 1, 50, 99, 100}) {
                UsageWindow window = new UsageWindow(used, 18_000L, 0L, (now + offset) / 1000L);
                for (String mode : new String[] {"relative", "absolute", "both", "hidden"}) {
                    equal(OriginalUsageFormat.reset(en, window, mode, now, now), UsageFormat.reset(en, window, mode, now, now));
                }
                for (String mode : new String[] {"remaining", "used"}) {
                    for (boolean compact : new boolean[] {false, true}) {
                        equal(OriginalUsageFormat.percent(window, mode, compact), UsageFormat.percent(en, window, mode, compact));
                    }
                }
                equal(NowBarCopy.limitText("Weekly", window, now, now), NowBarText.limitText(en, "Weekly", window, now, now));
                equal(NowBarCopy.chipExpandedText("Weekly", window, now, now), NowBarText.chipExpandedText(en, "Weekly", window, now, now));
                equal(NowBarCopy.focusCriticalText("W ", window, now, now), NowBarText.focusCriticalText(en, "W ", window, now, now));
                equal(NowBarCopy.wearLimitText("Week", window, now, now), NowBarText.wearLimitText(en, "Week", window, now, now));
            }
        }
        equal("Unavailable", UsageFormat.percent(en, null, "remaining", false));
        equal("확인 불가", UsageFormat.percent(ko, null, "remaining", false));
        equal("2시간 15분 후", UsageFormat.relative(ko, now + 8_100_000L, now));
        UsageWindow exhausted = new UsageWindow(100, 18_000L, 8100L, 0L);
        equal("2시간 15분 후 초기화", UsageFormat.reset(ko, exhausted, "relative", now, now));
        equal("주간: 2시간 15분 후 초기화", NowBarText.limitText(ko, "주간", exhausted, now, now));
        equal("5분 전 업데이트", UsageFormat.updated(ko, now - 300_000L, now));
        equal("3일 0시간", UsageFormat.compactDuration(ko, 259_200_000L));
        equal("Codex · 5시간", DisplayLabels.meter(ko, WidgetMeters.FIVE_HOUR, null, false));
        equal("차트 읽는 방법", DisplayLabels.history(ko, HistorySections.GUIDE));
        for (String text : new String[] {"1 reset credit", "2 reset credits", "Plus plan", "5h 50% · Week 30%", "Resets in 2d 4h", "12m", "Live monitor active"}) {
            equal(text, WearDisplayText.localize(en, text));
        }
        equal("초기화 크레딧 1개", WearDisplayText.localize(ko, "1 reset credit"));
        equal("3일 후 초기화", WearDisplayText.localize(ko, "Resets in 3d"));
        equal("인증하지 못했습니다(HTTP 401).", DisplayMessages.localize(ko, "Authentication failed (HTTP 401)."));
        equal("사용량 기간 2개가 초기화되었습니다.", DisplayMessages.localize(ko, "Reset applied to 2 usage windows."));
        equal("provider-specific detail", DisplayMessages.localize(ko, "provider-specific detail"));
        for (boolean success : new boolean[] {false, true}) {
            String link = "codexmeter://auth/complete?value='&x=<test>";
            equal(OAuthBrowserPage.render("Signed in successfully.", success, link), OAuthBrowserText.render(en, "Signed in successfully.", success, link));
            String localized = OAuthBrowserText.render(ko, "Signed in successfully.", success, link);
            if (!localized.contains("lang=\"ko\"") || !localized.contains("로그인되었습니다.")) throw new AssertionError("Korean callback copy missing");
            if (localized.contains("You’re connected") || localized.contains("Let’s try that again")) throw new AssertionError("English callback heading leaked");
            String unsafe = OAuthBrowserText.render(ko, "<script>alert(1)</script>", success, link);
            if (!unsafe.contains("&lt;script&gt;alert(1)&lt;/script&gt;")) throw new AssertionError("HTML escaping changed");
        }
        equal("하루 권장 사용량: 15.5%", ko.getString(R.string.insight_budget, "15.5"));
        equal("Suggested daily usage: 15.5%", en.getString(R.string.insight_budget, "15.5"));
        equal("최근 24시간 0분 기준: 하루 21.0% 사용",
                ko.getString(R.string.insight_recent_rate, "24시간 0분", "21.0"));
        equal("Based on the last 24h 0m: 21.0% per day",
                en.getString(R.string.insight_recent_rate, "24h 0m", "21.0"));
        equal("주의: 2시간 14분 전 데이터", ko.getString(R.string.insight_stale, "2시간 14분"));
        equal("현재 속도 유지 시 초기화 때 약 18.0% 남을 것으로 예상",
                ko.getString(R.string.insight_remaining_at_reset, "18.0"));
        equal("Estimated 18.0% remaining at reset if this rate continues",
                en.getString(R.string.insight_remaining_at_reset, "18.0"));
        equal("현재 속도 유지 시 10월 9일 23:20경 소진 예상",
                ko.getString(R.string.insight_depletion, "10월 9일 23:20"));
        equal("주간 사용량 초기화 감지", ko.getString(R.string.insight_event_reset, "주간"));
        equal("3분", ko.getString(R.string.insight_duration_minutes, 3L));
        equal("2시간 14분", ko.getString(R.string.insight_duration_hours, 2L, 14L));
        equal("10/08 · Weekly · 20% used\nObserved usage increase",
                en.getString(R.string.ledger_row, "10/08", "Weekly", 20, "Observed usage increase"));
        equal("10/08 · 주간 · 20% 사용\n사용량 증가 관측",
                ko.getString(R.string.ledger_row, "10/08", "주간", 20, "사용량 증가 관측"));
        equal("Observed usage increase (+5 pp)", en.getString(R.string.ledger_delta, "Observed usage increase", 5));
        equal("한도 증가 (-5 pp)", ko.getString(R.string.ledger_delta, "한도 증가", -5));
        equal("Observations: 3 · span: 6h · 8 pp observed increase", en.getString(R.string.ledger_segment, 3, "6h", 8));
        equal("6시간 동안 3회 관측 · 사용량 8 pp 증가", ko.getString(R.string.ledger_segment, 3, "6시간", 8));
        equal("At the latest observation: daily budget 20.0 pp · recent pace 10.0 pp/day",
                en.getString(R.string.ledger_budget, "20.0", "10.0"));
        equal("최근 관측 시점 기준: 하루 여유량 20.0 pp · 최근 속도 10.0 pp/일",
                ko.getString(R.string.ledger_budget, "20.0", "10.0"));
        equal("Retained observations: 1 (up to 2,048 or 90 days). Showing the latest 12.", en.getString(R.string.ledger_retained, 1));
        equal("관측값 1개 보관 중 (최대 2,048개 또는 90일). 최근 12개를 표시합니다.", ko.getString(R.string.ledger_retained, 1));
        System.out.println("Localization formatter assertions passed: " + checks);
    }
}
