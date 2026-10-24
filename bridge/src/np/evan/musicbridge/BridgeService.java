package np.evan.musicbridge;

import android.app.*;
import android.content.Intent;
import android.os.*;
import java.util.Collections;

/** Answers Realme's lock-screen music widget the way the stock Oplus Music service would (IMediaPlaybackService). */
public class BridgeService extends Service {
 static final String DESC="com.oplus.music.service.IMediaPlaybackService";
 static final String[] NAMES={"","openPlaylistItem","openPlaylist","setPlaylistCurrentPosition","getPlaylistCurrentPosition","getPlaylistLength","getPlaylist","getPlaylistItem","getCurrentPlaylistItem","getCurrentTrack","getCurrentCueTrack","getTrack","removeTracks","addTracks","getCueInnerPosition","setCueInnerPosition","isPlaying","stop","pause","play","previous","next","seekTo","getDuration","getCurrentPosition","setPlayMode","getPlayMode","isBufferingBlocked","getBufferingPercent","shakeSwitch","getMediaMountedCount","getTrackThumbPath","getLyricPath","getArtistUnknownString","getAlbumUnknownString","setPlayerAutoSave","setPlayerVolume","isDlnaMode","isDlnaAutoMode","getDlnaDeviceID","getDlnaDeviceList","playOnDlnaAutoMode","playOnDlnaMode","playOnNormalMode","setDlnaPlayerVolume","bindDlnaPlayerService","isDlnaOperator","getPrivateMusicInterface"};

 private Store.State s(){Store.State st=Store.read(this);return st==null?new Store.State():st;}
 private void log(String line){Store.log(this,line);}
 /** Polling calls repeat every second while playing; they are counted and summarised instead of logged one by one. */
 private final java.util.Map<String,Integer> polls=new java.util.TreeMap<>();private long pollsSince;
 private synchronized void poll(String name){polls.merge(name,1,Integer::sum);long now=SystemClock.elapsedRealtime();if(pollsSince==0)pollsSince=now;if(now-pollsSince>5000)flushPolls();}
 private synchronized void flushPolls(){if(polls.isEmpty())return;log("← polled "+polls);polls.clear();pollsSince=0;}

 @Override public void onCreate(){super.onCreate();foreground();Bridge.seed(this);log("lock-screen connection process started");}
 @Override public int onStartCommand(Intent i,int f,int id){foreground();return START_NOT_STICKY;}
 @Override public IBinder onBind(Intent i){log("System UI connected");return binder;}
 @Override public boolean onUnbind(Intent i){flushPolls();log("System UI disconnected");return false;}
 private void control(String cmd){sendBroadcast(new Intent(ControlReceiver.ACTION).setClass(this,ControlReceiver.class).putExtra("cmd",cmd));}

 /** The engine starts this with startForegroundService, so it must become a foreground service at once. */
 private void foreground(){
  NotificationManager nm=getSystemService(NotificationManager.class);
  nm.createNotificationChannel(new NotificationChannel("bridge","Music bridge",NotificationManager.IMPORTANCE_MIN));
  startForeground(7,new Notification.Builder(this,"bridge").setSmallIcon(android.R.drawable.ic_media_play).setContentTitle("Music bridge is running").setContentText("Feeds the lock-screen music widget").setOngoing(true).build());
 }

 private final Binder binder=new Binder(){
  @Override protected boolean onTransact(int code,Parcel data,Parcel reply,int flags)throws RemoteException{
   if(code==INTERFACE_TRANSACTION){reply.writeString(DESC);return true;}
   if(code<1||code>47)return super.onTransact(code,data,reply,flags);
   try{data.enforceInterface(DESC);}catch(SecurityException e){log("wrong interface for call "+code);return false;}
   Store.State st=s();
   if(code==16||code==23||code==24||code==47)poll(NAMES[code]);else{flushPolls();log("← "+NAMES[code]);}
   getSharedPreferences("engine",MODE_PRIVATE).edit().putString("caller",getPackageManager().getNameForUid(Binder.getCallingUid())).apply();
   switch(code){
    case 4:case 14:case 26:case 28:case 30:case 39:reply.writeNoException();reply.writeInt(0);break;
    case 5:reply.writeNoException();reply.writeInt(st.hasTrack?1:0);break;
    case 6:reply.writeNoException();reply.writeInt(0);break;
    case 7:data.readInt();reply.writeNoException();item(reply,st);break;
    case 8:reply.writeNoException();item(reply,st);break;
    case 9:reply.writeNoException();track(reply,st);break;
    case 10:reply.writeNoException();reply.writeInt(0);break;
    case 11:reply.writeNoException();track(reply,st);break;
    case 12:case 13:reply.writeNoException();reply.writeInt(0);break;
    case 16:reply.writeNoException();reply.writeInt(st.playing?1:0);break;
    case 17:control("stop");reply.writeNoException();break;
    case 18:control("pause");reply.writeNoException();break;
    case 19:control("play");reply.writeNoException();break;
    case 20:control("previous");reply.writeNoException();break;
    case 21:control("next");reply.writeNoException();break;
    case 22:{data.readInt();reply.writeNoException();break;}
    case 23:reply.writeNoException();reply.writeLong(st.duration);break;
    case 24:reply.writeNoException();reply.writeLong(Store.position(st));break;
    case 27:case 37:case 38:case 46:reply.writeNoException();reply.writeInt(0);break;
    case 31:{reply.writeNoException();reply.writeString(st.art);log("   cover path → "+st.art);break;}
    case 32:reply.writeNoException();reply.writeString(null);break;
    case 33:reply.writeNoException();reply.writeString("Unknown artist");break;
    case 34:reply.writeNoException();reply.writeString("Unknown album");break;
    case 40:reply.writeNoException();reply.writeStringList(Collections.<String>emptyList());break;
    case 47:reply.writeNoException();reply.writeStrongBinder(null);break;
    default:reply.writeNoException();
   }
   return true;
  }
 };
 /** PlaylistItem: type (0 = local file, so the cover is read as a plain image file) and id. */
 private static void item(Parcel r,Store.State s){if(!s.hasTrack){r.writeInt(0);return;}r.writeInt(1);r.writeInt(0);r.writeLong(s.id);}
 /** Track, in the field order of com.oplus.music.media.Track.writeToParcel. */
 private static void track(Parcel r,Store.State s){
  if(!s.hasTrack){r.writeInt(0);return;}
  r.writeInt(1);r.writeLong(s.id);r.writeLong(0);r.writeLong(0);r.writeLong(0);r.writeLong(0);
  r.writeString(null);r.writeString(null);r.writeString(s.art);r.writeString(s.title);r.writeString(s.album);r.writeString(s.artist);
  r.writeString(null);r.writeString(null);r.writeString(null);r.writeLong(0);r.writeLong(0);
 }
}
