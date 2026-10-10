package dev.bennett.codexmeter;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import androidx.appcompat.app.AppCompatActivity;
import androidx.activity.OnBackPressedCallback;

/** Two visible roots; old analysis intents still route into records. */
final class MatteNav {
    private MatteNav(){}
    static void install(AppCompatActivity activity,int selected){
        FrameLayout container=activity.findViewById(R.id.matte_bottom);if(container==null)return;
        boolean dark=Ui.isDark(activity);container.setVisibility(View.VISIBLE);container.setBackgroundColor(Ui.background(activity,dark));
        LinearLayout bar=new LinearLayout(activity);bar.setOrientation(LinearLayout.HORIZONTAL);bar.setPadding(Ui.dp(activity,16),Ui.dp(activity,8),Ui.dp(activity,16),Ui.dp(activity,8));
        int[] labels={R.string.matte_home,R.string.matte_records};int[] icons={R.drawable.matte_home,R.drawable.matte_records};
        int[] roots={1,2};selected=selected==0?2:selected;final int active=selected;
        for(int i=0;i<2;i++){final int index=roots[i];Button button=Ui.button(activity,activity.getString(labels[i]),index==active,dark);
            button.setTextSize(12);button.setSingleLine(false);button.setMinHeight(Ui.dp(activity,64));button.setSelected(index==active);
            button.setCompoundDrawablesRelativeWithIntrinsicBounds(0,icons[i],0,0);button.setCompoundDrawablePadding(Ui.dp(activity,5));
            androidx.core.widget.TextViewCompat.setCompoundDrawableTintList(button,ColorStateList.valueOf(index==active?Ui.onAccent(activity,dark):Ui.secondaryText(dark)));
            button.setContentDescription(activity.getString(labels[i]));if(index==active)androidx.core.view.ViewCompat.setStateDescription(button,activity.getString(R.string.matte_selected));button.setOnClickListener(v->{if(index!=active)open(activity,index);});
            LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(0,-2,1);if(i>0)params.setMarginStart(Ui.dp(activity,8));bar.addView(button,params);
        }container.removeAllViews();container.addView(bar);
        activity.getOnBackPressedDispatcher().addCallback(activity,new OnBackPressedCallback(true){public void handleOnBackPressed(){if(active==1)activity.moveTaskToBack(true);else open(activity,1);}});
    }
    static void open(AppCompatActivity activity,int index){
        Class<?> target=index==0||index==2?RecordsActivity.class:MainActivity.class;
        activity.startActivity(new Intent(activity,target).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT|Intent.FLAG_ACTIVITY_SINGLE_TOP));
    }
    static void homeAction(AppCompatActivity activity,String action){activity.startActivity(new Intent(activity,MainActivity.class).putExtra("matte_action",action).putExtra("matte_return",activity instanceof RecordsActivity||activity instanceof MeterSettingsActivity?2:1).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT|Intent.FLAG_ACTIVITY_SINGLE_TOP));}
}
