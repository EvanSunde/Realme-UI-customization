package np.evan.lockscreen;
import java.io.*;
import java.util.*;
/** Native theme transaction under the Shizuku shell identity; no hooks or overlays. */
public final class ShadeLab {
 public interface Phone {String run(String... args)throws Exception;void pause(long millis)throws Exception;}
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
  o.validate();user();String night=cmd("/system/bin/cmd","uimode","night");if(!o.testDarkMode&&night.contains("Night mode: yes"))throw new IOException("Use light mode for the verified resource path, or enable the experimental dark-mode test.");
  Map<String,byte[]> current=NativeThemeFiles.capture(root);String[] selected=engine();String originalBlur=blur();if(!property(originalBlur))throw new IOException("Unrecognized current blur property");
  if(!hasBackup())NativeThemeFiles.atomic(backup(),backupBytes(current.get(NativeThemeFiles.SYSTEMUI),originalBlur));else before();
  byte[] module=NativeThemeFiles.module(current.get(NativeThemeFiles.SYSTEMUI),o.colors());Map<String,byte[]> target=new TreeMap<>(current);if(module==null)target.remove(NativeThemeFiles.SYSTEMUI);else target.put(NativeThemeFiles.SYSTEMUI,module);
  return install(target,selected,o.changeBlur?Integer.toString(o.blurMode):originalBlur,"Applied shade choices. Pull down the panel to check the phone. Panel alpha is a blending experiment; dark mode and reboot persistence are unverified.");
 }
 public synchronized String restore()throws Exception{user();Before b=before();Map<String,byte[]> current=NativeThemeFiles.capture(root);if(b.module==null)current.remove(NativeThemeFiles.SYSTEMUI);else current.put(NativeThemeFiles.SYSTEMUI,b.module);return install(current,engine(),b.prop,"Restored the SystemUI module and blur value captured before the first Studio shade apply. Current clock and other theme resources were retained.");}
 private String install(Map<String,byte[]> target,String[] selected,String prop,String success)throws Exception{
  if(!target.containsKey("wallpaper"))target.put("wallpaper",NativeThemeFiles.emptyZip());
  File theme=new File(exports,"Evan-Shade-"+System.currentTimeMillis()+".theme");NativeThemeFiles.atomic(theme,NativeThemeFiles.pack(target));
  // A complete recovery pair is committed before Theme Store can change the selection.
  NativeThemeFiles.atomic(new File(work,"last-engine.txt"),(selected[0]+"\n"+selected[1]+"\n").getBytes("UTF-8"));
  boolean requested=false;Exception failure=null;
  try{
   requested=true;String response=cmd("/system/bin/am","startservice","-n","com.oplus.themestore/com.nearme.themespace.services.ThemeApplyService","--es","THEME_PATH",theme.getPath(),"--ez","IS_FROM_911","true");String lower=response.toLowerCase(Locale.US);if(lower.contains("error")||lower.contains("exception")||lower.contains("denial")||lower.contains("failure"))throw new IOException(response);
   int stable=0;for(int i=0;i<45;i++){boolean matches=false;try{matches=NativeThemeFiles.same(target,NativeThemeFiles.capture(root));}catch(IOException ignored){}stable=matches?stable+1:0;if(stable>=4)break;phone.pause(1000);}if(stable<4)throw new IOException("Native resource readback was not confirmed. Theme Store may have changed files; no manual SystemUI restart was requested. Read the report before repeating.");
  }catch(Exception e){failure=e;}
  finally{if(requested)try{engine(selected);}catch(Exception e){if(failure==null)failure=e;else failure.addSuppressed(e);}}
  if(failure!=null){note=failure.getMessage();throw failure;}
  blur(prop);cmd("/system/bin/am","crash","--user","0","com.android.systemui");note=success;return success;
 }
 public synchronized String report()throws Exception{
  StringBuilder b=new StringBuilder("Shade controls · user 0\n");b.append(cmd("/system/bin/cmd","uimode","night")).append("\nBlur property: ").append(blur()).append("\nBackup: ").append(hasBackup()?"available":"not captured yet");
  String[] e=engine();b.append("\nEngine package: ").append(e[0]).append("\nEngine class: ").append(e[1]);
  Map<String,byte[]> m=current();b.append("\nNative resource files: ").append(m.size());byte[] module=m.get(NativeThemeFiles.SYSTEMUI);b.append("\nSystemUI module: ").append(module==null?"none":module.length+" bytes");if(module!=null){byte[] c=NativeThemeFiles.unzip(module).get("assets/colors.xml");if(c!=null){DocumentRead.append(b,c);}}
  b.append("\n\n").append(note);return b.toString();
 }
 private static final class DocumentRead {static void append(StringBuilder b,byte[] xml)throws Exception{org.w3c.dom.NodeList nodes=NativeThemeFiles.xml(xml).getDocumentElement().getChildNodes();for(int i=0;i<nodes.getLength();i++)if(nodes.item(i) instanceof org.w3c.dom.Element){org.w3c.dom.Element e=(org.w3c.dom.Element)nodes.item(i);if(ShadeOptions.KEYS.contains(e.getAttribute("name")))b.append("\n").append(e.getAttribute("name")).append(": ").append(e.getTextContent());}}}
}
