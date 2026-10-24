package np.evan.lockscreen;
import android.graphics.*;
import org.json.*;
import java.io.*;
import java.util.*;
import java.util.zip.*;

/** Native counterpart of the user's Studio V2 document, with no running editor required. */
public final class StudioTheme {
 public static final String[] FONTS={"inter","outfit","manrope","sora","lexend","poppins","spacegrotesk","nunito","bebas","anton","archivoblack","bungee","righteous","unbounded","bigshoulders","syne","fraunces","playfair","dmserif","cormorant","robotoslab","abril","jetbrains","spacemono","major","orbitron","audiowide","chakra","rubikmono","barlow"};
 public static final String[] LAYOUTS={"single","stacked","stairs","hero","split","grid","tower","vertical","arc","words","bigmin","diagonal","wave","offset","wide","analog","rings"};
 public static final String[] FINISHES={"solid","glow","outline","neon","gradient","shadow","glass","outlineglow","duo","retro","emboss","chrome","holo","splittone","halftone","glitch","stencil","photo"};
 /** Layouts that are already single-digit shapes, or a dial, cannot be split into hours/minutes/date pieces. */
 public static final List<String> NO_SPLIT=Arrays.asList("grid","tower","vertical","arc","analog","rings");
 /** Layouts drawn as a dial or along a curve/edge are not stretched. */
 public static final List<String> NO_STRETCH=Arrays.asList("vertical","arc","analog","rings");
 /** Charging styles that redraw the digits themselves only apply to digit layouts. */
 /** Layouts without digit images: no colour shift/fill, weight flow or per-digit depth. */
 public static final List<String> NO_DIGITS=Arrays.asList("analog","words","rings");
 /** Per-digit depth needs separate digit images in straight layouts (Arc is curved text in the preview). */
 public static final List<String> NO_DEPTH=Arrays.asList("arc","analog","words","rings");
 public static final List<String> CHARGES=Arrays.asList("none","glow","color","fill","ring","bar");
 /** True while charging or full on the charger (MAML battery_state 1 or 3). */
 public static final String CHARGING="gt(eq(#battery_state,1)+eq(#battery_state,3),0)";
 public static final String[] ONES={"zero","one","two","three","four","five","six","seven","eight","nine","ten","eleven","twelve","thirteen","fourteen","fifteen","sixteen","seventeen","eighteen","nineteen"};
 public static final class Spec {
  public final JSONObject j;
  /** Still background for photo-filled digits (null for live wallpaper or black), the screen height, and the depth pass (0 all, 1 behind subject, 2 in front). */
  public Bitmap backdrop;public int screenH=2400,layer;
  public Spec(JSONObject value){j=value;}
  private Spec derive(JSONObject m){Spec o=new Spec(m);o.backdrop=backdrop;o.screenH=screenH;o.layer=layer;return o;}
  public Spec weighted(int w){try{JSONObject m=new JSONObject(j.toString());m.put("weight",w);return derive(m);}catch(JSONException e){return this;}}
  public boolean photo(){return finish().equals("photo")&&backdrop!=null;}
  /** Six weights from light to heavy, one per ten minutes of the hour, or null when off. */
  public int[] flow(){JSONObject f=j.optJSONObject("weightFlow");if(f==null||!f.optBoolean("on",false)||NO_DIGITS.contains(layout())||photo())return null;int a=Spec.n(f,"from",200,100,900),b=Spec.n(f,"to",900,100,900),w[]=new int[6];for(int i=0;i<6;i++)w[i]=Math.round(a+(b-a)*i/5f);return w;}
  public boolean front(int pos){JSONArray a=j.optJSONArray("depthFront");return a!=null&&!NO_DEPTH.contains(layout())&&a.optBoolean(pos,false);}
  public boolean anyFront(){for(int i=0;i<4;i++)if(front(i))return true;return false;}
  /** Whether this pass draws the digit at a position: the back pass skips front digits, the front pass draws only them. */
  public boolean draws(int pos){return layer==0||(layer==1)!=front(pos);}
  /** Clock colours for light mode (dark theme off), or null when the light set is off. */
  public int[] lightInks(){JSONObject l=j.optJSONObject("lightMode");if(l==null||!l.optBoolean("on",false))return null;String a=l.optString("color","#1d1d24"),b=l.optString("color2","#4b4b60");int c=a.matches("#[a-fA-F0-9]{6}")?Color.parseColor(a):0xff1d1d24,d=b.matches("#[a-fA-F0-9]{6}")?Color.parseColor(b):0xff4b4b60;return new int[]{c,b("twoTone",false)?d:c};}
  public int n(String k,int def,int lo,int hi){return Math.max(lo,Math.min(hi,j.optInt(k,def)));}
  public static int n(JSONObject o,String k,int def,int lo,int hi){return Math.max(lo,Math.min(hi,o==null?def:o.optInt(k,def)));}
  public boolean split(){return b("split",false)&&!NO_SPLIT.contains(layout());}
  /** The date block is its own movable piece when the clock is split or the user detached it. */
  public boolean dateApart(){return split()||b("detachDate",false);}
  public double stretch(JSONObject o,String k){return NO_STRETCH.contains(layout())?1:n(o,k,100,50,200)/100d;}
  public String charge(){String c=s("charge","none");return CHARGES.contains(c)?c:"none";}
  public int chargeInk(){String v=s("chargeColor","#7be0b0");return v.matches("#[a-fA-F0-9]{6}")?Color.parseColor(v):0xff7be0b0;}
  /** Partial overrides (font, weight, finish, colour) for one part; falls back to the shared style. */
  public Spec part(String key){JSONObject o=j.optJSONObject(key);if(o==null||o.length()==0)return this;try{JSONObject m=new JSONObject(j.toString());for(String k:new String[]{"font","weight","finish","color"})if(o.has(k))m.put(k,o.get(k));return derive(m);}catch(JSONException e){return this;}}
  public Integer override(String key){JSONObject o=j.optJSONObject(key);String v=o==null?"":o.optString("color","");return v.matches("#[a-fA-F0-9]{6}")?Color.parseColor(v):null;}
  public boolean b(String k,boolean def){return j.optBoolean(k,def);}
  public String s(String k,String def){return j.optString(k,def);}
  public String choice(String k,String def,String... values){String v=s(k,def);return Arrays.asList(values).contains(v)?v:def;}
  public String font(){return choice("font","inter",FONTS);}
  public String layout(){return choice("layout","stacked",LAYOUTS);}
  public String finish(){return choice("finish","solid",FINISHES);}
  public int size(){return n("size",240,80,600);}
  public int color(boolean minutes){String v=s(minutes&&b("twoTone",false)?"color2":"color","#ffffff");return v.matches("#[a-fA-F0-9]{6}")?Color.parseColor(v):0xffffffff;}
  public int secondary(){String v=s("color2","#bba7ff");return v.matches("#[a-fA-F0-9]{6}")?Color.parseColor(v):0xffbba7ff;}
  public boolean info(String k){JSONObject i=j.optJSONObject("info");return i!=null&&i.optBoolean(k,false);}
  public String night(){int a=n("warmFrom",18,0,23),b=n("warmTo",6,0,23);if(a==b)return "0";return a>b?"gt(gt(#hour,"+(a-1)+")+lt(#hour,"+b+"),0)":"gt(#hour,"+(a-1)+")*lt(#hour,"+b+")";}
 }
 public static final class Piece {
  public int digit;public double x,y,scale,rotation;public boolean minutes,colon;
  Piece(int digit,double x,double y,double scale,boolean minutes){this.digit=digit;this.x=x;this.y=y;this.scale=scale;this.minutes=minutes;}
 }
 public static final class Geometry {
  public double width,height;public final List<Piece> pieces=new ArrayList<>();public boolean divider=false,words=false,analog=false,rings=false;public double rotate=0;
 }
 public static Geometry geometry(String layout,int size,int spacing,int line,int minScale,int arc){
  Geometry g=new Geometry();double c=size*.62,h=size*1.28,gap=size*spacing*.01,step=size*line*.01,ms=minScale*.01;
  if(layout.equals("hero"))ms*=.4;
  if(layout.equals("words")){g.words=true;g.width=size*3.6;g.height=size*1.13;return g;}
  if(layout.equals("analog")){g.analog=true;g.width=g.height=size*1.8;return g;}
  if(layout.equals("rings")){g.rings=true;g.width=g.height=size*1.8;return g;}
  for(int i=0;i<4;i++){
   boolean min=i>=2;double x=0,y=0,scale=1;
   if(layout.equals("stacked")||layout.equals("stairs")){scale=min?ms:1;x=(i%2)*(c+gap)*scale+(min&&layout.equals("stairs")?size*.55:0);y=min?step:0;}
   else if(layout.equals("grid")){x=(i%2)*(c+size*.28+gap);y=i>=2?step:0;}
   else if(layout.equals("tower")){x=0;y=i*step;}
   else if(layout.equals("bigmin")){double hs=.45;scale=min?ms:hs;x=min?2*(c+gap)*hs+size*.06+(i-2)*(c+gap)*ms:i*(c+gap)*hs;y=(h-h*scale)*.5;}
   else if(layout.equals("diagonal")){scale=min?ms:1;double w=c*.8+gap;x=min?2*w+(i-2)*w*ms:i*w;y=i*step*.32;}
   else if(layout.equals("wave")){scale=min?ms:1;x=min?2*(c+gap)+(i-2)*(c+gap)*ms:i*(c+gap);y=(i%2)*step*.3;}
   else if(layout.equals("offset")){scale=min?ms:1;x=min?2*(c+gap)+(i-2)*(c+gap)*ms:i*(c+gap);y=min?step:0;}
   else if(layout.equals("wide")){scale=min?ms:1;x=min?2*(c+gap)+size*.9+(i-2)*(c+gap)*ms:i*(c+gap);y=(h-h*scale)*.5;}
   else if(layout.equals("arc")){double index=i<2?i:i+1,t=index/4d;x=index*(c+gap);y=4*(1-t)*t*(-arc*.9);}
   else {scale=min?ms:1;double separator=layout.equals("hero")?size*.05:layout.equals("split")?size*.25:size*.3;x=min?2*(c+gap)+separator+(i-2)*(c+gap)*scale:i*(c+gap);y=(h-h*scale)*.5;}
   Piece p=new Piece(i,x,y,scale,min);if(layout.equals("arc")){double t=(i<2?i:i+1)/4d;p.rotation=Math.toDegrees(Math.atan((1-2*t)*(-arc*.9)/(c+gap)));}g.pieces.add(p);
  }
  if(Arrays.asList("single","vertical","arc").contains(layout)){Piece p=new Piece(-1,2*(c+gap),0,1,false);p.colon=true;if(layout.equals("arc"))p.y=-arc*.9;g.pieces.add(p);}
  if(layout.equals("split"))g.divider=true;
  double minX=0,minY=0,maxX=0,maxY=0;for(Piece p:g.pieces){minX=Math.min(minX,p.x);minY=Math.min(minY,p.y);maxX=Math.max(maxX,p.x+(p.colon?size*.3:c)*p.scale);maxY=Math.max(maxY,p.y+h*p.scale);}
  for(Piece p:g.pieces){p.x-=minX;p.y-=minY;}g.width=maxX-minX;g.height=maxY-minY;if(layout.equals("vertical"))g.rotate=270;return g;
 }
 private static String fmt(double v){return String.format(Locale.US,"%.3f",v);}
 private static String esc(String s){return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;").replace("'","&apos;");}
 private static byte[] png(Bitmap b)throws IOException{ByteArrayOutputStream o=new ByteArrayOutputStream();if(!b.compress(Bitmap.CompressFormat.PNG,100,o))throw new IOException("Image encoding failed");return o.toByteArray();}
 private static byte[] zip(Map<String,byte[]> files)throws IOException{ByteArrayOutputStream o=new ByteArrayOutputStream();try(ZipOutputStream z=new ZipOutputStream(o)){for(Map.Entry<String,byte[]> e:files.entrySet()){z.putNextEntry(new ZipEntry(e.getKey()));z.write(e.getValue());z.closeEntry();}}return o.toByteArray();}
 private static int mix(int a,int b,float t){return Color.rgb(Math.round(Color.red(a)*(1-t)+Color.red(b)*t),Math.round(Color.green(a)*(1-t)+Color.green(b)*t),Math.round(Color.blue(a)*(1-t)+Color.blue(b)*t));}
 private static String words(int n){return n<20?ONES[n]:new String[]{"","","twenty","thirty","forty","fifty"}[n/10]+(n%10==0?"":"-"+ONES[n%10]);}
 private static String minuteWords(int n){return n==0?"o'clock":n<10?"oh "+ONES[n]:words(n);}
 private static Paint paint(Spec s,int ink,int second,double size,boolean date)throws IOException{
  Paint p=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);String family=date?s.choice("dateFont",s.font(),FONTS):s.font();if(date&&s.s("dateFont","inter").equals("same"))family=s.font();
  JSONObject styleD=s.j.optJSONObject("styleD");int dateWeight=styleD!=null&&styleD.has("weight")?Spec.n(styleD,"weight",500,100,900):Math.min(600,s.n("weight",500,100,900));
  p.setTypeface(StudioFonts.face(family,date?dateWeight:s.n("weight",800,100,900)));p.setTextSize((float)size);p.setColor(ink);p.setFontFeatureSettings("tnum");
  if(!date){p.setTextSkewX((float)-Math.tan(Math.toRadians(s.n("slant",0,0,20))));float glow=s.n("glow",60,0,100)/100f;
   switch(s.finish()){
    case "glow":p.setShadowLayer((float)size*(.015f+.1f*glow),0,0,ink);break;
    case "shadow":p.setShadowLayer((float)size*.06f,0,(float)size*.045f,0xb0000000);break;
    case "outline":p.setStyle(Paint.Style.STROKE);p.setStrokeWidth((float)size*.022f);break;
    case "neon":p.setShadowLayer((float)size*(.025f+.16f*glow),0,0,ink);p.setColor(Color.WHITE);break;
    case "gradient":p.setShader(new LinearGradient(0,0,0,(float)(size*1.25),ink,second,Shader.TileMode.CLAMP));break;
    case "glass":p.setColor((ink&0xffffff)|0x34000000);break;
    case "outlineglow":p.setStyle(Paint.Style.STROKE);p.setStrokeWidth((float)size*.022f);p.setShadowLayer((float)size*(.02f+.1f*glow),0,0,ink);break;
    case "halftone":{int t=Math.max(4,(int)Math.round(size*.07));Bitmap dot=Bitmap.createBitmap(t,t,Bitmap.Config.ARGB_8888);Paint d=new Paint(Paint.ANTI_ALIAS_FLAG);d.setColor(ink);new Canvas(dot).drawCircle(t/2f,t/2f,t*.4f,d);p.setShader(new BitmapShader(dot,Shader.TileMode.REPEAT,Shader.TileMode.REPEAT));break;}
   }
  }return p;
 }
 /** Finish details that need the canvas size or several drawing passes. Null for date and caption text. */
 static final class Look {final String finish;final int ink,second;final float size;Look(String finish,int ink,int second,double size){this.finish=finish;this.ink=ink;this.second=second;this.size=(float)size;}}
 private static Look look(Spec s,int ink,int second,double size){return new Look(s.finish(),ink,second,size);}
 private static void line(Canvas c,String text,float x,float y,Paint p,double tracking){if(tracking==0){c.drawText(text,x,y,p);return;}for(int i=0;i<text.length();){int count=Character.charCount(text.codePointAt(i));String ch=text.substring(i,i+count);c.drawText(ch,x,y,p);x+=p.measureText(ch)+tracking;i+=count;}}
 private static Paint plain(Paint p,int color){Paint q=new Paint(p);q.setShader(null);q.clearShadowLayer();q.setStyle(Paint.Style.FILL);q.setColor(color);return q;}
 private static Bitmap text(String text,Paint p,int w,int h,double tracking,boolean upper,Look look){
  Bitmap b=Bitmap.createBitmap(Math.max(1,w),Math.max(1,h),Bitmap.Config.ARGB_8888);Canvas c=new Canvas(b);if(upper)text=text.toUpperCase(Locale.US);
  Paint.FontMetrics fm=p.getFontMetrics();float total=p.measureText(text)+(float)tracking*Math.max(0,text.length()-1),x=(w-total)/2f,y=(h-fm.bottom-fm.top)/2;
  String finish=look==null?"":look.finish;
  if(finish.equals("chrome")||finish.equals("holo")||finish.equals("splittone")){
   p=new Paint(p);float top=h/2f-look.size*.45f,bottom=h/2f+look.size*.45f;
   if(finish.equals("chrome"))p.setShader(new LinearGradient(0,top,0,bottom,new int[]{mix(look.ink,0xffffffff,.8f),look.ink,mix(look.ink,0xff000000,.5f),mix(look.ink,0xffffffff,.9f),look.ink},new float[]{0,.35f,.5f,.62f,1},Shader.TileMode.CLAMP));
   else if(finish.equals("holo"))p.setShader(new LinearGradient(x,0,x+total,0,new int[]{0xffff5f6d,0xffffc371,0xff47e5bc,0xff5f9dff,0xffc86bff},null,Shader.TileMode.CLAMP));
   else p.setShader(new LinearGradient(0,top,0,bottom,new int[]{look.ink,look.ink,look.second,look.second},new float[]{0,.5f,.5f,1},Shader.TileMode.CLAMP));
  }
  switch(finish){
   case "retro":{float o=look.size*.012f;Paint q=plain(p,mix(look.second,0xff000000,.35f));for(int k=7;k>0;k--)line(c,text,x+k*o,y+k*o,q,tracking);break;}
   case "emboss":{float o=look.size*.014f;line(c,text,x+o,y+o,plain(p,0x8c000000),tracking);line(c,text,x-o,y-o,plain(p,0x80ffffff),tracking);break;}
   case "glitch":{float o=look.size*.028f;line(c,text,x-o,y,plain(p,0xccff2a55),tracking);line(c,text,x+o,y,plain(p,0xcc22e5ff),tracking);break;}
   case "stencil":{
    // A solid plate with the glyph cleared, so the wallpaper shows through the digit.
    Paint plate=new Paint(Paint.ANTI_ALIAS_FLAG);plate.setColor(look.ink);float m=look.size*.06f,r=look.size*.1f;c.drawRoundRect(m,m,w-m,h-m,r,r,plate);
    Paint cut=plain(p,0xff000000);cut.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));line(c,text,x,y,cut,tracking);return b;
   }
  }
  line(c,text,x,y,p,tracking);
  if(finish.equals("duo")){Paint q=plain(p,look.second);q.setStyle(Paint.Style.STROKE);q.setStrokeWidth(look.size*.03f);line(c,text,x,y,q,tracking);}
  if(finish.equals("glass")){Paint border=new Paint(p);border.setShader(null);border.setStyle(Paint.Style.STROKE);border.setStrokeWidth(Math.max(1,p.getTextSize()*.012f));border.setColor(0xbfffffff);line(c,text,x,y,border,tracking);}
  return b;
 }
 private static void store(Map<String,byte[]> files,String name,Bitmap b)throws IOException{try{files.put("advance/"+name,png(b));}finally{b.recycle();}}
 private static void image(StringBuilder x,String path,String srcid,String px,String py,String w,String h,String extra){x.append("<Image src=\"").append(path).append("\"");if(srcid!=null)x.append(" srcid=\"").append(esc(srcid)).append("\"");x.append(" x=\"").append(esc(px)).append("\" y=\"").append(esc(py)).append("\"");if(w!=null)x.append(" w=\"").append(w).append("\" h=\"").append(h).append("\"");x.append(extra==null?"":extra).append("/>");}
 private static String alpha(Spec s){return fmt(s.n("opacity",100,10,100)*2.55)+"*(1-("+s.night()+")*"+fmt(s.n("nightDim",0,0,70)/100d)+")"+(s.b("lowBatDim",false)?"*ifelse(lt(#battery_level,20),0.6,1)":"");}
 /** Opens one movable piece, centred on the editor's hpos/vpos percentage mapping and rotated about its centre. */
 /** Parallax source: the gravity sensor, a finger dragging across the lock screen, or both. */
 static boolean gyro(Spec s){String m=s.s("movement","still");return m.equals("tilt")||m.equals("parallax")&&!s.s("parallaxSrc","gyro").equals("drag");}
 static boolean drag(Spec s){return s.s("movement","still").equals("parallax")&&!s.s("parallaxSrc","gyro").equals("gyro");}
 /** Offset added to a placed group: the clock follows the tilt or the finger; the photo plane (scale<0) drifts the other way. */
 static String motion(Spec s,boolean horizontal,double scale){
  StringBuilder o=new StringBuilder();
  if(gyro(s))o.append("+(").append(fmt(scale)).append(")*max(-32,min(32,#studio_gravity_").append(horizontal?"x*5":"y*3").append("))");
  if(drag(s))o.append("+(").append(fmt(scale)).append(")*#studio_drag*max(-").append(horizontal?"60,min(60,(#touch_x-#touch_begin_x)*0.25":"30,min(30,(#touch_y-#touch_begin_y)*0.1").append("))");
  return o.toString();
 }
 private static void open(StringBuilder x,Spec s,String name,int hpos,int vpos,double rotation,double width,double height){
  String move=s.choice("movement","still","still","breathe","float","tilt","parallax"),tx=motion(s,true,1),ty=motion(s,false,1);
  x.append("<Group name=\"").append(name).append("\" x=\"#screen_width*").append(fmt(.5+hpos*.006)).append('-').append(fmt(width/2)).append(tx).append("\" y=\"#screen_height*").append(fmt(.12+vpos*.011)).append('-').append(fmt(height/2)).append(ty).append("\" rotation=\"").append(fmt(rotation)).append("\" pivotX=\"").append(fmt(width/2)).append("\" pivotY=\"").append(fmt(height/2)).append("\" alpha=\"").append(esc(alpha(s))).append("\">");
  if(move.equals("float"))x.append("<PositionAnimation><Position x=\"0\" y=\"0\" time=\"0\"/><Position x=\"0\" y=\"-24\" time=\"3500\"/><Position x=\"0\" y=\"0\" time=\"7000\"/></PositionAnimation>");
  if(move.equals("breathe"))x.append("<AlphaAnimation><Alpha a=\"255\" time=\"0\"/><Alpha a=\"178\" time=\"2500\"/><Alpha a=\"255\" time=\"5000\"/></AlphaAnimation>");
 }
 private static int metaSize(Spec s){return s.n("dateSize",38,20,120);}
 private static double metaWidth(Spec s){return Math.min(1040,metaSize(s)*23);}
 private static double metaHeight(Spec s){int dateH=(int)(metaSize(s)*1.5),infoH=(s.info("battery")||s.info("alarm")||s.info("weather")||s.info("progress"))?52:0;return (s.b("showDate",true)?dateH:0)+(!s.s("caption","").isEmpty()?dateH*.8:0)+infoH+(s.b("seconds",false)?38:0)+20;}
 /** Seconds, date, caption and info chips, stacked from y inside a block of the given width. */
 private static void meta(StringBuilder x,Spec s,String prefix,int ink,int ink2,double width,double y,Map<String,byte[]> files)throws Exception{
  int metaSize=metaSize(s),dateH=(int)(metaSize*1.5);
  if(s.b("seconds",false)){x.append("<DateTime x=\"").append(fmt(width/2)).append("\" y=\"").append(fmt(y)).append("\" size=\"30\" color=\"").append(String.format(Locale.US,"#%06X",ink&0xffffff)).append("\" format=\"ss\" align=\"center\"/>");y+=38;}
  if(s.s("infoPos","below").equals("above"))y=info(x,s,prefix,ink,width,y,files);
  if(s.b("showDate",true))y=date(x,s,prefix,ink,width,y,files);
  String caption=s.s("caption","");if(!caption.isEmpty()){if(caption.length()>60)caption=caption.substring(0,60);Paint cp=paint(s,ink,ink2,metaSize*.8,true);store(files,prefix+"caption.png",text(caption,cp,(int)width,(int)(dateH*.8),metaSize*s.n("dateTrack",0,0,30)*.01,s.s("dateCase","normal").equals("upper"),null));image(x,prefix+"caption.png",null,"0",fmt(y),null,null,null);y+=dateH*.8;}
  if(!s.s("infoPos","below").equals("above"))info(x,s,prefix,ink,width,y,files);
 }
 private static int warmed(int c,boolean warm){return warm?mix(c,0xffffb070,.6f):c;}
 /** Digit images for one part: the normal set, plus the charging variants the chosen style needs. */
 /** Digit images for one part: the normal set (six weights when the weight follows the hour), plus the charging variants. */
 private static void digitSet(Spec ps,String prefix,String name,int ink,int endInk,int size,String charge,int chargeInk,Matrix toScreen,int maxDigit,Map<String,byte[]> files)throws Exception{
  int cell=(int)Math.ceil(size*.62),glyphH=(int)Math.ceil(size*1.28),pad=(int)Math.ceil(size*.22),cw=cell+pad*2,ch=glyphH+pad*2;
  Paint paint=paint(ps,ink,endInk,size,false);int[] flow=toScreen==null?ps.flow():null;
  if(flow!=null)for(int k=0;k<6;k++){Spec w=ps.weighted(flow[k]);Paint wp=paint(w,ink,endInk,size,false);for(int d=0;d<10;d++)store(files,prefix+name+"_"+(k*10+d)+".png",text(""+d,wp,cw,ch,0,false,look(w,ink,endInk,size)));}
  else{if(toScreen!=null)photoFill(paint,ps.backdrop,toScreen);for(int d=0;d<=maxDigit;d++)store(files,prefix+name+"_"+d+".png",text(""+d,paint,cw,ch,0,false,toScreen==null?look(ps,ink,endInk,size):null));}
  if(charge.equals("color")){Paint cp=paint(ps,chargeInk,chargeInk,size,false);for(int d=0;d<=maxDigit;d++)store(files,prefix+name+"c_"+d+".png",text(""+d,cp,cw,ch,0,false,look(ps,chargeInk,chargeInk,size)));}
  if(charge.equals("fill")){
   // Five fill levels per digit: the digit dimmed, with its lower part in the charging colour up to the battery level.
   Paint base=plain(paint,(ink&0xffffff)|0x59000000),fill=plain(paint,chargeInk);
   for(int d=0;d<=maxDigit;d++)for(int l=0;l<5;l++){Bitmap b=text(""+d,base,cw,ch,0,false,null);Canvas c=new Canvas(b);float top=ch/2f+size*.45f-size*.9f*(l+1)/5f;c.save();c.clipRect(0,top,cw,ch);Bitmap f=text(""+d,fill,cw,ch,0,false,null);c.drawBitmap(f,0,0,null);f.recycle();c.restore();store(files,prefix+name+"f_"+(d*5+l)+".png",b);}
  }
 }
 /** The background photo seen through the digit, lifted a little so the digits read as lit windows. */
 private static void photoFill(Paint p,Bitmap bg,Matrix toScreen){
  BitmapShader shader=new BitmapShader(bg,Shader.TileMode.CLAMP,Shader.TileMode.CLAMP);Matrix inverse=new Matrix();if(toScreen.invert(inverse))shader.setLocalMatrix(inverse);
  p.setShader(shader);p.setColor(Color.WHITE);p.clearShadowLayer();p.setStyle(Paint.Style.FILL);
  ColorMatrix m=new ColorMatrix();m.setSaturation(1.15f);m.postConcat(new ColorMatrix(new float[]{1.3f,0,0,0,10,0,1.3f,0,0,10,0,0,1.3f,0,10,0,0,0,1,0}));p.setColorFilter(new ColorMatrixColorFilter(m));
 }
 /** Screen transform of a placed group, matching open(): rotation about its centre, then its position. */
 private static Matrix group(Spec s,int hpos,int vpos,double rot,double width,double height){Matrix m=new Matrix();m.setRotate((float)rot,(float)width/2,(float)height/2);m.postTranslate((float)(1080*(.5+hpos*.006)-width/2),(float)(s.screenH*(.12+vpos*.011)-height/2));return m;}
 /** Maps a digit image's own pixels to the screen: its box, its rotation, then the enclosing groups. */
 private static Matrix toScreen(int cw,int ch,double ix,double iy,double w,double h,double rot,Matrix parent){Matrix m=new Matrix();m.setScale((float)(w/cw),(float)(h/ch));if(rot!=0)m.postRotate((float)rot,(float)w/2,(float)h/2);m.postTranslate((float)ix,(float)iy);m.postConcat(parent);return m;}
 /** Highest digit a position can show (hour tens 0-2, minute tens 0-5); photo digits are drawn per position. */
 private static int maxDigit(Spec s,int pos){return pos==0?(s.b("h12",false)?1:2):pos==2?5:9;}
 /** One digit image (plus charging variants) at a position; fx/fy stretch width and height independently. */
 private static void digit(StringBuilder x,String prefix,String name,String id,double px,double py,double scale,double fx,double fy,int size,String charge,String rot,boolean flow){
  int cell=(int)Math.ceil(size*.62),glyphH=(int)Math.ceil(size*1.28),pad=(int)Math.ceil(size*.22),cw=cell+pad*2,ch=glyphH+pad*2;
  String ix=fmt((px-pad*scale)*fx),iy=fmt((py-pad*scale)*fy),w=fmt(cw*scale*fx),h=fmt(ch*scale*fy);boolean swap=charge.equals("color")||charge.equals("fill");
  image(x,prefix+name+".png",flow?"("+id+")+10*int(#minute/10)":id,ix,iy,w,h,(swap?" visibility=\"eq("+CHARGING+",0)\"":"")+(rot==null?"":rot));
  if(charge.equals("color"))image(x,prefix+name+"c.png",id,ix,iy,w,h," visibility=\""+CHARGING+"\""+(rot==null?"":rot));
  if(charge.equals("fill"))image(x,prefix+name+"f.png","("+id+")*5+min(4,int(#battery_level/20))",ix,iy,w,h," visibility=\""+CHARGING+"\""+(rot==null?"":rot));
 }
 /** Charging halo or battery ring around a block of the given size (shown only while charging). */
 private static void chargeAround(StringBuilder x,String prefix,String charge,int chargeInk,double w,double h,Map<String,byte[]> files)throws Exception{
  if(charge.equals("glow")){
   int gw=(int)Math.ceil(w*1.5),gh=(int)Math.ceil(h*1.5);Bitmap b=Bitmap.createBitmap(Math.max(1,gw),Math.max(1,gh),Bitmap.Config.ARGB_8888);Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
   p.setShader(new RadialGradient(gw/2f,gh/2f,Math.max(gw,gh)/2f,new int[]{(chargeInk&0xffffff)|0x8c000000,(chargeInk&0xffffff)|0x30000000,chargeInk&0xffffff},new float[]{0,.55f,1},Shader.TileMode.CLAMP));
   Canvas c=new Canvas(b);c.scale(1,(float)gh/Math.max(1,gw),gw/2f,gh/2f);c.drawCircle(gw/2f,gh/2f,gw/2f,p);store(files,prefix+"charge_glow.png",b);
   x.append("<Image src=\"").append(prefix).append("charge_glow.png\" x=\"").append(fmt((w-gw)/2)).append("\" y=\"").append(fmt((h-gh)/2)).append("\" visibility=\"").append(CHARGING).append("\"><AlphaAnimation><Alpha a=\"255\" time=\"0\"/><Alpha a=\"110\" time=\"1600\"/><Alpha a=\"255\" time=\"3200\"/></AlphaAnimation></Image>");
  }
  if(charge.equals("ring")){
   double d=Math.max(w,h)*1.15;int di=(int)Math.ceil(d+24);float c=di/2f,r=(float)d/2f,sw=Math.max(6,(float)d*.025f);
   Paint track=new Paint(Paint.ANTI_ALIAS_FLAG);track.setStyle(Paint.Style.STROKE);track.setStrokeWidth(sw);track.setColor(0x26ffffff);Paint arc=new Paint(track);arc.setColor(chargeInk);arc.setStrokeCap(Paint.Cap.ROUND);arc.setShadowLayer(sw,0,0,chargeInk);
   RectF o=new RectF(c-r,c-r,c+r,c+r);
   for(int i=0;i<=20;i++){Bitmap b=Bitmap.createBitmap(di,di,Bitmap.Config.ARGB_8888);Canvas cv=new Canvas(b);cv.drawCircle(c,c,r,track);if(i>0)cv.drawArc(o,-90,i*18,false,arc);store(files,prefix+"charge_ring_"+i+".png",b);}
   image(x,prefix+"charge_ring.png","int(#battery_level/5)",fmt((w-di)/2),fmt((h-di)/2),null,null," visibility=\""+CHARGING+"\"");
  }
 }
 private static void time(StringBuilder x,Spec s,String prefix,int ink,int ink2,Map<String,byte[]> files)throws Exception{
  boolean warm=prefix.contains("/warm/");int endInk=warm?mix(s.secondary(),0xffff9a55,.6f):s.secondary();
  if(s.layer!=2){Bitmap tint=Bitmap.createBitmap(1,1,Bitmap.Config.ARGB_8888);tint.eraseColor(endInk);store(files,prefix+"ink.png",tint);}
  boolean lightSet=prefix.contains("/light/");Spec hs=s.part("styleH"),ms=s.part("styleM");Integer ho=lightSet?null:s.override("styleH"),mo=lightSet?null:s.override("styleM"),dov=lightSet?null:s.override("styleD");
  int hInk=ho==null?ink:warmed(ho,warm),mInk=mo==null?ink2:warmed(mo,warm),dInk=dov==null?ink:warmed(dov,warm);
  if(s.layer==2&&!s.anyFront())return;boolean main=s.layer!=2;
  String charge=s.charge(),name="studio_clock_"+prefix.replace('/','_')+(main?"":"f");int chargeInk=s.chargeInk();
  if(s.split())splitTime(x,s,hs,ms,prefix,name,hInk,mInk,endInk,charge,chargeInk,files);
  else{
   int size=s.size();Geometry g=geometry(s.layout(),size,s.n("spacing",0,-10,30),s.n("lh",88,60,140),s.n("minScale",100,30,130),s.n("arc",55,-100,100));
   if(s.layout().equals("stairs")&&s.s("align","center").equals("right"))for(Piece piece:g.pieces)piece.x=g.width-piece.x-size*.62*piece.scale;
   double fx=s.stretch(s.j,"sx"),fy=s.stretch(s.j,"sy");boolean digitsLayout=!g.analog&&!g.words&&!g.rings;String cs=digitsLayout?charge:(charge.equals("color")||charge.equals("fill")?"none":charge);
   int cell=(int)Math.ceil(size*.62),pad=(int)Math.ceil(size*.22),glyphH=(int)Math.ceil(size*1.28),ch=glyphH+pad*2;
   if(digitsLayout&&main){if(!hs.photo())digitSet(hs,prefix,"h",hInk,endInk,size,cs,chargeInk,null,9,files);if(!ms.photo())digitSet(ms,prefix,"m",mInk,endInk,size,cs,chargeInk,null,9,files);store(files,prefix+"colon.png",text(":",paint(hs,hInk,endInk,size,false),(int)(size*.3)+pad*2,ch,0,false,look(hs,hInk,endInk,size)));
    if(cs.equals("color"))store(files,prefix+"colonc.png",text(":",paint(hs,chargeInk,chargeInk,size,false),(int)(size*.3)+pad*2,ch,0,false,look(hs,chargeInk,chargeInk,size)));}
   String[] digits=digitIds(s);
   double gw=g.width*fx,gh=g.height*fy,timeWidth=g.rotate==0?gw:gh,timeHeight=g.rotate==0?gh:gw;boolean apart=s.dateApart();
   double width=apart?timeWidth:Math.max(timeWidth,metaWidth(s)),height=timeHeight+(apart?0:metaHeight(s));
   open(x,s,name,s.n("hpos",0,-80,80),s.n("vpos",14,-10,80),s.n("rotate",0,-180,180),width,height);
   double left=s.s("align","center").equals("left")?0:s.s("align","center").equals("right")?width-timeWidth:(width-timeWidth)/2;
   double innerX=left+(g.rotate==0?0:gh/2-gw/2),innerY=g.rotate==0?0:gw/2-gh/2;
   x.append("<Group x=\"").append(fmt(innerX)).append("\" y=\"").append(fmt(innerY)).append("\" rotation=\"").append(fmt(g.rotate)).append("\" pivotX=\"").append(fmt(gw/2)).append("\" pivotY=\"").append(fmt(gh/2)).append("\">");
   Matrix inner=new Matrix();inner.setRotate((float)g.rotate,(float)gw/2,(float)gh/2);inner.postTranslate((float)innerX,(float)innerY);inner.postConcat(group(s,s.n("hpos",0,-80,80),s.n("vpos",14,-10,80),s.n("rotate",0,-180,180),width,height));
   if(main)chargeAround(x,prefix,cs,chargeInk,gw,gh,files);
   if(g.analog)analog(x,s,prefix,hInk,mInk,endInk,size,files);
   else if(g.rings)rings(x,s,hs,ms,prefix,hInk,mInk,endInk,size,files);
   else if(g.words){
    int ww=(int)Math.ceil(g.width),hh=(int)Math.ceil(size*.65),mh=(int)Math.ceil(size*.43);Paint hp=paint(hs,hInk,endInk,size*.5,false),mp=paint(ms,mInk,endInk,size*.3,false);
    for(int i=0;i<24;i++)store(files,prefix+"wordhour_"+i+".png",text(ONES[i%12==0?12:i%12],hp,ww,hh,0,false,look(hs,hInk,endInk,size*.5)));
    for(int i=0;i<60;i++)store(files,prefix+"wordminute_"+i+".png",text(minuteWords(i),mp,ww,mh,0,false,look(ms,mInk,endInk,size*.3)));
    image(x,prefix+"wordhour.png","#hour","0","0",fmt(ww*fx),fmt(hh*fy),null);image(x,prefix+"wordminute.png","#minute","0",fmt(hh*fy),fmt(ww*fx),fmt(mh*fy),null);
   }else for(Piece p:g.pieces){
    String rot=p.rotation==0?null:" rotation=\""+fmt(p.rotation)+"\" pivotX=\""+fmt((cell+2*pad)*p.scale*fx/2)+"\" pivotY=\""+fmt(ch*p.scale*fy/2)+"\"";
    if(p.colon&&!main)continue;
    if(p.colon){String cx=fmt((p.x-pad*p.scale)*fx),cy=fmt((p.y-pad*p.scale)*fy),cw2=fmt((size*.3+2*pad)*p.scale*fx),chh=fmt(ch*p.scale*fy),r=rot==null?"":rot;boolean swap=cs.equals("color");
     image(x,prefix+"colon.png",null,cx,cy,cw2,chh,(swap?" visibility=\"eq("+CHARGING+",0)\"":"")+r);if(swap)image(x,prefix+"colonc.png",null,cx,cy,cw2,chh," visibility=\""+CHARGING+"\""+r);}
    else if(s.draws(p.digit)){
     Spec ps=p.minutes?ms:hs;boolean photo=ps.photo();String nm=(p.minutes?"m":"h")+(photo?"p"+p.digit:"");
     if(photo&&main)digitSet(ps,prefix,nm,p.minutes?mInk:hInk,endInk,size,cs,chargeInk,toScreen(cell+2*pad,ch,(p.x-pad*p.scale)*fx,(p.y-pad*p.scale)*fy,(cell+2*pad)*p.scale*fx,ch*p.scale*fy,p.rotation,inner),maxDigit(s,p.digit),files);
     digit(x,prefix,nm,digits[p.digit],p.x,p.y,p.scale,fx,fy,size,cs,rot,!photo&&ps.flow()!=null);
    }
   }
   if(g.divider&&main)image(x,prefix+"ink.png",null,fmt((2*(cell+size*s.n("spacing",0,-10,30)*.01)+size*.08)*fx),fmt(size*.16*fy),fmt(size*.035*fx),fmt(size*.78*fy),null);
   x.append("</Group>");
   if(!apart&&main)meta(x,s,prefix,dInk,mInk,width,timeHeight+12,files);
   x.append("</Group>");
  }
  if(s.dateApart()&&main)metaPiece(x,s,prefix,name,dInk,mInk,files);
 }
 private static void metaPiece(StringBuilder x,Spec s,String prefix,String name,int ink,int ink2,Map<String,byte[]> files)throws Exception{
  boolean anyMeta=s.b("showDate",true)||!s.s("caption","").isEmpty()||s.b("seconds",false)||s.info("battery")||s.info("alarm")||s.info("weather")||s.info("progress");if(!anyMeta)return;
  JSONObject me=s.j.optJSONObject("meta");int hpos=s.n("hpos",0,-80,80),vpos=s.n("vpos",14,-10,80);double width=metaWidth(s);
  open(x,s,name+"d",Spec.n(me,"x",hpos,-80,80),Spec.n(me,"y",vpos+24,-10,80),Spec.n(me,"rot",0,-180,180),width,metaHeight(s));meta(x,s,prefix,ink,ink2,width,0,files);x.append("</Group>");
 }
 /** Uses #hour (always 0-23): Realme's engine reports #hour24 as 12 during the midnight hour when the phone is in 12-hour mode. */
 private static String[] digitIds(Spec s){String hr=s.b("h12",false)?"ifelse(eq(#hour12,0),12,#hour12)":"#hour";return new String[]{"int(("+hr+")/10)","int(("+hr+")%10)","int(#minute/10)","int(#minute%10)"};}
 /** Hours and minutes as two independently placed, sized, stretched and rotated groups. */
 private static void splitTime(StringBuilder x,Spec s,Spec hs,Spec ms,String prefix,String name,int hInk,int mInk,int endInk,String charge,int chargeInk,Map<String,byte[]> files)throws Exception{
  JSONObject mo=s.j.optJSONObject("mins");
  int size=s.size(),hpos=s.n("hpos",0,-80,80),vpos=s.n("vpos",14,-10,80),msize=Spec.n(mo,"size",size,60,600);
  int mx=Spec.n(mo,"x",hpos,-80,80),my=Spec.n(mo,"y",vpos+12,-10,80),mrot=Spec.n(mo,"rot",0,-180,180);
  if(s.layout().equals("words")){
   if(s.layer==2)return;
   double fx=s.stretch(s.j,"sx"),fy=s.stretch(s.j,"sy"),mfx=s.stretch(mo,"sx"),mfy=s.stretch(mo,"sy");String cs=charge.equals("color")||charge.equals("fill")?"none":charge;
   Paint hp=paint(hs,hInk,endInk,size*.5,false),mp=paint(ms,mInk,endInk,msize*.3,false);int hw=1,mw=1,hh=(int)Math.ceil(size*.65),mh=(int)Math.ceil(msize*.43);
   for(int i=1;i<=12;i++)hw=Math.max(hw,(int)Math.ceil(hp.measureText(ONES[i])+size*.2));for(int i=0;i<60;i++)mw=Math.max(mw,(int)Math.ceil(mp.measureText(minuteWords(i))+msize*.15));
   for(int i=0;i<24;i++)store(files,prefix+"wordhour_"+i+".png",text(ONES[i%12==0?12:i%12],hp,hw,hh,0,false,look(hs,hInk,endInk,size*.5)));
   for(int i=0;i<60;i++)store(files,prefix+"wordminute_"+i+".png",text(minuteWords(i),mp,mw,mh,0,false,look(ms,mInk,endInk,msize*.3)));
   open(x,s,name+"h",hpos,vpos,s.n("rotate",0,-180,180),hw*fx,hh*fy);chargeAround(x,prefix,cs,chargeInk,hw*fx,hh*fy,files);image(x,prefix+"wordhour.png","#hour","0","0",fmt(hw*fx),fmt(hh*fy),null);x.append("</Group>");
   open(x,s,name+"m",mx,my,mrot,mw*mfx,mh*mfy);image(x,prefix+"wordminute.png","#minute","0","0",fmt(mw*mfx),fmt(mh*mfy),null);x.append("</Group>");
   return;
  }
  String[] digits=digitIds(s);
  for(int part=0;part<2;part++){
   int sz=part==0?size:msize,cell=(int)Math.ceil(sz*.62),glyphH=(int)Math.ceil(sz*1.28);double gap=sz*s.n("spacing",0,-10,30)*.01;
   double fx=part==0?s.stretch(s.j,"sx"):s.stretch(mo,"sx"),fy=part==0?s.stretch(s.j,"sy"):s.stretch(mo,"sy");String file=part==0?"h":"m";
   Spec ps=part==0?hs:ms;boolean main=s.layer!=2,photo=ps.photo();int pad=(int)Math.ceil(sz*.22),ink=part==0?hInk:mInk;
   if(main&&!photo)digitSet(ps,prefix,file,ink,endInk,sz,charge,chargeInk,null,9,files);
   int gh=part==0?hpos:mx,gv=part==0?vpos:my;double grot=part==0?s.n("rotate",0,-180,180):mrot;
   double width=(2*cell+gap)*fx,height=glyphH*fy;open(x,s,name+file,gh,gv,grot,width,height);Matrix placed=group(s,gh,gv,grot,width,height);
   if(part==0&&main)chargeAround(x,prefix,charge,chargeInk,width,height,files);
   for(int i=0;i<2;i++){int pos=part*2+i;if(!s.draws(pos))continue;String nm=file+(photo?"p"+pos:"");
    if(photo&&main)digitSet(ps,prefix,nm,ink,endInk,sz,charge,chargeInk,toScreen(cell+2*pad,glyphH+2*pad,(i*(cell+gap)-pad)*fx,-pad*fy,(cell+2*pad)*fx,(glyphH+2*pad)*fy,0,placed),maxDigit(s,pos),files);
    digit(x,prefix,nm,digits[pos],i*(cell+gap),0,1,fx,fy,sz,charge,null,!photo&&ps.flow()!=null);}
   x.append("</Group>");
  }
 }
 /** A dial with hands that the lock-screen engine turns from the live time; no theme reapply is needed. */
 private static void analog(StringBuilder x,Spec s,String prefix,int ink,int ink2,int endInk,int size,Map<String,byte[]> files)throws Exception{
  int d=(int)Math.ceil(size*1.8);float c=d/2f,r=c-size*.12f;
  Paint face=paint(s,ink,endInk,size,false);face.setStyle(Paint.Style.STROKE);face.setStrokeCap(Paint.Cap.ROUND);
  Bitmap dial=Bitmap.createBitmap(d,d,Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(dial);face.setStrokeWidth(size*.03f);canvas.drawCircle(c,c,r,face);
  for(int i=0;i<12;i++){double a=Math.toRadians(i*30);boolean major=i%3==0;float outer=r-size*.07f,inner=outer-size*(major?.16f:.08f);face.setStrokeWidth(size*(major?.05f:.025f));canvas.drawLine(c+(float)Math.sin(a)*inner,c-(float)Math.cos(a)*inner,c+(float)Math.sin(a)*outer,c-(float)Math.cos(a)*outer,face);}
  store(files,prefix+"dial.png",dial);
  Paint hour=paint(s,ink,endInk,size,false);hour.setStyle(Paint.Style.STROKE);hour.setStrokeCap(Paint.Cap.ROUND);hour.setStrokeWidth(size*.075f);
  Bitmap hand=Bitmap.createBitmap(d,d,Bitmap.Config.ARGB_8888);new Canvas(hand).drawLine(c,c,c,c-r*.52f,hour);store(files,prefix+"hand_h.png",hand);
  Paint minute=paint(s,ink2,endInk,size,false);minute.setStyle(Paint.Style.STROKE);minute.setStrokeCap(Paint.Cap.ROUND);minute.setStrokeWidth(size*.05f);
  hand=Bitmap.createBitmap(d,d,Bitmap.Config.ARGB_8888);Canvas mc=new Canvas(hand);mc.drawLine(c,c,c,c-r*.8f,minute);mc.drawCircle(c,c,size*.07f,plain(minute,ink2));store(files,prefix+"hand_m.png",hand);
  String pivot=" pivotX=\""+fmt(c)+"\" pivotY=\""+fmt(c)+"\"";
  image(x,prefix+"dial.png",null,"0","0",null,null,null);
  image(x,prefix+"hand_h.png",null,"0","0",null,null," rotation=\"#hour12*30+#minute*0.5\""+pivot);
  image(x,prefix+"hand_m.png",null,"0","0",null,null," rotation=\"#minute*6\""+pivot);
  if(s.b("secHand",false)){
   // Optional seconds hand in colour 2; the theme then updates every second while the lock screen is visible.
   Paint sec=new Paint(Paint.ANTI_ALIAS_FLAG);sec.setStyle(Paint.Style.STROKE);sec.setStrokeCap(Paint.Cap.ROUND);sec.setStrokeWidth(Math.max(2,size*.02f));sec.setColor(endInk);
   hand=Bitmap.createBitmap(d,d,Bitmap.Config.ARGB_8888);Canvas sc=new Canvas(hand);sc.drawLine(c,c+r*.15f,c,c-r*.88f,sec);sc.drawCircle(c,c,size*.035f,plain(sec,endInk));store(files,prefix+"hand_s.png",hand);
   image(x,prefix+"hand_s.png",null,"0","0",null,null," rotation=\"#second*6\""+pivot);
  }
 }
 /** Hour ring (fills over 12 hours) and minute ring (fills over 60 minutes) with the time in the centre. */
 private static void rings(StringBuilder x,Spec s,Spec hs,Spec ms,String prefix,int ink,int ink2,int endInk,int size,Map<String,byte[]> files)throws Exception{
  int d=(int)Math.ceil(size*1.8);float c=d/2f,w=size*.11f,ro=c-size*.12f,ri=ro-w*1.9f;
  Paint track=new Paint(Paint.ANTI_ALIAS_FLAG);track.setStyle(Paint.Style.STROKE);track.setStrokeWidth(w);
  Bitmap t=Bitmap.createBitmap(d,d,Bitmap.Config.ARGB_8888);Canvas tc=new Canvas(t);track.setColor((ink&0xffffff)|0x26000000);tc.drawCircle(c,c,ro,track);track.setColor((ink2&0xffffff)|0x26000000);tc.drawCircle(c,c,ri,track);store(files,prefix+"rings_track.png",t);
  RectF outer=new RectF(c-ro,c-ro,c+ro,c+ro),inner=new RectF(c-ri,c-ri,c+ri,c+ri);
  Paint hp=paint(hs,ink,endInk,size,false);hp.setStyle(Paint.Style.STROKE);hp.setStrokeWidth(w);hp.setStrokeCap(Paint.Cap.ROUND);hp.setShader(null);
  for(int i=0;i<12;i++){Bitmap b=Bitmap.createBitmap(d,d,Bitmap.Config.ARGB_8888);new Canvas(b).drawArc(outer,-90,Math.max(.5f,i*30f),false,hp);store(files,prefix+"rh_"+i+".png",b);}
  Paint mp=paint(ms,ink2,endInk,size,false);mp.setStyle(Paint.Style.STROKE);mp.setStrokeWidth(w);mp.setStrokeCap(Paint.Cap.ROUND);mp.setShader(null);
  for(int i=0;i<60;i++){Bitmap b=Bitmap.createBitmap(d,d,Bitmap.Config.ARGB_8888);new Canvas(b).drawArc(inner,-90,Math.max(.5f,i*6f),false,mp);store(files,prefix+"rm_"+i+".png",b);}
  image(x,prefix+"rings_track.png",null,"0","0",null,null,null);image(x,prefix+"rh.png","int(#hour%12)","0","0",null,null,null);image(x,prefix+"rm.png","#minute","0","0",null,null,null);
  // Centre time: small digits sized to fit inside the minute ring.
  int ts=(int)Math.max(20,ri*.55f),cell=(int)Math.ceil(ts*.62),pad=(int)Math.ceil(ts*.22),cw=cell+pad*2,ch=(int)Math.ceil(ts*1.28)+pad*2;
  Paint dh=paint(hs,ink,endInk,ts,false),dm=paint(ms,ink2,endInk,ts,false);
  for(int i=0;i<10;i++){store(files,prefix+"rdh_"+i+".png",text(""+i,dh,cw,ch,0,false,look(hs,ink,endInk,ts)));store(files,prefix+"rdm_"+i+".png",text(""+i,dm,cw,ch,0,false,look(ms,ink2,endInk,ts)));}
  store(files,prefix+"rdc.png",text(":",dh,(int)(ts*.3)+pad*2,ch,0,false,look(hs,ink,endInk,ts)));
  String[] ids=digitIds(s);double total=4*cell+ts*.3,left=c-total/2,top=c-ts*.64-pad;
  for(int i=0;i<4;i++){double px=left+(i<2?i*cell:2*cell+ts*.3+(i-2)*cell);image(x,prefix+(i<2?"rdh":"rdm")+".png",ids[i],fmt(px-pad),fmt(top),fmt(cw),fmt(ch),null);}
  image(x,prefix+"rdc.png",null,fmt(left+2*cell-pad),fmt(top),null,null,null);
 }
 /** Days to a date for the engine to evaluate: Julian day numbers from #year/#month/#date (month is 0-based). */
 static String jdn(String y,String m,String d){String a="int((14-("+m+"))/12)",yy="(("+y+")+4800-"+a+")",mm="(("+m+")+12*"+a+"-3)";return "(("+d+")+int((153*"+mm+"+2)/5)+365*"+yy+"+int("+yy+"/4)-int("+yy+"/100)+int("+yy+"/400)-32045)";}
 public static long jdn(int y,int m,int d){int a=(14-m)/12;long yy=y+4800-a,mm=m+12*a-3;return d+(153*mm+2)/5+365*yy+yy/4-yy/100+yy/400-32045;}
 public static final String TODAY=jdn("#year","#month+1","#date");
 /** Counter expression: days from today to the target, or to its next yearly occurrence. */
 public static String countdown(int y,int m,int d,boolean yearly){
  if(!yearly)return jdn(y,m,d)+"-#studio_day";
  String thisYear=jdn("#year",""+m,""+d)+"-#studio_day",nextYear=jdn("#year+1",""+m,""+d)+"-#studio_day";
  // A Var expression that starts with ifelse skips one change registration in Realme's engine, so lead with 0+.
  return "0+ifelse(lt("+thisYear+",0),"+nextYear+","+thisYear+")";
 }
 /** Up to three day counters (a number in a circle, optional label) and a daily quote, each a movable piece. */
 /** Valid counters as {index, var expression}; their Vars are declared once at the root. */
 private static List<Object[]> counterList(Spec s){
  List<Object[]> out=new ArrayList<>();JSONArray counters=s.j.optJSONArray("counters");
  if(counters!=null)for(int k=0;k<counters.length()&&out.size()<3;k++){
   JSONObject c=counters.optJSONObject(k);if(c==null||!c.optBoolean("on",false))continue;String date=c.optString("date","");if(!date.matches("\\d{4}-\\d{2}-\\d{2}"))continue;
   int y=Integer.parseInt(date.substring(0,4)),m=Integer.parseInt(date.substring(5,7)),d=Integer.parseInt(date.substring(8,10));if(m<1||m>12||d<1||d>31)continue;
   out.add(new Object[]{k,countdown(y,m,d,c.optBoolean("yearly",false))});
  }return out;
 }
 private static void extras(StringBuilder x,Spec s,int ink,String pre,Map<String,byte[]> files)throws Exception{
  JSONArray counters=s.j.optJSONArray("counters");
  for(Object[] item:counterList(s)){
   int k=(Integer)item[0];JSONObject c=counters.optJSONObject(k);
   String var="studio_cnt"+k,label=c.optString("label","").replaceAll("[\\x00-\\x1f]"," ").trim();if(label.length()>24)label=label.substring(0,24);String f=pre+"cnt"+k;
   int dsz=Spec.n(c,"size",140,60,400),ts=(int)(dsz*.42),cell=(int)Math.ceil(ts*.62),pad=(int)Math.ceil(ts*.22),cw=cell+pad*2,ch=(int)Math.ceil(ts*1.28)+pad*2,lh=label.isEmpty()?0:(int)(dsz*.3);
   Paint dp=paint(s,ink,ink,ts,false);dp.clearShadowLayer();dp.setShader(null);dp.setStyle(Paint.Style.FILL);
   for(int i=0;i<10;i++)store(files,f+"_"+i+".png",text(""+i,dp,cw,ch,0,false,null));
   Bitmap disc=Bitmap.createBitmap(dsz,dsz,Bitmap.Config.ARGB_8888);Canvas dc=new Canvas(disc);Paint fill=new Paint(Paint.ANTI_ALIAS_FLAG);fill.setColor((ink&0xffffff)|0x2e000000);dc.drawCircle(dsz/2f,dsz/2f,dsz/2f-2,fill);fill.setStyle(Paint.Style.STROKE);fill.setStrokeWidth(Math.max(2,dsz*.025f));fill.setColor((ink&0xffffff)|0xb0000000);dc.drawCircle(dsz/2f,dsz/2f,dsz/2f-dsz*.03f,fill);store(files,f+"_disc.png",disc);
   double width=Math.max(dsz,label.isEmpty()?0:dsz*2.4),height=dsz+lh;
   x.append("<Group visibility=\"ge(#").append(var).append(",0)\">");open(x,s,"studio_counter_"+pre+k,Spec.n(c,"x",0,-80,80),Spec.n(c,"y",50,-10,80),Spec.n(c,"rot",0,-180,180),width,height);
   double cx=(width-dsz)/2;image(x,f+"_disc.png",null,fmt(cx),"0",null,null,null);
   String v="min(999,#"+var+")",ones="int("+v+"%10)",tens="int("+v+"/10)%10",hund="int("+v+"/100)";double top=dsz/2.0-ch/2.0;
   for(int len=1;len<=3;len++){String vis=len==1?"lt("+v+",10)":len==2?"ge("+v+",10)*lt("+v+",100)":"ge("+v+",100)";x.append("<Group visibility=\"").append(vis).append("\">");double start=width/2-len*cell/2.0;String[] ids=len==1?new String[]{ones}:len==2?new String[]{tens,ones}:new String[]{hund,tens,ones};for(int i=0;i<len;i++)image(x,f+".png",ids[i],fmt(start+i*cell-pad),fmt(top),fmt(cw),fmt(ch),null);x.append("</Group>");}
   if(!label.isEmpty()){Paint lp=paint(s,ink,ink,dsz*.2,true);store(files,f+"_label.png",text(label,lp,(int)Math.ceil(width),lh,0,false,null));image(x,f+"_label.png",null,"0",fmt(dsz),null,null,null);}
   x.append("</Group></Group>");
  }
  JSONObject q=s.j.optJSONObject("quote");JSONArray lines=q==null?null:q.optJSONArray("lines");
  if(q!=null&&q.optBoolean("on",false)&&lines!=null){
   List<String> list=new ArrayList<>();for(int i=0;i<lines.length()&&list.size()<30;i++){String l=lines.optString(i,"").replaceAll("[\\x00-\\x1f]"," ").trim();if(!l.isEmpty())list.add(l.length()>80?l.substring(0,80):l);}
   if(!list.isEmpty()){
    int fsz=Spec.n(q,"size",40,20,90),h=(int)(fsz*1.5),w=1040;
    for(int i=0;i<list.size();i++){Paint qp=paint(s,ink,ink,fsz,true);float m=qp.measureText(list.get(i));if(m>w-40)qp.setTextSize(fsz*(w-40)/m);store(files,pre+"quote_"+i+".png",text(list.get(i),qp,w,h,0,false,null));}
    open(x,s,"studio_quote"+pre,Spec.n(q,"x",0,-80,80),Spec.n(q,"y",62,-10,80),Spec.n(q,"rot",0,-180,180),w,h);image(x,pre+"quote.png","#studio_day%"+list.size(),"0","0",null,null,null);x.append("</Group>");
   }
  }
  shortcuts(x,s,ink,pre,files);alerts(x,s,ink,pre,files);
 }
 /** Icons for app shortcuts and the default camera; set by the app, absent in host checks. */
 public interface Apps {Bitmap icon(String pkg,String cls,int px);String camera();}
 public static Apps apps;
 public static final List<String> SHORTCUTS=Arrays.asList("app","camera","ring","wifi","data");
 /** Shortcut items that can actually be drawn and launched (an app needs a package and activity). */
 static List<JSONObject> shortcutItems(Spec s){
  List<JSONObject> out=new ArrayList<>();JSONObject sc=s.j.optJSONObject("shortcuts");if(sc==null||!sc.optBoolean("on",false))return out;JSONArray items=sc.optJSONArray("items");if(items==null)return out;
  for(int i=0;i<items.length()&&out.size()<4;i++){JSONObject it=items.optJSONObject(i);if(it==null||!SHORTCUTS.contains(it.optString("type")))continue;if(it.optString("type").equals("app")&&(!it.optString("pkg").matches("[A-Za-z0-9_.]{1,200}")||!it.optString("cls").matches("[A-Za-z0-9_.$]{1,300}")))continue;out.add(it);}
  return out;
 }
 /** Row geometry shared by the icons and their buttons: left x expression, top y expression, icon size, gap. */
 static String[] shortcutRow(Spec s,int n){JSONObject sc=s.j.optJSONObject("shortcuts");int size=Spec.n(sc,"size",120,60,220);double gap=size*.4,w=n*size+(n-1)*gap;
  return new String[]{"#screen_width*"+fmt(.5+Spec.n(sc,"x",0,-80,80)*.006)+"-"+fmt(w/2),"#screen_height*"+fmt(.12+Spec.n(sc,"y",70,-10,80)*.011)+"-"+fmt(size/2d),""+size,fmt(gap)};}
 private static int onInk(int ink){return (Color.red(ink)*299+Color.green(ink)*587+Color.blue(ink)*114)/1000>140?0xff16161a:0xfff4f4f6;}
 private static Bitmap plate(int size,int ink,boolean on){Bitmap b=Bitmap.createBitmap(size,size,Bitmap.Config.ARGB_8888);Canvas c=new Canvas(b);Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);p.setColor((ink&0xffffff)|(on?0xd9000000:0x2e000000));c.drawCircle(size/2f,size/2f,size/2f-2,p);if(!on){p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(Math.max(2,size*.025f));p.setColor((ink&0xffffff)|0xb0000000);c.drawCircle(size/2f,size/2f,size/2f-size*.03f,p);}return b;}
 /** Line glyphs on a 24-unit grid, drawn into the middle half of a size×size bitmap. */
 static Bitmap glyph(String kind,int size,int color){
  Bitmap b=Bitmap.createBitmap(size,size,Bitmap.Config.ARGB_8888);Canvas c=new Canvas(b);float u=size*.5f/24;c.translate(size*.25f,size*.25f);c.scale(u,u);
  Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);p.setColor(color);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2f);p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeJoin(Paint.Join.ROUND);Paint f=new Paint(p);f.setStyle(Paint.Style.FILL);
  Path bell=new Path();bell.moveTo(5,17);bell.lineTo(19,17);bell.lineTo(17,14.5f);bell.lineTo(17,10.5f);bell.cubicTo(17,7.5f,14.8f,5,12,5);bell.cubicTo(9.2f,5,7,7.5f,7,10.5f);bell.lineTo(7,14.5f);bell.close();
  switch(kind){
   case "camera":c.drawRoundRect(new RectF(3,7,21,19),2.5f,2.5f,p);c.drawLine(9,7,10,4.5f,p);c.drawLine(10,4.5f,14,4.5f,p);c.drawLine(14,4.5f,15,7,p);c.drawCircle(12,13,3.5f,p);break;
   case "ring2":c.drawPath(bell,p);c.drawCircle(12,20,1.4f,f);break;
   case "ring0":c.drawPath(bell,p);c.drawCircle(12,20,1.4f,f);c.drawLine(4,4,20,20,p);break;
   case "ring1":c.drawRoundRect(new RectF(8,4,16,20),2,2,p);c.drawLine(4.5f,9,4.5f,15,p);c.drawLine(19.5f,9,19.5f,15,p);c.drawLine(1.5f,10.5f,1.5f,13.5f,p);c.drawLine(22.5f,10.5f,22.5f,13.5f,p);break;
   case "wifi":for(float r:new float[]{5,9.5f,14}){c.drawArc(new RectF(12-r,19-r,12+r,19+r),-135,90,false,p);}c.drawCircle(12,19,1.6f,f);break;
   case "data":c.drawLine(8,19,8,5,p);c.drawLine(4.5f,8.5f,8,5,p);c.drawLine(11.5f,8.5f,8,5,p);c.drawLine(16,5,16,19,p);c.drawLine(12.5f,15.5f,16,19,p);c.drawLine(19.5f,15.5f,16,19,p);break;
   case "call":{Path h=new Path();h.moveTo(7,3.5f);h.lineTo(9.5f,3.5f);h.lineTo(11,8);h.lineTo(9,9.5f);h.quadTo(11,14.5f,14.5f,15);h.lineTo(16,13);h.lineTo(20.5f,14.5f);h.lineTo(20.5f,17);h.quadTo(20.5f,20,17.5f,20);h.quadTo(5.5f,19.5f,4,6.5f);h.quadTo(4,3.5f,7,3.5f);h.close();c.drawPath(h,f);break;}
   case "prev":{Path t=new Path();t.moveTo(18,5);t.lineTo(8,12);t.lineTo(18,19);t.close();c.drawPath(t,f);c.drawLine(6,5,6,19,p);break;}
   case "next":{Path t=new Path();t.moveTo(6,5);t.lineTo(16,12);t.lineTo(6,19);t.close();c.drawPath(t,f);c.drawLine(18,5,18,19,p);break;}
   case "play":{Path t=new Path();t.moveTo(7,4);t.lineTo(19,12);t.lineTo(7,20);t.close();c.drawPath(t,f);break;}
   case "pause":c.drawRoundRect(new RectF(6,5,10,19),1,1,f);c.drawRoundRect(new RectF(14,5,18,19),1,1,f);break;
   case "note":{c.drawLine(9,17,9,5,p);c.drawLine(9,5,19,3,p);c.drawLine(19,3,19,15,p);c.drawCircle(7,17,2.5f,f);c.drawCircle(17,15,2.5f,f);break;}
   case "sms":{Path m=new Path();m.addRoundRect(new RectF(3,4.5f,21,17),3,3,Path.Direction.CW);c.drawPath(m,p);Path t=new Path();t.moveTo(7,17);t.lineTo(6.5f,21);t.lineTo(11,17);c.drawPath(t,p);for(int i=0;i<3;i++)c.drawCircle(8+i*4,10.8f,1.2f,f);break;}
  }return b;
 }
 /** Shortcut icons: a plate per item with an app icon or a glyph; toggles show their live state. */
 private static void shortcuts(StringBuilder x,Spec s,int ink,String pre,Map<String,byte[]> files)throws Exception{
  List<JSONObject> items=shortcutItems(s);if(items.isEmpty())return;JSONObject sc=s.j.optJSONObject("shortcuts");String[] row=shortcutRow(s,items.size());int size=Integer.parseInt(row[2]);double gap=Double.parseDouble(row[3]);boolean tint=sc.optBoolean("tint",false);
  int on=onInk(ink);store(files,pre+"sc_off.png",plate(size,ink,false));store(files,pre+"sc_on.png",plate(size,ink,true));
  x.append("<Group name=\"studio_shortcuts").append(pre).append("\" x=\"").append(row[0]).append("\" y=\"").append(row[1]).append("\" alpha=\"").append(esc(alpha(s))).append("\">");
  for(int i=0;i<items.size();i++){
   JSONObject it=items.get(i);String type=it.optString("type"),f=pre+"sc"+i,left=fmt(i*(size+gap));
   if(type.equals("wifi")||type.equals("data")){
    String active=type.equals("wifi")?"gt(eq(#wifi_state,1)+eq(#wifi_state,2),0)":"eq(#data_state,1)";
    store(files,f+"_off.png",glyph(type,size,ink));store(files,f+"_on.png",glyph(type,size,on));
    image(x,pre+"sc_off.png",null,left,"0",null,null," visibility=\"eq("+active+",0)\"");image(x,f+"_off.png",null,left,"0",null,null," visibility=\"eq("+active+",0)\"");
    image(x,pre+"sc_on.png",null,left,"0",null,null," visibility=\""+active+"\"");image(x,f+"_on.png",null,left,"0",null,null," visibility=\""+active+"\"");
   }else{
    image(x,pre+"sc_off.png",null,left,"0",null,null,null);
    if(type.equals("ring")){for(int m=0;m<3;m++)store(files,f+"_"+m+".png",glyph("ring"+m,size,ink));image(x,f+".png","max(0,min(2,#ring_mode))",left,"0",null,null,null);}
    else if(type.equals("camera"))store(files,f+".png",glyph("camera",size,ink));
    else{
     Bitmap icon=apps==null?null:apps.icon(it.optString("pkg"),it.optString("cls"),(int)(size*.62));Bitmap b=Bitmap.createBitmap(size,size,Bitmap.Config.ARGB_8888);
     if(icon!=null){Canvas c=new Canvas(b);if(tint){Bitmap t=icon.copy(Bitmap.Config.ARGB_8888,true);new Canvas(t).drawColor(ink,PorterDuff.Mode.SRC_IN);c.drawBitmap(t,(size-t.getWidth())/2f,(size-t.getHeight())/2f,null);t.recycle();}else c.drawBitmap(icon,(size-icon.getWidth())/2f,(size-icon.getHeight())/2f,new Paint(Paint.FILTER_BITMAP_FLAG));icon.recycle();}
     store(files,f+".png",b);
    }
    if(!type.equals("ring"))image(x,f+".png",null,left,"0",null,null,null);
   }
  }
  x.append("</Group>");
 }
 /** A tap rather than a swipe: the finger lifted close to where it went down. Realme's engine only fires "click"
  *  for taps shorter than 100 ms (then waits 300 ms for a double tap), so ordinary taps never reached it; "up" always fires. */
 static final String TAP="lt(abs(#touch_x-#touch_begin_x)+abs(#touch_y-#touch_begin_y),48)";
 /** Touch targets for the shortcuts, drawn once over everything; the engine passes a touch to every button under it. */
 private static void shortcutButtons(StringBuilder x,Spec s){
  List<JSONObject> items=shortcutItems(s);if(items.isEmpty())return;String[] row=shortcutRow(s,items.size());int size=Integer.parseInt(row[2]);double gap=Double.parseDouble(row[3]);boolean haptic=s.j.optJSONObject("shortcuts").optBoolean("haptic",true);
  String camera=apps==null?null:apps.camera();
  for(int i=0;i<items.size();i++){
   JSONObject it=items.get(i);String type=it.optString("type"),cmd;
   switch(type){
    case "ring":cmd="<Command target=\"RingMode\" value=\"toggle\"/>";break;
    case "wifi":cmd="<Command target=\"Wifi\" value=\"toggle\"/>";break;
    case "data":cmd="<Command target=\"Data\" value=\"toggle\"/>";break;
    case "camera":cmd="<IntentCommand action=\"android.media.action.STILL_IMAGE_CAMERA_SECURE\""+(camera!=null&&camera.matches("[A-Za-z0-9_.]{1,200}")?" package=\""+camera+"\"":"")+"/>";break;
    default:cmd="<IntentCommand action=\"android.intent.action.MAIN\" category=\"android.intent.category.LAUNCHER\" package=\""+it.optString("pkg")+"\" class=\""+esc(it.optString("cls"))+"\"/>";
   }
   x.append("<Button x=\"").append(row[0]).append("+").append(fmt(i*(size+gap))).append("\" y=\"").append(row[1]).append("\" w=\"").append(size).append("\" h=\"").append(size).append("\" visibility=\"eq(#studio_hidden,0)\"><Triggers><Trigger action=\"up\" condition=\"").append(TAP).append("\">").append(haptic?"<VibratorCommand duration=\"18\"/>":"").append(cmd).append("</Trigger></Triggers></Button>");
  }
 }
 /** Experimental: Realme's own music widget (MusicControl). It only shows something when exactly one installed service answers
  *  com.oplus.music.service.command, such as the Studio Music Bridge test app. Names inside it are fixed by the engine. */
 private static void music(StringBuilder x,Spec s,int ink,Map<String,byte[]> files)throws Exception{
  JSONObject m=s.j.optJSONObject("music");if(m==null||!m.optBoolean("on",false))return;
  int h=Spec.n(m,"size",110,70,240),w=(int)(h*3.6),pad=(int)(h*.1),cover=h-2*pad;double gap=h*.14,left=h+gap,ic=h*.3,top=h*.6;
  Bitmap card=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);Canvas cc=new Canvas(card);Paint cp=new Paint(Paint.ANTI_ALIAS_FLAG);cp.setColor((ink&0xffffff)|0x24000000);cc.drawRoundRect(new RectF(1,1,w-1,h-1),h*.22f,h*.22f,cp);cp.setStyle(Paint.Style.STROKE);cp.setStrokeWidth(Math.max(2,h*.02f));cp.setColor((ink&0xffffff)|0x80000000);cc.drawRoundRect(new RectF(h*.02f,h*.02f,w-h*.02f,h-h*.02f),h*.21f,h*.21f,cp);store(files,"music_card.png",card);
  Bitmap def=Bitmap.createBitmap(cover,cover,Bitmap.Config.ARGB_8888);Canvas dc=new Canvas(def);Paint dp=new Paint(Paint.ANTI_ALIAS_FLAG);dp.setColor((ink&0xffffff)|0x40000000);dc.drawRoundRect(new RectF(0,0,cover,cover),cover*.14f,cover*.14f,dp);Bitmap note=glyph("note",cover,ink);dc.drawBitmap(note,0,0,null);note.recycle();store(files,"music_cover.png",def);
  for(String g:new String[]{"prev","play","pause","next"})store(files,"music_"+g+".png",glyph(g,(int)ic,ink));
  String col=String.format(Locale.US,"#%06x",ink&0xffffff),state="#studio_music.music_state";double tw=w-left-gap;
  x.append("<MusicControl name=\"studio_music\" x=\"#screen_width*").append(fmt(.5+Spec.n(m,"x",0,-80,80)*.006)).append("-").append(fmt(w/2d)).append("\" y=\"#screen_height*").append(fmt(.12+Spec.n(m,"y",58,-10,80)*.011)).append("-").append(fmt(h/2d))
   .append("\" w=\"").append(w).append("\" h=\"").append(h).append("\" autoShow=\"0\" defAlbumCover=\"music_cover.png\" visibility=\"eq(#studio_hidden,0)\" alpha=\"").append(esc(alpha(s))).append("\">");
  image(x,"music_card.png",null,"0","0",null,null,null);
  x.append("<Image name=\"music_album_cover\" src=\"music_cover.png\" x=\"").append(pad).append("\" y=\"").append(pad).append("\" w=\"").append(cover).append("\" h=\"").append(cover).append("\"/>");
  x.append("<Text name=\"music_track\" x=\"").append(fmt(left)).append("\" y=\"").append(fmt(h*.1)).append("\" w=\"").append(fmt(tw)).append("\" size=\"").append(fmt(h*.2)).append("\" color=\"").append(col).append("\" bold=\"true\" marqueeSpeed=\"40\" text=\"Nothing playing\"/>");
  x.append("<Text name=\"music_artist\" x=\"").append(fmt(left)).append("\" y=\"").append(fmt(h*.34)).append("\" w=\"").append(fmt(tw)).append("\" size=\"").append(fmt(h*.15)).append("\" color=\"").append(col).append("\" alpha=\"190\" text=\"\"/>");
  String[] names={"music_prev","music_play","music_next"};
  for(int i=0;i<3;i++){
   double bx=left+i*ic*1.6;
   if(i==1){image(x,"music_play.png",null,fmt(bx),fmt(top),null,null," visibility=\"eq("+state+",0)\"");image(x,"music_pause.png",null,fmt(bx),fmt(top),null,null," visibility=\"eq("+state+",1)\"");}
   else image(x,"music_"+(i==0?"prev":"next")+".png",null,fmt(bx),fmt(top),null,null,null);
   // Buttons named like this are wired to play/pause/previous/next by the engine itself; their hit area is the icon, a little larger.
   String bxs=fmt(bx-ic*.25),bys=fmt(top-ic*.25),bs=fmt(ic*1.5);
   x.append("<Button name=\"").append(names[i]).append("\" x=\"").append(bxs).append("\" y=\"").append(bys).append("\" w=\"").append(bs).append("\" h=\"").append(bs).append("\"/>");
   if(i==1)x.append("<Button name=\"music_pause\" x=\"").append(bxs).append("\" y=\"").append(bys).append("\" w=\"").append(bs).append("\" h=\"").append(bs).append("\"/>");
  }
  x.append("</MusicControl>");
 }
 /** Missed-call and unread-message badges; each shows only above zero and the pair stays centred. */
 private static void alerts(StringBuilder x,Spec s,int ink,String pre,Map<String,byte[]> files)throws Exception{
  JSONObject a=s.j.optJSONObject("alerts");if(a==null||!a.optBoolean("on",false))return;boolean calls=a.optBoolean("calls",true),sms=a.optBoolean("sms",true);if(!calls&&!sms)return;
  int h=Spec.n(a,"size",60,40,140),w=(int)(h*2.3),ts=(int)(h*.55),cell=(int)Math.ceil(ts*.62),pad=(int)Math.ceil(ts*.22),cw=cell+pad*2,ch=(int)Math.ceil(ts*1.28)+pad*2;double gap=h*.3;
  Bitmap pill=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);Canvas pc=new Canvas(pill);Paint pp=new Paint(Paint.ANTI_ALIAS_FLAG);pp.setColor((ink&0xffffff)|0x2e000000);pc.drawRoundRect(new RectF(1,1,w-1,h-1),h/2f,h/2f,pp);pp.setStyle(Paint.Style.STROKE);pp.setStrokeWidth(Math.max(2,h*.04f));pp.setColor((ink&0xffffff)|0xb0000000);pc.drawRoundRect(new RectF(h*.03f,h*.03f,w-h*.03f,h-h*.03f),h/2f,h/2f,pp);store(files,pre+"al_pill.png",pill);
  Paint dp=paint(s,ink,ink,ts,false);dp.clearShadowLayer();dp.setShader(null);dp.setStyle(Paint.Style.FILL);for(int i=0;i<10;i++)store(files,pre+"aln_"+i+".png",text(""+i,dp,cw,ch,0,false,null));
  String ca=calls?"gt(#call_missed_count,0)":"0",sa=sms?"gt(#sms_unread_count,0)":"0";
  String cx="#screen_width*"+fmt(.5+Spec.n(a,"x",0,-80,80)*.006),y="#screen_height*"+fmt(.12+Spec.n(a,"y",44,-10,80)*.011)+"-"+fmt(h/2d);
  String first=cx+"-("+ca+"*"+w+"+"+sa+"*"+w+"+"+ca+"*"+sa+"*"+fmt(gap)+")/2";
  x.append("<Group name=\"studio_alerts").append(pre).append("\" alpha=\"").append(esc(alpha(s))).append("\">");
  for(int b=0;b<2;b++){
   if(b==0&&!calls||b==1&&!sms)continue;String var=b==0?"#call_missed_count":"#sms_unread_count",vis=b==0?ca:sa,bx=b==0?first:first+"+"+ca+"*"+fmt(w+gap),g=b==0?"call":"sms";
   store(files,pre+"al_"+g+".png",glyph(g,h,ink));
   x.append("<Group x=\"").append(esc(bx)).append("\" y=\"").append(y).append("\" visibility=\"").append(vis).append("\">");
   image(x,pre+"al_pill.png",null,"0","0",null,null,null);image(x,pre+"al_"+g+".png",null,fmt(h*.12),"0",null,null,null);
   String v="min(99,"+var+")",top=fmt(h/2.0-ch/2.0);double right=w-h*.3;
   x.append("<Group visibility=\"lt(").append(v).append(",10)\">");image(x,pre+"aln.png","int("+v+"%10)",fmt(right-cell-pad),top,fmt(cw),fmt(ch),null);x.append("</Group>");
   x.append("<Group visibility=\"ge(").append(v).append(",10)\">");image(x,pre+"aln.png","int("+v+"/10)",fmt(right-2*cell-pad),top,fmt(cw),fmt(ch),null);image(x,pre+"aln.png","int("+v+"%10)",fmt(right-cell-pad),top,fmt(cw),fmt(ch),null);x.append("</Group>");
   x.append("</Group>");
  }
  x.append("</Group>");
 }
 private static double date(StringBuilder x,Spec s,String prefix,int ink,double width,double y,Map<String,byte[]> files)throws Exception{
  int size=metaSize(s),h=(int)(size*1.5);Paint p=paint(s,ink,ink,size,true);double tracking=size*s.n("dateTrack",0,0,30)*.01;
  String style=s.choice("dateStyle","full","full","short","dm","caps","num","day");boolean upper=s.s("dateCase","normal").equals("upper")||style.equals("caps");
  String[] weekdays={"Sunday","Monday","Tuesday","Wednesday","Thursday","Friday","Saturday"},months={"January","February","March","April","May","June","July","August","September","October","November","December"};
  boolean shortNames=Arrays.asList("short","caps","day").contains(style);boolean shortMonth=style.equals("short")||style.equals("caps");
  int wkW=1,moW=1,dyW=(int)(size*1.65),sp=(int)(size*.3);
  for(int i=0;i<weekdays.length;i++){if(shortNames)weekdays[i]=weekdays[i].substring(0,3);if(style.equals("full")||style.equals("short"))weekdays[i]+=",";String t=upper?weekdays[i].toUpperCase(Locale.US):weekdays[i];wkW=Math.max(wkW,(int)Math.ceil(p.measureText(t)+tracking*t.length()+size*.2));}
  for(int i=0;i<months.length;i++){if(shortMonth)months[i]=months[i].substring(0,3);if(style.equals("num"))months[i]=(i+1)+"/";String t=upper?months[i].toUpperCase(Locale.US):months[i];moW=Math.max(moW,(int)Math.ceil(p.measureText(t)+tracking*t.length()+size*.2));}
  for(int i=0;i<7;i++)store(files,prefix+"wk_"+i+".png",text(weekdays[i],p,wkW,h,tracking,upper,null));for(int i=0;i<12;i++)store(files,prefix+"mo_"+i+".png",text(months[i],p,moW,h,tracking,upper,null));for(int i=1;i<=31;i++)store(files,prefix+"dy_"+i+".png",text(""+i,p,dyW,h,tracking,false,null));
  String[] order=style.equals("dm")?new String[]{"dy","mo"}:style.equals("num")?new String[]{"mo","dy"}:style.equals("day")?new String[]{"wk","dy"}:style.equals("caps")?new String[]{"wk","dy","mo"}:new String[]{"wk","mo","dy"};
  double total=sp*(order.length-1);for(String part:order)total+=part.equals("wk")?wkW:part.equals("mo")?moW:dyW;
  double left=s.s("align","center").equals("left")?0:s.s("align","center").equals("right")?width-total:(width-total)/2;
  for(String part:order){int w=part.equals("wk")?wkW:part.equals("mo")?moW:dyW;image(x,prefix+part+".png",part.equals("wk")?"#day_of_week-1":part.equals("mo")?"#month":"#date",fmt(left),fmt(y),null,null,null);left+=w+sp;}return y+h;
 }
 private static double info(StringBuilder x,Spec s,String prefix,int ink,double width,double y,Map<String,byte[]> files)throws Exception{
  List<String> kinds=new ArrayList<>(),labels=new ArrayList<>();List<Integer> widths=new ArrayList<>();Paint p=paint(s,0xffffffff,0xffffffff,28,true);
  if(s.info("battery")){kinds.add("battery");labels.add("");widths.add(150);}
  if(s.info("alarm")){kinds.add("alarm");labels.add("Alarm "+s.s("nextAlarm","None set"));widths.add(Math.min((int)width,(int)p.measureText(labels.get(labels.size()-1))+36));}
  if(s.info("weather")){kinds.add("weather");labels.add(s.s("weatherText","Weather"));widths.add(Math.min((int)width,(int)p.measureText(labels.get(labels.size()-1))+36));}
  if(s.info("progress")){kinds.add("progress");labels.add("");widths.add(250);}
  if(kinds.isEmpty())return y;
  int start=0;double top=y;
  while(start<kinds.size()){
   int end=start,total=0;while(end<kinds.size()&&(end==start||total+12+widths.get(end)<=width)){if(end>start)total+=12;total+=widths.get(end);end++;}
   double left=s.s("align","center").equals("left")?0:s.s("align","center").equals("right")?width-total:(width-total)/2;
   for(int i=start;i<end;i++){
    int w=widths.get(i),h=42;String kind=kinds.get(i),name=prefix+"chip-"+kind+".png";Bitmap plate=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(plate);Paint fill=new Paint(Paint.ANTI_ALIAS_FLAG);fill.setColor(0x28ffffff);canvas.drawRoundRect(0,0,w,h,21,21,fill);
    if(!labels.get(i).isEmpty()){Bitmap words=text(labels.get(i),p,w-24,h,0,false,null);canvas.drawBitmap(words,12,0,null);words.recycle();}store(files,name,plate);image(x,name,null,fmt(left),fmt(top),null,null,null);
    if(kind.equals("battery"))x.append("<Text x=\"").append(fmt(left+w/2d)).append("\" y=\"").append(fmt(top+6)).append("\" size=\"28\" color=\"#ffffff\" align=\"center\" format=\"%d%% battery\" paras=\"#battery_level\"/>");
    if(kind.equals("progress")){x.append("<Text x=\"").append(fmt(left+12)).append("\" y=\"").append(fmt(top+7)).append("\" size=\"26\" color=\"#ffffff\" text=\"Day\"/>");image(x,"progress-track.png",null,fmt(left+70),fmt(top+17),"100","7",null);image(x,"ink.png",null,fmt(left+70),fmt(top+17),"100*(#hour*60+#minute)/1440","7",null);x.append("<Text x=\"").append(fmt(left+180)).append("\" y=\"").append(fmt(top+7)).append("\" size=\"26\" color=\"#ffffff\" format=\"%d%%\" paras=\"int((#hour*60+#minute)/14.4)\"/>");}
    left+=w+12;
   }start=end;top+=52;
  }return top;
 }
 public static byte[] build(ThemeBuilder.Design d,File photo,File subject)throws IOException{
  try{
   Spec base=new Spec(new JSONObject(d.v2));Map<String,byte[]> files=new LinkedHashMap<>();JSONObject schedule=base.j.optJSONObject("schedule"),styles=base.j.optJSONObject("scheduleStyles");
   boolean scheduled=schedule!=null&&schedule.optBoolean("on",false)&&styles!=null;
   List<Spec> slots=new ArrayList<>();List<String> names=new ArrayList<>();
   if(scheduled){for(String key:new String[]{"day","eve","night"}){JSONObject j=styles.optJSONObject(key);slots.add(new Spec(j==null?base.j:j));names.add(key);}}else{slots.add(base);names.add("current");}
   boolean moving=false,seconds=false,tilt=false;for(Spec s:slots){moving|=!s.s("movement","still").equals("still")||s.charge().equals("glow");seconds|=s.b("seconds",false)||s.layout().equals("analog")&&s.b("secHand",false);tilt|=gyro(s);}
   StringBuilder x=new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?><Lockscreen version=\"1\" screenWidth=\"1080\" frameRate=\"").append(moving?15:1).append("\" useVariableUpdater=\"Battery,DateTime.").append(seconds?"Second":"Minute").append("\"");
   if(base.s("bg","atmos").equals("atmos"))x.append(" showSysWallpaper=\"true\"");x.append("><Var name=\"notification\" expression=\"1\"/><Var name=\"studio_hidden\" expression=\"").append(base.b("startHidden",false)?1:0).append("\"/><Var name=\"studio_day\" expression=\"").append(esc(TODAY)).append("\"/>");
   for(Object[] c:counterList(base))x.append("<Var name=\"studio_cnt").append(c[0]).append("\" expression=\"").append(esc((String)c[1])).append("\"/>");
   if(drag(base))x.append("<Var name=\"studio_drag\" expression=\"0\"/>");
   if(tilt)x.append("<VariableBinders><SensorBinder type=\"gravity\" rate=\"2\"><Variable name=\"studio_gravity_x\" index=\"0\"/><Variable name=\"studio_gravity_y\" index=\"1\"/></SensorBinder></VariableBinders>");
   String bgMode=base.choice("bg","atmos","atmos","photo","gradient","black");byte[] jpg=null;Bitmap bg=null;
   // Parallax: the photo plane (background, subject, its shadow and rim) drifts against the clock, slightly enlarged so no edge shows.
   boolean parallax=base.s("movement","still").equals("parallax");double k=parallax?1.045:1;String px=parallax?motion(base,true,-.4):"",py=parallax?motion(base,false,-.4):"";
   JSONObject water=base.j.optJSONObject("water");boolean ripple=water!=null&&water.optBoolean("on",false)&&(bgMode.equals("photo")||bgMode.equals("gradient"));
   if(!bgMode.equals("atmos")){
    bg=ThemeBuilder.wallpaper(bgMode.equals("photo")?photo:null,d.height);
    if(bgMode.equals("black"))bg.eraseColor(Color.BLACK);
    if(bgMode.equals("gradient")){int[] colors=gradient(base.s("gradient","dusk"));Paint p=new Paint();p.setShader(new LinearGradient(0,0,1080,d.height,colors,null,Shader.TileMode.CLAMP));new Canvas(bg).drawRect(0,0,1080,d.height,p);}
    ByteArrayOutputStream bytes=new ByteArrayOutputStream();bg.compress(Bitmap.CompressFormat.JPEG,92,bytes);jpg=bytes.toByteArray();files.put("advance/bg.jpg",jpg);
    String bx=parallax?"0-#screen_width*"+fmt((k-1)/2)+px:"0",by=parallax?"0-#screen_height*"+fmt((k-1)/2)+py:"0",bw=parallax?"#screen_width*"+fmt(k):"#screen_width",bh=parallax?"#screen_height*"+fmt(k):"#screen_height";
    // Water: the engine's ripple mesh redraws only while a touch ripple settles.
    if(ripple)x.append("<WaterWallpaper src=\"bg.jpg\" x=\"").append(esc(bx)).append("\" y=\"").append(esc(by)).append("\" w=\"").append(bw).append("\" h=\"").append(bh).append("\" torsion=\"").append(Spec.n(water,"strength",2,1,5)).append("\" xmesh=\"27\" ymesh=\"48\"/>");
    else image(x,"bg.jpg",null,bx,by,bw,bh,null);
   }
   Bitmap pixel=Bitmap.createBitmap(1,1,Bitmap.Config.ARGB_8888);pixel.eraseColor(Color.BLACK);files.put("advance/dim.png",png(pixel));pixel.eraseColor(Color.WHITE);files.put("advance/ink.png",png(pixel));pixel.eraseColor(0x66ffffff);files.put("advance/progress-track.png",png(pixel));pixel.recycle();
   if(base.n("dim",0,0,100)>0)image(x,"dim.png",null,"0","0","#screen_width","#screen_height"," alpha=\""+Math.round(base.n("dim",0,0,100)*2.55*.8)+"\"");
   boolean subjectShown=base.b("subjectOn",true)&&bgMode.equals("photo")&&subject!=null,front=false;
   for(Spec s:slots){s.backdrop=bgMode.equals("photo")||bgMode.equals("gradient")?bg:null;s.screenH=d.height;front|=subjectShown&&s.anyFront();}
   for(Spec s:slots)s.layer=front?1:0;
   x.append("<Group name=\"studio_clock\" visibility=\"eq(#studio_hidden,0)\">");clocks(x,slots,names,scheduled,files);
   int[] lightInk=base.lightInks();
   if(lightInk==null)extras(x,base,base.color(false),"",files);
   else{x.append("<Group visibility=\"eq(#dark_mode,1)\">");extras(x,base,base.color(false),"",files);x.append("</Group><Group visibility=\"eq(#dark_mode,0)\">");extras(x,base,lightInk[0],"l",files);x.append("</Group>");}
   if(base.charge().equals("bar")){
    // Energy bar along the bottom edge, filled to the battery level, only while charging.
    int ci=base.chargeInk();Bitmap dot=Bitmap.createBitmap(1,1,Bitmap.Config.ARGB_8888);dot.eraseColor(0x30ffffff);files.put("advance/charge_track.png",png(dot));dot.eraseColor(ci);files.put("advance/charge_fill.png",png(dot));dot.recycle();
    String vis=" visibility=\""+CHARGING+"\"";image(x,"charge_track.png",null,"#screen_width*0.1","#screen_height*0.93","#screen_width*0.8","10",vis);image(x,"charge_fill.png",null,"#screen_width*0.1","#screen_height*0.93","#screen_width*0.008*#battery_level","10",vis);
   }
   x.append("</Group>");
   if(subjectShown){
    Bitmap b=ThemeBuilder.subject(subject,d.height);int scale=base.n("subjScale",100,50,200),shadow=base.n("subjShadow",0,0,100),rim=base.n("rimLight",0,0,100);double sc=scale/100d*k,sx=base.n("subjX",0,-30,30)/100d,sy=base.n("subjY",0,-30,30)/100d;
    // Light comes from the clock: the rim is on the subject's side facing it, and the shadow falls down and toward it.
    double vx=1080*(.5+base.n("hpos",0,-80,80)*.006)-1080*(.5+sx),vy=d.height*(.12+base.n("vpos",14,-10,80)*.011)-d.height*(.5+sy),len=Math.hypot(vx,vy);if(len<1){vx=0;vy=-1;}else{vx/=len;vy/=len;}
    String ix="#screen_width*(0.5+"+fmt(sx*k)+")-540*"+fmt(sc)+px,iy="#screen_height*(0.5+"+fmt(sy*k)+")-#screen_height*"+fmt(sc/2)+py,iw="1080*"+fmt(sc),ih="#screen_height*"+fmt(sc);
    if(shadow>0){double ox=vx*.45,oy=vy*.45+.9,ol=Math.hypot(ox,oy);ox=ox/ol*22*sc;oy=oy/ol*22*sc;Bitmap small=Bitmap.createScaledBitmap(b,180,Math.max(1,d.height/6),true),soft=soften(small,3);soft=tint(soft,0xff000000);store(files,"subject_shadow.png",soft);
     image(x,"subject_shadow.png",null,ix+"+"+fmt(ox),iy+"+"+fmt(oy),iw,ih," alpha=\""+Math.round(shadow*2.55*.75)+"\"");}
    Bitmap rimBitmap=null;
    if(rim>0){Bitmap half=Bitmap.createScaledBitmap(b,540,Math.max(1,d.height/2),true);Bitmap edge=tint(half.copy(Bitmap.Config.ARGB_8888,true),base.color(false));Paint cut=new Paint(Paint.FILTER_BITMAP_FLAG);cut.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_OUT));new Canvas(edge).drawBitmap(half,(float)(-vx*6),(float)(-vy*6),cut);half.recycle();rimBitmap=soften(edge,2);}
    store(files,"subject.png",b);image(x,"subject.png",null,ix,iy,iw,ih,null);
    if(rimBitmap!=null){store(files,"subject_rim.png",rimBitmap);image(x,"subject_rim.png",null,ix,iy,iw,ih," alpha=\""+Math.round(rim*2.55)+"\"");}
   }
   if(front){for(Spec s:slots)s.layer=2;x.append("<Group name=\"studio_front\" visibility=\"eq(#studio_hidden,0)\">");clocks(x,slots,names,scheduled,files);x.append("</Group>");}
   boolean dragging=drag(base);
   x.append("<Button h=\"#screen_height\" w=\"#screen_width\"><Triggers><Trigger action=\"up\"><ExternCommand command=\"unlock\" condition=\"gt(#touch_begin_y-#touch_y,440)\"/>").append(dragging?"<VariableCommand name=\"studio_drag\" expression=\"0\"/>":"").append("</Trigger>");
   if(dragging)x.append("<Trigger action=\"down\"><VariableCommand name=\"studio_drag\" expression=\"1\"/></Trigger><Trigger action=\"cancel\"><VariableCommand name=\"studio_drag\" expression=\"0\"/></Trigger>");
   if(base.b("dbl",true))x.append("<Trigger action=\"double\"><VariableCommand name=\"studio_hidden\" expression=\"1-#studio_hidden\"/></Trigger>");x.append("</Triggers></Button>");shortcutButtons(x,base);music(x,base,base.color(false),files);x.append("</Lockscreen>");
   byte[] manifest=x.toString().getBytes("UTF-8");files.put("advance/manifest.xml",manifest);for(String ratio:new String[]{"11x5","13x6","19x9","20x9","2x1","301x135","403x180","67x30"})files.put("advance/"+ratio+"/manifest.xml",manifest);
   if(bg!=null)bg.recycle();
   return ThemeBuilder.packageTheme(ThemeBuilder.wallpaperComponent(jpg==null?new byte[0]:jpg,!base.b("replaceWallpaper",false)||bgMode.equals("atmos")),zip(files));
  }catch(Exception e){if(e instanceof IOException)throw (IOException)e;throw new IOException("Studio theme: "+e.getMessage(),e);}
 }
 /** The clock for every scheduled slot (with its warm night variant), drawn in the slots' current depth pass. */
 private static void clocks(StringBuilder x,List<Spec> slots,List<String> names,boolean scheduled,Map<String,byte[]> files)throws Exception{
  for(int i=0;i<slots.size();i++){
   Spec s=slots.get(i);String visibility=scheduled?(i==0?"gt(#hour,5)*lt(#hour,18)":i==1?"gt(#hour,17)*lt(#hour,23)":"gt(gt(#hour,22)+lt(#hour,6),0)"):"1";x.append("<Group visibility=\"").append(visibility).append("\">");
   // Light mode: a second set in the light-mode colours, shown while the phone's dark theme is off.
   int[] light=s.lightInks();if(light!=null){x.append("<Group visibility=\"eq(#dark_mode,0)\">");time(x,s,names.get(i)+"/light/",light[0],light[1],files);x.append("</Group><Group visibility=\"eq(#dark_mode,1)\">");}
   if(s.b("warm",false)){x.append("<Group visibility=\"eq(").append(s.night()).append(",0)\">");time(x,s,names.get(i)+"/day/",s.color(false),s.color(true),files);x.append("</Group><Group visibility=\"").append(s.night()).append("\">");time(x,s,names.get(i)+"/warm/",mix(s.color(false),0xffffb070,.6f),(s.b("twoTone",false)?mix(s.color(true),0xffff9a55,.6f):mix(s.color(false),0xffffb070,.6f)),files);x.append("</Group>");}
   else time(x,s,names.get(i)+"/day/",s.color(false),s.color(true),files);if(light!=null)x.append("</Group>");x.append("</Group>");
  }
 }
 /** Cheap blur by scaling down and back up with filtering. */
 private static Bitmap soften(Bitmap src,int f){int w=src.getWidth(),h=src.getHeight();Bitmap a=Bitmap.createScaledBitmap(src,Math.max(1,w/f),Math.max(1,h/f),true),b=Bitmap.createScaledBitmap(a,Math.max(1,w/f/2),Math.max(1,h/f/2),true),c=Bitmap.createScaledBitmap(b,Math.max(1,w/f),Math.max(1,h/f),true),out=Bitmap.createScaledBitmap(c,w,h,true);a.recycle();b.recycle();c.recycle();src.recycle();return out;}
 /** Keeps the alpha of a bitmap and paints it one colour. */
 private static Bitmap tint(Bitmap src,int color){Bitmap out=src.isMutable()?src:src.copy(Bitmap.Config.ARGB_8888,true);if(out!=src)src.recycle();new Canvas(out).drawColor(color,PorterDuff.Mode.SRC_IN);return out;}
 public static int[] gradient(String key){switch(key){case "aurora":return new int[]{0xff0f2b3a,0xff1d5c63,0xff3a2a6b};case "ember":return new int[]{0xff1a0f14,0xff5a1f2a,0xffc4602f};case "forest":return new int[]{0xff0b1a14,0xff17402f,0xff2d6a4a};case "ocean":return new int[]{0xff06121f,0xff0d3b66,0xff3a86c8};case "mono":return new int[]{0xff0c0d10,0xff22252d};default:return new int[]{0xff1f2340,0xff4a2f63,0xff8a4a6b};}}
}
