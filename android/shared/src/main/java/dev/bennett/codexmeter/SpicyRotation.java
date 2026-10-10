package dev.bennett.codexmeter;

import java.util.Random;

/** Presentation only: never derives consumption, settlements or account value. */
public final class SpicyRotation {
    private SpicyRotation() {}
    public static String group(double used,double daily,boolean fresh,boolean bonus,long remaining,long seconds,boolean error) {
        if(error)return "network";
        if(!Double.isFinite(used))return "missing";
        if(!fresh)return "stale";
        if(bonus)return "bonus";
        if(used<15&&seconds>0&&remaining>0&&seconds*1000-remaining>=0&&seconds*1000-remaining<2*3600000)return "reset";
        if(used<15&&remaining>0&&remaining<=86400000)return "imminent";
        if(Double.isFinite(daily)&&daily>=13)return "burst";
        if(used==0)return "zero";
        if(used<5)return "tiny";
        if(used<15)return "low";
        if(used<30)return "moderate";
        if(used<50)return "mid";
        if(used<70)return "high";
        if(used<90)return "heavy";
        return "critical";
    }
    public static final class State {
        public int current=-1;public String group="";public long generation=Long.MIN_VALUE;
        public final int[] recent=new int[20];public int size;
        public int choose(String next,int[] candidates,long request,Random random) {
            boolean valid=false;for(int id:candidates)if(id==current)valid=true;
            if(valid&&group.equals(next)&&generation==request)return current;
            if(candidates.length==0)return -1;
            int avoid=size,chosen=-1;
            while(chosen<0){int count=0;for(int id:candidates){boolean excluded=false;
                for(int j=size-avoid;j<size;j++)if(recent[j]==id)excluded=true;
                if(!excluded&&random.nextInt(++count)==0)chosen=id;}
                if(chosen<0){if(avoid>0)avoid--;else return -1;}}
            if(size==recent.length){System.arraycopy(recent,1,recent,0,--size);}recent[size++]=chosen;
            current=chosen;group=next;generation=request;return chosen;
        }
    }
}
