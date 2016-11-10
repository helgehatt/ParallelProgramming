//Prototype implementation of Car Test class
//Mandatory assignment
//Course 02158 Concurrent Programming, DTU, Fall 2016

//Hans Henrik Lovengreen    Oct 3, 2016

public class CarTest extends Thread {

    CarTestingI cars;
    int testno;

    public CarTest(CarTestingI ct, int no) {
        cars = ct;
        testno = no;
    }

    public void run() {
        try {
            switch (testno) { 
            case 0:
                // Demonstration of startAll/stopAll.
                // Should let the cars go one round (unless very fast)
                cars.startAll();
                sleep(3000);
                cars.stopAll();
                break;
            case 1:
            	cars.println("Going to mach-speed!");
            	for (int i = 1; i < 9; i++) {
                    cars.setSpeed(i,10);
                    cars.setVariation(i, 0);
                };
                cars.startAll();
            	break;
            case 2:
            	cars.println("High variation");
            	for (int i = 1; i < 9; i++) {
                    cars.setVariation(i, 20);
                };
                cars.startAll();
            	break;
            case 3:
            	cars.println("Barrier on-off");
            	cars.barrierOn();
            	cars.startAll();
            	for (int i = 0; i < 100; i++){
            		cars.barrierOff();
            		cars.barrierOn();
            		Thread.sleep(100);
            	}
            	cars.stopAll();
            	break;
            case 4:
            	cars.println("Barrier threshold");
            	cars.barrierOn();
            	cars.startAll();
            	cars.startBarrierSet(4);
            	cars.startBarrierSet(9);
            	break;
            case 5:
            	cars.println("Remove and restore car");
            	for (int i = 0; i < 10; i++){
            		cars.removeCar(1);
                	cars.restoreCar(1);
            	}
            	break;
            case 19:
                // Demonstration of speed setting.
                // Change speed to double of default values
                cars.println("Doubling speeds");
                for (int i = 1; i < 9; i++) {
                    cars.setSpeed(i,50);
                };
                break;

            default:
                cars.println("Test " + testno + " not available");
            }

            cars.println("Test ended");

        } catch (Exception e) {
            System.err.println("Exception in test: "+e);
        }
    }

}



