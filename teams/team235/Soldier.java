package team235; // test

import java.util.ArrayList;

import battlecode.common.*;

public class Soldier
{
	private static int rallyRadius = 25;

	private final static int localScanRange = 14;


	private static boolean localscan = false;

	private static RobotController rc;
	private static MapLocation rallyPoint;
	private static int[][] neighborArray;
	private static int[] self = {2,2};
	private static int[][] surroundingIndices = new int[5][5];
	
	private static int[][] grandNeighborArray;

	public static void soldierCode(RobotController myRC) throws GameActionException
	{
		rc = myRC;
		rallyPoint = findRallyPoint();
		surroundingIndices = initSurroundingIndices(Direction.NORTH);
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
					case Constants.commandBuildIndividual: //TODO- implement this
						break;
					}
				}
				else
				{
					localscan = false;
					if(closeEnemyRobots.length == 0)
					{
						// TODO: move to reinforce since there are no robos right by us
						
					}
					else
					{						
						Direction toGo = getMove();
						goToLocation(rc.getLocation().add(toGo));
					}
					// enemy spotted
					//localscan = false;
					//MapLocation closestEnemy = findClosestRobot(enemyRobots);
					//smartCountNeighbors(enemyRobots,closestEnemy); // TODO: USE THIS!!!!!!
					//goToLocation(closestEnemy);
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
	
	private static Direction getMove() throws GameActionException
	{
		// micro code, for now only take engagements that are even or better
		
		grandNeighborArray = populate7x7neighbors();
		rc.setIndicatorString(0, bigArrayToString(grandNeighborArray));
		
		ArrayList<Direction> possibleMoves = new ArrayList<Direction>();
		
		// enemies within sight range: closeEnemyRobots
		Robot[] closeEnemyRobots = rc.senseNearbyGameObjects(Robot.class, rc.getLocation(), 14, rc.getTeam().opponent());
		
		// allies who may help us with this engagement:
		Robot[] closeAlliedRobots = rc.senseNearbyGameObjects(Robot.class, rc.getLocation(), 14, rc.getTeam());
		
		// possible directions that can be moved
		for(Direction d: Direction.values())
		{
			if(d != Direction.OMNI && d != Direction.NONE && rc.canMove(d))
			{
				possibleMoves.add(d);
			}
		}
		possibleMoves.add(Direction.NONE);
		
		for(Direction d : possibleMoves)
		{
			int attackableRobos = 0;
			
		}
		return possibleMoves.get(0);
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
		// if we are fairly close to the rally point and we have the necessary soldier counts to make up a wave, gogogogogo
		if(rc.getLocation().distanceSquaredTo(rallyPoint) < rallyRadius)
		{
			if(rc.readBroadcast(Constants.attackChannel) == Constants.attackAllIn)
			{
				while(true)
				{
					goToLocation(rc.senseEnemyHQLocation());
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
					moveOrDefuse(lookingAtCurrently);
					break;
				}
			}
		}
	}

	private static void moveOrDefuse(Direction dir) throws GameActionException
	{
		MapLocation ahead = rc.getLocation().add(dir);
		if(rc.senseMine(ahead) != null)
		{
			rc.defuseMine(ahead);
		}
		else
		{
			rc.move(dir);
		}
	}

	public static String intListToString(int[] intList)
	{
		String sofar = "";
		for(int anInt : intList)
		{
			sofar = sofar + anInt + " ";
		}
		return sofar;
	}
	
	private static void reallySmartCountNeighbors() throws GameActionException
	{
		// build ze 7x7 array of neighboring units
		grandNeighborArray = populate7x7neighbors();
	}

	// ARRAY-BASED NEIGHBOR DETECTION
	private static void smartCountNeighbors(Robot[] enemyRobots, MapLocation closestEnemy) throws GameActionException
	{
		//build a 5 by 5 array of neighboring units
		neighborArray = populateNeighbors(new int[5][5]); // 1500
		//get the total number of enemies and allies adjacent to each of the 8 adjacent tiles
		int[] adj = totalAllAdjacent(neighborArray); // 2500
		//also check your current position
		int me = totalAdjacent(neighborArray,self);
		//display the neighbor information to the indicator strings
		rc.setIndicatorString(0, "adjacent: " + intListToString(adj) + " me: " + me);
		//note: if the indicator string says 23, that means 2 enemies and 3 allies.
		//TODO: Now act on that data. I leave this to you. 
	}

	public static int[] locToIndex(MapLocation ref, MapLocation test, int offset) // 40 bytecodes??
	{
		int[] index = new int[2];
		index[0] = test.y-ref.y+offset;
		index[1] = test.x-ref.x+offset;
		return index;
	}

	public static int[][] initSurroundingIndices(Direction forward)
	{
		int[][] indices = new int[8][2];
		int startOrdinal = forward.ordinal();
		MapLocation myLoc = rc.getLocation();
		for(int i=0;i<8;i++)
		{
			indices[i] = locToIndex(myLoc, myLoc.add(Direction.values()[(i + startOrdinal) % 8]), 0);
		}
		return indices;
	}

	public static String arrayToString(int[][] array)
	{
		String outstr = "";
		for(int i = 0; i < 5; i++)
		{
			outstr = outstr + "; ";
			for(int j = 0; j < 5; j++)
			{
				outstr = outstr+array[i][j] + " ";
			}
		}
		return outstr;
	}
	
	public static String bigArrayToString(int[][] array)
	{
		String outstr = "";
		for(int i = 0; i < 7; i++)
		{
			outstr = outstr + "; ";
			for(int j = 0; j < 7; j++)
			{
				outstr = outstr+array[i][j] + " ";
			}
		}
		return outstr;
	}
	
	private static int[][] populate7x7neighbors() throws GameActionException
	{
		int[][] array = new int[7][7];
		MapLocation myLoc = rc.getLocation();
		Robot[] closeRobos = rc.senseNearbyGameObjects(Robot.class, 14);
		
		for(Robot aRobot : closeRobos)
		{
			RobotInfo info = rc.senseRobotInfo(aRobot);
			int[] index = locToIndex(myLoc, info.location, 3);
			if(index[0] >= 0 && index[0] <= 6 && index[1] >= 0 && index[1] <= 6)
			{
				array[index[0]][index[1]] = info.team == rc.getTeam() ? 100 : -100;
			}
		}
		return array;
	}

	public static int[][] populateNeighbors(int[][] array) throws GameActionException // 788
	{
		MapLocation myLoc = rc.getLocation();
		Robot[] nearbyRobots = rc.senseNearbyGameObjects(Robot.class, 8);

		for (Robot aRobot : nearbyRobots)
		{
			RobotInfo info = rc.senseRobotInfo(aRobot);
			int[] index = locToIndex(myLoc, info.location, 2);
			if(index[0] >= 0 && index[0] <= 4 && index[1] >= 0 && index[1] <= 4)
			{
				if(info.team == rc.getTeam())
				{
					array[index[0]][index[1]]=1;//1 is allied
				}
				else
				{
					array[index[0]][index[1]]=10;//10 is enemy
				}
			}
		}
		return array;
	}

	public static int totalAdjacent(int[][] neighbors, int[] index) // 270
	{ 
		int total = 0;
		for(int i = 0; i < 8; i++)
		{
			total = total + neighbors[index[0] + surroundingIndices[i][0]][index[1] + surroundingIndices[i][1]];
		}
		return total;
	}

	public static int[] addPoints(int[] p1, int[] p2) // 30
	{
		int[] tot = new int[2];
		tot[0] = p1[0] + p2[0];
		tot[1] = p1[1] + p2[1];
		return tot;
	}

	public static int[] totalAllAdjacent(int[][] neighbors) // 2454
	{
		//TODO compute only on open spaces (for planned movement)
		int[] allAdjacent = new int[8];
		for(int i=0;i<8;i++)
		{
			allAdjacent[i] =  totalAdjacent(neighbors, addPoints(self, surroundingIndices[i]));
		}
		return allAdjacent;
	}

	//heuristic: goodness or badness of a neighbor int, which includes allies and enemies
	public static double howGood(int neighborInt){
		double goodness = 0;
		double numberOfAllies = neighborInt%10;
		double numberOfEnemies = neighborInt-numberOfAllies;
		// goodness = ?????; //what heuristic will you use?
		return goodness;
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
