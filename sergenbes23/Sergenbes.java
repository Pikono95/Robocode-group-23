package sergenbes23;

import robocode.*;
import robocode.util.Utils;
import java.awt.Color;

/**
 * Sergenbes - SpinBot by default; switches to Tracker when few enemies remain.
 */
public class Sergenbes extends AdvancedRobot {

    private static final double HUNTER_THRESHOLD = 0.25; // switch at 25% players left

    private int    initialOthers = -1;
    private String targetName    = null;
    private int    searchDir     = 1;
    private int    searchCount   = 0;

    public void run() {
        setColors(Color.BLUE, Color.BLUE, Color.BLACK, Color.YELLOW, Color.YELLOW);

        initialOthers = getOthers();

        while (true) {
            if (isHunterMode()) {
                runTracker();
            } else {
                runSpinBot();
            }
        }
    }

    // ── SpinBot behaviour ────────────────────────────────────────────────────
    private void runSpinBot() {
        setAdjustGunForRobotTurn(false); // gun rotates with the body
        setTurnRight(10000);
        setMaxVelocity(5);
        ahead(10000);
    }

    // ── Tracker behaviour ────────────────────────────────────────────────────
    private void runTracker() {
        setAdjustGunForRobotTurn(true); // gun moves independently
        if (targetName == null) {
            // Sweep gun to find a target
            turnGunRight(searchDir * 10);
            searchCount++;
            if (searchCount > 5)  { searchDir = -searchDir; searchCount = 0; }
            return;
        }
        // Keep scanning for the target
        turnGunRight(searchDir * 10);
        searchCount++;
        if (searchCount > 11) { targetName = null; }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────
    private boolean isHunterMode() {
        if (initialOthers <= 0) return false;
        return (double) getOthers() / initialOthers <= HUNTER_THRESHOLD;
    }

    // ── Events ───────────────────────────────────────────────────────────────
    @Override
    public void onScannedRobot(ScannedRobotEvent e) {
        if (!isHunterMode()) {
            // SpinBot: just fire
            fire(3);
            return;
        }

        // Tracker: lock onto a target
        if (targetName == null) {
            targetName = e.getName();
        }
        if (!e.getName().equals(targetName)) {
            return; // ignore others while tracking
        }

        searchCount = 0; // reset search since we found our target

        double gunTurn = Utils.normalRelativeAngleDegrees(
            e.getBearing() + (getHeading() - getRadarHeading()));
        turnGunRight(gunTurn);

        if (e.getDistance() > 150) {
            // Move toward target
            turnRight(e.getBearing());
            ahead(e.getDistance() - 140);
        } else {
            // Close enough – fire
            fire(3);
            if (e.getBearing() > -90 && e.getBearing() <= 90) {
                back(40);
            } else {
                ahead(40);
            }
        }
        scan();
    }

    @Override
    public void onHitRobot(HitRobotEvent e) {
        if (!isHunterMode()) {
            // SpinBot: fire and nudge
            if (e.getBearing() > -10 && e.getBearing() < 10) {
                fire(3);
            }
            if (e.isMyFault()) {
                turnRight(10);
            }
        } else {
            // Tracker: target whoever we ran into
            targetName  = e.getName();
            searchCount = 0;
            double gunTurn = Utils.normalRelativeAngleDegrees(
                e.getBearing() + (getHeading() - getRadarHeading()));
            turnGunRight(gunTurn);
            fire(3);
            back(50);
        }
    }

    @Override
    public void onRobotDeath(RobotDeathEvent e) {
        if (e.getName().equals(targetName)) {
            targetName  = null;
            searchCount = 0;
        }
    }

    @Override
    public void onHitWall(HitWallEvent e) {
        back(20);
        turnRight(45);
    }

    @Override
    public void onWin(WinEvent e) {
        for (int i = 0; i < 30; i++) {
            turnRight(30);
            turnLeft(30);
        }
    }
}
