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
    private final Handler handler=new Handler(Looper.getMainLooper());private TextView status;private boolean visible;
    private final Runnable poll=new Runnable(){public void run(){if(!visible)return;if(TaskStatusStore.enabled(TaskStatusActivity.this))LanSync.taskStatus(TaskStatusActivity.this,ok->runOnUiThread(()->{if(visible)render();}));render();handler.postDelayed(this,15000);}};
    @Override protected void onCreate(Bundle state){super.onCreate(state);boolean dark=Ui.isDark(this);setTitle(R.string.task_trial_title);
        ScrollView scroll=new ScrollView(this);LinearLayout content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);content.setPadding(Ui.dp(this,20),Ui.dp(this,24),Ui.dp(this,20),Ui.dp(this,24));scroll.addView(content);scroll.setBackgroundColor(dark?0xff0d0e10:0xfff2f3f5);
        content.addView(LedgerUi.heading(this,getString(R.string.task_trial_title),dark));Ui.addSpacer(content,20);
        SwitchCompat toggle=new SwitchCompat(this);toggle.setText(R.string.task_trial_toggle);toggle.setTextColor(Ui.mainText(dark));toggle.setMinHeight(Ui.dp(this,48));toggle.setChecked(TaskStatusStore.enabled(this));content.addView(toggle);
        toggle.setOnCheckedChangeListener((button,on)->{try{TaskStatusStore.enable(this,on);render();handler.removeCallbacks(poll);if(on)handler.post(poll);}catch(Exception ignored){button.setChecked(false);Toast.makeText(this,R.string.task_trial_signin,Toast.LENGTH_LONG).show();}});
        Ui.addSpacer(content,16);status=LedgerUi.heading(this,"",dark);status.setTextSize(18);content.addView(status);Ui.addSpacer(content,24);
        content.addView(LedgerUi.caption(this,getString(R.string.task_trial_limits),dark));Ui.addSpacer(content,16);
        content.addView(LedgerUi.action(this,getString(R.string.ui_refresh_56e3ba),false,dark,()->{if(TaskStatusStore.enabled(this))LanSync.taskStatus(this,ok->runOnUiThread(()->{if(visible)render();}));}));setContentView(scroll);render();
    }
    private void render(){if(status!=null)status.setText(TaskStatusStore.text(this,3));}
    @Override protected void onResume(){super.onResume();visible=true;handler.removeCallbacks(poll);handler.post(poll);}
    @Override protected void onPause(){visible=false;handler.removeCallbacks(poll);super.onPause();}
}
