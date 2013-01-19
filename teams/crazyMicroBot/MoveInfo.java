package crazyMicroBot;

import battlecode.common.*;

public class MoveInfo implements Comparable<MoveInfo>
{
	public int attackableRobos;
	public Direction d;
	public int x;
	public int y;
	public int val;
	
	public MoveInfo(int a, Direction dd, int xx, int yy, int vall)
	{
		attackableRobos = a;
		d = dd;
		x = xx;
		y = yy;
		val = vall;
	}

	@Override
	public int compareTo(MoveInfo m)
	{
		// -1 means m is better than this guy
		if(attackableRobos > 0 && val >= 0 && m.attackableRobos >0 && m.val >=0)
		{
			return val-m.val;
		}
		if(attackableRobos > 0 && val >= 0 && m.attackableRobos >0 && m.val <0)
		{
			return 1;
		}
		if(attackableRobos > 0 && val >= 0 && m.attackableRobos <=0 && m.val >=0)
		{
			return 1;
		}
		if(attackableRobos > 0 && val >= 0 && m.attackableRobos <=0 && m.val <0)
		{
			return 1;
		}
		if(attackableRobos > 0 && val < 0 && m.attackableRobos >0 && m.val >=0)
		{
			return -1;
		}
		if(attackableRobos > 0 && val < 0 && m.attackableRobos >0 && m.val <0)
		{
			return val-m.val;
		}
		if(attackableRobos > 0 && val < 0 && m.attackableRobos <=0 && m.val >=0)
		{
			return -1;
		}
		if(attackableRobos > 0 && val < 0 && m.attackableRobos <=0 && m.val <0)
		{
			return -1;
		}
		if(attackableRobos <= 0 && val >= 0 && m.attackableRobos >0 && m.val >=0)
		{
			return -1;
		}
		if(attackableRobos <= 0 && val >= 0 && m.attackableRobos >0 && m.val <0)
		{
			return 1;
		}
		if(attackableRobos <= 0 && val >= 0 && m.attackableRobos <=0 && m.val >=0)
		{
			return val-m.val;
		}
		if(attackableRobos <= 0 && val >= 0 && m.attackableRobos <=0 && m.val <0)
		{
			return 1;
		}
		if(attackableRobos <= 0 && val < 0 && m.attackableRobos >0 && m.val >=0)
		{
			return -1;
		}
		if(attackableRobos <= 0 && val < 0 && m.attackableRobos >0 && m.val <0)
		{
			return 1;
		}
		if(attackableRobos <= 0 && val < 0 && m.attackableRobos <=0 && m.val >=0)
		{
			return -1;
		}
		if(attackableRobos <= 0 && val < 0 && m.attackableRobos <=0 && m.val <0)
		{
			return val-m.val;
		}
		return 0;
	}
}
