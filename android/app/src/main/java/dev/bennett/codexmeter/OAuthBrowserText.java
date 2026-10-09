package dev.bennett.codexmeter;

import android.content.Context;
import android.content.res.Configuration;
import java.util.Locale;

/** Replaces only visible copy in the original callback page; URLs and scripts stay intact. */
final class OAuthBrowserText {
    private OAuthBrowserText() {}

    static String render(Context context, String message, boolean success, String appLink) {
        String page = OAuthBrowserPage.render(DisplayMessages.localize(context, message), success, appLink);
        Configuration configuration = new Configuration(context.getResources().getConfiguration());
        configuration.setLocale(Locale.ENGLISH);
        Context english = context.createConfigurationContext(configuration);
        for (int id : new int[] {R.string.ui_you_re_connected_7afaca, R.string.ui_let_s_try_that_again_9512e3, R.string.ui_sign_in_complete_8ae00b, R.string.ui_sign_in_needs_attention_5ecb53, R.string.ui_open_codex_meter_a55de8, R.string.ui_back_to_codex_meter_83703a, R.string.ui_returning_to_the_app_automatically_33e7dc, R.string.ui_return_to_the_app_to_restart_secure_sign_in_298271}) {
            page = page.replace(">" + english.getString(id) + "<",
                    ">" + OAuthBrowserPage.htmlEscape(context.getString(id)) + "<");
        }
        return page.replace("<html lang=\"en\">", "<html lang=\""
                + context.getString(R.string.ui_en_094b0f) + "\">");
    }
}
