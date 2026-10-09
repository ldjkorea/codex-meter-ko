package dev.bennett.codexmeter;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Advanced release picker with explicit downgrade constraints. */
public final class ReleaseHistoryActivity extends AppCompatActivity {
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private LinearLayout content;
    private boolean dark;

    @Override
    protected void onCreate(Bundle bundle) {
        Ui.applySelectedTheme(this);
        super.onCreate(bundle);
        dark = Ui.isDark(this);
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finish();
            }
        });
        content = Ui.installPage(this, ReleaseHistoryActivity.this.getString(R.string.ui_release_history_5044ba), true).content;
        List<GitHubRelease> cached = UpdatePreferences.releases(this);
        if (cached.isEmpty()) {
            showLoading();
        } else {
            render(cached, false);
        }
        refresh();
    }

    @Override
    protected void onDestroy() {
        executor.shutdownNow();
        super.onDestroy();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    private void showLoading() {
        content.removeAllViews();
        content.addView(Ui.indeterminateLoading(this, ReleaseHistoryActivity.this.getString(R.string.ui_loading_release_history_53dbba)));
    }

    private void refresh() {
        executor.execute(() -> {
            try {
                List<GitHubRelease> releases = ReleaseUpdateClient.check(getApplicationContext());
                runOnUiThread(() -> render(releases, false));
            } catch (Exception exception) {
                List<GitHubRelease> cached = UpdatePreferences.releases(getApplicationContext());
                runOnUiThread(() -> render(cached, true));
            }
        });
    }

    private void render(List<GitHubRelease> releases, boolean failed) {
        content.removeAllViews();
        LinearLayout notice = Ui.card(this, dark);
        TextView current = Ui.text(this, ReleaseHistoryActivity.this.getString(R.string.ui_installed_version_baa271)
                + UpdatePreferences.installedVersion(this), 18,
                Ui.mainText(dark));
        current.setTypeface(Ui.mediumTypeface(this));
        notice.addView(current);
        String note = this.getString(R.string.ui_newer_and_matching_releases_from_codex_meter_1_s_onward_59f0d8, ReleaseUpdatePolicy.FIRST_IN_APP_UPDATE_VERSION, ReleaseUpdatePolicy.FIRST_IN_APP_UPDATE_VERSION);
        TextView detail = Ui.text(this, note, 13, Ui.secondaryText(dark));
        LinearLayout.LayoutParams detailParams = new LinearLayout.LayoutParams(-1, -2);
        detailParams.setMargins(0, Ui.dp(this, 8), 0, 0);
        notice.addView(detail, detailParams);
        content.addView(notice);

        if (failed) {
            TextView warning = Ui.text(this,
                    UpdatePreferences.lastError(this), 13, Ui.danger(dark));
            LinearLayout.LayoutParams warningParams = new LinearLayout.LayoutParams(-1, -2);
            warningParams.setMargins(Ui.dp(this, 4), Ui.dp(this, 16), 0, 0);
            content.addView(warning, warningParams);
        }
        if (releases == null || releases.isEmpty()) {
            LinearLayout empty = Ui.card(this, dark);
            TextView title = Ui.text(this, ReleaseHistoryActivity.this.getString(R.string.ui_no_installable_releases_yet_ec6ca4), 18,
                    Ui.mainText(dark));
            title.setTypeface(Ui.mediumTypeface(this));
            empty.addView(title);
            TextView detailEmpty = Ui.text(this,
                    ReleaseHistoryActivity.this.getString(R.string.ui_github_currently_has_no_published_release_containing_bo_b6552e), 14, Ui.secondaryText(dark));
            LinearLayout.LayoutParams emptyParams = new LinearLayout.LayoutParams(-1, -2);
            emptyParams.setMargins(0, Ui.dp(this, 8), 0, 0);
            empty.addView(detailEmpty, emptyParams);
            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(-1, -2);
            cardParams.setMargins(0, Ui.dp(this, 20), 0, 0);
            content.addView(empty, cardParams);
            return;
        }

        TextView heading = Ui.text(this, ReleaseHistoryActivity.this.getString(R.string.ui_published_versions_bed379), 15, Ui.secondaryText(dark));
        heading.setTypeface(Ui.mediumTypeface(this));
        LinearLayout.LayoutParams headingParams = new LinearLayout.LayoutParams(-1, -2);
        headingParams.setMargins(Ui.dp(this, 4), Ui.dp(this, 24), 0, Ui.dp(this, 10));
        content.addView(heading, headingParams);
        for (GitHubRelease release : releases) {
            addRelease(release);
        }
    }

    private void addRelease(GitHubRelease release) {
        String installedVersion = UpdatePreferences.installedVersion(this);
        int comparison = ReleaseVersion.compare(release.version, installedVersion);
        boolean irreversible = ReleaseUpdatePolicy.isIrreversible(release.version);
        boolean returnToStable = UpdateChannel.isReturnToStable(release, installedVersion);
        LinearLayout card = Ui.card(this, dark);
        String suffix = release.prerelease ? ReleaseHistoryActivity.this.getString(R.string.ui_prerelease_066fca)
                : irreversible ? ReleaseHistoryActivity.this.getString(R.string.ui_irreversible_0da9dd)
                : comparison > 0 ? ReleaseHistoryActivity.this.getString(R.string.ui_update_83ea30)
                : comparison == 0 ? ReleaseHistoryActivity.this.getString(R.string.ui_installed_d73d01) : ReleaseHistoryActivity.this.getString(R.string.ui_older_09f4a5);
        TextView title = Ui.text(this, release.name, 18, Ui.mainText(dark));
        title.setTypeface(Ui.mediumTypeface(this));
        card.addView(title);
        String published = release.publishedAt.length() >= 10
                ? release.publishedAt.substring(0, 10) : ReleaseHistoryActivity.this.getString(R.string.ui_unknown_date_0ad240);
        TextView summary = Ui.text(this, "v" + release.version + suffix + " · " + published,
                13, irreversible ? Ui.danger(dark) : Ui.secondaryText(dark));
        LinearLayout.LayoutParams summaryParams = new LinearLayout.LayoutParams(-1, -2);
        summaryParams.setMargins(0, Ui.dp(this, 6), 0, Ui.dp(this, irreversible ? 8 : 14));
        card.addView(summary, summaryParams);

        if (irreversible) {
            TextView irreversibleNote = Ui.text(this,
                    DisplayMessages.localize(this, ReleaseUpdatePolicy.irreversibleSummary())
                            + ReleaseHistoryActivity.this.getString(R.string.ui_update_manually_from_the_github_release_page_886d94),
                    13, Ui.secondaryText(dark));
            LinearLayout.LayoutParams irreversibleParams = new LinearLayout.LayoutParams(-1, -2);
            irreversibleParams.setMargins(0, 0, 0, Ui.dp(this, 14));
            card.addView(irreversibleNote, irreversibleParams);
        }

        if (!release.notes.isEmpty()) {
            TextView notesHeading = Ui.text(this, ReleaseHistoryActivity.this.getString(R.string.ui_what_s_new_369702), 13, Ui.secondaryText(dark));
            notesHeading.setTypeface(Ui.mediumTypeface(this));
            LinearLayout.LayoutParams notesHeadingParams = new LinearLayout.LayoutParams(-1, -2);
            notesHeadingParams.setMargins(0, 0, 0, Ui.dp(this, 8));
            card.addView(notesHeading, notesHeadingParams);
            card.addView(ReleaseNotesUi.create(this, release.notes, dark));
            LinearLayout.LayoutParams spacer = new LinearLayout.LayoutParams(-1, Ui.dp(this, 14));
            android.widget.Space space = new android.widget.Space(this);
            card.addView(space, spacer);
        }

        if (irreversible) {
            Button github = Ui.nativePrimaryButton(this, ReleaseHistoryActivity.this.getString(R.string.ui_open_on_github_8b81ad));
            github.setOnClickListener(view -> openUrl(release.pageUrl));
            card.addView(github, new LinearLayout.LayoutParams(-1, Ui.dp(this, 54)));
            Button details = Ui.button(this, ReleaseHistoryActivity.this.getString(R.string.ui_view_release_details_d71096), false, dark);
            details.setOnClickListener(view -> startActivity(new Intent(this, UpdateActivity.class)
                    .putExtra(UpdateActivity.EXTRA_VERSION, release.version)));
            LinearLayout.LayoutParams detailsParams =
                    new LinearLayout.LayoutParams(-1, Ui.dp(this, 54));
            detailsParams.setMargins(0, Ui.dp(this, 10), 0, 0);
            card.addView(details, detailsParams);
        } else {
            Button action = Ui.button(this,
                    returnToStable ? ReleaseHistoryActivity.this.getString(R.string.ui_view_stable_return_61bcbb)
                            : comparison < 0 ? ReleaseHistoryActivity.this.getString(R.string.ui_view_downgrade_85bf39) : comparison == 0 ? ReleaseHistoryActivity.this.getString(R.string.ui_view_reinstall_dacfdb)
                            : ReleaseHistoryActivity.this.getString(R.string.ui_view_update_6fcad3), comparison > 0 || returnToStable, dark);
            action.setOnClickListener(view -> startActivity(new Intent(this, UpdateActivity.class)
                    .putExtra(UpdateActivity.EXTRA_VERSION, release.version)));
            card.addView(action, new LinearLayout.LayoutParams(-1, Ui.dp(this, 54)));
        }
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(-1, -2);
        cardParams.setMargins(0, 0, 0, Ui.dp(this, 14));
        content.addView(card, cardParams);
    }

    private void openUrl(String url) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (RuntimeException exception) {
            Toast.makeText(this, DisplayMessages.localize(this, ReleaseHistoryActivity.this.getString(R.string.ui_no_browser_can_open_the_github_release_page_ed52f0)),
                    Toast.LENGTH_LONG).show();
        }
    }
}
