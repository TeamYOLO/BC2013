package team235;

import battlecode.common.*;

public class HQ {
	
	private static int rallyRadius = 25;

	private static int nukeChannel = 2894;
	private static int opponentNukeHalfDone = 56893349;

	private static int campChannel = 45127;
	private static int gen = 6;
	private static int genInProduction = 83741234;
	private static int sup = 0;

	private static int attackChannel = 8888;
	private static int ALLIN = 13371337;
	
	private static int rallyXChannel = 629;
	private static int rallyYChannel = 58239;

	private static RobotController rc;
	private static double minPowerThreshold = 100; //TODO-findthisvalue
	private static double minRoundThreshold = 100; //TODO-findthisvalue
	private static int gencount = 0;
	private static int soldiercount = 0;
	private static int othercount = 0;

	public static void hqCode(RobotController myRC) throws GameActionException
	{
		rc = myRC;
		Direction defaultSpawnDir = rc.getLocation().directionTo(rc.senseEnemyHQLocation());

		while(true) 
		{
			rc.broadcast(attackChannel, 0);
			
			MapLocation rally = findRallyPoint();
			rc.broadcast(rallyXChannel, rally.x);
			rc.broadcast(rallyYChannel, rally.y);
			
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
					if(!rc.hasUpgrade(Upgrade.FUSION))
					{
						rc.researchUpgrade(Upgrade.FUSION);
					}
					else if(!rc.hasUpgrade(Upgrade.DEFUSION))
					{
						rc.researchUpgrade(Upgrade.DEFUSION);
					}
					else
					{
						rc.researchUpgrade(Upgrade.NUKE);
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
			
			shallWeAllIn();

			rc.setIndicatorString(0, displayString);
			rc.yield();
		}
	}
	
	private static void shallWeAllIn() throws GameActionException
	{
		int massedRobos = 0;
		
		Robot[] robos = rc.senseNearbyGameObjects(Robot.class, findRallyPoint(), rallyRadius, rc.getTeam());
		for(Robot r : robos)
		{
			if(rc.senseRobotInfo(r).type == RobotType.SOLDIER)
			{
				++massedRobos;
			}
		}
		
		if(massedRobos > 15) // if we should all in...
		{
			rc.broadcast(attackChannel, ALLIN);
		}
	}
	
	private static MapLocation findRallyPoint()
	{
		MapLocation enemyLoc = rc.senseEnemyHQLocation();
		MapLocation ourLoc = rc.senseHQLocation();
		int x = (enemyLoc.x + 2 * ourLoc.x) / 3;
		int y = (enemyLoc.y + 2 * ourLoc.y) / 3;
		return new MapLocation(x,y);
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
		if(rc.hasUpgrade(Upgrade.FUSION))
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
