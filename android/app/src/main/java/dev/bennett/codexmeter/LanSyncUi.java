package dev.bennett.codexmeter;

import android.app.AlertDialog;
import android.text.InputType;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

final class LanSyncUi {
    static void show(AppCompatActivity a,boolean dark){
        LinearLayout form=new LinearLayout(a);form.setOrientation(LinearLayout.VERTICAL);int pad=Ui.dp(a,20);form.setPadding(pad,pad,pad,pad);
        form.addView(LedgerUi.caption(a,a.getString(R.string.lan_sync_help),dark));long last=LanSync.last(a);
        if(last>0)form.addView(LedgerUi.caption(a,a.getString(R.string.lan_sync_last,android.text.format.DateFormat.format("MM.dd HH:mm",last)),dark));
        EditText code=new EditText(a);code.setTextColor(Ui.mainText(dark));code.setHint(R.string.lan_sync_code);code.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);code.setSingleLine(false);code.setMinLines(3);form.addView(code);
        form.addView(LedgerUi.action(a,a.getString(R.string.lan_sync_now),false,dark,()->sync(a)));
        AlertDialog dialog=new AlertDialog.Builder(a).setTitle(R.string.lan_sync_title).setView(form).setPositiveButton(R.string.lan_sync_connect,null)
            .setNeutralButton(R.string.lan_sync_disconnect,(d,w)->{try{LanSync.disconnect(a);LanWifiScheduler.ensureScheduled(a);Toast.makeText(a,R.string.lan_sync_disconnected,Toast.LENGTH_SHORT).show();}catch(Exception ignored){Toast.makeText(a,R.string.lan_sync_invalid,Toast.LENGTH_LONG).show();}})
            .setNegativeButton(R.string.ui_done_e9b450,null).create();
        dialog.setOnShowListener(d->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{try{LanSync.pair(a,code.getText().toString());LanWifiScheduler.install(a);dialog.dismiss();sync(a);}catch(Exception ignored){code.setError(a.getString(R.string.lan_sync_invalid));}}));dialog.show();
    }
    private static void sync(AppCompatActivity a){
        Toast.makeText(a,R.string.lan_sync_working,Toast.LENGTH_SHORT).show();LanSync.schedule(a,ok->a.runOnUiThread(()->{if(!a.isDestroyed()&&!a.isFinishing())Toast.makeText(a,ok?R.string.lan_sync_done:R.string.lan_sync_wait,Toast.LENGTH_LONG).show();}));}
}
