#define sem 	byte

#define P(X)	atomic { X > 0 -> X--; }
#define V(X)	atomic { X++; }

sem mutex	= 1;
sem up 		= 0;
sem down 	= 0;

byte upCount 	 = 0;
byte downCount 	 = 0;
byte upDelayed	 = 0;
byte downDelayed = 0;

mtype = { UP, DOWN }

pid p1, p2, p3, p4;

init {
	atomic {
		p1 = run Car(UP);
		p2 = run Car(DOWN);
		p3 = run Car(UP);
		p4 = run Car(DOWN);
	}
}

inline ENTER(dir) {
	if 
	:: dir == DOWN ->
		P(mutex);
		if 
		:: upCount > 0 		-> downDelayed++; V(mutex); P(down) 
		:: else				-> skip
		fi;
		downCount++;
		if 
		:: downDelayed > 0 	-> downDelayed--; V(down)
		:: else 			-> V(mutex)
		fi		
	:: else 		->
		P(mutex);
		if 
		:: downCount > 0 	-> upDelayed++; V(mutex); P(up) 
		:: else		 		-> skip
		fi;
		upCount++;
		if 
		:: upDelayed > 0 	-> upDelayed--; V(up)
		:: else 			-> V(mutex)
		fi
	fi;
}

inline LEAVE(dir) {
	if
	:: dir == DOWN ->
		P(mutex);
		downCount--;
		if 
		:: downCount == 0 && upDelayed > 0 	-> upDelayed--; V(up)
		:: else					 			-> V(mutex)
		fi	
	:: else			->
		P(mutex);
		upCount--;
		if 
		:: upCount == 0 && downDelayed > 0 	-> downDelayed--; V(down)
		:: else					 			-> V(mutex)
		fi
	fi;
}

proctype Car(mtype dir) {
	do
	::
		skip;

entering:
		ENTER(dir);	

leaving:	
		LEAVE(dir);
		
exiting:
		if :: skip :: break fi
	od;
}

active proctype Check_Inv() {
end: upCount > 0 && downCount > 0 -> assert(false);
}

//ltl obl1 { [] ( ( Car[p1]@entering && [] !Car[p2]@entering && [] !Car[p3]@entering && [] !Car[p4]@entering ) -> <> Car[p1]@leaving ) }
//ltl obl2 { [] ( ( Car[p1]@leaving  && [] !Car[p2]@leaving  && [] !Car[p3]@leaving  && [] !Car[p4]@leaving  ) -> <> Car[p1]@exiting ) }
//ltl res1 { [] ( ( Car[p1]@entering || Car[p2]@entering || Car[p3]@entering || Car[p4]@entering ) -> <> ( Car[p1]@leaving || Car[p2]@leaving || Car[p3]@leaving || Car[p4]@leaving ) ) }
//ltl res2 { [] ( ( Car[p1]@leaving  || Car[p2]@leaving  || Car[p3]@leaving  || Car[p4]@leaving  ) -> <> ( Car[p1]@exiting || Car[p2]@exiting || Car[p3]@exiting || Car[p4]@exiting ) ) }