"""Narrow dashboard presentation hooks against the verified 2.8.2 source."""
MAIN_PATCHES = [
('    private TextView freshnessView;', '    private TextView freshnessView;\n    private LedgerDashboard.State ledgerOverview;'),
('            if (snapshot != null) {\n                if (freshnessView != null)',
 '            if (snapshot != null) {\n                if (ledgerOverview != null) ledgerOverview.updateClock();\n                if (freshnessView != null)'),
('        menu.add(Menu.NONE, MENU_SETTINGS, 1,',
 '        menu.add(Menu.NONE, 8103, 2, getString(R.string.next_title));\n        menu.add(Menu.NONE, MENU_SETTINGS, 1,'),
('        if (item.getItemId() == MENU_SETTINGS) {',
 '        if (item.getItemId() == 8103) {\n            Ui.startSecondaryActivity(this, LedgerAnalyticsActivity.class);\n            return true;\n        }\n        if (item.getItemId() == MENU_SETTINGS) {'),
('            freshnessView = null;', '            freshnessView = null;\n            ledgerOverview = null;'),
('            UsageSnapshot lastSuccess = AppPreferences.loadSnapshot(this);',
 '            UsageSnapshot lastSuccess = AppPreferences.loadSnapshot(this);\n            if (SecureTokenStore.isSignedIn(this))\n                ledgerOverview = LedgerDashboard.addOverview(this, this.content, this.executor, lastSuccess, this.dark);'),
('                this.content.addView(dashboard);',
 '                if (ledgerOverview != null) {\n                    this.content.addView(LedgerUi.heading(this, getString(R.string.ui_ledger_limits), this.dark));\n                    Ui.addSpacer(this.content, 10);\n                }\n                this.content.addView(dashboard);'),
('''        LedgerDashboard.add(this, card, this.executor, this.dark);
        Button analytics = Ui.button(this, getString(R.string.next_title), false, this.dark);
        analytics.setOnClickListener(view -> Ui.startSecondaryActivity(this, LedgerAnalyticsActivity.class));
        card.addView(analytics);
        Button record = Ui.button(this, getString(R.string.next_manual), false, this.dark);
        record.setOnClickListener(view -> startActivity(new Intent(this, LedgerAnalyticsActivity.class)
                .putExtra("manual_record", true)));
        card.addView(record);
''', '')
]

def dashboard_patch(source):
    for before, after in MAIN_PATCHES:
        assert source.count(before) == 1, ("Dashboard hook mismatch", before)
        source = source.replace(before, after)
    return source
