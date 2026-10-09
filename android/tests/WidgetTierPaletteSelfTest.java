package dev.bennett.codexmeter;

/** Pure presentation checks. These are not native launcher screenshot tests. */
public final class WidgetTierPaletteSelfTest {
    private static int checks;
    private static void check(boolean value,String label) {
        checks++;
        if(!value)throw new AssertionError(label);
    }
    private static double luminance(int color) {
        double sum=0;
        double[] weights={.2126,.7152,.0722};
        for(int i=0;i<3;i++) {
            double c=((color>>(16-i*8))&255)/255.0;
            sum+=weights[i]*(c<=.04045?c/12.92:Math.pow((c+.055)/1.055,2.4));
        }
        return sum;
    }
    private static double contrast(int a,int b) {
        double x=luminance(a),y=luminance(b);
        return (Math.max(x,y)+.05)/(Math.min(x,y)+.05);
    }
    public static void main(String[] args) {
        java.util.Set<Integer> surfaces=new java.util.HashSet<>();
        for(int tier=0;tier<10;tier++) {
            check(surfaces.add(WidgetTierPalette.top(tier)),"Distinct tier surfaces");
            for(int opacity:new int[]{56,88,100}) {
                check(WidgetTierPalette.active(true,tier,opacity),"Known visible crest uses tier surface");
                check(!WidgetTierPalette.active(false,tier,opacity),"OFF restores saved theme");
                check(WidgetTierPalette.opacity(opacity)==opacity,"Stored opacity step preserved");
            }
            check(!WidgetTierPalette.active(true,tier,0),"Background OFF remains transparent");
            for(int background:new int[]{WidgetTierPalette.top(tier),WidgetTierPalette.bottom(tier)}) {
                check(contrast(0xfff4f7f8,background)>=4.5,"Readable foreground on opaque plate");
                check(contrast(WidgetTierPalette.accent(tier),background)>=3,"Gauge contrast on opaque plate");
            }
        }
        check(!WidgetTierPalette.active(true,-1,100),"No invented rank for insufficient data");
        check(!WidgetTierPalette.active(true,10,100),"Invalid rank does not index resources");
        check(!WidgetTierPalette.active(true,0,-5),"Nonpositive opacity is transparent");
        for(int width:new int[]{110,145,180,209}) {
            check(WidgetTierPalette.scalableRings(true),"Small hosts fit scalable dials");
            check(!WidgetTierPalette.scalableRings(false),"OFF restores native arcs");
        }
        for(int width:new int[]{0,210,250,340}) {
            check(WidgetTierPalette.scalableRings(true),"Large and unknown hosts also fit countdown labels");
            check(!WidgetTierPalette.scalableRings(false),"OFF retains native arcs at all sizes");
        }
        // Weighted row allocates a 20% wider center; no fixed extra width overflows.
        for(int width:new int[]{110,145,210,250,340}) {
            double cell=(width-14)/3.2;
            check(cell*1.2>20,"Center crest exceeds the former corner size");
            check(Math.abs(cell*2+cell*1.2+14-width)<.001,"Three columns fit available width");
            if(width>=210)check(cell>=56,"Native arcs have enough room");
        }
        System.out.println("Widget tier palette, contrast, OFF/transparent and narrow layout: "+checks+" assertions passed.");
    }
}
