using System;
using System.Collections.Generic;
using System.Drawing;
using System.IO;
using System.Linq;
using System.Net;
using System.Net.Http;
using System.Text;
using System.Threading;
using System.Threading.Tasks;

namespace CodexMeterWidget {
 public static class Tests {
  static int count;
  static void Check(bool value,string message){count++;if(!value)throw new Exception(message);}
  static void Reject(Action action,string name){bool failed=false;try{action();}catch{failed=true;}Check(failed,name);}
  sealed class Fake:HttpMessageHandler {
   public HttpStatusCode Status=HttpStatusCode.OK;public string Body;public string Url,Method,Auth,Account,Payload;
   protected override async Task<HttpResponseMessage> SendAsync(HttpRequestMessage r,CancellationToken c){c.ThrowIfCancellationRequested();Url=r.RequestUri.ToString();Method=r.Method.ToString();Auth=r.Headers.Authorization==null?null:r.Headers.Authorization.ToString();Account=r.Headers.Contains("ChatGPT-Account-Id")?r.Headers.GetValues("ChatGPT-Account-Id").Single():null;Payload=r.Content==null?null:await r.Content.ReadAsStringAsync();return new HttpResponseMessage(Status){Content=new StringContent(Body??"{}")};}
  }
  static string Jwt(string id){return "x."+Login.Base64(Encoding.UTF8.GetBytes(Json.Write(new Dictionary<string,object>{{"https://api.openai.com/auth",new{chatgpt_account_id=id}}})))+".y";}
  static string Response(double used,long reset){return Json.Write(new{plan_type="pro",rate_limit=new{secondary_window=new{used_percent=used,limit_window_seconds=604800,reset_at=reset/1000}}});}
  static Observation Row(long at,long end,double used,string plan="pro"){return new Observation{At=at,Reset=end,Used=used,Seconds=604800,Meter="weekly",Plan=plan};}
  [STAThread] public static int Main(string[] args){
   if(args.Length>0&&args[0]=="--lan-probe")return Probe(args[1]);
   string dir=Path.Combine(Environment.GetEnvironmentVariable("CODEX_METER_TEST_ROOT")??Path.GetTempPath(),"lan-"+Guid.NewGuid().ToString("N").Substring(0,8));Directory.CreateDirectory(dir);
   try {
    long now=Clock.Now,end=now+TierRules.Week;var snap=Snapshot.Parse(Response(37.125,end),now);
    Check(snap.Main.Meter=="weekly"&&snap.Main.Used==37.125&&snap.Quotas.Count==1,"weekly-only precision");Check(snap.Fresh(now)&&!snap.Fresh(now+16*60000)&&!snap.Fresh(now-1),"freshness");
    Check(Snapshot.Parse("{}",now).Main==null,"absent limit");Reject(()=>Snapshot.Parse(Response(101,end),now),"invalid percentage");
    foreach(long seconds in new[]{18000L,604800L,2592000L}) {var q=Snapshot.Parse(Json.Write(new{rate_limit=new{primary_window=new{used_percent=0,limit_window_seconds=seconds,reset_after_seconds=120}}}),now);Check(q.Main!=null&&q.Main.Reset==now+120000,"quota window");}
    var tokens=Tokens.Parse(Json.Write(new{access_token="mock-access",refresh_token="mock-refresh",id_token=Jwt("A"),expires_in=3600}));Check(tokens.Account=="A","account extraction");
    Check(Tokens.Parse(Json.Write(new{access_token="new"}),tokens).Refresh==tokens.Refresh,"refresh rotation fallback");Reject(()=>Tokens.Parse("{}"),"missing credentials");
    string verifier="dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk";
    Check(Login.Challenge(verifier)=="E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM","RFC7636 PKCE vector");
    Check(Login.CodeFromTarget("/auth/callback?code=ok&state=valid","valid")=="ok","valid callback");
    foreach(string target in new[]{"/other?code=x&state=valid","/auth/callback?code=x&state=wrong","/auth/callback?code=x&state=valid&state=valid","/auth/callback?state=valid"})Check(Login.CodeFromTarget(target,"valid")==null,"reject callback");
    Check(Login.RandomValue()!=Login.RandomValue(),"random nonce");Check(Login.Authorize("http://localhost:1455/auth/callback",verifier,"valid").StartsWith("https://auth.openai.com/oauth/authorize?"),"fixed authorize host");
    var store=new LocalStore(dir);long generation=store.Generation;Check(store.SaveTokens(tokens,generation),"save credentials");Check(store.LoadTokens().Refresh==tokens.Refresh,"DPAPI restart");Check(!Encoding.UTF8.GetString(File.ReadAllBytes(Path.Combine(dir,"credentials.dpapi"))).Contains("mock-refresh"),"ciphertext only");
    store.SaveSnapshot(tokens,snap,generation);store.SaveSnapshot(tokens,snap,generation);Check(new LocalStore(dir).LoadRows("A").Count==1,"dedup restart");Check(store.LoadRows("B").Count==0&&store.LoadSnapshot("B")==null,"account isolation");
    var jobs=Enumerable.Range(1,12).Select(i=>Task.Run(()=>store.SaveSnapshot(tokens,Snapshot.Parse(Response(37.125+i/100.0,end),now+i*1000),generation))).ToArray();Task.WaitAll(jobs);Check(store.LoadRows("A").Count==13,"concurrent writes");Check(store.LoadSnapshot("A").At==now+12000,"newest snapshot survives out of order writes");
    store.SignOut();Check(store.LoadTokens()==null&&store.LoadRows("A").Count==13,"logout preserves records");Check(!store.SaveTokens(tokens,generation),"stale auth generation");Reject(()=>store.SaveSnapshot(tokens,snap,generation),"stale record generation");
    Check(!File.ReadAllText(Directory.GetFiles(Path.Combine(dir,"accounts",LocalStore.AccountKey("A")),"records-*.json").Single()).Contains("mock-access"),"records exclude credentials");
    string policy="weekly|pro|604800",day=Clock.Day(now);var rows=new List<Observation>{Row(now,end,10),Row(now+1000,end,23)};Check(DailyUsage.Observed(rows,policy,day)==13,"observed percentage points");
    rows[1].Used=10;Check(DailyUsage.Observed(rows,policy,day)==0,"true zero");rows[1].Used=0;Check(DailyUsage.Observed(rows,policy,day)==null,"reset or correction not negative");rows[1].Used=23;rows[1].Reset+=1000;Check(DailyUsage.Observed(rows,policy,day)==null,"early reset excluded");rows[1].Reset=end;rows[1].Plan="plus";Check(DailyUsage.Observed(rows,policy,day)==null,"policy change excluded");
    long midnight=(long)(new DateTime(2026,10,10,0,0,0,DateTimeKind.Utc).AddHours(-9)-new DateTime(1970,1,1,0,0,0,DateTimeKind.Utc)).TotalMilliseconds;
    Check(DailyUsage.Observed(new List<Observation>{Row(midnight-1000,end,10),Row(midnight+1000,end,23)},policy,Clock.Day(midnight))==null,"cross midnight not fabricated");
    Check(DailyUsage.Observed(new List<Observation>{Row(now,end,10),Row(now+7*3600000L,end,23)},policy,day)==null,"offline gap excluded");
    var evidence=new List<Evidence>();var rating=new TierState();for(int i=0;i<8;i++){evidence.Add(new Evidence{End=now-(7-i)*TierRules.Week,Used=100,Count=60});rating=TierRules.Evaluate(rating,evidence,evidence.Last().End);Check(rating.Tier<=9,"tier bounds");if(i==0)Check(rating.Tier==-1,"one window unplaced");}
    Check(rating.Tier==8&&rating.Count==8,"one-level promotion cap");evidence.Add(new Evidence{End=now+TierRules.Week,Used=100,Count=60});rating=TierRules.Evaluate(rating,evidence,now+TierRules.Week);Check(rating.Tier==9,"challenger after sustained promotion");Check(TierRules.Evaluate(rating,evidence,rating.End).Tier==rating.Tier,"tier idempotency");
    var full=new List<Observation>();long closed=now-1000;for(int i=0;i<=42;i++)full.Add(Row(closed-TierRules.Week+1000+i*(TierRules.Week-2000)/42,closed,i*2));Check(TierRules.Completed(full,policy,now).Count==1,"completed observed window");full[20].Used=0;Check(TierRules.Completed(full,policy,now).Count==0,"correction invalidates tier evidence");
    var p=new Preferences{FontFactor=double.NaN,Opacity=double.NaN,Width=-1,Height=-1};p.Validate();Check(p.FontFactor==1&&p.Opacity==.94&&p.Width==340&&p.Height==330,"invalid settings recovery");
    var fake=new Fake{Body=Response(37,end)};using(var api=new MeterApi(fake)){var result=api.Fetch(tokens,CancellationToken.None).GetAwaiter().GetResult();Check(result.Main.Meter=="weekly"&&fake.Url=="https://chatgpt.com/backend-api/wham/usage"&&fake.Method=="GET","usage route");Check(fake.Auth=="Bearer mock-access"&&fake.Account=="A","required headers");fake.Status=HttpStatusCode.Unauthorized;fake.Body="private-secret";try{api.Fetch(tokens,CancellationToken.None).GetAwaiter().GetResult();throw new Exception("missing failure");}catch(InvalidOperationException e){Check(!e.Message.Contains("private-secret"),"safe server failure");}}
    fake=new Fake{Body=Json.Write(new{access_token="rotated",refresh_token="rotated-refresh"})};using(var api=new MeterApi(fake)){Check(api.Refresh(tokens,CancellationToken.None).GetAwaiter().GetResult().Account=="A","mock refresh");Check(fake.Url=="https://auth.openai.com/oauth/token"&&fake.Payload.Contains("refresh_token"),"refresh route");}
    fake=new Fake{Body=new string('x',2097153)};using(var api=new MeterApi(fake))Reject(()=>api.Fetch(tokens,CancellationToken.None).GetAwaiter().GetResult(),"response bound");
    fake=new Fake{Body="{}"};using(var api=new MeterApi(fake)){var cancel=new CancellationToken(true);Reject(()=>api.Fetch(tokens,cancel).GetAwaiter().GetResult(),"cancelled network");}
    LanChecks(store,tokens,snap);
    string previews=args.Length>0?args[0]:Path.Combine(dir,"previews");Directory.CreateDirectory(previews);var images=new Image[10];for(int i=0;i<10;i++)using(var stream=typeof(Tests).Assembly.GetManifestResourceStream("prestige_"+i))using(var img=Image.FromStream(stream))images[i]=new Bitmap(img);
    foreach(int width in new[]{340,400,640})foreach(double font in new[]{1,1.25,1.5})foreach(bool empty in new[]{false,true}){p=new Preferences{Width=width,Height=(int)(330*font),FontFactor=font,ShowFive=true};var size=new Size(p.Width,p.Height);using(var image=new Bitmap(size.Width,size.Height))using(var g=Graphics.FromImage(image)){RectangleF action,menu;WidgetForm.Draw(g,size,p,empty?null:snap,new TierState{Tier=empty?-1:7,Count=8},empty?null:(double?)13.2,empty?"ChatGPT 로그인으로 시작하세요.":"정상 조회 · 테스트 데이터",false,false,images,out action,out menu);Check(action.Bottom<=size.Height&&action.Right<=size.Width,"action in bounds");image.Save(Path.Combine(previews,"widget-"+width+"-"+font+"-"+(empty?"empty":"usage")+".png"));}}
    using(var api=new MeterApi(new Fake()))using(var form=new WidgetForm(store,api,true)){Check(form.TopMost&&form.Width>=340&&Math.Abs(form.Opacity-.94)<.001,"form settings, no visible window");Check(form.MinimumSize.Height>=330,"large-font safe minimum");}
    foreach(var image in images)image.Dispose();Console.WriteLine("Windows widget: "+count+" assertions passed; 18 offscreen renders. No live OAuth/API/registry calls.");return 0;
   }catch(Exception e){Console.Error.WriteLine(e);return 1;}finally{Console.WriteLine("Isolated synthetic test data: "+dir);}
  }
  static Dictionary<string,object> Push(Tokens tokens,string secret,Snapshot snapshot){return new Dictionary<string,object>{{"v",1},{"id",Guid.NewGuid().ToString("N")},{"at",Clock.Now},{"account",LocalStore.AccountKey(tokens.Account)},{"secret",secret},{"op","push"},{"floor",0L},{"observed",snapshot.At},{"reset",snapshot.Main.Reset},{"tier",7},{"policy","weekly|pro|604800"},{"percent",87.5},{"rows",new[]{Row(snapshot.At,snapshot.Main.Reset,37.125)}},{"days",new object[]{new{day="2026-01-01",points=12.0}}},{"events",new object[0]}};}
  static void LanChecks(LocalStore store,Tokens tokens,Snapshot snap){
    long generation=store.Generation;store.SaveTokens(tokens,generation);
    using(var server=new LanSync(store,IPAddress.Loopback)){
     var code=server.PairCode("127.0.0.1").Split('|');Check(code[3].Length==64&&code[4].Length==64,"pinned TLS plus random secret");
     var request=Push(tokens,code[4],snap);int before=store.LoadRows(tokens.Account).Count;store.Exchange(request,code[4]);store.Exchange(request,code[4]);
     Check(store.LoadRows(tokens.Account).Count==before,"peer merge duplicate ignored");Check(store.LoadMirror(tokens.Account).Tier==7,"mobile canonical tier");
     var mirror=store.LoadMirror(tokens.Account);Check(mirror.Current(snap),"matching policy shows phone tier");mirror.Reset+=1000;Check(mirror.Current(snap),"API reset drift tolerance");mirror.Policy="weekly|plus|604800";Check(!mirror.Current(snap),"plan change hides old tier");mirror.Policy="weekly|pro|604800";mirror.At=Clock.Now-16*60000;Check(!mirror.Current(snap),"old phone tier not shown as current");
     Check(File.ReadAllText(Path.Combine(store.DirectoryPath,"accounts",LocalStore.AccountKey(tokens.Account),"shared-history.json")).Contains("2026-01-01"),"historical daily archive shared");
     Check(!File.ReadAllText(Path.Combine(store.DirectoryPath,"accounts",LocalStore.AccountKey(tokens.Account),"shared-history.json")).Contains(code[4]),"pairing secret not in history");
     request=Push(tokens,"wrong",snap);Reject(()=>store.Exchange(request,code[4]),"wrong pairing secret");request=Push(tokens,code[4],snap);request["account"]=LocalStore.AccountKey("B");Reject(()=>store.Exchange(request,code[4]),"account mismatch");
     request=Push(tokens,code[4],snap);request["tier"]=10;Reject(()=>store.Exchange(request,code[4]),"invalid tier");
     request=Push(tokens,code[4],snap);request["floor"]=Clock.Now+60000;store.Exchange(request,code[4]);
     var pull=new Dictionary<string,object>{{"v",1},{"id",Guid.NewGuid().ToString("N")},{"at",Clock.Now},{"account",LocalStore.AccountKey(tokens.Account)},{"secret",code[4]},{"op","pull"},{"floor",0L}};
     Check(((Observation[])store.Exchange(pull,code[4])["rows"]).Length==0,"clear boundary prevents resurrection without deleting original PC files");
     store.SignOut();Reject(()=>store.Exchange(pull,code[4]),"logout rejects paired peer");
    }
    Check(LanSync.Private(IPAddress.Parse("192.168.1.2"))&&!LanSync.Private(IPAddress.Parse("8.8.8.8")),"LAN only");
    using(var stream=new MemoryStream()){LanSync.Write(stream,"한글 실제 관측 37.125");stream.Position=0;Check(LanSync.Read(stream)=="한글 실제 관측 37.125","bounded gzip UTF8 frame");}
    using(var stream=new MemoryStream(new byte[]{0,64,0,1}))Reject(()=>LanSync.Read(stream),"oversized frame");
  }
  static int Probe(string dir){try{Directory.CreateDirectory(dir);var store=new LocalStore(dir);var tokens=new Tokens{Account="lan-integration",Access="synthetic-access",Refresh="synthetic-refresh",Expires=Clock.Now+600000};store.SaveTokens(tokens,store.Generation);
    long at=Clock.Now;store.SaveSnapshot(tokens,Snapshot.Parse(Response(25,at+604800000),at),store.Generation);
    using(var server=new LanSync(store,IPAddress.Loopback)){File.WriteAllText(Path.Combine(dir,"pair.txt"),server.PairCode("127.0.0.1"));while(!File.Exists(Path.Combine(dir,"stop")))Thread.Sleep(100);}
    return 0;}catch(Exception e){Console.Error.WriteLine(e);return 1;}}
 }
}
