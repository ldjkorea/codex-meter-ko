package dev.bennett.codexmeter;
import java.io.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import org.json.*;

/** Executes the same JVM wire used by Android against the real C# TLS listener, synthetic account only. */
public final class LanSyncSelfTest {
    static int checks;
    static void check(boolean value,String note){checks++;if(!value)throw new AssertionError(note);}
    interface Action{void run()throws Exception;}
    static void reject(Action action,String note)throws Exception{try{action.run();}catch(Exception expected){checks++;return;}throw new AssertionError(note);}
    public static void main(String[] args)throws Exception {
        for(String ip:new String[]{"10.1.2.3","192.168.1.7","172.16.0.1","172.31.255.254","127.0.0.1"})check(LanSyncWire.privateHost(ip),"private IPv4");
        for(String ip:new String[]{"8.8.8.8","172.32.0.1","host.example","192.168.0.999","192.168.01.1","::1"})check(!LanSyncWire.privateHost(ip),"reject external/DNS/ambiguous address");
        ByteArrayOutputStream out=new ByteArrayOutputStream();LanSyncWire.write(out,"한국어 실제 관측 37.125");check(LanSyncWire.read(new ByteArrayInputStream(out.toByteArray())).equals("한국어 실제 관측 37.125"),"UTF8/gzip roundtrip");
        reject(()->LanSyncWire.read(new ByteArrayInputStream(new byte[]{0,64,0,1})),"compressed size bound");
        reject(()->new LanSyncWire.Pair("cm1|8.8.8.8|1234|"+"a".repeat(64)+"|"+"b".repeat(64)),"pairing rejects public IP");
        if(args.length==0){System.out.println("LAN pairing/framing: "+checks+" assertions passed (no network).");return;}
        String code=Files.readString(Path.of(args[0]),StandardCharsets.UTF_8);LanSyncWire.Pair pair=new LanSyncWire.Pair(code);
        String account=LanSyncWire.hash("lan-integration".getBytes(StandardCharsets.UTF_8));
        JSONObject pull=LanSyncWire.exchange(pair,account,new JSONObject().put("op","pull").put("floor",0));JSONArray rows=pull.getJSONArray("rows");check(rows.length()==1&&rows.getJSONObject(0).getDouble("Used")==25,"C# PC observation arrives in Android wire");
        JSONObject row=new JSONObject(rows.getJSONObject(0).toString()).put("At",System.currentTimeMillis()).put("Used",37.125).put("Decimal","37.125").put("Source","api_precise").put("Manual",true);rows.put(row);
        JSONObject payload=new JSONObject().put("op","push").put("floor",0).put("observed",row.getLong("At")).put("reset",row.getLong("Reset")).put("tier",7).put("policy","weekly|pro|604800").put("percent",87.5).put("rows",rows).put("days",new JSONArray().put(new JSONObject().put("day","2026-01-01").put("points",13.25))).put("events",new JSONArray());
        check(LanSyncWire.exchange(pair,account,new JSONObject(payload.toString())).getBoolean("ok"),"Android history/tier committed by PC");check(LanSyncWire.exchange(pair,account,new JSONObject(payload.toString())).getBoolean("ok"),"retry acknowledged");
        JSONObject repeated=LanSyncWire.exchange(pair,account,new JSONObject().put("op","pull").put("floor",0));check(repeated.getJSONArray("rows").length()==2,"bidirectional dedup");
        Path dir=Path.of(args[0]).getParent();Path mirror=dir.resolve("accounts").resolve(account).resolve("phone-mirror.json");JSONObject saved=new JSONObject(Files.readString(mirror));check(saved.getInt("Tier")==7,"canonical phone tier reused immediately");
        String archive=Files.readString(mirror.getParent().resolve("shared-history.json"));check(archive.contains("2026-01-01")&&!archive.contains(pair.secret)&&!archive.contains("synthetic-access"),"past daily archive transferred without tokens/secret");
        LanSyncWire.Pair wrongPin=new LanSyncWire.Pair(code.replace(pair.pin,"0".repeat(64)));reject(()->LanSyncWire.exchange(wrongPin,account,new JSONObject().put("op","pull").put("floor",0)),"wrong TLS certificate rejected");
        LanSyncWire.Pair wrongSecret=new LanSyncWire.Pair(code.replace(pair.secret,"0".repeat(64)));reject(()->LanSyncWire.exchange(wrongSecret,account,new JSONObject().put("op","pull").put("floor",0)),"wrong pairing key rejected");
        reject(()->LanSyncWire.exchange(pair,LanSyncWire.hash("other-account".getBytes(StandardCharsets.UTF_8)),new JSONObject().put("op","pull").put("floor",0)),"other account rejected");
        check(LanSyncWire.exchange(pair,account,new JSONObject().put("op","pull").put("floor",0)).getJSONArray("rows").length()==2,"server remains usable after rejected peers");
        System.out.println("LAN Java/C# TLS integration: "+checks+" assertions passed; synthetic loopback only, no physical phone proof.");
    }
}
