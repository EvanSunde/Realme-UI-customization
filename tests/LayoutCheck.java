import np.evan.lockscreen.ThemeBuilder;
import java.io.*;
import javax.xml.parsers.*;
import org.w3c.dom.*;
public class LayoutCheck {
 public static void main(String[] args)throws Exception {
  int checked=0;
  for(int size:new int[]{90,180,260})for(boolean stacked:new boolean[]{false,true})for(boolean twelve:new boolean[]{false,true})for(boolean date:new boolean[]{false,true}){
   ThemeBuilder.Design d=new ThemeBuilder.Design();d.size=size;d.stacked=stacked;d.twelve=twelve;d.date=date;d.color=0xffabcd12;d.horizontal=-120;d.vertical=65;
   String xml=ThemeBuilder.layout(d);
   Document doc=DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new ByteArrayInputStream(xml.getBytes("UTF-8")));
   if(!doc.getDocumentElement().getTagName().equals("Lockscreen"))throw new AssertionError("Root");
   if(doc.getElementsByTagName("ExternCommand").getLength()!=1)throw new AssertionError("Unlock command missing");
   if(doc.getElementsByTagName("DateTime").getLength()!=(date?1:0))throw new AssertionError("Date toggle");
   if(doc.getElementsByTagName("Image").getLength()!=(stacked?5:6))throw new AssertionError("Clock glyph count");
   for(int i=0;i<doc.getElementsByTagName("Image").getLength();i++){
    Element e=(Element)doc.getElementsByTagName("Image").item(i);
    if(e.hasAttribute("srcid")&&!e.getAttribute("srcid").startsWith("int("))throw new AssertionError("Non-integral digit expression");
   }
   checked++;
  }
  System.out.println("Validated "+checked+" generated lockscreen layouts, both time formats, both clock arrangements, date visibility and unlock command.");
 }
}
