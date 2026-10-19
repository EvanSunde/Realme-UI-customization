import np.evan.lockscreen.AodLab;
import java.io.*;
import java.nio.file.*;
import java.util.*;

public class AodCheck {
 static class Phone implements AodLab.Phone {
  final Map<String,String> values=new HashMap<>();File backup;int writes=0,current=0;String failKey=null;boolean denyRestore=false,ignore=false,unsupported=false;
  Phone(){values.put("0/secure/Setting_AodSwitchEnable","1");values.put("0/secure/Setting_is_aod_light_os","0");values.put("0/secure/Setting_AodClockModeFolder","original-clock");}
  String key(int user,String ns,String name){return user+"/"+ns+"/"+name;}
  public int currentUser(){return current;}
  public boolean supports(String path){return !unsupported;}
  public String read(int user,String ns,String name){return values.get(key(user,ns,name));}
  public void write(int user,String ns,String name,String value)throws Exception{
   check(backup.isFile(),"Backup must be committed before any setting write");writes++;
   if(name.equals(failKey)||denyRestore)throw new IOException("Denied");
   if(ignore)return;String k=key(user,ns,name);if(value==null)values.remove(k);else values.put(k,value);
  }
 }
 static class Timer implements AodLab.Timer {Runnable action;long delay;public AodLab.Task after(Runnable a,long ms){action=a;delay=ms;return ()->action=null;}void fire(){Runnable a=action;action=null;a.run();}}
 static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
 static void mustFail(AodLab lab)throws Exception{boolean failed=false;try{lab.start();}catch(IOException expected){failed=true;}check(failed,"Test must report failure");}
 static AodLab lab(Phone p,File f,Timer t){p.backup=f;return new AodLab(p,f,t,()->1000L);}
 public static void main(String[] args)throws Exception{
  File dir=Files.createTempDirectory("aod-check").toFile(),f=new File(dir,"backup.bin");Phone p=new Phone();Timer t=new Timer();AodLab a=lab(p,f,t);Map<String,String> original=new HashMap<>(p.values);
  a.inspect();check(p.writes==0,"Inspection must be read-only");
  a.start();check(a.pending()&&a.until()==61000&&t.delay==60000,"Arm native test rollback");
  check(AodLab.TEMPLATE.equals(p.read(0,"secure","Setting_AodClockModeFolder")),"Use the built-in template");
  check("1".equals(p.read(0,"system","aod_smart_display_music_info_enabled")),"Enable native legacy metadata");
  int before=p.writes;mustFail(a);check(p.writes==before,"Do not overwrite an active backup");
  t.fire();check(p.values.equals(original)&&!a.pending()&&!f.exists(),"Timed rollback restores exact values, including absent settings");
  a.start();Runnable stale=t.action;a.restore();a.start();stale.run();check(a.pending(),"A stale timer must not restore a later test");t.fire();
  a.start();p.values.put("0/secure/Setting_AodClockModeFolder","owner-new-look");a.restore();check("owner-new-look".equals(p.read(0,"secure","Setting_AodClockModeFolder")),"Preserve AOD choices changed outside the test");
  p=new Phone();t=new Timer();a=lab(p,f,t);a.start();p.current=10;
  AodLab restarted=lab(p,f,new Timer());restarted.recover();check(!restarted.pending()&&"original-clock".equals(p.read(0,"secure","Setting_AodClockModeFolder")),"Recover interrupted test for the original Android user");check(p.read(10,"secure","Setting_AodClockModeFolder")==null,"Never restore settings into another user");
  p=new Phone();a=lab(p,f,new Timer());p.failKey="aod_smart_display_music_info_enabled";mustFail(a);check(!a.pending()&&"original-clock".equals(p.read(0,"secure","Setting_AodClockModeFolder")),"Roll back a partial apply");
  p=new Phone();a=lab(p,f,new Timer());p.ignore=true;mustFail(a);check(!a.pending(),"Reject settings that fail readback verification");
  p=new Phone();a=lab(p,f,new Timer());a.start();p.denyRestore=true;boolean failure=false;try{a.restore();}catch(IOException expected){failure=true;}check(failure&&a.pending()&&f.isFile(),"Retain backup when restoration is denied");p.denyRestore=false;a.restore();check(!a.pending(),"Allow retry after a failed restore");
  p=new Phone();a=lab(p,f,new Timer());p.values.put("0/secure/Setting_AodSwitchEnable","0");mustFail(a);check(p.writes==0&&!f.exists(),"Do not enable AOD as a side effect");
  p=new Phone();a=lab(p,f,new Timer());p.values.put("0/secure/Setting_is_aod_light_os","1");mustFail(a);check(p.writes==0&&!f.exists(),"Reject the unsupported Light OS route");
  p=new Phone();t=new Timer();a=lab(p,f,t);original=new HashMap<>(p.values);a.start("vertical",true,true);check(a.enabled()&&a.until()==0&&t.action==null,"Permanent enable must not schedule a restore");check("1".equals(p.read(0,"secure","Setting_AodSceneInfoSwitchEnable")),"Native smart-update setting is enabled explicitly");
  restarted=lab(p,f,new Timer());before=p.writes;restarted.recover();check(restarted.enabled()&&restarted.matches()&&p.writes==before,"Permanent enable survives helper restart without rewriting settings");
  restarted.start("horizontal",true,false);check("aod/aod_layout_0020000.xml".equals(p.read(0,"secure","Setting_AodClockModeFolder")),"Switch to another native style");check(p.read(0,"secure","Setting_AodSceneInfoSwitchEnable")==null,"Unchecking smart updates restores the pre-Studio choice");restarted.restore();check(p.values.equals(original),"Multiple style changes retain the initial restore baseline");
  p=new Phone();t=new Timer();a=lab(p,f,t);a.start();stale=t.action;a.keep();check(a.enabled()&&t.action==null&&a.until()==0,"Keep cancels the temporary restore");stale.run();check(a.enabled(),"An already queued timer cannot undo Keep");a.restore();
  p=new Phone();a=lab(p,f,new Timer());before=p.writes;a.start("current",true,false);check("original-clock".equals(p.read(0,"secure","Setting_AodClockModeFolder")),"Current-style mode never replaces the existing clock/picture");check(p.writes-before==2,"Current-style mode writes only the two music keys");a.restore();
  p=new Phone();a=lab(p,f,new Timer());a.start("vertical",true,false);p.values.put("0/secure/Setting_AodClockModeFolder","owner-later-choice");before=p.writes;a.inspect();check(!a.matches()&&p.writes==before,"Inspect detects manual changes without reapplying");a.start("horizontal",true,false);a.restore();check("owner-later-choice".equals(p.read(0,"secure","Setting_AodClockModeFolder")),"An explicit re-enable retains intervening owner choices for restoration");
  p=new Phone();a=lab(p,f,new Timer());p.unsupported=true;mustFail(a);check(p.writes==0&&!f.exists(),"Refuse a missing native media template before writing settings");
  p=new Phone();a=lab(p,f,new Timer());boolean rejected=false;try{a.start("/sdcard/custom.xml",true,false);}catch(IOException e){rejected=true;}check(rejected&&p.writes==0,"Only known native style IDs are accepted");
  p=new Phone();a=lab(p,f,new Timer());a.start("vertical",true,false);p.current=10;before=p.writes;rejected=false;try{a.start("horizontal",true,false);}catch(IOException e){rejected=true;}check(rejected&&p.writes==before,"Do not adopt another Android user's backup");a.restore();
  p=new Phone();a=lab(p,f,new Timer());a.start();p.values.put("0/system/aod_smart_display_music_info_enabled","0");rejected=false;try{a.keep();}catch(IOException e){rejected=true;}check(rejected&&!a.enabled(),"Keep refuses settings changed during the trial");a.restore();
  p=new Phone();t=new Timer();p.backup=f;final long[] time={1000};a=new AodLab(p,f,t,()->time[0]);a.start();time[0]=62000;rejected=false;try{a.keep();}catch(IOException e){rejected=true;}check(rejected&&!a.pending(),"Late Keep restores instead of silently adopting an expired test");
  p=new Phone();a=lab(p,f,new Timer());a.start("vertical",true,false);p.denyRestore=true;try{a.restore();}catch(IOException e){}p.denyRestore=false;restarted=lab(p,f,new Timer());restarted.recover();check(!restarted.pending()&&"original-clock".equals(p.read(0,"secure","Setting_AodClockModeFolder")),"A failed persistent restore is retried as recovery, not re-enabled");
  p=new Phone();p.values.put("0/secure/Setting_AodClockModeFolder",AodLab.TEMPLATE);p.values.put("0/system/aod_smart_display_music_info_enabled","1");p.values.put("0/secure/Setting_AodSceneMusicSwitchEnable","0");
  try(DataOutputStream out=new DataOutputStream(new FileOutputStream(f))){out.writeUTF("EvanAodBackup1");out.writeInt(0);out.writeInt(3);out.writeBoolean(true);out.writeUTF("original-clock");out.writeBoolean(false);out.writeBoolean(false);}a=lab(p,f,new Timer());a.recover();check(!a.pending()&&"original-clock".equals(p.read(0,"secure","Setting_AodClockModeFolder"))&&p.read(0,"system","aod_smart_display_music_info_enabled")==null,"Migrate and restore an interrupted 0.5.1 test");
  try(FileOutputStream out=new FileOutputStream(f)){out.write(new byte[]{1,2,3});}p=new Phone();a=lab(p,f,new Timer());a.recover();mustFail(a);check(p.writes==0&&f.isFile(),"Fail closed on a corrupt backup");f.delete();dir.delete();
  System.out.println("Validated AOD inspection, backup-before-write, bounded test, exact timed restore, user isolation, manual-choice preservation, interrupted recovery, partial failures, readbacks, restore retry and unsupported-mode rejection, durable enable, Keep, native-style switching, current-style preservation, smart-update rollback, external-change rebasing and legacy backup migration.");
 }
}
