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

		try{
			if (rc.getType()==RobotType.SOLDIER){
				Soldier.soldierCode(rc);
			}else{
				HQ.hqCode(rc);
			}
		}catch (Exception e){
			System.out.println("caught exception before it killed us:");
			e.printStackTrace();
		}
	}

}