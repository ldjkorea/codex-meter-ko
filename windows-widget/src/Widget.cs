using System;
using System.Diagnostics;
using System.Drawing;
using System.Drawing.Drawing2D;
using System.Globalization;
using System.IO;
using System.Linq;
using System.Threading;
using System.Threading.Tasks;
using System.Web.Script.Serialization;
using System.Windows.Forms;
using Microsoft.Win32;

namespace CodexMeterWidget {
    public sealed class Preferences {
        public int X=80,Y=80,Width=400,Height=286;public bool TopMost=true,ShowFive=false,LanEnabled=false;public double Opacity=0.94,FontFactor=1;
        public void Validate(){FontFactor=FontFactor==1.25||FontFactor==1.5?FontFactor:1;Opacity=double.IsNaN(Opacity)?0.94:Opacity;Width=Math.Max(340,Math.Min(900,Width));Height=Math.Max((int)(330*FontFactor),Math.Min(700,Height));Opacity=Math.Max(.55,Math.Min(1,Opacity));FontFactor=FontFactor==1.25||FontFactor==1.5?FontFactor:1;}
    }
    public sealed class WidgetForm:Form {
        private readonly LocalStore store;private readonly MeterApi api;private readonly bool preview;
        private readonly CancellationTokenSource lifetime=new CancellationTokenSource();private CancellationTokenSource operation;
        private readonly NotifyIcon tray;private readonly System.Windows.Forms.Timer timer=new System.Windows.Forms.Timer();private readonly Image[] crests=new Image[10];
        private Preferences prefs;private Snapshot snapshot;private Tokens tokens;private TierState tier=new TierState();private double? today;private string status="ChatGPT 로그인으로 시작하세요.";
        private bool busy,exiting,loggedOut;private long lastAttempt;private RectangleF action,menuButton;private ContextMenuStrip menu;
        private LanSync lan;private long mirrorReceived;private bool readingMirror;
        public static readonly string[] TierNames={"Iron","Bronze","Silver","Gold","Platinum","Emerald","Diamond","Master","Grandmaster","Challenger"};
        public WidgetForm(LocalStore storage,MeterApi client,bool testPreview=false) {
            store=storage;api=client;preview=testPreview;prefs=LoadPrefs();prefs.Validate();Text="Codex Meter Widget";FormBorderStyle=FormBorderStyle.None;ShowInTaskbar=false;DoubleBuffered=true;AutoScaleMode=AutoScaleMode.Dpi;
            BackColor=Color.FromArgb(13,14,16);MinimumSize=new Size(340,(int)(330*prefs.FontFactor));Size=new Size(prefs.Width,prefs.Height);StartPosition=FormStartPosition.Manual;Location=new Point(prefs.X,prefs.Y);if(!Screen.AllScreens.Any(x=>Rectangle.Intersect(x.WorkingArea,Bounds).Width>=80)){Location=Screen.PrimaryScreen.WorkingArea.Location+new Size(40,40);}
            KeyPreview=true;KeyDown+=(s,e)=>{if(e.KeyCode==Keys.F5)RefreshUsage();else if(e.KeyCode==Keys.Apps||(e.Shift&&e.KeyCode==Keys.F10))OpenMenu(new Point(20,20));};TopMost=prefs.TopMost;Opacity=prefs.Opacity;AccessibleName="Codex Meter 사용량 위젯";
            for(int i=0;i<10;i++){string path=Path.Combine(AppDomain.CurrentDomain.BaseDirectory,"assets","prestige_"+i+".png");using(var f=typeof(WidgetForm).Assembly.GetManifestResourceStream("prestige_"+i)){if(f!=null)using(var image=Image.FromStream(f))crests[i]=new Bitmap(image);else if(File.Exists(path))using(var image=Image.FromFile(path))crests[i]=new Bitmap(image);}}
            tray=new NotifyIcon{Icon=preview?SystemIcons.Application:Icon.ExtractAssociatedIcon(Application.ExecutablePath),Text="Codex Meter Widget",Visible=!preview};tray.DoubleClick+=(s,e)=>{Show();Activate();};
            FormClosing+=(s,e)=>{if(!exiting&&!preview){e.Cancel=true;Hide();tray.ShowBalloonTip(1800,"Codex Meter","위젯은 트레이에서 다시 열 수 있습니다.",ToolTipIcon.Info);}else lifetime.Cancel();};
            MouseUp+=(s,e)=>{if(e.Button==MouseButtons.Right){OpenMenu(e.Location);return;}if(e.Button==MouseButtons.Left){if(menuButton.Contains(e.Location))OpenMenu(e.Location);else if(action.Contains(e.Location)){if(tokens==null)BeginLogin();else RefreshUsage();}}};
            ResizeEnd+=(s,e)=>SavePrefs();timer.Interval=1000;timer.Tick+=(s,e)=>{if(!preview&&tokens!=null&&!busy)ReadPhoneTier();Invalidate();if(!preview&&tokens!=null&&!busy&&Clock.Now-lastAttempt>=300000)RefreshUsage();};
            if(!preview){try{tokens=store.LoadTokens();if(tokens!=null){snapshot=store.LoadSnapshot(tokens.Account);status=snapshot==null?"사용량을 조회해 주세요.":"마지막 관측값을 불러왔습니다.";StartLan();}}catch{status="저장된 로그인 정보를 읽지 못했습니다. 다시 로그인해 주세요.";}Shown+=(s,e)=>{timer.Start();if(tokens!=null)RefreshUsage();};OpenMenu(Point.Empty,false);}
        }
        protected override CreateParams CreateParams {get{var p=base.CreateParams;p.Style|=0x40000;return p;}}
        protected override void WndProc(ref Message m){
            if(m.Msg==0x84){base.WndProc(ref m);if((int)m.Result==1){var point=PointToClient(new Point((short)((long)m.LParam&65535),(short)(((long)m.LParam>>16)&65535)));int border=8;
                bool l=point.X<border,r=point.X>=ClientSize.Width-border,t=point.Y<border,b=point.Y>=ClientSize.Height-border;
                if(b&&r)m.Result=(IntPtr)17;else if(b&&l)m.Result=(IntPtr)16;else if(t&&l)m.Result=(IntPtr)13;else if(t&&r)m.Result=(IntPtr)14;else if(l)m.Result=(IntPtr)10;else if(r)m.Result=(IntPtr)11;else if(t)m.Result=(IntPtr)12;else if(b)m.Result=(IntPtr)15;else if(point.Y<42&&!menuButton.Contains(point))m.Result=(IntPtr)2;
            }return;}base.WndProc(ref m);
        }
        private Preferences LoadPrefs(){try{string p=Path.Combine(store.DirectoryPath,"widget.json");return File.Exists(p)?new JavaScriptSerializer().Deserialize<Preferences>(File.ReadAllText(p)):new Preferences();}catch{return new Preferences();}}
        private void SavePrefs(){if(preview)return;prefs.X=Left;prefs.Y=Top;prefs.Width=Width;prefs.Height=Height;try{LocalStore.AtomicWrite(Path.Combine(store.DirectoryPath,"widget.json"),System.Text.Encoding.UTF8.GetBytes(Json.Write(prefs)));}catch{status="화면 설정 저장 실패. 기존 설정은 보존됩니다.";Invalidate();}}
        private void OpenMenu(Point at,bool show=true){
            if(menu!=null)menu.Dispose();menu=new ContextMenuStrip();Add("새로고침",()=>RefreshUsage(),!busy&&tokens!=null);
            Add(tokens==null?"ChatGPT 로그인":"계정 변경",()=>BeginLogin(),!busy);Add("로그인 대기 취소",()=>{if(operation!=null)operation.Cancel();},busy&&tokens==null);
            Add("위젯 숨기기",()=>Hide());var top=Add("항상 위에 표시",()=>{prefs.TopMost=!prefs.TopMost;TopMost=prefs.TopMost;SavePrefs();});top.Checked=prefs.TopMost;
            var five=Add("5시간 한도 표시",()=>{prefs.ShowFive=!prefs.ShowFive;MinimumSize=new Size(340,(int)(330*prefs.FontFactor));SavePrefs();Invalidate();});five.Checked=prefs.ShowFive;
            var opacity=new ToolStripMenuItem("투명도");foreach(double value in new[]{.65,.8,.94,1}){double chosen=value;var item=new ToolStripMenuItem(((int)(value*100))+"%",null,(s,e)=>{prefs.Opacity=chosen;Opacity=chosen;SavePrefs();});item.Checked=Math.Abs(chosen-prefs.Opacity)<.001;opacity.DropDownItems.Add(item);}menu.Items.Add(opacity);
            var fonts=new ToolStripMenuItem("글자 크기");foreach(double value in new[]{1,1.25,1.5}){double chosen=value;fonts.DropDownItems.Add(new ToolStripMenuItem((value*100)+"%",null,(s,e)=>{prefs.FontFactor=chosen;MinimumSize=new Size(340,(int)(330*chosen));if(Height<MinimumSize.Height)Height=MinimumSize.Height;SavePrefs();Invalidate();}));}menu.Items.Add(fonts);
            bool startup=StartupEnabled();var auto=Add("Windows 시작 시 실행",()=>SetStartup(!StartupEnabled()));auto.Checked=startup;
            Add("휴대폰 연결 코드",()=>PairPhone(),tokens!=null);var share=Add("휴대폰 자동 공유",()=>{prefs.LanEnabled=!prefs.LanEnabled;if(prefs.LanEnabled)StartLan();else StopLan();SavePrefs();});share.Checked=prefs.LanEnabled;share.Enabled=tokens!=null;
            Add("기록 폴더 열기",()=>Process.Start(new ProcessStartInfo(store.DirectoryPath){UseShellExecute=true}));Add("사용 안내",()=>MessageBox.Show(this,"상단을 끌어 위치를 바꾸고 가장자리를 끌어 크기를 조절하세요.\n\n5분 간격으로 계정 한도를 조회합니다. 실패하거나 오래된 값은 마지막 관측값으로 표시합니다.\n\n휴대폰 연결 코드를 앱 설정에 한 번 등록하면 같은 Wi-Fi에서 기록을 공유하고 휴대폰과 같은 티어를 표시합니다. PC 위젯이 실행 중이어야 하며, 휴대폰 Wi-Fi 연결·앱 실행·정상 조회 때 동기화를 시도하며, 백그라운드에서는 절전 정책에 따라 늦어질 수 있습니다. Windows 방화벽은 개인 네트워크만 허용해 주세요. 로그인은 기기별로 유지합니다.\n\n비공식 개인 도구 · Windows 0.1.2 beta","Codex Meter"));
            Add("로그아웃",()=>SignOut(),tokens!=null);Add("종료",()=>{SavePrefs();exiting=true;Close();});tray.ContextMenuStrip=menu;if(show)menu.Show(this,at);
        }
        private ToolStripMenuItem Add(string name,Action action,bool enabled=true){var item=new ToolStripMenuItem(name,null,(s,e)=>action()){Enabled=enabled};menu.Items.Add(item);return item;}
        private void StartLan(){if(lan!=null||tokens==null||!prefs.LanEnabled||preview)return;try{lan=new LanSync(store);}catch{status="휴대폰 공유 시작 실패. 연결 코드를 다시 열어 주세요.";}}
        private void StopLan(){if(lan!=null){lan.Dispose();lan=null;}mirrorReceived=0;}
        private void PairPhone(){try{prefs.LanEnabled=true;StartLan();if(lan==null)throw new IOException();SavePrefs();
            var addresses=System.Net.NetworkInformation.NetworkInterface.GetAllNetworkInterfaces().Where(n=>n.OperationalStatus==System.Net.NetworkInformation.OperationalStatus.Up)
                .SelectMany(n=>n.GetIPProperties().UnicastAddresses).Select(n=>n.Address).Where(a=>LanSync.Private(a)&&!System.Net.IPAddress.IsLoopback(a)).Select(a=>a.ToString()).Distinct().ToArray();
            if(addresses.Length==0){MessageBox.Show(this,"PC를 휴대폰과 같은 Wi-Fi 또는 공유기에 연결해 주세요.");return;}
            using(var form=new Form{Text="휴대폰 연결",Width=580,Height=260,StartPosition=FormStartPosition.CenterParent,FormBorderStyle=FormBorderStyle.FixedDialog,MaximizeBox=false,MinimizeBox=false}){
                var label=new Label{Left=16,Top=15,Width=540,Height=48,Text="휴대폰 앱 → 설정 → PC·휴대폰 자동 공유에 연결 코드를 붙여넣으세요.\n같은 ChatGPT 계정으로 로그인하고 방화벽은 개인 네트워크만 허용하세요."};
                var ips=new ComboBox{Left=16,Top=66,Width=530,DropDownStyle=ComboBoxStyle.DropDownList};ips.Items.AddRange(addresses);var code=new TextBox{Left=16,Top=102,Width=530,Height=54,Multiline=true,ReadOnly=true};
                ips.SelectedIndexChanged+=(s,e)=>code.Text=lan.PairCode((string)ips.SelectedItem);ips.SelectedIndex=0;
                var copy=new Button{Left=350,Top=170,Width=196,Text="연결 코드 복사"};copy.Click+=(s,e)=>{Clipboard.SetText(code.Text);copy.Text="복사했습니다";};form.Controls.AddRange(new Control[]{label,ips,code,copy});form.ShowDialog(this);
            }}catch{MessageBox.Show(this,"연결 준비에 실패했습니다. 기존 기록은 그대로 보존됩니다.");}}
        private async void ReadPhoneTier(){if(readingMirror||!prefs.LanEnabled||tokens==null)return;readingMirror=true;long generation=store.Generation;string account=tokens.Account;
            try{var mirror=await Task.Run(()=>store.LoadMirror(account));if(IsDisposed||generation!=store.Generation||!prefs.LanEnabled||tokens==null||tokens.Account!=account||mirror==null)return;
                tier=new TierState{Tier=mirror.Current(snapshot)?mirror.Tier:-1,Count=8};if(mirrorReceived!=mirror.Received){mirrorReceived=mirror.Received;var main=snapshot==null?null:snapshot.Main;
                    today=main==null?null:mirror.Today;status="휴대폰 동기화 · "+DateTimeOffset.FromUnixTimeMilliseconds(mirror.Received).LocalDateTime.ToString("HH:mm");}}
            catch{if(!IsDisposed)status="휴대폰 기록을 읽지 못했습니다. 기존 기록을 유지합니다.";}finally{readingMirror=false;}}
        private bool StartupEnabled(){using(var key=Registry.CurrentUser.OpenSubKey(@"Software\Microsoft\Windows\CurrentVersion\Run")){return key!=null&&key.GetValue("CodexMeterWidget")!=null;}}
        private void SetStartup(bool on){try{using(var key=Registry.CurrentUser.CreateSubKey(@"Software\Microsoft\Windows\CurrentVersion\Run")){if(on)key.SetValue("CodexMeterWidget","\""+Application.ExecutablePath+"\"");else key.DeleteValue("CodexMeterWidget",false);}}catch{MessageBox.Show(this,"자동 실행 설정을 저장하지 못했습니다.");}}
        private async void BeginLogin(){
            if(busy)return;if(tokens!=null&&MessageBox.Show(this,"계정을 변경하려면 현재 위젯에서 로그아웃합니다. 기록은 계정별로 보존됩니다.","계정 변경",MessageBoxButtons.OKCancel)!=DialogResult.OK)return;
            try{store.SignOut();StopLan();}catch{status="로그아웃 정보를 저장하지 못했습니다.";Invalidate();return;}tokens=null;snapshot=null;tier=new TierState();today=null;loggedOut=false;busy=true;status="브라우저에서 로그인해 주세요.";Invalidate();operation=CancellationTokenSource.CreateLinkedTokenSource(lifetime.Token);long generation=store.Generation;
            try{var result=await Login.Run(api,operation.Token);if(lifetime.IsCancellationRequested||generation!=store.Generation)return;if(!store.SaveTokens(result,generation))return;tokens=result;status="로그인했습니다.";}
            catch(OperationCanceledException){status="로그인 대기를 취소했습니다.";}catch{status="로그인 실패. 메뉴에서 다시 시도해 주세요.";}finally{busy=false;operation.Dispose();operation=null;if(!IsDisposed)Invalidate();}
            if(tokens!=null&&!lifetime.IsCancellationRequested){StartLan();RefreshUsage();}
        }
        private async void RefreshUsage(){
            if(busy||tokens==null)return;if(Clock.Now-lastAttempt<30000){status="잠시 후 다시 조회할 수 있습니다.";Invalidate();return;}
            busy=true;lastAttempt=Clock.Now;status="조회 중…";Invalidate();long generation=store.Generation;var current=tokens;operation=CancellationTokenSource.CreateLinkedTokenSource(lifetime.Token);
            try{
                if(current.Expires<=Clock.Now+60000){current=await api.Refresh(current,operation.Token);if(!store.SaveTokens(current,generation))return;tokens=current;}
                var value=await api.Fetch(current,operation.Token);var account=current.Account;
                var rating=await Task.Run(()=>{store.SaveSnapshot(current,value,generation);var rows=store.LoadRows(account);var main=value.Main;return new {Tier=main!=null&&main.Meter=="weekly"?store.Rating(account,"weekly|"+value.Plan+"|"+main.Seconds,rows,generation):new TierState(),Today=main==null?null:DailyUsage.Observed(rows,main.Meter+"|"+value.Plan+"|"+main.Seconds,Clock.Day(Clock.Now))};},operation.Token);
                if(lifetime.IsCancellationRequested||generation!=store.Generation)return;snapshot=value;tier=prefs.LanEnabled?new TierState():rating.Tier;today=rating.Today;status=value.Main==null?"제공된 주요 한도가 없습니다.":"정상 조회 · "+DateTime.Now.ToString("HH:mm");loggedOut=false;ReadPhoneTier();
            }catch(OperationCanceledException){status="조회가 취소되었습니다.";}catch(Exception e){status=e is InvalidOperationException?e.Message:"조회 또는 기록 저장 실패. 마지막 관측값을 유지합니다.";loggedOut=true;}
            finally{busy=false;operation.Dispose();operation=null;if(!IsDisposed)Invalidate();}
        }
        private void SignOut(){if(operation!=null)operation.Cancel();try{store.SignOut();StopLan();tokens=null;snapshot=null;tier=new TierState();today=null;status="로그아웃했습니다. 계정별 기록은 보존됩니다.";Invalidate();}catch{status="로그아웃 저장 실패.";Invalidate();}}
        public void Fixture(Snapshot value,int rank,double? daily,string note){snapshot=value;tier=new TierState{Tier=rank};today=daily;status=note;}
        protected override void OnPaint(PaintEventArgs e){base.OnPaint(e);Draw(e.Graphics,ClientSize,prefs,snapshot,tier,today,status,busy,loggedOut,crests,out action,out menuButton);}
        public static void Draw(Graphics g,Size size,Preferences options,Snapshot value,TierState rank,double? daily,string note,bool working,bool failed,Image[] images,out RectangleF action,out RectangleF menuButton){
            g.SmoothingMode=SmoothingMode.AntiAlias;g.Clear(Color.FromArgb(13,14,16));float scale=Math.Max(.8f,Math.Min(1.15f,size.Width/400f)),font=(float)options.FontFactor;Color fg=Color.FromArgb(245,245,245),muted=Color.FromArgb(190,196,204),mint=Color.FromArgb(104,225,177);
            using(var border=new Pen(Color.FromArgb(53,60,67),1))g.DrawRectangle(border,1,1,size.Width-3,size.Height-3);
            Action<string,float,float,float,Color,float> text=(message,x,y,points,color,width)=>{using(var f=new Font("Malgun Gothic",points*scale*font,FontStyle.Regular,GraphicsUnit.Pixel))using(var b=new SolidBrush(color))using(var format=new StringFormat{Trimming=StringTrimming.EllipsisCharacter,FormatFlags=StringFormatFlags.NoWrap})g.DrawString(message,f,b,new RectangleF(x*scale,y,width*scale,points*scale*font*1.7f),format);};
            text("CODEX METER",20,14,13,muted,280);menuButton=new RectangleF(size.Width-46,9,36,34);text("⋯",size.Width/scale-42,8,26,fg,28);
            float top=54*font;var main=value==null?null:value.Quotas.FirstOrDefault(x=>x.Meter=="weekly")??value.Quotas.FirstOrDefault(x=>x.Meter=="monthly")??(options.ShowFive?value.Quotas.FirstOrDefault(x=>x.Meter=="five_hour"):null);bool fresh=value!=null&&value.Fresh(Clock.Now)&&!failed;
            if(images!=null&&images.Length==10&&images[Math.Max(0,Math.Min(9,rank.Tier))]!=null){var rect=new RectangleF(18*scale,top,96*scale,96*scale);if(rank.Tier<0){using(var attrs=new System.Drawing.Imaging.ImageAttributes()){var matrix=new System.Drawing.Imaging.ColorMatrix();matrix.Matrix33=.35f;attrs.SetColorMatrix(matrix);g.DrawImage(images[0],Rectangle.Round(rect),0,0,images[0].Width,images[0].Height,GraphicsUnit.Pixel,attrs);}}else g.DrawImage(images[rank.Tier],rect);}
            text(rank.Tier<0?"티어 기록 수집 중":TierNames[rank.Tier]+(rank.Count<4?" · 잠정":""),20,top+98*scale,11,muted,130);
            string label=main==null?"사용량":main.Meter=="weekly"?"주간 잔여량":main.Meter=="monthly"?"월간 잔여량":"5시간 잔여량";
            text(label,142,top,13,muted,230);text(main==null?"—":(100-main.Used).ToString("0.#",CultureInfo.InvariantCulture)+"%",140,top+25*scale*font,40,fg,235);
            float barY=top+90*scale*font;using(var track=new SolidBrush(Color.FromArgb(42,48,55)))g.FillRectangle(track,142*scale,barY,size.Width-165*scale,5*scale);if(main!=null)using(var fill=new SolidBrush(fresh?mint:muted))g.FillRectangle(fill,142*scale,barY,(size.Width-165*scale)*(float)(100-main.Used)/100,5*scale);
            long left=main==null?0:main.Reset-Clock.Now;string reset=main==null||main.Reset<=0?"초기화 정보 없음":left<=0?"초기화 확인 필요":TimeSpan.FromMilliseconds(left).TotalDays>=1?(int)TimeSpan.FromMilliseconds(left).TotalDays+"일 "+TimeSpan.FromMilliseconds(left).Hours+"시간 후 초기화":TimeSpan.FromMilliseconds(left).Hours+"시간 "+TimeSpan.FromMilliseconds(left).Minutes+"분 후 초기화";
            float lower=top+130*scale*font;text(reset,20,lower,13,muted,350);text("오늘 관측 · "+(main!=null&&daily.HasValue?daily.Value.ToString("0.#",CultureInfo.InvariantCulture)+"%p":"비교 관측 부족"),20,lower+24*font,12,muted,350);
            var five=value==null?null:value.Quotas.FirstOrDefault(x=>x.Meter=="five_hour");if(options.ShowFive&&five!=null&&main!=five)text("5시간 잔여 · "+(100-five.Used).ToString("0.#")+"%",20,lower+48*font,12,muted,350);
            float bottom=size.Height-36*font;text((main!=null&&!fresh?"마지막 관측값 · ":"")+note,20,bottom,11,muted,240);action=new RectangleF(size.Width-116*scale,size.Height-41*font,104*scale,30*font);using(var b=new SolidBrush(Color.FromArgb(35,44,42)))g.FillRectangle(b,action);text(working?"처리 중":value==null&&note.Contains("로그인")?"로그인":"새로고침",size.Width/scale-112,size.Height-37*font,12,mint,100);
        }
        protected override void Dispose(bool disposing){if(disposing){StopLan();lifetime.Cancel();if(operation!=null)operation.Cancel();timer.Dispose();tray.Visible=false;tray.Dispose();if(menu!=null)menu.Dispose();foreach(var image in crests)if(image!=null)image.Dispose();lifetime.Dispose();}base.Dispose(disposing);}
    }
}
