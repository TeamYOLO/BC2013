package team235;

import battlecode.common.*;

public class HQ {

	
	private static int campChannel = 45123;
	private static int gen = 6;
	private static int sup = 0;
	
	private static RobotController rc;
	private static double Eprime [] = new double[10];
	private static double Eprev = 40 ;
	private static int index = 0;
	private static double minPowerThreshold = 100; //TODO-findthisvalue
	private static double minRoundThreshold = 100; //TODO-findthisvalue
	private static int gencount = 0;
	public static void hqCode(RobotController myRC) throws GameActionException{
		rc=myRC;
		while(true) {
			if (rc.isActive()) {
				// Spawn a soldier
				Direction dir = rc.getLocation().directionTo(rc.senseEnemyHQLocation());
				if (rc.canMove(dir))
					rc.spawn(dir);
				
				if(doWeNeedGenerator()) {
					gencount++;
					rc.broadcast(campChannel, gen);
				}
				//Eprime[index] = rc.getTeamPower() - Eprev;
				//Eprev = rc.getTeamPower(); 
				
				
			}
			
			
			
			rc.yield();
		}
	}
	public static boolean doWeNeedGenerator() {
		double sum = 0;
		int teambots =rc.senseNearbyGameObjects(Robot.class, 100000, rc.getTeam()).length;
		if(rc.getTeamPower()<minPowerThreshold && Clock.getRoundNum()>minRoundThreshold) {
			return true;
		}
		if(3*teambots>40+10*gencount)
		{
		  return true;
		}
		//for(int i = 0; i < Eprime.length; ++i) {
		//	sum = sum + Eprime[i];
		//}
		
		return false;
	}
}
