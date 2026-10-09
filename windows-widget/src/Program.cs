using System;
using System.IO;
using System.Threading;
using System.Windows.Forms;
using System.Web.Script.Serialization;

namespace CodexMeterWidget {
    public sealed partial class LocalStore {
        public TierState Rating(string account,string policy,System.Collections.Generic.List<Observation> rows,long expected){lock(gate){
            if(expected!=generation)throw new OperationCanceledException();string path=Path.Combine(DirectoryPath,"accounts",AccountKey(account),"tier-"+AccountKey(policy)+".json");
            var state=File.Exists(path)?new JavaScriptSerializer().Deserialize<TierState>(File.ReadAllText(path)):new TierState();
            if(state.Tier< -1||state.Tier>9||state.Count<0||state.Count>8||state.Previous< -1||state.Previous>9||state.LowStreak<0||state.LowStreak>1||state.End<0||double.IsNaN(state.Score)||state.Score<0||state.Score>100)throw new InvalidDataException("티어 기록을 확인할 수 없습니다.");
            var evidence=TierRules.Completed(rows,policy,Clock.Now);var prior=state;foreach(var e in evidence)state=TierRules.Evaluate(state,evidence,e.End);
            if(state.End!=prior.End)AtomicWrite(path,System.Text.Encoding.UTF8.GetBytes(Json.Write(state)));return state;
        }}
    }
    internal static class Program {
        [STAThread] private static void Main(){
            bool created;using(var instance=new Mutex(true,"Local\\CodexMeterWidget-v1",out created)){
                if(!created){MessageBox.Show("Codex Meter 위젯이 이미 실행 중입니다. 작업 표시줄의 숨겨진 아이콘에서 다시 열 수 있습니다.");return;}
                System.Net.ServicePointManager.SecurityProtocol=System.Net.SecurityProtocolType.Tls12;
                Application.EnableVisualStyles();Application.SetCompatibleTextRenderingDefault(false);
                string data=Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData),"CodexMeterWidget");
                try{Directory.CreateDirectory(data);using(var api=new MeterApi())using(var form=new WidgetForm(new LocalStore(data),api))Application.Run(form);}
                catch{MessageBox.Show("위젯을 시작하지 못했습니다. 기존 데이터를 지우지 말고 실행 환경을 확인해 주세요.","Codex Meter");}
            }
        }
    }
}
