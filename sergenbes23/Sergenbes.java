package sergenbes23;

import robocode.*;
import java.awt.Color;

/**
 * Sergenbes - SpinBot by default; switches to Walls behaviour when player count drops to 50%.
 */
public class Sergenbes extends AdvancedRobot {

	private static final double WALLS_THRESHOLD = 0.50;

	private int     initialOthers    = -1;
	private boolean wallsInitialized = false;
	private boolean peek             = false;
	private double  moveAmount;

	public void run() {
		setBodyColor(Color.blue);
		setGunColor(Color.blue);
		setRadarColor(Color.black);
		setScanColor(Color.yellow);

		initialOthers = getOthers();
		moveAmount    = Math.max(getBattleFieldWidth(), getBattleFieldHeight());

		while (true) {
			if (isWallsMode()) {
				runWalls();
			} else {
				runSpinBot();
			}
		}
	}

	// ── SpinBot behaviour ────────────────────────────────────────────────────
	private void runSpinBot() {
		setTurnRight(10000);
		setMaxVelocity(5);
		ahead(10000);
	}

	// ── Walls behaviour ──────────────────────────────────────────────────────
	private void runWalls() {
		if (!wallsInitialized) {
			turnLeft(getHeading() % 90);
			peek = false;
			ahead(moveAmount);
			peek = true;
			turnGunRight(90);
			turnRight(90);
			wallsInitialized = true;
		}

		peek = true;
		ahead(moveAmount);
		peek = false;
		turnRight(90);
	}

	// ── Helpers ──────────────────────────────────────────────────────────────
	private boolean isWallsMode() {
		if (initialOthers <= 0) return false;
		return (double) getOthers() / initialOthers <= WALLS_THRESHOLD;
	}

	// ── Events ───────────────────────────────────────────────────────────────
	public void onScannedRobot(ScannedRobotEvent e) {
		if (!isWallsMode()) {
			fire(3);
			return;
		}

		// Walls: fire at detected robot
		fire(2);
		if (peek) {
			scan();
		}
	}

	public void onHitRobot(HitRobotEvent e) {
		if (!isWallsMode()) {
			// SpinBot: fire if dead ahead, back off if our fault
			if (e.getBearing() > -10 && e.getBearing() < 10) {
				fire(3);
			}
			if (e.isMyFault()) {
				back(50);
			}
		} else {
			// Walls: move away from the robot we hit
			if (e.getBearing() > -90 && e.getBearing() < 90) {
				back(100);
			} else {
				ahead(100);
			}
		}
	}

	public void onHitWall(HitWallEvent e) {
		// Let the walls loop handle re-alignment naturally
		back(20);
	}
}
