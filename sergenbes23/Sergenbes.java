package sergenbes23;

import robocode.*;
import java.awt.Color;

/**
 * Sergenbes - SpinBot by default; switches to Tracker when few enemies remain.
 */
public class Sergenbes extends AdvancedRobot {

    /**
	 * Main run method - Sets colors and initiates a circular movement pattern
	 */
	public void run() {
		// Set colors
		setBodyColor(Color.blue);
		setGunColor(Color.blue);
		setRadarColor(Color.black);
		setScanColor(Color.yellow);

		// Infinite loop for continuous operation
		while (true) {
			// Set extreme right turn to create circular movement
			setTurnRight(10000);
			// Limit speed to 5 pixels per turn
			setMaxVelocity(5);
			// Move forward while turning to create the circular pattern
			ahead(10000);
			// Loop repeats automatically
		}
	}

	/**
	 * Fires at maximum power when the radar detects an enemy robot
	 */
	public void onScannedRobot(ScannedRobotEvent e) {
		fire(3);
	}

	/**
	 * Handles robot collision events by firing at close enemies and adjusting a direction
	 * if this robot initiated the collision, helping maintain the spinning movement pattern.
	 */
	public void onHitRobot(HitRobotEvent e) {
		if (e.getBearing() > -10 && e.getBearing() < 10) {
			fire(3);
		}
		if (e.isMyFault()) {
			back(50);
		}
	}
}
