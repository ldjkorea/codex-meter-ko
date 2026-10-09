package dev.bennett.codexmeter;

import android.app.AlertDialog;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Full local-history view: scrubbable charts always, with every extra highlight —
 * chart guide, previous-window list, insight rows, and value estimates — individually
 * customizable so the page can stay as minimal as the user likes.
 */
public final class UsageHistoryActivity extends AppCompatActivity {
    private static final int MAX_BREAKDOWN_WINDOWS = 5;
    private static final int MENU_CUSTOMIZE = 8201;

    private LinearLayout content;
    private boolean dark;

    @Override
    protected void onCreate(Bundle state) {
        Ui.applySelectedTheme(this);
        super.onCreate(state);
        if(!getIntent().getBooleanExtra("advanced",false)){startActivity(new android.content.Intent(this,LedgerAnalyticsActivity.class));finish();return;}
        dark = Ui.isDark(this);
        content = Ui.installPage(this, UsageHistoryActivity.this.getString(R.string.ui_usage_history_b2a357), true).content;
        render();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(Menu.NONE, MENU_CUSTOMIZE, 0, UsageHistoryActivity.this.getString(R.string.ui_customize_239dce))
                .setIcon(R.drawable.ic_oui_edit_outline)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == MENU_CUSTOMIZE) {
            showCustomizeDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /** Checklist of every optional highlight; changes persist and apply immediately. */
    private void showCustomizeDialog() {
        List<String> keys = HistorySections.all();
        String[] labels = new String[keys.size()];
        boolean[] checked = new boolean[keys.size()];
        for (int i = 0; i < keys.size(); i++) {
            labels[i] = DisplayLabels.history(this, keys.get(i));
            checked[i] = AppPreferences.isHistorySectionVisible(this, keys.get(i));
        }
        new AlertDialog.Builder(this)
                .setTitle(UsageHistoryActivity.this.getString(R.string.ui_highlights_to_show_b49cc0))
                .setMultiChoiceItems(labels, checked, (dialog, which, isChecked) ->
                        AppPreferences.setHistorySectionVisible(this, keys.get(which), isChecked))
                .setPositiveButton(UsageHistoryActivity.this.getString(R.string.ui_done_e9b450), null)
                .setOnDismissListener(dialog -> render())
                .show();
    }

    private boolean visible(String key) {
        return AppPreferences.isHistorySectionVisible(this, key);
    }

    private void render() {
        content.removeAllViews();
        UsageSnapshot snapshot = AppPreferences.loadSnapshot(this);
        UsageHistory five = AppPreferences.loadUsageHistory(this, UsageHistory.FIVE_HOUR);
        UsageHistory weekly = AppPreferences.loadUsageHistory(this, UsageHistory.WEEKLY);
        UsageHistory monthly = AppPreferences.loadUsageHistory(this, UsageHistory.MONTHLY);
        // Dollar figures ride on the value-estimates highlight; hiding it hides them all.
        PlanPricing pricing = null; // No verified quota-to-dollar conversion in this release.

        if (visible(HistorySections.GUIDE)) {
            LinearLayout guide = Ui.card(this, dark);
            guide.addView(Ui.text(this,
                    UsageHistoryActivity.this.getString(R.string.ui_the_solid_line_is_this_window_s_usage_faint_lines_are_p_681efd),
                    13, Ui.secondaryText(dark)));
            content.addView(guide);
            Ui.addSpacer(content, 20);
        }

        // Windows still waiting for usage data are skipped instead of rendering blank charts.
        boolean hasCharts = false;
        UsageWindow fiveWindow = snapshot == null ? null : snapshot.fiveHour;
        if (AppPreferences.showDashboardFiveHour(this) && fiveWindow != null && snapshot.fetchedAtMillis > 0L) {
            addWindowSection(UsageHistoryActivity.this.getString(R.string.ui_5_hour_bc4288), fiveWindow, snapshot, five, pricing);
            hasCharts = true;
        }
        UsageWindow weeklyWindow = snapshot == null ? null : snapshot.weekly;
        if (AppPreferences.showDashboardWeekly(this) && weeklyWindow != null && snapshot.fetchedAtMillis > 0L) {
            addWindowSection(UsageHistoryActivity.this.getString(R.string.ui_weekly_158f3d), weeklyWindow, snapshot, weekly, pricing);
            hasCharts = true;
        }
        UsageWindow monthlyWindow = snapshot == null ? null : snapshot.monthly;
        if (AppPreferences.showDashboardMonthly(this) && monthlyWindow != null && snapshot.fetchedAtMillis > 0L) {
            addWindowSection(UsageHistoryActivity.this.getString(R.string.ui_monthly_d31edb), monthlyWindow, snapshot, monthly, pricing);
            hasCharts = true;
        }
        if (!hasCharts) {
            LinearLayout waiting = Ui.card(this, dark);
            waiting.addView(Ui.text(this,
                    UsageHistoryActivity.this.getString(R.string.ui_charts_appear_once_openai_reports_your_5_hour_weekly_or_38aa10),
                    13, Ui.secondaryText(dark)));
            content.addView(waiting);
            Ui.addSpacer(content, 20);
        }

        if (pricing != null && hasCharts) {
            content.addView(Ui.separator(this, UsageHistoryActivity.this.getString(R.string.ui_estimated_value_7332c4)));
            content.addView(buildValueCard(snapshot, pricing));
            Ui.addSpacer(content, 20);
        }

        UsageInsightDisplay.addEventHistory(this, content, dark);
        UsageLedgerDisplay.addHistory(this, content, snapshot, dark);
        Button analytics = Ui.button(this, getString(R.string.next_title), false, dark);
        analytics.setOnClickListener(view -> Ui.startSecondaryActivity(this, LedgerAnalyticsActivity.class));
        content.addView(analytics);

        Button clear = Ui.button(this, UsageHistoryActivity.this.getString(R.string.ui_clear_local_history_ab1b10), false, dark);
        clear.setEnabled(!five.samples.isEmpty() || !weekly.samples.isEmpty()
                || !monthly.samples.isEmpty() || UsageEventStore.hasEvents(this)
                || UsageLedgerStore.hasObservations(this));
        clear.setOnClickListener(view -> new AlertDialog.Builder(this)
                .setTitle(UsageHistoryActivity.this.getString(R.string.ui_clear_usage_history_03c461))
                .setMessage(UsageHistoryActivity.this.getString(R.string.ui_this_removes_every_locally_stored_usage_sample_your_lat_e3f515))
                .setNegativeButton(UsageHistoryActivity.this.getString(R.string.ui_cancel_77dfd2), null)
                .setPositiveButton(UsageHistoryActivity.this.getString(R.string.ui_clear_719ea3), (dialog, which) -> {
                    android.content.Context app = getApplicationContext();
                    clear.setEnabled(false);
                    new Thread(() -> {
                        AccountSession.clearHistory(app);
                        runOnUiThread(() -> { if (!isDestroyed() && !isFinishing()) render(); });
                    }, "codex-clear-history").start();
                })
                .show());
        content.addView(clear, new LinearLayout.LayoutParams(-1, Ui.dp(this, 58)));
    }

    private void addWindowSection(String label, UsageWindow window, UsageSnapshot snapshot,
            UsageHistory history, PlanPricing pricing) {
        content.addView(Ui.separator(this, label + UsageHistoryActivity.this.getString(R.string.ui_window_3621b4)));
        content.addView(buildChartCard(label, window, snapshot, history, pricing));
        Ui.addSpacer(content, 12);
        LinearLayout insights = buildInsightsCard(window, snapshot, history, pricing);
        if (insights != null) {
            content.addView(insights);
            Ui.addSpacer(content, 12);
        }
        Ui.addSpacer(content, 8);
    }

    private LinearLayout buildChartCard(String label, UsageWindow window, UsageSnapshot snapshot,
            UsageHistory history, PlanPricing pricing) {
        LinearLayout card = Ui.card(this, dark);
        card.setPadding(Ui.dp(this, 6), Ui.dp(this, 8), Ui.dp(this, 6), Ui.dp(this, 8));
        long now = System.currentTimeMillis();
        UsagePace.Assessment pace = snapshot == null
                ? UsagePace.assess(null, 0L, now, UsagePace.BALANCED)
                : UsagePacePreferences.assess(this, snapshot, window, now);
        UsageBurnChartView chart = new UsageBurnChartView(this);
        chart.setScrubEnabled(true);
        chart.setData(label, window, history,
                snapshot == null ? now : snapshot.fetchedAtMillis, pace);
        card.addView(chart, new LinearLayout.LayoutParams(-1, Ui.dp(this, 200)));

        List<UsageStats.WindowStats> breakdown =
                UsageStats.windowBreakdown(history, MAX_BREAKDOWN_WINDOWS);
        boolean showWindowRows = visible(HistorySections.WINDOW_LIST) && breakdown.size() > 1;

        String defaultDetail = showWindowRows
                ? UsageHistoryActivity.this.getString(R.string.ui_drag_to_inspect_tap_a_window_to_compare_65b49d) : UsageHistoryActivity.this.getString(R.string.ui_drag_to_inspect_db171c);
        TextView scrubDetail = Ui.text(this, defaultDetail, 12, Ui.secondaryText(dark));
        LinearLayout.LayoutParams scrubParams = new LinearLayout.LayoutParams(-1, -2);
        scrubParams.setMargins(Ui.dp(this, 12), Ui.dp(this, 2), Ui.dp(this, 12), Ui.dp(this, 6));
        card.addView(scrubDetail, scrubParams);
        chart.setOnScrubListener(new UsageBurnChartView.OnScrubListener() {
            @Override
            public void onScrub(long timeMillis, double usedPercent, boolean historicalWindow) {
                String moment = UsageFormat.absolute(UsageHistoryActivity.this, timeMillis,
                        System.currentTimeMillis());
                String text = moment + " — " + Math.round(usedPercent) + UsageHistoryActivity.this.getString(R.string.ui_used_3186a6);
                if (pricing != null) {
                    text += " · ≈ " + PlanPricing.formatUsd(
                            pricing.estimatedValueUsd(history.kind, usedPercent));
                }
                scrubDetail.setTextColor(Ui.mainText(dark));
                scrubDetail.setText(text);
            }

            @Override
            public void onScrubEnd() {
                scrubDetail.setTextColor(Ui.secondaryText(dark));
                scrubDetail.setText(defaultDetail);
            }
        });

        if (showWindowRows) {
            View divider = new View(this);
            divider.setBackgroundColor(Ui.divider(dark));
            LinearLayout.LayoutParams dividerParams = new LinearLayout.LayoutParams(-1, 1);
            dividerParams.setMargins(Ui.dp(this, 12), Ui.dp(this, 4), Ui.dp(this, 12),
                    Ui.dp(this, 4));
            card.addView(divider, dividerParams);
            addWindowRows(card, chart, history, breakdown, pricing);
        }
        return card;
    }

    /** Tappable per-window rows that select a window on the chart for scrubbing. */
    private void addWindowRows(LinearLayout card, UsageBurnChartView chart, UsageHistory history,
            List<UsageStats.WindowStats> breakdown, PlanPricing pricing) {
        boolean dayGranularity = UsageHistory.WEEKLY.equals(history.kind)
                || UsageHistory.MONTHLY.equals(history.kind);
        TextView[] titles = new TextView[breakdown.size()];
        Runnable[] selections = new Runnable[breakdown.size()];
        for (int index = breakdown.size() - 1; index >= 0; index--) {
            UsageStats.WindowStats stats = breakdown.get(index);
            boolean current = !stats.complete;
            String rowTitle = current ? UsageHistoryActivity.this.getString(R.string.ui_current_window_eaa1ee)
                    : windowRangeLabel(stats, dayGranularity);
            StringBuilder subtitle = new StringBuilder();
            subtitle.append(stats.finalPercent).append(UsageHistoryActivity.this.getString(R.string.ui_used_3186a6));
            if (stats.averageBurnPercentPerHour > 0d) {
                subtitle.append(UsageHistoryActivity.this.getString(R.string.ui_avg_9ef413)).append(formatRate(stats.averageBurnPercentPerHour));
            }
            if (pricing != null) {
                subtitle.append(" · ≈ ").append(PlanPricing.formatUsd(
                        pricing.estimatedValueUsd(history.kind, stats.finalPercent)));
            }
            if (stats.exhausted) subtitle.append(UsageHistoryActivity.this.getString(R.string.ui_hit_limit_1530b1));

            LinearLayout row = Ui.horizontal(this, Gravity.CENTER_VERTICAL);
            row.setPadding(Ui.dp(this, 12), Ui.dp(this, 8), Ui.dp(this, 12), Ui.dp(this, 8));
            LinearLayout texts = new LinearLayout(this);
            texts.setOrientation(LinearLayout.VERTICAL);
            TextView titleView = Ui.text(this, rowTitle, 14,
                    current ? Ui.accent(this, dark) : Ui.mainText(dark));
            titleView.setTypeface(Ui.mediumTypeface(this));
            texts.addView(titleView);
            texts.addView(Ui.text(this, subtitle.toString(), 12, Ui.secondaryText(dark)));
            row.addView(texts, new LinearLayout.LayoutParams(0, -2, 1f));
            card.addView(row, new LinearLayout.LayoutParams(-1, -2));

            titles[index] = titleView;
            int chartWindowIndex = chart.windowCount() - breakdown.size() + index;
            boolean selectsCurrent = current;
            int rowIndex = index;
            selections[index] = () -> {
                chart.setSelectedWindow(selectsCurrent ? -1 : chartWindowIndex);
                for (int i = 0; i < titles.length; i++) {
                    boolean selected = i == rowIndex;
                    titles[i].setTextColor(selected ? Ui.accent(this, dark)
                            : Ui.mainText(dark));
                }
            };
            row.setOnClickListener(view -> selections[rowIndex].run());
            row.setClickable(true);
            row.setFocusable(true);
            row.setContentDescription(UsageHistoryActivity.this.getString(R.string.ui_inspect_c88107) + rowTitle + ". " + subtitle);
        }
    }

    private LinearLayout buildInsightsCard(UsageWindow window, UsageSnapshot snapshot,
            UsageHistory history, PlanPricing pricing) {
        long now = System.currentTimeMillis();
        long observedAt = snapshot == null ? now : snapshot.fetchedAtMillis;
        LinearLayout card = Ui.card(this, dark);
        TextView title = Ui.text(this, UsageHistoryActivity.this.getString(R.string.ui_insights_b45103), 16, Ui.mainText(dark));
        title.setTypeface(Ui.mediumTypeface(this));
        card.addView(title);
        int rows = 0;

        // Current position against the typical pace of completed windows.
        long resetAt = window.effectiveResetAtMillis(observedAt);
        long durationMillis = window.windowSeconds * 1000L;
        if (visible(HistorySections.INSIGHT_PACE) && resetAt > 0L && durationMillis > 0L) {
            double elapsedFraction = 1d - Math.max(0d, Math.min(1d,
                    (resetAt - now) / (double) durationMillis));
            double typical = UsageStats.typicalUsedPercentAt(history, elapsedFraction);
            if (typical >= 0d) {
                long delta = Math.round(window.usedPercent - typical);
                String value;
                if (delta >= 2L) {
                    value = UsageHistoryActivity.this.getString(R.string.ui_1_d_pts_ahead_of_typical_9814ba, delta);
                } else if (delta <= -2L) {
                    value = UsageHistoryActivity.this.getString(R.string.ui_1_d_pts_behind_typical_a567f7, -delta);
                } else {
                    value = UsageHistoryActivity.this.getString(R.string.ui_on_par_with_typical_961f7b);
                }
                addStatRow(card, UsageHistoryActivity.this.getString(R.string.ui_pace_vs_previous_windows_9d9553), value);
                rows++;
            }
        }

        if (visible(HistorySections.INSIGHT_EXHAUSTION)) {
            UsagePace.Assessment pace = snapshot == null ? null
                    : UsagePacePreferences.assess(this, snapshot, window, now);
            if (pace != null && pace.available) {
                addStatRow(card, UsageHistoryActivity.this.getString(R.string.ui_projected_exhaustion_9f0e66),
                        UsageFormat.relative(UsageHistoryActivity.this, pace.estimatedExhaustionAtMillis, now));
                rows++;
            }
        }

        if (visible(HistorySections.INSIGHT_AVERAGE)) {
            double averageFinal = UsageStats.averageFinalPercent(history);
            if (averageFinal >= 0d) {
                addStatRow(card, UsageHistoryActivity.this.getString(R.string.ui_avg_completed_window_35558e), Math.round(averageFinal) + UsageHistoryActivity.this.getString(R.string.ui_used_3186a6));
                rows++;
            }
        }

        if (visible(HistorySections.INSIGHT_PEAK)) {
            double peakBurn = UsageStats.peakBurnPercentPerHour(history);
            if (peakBurn > 0d) {
                String value = formatRate(peakBurn);
                if (pricing != null) {
                    value += " · ≈ " + PlanPricing.formatUsd(
                            pricing.windowValueUsd(history.kind) * peakBurn / 100d) + "/h";
                }
                addStatRow(card, UsageHistoryActivity.this.getString(R.string.ui_peak_burn_observed_bcc9b9), value);
                rows++;
            }
        }

        if (pricing != null) {
            addStatRow(card, UsageHistoryActivity.this.getString(R.string.ui_est_value_used_this_window_3f3ec1),
                    "≈ " + PlanPricing.formatUsd(pricing.estimatedValueUsd(history.kind,
                            window.usedPercent))
                            + UsageHistoryActivity.this.getString(R.string.ui_of_607f2d) + PlanPricing.formatUsd(
                                    pricing.windowValueUsd(history.kind)));
            rows++;
        }
        return rows == 0 ? null : card;
    }

    private LinearLayout buildValueCard(UsageSnapshot snapshot, PlanPricing pricing) {
        LinearLayout card = Ui.card(this, dark);
        TextView title = Ui.text(this, pricing.planLabel + " · "
                + PlanPricing.formatUsd(pricing.monthlyPriceUsd) + "/month", 16,
                Ui.mainText(dark));
        title.setTypeface(Ui.mediumTypeface(this));
        card.addView(title);
        addStatRow(card, UsageHistoryActivity.this.getString(R.string.ui_est_included_usage_a988a3),
                UsageHistoryActivity.this.getString(R.string.ui_1_s_month_91a7f1, PlanPricing.formatUsd(pricing.monthlyValueUsd)));
        addStatRow(card, UsageHistoryActivity.this.getString(R.string.ui_weekly_allowance_0dfb07),
                "≈ " + PlanPricing.formatUsd(pricing.weeklyValueUsd()));
        addStatRow(card, UsageHistoryActivity.this.getString(R.string.ui_5_hour_allowance_059e87),
                "≈ " + PlanPricing.formatUsd(pricing.fiveHourValueUsd()));
        addStatRow(card, UsageHistoryActivity.this.getString(R.string.ui_vs_subscription_price_9e8586),
                UsageHistoryActivity.this.getString(R.string.ui_1_dx_the_monthly_cost_926b6a, Math.round(pricing.valueMultiplier())));
        if (snapshot.weekly != null) {
            addStatRow(card, UsageHistoryActivity.this.getString(R.string.ui_weekly_value_remaining_baeb07),
                    "≈ " + PlanPricing.formatUsd(pricing.estimatedValueUsd(UsageHistory.WEEKLY,
                            snapshot.weekly.remainingPercent())));
        }
        TextView disclaimer = Ui.text(this,
                UsageHistoryActivity.this.getString(R.string.ui_rough_community_estimates_comparing_plan_allowances_wit_51660e),
                12, Ui.secondaryText(dark));
        LinearLayout.LayoutParams disclaimerParams = new LinearLayout.LayoutParams(-1, -2);
        disclaimerParams.setMargins(0, Ui.dp(this, 10), 0, 0);
        card.addView(disclaimer, disclaimerParams);
        return card;
    }

    private void addStatRow(LinearLayout card, String label, String value) {
        LinearLayout row = Ui.horizontal(this, Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(-1, -2);
        rowParams.setMargins(0, Ui.dp(this, 8), 0, 0);
        TextView labelView = Ui.text(this, label, 13, Ui.secondaryText(dark));
        row.addView(labelView, new LinearLayout.LayoutParams(0, -2, 1f));
        TextView valueView = Ui.text(this, value, 13, Ui.mainText(dark));
        valueView.setTypeface(PretendardFont.regular(this));
        valueView.setGravity(Gravity.END);
        row.addView(valueView, new LinearLayout.LayoutParams(-2, -2));
        card.addView(row, rowParams);
    }

    private String windowRangeLabel(UsageStats.WindowStats stats, boolean dayGranularity) {
        boolean is24Hour = DateFormat.is24HourFormat(this);
        if (dayGranularity) {
            SimpleDateFormat day = new SimpleDateFormat(UsageHistoryActivity.this.getString(R.string.ui_mmm_d_a72e0f), Locale.getDefault());
            return day.format(new Date(stats.windowStartMillis)) + " – "
                    + day.format(new Date(stats.resetAtMillis));
        }
        SimpleDateFormat day = new SimpleDateFormat(UsageHistoryActivity.this.getString(R.string.ui_mmm_d_a72e0f), Locale.getDefault());
        SimpleDateFormat time = new SimpleDateFormat(is24Hour ? "HH:mm" : UsageHistoryActivity.this.getString(R.string.ui_h_mm_a_5daf91),
                Locale.getDefault());
        return day.format(new Date(stats.windowStartMillis)) + " · "
                + time.format(new Date(stats.windowStartMillis)) + " – "
                + time.format(new Date(stats.resetAtMillis));
    }

    private String formatRate(double percentPerHour) {
        if (percentPerHour >= 10d) {
            return this.getString(R.string.ui_1_d_h_89f286, Math.round(percentPerHour));
        }
        return String.format(Locale.US, this.getString(R.string.ui_1f_h_cd962b), percentPerHour);
    }
}
