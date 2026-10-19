import np.evan.lockscreen.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import org.w3c.dom.*;

public final class ShadeCheck {
 static void check(boolean v,String message){if(!v)throw new AssertionError(message);}
 interface Task {void run()throws Exception;}
 static void fails(Task t,String message)throws Exception{boolean failed=false;try{t.run();}catch(Exception e){failed=true;}check(failed,message);}
 static byte[] utf(String s)throws Exception{return s.getBytes("UTF-8");}
 static class Phone implements ShadeLab.Phone {
  File root;String blur="3",pkg=EngineSelection.ENGINE,cls=EngineSelection.ENGINE_CLASS,badRead=null;boolean night=false,reject=false,stall=false;int imports=0,restarts=0,writes=0;
  Phone(File root){this.root=root;}
  public String run(String... a)throws Exception{
   String binary=new File(a[0]).getName();
   if(binary.equals("settings")){
    check(a[1].equals("--user")&&a[2].equals("0"),"Settings must target user 0");String mode=a[3],key=a[5];boolean p=key.equals(EngineSelection.PKG_KEY);
    if(mode.equals("get"))return badRead!=null?badRead:p?pkg:cls;
    writes++;if(p)pkg=mode.equals("delete")?"null":a[6];else cls=mode.equals("delete")?"null":a[6];return "";
   }
   if(binary.equals("getprop"))return blur;
   if(binary.equals("setprop")){blur=a[2];return "";}
   if(binary.equals("cmd"))return "Night mode: "+(night?"yes":"no");
   if(binary.equals("am")&&a[1].equals("get-current-user"))return "0";
   if(binary.equals("am")&&a[1].equals("crash")){restarts++;return "";}
   if(binary.equals("am")&&a[1].equals("startservice")){
    imports++;if(reject)throw new IOException("Rejected native service");if(stall)return "Starting service";
    int ix=Arrays.asList(a).indexOf("THEME_PATH");Map<String,byte[]> outer=NativeThemeFiles.unzip(NativeThemeFiles.read(new File(a[ix+1])));
    Document metadata=NativeThemeFiles.xml(outer.get("themeInfo.xml"));NodeList names=metadata.getElementsByTagName("package");Map<String,byte[]> staged=new TreeMap<>();
    for(int i=0;i<names.getLength();i++){String n=((Element)names.item(i)).getAttribute("name");check(outer.containsKey(n),"Exact staged path must exist");staged.put(n,outer.get(n));}
    for(File f:root.listFiles())remove(f);for(Map.Entry<String,byte[]> e:staged.entrySet())NativeThemeFiles.atomic(new File(root,e.getKey()),e.getValue());
    NativeThemeFiles.atomic(new File(root,"config"),utf("regenerated"));pkg=EngineSelection.DEFAULT;cls=EngineSelection.DEFAULT_CLASS;return "Starting service";
   }
   throw new IOException("Unexpected command "+Arrays.toString(a));
  }
  public void pause(long ignored){}
 }
 static void remove(File f)throws Exception{if(f.isDirectory())for(File child:f.listFiles())remove(child);Files.delete(f.toPath());}
 static File seed(File base)throws Exception{
  File root=new File(base,"theme");root.mkdirs();NativeThemeFiles.atomic(new File(root,"lock/lockstyle"),utf("current-clock"));NativeThemeFiles.atomic(new File(root,"wallpaper"),NativeThemeFiles.emptyZip());NativeThemeFiles.atomic(new File(root,"icons"),utf("owner-icons"));
  Map<String,byte[]> module=new LinkedHashMap<>();module.put("assets/colors.xml",utf("<resources><color name=\"owner_color\">#FF123456</color></resources>"));module.put("res/drawable/owner.png",new byte[]{1,2,3});NativeThemeFiles.atomic(new File(root,NativeThemeFiles.SYSTEMUI),NativeThemeFiles.zip(module));return root;
 }
 public static void main(String[] args)throws Exception{
  File dir=Files.createTempDirectory("shade-check-").toFile();try{
   File root=seed(dir),work=new File(dir,"backup");Phone p=new Phone(root);ShadeLab lab=new ShadeLab(p,root,work,new File(dir,"exports"));
   Map<String,byte[]> original=NativeThemeFiles.capture(root);ShadeOptions o=new ShadeOptions();o.panelAlpha=0;o.cardOpacity=25;o.changeBlur=true;o.blurMode=0;
   check(o.colors().get(ShadeOptions.PANEL).equals("#0080CBC4"),"Panel alpha 0 must reach the actual resource");check(o.colors().get(ShadeOptions.BASE).equals("#40FAFAFA"),"25% base alpha byte");o.panelAlpha=100;check(o.colors().get(ShadeOptions.PANEL).equals("#FF80CBC4"),"Full panel alpha");o.panelAlpha=50;check(o.colors().get(ShadeOptions.PANEL).equals("#8080CBC4"),"Intermediate panel alpha");
   lab.apply(o);check(p.imports==1&&p.restarts==1&&p.blur.equals("0"),"One apply, one restart, verified property");check(p.pkg.equals(EngineSelection.ENGINE)&&p.cls.equals(EngineSelection.ENGINE_CLASS),"Restore both engine values reset by importer");check(lab.hasBackup(),"Persistent backup captured");
   Map<String,byte[]> applied=NativeThemeFiles.capture(root);for(String key:original.keySet())if(!key.equals(NativeThemeFiles.SYSTEMUI))check(Arrays.equals(original.get(key),applied.get(key)),"Retain native resource "+key);
   Map<String,byte[]> contents=NativeThemeFiles.unzip(applied.get(NativeThemeFiles.SYSTEMUI));check(Arrays.equals(contents.get("res/drawable/owner.png"),new byte[]{1,2,3}),"Retain existing drawable payload");String colors=new String(contents.get("assets/colors.xml"),"UTF-8");check(colors.contains("owner_color")&&colors.contains("#8080CBC4"),"Merge existing colours and panel alpha");
   // Later edits to the clock must survive restoration of the shade backup.
   NativeThemeFiles.atomic(new File(root,"lock/lockstyle"),utf("later-clock"));new ShadeLab(p,root,work,new File(dir,"exports")).restore();check(p.blur.equals("3"),"Restore original blur value across service recreation");check(Arrays.equals(NativeThemeFiles.read(new File(root,NativeThemeFiles.SYSTEMUI)),original.get(NativeThemeFiles.SYSTEMUI)),"Restore exact original SystemUI module");check(new String(NativeThemeFiles.read(new File(root,"lock/lockstyle")),"UTF-8").equals("later-clock"),"Keep latest clock during shade restore");
   o.panelEnabled=false;o.cardsEnabled=false;lab.apply(o);colors=new String(NativeThemeFiles.unzip(NativeThemeFiles.read(new File(root,NativeThemeFiles.SYSTEMUI))).get("assets/colors.xml"),"UTF-8");check(colors.contains("owner_color")&&!colors.contains(ShadeOptions.PANEL)&&!colors.contains(ShadeOptions.BASE),"System colour toggles remove only the four managed resources");
   // Oplus ignores SystemUI theme colours at night, so applying stores them for light mode and says so.
   p.night=true;int imports=p.imports;check(lab.apply(o).contains("light mode")&&p.imports==imports+1,"Dark mode applies and explains when the colours appear");ShadeLab.Readback night=lab.readback();check(night.night&&night.text.contains(ShadeLab.DARK),"Readback reports paused shade colours in dark mode");p.night=false;
   File[] kept=new File(dir,"exports").listFiles();check(kept!=null&&kept.length==0,"Successful imports leave no full-theme copies in Downloads");
   o.panelEnabled=true;lab.apply(o);ShadeLab.Readback day=lab.readback();check(!day.night&&day.colors.get(ShadeOptions.PANEL).equals("#8080CBC4")&&day.colors.size()==1&&day.text.contains(ShadeOptions.PANEL),"One readback returns the report and managed colours");o.panelEnabled=false;
   // Blur alone skips the theme import entirely.
   imports=p.imports;int restarts=p.restarts;lab.blurOnly(2);check(p.blur.equals("2")&&p.imports==imports&&p.restarts==restarts+1,"Blur-only sets the property and restarts SystemUI without importing");fails(()->lab.blurOnly(4),"Reject unsupported blur mode");check(p.blur.equals("2"),"Rejected blur leaves the property");
   o.panelAlpha=101;imports=p.imports;fails(()->lab.apply(o),"Invalid alpha");check(p.imports==imports,"Invalid alpha never imports");o.panelAlpha=50;
   File other=Files.createTempDirectory(dir.toPath(),"failed-read").toFile(),otherRoot=seed(other);Phone bad=new Phone(otherRoot);ShadeLab guarded=new ShadeLab(bad,otherRoot,new File(other,"backup"),new File(other,"exports"));
   for(String read:new String[]{"cmd: Failure calling service settings: Failed transaction (2147483646)","com.oplus.uiengine\ncmd: Failure",""}){bad.badRead=read;fails(()->guarded.apply(new ShadeOptions()),"Reject invalid engine read");check(bad.imports==0&&!guarded.hasBackup()&&bad.writes==0,"Failed reads cannot become a backup or be written back");}
   bad.badRead=null;bad.reject=true;fails(()->guarded.apply(new ShadeOptions()),"Rejected service");check(bad.restarts==0&&bad.pkg.equals(EngineSelection.ENGINE)&&bad.cls.equals(EngineSelection.ENGINE_CLASS),"Denied import avoids manual restart and retains engine");
   bad.reject=false;bad.stall=true;fails(()->guarded.apply(new ShadeOptions()),"Stalled importer");check(bad.restarts==0,"Unconfirmed import avoids manual restart");
   // Ordinary clock/icon exports retain the currently installed shade and modules.
   File export=new File(dir,"new-clock.theme");Map<String,byte[]> incoming=new TreeMap<>();incoming.put("wallpaper",NativeThemeFiles.emptyZip());incoming.put("lock/lockstyle",utf("new-clock"));NativeThemeFiles.atomic(export,NativeThemeFiles.pack(incoming));NativeThemeFiles.preserveExport(export,root);Map<String,byte[]> combined=NativeThemeFiles.unzip(NativeThemeFiles.read(export));check(combined.containsKey("icons")&&combined.containsKey(NativeThemeFiles.SYSTEMUI),"Clock apply must carry forward shade and icon modules");check(new String(combined.get("lock/lockstyle"),"UTF-8").equals("new-clock"),"New clock wins over captured clock");
   fails(()->NativeThemeFiles.unzip(NativeThemeFiles.zip(Collections.singletonMap("../bad",new byte[0]))),"Reject unsafe paths");
   System.out.println("Validated shade ARGB extremes and intermediate values; native staging, clock/icon retention, merged assets, both engine settings, persistent backup/restore, later clock preservation, dark-mode messaging, export cleanup, single readback, blur-only route, invalid reads, rejected/stalled imports and no restart after unconfirmed changes.");
  }finally{remove(dir);}
 }
}
