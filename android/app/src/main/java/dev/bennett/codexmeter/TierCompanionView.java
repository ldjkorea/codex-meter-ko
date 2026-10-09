package dev.bennett.codexmeter;

import android.content.Context;
import android.widget.ImageView;
import android.animation.ValueAnimator;
import android.provider.Settings;

/** Bounded promotion pulse, no recurring animator; respects visibility and system motion scale. */
final class TierCompanionView extends ImageView {
    private ValueAnimator animator;
    private final int tier;
    public TierCompanionView(Context c){this(c,-1);}
    TierCompanionView(Context c,int tier){super(c);this.tier=tier;setImageResource(TierTheme.MASCOTS[Math.max(0,tier)]);if(tier<0)setAlpha(.45f);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);}
    void promote(){if(!TierTheme.effects(getContext())||Settings.Global.getFloat(getContext().getContentResolver(),Settings.Global.ANIMATOR_DURATION_SCALE,1)==0)return;
        String policy=TierStore.policy(getContext()),key=SubscriptionStore.key(getContext(),AppPreferences.loadSnapshot(getContext()));
        String marker=key+"|"+policy+"|"+TierStore.read(getContext(),policy).end;
        android.content.SharedPreferences p=getContext().getSharedPreferences("codex_tier_effects",0);
        if(marker.equals(p.getString("last_promotion","")))return;
        p.edit().putString("last_promotion",marker).apply();pulse(1.035f,900);}
    private void pulse(float peak,long duration){if(!TierTheme.effects(getContext())||Settings.Global.getFloat(getContext().getContentResolver(),Settings.Global.ANIMATOR_DURATION_SCALE,1)==0)return;
        stop();animator=ValueAnimator.ofFloat(1,peak,1);animator.setDuration(duration);animator.addUpdateListener(v->{if(!getGlobalVisibleRect(new android.graphics.Rect())){stop();return;}float s=(float)v.getAnimatedValue();setScaleX(s);setScaleY(s);});animator.start();}
    @Override protected void onAttachedToWindow(){super.onAttachedToWindow();if(tier>=7)post(()->{if(isAttachedToWindow())pulse(1.012f,1600);});}
    private void stop(){if(animator!=null){animator.cancel();animator=null;}setScaleX(1);setScaleY(1);}
    @Override protected void onDetachedFromWindow(){stop();super.onDetachedFromWindow();}
    @Override protected void onWindowVisibilityChanged(int v){super.onWindowVisibilityChanged(v);if(v!=VISIBLE)stop();}
    @Override protected void onVisibilityChanged(android.view.View v,int state){super.onVisibilityChanged(v,state);if(state!=VISIBLE)stop();}
}
