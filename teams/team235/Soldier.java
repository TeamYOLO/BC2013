package team235;

import battlecode.common.*;

public class Soldier
{
	private static int nukeChannel = 2894;
	private static int opponentNukeHalfDone = 56893349;

	private static int campChannel = 45123;
	private static int gen = 6;
	private static int sup = 0;

	private static int massAmount = 15;

	private static RobotController rc;
	private static MapLocation rallyPoint;
	private static int[][] neighborArray;
	private static int[] self = {2,2};
	private static int[][] surroundingIndices = new int[5][5];

	public static void soldierCode(RobotController myRC)
	{
		rc = myRC;
		rallyPoint = findRallyPoint();
		surroundingIndices = initSurroundingIndices(Direction.NORTH);
		while(true)
		{
			try
			{
				Robot[] enemyRobots = rc.senseNearbyGameObjects(Robot.class,63,rc.getTeam().opponent());
				if(enemyRobots.length==0) // no enemies are nearby
				{
					if(rc.readBroadcast(nukeChannel) != opponentNukeHalfDone)
					{
						if (expandOrRally())
						{	
							// we should be expanding
							expand();
						}
						else
						{
							//we should be massing and dancing
							mass();
						}
					}
					else // if opponent nuke is half done, we must begin the attack asap
					{
						mass();
					}
				}
				else
				{
					// enemy spotted
					MapLocation closestEnemy = findClosestRobot(enemyRobots);
					smartCountNeighbors(enemyRobots,closestEnemy); // TODO: USE THIS!!!!!!
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

	private static void expand() throws GameActionException
	{
		rallyPoint = findClosestLocation(rc.senseAllEncampmentSquares());

		if(rc.getLocation().distanceSquaredTo(rallyPoint) < 1) // if we are at the location of the rally point
		{
			if(rc.isActive()) // if we are allowed to capture
			{
				if(rc.senseCaptureCost() < rc.getTeamPower()) // if we have enough power to capture
				{
					if(rc.readBroadcast(campChannel) == gen)
					{
						rc.broadcast(campChannel, sup);
						rc.captureEncampment(RobotType.GENERATOR);
					}
					else if(rc.readBroadcast(campChannel) == sup)
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
			goToLocation(rallyPoint);
		}
		else
		{
			goToLocation(rallyPoint);
		}
	}

	private static void mass() throws GameActionException
	{
		rallyPoint = findRallyPoint();
		// if we are fairly close to the rally point and we have the necessary soldier counts to make up a wave, gogogogogo
		if(rc.getLocation().distanceSquaredTo(rallyPoint) < 13)
		{
			int soldierCount = rc.senseNearbyGameObjects(Robot.class, rallyPoint, 25, rc.getTeam()).length;
			if(soldierCount > massAmount)
			{
				while(true)
				{
					goToLocation(rc.senseEnemyHQLocation());
					rc.yield();
				}
			}
			else
			{
				// perhaps plant a mine?
				//if (soldierCount + 5 < massAmount)
				//{
				//	rc.layMine();
				//}
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

	private static MapLocation findRallyPoint()
	{
		MapLocation enemyLoc = rc.senseEnemyHQLocation();
		MapLocation ourLoc = rc.senseHQLocation();
		int x = (enemyLoc.x + 2 * ourLoc.x) / 3;
		int y = (enemyLoc.y + 2 * ourLoc.y) / 3;
		return new MapLocation(x,y);
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

	public static int[] locToIndex(MapLocation ref, MapLocation test,int offset) // 40 bytecodes??
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

	public static boolean expandOrRally() // true for expand
	{
		// rush distance
		//double rushDistance = rc.senseHQLocation().distanceSquaredTo(rc.senseEnemyHQLocation());
		// number of encampments
		int numCamps = rc.senseAllEncampmentSquares().length;
		int numAlliedCamps = rc.senseAlliedEncampmentSquares().length;

		if(numAlliedCamps > 12) return false;
		if(numCamps < 20 && numAlliedCamps >8 ) return false; 
		if(numCamps < 10 && numAlliedCamps >3)  return false;
		else if(numCamps < 40 && numAlliedCamps > numCamps-1/2) return false;

		return true;
	}
}
