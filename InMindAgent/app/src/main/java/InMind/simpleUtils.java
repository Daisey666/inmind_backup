package InMind;

import java.util.Date;

public class simpleUtils 
{
	
	public static Date addMillisec(Date base, int millisec) {
		return new Date(base.getTime()+millisec);
	}
	
	//returns date1 - date2 in milliseconds
	public static int subtractDatesInMillisec(Date date1, Date date2) {
		return (int)(date1.getTime() - date2.getTime());
	}
}
