package dev.bennett.codexmeter;
import java.util.*;
public final class TierEvolutionSelfTest {
    static int checks;
    static final String P="weekly|plus|604800";
    static final long BASE=1700000000000L;
    static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    static List<TierEvolution.Evidence> weeks(double... used){List<TierEvolution.Evidence> r=new ArrayList<>();for(int i=0;i<used.length;i++)r.add(new TierEvolution.Evidence(P,BASE+(i+1)*TierEvolution.WEEK,used[i],30));return r;}
    static TierEvolution.State run(List<TierEvolution.Evidence> e){TierEvolution.State s=TierEvolution.State.empty();for(TierEvolution.Evidence w:e){TierEvolution.State n=TierEvolution.evaluate(s,e,P,w.end);if(s.tier>=0)check(Math.abs(n.tier-s.tier)<=1,"At most one tier move");s=n;}return s;}
    public static void main(String[] args)throws Exception{
        check(run(weeks()).tier==-1,"New account placing");check(run(weeks(100)).tier==-1,"One 100% week cannot place");
        TierEvolution.State two=run(weeks(95,95));check(two.provisional()&&two.tier<=4,"Two weeks provisional and capped");
        TierEvolution.State four=run(weeks(80,80,80,80));check(!four.provisional()&&four.count==4,"Four weeks stable");
        TierEvolution.State eight=run(weeks(100,100,100,100,100,100,100,100));check(eight.count==8&&eight.tier<9,"Eight weeks does not jump to highest");
        check(run(weeks(100,100,100,100,100,100,100,100,100,100,100,100)).tier==9,"Sustained high use eventually Challenger");
        check(run(weeks(5,5,5,5,5,5,5,5)).tier==0,"Steady low utilization stays low");
        List<TierEvolution.Evidence> rest=weeks(90,90,90,90,90,90,90,90,0);TierEvolution.State before=run(rest.subList(0,8));
        TierEvolution.State after=TierEvolution.evaluate(before,rest,P,rest.get(8).end);check(after.tier>=before.tier,"One rest is not demotion");
        check(run(weeks(90,90,90,90,90,90,90,90,0,0,0,0,0,0,0,0)).tier<before.tier,"Repeated low use can adjust down");
        check(TierEvolution.evaluate(after,rest,P,after.end)==after,"Same end is idempotent");
        check(TierEvolution.evaluate(after,rest,"weekly|pro|604800",after.end+TierEvolution.WEEK)==after,"Plan isolation");
        check(TierEvolution.evaluate(after,rest,P,after.end+TierEvolution.WEEK)==after,"No fabricated missing week");
        List<TierEvolution.Evidence> gap=new ArrayList<>(rest);gap.add(new TierEvolution.Evidence(P,after.end+4*TierEvolution.WEEK,0,40));
        check(TierEvolution.evaluate(after,gap,P,gap.get(gap.size()-1).end).tier>=after.tier,"Observation gap is not demotion");
        List<TierEvolution.Evidence> duplicate=new ArrayList<>(weeks(80,80,80,80));duplicate.add(duplicate.get(3));check(run(duplicate).count==4,"Duplicate closed window counted once");
        boolean bad=false;try{new TierEvolution.Evidence("five_hour|plus|18000",BASE,100,40);}catch(IllegalArgumentException e){bad=true;}check(bad,"No mixing quota types");
        check(run(weeks(20,25,40,50,60,70,80,90)).tier>run(weeks(90,80,70,60,50,40,25,20)).tier,"Long term increasing and declining patterns separated");
        boolean invalidState=false;try{new TierEvolution.State(100,0,4,0,BASE,80);}catch(IllegalArgumentException e){invalidState=true;}
        check(invalidState,"Corrupt out-of-range persisted tier rejected");
        for(int mask=1;mask<32;mask++){
            List<String> selected=new ArrayList<>();for(int i=0;i<5;i++)if((mask&(1<<i))!=0)selected.add(EvolutionElements.ALL[i]);
            String csv=EvolutionElements.save(selected);check(EvolutionElements.parse(csv).equals(selected),"All 1–5 combinations persist order");
            List<String> hidden=EvolutionElements.visible(csv,false,true);check(hidden.stream().noneMatch(k->k.startsWith("five")),"OFF fully removes gauge and countdown");
            check(EvolutionElements.visible(csv,true,true).equals(selected),"ON restores selection");
            check(EvolutionElements.columns(selected.size(),180)<=2,"Narrow widget uses multiple rows");
        }
        check(EvolutionElements.parse("tier,tier,invalid,weekly").size()==2,"No duplicate elements");
        check(EvolutionElements.visible("five,five_reset",false,true).isEmpty(),"No fallback when all chosen elements hidden");
        boolean empty=false;try{EvolutionElements.save(Collections.emptyList());}catch(IllegalArgumentException e){empty=true;}check(empty,"At least one element");
        check(KoreanUpdateTrust.official("https://github.com/ldjkorea/codex-meter-ko/releases/tag/v2.8.8-beta"),"Own release accepted");
        for(String url:new String[]{"http://github.com/ldjkorea/codex-meter-ko/releases/tag/v2.8.8-beta","https://github.com.evil/ldjkorea/codex-meter-ko/releases/tag/x","https://github.com/BenItBuhner/Codex-Meter/releases/tag/x","https://github.com/ldjkorea/codex-meter-ko/releases/../x","https://github.com/ldjkorea/codex-meter-ko/releases/%2e%2e/x"})check(!KoreanUpdateTrust.official(url),"Foreign, insecure and traversal URL rejected");
        check(KoreanUpdateTrust.compatible("dev.bennett.codexmeter",KoreanUpdateTrust.CERTIFICATE,37,38),"Valid update identity");
        check(!KoreanUpdateTrust.compatible("foreign",KoreanUpdateTrust.CERTIFICATE,37,38),"Wrong package rejected");
        check(!KoreanUpdateTrust.compatible("dev.bennett.codexmeter","other",37,38),"Wrong certificate rejected");
        for(long code:new long[]{36,37})check(!KoreanUpdateTrust.compatible("dev.bennett.codexmeter",KoreanUpdateTrust.CERTIFICATE,37,code),"Equal or older code rejected");
        System.out.println("Tier evolution, 31 widget combinations and update trust: "+checks+" assertions passed (JVM, not native UI).");
    }
}
