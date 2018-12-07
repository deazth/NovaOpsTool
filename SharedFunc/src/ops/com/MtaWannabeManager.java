/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ops.com;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author S53788
 */
public class MtaWannabeManager {
  private ThreadPoolExecutor tpe;
  
  /**
   * Create the manager with specified pool size <br />
   * At most, only the specified number of thread will execute at any given time
   * @param threadPoolSize 
   */
  public MtaWannabeManager(int threadPoolSize){
    tpe = (ThreadPoolExecutor) Executors.newFixedThreadPool(threadPoolSize);
  }
  
  /**
   * Queue the job
   * @param thread 
   */
  public void addJob(Runnable thread){
    tpe.execute(thread);
  }
  
  /**
   * Stop accepting new thread and block the program until the task completed<br />
   * or until it timed out.
   * @param timeout how long should it wait
   * @param unit the unit of that how long (sec, minute, or even days)
   * @return true if all queued task are completed. false if it timed out first
   */
  public boolean waitJobToComplete(long timeout, TimeUnit unit){
    tpe.shutdown();
    try {
      return tpe.awaitTermination(timeout, unit);
    } catch (InterruptedException e) {
      
      return false;
    }
    
  }
}
