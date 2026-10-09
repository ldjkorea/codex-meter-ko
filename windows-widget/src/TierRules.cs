using System;
using System.Collections.Generic;
using System.Linq;

namespace CodexMeterWidget {
    public sealed class TierState {public int Tier=-1,Previous=-1,Count,LowStreak;public long End;public double Score;}
    public sealed class Evidence {public long End;public double Used;public int Count;}
    // Port of the Android version-2 weekly rule. Current quota percent never creates a rank.
    public static class TierRules {
        public const long Week=604800000;private static readonly double[] Bounds={10,20,30,45,60,75,85,92,97};
        public static List<Evidence> Completed(List<Observation> all,string policy,long now){
            var accepted=new List<Evidence>();
            foreach(var group in all.Where(x=>x.Policy==policy&&x.Seconds==604800&&x.Reset<=now&&x.Reset>x.At).GroupBy(x=>x.Reset)){
                var rows=group.OrderBy(x=>x.At).GroupBy(x=>x.At).Select(x=>x.First()).ToList();var first=rows.First();var last=rows.Last();long start=last.Reset-Week;
                if(rows.Count<3||first.At-start>3600000||last.Reset-last.At>3600000||last.At-first.At<Week*.8)continue;
                bool safe=true;for(int i=1;i<rows.Count;i++)if(rows[i].Used<rows[i-1].Used||rows[i].At-rows[i-1].At>6*3600000L)safe=false;
                if(all.Any(x=>x.Meter=="weekly"&&x.At>=start&&x.At<last.Reset&&(x.Policy!=policy||x.Reset!=last.Reset)))safe=false;
                if(safe)accepted.Add(new Evidence{End=last.Reset,Used=last.Used,Count=rows.Count});
            }return accepted.OrderBy(x=>x.End).ToList();
        }
        public static TierState Evaluate(TierState prior,List<Evidence> all,long end){
            if(end<=prior.End)return prior;var unique=all.Where(x=>x.End<=end&&x.End>end-11*Week&&x.Count>=3&&x.Used>=0&&x.Used<=100&&!double.IsNaN(x.Used)).OrderBy(x=>x.End).GroupBy(x=>x.End).Select(x=>x.First()).ToList();
            if(unique.Count==0||unique.Last().End!=end)return prior;if(unique.Count>8)unique=unique.Skip(unique.Count-8).ToList();int n=unique.Count,strong=0;double total=0,weight=0;
            for(int i=0;i<n;i++){double w=i>=n-4?2:1;total+=unique[i].Used*w;weight+=w;if(unique[i].Used>=60)strong++;}
            double trend=n>=4?Math.Max(-2,Math.Min(2,(unique[n-1].Used+unique[n-2].Used-unique[n-3].Used-unique[n-4].Used)/20)):0,score=Math.Max(0,Math.Min(100,total/weight*.95+5.0*strong/n+trend));
            int desired=0;while(desired<9&&score>=Bounds[desired])desired++;if(n<4)desired=Math.Min(4,desired);else if(n<8)desired=Math.Min(7,desired);
            int tier=prior.Tier,streak=prior.LowStreak;if(n<2)return new TierState{Tier=tier,Previous=tier,Count=n,End=end,Score=score};
            if(tier<0)tier=desired;else {bool continuous=prior.End>0&&end-prior.End<=Week+3600000;if(desired>tier&&score>=Bounds[tier]+2&&n>=4){tier++;streak=0;}else if(desired<tier&&continuous&&n>=4&&unique[n-1].Used<Bounds[tier-1]-3&&unique[n-2].Used<Bounds[tier-1]-3){streak++;if(streak>=2){tier--;streak=0;}}else streak=0;}
            return new TierState{Tier=tier,Previous=prior.Tier,Count=n,LowStreak=streak,End=end,Score=score};
        }
        public static TierState FromRows(List<Observation> rows,string policy,long now){var evidence=Completed(rows,policy,now);var state=new TierState();foreach(var e in evidence)state=Evaluate(state,evidence,e.End);return state;}
    }
}
