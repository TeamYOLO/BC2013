package team235;
import battlecode.common.*;

// it's spelled Battlecode

public class RobotPlayer
{
	private static RobotController rc;
	private static MapLocation rallyPoint;
	private static int[][] neighborArray;
	private static int[] self = {2,2};
	private static int[][] surroundingIndices = new int[5][5];
	public static void run(RobotController myRC)
	{
		rc = myRC;
		rallyPoint = findRallyPoint();
		surroundingIndices = initSurroundingIndices(Direction.NORTH);
		while(true){
			try{
				if (rc.getType()==RobotType.SOLDIER){
					soldierCode();
				}else{
					hqCode();
				}
			}catch (Exception e){
				System.out.println("caught exception before it killed us:");
				e.printStackTrace();
			}
			rc.yield();
		}
	}
	private static void soldierCode(){
		while(true){
			try{
				Robot[] enemyRobots = rc.senseNearbyGameObjects(Robot.class,1000000,rc.getTeam().opponent());
				if(enemyRobots.length==0){//no enemies nearby
					if (Clock.getRoundNum()<200){
						goToLocation(rallyPoint);
					}else{
						goToLocation(rc.senseEnemyHQLocation());
					}
				}else{//someone spotted
					MapLocation closestEnemy = findClosest(enemyRobots);
					smartCountNeighbors(enemyRobots,closestEnemy);
					goToLocation(closestEnemy);
				}
			}catch (Exception e){
				System.out.println("Soldier Exception");
				e.printStackTrace();
			}
			rc.yield();
		}
	}
	private static MapLocation findClosest(Robot[] enemyRobots) throws GameActionException {
		int closestDist = 1000000;
		MapLocation closestEnemy=null;
		for (int i=0;i<enemyRobots.length;i++){
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
	private static void goToLocation(MapLocation whereToGo) throws GameActionException {
		int dist = rc.getLocation().distanceSquaredTo(whereToGo);
		if (dist>0&&rc.isActive()){
			Direction dir = rc.getLocation().directionTo(whereToGo);
			int[] directionOffsets = {0,1,-1,2,-2};
			Direction lookingAtCurrently = null;
			lookAround: for (int d:directionOffsets){
				lookingAtCurrently = Direction.values()[(dir.ordinal()+d+8)%8];
				if(rc.canMove(lookingAtCurrently)){
					moveOrDefuse(lookingAtCurrently);
					break lookAround;
				}
			}
		}
	}
	private static void moveOrDefuse(Direction dir) throws GameActionException{
		MapLocation ahead = rc.getLocation().add(dir);
		if(rc.senseMine(ahead)!= null){
			rc.defuseMine(ahead);
		}else{
			rc.move(dir);
		}
	}
	private static MapLocation findRallyPoint() {
		MapLocation enemyLoc = rc.senseEnemyHQLocation();
		MapLocation ourLoc = rc.senseHQLocation();
		int x = (enemyLoc.x+3*ourLoc.x)/4;
		int y = (enemyLoc.y+3*ourLoc.y)/4;
		MapLocation rallyPoint = new MapLocation(x,y);
		return rallyPoint;
	}
	public static void hqCode() throws GameActionException{
		if (rc.isActive()) {
			// Spawn a soldier
			Direction dir = rc.getLocation().directionTo(rc.senseEnemyHQLocation());
			if (rc.canMove(dir))
				rc.spawn(dir);
		}
	}
	public static String intListToString(int[] intList){
		String sofar = "";
		for(int anInt:intList){
			sofar = sofar+anInt+" ";
		}
		return sofar;
	}
	// ARRAY-BASED NEIGHBOR DETECTION
	private static void smartCountNeighbors(Robot[] enemyRobots,MapLocation closestEnemy) throws GameActionException{
		//build a 5 by 5 array of neighboring units
		neighborArray = populateNeighbors(new int[5][5]);/*1500*/
		//get the total number of enemies and allies adjacent to each of the 8 adjacent tiles
		int[] adj = totalAllAdjacent(neighborArray);/*2500*/
		//also check your current position
		int me = totalAdjacent(neighborArray,self);
		//display the neighbor information to the indicator strings
		rc.setIndicatorString(0, "adjacent: "+intListToString(adj)+" me: "+me);
		//note: if the indicator string says 23, that means 2 enemies and 3 allies.
		//TODO: Now act on that data. I leave this to you. 
	}
	public static int[] locToIndex(MapLocation ref, MapLocation test,int offset){/*40*/
		int[] index = new int[2];
		index[0] = test.y-ref.y+offset;
		index[1] = test.x-ref.x+offset;
		return index;
	}
	public static int[][] initSurroundingIndices(Direction forward){
		int[][] indices = new int[8][2];
		// Direction forward =rc.getLocation().directionTo(rc.senseEnemyHQLocation());
		int startOrdinal = forward.ordinal();
		MapLocation myLoc = rc.getLocation();
		for(int i=0;i<8;i++){
			indices[i] = locToIndex(myLoc,myLoc.add(Direction.values()[(i+startOrdinal)%8]),0);
		}
		return indices;
	}
	public static String arrayToString(int[][] array){
		String outstr = "";
		for(int i=0;i<5;i++){
			outstr = outstr + "; ";
			for(int j=0;j<5;j++)
				outstr = outstr+array[i][j]+" ";
		}
		return outstr;
	}
	public static int[][] populateNeighbors(int[][] array) throws GameActionException{/*788*/
		MapLocation myLoc=rc.getLocation();
		Robot[] nearbyRobots = rc.senseNearbyGameObjects(Robot.class,8);
		// rc.setIndicatorString(2, "number of bots: "+nearbyRobots.length);
		for (Robot aRobot:nearbyRobots){
			RobotInfo info = rc.senseRobotInfo(aRobot);
			int[] index = locToIndex(myLoc,info.location,2);
			if(index[0]>=0&&index[0]<=4&&index[1]>=0&&index[1]<=4){
				if(info.team==rc.getTeam()){
					array[index[0]][index[1]]=1;//1 is allied
				}else{
					array[index[0]][index[1]]=10;//10 is enemy
				}
			}
		}
		return array;
	}
	public static int totalAdjacent(int[][] neighbors,int[] index){/*270*/
		int total = 0;
		for(int i=0;i<8;i++){
			total = total+neighbors[index[0]+surroundingIndices[i][0]][index[1]+surroundingIndices[i][1]];
		}
		return total;
	}
	public static int[] addPoints(int[] p1, int[] p2){/*30*/
		int[] tot = new int[2];
		tot[0] = p1[0]+p2[0];
		tot[1] = p1[1]+p2[1];
		return tot;
	}
	public static int[] totalAllAdjacent(int[][] neighbors){/*2454*/
		//TODO compute only on open spaces (for planned movement)
		int[] allAdjacent = new int[8];
		for(int i=0;i<8;i++){
			allAdjacent[i] =  totalAdjacent(neighbors,addPoints(self,surroundingIndices[i]));
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
}