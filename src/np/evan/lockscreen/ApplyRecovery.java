package np.evan.lockscreen;
import java.util.concurrent.*;
/** Timed rollback lives in the Shizuku process, including when the editor closes. */
public final class ApplyRecovery {
 public interface Action {void run()throws Exception;}
 private final ScheduledExecutorService timer=Executors.newSingleThreadScheduledExecutor(r->{Thread t=new Thread(r,"studio-recovery");t.setDaemon(true);return t;});
 private ScheduledFuture<?> task;private Action rollback;private long until=0;private String state="idle";
 public synchronized long arm(Action action){if(pending())throw new IllegalStateException("Confirm or revert the previous theme first");rollback=action;state="pending";until=System.currentTimeMillis()+15000;task=timer.schedule(()->{try{revert();}catch(Exception ignored){}},15,TimeUnit.SECONDS);return until;}
 public synchronized boolean pending(){return state.equals("pending")||state.equals("reverting");}
 public synchronized boolean keep(){if(!state.equals("pending"))return false;task.cancel(false);until=0;state="kept";rollback=null;return true;}
 public void revert()throws Exception{
  Action action;synchronized(this){if(!state.equals("pending"))return;task.cancel(false);until=0;state="reverting";action=rollback;rollback=null;}
  try{action.run();synchronized(this){state="restored";}}catch(Exception e){synchronized(this){state="failed: "+e.getMessage();}throw e;}
 }
 public synchronized String status(){return state;}
}
