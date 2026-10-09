package dev.bennett.codexmeter;

import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;
import androidx.appcompat.app.AppCompatActivity;

/** Changes in the installed APK, readable even before the next network check. */
public final class WhatsNewActivity extends AppCompatActivity {
    @Override protected void onCreate(Bundle state){
        Ui.applySelectedTheme(this);super.onCreate(state);boolean dark=Ui.isDark(this);
        LinearLayout content=Ui.installPage(this,getString(R.string.ui_what_s_new_369702),true).content;
        LinearLayout card=Ui.card(this,dark);
        for(ReleaseCatalog.Entry entry:ReleaseCatalog.all(this))if(entry.version.equals(AppConstants.VERSION_NAME)){
            card.addView(LedgerUi.heading(this,"v"+entry.version+" · "+entry.title,dark));Ui.addSpacer(card,16);
            card.addView(ReleaseNotesUi.create(this,entry.notes,dark));break;
        }
        content.addView(card);Ui.addSpacer(content,16);
        content.addView(LedgerUi.action(this,getString(R.string.ui_release_history_5044ba),false,dark,()->startActivity(new Intent(this,ReleaseHistoryActivity.class))));
    }
    @Override public boolean onSupportNavigateUp(){finish();return true;}
}
