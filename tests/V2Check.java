import np.evan.lockscreen.*;
import java.util.*;
public final class V2Check {
 private static void check(boolean b,String text){if(!b)throw new AssertionError(text);}
 public static void main(String[] args)throws Exception{
  int cases=0;Set<String> shapes=new HashSet<>();
  for(String layout:StudioTheme.LAYOUTS)for(int size:new int[]{80,240,600})for(int spacing:new int[]{-10,2,30})for(int line:new int[]{60,88,140})for(int min:new int[]{30,100,130})for(int arc:new int[]{-100,0,100}){
   StudioTheme.Geometry g=StudioTheme.geometry(layout,size,spacing,line,min,arc);
   check(Double.isFinite(g.width)&&Double.isFinite(g.height)&&g.width>0&&g.height>0,"Invalid "+layout+" bounds");
   check(g.words||g.analog||g.pieces.size()>=4,"Clock missing digits");if(g.analog)check(g.width==g.height&&g.width==size*1.8,"Analog dial must be square");
   for(StudioTheme.Piece p:g.pieces)check(Double.isFinite(p.x)&&Double.isFinite(p.y)&&p.x>=0&&p.y>=0&&p.scale>0,"Invalid digit transform");cases++;
  }
  for(String layout:StudioTheme.LAYOUTS){StudioTheme.Geometry g=StudioTheme.geometry(layout,240,2,88,100,55);StringBuilder key=new StringBuilder(g.width+":"+g.height+":"+g.rotate+":"+g.words+":"+g.divider);for(StudioTheme.Piece p:g.pieces)key.append(':').append(p.x).append('/').append(p.y);check(shapes.add(key.toString()),"Two layout choices produce the same arrangement: "+layout);}
  final int[] restored={0};ApplyRecovery recovery=new ApplyRecovery();recovery.arm(()->restored[0]++);check(recovery.pending(),"Confirmation not armed");boolean rejected=false;try{recovery.arm(()->{});}catch(IllegalStateException e){rejected=true;}check(rejected,"A pending rollback must not be replaced");check(recovery.keep(),"Keep failed");check(!recovery.pending()&&restored[0]==0,"Keeping ran rollback");recovery.revert();check(restored[0]==0,"Kept theme reverted");recovery.arm(()->restored[0]++);recovery.revert();check(restored[0]==1&&recovery.status().equals("restored"),"Revert did not restore previous theme");
  check(StudioTheme.LAYOUTS.length==16&&StudioTheme.FINISHES.length==17,"Expected 16 layouts and 17 finishes");for(String l:StudioTheme.NO_SPLIT)check(Arrays.asList(StudioTheme.LAYOUTS).contains(l),"Unknown no-split layout "+l);
  System.out.println("Validated "+cases+" V2 layout combinations, "+StudioTheme.LAYOUTS.length+" distinct arrangements and real confirmation/rollback state transitions.");
 }
}
