#define sem 	byte

#define P(X)	atomic { X > 0 -> X--; }
#define V(X)	atomic { X++; }

sem mutex	= 1;
sem up 		= 0;
sem down 	= 0;

byte nup 	= 0;
byte ndown 	= 0;
byte dup 	= 0;
byte ddown 	= 0;

mtype = { UP, DOWN }

init {
	atomic {
		run Car(UP);
		run Car(UP);
		run Car(UP);
		run Car(UP);
		run Car(DOWN);
		run Car(DOWN);
		run Car(DOWN);
		run Car(DOWN);
	}
}

inline ENTER(type) {
	if 
	:: type == DOWN ->
		P(mutex);
		if 
		:: nup > 0 		-> ddown++; V(mutex); P(down) 
		:: else			-> skip
		fi;
		ndown++;
		if 
		:: ddown > 0 	-> ddown--; V(down)
		:: else 		-> V(mutex)
		fi		
	:: else 		->
		P(mutex);
		if 
		:: ndown > 0 	-> dup++; V(mutex); P(up) 
		:: else		 	-> skip
		fi;
		nup++;
		if 
		:: dup > 0 		-> dup--; V(up)
		:: else 		-> V(mutex)
		fi
	fi;
}

inline LEAVE(type) {
	if
	:: type == DOWN ->
		P(mutex);
		ndown--;
		if 
		:: ndown == 0 && dup > 0 -> dup--; V(up)
		:: else					 -> V(mutex)
		fi	
	:: else			->
		P(mutex);
		nup--;
		if 
		:: nup == 0 && ddown > 0 -> ddown--; V(down)
		:: else					 -> V(mutex)
		fi
	fi;
}

proctype Car(mtype type) {
	do
	::
		ENTER(type);		
		LEAVE(type)
	od;
}

active proctype Check_Inv() {
end: nup > 0 && ndown > 0 -> assert(false);
}
