using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Text;
using System.Threading;
using System.Text.RegularExpressions;

namespace CodexMeterWidget {
    public sealed class TaskView {
        public string Thread,Turn,State,Name; public long At,Seen,Sequence;
    }
    // Read-only fallback. Never starts Codex, installs hooks, writes transcripts or interprets tool errors.
    public sealed class TaskObserve:IDisposable {
        private readonly object gate=new object();private readonly string root;
        private readonly Dictionary<string,long> positions=new Dictionary<string,long>();
        private readonly Dictionary<string,TaskView> tasks=new Dictionary<string,TaskView>();
        private readonly string epoch=Guid.NewGuid().ToString("N");private readonly DateTime opened=DateTime.UtcNow;private Timer timer;private bool stopped,scanning,verified;private long sequence,checkedAt;
        public TaskObserve(string directory){root=directory;Scan(true);timer=new Timer(_=>Scan(false),null,2000,2000);}
        private void Scan(bool initial){lock(gate){if(stopped||scanning)return;scanning=true;try{
            if(!Directory.Exists(root))return;
            var files=Directory.EnumerateFiles(root,"rollout-*.jsonl",SearchOption.AllDirectories).Select(p=>new FileInfo(p)).OrderByDescending(f=>f.LastWriteTimeUtc).Take(24).ToArray();
            var active=new HashSet<string>(files.Select(f=>f.FullName));foreach(string old in positions.Keys.Where(p=>!active.Contains(p)).ToArray())positions.Remove(old);
            foreach(var file in files){long offset;if(!positions.TryGetValue(file.FullName,out offset)){offset=initial||file.CreationTimeUtc<opened?file.Length:0;positions[file.FullName]=offset;}
                if(file.Length<offset){positions[file.FullName]=file.Length;continue;}if(file.Length==offset)continue;
                var match=Regex.Match(file.Name,@"([a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12})\.jsonl$");if(!match.Success)continue;
                using(var stream=new FileStream(file.FullName,FileMode.Open,FileAccess.Read,FileShare.ReadWrite|FileShare.Delete)){
                    stream.Seek(offset,SeekOrigin.Begin);int amount=(int)Math.Min(1024*1024,stream.Length-offset);byte[] bytes=new byte[amount];int read=stream.Read(bytes,0,amount),start=0;
                    for(int i=0;i<read;i++)if(bytes[i]==10){if(i-start<=256*1024){try{var entry=Json.Read(Encoding.UTF8.GetString(bytes,start,i-start));
                        if(Json.Text(entry,"type")=="event_msg"){var payload=entry.ContainsKey("payload")?entry["payload"] as Dictionary<string,object>:null;
                            if(payload!=null){string kind=Json.Text(payload,"type");if(kind=="task_started"||kind=="task_complete"||kind=="turn_aborted"){
                                DateTimeOffset at;if(DateTimeOffset.TryParse(Json.Text(entry,"timestamp"),out at))Apply(match.Groups[1].Value,Json.Text(payload,"turn_id"),kind,at.ToUnixTimeMilliseconds());}}}
                    }catch{/* Malformed/partial entry cannot interrupt Codex or the widget. */}}start=i+1;}
                    positions[file.FullName]=offset+start;
                    // An oversize incomplete line is skipped, never retained as transcript text.
                    if(start==0&&read==1024*1024)positions[file.FullName]=stream.Length;
                }}checkedAt=Clock.Now;
            }catch{checkedAt=0;}finally{scanning=false;}}}
        public void Apply(string thread,string turn,string kind,long at){lock(gate){if(stopped||!Valid(thread)||!Valid(turn)||at<=0||at>Clock.Now+300000)return;
            TaskView prior;tasks.TryGetValue(thread,out prior);if(prior!=null&&(at<prior.At||(kind!="task_started"&&turn!=prior.Turn)||(kind=="task_started"&&turn==prior.Turn)))return;
            string state=kind=="task_started"?"running":kind=="task_complete"?"completed":kind=="turn_aborted"?"interrupted":null;if(state==null)return;
            if(prior==null&&kind!="task_started")return;
            if(prior!=null&&prior.Turn==turn&&prior.State==state)return;
            tasks[thread]=new TaskView{Thread=thread,Turn=turn,State=state,Name="Codex · "+thread.Substring(0,6),At=at,Seen=Clock.Now,Sequence=++sequence};verified=true;
            while(tasks.Count>10)tasks.Remove(tasks.OrderBy(x=>x.Value.Sequence).First().Key);
        }}
        private static bool Valid(string id){return id!=null&&Regex.IsMatch(id,@"^[a-f0-9-]{16,40}$");}
        public Dictionary<string,object> Snapshot(){lock(gate){return new Dictionary<string,object>{{"epoch",epoch},{"device",Environment.MachineName.Length==0?"pc":LocalStore.AccountKey(Environment.MachineName)},{"checked",checkedAt},{"verified",verified},{"source","local_log_trial"},{"rows",tasks.Values.OrderByDescending(t=>t.Sequence).Take(3).ToArray()}};}}
        public void Dispose(){lock(gate){stopped=true;if(timer!=null){timer.Dispose();timer=null;}tasks.Clear();positions.Clear();}}
    }
}
