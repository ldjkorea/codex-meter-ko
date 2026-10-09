package dev.bennett.codexmeter;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.text.InputType;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.time.LocalDate;
import java.util.Locale;

/** Local user-entered billing settings, isolated by hashed account and plan. Never exported. */
final class SubscriptionUi {
    private SubscriptionUi() {}
    private static String key(AppCompatActivity activity,UsageSnapshot snapshot){return SubscriptionStore.key(activity,snapshot);}
    static SubscriptionCost load(AppCompatActivity activity,UsageSnapshot snapshot){return SubscriptionStore.load(activity,snapshot);}
    static LinearLayout compact(AppCompatActivity activity,boolean dark){
        UsageSnapshot snapshot=AppPreferences.loadSnapshot(activity);SubscriptionCost cost=load(activity,snapshot);
        LinearLayout card=Ui.card(activity,dark);card.addView(LedgerUi.heading(activity,activity.getString(R.string.fun_title),dark));Ui.addSpacer(card,10);
        card.addView(LedgerUi.caption(activity,cost==null?activity.getString(R.string.v3_payment_missing)
                :activity.getString(R.string.v3_user_payment,money(cost)),dark));
        card.addView(LedgerUi.caption(activity,activity.getString(R.string.fun_preview),dark));Ui.addSpacer(card,12);
        card.addView(LedgerUi.action(activity,activity.getString(R.string.fun_title),false,dark,()->activity.startActivity(
                new android.content.Intent(activity,FunActivity.class))));return card;
    }
    static void render(AppCompatActivity activity,LinearLayout parent,boolean dark,Runnable refresh){
        UsageSnapshot snapshot=AppPreferences.loadSnapshot(activity);SubscriptionCost cost=load(activity,snapshot);
        LinearLayout billing=Ui.card(activity,dark);billing.addView(LedgerUi.heading(activity,activity.getString(R.string.v3_payment),dark));Ui.addSpacer(billing,12);
        billing.addView(LedgerUi.caption(activity,snapshot==null?activity.getString(R.string.v3_payment_missing):snapshot.planType,dark));
        billing.addView(LedgerUi.amount(activity,cost==null?"—":money(cost),dark,32));
        billing.addView(LedgerUi.caption(activity,cost==null?activity.getString(R.string.v3_payment_missing):activity.getString(R.string.v3_billing_range,cost.start.toString(),cost.end.toString()),dark));
        if(cost!=null&&!cost.active(LedgerAggregation.day(System.currentTimeMillis())))billing.addView(LedgerUi.badge(activity,activity.getString(R.string.v3_payment_expired),dark,true));
        Ui.addSpacer(billing,16);Button edit=LedgerUi.action(activity,activity.getString(R.string.v3_payment_edit),true,dark,()->edit(activity,snapshot,dark,refresh));edit.setEnabled(key(activity,snapshot)!=null);billing.addView(edit);parent.addView(billing);
        LedgerUi.section(parent,activity.getString(R.string.v3_value_basis),dark);
        LinearLayout basis=Ui.card(activity,dark);basis.addView(LedgerUi.amount(activity,activity.getString(R.string.v3_value_unknown),dark,23));Ui.addSpacer(basis,12);
        basis.addView(LedgerUi.caption(activity,activity.getString(R.string.v3_value_explanation),dark));Ui.addSpacer(basis,16);
        basis.addView(LedgerUi.action(activity,activity.getString(R.string.v3_pricing_method),false,dark,()->new AlertDialog.Builder(activity)
            .setTitle(R.string.v3_pricing_method).setMessage(R.string.v3_pricing_explanation).setPositiveButton(R.string.ui_done_e9b450,null).show()));parent.addView(basis);
    }
    private static String money(SubscriptionCost cost){return cost.currency+" "+cost.amount.stripTrailingZeros().toPlainString();}
    static void edit(AppCompatActivity activity,UsageSnapshot snapshot,boolean dark,Runnable refresh){
        String accountKey=key(activity,snapshot);if(accountKey==null)return;
        SubscriptionCost old=load(activity,snapshot);LocalDate today=LedgerAggregation.day(System.currentTimeMillis());
        LocalDate[] dates={old==null?today:old.start,old==null?today.plusMonths(1):old.end};
        LinearLayout form=new LinearLayout(activity);form.setOrientation(LinearLayout.VERTICAL);form.setPadding(Ui.dp(activity,24),Ui.dp(activity,12),Ui.dp(activity,24),0);
        form.addView(LedgerUi.caption(activity,activity.getString(R.string.ux_tax_amount),dark));EditText amount=new EditText(activity);amount.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);amount.setHint("0.00");if(old!=null)amount.setText(old.amount.toPlainString());form.addView(amount);
        form.addView(LedgerUi.caption(activity,activity.getString(R.string.v3_currency_hint),dark));EditText currency=new EditText(activity);currency.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);currency.setText(old==null?"KRW":old.currency);form.addView(currency);
        for(int i=0;i<2;i++){final int index=i;Button date=LedgerUi.action(activity,activity.getString(i==0?R.string.v3_billing_start:R.string.v3_billing_end,dates[i].toString()),false,dark,()->{});
            date.setOnClickListener(v->new DatePickerDialog(activity,(picker,year,month,day)->{dates[index]=LocalDate.of(year,month+1,day);date.setText(activity.getString(index==0?R.string.v3_billing_start:R.string.v3_billing_end,dates[index].toString()));},dates[index].getYear(),dates[index].getMonthValue()-1,dates[index].getDayOfMonth()).show());form.addView(date);}
        TextView error=LedgerUi.caption(activity,"",dark);form.addView(error);
        android.widget.ScrollView scroll=new android.widget.ScrollView(activity);scroll.addView(form);
        AlertDialog dialog=new AlertDialog.Builder(activity).setTitle(R.string.v3_payment_edit).setView(scroll).setNegativeButton(R.string.ui_cancel_77dfd2,null).setPositiveButton(R.string.widget_config_save,null).create();
        dialog.setOnShowListener(unused->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
            try{if(!accountKey.equals(key(activity,AppPreferences.loadSnapshot(activity))))throw new IllegalStateException();
                SubscriptionCost cost=new SubscriptionCost(amount.getText().toString().trim(),currency.getText().toString().trim().toUpperCase(Locale.ROOT),dates[0],dates[1]);
                if(!SubscriptionStore.save(activity,snapshot,accountKey,cost))throw new IllegalStateException();
                dialog.dismiss();refresh.run();
            }catch(Exception ignored){error.setText(R.string.v3_payment_invalid);}
        }));dialog.show();
    }
}
