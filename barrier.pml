#define CARS 	4

#define P(X)	atomic { X > 0 -> X--; }
#define V(X)	atomic { X++; }

int mutex 	= 1;
int wait	= 0;

int count	= 0;
bool isOn	= 1;

int syncCount[CARS]

inline abs(X) {
	if :: X < 0 -> X = -X else skip fi
}

inline SYNC() {
	P(mutex);
	syncCount[_pid]++;
	if
	:: isOn == 1	->
		count++;

		if 
		:: count == CARS->
			do
			:: count > 0 	-> count--; V(wait)
			:: else 	 	-> break
			od
		:: else			-> 
			skip
		fi

		V(mutex);
		P(wait)
	:: else			-> V(mutex)
	fi;
}

active [CARS] proctype Car() {
	do
	::
		SYNC()
	od;
}

active proctype Check_Inv() {
	do
	::
end:	for (i : 1 .. CARS) {
			assert(abs(syncCount[i] - syncCount[i-1]) <= 5);
		}
	od;
}