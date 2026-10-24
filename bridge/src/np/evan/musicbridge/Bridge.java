package np.evan.musicbridge;

import android.content.*;
import android.graphics.*;
import android.media.MediaMetadata;
import android.media.session.*;
import android.os.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

/** What the lock screen is shown (a test track or the phone's active player), the cover files, and a call log. */
public final class Bridge {
 static final String META="com.oppo.music.service.meta_changed",STATE="com.oppo.music.service.playstate_changed",QUEUE="com.oppo.music.service.queue_changed",THUMB="com.oppo.music.thumb.update";
 static final String[] ART_WAYS={"App storage","Android/media","Download folder"};
 static volatile boolean live,playing,hasTrack=true;
 static volatile String title="Bridge test song",artist="Lockscreen Studio",album="Music Bridge";
 static volatile long id=1,duration=214000,position,positionAt=SystemClock.elapsedRealtime();
 static volatile int artWay;
 static final String[] artPaths=new String[3],artStatus={"not written","not written","not written"};
 static volatile String boundBy="nobody yet";
 private static final ArrayDeque<String> LOG=new ArrayDeque<>();private static String last;private static int repeat;
 private static int testIndex;private static Bitmap lastArt;

 static synchronized void log(String s){
  if(s.equals(last)){repeat++;LOG.pollLast();LOG.addLast(stamp()+s+"  ×"+(repeat+1));return;}
  last=s;repeat=0;LOG.addLast(stamp()+s);while(LOG.size()>300)LOG.pollFirst();
 }
 static synchronized String logText(){StringBuilder b=new StringBuilder();Iterator<String> it=LOG.descendingIterator();while(it.hasNext())b.append(it.next()).append('\n');return b.toString();}
 static synchronized void clearLog(){LOG.clear();last=null;}
 private static String stamp(){return new SimpleDateFormat("HH:mm:ss ",Locale.US).format(new Date());}
 static long position(){long p=position+(playing?SystemClock.elapsedRealtime()-positionAt:0);return Math.max(0,duration>0?Math.min(duration,p):p);}
 static String artPath(){String p=artPaths[artWay];if(p!=null)return p;for(String q:artPaths)if(q!=null)return q;return null;}

 /** Tells the engine something changed; it then calls the service again for the details. */
 static void notifyEngine(Context c,boolean art){
  c.sendBroadcast(new Intent(STATE));c.sendBroadcast(new Intent(META));
  if(art&&artPath()!=null)c.sendBroadcast(new Intent(THUMB).putExtra("thumb path",artPath()).putExtra("is online",false));
  log("→ broadcast "+(art?"state+meta+thumb":"state+meta"));
 }

 /** The cover goes to three places, because System UI has to open the file itself and may not be allowed into all of them. */
 static void writeArt(Context c,Bitmap b){
  if(b==null){for(int i=0;i<3;i++){artPaths[i]=null;artStatus[i]="no cover";}return;}
  File[] files=new File[3];
  files[0]=new File(c.getFilesDir(),"cover.png");
  File[] media=c.getExternalMediaDirs();files[1]=media.length>0&&media[0]!=null?new File(media[0],"cover.png"):null;
  files[2]=new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),"StudioMusicBridge/cover.png");
  for(int i=0;i<3;i++){
   File f=files[i];if(f==null){artPaths[i]=null;artStatus[i]="unavailable";continue;}
   try{
    File dir=f.getParentFile();if(dir!=null&&!dir.isDirectory()&&!dir.mkdirs())throw new IOException("cannot create "+dir);
    File tmp=new File(f.getPath()+".tmp");try(OutputStream o=new FileOutputStream(tmp)){b.compress(Bitmap.CompressFormat.PNG,100,o);}if(!tmp.renameTo(f))throw new IOException("rename failed");
    if(i==0){f.setReadable(true,false);for(File d=f.getParentFile();d!=null&&d.getPath().startsWith(c.getApplicationInfo().dataDir);d=d.getParentFile())d.setExecutable(true,false);}
    artPaths[i]=f.getAbsolutePath();artStatus[i]="written";
   }catch(Exception e){artPaths[i]=null;artStatus[i]="failed: "+e.getMessage();}
  }
 }

 static Bitmap testCover(int n){
  Bitmap b=Bitmap.createBitmap(512,512,Bitmap.Config.ARGB_8888);Canvas c=new Canvas(b);Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
  int[][] colours={{0xff7f5af0,0xff2cb67d},{0xffff8906,0xfff25f4c},{0xff3da9fc,0xff094067}};int[] k=colours[n%3];
  p.setShader(new LinearGradient(0,0,512,512,k[0],k[1],Shader.TileMode.CLAMP));c.drawRect(0,0,512,512,p);p.setShader(null);
  p.setColor(0xffffffff);p.setTextAlign(Paint.Align.CENTER);p.setTextSize(150);p.setFakeBoldText(true);c.drawText("♪",256,250,p);p.setTextSize(64);c.drawText("TEST "+(n+1),256,380,p);
  return b;
 }
 static void testTrack(Context c,int n){
  testIndex=n;live=false;hasTrack=true;title="Bridge test song "+(n+1);artist="Lockscreen Studio";album="Music Bridge";id=100+n;duration=200000+n*15000;position=0;positionAt=SystemClock.elapsedRealtime();
  Bitmap b=testCover(n);writeArt(c,b);b.recycle();log("test track "+(n+1)+" ready");notifyEngine(c,true);
 }
 static void control(Context c,String cmd){
  log("control: "+cmd);
  MediaController m=MediaListener.controller;
  if(live&&m!=null){MediaController.TransportControls t=m.getTransportControls();switch(cmd){case "play":t.play();break;case "pause":case "stop":t.pause();break;case "next":t.skipToNext();break;case "previous":t.skipToPrevious();break;}return;}
  switch(cmd){
   case "play":position=position();positionAt=SystemClock.elapsedRealtime();playing=true;break;
   case "pause":case "stop":position=position();positionAt=SystemClock.elapsedRealtime();playing=false;break;
   case "next":testTrack(c,testIndex+1);return;
   case "previous":testTrack(c,Math.max(0,testIndex-1));return;
  }
  new Handler(Looper.getMainLooper()).post(()->notifyEngine(c,false));
 }

 /** Copies the active player's details; called from the notification listener. */
 static void fromController(Context c,MediaController m){
  if(m==null){hasTrack=false;playing=false;log("no active player");notifyEngine(c,false);return;}
  MediaMetadata md=m.getMetadata();PlaybackState st=m.getPlaybackState();
  String t=md==null?null:md.getString(MediaMetadata.METADATA_KEY_TITLE),a=md==null?null:first(md.getString(MediaMetadata.METADATA_KEY_ARTIST),md.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST));
  boolean changed=!Objects.equals(t,title)||!Objects.equals(a,artist);
  live=true;hasTrack=t!=null;title=t==null?"":t;artist=a==null?"":a;album=md==null?"":first(md.getString(MediaMetadata.METADATA_KEY_ALBUM),"");
  duration=md==null?0:Math.max(0,md.getLong(MediaMetadata.METADATA_KEY_DURATION));
  playing=st!=null&&st.getState()==PlaybackState.STATE_PLAYING;position=st==null?0:Math.max(0,st.getPosition());positionAt=st==null?SystemClock.elapsedRealtime():st.getLastPositionUpdateTime();
  Bitmap art=md==null?null:md.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART);if(art==null&&md!=null)art=md.getBitmap(MediaMetadata.METADATA_KEY_ART);if(art==null&&md!=null)art=md.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON);
  boolean newArt=art!=null&&(lastArt==null||!art.sameAs(lastArt));
  if(changed)id++;
  if(newArt||changed&&art==null){writeArt(c,art);lastArt=art==null?null:art.copy(Bitmap.Config.ARGB_8888,false);}
  log("player "+m.getPackageName()+": "+(playing?"playing ":"paused ")+title+" — "+artist);
  notifyEngine(c,newArt||changed);
 }
 private static String first(String a,String b){return a!=null&&!a.isEmpty()?a:b;}
}
