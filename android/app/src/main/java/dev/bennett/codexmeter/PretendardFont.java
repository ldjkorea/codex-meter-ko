package dev.bennett.codexmeter;

import android.content.Context;
import android.graphics.Typeface;
import androidx.core.content.res.ResourcesCompat;

/** The app's redistributable UI typography (Pretendard SIL OFL 1.1). */
final class PretendardFont {
    private static Typeface regular, medium, semibold, bold;

    private PretendardFont() {}

    private static Typeface load(Context context, int resId, Typeface fallback) {
        if (context == null) return fallback;
        try {
            Typeface font = ResourcesCompat.getFont(context, resId);
            return font != null ? font : fallback;
        } catch (RuntimeException ex) {
            return fallback;
        }
    }

    static synchronized Typeface regular(Context context) {
        if (regular == null) regular = load(context, R.font.pretendard_regular, Typeface.DEFAULT);
        return regular;
    }

    static synchronized Typeface medium(Context context) {
        if (medium == null) medium = load(context, R.font.pretendard_medium, Typeface.DEFAULT);
        return medium;
    }

    static synchronized Typeface semibold(Context context) {
        if (semibold == null) semibold = load(context, R.font.pretendard_semibold, Typeface.DEFAULT_BOLD);
        return semibold;
    }

    static synchronized Typeface bold(Context context) {
        if (bold == null) bold = load(context, R.font.pretendard_bold, Typeface.DEFAULT_BOLD);
        return bold;
    }
}
