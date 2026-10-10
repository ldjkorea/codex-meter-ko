using System;
using System.Collections.Generic;
using System.Globalization;
using System.IO;
using System.Linq;
using System.Net;
using System.Net.Http;
using System.Security.Cryptography;
using System.Text;
using System.Threading;
using System.Threading.Tasks;
using System.Web.Script.Serialization;

namespace CodexMeterWidget {
    public static class Json {
        public static string Write(object value) { return new JavaScriptSerializer().Serialize(value); }
        public static Dictionary<string, object> Read(string value) { return new JavaScriptSerializer { MaxJsonLength=2097152 }.Deserialize<Dictionary<string, object>>(value); }
        public static Dictionary<string,object> Object(Dictionary<string,object> value,string key) { object result; return value.TryGetValue(key,out result)?result as Dictionary<string,object>:null; }
        public static string Text(Dictionary<string,object> value,string key,string fallback="") { object result; return value!=null&&value.TryGetValue(key,out result)&&result!=null?Convert.ToString(result,CultureInfo.InvariantCulture):fallback; }
        public static long Integer(Dictionary<string,object> value,string key,long fallback=0) { long result;return long.TryParse(Text(value,key),out result)?result:fallback; }
    }
    public static class Clock {
        public static long Now { get { return (long)(DateTime.UtcNow-new DateTime(1970,1,1)).TotalMilliseconds; } }
        public static string Day(long at) { return new DateTime(1970,1,1,0,0,0,DateTimeKind.Utc).AddMilliseconds(at).AddHours(9).ToString("yyyy-MM-dd"); }
    }
    public sealed class Quota {
        public string Meter; public double Used; public long Seconds, Reset;
    }
    public sealed class Snapshot {
        public string Plan; public long At; public List<Quota> Quotas=new List<Quota>();
        public Quota Main { get { return Quotas.FirstOrDefault(x=>x.Meter=="weekly")??Quotas.FirstOrDefault(x=>x.Meter=="monthly")??Quotas.FirstOrDefault(x=>x.Meter=="five_hour"); } }
        public bool Fresh(long now) { return At>0 && now>=At && now-At<=15*60000 && Main!=null && Main.Reset>now; }
        public static Snapshot Parse(string raw,long at) {
            var root=Json.Read(raw);var result=new Snapshot { Plan=Json.Text(root,"plan_type","unknown"),At=at };var limits=Json.Object(root,"rate_limit");
            foreach(string key in new[]{"primary_window","secondary_window"}) {
                var row=limits==null?null:Json.Object(limits,key);if(row==null)continue;
                double used;if(!double.TryParse(Json.Text(row,"used_percent"),NumberStyles.Float,CultureInfo.InvariantCulture,out used)||double.IsNaN(used)||double.IsInfinity(used)||used<0||used>100)throw new InvalidDataException("사용률 응답이 올바르지 않습니다.");
                long seconds=Json.Integer(row,"limit_window_seconds");string meter=seconds>=10800&&seconds<=28800?"five_hour":seconds>=432000&&seconds<=777600?"weekly":seconds>=864000&&seconds<=3888000?"monthly":"";
                if(meter.Length==0||result.Quotas.Any(x=>x.Meter==meter))continue;
                long reset=Json.Integer(row,"reset_at");if(reset>0)reset=checked(reset*1000);else {long after=Json.Integer(row,"reset_after_seconds");reset=after>0?checked(at+after*1000):0;}
                result.Quotas.Add(new Quota { Meter=meter,Used=used,Seconds=seconds,Reset=reset });
            }
            return result;
        }
    }
    public sealed class Tokens {
        public string Access,Refresh,Account; public long Expires;
        public static string AccountFromJwt(string token) {
            try { var pieces=token.Split('.');if(pieces.Length!=3)return "";string value=pieces[1].Replace('-','+').Replace('_','/');value=value.PadRight((value.Length+3)/4*4,'=');var claims=Json.Read(Encoding.UTF8.GetString(Convert.FromBase64String(value)));string id=Json.Text(claims,"chatgpt_account_id");var auth=Json.Object(claims,"https://api.openai.com/auth");if(id.Length==0)id=Json.Text(auth,"chatgpt_account_id");return id; } catch { return ""; }
        }
        public static Tokens Parse(string raw,Tokens prior=null) {
            var row=Json.Read(raw);string access=Json.Text(row,"access_token"),refresh=Json.Text(row,"refresh_token",prior==null?"":prior.Refresh);
            string account=AccountFromJwt(Json.Text(row,"id_token"));if(account.Length==0)account=AccountFromJwt(access);if(account.Length==0&&prior!=null)account=prior.Account;
            if(access.Length==0||refresh.Length==0||account.Length==0)throw new InvalidDataException("계정 정보가 충분하지 않습니다. 다시 로그인해 주세요.");
            return new Tokens { Access=access,Refresh=refresh,Account=account,Expires=checked(Clock.Now+Math.Max(60,Math.Min(86400,Json.Integer(row,"expires_in",3600)))*1000) };
        }
    }
    public sealed partial class LocalStore {
        public readonly string DirectoryPath;private readonly object gate=new object();private long generation;
        public LocalStore(string path) {DirectoryPath=path;}
        public long Generation {get {lock(gate)return generation;} }
        public static string AccountKey(string account) {using(var sha=SHA256.Create())return BitConverter.ToString(sha.ComputeHash(Encoding.UTF8.GetBytes(account))).Replace("-","").ToLowerInvariant();}
        public static void AtomicWrite(string path,byte[] bytes) {
            Directory.CreateDirectory(Path.GetDirectoryName(path));string temp=path+"."+Guid.NewGuid().ToString("N")+".tmp";
            try {using(var f=new FileStream(temp,FileMode.CreateNew,FileAccess.Write,FileShare.None)){f.Write(bytes,0,bytes.Length);f.Flush(true);}if(File.Exists(path))File.Replace(temp,path,path+".bak");else File.Move(temp,path);}
            finally {if(File.Exists(temp))File.Delete(temp);}
        }
        public Tokens LoadTokens() {lock(gate){string p=Path.Combine(DirectoryPath,"credentials.dpapi");if(!File.Exists(p))return null;var raw=ProtectedData.Unprotect(File.ReadAllBytes(p),null,DataProtectionScope.CurrentUser);return new JavaScriptSerializer().Deserialize<Tokens>(Encoding.UTF8.GetString(raw));}}
        public bool SaveTokens(Tokens tokens,long expected) {lock(gate){if(expected!=generation)return false;var bytes=ProtectedData.Protect(Encoding.UTF8.GetBytes(Json.Write(tokens)),null,DataProtectionScope.CurrentUser);AtomicWrite(Path.Combine(DirectoryPath,"credentials.dpapi"),bytes);return true;}}
        public void SignOut() {lock(gate){generation++;foreach(string name in new[]{"credentials.dpapi","credentials.dpapi.bak"}){string p=Path.Combine(DirectoryPath,name);if(File.Exists(p))File.Delete(p);}}}
        public void SaveSnapshot(Tokens tokens,Snapshot snapshot,long expected) {lock(gate){if(expected!=generation)throw new OperationCanceledException();string dir=Path.Combine(DirectoryPath,"accounts",AccountKey(tokens.Account));string chunk=Path.Combine(dir,"records-"+Clock.Day(snapshot.At)+".json");var list=File.Exists(chunk)?new JavaScriptSerializer().Deserialize<List<Observation>>(File.ReadAllText(chunk)):new List<Observation>();foreach(var q in snapshot.Quotas)if(!list.Any(x=>x.At==snapshot.At&&x.Meter==q.Meter))list.Add(new Observation {At=snapshot.At,Plan=snapshot.Plan,Meter=q.Meter,Used=q.Used,Reset=q.Reset,Seconds=q.Seconds});AtomicWrite(chunk,Encoding.UTF8.GetBytes(Json.Write(list)));string latest=Path.Combine(dir,"latest.json");var prior=File.Exists(latest)?new JavaScriptSerializer().Deserialize<Snapshot>(File.ReadAllText(latest)):null;if(prior==null||snapshot.At>=prior.At)AtomicWrite(latest,Encoding.UTF8.GetBytes(Json.Write(snapshot)));}}
        public Snapshot LoadSnapshot(string account) {string p=Path.Combine(DirectoryPath,"accounts",AccountKey(account),"latest.json");return File.Exists(p)?new JavaScriptSerializer().Deserialize<Snapshot>(File.ReadAllText(p)):null;}
        public List<Observation> LoadRows(string account) {string dir=Path.Combine(DirectoryPath,"accounts",AccountKey(account));var rows=new List<Observation>();if(!Directory.Exists(dir))return rows;string earliest=Clock.Day(Clock.Now-90L*86400000);foreach(string p in Directory.GetFiles(dir,"records-????-??-??.json")){string date=Path.GetFileName(p).Substring(8,10);if(string.CompareOrdinal(date,earliest)>=0)rows.AddRange(new JavaScriptSerializer().Deserialize<List<Observation>>(File.ReadAllText(p)));}return rows;}
    }
    public sealed class Observation {public long At,Reset,Seconds;public string Plan,Meter,Decimal,Source;public bool Manual;public double Used;public string Policy {get{return Meter+"|"+Plan+"|"+Seconds;}}}
    public static class DailyUsage {
        public static double? Observed(List<Observation> rows,string policy,string date) {
            Observation before=null;double total=0;bool measured=false;string meter=policy.Split('|')[0];
            foreach(var row in rows.Where(x=>x.Meter==meter).OrderBy(x=>x.At)) {
                if(before!=null&&row.Policy==policy&&before.Policy==policy&&before.Reset==row.Reset&&row.Reset>row.At&&before.Reset>before.At&&row.At>before.At&&row.At-before.At<=6*3600000L&&row.Used>=before.Used&&Clock.Day(row.At)==date&&Clock.Day(before.At)==date){total+=row.Used-before.Used;measured=true;}before=row;
            }return measured?(double?)total:null;
        }
    }
    public sealed class MeterApi:IDisposable {
        private readonly HttpClient client;public const string ClientId="app_EMoamEEZ73f0CkXaXp7hrann";
        public MeterApi(HttpMessageHandler handler=null) {client=new HttpClient(handler??new HttpClientHandler {AllowAutoRedirect=false});client.Timeout=TimeSpan.FromSeconds(30);client.DefaultRequestHeaders.UserAgent.ParseAdd("codex-meter-windows/0.1.3");}
        private async Task<string> Send(HttpRequestMessage request,CancellationToken cancel) {
            using(var timeout=CancellationTokenSource.CreateLinkedTokenSource(cancel)){timeout.CancelAfter(30000);cancel=timeout.Token;
            using(request)using(var response=await client.SendAsync(request,HttpCompletionOption.ResponseHeadersRead,cancel).ConfigureAwait(false)) {
                if(!response.IsSuccessStatusCode)throw new InvalidOperationException("조회 실패 (HTTP "+(int)response.StatusCode+"). 잠시 후 다시 시도해 주세요.");
                using(var stream=await response.Content.ReadAsStreamAsync().ConfigureAwait(false))using(var output=new MemoryStream()){byte[] buffer=new byte[8192];int count;while((count=await stream.ReadAsync(buffer,0,buffer.Length,cancel).ConfigureAwait(false))>0){if(output.Length+count>2097152)throw new InvalidDataException("응답 크기를 초과했습니다.");output.Write(buffer,0,count);}return Encoding.UTF8.GetString(output.ToArray());}
            }
        }
        }
        public async Task<Tokens> Exchange(string code,string verifier,string redirect,CancellationToken cancel) {
            var request=new HttpRequestMessage(HttpMethod.Post,"https://auth.openai.com/oauth/token");request.Content=new FormUrlEncodedContent(new Dictionary<string,string>{{"grant_type","authorization_code"},{"client_id",ClientId},{"code",code},{"code_verifier",verifier},{"redirect_uri",redirect}});return Tokens.Parse(await Send(request,cancel).ConfigureAwait(false));
        }
        public async Task<Tokens> Refresh(Tokens prior,CancellationToken cancel) {
            var request=new HttpRequestMessage(HttpMethod.Post,"https://auth.openai.com/oauth/token");request.Content=new StringContent(Json.Write(new{grant_type="refresh_token",refresh_token=prior.Refresh,client_id=ClientId}),Encoding.UTF8,"application/json");return Tokens.Parse(await Send(request,cancel).ConfigureAwait(false),prior);
        }
        public async Task<Snapshot> Fetch(Tokens tokens,CancellationToken cancel) {
            var request=new HttpRequestMessage(HttpMethod.Get,"https://chatgpt.com/backend-api/wham/usage");request.Headers.Authorization=new System.Net.Http.Headers.AuthenticationHeaderValue("Bearer",tokens.Access);request.Headers.Add("ChatGPT-Account-Id",tokens.Account);return Snapshot.Parse(await Send(request,cancel).ConfigureAwait(false),Clock.Now);
        }
        public void Dispose(){client.Dispose();}
    }
}
