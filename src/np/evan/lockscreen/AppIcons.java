package np.evan.lockscreen;

import android.content.*;
import android.content.pm.*;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.provider.MediaStore;
import android.util.Base64;
import org.json.*;
import java.io.ByteArrayOutputStream;
import java.util.*;

/** Launcher apps for shortcut pickers, and their icons and the default camera for theme building. */
public final class AppIcons implements StudioTheme.Apps {
 private final Context context;
 public AppIcons(Context context){this.context=context;}
 private static Bitmap draw(Drawable d,int px){Bitmap b=Bitmap.createBitmap(px,px,Bitmap.Config.ARGB_8888);Canvas c=new Canvas(b);d.setBounds(0,0,px,px);d.draw(c);return b;}
 @Override public Bitmap icon(String pkg,String cls,int px){try{return draw(context.getPackageManager().getActivityIcon(new ComponentName(pkg,cls)),Math.max(8,px));}catch(Exception e){return null;}}
 @Override public String camera(){try{ResolveInfo r=context.getPackageManager().resolveActivity(new Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA_SECURE),0);return r==null||r.activityInfo==null||"android".equals(r.activityInfo.packageName)?null:r.activityInfo.packageName;}catch(Exception e){return null;}}
 /** Launcher activities sorted by label, with small PNG icons as data URLs for the editor. */
 public static JSONArray list(Context context)throws JSONException{
  PackageManager pm=context.getPackageManager();Intent main=new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
  List<ResolveInfo> found=pm.queryIntentActivities(main,0);List<String[]> rows=new ArrayList<>();Map<String,ResolveInfo> byKey=new HashMap<>();
  for(ResolveInfo r:found){if(r.activityInfo==null||context.getPackageName().equals(r.activityInfo.packageName))continue;String key=r.activityInfo.packageName+"/"+r.activityInfo.name;if(byKey.containsKey(key))continue;byKey.put(key,r);rows.add(new String[]{String.valueOf(r.loadLabel(pm)),key});}
  rows.sort((a,b)->a[0].compareToIgnoreCase(b[0]));
  JSONArray out=new JSONArray();
  for(String[] row:rows){
   ResolveInfo r=byKey.get(row[1]);String icon="";
   try{Bitmap b=draw(r.loadIcon(pm),64);ByteArrayOutputStream o=new ByteArrayOutputStream();b.compress(Bitmap.CompressFormat.PNG,100,o);b.recycle();icon="data:image/png;base64,"+Base64.encodeToString(o.toByteArray(),Base64.NO_WRAP);}catch(Exception ignored){}
   out.put(new JSONObject().put("label",row[0]).put("pkg",r.activityInfo.packageName).put("cls",r.activityInfo.name).put("icon",icon));
  }
  return out;
 }
}
