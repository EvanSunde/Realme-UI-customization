package np.evan.lockscreen;
import android.os.Binder;
import android.os.Parcel;
import android.os.RemoteException;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;
import java.util.zip.ZipFile;
/** Runs under Shizuku's shell identity. Accepts only this editor's exports. */
public final class ShellService extends Binder {
 public static final String DESCRIPTOR="np.evan.lockscreen.ThemeShell";
 private final EngineSelection selection=new EngineSelection(this::command);
 private final ShadeLab shade=new ShadeLab(new ShadeLab.Phone(){public String run(String... args)throws Exception{return command(args);}public void pause(long millis)throws Exception{Thread.sleep(millis);}},new File("/data/theme"),new File("/data/local/tmp/evan-lockscreen-shade"),new File("/storage/emulated/0/Download/EvanLockscreen"));
 private final AodLab aod=new AodLab(new AodLab.Phone(){
  public int currentUser()throws Exception{return Integer.parseInt(command("/system/bin/am","get-current-user"));}
  public String read(int user,String space,String key)throws Exception{String value=command("/system/bin/settings","--user",Integer.toString(user),"get",space,key);return value.equals("null")?null:value;}
  public void write(int user,String space,String key,String value)throws Exception{if(value==null)command("/system/bin/settings","--user",Integer.toString(user),"delete",space,key);else command("/system/bin/settings","--user",Integer.toString(user),"put",space,key,value);}
  public boolean supports(String template)throws Exception{return nativeAodTemplates().contains(template);}
 },new File("/data/local/tmp/evan-lockscreen-aod/backup.bin"));
 private final ApplyRecovery recovery=new ApplyRecovery();private String lastThemePath=null,awaitingPath=null;
 public ShellService(){attachInterface(null,DESCRIPTOR);aod.recover();}
 @Override protected boolean onTransact(int code,Parcel data,Parcel reply,int flags)throws RemoteException{
  if(code==INTERFACE_TRANSACTION){reply.writeString(DESCRIPTOR);return true;}
  if(code==16777115){System.exit(0);return true;}
  if(code<1||code>17)return super.onTransact(code,data,reply,flags);
  data.enforceInterface(DESCRIPTOR);String result;
  try{
   if(code>=15){result=shadeAction(code,data);}
   else if(code>=9){result=aodAction(code,data);}
   else if(code==1){String path=data.readString();boolean safe=data.readInt()!=0;if(recovery.pending())throw new IOException("Keep or revert the previous theme first");String previous=lastThemePath;result=apply(path,false);if(!result.contains("unavailable")){if(safe){awaitingPath=path;long until=recovery.arm(()->{if(previous!=null){try{String restored=apply(previous,false);if(restored.contains("unavailable"))throw new IOException("Rollback completion is unknown");lastThemePath=previous;}catch(Exception e){selection.restore();lastThemePath=null;}}else selection.restore();});result="CONFIRM:"+until+":Keep this lock screen? Otherwise the last Studio theme, or default keyguard, is restored.";}else lastThemePath=path;}}
   else if(code==5){if(recovery.pending())throw new IOException("Keep or revert the clock before applying icons");result=apply(data.readString(),true);}
   else if(code==6){if(recovery.keep()){lastThemePath=awaitingPath;result="Lock screen kept.";}else result="The confirmation period has ended. "+recovery.status();}
   else if(code==7){recovery.revert();result=recovery.status().equals("restored")?"Previous Studio theme, or the default lock screen, restored.":"RECOVERY:"+recovery.status();}
   else if(code==8){result="RECOVERY:"+recovery.status();}
   else if(code==2){if(recovery.pending())recovery.keep();selection.restore();result="Default lock screen selected. Turn the screen off and back on. Wallpaper is unchanged.";}
   else if(code==4){result=diagnostics();}
   else{selection.activate();result="Theme engine selected immediately. Turn the screen off and back on to check your clock.";}
  }catch(Exception e){result="ERROR: "+e.getMessage();}
  reply.writeNoException();reply.writeString(result);return true;
 }
 private String shadeAction(int code,Parcel data)throws Exception{
  boolean error=false;String note=shade.note(),report="";
  try{if(code!=15&&recovery.pending())throw new IOException("Keep or revert the clock theme before changing the shade");if(code==16)note=shade.apply(ShadeJson.parse(new org.json.JSONObject(data.readString())));else if(code==17)note=shade.restore();}
  catch(Exception e){error=true;note=e.getMessage();}
  ShadeLab.Readback r=null;try{r=shade.readback();report=r.text;}catch(Exception e){report="Readback unavailable: "+e.getMessage();if(!error){error=true;note=report;}}
  org.json.JSONObject colors=new org.json.JSONObject();String blur="";boolean night=false;if(r!=null){for(Map.Entry<String,String> c:r.colors.entrySet())colors.put(c.getKey(),c.getValue());blur=r.blur;night=r.night;}
  return "SHADE:"+new org.json.JSONObject().put("error",error).put("note",note).put("report",report).put("backup",shade.hasBackup()).put("blur",blur).put("night",night).put("colors",colors).toString();
 }
 private Set<String> aodTemplates;
 private synchronized Set<String> nativeAodTemplates()throws Exception{
  if(aodTemplates!=null)return aodTemplates;
  Set<String> found=new HashSet<>();String paths=command("/system/bin/pm","path","--user",command("/system/bin/am","get-current-user"),"com.oplus.aod");
  for(String line:paths.split("\n"))if(line.startsWith("package:"))try(ZipFile apk=new ZipFile(line.substring(8).trim())){
   for(String[] style:AodLab.STYLES){java.util.zip.ZipEntry entry=apk.getEntry("assets/"+style[2]);if(entry==null||entry.getSize()>65536)continue;
    try(InputStream in=apk.getInputStream(entry);ByteArrayOutputStream bytes=new ByteArrayOutputStream()){byte[] block=new byte[4096];int n;while((n=in.read(block))!=-1){bytes.write(block,0,n);if(bytes.size()>65536)throw new IOException("Unexpected native AOD asset size");}String xml=bytes.toString("UTF-8");if(xml.contains("<AodMediaView")&&xml.contains("supportVisiableMedia=\"true\""))found.add(style[2]);}
   }
  }
  aodTemplates=found;return found;
 }
 private String aodAction(int code,Parcel data)throws Exception{
  boolean error=false;String failure="";
  try{if(code==10||code==13){if(recovery.pending())throw new IOException("Keep or revert the lock-screen theme first");org.json.JSONObject request=new org.json.JSONObject(data.readString());aod.start(request.optString("style","vertical"),code==13,request.optBoolean("smart",false));}else if(code==11)aod.restore();else if(code==14)aod.keep();aod.inspect();}
  catch(Exception e){error=true;failure=e.getMessage();}
  org.json.JSONArray styles=new org.json.JSONArray();String catalogError="";try{for(String[] s:AodLab.STYLES)if(aod.available(s[0]))styles.put(new org.json.JSONObject().put("id",s[0]).put("name",s[1]).put("tested",s[0].equals("vertical")));}catch(Exception e){catalogError="Built-in style scan unavailable. Keep current style is still available.";}
  boolean matches=false;try{matches=aod.matches();}catch(Exception e){error=true;failure="Could not check current AOD settings: "+e.getMessage();}
  return "AOD:"+new org.json.JSONObject().put("active",aod.pending()).put("enabled",aod.enabled()).put("matches",matches).put("style",aod.style()).put("smart",aod.smart()).put("styles",styles).put("catalogNote",catalogError).put("until",aod.until()).put("note",error?failure:aod.note()).put("report",aod.cachedReport()).put("error",error).toString();
 }
 private String apply(String path,boolean iconsOnly)throws Exception{
  if(path==null)throw new IOException("No exported file");
  File file=new File(path).getCanonicalFile();
  String root=new File("/storage/emulated/0/Download/EvanLockscreen").getCanonicalPath()+"/";
  if(!file.getPath().startsWith(root)||!file.getName().matches((iconsOnly?"Evan-Icons-":"Evan-Lockscreen-")+"[0-9]+\\.theme"))throw new IOException("Only this editor's Downloads/EvanLockscreen exports are allowed");
  if(!file.isFile())throw new IOException("Export is missing: "+file);
  try(ZipFile zip=new ZipFile(file)){
   if(zip.getEntry("themeInfo.xml")==null||zip.getEntry("wallpaper")==null||zip.getEntry(iconsOnly?"icons":"lock/lockstyle")==null)throw new IOException("Incomplete native theme package");
  }
  // A full native import clears unstaged modules. Preserve the existing shade,
  // clock (for icon-only applies), and other current native theme resources.
  NativeThemeFiles.preserveExport(file,new File("/data/theme"));
  if(!iconsOnly){
   String services=command("/system/bin/cmd","package","query-services","--components","-a","oplus.intent.action.keyguard","-p",EngineSelection.ENGINE);
   if(!services.contains("EngineKeyguardCtrl"))throw new IOException("The tested Realme lock-screen engine was not found");
   selection.probe();
  }
  // am returning means accepted, not applied. Wait for Theme Store's asynchronous callback.
  final String[] previousEngine=iconsOnly?shade.captureEngine():null;
  CompletableFuture<Boolean> completion=new CompletableFuture<>();Process logs=null;
  try{
   String since=new SimpleDateFormat("MM-dd HH:mm:ss.SSS",Locale.US).format(new Date());
   logs=new ProcessBuilder("/system/bin/logcat","-v","brief","-T",since,"ApplyColorTheme:I","*:S").redirectErrorStream(true).start();
   final Process stream=logs;
   Thread watcher=new Thread(()->{
    try(BufferedReader reader=new BufferedReader(new InputStreamReader(stream.getInputStream(),"UTF-8"))){
     String line;while((line=reader.readLine())!=null){
      if(line.contains("applyThemeByPathInThread, apply success!")){completion.complete(true);return;}
      if(line.contains("apply fail")||line.contains("applyColorThemeByPathInThread -- Throwable")){completion.complete(false);return;}
     }
    }catch(IOException ignored){}
   },"theme-completion");watcher.setDaemon(true);watcher.start();
   command("/system/bin/am","startservice","-n","com.oplus.themestore/com.nearme.themespace.services.ThemeApplyService","--es","THEME_PATH",file.getPath(),"--ez","IS_FROM_911","true");
   boolean applied;
   try{applied=completion.get(60,TimeUnit.SECONDS);}
   catch(TimeoutException e){
    // A timeout is not an apply acknowledgement. Do not select an incomplete clock theme.
    return "Theme Store's completion log was unavailable. Check the result on your phone; if the new clock was installed, tap Activate clock now. No automatic activation ran.";
   }
   if(!applied)throw new IOException("Theme Store reported an apply failure. No clock activation ran.");
   if(iconsOnly)return "Theme Store finished applying icons. Check the launcher; if the clock reverted to default, tap Activate clock now.";
   selection.activate();
   return "Theme Store finished; the custom clock was selected automatically. Turn the screen off and back on to check your design.";
  }finally{if(logs!=null)logs.destroy();if(previousEngine!=null)shade.restoreEngine(previousEngine);}
 }
 private String diagnostics()throws Exception{
  StringBuilder out=new StringBuilder("Selected lock-screen package: ").append(command("/system/bin/settings","get","system",EngineSelection.PKG_KEY)).append("\nSelected class: ").append(command("/system/bin/settings","get","system",EngineSelection.CLASS_KEY));
  out.append("\n\nKeyguard services:\n").append(command("/system/bin/cmd","package","query-services","--components","-a","oplus.intent.action.keyguard"));
  String packages=command("/system/bin/pm","list","packages");out.append("\n\nPossible AI / photo packages (not confirmed APIs):\n");int count=0;
  for(String line:packages.split("\n")){String lower=line.toLowerCase(Locale.US);if(lower.contains("gallery")||lower.contains("photos")||lower.contains("segment")||lower.contains("vision")||lower.contains("intelligence")||lower.contains(".ai")||lower.contains(".gms")||lower.contains("google.android.as")){out.append(line).append('\n');if(++count>=40)break;}}
  return out.toString();
 }
 private String command(String... args)throws Exception{
  Process p=new ProcessBuilder(args).redirectErrorStream(true).start();ByteArrayOutputStream output=new ByteArrayOutputStream();
  Thread reader=new Thread(()->{
   try(InputStream in=p.getInputStream()){
    byte[] b=new byte[2048];int n;while((n=in.read(b))!=-1)if(output.size()<32768)output.write(b,0,Math.min(n,32768-output.size()));
   }catch(IOException ignored){}
  });reader.setDaemon(true);reader.start();
  if(!p.waitFor(15,TimeUnit.SECONDS)){p.destroy();throw new IOException("Phone command timed out");}
  reader.join(2000);String text=output.toString("UTF-8").trim(),lower=text.toLowerCase(Locale.US);
  if(p.exitValue()!=0||lower.startsWith("error")||lower.startsWith("exception")||lower.startsWith("cmd: failure")||lower.contains("failure calling service")||lower.contains("\nerror:")||lower.contains("securityexception")||lower.contains("permission denial")||lower.contains("permission denied"))throw new IOException(text.isEmpty()?"Phone command failed":text);
  return text;
 }
}
