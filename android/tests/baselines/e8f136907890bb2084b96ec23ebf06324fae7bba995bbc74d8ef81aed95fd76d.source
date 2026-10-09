package dev.bennett.codexmeter;

import android.content.Context;

/** Serialize account boundaries with quota and reset-credit requests. Call off the UI thread. */
final class AccountSession {
    private AccountSession() { }

    static AuthTokens signOut(Context context) {
        synchronized (UsageApi.NETWORK_LOCK) {
            AuthTokens previous = SecureTokenStore.load(context);
            SecureTokenStore.clear(context);
            AppPreferences.clearSnapshot(context);
            AppPreferences.setOAuthPending(context, false, "");
            RefreshScheduler.cancelAll(context);
            ResetAlertScheduler.cancelAll(context);
            WidgetRenderer.updateAll(context);
            return previous;
        }
    }

    static void install(Context context, AuthTokens tokens, boolean imported) throws Exception {
        synchronized (UsageApi.NETWORK_LOCK) {
            AuthTokens previous = SecureTokenStore.load(context);
            // Unknown identity cannot prove that old history belongs to this account.
            boolean sameAccount = previous != null && !previous.accountId.isEmpty()
                    && previous.accountId.equals(tokens.accountId);
            if (imported || !sameAccount) AppPreferences.clearSnapshot(context);
            SecureTokenStore.save(context, tokens);
            AppPreferences.setOAuthPending(context, false, "");
        }
    }

    static void clearHistory(Context context) {
        synchronized (UsageApi.NETWORK_LOCK) {
            AppPreferences.clearUsageHistory(context);
        }
    }
}
