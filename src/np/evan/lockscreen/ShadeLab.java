package np.evan.lockscreen;
import java.io.*;
import java.util.*;
/** Native theme transaction under the Shizuku shell identity; no hooks or overlays. */
public final class ShadeLab {
 public interface Phone {String run(String... args)throws Exception;void pause(long millis)throws Exception;default String themeStore()throws Exception{return "com.oplus.themestore";}}
 private final Phone phone;private final File root,work,exports;private String note="Read the shade state, then apply your choices.";
 public ShadeLab(Phone phone,File root,File work,File exports){this.phone=phone;this.root=root;this.work=work;this.exports=exports;}
 private String cmd(String... a)throws Exception{return phone.run(a);}
 public static boolean engineValue(String value){return value!=null&&(value.equals("null")||value.matches("[A-Za-z_][A-Za-z0-9_$]*(\\.[A-Za-z_][A-Za-z0-9_$]*)+"));}
 private String setting(String key)throws Exception{String v=cmd("/system/bin/settings","--user","0","get","system",key).trim();if(!engineValue(v))throw new IOException("Invalid engine readback for "+key+". No error text was saved.");return v;}
 private String[] engine()throws Exception{return new String[]{setting(EngineSelection.PKG_KEY),setting(EngineSelection.CLASS_KEY)};}
 public synchronized String[] captureEngine()throws Exception{return engine();}
 public synchronized void restoreEngine(String[] pair)throws Exception{engine(pair);}
 private void engine(String[] pair)throws Exception{for(int i=1;i>=0;i--){String k=i==0?EngineSelection.PKG_KEY:EngineSelection.CLASS_KEY,v=pair[i];if(!engineValue(v))throw new IOException("Invalid saved engine selection");if(v.equals("null"))cmd("/system/bin/settings","--user","0","delete","system",k);else cmd("/system/bin/settings","--user","0","put","system",k,v);}if(!Arrays.equals(pair,engine()))throw new IOException("The engine selection did not retain both values");}
 public String blur()throws Exception{return cmd("/system/bin/getprop","debug.systemui.gaussblur_level").trim();}
 private static boolean property(String value){return value!=null&&(value.isEmpty()||value.matches("-?[0-9]{1,6}"));}
 private void blur(String value)throws Exception{if(!property(value))throw new IOException("Invalid blur backup");cmd("/system/bin/setprop","debug.systemui.gaussblur_level",value);if(!blur().equals(value))throw new IOException("Blur property readback differs");}
 private File backup(){return new File(work,"before-studio.bin");}
 public boolean hasBackup(){return backup().isFile();}
 public String note(){return note;}
 private static byte[] backupBytes(byte[] module,String prop)throws Exception{ByteArrayOutputStream b=new ByteArrayOutputStream();try(DataOutputStream out=new DataOutputStream(b)){out.writeInt(0x53484431);out.writeUTF(prop);out.writeInt(module==null?-1:module.length);if(module!=null)out.write(module);}return b.toByteArray();}
 private static final class Before {byte[] module;String prop;}
 private Before before()throws Exception{Before b=new Before();try(DataInputStream in=new DataInputStream(new ByteArrayInputStream(NativeThemeFiles.read(backup())))){if(in.readInt()!=0x53484431)throw new IOException("Unrecognized shade backup");b.prop=in.readUTF();if(!property(b.prop))throw new IOException("Invalid blur backup");int n=in.readInt();if(n < -1||n>NativeThemeFiles.LIMIT)throw new IOException("Invalid module backup size");if(n>=0){b.module=new byte[n];in.readFully(b.module);}if(in.read()!=-1)throw new IOException("Invalid backup trailing data");}return b;}
 public synchronized Map<String,byte[]> current()throws Exception{user();return NativeThemeFiles.capture(root);}
 private void user()throws Exception{if(!cmd("/system/bin/am","get-current-user").trim().equals("0"))throw new IOException("Shade controls are tested only for Android user 0");}
 public synchronized String apply(ShadeOptions o)throws Exception{
  o.validate();user();boolean dark=night();
  Map<String,byte[]> current=NativeThemeFiles.capture(root);String[] selected=engine();String originalBlur=blur();if(!property(originalBlur))throw new IOException("Unrecognized current blur property");
  if(!hasBackup())NativeThemeFiles.atomic(backup(),backupBytes(current.get(NativeThemeFiles.SYSTEMUI),originalBlur));else before();
  byte[] module=NativeThemeFiles.module(current.get(NativeThemeFiles.SYSTEMUI),o.colors());Map<String,byte[]> target=new TreeMap<>(current);if(module==null)target.remove(NativeThemeFiles.SYSTEMUI);else target.put(NativeThemeFiles.SYSTEMUI,module);
  return install(target,selected,o.changeBlur?Integer.toString(o.blurMode):originalBlur,dark?"Applied. Dark mode is on, so the colours appear when you switch to light mode. "+DARK:"Applied shade choices. Pull down the panel to check the phone.");
 }
 /** Blur is a SystemUI property, not a theme resource: it changes without a theme import and also works in dark mode. */
 public synchronized String blurOnly(int mode)throws Exception{
  if(mode < -1||mode>3)throw new IOException("Choose a supported blur mode");user();String original=blur();if(!property(original))throw new IOException("Unrecognized current blur property");
  if(!hasBackup())NativeThemeFiles.atomic(backup(),backupBytes(NativeThemeFiles.capture(root).get(NativeThemeFiles.SYSTEMUI),original));
  blur(Integer.toString(mode));cmd("/system/bin/am","crash","--user","0","com.android.systemui");note="Blur updated. Debug properties reset after a reboot; apply blur again after restarting the phone.";return note;
 }
 public synchronized String restore()throws Exception{user();Before b=before();Map<String,byte[]> current=NativeThemeFiles.capture(root);if(b.module==null)current.remove(NativeThemeFiles.SYSTEMUI);else current.put(NativeThemeFiles.SYSTEMUI,b.module);return install(current,engine(),b.prop,"Restored the SystemUI module and blur value captured before the first Studio shade apply. Current clock and other theme resources were retained.");}
 private String install(Map<String,byte[]> target,String[] selected,String prop,String success)throws Exception{
  if(!target.containsKey("wallpaper"))target.put("wallpaper",NativeThemeFiles.emptyZip());
  // Each export is a full copy of the current theme; keep at most the one being imported.
  File[] old=exports.listFiles((d,n)->n.matches("Lockscreen-Shade-[0-9]+\\.theme"));if(old!=null)for(File f:old)f.delete();
  File theme=new File(exports,"Lockscreen-Shade-"+System.currentTimeMillis()+".theme");NativeThemeFiles.atomic(theme,NativeThemeFiles.pack(target));
  // A complete recovery pair is committed before Theme Store can change the selection.
  NativeThemeFiles.atomic(new File(work,"last-engine.txt"),(selected[0]+"\n"+selected[1]+"\n").getBytes("UTF-8"));
  boolean requested=false;Exception failure=null;
  try{
   requested=true;String response=cmd("/system/bin/am","startservice","-n",phone.themeStore()+"/com.nearme.themespace.services.ThemeApplyService","--es","THEME_PATH",theme.getPath(),"--ez","IS_FROM_911","true");String lower=response.toLowerCase(Locale.US);if(lower.contains("error")||lower.contains("exception")||lower.contains("denial")||lower.contains("failure"))throw new IOException(response);
   // Poll cheap file sizes; read and compare the bytes only once the sizes have settled.
   Map<String,Long> want=NativeThemeFiles.sizes(target);int stable=0;boolean verified=false;for(int i=0;i<45&&!verified;i++){boolean sized=false;try{sized=want.equals(NativeThemeFiles.sizes(root));}catch(IOException ignored){}stable=sized?stable+1:0;if(stable>=3)try{verified=NativeThemeFiles.same(target,NativeThemeFiles.capture(root));}catch(IOException ignored){}if(!verified)phone.pause(1000);}if(!verified)throw new IOException("Native resource readback was not confirmed. Theme Store may have changed files; no manual SystemUI restart was requested. Read the report before repeating.");
  }catch(Exception e){failure=e;}
  finally{if(requested)try{engine(selected);}catch(Exception e){if(failure==null)failure=e;else failure.addSuppressed(e);}}
  if(failure!=null){note=failure.getMessage();throw failure;}
  theme.delete();blur(prop);cmd("/system/bin/am","crash","--user","0","com.android.systemui");note=success;return success;
 }
 /** Oplus rejects every theme package except icons, lockscreen and launcher while night mode is active (OplusBaseFile.rejectTheme). */
 public static final String DARK="Realme's theme engine ignores notification shade theme colours while dark mode is on; blur is not a theme resource.";
 private boolean night()throws Exception{return cmd("/system/bin/cmd","uimode","night").contains("Night mode: yes");}
 public static final class Readback {public String text,blur="";public boolean night;public final Map<String,String> colors=new LinkedHashMap<>();}
 /** One capture of the native theme serves both the readable report and the editor's colour readback. */
 public synchronized Readback readback()throws Exception{
  Readback r=new Readback();r.night=night();r.blur=blur();StringBuilder b=new StringBuilder("Shade controls · user 0\nDark mode: ").append(r.night?"on (shade theme colours paused)":"off").append("\nBlur property: ").append(r.blur.isEmpty()?"device default":r.blur).append("\nBackup: ").append(hasBackup()?"available":"not captured yet");
  String[] e=engine();b.append("\nEngine package: ").append(e[0]).append("\nEngine class: ").append(e[1]);
  Map<String,byte[]> m=current();b.append("\nNative resource files: ").append(m.size());byte[] module=m.get(NativeThemeFiles.SYSTEMUI);b.append("\nSystemUI module: ").append(module==null?"none":module.length+" bytes");
  if(module!=null){byte[] c=NativeThemeFiles.unzip(module).get("assets/colors.xml");if(c!=null){org.w3c.dom.NodeList nodes=NativeThemeFiles.xml(c).getDocumentElement().getChildNodes();for(int i=0;i<nodes.getLength();i++)if(nodes.item(i) instanceof org.w3c.dom.Element){org.w3c.dom.Element el=(org.w3c.dom.Element)nodes.item(i);if(ShadeOptions.KEYS.contains(el.getAttribute("name"))){r.colors.put(el.getAttribute("name"),el.getTextContent());b.append("\n").append(el.getAttribute("name")).append(": ").append(el.getTextContent());}}}}
  if(r.night)b.append("\n\n").append(DARK);b.append("\n\n").append(note);r.text=b.toString();return r;
 }
}
