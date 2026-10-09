package dev.bennett.codexmeter;

import android.widget.Spinner;

/* JADX INFO: loaded from: classes.dex */
final class WidgetOptionCatalog {
    static String[] THEME_LABELS(android.content.Context context) {
        return context.getResources().getStringArray(R.array.widget_theme_labels);
    }
    static final String[] THEME_VALUES = {WidgetOptions.THEME_SYSTEM, WidgetOptions.THEME_DARK, WidgetOptions.THEME_LIGHT};
    static String[] SURFACE_LABELS(android.content.Context context) {
        return context.getResources().getStringArray(R.array.widget_surface_labels);
    }
    static final String[] SURFACE_VALUES = {WidgetOptions.SURFACE_ONE_UI};
    static String[] STYLE_LABELS(android.content.Context context) {
        return context.getResources().getStringArray(R.array.widget_style_labels);
    }
    static final String[] STYLE_VALUES = {WidgetOptions.STYLE_AUTO, WidgetOptions.STYLE_DIALS,
            WidgetOptions.STYLE_BARS};
    static String[] DENSITY_LABELS(android.content.Context context) {
        return context.getResources().getStringArray(R.array.widget_density_labels);
    }
    static final String[] DENSITY_VALUES = {"auto", "compact", WidgetOptions.DENSITY_COMFORTABLE};
    static String[] GRAPHIC_LABELS(android.content.Context context) {
        return context.getResources().getStringArray(R.array.widget_graphic_labels);
    }
    static final String[] GRAPHIC_VALUES = {"auto", WidgetOptions.GRAPHIC_LARGE, WidgetOptions.GRAPHIC_MAX};
    static String[] ACCENT_LABELS(android.content.Context context) {
        return context.getResources().getStringArray(R.array.widget_accent_labels);
    }
    static final String[] ACCENT_VALUES = {WidgetOptions.ACCENT_MINT, WidgetOptions.ACCENT_BLUE, WidgetOptions.ACCENT_AMBER, WidgetOptions.ACCENT_VIOLET, WidgetOptions.ACCENT_ROSE, WidgetOptions.ACCENT_CYAN, WidgetOptions.ACCENT_LIME, WidgetOptions.ACCENT_MONO};
    /** One UI 7-style discrete fill strengths when the widget background is enabled. */
    static String[] OPACITY_LABELS(android.content.Context context) {
        return context.getResources().getStringArray(R.array.widget_opacity_labels);
    }
    static final int[] OPACITY_VALUES = WidgetOptions.OPACITY_LEVELS;
    static String[] RESET_LABELS(android.content.Context context) {
        return context.getResources().getStringArray(R.array.widget_reset_labels);
    }
    static final String[] RESET_VALUES = {WidgetOptions.RESET_ABSOLUTE, WidgetOptions.RESET_RELATIVE, "both", WidgetOptions.RESET_HIDDEN};
    static String[] METRIC_LABELS(android.content.Context context) {
        return context.getResources().getStringArray(R.array.widget_metric_labels);
    }
    static final String[] METRIC_VALUES = {WidgetOptions.METRIC_BOTH,
            WidgetOptions.METRIC_FIVE_HOUR, WidgetOptions.METRIC_WEEKLY};
    static String[] DISPLAY_LABELS(android.content.Context context) {
        return context.getResources().getStringArray(R.array.widget_display_labels);
    }
    static final String[] DISPLAY_VALUES = {WidgetOptions.DISPLAY_REMAINING, WidgetOptions.DISPLAY_USED};

    private WidgetOptionCatalog() {
    }

    static void selectString(Spinner spinner, String[] values, String selected) {
        for (int i = 0; i < values.length; i++) {
            if (values[i].equals(selected)) {
                spinner.setSelection(i);
                return;
            }
        }
        spinner.setSelection(0);
    }
}
