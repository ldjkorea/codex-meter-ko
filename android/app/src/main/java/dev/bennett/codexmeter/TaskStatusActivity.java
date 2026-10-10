package dev.bennett.codexmeter;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

/** Optional observer only. No approvals, task commands or account quota calls. */
public final class TaskStatusActivity extends AppCompatActivity {
    private final Handler handler=new Handler(Looper.getMainLooper());private TextView status,feedback;private boolean visible,busy;private android.widget.Button refresh;
    private final Runnable poll=new Runnable(){public void run(){if(!visible)return;check();handler.postDelayed(this,15000);}};
    @Override protected void onCreate(Bundle state){Ui.applySelectedTheme(this);super.onCreate(state);boolean dark=Ui.isDark(this);
        LinearLayout content=Ui.installPage(this,getString(R.string.task_trial_title),true).content;
        ((androidx.swiperefreshlayout.widget.SwipeRefreshLayout)findViewById(R.id.dashboard_refresh)).setEnabled(false);
        LinearLayout card=Ui.card(this,dark);content.addView(card);
        SwitchCompat toggle=new SwitchCompat(this);toggle.setText(R.string.task_trial_toggle);toggle.setTextColor(Ui.mainText(dark));toggle.setMinHeight(Ui.dp(this,48));toggle.setChecked(TaskStatusStore.enabled(this));card.addView(toggle);
        toggle.setOnCheckedChangeListener((button,on)->{try{TaskStatusStore.enable(this,on);render();handler.removeCallbacks(poll);if(on)handler.post(poll);}catch(Exception ignored){button.setChecked(false);Toast.makeText(this,R.string.task_trial_signin,Toast.LENGTH_LONG).show();}});
        Ui.addSpacer(content,16);status=LedgerUi.heading(this,"",dark);status.setTextSize(16);card.addView(status);Ui.addSpacer(card,12);
        feedback=LedgerUi.caption(this,"",dark);card.addView(feedback);Ui.addSpacer(card,12);
        refresh=LedgerUi.action(this,getString(R.string.ui_refresh_56e3ba),true,dark,this::check);card.addView(refresh);Ui.addSpacer(content,16);
        content.addView(LedgerUi.action(this,getString(R.string.task_trial_connection),false,dark,()->LanSyncUi.show(this,dark)));
        Ui.addSpacer(content,8);content.addView(LedgerUi.action(this,getString(R.string.hud_add_task_widget),false,dark,this::pinWidget));
        Ui.addSpacer(content,16);content.addView(LedgerUi.action(this,getString(R.string.hud_status_help),false,dark,()->new android.app.AlertDialog.Builder(this).setTitle(R.string.hud_status_help).setMessage(R.string.task_trial_limits).setPositiveButton(R.string.ui_done_e9b450,null).show()));render();
    }
    private void render(){if(status!=null){String text=TaskStatusStore.text(this,3);if(!text.contentEquals(status.getText()))status.setText(text);}if(refresh!=null)refresh.setEnabled(!busy&&TaskStatusStore.enabled(this));}
    private void check(){render();if(busy||!TaskStatusStore.enabled(this))return;busy=true;refresh.setEnabled(false);feedback.setText(R.string.task_trial_connecting);
        LanSync.taskStatus(this,ok->runOnUiThread(()->{busy=false;if(isDestroyed()||isFinishing())return;refresh.setEnabled(TaskStatusStore.enabled(this));feedback.setText(ok?R.string.hud_status_checked:R.string.task_trial_remote_wait);if(visible)render();}));}
    private void pinWidget(){android.appwidget.AppWidgetManager manager=android.appwidget.AppWidgetManager.getInstance(this);if(manager.isRequestPinAppWidgetSupported())manager.requestPinAppWidget(new android.content.ComponentName(this,TaskStatusWidget.class),null,null);else Toast.makeText(this,R.string.hud_add_task_widget_help,Toast.LENGTH_LONG).show();}
    @Override public boolean onSupportNavigateUp(){finish();return true;}
    @Override protected void onResume(){super.onResume();visible=true;handler.removeCallbacks(poll);handler.post(poll);}
    @Override protected void onPause(){visible=false;handler.removeCallbacks(poll);super.onPause();}
}
