package dev.bennett.codexmeter;

import android.content.Context;
import android.os.Build;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.widget.TextView;

/** Shared TextView wiring for rendered GitHub release notes. */
public final class ReleaseNotesUi {
    private ReleaseNotesUi() {
    }

    public static TextView create(Context context, String markdown, boolean dark) {
        TextView view = Ui.text(context, "", 14, Ui.mainText(dark));
        apply(view, markdown);
        return view;
    }

    public static void apply(TextView view, String markdown) {
        String html = ReleaseNotesMarkdown.toHtml(markdown);
        if (html.isEmpty()) {
            view.setText("");
            return;
        }
        CharSequence rendered;
        if (Build.VERSION.SDK_INT >= 24) {
            rendered = Html.fromHtml(html, Html.FROM_HTML_MODE_COMPACT);
        } else {
            rendered = Html.fromHtml(html);
        }
        view.setText(rendered);
        // Give wrapped bullets one consistent text edge instead of Android HTML's narrow default.
        android.text.SpannableStringBuilder aligned=new android.text.SpannableStringBuilder(rendered);
        for(android.text.style.BulletSpan bullet:aligned.getSpans(0,aligned.length(),android.text.style.BulletSpan.class)){
            int start=aligned.getSpanStart(bullet),end=aligned.getSpanEnd(bullet);
            aligned.removeSpan(bullet);
            aligned.setSpan(new android.text.style.BulletSpan(Ui.dp(view.getContext(),10)),start,end,android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            aligned.setSpan(new android.text.style.LeadingMarginSpan.Standard(Ui.dp(view.getContext(),8)),start,end,android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        view.setText(aligned);
        view.setLineSpacing(Ui.dp(view.getContext(),4),1.12f);
        if(Build.VERSION.SDK_INT>=29)view.setBreakStrategy(android.graphics.text.LineBreaker.BREAK_STRATEGY_HIGH_QUALITY);
        view.setHyphenationFrequency(android.text.Layout.HYPHENATION_FREQUENCY_NONE);
        view.setMovementMethod(LinkMovementMethod.getInstance());
        view.setLinksClickable(true);
    }
}
