package team235;

import battlecode.common.*;

import java.util.*;

public class HQ
{
	private static int allInRound = -100;
	
	private static boolean rushedWeAreBeing = false;

	private static RobotController rc;
	private static double minPowerThreshold = 100; //TODO-findthisvalue
	private static double minRoundThreshold = 100; //TODO-findthisvalue
	private static int gencount = 0;
	private static int soldiercount = 0;
	private static int othercount = 0;

	private static int optimalBuildings = 0;
	private static int farAwayButSafeBuildings = 0;

	private static int powerThreshold = 500;

	private static boolean expandPhase = true;

	private static Robot[] enemyRobots;

	private static int[] buildOrder;

	private static int rushDistance;

	private static Direction defaultSpawnDir;

	static int numBadCamps = 0;
	private static ArrayList<MapLocation> untakeableCamps = new ArrayList<MapLocation>();

	public static void hqCode(RobotController myRC) throws GameActionException
	{
		rc = myRC;
		defaultSpawnDir = rc.getLocation().directionTo(rc.senseEnemyHQLocation());
		evaluateMap();
		while(true) 
		{
			rc.broadcast(Constants.attackChannel, Constants.attackContinueRallying);
			enemyRobots = rc.senseNearbyGameObjects(Robot.class, new MapLocation(0,0), 1000000, rc.getTeam().opponent());

			if(expandOrRally())
			{
				rc.broadcast(Constants.commandChannel, Constants.commandExpand);
				broadcastUpdatedBuildOrder();

				MapLocation rally = findRallyPoint();
				rc.broadcast(Constants.rallyXChannel, rally.x);
				rc.broadcast(Constants.rallyYChannel, rally.y);
			}
			else
			{
				MapLocation rally = findRallyPoint();
				rc.broadcast(Constants.rallyXChannel, rally.x);
				rc.broadcast(Constants.rallyYChannel, rally.y);
				rc.broadcast(Constants.commandChannel,Constants.commandRally);
			}

			if (rc.isActive()) 
			{
				int readIn = rc.readBroadcast(Constants.campChannel);

				if(!doWeNeedGenerator())
				{
					rc.broadcast(Constants.campChannel, Constants.campSupplier);

					// Spawn a soldier
					Team defaultScan = rc.senseMine(rc.getLocation().add(defaultSpawnDir));
					if (rc.canMove(defaultSpawnDir) && (defaultScan == null || defaultScan == rc.getTeam()))
					{
						rc.spawn(defaultSpawnDir);
					}
					else
					{
						for(Direction d : Direction.values()) // TODO: optimize secondary direction finding
						{
							if(d != Direction.OMNI && d != Direction.NONE)
							{
								Team scan = rc.senseMine(rc.getLocation().add(d));
								if(rc.canMove(d) && (scan == null || scan == rc.getTeam()))
								{
									rc.spawn(d);
									break;
								}
							}
						}
						if(rc.isActive())
						{
							// if there are no valid spawn directions
							rc.researchUpgrade(Upgrade.NUKE);
						}
					}
				}
				else // we do need a generator
				{
					if(readIn == Constants.campSupplier || (readIn != Constants.campGenInProduction && readIn != Constants.campGen))
					{
						rc.broadcast(Constants.campChannel, Constants.campGen);
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
					rc.broadcast(Constants.commandChannel, Constants.commandEnemyNukeHalfDone);
					while(!rc.hasUpgrade(Upgrade.DEFUSION))
					{
						if(rc.isActive())
						{
							rc.researchUpgrade(Upgrade.DEFUSION);

						}
						rc.yield();
					}
					enemyRobots = rc.senseNearbyGameObjects(Robot.class, new MapLocation(0,0), 1000000, rc.getTeam().opponent());
				}
			}
			shallWeAllIn();
			rc.yield();
		}
	}

	private static void broadcastUpdatedBuildOrder() throws GameActionException
	{
		boolean scrambledEggs = false;
		//scramble check
		for(int i = 0; i < buildOrder.length; i++)
		{
			int tmp = rc.readBroadcast(Constants.buildOrderBeginChannel + i);
			if(!(tmp == 0 || tmp == Constants.buildOrderArt || tmp == Constants.buildOrderGen || tmp == Constants.buildOrderSup || tmp == Constants.buildOrderHeal || tmp == Constants.buildOrderShield))
			{
				scrambledEggs = true;
				break;
			}
		}

		if(!scrambledEggs)
		{
			for(int i = 0; i < buildOrder.length; i++)
			{
				buildOrder[i] = rc.readBroadcast(Constants.buildOrderBeginChannel + i);

			}
		}
		else
		{
			for(int i = 0; i < buildOrder.length; i++)
			{
				rc.broadcast(Constants.buildOrderBeginChannel + i, buildOrder[i]);
			}
		}
	}



	private static void shallWeAllIn() throws GameActionException
	{
		int massedRobos = 0;
		double massedAmountNeeded = .5*(40 + (10 * gencount) - (1 * othercount));

		if(rc.senseEnemyNukeHalfDone()) massedAmountNeeded -= 10;
		if(rc.senseEnemyNukeHalfDone() && Clock.getRoundNum() < 300)
		{
			massedAmountNeeded = 5;
		}

		int rallyRadius = 33;
		if(massedAmountNeeded > 50) rallyRadius = 63;

		Robot[] robos = rc.senseNearbyGameObjects(Robot.class, findRallyPoint(), rallyRadius, rc.getTeam());

		for(Robot r : robos)
		{
			if(rc.senseRobotInfo(r).type == RobotType.SOLDIER)
			{
				++massedRobos;
			}
		}

		if(massedRobos > massedAmountNeeded) // if we should all in...
		{
			rc.broadcast(Constants.attackChannel, Constants.attackAllIn);
			allInRound = Clock.getRoundNum();
			if(rc.senseEnemyNukeHalfDone())
			{
				MapLocation enemyHQ = rc.senseEnemyHQLocation();
				while(true)
				{
					rc.broadcast(Constants.commandChannel, Constants.commandEnemyNukeHalfDone);
					rc.broadcast(Constants.rallyXChannel, enemyHQ.x);
					rc.broadcast(Constants.rallyYChannel, enemyHQ.y);
					//rc.broadcast(Constants.attackChannel, Constants.attackAllIn);
					if(rc.isActive())
					{
						// Spawn a soldier
						Team defaultScan = rc.senseMine(rc.getLocation().add(defaultSpawnDir));
						if (rc.canMove(defaultSpawnDir) && (defaultScan == null || defaultScan == rc.getTeam()))
						{
							rc.spawn(defaultSpawnDir);
						}
						else
						{
							for(Direction d : Direction.values()) // TODO: optimize secondary direction finding
							{
								if(d != Direction.OMNI && d != Direction.NONE)
								{
									Team scan = rc.senseMine(rc.getLocation().add(d));
									if(rc.canMove(d) && (scan == null || scan == rc.getTeam()))
									{
										rc.spawn(d);
										break;
									}
								}
							}
							if(rc.isActive())
							{
								// if there are no valid spawn directions
								rc.researchUpgrade(Upgrade.NUKE);
							}
						}
					}
					rc.yield();
				}
			}
		}

	}

	private static MapLocation findRallyPoint() throws GameActionException
	{

		MapLocation closestEnemy = null;
		closestEnemy = Util.findClosestRobot(rc, enemyRobots);


		if(closestEnemy != null && closestEnemy.distanceSquaredTo(rc.getLocation()) < rushDistance*.2) {
			return closestEnemy;
		}

		MapLocation enemyLoc = rc.senseEnemyHQLocation();
		MapLocation ourLoc = rc.senseHQLocation();
		int x = (enemyLoc.x + 2 * ourLoc.x) / 3;
		int y = (enemyLoc.y + 2 * ourLoc.y) / 3;
		return new MapLocation(x,y);
	}

	public static boolean doWeNeedGenerator() throws GameActionException
	{
		if(rc.readBroadcast(Constants.campChannel) == Constants.campGenInProduction) return false;
		if(Clock.getRoundNum() - allInRound < 80) return false;
		if(rc.readBroadcast(Constants.commandChannel) == Constants.commandRally && rc.getTeamPower()>powerThreshold) return false;

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
	public static void evaluateMap() throws GameActionException 
	{
		MapLocation neutralCamps[] = rc.senseEncampmentSquares(rc.getLocation(), 1000000, Team.NEUTRAL);
		MapLocation me = rc.getLocation();
		MapLocation them = rc.senseEnemyHQLocation();
		rushDistance = them.distanceSquaredTo(me);

		if(neutralCamps.length < 80)
		{
			for (MapLocation loc : neutralCamps) {
				double toUs=loc.distanceSquaredTo(me);
				double toThem = loc.distanceSquaredTo(them);			
				if(toUs/toThem < .81)
				{
					if(rushDistance > toUs)
						optimalBuildings++;
					else farAwayButSafeBuildings++;
				}
			}
		}
		else
		{
			optimalBuildings = 20;
			// TODO: figure out what to do with far away but safe
		}

		findUntakeableCamps();
		optimalBuildings-=numBadCamps;

		if(optimalBuildings > 20) optimalBuildings = 20;

		buildOrder = new int[optimalBuildings];

		for(int i = 0; i < optimalBuildings; i++)
		{
			buildOrder[i] = (i+1) % 3 == 0 ? Constants.buildOrderGen : Constants.buildOrderSup;
			rc.broadcast(Constants.buildOrderBeginChannel + i, buildOrder[i]);
		}
	}
	private static void findUntakeableCamps() {
		MapLocation hq = rc.senseHQLocation();
		MapLocation enemy = rc.senseEnemyHQLocation();
		Direction hqToEnemy = hq.directionTo(enemy);

		int camp1 = 0;
		int camp2 = 0; // +1
		int camp3 = 0; // -1

		for(int i = 0; i <= 4; i++) {
			if(rc.senseEncampmentSquare(hq.add(hqToEnemy,i))) camp1++;
			if(rc.senseEncampmentSquare(hq.add(Direction.values()[((hqToEnemy.ordinal() + 1)%8)],i))) camp2++;
			if(rc.senseEncampmentSquare(hq.add(Direction.values()[((hqToEnemy.ordinal()  + 8 + 1)%8)],i))) camp3++;
		}

		if(camp1 <= camp2 && camp1 <= camp3)
		{
			numBadCamps = camp1;
			for(int i = 0; i <= 4; i++) {
				untakeableCamps.add(hq.add(hqToEnemy,i));
			}
		}
		else if(camp2 < camp1 && camp2 <= camp3)
		{
			numBadCamps = camp2;
			for(int i = 0; i < 4; i++) {
				untakeableCamps.add(hq.add(Direction.values()[((hqToEnemy.ordinal() + 1)%8)],i));
			}
		}
		else if(camp3 < camp1 && camp3 < camp2)
		{
			numBadCamps = camp3;
			for(int i = 0; i < 4; i++) {
				untakeableCamps.add(hq.add(Direction.values()[((hqToEnemy.ordinal() + 8 + 1)%8)],i));
			}
		}

	}

	public static void singleExpand() throws GameActionException
	{
		int numCamps = rc.senseAlliedEncampmentSquares().length;
		if(numCamps >= optimalBuildings + farAwayButSafeBuildings)
			return;

		if(numCamps < optimalBuildings)
		{

		}

		if(false) { //figure out condition
			MapLocation expansion = null;
			expansion = findClosestEmptyCamp();
			rc.broadcast(Constants.commandChannel, Constants.commandBuildIndividual);
			rc.broadcast(Constants.singleExpandXChannel, expansion.x);
			rc.broadcast(Constants.singleExpandYChannel, expansion.y);
		}
	}

	public static boolean expandOrRally() throws GameActionException //true for expand
	{
		// rush distance
		//double rushDistance = rc.senseHQLocation().distanceSquaredTo(rc.senseEnemyHQLocation());
		// number of encampments
		if(rushedWeAreBeing)
		{
			return false;
		}
		
		if(Clock.getRoundNum() < 201)
		{
			if(enemyRobots.length * 30 > Clock.getRoundNum())
			{
				rc.setIndicatorString(0, "AW MA GAWD");
				rushedWeAreBeing = true;
				return false;
			}
		}
		
		rc.setIndicatorString(0, "WE ARE OKAY GUYSSS");
		if(!expandPhase)
		{ 
			singleExpand();
			return false;
		}

		int numAlliedCamps = rc.senseAlliedEncampmentSquares().length;
		if(numAlliedCamps > 13)
		{
			rc.broadcast(Constants.commandChannel, Constants.commandRally);
			expandPhase = false;
			return false;
		}

		if(numAlliedCamps>=optimalBuildings)
		{
			rc.broadcast(Constants.commandChannel, Constants.commandRally);
			expandPhase = false;
			return false;
		} 

		rc.broadcast(Constants.commandChannel, Constants.commandExpand);
		return true;
	}

	private static MapLocation findClosestEmptyCamp() throws GameActionException
	{
		MapLocation[] locArray = rc.senseEncampmentSquares(rc.getLocation(), 1000000, Team.NEUTRAL);
		int closestDist = 1000000;
		MapLocation me = rc.getLocation();
		MapLocation closestLocation = null;
		for (int i = 0; i < locArray.length; i++)
		{
			MapLocation aLocation = locArray[i];
			int dist = aLocation.distanceSquaredTo(me);
			if (dist < closestDist && rc.senseNearbyGameObjects(Robot.class, aLocation, 0, rc.getTeam()).length < 1)
			{
				closestDist = dist;
				closestLocation = aLocation;
			}
		}
		return closestLocation;
	}

}
