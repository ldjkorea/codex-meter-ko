package dev.bennett.codexmeter;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import java.util.*;

/** Per-id editor. Hidden choices remain selected for OFF→ON restoration. */
public final class EvolutionWidgetConfigActivity extends AppCompatActivity {
    private int id;private List<String> elements;private LinearLayout content;private FrameLayout preview;private boolean dark,theme;private int opacity;
    protected void onCreate(Bundle b){super.onCreate(b);setResult(RESULT_CANCELED);id=getIntent().getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,-1);
        if(id<0){finish();return;}elements=EvolutionElements.parse(b==null?EvolutionWidget.selected(this,id):b.getString("elements"));
        theme=b==null?EvolutionWidget.prefs(this).getBoolean(id+".theme",true):b.getBoolean("theme");opacity=b==null?EvolutionWidget.prefs(this).getInt(id+".opacity",88):b.getInt("opacity");
        Ui.ConfigPage p=Ui.installConfigPage(this,getString(R.string.evo_widget));content=p.content;preview=p.preview;dark=Ui.isDark(this);
        p.cancel.setOnClickListener(v->finish());p.save.setOnClickListener(v->save());render();}
    protected void onSaveInstanceState(Bundle b){super.onSaveInstanceState(b);b.putString("elements",String.join(",",elements));b.putBoolean("theme",theme);b.putInt("opacity",opacity);}
    private void render(){content.removeAllViews();content.addView(LedgerUi.caption(this,getString(R.string.evo_widget_help),dark));
        int[] labels={R.string.evo_five_usage,R.string.evo_week_usage,R.string.fun_tier,R.string.evo_five_reset,R.string.evo_week_reset};
        for(int i=0;i<5;i++){String key=EvolutionElements.ALL[i];CheckBox c=new CheckBox(this);c.setText(labels[i]);c.setTextColor(Ui.mainText(dark));c.setMinHeight(Ui.dp(this,48));c.setChecked(elements.contains(key));
            c.setOnCheckedChangeListener((v,on)->{if(!on&&elements.size()==1){c.setChecked(true);Toast.makeText(this,R.string.evo_select_one,Toast.LENGTH_SHORT).show();return;}if(on){if(!elements.contains(key))elements.add(key);}else elements.remove(key);render();});content.addView(c);}
        for(int i=0;i<elements.size();i++){final int pos=i;String key=elements.get(i);int index=Arrays.asList(EvolutionElements.ALL).indexOf(key);
            if(i>0)content.addView(LedgerUi.action(this,"↑ "+getString(labels[index]),false,dark,()->{Collections.swap(elements,pos,pos-1);render();}));}
        SwitchCompat themeSwitch=new SwitchCompat(this);themeSwitch.setText(R.string.evo_theme);themeSwitch.setTextColor(Ui.mainText(dark));themeSwitch.setMinHeight(Ui.dp(this,48));themeSwitch.setChecked(theme);themeSwitch.setOnCheckedChangeListener((v,on)->{theme=on;updatePreview();});content.addView(themeSwitch);
        content.addView(LedgerUi.caption(this,getString(R.string.evo_opacity),dark));SeekBar opacityBar=new SeekBar(this);opacityBar.setMax(100);opacityBar.setProgress(opacity);opacityBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar s,int v,boolean user){opacity=v;updatePreview();}public void onStartTrackingTouch(SeekBar s){}public void onStopTrackingTouch(SeekBar s){}});content.addView(opacityBar);
        updatePreview();
    }
    private void updatePreview(){preview.removeAllViews();preview.addView(EvolutionWidget.render(this,id,AppWidgetManager.getInstance(this).getAppWidgetOptions(id),String.join(",",elements),theme,opacity).apply(this,preview));}
    private void save(){try{String csv=EvolutionElements.save(elements);
        if(!EvolutionWidget.prefs(this).edit().putString(id+".elements",csv).putBoolean(id+".theme",theme).putInt(id+".opacity",opacity).commit())throw new IllegalStateException();
        EvolutionWidget.updateAll(this);setResult(RESULT_OK,new Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,id));finish();
    }catch(Exception e){Toast.makeText(this,R.string.fun_load_failed,Toast.LENGTH_LONG).show();}}
}
