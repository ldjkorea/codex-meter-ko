package dev.bennett.codexmeter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Pure weekly-close evaluator. Display choices and money are deliberately absent. */
public final class TierEvolution {
    public static final int VERSION = 2;
    public static final long WEEK = 604800000L;
    private static final double[] BOUNDS = {10,20,30,45,60,75,85,92,97};
    public static final class Evidence {
        public final String policy;
        public final long end;
        public final double used;
        public final int observations;
        public Evidence(String policy,long end,double used,int observations) {
            if(policy==null || !policy.startsWith("weekly|") || !policy.endsWith("|604800")
                    || end<=0 || !Double.isFinite(used) || used<0 || used>100 || observations<3)
                throw new IllegalArgumentException("Invalid closed weekly evidence");
            this.policy=policy;this.end=end;this.used=used;this.observations=observations;
        }
    }
    public static final class State {
        public final int tier,previous,count,lowStreak;
        public final long end;
        public final double score;
        public State(int tier,int previous,int count,int lowStreak,long end,double score) {
            if(tier < -1 || tier > 9 || previous < -1 || previous > 9 || count < 0 || count > 8
                    || lowStreak < 0 || lowStreak > 1 || end < 0 || !Double.isFinite(score) || score < 0 || score > 100)
                throw new IllegalArgumentException("Invalid tier state");
            this.tier=tier;this.previous=previous;this.count=count;this.lowStreak=lowStreak;this.end=end;this.score=score;
        }
        public static State empty(){return new State(-1,-1,0,0,0,0);}
        public boolean provisional(){return count<4;}
    }
    public static State evaluate(State prior,List<Evidence> all,String policy,long end) {
        if(end<=prior.end)return prior;
        List<Evidence> input=new ArrayList<>();
        for(Evidence e:all)if(e.policy.equals(policy)&&e.end<=end&&e.end> end-11*WEEK)input.add(e);
        input.sort(Comparator.comparingLong(e->e.end));
        List<Evidence> unique=new ArrayList<>();
        for(Evidence e:input)if(unique.isEmpty()||unique.get(unique.size()-1).end!=e.end)unique.add(e);
        if(unique.isEmpty()||unique.get(unique.size()-1).end!=end)return prior;
        if(unique.size()>8)unique=new ArrayList<>(unique.subList(unique.size()-8,unique.size()));
        int n=unique.size();double total=0,weight=0;int strong=0;
        for(int i=0;i<n;i++){double w=i>=n-4?2:1;total+=unique.get(i).used*w;weight+=w;if(unique.get(i).used>=60)strong++;}
        double mean=total/weight;
        // Consistency rewards meaningful use, never steady low use. Trend is bounded to 2 points.
        double trend=n>=4?Math.max(-2,Math.min(2,(unique.get(n-1).used+unique.get(n-2).used-unique.get(n-3).used-unique.get(n-4).used)/20)):0;
        double score=Math.max(0,Math.min(100,mean*.95+5.0*strong/n+trend));
        int desired=0;while(desired<9&&score>=BOUNDS[desired])desired++;
        if(n<4)desired=Math.min(4,desired);else if(n<8)desired=Math.min(7,desired);
        int tier=prior.tier,streak=prior.lowStreak;
        if(n<2)return new State(tier,tier,n,0,end,score);
        if(tier<0)tier=desired;
        else {
            boolean continuous=prior.end>0&&end-prior.end<=WEEK+3600000L;
            if(desired>tier&&score>=BOUNDS[tier]+2&&n>=4){tier++;streak=0;}
            else if(desired<tier&&continuous&&n>=4&&unique.get(n-1).used<BOUNDS[tier-1]-3
                    &&unique.get(n-2).used<BOUNDS[tier-1]-3){
                streak++;if(streak>=2){tier--;streak=0;}
            }else streak=0;
        }
        return new State(tier,prior.tier,n,streak,end,score);
    }
}
