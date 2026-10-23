import np.evan.lockscreen.*;
import java.util.*;
public final class V2Check {
 private static void check(boolean b,String text){if(!b)throw new AssertionError(text);}
 public static void main(String[] args)throws Exception{
  int cases=0;Set<String> shapes=new HashSet<>();
  for(String layout:StudioTheme.LAYOUTS)for(int size:new int[]{80,240,600})for(int spacing:new int[]{-10,2,30})for(int line:new int[]{60,88,140})for(int min:new int[]{30,100,130})for(int arc:new int[]{-100,0,100}){
   StudioTheme.Geometry g=StudioTheme.geometry(layout,size,spacing,line,min,arc);
   check(Double.isFinite(g.width)&&Double.isFinite(g.height)&&g.width>0&&g.height>0,"Invalid "+layout+" bounds");
   check(g.words||g.analog||g.rings||g.pieces.size()>=4,"Clock missing digits");if(g.analog||g.rings)check(g.width==g.height&&g.width==size*1.8,"Dial layouts must be square");
   for(StudioTheme.Piece p:g.pieces)check(Double.isFinite(p.x)&&Double.isFinite(p.y)&&p.x>=0&&p.y>=0&&p.scale>0,"Invalid digit transform");cases++;
  }
  for(String layout:StudioTheme.LAYOUTS){StudioTheme.Geometry g=StudioTheme.geometry(layout,240,2,88,100,55);StringBuilder key=new StringBuilder(g.width+":"+g.height+":"+g.rotate+":"+g.words+":"+g.divider+":"+g.analog+":"+g.rings);for(StudioTheme.Piece p:g.pieces)key.append(':').append(p.x).append('/').append(p.y);check(shapes.add(key.toString()),"Two layout choices produce the same arrangement: "+layout);}
  final int[] restored={0};ApplyRecovery recovery=new ApplyRecovery();recovery.arm(()->restored[0]++);check(recovery.pending(),"Confirmation not armed");boolean rejected=false;try{recovery.arm(()->{});}catch(IllegalStateException e){rejected=true;}check(rejected,"A pending rollback must not be replaced");check(recovery.keep(),"Keep failed");check(!recovery.pending()&&restored[0]==0,"Keeping ran rollback");recovery.revert();check(restored[0]==0,"Kept theme reverted");recovery.arm(()->restored[0]++);recovery.revert();check(restored[0]==1&&recovery.status().equals("restored"),"Revert did not restore previous theme");
  check(StudioTheme.LAYOUTS.length==17&&StudioTheme.FINISHES.length==17,"Expected 17 layouts and 17 finishes");for(String l:StudioTheme.NO_STRETCH)check(Arrays.asList(StudioTheme.LAYOUTS).contains(l),"Unknown no-stretch layout "+l);
  // Day counters: the Java day number matches java.time, and the engine expression gives the same number.
  for(java.time.LocalDate d=java.time.LocalDate.of(1999,12,25);d.isBefore(java.time.LocalDate.of(2032,3,5));d=d.plusDays(37)){
   long want=d.toEpochDay()+2440588;check(StudioTheme.jdn(d.getYear(),d.getMonthValue(),d.getDayOfMonth())==want,"Day number for "+d);
   String e=StudioTheme.TODAY.replace("#year",""+d.getYear()).replace("#month",""+(d.getMonthValue()-1)).replace("#date",""+d.getDayOfMonth());check(new Expr(e).eval()==want,"Engine day number for "+d);
  }
  check(StudioTheme.jdn(2027,2,14)-StudioTheme.jdn(2026,10,23)==114,"Days between dates");
  long today=StudioTheme.jdn(2026,10,23);
  String yearly=StudioTheme.countdown(2000,2,14,true),once=StudioTheme.countdown(2026,12,25,false);
  check(new Expr(day(yearly,2026,10,23,today)).eval()==114,"Yearly counter rolls over to next year");
  check(new Expr(day(yearly,2027,2,14,StudioTheme.jdn(2027,2,14))).eval()==0,"Yearly counter is 0 on the day");
  check(new Expr(day(StudioTheme.countdown(2000,12,25,true),2026,10,23,today)).eval()==63,"Yearly counter later this year");
  check(new Expr(day(once,2026,10,23,today)).eval()==63&&new Expr(day(once,2027,1,1,StudioTheme.jdn(2027,1,1))).eval()<0,"One-off counter goes negative after the date");
  for(int state=0;state<5;state++)check(new Expr(StudioTheme.CHARGING.replace("#battery_state",""+state)).eval()==(state==1||state==3?1:0),"Charging condition for state "+state);for(String l:StudioTheme.NO_SPLIT)check(Arrays.asList(StudioTheme.LAYOUTS).contains(l),"Unknown no-split layout "+l);
  System.out.println("Validated "+cases+" V2 layout combinations, day-counter maths, "+StudioTheme.LAYOUTS.length+" distinct arrangements and real confirmation/rollback state transitions.");
 }
 private static String day(String e,int y,int m,int d,long jdn){return e.replace("#studio_day",""+jdn).replace("#year",""+y).replace("#month",""+(m-1)).replace("#date",""+d);}
 /** Minimal evaluator for the engine's integer expressions: + - * / ( ), int() and the comparison functions used. */
 private static final class Expr {
  private final String t;private int i;Expr(String t){this.t=t.replace(" ","");}
  long eval(){long v=sum();if(i!=t.length())throw new AssertionError("Unparsed: "+t.substring(i));return v;}
  private long sum(){long v=prod();while(i<t.length()&&(t.charAt(i)=='+'||t.charAt(i)=='-')){char o=t.charAt(i++);long r=prod();v=o=='+'?v+r:v-r;}return v;}
  private long prod(){long v=atom();while(i<t.length()&&(t.charAt(i)=='*'||t.charAt(i)=='/')){char o=t.charAt(i++);long r=atom();v=o=='*'?v*r:v/r;}return v;}
  private long atom(){
   for(String f:new String[]{"int","ifelse","lt","ge","gt","eq","min"})if(t.startsWith(f+"(",i)){i+=f.length()+1;List<Long> a=new ArrayList<>();a.add(sum());while(t.charAt(i)==','){i++;a.add(sum());}if(t.charAt(i++)!=')')throw new AssertionError("Missing )");
    switch(f){case "int":return a.get(0);case "ifelse":return a.get(0)!=0?a.get(1):a.get(2);case "lt":return a.get(0)<a.get(1)?1:0;case "ge":return a.get(0)>=a.get(1)?1:0;case "gt":return a.get(0)>a.get(1)?1:0;case "eq":return a.get(0).equals(a.get(1))?1:0;default:return Math.min(a.get(0),a.get(1));}}
   if(t.charAt(i)=='('){i++;long v=sum();if(t.charAt(i++)!=')')throw new AssertionError("Missing )");return v;}
   if(t.charAt(i)=='-'){i++;return -atom();}
   int s=i;while(i<t.length()&&Character.isDigit(t.charAt(i)))i++;if(s==i)throw new AssertionError("Bad token at "+t.substring(s));return Long.parseLong(t.substring(s,i));
  }
 }
}
