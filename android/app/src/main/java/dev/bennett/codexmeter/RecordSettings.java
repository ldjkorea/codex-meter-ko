package dev.bennett.codexmeter;

import android.app.AlertDialog;
import android.content.Intent;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import java.util.function.Consumer;
import org.json.JSONArray;
import org.json.JSONObject;

/** Compact records controls and a separate settings hub over existing stores/forms. */
final class RecordSettings {
    private RecordSettings() { }
    static void add(AppCompatActivity a,LinearLayout parent,boolean dark) {
        LinearLayout display=card(a,parent,dark,R.string.ux_display);
        toggle(a,display,dark,R.string.matte_five_hour,AppPreferences.showDashboardFiveHour(a),on->AppPreferences.setShowDashboardFiveHour(a,on));
        toggle(a,display,dark,R.string.ux_weekly,AppPreferences.showDashboardWeekly(a),on->AppPreferences.setShowDashboardWeekly(a,on));
        toggle(a,display,dark,R.string.ui_ledger_home,AppPreferences.showUsageOverview(a),on->AppPreferences.setShowUsageOverview(a,on));
        action(a,parent,dark,R.string.matte_settings,()->a.startActivity(new Intent(a,MeterSettingsActivity.class)));
    }
    static void hub(AppCompatActivity a,LinearLayout parent,boolean dark,Consumer<Boolean> export,Runnable clear,Runnable details) {
        UsageSnapshot snapshot=AppPreferences.loadSnapshot(a);
        SubscriptionCost bill=SubscriptionStore.load(a,snapshot);
        LinearLayout account=card(a,parent,dark,R.string.ux_account_subscription);
        account.addView(LedgerUi.caption(a,snapshot==null?a.getString(R.string.ui_not_connected_8b02f3):snapshot.planType,dark));
        account.addView(LedgerUi.amount(a,bill==null?"—":FunActivity.money(bill.currency,bill.amount),dark,27));
        account.addView(LedgerUi.caption(a,bill==null?a.getString(R.string.ux_tax_amount):a.getString(R.string.ux_bill_dates,bill.start.toString(),bill.end.toString()),dark));
        Button edit=action(a,account,dark,bill==null?R.string.ux_register:R.string.v3_payment_edit,()->SubscriptionUi.edit(a,AppPreferences.loadSnapshot(a),dark,a::recreate));
        edit.setEnabled(SubscriptionStore.key(a,snapshot)!=null);
        action(a,account,dark,R.string.ux_payment_history,()->billingHistory(a,dark));
        action(a,account,dark,R.string.matte_account_manage,()->MatteNav.homeAction(a,"account"));

        LinearLayout fun=card(a,parent,dark,R.string.ux_value_coach);
        toggle(a,fun,dark,R.string.evo_effects,TierTheme.effects(a),on->a.getSharedPreferences("codex_tier_effects",0).edit().putBoolean("enabled",on).apply());
        action(a,fun,dark,R.string.fun_tone,()->tone(a));
        fun.addView(LedgerUi.caption(a,a.getString(toneLabel(FunStore.tone(a))),dark));
        action(a,fun,dark,R.string.fun_why,()->new AlertDialog.Builder(a).setTitle(R.string.fun_why).setMessage(R.string.fun_tier_help).setPositiveButton(R.string.ui_done_e9b450,null).show());
        action(a,fun,dark,R.string.matte_recaps,()->a.startActivity(new Intent(a,FunActivity.class)));
        action(a,fun,dark,R.string.ux_value_method,()->SubscriptionValueUi.help(a));

        LinearLayout notify=card(a,parent,dark,R.string.matte_notify);
        page(a,notify,dark,R.string.ui_refresh_usage_903040,"refresh_usage");
        page(a,notify,dark,R.string.ui_notifications_753a22,"notifications");
        page(a,notify,dark,R.string.matte_now_bar,"now_bar");

        LinearLayout display=card(a,parent,dark,R.string.ux_widget_screen);
        action(a,display,dark,R.string.matte_widgets,()->MatteNav.homeAction(a,"widgets"));
        page(a,display,dark,R.string.ui_appearance_41def7,"appearance");
        action(a,display,dark,R.string.matte_limits,()->a.startActivity(new Intent(a,DashboardReorderActivity.class)));

        LinearLayout data=card(a,parent,dark,R.string.matte_data);
        action(a,data,dark,R.string.next_export_csv,()->export.accept(false));
        action(a,data,dark,R.string.next_export_json,()->export.accept(true));
        action(a,data,dark,R.string.ux_data_details,details);
        page(a,data,dark,R.string.ui_backup_transfer_84bb29,"transfer");
        page(a,data,dark,R.string.ui_privacy_cf0148,"privacy");
        page(a,data,dark,R.string.ui_diagnostics_3af227,"diagnostics");
        data.addView(LedgerUi.caption(a,a.getString(R.string.matte_retention,a.getResources().getQuantityString(R.plurals.matte_days,UsageLedgerDatabase.RAW_DAYS,UsageLedgerDatabase.RAW_DAYS),a.getResources().getQuantityString(R.plurals.matte_days,UsageLedgerDatabase.DAILY_DAYS,UsageLedgerDatabase.DAILY_DAYS)),dark));
        action(a,data,dark,R.string.ui_clear_local_history_ab1b10,clear);

        LinearLayout info=card(a,parent,dark,R.string.matte_about);
        info.addView(LedgerUi.caption(a,"Codex Meter "+AppConstants.VERSION_NAME,dark));
        page(a,info,dark,R.string.ui_updates_c76d18,"updates");
        action(a,info,dark,R.string.ux_licenses,()->a.startActivity(new Intent(a,AboutActivity.class)));
    }
    private static void billingHistory(AppCompatActivity a,boolean dark) {
        try {
            JSONArray bills=SubscriptionStore.history(a,AppPreferences.loadSnapshot(a));StringBuilder text=new StringBuilder();
            for(int i=bills.length()-1;i>=0;i--){JSONObject b=bills.getJSONObject(i);if(text.length()>0)text.append("\n\n");text.append(FunActivity.money(b.getString("currency"),new java.math.BigDecimal(b.getString("amount")))).append("\n").append(a.getString(R.string.ux_bill_dates,b.getString("start"),b.getString("end")));}
            new AlertDialog.Builder(a).setTitle(R.string.ux_payment_history).setMessage(text.length()==0?a.getString(R.string.ui_ledger_none):text.toString()).setPositiveButton(R.string.ui_done_e9b450,null).show();
        }catch(Exception ignored){Toast.makeText(a,R.string.fun_load_failed,Toast.LENGTH_LONG).show();}
    }
    private static void tone(AppCompatActivity a) {
        String[] tones={"calm","playful","spicy","off"};
        int[] examples={R.string.ux_example_calm,R.string.ux_example_playful,R.string.ux_example_spicy,R.string.ux_example_off};
        String[] labels=new String[4];for(int i=0;i<4;i++)labels[i]=a.getString(toneLabel(tones[i]))+"\n"+a.getString(examples[i]);
        new AlertDialog.Builder(a).setTitle(R.string.fun_tone).setSingleChoiceItems(labels,java.util.Arrays.asList(tones).indexOf(FunStore.tone(a)),(dialog,index)->{
            if(FunStore.tone(a,tones[index])){dialog.dismiss();a.recreate();}else Toast.makeText(a,R.string.fun_load_failed,Toast.LENGTH_LONG).show();
        }).setNegativeButton(R.string.ui_cancel_77dfd2,null).show();
    }
    private static int toneLabel(String tone){switch(tone){case "calm":return R.string.fun_tone_calm;case "spicy":return R.string.fun_tone_spicy;case "off":return R.string.fun_tone_off;default:return R.string.fun_tone_playful;}}
    private static LinearLayout card(AppCompatActivity a,LinearLayout parent,boolean dark,int title){LinearLayout card=Ui.card(a,dark);card.addView(LedgerUi.heading(a,a.getString(title),dark));Ui.addSpacer(card,12);parent.addView(card);Ui.addSpacer(parent,16);return card;}
    private static Button action(AppCompatActivity a,LinearLayout parent,boolean dark,int title,Runnable run){Ui.addSpacer(parent,8);Button button=LedgerUi.action(a,a.getString(title),false,dark,run);parent.addView(button);return button;}
    private static void page(AppCompatActivity a,LinearLayout parent,boolean dark,int title,String page){action(a,parent,dark,title,()->a.startActivity(new Intent(a,SettingsActivity.class).putExtra("settings_page",page)));}
    private static void toggle(AppCompatActivity a,LinearLayout parent,boolean dark,int title,boolean checked,Consumer<Boolean> change){
        SwitchCompat toggle=new SwitchCompat(a);toggle.setText(title);toggle.setTextColor(Ui.mainText(dark));toggle.setMinHeight(Ui.dp(a,48));toggle.setChecked(checked);
        toggle.setOnCheckedChangeListener((button,on)->{change.accept(on);a.recreate();});parent.addView(toggle);
    }
}
