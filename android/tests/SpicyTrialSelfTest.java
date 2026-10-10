package dev.bennett.codexmeter;

import java.nio.file.*;
import java.util.*;
import org.json.*;

public final class SpicyTrialSelfTest {
    static int checks;static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    static String group(double used){return SpicyRotation.group(used,Double.NaN,true,false,3*86400000L,604800,false);}
    public static void main(String[] args)throws Exception {
        Map<String,int[]> groups=new HashMap<>();List<String> text=new ArrayList<>();List<Integer> ids=new ArrayList<>();String group="";
        long prepare=System.nanoTime();for(String line:Files.readAllLines(Path.of(args[0]))){if(line.isEmpty())continue;if(line.startsWith("[")){if(!group.isEmpty())groups.put(group,ids.stream().mapToInt(i->i).toArray());group=line.substring(1,line.length()-1);ids.clear();}else{ids.add(text.size());text.add(line);}}groups.put(group,ids.stream().mapToInt(i->i).toArray());prepare=System.nanoTime()-prepare;
        check(text.size()==600,"600 active literal lines");Set<String> distinct=new HashSet<>();for(String line:text){check(distinct.add(line.replaceAll("[\\s\\p{P}]", "")),"No normalized exact duplicates");check(line.length()<=110,"Short Korean copy");}
        double[] values={0,.001,4.999,5,14.999,15,29.999,30,49.999,50,69.999,70,89.999,90,100};String[] expected={"zero","tiny","tiny","low","low","moderate","moderate","mid","mid","high","high","heavy","heavy","critical","critical"};for(int i=0;i<values.length;i++)check(group(values[i]).equals(expected[i]),"Used-percent boundary "+values[i]);
        check(group(5).equals("low")&&group(95).equals("critical"),"Used versus remaining");
        check(SpicyRotation.group(5,13,true,false,3*86400000L,604800,false).equals("burst"),"Daily 13 percentage points");
        check(SpicyRotation.group(5,.13,true,false,3*86400000L,604800,false).equals("low"),"Daily not fraction");
        check(SpicyRotation.group(5,Double.NaN,true,false,604800000L-1000,604800,false).equals("reset"),"Reset instead of lazy copy");
        check(SpicyRotation.group(5,Double.NaN,true,false,3600000,604800,false).equals("imminent"),"Imminent");
        check(SpicyRotation.group(95,13,false,true,3600000,604800,false).equals("stale"),"No stale bonus/burst claim");
        check(SpicyRotation.group(Double.NaN,Double.NaN,true,false,0,0,false).equals("missing"),"Unknown not zero");
        check(SpicyRotation.group(5,Double.NaN,true,true,3600000,604800,false).equals("bonus"),"Confirmed extra");
        check(SpicyRotation.group(5,Double.NaN,true,false,3600000,604800,true).equals("network"),"Confirmed failed query");
        Random random=new Random(472817);SpicyRotation.State state=new SpicyRotation.State();int[] candidates=groups.get("low");check(candidates.length==80,"5% has 80 eligible lines");List<Integer> observed=new ArrayList<>();
        for(int i=0;i<240;i++){int id=state.choose("low",candidates,i,random);check(!observed.subList(Math.max(0,observed.size()-20),observed.size()).contains(id),"Exclude last twenty across exhaustion");observed.add(id);check(state.choose("low",candidates,i,random)==id,"Rotation/countdown/background keep generation");}
        SpicyRotation.State restored=new SpicyRotation.State();restored.current=state.current;restored.group=state.group;restored.generation=state.generation;restored.size=state.size;System.arraycopy(state.recent,0,restored.recent,0,state.size);
        check(restored.choose("low",candidates,240,random)!=state.current,"Restart persisted recent IDs");check(restored.choose("critical",groups.get("critical"),240,random)>=groups.get("critical")[0],"Context overrides repeat rules");
        SpicyRotation.State zero=new SpicyRotation.State();int previous=-1;for(int i=0;i<60;i++){int current=zero.choose("zero",groups.get("zero"),i,random);check(current!=previous,"Small group relaxes oldest, not previous");previous=current;}
        long[] times=new long[10000];for(int i=0;i<2000;i++)state.choose("low",candidates,i+1000,random);for(int i=0;i<times.length;i++){long at=System.nanoTime();state.choose("low",candidates,i+5000,random);times[i]=System.nanoTime()-at;}Arrays.sort(times);
        long now=System.currentTimeMillis();JSONObject source=new JSONObject().put("epoch","a".repeat(32)).put("device","b".repeat(64)).put("source","local_log_trial").put("checked",now).put("verified",true).put("rows",new JSONArray().put(new JSONObject().put("Thread","c".repeat(32)).put("Turn","d".repeat(32)).put("State","running").put("Name","private prompt").put("command","secret command").put("At",now).put("Seen",now).put("Sequence",1)));
        JSONObject clean=TaskStatusData.clean(source,now);check(!clean.toString().contains("private prompt")&&!clean.toString().contains("command"),"Only metadata allow-list");
        JSONObject older=new JSONObject(clean.toString()).put("checked",now-1);check(!TaskStatusData.newer(clean,older),"Reverse contact rejected");check(TaskStatusData.clean(null,now).getJSONArray("rows").isEmpty(),"OFF peer unverified");
        source.getJSONArray("rows").getJSONObject(0).put("State","failed");boolean rejected=false;try{TaskStatusData.clean(source,now);}catch(IllegalArgumentException e){rejected=true;}check(rejected,"Unobservable failure rejected");
        System.out.println("Spicy/task metadata: "+checks+" assertions; 600 literals; 5%: 240 draws without last-20 repetition. Prepare="+prepare/1e6+"ms; choose p50="+times[5000]/1e6+"ms p95="+times[9500]/1e6+"ms (JVM, not Android frame proof).");
    }
}
