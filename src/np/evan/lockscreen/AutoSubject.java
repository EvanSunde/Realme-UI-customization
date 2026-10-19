package np.evan.lockscreen;

import android.content.*;
import android.graphics.*;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.segmentation.subject.*;
import java.io.*;
import java.nio.FloatBuffer;
import java.util.concurrent.Executor;

/** One-photo ML Kit subject cutout, based on Atmos's optional segmentation path. */
public final class AutoSubject {
 public interface Callback {void finish(File image,String error);}
 private static final String PREF="auto_subject_guard";
 private AutoSubject(){}
 public static void reset(Context ctx){ctx.getSharedPreferences(PREF,Context.MODE_PRIVATE).edit().clear().commit();}
 public static void extract(Context ctx,File source,int height,File target,Executor worker,Callback done)throws Exception{
  android.content.SharedPreferences pref=ctx.getSharedPreferences(PREF,Context.MODE_PRIVATE);
  if(pref.getInt("crashes",0)>=2)throw new IOException("Automatic separation was disabled after repeated crashes. Import a subject PNG instead.");
  if(pref.getBoolean("inFlight",false)){
   pref.edit().putBoolean("inFlight",false).putInt("crashes",pref.getInt("crashes",0)+1).commit();
   throw new IOException("Subject separation was interrupted last time. Try again; if this repeats, use a transparent PNG.");
  }
  Bitmap wallpaper=ThemeBuilder.wallpaper(source,height);
  int h=Math.max(1,Math.round(height*1024f/Math.max(1080,height))),w=Math.max(1,Math.round(1080*1024f/Math.max(1080,height)));
  Bitmap input=Bitmap.createScaledBitmap(wallpaper,w,h,true);wallpaper.recycle();
  SubjectSegmenter segmenter=null;
  try{
   SubjectSegmenterOptions opts=new SubjectSegmenterOptions.Builder().enableForegroundConfidenceMask().build();
   segmenter=SubjectSegmentation.getClient(opts);
   if(!pref.edit().putBoolean("inFlight",true).commit())throw new IOException("Cannot save subject processing state");
   final SubjectSegmenter model=segmenter;
   segmenter.process(InputImage.fromBitmap(input,0)).addOnSuccessListener(worker,result->{
    try{
     FloatBuffer floats=result.getForegroundConfidenceMask();if(floats==null)throw new IOException("The model returned no subject; it may still be downloading through Play services");
     FloatBuffer mask=floats.duplicate();mask.rewind();int count=w*h;if(mask.remaining()<count)throw new IOException("Incomplete subject mask");
     int[] pixels=new int[count];input.getPixels(pixels,0,w,0,0,w,h);int confident=0,high=0;
     for(int i=0;i<count;i++){
      float v=mask.get();if(!Float.isFinite(v))v=0;
      if(v>.55f)confident++;if(v>.75f)high++;
      float a=Math.max(0,Math.min(1,(v-.28f)/.44f));a=a*a*(3-2*a);
      pixels[i]=(pixels[i]&0xffffff)|((int)(255*a)<<24);
     }
     if(confident<count*.012f||high<count*.003f||confident>count*.90f)throw new IOException("No clear subject found in this photo. Try another photo or import a cutout PNG.");
     Bitmap cut=Bitmap.createBitmap(pixels,w,h,Bitmap.Config.ARGB_8888);
     try(FileOutputStream out=new FileOutputStream(target)){if(!cut.compress(Bitmap.CompressFormat.PNG,100,out))throw new IOException("Cannot save the separated subject");}
     finally{cut.recycle();}
     pref.edit().putBoolean("inFlight",false).putInt("crashes",0).commit();done.finish(target,null);
    }catch(Throwable e){pref.edit().putBoolean("inFlight",false).commit();target.delete();done.finish(null,e.getMessage()==null?"Subject extraction failed":e.getMessage());}
    finally{input.recycle();model.close();}
   }).addOnFailureListener(worker,e->{pref.edit().putBoolean("inFlight",false).commit();input.recycle();model.close();target.delete();done.finish(null,"ML Kit subject model unavailable: "+e.getMessage()+". It may still be downloading through Play services.");});
  }catch(Throwable e){input.recycle();if(segmenter!=null)segmenter.close();pref.edit().putBoolean("inFlight",false).commit();target.delete();if(e instanceof Exception)throw (Exception)e;throw new IOException("Subject separation could not start: "+e.getMessage());}
 }
}
