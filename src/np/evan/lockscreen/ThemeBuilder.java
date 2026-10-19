package np.evan.lockscreen;

import android.graphics.*;
import java.io.*;
import java.util.*;
import java.util.zip.*;

public final class ThemeBuilder {
    public static final class Design {
        public int size=180,color=Color.WHITE,vertical=18,horizontal=0,font=0,height=2400,spacing=0,opacity=255,effect=0,motion=0,background=1,dateFormat=0,dateSize=38,subjectScale=100,subjectX=0,subjectY=0,dim=0;
        public boolean stacked=false,twelve=false,date=true,seconds=false,battery=false,hidden=false,tapHide=true,depth=false,nightColor=false;
        public String customText="",v2="";
        public Design copy(){Design d=new Design();
            d.size=size;d.color=color;d.vertical=vertical;d.horizontal=horizontal;d.font=font;d.height=height;d.spacing=spacing;d.opacity=opacity;d.effect=effect;d.motion=motion;d.background=background;d.dateFormat=dateFormat;d.dateSize=dateSize;d.subjectScale=subjectScale;d.subjectX=subjectX;d.subjectY=subjectY;d.dim=dim;
            d.stacked=stacked;d.twelve=twelve;d.date=date;d.seconds=seconds;d.battery=battery;d.hidden=hidden;d.tapHide=tapHide;d.depth=depth;d.nightColor=nightColor;d.customText=customText;d.v2=v2;return d;
        }
    }
    public static final String[] DATE_FORMATS={"EEE, dd MMM","EEEE, MMMM d","dd / MM / yyyy","MMMM d","EEEE"};
    public static Typeface face(int index){return Typeface.create(new String[]{"sans-serif-light","sans-serif-medium","sans-serif-condensed","monospace","serif","sans-serif-black"}[Math.max(0,Math.min(5,index))],Typeface.NORMAL);}
    public static Bitmap wallpaper(File photo,int height) throws IOException {
        Bitmap result=Bitmap.createBitmap(1080,height,Bitmap.Config.ARGB_8888);Canvas c=new Canvas(result);
        Paint p=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
        p.setShader(new LinearGradient(0,0,1080,height,new int[]{Color.rgb(9,17,24),Color.rgb(12,43,53)},null,Shader.TileMode.CLAMP));
        c.drawRect(0,0,1080,height,p);p.setShader(null);
        if(photo!=null && photo.isFile()) {
            BitmapFactory.Options o=new BitmapFactory.Options();o.inJustDecodeBounds=true;BitmapFactory.decodeFile(photo.getPath(),o);
            if(o.outWidth<=0||o.outHeight<=0)throw new IOException("Cannot decode the selected image");
            int sample=1;while(Math.max(o.outWidth,o.outHeight)/sample>3200)sample*=2;
            o.inJustDecodeBounds=false;o.inSampleSize=sample;
            Bitmap b=BitmapFactory.decodeFile(photo.getPath(),o);if(b==null)throw new IOException("Cannot decode wallpaper");
            try {
                android.media.ExifInterface exif=new android.media.ExifInterface(photo.getPath());
                int orientation=exif.getAttributeInt(android.media.ExifInterface.TAG_ORIENTATION,1);Matrix rotation=new Matrix();
                switch(orientation){case 2:rotation.setScale(-1,1);break;case 3:rotation.setRotate(180);break;case 4:rotation.setScale(1,-1);break;case 5:rotation.setRotate(90);rotation.postScale(-1,1);break;case 6:rotation.setRotate(90);break;case 7:rotation.setRotate(-90);rotation.postScale(-1,1);break;case 8:rotation.setRotate(-90);break;}
                if(!rotation.isIdentity()){Bitmap turned=Bitmap.createBitmap(b,0,0,b.getWidth(),b.getHeight(),rotation,true);if(turned!=b)b.recycle();b=turned;}
            }catch(IOException ignored){}
            float scale=Math.max(1080f/b.getWidth(),height/(float)b.getHeight());
            float w=b.getWidth()*scale,h=b.getHeight()*scale;
            c.drawBitmap(b,null,new RectF((1080-w)/2,(height-h)/2,(1080+w)/2,(height+h)/2),p);b.recycle();
        }
        return result;
    }
    public static int cell(Design d){return (int)Math.ceil(d.size*0.68f);}
    public static int glyphHeight(Design d){return (int)Math.ceil(d.size*1.28f);}
    public static Bitmap glyph(char digit,Design d) {
        int w=digit==':'?Math.max(20,(int)(d.size*.30f)):cell(d),h=glyphHeight(d);
        Bitmap b=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);Canvas c=new Canvas(b);
        Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);p.setTypeface(face(d.font));p.setTextSize(d.size);p.setColor(d.color);p.setTextAlign(Paint.Align.CENTER);
        if(d.effect==1)p.setShadowLayer(d.size*.035f,0,d.size*.025f,0xb0000000);
        if(d.effect==2)p.setShadowLayer(d.size*.055f,0,0,d.color);
        if(d.effect==3){p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(Math.max(2,d.size*.012f));}
        Paint.FontMetrics fm=p.getFontMetrics();c.drawText(String.valueOf(digit),w/2f,(h-fm.bottom-fm.top)/2f,p);return b;
    }
    private static byte[] image(Bitmap b,boolean jpg)throws IOException {
        ByteArrayOutputStream out=new ByteArrayOutputStream();
        if(!b.compress(jpg?Bitmap.CompressFormat.JPEG:Bitmap.CompressFormat.PNG,jpg?92:100,out))throw new IOException("Image encoding failed");
        return out.toByteArray();
    }
    private static byte[] zip(Map<String,byte[]> files)throws IOException {
        ByteArrayOutputStream out=new ByteArrayOutputStream();
        try(ZipOutputStream z=new ZipOutputStream(out)) { for(Map.Entry<String,byte[]> e:files.entrySet()){z.putNextEntry(new ZipEntry(e.getKey()));z.write(e.getValue());z.closeEntry();} }
        return out.toByteArray();
    }
    private static String esc(String s){return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;").replace("'","&apos;");}
    private static String color(int c){return String.format(Locale.US,"#%06X",c&0xffffff);}
    public static String layout(Design d){
        int gh=glyphHeight(d),frame=d.motion>0?15:1;
        StringBuilder x=new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?><Lockscreen version=\"1\" screenWidth=\"1080\" frameRate=\"").append(frame).append("\" useVariableUpdater=\"Battery,DateTime.").append(d.seconds?"Second":"Minute").append("\"");
        if(d.background==2)x.append(" showSysWallpaper=\"true\"");
        x.append("><Var name=\"notification\" expression=\"1\"/><Var name=\"studio_hidden\" expression=\"").append(d.hidden?1:0).append("\"/>");
        if(d.background!=2)x.append("<Image src=\"bg.jpg\" x=\"0\" y=\"0\" w=\"#screen_width\" h=\"#screen_height\"/>");
        if(d.dim>0)x.append("<Image src=\"dim.png\" x=\"0\" y=\"0\" w=\"#screen_width\" h=\"#screen_height\" alpha=\"").append(d.dim*255/100).append("\"/>");
        int total=d.stacked?cell(d)*2+d.spacing:cell(d)*4+Math.max(20,(int)(d.size*.30f))+d.spacing*3;
        x.append("<Var name=\"clock_x\" expression=\"(#screen_width-").append(total).append(")/2+").append(d.horizontal).append("\"/><Var name=\"clock_y\" expression=\"#screen_height*").append(d.vertical).append("/100\"/>");
        x.append("<Group name=\"studio_clock\" visibility=\"eq(#studio_hidden,0)\" alpha=\"").append(d.opacity).append("\">");
        if(d.nightColor){
            x.append("<Group visibility=\"eq(gt(#hour24,17)+lt(#hour24,6),0)\">");clock(x,d,"time/",d.color);x.append("</Group><Group visibility=\"gt(gt(#hour24,17)+lt(#hour24,6),0)\">");clock(x,d,"night/",0xffffd7b5);x.append("</Group>");
        }else clock(x,d,"time/",d.color);
        x.append("</Group>");
        if(d.depth&&d.background!=2)x.append("<Image src=\"subject.png\" x=\"#screen_width/2-540*").append(d.subjectScale).append("/100+").append(d.subjectX).append("\" y=\"(#screen_height-#screen_height*").append(d.subjectScale).append("/100)/2+").append(d.subjectY).append("\" w=\"1080*").append(d.subjectScale).append("/100\" h=\"#screen_height*").append(d.subjectScale).append("/100\"/>");
        x.append("<Button h=\"#screen_height\" w=\"#screen_width\"><Triggers><Trigger action=\"up\"><ExternCommand command=\"unlock\" condition=\"gt(#touch_begin_y-#touch_y,440)\"/></Trigger>");
        if(d.tapHide)x.append("<Trigger action=\"double\"><VariableCommand name=\"studio_hidden\" expression=\"1-#studio_hidden\"/></Trigger>");
        return x.append("</Triggers></Button></Lockscreen>").toString();
    }
    private static void clock(StringBuilder x,Design d,String prefix,int ink){
        int c=cell(d),gh=glyphHeight(d),colon=Math.max(20,(int)(d.size*.30f)),gap=d.spacing;
        String hour=d.twelve?"ifelse(eq(#hour12,0),12,#hour12)":"#hour24";
        x.append("<Group>");
        if(d.motion==1)x.append("<PositionAnimation><Item x=\"0\" y=\"0\" time=\"0\"/><Item x=\"0\" y=\"-18\" time=\"1800\"/><Item x=\"0\" y=\"0\" time=\"3600\"/></PositionAnimation>");
        if(d.motion==2)x.append("<AlphaAnimation><Item value=\"255\" time=\"0\"/><Item value=\"165\" time=\"2200\"/><Item value=\"255\" time=\"4400\"/></AlphaAnimation>");
        String[] numbers={"int(("+hour+")/10)","int(("+hour+")%10)","int(#minute/10)","int(#minute%10)"};
        for(int i=0;i<4;i++){
            int ox=d.stacked?(i%2)*(c+gap):(i<2?i*(c+gap):i*(c+gap)+colon),oy=d.stacked&&i>=2?gh+gap:0;
            x.append("<Image src=\"").append(prefix).append("t.png\" srcid=\"").append(numbers[i]).append("\" x=\"#clock_x+").append(ox).append("\" y=\"#clock_y+").append(oy).append("\"/>");
        }
        if(!d.stacked)x.append("<Image src=\"").append(prefix).append("colon.png\" x=\"#clock_x+").append((c+gap)*2).append("\" y=\"#clock_y\"/>");
        int y=gh*(d.stacked?2:1)+(d.stacked?gap:0)+45;
        if(d.date){x.append("<DateTime x=\"#screen_width/2+").append(d.horizontal).append("\" y=\"#clock_y+").append(y).append("\" color=\"").append(color(ink)).append("\" size=\"").append(d.dateSize).append("\" format=\"").append(DATE_FORMATS[Math.max(0,Math.min(4,d.dateFormat))]).append("\" align=\"center\" alignV=\"center\"/>");y+=d.dateSize+20;}
        if(d.seconds){x.append("<DateTime x=\"#screen_width/2+").append(d.horizontal).append("\" y=\"#clock_y+").append(y).append("\" size=\"32\" color=\"").append(color(ink)).append("\" format=\"ss\" align=\"center\"/>");y+=48;}
        if(d.battery){x.append("<Text x=\"#screen_width/2+").append(d.horizontal).append("\" y=\"#clock_y+").append(y).append("\" size=\"30\" color=\"").append(color(ink)).append("\" align=\"center\" format=\"%d%% battery\" paras=\"#battery_level\"/>");y+=46;}
        if(!d.customText.isEmpty())x.append("<Text x=\"#screen_width/2+").append(d.horizontal).append("\" y=\"#clock_y+").append(y).append("\" size=\"32\" color=\"").append(color(ink)).append("\" text=\"").append(esc(d.customText)).append("\" align=\"center\"/>");
        x.append("</Group>");
    }
    public static Bitmap subject(File file,int height)throws IOException{
        Bitmap mask=Bitmap.createBitmap(1080,height,Bitmap.Config.ARGB_8888);
        BitmapFactory.Options o=new BitmapFactory.Options();o.inJustDecodeBounds=true;BitmapFactory.decodeFile(file.getPath(),o);
        int sample=1;while(Math.max(o.outWidth,o.outHeight)/sample>3200)sample*=2;o.inJustDecodeBounds=false;o.inSampleSize=sample;
        Bitmap b=BitmapFactory.decodeFile(file.getPath(),o);if(b==null){mask.recycle();throw new IOException("Cannot decode subject PNG");}
        Canvas c=new Canvas(mask);float scale=Math.max(1080f/b.getWidth(),height/(float)b.getHeight());float w=b.getWidth()*scale,h=b.getHeight()*scale;
        c.drawBitmap(b,null,new RectF((1080-w)/2,(height-h)/2,(1080+w)/2,(height+h)/2),new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG));b.recycle();return mask;
    }
    public static byte[] build(Design d,File photo)throws IOException{return build(d,photo,null);}
    public static byte[] build(Design d,File photo,File subject)throws IOException{
        if(!d.v2.isEmpty())return StudioTheme.build(d,photo,subject);
        Bitmap bg=wallpaper(photo,d.height);byte[] jpg=image(bg,true);bg.recycle();
        Map<String,byte[]> lock=new LinkedHashMap<>();String xml=layout(d);
        lock.put("advance/manifest.xml",xml.getBytes("UTF-8"));
        for(String ratio:new String[]{"11x5","13x6","19x9","20x9","2x1","301x135","403x180","67x30"})lock.put("advance/"+ratio+"/manifest.xml",xml.getBytes("UTF-8"));
        if(d.background!=2)lock.put("advance/bg.jpg",jpg);
        Bitmap black=Bitmap.createBitmap(1,1,Bitmap.Config.ARGB_8888);black.eraseColor(Color.BLACK);lock.put("advance/dim.png",image(black,false));black.recycle();
        for(int pass=0;pass<(d.nightColor?2:1);pass++){
            Design ink=d.copy();if(pass==1)ink.color=0xffffd7b5;String prefix=pass==0?"time/":"night/";
            for(char n='0';n<='9';n++){Bitmap g=glyph(n,ink);lock.put("advance/"+prefix+"t_"+n+".png",image(g,false));g.recycle();}
            Bitmap colon=glyph(':',ink);lock.put("advance/"+prefix+"colon.png",image(colon,false));colon.recycle();
        }
        if(d.depth&&d.background!=2){if(subject==null)throw new IOException("Import a transparent subject PNG before enabling depth");Bitmap cutout=subject(subject,d.height);lock.put("advance/subject.png",image(cutout,false));cutout.recycle();}
        return packageTheme(wallpaperComponent(jpg,d.background!=0),zip(lock));
    }
    public static byte[] wallpaperComponent(byte[] image,boolean preserve)throws IOException{
        Map<String,byte[]> wall=new LinkedHashMap<>();if(!preserve){wall.put("_default_wallpaper.jpg",image);wall.put("_default_keyguard_wallpaper.jpg",image);}return zip(wall);
    }
    public static byte[] packageTheme(byte[] wallpaperZip,byte[] lockZip)throws IOException {
        String id=UUID.randomUUID().toString().toUpperCase(Locale.US);
        String info="<?xml version=\"1.0\" encoding=\"UTF-8\"?><OppoSmartPhoneThemeInfo><Summary>Lockscreen Studio</Summary><Author>Lockscreen Studio</Author><EditorVersion>6</EditorVersion><UUID>"+id+"</UUID><VersionCode>1</VersionCode><PackageName>"+id+"</PackageName><Description>Personal wallpaper and clock design</Description><IsGlobalTheme>0</IsGlobalTheme><lockState>0</lockState><disableLockPictorial>0</disableLockPictorial><enableLauncherApplyEffect>0</enableLauncherApplyEffect><package name=\"wallpaper\" version=\"\"/><package name=\"lock/lockstyle\" version=\"\"/></OppoSmartPhoneThemeInfo>";
        Map<String,byte[]> out=new LinkedHashMap<>();out.put("themeInfo.xml",info.getBytes("UTF-8"));out.put("wallpaper",wallpaperZip);out.put("lock/lockstyle",lockZip);
        // This path is for the native apply service; Theme Store's preview importer is not used.
        return zip(out);
    }
}
