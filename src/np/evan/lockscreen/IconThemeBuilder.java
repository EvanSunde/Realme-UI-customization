package np.evan.lockscreen;

import android.content.*;
import android.content.pm.*;
import android.content.res.*;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.util.Xml;
import org.xmlpull.v1.XmlPullParser;
import java.io.*;
import java.util.*;
import java.util.zip.*;

/** Creates independently staged Oppo launcher/icon modules from a user-installed icon pack. */
public final class IconThemeBuilder {
 private IconThemeBuilder(){}
 public static final String[] SHAPES={"Original","Circle","Rounded square","Squircle","Diamond","Hexagon"};
 private static byte[] zip(Map<String,byte[]> items)throws IOException{
  ByteArrayOutputStream bytes=new ByteArrayOutputStream();try(ZipOutputStream z=new ZipOutputStream(bytes)){
   for(Map.Entry<String,byte[]> entry:items.entrySet()){z.putNextEntry(new ZipEntry(entry.getKey()));z.write(entry.getValue());z.closeEntry();}
  }return bytes.toByteArray();
 }
 private static byte[] png(Bitmap b)throws IOException{ByteArrayOutputStream o=new ByteArrayOutputStream();if(!b.compress(Bitmap.CompressFormat.PNG,100,o))throw new IOException("Icon encoding failed");return o.toByteArray();}
 private static String xml(String s){return s.replace("&","&amp;").replace("<","&lt;").replace("\"","&quot;").replace("'","&apos;");}
 public static Path shape(int kind,float size){
  Path p=new Path();float inset=size*.035f,a=inset,b=size-inset;
  if(kind==1)p.addCircle(size/2,size/2,(b-a)/2,Path.Direction.CW);
  else if(kind==2)p.addRoundRect(a,a,b,b,size*.23f,size*.23f,Path.Direction.CW);
  else if(kind==3){float k=size*.19f;p.moveTo(size/2,a);p.cubicTo(size-k,a,b,a+k,b,size/2);p.cubicTo(b,b-k,b-k,b,size/2,b);p.cubicTo(a+k,b,a,b-k,a,size/2);p.cubicTo(a,a+k,a+k,a,size/2,a);p.close();}
  else if(kind==4){p.moveTo(size/2,a);p.lineTo(b,size/2);p.lineTo(size/2,b);p.lineTo(a,size/2);p.close();}
  else if(kind==5){for(int i=0;i<6;i++){double angle=Math.PI*i/3-Math.PI/2;float x=size/2+(float)Math.cos(angle)*(b-a)/2,y=size/2+(float)Math.sin(angle)*(b-a)/2;if(i==0)p.moveTo(x,y);else p.lineTo(x,y);}p.close();}
  else p.addRect(a,a,b,b,Path.Direction.CW);
  return p;
 }
 private static Bitmap render(Drawable icon,int shape)throws IOException{
  Bitmap b=Bitmap.createBitmap(192,192,Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(b);
  if(shape>0)canvas.clipPath(shape(shape,192));
  icon.setBounds(0,0,192,192);icon.draw(canvas);return b;
 }
 private static byte[] mask(int shape)throws IOException{
  Bitmap b=Bitmap.createBitmap(192,192,Bitmap.Config.ARGB_8888);Canvas c=new Canvas(b);Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);p.setColor(Color.BLACK);c.drawPath(shape(shape,192),p);byte[] bytes=png(b);b.recycle();return bytes;
 }
 private static XmlPullParser filter(Context ctx,String pkg)throws Exception{
  Context other=ctx.createPackageContext(pkg,Context.CONTEXT_IGNORE_SECURITY);
  int id=other.getResources().getIdentifier("appfilter","xml",pkg);
  if(id!=0)return other.getResources().getXml(id);
  try{InputStream in=other.getAssets().open("appfilter.xml");XmlPullParser parser=Xml.newPullParser();parser.setInput(in,"UTF-8");return parser;}catch(IOException e){return null;}
 }
 public static boolean isPack(Context context,String pkg){try{return filter(context,pkg)!=null;}catch(Exception e){return false;}}
 private static Map<String,String> mappings(Context ctx,String pkg)throws Exception{
  Map<String,String> map=new HashMap<>();XmlPullParser x=filter(ctx,pkg);if(x==null)throw new IOException("That app has no icon-pack appfilter.xml");
  int count=0;while(x.next()!=XmlPullParser.END_DOCUMENT){if(x.getEventType()!=XmlPullParser.START_TAG||!"item".equals(x.getName()))continue;
   String component=x.getAttributeValue(null,"component"),drawable=x.getAttributeValue(null,"drawable");if(component==null||drawable==null)continue;
   int left=component.indexOf('{'),right=component.indexOf('/',left+1);if(left<0||right<=left+1)continue;
   String packageName=component.substring(left+1,right);if(packageName.matches("[a-zA-Z0-9_.]+")&&drawable.matches("[a-zA-Z0-9_]+"))map.put(packageName,drawable);
   if(++count>30000)throw new IOException("Icon pack has too many mappings");
  }return map;
 }
 public static List<String[]> packs(Context context){
  PackageManager pm=context.getPackageManager();Map<String,String[]> result=new TreeMap<>();
  for(String action:new String[]{"org.adw.launcher.THEMES","com.novalauncher.THEME"}){
   Intent intent=new Intent(action);for(ResolveInfo info:pm.queryIntentActivities(intent,0)){
    if(info.activityInfo==null)continue;String pkg=info.activityInfo.packageName;
    if(isPack(context,pkg))result.put(pkg,new String[]{pkg,info.loadLabel(pm).toString()});
   }
  }
  // Some packs expose only a launcher activity and an appfilter resource.
  Intent launchers=new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
  for(ResolveInfo info:pm.queryIntentActivities(launchers,0)){
   if(info.activityInfo==null)continue;String pkg=info.activityInfo.packageName;
   if(!result.containsKey(pkg)&&isPack(context,pkg))result.put(pkg,new String[]{pkg,info.loadLabel(pm).toString()});
  }
  return new ArrayList<>(result.values());
 }
 public static byte[] build(Context ctx,String packageName,int shape)throws Exception{
  if(shape<0||shape>=SHAPES.length)throw new IOException("Choose a supported shape");
  if(packageName==null||!packageName.matches("[a-zA-Z0-9_.]+")||!isPack(ctx,packageName))throw new IOException("Select an installed icon pack with appfilter.xml");
  PackageManager pm=ctx.getPackageManager();Context pack=ctx.createPackageContext(packageName,Context.CONTEXT_IGNORE_SECURITY);
  Map<String,String> names=mappings(ctx,packageName);Map<String,byte[]> files=new LinkedHashMap<>(),launcher=new LinkedHashMap<>();
  StringBuilder all=new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?><iconinfo>");Set<String> seen=new HashSet<>();
  Intent intent=new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
  for(ResolveInfo r:pm.queryIntentActivities(intent,0)){
   if(r.activityInfo==null)continue;String pkg=r.activityInfo.packageName;if(!pkg.matches("[a-zA-Z0-9_.]+")||!seen.add(pkg))continue;
   String iconName=names.get(pkg);Drawable drawable=null;
   if(iconName!=null){int id=pack.getResources().getIdentifier(iconName,"drawable",packageName);if(id!=0)try{drawable=pack.getDrawable(id);}catch(Exception ignored){}}
   if(drawable==null)try{drawable=r.loadIcon(pm);}catch(Exception ignored){}
   if(drawable==null)continue;
   Bitmap b=render(drawable,shape);String file="evan_"+seen.size()+".png";
   files.put("res/drawable-xxhdpi/"+file,png(b));b.recycle();all.append("<icon name=\"").append(file).append("\" package=\"").append(xml(pkg)).append("\"/>");
   if(seen.size()>=500)break;
  }
  if(files.isEmpty())throw new IOException("No launcher app icons are visible. Check Android package visibility.");
  all.append("</iconinfo>");files.put("allApps.xml",all.toString().getBytes("UTF-8"));
  launcher.put("themeInfo.xml","<?xml version=\"1.0\"?><OppoSmartPhoneThemeInfo><Scale>0.80</Scale><XOffsetPCT>0.00</XOffsetPCT><YOffsetPCT>0.00</YOffsetPCT></OppoSmartPhoneThemeInfo>".getBytes("UTF-8"));
  if(shape>0){byte[] m=mask(shape);launcher.put("res/drawable-xxhdpi/new_iphone_style_mask.png",m);launcher.put("res/drawable-xxhdpi/new_iphone_style_bg.png",m);}
  return packageIcons(zip(files),zip(launcher));
 }
 public static byte[] packageIcons(byte[] icons,byte[] launcher)throws IOException{
  String id=UUID.randomUUID().toString().toUpperCase(Locale.US);
  String info="<?xml version=\"1.0\" encoding=\"UTF-8\"?><OppoSmartPhoneThemeInfo><Summary>Evan Icons</Summary><Author>Evan Lockscreen Editor</Author><EditorVersion>6</EditorVersion><UUID>"+id+"</UUID><VersionCode>1</VersionCode><PackageName>"+id+"</PackageName><IsGlobalTheme>0</IsGlobalTheme><lockState>0</lockState><package name=\"wallpaper\" version=\"\"/><package name=\"icons\" version=\"\"/><package name=\"com.oppo.launcher\" version=\"\"/></OppoSmartPhoneThemeInfo>";
  Map<String,byte[]> root=new LinkedHashMap<>();root.put("themeInfo.xml",info.getBytes("UTF-8"));root.put("wallpaper",ThemeBuilder.wallpaperComponent(new byte[0],true));root.put("icons",icons);root.put("com.oppo.launcher",launcher);return zip(root);
 }
}
