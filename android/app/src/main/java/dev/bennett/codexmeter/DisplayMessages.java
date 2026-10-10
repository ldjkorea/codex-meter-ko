package dev.bennett.codexmeter;

import android.content.Context;
import android.content.res.Configuration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/** Localizes known messages at the display boundary, leaving stored/server errors intact. */
final class DisplayMessages {
    private static Map<String, Integer> englishMessages;
    private DisplayMessages() {}

    static String localize(Context context, String message) {
        if (message == null) return null;
        Integer id = messages(context).get(message);
        if (id != null) return context.getString(id);
        java.util.regex.Matcher reset = java.util.regex.Pattern.compile(
                "Reset applied to (\\d+) usage window(s?)\\.(.*)").matcher(message);
        if (reset.matches()) {
            String result = reset.group(2).isEmpty() ? context.getString(R.string.ui_reset_applied_to_1_s_usage_window_037f32, reset.group(1)) : context.getString(R.string.ui_reset_applied_to_1_s_usage_windows_53c377, reset.group(1));
            String warning = reset.group(3);
            return result + (warning.isEmpty() ? "" : " " + localize(context, warning.trim()));
        }
        java.util.regex.Matcher http = java.util.regex.Pattern.compile(
                "(Authentication|Usage refresh) failed \\(HTTP (\\d+)\\)\\.").matcher(message);
        if (http.matches()) return "Authentication".equals(http.group(1))
                ? context.getString(R.string.ui_authentication_failed_http_1_s_e0cfb9, http.group(2))
                : context.getString(R.string.ui_usage_refresh_failed_http_1_s_74f0ea, http.group(2));
        if (message.startsWith("Sign-in failed: ")) return context.getString(R.string.ui_sign_in_failed_1_s_268920, message.substring(16));
        return message;
    }

    private static synchronized Map<String, Integer> messages(Context context) {
        if (englishMessages == null) {
            Configuration configuration = new Configuration(context.getResources().getConfiguration());
            configuration.setLocale(Locale.ENGLISH);
            Context english = context.createConfigurationContext(configuration);
            Map<String, Integer> values = new HashMap<>();
            for (int id : new int[] {
                    R.string.ui_the_authorization_server_returned_incomplete_credential_50045d,
                    R.string.ui_server_response_was_unexpectedly_large_82861f,
                    R.string.ui_sign_in_cancelled_24d3d6,
                    R.string.ui_already_signed_in_2c7ee9,
                    R.string.ui_preparing_secure_sign_in_78f0c0,
                    R.string.ui_complete_sign_in_in_your_browser_fb6f2a,
                    R.string.ui_sign_in_timed_out_start_again_from_the_app_abf8e5,
                    R.string.ui_the_sign_in_state_did_not_match_return_to_codex_meter_a_39f3f1,
                    R.string.ui_the_authorization_response_did_not_include_a_code_c4d910,
                    R.string.ui_sign_in_failed_because_no_authorization_code_was_return_9ae3ba,
                    R.string.ui_securing_your_chatgpt_session_5b6b31,
                    R.string.ui_your_chatgpt_account_is_connected_returning_to_codex_me_463a34,
                    R.string.ui_signed_in_successfully_d4c69c,
                    R.string.ui_loading_codex_usage_0a8071,
                    R.string.ui_signed_in_usage_can_be_refreshed_from_the_app_0fe32a,
                    R.string.ui_could_not_open_the_local_oauth_callback_port_1455_or_14_c009da,
                    R.string.ui_the_browser_callback_was_empty_5f9d17,
                    R.string.ui_codex_meter_sign_in_2b155d,
                    R.string.ui_cancel_77dfd2,
                    R.string.ui_sign_in_could_not_be_completed_b22cfc,
                    R.string.ui_codex_usage_access_was_denied_for_this_account_648526,
                    R.string.ui_the_codex_usage_endpoint_is_unavailable_or_has_changed_444b7d,
                    R.string.ui_openai_returned_no_recognizable_codex_usage_data_43401f,
                    R.string.ui_usage_was_received_but_it_could_not_be_saved_on_this_de_358c80,
                    R.string.ui_sign_in_to_chatgpt_first_6abe9d,
                    R.string.ui_reset_credit_refresh_failed_48f3a6,
                    R.string.ui_could_not_persist_encrypted_credentials_539f77,
                    R.string.ui_could_not_load_codex_reset_credits_29912a,
                    R.string.ui_reset_credits_were_received_but_could_not_be_saved_on_t_28996f,
                    R.string.ui_could_not_apply_the_codex_reset_4535e7,
                    R.string.ui_the_reset_succeeded_but_the_new_usage_values_could_not__461ee6,
                    R.string.ui_transfer_file_is_empty_8e6a19,
                    R.string.ui_not_a_codex_meter_transfer_file_2aaafe,
                    R.string.ui_transfer_file_does_not_contain_any_sections_afbab7,
                    R.string.ui_app_settings_d69225,
                    R.string.ui_notifications_753a22,
                    R.string.ui_now_bar_a96362,
                    R.string.ui_authentication_ee1acf,
                    R.string.ui_theme_refresh_updates_and_default_widget_look_8e2b85,
                    R.string.ui_low_usage_alerts_and_reset_credit_reminders_e3849a,
                    R.string.ui_display_mode_percentage_mode_and_auto_start_43c017,
                    R.string.ui_chatgpt_sign_in_tokens_sensitive_19bc11,
                    R.string.ui_select_at_least_one_section_to_export_2dcfc6,
                    R.string.ui_no_chatgpt_authentication_is_saved_on_this_device_to_ex_7be855,
                    R.string.ui_export_target_is_incomplete_71bc17,
                    R.string.ui_could_not_open_the_export_file_for_writing_3dd7ee,
                    R.string.ui_import_source_is_incomplete_90fa35,
                    R.string.ui_could_not_open_the_import_file_for_reading_a8cc04,
                    R.string.ui_transfer_file_is_too_large_81b1f6,
                    R.string.ui_this_file_has_no_app_settings_to_import_602e0a,
                    R.string.ui_this_file_has_no_notification_settings_to_import_e84344,
                    R.string.ui_this_file_has_no_now_bar_settings_to_import_dbcd62,
                    R.string.ui_this_file_has_no_authentication_to_import_186273,
                    R.string.ui_select_at_least_one_section_to_import_8df3bf,
                    R.string.ui_imported_authentication_could_not_refresh_usage_d9f3eb,
                    R.string.ui_imported_authentication_is_incomplete_or_invalid_2f91b3,
                    R.string.ui_codex_usage_reset_applied_400a47,
                    R.string.ui_there_is_no_used_codex_allowance_to_reset_right_now_c0ad00,
                    R.string.ui_no_reset_credit_is_currently_available_f2f705,
                    R.string.ui_that_reset_request_was_already_redeemed_8e7651,
                    R.string.ui_openai_returned_an_unrecognized_reset_result_64305e,
                    R.string.ui_irreversible_manual_github_update_required_365c64,
                    R.string.ui_codex_meter_7332c0,
                    R.string.ui_codex_2c5d5b,
                    R.string.ui_the_operation_failed_8a6308,
                    R.string.ui_the_reset_could_not_be_applied_473262,
                    R.string.ui_the_update_could_not_be_prepared_3c9719,
                    R.string.ui_every_hour_6a76cd,
                    R.string.ui_every_6_hours_d146d2,
                    R.string.ui_every_12_hours_3c61de,
                    R.string.ui_weekly_158f3d,
                    R.string.ui_daily_728298,
                    R.string.ui_important_this_file_contains_chatgpt_authentication_tok_5b29bb,
            }) {
                values.put(english.getString(id), id);
                // Stored errors and protected auth code may still use the original product name.
                values.put(english.getString(id).replace("GPT HUD", "Codex Meter"), id);
            }
            englishMessages = values;
        }
        return englishMessages;
    }
}
