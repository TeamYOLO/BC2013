package team235; // test

import battlecode.common.*;

public class Soldier
{
	private static int rallyRadius = 25;

	private final static int localScanRange = 14;

	private static boolean localscan = false;

	private static RobotController rc;
	private static MapLocation rallyPoint;

	public static void soldierCode(RobotController myRC) throws GameActionException
	{
		rc = myRC;
		rallyPoint = findRallyPoint();
		while(true)
		{
			try
			{
				// returns all enemy robots within our sight range (aka that we can actually fight right now or very soon)
				Robot[] closeEnemyRobots = rc.senseNearbyGameObjects(Robot.class, rc.getLocation(), 14, rc.getTeam().opponent());

				// returns all enemy robots reasonably close to us (aka that we should move toward and potentially assist our allies in fighting)
				Robot[] enemyRobots = rc.senseNearbyGameObjects(Robot.class,63,rc.getTeam().opponent());

				if(closeEnemyRobots.length == 0 && enemyRobots.length == 0) // no enemies are nearby
				{
					int command = HQCommand();
					switch (command) {
					case Constants.commandExpand:
						expand();
						break;
					case Constants.commandRally:
						rally();
						break;
					case Constants.commandEnemyNukeHalfDone:
						rally();
						break;
					case Constants.commandBuildIndividual: //TODO- implement this
						break;
					}
				}
				else
				{
					// enemy spotted
					localscan = false;
					MapLocation closestEnemy = findClosestRobot(enemyRobots);
					goToLocation(closestEnemy);
				}
			}
			catch (Exception e)
			{
				System.out.println("Soldier Exception");
				e.printStackTrace();
			}
			rc.yield();
		}
	}


	private static MapLocation findRallyPoint() throws GameActionException
	{
		return new MapLocation(rc.readBroadcast(Constants.rallyXChannel), rc.readBroadcast(Constants.rallyYChannel));
	}

	private static int getNumberOfAlliedRobosAfterMe() throws GameActionException
	{
		int retval = 0;
		int myID = rc.getRobot().getID();

		Robot[] robos = rc.senseNearbyGameObjects(Robot.class, new MapLocation(0,0), 1000000, rc.getTeam());

		for(Robot r : robos)
		{
			if(r.getID() > myID)
			{
				++retval;
			}
		}

		return retval;
	}

	private static void expand() throws GameActionException
	{
		//rallyPoint = findClosestLocation(rc.senseAllEncampmentSquares());
		if(!localscan) {
			rallyPoint = findClosestEmptyCamp();
		}
		if(rc.getLocation().distanceSquaredTo(rallyPoint) < 1) // if we are at the location of the rally point
		{
			if(!localscan) {
				rallyPoint = findFurthestLocalCamp();
				localscan=true;
			}
		}
		if(rc.getLocation().distanceSquaredTo(rallyPoint) < 1) // if we are at the location of the rally point
		{


			if(rc.isActive()) // if we are allowed to capture
			{
				if(rc.senseCaptureCost() + 1.8 * getNumberOfAlliedRobosAfterMe() < rc.getTeamPower()) // if we have enough power to capture
				{
					int readIn=0;
					for(int i = Constants.buildOrderBeginChannel ; i <= Constants.buildOrderEndChannel; i++) {
						readIn=rc.readBroadcast(i);
						if(readIn==Constants.buildOrderGen) {
							rc.broadcast(i, 0);
							rc.captureEncampment(RobotType.GENERATOR);
							break;
						}
						else if(readIn==Constants.buildOrderSup) {
							rc.broadcast(i, 0);
							rc.captureEncampment(RobotType.SUPPLIER);
							break;
						}
						else if(readIn==Constants.buildOrderShield) {
							rc.broadcast(i, 0);
							rc.captureEncampment(RobotType.SHIELDS);
							break;
						}
						else if(readIn==Constants.buildOrderHeal) {
							rc.broadcast(i, 0);
							rc.captureEncampment(RobotType.MEDBAY);
							break;
						}
						else if(readIn==Constants.buildOrderArt) {
							rc.broadcast(i, 0);
							rc.captureEncampment(RobotType.ARTILLERY);
							break;
						}
					}


					//					readIn = rc.readBroadcast(Constants.campChannel);
					//					if(readIn == Constants.campGen)
					//					{
					//						rc.broadcast(Constants.campChannel, Constants.campGenInProduction);
					//						rc.captureEncampment(RobotType.GENERATOR);
					//					}
					//					else if(readIn == Constants.campGenInProduction)
					//					{
					//						rc.captureEncampment(RobotType.SUPPLIER);
					//					}
					//					else if(readIn == Constants.campSupplier)
					//					{ 
					//						rc.captureEncampment(RobotType.SUPPLIER);
					//					}
					//					else // TODO: transmissions may be being scrambled, for now just make supplier
					//					{
					//						rc.captureEncampment(RobotType.SUPPLIER);
					//					}
				}
			}
		}
		else if(rc.senseNearbyGameObjects(Robot.class, rallyPoint, 0, rc.getTeam()).length > 0) // if there is an allied robot on our rally point
		{
			rallyPoint = findClosestEmptyCamp();
			if(rallyPoint == null)
			{
				rallyPoint = findRallyPoint();
			}
			goToLocation(rallyPoint);
		}
		else
		{
			goToLocation(rallyPoint);
		}
	}

	private static void rally() throws GameActionException
	{
		rallyPoint = findRallyPoint();
		rc.setIndicatorString(0, rallyPoint.x + " " + rallyPoint.y);
		// if we are fairly close to the rally point and we have the necessary soldier counts to make up a wave, gogogogogo
		if(rc.getLocation().distanceSquaredTo(rallyPoint) < rallyRadius)
		{
			if(rc.readBroadcast(Constants.attackChannel) == Constants.attackAllIn)
			{
				while(true)
				{
					Robot[] closeEnemyRobots = rc.senseNearbyGameObjects(Robot.class, rc.getLocation(), 14, rc.getTeam().opponent());
					if(closeEnemyRobots.length == 0) {
						goToLocation(rc.senseEnemyHQLocation());
					}
					else {
						goToLocation(Util.findClosestRobot(rc, closeEnemyRobots));
					}
					rc.yield();
				}
			}
			else
			{
				goToLocation(rallyPoint);
			}
		}
		else 
		{
			goToLocation(rallyPoint);
		}
	}

	private static MapLocation findClosestRobot(Robot[] enemyRobots) throws GameActionException
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

	private static MapLocation findClosestLocation(MapLocation[] locArray) throws GameActionException {
		int closestDist = 1000000;
		MapLocation me = rc.getLocation();
		MapLocation closestLocation=null;
		for (int i=0; i<locArray.length; i++)
		{
			MapLocation aLocation = locArray[i];
			int dist = aLocation.distanceSquaredTo(me);
			if (dist < closestDist)
			{
				closestDist = dist;
				closestLocation = aLocation;
			}
		}
		return closestLocation;
	}
	private static MapLocation findFurthestLocalCamp() throws GameActionException {
		MapLocation result = null;
		MapLocation me = rc.getLocation();
		MapLocation[] locArray = rc.senseEncampmentSquares(me, localScanRange, Team.NEUTRAL);
		int furthestDist = -1;

		for (int i = 0; i < locArray.length; i++)
		{
			MapLocation aLocation = locArray[i];
			int dist = aLocation.distanceSquaredTo(me);
			if (dist > furthestDist && rc.senseNearbyGameObjects(Robot.class, aLocation, 0, rc.getTeam()).length < 1)
			{
				furthestDist = dist;
				result = aLocation;
			}
		}
		return result;

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

	private static void goToLocation(MapLocation whereToGo) throws GameActionException
	{
		MapLocation meee = rc.getLocation();
		int dist = meee.distanceSquaredTo(whereToGo);
		if (dist > 0 && rc.isActive())
		{
			Direction dir = meee.directionTo(whereToGo);
			int[] directionOffsets = {0, 1, -1, 2, -2};
			Direction lookingAtCurrently = null;
			for (int d : directionOffsets){
				lookingAtCurrently = Direction.values()[(dir.ordinal()+d+8)%8];
				if(rc.canMove(lookingAtCurrently))
				{
					boolean lol = moveOrDefuse(lookingAtCurrently);
					if(lol) break;
				}
			}
		}
	}

	private static boolean moveOrDefuse(Direction dir) throws GameActionException // true means we moved or are defusing
	{
		boolean retval = false;
		MapLocation ahead = rc.getLocation().add(dir);
		Team miney = rc.senseMine(ahead);
		if(miney == Team.NEUTRAL)
		{
			if(rc.senseNearbyGameObjects(Robot.class, rc.getLocation(), 14, rc.getTeam().opponent()).length == 0)
			{
				rc.defuseMine(ahead);
				retval = true;
			}
			else
			{
				retval = false;
			}
		}
		else if(miney == rc.getTeam().opponent())
		{
			rc.defuseMine(ahead);
			retval = true;
		}
		else
		{
			rc.move(dir);
			retval = true;
		}
		return retval;
	}

	public static int HQCommand() throws GameActionException // true for expand
	{
		return rc.readBroadcast(Constants.commandChannel);
	}

	public static void expandIndividual() throws GameActionException
	{
		MapLocation expandLocation = new MapLocation(rc.readBroadcast(Constants.singleExpandXChannel),rc.readBroadcast(Constants.singleExpandYChannel));
		rc.broadcast(Constants.commandChannel, Constants.commandRally);
		while(true) {
			if(rc.getLocation().distanceSquaredTo(expandLocation) < 1) // if we are at the location of the rally point
			{
				if(rc.isActive()) // if we are allowed to capture
				{
					if(rc.senseCaptureCost() + 1.8 * getNumberOfAlliedRobosAfterMe() < rc.getTeamPower()) // if we have enough power to capture
					{
						int readIn = rc.readBroadcast(Constants.campChannel);
						if(readIn == Constants.campGen)
						{
							rc.broadcast(Constants.campChannel, Constants.campGenInProduction);
							rc.captureEncampment(RobotType.GENERATOR);
						}
						else if(readIn == Constants.campGenInProduction)
						{
							rc.captureEncampment(RobotType.SUPPLIER);
						}
						else if(readIn == Constants.campSupplier)
						{ 
							rc.captureEncampment(RobotType.SUPPLIER);
						}
						else // TODO: transmissions may be being scrambled, for now just make supplier
						{
							rc.captureEncampment(RobotType.SUPPLIER);
						}
						break;
					}
				}
			}
			else if(rc.senseNearbyGameObjects(Robot.class, expandLocation, 0, rc.getTeam()).length > 0) // if there is an allied robot on our rally point
			{
				expandLocation = findClosestEmptyCamp();
				if(expandLocation == null)
				{
					expandLocation = findRallyPoint();
				}
				goToLocation(expandLocation);
			}
			else
			{
				goToLocation(expandLocation);
			}
			rc.yield();
		}
	}
}
