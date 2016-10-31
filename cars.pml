#define CARS	4

#define P(X)	atomic { X > 0 -> X--; }
#define V(X)	atomic { X++; }

pid up1;
pid up2;
pid down1;
pid down2;

int mutex	= 1;
int up 		= 0;
int down 	= 0;

int nup 	= 0;
int ndown 	= 0;
int dup 	= 0;
int ddown 	= 0;

mtype = { UP, DOWN }
mtype cars[CARS];

init {
	atomic {
		up1 = run Car(UP);
		up2 = run Car(UP);
		down1 = run Car(DOWN);
		down2 = run Car(DOWN)
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
end:	!(mutex <=1 && (nup == 0 || ndown == 0)) -> assert(false)
}
