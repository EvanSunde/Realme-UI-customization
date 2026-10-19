package np.evan.lockscreen;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.LongSupplier;

/** Reversible native AOD settings. The OS owns media updates and rendering. */
public final class AodLab {
 public interface Phone {
  int currentUser() throws Exception;
  String read(int user,String namespace,String key) throws Exception;
  void write(int user,String namespace,String key,String value) throws Exception;
  boolean supports(String template) throws Exception;
 }
 public interface Task {void cancel();}
 public interface Timer {Task after(Runnable action,long millis);}
 public static final String TEMPLATE="aod/aod_layout_0010000.xml";
 public static final String[][] STYLES={
  {"vertical","Vertical digital",TEMPLATE},
  {"horizontal","Horizontal digital","aod/aod_layout_0020000.xml"},
  {"left","Left aligned digital","aod/aod_layout_0020006.xml"},
  {"analog","Analog dial","aod/aod_layout_0030001.xml"}
 };
 public static final long DURATION=60000;
 private static final String MAGIC="EvanAodBackup2",OLD_MAGIC="EvanAodBackup1";
 private static final String[][] KEYS={
  {"secure","Setting_AodClockModeFolder"},
  {"system","aod_smart_display_music_info_enabled"},
  {"secure","Setting_AodSceneMusicSwitchEnable"},
  {"secure","Setting_AodSceneInfoSwitchEnable"}
 };
 private final Phone phone;private final File backup;private final Timer timer;private final LongSupplier clock;
 private int user;private String[] originals,applied;private long until,generation;private Task task;private boolean enabled;
 private String style="vertical",note="Ready to inspect native AOD.",report="No scan yet.";
 public AodLab(Phone phone,File backup){this(phone,backup,productionTimer(),System::currentTimeMillis);}
 public AodLab(Phone phone,File backup,Timer timer,LongSupplier clock){this.phone=phone;this.backup=backup;this.timer=timer;this.clock=clock;}
 private static Timer productionTimer(){ScheduledExecutorService executor=Executors.newSingleThreadScheduledExecutor(r->{Thread t=new Thread(r,"aod-test-restore");t.setDaemon(true);return t;});return (r,ms)->{ScheduledFuture<?> f=executor.schedule(r,ms,TimeUnit.MILLISECONDS);return ()->f.cancel(false);};}
 /** Only interrupted tests roll back. Explicit enable survives helper restarts. */
 public synchronized void recover(){if(!backup.exists())return;try{load();if(enabled)note="Saved native AOD music mode loaded. Read settings to check whether Android has changed it.";else{restore();note="Recovered an interrupted AOD test. "+note;}}catch(Exception e){note="AOD recovery needs attention: "+e.getMessage();}}
 public synchronized boolean pending(){return backup.exists()||originals!=null;}
 public synchronized boolean enabled(){return enabled;}
 public synchronized long until(){return until;}
 public synchronized String style(){return style;}
 public synchronized boolean smart(){return applied!=null&&applied.length>3&&"1".equals(applied[3]);}
 public synchronized String note(){return note;}
 public synchronized String cachedReport(){return report;}
 public synchronized boolean matches()throws Exception{if(applied==null||phone.currentUser()!=user)return false;for(int i=0;i<applied.length;i++)if(!Objects.equals(applied[i],read(i)))return false;return true;}
 public synchronized boolean available(String id)throws Exception{String path=template(id);return path==null||phone.supports(path);}
 private static String template(String id)throws IOException{if("current".equals(id))return null;for(String[] s:STYLES)if(s[0].equals(id))return s[2];throw new IOException("Unknown built-in AOD style");}
 private String read(int i)throws Exception{return phone.read(user,KEYS[i][0],KEYS[i][1]);}
 private void write(int i,String value)throws Exception{phone.write(user,KEYS[i][0],KEYS[i][1],value);if(!Objects.equals(value,read(i)))throw new IOException("AOD setting did not stick: "+KEYS[i][1]);}
 private void cancel(){generation++;if(task!=null){task.cancel();task=null;}until=0;}
 public synchronized String inspect()throws Exception{
  int id=phone.currentUser();StringBuilder b=new StringBuilder("Native AOD · Android user ").append(id).append("\nRead at ").append(new Date(clock.getAsLong())).append('\n');
  String[][] keys={{"secure","Setting_AodSwitchEnable"},{"secure","Setting_is_aod_light_os"},{"secure","aod_media_support"},{"secure","Setting_AodClockMode"},{"secure","Setting_AodClockModeFolder"},{"secure","Setting_AodClockModeFolderRamless"},{"secure","Setting_AodEnableClockOnly"},{"secure","Setting_AodSceneMusicSwitchEnable"},{"secure","Setting_AodSceneInfoSwitchEnable"},{"system","aod_smart_display_music_info_enabled"},{"system","aod_smart_display_cur_state"}};
  for(String[] k:keys)b.append(k[0]).append('/').append(k[1]).append(" = ").append(Objects.toString(phone.read(id,k[0],k[1]),"<unset>")).append('\n');
  boolean match=matches();b.append("\nStudio mode: ").append(enabled?"enabled":pending()?"temporary test / recovery pending":"off").append("\nSaved settings match: ").append(match).append("\nSelected style: ").append(style).append('\n');
  b.append("\nNative title/artist updates do not require a theme reapply or a running Studio app.\nSmart-update cards require an OEM-supported app/service. Google Maps directions, artwork and playback controls have not been validated.\nNo custom AOD files are imported or deleted.");
  report=b.toString();if(enabled)note=match?"Native AOD music is enabled. No automatic restore is scheduled. Lock the phone to check the display.":"Saved AOD settings differ from this Android user's current settings. Studio has not overwritten them. Enable again to apply your choices, or restore to remove Studio's remaining changes.";return report;
 }
 public synchronized void start()throws Exception{start("vertical",false,false);}
 public synchronized void start(String id,boolean permanent,boolean smartUpdates)throws Exception{
  if(pending()&&(!enabled||!permanent))throw new IOException(enabled?"Restore the enabled mode before running another temporary test.":"Keep or restore the unfinished AOD test first.");
  if(pending()&&originals==null)throw new IOException("Restore the unreadable AOD backup before enabling again.");
  int currentUser=phone.currentUser();if(pending()&&user!=currentUser)throw new IOException("Restore AOD in the original Android user first.");
  if(!"1".equals(phone.read(currentUser,"secure","Setting_AodSwitchEnable")))throw new IOException("Enable Always-On Display in Android settings first.");
  if("1".equals(phone.read(currentUser,"secure","Setting_is_aod_light_os")))throw new IOException("This mode requires full native AOD; Light OS is selected.");
  String path=template(id);if(path!=null&&!phone.supports(path))throw new IOException("The selected built-in AOD style with song info was not found on this firmware.");
  int previousUser=user;String[] oldOriginals=originals,oldApplied=applied;boolean oldEnabled=enabled;String oldStyle=style;
  user=currentUser;String[] now=new String[KEYS.length];for(int i=0;i<now.length;i++)now[i]=read(i);
  originals=oldOriginals==null?now.clone():Arrays.copyOf(oldOriginals,KEYS.length);
  if(oldOriginals!=null)for(int i=0;i<now.length;i++)if(i>=oldApplied.length||!Objects.equals(now[i],oldApplied[i]))originals[i]=now[i];
  applied=new String[]{path==null?now[0]:path,"1","0",smartUpdates?"1":originals[3]};enabled=false;style=id;
  try{save();}catch(Exception e){user=previousUser;originals=oldOriginals;applied=oldApplied;enabled=oldEnabled;style=oldStyle;throw e;}
  try{
   for(int i=0;i<applied.length;i++)if(!Objects.equals(now[i],applied[i]))write(i,applied[i]);
   if(permanent){enabled=true;try{save();}catch(Exception e){enabled=false;throw e;}cancel();note="Native AOD music enabled. It stays selected without a timer; no theme reapply is needed for song changes. Lock the phone to check it.";}
   else{until=clock.getAsLong()+DURATION;final long token=++generation;task=timer.after(()->{synchronized(AodLab.this){if(token!=generation)return;try{restore();}catch(Exception ignored){}}},DURATION);note="Test selected for 60 seconds. Lock the phone, play music and check a track change. Tap Keep enabled if it works; otherwise the previous settings return.";}
  }catch(Exception e){String failure=e.getMessage();try{restore();}catch(Exception restoreError){failure+="; restore needs attention: "+restoreError.getMessage();}throw new IOException(failure,e);}
 }
 public synchronized void keep()throws Exception{
  if(enabled){note="Native AOD music is already enabled.";return;}
  if(!pending()||until==0||clock.getAsLong()>=until){if(pending())restore();throw new IOException("The AOD test has ended. Enable native AOD music to apply it again.");}
  if(!matches())throw new IOException("AOD settings changed during the test. Restore first, then enable your preferred style.");
  enabled=true;try{save();}catch(Exception e){enabled=false;throw e;}cancel();note="Native AOD music kept enabled. The 60-second restore is cancelled. Android handles subsequent song updates.";
 }
 public synchronized void restore()throws Exception{
  cancel();enabled=false;
  if(originals==null){if(!backup.exists()){note="No Studio AOD changes to restore.";return;}load();enabled=false;}
  List<String> failures=new ArrayList<>(),external=new ArrayList<>();
  for(int i=0;i<originals.length;i++)try{
   String current=read(i);if(Objects.equals(current,originals[i]))continue;
   if(!Objects.equals(current,applied[i])){external.add(KEYS[i][1]);continue;}
   write(i,originals[i]);
  }catch(Exception e){failures.add(KEYS[i][1]+": "+e.getMessage());}
  if(!failures.isEmpty()){try{save();}catch(Exception ignored){}note="AOD restore incomplete. Retry Restore previous AOD. "+String.join("; ",failures);throw new IOException(note);}
  if(backup.exists()&&!backup.delete()){note="Settings restored; the AOD backup could not be cleared. Retry Restore previous AOD.";throw new IOException(note);}
  originals=null;applied=null;style="vertical";note="Previous AOD settings restored. Lock the phone again."+(external.isEmpty()?"":" Kept settings changed outside Studio: "+String.join(", ",external));
 }
 private static void value(DataOutputStream out,String value)throws Exception{out.writeBoolean(value!=null);if(value!=null){if(value.length()>4096)throw new IOException("AOD setting is too long to back up");out.writeUTF(value);}}
 private static String value(DataInputStream in)throws Exception{String s=in.readBoolean()?in.readUTF():null;if(s!=null&&s.length()>4096)throw new IOException("Invalid AOD backup value");return s;}
 private void save()throws Exception{
  File parent=backup.getParentFile();if(parent!=null&&!parent.isDirectory()&&!parent.mkdirs())throw new IOException("Cannot create the AOD backup folder");
  File temporary=new File(backup.getPath()+".tmp");
  try(FileOutputStream bytes=new FileOutputStream(temporary);DataOutputStream out=new DataOutputStream(bytes)){
   out.writeUTF(MAGIC);out.writeInt(user);out.writeInt(originals.length);for(String s:originals)value(out,s);for(String s:applied)value(out,s);out.writeBoolean(enabled);out.writeUTF(style);out.flush();bytes.getFD().sync();
  }catch(Exception e){temporary.delete();throw e;}
  if(!temporary.renameTo(backup)){temporary.delete();throw new IOException("Cannot commit the AOD backup. No new settings were written.");}
 }
 private void load()throws Exception{
  if(backup.length()>131072)throw new IOException("Unexpected AOD backup size; no settings were changed.");
  try(DataInputStream in=new DataInputStream(new FileInputStream(backup))){
   String magic=in.readUTF();boolean legacy=OLD_MAGIC.equals(magic);if(!legacy&&!MAGIC.equals(magic))throw new IOException("Unrecognised AOD backup; no settings were changed.");
   int id=in.readInt(),count=in.readInt();if(id<0||id>100000||(count!=3&&count!=KEYS.length)||legacy&&count!=3)throw new IOException("Invalid AOD backup header");
   String[] values=new String[count],targets=new String[count];for(int i=0;i<count;i++)values[i]=value(in);
   boolean persistent=false;String selected="vertical";
   if(legacy){targets[0]=TEMPLATE;targets[1]="1";targets[2]="0";}
   else{for(int i=0;i<count;i++)targets[i]=value(in);persistent=in.readBoolean();selected=in.readUTF();String path=template(selected);if(path!=null&&!path.equals(targets[0]))throw new IOException("Invalid AOD backup style");if(!"1".equals(targets[1])||!"0".equals(targets[2])||count>3&&!Objects.equals(targets[3],values[3])&&!"1".equals(targets[3]))throw new IOException("Invalid AOD backup targets");}
   if(in.read()!=-1)throw new IOException("Unexpected AOD backup data");user=id;originals=values;applied=targets;enabled=persistent;style=selected;
  }
 }
}
