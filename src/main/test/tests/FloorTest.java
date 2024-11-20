package tests;

import allena.elevatorsimulator.*;

import static org.junit.Assert.assertTrue;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.Time;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.junit.Test;

public class FloorTest {

		@Test
		public void test() throws ParseException, UnknownHostException {
			Floor floor;
				floor = new Floor(InetAddress.getLocalHost());
			SimpleDateFormat sdf = new SimpleDateFormat("hh:mm:ss.S");
			String[] dates = { "09:09:09.1", "11:11:11.1", "12:45:06.7", "13:04:56.5", "14:05:15.0", "17:00:00.0",
					"17:20:10.1", "17:30:00.0", "16:05:34.4", "16:50:50.5" };
			Date date;
			Time[] time = new Time[10];
			for (int i = 0; i < 10; i++) {
				date = sdf.parse(dates[i]);
				time[i] = new Time(date.getTime());
			}
			ControlDate[] msg = new ControlDate[10];
			msg[0] = new ControlDate(time[0], 5, false, 2);
			msg[1] = new ControlDate(time[1], 2, true, 4);
			msg[2] = new ControlDate(time[2], 4, true, 6);
			msg[3] = new ControlDate(time[3], 3, false, 1);
			msg[4] = new ControlDate(time[4], 1, true, 9);
			msg[5] = new ControlDate(time[5], 2, true, 5);
			msg[6] = new ControlDate(time[6], 6, false, 2);
			msg[7] = new ControlDate(time[7], 2, false, 1);
			msg[8] = new ControlDate(time[8], 1, true, 4);
			msg[9] = new ControlDate(time[9], 4, false, 3);
			for (int i = 0; i < 10; i++) {
				assertTrue(msg[i].equals(floor.getdata(i)));
			}
		}
	}
