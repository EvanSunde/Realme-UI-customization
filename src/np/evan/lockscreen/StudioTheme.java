package np.evan.lockscreen;
import android.graphics.*;
import org.json.*;
import java.io.*;
import java.util.*;
import java.util.zip.*;

/** Native counterpart of the user's Studio V2 document, with no running editor required. */
public final class StudioTheme {
 public static final String[] FONTS={"inter","outfit","manrope","sora","lexend","poppins","spacegrotesk","nunito","bebas","anton","archivoblack","bungee","righteous","unbounded","bigshoulders","syne","fraunces","playfair","dmserif","cormorant","robotoslab","abril","jetbrains","spacemono","major","orbitron","audiowide","chakra","rubikmono","barlow"};
 public static final String[] LAYOUTS={"single","stacked","stairs","hero","split","grid","tower","vertical","arc","words","bigmin","diagonal","wave","offset","wide","analog"};
 public static final String[] FINISHES={"solid","glow","outline","neon","gradient","shadow","glass","outlineglow","duo","retro","emboss","chrome","holo","splittone","halftone","glitch","stencil"};
 /** Layouts that are already single-digit shapes, or a dial, cannot be split into hours/minutes/date pieces. */
 public static final List<String> NO_SPLIT=Arrays.asList("grid","tower","vertical","arc","analog");
 public static final String[] ONES={"zero","one","two","three","four","five","six","seven","eight","nine","ten","eleven","twelve","thirteen","fourteen","fifteen","sixteen","seventeen","eighteen","nineteen"};
 public static final class Spec {
  public final JSONObject j;
  public Spec(JSONObject value){j=value;}
  public int n(String k,int def,int lo,int hi){return Math.max(lo,Math.min(hi,j.optInt(k,def)));}
  public static int n(JSONObject o,String k,int def,int lo,int hi){return Math.max(lo,Math.min(hi,o==null?def:o.optInt(k,def)));}
  public boolean split(){return b("split",false)&&!NO_SPLIT.contains(layout());}
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
  public String night(){int a=n("warmFrom",18,0,23),b=n("warmTo",6,0,23);if(a==b)return "0";return a>b?"gt(gt(#hour24,"+(a-1)+")+lt(#hour24,"+b+"),0)":"gt(#hour24,"+(a-1)+")*lt(#hour24,"+b+")";}
 }
 public static final class Piece {
  public int digit;public double x,y,scale,rotation;public boolean minutes,colon;
  Piece(int digit,double x,double y,double scale,boolean minutes){this.digit=digit;this.x=x;this.y=y;this.scale=scale;this.minutes=minutes;}
 }
 public static final class Geometry {
  public double width,height;public final List<Piece> pieces=new ArrayList<>();public boolean divider=false,words=false,analog=false;public double rotate=0;
 }
 public static Geometry geometry(String layout,int size,int spacing,int line,int minScale,int arc){
  Geometry g=new Geometry();double c=size*.62,h=size*1.28,gap=size*spacing*.01,step=size*line*.01,ms=minScale*.01;
  if(layout.equals("hero"))ms*=.4;
  if(layout.equals("words")){g.words=true;g.width=size*3.6;g.height=size*1.13;return g;}
  if(layout.equals("analog")){g.analog=true;g.width=g.height=size*1.8;return g;}
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
  p.setTypeface(StudioFonts.face(family,date?Math.min(600,s.n("weight",500,100,900)):s.n("weight",800,100,900)));p.setTextSize((float)size);p.setColor(ink);p.setFontFeatureSettings("tnum");
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
 private static void open(StringBuilder x,Spec s,String name,int hpos,int vpos,double rotation,double width,double height){
  String move=s.choice("movement","still","still","breathe","float","tilt"),tx=move.equals("tilt")?"+max(-32,min(32,#studio_gravity_x*5))":"",ty=move.equals("tilt")?"+max(-32,min(32,#studio_gravity_y*3))":"";
  x.append("<Group name=\"").append(name).append("\" x=\"#screen_width*").append(fmt(.5+hpos*.006)).append('-').append(fmt(width/2)).append(tx).append("\" y=\"#screen_height*").append(fmt(.12+vpos*.011)).append('-').append(fmt(height/2)).append(ty).append("\" rotation=\"").append(fmt(rotation)).append("\" pivotX=\"").append(fmt(width/2)).append("\" pivotY=\"").append(fmt(height/2)).append("\" alpha=\"").append(esc(alpha(s))).append("\">");
  if(move.equals("float"))x.append("<PositionAnimation><Item x=\"0\" y=\"0\" time=\"0\"/><Item x=\"0\" y=\"-24\" time=\"3500\"/><Item x=\"0\" y=\"0\" time=\"7000\"/></PositionAnimation>");
  if(move.equals("breathe"))x.append("<AlphaAnimation><Item value=\"255\" time=\"0\"/><Item value=\"178\" time=\"2500\"/><Item value=\"255\" time=\"5000\"/></AlphaAnimation>");
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
 private static void time(StringBuilder x,Spec s,String prefix,int ink,int ink2,Map<String,byte[]> files)throws Exception{
  int endInk=prefix.contains("/warm/")?mix(s.secondary(),0xffff9a55,.6f):s.secondary();
  Bitmap tint=Bitmap.createBitmap(1,1,Bitmap.Config.ARGB_8888);tint.eraseColor(endInk);store(files,prefix+"ink.png",tint);
  if(s.split()){splitTime(x,s,prefix,ink,ink2,endInk,files);return;}
  int size=s.size();Geometry g=geometry(s.layout(),size,s.n("spacing",0,-10,30),s.n("lh",88,60,140),s.n("minScale",100,30,130),s.n("arc",55,-100,100));
  if(s.layout().equals("stairs")&&s.s("align","center").equals("right"))for(Piece piece:g.pieces)piece.x=g.width-piece.x-size*.62*piece.scale;
  int cell=(int)Math.ceil(size*.62),glyphH=(int)Math.ceil(size*1.28),pad=(int)Math.ceil(size*.22),cw=cell+pad*2,ch=glyphH+pad*2;
  if(!g.analog&&!g.words){
   Paint hour=paint(s,ink,endInk,size,false),minute=paint(s,ink2,endInk,size,false);
   for(int part=0;part<2;part++)for(int digit=0;digit<10;digit++)store(files,prefix+(part==0?"h_":"m_")+digit+".png",text(""+digit,part==0?hour:minute,cw,ch,0,false,look(s,part==0?ink:ink2,endInk,size)));
   store(files,prefix+"colon.png",text(":",hour,(int)(size*.3)+pad*2,ch,0,false,look(s,ink,endInk,size)));
  }
  String[] digits=digitIds(s);
  // All elements use positions relative to the same clock group. Rotation remains a native group transform.
  double timeWidth=g.rotate==0?g.width:g.height,timeHeight=g.rotate==0?g.height:g.width;
  double width=Math.max(timeWidth,metaWidth(s)),height=timeHeight+metaHeight(s);
  open(x,s,"studio_clock_"+prefix.replace('/','_'),s.n("hpos",0,-80,80),s.n("vpos",14,-10,80),s.n("rotate",0,-180,180),width,height);
  double left=s.s("align","center").equals("left")?0:s.s("align","center").equals("right")?width-timeWidth:(width-timeWidth)/2;
  x.append("<Group x=\"").append(fmt(left+(g.rotate==0?0:g.height/2-g.width/2))).append("\" y=\"").append(fmt(g.rotate==0?0:g.width/2-g.height/2)).append("\" rotation=\"").append(fmt(g.rotate)).append("\" pivotX=\"").append(fmt(g.width/2)).append("\" pivotY=\"").append(fmt(g.height/2)).append("\">");
  if(g.analog)analog(x,s,prefix,ink,ink2,endInk,size,files);
  else if(g.words){
   int ww=(int)Math.ceil(g.width),hh=(int)Math.ceil(size*.65),mh=(int)Math.ceil(size*.43);Paint hp=paint(s,ink,endInk,size*.5,false),mp=paint(s,ink2,endInk,size*.3,false);
   for(int i=0;i<24;i++)store(files,prefix+"wordhour_"+i+".png",text(ONES[i%12==0?12:i%12],hp,ww,hh,0,false,look(s,ink,endInk,size*.5)));
   for(int i=0;i<60;i++)store(files,prefix+"wordminute_"+i+".png",text(minuteWords(i),mp,ww,mh,0,false,look(s,ink2,endInk,size*.3)));
   image(x,prefix+"wordhour.png","#hour24","0","0",null,null,null);image(x,prefix+"wordminute.png","#minute","0",fmt(hh),null,null,null);
  }else for(Piece p:g.pieces){
   String path=prefix+(p.colon?"colon.png":p.minutes?"m.png":"h.png");String id=p.colon?null:digits[p.digit];
   image(x,path,id,fmt(p.x-pad*p.scale),fmt(p.y-pad*p.scale),fmt((p.colon?size*.3+2*pad:cw)*p.scale),fmt(ch*p.scale),p.rotation==0?null:" rotation=\""+fmt(p.rotation)+"\" pivotX=\""+fmt(cw*p.scale/2)+"\" pivotY=\""+fmt(ch*p.scale/2)+"\"");
  }
  if(g.divider)image(x,prefix+"ink.png",null,fmt(2*(cell+size*s.n("spacing",0,-10,30)*.01)+size*.08),fmt(size*.16),fmt(size*.035),fmt(size*.78),null);
  x.append("</Group>");
  meta(x,s,prefix,ink,ink2,width,timeHeight+12,files);
  x.append("</Group>");
 }
 private static String[] digitIds(Spec s){String hr=s.b("h12",false)?"ifelse(eq(#hour12,0),12,#hour12)":"#hour24";return new String[]{"int(("+hr+")/10)","int(("+hr+")%10)","int(#minute/10)","int(#minute%10)"};}
 /** Hours, minutes and the date block as three independently placed and rotated groups. */
 private static void splitTime(StringBuilder x,Spec s,String prefix,int ink,int ink2,int endInk,Map<String,byte[]> files)throws Exception{
  JSONObject mo=s.j.optJSONObject("mins"),me=s.j.optJSONObject("meta");
  int size=s.size(),hpos=s.n("hpos",0,-80,80),vpos=s.n("vpos",14,-10,80),msize=Spec.n(mo,"size",size,60,600);String name="studio_clock_"+prefix.replace('/','_');
  int mx=Spec.n(mo,"x",hpos,-80,80),my=Spec.n(mo,"y",vpos+12,-10,80),mrot=Spec.n(mo,"rot",0,-180,180);
  if(s.layout().equals("words")){
   Paint hp=paint(s,ink,endInk,size*.5,false),mp=paint(s,ink2,endInk,msize*.3,false);int hw=1,mw=1,hh=(int)Math.ceil(size*.65),mh=(int)Math.ceil(msize*.43);
   for(int i=1;i<=12;i++)hw=Math.max(hw,(int)Math.ceil(hp.measureText(ONES[i])+size*.2));for(int i=0;i<60;i++)mw=Math.max(mw,(int)Math.ceil(mp.measureText(minuteWords(i))+msize*.15));
   for(int i=0;i<24;i++)store(files,prefix+"wordhour_"+i+".png",text(ONES[i%12==0?12:i%12],hp,hw,hh,0,false,look(s,ink,endInk,size*.5)));
   for(int i=0;i<60;i++)store(files,prefix+"wordminute_"+i+".png",text(minuteWords(i),mp,mw,mh,0,false,look(s,ink2,endInk,msize*.3)));
   open(x,s,name+"h",hpos,vpos,s.n("rotate",0,-180,180),hw,hh);image(x,prefix+"wordhour.png","#hour24","0","0",null,null,null);x.append("</Group>");
   open(x,s,name+"m",mx,my,mrot,mw,mh);image(x,prefix+"wordminute.png","#minute","0","0",null,null,null);x.append("</Group>");
  }else{
   String[] digits=digitIds(s);
   for(int part=0;part<2;part++){
    int sz=part==0?size:msize,cell=(int)Math.ceil(sz*.62),glyphH=(int)Math.ceil(sz*1.28),pad=(int)Math.ceil(sz*.22),cw=cell+pad*2,ch=glyphH+pad*2;double gap=sz*s.n("spacing",0,-10,30)*.01;
    int color=part==0?ink:ink2;Paint paint=paint(s,color,endInk,sz,false);String file=part==0?"h":"m";
    for(int digit=0;digit<10;digit++)store(files,prefix+file+"_"+digit+".png",text(""+digit,paint,cw,ch,0,false,look(s,color,endInk,sz)));
    double width=2*cell+gap;open(x,s,name+file,part==0?hpos:mx,part==0?vpos:my,part==0?s.n("rotate",0,-180,180):mrot,width,glyphH);
    for(int i=0;i<2;i++)image(x,prefix+file+".png",digits[part*2+i],fmt(i*(cell+gap)-pad),fmt(-pad),fmt(cw),fmt(ch),null);
    x.append("</Group>");
   }
  }
  boolean anyMeta=s.b("showDate",true)||!s.s("caption","").isEmpty()||s.b("seconds",false)||s.info("battery")||s.info("alarm")||s.info("weather")||s.info("progress");
  if(anyMeta){double width=metaWidth(s);open(x,s,name+"d",Spec.n(me,"x",hpos,-80,80),Spec.n(me,"y",vpos+24,-10,80),Spec.n(me,"rot",0,-180,180),width,metaHeight(s));meta(x,s,prefix,ink,ink2,width,0,files);x.append("</Group>");}
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
    if(kind.equals("progress")){x.append("<Text x=\"").append(fmt(left+12)).append("\" y=\"").append(fmt(top+7)).append("\" size=\"26\" color=\"#ffffff\" text=\"Day\"/>");image(x,"progress-track.png",null,fmt(left+70),fmt(top+17),"100","7",null);image(x,"ink.png",null,fmt(left+70),fmt(top+17),"100*(#hour24*60+#minute)/1440","7",null);x.append("<Text x=\"").append(fmt(left+180)).append("\" y=\"").append(fmt(top+7)).append("\" size=\"26\" color=\"#ffffff\" format=\"%d%%\" paras=\"int((#hour24*60+#minute)/14.4)\"/>");}
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
   boolean moving=false,seconds=false,tilt=false;for(Spec s:slots){moving|=!s.s("movement","still").equals("still");seconds|=s.b("seconds",false);tilt|=s.s("movement","still").equals("tilt");}
   StringBuilder x=new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?><Lockscreen version=\"1\" screenWidth=\"1080\" frameRate=\"").append(moving?15:1).append("\" useVariableUpdater=\"Battery,DateTime.").append(seconds?"Second":"Minute").append("\"");
   if(base.s("bg","atmos").equals("atmos"))x.append(" showSysWallpaper=\"true\"");x.append("><Var name=\"notification\" expression=\"1\"/><Var name=\"studio_hidden\" expression=\"").append(base.b("startHidden",false)?1:0).append("\"/>");
   if(tilt)x.append("<VariableBinders><SensorBinder type=\"gravity\" rate=\"2\"><Variable name=\"studio_gravity_x\" index=\"0\"/><Variable name=\"studio_gravity_y\" index=\"1\"/></SensorBinder></VariableBinders>");
   String bgMode=base.choice("bg","atmos","atmos","photo","gradient","black");byte[] jpg=null;
   if(!bgMode.equals("atmos")){
    Bitmap bg=ThemeBuilder.wallpaper(bgMode.equals("photo")?photo:null,d.height);
    if(bgMode.equals("black"))bg.eraseColor(Color.BLACK);
    if(bgMode.equals("gradient")){int[] colors=gradient(base.s("gradient","dusk"));Paint p=new Paint();p.setShader(new LinearGradient(0,0,1080,d.height,colors,null,Shader.TileMode.CLAMP));new Canvas(bg).drawRect(0,0,1080,d.height,p);}
    ByteArrayOutputStream bytes=new ByteArrayOutputStream();bg.compress(Bitmap.CompressFormat.JPEG,92,bytes);jpg=bytes.toByteArray();bg.recycle();files.put("advance/bg.jpg",jpg);image(x,"bg.jpg",null,"0","0","#screen_width","#screen_height",null);
   }
   Bitmap pixel=Bitmap.createBitmap(1,1,Bitmap.Config.ARGB_8888);pixel.eraseColor(Color.BLACK);files.put("advance/dim.png",png(pixel));pixel.eraseColor(Color.WHITE);files.put("advance/ink.png",png(pixel));pixel.eraseColor(0x66ffffff);files.put("advance/progress-track.png",png(pixel));pixel.recycle();
   if(base.n("dim",0,0,100)>0)image(x,"dim.png",null,"0","0","#screen_width","#screen_height"," alpha=\""+Math.round(base.n("dim",0,0,100)*2.55*.8)+"\"");
   x.append("<Group name=\"studio_clock\" visibility=\"eq(#studio_hidden,0)\">");
   for(int i=0;i<slots.size();i++){
    Spec s=slots.get(i);String visibility=scheduled?(i==0?"gt(#hour24,5)*lt(#hour24,18)":i==1?"gt(#hour24,17)*lt(#hour24,23)":"gt(gt(#hour24,22)+lt(#hour24,6),0)"):"1";x.append("<Group visibility=\"").append(visibility).append("\">");
    if(s.b("warm",false)){x.append("<Group visibility=\"eq(").append(s.night()).append(",0)\">");time(x,s,names.get(i)+"/day/",s.color(false),s.color(true),files);x.append("</Group><Group visibility=\"").append(s.night()).append("\">");time(x,s,names.get(i)+"/warm/",mix(s.color(false),0xffffb070,.6f),(s.b("twoTone",false)?mix(s.color(true),0xffff9a55,.6f):mix(s.color(false),0xffffb070,.6f)),files);x.append("</Group>");}
    else time(x,s,names.get(i)+"/day/",s.color(false),s.color(true),files);x.append("</Group>");
   }x.append("</Group>");
   if(base.b("subjectOn",true)&&bgMode.equals("photo")&&subject!=null){Bitmap b=ThemeBuilder.subject(subject,d.height);store(files,"subject.png",b);int scale=base.n("subjScale",100,50,200);image(x,"subject.png",null,"#screen_width*(0.5+"+fmt(base.n("subjX",0,-30,30)/100d)+")-540*"+scale+"/100","#screen_height*(0.5+"+fmt(base.n("subjY",0,-30,30)/100d)+")-#screen_height*"+scale+"/200","1080*"+scale+"/100","#screen_height*"+scale+"/100",null);}
   x.append("<Button h=\"#screen_height\" w=\"#screen_width\"><Triggers><Trigger action=\"up\"><ExternCommand command=\"unlock\" condition=\"gt(#touch_begin_y-#touch_y,440)\"/></Trigger>");
   if(base.b("dbl",true))x.append("<Trigger action=\"double\"><VariableCommand name=\"studio_hidden\" expression=\"1-#studio_hidden\"/></Trigger>");x.append("</Triggers></Button></Lockscreen>");
   byte[] manifest=x.toString().getBytes("UTF-8");files.put("advance/manifest.xml",manifest);for(String ratio:new String[]{"11x5","13x6","19x9","20x9","2x1","301x135","403x180","67x30"})files.put("advance/"+ratio+"/manifest.xml",manifest);
   return ThemeBuilder.packageTheme(ThemeBuilder.wallpaperComponent(jpg==null?new byte[0]:jpg,!base.b("replaceWallpaper",false)||bgMode.equals("atmos")),zip(files));
  }catch(Exception e){if(e instanceof IOException)throw (IOException)e;throw new IOException("Studio theme: "+e.getMessage(),e);}
 }
 public static int[] gradient(String key){switch(key){case "aurora":return new int[]{0xff0f2b3a,0xff1d5c63,0xff3a2a6b};case "ember":return new int[]{0xff1a0f14,0xff5a1f2a,0xffc4602f};case "forest":return new int[]{0xff0b1a14,0xff17402f,0xff2d6a4a};case "ocean":return new int[]{0xff06121f,0xff0d3b66,0xff3a86c8};case "mono":return new int[]{0xff0c0d10,0xff22252d};default:return new int[]{0xff1f2340,0xff4a2f63,0xff8a4a6b};}}
}
