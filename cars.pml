#define CARS	4

#define P(X)	atomic { (X > 0) -> X--; }
#define V(X)	{ X++ }

pid up1;
pid up2;
pid down1;
pid down2;

int crit	= 1;
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
	P(crit);
	
	if 
		:: type == DOWN	->
		
			do
				:: !(nup > 0) 	-> break
				:: else 		->
					
					ddown++;
					V(crit);
					P(down)
			od;
			
			ndown++;
			
			if
				:: (ddown > 0) -> 
					
					ddown--;
					V(down)

				:: else -> V(crit)
			fi
			 
		:: type == UP	->
		
			do
				:: !(ndown > 0) -> break
				:: else			->
					
					dup++;
					V(crit);
					P(up)
			od;
			
			nup++;
			
			if
				:: (dup > 0) -> 
					
					dup--;
					V(up)

				:: else -> V(crit)
			fi
	fi;
}

inline LEAVE(type) {
	P(crit);
	
	if 
		:: type == DOWN -> 
		
			ndown--;
			
			if
				:: (ndown == 0 && dup > 0) -> 
					
					dup--;
					V(up)
					
				:: else -> V(crit)
			fi
			
		:: type == UP 	-> 
		
			nup--;
			
			if
				:: (nup == 0 && ddown > 0) -> 
					
					ddown--;
					V(down)
					
				:: else -> V(crit)
			fi
	fi;	
}

proctype Car(mtype type) {
		ENTER(type);
		
		LEAVE(type)
}

active proctype Check_Inv() {
	!(crit <=1 && (nup == 0 || ndown == 0)) -> assert(true)
}
