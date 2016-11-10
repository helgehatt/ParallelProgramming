
#define sem		int

#define CARS 	4

#define P(X)	atomic { X > 0 -> X--; }
#define V(X)	atomic { X++; }

sem mutex 	= 1;
sem wait	= 0;

int count	= 0;
bool isOn	= 1;

int syncCount[CARS];

inline Check_Inv() {
	int i;
	for (i in syncCount) {
		assert(syncCount[i] == syncCount[_pid])
	}
}

inline SYNC() {
	P(mutex);
	syncCount[_pid]++;
	if
	:: isOn == 1 ->
		if 
		:: count == CARS-1 ->
			Check_Inv();
			do
			:: count > 0 	-> count--; V(wait)
			:: else 	 	-> break
			od
			V(mutex)			
		:: else	-> count++; V(mutex); P(wait)
		fi
	:: else	-> V(mutex)
	fi
}

active [CARS] proctype Car() {
	do
	::
		SYNC()
	od
}