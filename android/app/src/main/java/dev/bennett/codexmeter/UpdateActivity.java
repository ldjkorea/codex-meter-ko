package dev.bennett.codexmeter;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** User-facing secure download and PackageInstaller hand-off flow. */
public final class UpdateActivity extends AppCompatActivity {
    public static final String EXTRA_VERSION = "release_version";
    public static final String EXTRA_FORCE_CHECK = "force_check";
    public static final String EXTRA_START_INSTALL = "start_install";

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private LinearLayout content;
    private GitHubRelease release;
    private ProgressBar progress;
    private TextView status;
    private boolean waitingForInstallPermission;
    private boolean operationRunning;
    private boolean startInstallPending;
    private boolean dark;
    private volatile boolean cancelled;

    @Override
    protected void onCreate(Bundle bundle) {
        Ui.applySelectedTheme(this);
        super.onCreate(bundle);
        dark = Ui.isDark(this);
        content = Ui.installPage(this, UpdateActivity.this.getString(R.string.ui_app_update_45b5d1), true).content;
        String requested = getIntent().getStringExtra(EXTRA_VERSION);
        release = UpdatePreferences.findVersion(this, requested);
        boolean force = getIntent().getBooleanExtra(EXTRA_FORCE_CHECK, false);
        startInstallPending = getIntent().getBooleanExtra(EXTRA_START_INSTALL, false);
        UpdateNotificationManager.dismiss(this);
        if (release == null || force) {
            checkReleases(requested);
        } else {
            render();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        String requested = intent.getStringExtra(EXTRA_VERSION);
        startInstallPending = intent.getBooleanExtra(EXTRA_START_INSTALL, false);
        UpdateNotificationManager.dismiss(this);
        if (requested != null && !requested.trim().isEmpty()) {
            release = UpdatePreferences.findVersion(this, requested);
        }
        if (release == null) {
            checkReleases(requested);
        } else {
            render();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (waitingForInstallPermission && canInstallPackages()) {
            waitingForInstallPermission = false;
            beginInstall();
            return;
        }
        if (status != null && !operationRunning) {
            String error = UpdatePreferences.installError(this);
            if (!error.isEmpty()) {
                setStatus(error, Ui.danger(dark));
            }
        }
    }

    @Override
    protected void onDestroy() {
        cancelled=true;
        executor.shutdownNow();
        super.onDestroy();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    private void checkReleases(String requestedVersion) {
        operationRunning = true;
        content.removeAllViews();
        content.addView(Ui.indeterminateLoading(this, UpdateActivity.this.getString(R.string.ui_checking_for_updates_78948b)));
        executor.execute(() -> {
            try {
                List<GitHubRelease> releases = ReleaseUpdateClient.check(getApplicationContext());
                GitHubRelease selected = GitHubReleaseParser.findVersion(releases, requestedVersion);
                if (selected == null) {
                    selected = UpdateChannel.trackedRelease(releases,
                            UpdatePreferences.channel(getApplicationContext()));
                }
                GitHubRelease result = selected;
                postUi(() -> {
                    operationRunning = false;
                    release = result;
                    render();
                });
            } catch (Exception exception) {
                postUi(() -> {
                    operationRunning = false;
                    renderError(ReleaseUpdateClient.safeMessage(exception));
                });
            }
        });
    }

    private void render() {
        content.removeAllViews();
        if (release == null) {
            String error = UpdatePreferences.lastError(this);
            renderError(error.isEmpty()
                    ? UpdateActivity.this.getString(R.string.ui_no_installable_github_releases_are_published_yet_d039af)
                    : error);
            return;
        }
        String installedVersion = UpdatePreferences.installedVersion(this);
        int comparison = ReleaseVersion.compare(release.version, installedVersion);
        boolean irreversible = ReleaseUpdatePolicy.isIrreversible(release.version);
        boolean returnToStable = UpdateChannel.isReturnToStable(release, installedVersion);
        LinearLayout card = Ui.card(this, dark);
        TextView title = Ui.text(this,
                comparison > 0 ? "Codex Meter " + release.version + UpdateActivity.this.getString(R.string.ui_is_available_7b679e)
                        : comparison == 0 ? "Codex Meter " + release.version
                        : returnToStable ? UpdateActivity.this.getString(R.string.ui_return_to_codex_meter_399f2f) + release.version
                        : UpdateActivity.this.getString(R.string.ui_older_release_a0253c) + release.version,
                20, Ui.mainText(dark));
        title.setTypeface(Ui.mediumTypeface(this));
        card.addView(title);
        String detail;
        if (irreversible) {
            detail = DisplayMessages.localize(this, ReleaseUpdatePolicy.irreversibleSummary())
                    + UpdateActivity.this.getString(R.string.ui_installed_b82ebd) + installedVersion;
        } else if (comparison > 0) {
            detail = UpdateActivity.this.getString(R.string.ui_installed_fac976) + installedVersion + (release.prerelease
                    ? UpdateActivity.this.getString(R.string.ui_verified_github_alpha_upgrade_8f3a3e) : UpdateActivity.this.getString(R.string.ui_verified_github_upgrade_306e05));
        } else if (comparison == 0) {
            detail = UpdateActivity.this.getString(R.string.ui_this_version_is_currently_installed_you_can_verify_and__bed169);
        } else if (returnToStable) {
            detail = UpdateActivity.this.getString(R.string.ui_installed_fac976) + installedVersion + UpdateActivity.this.getString(R.string.ui_alpha_builds_share_the_stable_version_code_so_the_newes_812e38);
        } else {
            detail = UpdateActivity.this.getString(R.string.ui_installed_fac976) + installedVersion
                    + UpdateActivity.this.getString(R.string.ui_android_requires_uninstalling_before_this_downgrade_acfb8a);
        }
        TextView summary = Ui.text(this, detail, 14,
                irreversible ? Ui.danger(dark) : Ui.secondaryText(dark));
        LinearLayout.LayoutParams summaryParams = new LinearLayout.LayoutParams(-1, -2);
        summaryParams.setMargins(0, Ui.dp(this, 8), 0, Ui.dp(this, 18));
        card.addView(summary, summaryParams);

        if (irreversible) {
            TextView irreversibleDetail = Ui.text(this, this.getString(R.string.ui_builds_before_codex_meter_1_s_lack_working_in_app_updat_e83f29, ReleaseUpdatePolicy.FIRST_IN_APP_UPDATE_VERSION),
                    13, Ui.secondaryText(dark));
            LinearLayout.LayoutParams irreversibleParams = new LinearLayout.LayoutParams(-1, -2);
            irreversibleParams.setMargins(0, 0, 0, Ui.dp(this, 18));
            card.addView(irreversibleDetail, irreversibleParams);
            Button github = Ui.nativePrimaryButton(this, UpdateActivity.this.getString(R.string.ui_open_on_github_8b81ad));
            github.setOnClickListener(view -> openReleasePage());
            card.addView(github, new LinearLayout.LayoutParams(-1, Ui.dp(this, 60)));
            progress = null;
            status = null;
        } else {
            Button action = Ui.nativePrimaryButton(this, comparison > 0
                    ? getString(R.string.ui_download_and_install_0388ee)
                    : getString(R.string.ui_open_on_github_8b81ad));
            action.setOnClickListener(view -> {
                if (comparison > 0) requestInstall(); else openReleasePage();
            });
            card.addView(action, new LinearLayout.LayoutParams(-1, Ui.dp(this, 60)));

            progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
            progress.setMax(1000);
            progress.setVisibility(View.GONE);
            LinearLayout.LayoutParams progressParams =
                    new LinearLayout.LayoutParams(-1, Ui.dp(this, 8));
            progressParams.setMargins(0, Ui.dp(this, 18), 0, 0);
            card.addView(progress, progressParams);
            status = Ui.text(this, "", 13, Ui.secondaryText(dark));
            status.setVisibility(View.GONE);
            LinearLayout.LayoutParams statusParams = new LinearLayout.LayoutParams(-1, -2);
            statusParams.setMargins(0, Ui.dp(this, 10), 0, 0);
            card.addView(status, statusParams);
        }
        content.addView(card);

        if (!release.notes.isEmpty()) {
            TextView heading = Ui.text(this, UpdateActivity.this.getString(R.string.ui_what_s_new_369702), 15, Ui.secondaryText(dark));
            heading.setTypeface(Ui.mediumTypeface(this));
            LinearLayout.LayoutParams headingParams = new LinearLayout.LayoutParams(-1, -2);
            headingParams.setMargins(Ui.dp(this, 4), Ui.dp(this, 24), 0, Ui.dp(this, 10));
            content.addView(heading, headingParams);
            LinearLayout notesCard = Ui.card(this, dark);
            notesCard.addView(ReleaseNotesUi.create(this, release.notes, dark));
            content.addView(notesCard);
        }

        Button history = Ui.button(this, UpdateActivity.this.getString(R.string.ui_release_history_5044ba), false, dark);
        history.setOnClickListener(view ->
                Ui.startSecondaryActivity(this, ReleaseHistoryActivity.class));
        LinearLayout.LayoutParams historyParams =
                new LinearLayout.LayoutParams(-1, Ui.dp(this, 56));
        historyParams.setMargins(0, Ui.dp(this, 20), 0, 0);
        content.addView(history, historyParams);

        if (startInstallPending && comparison > 0 && !irreversible) {
            startInstallPending = false;
            content.post(this::requestInstall);
        } else {
            startInstallPending = false;
        }
    }

    private void renderError(String message) {
        startInstallPending = false;
        content.removeAllViews();
        LinearLayout card = Ui.card(this, dark);
        TextView title = Ui.text(this, UpdateActivity.this.getString(R.string.ui_update_check_unavailable_2aa80e), 20, Ui.mainText(dark));
        title.setTypeface(Ui.mediumTypeface(this));
        card.addView(title);
        TextView detail = Ui.text(this, message, 14, Ui.secondaryText(dark));
        LinearLayout.LayoutParams detailParams = new LinearLayout.LayoutParams(-1, -2);
        detailParams.setMargins(0, Ui.dp(this, 8), 0, Ui.dp(this, 18));
        card.addView(detail, detailParams);
        Button retry = Ui.nativePrimaryButton(this, UpdateActivity.this.getString(R.string.ui_check_again_e18504));
        retry.setOnClickListener(view -> checkReleases(getIntent().getStringExtra(EXTRA_VERSION)));
        card.addView(retry, new LinearLayout.LayoutParams(-1, Ui.dp(this, 60)));
        content.addView(card);

        Button history = Ui.button(this, UpdateActivity.this.getString(R.string.ui_release_history_5044ba), false, dark);
        history.setOnClickListener(view ->
                Ui.startSecondaryActivity(this, ReleaseHistoryActivity.class));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, Ui.dp(this, 56));
        params.setMargins(0, Ui.dp(this, 20), 0, 0);
        content.addView(history, params);
    }

    private void requestInstall() {
        if (operationRunning || release == null
                || ReleaseVersion.compare(release.version, UpdatePreferences.installedVersion(this)) <= 0
                || ReleaseUpdatePolicy.isIrreversible(release.version)) {
            return;
        }
        UpdatePreferences.setInstallError(this, "");
        if (!canInstallPackages()) {
            waitingForInstallPermission = true;
            new AlertDialog.Builder(this)
                    .setTitle(UpdateActivity.this.getString(R.string.ui_allow_app_installs_47f4fc))
                    .setMessage(UpdateActivity.this.getString(R.string.ui_android_requires_permission_for_codex_meter_to_hand_its_7e7f1a))
                    .setNegativeButton(UpdateActivity.this.getString(R.string.ui_cancel_77dfd2), (dialog, which) ->
                            waitingForInstallPermission = false)
                    .setPositiveButton(UpdateActivity.this.getString(R.string.ui_open_settings_fd7108), (dialog, which) -> {
                        try {
                            startActivity(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                                    Uri.parse("package:" + getPackageName())));
                        } catch (RuntimeException exception) {
                            waitingForInstallPermission = false;
                            Toast.makeText(this, DisplayMessages.localize(this, UpdateActivity.this.getString(R.string.ui_could_not_open_install_permission_settings_82d235)),
                                    Toast.LENGTH_LONG).show();
                        }
                    })
                    .show();
            return;
        }
        beginInstall();
    }

    private boolean canInstallPackages() {
        return getPackageManager().canRequestPackageInstalls();
    }

    private void beginInstall() {
        if (operationRunning || release == null
                || ReleaseVersion.compare(release.version, UpdatePreferences.installedVersion(this)) <= 0
                || ReleaseUpdatePolicy.isIrreversible(release.version)) {
            return;
        }
        operationRunning = true;
        progress.setVisibility(View.VISIBLE);
        progress.setProgress(0);
        setStatus(getString(R.string.update_downloading), Ui.secondaryText(dark));
        executor.execute(() -> {
            try {
                UpdateInstaller.PreparedUpdate prepared = UpdateInstaller.prepare(
                        getApplicationContext(), release, (downloaded, total) -> {
                            if (cancelled || Thread.currentThread().isInterrupted()) {
                                throw new java.util.concurrent.CancellationException("Update cancelled");
                            }
                            postUi(() -> {
                                if (progress != null && total > 0L) {
                                    progress.setProgress((int) Math.min(1000L,
                                            downloaded * 1000L / total));
                                }
                            });
                        });
                if (cancelled || Thread.currentThread().isInterrupted()) {
                    throw new java.util.concurrent.CancellationException("Update cancelled");
                }
                postUi(() -> setStatus(getString(R.string.update_opening_installer),
                        Ui.secondaryText(dark)));
                UpdateInstaller.commit(getApplicationContext(), prepared);
                postUi(() -> {
                    operationRunning = false;
                    setStatus(getString(R.string.update_waiting_for_installer),
                            Ui.secondaryText(dark));
                });
            } catch (Exception exception) {
                UpdatePreferences.setInstallError(getApplicationContext(),
                        safeMessage(exception));
                postUi(() -> {
                    operationRunning = false;
                    progress.setVisibility(View.GONE);
                    setStatus(safeMessage(exception), Ui.danger(dark));
                });
            }
        });
    }

    private void setStatus(String message, int color) {
        if (status == null) {
            return;
        }
        status.setTextColor(color);
        status.setText(message);
        status.setVisibility(message == null || message.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void openReleasePage() {
        String url = release == null ? "" : release.pageUrl;
        if (url.isEmpty() && release != null) {
            url = release.apkUrl;
        }
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (RuntimeException exception) {
            Toast.makeText(this, DisplayMessages.localize(this, UpdateActivity.this.getString(R.string.ui_no_browser_can_open_the_github_release_page_ed52f0)),
                    Toast.LENGTH_LONG).show();
        }
    }

    private static String safeMessage(Exception exception) {
        String message = exception == null ? "" : exception.getMessage();
        if (message == null || message.trim().isEmpty()) {
            message = "The update could not be prepared.";
        }
        return message.length() <= 240 ? message : message.substring(0, 240);
    }

    private void postUi(Runnable action) {
        runOnUiThread(() -> {
            if (!cancelled && !isFinishing() && !isDestroyed()) {
                action.run();
            }
        });
    }
}
