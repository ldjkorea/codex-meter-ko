using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.IO;
using System.Net;
using System.Net.Sockets;
using System.Security.Cryptography;
using System.Text;
using System.Threading;
using System.Threading.Tasks;

namespace CodexMeterWidget {
    public static class Login {
        public static string RandomValue(){byte[] bytes=new byte[32];using(var random=RandomNumberGenerator.Create())random.GetBytes(bytes);return Base64(bytes);}
        public static string Base64(byte[] bytes){return Convert.ToBase64String(bytes).TrimEnd('=').Replace('+','-').Replace('/','_');}
        public static string Challenge(string verifier){using(var sha=SHA256.Create())return Base64(sha.ComputeHash(Encoding.ASCII.GetBytes(verifier)));}
        public static string Authorize(string redirect,string verifier,string state){
            var values=new Dictionary<string,string>{{"response_type","code"},{"client_id",MeterApi.ClientId},{"redirect_uri",redirect},{"scope","openid profile email offline_access"},{"code_challenge",Challenge(verifier)},{"code_challenge_method","S256"},{"id_token_add_organizations","true"},{"codex_cli_simplified_flow","true"},{"state",state},{"originator","codex-meter-windows"}};
            var parts=new List<string>();foreach(var pair in values)parts.Add(Uri.EscapeDataString(pair.Key)+"="+Uri.EscapeDataString(pair.Value));return "https://auth.openai.com/oauth/authorize?"+string.Join("&",parts);
        }
        public static string CodeFromTarget(string target,string expected){
            if(target==null||target.Length>8192||!target.StartsWith("/auth/callback?",StringComparison.Ordinal))return null;
            var values=new Dictionary<string,string>();foreach(string pair in target.Substring(target.IndexOf('?')+1).Split('&')){int at=pair.IndexOf('=');if(at<0)continue;string key=Uri.UnescapeDataString(pair.Substring(0,at));if(values.ContainsKey(key))return null;values[key]=Uri.UnescapeDataString(pair.Substring(at+1).Replace('+',' '));}
            string state,code;if(!values.TryGetValue("state",out state)||!values.TryGetValue("code",out code)||code.Length==0)return null;
            var a=Encoding.UTF8.GetBytes(state);var b=Encoding.UTF8.GetBytes(expected);int different=a.Length^b.Length;for(int i=0;i<Math.Min(a.Length,b.Length);i++)different|=a[i]^b[i];return different==0?code:null;
        }
        public static async Task<Tokens> Run(MeterApi api,CancellationToken cancel){
            TcpListener listener=null;foreach(int port in new[]{1455,1457}){try{var candidate=new TcpListener(IPAddress.Loopback,port);candidate.Start(4);listener=candidate;break;}catch(SocketException){}}
            if(listener==null)throw new InvalidOperationException("로그인 포트가 사용 중입니다. 진행 중인 다른 로그인을 마친 뒤 다시 시도해 주세요.");
            using(var deadline=CancellationTokenSource.CreateLinkedTokenSource(cancel)){
                deadline.CancelAfter(TimeSpan.FromMinutes(5));using(deadline.Token.Register(()=>listener.Stop()))try{
                    string verifier=RandomValue(),state=RandomValue(),redirect="http://localhost:"+((IPEndPoint)listener.LocalEndpoint).Port+"/auth/callback";
                    Process.Start(new ProcessStartInfo(Authorize(redirect,verifier,state)){UseShellExecute=true});
                    while(true){
                        deadline.Token.ThrowIfCancellationRequested();using(var client=await listener.AcceptTcpClientAsync().ConfigureAwait(false)){
                            client.ReceiveTimeout=5000;client.SendTimeout=5000;string code=null;
                            await Task.Run(()=>{using(var stream=client.GetStream()){
                                var request=new StringBuilder();while(request.Length<16384){int value=stream.ReadByte();if(value<0)break;request.Append((char)value);if(request.ToString().EndsWith("\r\n\r\n",StringComparison.Ordinal))break;}
                                string[] first=request.ToString().Split(new[]{'\r','\n'},StringSplitOptions.RemoveEmptyEntries);if(first.Length>0){string[] line=first[0].Split(' ');if(line.Length==3&&line[0]=="GET")code=CodeFromTarget(line[1],state);}
                                byte[] body=Encoding.UTF8.GetBytes(code==null?"로그인 요청을 확인할 수 없습니다.":"로그인 응답을 받았습니다. Codex Meter 위젯에서 결과를 확인해 주세요.");
                                byte[] header=Encoding.ASCII.GetBytes("HTTP/1.1 "+(code==null?"400 Bad Request":"200 OK")+"\r\nContent-Type: text/plain; charset=utf-8\r\nContent-Length: "+body.Length+"\r\nConnection: close\r\nCache-Control: no-store\r\n\r\n");stream.Write(header,0,header.Length);stream.Write(body,0,body.Length);
                            }},deadline.Token).ConfigureAwait(false);
                            if(code!=null)return await api.Exchange(code,verifier,redirect,deadline.Token).ConfigureAwait(false);
                        }
                    }
                }catch(Exception){if(deadline.IsCancellationRequested)throw new OperationCanceledException("로그인이 취소되었거나 대기 시간이 끝났습니다.");throw;}finally{listener.Stop();}
            }
        }
    }
}
