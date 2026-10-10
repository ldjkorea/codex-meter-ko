using System;
using System.Collections.Generic;
using System.IO;
using System.Runtime.InteropServices;
using System.Text;

namespace CodexMeterWidget {
 // OS-bundled SQLite, opened read-only. Never reads prompts, previews, auth or token columns.
 internal static class TaskTitles {
  [DllImport("winsqlite3",CallingConvention=CallingConvention.Cdecl)] static extern int sqlite3_open_v2(byte[] path,out IntPtr db,int flags,IntPtr vfs);
  [DllImport("winsqlite3",CallingConvention=CallingConvention.Cdecl)] static extern int sqlite3_close(IntPtr db);
  [DllImport("winsqlite3",CallingConvention=CallingConvention.Cdecl)] static extern int sqlite3_busy_timeout(IntPtr db,int ms);
  [DllImport("winsqlite3",CallingConvention=CallingConvention.Cdecl)] static extern int sqlite3_prepare_v2(IntPtr db,byte[] sql,int length,out IntPtr statement,IntPtr tail);
  [DllImport("winsqlite3",CallingConvention=CallingConvention.Cdecl)] static extern int sqlite3_bind_text(IntPtr statement,int index,byte[] value,int length,IntPtr destructor);
  [DllImport("winsqlite3",CallingConvention=CallingConvention.Cdecl)] static extern int sqlite3_step(IntPtr statement);
  [DllImport("winsqlite3",CallingConvention=CallingConvention.Cdecl)] static extern IntPtr sqlite3_column_text(IntPtr statement,int column);
  [DllImport("winsqlite3",CallingConvention=CallingConvention.Cdecl)] static extern int sqlite3_column_bytes(IntPtr statement,int column);
  [DllImport("winsqlite3",CallingConvention=CallingConvention.Cdecl)] static extern int sqlite3_finalize(IntPtr statement);
  static byte[] Utf8(string s){return Encoding.UTF8.GetBytes(s+"\0");}
  public static string Clean(string name) {
   if(string.IsNullOrWhiteSpace(name))return "";
   var b=new StringBuilder();foreach(char ch in name){if(char.IsControl(ch)||ch=='\u2028'||ch=='\u2029')b.Append(' ');else b.Append(ch);}
   string value=System.Text.RegularExpressions.Regex.Replace(b.ToString(),@"\s+"," ").Trim();
   value=System.Text.RegularExpressions.Regex.Replace(value,@"(?i)(bearer\s+\S+|sk-[a-z0-9_-]{12,}|eyJ[a-z0-9_-]+\.[a-z0-9_-]+\.[a-z0-9_-]+)","[redacted]");
   if(value.Length>80){int end=80;if(char.IsHighSurrogate(value[end-1]))end--;value=value.Substring(0,end)+"…";}return value;
  }
  public static string Read(string path,string thread) {
   if(!File.Exists(path))return "";IntPtr db=IntPtr.Zero,statement=IntPtr.Zero;
   try {
    if(sqlite3_open_v2(Utf8(path),out db,1,IntPtr.Zero)!=0)return "";sqlite3_busy_timeout(db,40);
    // title is the app's displayed thread title, not first_user_message.
    byte[] sql=Utf8("SELECT CASE WHEN length(trim(COALESCE(name,'')))>0 THEN name ELSE title END FROM threads WHERE id=?1 LIMIT 1");
    if(sqlite3_prepare_v2(db,sql,-1,out statement,IntPtr.Zero)!=0){
     if(statement!=IntPtr.Zero){sqlite3_finalize(statement);statement=IntPtr.Zero;}
     sql=Utf8("SELECT title FROM threads WHERE id=?1 LIMIT 1");if(sqlite3_prepare_v2(db,sql,-1,out statement,IntPtr.Zero)!=0)return "";
    }
    byte[] id=Encoding.UTF8.GetBytes(thread);if(sqlite3_bind_text(statement,1,id,id.Length,new IntPtr(-1))!=0||sqlite3_step(statement)!=100)return "";
    int size=sqlite3_column_bytes(statement,0);if(size<=0||size>16384)return "";byte[] bytes=new byte[size];Marshal.Copy(sqlite3_column_text(statement,0),bytes,0,size);return Clean(Encoding.UTF8.GetString(bytes));
   }catch{return "";}finally{if(statement!=IntPtr.Zero)sqlite3_finalize(statement);if(db!=IntPtr.Zero)sqlite3_close(db);}
  }
 }
}
