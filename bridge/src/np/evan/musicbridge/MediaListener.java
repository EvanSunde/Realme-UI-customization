package np.evan.musicbridge;

import android.content.ComponentName;
import android.media.MediaMetadata;
import android.media.session.*;
import android.service.notification.NotificationListenerService;
import java.util.*;

/** Notification access lets the bridge see the phone's active media sessions (Spotify, YouTube Music, …). */
public class MediaListener extends NotificationListenerService {
 static volatile MediaController controller;
 private MediaSessionManager sessions;
 private final MediaController.Callback callback=new MediaController.Callback(){
  @Override public void onMetadataChanged(MediaMetadata m){if(Bridge.live)Bridge.fromController(getApplicationContext(),controller);}
  @Override public void onPlaybackStateChanged(PlaybackState s){if(Bridge.live)Bridge.fromController(getApplicationContext(),controller);}
  @Override public void onSessionDestroyed(){pick(sessions.getActiveSessions(component()));}
 };
 private final MediaSessionManager.OnActiveSessionsChangedListener changed=this::pick;
 private ComponentName component(){return new ComponentName(this,MediaListener.class);}
 @Override public void onListenerConnected(){
  sessions=getSystemService(MediaSessionManager.class);
  try{sessions.addOnActiveSessionsChangedListener(changed,component());pick(sessions.getActiveSessions(component()));Bridge.log("notification access connected");}catch(SecurityException e){Bridge.log("no media access: "+e.getMessage());}
 }
 @Override public void onListenerDisconnected(){if(sessions!=null)sessions.removeOnActiveSessionsChangedListener(changed);if(controller!=null)controller.unregisterCallback(callback);controller=null;}
 /** Prefers a player that is playing; otherwise the most recent session. */
 private void pick(List<MediaController> list){
  MediaController best=null;if(list!=null)for(MediaController m:list){PlaybackState s=m.getPlaybackState();if(s!=null&&s.getState()==PlaybackState.STATE_PLAYING){best=m;break;}if(best==null)best=m;}
  if(controller!=null&&best!=null&&controller.getSessionToken().equals(best.getSessionToken()))return;
  if(controller!=null)controller.unregisterCallback(callback);controller=best;if(best!=null)best.registerCallback(callback);
  if(Bridge.live)Bridge.fromController(getApplicationContext(),best);
 }
}
