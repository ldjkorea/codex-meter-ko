package dev.bennett.codexmeter;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.Locale;

/** Presentation components confined to the ledger screens; existing surfaces retain their styles. */
final class LedgerUi {
    private LedgerUi() { }

    static int muted(boolean dark) { return dark ? Color.rgb(168, 173, 181) : Color.rgb(88, 94, 104); }
    static int tint(boolean dark) { return dark ? Color.rgb(23, 25, 28) : Color.rgb(249, 249, 250); }
    static String number(double value) { return UsagePrecision.number(value); }
    static boolean stacked(Context context) {
        android.content.res.Configuration config = context.getResources().getConfiguration();
        return config.fontScale > 1.25f || config.screenWidthDp < 360;
    }
    static GradientDrawable shape(Context context, int color, int radius) {
        GradientDrawable result = new GradientDrawable();
        result.setColor(color); result.setCornerRadius(Ui.dp(context, radius)); return result;
    }
    static TextView heading(Context context, String text, boolean dark) {
        TextView view = Ui.text(context, text, 19, Ui.mainText(dark));
        view.setTypeface(PretendardFont.semibold(context)); return view;
    }
    static TextView caption(Context context, String text, boolean dark) {
        return Ui.text(context, text, 13, muted(dark));
    }
    static TextView badge(Context context, String text, boolean dark, boolean warning) {
        TextView view = Ui.text(context, text, 12, warning ? Ui.mainText(dark) : muted(dark));
        view.setPadding(Ui.dp(context, 10), Ui.dp(context, 6), Ui.dp(context, 10), Ui.dp(context, 6));
        view.setBackground(shape(context, Ui.controlSurface(context, dark), 9));
        view.setLayoutParams(new LinearLayout.LayoutParams(-2, -2)); return view;
    }
    static TextView amount(Context context, String text, boolean dark, int size) {
        TextView view = Ui.text(context, text, size, Ui.accent(context, dark));
        view.setTypeface(PretendardFont.bold(context)); return view;
    }
    static Button action(Context context, String text, boolean primary, boolean dark, Runnable click) {
        Button button = Ui.button(context, text, primary, dark);
        button.setSingleLine(false); button.setTextSize(15);
        button.setMinHeight(Ui.dp(context, 48)); button.setMinimumHeight(Ui.dp(context, 48));
        button.setOnClickListener(view -> click.run()); return button;
    }
    static LinearLayout pair(Context context, View first, View second) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(stacked(context) ? LinearLayout.VERTICAL : LinearLayout.HORIZONTAL);
        if (stacked(context)) {
            row.addView(first, new LinearLayout.LayoutParams(-1, -2));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
            params.topMargin = Ui.dp(context, 10); row.addView(second, params);
        } else {
            row.addView(first, new LinearLayout.LayoutParams(0, -2, 1));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, -2, 1);
            params.setMarginStart(Ui.dp(context, 10)); row.addView(second, params);
        }
        return row;
    }
    static LinearLayout tile(Context context, String title, String value, String note, boolean dark) {
        LinearLayout card = Ui.card(context, dark);
        card.addView(caption(context, title, dark)); Ui.addSpacer(card, 8);
        card.addView(amount(context, value, dark, 26)); Ui.addSpacer(card, 8);
        card.addView(caption(context, note, dark)); return card;
    }
    static LinearLayout tabs(Context context, String[] labels, int selected, boolean dark,
            java.util.function.IntConsumer click) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(stacked(context) ? LinearLayout.VERTICAL : LinearLayout.HORIZONTAL);
        for (int i = 0; i < labels.length; i++) {
            final int index = i;
            Button button = action(context, labels[i], i == selected, dark, () -> click.accept(index));
            button.setSelected(i == selected);
            LinearLayout.LayoutParams params = stacked(context)
                    ? new LinearLayout.LayoutParams(-1, -2) : new LinearLayout.LayoutParams(0, -2, 1);
            if (i > 0) { if (stacked(context)) params.topMargin = Ui.dp(context, 6); else params.setMarginStart(Ui.dp(context, 6)); }
            row.addView(button, params);
        }
        return row;
    }
    static void section(LinearLayout parent, String title, boolean dark) {
        Ui.addSpacer(parent, 16); parent.addView(heading(parent.getContext(), title, dark)); Ui.addSpacer(parent, 10);
    }
}
