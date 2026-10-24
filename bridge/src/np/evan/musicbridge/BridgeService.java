package np.evan.musicbridge;

import android.app.*;
import android.content.Intent;
import android.os.*;
import java.util.Collections;

/** Answers Realme's lock-screen music widget the way the stock Oplus Music service would (IMediaPlaybackService). */
public class BridgeService extends Service {
 static final String DESC="com.oplus.music.service.IMediaPlaybackService";
 static final String[] NAMES={"","openPlaylistItem","openPlaylist","setPlaylistCurrentPosition","getPlaylistCurrentPosition","getPlaylistLength","getPlaylist","getPlaylistItem","getCurrentPlaylistItem","getCurrentTrack","getCurrentCueTrack","getTrack","removeTracks","addTracks","getCueInnerPosition","setCueInnerPosition","isPlaying","stop","pause","play","previous","next","seekTo","getDuration","getCurrentPosition","setPlayMode","getPlayMode","isBufferingBlocked","getBufferingPercent","shakeSwitch","getMediaMountedCount","getTrackThumbPath","getLyricPath","getArtistUnknownString","getAlbumUnknownString","setPlayerAutoSave","setPlayerVolume","isDlnaMode","isDlnaAutoMode","getDlnaDeviceID","getDlnaDeviceList","playOnDlnaAutoMode","playOnDlnaMode","playOnNormalMode","setDlnaPlayerVolume","bindDlnaPlayerService","isDlnaOperator","getPrivateMusicInterface"};

 @Override public void onCreate(){super.onCreate();foreground();Bridge.log("service created");}
 @Override public int onStartCommand(Intent i,int f,int id){foreground();Bridge.log("started by "+(i==null?"system":String.valueOf(i.getComponent()==null?i.getAction():"lock screen")));return START_NOT_STICKY;}
 @Override public IBinder onBind(Intent i){Bridge.log("bound ("+i.getAction()+")");return binder;}
 @Override public boolean onUnbind(Intent i){Bridge.log("unbound");return false;}

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
   try{data.enforceInterface(DESC);}catch(SecurityException e){Bridge.log("wrong interface for call "+code);return false;}
   String caller=getPackageManager().getNameForUid(Binder.getCallingUid());Bridge.boundBy=caller+" (uid "+Binder.getCallingUid()+")";
   Bridge.log("← "+NAMES[code]+" from "+caller);
   switch(code){
    case 4:case 14:case 26:case 28:case 30:case 39:reply.writeNoException();reply.writeInt(0);break;
    case 5:reply.writeNoException();reply.writeInt(Bridge.hasTrack?1:0);break;
    case 6:reply.writeNoException();reply.writeInt(0);break;
    case 7:data.readInt();reply.writeNoException();item(reply);break;
    case 8:reply.writeNoException();item(reply);break;
    case 9:reply.writeNoException();track(reply);break;
    case 10:reply.writeNoException();reply.writeInt(0);break;
    case 11:reply.writeNoException();track(reply);break;
    case 12:case 13:reply.writeNoException();reply.writeInt(0);break;
    case 16:reply.writeNoException();reply.writeInt(Bridge.playing?1:0);break;
    case 17:Bridge.control(getApplicationContext(),"stop");reply.writeNoException();break;
    case 18:Bridge.control(getApplicationContext(),"pause");reply.writeNoException();break;
    case 19:Bridge.control(getApplicationContext(),"play");reply.writeNoException();break;
    case 20:Bridge.control(getApplicationContext(),"previous");reply.writeNoException();break;
    case 21:Bridge.control(getApplicationContext(),"next");reply.writeNoException();break;
    case 22:{int to=data.readInt();Bridge.position=to;Bridge.positionAt=SystemClock.elapsedRealtime();reply.writeNoException();break;}
    case 23:reply.writeNoException();reply.writeLong(Bridge.duration);break;
    case 24:reply.writeNoException();reply.writeLong(Bridge.position());break;
    case 27:case 37:case 38:case 46:reply.writeNoException();reply.writeInt(0);break;
    case 31:{String p=Bridge.artPath();reply.writeNoException();reply.writeString(p);Bridge.log("   cover path → "+p);break;}
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
 private static void item(Parcel r){if(!Bridge.hasTrack){r.writeInt(0);return;}r.writeInt(1);r.writeInt(0);r.writeLong(Bridge.id);}
 /** Track, in the field order of com.oplus.music.media.Track.writeToParcel. */
 private static void track(Parcel r){
  if(!Bridge.hasTrack){r.writeInt(0);return;}
  r.writeInt(1);r.writeLong(Bridge.id);r.writeLong(0);r.writeLong(0);r.writeLong(0);r.writeLong(0);
  r.writeString(null);r.writeString(null);r.writeString(Bridge.artPath());r.writeString(Bridge.title);r.writeString(Bridge.album);r.writeString(Bridge.artist);
  r.writeString(null);r.writeString(null);r.writeString(null);r.writeLong(0);r.writeLong(0);
 }
}
