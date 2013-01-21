package team235day6;

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
				Soldier.soldierCode(rc);
			}
			else if(t == RobotType.HQ)
			{
				HQ.hqCode(rc);
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