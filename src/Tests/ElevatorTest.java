package Tests;

import allena.elevatorsimulator.*;
import static org.junit.Assert.assertTrue;

import java.net.InetAddress;
import java.sql.Time;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import org.json.JSONObject;
import org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

public class ElevatorTest {

	 private static ControlDate date;
	 
     private static Elevator elevator;
	 
     private static JSONObject subObj;
	 
	@Before
	public void setUpBeforeClass() throws Exception {
		System.out.println("GOing through");
		elevator = new Elevator(1,InetAddress.getLocalHost());
	}

	
	@Test
	public void testgoToDestination() {
		subObj = new JSONObject();
		try { 
			subObj.put("id", 1);
			subObj.put("InetAddress", InetAddress.getLocalHost().getHostName());
			subObj.put("currFloor", 1);
			subObj.put("State", Elevator.ElevatorState.IDLE );
			subObj.put("destinationFloor", 3);	
		} catch (Exception ignore ) {}
		elevator.goToDestination(subObj);
		assertTrue(3 == elevator.getCurrentFloor());
	}
	
	@Test
	public void testDoorJamFault() {
		elevator.setFaultFlag(false); 
		elevator.checkDoorFaultTest(7); 
		assertTrue(elevator.getFaultFlag()); 
	}
	
	@Test
	public void testFloorTimingFault() {
		subObj = new JSONObject();
		try { 
			subObj.put("id", 1);
			subObj.put("InetAddress", InetAddress.getLocalHost().getHostName());
			subObj.put("currFloor", 1);
			subObj.put("State", Elevator.ElevatorState.IDLE );
			subObj.put("destinationFloor", 7);	
		} catch (Exception e ) {
		}
		elevator.goToDestinationTest(subObj);
		assertTrue(elevator.getTFaultFlag());
		
	}


}
