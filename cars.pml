pid up1;
pid up2;
pid down1;
pid down2;

int up = 0;
int down = 0;
int waiting = 0;

carType = {UP, DOWN}

init {
	atomic {
		up1 = run Car(UP);
		up2 = run Car(UP);
		down1 = run Car(DOWN);
		down2 = run Car(DOWN)
	}
}

proctype Car(type) {
	if 
		:: waiting == 0 -> doSomething();
		:: waiting == 5 -> doSomethingElse();
	fi
}