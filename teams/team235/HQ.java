package team235;

import battlecode.common.*;

public class HQ {

	private static RobotController rc;


	public static void hqCode(RobotController myRC) throws GameActionException{
		rc=myRC;
		while(true) {
			if (rc.isActive()) {
				// Spawn a soldier
				Direction dir = rc.getLocation().directionTo(rc.senseEnemyHQLocation());
				if (rc.canMove(dir))
					rc.spawn(dir);
			}
			rc.yield();
		}
	}
}
