package np.evan.musicbridge;

import android.app.ActivityManager;
import android.content.*;
import android.graphics.*;
import android.media.MediaMetadata;
import android.media.session.*;
import android.os.*;
import java.io.*;
import java.util.*;

/** App-process side: decides what the lock screen shows (test song or the active player), writes covers and tells System UI. */
public final class Bridge {
 static final String META="com.oppo.music.service.meta_changed",STATE="com.oppo.music.service.playstate_changed",THUMB="com.oppo.music.thumb.update";
 static final String[] ART_WAYS={"App storage","Android/media","Download folder"};
 static final String[] artPaths=new String[3],artStatus={"not written","not written","not written"};
 private static int testIndex;private static Bitmap lastArt;

 static SharedPreferences prefs(Context c){return c.getSharedPreferences("bridge",Context.MODE_PRIVATE);}
 static int artWay(Context c){return prefs(c).getInt("artWay",0);}
 static boolean reconnect(Context c){return prefs(c).getBoolean("reconnect",true);}
 static void log(Context c,String s){Store.log(c,s);}
 static Store.State state(Context c){Store.State s=Store.read(c);return s==null?new Store.State():s;}

 /** Broadcasts first (they work while the lock screen is listening); then, if enabled, restarts the :engine process so
  *  System UI reconnects, which is when the engine re-reads title, artist and cover. */
 static void publish(Context c,Store.State s,boolean art){
  Store.write(c,s);
  c.sendBroadcast(new Intent(STATE));c.sendBroadcast(new Intent(META));
  if(art&&s.art!=null)c.sendBroadcast(new Intent(THUMB).putExtra("thumb path",s.art).putExtra("is online",false));
  log(c,"→ broadcast "+(art?"state+meta+thumb":"state+meta"));
  if(reconnect(c))new Handler(Looper.getMainLooper()).postDelayed(()->restartEngine(c),400);
 }
 static void restartEngine(Context c){
  for(ActivityManager.RunningAppProcessInfo p:c.getSystemService(ActivityManager.class).getRunningAppProcesses())
   if(p.processName.endsWith(":engine")){log(c,"↻ restarting the lock-screen connection");android.os.Process.killProcess(p.pid);return;}
  log(c,"↻ nothing to restart (System UI is not connected)");
 }

 /** The cover goes to three places, because System UI has to open the file itself and may not be allowed into all of them. */
 static String writeArt(Context c,Bitmap b){
  if(b==null){for(int i=0;i<3;i++){artPaths[i]=null;artStatus[i]="no cover";}return null;}
  long stamp=System.currentTimeMillis();
  File[] files=new File[3];
  files[0]=new File(c.getFilesDir(),"cover.png");
  File[] media=c.getExternalMediaDirs();files[1]=media.length>0&&media[0]!=null?new File(media[0],"cover.png"):null;
  files[2]=new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),"StudioMusicBridge/cover.png");
  for(int i=0;i<3;i++){
   File f=files[i];if(f==null){artPaths[i]=null;artStatus[i]="unavailable";continue;}
   try{
    File dir=f.getParentFile();if(dir!=null&&!dir.isDirectory()&&!dir.mkdirs())throw new IOException("cannot create folder");
    File tmp=new File(f.getPath()+".tmp");try(OutputStream o=new FileOutputStream(tmp)){b.compress(Bitmap.CompressFormat.PNG,100,o);}if(!tmp.renameTo(f))throw new IOException("rename failed");
    if(i==0){f.setReadable(true,false);for(File d=f.getParentFile();d!=null&&d.getPath().startsWith(c.getApplicationInfo().dataDir);d=d.getParentFile())d.setExecutable(true,false);}
    artPaths[i]=f.getAbsolutePath();artStatus[i]="written "+android.text.format.DateFormat.format("HH:mm:ss",stamp);
   }catch(Exception e){artPaths[i]=null;artStatus[i]="failed: "+e.getMessage();}
  }
  String p=artPaths[artWay(c)];if(p!=null)return p;for(String q:artPaths)if(q!=null)return q;return null;
 }

 static Bitmap testCover(int n){
  Bitmap b=Bitmap.createBitmap(512,512,Bitmap.Config.ARGB_8888);Canvas c=new Canvas(b);Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
  int[][] colours={{0xff7f5af0,0xff2cb67d},{0xffff8906,0xfff25f4c},{0xff3da9fc,0xff094067}};int[] k=colours[n%3];
  p.setShader(new LinearGradient(0,0,512,512,k[0],k[1],Shader.TileMode.CLAMP));c.drawRect(0,0,512,512,p);p.setShader(null);
  p.setColor(0xffffffff);p.setTextAlign(Paint.Align.CENTER);p.setTextSize(150);p.setFakeBoldText(true);c.drawText("♪",256,250,p);p.setTextSize(64);c.drawText("TEST "+(n+1),256,380,p);
  return b;
 }
 /** Fills in the test song before System UI's first call when nothing is set yet. */
 static void seed(Context c){if(Store.read(c)==null){Store.State s=testState(c,0);Store.write(c,s);log(c,"test song 1 prepared");}}
 private static Store.State testState(Context c,int n){
  testIndex=n;Store.State s=new Store.State();s.hasTrack=true;s.live=false;s.title="Bridge test song "+(n+1);s.artist="Lockscreen Studio";s.album="Music Bridge";s.id=100+n;s.duration=200000+n*15000;s.positionAt=SystemClock.elapsedRealtime();
  Bitmap b=testCover(n);s.art=writeArt(c,b);b.recycle();return s;
 }
 static void testTrack(Context c,int n){Store.State s=testState(c,n);log(c,"test song "+(n+1)+" ready");publish(c,s,true);}

 static void control(Context c,String cmd,String from){
  log(c,"control from "+from+": "+cmd);Store.State s=state(c);
  MediaController m=MediaListener.controller;
  if(s.live&&m!=null){MediaController.TransportControls t=m.getTransportControls();switch(cmd){case "play":t.play();break;case "pause":case "stop":t.pause();break;case "next":t.skipToNext();break;case "previous":t.skipToPrevious();break;}return;}
  switch(cmd){
   case "play":s.position=Store.position(s);s.positionAt=SystemClock.elapsedRealtime();s.playing=true;break;
   case "pause":case "stop":s.position=Store.position(s);s.positionAt=SystemClock.elapsedRealtime();s.playing=false;break;
   case "next":testTrack(c,testIndex+1);return;
   case "previous":testTrack(c,Math.max(0,testIndex-1));return;
  }
  publish(c,s,false);
 }

 /** Copies the active player's details; called from the notification listener. */
 static void fromController(Context c,MediaController m){
  Store.State old=state(c),s=new Store.State();s.live=true;
  if(m==null){s.hasTrack=false;log(c,"no active player");publish(c,s,false);return;}
  MediaMetadata md=m.getMetadata();PlaybackState st=m.getPlaybackState();
  String t=md==null?null:md.getString(MediaMetadata.METADATA_KEY_TITLE),a=md==null?null:first(md.getString(MediaMetadata.METADATA_KEY_ARTIST),md.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST));
  s.hasTrack=t!=null;s.title=t==null?"":t;s.artist=a==null?"":a;s.album=md==null?"":first(md.getString(MediaMetadata.METADATA_KEY_ALBUM),"");
  s.duration=md==null?0:Math.max(0,md.getLong(MediaMetadata.METADATA_KEY_DURATION));
  s.playing=st!=null&&st.getState()==PlaybackState.STATE_PLAYING;s.position=st==null?0:Math.max(0,st.getPosition());s.positionAt=st==null?SystemClock.elapsedRealtime():st.getLastPositionUpdateTime();
  boolean changed=!old.live||!Objects.equals(s.title,old.title)||!Objects.equals(s.artist,old.artist);s.id=changed?old.id+1:old.id;
  Bitmap art=md==null?null:md.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART);if(art==null&&md!=null)art=md.getBitmap(MediaMetadata.METADATA_KEY_ART);if(art==null&&md!=null)art=md.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON);
  boolean newArt=art!=null&&(lastArt==null||!art.sameAs(lastArt));
  if(newArt||changed&&art==null){s.art=writeArt(c,art);lastArt=art==null?null:art.copy(Bitmap.Config.ARGB_8888,false);}else s.art=old.art;
  if(!changed&&!newArt&&s.playing==old.playing)return;
  log(c,"player "+m.getPackageName()+": "+(s.playing?"playing ":"paused ")+s.title+" — "+s.artist);
  publish(c,s,newArt||changed);
 }
 private static String first(String a,String b){return a!=null&&!a.isEmpty()?a:b;}
}
