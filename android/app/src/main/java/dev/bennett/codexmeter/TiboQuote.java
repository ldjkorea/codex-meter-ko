package dev.bennett.codexmeter;

import android.content.Intent;
import android.net.Uri;
import android.widget.LinearLayout;
import androidx.appcompat.app.AppCompatActivity;

/** A verified, dated excerpt bundled offline; never presented as a live reset announcement. */
final class TiboQuote {
    static final String SOURCE="https://x.com/thsottiaux/status/2045299702590259631";
    static void add(AppCompatActivity a,LinearLayout parent,boolean dark){
        LinearLayout quote=new LinearLayout(a);quote.setOrientation(LinearLayout.VERTICAL);
        quote.setPadding(Ui.dp(a,12),Ui.dp(a,18),Ui.dp(a,12),Ui.dp(a,18));
        android.widget.TextView text=LedgerUi.heading(a,"“"+a.getString(R.string.polish_tibo_quote)+"”",dark);
        text.setTextSize(18);text.setLineSpacing(Ui.dp(a,3),1.08f);quote.addView(text);Ui.addSpacer(quote,8);
        android.widget.Button source=LedgerUi.action(a,a.getString(R.string.polish_tibo_source),false,dark,()->{
            try{a.startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(SOURCE)));}
            catch(RuntimeException ignored){android.widget.Toast.makeText(a,R.string.polish_open_source_failed,android.widget.Toast.LENGTH_SHORT).show();}
        });
        source.setGravity(android.view.Gravity.START|android.view.Gravity.CENTER_VERTICAL);source.setTextSize(12);
        quote.addView(source);parent.addView(quote);
    }
}
