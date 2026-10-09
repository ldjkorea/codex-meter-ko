package dev.bennett.codexmeter;

import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.Drawable;

/** Local original metal/crystal frames. No downloaded images or observation writes. */
final class TierTheme {
    static final int[] COLORS={0xffa9b3bd,0xffdfa778,0xffd5e1eb,0xffe8ce91,0xffaee4ee,0xff68e1b1,0xff9bdcfa,0xffc7a7f4,0xfff1a0ac,0xffffdfa1};
    static final int[] MASCOTS={R.drawable.evo_0,R.drawable.evo_1,R.drawable.evo_2,R.drawable.evo_3,R.drawable.evo_4,R.drawable.evo_5,R.drawable.evo_6,R.drawable.evo_7,R.drawable.evo_8,R.drawable.evo_9};
    static final int[] EMBLEMS={R.drawable.badge_iron,R.drawable.badge_bronze,R.drawable.badge_silver,R.drawable.badge_gold,R.drawable.badge_platinum,R.drawable.badge_emerald,R.drawable.badge_diamond,R.drawable.badge_master,R.drawable.badge_grandmaster,R.drawable.badge_challenger};
    private static String cachedBlob="",cachedPolicy="",cachedRevision="";
    private static int cachedTier=-1;
    static synchronized int tier(Context c){
        // Read only encrypted identity change markers here; never decrypt tokens per chart/bar/color.
        String blob=c.getSharedPreferences("secure_auth_v1",0).getString("blob","");
        String policy=TierStore.policy(c),revision=c.getSharedPreferences("codex_tier_evolution_v2",0).getString("revision","");
        if(!blob.equals(cachedBlob)||!policy.equals(cachedPolicy)||!revision.equals(cachedRevision)){
            int tier=blob.isEmpty()?-1:TierStore.read(c,policy).tier;
            if(!blob.equals(c.getSharedPreferences("secure_auth_v1",0).getString("blob","")))return -1;
            cachedTier=tier;cachedBlob=blob;cachedPolicy=policy;cachedRevision=revision;
        }return cachedTier;
    }
    static int accent(Context c,boolean dark){int t=tier(c);int color=t<0?0xffc2c9d0:COLORS[t];
        if(!dark){float[] hsv=new float[3];Color.colorToHSV(color,hsv);hsv[1]=Math.max(.3f,hsv[1]);hsv[2]=.38f;color=Color.HSVToColor(hsv);}return color;}
    static boolean effects(Context c){return c.getSharedPreferences("codex_tier_effects",0).getBoolean("enabled",true);}
    static void frame(android.view.View view,int tier,boolean dark){view.setBackground(new Frame(tier,dark));}
    static final class Frame extends Drawable {
        final int tier;final boolean dark,fill;final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
        Frame(int t,boolean d){this(t,d,true);}
        Frame(int t,boolean d,boolean f){tier=t;dark=d;fill=f;}
        public void draw(Canvas c){Rect b=getBounds();float density=c.getDensity()>0?c.getDensity()/160f:1;
            float pad=3*density,r=tier<2?10:22;RectF box=new RectF(b.left+pad,b.top+pad,b.right-pad,b.bottom-pad);
            p.setShader(null);p.setStyle(Paint.Style.FILL);p.setColor(dark?0xff17191c:0xfff9f9fa);if(fill)c.drawRoundRect(box,r*density,r*density,p);
            int color=tier<0?0xff66717b:COLORS[tier];p.setColor(color);p.setAlpha(145);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(density);
            c.drawRoundRect(box,r*density,r*density,p);
            if(tier>=3){box.inset(4*density,4*density);p.setAlpha(60);c.drawRoundRect(box,r*density,r*density,p);}
            p.setAlpha(170);int detail=tier<0?0:tier+1;float cy=b.top+13*density;
            for(int i=0;i<detail;i++){float x=b.centerX()+(i-(detail-1)/2f)*10*density;
                if(tier>=5){Path gem=new Path();gem.moveTo(x,cy-4*density);gem.lineTo(x+3*density,cy);gem.lineTo(x,cy+4*density);gem.lineTo(x-3*density,cy);gem.close();c.drawPath(gem,p);}
                else c.drawLine(x,cy,x+4*density,cy,p);}
            p.setAlpha(95);for(int i=0;i<Math.min(4,Math.max(1,tier));i++){float y=b.bottom-(12+i*4)*density;
                c.drawLine(b.left+12*density,y,b.left+(24+i*3)*density,y,p);c.drawLine(b.right-12*density,y,b.right-(24+i*3)*density,y,p);}
            if(tier>=7){p.setAlpha(90);c.drawLine(b.left+5*density,b.top+30*density,b.left+5*density,b.top+65*density,p);c.drawLine(b.right-5*density,b.top+30*density,b.right-5*density,b.top+65*density,p);}
            if(tier==0){p.setAlpha(55);for(int i=0;i<7;i++)c.drawLine(b.left+(8+i*3)*density,b.top+5*density,b.left+(12+i*3)*density,b.top+9*density,p);}
            if(tier>=4){p.setAlpha(95);c.drawLine(b.left+11*density,b.top+24*density,b.left+22*density,b.top+24*density,p);c.drawLine(b.right-11*density,b.top+24*density,b.right-22*density,b.top+24*density,p);}
            p.setAlpha(255);
        }
        public void setAlpha(int a){}public void setColorFilter(ColorFilter f){p.setColorFilter(f);}public int getOpacity(){return PixelFormat.TRANSLUCENT;}
    }
    static android.widget.FrameLayout companion(android.widget.ImageView mascot,int tier){Context c=mascot.getContext();android.widget.FrameLayout frame=new android.widget.FrameLayout(c);
        frame.addView(mascot,new android.widget.FrameLayout.LayoutParams(-1,-1));
        android.widget.ImageView badge=new android.widget.ImageView(c);badge.setImageResource(EMBLEMS[Math.max(0,tier)]);badge.setAlpha(tier<0?.35f:1);badge.setImportantForAccessibility(android.view.View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        android.widget.FrameLayout.LayoutParams p=new android.widget.FrameLayout.LayoutParams(Ui.dp(c,44),Ui.dp(c,44),android.view.Gravity.BOTTOM|android.view.Gravity.END);frame.addView(badge,p);return frame;}
}
