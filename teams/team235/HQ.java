package team235;

import battlecode.common.*;

public class HQ {

	private static int nukeChannel = 2894;
	private static int opponentNukeHalfDone = 56893349;
	
	private static int campChannel = 45123;
	private static int gen = 6;
	private static int sup = 0;

	private static RobotController rc;
	private static double Eprime [] = new double[10];
	private static double Eprev = 40;
	private static double minPowerThreshold = 100; //TODO-findthisvalue
	private static double minRoundThreshold = 100; //TODO-findthisvalue
	private static int gencount = 0;
	
	public static void hqCode(RobotController myRC) throws GameActionException
	{
		rc = myRC;
		Direction defaultSpawnDir = rc.getLocation().directionTo(rc.senseEnemyHQLocation());
		while(true) 
		{
			if (rc.isActive()) 
			{
				// Spawn a soldier
				if (rc.canMove(defaultSpawnDir))
				{
					rc.spawn(defaultSpawnDir);
				}
				else
				{
					for(Direction d : Direction.values()) // TODO: optimize secondary direction finding
					{
						if(rc.canMove(d))
						{
							rc.spawn(d);
							break;
						}
					}
				}
				if(doWeNeedGenerator())
				{
					gencount++;
					rc.broadcast(campChannel, gen);
				}
				//Eprime[index] = rc.getTeamPower() - Eprev;
				//Eprev = rc.getTeamPower();
			}
			
			if(Clock.getRoundNum() > 200)
			{
				if(rc.senseEnemyNukeHalfDone())
				{
					rc.broadcast(nukeChannel, opponentNukeHalfDone);
				}
			}
			
			rc.yield();
		}
	}
	public static boolean doWeNeedGenerator()
	{
		//double sum = 0;
		int teambots = rc.senseNearbyGameObjects(Robot.class, 100000, rc.getTeam()).length;
		if(rc.getTeamPower() < minPowerThreshold && Clock.getRoundNum() > minRoundThreshold)
		{
			return true;
		}
		if(3 * teambots > 40 + 10 * gencount)
		{
			return true;
		}
		//for(int i = 0; i < Eprime.length; ++i) {
		//	sum = sum + Eprime[i];
		//}

		return false;
	}
}
