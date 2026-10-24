package np.evan.musicbridge;

import android.Manifest;
import android.app.Activity;
import android.content.*;
import android.content.pm.*;
import android.graphics.Typeface;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
import java.util.List;

/** One screen: what System UI has asked for, which cover file to offer, and test controls. */
public class BridgeActivity extends Activity {
 private TextView status,log;private final Handler ui=new Handler(Looper.getMainLooper());
 private final Runnable tick=new Runnable(){public void run(){refresh();ui.postDelayed(this,1000);}};

 @Override protected void onCreate(Bundle b){
  super.onCreate(b);
  if(checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)!=PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.READ_EXTERNAL_STORAGE},1);
  int pad=(int)(16*getResources().getDisplayMetrics().density);
  LinearLayout col=new LinearLayout(this);col.setOrientation(LinearLayout.VERTICAL);col.setPadding(pad,pad,pad,pad);
  ScrollView scroll=new ScrollView(this);scroll.addView(col);setContentView(scroll);
  TextView h=new TextView(this);h.setText("Studio Music Bridge (test 0.2)");h.setTextSize(22);h.setTypeface(Typeface.DEFAULT_BOLD);col.addView(h);
  TextView how=new TextView(this);how.setText("1. In Lockscreen Studio 0.9.3: Style → Information → Music widget → on, then apply.\n2. Lock the phone: the widget should show the test song and its cover.\n3. On the lock screen, tap the widget's play and next buttons.\n4. Come back here and copy the log.");how.setPadding(0,pad/2,0,pad/2);col.addView(how);
  status=new TextView(this);status.setTypeface(Typeface.MONOSPACE);status.setTextSize(12);col.addView(status);
  col.addView(row(button("Test song",v->Bridge.testTrack(this,0)),button("Follow what's playing",v->follow())));
  col.addView(row(button("Play / pause",v->Bridge.control(this,Bridge.state(this).playing?"pause":"play","app")),button("Next song",v->Bridge.control(this,"next","app"))));
  col.addView(row(button("Send update",v->Bridge.publish(this,Bridge.state(this),true)),button("Reconnect now",v->Bridge.restartEngine(this))));
  Switch rc=new Switch(this);rc.setText("Reconnect after every change (makes System UI re-read the song)");rc.setChecked(Bridge.reconnect(this));rc.setOnCheckedChangeListener((x,on)->{Bridge.prefs(this).edit().putBoolean("reconnect",on).apply();Bridge.log(this,"reconnect after change: "+on);});col.addView(rc);
  TextView wl=new TextView(this);wl.setText("\nCover file offered to the lock screen:");col.addView(wl);
  RadioGroup ways=new RadioGroup(this);for(int i=0;i<Bridge.ART_WAYS.length;i++){RadioButton r=new RadioButton(this);r.setId(1000+i);r.setText(Bridge.ART_WAYS[i]);ways.addView(r);}
  ways.check(1000+Bridge.artWay(this));ways.setOnCheckedChangeListener((g,id)->{Bridge.prefs(this).edit().putInt("artWay",id-1000).apply();Bridge.log(this,"cover location: "+Bridge.ART_WAYS[id-1000]);Store.State s=Bridge.state(this);if(!s.live)Bridge.testTrack(this,0);else Bridge.fromController(this,MediaListener.controller);});col.addView(ways);
  col.addView(row(button("Copy log",v->copy()),button("Clear log",v->{Store.clearLog(this);refresh();})));
  log=new TextView(this);log.setTypeface(Typeface.MONOSPACE);log.setTextSize(11);log.setTextIsSelectable(true);log.setPadding(0,pad/2,0,0);col.addView(log);
  Bridge.seed(this);
 }
 @Override protected void onResume(){super.onResume();ui.post(tick);}
 @Override protected void onPause(){super.onPause();ui.removeCallbacks(tick);}
 @Override public void onRequestPermissionsResult(int c,String[] p,int[] r){if(!Bridge.state(this).live)Bridge.testTrack(this,0);}

 private void refresh(){
  List<ResolveInfo> found=getPackageManager().queryIntentServices(new Intent("com.oplus.music.service.command"),0);Store.State s=Bridge.state(this);
  StringBuilder b=new StringBuilder();
  b.append("Services answering the music action: ").append(found.size()).append(found.size()==1?" (good)":" (must be exactly 1)").append('\n');
  b.append("Last caller: ").append(getSharedPreferences("engine",MODE_MULTI_PROCESS).getString("caller","nobody yet")).append('\n');
  b.append("Mode: ").append(s.live?"following "+(MediaListener.controller==null?"(no player)":MediaListener.controller.getPackageName()):"test song").append('\n');
  b.append("Now: ").append(s.hasTrack?s.title+" — "+s.artist:"nothing").append(s.playing?"  ▶":"  ⏸").append('\n');
  b.append("Cover offered: ").append(s.art).append('\n');
  for(int i=0;i<3;i++)b.append(i==Bridge.artWay(this)?"* ":"  ").append(Bridge.ART_WAYS[i]).append(": ").append(Bridge.artStatus[i]).append('\n');
  if(s.live&&!listenerAllowed())b.append("Notification access is OFF — tap Follow what's playing to allow it.\n");
  status.setText(b);log.setText(Store.tail(this,300));
 }
 private boolean listenerAllowed(){String v=Settings.Secure.getString(getContentResolver(),"enabled_notification_listeners");return v!=null&&v.contains(getPackageName());}
 private void follow(){
  if(!listenerAllowed()){Toast.makeText(this,"Allow notification access for Studio Music Bridge, then tap Follow again",Toast.LENGTH_LONG).show();startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));return;}
  Store.State s=Bridge.state(this);s.live=true;Store.write(this,s);Bridge.fromController(this,MediaListener.controller);
 }
 private void copy(){((ClipboardManager)getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("bridge log",status.getText()+"\n"+Store.tail(this,300)));Toast.makeText(this,"Log copied",Toast.LENGTH_SHORT).show();}
 private Button button(String t,View.OnClickListener l){Button b=new Button(this);b.setText(t);b.setAllCaps(false);b.setOnClickListener(l);return b;}
 private LinearLayout row(View... views){LinearLayout r=new LinearLayout(this);for(View v:views)r.addView(v,new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1));return r;}
}
