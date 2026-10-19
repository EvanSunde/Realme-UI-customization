package np.evan.lockscreen;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.*;
/** Bounded native theme files, exact package paths and colour-only module editing. */
public final class NativeThemeFiles {
 public static final int LIMIT=64*1024*1024,FILES=4096;
 public static final String SYSTEMUI="com.android.systemui";
 private static final Set<String> RETAIN=new HashSet<>(Arrays.asList("applying","video","ring","store","audio","config"));
 public static void path(String p)throws IOException{if(p==null||p.isEmpty()||p.startsWith("/")||p.contains("\\")||Arrays.asList(p.split("/",-1)).contains("..")||Arrays.asList(p.split("/",-1)).contains("")||p.indexOf('\0')>=0)throw new IOException("Invalid native resource path");}
 public static byte[] read(File f)throws IOException{if(Files.isSymbolicLink(f.toPath())||!f.isFile()||f.length()>LIMIT)throw new IOException("Unreadable or oversized theme file: "+f.getName());try(InputStream in=new FileInputStream(f)){return bytes(in,LIMIT);}}
 private static byte[] bytes(InputStream in,int max)throws IOException{ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1){if(out.size()+n>max)throw new IOException("Theme data exceeds the supported size");out.write(b,0,n);}return out.toByteArray();}
 public static Map<String,byte[]> capture(File root)throws IOException{Map<String,byte[]> m=new TreeMap<>();File[] children=root.listFiles();if(children==null)throw new IOException("Cannot read the current native theme");for(File f:children)if(!RETAIN.contains(f.getName()))walk(root,f,m);return m;}
 private static void walk(File root,File f,Map<String,byte[]> m)throws IOException{
  if(Files.isSymbolicLink(f.toPath()))throw new IOException("Theme symlinks are unsupported");
  if(f.isDirectory()){File[] a=f.listFiles();if(a==null)throw new IOException("Cannot read theme directory");for(File c:a)walk(root,c,m);return;}
  String n=root.toPath().relativize(f.toPath()).toString();path(n);m.put(n,read(f));check(m);
 }
 /** File sizes only, so an import can be polled without reading every resource each second. */
 public static Map<String,Long> sizes(File root)throws IOException{Map<String,Long> m=new TreeMap<>();File[] children=root.listFiles();if(children==null)throw new IOException("Cannot read the current native theme");for(File f:children)if(!RETAIN.contains(f.getName()))sizes(root,f,m);return m;}
 private static void sizes(File root,File f,Map<String,Long> m)throws IOException{if(Files.isSymbolicLink(f.toPath()))throw new IOException("Theme symlinks are unsupported");if(f.isDirectory()){File[] a=f.listFiles();if(a==null)throw new IOException("Cannot read theme directory");for(File c:a)sizes(root,c,m);}else m.put(root.toPath().relativize(f.toPath()).toString(),f.length());}
 public static Map<String,Long> sizes(Map<String,byte[]> files){Map<String,Long> m=new TreeMap<>();for(Map.Entry<String,byte[]> e:files.entrySet())m.put(e.getKey(),(long)e.getValue().length);return m;}
 public static void check(Map<String,byte[]> m)throws IOException{long total=0;if(m.size()>FILES)throw new IOException("Too many theme resources");for(Map.Entry<String,byte[]> e:m.entrySet()){path(e.getKey());total+=e.getValue().length;if(total>LIMIT)throw new IOException("Current theme is larger than 64 MB");}}
 public static Map<String,byte[]> unzip(byte[] raw)throws IOException{Map<String,byte[]> m=new LinkedHashMap<>();try(ZipInputStream z=new ZipInputStream(new ByteArrayInputStream(raw))){ZipEntry e;while((e=z.getNextEntry())!=null){if(e.isDirectory())continue;path(e.getName());if(m.containsKey(e.getName()))throw new IOException("Duplicate theme entry");m.put(e.getName(),bytes(z,LIMIT));check(m);}}return m;}
 public static byte[] zip(Map<String,byte[]> files)throws IOException{check(files);ByteArrayOutputStream b=new ByteArrayOutputStream();try(ZipOutputStream z=new ZipOutputStream(b)){for(Map.Entry<String,byte[]> e:files.entrySet()){ZipEntry entry=new ZipEntry(e.getKey());entry.setTime(0);z.putNextEntry(entry);z.write(e.getValue());z.closeEntry();}}return b.toByteArray();}
 public static byte[] emptyZip()throws IOException{return zip(Collections.emptyMap());}
 public static Document xml(byte[] data)throws Exception{if(data==null||data.length>512*1024)throw new IOException("Unexpected theme XML");String text=new String(data,"UTF-8");if(text.toUpperCase(Locale.US).contains("<!DOCTYPE")||text.toUpperCase(Locale.US).contains("<!ENTITY"))throw new IOException("External XML declarations are unsupported");DocumentBuilderFactory f=DocumentBuilderFactory.newInstance();f.setExpandEntityReferences(false);return f.newDocumentBuilder().parse(new ByteArrayInputStream(data));}
 public static byte[] module(byte[] old,Map<String,String> colors)throws Exception{
  Map<String,byte[]> files=old==null?new LinkedHashMap<>():unzip(old);
  Document d=files.containsKey("assets/colors.xml")?xml(files.get("assets/colors.xml")):DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
  if(d.getDocumentElement()==null)d.appendChild(d.createElement("resources"));Element root=d.getDocumentElement();if(!root.getTagName().equals("resources"))throw new IOException("Unsupported SystemUI colours format");
  NodeList nodes=root.getChildNodes();for(int i=nodes.getLength()-1;i>=0;i--){Node n=nodes.item(i);if(n instanceof Element){Element e=(Element)n;String pkg=e.getAttribute("package");if(e.getTagName().equals("color")&&ShadeOptions.KEYS.contains(e.getAttribute("name"))&&(pkg.isEmpty()||pkg.equals(SYSTEMUI)))root.removeChild(n);}}
  for(Map.Entry<String,String> c:colors.entrySet()){if(!ShadeOptions.KEYS.contains(c.getKey())||!c.getValue().matches("#[0-9a-fA-F]{8}"))throw new IOException("Unsupported shade resource");Element e=d.createElement("color");e.setAttribute("name",c.getKey());e.setTextContent(c.getValue());root.appendChild(e);}
  boolean has=false;for(int i=0;i<root.getChildNodes().getLength();i++)if(root.getChildNodes().item(i) instanceof Element)has=true;
  if(has){ByteArrayOutputStream b=new ByteArrayOutputStream();Transformer t=TransformerFactory.newInstance().newTransformer();t.setOutputProperty(OutputKeys.ENCODING,"UTF-8");t.transform(new DOMSource(d),new StreamResult(b));files.put("assets/colors.xml",b.toByteArray());}else files.remove("assets/colors.xml");
  return files.isEmpty()?null:zip(files);
 }
 public static byte[] pack(Map<String,byte[]> resources)throws IOException{
  Map<String,byte[]> r=new LinkedHashMap<>(resources);r.remove("config");r.remove("themeInfo.xml");if(!r.containsKey("wallpaper"))r.put("wallpaper",emptyZip());check(r);
  String id=UUID.randomUUID().toString().toUpperCase(Locale.US);StringBuilder meta=new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?><OppoSmartPhoneThemeInfo><Summary>Lockscreen Studio</Summary><Author>Lockscreen Studio</Author><EditorVersion>6</EditorVersion><UUID>").append(id).append("</UUID><VersionCode>1</VersionCode><PackageName>").append(id).append("</PackageName><IsGlobalTheme>0</IsGlobalTheme><lockState>0</lockState><disableLockPictorial>0</disableLockPictorial><enableLauncherApplyEffect>0</enableLauncherApplyEffect>");
  for(String n:r.keySet()){if(!n.matches("[A-Za-z0-9_./-]+"))throw new IOException("Unsupported native module name: "+n);meta.append("<package name=\"").append(n).append("\" version=\"\"/>");}meta.append("</OppoSmartPhoneThemeInfo>");r.put("themeInfo.xml",meta.toString().getBytes("UTF-8"));return zip(r);
 }
 public static void preserveExport(File exported,File nativeRoot)throws Exception{
  Map<String,byte[]> incoming=unzip(read(exported));Document info=xml(incoming.get("themeInfo.xml"));Map<String,byte[]> merged=capture(nativeRoot);NodeList modules=info.getElementsByTagName("package");if(modules.getLength()==0)throw new IOException("No native modules in export");
  for(int i=0;i<modules.getLength();i++){String name=((Element)modules.item(i)).getAttribute("name");path(name);byte[] value=incoming.get(name);if(value==null)throw new IOException("Missing exported module: "+name);merged.put(name,value);}atomic(exported,pack(merged));
 }
 public static boolean same(Map<String,byte[]> a,Map<String,byte[]> b){if(!a.keySet().equals(b.keySet()))return false;for(String n:a.keySet())if(!Arrays.equals(a.get(n),b.get(n)))return false;return true;}
 public static void atomic(File file,byte[] bytes)throws IOException{File parent=file.getParentFile();if(!parent.isDirectory()&&!parent.mkdirs())throw new IOException("Cannot create theme work directory");File temp=File.createTempFile("studio-",".tmp",parent);try{try(FileOutputStream out=new FileOutputStream(temp)){out.write(bytes);out.getFD().sync();}Files.move(temp.toPath(),file.toPath(),StandardCopyOption.REPLACE_EXISTING);}finally{temp.delete();}}
}
