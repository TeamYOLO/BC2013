package team235;

import battlecode.common.*;

public class HQ {

	private static RobotController rc;
	private static MapLocation rallyPoint;
	private static int[][] neighborArray;
	private static int[] self = {2,2};
	private static int[][] surroundingIndices = new int[5][5];


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
