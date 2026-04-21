package sergenbes23;

import robocode.*;
import java.awt.Color;

/**
 * Sergenbes - Kamikaze (RamFire) by default; switches to SpinBot when HP drops to 50%.
 */
public class Sergenbes extends AdvancedRobot {

	private static final double SPINBOT_HP_THRESHOLD = 50.0; // switch when energy drops below this

	private int turnDirection = 1;

	public void run() {
		setBodyColor(Color.blue);
		setGunColor(Color.blue);
		setRadarColor(Color.black);
		setScanColor(Color.yellow);

		while (true) {
			if (isSpinBotMode()) {
				runSpinBot();
			} else {
				runKamikaze();
			}
		}
	}

	// ── SpinBot behaviour ────────────────────────────────────────────────────
	private void runSpinBot() {
		setTurnRight(10000);
		setMaxVelocity(5);
		ahead(10000);
	}

	// ── Kamikaze behaviour ───────────────────────────────────────────────────
	private void runKamikaze() {
		turnRight(5 * turnDirection);
	}

	// ── Helpers ──────────────────────────────────────────────────────────────
	private boolean isSpinBotMode() {
		return getEnergy() <= SPINBOT_HP_THRESHOLD;
	}

	// ── Events ───────────────────────────────────────────────────────────────
	public void onScannedRobot(ScannedRobotEvent e) {
		if (isSpinBotMode()) {
			fire(3);
			return;
		}

		// Kamikaze: charge at the target
		if (e.getBearing() >= 0) {
			turnDirection = 1;
		} else {
			turnDirection = -1;
		}
		turnRight(e.getBearing());
		ahead(e.getDistance() + 5);
		scan();
	}

	public void onHitRobot(HitRobotEvent e) {
		if (isSpinBotMode()) {
			// SpinBot: fire if dead ahead, back off if our fault
			if (e.getBearing() > -10 && e.getBearing() < 10) {
				fire(3);
			}
			if (e.isMyFault()) {
				back(50);
			}
			return;
		}

		// Kamikaze: face the target, fire without killing it, then ram again
		if (e.getBearing() >= 0) {
			turnDirection = 1;
		} else {
			turnDirection = -1;
		}
		turnRight(e.getBearing());

		if (e.getEnergy() > 16) {
			fire(3);
		} else if (e.getEnergy() > 10) {
			fire(2);
		} else if (e.getEnergy() > 4) {
			fire(1);
		} else if (e.getEnergy() > 2) {
			fire(.5);
		} else if (e.getEnergy() > .4) {
			fire(.1);
		}
		ahead(40);
	}
}
