package dev.bennett.codexmeter;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.cert.X509Certificate;
import java.util.Locale;
import java.util.UUID;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import javax.net.ssl.*;
import org.json.JSONObject;

/** Pinned TLS on a private IPv4 LAN. No OAuth tokens, discovery broadcasts or public endpoints. */
public final class LanSyncWire {
    public static final int MAX_COMPRESSED=4*1024*1024, MAX_EXPANDED=32*1024*1024;
    public static final class Pair {
        public final String host,pin,secret;public final int port;
        public Pair(String code) {
            String[] fields=code.trim().split("\\|",-1);
            if(fields.length!=5||!fields[0].equals("cm1")||!privateHost(fields[1])
                ||!fields[2].matches("[0-9]{1,5}")||!fields[3].matches("[a-f0-9]{64}")||!fields[4].matches("[a-f0-9]{64}"))throw new IllegalArgumentException("Invalid LAN pairing code");
            host=fields[1];port=Integer.parseInt(fields[2]);pin=fields[3];secret=fields[4];
            if(port<1||port>65535)throw new IllegalArgumentException("Invalid port");
        }
    }
    public static boolean privateHost(String host) {
        if(host==null||!host.matches("[0-9]{1,3}(\\.[0-9]{1,3}){3}"))return false;
        String[] p=host.split("\\.");int[] b=new int[4];for(int i=0;i<4;i++){b[i]=Integer.parseInt(p[i]);if(b[i]>255||!p[i].equals(Integer.toString(b[i])))return false;}
        return b[0]==10||b[0]==127||b[0]==192&&b[1]==168||b[0]==172&&b[1]>=16&&b[1]<=31;
    }
    public static String hash(byte[] bytes)throws Exception {
        byte[] digest=MessageDigest.getInstance("SHA-256").digest(bytes);StringBuilder out=new StringBuilder();
        for(byte b:digest)out.append(String.format(Locale.ROOT,"%02x",b&255));return out.toString();
    }
    public static JSONObject exchange(Pair pair,String account,JSONObject request)throws Exception {
        String id=UUID.randomUUID().toString().replace("-","");request.put("v",1).put("id",id).put("at",System.currentTimeMillis()).put("account",account).put("secret",pair.secret);
        TrustManager[] trust={new X509TrustManager(){
            public X509Certificate[] getAcceptedIssuers(){return new X509Certificate[0];}
            public void checkClientTrusted(X509Certificate[] c,String a)throws java.security.cert.CertificateException{throw new java.security.cert.CertificateException("Server pin only");}
            public void checkServerTrusted(X509Certificate[] c,String a)throws java.security.cert.CertificateException{
                try{if(c.length!=1||!pair.pin.equals(hash(c[0].getEncoded())))throw new Exception("Pin mismatch");}
                catch(Exception e){throw new java.security.cert.CertificateException("PC certificate does not match pairing",e);}
            }} };
        SSLContext ssl=SSLContext.getInstance("TLS");ssl.init(null,trust,null);
        try(Socket connection=new Socket()) {
            connection.connect(new InetSocketAddress(pair.host,pair.port),5000);connection.setSoTimeout(10000);
            try(SSLSocket socket=(SSLSocket)ssl.getSocketFactory().createSocket(connection,pair.host,pair.port,true)) {
                socket.setSoTimeout(10000);socket.setEnabledProtocols(new String[]{"TLSv1.2"});socket.startHandshake();
                write(socket.getOutputStream(),request.toString());JSONObject response=new JSONObject(read(socket.getInputStream()));
                if(response.getInt("v")!=1||!id.equals(response.getString("id"))||!account.equals(response.getString("account")))throw new IOException("Unexpected sync response");return response;
            }
        }
    }
    public static void write(OutputStream stream,String value)throws IOException {
        byte[] text=value.getBytes(StandardCharsets.UTF_8);if(text.length>MAX_EXPANDED)throw new IOException("Expanded bound");
        ByteArrayOutputStream buffer=new ByteArrayOutputStream();try(GZIPOutputStream gzip=new GZIPOutputStream(buffer)){gzip.write(text);}
        byte[] payload=buffer.toByteArray();if(payload.length>MAX_COMPRESSED)throw new IOException("Frame bound");
        DataOutputStream output=new DataOutputStream(stream);output.writeInt(payload.length);output.write(payload);output.flush();
    }
    public static String read(InputStream stream)throws IOException {
        DataInputStream input=new DataInputStream(stream);int size=input.readInt();if(size<=0||size>MAX_COMPRESSED)throw new IOException("Frame bound");
        byte[] payload=new byte[size];input.readFully(payload);
        try(GZIPInputStream gzip=new GZIPInputStream(new ByteArrayInputStream(payload));ByteArrayOutputStream out=new ByteArrayOutputStream()) {
            byte[] buffer=new byte[8192];int n;while((n=gzip.read(buffer))!=-1){if(out.size()+n>MAX_EXPANDED)throw new IOException("Expanded bound");out.write(buffer,0,n);}return new String(out.toByteArray(),StandardCharsets.UTF_8);
        }
    }
}
