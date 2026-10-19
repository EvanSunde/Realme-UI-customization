package np.evan.lockscreen;
import java.io.IOException;
/** Realme's native lock-screen selection; never changes device credentials. */
public final class EngineSelection {
 public interface Commands {String run(String... args)throws Exception;}
 public static final String PKG_KEY="oplus_customize_unlock_change_pkg",CLASS_KEY="oplus_customize_unlock_change_class";
 public static final String ENGINE="com.oplus.uiengine",ENGINE_CLASS=ENGINE+".ctrl.EngineKeyguardCtrl";
 public static final String DEFAULT="com.android.systemui",DEFAULT_CLASS=DEFAULT+".keyguard.KeyguardService";
 public interface Pause {void waitMillis(long millis)throws Exception;}
 private final Commands commands;
 private final Pause pause;
 public EngineSelection(Commands commands){this(commands,Thread::sleep);}
 public EngineSelection(Commands commands,Pause pause){this.commands=commands;this.pause=pause;}
 public void probe()throws Exception{
  String current=read(PKG_KEY);
  if(current.equals("null"))throw new IOException("No current lock-screen selection; apply a stock theme first");
  commands.run("/system/bin/settings","--user","0","put","system",PKG_KEY,current);
 }
 public void activate()throws Exception{
  // The later shade tests verified that the matching class is also required
  // after Theme Store selects the stock keyguard during a full native import.
  if(!ENGINE_CLASS.equals(read(CLASS_KEY)))commands.run("/system/bin/settings","--user","0","put","system",CLASS_KEY,ENGINE_CLASS);
  // Retry only during this foreground action if Theme Store briefly resets the selection.
  for(int attempt=0;attempt<3;attempt++){
   commands.run("/system/bin/settings","--user","0","put","system",PKG_KEY,ENGINE);
   pause.waitMillis(750);
   if(!ENGINE.equals(read(PKG_KEY)))continue;
   pause.waitMillis(750);
   if(ENGINE.equals(read(PKG_KEY))&&ENGINE_CLASS.equals(read(CLASS_KEY)))return;
  }
  throw new IOException("Theme Store is still resetting the clock selection. Wait for it to finish, then tap Activate clock now.");
 }
 public void restore()throws Exception{select(DEFAULT,DEFAULT_CLASS);}
 private void select(String pkg,String cls)throws Exception{
  commands.run("/system/bin/settings","--user","0","put","system",PKG_KEY,pkg);
  commands.run("/system/bin/settings","--user","0","put","system",CLASS_KEY,cls);
  if(!pkg.equals(read(PKG_KEY))||!cls.equals(read(CLASS_KEY)))
   throw new IOException("The phone did not retain the requested lock-screen selection");
 }
 private String read(String key)throws Exception{String v=commands.run("/system/bin/settings","--user","0","get","system",key).trim();if(!ShadeLab.engineValue(v))throw new IOException("Invalid lockscreen engine readback");return v;}
}
