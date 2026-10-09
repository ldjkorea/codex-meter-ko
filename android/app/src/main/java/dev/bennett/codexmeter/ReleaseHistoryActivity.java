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
    private final java.util.Set<String> expanded=new java.util.HashSet<>();

    @Override
    protected void onCreate(Bundle bundle) {
        Ui.applySelectedTheme(this);
        super.onCreate(bundle);
        dark = Ui.isDark(this);
        if(bundle!=null&&bundle.getStringArrayList("expanded_versions")!=null)expanded.addAll(bundle.getStringArrayList("expanded_versions"));
        else expanded.add(AppConstants.VERSION_NAME);
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finish();
            }
        });
        content = Ui.installPage(this, ReleaseHistoryActivity.this.getString(R.string.ui_release_history_5044ba), true).content;
        List<GitHubRelease> cached = UpdatePreferences.releases(this);
        render(cached, false);
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

    @Override protected void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        state.putStringArrayList("expanded_versions",new java.util.ArrayList<>(expanded));
    }

    private void render(List<GitHubRelease> releases, boolean failed) {
        if(isDestroyed()||isFinishing())return;
        content.removeAllViews();
        LinearLayout notice=Ui.card(this,dark);
        notice.addView(LedgerUi.heading(this,getString(R.string.ui_installed_version_baa271)+UpdatePreferences.installedVersion(this),dark));
        Ui.addSpacer(notice,8);
        notice.addView(LedgerUi.caption(this,getString(R.string.polish_history_intro),dark));
        if(failed){Ui.addSpacer(notice,8);notice.addView(LedgerUi.caption(this,getString(R.string.polish_history_offline),dark));}
        content.addView(notice);Ui.addSpacer(content,16);
        List<ReleaseCatalog.Entry> archive=ReleaseCatalog.all(this);
        java.util.Set<String> known=new java.util.HashSet<>();
        for(ReleaseCatalog.Entry entry:archive)known.add(entry.version);
        // Future verified releases retain the existing secure update flow.
        for(GitHubRelease release:releases){
            String base=release.version.replaceFirst("^v","").replaceFirst("[-+].*$","");
            if(!known.contains(base))addRelease(release);
        }
        for(ReleaseCatalog.Entry entry:archive){
            LinearLayout card=Ui.card(this,dark);
            Button header=LedgerUi.action(this,"v"+entry.version+" · "+entry.title,false,dark,()->{});
            header.setGravity(android.view.Gravity.START|android.view.Gravity.CENTER_VERTICAL);
            header.setTextSize(17);header.setMinHeight(Ui.dp(this,56));
            card.addView(header);
            TextView origin=LedgerUi.caption(this,getString(entry.origin.equals("upstream")?R.string.polish_history_upstream:R.string.polish_history_korean),dark);
            card.addView(origin);
            LinearLayout body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);Ui.addSpacer(body,14);
            body.addView(ReleaseNotesUi.create(this,entry.notes,dark));
            if(!entry.original.isEmpty()){
                TextView original=ReleaseNotesUi.create(this,entry.original,dark);original.setVisibility(android.view.View.GONE);
                body.addView(LedgerUi.action(this,getString(R.string.polish_original_notes),false,dark,()->original.setVisibility(original.getVisibility()==android.view.View.VISIBLE?android.view.View.GONE:android.view.View.VISIBLE)));
                body.addView(original);
            }
            for(GitHubRelease release:releases){
                if(release.version.replaceFirst("^v","").replaceFirst("[-+].*$","").equals(entry.version)
                        &&ReleaseVersion.compare(release.version,UpdatePreferences.installedVersion(this))>0){
                    Ui.addSpacer(body,14);
                    body.addView(LedgerUi.action(this,getString(R.string.ui_view_update_6fcad3),true,dark,()->startActivity(new Intent(this,UpdateActivity.class).putExtra(UpdateActivity.EXTRA_VERSION,release.version))));
                    break;
                }
            }
            body.setVisibility(expanded.contains(entry.version)?android.view.View.VISIBLE:android.view.View.GONE);
            header.setContentDescription(header.getText()+" · "+getString(expanded.contains(entry.version)?R.string.polish_collapse:R.string.polish_expand));
            header.setOnClickListener(v->{boolean open=!expanded.contains(entry.version);if(open)expanded.add(entry.version);else expanded.remove(entry.version);
                body.setVisibility(open?android.view.View.VISIBLE:android.view.View.GONE);
                header.setContentDescription(header.getText()+" · "+getString(open?R.string.polish_collapse:R.string.polish_expand));});
            card.addView(body);content.addView(card);Ui.addSpacer(content,12);
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
