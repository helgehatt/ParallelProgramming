pid up1, up2, down1, down2;
int up=0,down=0,waiting = 0;

init {
	atomic {
		up1 = run Car(UP);
		up2 = run Car(UP);
		down1 = run Car(DOWN);
		down2 = run Car(DOWN)
	}
}

active proctype Car(d) {
	do 
	:: 