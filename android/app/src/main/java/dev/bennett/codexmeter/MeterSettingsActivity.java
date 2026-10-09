package dev.bennett.codexmeter;

/** Consolidated settings. Uses the existing account-safe ledger export/delete controller. */
public final class MeterSettingsActivity extends LedgerAnalyticsActivity {
    @Override protected boolean settingsRoot(){return true;}
}
