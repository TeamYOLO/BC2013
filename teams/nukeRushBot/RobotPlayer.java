package nukeRushBot;
import battlecode.common.*;

public class RobotPlayer
{
	private static RobotController rc;

	public static void run(RobotController myRC) throws GameActionException
	{
		while(true) {
			myRC.researchUpgrade(Upgrade.NUKE);
			myRC.yield();
		}
	}
}



