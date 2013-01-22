package team235;

import battlecode.common.*;

import java.util.ArrayList;

public class Supplier
{
	private static RobotController rc;
	private static ArrayList<Integer> enemyChannels = new ArrayList<Integer>();

	public static void supplierCode(RobotController myRC) throws GameActionException
	{
		rc = myRC;

		// determine number of suppliers and gens before me
		int order = determineMyOrder();
		rc.yield();

		while(Clock.getRoundNum() < 250)
		{
			rc.yield();
		}

		if(order < 14)
		{
			int channelMin = 5000 * order;
			int channelMax = channelMin + 5000;
			String s = "";

			if(channelMin == 30000)
			{
				// scan phase for our channel range
				for(int i = channelMin; i < channelMax; i++)
				{
					if(rc.readBroadcast(i) != 0 && i != Constants.attackChannel && !(i >= Constants.buildOrderBeginChannel && i <= Constants.buildOrderEndChannel) && 
							i != Constants.campChannel && i != Constants.commandChannel && i != Constants.rallyXChannel
							&& i != Constants.rallyYChannel && i != Constants.singleExpandXChannel && i != Constants.singleExpandYChannel)
					{
						enemyChannels.add(i);
						s += (i + " " + rc.readBroadcast(i) + ";");
					}
					if(i % 150 == 0) rc.yield();
				}
			}
			else
			{
				// scan phase for the other channel range
				for(int i = channelMin; i < channelMax && i < 65536; i++)
				{
					if(rc.readBroadcast(i) != 0)
					{
						enemyChannels.add(i);
					}
					if(i % 150 == 0) rc.yield();
				}
			}
			
			
			/*for(int i: enemyChannels)
			{
				s += i + " ";
			}*/
			rc.setIndicatorString(0,s);

			while(Clock.getRoundNum() < 309)
			{
				rc.yield();
			}

			// jam phase TURN THE HEAT UP
			while(true)
			{
				if(enemyChannels.size() < 20)
				{
					for(int i : enemyChannels)
					{
						rc.broadcast(i, 0);
					}
				}
				rc.yield();
			}
		}
		else
		{
			while(true)
			{
				rc.yield();
			}
		}

	}

	private static int determineMyOrder() throws GameActionException
	{
		Robot[] robos = rc.senseNearbyGameObjects(Robot.class, rc.getLocation(), 1000000, rc.getTeam());
		int beforeMe = 0;
		int myID = rc.getRobot().getID();

		for(Robot r: robos)
		{
			if(r.getID() < myID)
			{
				RobotInfo info = rc.senseRobotInfo(r);
				if(info.type == RobotType.GENERATOR || info.type == RobotType.SUPPLIER)
				{
					beforeMe++;
				}
			}
		}
		return beforeMe;
	}
}
