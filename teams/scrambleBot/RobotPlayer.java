package scrambleBot;
import battlecode.common.*;

public class RobotPlayer
{
	private static RobotController rc;
	
	public static void run(RobotController myRC) throws GameActionException
	{
		while(true) {
			myRC.broadcast(Constants.attackChannel, 27);
			
			for(int i = Constants.buildOrderBeginChannel; i<= Constants.buildOrderEndChannel; i++) {
				myRC.broadcast(i, 27);
			}
			myRC.broadcast(Constants.campChannel, 27);
			myRC.broadcast(Constants.commandChannel, 27);
			myRC.broadcast(Constants.rallyXChannel, 27);
			myRC.broadcast(Constants.rallyYChannel, 27);
			myRC.broadcast(Constants.singleExpandXChannel, 27);
			myRC.broadcast(Constants.singleExpandYChannel, 27);


			myRC.yield();
			}
		}
	}


