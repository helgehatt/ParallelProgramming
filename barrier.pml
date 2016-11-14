
#define sem		byte

#define CARS 	4

#define P(X)	atomic { X > 0 -> X--; }
#define V(X)	atomic { X++; }

sem mutex 	= 1;
sem enter	= 0;
sem leave	= 0;

byte nenter	= 0;
byte nleave = 0;

byte syncCount[CARS];
byte barrierCount = 0;

active [CARS] proctype Car() {
	do
	:: 
		P(mutex);
		syncCount[_pid]++;
		
enter:
		if 
		:: nenter == CARS-1 ->
			do
			:: nenter > 0 	-> nenter--; V(enter)
			:: else 	 	-> break
			od			
		:: else	-> nenter++; V(mutex); P(enter); P(mutex)
		fi;
	
leave:
		if 
		:: nleave == CARS-1 ->
			do
			:: nleave > 0 	-> nleave--; V(leave)
			:: else 	 	-> barrierCount++; break
			od;
			V(mutex)
		:: else	-> nleave++; V(mutex); P(leave)
		fi;
		
exit:
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