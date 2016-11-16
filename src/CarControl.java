//Prototype implementation of Car Control
//Mandatory assignment
//Course 02158 Concurrent Programming, DTU, Fall 2016

//Hans Henrik Lovengreen    Oct 3, 2016


import java.awt.Color;

class Barrier {
		
	Semaphore	mutex	= new Semaphore(1), // Mutual exclusion semaphore
				enter	= new Semaphore(0), // Enter barrier semaphore
				leave	= new Semaphore(0); // Leave barrier semaphore
				
	int 		enterCount	= 0, // Cars currently in enter phase
				leaveCount	= 0; // Cars currently in leave phase
	
	boolean		isOn	= false; // Barrier on/off switch
	
	public void sync() throws InterruptedException {
		mutex.P();
		if (isOn) {
			// The last car hands out enter coconuts	
			if (enterCount == 8) for (; enterCount > 0; enterCount--) enter.V();
			else { // Wait for enter coconut
				enterCount++; mutex.V(); enter.P(); 
				mutex.P(); // Get mutex again before continuing
			}
			// The last car hands out leave coconuts
			if (leaveCount == 8) {
				for (; leaveCount > 0; leaveCount--) leave.V();
				mutex.V();
			}
			else { // Wait for leave coconut
				leaveCount++; mutex.V(); leave.P();
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
		// Hand out coconuts for everyone waiting
		for (int i =              enterCount; i > 0; i--) enter.V();
		for (int i = leaveCount + enterCount; i > 0; i--) leave.V();
		// Reset counts
		enterCount = 0;
		leaveCount = 0;
		mutex.V();		
	} 

}


class BarrierMonitor extends Barrier {
				
	int			enterCount	= 0, // Cars currently in enter phase
				leaveCount	= 0; // Cars currently in leave phase
	
	boolean		isOn = false; // Barrier on/off switch

	public synchronized void sync() throws InterruptedException {
		if (!isOn) return;

		enterCount++;
		
		// Last car notifies all
		if (enterCount == 9) {
			leaveCount = 0;
			notifyAll();		
		} // Wait
		else while (enterCount < 9) wait();
		
		// Added to easily release cars when barrier is turned off
		if (!isOn) return;
		
		leaveCount++;

		// Last car notifies all
		if (leaveCount == 9) {
			enterCount = 0;
			notifyAll();
		} // Wait
		else while (leaveCount < 9) wait();
	}
	
	public synchronized void on() throws InterruptedException {
		isOn = true;
		enterCount = 0;
		leaveCount = 0;
	}

	public synchronized void off() throws InterruptedException {
		isOn = false;
		enterCount = 9;
		leaveCount = 9;
		notifyAll();
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
	
	Semaphore	mutex 	= new Semaphore(1), // Mutual exclusion semaphore
			  	up 		= new Semaphore(0), // Going up semaphore
			  	down 	= new Semaphore(0); // Going down semaphore
	
	int 		upCount 	= 0, // Children going up
				downCount 	= 0, // Children going down
				upDelayed 	= 0, // Children delayed up
				downDelayed = 0; // Children delayed down
	
	
	public void enter(int no) throws InterruptedException {
		if (no < 5)	// Going down
		{
			mutex.P();
			// Wait if someone is going in the opposite direction
			if (upCount > 0) { downDelayed++; mutex.V(); down.P(); }
			downCount++;
			// Pass the baton
			if (downDelayed > 0) { downDelayed--; down.V();	}
			else mutex.V();
		}
		else 		// Going up
		{
			mutex.P();
			// Wait if someone is going in the opposite direction
			if (downCount > 0) { upDelayed++; mutex.V(); up.P(); }
			upCount++;
			// Pass the baton 
			if (upDelayed > 0) { upDelayed--; up.V(); }
			else mutex.V();
		}
	}
	
	public void leave(int no) throws InterruptedException {
		if (no < 5)	// Going down
		{
			mutex.P();
			downCount--;
			// Pass the baton
			if (downCount == 0 && upDelayed > 0) { upDelayed--; up.V();	}
			else mutex.V();
		}
		else		// Going up
		{
			mutex.P();
			upCount--;
			// Pass the baton
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
		if (no < 5)	// Going down
		{
			// Wait if someone is going in the opposite direction
			if (upCount > 0) { downDelayed++;  while(upCount > 0) wait(); }
			downCount++;
			// Pass the baton
			if (downDelayed > 0) { downDelayed--; notify();	}
			
		}
		else 		// Going up
		{			
			// Wait if someone is going in the opposite direction
			if (downCount > 0) { upDelayed++;  while(downCount > 0) wait(); }
			upCount++;
			// Pass the baton
			if (upDelayed > 0) { upDelayed--; notify(); }
			
		}
	}
	
	public synchronized void leave(int no) throws InterruptedException {
		if (no < 5)
		{			
			downCount--;
			// Pass the baton		
			if (downCount == 0 && upDelayed > 0) { upDelayed--; notify(); }			
		}
		else
		{			
			upCount--;
			// Pass the baton	
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
			// Wait if someone is going in the opposite direction
			// OR you have been told to wait
			if (upCount > 0 || downWait) { downDelayed++;  while(upCount > 0 || downWait) wait(); }
			downCount++;
		}
		else 
		{
			// Wait if someone is going in the opposite direction
			// OR you have been told to wait
			if (downCount > 0 || upWait) { upDelayed++; while(downCount > 0 || upWait) wait(); }
			upCount++;	
		}
	}
	
	public synchronized void leave(int no) throws InterruptedException {
		if (no < 5)
		{			
			downCount--;
			// Let the opposite direction know its their turn
			if (downCount == 0 && upDelayed > 0) { upDelayed = 0; upWait = false; notifyAll(); }	
			// Switch priority if someone is waiting in the opposite direction
			else if (upDelayed > 0) downWait = true; 		
		}
		else
		{			
			upCount--;
			// Let the opposite direction know its their turn
			if (upCount == 0 && downDelayed > 0) { downDelayed = 0; downWait = false; notifyAll(); }	
			// Switch priority if someone is waiting in the opposite direction
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
        barrier = new BarrierMonitor();
        
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






