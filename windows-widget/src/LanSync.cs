using System;
using System.Collections.Generic;
using System.IO;
using System.IO.Compression;
using System.Linq;
using System.Net;
using System.Net.Security;
using System.Net.Sockets;
using System.Security.Authentication;
using System.Security.Cryptography;
using System.Security.Cryptography.X509Certificates;
using System.Text;
using System.Threading;
using System.Threading.Tasks;
using System.Web.Script.Serialization;

namespace CodexMeterWidget {
    // Private LAN only; certificate pin and random pairing secret. Never transfers OAuth credentials.
    public sealed class PhoneMirror {
        public string Account,Policy; public long At,Received,Reset,Floor; public int Tier=-1; public double Percent; public double? Today;
        public bool Current(Snapshot snapshot) { return snapshot!=null && snapshot.Fresh(Clock.Now)
            && snapshot.Main!=null && Policy==snapshot.Main.Meter+"|"+(snapshot.Plan??"unknown").Trim().ToLowerInvariant()+"|"+snapshot.Main.Seconds
            && Reset>0&&Math.Abs(Reset-snapshot.Main.Reset)<=Math.Min(900000,Math.Max(60000,snapshot.Main.Seconds*1000/20))
            && At<=Clock.Now && Clock.Now-At<=15*60000; }
    }
    public sealed partial class LocalStore {
        public Func<Dictionary<string,object>> TaskStatus;
        public PhoneMirror LoadMirror(string account) { lock(gate){string p=Path.Combine(DirectoryPath,"accounts",AccountKey(account),"phone-mirror.json");return File.Exists(p)?LanSync.JsonCodec.Deserialize<PhoneMirror>(File.ReadAllText(p)):null;} }
        public Dictionary<string,object> Exchange(Dictionary<string,object> request,string secret) { lock(gate){
            var tokens=LoadTokens();string id=Json.Text(request,"id");
            if(tokens==null||Json.Text(request,"account")!=AccountKey(tokens.Account)||!LanSync.Equal(Json.Text(request,"secret"),secret)
                ||Json.Integer(request,"v")!=1||!System.Text.RegularExpressions.Regex.IsMatch(id,"^[a-f0-9]{32}$")
                ||Math.Abs(Json.Integer(request,"at")-Clock.Now)>300000)throw new InvalidDataException("Pairing/account mismatch");
            string dir=Path.Combine(DirectoryPath,"accounts",AccountKey(tokens.Account));var prior=LoadMirror(tokens.Account);
            long floor=Math.Max(prior==null?0:prior.Floor,Json.Integer(request,"floor"));if(floor<0||floor>Clock.Now+300000)throw new InvalidDataException("Invalid history boundary");
            var observer=TaskStatus;string op=Json.Text(request,"op");
            if(op=="task-status")return new Dictionary<string,object>{{"v",1},{"id",id},{"account",AccountKey(tokens.Account)},{"tasks",observer==null?null:observer()}};
            if(op=="pull") {
                var rows=LoadRows(tokens.Account).Where(x=>x.At>floor).ToArray();
                return new Dictionary<string,object>{{"v",1},{"id",id},{"account",AccountKey(tokens.Account)},{"rows",rows},{"floor",floor},{"tasks",Json.Text(request,"task_status")=="True"&&observer!=null?observer():null}};
            }
            if(op!="push")throw new InvalidDataException("Invalid operation");
            var rowsIn=LanSync.JsonCodec.Deserialize<Observation[]>(LanSync.JsonCodec.Serialize(request["rows"]));
            if(rowsIn==null||rowsIn.Length>100000)throw new InvalidDataException("Too many observations");
            foreach(var r in rowsIn)if(r==null||r.At<=0||r.At>Clock.Now+300000||r.Reset<0||r.Seconds<=0||r.Seconds>31622400
                ||double.IsNaN(r.Used)||double.IsInfinity(r.Used)||r.Used<0||r.Used>100
                ||r.Meter==null||!System.Text.RegularExpressions.Regex.IsMatch(r.Meter,"^[a-z0-9_:.-]{1,120}$")
                ||r.Plan==null||!System.Text.RegularExpressions.Regex.IsMatch(r.Plan,"^[a-z0-9_-]{1,40}$"))throw new InvalidDataException("Invalid observation");
            int tier=(int)Json.Integer(request,"tier",-1);long at=Json.Integer(request,"observed"),reset=Json.Integer(request,"reset");
            double percent; if(!double.TryParse(Json.Text(request,"percent"),System.Globalization.NumberStyles.Float,System.Globalization.CultureInfo.InvariantCulture,out percent)
                ||double.IsNaN(percent)||double.IsInfinity(percent)||percent<0||tier< -1||tier>9||at<0||at>Clock.Now+300000||reset<0)throw new InvalidDataException("Invalid tier");
            if(prior!=null&&at<prior.At)throw new InvalidDataException("Older mirror rejected");
            // Merge by existing observation identity; a retry never counts a observation twice.
            foreach(var group in rowsIn.Where(r=>r.At>floor&&r.At>=Clock.Now-90L*86400000).GroupBy(r=>Clock.Day(r.At))) {
                string path=Path.Combine(dir,"records-"+group.Key+".json");var rows=File.Exists(path)?LanSync.JsonCodec.Deserialize<List<Observation>>(File.ReadAllText(path)):new List<Observation>();
                var keys=new HashSet<string>(rows.Select(r=>r.Meter+"|"+r.At));foreach(var r in group)if(keys.Add(r.Meter+"|"+r.At))rows.Add(r);
                AtomicWrite(path,Encoding.UTF8.GetBytes(LanSync.JsonCodec.Serialize(rows)));
            }
            // The mobile app is the canonical tier calculator, including its billing cycle and credits.
            // Store its long-term daily archives as well, without adding overlapping percentages.
            var archive=new Dictionary<string,object>(request);archive.Remove("secret");AtomicWrite(Path.Combine(dir,"shared-history.json"),Encoding.UTF8.GetBytes(LanSync.JsonCodec.Serialize(archive)));
            double today;double? daily=double.TryParse(Json.Text(request,"today"),System.Globalization.NumberStyles.Float,System.Globalization.CultureInfo.InvariantCulture,out today)&&!double.IsNaN(today)&&!double.IsInfinity(today)&&today>=0?(double?)today:null;
            var mirror=new PhoneMirror{Account=AccountKey(tokens.Account),Policy=Json.Text(request,"policy"),At=at,Received=Clock.Now,Reset=reset,Floor=floor,Tier=tier,Percent=percent,Today=daily};
            AtomicWrite(Path.Combine(dir,"phone-mirror.json"),Encoding.UTF8.GetBytes(LanSync.JsonCodec.Serialize(mirror)));
            return new Dictionary<string,object>{{"v",1},{"id",id},{"account",mirror.Account},{"ok",true}};
        }}
    }
    public sealed class LanSync:IDisposable {
        internal static JavaScriptSerializer JsonCodec {get{return new JavaScriptSerializer{MaxJsonLength=32*1024*1024,RecursionLimit=64};}}
        private readonly LocalStore store;private TcpListener listener;private readonly X509Certificate2 cert;private readonly string secret;private bool stopped;
        public int Port {get;private set;}
        public static bool Private(IPAddress address) {var b=address.GetAddressBytes();return b.Length==4&&(b[0]==10||b[0]==127||b[0]==192&&b[1]==168||b[0]==172&&b[1]>=16&&b[1]<=31);}
        internal static bool Equal(string a,string b){if(a==null||b==null||a.Length!=b.Length)return false;int diff=0;for(int i=0;i<a.Length;i++)diff|=a[i]^b[i];return diff==0;}
        public static string Fingerprint(X509Certificate certificate){using(var sha=SHA256.Create())return BitConverter.ToString(sha.ComputeHash(certificate.GetRawCertData())).Replace("-","").ToLowerInvariant();}
        public LanSync(LocalStore storage,IPAddress bind=null) {
            store=storage;string path=Path.Combine(store.DirectoryPath,"lan-pair.dpapi");byte[] bytes;
            if(File.Exists(path))bytes=ProtectedData.Unprotect(File.ReadAllBytes(path),null,DataProtectionScope.CurrentUser);
            else {
                using(var rsa=RSA.Create(2048)) {
                    var request=new CertificateRequest("CN=Codex Meter LAN",rsa,HashAlgorithmName.SHA256,RSASignaturePadding.Pkcs1);
                    using(var created=request.CreateSelfSigned(DateTimeOffset.UtcNow.AddDays(-1),DateTimeOffset.UtcNow.AddYears(10))) {
                        byte[] key=new byte[32];using(var random=RandomNumberGenerator.Create())random.GetBytes(key);
                        bytes=Encoding.UTF8.GetBytes(JsonCodec.Serialize(new Dictionary<string,object>{{"pfx",Convert.ToBase64String(created.Export(X509ContentType.Pfx))},{"secret",BitConverter.ToString(key).Replace("-","").ToLowerInvariant()}}));
                    }
                }
                LocalStore.AtomicWrite(path,ProtectedData.Protect(bytes,null,DataProtectionScope.CurrentUser));
            }
            var pair=Json.Read(Encoding.UTF8.GetString(bytes));secret=Json.Text(pair,"secret");cert=new X509Certificate2(Convert.FromBase64String(Json.Text(pair,"pfx")),"",X509KeyStorageFlags.UserKeySet);
            listener=new TcpListener(bind??IPAddress.Any,bind==null?47653:0);listener.Start(4);Port=((IPEndPoint)listener.LocalEndpoint).Port;Task.Run((Action)Run);
        }
        public string PairCode(string ip){var address=IPAddress.Parse(ip);if(!Private(address))throw new InvalidDataException("Private IPv4 required");return "cm1|"+ip+"|"+Port+"|"+Fingerprint(cert)+"|"+secret;}
        private void Run(){while(!stopped){try{using(var client=listener.AcceptTcpClient()) {
                    if(!Private(((IPEndPoint)client.Client.RemoteEndPoint).Address))continue;
                    client.ReceiveTimeout=10000;client.SendTimeout=10000;
                    using(var stream=new SslStream(client.GetStream(),false)){stream.ReadTimeout=10000;stream.WriteTimeout=10000;stream.AuthenticateAsServer(cert,false,SslProtocols.Tls12,false);
                        var request=JsonCodec.Deserialize<Dictionary<string,object>>(Read(stream));var response=store.Exchange(request,secret);Write(stream,JsonCodec.Serialize(response));}
                }}catch{if(stopped)return; /* Failed peer must not interrupt the widget or expose server errors. */}}}
        public static string Read(Stream stream){byte[] head=new byte[4];Fill(stream,head);int size=(head[0]<<24)|(head[1]<<16)|(head[2]<<8)|head[3];if(size<=0||size>4*1024*1024)throw new InvalidDataException("Frame bound");byte[] payload=new byte[size];Fill(stream,payload);
            using(var input=new MemoryStream(payload))using(var gzip=new GZipStream(input,CompressionMode.Decompress))using(var output=new MemoryStream()) {byte[] buffer=new byte[8192];int n;while((n=gzip.Read(buffer,0,buffer.Length))>0){if(output.Length+n>32*1024*1024)throw new InvalidDataException("Expanded bound");output.Write(buffer,0,n);}return Encoding.UTF8.GetString(output.ToArray());}}
        public static void Write(Stream stream,string value){byte[] bytes;using(var output=new MemoryStream()){using(var gzip=new GZipStream(output,CompressionMode.Compress,true)){var text=Encoding.UTF8.GetBytes(value);if(text.Length>32*1024*1024)throw new InvalidDataException("Expanded bound");gzip.Write(text,0,text.Length);}bytes=output.ToArray();}if(bytes.Length>4*1024*1024)throw new InvalidDataException("Frame bound");int n=bytes.Length;stream.Write(new[]{(byte)(n>>24),(byte)(n>>16),(byte)(n>>8),(byte)n},0,4);stream.Write(bytes,0,n);stream.Flush();}
        private static void Fill(Stream stream,byte[] bytes){int at=0,n;while(at<bytes.Length){n=stream.Read(bytes,at,bytes.Length-at);if(n<=0)throw new EndOfStreamException();at+=n;}}
        public void Dispose(){stopped=true;listener.Stop();cert.Dispose();}
    }
}
