package donothingbot;

import battlecode.common.*;
import battlecode.engine.instrumenter.lang.System;

public class RobotPlayer
{
	private static RobotController rc;
	
	public static void run(RobotController myRC) throws GameActionException
	{
		while(true) {
			myRC.yield();
		}
	}
}