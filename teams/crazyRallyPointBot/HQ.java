package crazyRallyPointBot;

import battlecode.common.*;
import battlecode.engine.instrumenter.lang.System;

import java.util.*;

public class HQ {
	private static int allInRound = -100;

	private static int rallyRadius = 33;

	private static int massAmount = 25;

	private static RobotController rc;
	private static double minPowerThreshold = 100; //TODO-findthisvalue
	private static double minRoundThreshold = 100; //TODO-findthisvalue
	private static int gencount = 0;
	private static int soldiercount = 0;
	private static int othercount = 0;

	private static int optimalBuildings = 0;
	private static int farAwayButSafeBuildings = 0;

	private static int powerThreshold =500;

	private static boolean expandPhase = true;
	
	private static int rushDistance = 0;
	
	private static Robot[] alliedRobots;
	private static Robot[] enemyRobots;
	private static Robot[] alliedRobotsMassed;

	private static MapLocation rallyPoint = new MapLocation(0,0);
	private static MapLocation centerOfMass = new MapLocation(0,0);
	
	private static MapLocation enemyHQ;
	
	private static int rallyPercent = 20;
	private static double underEstimateFactor = 1.0;
		
	public static void hqCode(RobotController myRC) throws GameActionException
	{
		rc = myRC;
		Direction defaultSpawnDir = rc.getLocation().directionTo(rc.senseEnemyHQLocation());
		evaluateMap();
		rallyPoint = rc.getLocation();
		while(true) 
		{
			alliedRobots = rc.senseNearbyGameObjects(Robot.class, new MapLocation(0,0), 1000000, rc.getTeam());

			
			enemyRobots = rc.senseNearbyGameObjects(Robot.class, new MapLocation(0,0), 1000000, rc.getTeam().opponent());

			rc.broadcast(Constants.attackChannel, 0);
			rc.broadcast(Constants.moveTypeChannel, Constants.moveTypeAMove);

			if(expandOrRally())
			{
				rc.broadcast(Constants.commandChannel, Constants.commandExpand);
			}
			else
			{
				rc.broadcast(Constants.commandChannel,Constants.commandRally);
				rallyPoint = findRallyPoint();
				rc.broadcast(Constants.rallyXChannel, rallyPoint.x);
				rc.broadcast(Constants.rallyYChannel, rallyPoint.y);

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
				}
			}

			//shallWeAllIn();
			rc.setIndicatorString(0, rallyPoint.x + " " + rallyPoint.y + " " + rallyPercent +" "+rc.readBroadcast(Constants.moveTypeChannel));
			rc.yield();
		}
	}



	/*private static void shallWeAllIn() throws GameActionException
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

		if(massedRobos > massAmount) // if we should all in...
		{
			rc.broadcast(Constants.attackChannel, Constants.attackAllIn);
			allInRound = Clock.getRoundNum();
		}
	}*/

	private static MapLocation findRallyPoint() throws GameActionException
	{
		
		
		
		MapLocation closestEnemy = null;
		closestEnemy = Util.findClosestRobot(rc, enemyRobots);
		int x=0;
		int y=0;
		int xsum = 0;
		int ysum = 0;
		if(closestEnemy != null && closestEnemy.distanceSquaredTo(rc.getLocation()) < rushDistance*.2) {
			rc.broadcast(Constants.moveTypeChannel, Constants.moveTypeAMove);
			return closestEnemy;
		}
		
		
		
		
		//int [] xLocations = new int [alliedRobots.length];
		//int [] yLocations = new int [alliedRobots.length];

		//median code (uses too much bytecode as is)
		//for(int i=0; i < alliedRobots.length ; i++)
//		{
//			RobotInfo r = rc.senseRobotInfo(alliedRobots[i]);
//			xLocations[i] = r.location.x;
//			yLocations[i] = r.location.y;
//
//		}
		
		//int lol = Clock.getBytecodeNum();
		//Arrays.sort(xLocations);
		//Arrays.sort(yLocations);
		//rc.setIndicatorString(0, "bytecodes uuusssed :" + (Clock.getBytecodeNum()-lol));
		//centerOfMass = new MapLocation(xLocations[xLocations.length/2], yLocations[yLocations.length/2]);

		int lol = Clock.getBytecodeNum();
		
		ArrayList<Robot> soldiers = new ArrayList<Robot>();
		int gdogs = 0;
		int others = 0;
		for(Robot r : alliedRobots)
		{
			RobotType rt = rc.senseRobotInfo(r).type;
			if(rt == RobotType.SOLDIER) soldiers.add(r);
			if(rt == RobotType.GENERATOR) gdogs++;
			else others++;
		}
		
		double decay = .8;
		if(rc.hasUpgrade(Upgrade.FUSION))
		{
			decay = .99;
		}

		
		for(int i=0; i < soldiers.size() ; i++)
		{
			RobotInfo r = rc.senseRobotInfo(soldiers.get(i));
			xsum += r.location.x;
			ysum += r.location.y;
		}
		
		centerOfMass = new MapLocation((int)(((double)xsum)/soldiers.size()), (int)(((double)ysum)/soldiers.size()));
		rc.setIndicatorString(1, "center of mass :" + centerOfMass.x + " " + centerOfMass.y);

		
		
		alliedRobotsMassed = rc.senseNearbyGameObjects(Robot.class, centerOfMass, 63, rc.getTeam());

		
		int ralliedBots = 0;
		
		for(Robot r : alliedRobotsMassed)
		{
			RobotType rt = rc.senseRobotInfo(r).type;
			if(rt == RobotType.SOLDIER) ++ralliedBots;
		}			
		
		if(enemyRobots.length < underEstimateFactor * ralliedBots && alliedRobotsMassed.length > .5*(40 + (10 * gencount) - (1 * othercount)))
		{
			rc.broadcast(Constants.moveTypeChannel, Constants.moveTypeAMove);
			return centerOfMass.add(centerOfMass.directionTo(enemyHQ), 2);
		}
		else {
			rc.broadcast(Constants.moveTypeChannel, Constants.moveTypeRightClick);
			return rc.getLocation().add(rc.getLocation().directionTo(enemyHQ), 5);
		}
		
		
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

		for(Robot r : alliedRobots)
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

	public static void evaluateMap() throws GameActionException {
		MapLocation neutralCamps[] = rc.senseEncampmentSquares(rc.getLocation(), 1000000, Team.NEUTRAL);
		MapLocation me = rc.getLocation();
		enemyHQ = rc.senseEnemyHQLocation();
		rushDistance = enemyHQ.distanceSquaredTo(me);
		for (MapLocation loc : neutralCamps) {
			double toUs=loc.distanceSquaredTo(me);
			double toThem = loc.distanceSquaredTo(enemyHQ);			
			if(toUs/toThem < .81)
			{
				if(rushDistance > toUs)
					optimalBuildings++;
				else farAwayButSafeBuildings++;
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
