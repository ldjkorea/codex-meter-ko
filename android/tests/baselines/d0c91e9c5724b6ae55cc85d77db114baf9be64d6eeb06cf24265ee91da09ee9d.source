package dev.bennett.codexmeter;

import java.time.LocalDate;
import java.util.List;

/** Read-only display decisions. Missing comparison data is never presented as zero usage. */
public final class LedgerPresentation {
    private LedgerPresentation() { }

    public static boolean measured(LedgerAggregation.Day day) {
        return day != null && day.coveredMillis > 0;
    }

    public static LedgerAggregation.Day find(List<LedgerAggregation.Day> days, String policy, LocalDate date) {
        for (LedgerAggregation.Day day : days)
            if (day.policy.equals(policy) && day.date.equals(date.toString())) return day;
        return null;
    }

    public static LedgerRecord latest(List<LedgerRecord> records) {
        LedgerRecord best = null;
        for (LedgerRecord row : records)
            if (best == null || row.at > best.at || row.at == best.at && "weekly".equals(row.meter)) best = row;
        return best;
    }

    public static Period period(List<LedgerAggregation.Day> days, String policy, LocalDate end, int count) {
        Period result = new Period();
        for (int i = 0; i < count; i++) {
            LedgerAggregation.Day day = find(days, policy, end.minusDays(i));
            if (day != null) result.recordedDays++;
            if (measured(day)) { result.points += day.points; result.measuredDays++; }
        }
        return result;
    }

    public static final class Period {
        public double points;
        public int recordedDays, measuredDays;
    }
}
