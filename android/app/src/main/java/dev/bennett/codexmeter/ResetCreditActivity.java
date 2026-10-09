package dev.bennett.codexmeter;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import androidx.appcompat.app.AppCompatActivity;
import dev.oneuiproject.oneui.widget.CardItemView;
import dev.oneuiproject.oneui.widget.RoundedLinearLayout;

/* JADX INFO: loaded from: classes.dex */
public final class ResetCreditActivity extends AppCompatActivity {
    private LinearLayout content;
    private boolean dark;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private int expiryNotificationId = -1;
    private Button useButton;

    @Override // android.app.Activity
    protected void onCreate(Bundle bundle) {
        Ui.applySelectedTheme(this);
        super.onCreate(bundle);
        this.dark = Ui.isDark(this);
        this.content = Ui.installPage(this, ResetCreditActivity.this.getString(R.string.ui_codex_reset_77be31), true).content;
        rebuild();
        refreshDetailsIfNeeded();
        if (bundle == null) {

        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        rebuild();

    }

    @Override // android.app.Activity
    protected void onDestroy() {
        this.executor.shutdownNow();
        super.onDestroy();
    }

    public void rebuild() {
        this.content.removeAllViews();
        ResetCreditsSnapshot snapshot = AppPreferences.loadResetCredits(this);
        int available = snapshot == null ? 0 : snapshot.availableCount;
        long now = System.currentTimeMillis();
        List<RateLimitResetCredit> availableCredits = snapshot == null
                ? Collections.emptyList()
                : snapshot.availableCreditsByExpiry(now);
        long nextExpiry = snapshot == null ? 0L : snapshot.nextExpiryMillis(now);

        this.content.addView(Ui.separator(this, ResetCreditActivity.this.getString(R.string.ui_available_credits_18001d)));
        RoundedLinearLayout summaryCard = Ui.seslRowCard(this, this.dark);
        summaryCard.addView(Ui.actionRow(
                this,
                available <= 0
                        ? ResetCreditActivity.this.getString(R.string.ui_no_resets_available_d6246f)
                        : (available == 1 ? ResetCreditActivity.this.getString(R.string.ui_1_reset_available_6972a9) : ResetCreditActivity.this.getString(R.string.ui_1_d_resets_available_8cbcb5, available)),
                summaryText(available, nextExpiry, now),
                R.drawable.ic_oui_battery,
                null));
        this.content.addView(summaryCard);

        this.content.addView(Ui.separator(this, ResetCreditActivity.this.getString(R.string.ui_credit_expirations_33fb9f)));
        RoundedLinearLayout expirations = Ui.seslRowCard(this, this.dark);
        addCreditExpirations(expirations, availableCredits, available, now);
        this.content.addView(expirations);

        String visibleResetCreditsError = AppPreferences.getVisibleResetCreditsError(this);
        if (!visibleResetCreditsError.isEmpty()) {
            Ui.addSpacer(this.content, 12);
            RoundedLinearLayout errorCard = Ui.seslCard(this, this.dark);
            errorCard.addView(Ui.text(this, visibleResetCreditsError, 13.0f,
                    Ui.danger(this.dark)));
            this.content.addView(errorCard);
        }

        Ui.addSpacer(content,16);content.addView(LedgerUi.caption(this,getString(R.string.v3_reset_readonly),dark));
    }

    private String summaryText(int available, long nextExpiry, long now) {
        if (nextExpiry > 0L) {
            return ResetCreditActivity.this.getString(R.string.ui_next_expires_1_s_fbb454, UsageFormat.absolute(this, nextExpiry, now))
                    + " · " + UsageFormat.relative(ResetCreditActivity.this, nextExpiry, now);
        }
        if (available > 0) {
            return ResetCreditActivity.this.getString(R.string.ui_openai_will_choose_an_eligible_credit_8a9cf1);
        }
        return ResetCreditActivity.this.getString(R.string.ui_no_reset_credit_is_currently_available_4e348f);
    }

    private void addCreditExpirations(RoundedLinearLayout card,
            List<RateLimitResetCredit> credits, int availableCount, long nowMillis) {
        for (int index = 0; index < credits.size(); index++) {
            RateLimitResetCredit credit = credits.get(index);
            String titleText = credit.title.trim().isEmpty()
                    ? ResetCreditActivity.this.getString(R.string.ui_reset_credit_1_d_37f5d4, index + 1) : credit.title.trim();
            if (index == 0 && credit.expiresAtMillis > 0L) {
                titleText = titleText + ResetCreditActivity.this.getString(R.string.ui_next_7271e9);
            }
            String expiryText = credit.expiresAtMillis > 0L
                    ? UsageFormat.absolute(this, credit.expiresAtMillis, nowMillis)
                            + " · " + UsageFormat.relative(ResetCreditActivity.this, credit.expiresAtMillis, nowMillis)
                    : ResetCreditActivity.this.getString(R.string.ui_expiration_unavailable_7fca09);
            CardItemView row = Ui.actionRow(this, titleText, expiryText, 0, null);
            row.setShowTopDivider(index > 0);
            card.addView(row);
        }

        int missingCount = Math.max(0, availableCount - credits.size());
        if (missingCount > 0) {
            String missingText = credits.isEmpty()
                    ? ResetCreditActivity.this.getString(R.string.ui_expiration_details_are_not_available_yet_730fcd)
                    : ResetCreditActivity.this.getString(R.string.ui_1_d_additional_credit_2_s_without_expiration_details_606935, missingCount, missingCount == 1 ? "" : ResetCreditActivity.this.getString(R.string.ui_s_a0f149));
            CardItemView missing = Ui.actionRow(this, ResetCreditActivity.this.getString(R.string.ui_more_credits_272f4e), missingText, 0, null);
            missing.setShowTopDivider(!credits.isEmpty());
            card.addView(missing);
        } else if (availableCount == 0) {
            card.addView(Ui.actionRow(this, ResetCreditActivity.this.getString(R.string.ui_no_available_credits_151c01),
                    ResetCreditActivity.this.getString(R.string.ui_earn_credits_from_chatgpt_codex_759807), 0, null));
        }
    }

    private void refreshDetailsIfNeeded() {
        ResetCreditsSnapshot resetCreditsSnapshotLoadResetCredits = AppPreferences.loadResetCredits(this);
        long now = System.currentTimeMillis();
        long jMax = resetCreditsSnapshotLoadResetCredits == null ? Long.MAX_VALUE : Math.max(0L, now - resetCreditsSnapshotLoadResetCredits.fetchedAtMillis);
        boolean missingDetails = resetCreditsSnapshotLoadResetCredits != null
                && resetCreditsSnapshotLoadResetCredits.availableCount > 0
                && resetCreditsSnapshotLoadResetCredits.availableCreditsByExpiry(now).size()
                        < resetCreditsSnapshotLoadResetCredits.availableCount;
        if (SecureTokenStore.isSignedIn(this) && (jMax >= 300000 || missingDetails)) {
            final Context applicationContext = getApplicationContext();
            this.executor.execute(new Runnable() { // from class: dev.bennett.codexmeter.ResetCreditActivity.4
                @Override // java.lang.Runnable
                public void run() {
                    try {
                        ResetCreditApi.refreshAndCache(applicationContext);
                        ResetCreditActivity.this.runOnUiThread(new Runnable() { // from class: dev.bennett.codexmeter.ResetCreditActivity.4.1
                            @Override // java.lang.Runnable
                            public void run() {
                                ResetCreditActivity.this.rebuild();
                            }
                        });
                    } catch (Exception e) {
                        AppPreferences.setResetCreditsError(applicationContext, ResetCreditActivity.safeMessage(e));
                    }
                }
            });
        }
    }

    public static String safeMessage(Exception exc) {
        String message = exc == null ? "" : exc.getMessage();
        if (message == null || message.trim().isEmpty()) {
            return "The reset could not be applied.";
        }
        String strTrim = message.trim();
        return strTrim.length() > 240 ? strTrim.substring(0, 240) : strTrim;
    }
}
