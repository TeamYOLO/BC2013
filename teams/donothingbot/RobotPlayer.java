package donothingbot;

import battlecode.common.*;

public class RobotPlayer
{
	private static RobotController rc;
	
	public static void run(RobotController myRC)
	{
		while(true) {
			myRC.yield();
		}
	}
}