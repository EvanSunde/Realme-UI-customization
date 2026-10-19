package np.evan.lockscreen;
import android.content.Context;
import android.graphics.Typeface;
import org.json.*;
import java.io.*;
import java.util.*;
public final class StudioFonts {
 private static Context context;private static JSONObject catalog;private static final Map<String,Typeface> cache=new HashMap<>();
 public static synchronized void init(Context ctx)throws Exception{context=ctx.getApplicationContext();try(InputStream in=context.getAssets().open("fonts/catalog.json")){ByteArrayOutputStream b=new ByteArrayOutputStream();byte[] c=new byte[4096];int n;while((n=in.read(c))!=-1)b.write(c,0,n);catalog=new JSONObject(b.toString("UTF-8"));}}
 public static synchronized Typeface face(String family,int weight)throws IOException{
  String key=family+":"+weight;if(cache.containsKey(key))return cache.get(key);
  if(context==null||catalog==null)throw new IOException("Font catalog is not ready");
  JSONArray items=catalog.optJSONArray(family);if(items==null||items.length()==0)throw new IOException("Missing typeface: "+family);
  JSONObject best=items.optJSONObject(0);int distance=Integer.MAX_VALUE;
  for(int i=0;i<items.length();i++){JSONObject item=items.optJSONObject(i);int d=Math.abs(item.optInt("weight",400)-weight);if(item.optBoolean("variable")){best=item;break;}if(d<distance){distance=d;best=item;}}
  Typeface.Builder builder=new Typeface.Builder(context.getAssets(),"fonts/"+best.optString("file")).setWeight(weight);
  if(best.optBoolean("variable"))builder.setFontVariationSettings("'wght' "+weight);
  Typeface value=builder.build();cache.put(key,value);return value;
 }
}
