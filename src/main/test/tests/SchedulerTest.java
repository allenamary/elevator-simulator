package tests;

import allena.elevatorsimulator.*;

import static org.junit.Assert.*;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.Time;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

public class SchedulerTest {
	
	private static InetAddress floorAddress;

	private static JSONObject subObj;

	@Test
	public void test() {
		try {
			floorAddress = InetAddress.getLocalHost();
			Scheduler s = new Scheduler(floorAddress);
			Elevator e = new Elevator(1, InetAddress.getLocalHost());
			Thread.sleep(1000); 
			System.out.println(s.getNumElevators());
			assertEquals(s.getNumElevators(), 1);
			subObj = new JSONObject();
			subObj.put("id", 1);
			subObj.put("InetAddress", InetAddress.getLocalHost().getHostName());
			subObj.put("currFloor", 1);
			subObj.put("State", Elevator.ElevatorState.IDLE );
			subObj.put("destinationFloor", 2);
			JSONObject elevInitState = s.getElevatorInfo(1);
			System.out.println("INitial: " + elevInitState);
			e.goToDestination(subObj);
			Thread.sleep(1000);
			JSONObject elevCurrState = s.getElevatorInfo(1);
			System.out.println("Current: " + elevCurrState);
			assertFalse((s.getElevatorInfo(1).toString()).equals(elevInitState.toString()));
			SimpleDateFormat sdf = new SimpleDateFormat("hh:mm:ss.S");
			Date date = sdf.parse("09:09:09.1");
			Time time = new Time(date.getTime());
			ControlDate c = new ControlDate(time, 5, false, 2);	
			byte msg[] =c.getByteArray();
			String msgString =  c.toString();
			DatagramPacket sendPacket = new DatagramPacket(msg, msg.length, InetAddress.getLocalHost(), 23);
			DatagramSocket sendSocket = new DatagramSocket();
			sendSocket.send(sendPacket);
			Thread.sleep(500); 
			System.out.println("REQUESTS  " + s.currReq);
			assertTrue(s.currReq != null);
			sendSocket.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	

}
