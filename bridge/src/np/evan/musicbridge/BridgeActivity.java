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

/** One screen: what the engine has asked for, which cover file to offer, and test controls. */
public class BridgeActivity extends Activity {
 private TextView status,log;private RadioGroup ways;private final Handler ui=new Handler(Looper.getMainLooper());
 private final Runnable tick=new Runnable(){public void run(){refresh();ui.postDelayed(this,1000);}};

 @Override protected void onCreate(Bundle b){
  super.onCreate(b);
  if(checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)!=PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.READ_EXTERNAL_STORAGE},1);
  int pad=(int)(16*getResources().getDisplayMetrics().density);
  LinearLayout col=new LinearLayout(this);col.setOrientation(LinearLayout.VERTICAL);col.setPadding(pad,pad,pad,pad);
  ScrollView scroll=new ScrollView(this);scroll.addView(col);setContentView(scroll);
  TextView h=new TextView(this);h.setText("Studio Music Bridge (test)");h.setTextSize(22);h.setTypeface(Typeface.DEFAULT_BOLD);col.addView(h);
  TextView how=new TextView(this);how.setText("1. In Lockscreen Studio, turn on Extras → Music widget and apply.\n2. Lock the phone. The widget should show the test song and cover.\n3. Then try the buttons below and the widget's own buttons. The log shows every call the lock screen makes.");how.setPadding(0,pad/2,0,pad/2);col.addView(how);
  status=new TextView(this);status.setTypeface(Typeface.MONOSPACE);status.setTextSize(12);col.addView(status);

  col.addView(row(button("Test song",v->Bridge.testTrack(this,0)),button("Follow what's playing",v->follow())));
  col.addView(row(button("Play / pause",v->Bridge.control(this,Bridge.playing?"pause":"play")),button("Next song",v->Bridge.control(this,"next"))));
  col.addView(row(button("Send update",v->Bridge.notifyEngine(this,true)),button("Restart bridge",v->restart())));
  TextView wl=new TextView(this);wl.setText("\nCover file offered to the lock screen:");col.addView(wl);
  ways=new RadioGroup(this);for(int i=0;i<Bridge.ART_WAYS.length;i++){RadioButton r=new RadioButton(this);r.setId(1000+i);r.setText(Bridge.ART_WAYS[i]);ways.addView(r);}
  ways.check(1000+Bridge.artWay);ways.setOnCheckedChangeListener((g,id)->{Bridge.artWay=id-1000;Bridge.log("cover location: "+Bridge.ART_WAYS[Bridge.artWay]);Bridge.notifyEngine(this,true);});col.addView(ways);
  col.addView(row(button("Copy log",v->copy()),button("Clear log",v->{Bridge.clearLog();refresh();})));
  log=new TextView(this);log.setTypeface(Typeface.MONOSPACE);log.setTextSize(11);log.setTextIsSelectable(true);log.setPadding(0,pad/2,0,0);col.addView(log);
  if(Bridge.artPath()==null&&!Bridge.live)Bridge.testTrack(this,0);
 }
 @Override protected void onResume(){super.onResume();ui.post(tick);}
 @Override protected void onPause(){super.onPause();ui.removeCallbacks(tick);}
 @Override public void onRequestPermissionsResult(int c,String[] p,int[] r){if(!Bridge.live)Bridge.testTrack(this,0);}

 private void refresh(){
  List<ResolveInfo> found=getPackageManager().queryIntentServices(new Intent("com.oplus.music.service.command"),0);
  StringBuilder s=new StringBuilder();
  s.append("Services answering the music action: ").append(found.size()).append(found.size()==1?" (good)":" (must be exactly 1)").append('\n');
  for(ResolveInfo r:found)s.append("  ").append(r.serviceInfo.packageName).append('\n');
  s.append("Lock screen caller: ").append(Bridge.boundBy).append('\n');
  s.append("Mode: ").append(Bridge.live?"following "+(MediaListener.controller==null?"(no player)":MediaListener.controller.getPackageName()):"test song").append('\n');
  s.append("Now: ").append(Bridge.hasTrack?Bridge.title+" — "+Bridge.artist:"nothing").append(Bridge.playing?"  ▶":"  ⏸").append('\n');
  for(int i=0;i<3;i++)s.append(i==Bridge.artWay?"* ":"  ").append(Bridge.ART_WAYS[i]).append(": ").append(Bridge.artStatus[i]).append('\n');
  if(Bridge.live&&!listenerAllowed())s.append("Notification access is OFF — tap Follow what's playing to allow it.\n");
  status.setText(s);log.setText(Bridge.logText());
 }
 private boolean listenerAllowed(){String v=Settings.Secure.getString(getContentResolver(),"enabled_notification_listeners");return v!=null&&v.contains(getPackageName());}
 private void follow(){
  Bridge.live=true;
  if(!listenerAllowed()){Toast.makeText(this,"Allow notification access for Studio Music Bridge",Toast.LENGTH_LONG).show();startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));return;}
  Bridge.fromController(this,MediaListener.controller);
 }
 /** Kills this process so the lock screen loses its connection and binds again on its next call. */
 private void restart(){Bridge.log("restarting bridge…");ui.postDelayed(()->android.os.Process.killProcess(android.os.Process.myPid()),300);}
 private void copy(){((ClipboardManager)getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("bridge log",status.getText()+"\n"+Bridge.logText()));Toast.makeText(this,"Log copied",Toast.LENGTH_SHORT).show();}
 private Button button(String t,View.OnClickListener l){Button b=new Button(this);b.setText(t);b.setAllCaps(false);b.setOnClickListener(l);return b;}
 private LinearLayout row(View... views){LinearLayout r=new LinearLayout(this);for(View v:views)r.addView(v,new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1));return r;}
}
