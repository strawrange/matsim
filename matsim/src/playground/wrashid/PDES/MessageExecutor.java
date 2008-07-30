package playground.wrashid.PDES;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class MessageExecutor extends Thread {
	int id=0;
	private Message message;
	private Scheduler scheduler;
	//public volatile boolean hasAqiredLocks=false;
	public Lock lock1=new ReentrantLock();
	public Lock lock2=new ReentrantLock();
	public Condition hasAcquiredLock=lock1.newCondition();
	public Condition mayStart=lock2.newCondition();

    private static ThreadLocal simTime = new ThreadLocal();

    public static double getSimTime() {
        return ((Double) (simTime.get())).doubleValue();
    }

    public static void setSimTime(double obj) {
    	simTime.set(obj);
    }


	public MessageExecutor(Message message){
		this.message=message;
	}
	
	public MessageExecutor(int id){
		this.id=id;
	}

	public void run() {
		MessageExecutor.setSimTime(0);
		boolean acquiredLock=false;
		
		// of course here is a problem:
		// we first check if empty and then try to get the element. For this reason
		// several threads may try to get the same message
		// TODO: in order to improve this, we could leave out this check and instead make a try/catch
		// This would also improve performance.
		
		try{
			while (true || getSimTime()<SimulationParameters.maxSimulationLength){
				// this is needed, because for the same street the lock should happen before the next road can do the lock
				
				//while (scheduler.queue.getCounter()%(this.id+1)!=0 && !scheduler.queue.isEmpty()){
					
				//}
				
				
				// attention: race condition. It might happen, that before we do the lock, the later message aquires the lock (because we go sleeping)
				
				
				acquiredLock=false;
				synchronized (scheduler){
					message=scheduler.queue.getNextMessage();
					if (message==null){
						break;
					}
					executeMessage();
				}				
				
				

				
				
				/*
				 //Method, which should function, but does not...
				acquiredLock=false;
				synchronized (scheduler){
					message=scheduler.queue.getNextMessage();
					if (message==null){
						break;
					}
					acquiredLock=message.lock.tryLock();
					if (!acquiredLock){
						((Road)message.receivingUnit).waitingOnLock.add(message);
					}
				}
				
				
				if (!acquiredLock){
					while(((Road)message.receivingUnit).waitingOnLock.peek()!=message){}
					while (!message.lock.tryLock()){}
					((Road)message.receivingUnit).waitingOnLock.poll();
				}
				
				executeMessage();
				message.lock.unlock();
				
*/


				/*
				scheduler.lock.lock();
				
				System.out.println("start:"+id);
				
				for (int i=0;i<100000000;i++){
					
				}
				
				System.out.println("end:"+id);
				
				scheduler.lock.unlock();
				*/
			}
		} catch (java.lang.NullPointerException npe){
			// ignore this exception, because it comes from the fact, that we do not check 'scheduler.queue.isEmpty()'
			npe.printStackTrace();
		} catch (Exception e){
			e.printStackTrace();
		} finally {
		}
		
		scheduler.timer.endTimer();
		scheduler.timer.printMeasuredTime("ThreadId-"+id + ": ");
	}

	private void executeMessage(){
		MessageExecutor.setSimTime(message.getMessageArrivalTime());
		message.printMessageLogString();
		if (message instanceof SelfhandleMessage){
			((SelfhandleMessage) message).selfhandleMessage();
		} else {
			message.receivingUnit.handleMessage(message);
		}
	}

	public void setMessage(Message message) {
		this.message = message;
	}

	public void setScheduler(Scheduler scheduler) {
		this.scheduler = scheduler;
	}

}
