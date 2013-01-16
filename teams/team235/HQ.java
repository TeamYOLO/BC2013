package team235;

import battlecode.common.*;

public class HQ {

	private static int nukeChannel = 2894;
	private static int opponentNukeHalfDone = 56893349;

	private static int campChannel = 45127;
	private static int gen = 6;
	private static int sup = 0;

	private static RobotController rc;
	private static double Eprime [] = new double[10];
	private static double Eprev = 40;
	private static double minPowerThreshold = 100; //TODO-findthisvalue
	private static double minRoundThreshold = 100; //TODO-findthisvalue
	private static int gencount = 0;

	private static int prevRoundsOfEnergyDecline = 0;
	private static boolean researchedFusion = false;

	public static void hqCode(RobotController myRC) throws GameActionException
	{
		rc = myRC;
		Direction defaultSpawnDir = rc.getLocation().directionTo(rc.senseEnemyHQLocation());
		while(true) 
		{
			/*if(!researchedFusion)
			{
				double currentEnergy = rc.getTeamPower();
				if(currentEnergy < Eprev || currentEnergy < 40)
				{
					++prevRoundsOfEnergyDecline;
				}
				else
				{
					prevRoundsOfEnergyDecline = 0;
				}
				Eprev = currentEnergy;

				if(prevRoundsOfEnergyDecline > 7 && currentEnergy < 40)
				{
					// begin researching fusion (the energy decay upgrade)
					while(!rc.hasUpgrade(Upgrade.FUSION))
					{
						if(rc.isActive())
						{
							rc.researchUpgrade(Upgrade.FUSION);
							rc.yield();
						}
					}
					researchedFusion = true;
				}
			}
			 */
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
					if(!researchedFusion)
					{
						while(!rc.hasUpgrade(Upgrade.FUSION))
						{
							if(rc.isActive())
							{
								rc.researchUpgrade(Upgrade.FUSION);
								rc.yield();
							}
						}
						researchedFusion = true;
					}
					else
					{
						gencount++;
						rc.broadcast(campChannel, gen);
					}
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
		if(!researchedFusion)
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
		}
		else
		{
			int teambots = rc.senseNearbyGameObjects(Robot.class, 100000, rc.getTeam()).length;
			
			if(2.5 * teambots > 40 + 10 * gencount && rc.getTeamPower() < 300)
			{
				return true;
			}
		}

		return false;
	}
}
