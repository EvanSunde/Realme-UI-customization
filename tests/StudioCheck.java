import np.evan.lockscreen.ThemeBuilder;
import java.io.*;
import java.util.*;
import java.util.zip.*;
import java.lang.reflect.Field;
import javax.xml.parsers.*;
import org.w3c.dom.*;
public class StudioCheck {
 static Document xml(String s)throws Exception{return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new ByteArrayInputStream(s.getBytes("UTF-8")));}
 static void check(boolean b,String s){if(!b)throw new AssertionError(s);}
 static Map<String,byte[]> entries(byte[] bytes)throws Exception{
  Map<String,byte[]> map=new HashMap<>();try(ZipInputStream z=new ZipInputStream(new ByteArrayInputStream(bytes))){ZipEntry e;while((e=z.getNextEntry())!=null){ByteArrayOutputStream o=new ByteArrayOutputStream();byte[] c=new byte[1024];int n;while((n=z.read(c))!=-1)o.write(c,0,n);map.put(e.getName(),o.toByteArray());}}return map;
 }
 public static void main(String[] args)throws Exception{
  int checked=0;
  for(int background=0;background<3;background++)for(int motion=0;motion<3;motion++)for(boolean depth:new boolean[]{false,true})for(boolean night:new boolean[]{false,true})for(boolean hidden:new boolean[]{false,true}){
   ThemeBuilder.Design d=new ThemeBuilder.Design();d.background=background;d.motion=motion;d.depth=depth;d.nightColor=night;d.hidden=hidden;d.seconds=true;d.battery=true;d.customText="Evan < & > \" studio '";d.subjectScale=130;d.subjectX=-42;d.subjectY=70;
   String s=ThemeBuilder.layout(d);Document root=xml(s);
   check(root.getElementsByTagName("ExternCommand").getLength()==1,"Native unlock must be retained");
   boolean bg=false,subject=false;NodeList images=root.getElementsByTagName("Image");
   for(int i=0;i<images.getLength();i++){Element e=(Element)images.item(i);if(e.getAttribute("src").equals("bg.jpg"))bg=true;if(e.getAttribute("src").equals("subject.png"))subject=true;}
   check(bg==(background!=2),"Live mode must not paint an opaque background");
   check(subject==(depth&&background!=2),"Depth must not freeze a subject over a rotating live wallpaper");
   if(subject)check(s.indexOf("studio_clock")<s.indexOf("src=\"subject.png\""),"Subject must render above clock");
   check(s.contains("DateTime.Second")&&s.contains("#battery_level"),"Native dynamic sources missing");
   check(root.getElementsByTagName("PositionAnimation").getLength()==(motion==1?(night?2:1):0),"Float animation");
   check(root.getElementsByTagName("AlphaAnimation").getLength()==(motion==2?(night?2:1):0),"Breathing animation");
   // Keyframes use MAML's documented child tags; generic <Item> keyframes did not animate on the phone.
   check(root.getElementsByTagName("Item").getLength()==0&&root.getElementsByTagName("Position").getLength()==(motion==1?(night?6:3):0)&&root.getElementsByTagName("Alpha").getLength()==(motion==2?(night?6:3):0),"Keyframe tags");
   NodeList texts=root.getElementsByTagName("Text");boolean escaped=false;for(int i=0;i<texts.getLength();i++)if(((Element)texts.item(i)).getAttribute("text").equals(d.customText))escaped=true;check(escaped,"Caption must escape XML without losing text");
   ThemeBuilder.Design copy=d.copy();for(Field field:ThemeBuilder.Design.class.getFields())check(Objects.equals(field.get(d),field.get(copy)),"Scene snapshot lost "+field.getName());
   checked++;
  }
  check(entries(ThemeBuilder.wallpaperComponent(new byte[]{1,2,3},true)).isEmpty(),"Preserve mode must have no image entry");
  File empty=File.createTempFile("wallpaper-preserve-",".zip");try{try(OutputStream out=new FileOutputStream(empty)){out.write(ThemeBuilder.wallpaperComponent(new byte[]{},true));}try(ZipFile z=new ZipFile(empty)){check(z.size()==0,"Theme Store can open an empty ZIP");}}finally{empty.delete();}
  Map<String,byte[]> wall=entries(ThemeBuilder.wallpaperComponent(new byte[]{7,8},false));check(wall.size()==2&&wall.containsKey("_default_wallpaper.jpg")&&wall.containsKey("_default_keyguard_wallpaper.jpg"),"Explicit replacement mode");
  ThemeBuilder.Design d=new ThemeBuilder.Design();d.tapHide=false;check(!ThemeBuilder.layout(d).contains("action=\"double\""),"Disabled gesture must not be exported");
  System.out.println("Validated "+checked+" studio combinations: live transparency, wallpaper preservation, depth layer order, animations, dynamic data, XML escaping, gestures and full scene snapshots.");
 }
}
