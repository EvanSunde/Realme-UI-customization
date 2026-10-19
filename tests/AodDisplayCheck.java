import np.evan.lockscreen.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;

public final class AodDisplayCheck {
 static void check(boolean v,String message){if(!v)throw new AssertionError(message);}
 interface Task {void run()throws Exception;}
 static void fails(Task t,String message)throws Exception{boolean failed=false;try{t.run();}catch(Exception e){failed=true;}check(failed,message);}
 static final class Phone implements AodLab.Phone {
  final Map<String,String> values=new HashMap<>();int user=0,writes=0;boolean ignore=false;
  public int currentUser(){return user;}
  public String read(int u,String space,String key){return values.get(u+"/"+space+"/"+key);}
  public void write(int u,String space,String key,String value){writes++;if(ignore)return;if(value==null)values.remove(u+"/"+space+"/"+key);else values.put(u+"/"+space+"/"+key,value);}
  public boolean supports(String template){return true;}
  String get(String key){return values.get(user+"/secure/"+key);}
  void put(String key,String value){values.put(user+"/secure/"+key,value);}
 }
 public static void main(String[] args)throws Exception{
  File dir=Files.createTempDirectory("aod-display-").toFile();File backup=new File(dir,"display.bin");
  try{
   Phone p=new Phone();p.put("Setting_AodSwitchEnable","1");p.put("Setting_AodEnable","1");p.put("Setting_AodUserEnergySavingSet","2");p.put("Setting_AodClockModeFolder","aod/aod_layout_0010000.xml");
   AodDisplay d=new AodDisplay(p,backup);AodDisplay.State s=d.read();check(s.on&&s.mode.equals("saving")&&!s.backup,"Read power saving mode");
   d.apply(true,"allday",6,0,0,0);check("1".equals(p.get("Setting_AodEnableImmediate"))&&"0".equals(p.get("Setting_AodUserSetTime"))&&"0".equals(p.get("Setting_AodUserEnergySavingSet")),"All day is exclusive");check(backup.isFile()&&d.read().mode.equals("allday"),"Backup before first write");
   d.apply(true,"scheduled",22,30,7,5);check("1".equals(p.get("Setting_AodUserSetTime"))&&"22".equals(p.get("Setting_AodSetTimeBeginHour"))&&"30".equals(p.get("Setting_AodSetTimeBeginMin"))&&"7".equals(p.get("Setting_AodSetTimeEndHour"))&&"5".equals(p.get("Setting_AodSetTimeEndMin"))&&"0".equals(p.get("Setting_AodEnableImmediate")),"Scheduled with times");
   d.apply(true,"saving",0,0,0,0);check("2".equals(p.get("Setting_AodUserEnergySavingSet")),"Power saving reuses the phone's own OEM value");
   d.apply(false,"saving",0,0,0,0);check("0".equals(p.get("Setting_AodSwitchEnable"))&&!d.read().on,"Off only clears the switch");check("2".equals(p.get("Setting_AodUserEnergySavingSet")),"Off keeps the chosen mode");
   for(int[] bad:new int[][]{{24,0,0,0},{0,60,0,0},{0,0,-1,0}}){int writes=p.writes;fails(()->d.apply(true,"scheduled",bad[0],bad[1],bad[2],bad[3]),"Reject invalid time");check(p.writes==writes,"Invalid input never writes");}
   fails(()->d.apply(true,"sometimes",0,0,0,0),"Reject unknown mode");
   p.ignore=true;fails(()->d.apply(true,"allday",0,0,0,0),"Ignored writes are reported");p.ignore=false;
   p.user=10;fails(()->d.apply(true,"allday",0,0,0,0),"Other user is rejected while a backup exists");fails(d::restore,"Restore only in the original user");p.user=0;
   d.restore();check(!backup.exists()&&"1".equals(p.get("Setting_AodSwitchEnable"))&&p.get("Setting_AodEnableImmediate")==null&&p.get("Setting_AodUserSetTime")==null&&"2".equals(p.get("Setting_AodUserEnergySavingSet"))&&p.get("Setting_AodSetTimeBeginHour")==null,"Restore exact originals including unset values");
   check(d.restore().contains("No Studio"),"Second restore is a no-op");
   Phone fresh=new Phone();AodDisplay f=new AodDisplay(fresh,new File(dir,"fresh.bin"));fails(()->f.apply(true,"allday",0,0,0,0),"Turning on without a native style needs Settings first");check(fresh.writes==0,"No writes without a style");
   fresh.put("Setting_AodClockModeFolder","aod/x.xml");fresh.put("Setting_is_aod_light_os","1");fails(()->f.apply(true,"allday",0,0,0,0),"Light OS is rejected");
   fresh.put("Setting_is_aod_light_os","0");f.apply(true,"saving",0,0,0,0);check("1".equals(fresh.get("Setting_AodUserEnergySavingSet"))&&"1".equals(fresh.get("Setting_AodEnable")),"Power saving defaults to 1 and turning on sets both switches");
   Files.write(new File(dir,"bad.bin").toPath(),"junk".getBytes("UTF-8"));AodDisplay bad=new AodDisplay(fresh,new File(dir,"bad.bin"));int writes=fresh.writes;fails(bad::restore,"Unreadable backup is not trusted");fails(()->bad.apply(true,"allday",0,0,0,0),"Unreadable backup blocks new changes");check(fresh.writes==writes,"No writes after an unreadable backup");
   System.out.println("Validated AOD display: mutually exclusive all-day/scheduled/power-saving modes, schedule times, OEM power-saving value reuse, off switch, input validation, ignored writes, user isolation, Light OS and missing-style guards, exact restore and unreadable backups.");
  }finally{for(File f:dir.listFiles())f.delete();dir.delete();}
 }
}
