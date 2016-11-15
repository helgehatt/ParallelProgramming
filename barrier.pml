
#define sem		byte

#define CARS 	4

#define P(X)	atomic { X > 0 -> X--; }
#define V(X)	atomic { X++; }

sem mutex 	= 1;
sem enter	= 0;
sem leave	= 0;

byte enterCount	= 0;
byte leaveCount = 0;

byte syncCount[CARS];
byte barrierCount = 0;

active [CARS] proctype Car() {
	do
	:: 
		P(mutex);
		syncCount[_pid]++;
		
entering:
		if 
		:: enterCount == CARS-1 ->
			do
			:: enterCount > 0 	-> enterCount--; V(enter)
			:: else 	 		-> break
			od			
		:: else	-> enterCount++; V(mutex); P(enter); P(mutex)
		fi;
	
leaving:
		if 
		:: leaveCount == CARS-1 ->
			do
			:: leaveCount > 0 	-> leaveCount--; V(leave)
			:: else 	 		-> barrierCount++; break
			od;
			V(mutex)
		:: else	-> leaveCount++; V(mutex); P(leave)
		fi;
		
exiting:
		if :: skip :: break fi
	od;
}

active proctype Check_Inv() {
	if
	:: syncCount[0] - barrierCount > 1 -> assert(false)
	:: syncCount[1] - barrierCount > 1 -> assert(false)
	:: syncCount[2] - barrierCount > 1 -> assert(false)
	:: syncCount[3] - barrierCount > 1 -> assert(false)
	fi;
}

//ltl release { [] (Car[0]@entering && Car[1]@entering && Car[2]@entering && Car[3]@entering 
//            -> <> Car[0]@leaving  && Car[1]@leaving  && Car[2]@leaving  && Car[3]@leaving) }

ltl block { [] ([] Car[0]@entering -> [] !Car[1]@exiting) }