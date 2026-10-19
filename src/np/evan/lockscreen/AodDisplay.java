package np.evan.lockscreen;

import java.io.*;
import java.util.*;

/** Reversible AOD visibility settings that SystemUI reads directly: on/off, all day, scheduled or power saving. */
public final class AodDisplay {
 // Order matters: the backup stores values in this order.
 public static final String[] KEYS={"Setting_AodSwitchEnable","Setting_AodEnable","Setting_AodEnableImmediate","Setting_AodUserSetTime","Setting_AodUserEnergySavingSet","Setting_AodSetTimeBeginHour","Setting_AodSetTimeBeginMin","Setting_AodSetTimeEndHour","Setting_AodSetTimeEndMin"};
 private static final int SWITCH=0,ENABLE=1,ALWAYS=2,TIMED=3,SAVING=4,BEGIN_H=5,BEGIN_M=6,END_H=7,END_M=8;
 public static final List<String> MODES=Arrays.asList("allday","scheduled","saving");
 private static final String MAGIC="EvanAodDisplay1";
 private final AodLab.Phone phone;private final File backup;private String note="Read the phone to see the current Always-On Display mode.";
 public AodDisplay(AodLab.Phone phone,File backup){this.phone=phone;this.backup=backup;}
 public static final class State {public boolean on,backup;public String mode="default",style;public int beginHour,beginMin,endHour,endMin;}
 public synchronized String note(){return note;}
 private static int number(String value,int fallback){try{return value==null?fallback:Integer.parseInt(value.trim());}catch(NumberFormatException e){return fallback;}}
 private String[] values(int user)throws Exception{String[] v=new String[KEYS.length];for(int i=0;i<v.length;i++)v[i]=phone.read(user,"secure",KEYS[i]);return v;}
 public synchronized State read()throws Exception{
  int user=phone.currentUser();String[] v=values(user);State s=new State();s.on="1".equals(v[SWITCH]);s.backup=backup.isFile();s.style=phone.read(user,"secure","Setting_AodClockModeFolder");
  // The AOD app keeps the three modes mutually exclusive; all-day wins, as in its own settings screen.
  s.mode="1".equals(v[ALWAYS])?"allday":"1".equals(v[TIMED])?"scheduled":number(v[SAVING],0)>0?"saving":"default";
  s.beginHour=number(v[BEGIN_H],6);s.beginMin=number(v[BEGIN_M],0);s.endHour=number(v[END_H],0);s.endMin=number(v[END_M],0);return s;
 }
 public synchronized String apply(boolean on,String mode,int beginHour,int beginMin,int endHour,int endMin)throws Exception{
  if(!MODES.contains(mode))throw new IOException("Choose all day, scheduled or power saving");
  if(beginHour<0||beginHour>23||endHour<0||endHour>23||beginMin<0||beginMin>59||endMin<0||endMin>59)throw new IOException("Choose a valid schedule time");
  int user=phone.currentUser();String[] now=values(user);
  if("1".equals(phone.read(user,"secure","Setting_is_aod_light_os")))throw new IOException("Light OS AOD is selected; choose the full Always-On Display in Settings first.");
  String style=phone.read(user,"secure","Setting_AodClockModeFolder");
  if(on&&(style==null||style.trim().isEmpty()))throw new IOException("Turn Always-On Display on once in Settings so Android picks a style, then use Studio.");
  Backup b=backup.isFile()?load():null;if(b!=null&&b.user!=user)throw new IOException("Restore AOD display settings in the original Android user first.");
  if(b==null){b=new Backup();b.user=user;b.values=now.clone();save(b);}
  Map<Integer,String> target=new LinkedHashMap<>();
  if(!on)target.put(SWITCH,"0");
  else{
   target.put(SWITCH,"1");target.put(ENABLE,"1");
   // Power saving is 1 or 2 depending on an OEM burn-in feature; reuse the value the phone wrote itself.
   int saving=Math.max(number(now[SAVING],0),number(b.values[SAVING],0));
   target.put(ALWAYS,mode.equals("allday")?"1":"0");target.put(TIMED,mode.equals("scheduled")?"1":"0");target.put(SAVING,mode.equals("saving")?Integer.toString(saving>0?saving:1):"0");
   if(mode.equals("scheduled")){target.put(BEGIN_H,Integer.toString(beginHour));target.put(BEGIN_M,Integer.toString(beginMin));target.put(END_H,Integer.toString(endHour));target.put(END_M,Integer.toString(endMin));}
  }
  for(Map.Entry<Integer,String> e:target.entrySet())write(user,e.getKey(),e.getValue());
  note=!on?"Always-On Display turned off.":mode.equals("allday")?"Always-On Display shows all day.":mode.equals("scheduled")?String.format(Locale.US,"Always-On Display shows from %02d:%02d to %02d:%02d.",beginHour,beginMin,endHour,endMin):"Always-On Display uses power saving: it hides when the phone stays still.";
  return note;
 }
 public synchronized String restore()throws Exception{
  if(!backup.isFile()){note="No Studio AOD display changes to restore.";return note;}
  Backup b=load();if(phone.currentUser()!=b.user)throw new IOException("Restore AOD display settings in the original Android user.");
  for(int i=0;i<KEYS.length;i++)if(!Objects.equals(phone.read(b.user,"secure",KEYS[i]),b.values[i]))write(b.user,i,b.values[i]);
  if(!backup.delete())throw new IOException("Settings restored; the AOD display backup could not be cleared.");
  note="AOD display settings from before Studio's first change are restored.";return note;
 }
 private void write(int user,int key,String value)throws Exception{phone.write(user,"secure",KEYS[key],value);if(!Objects.equals(value,phone.read(user,"secure",KEYS[key])))throw new IOException("AOD setting did not stick: "+KEYS[key]);}
 private static final class Backup {int user;String[] values;}
 private void save(Backup b)throws Exception{
  ByteArrayOutputStream bytes=new ByteArrayOutputStream();try(DataOutputStream out=new DataOutputStream(bytes)){out.writeUTF(MAGIC);out.writeInt(b.user);out.writeInt(b.values.length);for(String v:b.values){out.writeBoolean(v!=null);if(v!=null)out.writeUTF(v);}}
  NativeThemeFiles.atomic(backup,bytes.toByteArray());
 }
 private Backup load()throws Exception{
  try(DataInputStream in=new DataInputStream(new ByteArrayInputStream(NativeThemeFiles.read(backup)))){
   if(!MAGIC.equals(in.readUTF()))throw new IOException("Unrecognised AOD display backup; no settings were changed.");
   Backup b=new Backup();b.user=in.readInt();int n=in.readInt();if(b.user<0||n!=KEYS.length)throw new IOException("Invalid AOD display backup");
   b.values=new String[n];for(int i=0;i<n;i++){String v=in.readBoolean()?in.readUTF():null;if(v!=null&&v.length()>64)throw new IOException("Invalid AOD display backup value");b.values[i]=v;}
   if(in.read()!=-1)throw new IOException("Unexpected AOD display backup data");return b;
  }
 }
}
