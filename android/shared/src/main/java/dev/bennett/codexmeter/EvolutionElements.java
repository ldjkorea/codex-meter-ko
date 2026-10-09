package dev.bennett.codexmeter;
import java.util.*;
/** Pure ordered layout policy; saved selection survives temporary visibility filtering. */
public final class EvolutionElements {
    public static final String[] ALL={"five","weekly","tier","five_reset","weekly_reset"};
    public static final String DEFAULT="weekly,tier,weekly_reset";
    public static List<String> parse(String csv){List<String> r=new ArrayList<>();if(csv!=null)for(String k:csv.split(","))if(Arrays.asList(ALL).contains(k)&&!r.contains(k))r.add(k);return r;}
    public static String save(List<String> list){List<String> clean=parse(String.join(",",list));if(clean.isEmpty())throw new IllegalArgumentException("Select at least one element");return String.join(",",clean);}
    public static List<String> visible(String csv,boolean five,boolean weekly){List<String> r=parse(csv);r.removeIf(k->k.startsWith("five")&&!five||k.startsWith("weekly")&&!weekly);return r;}
    public static int columns(int count,int width){return count<=2?Math.max(1,count):width<300?2:count==3?3:count==4&&width<480?2:width<600?3:count;}
}
