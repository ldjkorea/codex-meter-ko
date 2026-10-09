package dev.bennett.codexmeter;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.nio.charset.StandardCharsets;
import java.util.List;

/** Device note manager. Export/copy is explicitly user initiated, never sent automatically. */
public final class TestNotesActivity extends AppCompatActivity {
    private LinearLayout content;private boolean dark;private int filter;
    private static final int COPY=8650,MD=8651,TXT=8652,JSON=8653,CLEAR=8654;
    private static final int[] STATUS={R.string.fun_note_open,R.string.fun_note_verify,R.string.fun_note_resolved};
    @Override protected void onCreate(Bundle state){Ui.applySelectedTheme(this);super.onCreate(state);dark=Ui.isDark(this);content=Ui.installPage(this,getString(R.string.fun_notes),true).content;if(state!=null)filter=state.getInt("filter");}
    @Override protected void onResume(){super.onResume();render();}
    @Override protected void onPause(){NotesUi.flush(this);super.onPause();}
    @Override protected void onDestroy(){NotesUi.close(this);super.onDestroy();}
    @Override protected void onSaveInstanceState(Bundle state){super.onSaveInstanceState(state);state.putInt("filter",filter);}
    @Override public boolean onSupportNavigateUp(){finish();return true;}
    @Override public boolean onCreateOptionsMenu(Menu menu){menu.add(0,COPY,0,R.string.fun_note_copy);menu.add(0,MD,1,R.string.fun_note_export_md);menu.add(0,TXT,2,R.string.fun_note_export_txt);menu.add(0,JSON,3,R.string.fun_note_export_json);menu.add(0,CLEAR,4,R.string.fun_note_clear);return true;}
    @Override public boolean onOptionsItemSelected(MenuItem item){int id=item.getItemId();
        if(id==COPY){NotesUi.WORKER.execute(()->{try{String text=TestNotesStore.export(getApplicationContext(),false);runOnUiThread(()->{if(isDestroyed())return;((ClipboardManager)getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("Codex Meter test notes",text));Toast.makeText(this,R.string.fun_note_copy_done,Toast.LENGTH_LONG).show();});}catch(Exception ignored){failed();}});return true;}
        if(id==MD||id==TXT||id==JSON){Intent intent=new Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType(id==JSON?"application/json":"text/plain").putExtra(Intent.EXTRA_TITLE,"CodexMeter-test-notes."+(id==JSON?"json":id==MD?"md":"txt"));startActivityForResult(intent,id);return true;}
        if(id==CLEAR){new AlertDialog.Builder(this).setTitle(R.string.fun_note_clear).setMessage(R.string.fun_note_clear_confirm).setNegativeButton(R.string.ui_cancel_77dfd2,null).setPositiveButton(R.string.ui_clear_719ea3,(d,w)->mutate(()->TestNotesStore.clear(getApplicationContext()))).show();return true;}
        return super.onOptionsItemSelected(item);
    }
    @Override protected void onActivityResult(int request,int result,Intent intent){super.onActivityResult(request,result,intent);if(result!=RESULT_OK||intent==null||intent.getData()==null||request<MD||request>JSON)return;
        android.net.Uri uri=intent.getData();android.content.Context app=getApplicationContext();NotesUi.WORKER.execute(()->{try{try(java.io.OutputStream stream=app.getContentResolver().openOutputStream(uri)){if(stream==null)throw new IllegalStateException();stream.write(TestNotesStore.export(app,request==JSON).getBytes(StandardCharsets.UTF_8));stream.flush();}runOnUiThread(()->{if(!isDestroyed())Toast.makeText(this,R.string.fun_note_export_done,Toast.LENGTH_LONG).show();});}catch(Exception ignored){failed();}});
    }
    private void render(){
        if(isDestroyed())return;content.removeAllViews();content.addView(LedgerUi.caption(this,getString(R.string.fun_note_scope),dark));Ui.addSpacer(content,12);
        android.widget.Button add=LedgerUi.action(this,getString(R.string.fun_note_add),true,dark,()->NotesUi.edit(this,"notes",null,this::render));add.setEnabled(TestNotesStore.enabled(this));content.addView(add);
        Spinner selector=Ui.spinner(this,new String[]{getString(R.string.fun_note_all),getString(STATUS[0]),getString(STATUS[1]),getString(STATUS[2]),getString(R.string.fun_note_draft)},dark);selector.setSelection(filter);content.addView(selector);
        selector.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){public void onItemSelected(AdapterView<?> p,android.view.View v,int position,long id){if(position!=filter){filter=position;render();}}public void onNothingSelected(AdapterView<?> p){}});
        try{List<TestNote> notes=TestNotesStore.load(this);notes.sort((a,b)->Long.compare(b.updated,a.updated));int shown=0;
            for(TestNote note:notes){if(filter==4&&!note.draft||filter>=1&&filter<=3&&note.status!=filter-1)continue;shown++;
                LinearLayout card=Ui.card(this,dark);card.addView(LedgerUi.heading(this,getString(STATUS[note.status])+(note.draft?" · "+getString(R.string.fun_note_draft):""),dark));Ui.addSpacer(card,10);
                card.addView(LedgerUi.caption(this,note.text,dark));card.addView(LedgerUi.caption(this,getString(R.string.fun_note_meta,V3Display.time(note.created),V3Display.time(note.updated),note.version,note.screen),dark));Ui.addSpacer(card,10);
                card.addView(LedgerUi.action(this,getString(R.string.fun_note_edit),false,dark,()->NotesUi.edit(this,note.screen,note,this::render)));
                card.addView(LedgerUi.action(this,getString(STATUS[note.status]),false,dark,()->new AlertDialog.Builder(this).setTitle(R.string.fun_notes).setItems(new String[]{getString(STATUS[0]),getString(STATUS[1]),getString(STATUS[2])},(dialog,status)->{
                    long now=Math.max(note.updated,System.currentTimeMillis());TestNote changed=new TestNote(note.id,note.text,note.screen,note.version,note.build,note.created,now,Math.max(note.revision+1,now),status,note.draft);mutate(()->TestNotesStore.save(getApplicationContext(),changed));}).show()));
                card.addView(LedgerUi.action(this,getString(R.string.fun_note_delete),false,dark,()->new AlertDialog.Builder(this).setTitle(R.string.fun_note_delete).setNegativeButton(R.string.ui_cancel_77dfd2,null).setPositiveButton(R.string.ui_clear_719ea3,(d,w)->mutate(()->TestNotesStore.delete(getApplicationContext(),note.id))).show()));Ui.addSpacer(content,12);content.addView(card);
            }if(shown==0)content.addView(LedgerUi.caption(this,getString(R.string.fun_note_empty),dark));
        }catch(Exception ignored){content.addView(LedgerUi.caption(this,getString(R.string.fun_load_failed),dark));}
    }
    private interface Mutation {boolean run();}
    private void mutate(Mutation task){NotesUi.WORKER.execute(()->{boolean ok=task.run();runOnUiThread(()->{if(isDestroyed())return;if(ok)render();else Toast.makeText(this,R.string.fun_note_failed,Toast.LENGTH_LONG).show();});});}
    private void failed(){runOnUiThread(()->{if(!isDestroyed())Toast.makeText(this,R.string.fun_note_failed,Toast.LENGTH_LONG).show();});}
}
