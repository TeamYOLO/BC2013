package team235;

import battlecode.common.*;

public class Util {
	public static MapLocation findClosestRobot(RobotController rc, Robot[] enemyRobots) throws GameActionException
	{
		int closestDist = 1000000;
		MapLocation closestEnemy = null;
		for (int i = 0; i < enemyRobots.length; i++)
		{
			Robot arobot = enemyRobots[i];
			RobotInfo arobotInfo = rc.senseRobotInfo(arobot);
			int dist = arobotInfo.location.distanceSquaredTo(rc.getLocation());
			if (dist<closestDist){
				closestDist = dist;
				closestEnemy = arobotInfo.location;
			}
		}
		return closestEnemy;
	}
	
}
