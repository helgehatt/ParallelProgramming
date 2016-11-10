//Prototype implementation of Car Control
//Mandatory assignment
//Course 02158 Concurrent Programming, DTU, Fall 2016

//Hans Henrik Lovengreen    Oct 3, 2016


import java.awt.Color;

class Barrier {
		
	Semaphore	mutex	= new Semaphore(1),
				wait	= new Semaphore(0);
				
	int 		count	= 0;
	boolean		isOn	= false;
	
	public void sync() throws InterruptedException {
		mutex.P();
		if (isOn) {
			if (count == 8) {
				for (; count > 0; count--) wait.V();
				mutex.V();
			} else { count++; mutex.V(); wait.P(); }
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
		for(; 0 < count; count--) wait.V();
		mutex.V();		
	} 

}


class BarrierMonitor extends Barrier {
	
				
	int 		count	= 0;
	boolean		isOn	= false;
	
	boolean 	synced 	= false;
	
	public synchronized void sync() throws InterruptedException {
		
		if (isOn) {
			while (synced) wait();
			
			count++;
			
			if(count == 9) {
				synced = true;
			} else while (!synced) wait();
			
			if (synced) {
				notify();
				count--;				
				if (count == 0) synced = false;
			}			
		}		
	}

	public synchronized void on() throws InterruptedException {		
		isOn = true;		
	}

	public synchronized void off() throws InterruptedException {		
		isOn = false;
		if (count > 0) {
			synced = true;
			notify();				
		}
	}
}


class Alley {
	
	Semaphore	mutex 	= new Semaphore(1), // Semaphore for critical region
			  	up 		= new Semaphore(0),
			  	down 	= new Semaphore(0);
	
	int 		nup 	= 0, // Children going up
				ndown 	= 0, // Children going down
				dup 	= 0, // Children delayed up
				ddown 	= 0; // Children delayed down
	
	
	public void enter(int no) throws InterruptedException {
		if (no < 5)
		{
			mutex.P();
			if (nup > 0) { ddown++; mutex.V(); down.P(); }
			ndown++;
			if (ddown > 0) { ddown--; down.V();	}
			else mutex.V();
		}
		else 
		{
			mutex.P();
			if (ndown > 0) { dup++; mutex.V(); up.P(); }
			nup++;
			if (dup > 0) { dup--; up.V(); }
			else mutex.V();
		}
	}
	
	public void leave(int no) throws InterruptedException {
		if (no < 5)
		{
			mutex.P();
			ndown--;			
			if (ndown == 0 && dup > 0) { dup--; up.V();	}
			else mutex.V();
		}
		else
		{
			mutex.P();
			nup--;			
			if (nup == 0 && ddown > 0) { ddown--; down.V();	}
			else mutex.V();
		}		
	}
}


class AlleyMonitor extends Alley {
		
	int 		nup 	= 0, // Children going up
				ndown 	= 0, // Children going down
				dup 	= 0, // Children delayed up
				ddown 	= 0; // Children delayed down
	
	
	public synchronized void enter(int no) throws InterruptedException {
		if (no < 5)
		{			
			if (nup > 0) { ddown++;  while(nup>0) {wait();} }
			ndown++;
			if (ddown > 0) { ddown--; notify();	}
			
		}
		else 
		{			
			if (ndown > 0) { dup++;  while(ndown>0) {wait();} }
			nup++;
			if (dup > 0) { dup--; notify(); }
			
		}
	}
	
	public synchronized void leave(int no) throws InterruptedException {
		if (no < 5)
		{			
			ndown--;			
			if (ndown == 0 && dup > 0) { dup--; notify();}			
		}
		else
		{			
			nup--;			
			if (nup == 0 && ddown > 0) { ddown--; notify();	}			
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
                	
                newpos = nextPos(curpos);
                
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
        alley = new AlleyMonitor(); 
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
        cd.println("Barrier threshold setting not implemented in this version");
         // This sleep is for illustrating how blocking affects the GUI
        // Remove when feature is properly implemented.
        try { Thread.sleep(3000); } catch (InterruptedException e) { }
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






