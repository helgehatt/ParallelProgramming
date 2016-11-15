//Prototype implementation of Car Control
//Mandatory assignment
//Course 02158 Concurrent Programming, DTU, Fall 2016

//Hans Henrik Lovengreen    Oct 3, 2016


import java.awt.Color;

class Barrier {
		
	Semaphore	mutex	= new Semaphore(1),
				enter	= new Semaphore(0),
				leave	= new Semaphore(0);
				
	int 		enterCount	= 0,
				leaveCount	= 0;
	
	boolean		isOn	= false;
	
	public void sync() throws InterruptedException {
		mutex.P();
		if (isOn) {
			if (enterCount == 8) {
				for (; enterCount > 0; enterCount--) enter.V();
			} else { 
				enterCount++; 
				mutex.V();
				enter.P();
				mutex.P();
			}
			
			if (leaveCount == 8) {
				for (; leaveCount > 0; leaveCount--) leave.V();
				mutex.V();
			} else { 
				leaveCount++; 
				mutex.V();
				leave.P(); 
			}
		} else { mutex.V(); }
	}

	public void on() throws InterruptedException {
		mutex.P();
		isOn = true;
		mutex.V();		
	}

	public void off() throws InterruptedException {
		mutex.P();
		isOn = false;
		for (int i =          enterCount; i > 0; i--) enter.V();
		for (int i = leaveCount + enterCount; i > 0; i--) leave.V();
		enterCount = 0;
		leaveCount = 0;
		mutex.V();		
	} 

}


class BarrierMonitor extends Barrier {
				
	int			enterCount	= 0,
				leaveCount	= 0;
	
	Object		enterLock = new Object();
	Object		leaveLock = new Object();
	
	boolean		isOn = false;
	
	public void sync() throws InterruptedException {
		
		synchronized (enterLock) {
			if (!isOn) return;
			
			if (enterCount == 8) {
				enterCount = 0;
				enterLock.notifyAll();
			} else {
				enterCount++;
				enterLock.wait();
			}	
			if (!isOn) return;
		}
		
		synchronized (leaveLock) {
			if (leaveCount == 8) {
				leaveCount = 0;
				leaveLock.notifyAll();
			} else {
				leaveCount++;
				leaveLock.wait();
			}	
		}		
	}

	public void on() throws InterruptedException {
		synchronized (enterLock) {
			isOn = true;			
		}		
	}

	public void off() throws InterruptedException {
		synchronized (enterLock) {
			isOn = false;
			enterLock.notifyAll();	
		}
		
		synchronized (leaveLock) {
			leaveLock.notifyAll();
		}		
	}
}


class BarrierThreshold extends Barrier {
	
	Semaphore	mutex	= new Semaphore(1),
				enter	= new Semaphore(0),
				leave	= new Semaphore(0),
				tWait	= new Semaphore(0);
			
	int 		enterCount	= 0,
				leaveCount	= 0,
				threshold	= 9;

	boolean		isOn		= false,
				tWaiting	= false;
	
	public void sync() throws InterruptedException {
		mutex.P();
		if (isOn) {
			if (enterCount == threshold-1) {
				for (; enterCount > 0; enterCount--) enter.V();
			} else { 
				enterCount++; 
				mutex.V();
				enter.P();
				mutex.P();
			}
			
			if (leaveCount == threshold-1) {
				for (; leaveCount > 0; leaveCount--) leave.V();
				if (tWaiting) { tWaiting = false; tWait.V(); }
				mutex.V();
			} else { 
				leaveCount++; 
				mutex.V();
				leave.P(); 
			}
		} else { mutex.V(); }
	}  // Wait for others to arrive (if barrier active)

	public void on() throws InterruptedException {
		mutex.P();
		isOn = true;
		mutex.V();
		
	}    // Activate barrier

	public void off() throws InterruptedException {
		mutex.P();
		isOn = false;
		for (int i =        	  enterCount; i > 0; i--) enter.V();
		for (int i = leaveCount + enterCount; i > 0; i--) leave.V();
		enterCount = 0;
		leaveCount = 0;
		if (tWaiting){ tWaiting = false; tWait.V(); }
		mutex.V();
	}   // Deactivate barrier
	
	public void set(int k) throws InterruptedException{
		mutex.P();
		if (1 < k && k < 10){
			if (k < threshold){
				threshold = k;
				while (k <= enterCount+leaveCount){
					for (int i = 0; i < k; i++){
						if (leaveCount > 0) {
							leaveCount--;
							leave.V();
						}
						else {
							enterCount--;
							enter.V();
							leaveCount--;
							leave.V();
						}
					}
				}
			}
			else if (threshold < k){
				if (0 < leaveCount+enterCount){
					tWaiting = true;
					mutex.V();
					tWait.P();
					mutex.P();
				}
				threshold = k;
			}
		}
		mutex.V();
	}
}

class Alley {
	
	Semaphore	mutex 	= new Semaphore(1), // Semaphore for critical region
			  	up 		= new Semaphore(0),
			  	down 	= new Semaphore(0);
	
	int 		upCount 	= 0, // Children going up
				downCount 	= 0, // Children going down
				upDelayed 	= 0, // Children delayed up
				downDelayed = 0; // Children delayed down
	
	
	public void enter(int no) throws InterruptedException {
		if (no < 5)
		{
			mutex.P();
			if (upCount > 0) { downDelayed++; mutex.V(); down.P(); }
			downCount++;
			if (downDelayed > 0) { downDelayed--; down.V();	}
			else mutex.V();
		}
		else 
		{
			mutex.P();
			if (downCount > 0) { upDelayed++; mutex.V(); up.P(); }
			upCount++;
			if (upDelayed > 0) { upDelayed--; up.V(); }
			else mutex.V();
		}
	}
	
	public void leave(int no) throws InterruptedException {
		if (no < 5)
		{
			mutex.P();
			downCount--;			
			if (downCount == 0 && upDelayed > 0) { upDelayed--; up.V();	}
			else mutex.V();
		}
		else
		{
			mutex.P();
			upCount--;			
			if (upCount == 0 && downDelayed > 0) { downDelayed--; down.V();	}
			else mutex.V();
		}		
	}
}


class AlleyMonitor extends Alley {
		
	int 	upCount 	= 0, // Children going up
			downCount 	= 0, // Children going down
			upDelayed 	= 0, // Children delayed up
			downDelayed = 0; // Children delayed down
	
	
	public synchronized void enter(int no) throws InterruptedException {
		if (no < 5)
		{			
			if (upCount > 0) { downDelayed++;  while(upCount > 0) wait(); }
			downCount++;
			if (downDelayed > 0) { downDelayed--; notify();	}
			
		}
		else 
		{			
			if (downCount > 0) { upDelayed++;  while(downCount > 0) wait(); }
			upCount++;
			if (upDelayed > 0) { upDelayed--; notify(); }
			
		}
	}
	
	public synchronized void leave(int no) throws InterruptedException {
		if (no < 5)
		{			
			downCount--;			
			if (downCount == 0 && upDelayed > 0) { upDelayed--; notify(); }			
		}
		else
		{			
			upCount--;			
			if (upCount == 0 && downDelayed > 0) { downDelayed--; notify();	}			
		}		
	}
}

class AlleyMonitorFair extends Alley {
	
	int 		upCount 	= 0, // Children going up
				downCount 	= 0, // Children going down
				upDelayed 	= 0, // Children delayed up
				downDelayed = 0; // Children delayed down	
	boolean 	upWait 		= false, // Children going up should wait
				downWait 	= false; // Children going down should wait
	

	public synchronized void enter(int no) throws InterruptedException {
		if (no < 5)
		{
			if (upCount > 0 || downWait) { downDelayed++;  while(upCount > 0 || downWait) wait(); }
			downCount++;
		}
		else 
		{			
			if (downCount > 0 || upWait) { upDelayed++; while(downCount > 0 || upWait) wait(); }
			upCount++;	
		}
	}
	
	public synchronized void leave(int no) throws InterruptedException {
		if (no < 5)
		{			
			downCount--;
			if (downCount == 0 && upDelayed > 0) { upDelayed = 0; upWait = false; notifyAll(); }	
			else if (upDelayed > 0) downWait = true; 		
		}
		else
		{			
			upCount--;
			if (upCount == 0 && downDelayed > 0) { downDelayed = 0; downWait = false; notifyAll(); }	
			else if (downDelayed > 0) upWait = true; 				
		}		
	}
}

class Gate {

    Semaphore g = new Semaphore(0);
    Semaphore e = new Semaphore(1);
    boolean isopen = false;

    public void pass() throws InterruptedException {
        g.P(); 
        g.V();
    }

    public void open() {
        try { e.P(); } catch (InterruptedException e) {}
        if (!isopen) { g.V();  isopen = true; }
        e.V();
    }

    public void close() {
        try { e.P(); } catch (InterruptedException e) {}
        if (isopen) { 
            try { g.P(); } catch (InterruptedException e) {}
            isopen = false;
        }
        e.V();
    }

}

class Car extends Thread {

    int basespeed = 100;             // Rather: degree of slowness
    int variation =  50;             // Percentage of base speed

    CarDisplayI cd;                  // GUI part

    int no;                          // Car number
    Pos startpos;                    // Startpositon (provided by GUI)
    Pos barpos;                      // Barrierpositon (provided by GUI)
    Color col;                       // Car  color
    Gate mygate;                     // Gate at startposition
    Semaphore[][] tiles;
    Alley alley;
    boolean inAlley;
    Barrier barrier;
    boolean running;


    int speed;                       // Current car speed
    Pos curpos;                      // Current position 
    Pos newpos;                      // New position to go to
    
    Pos enter;
    Pos leave;

    public Car(int no, CarDisplayI cd, Gate g, Semaphore[][] tiles, Alley alley, Barrier barrier) {

        this.no = no;
        this.cd = cd;
        mygate = g;
        this.tiles = tiles;
        this.alley = alley;
        inAlley = false;
        this.barrier = barrier;
        startpos = cd.getStartPos(no);
        barpos = cd.getBarrierPos(no);  // For later use
        running = true;

        col = chooseColor();

        // do not change the special settings for car no. 0
        if (no==0) {
            basespeed = 0;  
            variation = 0; 
            setPriority(Thread.MAX_PRIORITY); 
        }
        
        switch(no) {
        case 1:
        case 2:
        	enter = new Pos(2,1);
        	leave = new Pos(9,1);
        	break;
        case 3:
        case 4:
        	enter = new Pos(1,3);
        	leave = new Pos(9,1);
        	break;
        default:
        	enter = new Pos(10,0);
        	leave = new Pos(0,2);
        }
    }

    public synchronized void setSpeed(int speed) { 
        if (no != 0 && speed >= 0) {
            basespeed = speed;
        }
        else
            cd.println("Illegal speed settings");
    }

    public synchronized void setVariation(int var) { 
        if (no != 0 && 0 <= var && var <= 100) {
            variation = var;
        }
        else
            cd.println("Illegal variation settings");
    }

    synchronized int chooseSpeed() { 
        double factor = (1.0D+(Math.random()-0.5D)*2*variation/100);
        return (int) Math.round(factor*basespeed);
    }

    private int speed() {
        // Slow down if requested
        final int slowfactor = 3;  
        return speed * (cd.isSlow(curpos)? slowfactor : 1);
    }

    Color chooseColor() { 
        return Color.blue; // You can get any color, as longs as it's blue 
    }

    Pos nextPos(Pos pos) {
        // Get my track from display
        return cd.nextPos(no,pos);
    }

    boolean atGate(Pos pos) {
        return pos.equals(startpos);
    }
    
    boolean atEnter(Pos pos) {
    	return pos.equals(enter);
    }
    
    boolean atLeave(Pos pos) {
    	return pos.equals(leave);
    }
    
    boolean atBarrier(Pos pos) {
    	return pos.equals(barpos);
    }

   public void run() {
        try {

            speed = chooseSpeed();
            curpos = startpos;
            cd.mark(curpos,col,no);

            while (running) {
                sleep(speed());
            	
                newpos = nextPos(curpos);
  
                if (atGate(curpos)) { 
                    mygate.pass(); 
                    speed = chooseSpeed();
                } else if (atEnter(curpos)) {
                	alley.enter(no);
                	inAlley = true;
                } else if (atLeave(curpos)) {
                	alley.leave(no);
                	inAlley = false;
                } else if (atBarrier(curpos)) {
                	barrier.sync();
                }
                
                tiles[newpos.row][newpos.col].P();
                
                //  Move to new position 
                cd.clear(curpos);
                cd.mark(curpos,newpos,col,no);
                sleep(speed());
                cd.clear(curpos,newpos);
                cd.mark(newpos,col,no);

                tiles[curpos.row][curpos.col].V();
                
                curpos = newpos;
            }

            // Remove car
        	if (inAlley)
    			try { alley.leave(no); } catch (InterruptedException e) { }
        	cd.clear(curpos);
            tiles[curpos.row][curpos.col].V();

        } catch (Exception e) {
            cd.println("Exception in Car no. " + no);
            System.err.println("Exception in Car no. " + no + ":" + e);
            e.printStackTrace();
        }
    }



}

public class CarControl implements CarControlI{

    CarDisplayI cd;           // Reference to GUI
    Car[]  car;               // Cars
    Gate[] gate;              // Gates
    Semaphore[][] tiles;
    Alley alley;
    Barrier barrier;

    public CarControl(CarDisplayI cd) {
        this.cd = cd;
        car  = new  Car[9];
        gate = new Gate[9];
        tiles = new Semaphore[11][12];
        alley = new AlleyMonitorFair(); 
        barrier = new BarrierThreshold();
        
        for (int x = 0; x < tiles.length; x++) {
        	for (int y = 0; y < tiles[0].length; y++) {
        		tiles[x][y] = new Semaphore(1);
        	}
        }

        for (int no = 0; no < 9; no++) {
            gate[no] = new Gate();
            car[no] = new Car(no,cd,gate[no], tiles, alley, barrier);
            car[no].start();
        }
    }

   public void startCar(int no) {
        gate[no].open();
    }

    public void stopCar(int no) {
        gate[no].close();
    }

    public void barrierOn() { 
    	try { barrier.on(); } catch (InterruptedException e) { }
    }

    public void barrierOff() { 
    	try { barrier.off(); } catch (InterruptedException e) { }
    }

    public void barrierSet(int k) { 
        if (barrier instanceof BarrierThreshold)
			try {
				((BarrierThreshold) barrier).set(k);
			} catch (InterruptedException e1) {}
        else
        	cd.println("Barrier threshold setting not implemented in this version");
     }

    public void removeCar(int no) {
    	car[no].running = false;
    }

    public void restoreCar(int no) {
    	if (car[no].running == true) return;
    	car[no] = new Car(no, cd, gate[no], tiles, alley, barrier);
    	car[no].start();
    }

    /* Speed settings for testing purposes */

    public void setSpeed(int no, int speed) { 
        car[no].setSpeed(speed);
    }

    public void setVariation(int no, int var) { 
        car[no].setVariation(var);
    }

}






