"""Exact reviewed source substitutions for the 2.8.2 stability audit."""
PATCHES = {
"MainActivity.java": [( '''        final AuthTokens authTokensLoad = SecureTokenStore.load(this);
        SecureTokenStore.clear(this);
        AppPreferences.clearSnapshot(this);
        AppPreferences.setOAuthPending(this, false, "");
        RefreshScheduler.cancelAll(this);
        ResetAlertScheduler.cancelAll(this);
        WidgetRenderer.updateAll(this);
        rebuild();
        this.executor.execute(new Runnable() { // from class: dev.bennett.codexmeter.MainActivity.11
            @Override // java.lang.Runnable
            public void run() {
                OAuthClient.revokeBestEffort(MainActivity.this.getApplicationContext(),
                        authTokensLoad);
            }
        });''', '''        android.content.Context app = getApplicationContext();
        new Thread(() -> {
            AuthTokens previous = AccountSession.signOut(app);
            runOnUiThread(() -> { if (!isDestroyed() && !isFinishing()) rebuild(); });
            OAuthClient.revokeBestEffort(app, previous);
        }, "codex-sign-out").start();''' )],
"SettingsActivity.java": [( '''                        AuthTokens tokens = SecureTokenStore.load(requireContext());
                        SecureTokenStore.clear(requireContext());
                        AppPreferences.clearSnapshot(requireContext());
                        AppPreferences.setOAuthPending(requireContext(), false, "");
                        RefreshScheduler.cancelAll(requireContext());
                        ResetAlertScheduler.cancelAll(requireContext());
                        WidgetRenderer.updateAll(requireContext());
                        Toast.makeText(requireContext(), DisplayMessages.localize(requireContext(), requireContext().getString(R.string.ui_signed_out_05d2a7)), Toast.LENGTH_SHORT).show();
                        requireActivity().recreate();
                        if (tokens != null) {
                            Context app = requireContext().getApplicationContext();
                            new Thread(() -> OAuthClient.revokeBestEffort(app, tokens),
                                    "codex-sign-out").start();
                        }''', '''                        Context app = requireContext().getApplicationContext();
                        androidx.fragment.app.FragmentActivity activity = requireActivity();
                        new Thread(() -> {
                            AuthTokens tokens = AccountSession.signOut(app);
                            activity.runOnUiThread(() -> {
                                if (activity.isDestroyed() || activity.isFinishing()) return;
                                Toast.makeText(activity, DisplayMessages.localize(activity,
                                        activity.getString(R.string.ui_signed_out_05d2a7)), Toast.LENGTH_SHORT).show();
                                activity.recreate();
                            });
                            OAuthClient.revokeBestEffort(app, tokens);
                        }, "codex-sign-out").start();''' )],
"OAuthService.java": [( '''                SecureTokenStore.save(this, tokens);''', '''                AccountSession.install(this, tokens, false);''' )],
"SettingsTransferStore.java": [( '''        SecureTokenStore.save(context, tokens);
        AppPreferences.clearSnapshot(context);
        AppPreferences.setOAuthPending(context, false, "");''', '''        AccountSession.install(context, tokens, true);''' )],
"UsageHistoryActivity.java": [( '''                    AppPreferences.clearUsageHistory(this);
                    render();''', '''                    android.content.Context app = getApplicationContext();
                    clear.setEnabled(false);
                    new Thread(() -> {
                        AccountSession.clearHistory(app);
                        runOnUiThread(() -> { if (!isDestroyed() && !isFinishing()) render(); });
                    }, "codex-clear-history").start();''' )],
"LedgerAnalyticsActivity.java": [( '    @Override protected void onDestroy(){worker.shutdownNow();super.onDestroy();}\n', ''),
    ('private final ExecutorService worker=Executors.newSingleThreadExecutor();', '// Survive rotation/finish: accepted manual saves and SAF exports finish in order.\n    private static final ExecutorService worker=Executors.newSingleThreadExecutor();'),
    ('LedgerMaintenanceScheduler.schedule(getApplicationContext())', 'UsageLedgerDatabase.scheduleMaintenance(getApplicationContext())'),
    ('AppPreferences.clearUsageHistory(getApplicationContext())', 'AccountSession.clearHistory(getApplicationContext())')],
"AppPreferences.java": [
    ('    public static void clearSnapshot(Context context) {',
     '    // Called by AccountSession off the UI thread; deletion must survive process termination.\n    @android.annotation.SuppressLint("ApplySharedPref")\n    public static void clearSnapshot(Context context) {'),
    ('    public static void clearUsageHistory(Context context) {',
     '    // Commit before returning so a process restart cannot restore deleted preference history.\n    @android.annotation.SuppressLint("ApplySharedPref")\n    public static void clearUsageHistory(Context context) {'),
    ('.remove(KEY_HISTORY_MONTHLY)\n                .remove(KEY_REFRESH_FAILURES).apply();', '.remove(KEY_HISTORY_MONTHLY)\n                .remove(KEY_REFRESH_FAILURES).commit();'),
    ('.remove(KEY_HISTORY_MONTHLY).apply();', '.remove(KEY_HISTORY_MONTHLY).commit();')],
}

def reviewed_release_patch(name, source):
    for before, after in PATCHES.get(name, []):
        assert source.count(before) == 1, (name, "reviewed release patch no longer matches", before)
        source = source.replace(before, after)
    return source

# Transfer import also waits on the account boundary off the UI thread.
PATCHES["SettingsActivity.java"].append(('''        private void finishImport(SettingsTransfer.Document document, boolean appSettings,
                boolean notifications, boolean nowBar, boolean authentication) {
            try {
                SettingsTransferStore.ApplyResult result = SettingsTransferStore.apply(
                        requireContext(), document, appSettings, notifications, nowBar,
                        authentication);
                pendingImportDocument = null;
                StringBuilder message = new StringBuilder(requireContext().getString(R.string.ui_imported_eec56a));
                for (int i = 0; i < result.appliedSections.size(); i++) {
                    if (i > 0) message.append(", ");
                    message.append(DisplayMessages.localize(requireContext(), SettingsTransfer.sectionTitle(result.appliedSections.get(i))));
                }
                message.append('.');
                if (result.authenticationImported) {
                    message.append(requireContext().getString(R.string.ui_authentication_replaced_keep_the_file_private_dc5598));
                }
                Toast.makeText(requireContext(), DisplayMessages.localize(requireContext(), message.toString()), Toast.LENGTH_LONG).show();
                requireActivity().recreate();
            } catch (Exception exception) {
                Toast.makeText(requireContext(), DisplayMessages.localize(requireContext(), exception.getMessage() == null || exception.getMessage().isEmpty()
                                ? requireContext().getString(R.string.ui_could_not_import_transfer_file_033ff7)
                                : exception.getMessage()),
                        Toast.LENGTH_LONG).show();
            }
        }''', '''        private void finishImport(SettingsTransfer.Document document, boolean appSettings,
                boolean notifications, boolean nowBar, boolean authentication) {
            Context app = requireContext().getApplicationContext();
            androidx.fragment.app.FragmentActivity activity = requireActivity();
            new Thread(() -> {
                try {
                    SettingsTransferStore.ApplyResult result = SettingsTransferStore.apply(
                            app, document, appSettings, notifications, nowBar, authentication);
                    activity.runOnUiThread(() -> {
                        if (activity.isDestroyed() || activity.isFinishing()) return;
                        pendingImportDocument = null;
                        StringBuilder message = new StringBuilder(activity.getString(R.string.ui_imported_eec56a));
                        for (int i = 0; i < result.appliedSections.size(); i++) {
                            if (i > 0) message.append(", ");
                            message.append(DisplayMessages.localize(activity,
                                    SettingsTransfer.sectionTitle(result.appliedSections.get(i))));
                        }
                        message.append('.');
                        if (result.authenticationImported) {
                            message.append(activity.getString(R.string.ui_authentication_replaced_keep_the_file_private_dc5598));
                        }
                        Toast.makeText(activity, DisplayMessages.localize(activity, message.toString()), Toast.LENGTH_LONG).show();
                        activity.recreate();
                    });
                } catch (Exception exception) {
                    activity.runOnUiThread(() -> {
                        if (activity.isDestroyed() || activity.isFinishing()) return;
                        Toast.makeText(activity, DisplayMessages.localize(activity,
                                exception.getMessage() == null || exception.getMessage().isEmpty()
                                        ? activity.getString(R.string.ui_could_not_import_transfer_file_033ff7)
                                        : exception.getMessage()), Toast.LENGTH_LONG).show();
                    });
                }
            }, "codex-import-settings").start();
        }'''))
