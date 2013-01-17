package team235;

import battlecode.common.*;

public class HQ {

	private static int nukeChannel = 2894;
	private static int opponentNukeHalfDone = 56893349;

	private static int campChannel = 45127;
	private static int gen = 6;
	private static int genInProduction = 83741234;
	private static int sup = 0;

	private static RobotController rc;
	private static double Eprime [] = new double[10];
	private static double Eprev = 40;
	private static double minPowerThreshold = 100; //TODO-findthisvalue
	private static double minRoundThreshold = 100; //TODO-findthisvalue
	private static int gencount = 0;
	private static int soldiercount = 0;
	private static int othercount = 0;

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
			String displayString = "";
			
			if (rc.isActive()) 
			{
				int readIn = rc.readBroadcast(campChannel);
				
				if(!doWeNeedGenerator())
				{
					rc.broadcast(campChannel, sup);
					displayString += "sup";
					
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
				}
				else // we do need a generator
				{
					if(readIn == sup || (readIn != genInProduction && readIn != gen))
					{
						rc.broadcast(campChannel, gen);
						displayString += " gen";
					}
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
					else if(!rc.hasUpgrade(Upgrade.DEFUSION))
					{
						rc.researchUpgrade(Upgrade.DEFUSION);
					}
					else
					{
						// ??? what to do if we shouldn't be producing troops and already have our vital upgrades?
					}
				}
			}

			if(Clock.getRoundNum() > 200)
			{
				if(rc.senseEnemyNukeHalfDone())
				{
					rc.broadcast(nukeChannel, opponentNukeHalfDone);
				}
			}

			rc.setIndicatorString(0, displayString);
			rc.yield();
		}
	}
	public static boolean doWeNeedGenerator() throws GameActionException
	{
		if(rc.readBroadcast(campChannel) == genInProduction) return false;
		
		if(rc.getTeamPower() < minPowerThreshold && Clock.getRoundNum() > minRoundThreshold)
		{
			return true;
		}

		gencount = 0;
		soldiercount = 0;
		othercount = 0;

		Robot[] robos = rc.senseNearbyGameObjects(Robot.class, new MapLocation(0,0), 1000000, rc.getTeam());
		for(Robot r : robos)
		{
			RobotType rt = rc.senseRobotInfo(r).type;
			if(rt == RobotType.GENERATOR) ++gencount;
			else if(rt == RobotType.SOLDIER) ++soldiercount;
			else ++othercount;
		}

		double decay = .8;
		if(researchedFusion)
		{
			decay = .99;
		}

		if((40 + (10 * gencount) - (1.6 * soldiercount) - (1 * othercount)) * decay < 1)
		{
			return true;
		}
		return false;
	}
}
