package np.evan.musicbridge;

import android.content.Context;
import android.os.SystemClock;
import org.json.JSONObject;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

/** State and log shared between the app process and the :engine process (the one System UI talks to). */
final class Store {
 static final class State {boolean hasTrack,playing,live;String title="",artist="",album="",art;long id,duration,position,positionAt;}
 private static State cached;private static long cachedAt=-1;

 static synchronized void write(Context c,State s){
  try{JSONObject j=new JSONObject().put("hasTrack",s.hasTrack).put("playing",s.playing).put("live",s.live).put("title",s.title).put("artist",s.artist).put("album",s.album).put("art",s.art==null?"":s.art)
    .put("id",s.id).put("duration",s.duration).put("position",s.position).put("positionAt",s.positionAt);
   File f=new File(c.getFilesDir(),"state.json"),t=new File(c.getFilesDir(),"state.tmp");
   try(OutputStream o=new FileOutputStream(t)){o.write(j.toString().getBytes(StandardCharsets.UTF_8));}t.renameTo(f);
  }catch(Exception e){log(c,"state write failed: "+e);}
 }
 /** Read on every call from System UI; cheap because it is re-parsed only when the file changed. */
 static synchronized State read(Context c){
  File f=new File(c.getFilesDir(),"state.json");if(!f.isFile())return null;
  if(cached!=null&&f.lastModified()==cachedAt)return cached;
  try(InputStream in=new FileInputStream(f)){ByteArrayOutputStream b=new ByteArrayOutputStream();byte[] buf=new byte[4096];int n;while((n=in.read(buf))>0)b.write(buf,0,n);
   JSONObject j=new JSONObject(b.toString("UTF-8"));State s=new State();s.hasTrack=j.optBoolean("hasTrack");s.playing=j.optBoolean("playing");s.live=j.optBoolean("live");s.title=j.optString("title");s.artist=j.optString("artist");s.album=j.optString("album");
   String a=j.optString("art");s.art=a.isEmpty()?null:a;s.id=j.optLong("id");s.duration=j.optLong("duration");s.position=j.optLong("position");s.positionAt=j.optLong("positionAt");cached=s;cachedAt=f.lastModified();return s;
  }catch(Exception e){return cached;}
 }
 static long position(State s){long p=s.position+(s.playing?SystemClock.elapsedRealtime()-s.positionAt:0);return Math.max(0,s.duration>0?Math.min(s.duration,p):p);}

 static synchronized void log(Context c,String line){
  try{File f=new File(c.getFilesDir(),"log.txt");if(f.length()>200_000){File old=new File(c.getFilesDir(),"log.old");old.delete();f.renameTo(old);}
   try(Writer w=new OutputStreamWriter(new FileOutputStream(f,true),StandardCharsets.UTF_8)){w.write(new SimpleDateFormat("HH:mm:ss ",Locale.US).format(new Date())+line+"\n");}
  }catch(IOException ignored){}
 }
 /** Newest lines first. */
 static String tail(Context c,int max){
  File f=new File(c.getFilesDir(),"log.txt");if(!f.isFile())return "";
  try{List<String> lines=new ArrayList<>();try(BufferedReader r=new BufferedReader(new InputStreamReader(new FileInputStream(f),StandardCharsets.UTF_8))){String l;while((l=r.readLine())!=null){lines.add(l);if(lines.size()>max)lines.remove(0);}}
   Collections.reverse(lines);return String.join("\n",lines);}catch(IOException e){return "";}
 }
 static void clearLog(Context c){new File(c.getFilesDir(),"log.txt").delete();}
}
