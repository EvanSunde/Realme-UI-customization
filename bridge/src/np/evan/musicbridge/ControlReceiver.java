package np.evan.musicbridge;

import android.content.*;

/** Play/pause/next/previous pressed on the lock screen arrive in the :engine process and are handed to the app process here. */
public class ControlReceiver extends BroadcastReceiver {
 static final String ACTION="np.evan.musicbridge.CONTROL";
 @Override public void onReceive(Context c,Intent i){String cmd=i.getStringExtra("cmd");if(cmd!=null)Bridge.control(c.getApplicationContext(),cmd,"lock screen");}
}
