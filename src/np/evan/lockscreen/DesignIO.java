package np.evan.lockscreen;
import org.json.*;
import java.lang.reflect.Field;
public final class DesignIO {
 public static JSONObject json(ThemeBuilder.Design d)throws Exception{
  JSONObject j=new JSONObject();for(Field f:ThemeBuilder.Design.class.getFields())j.put(f.getName(),f.get(d));return j;
 }
 public static ThemeBuilder.Design parse(JSONObject j)throws Exception{
  ThemeBuilder.Design d=new ThemeBuilder.Design();
  for(Field f:ThemeBuilder.Design.class.getFields()){
   if(f.getType()==int.class)f.setInt(d,j.optInt(f.getName(),f.getInt(d)));
   else if(f.getType()==boolean.class)f.setBoolean(d,j.optBoolean(f.getName(),f.getBoolean(d)));
   else if(f.getType()==String.class)f.set(d,j.optString(f.getName(),""));
  }
  d.size=clamp(d.size,90,320);d.vertical=clamp(d.vertical,3,72);d.horizontal=clamp(d.horizontal,-420,420);d.height=clamp(d.height,1800,2700);d.font=clamp(d.font,0,5);d.spacing=clamp(d.spacing,0,45);d.opacity=clamp(d.opacity,40,255);d.effect=clamp(d.effect,0,3);d.motion=clamp(d.motion,0,2);d.background=clamp(d.background,0,2);d.dateFormat=clamp(d.dateFormat,0,4);d.dateSize=clamp(d.dateSize,24,72);d.dim=clamp(d.dim,0,75);d.subjectScale=clamp(d.subjectScale,50,160);d.subjectX=clamp(d.subjectX,-500,500);d.subjectY=clamp(d.subjectY,-700,700);
  if(d.v2.length()>150000)throw new IllegalArgumentException("Studio document is too large");if(!d.v2.isEmpty())new JSONObject(d.v2);
  d.customText=d.customText.replaceAll("[\\p{Cntrl}]", " ");if(d.customText.length()>80)d.customText=d.customText.substring(0,80);return d;
 }
 private static int clamp(int n,int low,int high){return Math.max(low,Math.min(high,n));}
}
