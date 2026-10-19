import np.evan.lockscreen.*;
import java.io.*;
import java.util.*;
import java.util.zip.*;
import javax.xml.parsers.*;
import org.w3c.dom.*;
public class ApplyCheck {
 static class Phone implements EngineSelection.Commands {
  Map<String,String> settings=new HashMap<>();boolean denyEngine=false,ignoreEngine=false,denyAll=false;int resets=0,packageWrites=0,classWrites=0;
  Phone(){settings.put(EngineSelection.PKG_KEY,EngineSelection.DEFAULT);settings.put(EngineSelection.CLASS_KEY,"owner.existing.EngineClass");}
  public String run(String... a)throws Exception{
   if(a.length>2&&a[1].equals("--user")){String[] normalized=new String[a.length-2];normalized[0]=a[0];System.arraycopy(a,3,normalized,1,a.length-3);a=normalized;}
   if(a[1].equals("get")){
    if(a[3].equals(EngineSelection.PKG_KEY)&&resets>0){resets--;settings.put(a[3],EngineSelection.DEFAULT);}
    return settings.get(a[3]);
   }
   if(denyAll||(denyEngine&&a[3].equals(EngineSelection.PKG_KEY)&&a[4].equals(EngineSelection.ENGINE)))throw new IOException("Permission denied");
   if(a[3].equals(EngineSelection.PKG_KEY))packageWrites++;else classWrites++;
   if(!(ignoreEngine&&a[3].equals(EngineSelection.PKG_KEY)&&a[4].equals(EngineSelection.ENGINE)))settings.put(a[3],a[4]);return "";
  }
 }
 static void check(boolean v,String message){if(!v)throw new AssertionError(message);}
 static Map<String,byte[]> entries(byte[] bytes)throws Exception{
  Map<String,byte[]> map=new HashMap<>();try(ZipInputStream z=new ZipInputStream(new ByteArrayInputStream(bytes))){ZipEntry e;while((e=z.getNextEntry())!=null){ByteArrayOutputStream o=new ByteArrayOutputStream();byte[] c=new byte[1024];int n;while((n=z.read(c))!=-1)o.write(c,0,n);map.put(e.getName(),o.toByteArray());}}return map;
 }
 static void expectFailure(EngineSelection s)throws Exception{boolean failed=false;try{s.activate();}catch(IOException expected){failed=true;}check(failed,"Activation must report failure");}
 public static void main(String[] args)throws Exception{
  Phone p=new Phone();EngineSelection s=new EngineSelection(p,millis->{});s.probe();int before=p.packageWrites;s.activate();
  check(EngineSelection.ENGINE.equals(p.settings.get(EngineSelection.PKG_KEY)),"Engine package");
  check(p.packageWrites==before+1&&p.classWrites==1,"Activation sets the verified engine package/class pair");
  check(EngineSelection.ENGINE_CLASS.equals(p.settings.get(EngineSelection.CLASS_KEY)),"Activation repairs the class reset by Theme Store");
  p.resets=2;before=p.packageWrites;s.activate();check(p.packageWrites==before+3,"Retry observed transient resets");
  p.resets=100;before=p.packageWrites;expectFailure(s);check(p.packageWrites==before+3,"Retries must be bounded");p.resets=0;
  s.restore();check(EngineSelection.DEFAULT_CLASS.equals(p.settings.get(EngineSelection.CLASS_KEY)),"Default restore");
  p.denyEngine=true;expectFailure(s);check(EngineSelection.DEFAULT.equals(p.settings.get(EngineSelection.PKG_KEY)),"Denied single write leaves default unchanged");
  p.denyEngine=false;p.ignoreEngine=true;expectFailure(s);p.ignoreEngine=false;
  p.denyAll=true;boolean denied=false;try{s.probe();}catch(IOException expected){denied=true;}check(denied,"Probe must surface denied writes");
  Phone direct=new Phone();List<Long> pauses=new ArrayList<>();
  new EngineSelection(direct,pauses::add).activate();
  check(direct.packageWrites==1&&pauses.equals(Arrays.asList(750L,750L)),"Backup activation must run immediately, with only verification waits");
  byte[] wall={1,2,3},lock={4,5,6};Map<String,byte[]> entries=new HashMap<>();
  try(ZipInputStream z=new ZipInputStream(new ByteArrayInputStream(ThemeBuilder.packageTheme(wall,lock)))){
   ZipEntry e;while((e=z.getNextEntry())!=null){ByteArrayOutputStream b=new ByteArrayOutputStream();byte[] chunk=new byte[128];int n;while((n=z.read(chunk))!=-1)b.write(chunk,0,n);entries.put(e.getName(),b.toByteArray());}
  }
  Document meta=DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new ByteArrayInputStream(entries.get("themeInfo.xml")));
  NodeList modules=meta.getElementsByTagName("package");Map<String,byte[]> staged=new HashMap<>();
  // ThemeApplyService stages exact names from package metadata, not directory prefixes.
  for(int i=0;i<modules.getLength();i++){String name=((Element)modules.item(i)).getAttribute("name");check(entries.containsKey(name),"Module skipped by Theme Store: "+name);staged.put(name,entries.get(name));}
  check(Arrays.equals(lock,staged.get("lock/lockstyle")),"Native engine bundle missing after staging");
  check(Arrays.equals(wall,staged.get("wallpaper")),"Wallpaper missing after staging");
  Map<String,byte[]> iconModules=entries(IconThemeBuilder.packageIcons(new byte[]{9,8},new byte[]{7,6}));
  Document iconInfo=DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new ByteArrayInputStream(iconModules.get("themeInfo.xml")));
  NodeList iconNames=iconInfo.getElementsByTagName("package");
  for(int i=0;i<iconNames.getLength();i++)check(iconModules.containsKey(((Element)iconNames.item(i)).getAttribute("name")),"Missing exact icon module");
  check(!iconModules.containsKey("lock/lockstyle")&&iconModules.containsKey("icons")&&iconModules.containsKey("com.oppo.launcher"),"Icon export must not replace the clock bundle");
  System.out.println("Validated lock and independent icon module staging, engine selection, default restore, immediate backup activation, preserved class setting, bounded reset retries and permission errors.");
 }
}
