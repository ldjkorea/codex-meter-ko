package dev.bennett.codexmeter;

import android.content.Context;
import android.widget.LinearLayout;
import java.util.HashMap;
import java.util.Map;

/** Supplemental history cards using the existing native presentation components. */
final class UsageLedgerDisplay {
    private UsageLedgerDisplay() {}

    static void addHistory(Context context, LinearLayout content, UsageSnapshot snapshot, boolean dark) {
        content.addView(Ui.separator(context, context.getString(R.string.ledger_title)));
        LinearLayout card = Ui.card(context, dark);
        card.addView(Ui.text(context, context.getString(R.string.ledger_note), 12, Ui.secondaryText(dark)));
        UsageLedger ledger = UsageLedgerStore.load(context);
        if (ledger == null) {
            card.addView(Ui.text(context, context.getString(R.string.ledger_unreadable), 13, Ui.mainText(dark)));
        } else if (ledger.observations.isEmpty()) {
            card.addView(Ui.text(context, context.getString(R.string.ledger_empty), 13, Ui.secondaryText(dark)));
        } else {
            long now = System.currentTimeMillis();
            if (snapshot != null) {
                addInsight(context, card, ledger, UsageHistory.FIVE_HOUR, snapshot.fiveHour, snapshot, now, dark);
                addInsight(context, card, ledger, UsageHistory.WEEKLY, snapshot.weekly, snapshot, now, dark);
                addInsight(context, card, ledger, UsageHistory.MONTHLY, snapshot.monthly, snapshot, now, dark);
            }
            card.addView(Ui.text(context, context.getString(R.string.ledger_retained,
                    ledger.observations.size()), 12, Ui.secondaryText(dark)));
            Map<String, UsageLedger.Observation> previous = new HashMap<>();
            Map<UsageLedger.Observation, UsageLedger.Change> changes = new HashMap<>();
            Map<UsageLedger.Observation, Integer> deltas = new HashMap<>();
            for (UsageLedger.Observation observation : ledger.observations) {
                UsageLedger.Observation before = previous.put(observation.kind, observation);
                changes.put(observation, UsageLedger.change(before, observation));
                if (before != null) deltas.put(observation, observation.usedPercent - before.usedPercent);
            }
            // Bound the native view count independently of the retained observations.
            int from = Math.max(0, ledger.observations.size() - 12);
            for (int i = ledger.observations.size() - 1; i >= from; i--) {
                UsageLedger.Observation observation = ledger.observations.get(i);
                UsageLedger.Change change = changes.get(observation);
                String label = context.getString(changeResource(change));
                if (change == UsageLedger.Change.USAGE_INCREASE || change == UsageLedger.Change.ALLOWANCE_INCREASE)
                    label = context.getString(R.string.ledger_delta, label, deltas.get(observation));
                Ui.addSpacer(card, 8);
                card.addView(Ui.text(context, context.getString(R.string.ledger_row,
                        UsageInsightDisplay.timestamp(context, observation.atMillis), meter(context, observation.kind),
                        observation.usedPercent, label), 13, Ui.mainText(dark)));
            }
        }
        content.addView(card);
        Ui.addSpacer(content, 20);
    }

    private static void addInsight(Context context, LinearLayout card, UsageLedger ledger, String kind,
            UsageWindow window, UsageSnapshot snapshot, long now, boolean dark) {
        if (window == null) return;
        UsageLedgerInsights.Result result = UsageLedgerInsights.analyze(ledger, kind, window,
                snapshot.fetchedAtMillis, now, UsageInsightDisplay.normalRefreshMinutes(context, snapshot, now));
        Ui.addSpacer(card, 12);
        card.addView(Ui.text(context, meter(context, kind), 14, Ui.mainText(dark)));
        if (result.status != UsageLedgerInsights.Status.READY) {
            card.addView(Ui.text(context, context.getString(statusResource(result.status)), 12, Ui.secondaryText(dark)));
            return;
        }
        card.addView(Ui.text(context, context.getString(R.string.ledger_segment,
                result.observationCount, UsageInsightDisplay.duration(context, result.spanMillis),
                result.consumedPoints), 13, Ui.mainText(dark)));
        card.addView(Ui.text(context, context.getString(R.string.ledger_budget,
                UsageInsightDisplay.decimal(result.dailyBudget), UsageInsightDisplay.decimal(result.recentDailyUsage)),
                12, Ui.secondaryText(dark)));
        String forecast = result.exhaustionAtMillis < result.resetAtMillis
                ? context.getString(R.string.insight_depletion, UsageInsightDisplay.timestamp(context, result.exhaustionAtMillis))
                : context.getString(R.string.insight_remaining_at_reset, UsageInsightDisplay.decimal(result.remainingAtReset));
        card.addView(Ui.text(context, forecast, 12, Ui.secondaryText(dark)));
    }

    private static String meter(Context context, String kind) {
        return context.getString(UsageHistory.WEEKLY.equals(kind) ? R.string.ui_weekly_158f3d
                : UsageHistory.MONTHLY.equals(kind) ? R.string.ui_monthly_d31edb : R.string.ui_5_hour_bc4288);
    }

    private static int statusResource(UsageLedgerInsights.Status status) {
        switch (status) {
            case UNREADABLE: return R.string.ledger_unreadable;
            case EMPTY: return R.string.ledger_empty;
            case STALE: return R.string.ledger_stale;
            case MISSING_RESET: return R.string.ledger_missing_reset;
            case MISMATCH: return R.string.ledger_mismatch;
            case LOW_CHANGE: return R.string.ledger_low_change;
            case GAP: return R.string.ledger_gap;
            case LARGE_JUMP: return R.string.ledger_large_jump;
            default: return R.string.ledger_warmup;
        }
    }

    private static int changeResource(UsageLedger.Change change) {
        switch (change) {
            case NO_CHANGE: return R.string.ledger_change_none;
            case USAGE_INCREASE: return R.string.ledger_change_usage;
            case ALLOWANCE_INCREASE: return R.string.ledger_change_allowance;
            case RESET_OBSERVED: return R.string.ledger_change_reset;
            case WINDOW_CHANGED: return R.string.ledger_change_window;
            case PLAN_CHANGED: return R.string.ledger_change_plan;
            case GAP: return R.string.ledger_change_gap;
            case MISSING_RESET: return R.string.ledger_change_unknown;
            default: return R.string.ledger_change_baseline;
        }
    }
}
