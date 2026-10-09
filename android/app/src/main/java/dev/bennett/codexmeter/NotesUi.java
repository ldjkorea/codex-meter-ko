package dev.bennett.codexmeter;

import android.app.AlertDialog;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

/** Quick dialog, debounced durable draft and explicit completion. No automatic external submission. */
final class NotesUi {
    static final ExecutorService WORKER=Executors.newSingleThreadExecutor();
    private static final Handler MAIN=new Handler(Looper.getMainLooper());
    private static final Map<AppCompatActivity,Editor> OPEN=new WeakHashMap<>();
    private static final AtomicLong REV=new AtomicLong(System.currentTimeMillis());
    private static final int ADD=8640,LIST=8641;
    private NotesUi(){}
    // Retain archived notes and draft writers, but expose no test UI even with a legacy flag.
    static void menu(AppCompatActivity a,Menu menu,String screen) { }
    static void prepare(AppCompatActivity a,Menu menu) { menu.removeItem(ADD);menu.removeItem(LIST); }
    static boolean select(AppCompatActivity a,MenuItem item,String screen) { return false; }
    static void flush(AppCompatActivity a){Editor editor=OPEN.get(a);if(editor!=null)editor.save(false,null);}
    static void close(AppCompatActivity a){Editor editor=OPEN.get(a);if(editor!=null){editor.save(false,null);editor.dialog.dismiss();OPEN.remove(a);}}
    static void edit(AppCompatActivity a,String screen,TestNote existing,Runnable refresh){
        try{if(existing==null)for(TestNote note:TestNotesStore.load(a))if(note.draft&&note.screen.equals(screen))existing=note;
            Editor editor=new Editor(a,screen,existing,refresh);OPEN.put(a,editor);editor.show();
        }catch(Exception ignored){android.widget.Toast.makeText(a,R.string.fun_load_failed,android.widget.Toast.LENGTH_LONG).show();}
    }
    private static final class Editor {
        final AppCompatActivity activity;final android.content.Context context;final String id,screen;final long created;final int initialStatus;final boolean dark;final Runnable refresh;
        final EditText text;final TextView state;final AlertDialog dialog;long lastRevision;boolean completing,dirty;final Runnable debounce=()->save(false,null);
        Editor(AppCompatActivity a,String screen,TestNote note,Runnable refresh){
            activity=a;context=a.getApplicationContext();this.screen=note==null?screen:note.screen;this.refresh=refresh;dark=Ui.isDark(a);created=note==null?System.currentTimeMillis():note.created;id=note==null?java.util.UUID.randomUUID().toString():note.id;initialStatus=note==null?0:note.status;
            if(note!=null)REV.accumulateAndGet(note.revision,Math::max);
            LinearLayout form=new LinearLayout(a);form.setOrientation(LinearLayout.VERTICAL);form.setPadding(Ui.dp(a,24),Ui.dp(a,12),Ui.dp(a,24),0);
            text=new EditText(a);text.setHint(R.string.fun_note_hint);text.setMinLines(3);text.setMaxLines(7);text.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE|android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);text.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(32000)});if(note!=null)text.setText(note.text);form.addView(text);
            state=LedgerUi.caption(a,note==null?"":a.getString(R.string.fun_note_saved),dark);state.setAccessibilityLiveRegion(android.view.View.ACCESSIBILITY_LIVE_REGION_POLITE);form.addView(state);
            dialog=new AlertDialog.Builder(a).setTitle(R.string.fun_note_add).setView(form).setNegativeButton(R.string.ui_cancel_77dfd2,null).setPositiveButton(R.string.fun_note_done,null).create();
            text.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int start,int count,int after){}public void onTextChanged(CharSequence s,int start,int before,int count){dirty=true;state.setText(R.string.fun_note_saving);MAIN.removeCallbacks(debounce);MAIN.postDelayed(debounce,250);}public void afterTextChanged(Editable s){}});
            dialog.setOnShowListener(unused->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{if(text.getText().toString().trim().isEmpty())return;completing=true;save(true,()->{dialog.dismiss();if(refresh!=null)refresh.run();});}));
            dialog.setOnDismissListener(unused->{MAIN.removeCallbacks(debounce);if(!completing&&!text.getText().toString().isEmpty())save(false,null);OPEN.remove(activity);});
        }
        void show(){dialog.show();}
        void save(boolean done,Runnable after){
            if(completing&&!done)return;
            if(!dirty&&!done)return;
            MAIN.removeCallbacks(debounce);String value=text.getText().toString();if(value.isEmpty())return;
            long revision=REV.incrementAndGet();lastRevision=revision;state.setText(R.string.fun_note_saving);
            TestNote note=new TestNote(id,value,screen,AppConstants.VERSION_NAME,"35-release-matte-v1",created,Math.max(created,System.currentTimeMillis()),revision,initialStatus,!done);
            WORKER.execute(()->{boolean saved=TestNotesStore.save(context,note);MAIN.post(()->{if(activity.isDestroyed()||revision!=lastRevision)return;state.setText(saved?R.string.fun_note_saved:R.string.fun_note_failed);if(saved&&after!=null)after.run();if(!saved)completing=false;});});
        }
    }
}
