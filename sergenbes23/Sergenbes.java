package sergenbes23;

import robocode.*;
import robocode.util.Utils;
import java.awt.Color;

/**
 * Sergenbes - A strategic melee robot with three combat phases:
 *
 *   Phase 1 (EVADE)  - Full field: dodge combat, preserve health.
 *   Phase 2 (WALLS)  - <= 50% players left: hug walls, fire predictive shots only.
 *   Phase 3 (HUNTER) - <= 25% players left: chase target, fire rapidly at close range.
 */
public class Sergenbes extends Robot {

    // ── Phase thresholds ────────────────────────────────────────────────────
    private static final double WALLS_THRESHOLD  = 0.50; // switch to WALLS  at 50%
    private static final double HUNTER_THRESHOLD = 0.25; // switch to HUNTER at 25%

    // ── Tuning constants ────────────────────────────────────────────────────
    private static final double BULLET_SPEED        = 20 - 3 * 2; // power 2 bullet
    private static final double HUNT_FIRE_POWER     = 3.0;
    private static final double WALLS_FIRE_POWER    = 2.0;
    private static final double HUNT_CLOSE_DISTANCE = 150;
    private static final double WALL_MARGIN         = 60;  // how far from wall edge to hug

    // ── State ────────────────────────────────────────────────────────────────
    private int    initialOthers   = -1;
    private String targetName      = null;
    private double targetBearing   = 0;
    private double targetDistance  = Double.MAX_VALUE;
    private double targetHeading   = 0;
    private double targetVelocity  = 0;
    private int    searchDir       = 1;   // gun sweep direction
    private int    searchCount     = 0;

    // Walls mode
    private double moveAmount;
    private boolean wallsPeek = false;

    private enum Mode { EVADE, WALLS, HUNTER }
    private Mode mode = Mode.EVADE;

    // ────────────────────────────────────────────────────────────────────────
    public void run() {
        setColors(Color.DARK_GRAY, Color.RED, Color.ORANGE, Color.YELLOW, Color.RED);
        setAdjustGunForRobotTurn(true);
        setAdjustRadarForGunTurn(true);

        initialOthers = getOthers();
        moveAmount    = Math.max(getBattleFieldWidth(), getBattleFieldHeight());

        while (true) {
            updateMode();

            switch (mode) {
                case EVADE:  runEvade();  break;
                case WALLS:  runWalls();  break;
                case HUNTER: runHunter(); break;
            }
        }
    }

    // ── Mode selection ───────────────────────────────────────────────────────
    private void updateMode() {
        if (initialOthers <= 0) return;
        double ratio = (double) getOthers() / initialOthers;
        if (ratio <= HUNTER_THRESHOLD) {
            mode = Mode.HUNTER;
        } else if (ratio <= WALLS_THRESHOLD) {
            mode = Mode.WALLS;
        } else {
            mode = Mode.EVADE;
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // PHASE 1 – EVADE
    // Move in an erratic zigzag pattern; rotate radar; avoid bullets.
    // ════════════════════════════════════════════════════════════════════════
    private void runEvade() {
        // Sweep radar constantly
        turnGunRight(searchDir * 15);
        searchCount++;
        if (searchCount > 5) {
            searchDir = -searchDir;
            searchCount = 0;
        }

        // Erratic movement: small random-feeling steps + direction changes
        ahead(80);
        turnRight(35);
        ahead(80);
        turnLeft(35);
    }

    // ════════════════════════════════════════════════════════════════════════
    // PHASE 2 – WALLS
    // Hug the battlefield perimeter; fire only when a predictive shot will hit.
    // ════════════════════════════════════════════════════════════════════════
    private boolean wallsInitialized = false;

    private void runWalls() {
        if (!wallsInitialized) {
            // Align to nearest wall
            turnLeft(getHeading() % 90);
            wallsPeek = false;
            ahead(moveAmount);
            wallsPeek = true;
            turnGunRight(90);
            turnRight(90);
            wallsInitialized = true;
        }

        wallsPeek = true;
        ahead(moveAmount);
        wallsPeek = false;
        turnRight(90);
    }

    // ════════════════════════════════════════════════════════════════════════
    // PHASE 3 – HUNTER
    // Chase the tracked target; unload at close range.
    // ════════════════════════════════════════════════════════════════════════
    private void runHunter() {
        if (targetName == null) {
            // No target yet – sweep the gun to find one
            turnGunRight(searchDir * 10);
            searchCount++;
            if (searchCount > 6) { searchDir = -searchDir; searchCount = 0; }
            return;
        }

        // Aim gun toward last-known target bearing
        double gunTurn = Utils.normalRelativeAngleDegrees(
            targetBearing + (getHeading() - getRadarHeading()));
        turnGunRight(gunTurn);

        if (targetDistance > HUNT_CLOSE_DISTANCE) {
            // Move toward target
            turnRight(targetBearing);
            ahead(targetDistance - HUNT_CLOSE_DISTANCE + 20);
        } else {
            // Close enough – fire rapidly
            for (int i = 0; i < 3; i++) {
                fire(HUNT_FIRE_POWER);
            }
            // Keep circling the target so we're harder to hit
            turnRight(targetBearing + 90);
            ahead(60);
        }
        scan();
    }

    // ════════════════════════════════════════════════════════════════════════
    // EVENT HANDLERS
    // ════════════════════════════════════════════════════════════════════════

    @Override
    public void onScannedRobot(ScannedRobotEvent e) {
        updateMode();

        switch (mode) {
            case EVADE:  handleScanEvade(e);  break;
            case WALLS:  handleScanWalls(e);  break;
            case HUNTER: handleScanHunter(e); break;
        }
    }

    private void handleScanEvade(ScannedRobotEvent e) {
        // In evasion we do NOT fire – just try to keep away.
        // Turn away from nearby robots.
        if (e.getDistance() < 200) {
            // Turn away and move
            if (e.getBearing() >= 0) {
                turnLeft(30);
            } else {
                turnRight(30);
            }
        }
    }

    private void handleScanWalls(ScannedRobotEvent e) {
        // Fire only if a linear-prediction shot would hit.
        if (willHit(e)) {
            fire(WALLS_FIRE_POWER);
        }
        if (wallsPeek) {
            scan();
        }
    }

    private void handleScanHunter(ScannedRobotEvent e) {
        // Prefer the closest robot as target
        if (targetName == null || e.getDistance() < targetDistance || e.getName().equals(targetName)) {
            targetName     = e.getName();
            targetBearing  = e.getBearing();
            targetDistance = e.getDistance();
            targetHeading  = e.getHeading();
            targetVelocity = e.getVelocity();
        }

        // If we've found our target, fire immediately with prediction
        if (e.getName().equals(targetName)) {
            double gunTurn = Utils.normalRelativeAngleDegrees(
                e.getBearing() + (getHeading() - getRadarHeading()));
            turnGunRight(gunTurn);
            fire(HUNT_FIRE_POWER);
            scan();
        }
    }

    // ── Predictive firing helper ─────────────────────────────────────────────
    /**
     * Returns true if a bullet fired now (power = WALLS_FIRE_POWER) is likely
     * to intersect the scanned robot based on linear movement prediction.
     */
    private boolean willHit(ScannedRobotEvent e) {
        double bulletSpeed = 20 - 3 * WALLS_FIRE_POWER;

        // Absolute bearing to target
        double absBearing = Math.toRadians(getHeading() + e.getBearing());

        // Target's current position (relative to us)
        double targetX = getX() + e.getDistance() * Math.sin(absBearing);
        double targetY = getY() + e.getDistance() * Math.cos(absBearing);

        // Predict where target will be when bullet arrives
        double travelTime = e.getDistance() / bulletSpeed;
        double futureX = targetX + Math.sin(Math.toRadians(e.getHeading())) * e.getVelocity() * travelTime;
        double futureY = targetY + Math.cos(Math.toRadians(e.getHeading())) * e.getVelocity() * travelTime;

        // Angle from us to the predicted position
        double predictedAngle = Math.toDegrees(Math.atan2(futureX - getX(), futureY - getY()));
        double currentGunHeading = getGunHeading();
        double angleDiff = Math.abs(Utils.normalRelativeAngleDegrees(predictedAngle - currentGunHeading));

        // Fire if gun is aimed within 10 degrees of predicted position
        return angleDiff < 10;
    }

    @Override
    public void onHitByBullet(HitByBulletEvent e) {
        // Always dodge when hit, regardless of mode
        double bearing = e.getBearing();
        if (bearing >= 0) {
            turnRight(45);
        } else {
            turnLeft(45);
        }
        ahead(100);
    }

    @Override
    public void onHitWall(HitWallEvent e) {
        // Back off and reorient
        back(30);
        turnRight(90);
    }

    @Override
    public void onHitRobot(HitRobotEvent e) {
        updateMode();
        if (mode == Mode.HUNTER) {
            // Ram and fire
            fire(HUNT_FIRE_POWER);
        } else {
            // Avoid collision
            if (e.getBearing() > -90 && e.getBearing() < 90) {
                back(60);
            } else {
                ahead(60);
            }
        }
    }

    @Override
    public void onRobotDeath(RobotDeathEvent e) {
        // If our tracked target died, reset so we pick a new one
        if (e.getName().equals(targetName)) {
            targetName     = null;
            targetDistance = Double.MAX_VALUE;
        }
        // Re-evaluate mode with updated player count
        updateMode();
        // Reset walls state so phase 2 re-initialises if we switch into it
        if (mode == Mode.WALLS) {
            wallsInitialized = false;
        }
    }

    @Override
    public void onWin(WinEvent e) {
        // Victory spin
        for (int i = 0; i < 30; i++) {
            turnRight(30);
            turnLeft(30);
        }
    }
}
