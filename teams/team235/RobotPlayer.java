package team235;

import battlecode.common.*;
public class RobotPlayer
{
	private static RobotController rc;
	
	public static void run(RobotController myRC)
	{
		rc = myRC;
		RobotType t = rc.getType();

			
		try
		{
			if (t == RobotType.SOLDIER)
			{
//				if(rc.senseHQLocation().distanceSquaredTo(rc.senseEnemyHQLocation()) < Constants.theMagicNumber) {
//					team235.secretRushStratGG.Soldier.soldierCode(rc);
//				}
//				else {
//					Soldier.soldierCode(rc);
//				}
				Soldier.soldierCode(rc);
			}
			else if(t == RobotType.HQ)
			{
//				if(rc.senseHQLocation().distanceSquaredTo(rc.senseEnemyHQLocation()) < Constants.theMagicNumber) {
//					team235.secretRushStratGG.HQ.hqCode(rc);
//				}
//				else {
//					HQ.hqCode(rc);
//				}
				HQ.hqCode(rc);

			}
			else if(t == RobotType.GENERATOR || t == RobotType.SUPPLIER)
			{
//				if(rc.senseHQLocation().distanceSquaredTo(rc.senseEnemyHQLocation()) < Constants.theMagicNumber) {
//					while(true) {
//						rc.yield();
//					}
//				}
				Supplier.supplierCode(rc);
			}
			else
			{
				while(true)
				{
					rc.yield();
				}
			}
		}
		catch (Exception e)
		{
			System.out.println("caught exception before it killed us:");
			e.printStackTrace();
		}
	}
}