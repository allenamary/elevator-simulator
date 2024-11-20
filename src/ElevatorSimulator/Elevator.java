package allena.elevatorsimulator;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Random;

import org.json.JSONException;
import org.json.JSONObject;


public class Elevator {

	public enum ElevatorState {
		IDLE, DOOR_OPEN, DOOR_CLOSED, UP, DOWN, FIXING_DOORS
	}

	private DatagramPacket receivePacket, subscribePacket, ackPacket;
	
    private DatagramSocket sendSocket, receiveSocket, subscribeSocket, ackSocket;
	
    private InetAddress schedulerAddress;
	
	public ElevatorState state;
	
    private int id;
	
    private int currFloor;

	private int updateStatusPort = 1026;

	private int ackPort = 1040;
	
    private int subscriptionPort = 1035;
	
    private JSONObject subObj;

	private int timer;

	private static int timer_time = 6;

	private boolean fault = false, tFault = false;

	public Elevator(int id, InetAddress schedulerAddress) {
		this.schedulerAddress = schedulerAddress;
		this.id = id;
		this.currFloor = 1;
		state = ElevatorState.IDLE;
		timer = timer_time;

		subObj = new JSONObject();
		updateJSONObj();

		byte[] subArr = subObj.toString().getBytes();

		try {
			receiveSocket = new DatagramSocket(69);
			subscribeSocket = new DatagramSocket();
			sendSocket = new DatagramSocket();
			ackSocket = new DatagramSocket(ackPort);

			subscribePacket = new DatagramPacket(subArr, subArr.length, schedulerAddress, subscriptionPort);
			subscribeSocket.send(subscribePacket);
			subscribeSocket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void setCurrFloor(int floor) {
		this.currFloor = floor;
	}
	private void updateJSONObj() {
		try {
			subObj.put("id", id);
			subObj.put("InetAddress", InetAddress.getLocalHost().getHostName());
			subObj.put("currFloor", currFloor);
			subObj.put("State", state);
		} catch (JSONException e1) {
			e1.printStackTrace();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
	}

	private void sendStateUpdate() {
		try {
			byte[] subArr = subObj.toString().getBytes();
			DatagramPacket updateStatePacket = new DatagramPacket(subArr, subArr.length, schedulerAddress,
					updateStatusPort);
			DatagramSocket updateStateSocket = new DatagramSocket();
			updateStateSocket.send(updateStatePacket);
			updateStateSocket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void checkDoorFault() {
		Random r = new Random();
		int val = r.nextInt(10); 

		if (val >= 7) {
			state = ElevatorState.FIXING_DOORS;
			System.out.println("Door is jamed. Please stand by while fixing ....");
			try {
				Thread.sleep(3000); 
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			System.out.println("Fixed!");
		}
	}

	public void moveElevator(JSONObject obj) {
		try {
			timer = timer_time;
			int passengerFloor = obj.getInt("floor");
			int dir = currFloor - passengerFloor;
			if (dir < 0) {
				for (int i = currFloor; i <= passengerFloor; i++) {
					updateTimer();
					System.out.println("Elevator: moving to floor " + currFloor++);
					
					Thread.sleep(2000);
					updateJSONObj();
					state = ElevatorState.UP;
					sendStateUpdate();
				}
				currFloor--;
				state = ElevatorState.DOOR_OPEN;
				System.out.println("got to passenger(s) who made the request");
				checkDoorFault();
				state = ElevatorState.DOOR_CLOSED;
				goToDestination(obj);

			} else if (dir == 0) {
				System.out.println("elevator at passenger floor: open doors");
				state = ElevatorState.DOOR_OPEN;
				checkDoorFault();
				state = ElevatorState.DOOR_CLOSED;
				goToDestination(obj);
			} else {
				for (int i = currFloor; i >= passengerFloor; i--) {
					updateTimer();
					System.out.println("Elevator: moving to floor " + currFloor--);
					Thread.sleep(2000);
					updateJSONObj();
					state = ElevatorState.DOWN;
					sendStateUpdate();
				}
				currFloor++;
				state = ElevatorState.DOOR_OPEN;
				System.out.println("got to passenger(s) who made the request");
				checkDoorFault();
				state = ElevatorState.DOOR_CLOSED;
				goToDestination(obj);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private void updateTimer() throws Exception {
		timer--;
		if (timer == 0) {
			tFault = true;
			throw new Exception("Fatal floor timing error.. exiting");
		}
	}

	public void goToDestination(JSONObject obj) {
		int destinationFloor;
		try {
			timer = timer_time;
			destinationFloor = obj.getInt("destinationFloor");
			int goToDestination = currFloor - destinationFloor;
			if (goToDestination < 0) {
				for (int i = currFloor; i < destinationFloor; i++) {
					updateTimer();
					System.out.println("Elevator: moving to floor " + ++currFloor);
					
					Thread.sleep(2000);

					updateJSONObj();
					state = ElevatorState.UP;
					sendStateUpdate();
				}
				state = ElevatorState.DOOR_OPEN;
				checkDoorFault();
				state = ElevatorState.DOOR_CLOSED;
				state = ElevatorState.IDLE;

			} else {
				for (int i = currFloor; i > destinationFloor; i--) {
					updateTimer();
					System.out.println("Elevator: moving to floor " + --currFloor);
					
					Thread.sleep(2000);

					updateJSONObj();
					state = ElevatorState.DOWN;
					sendStateUpdate();
				}
				state = ElevatorState.DOOR_OPEN;
				checkDoorFault();
				state = ElevatorState.DOOR_CLOSED;
				state = ElevatorState.IDLE;
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	private void receiveAndRespond() {

		String txt;
		while (true) {
			try {

				byte data[] = new byte[100];
				receivePacket = new DatagramPacket(data, data.length);
				System.out.println("Elevator: Currently idle. Waiting for request \n ");
				receiveSocket.receive(receivePacket);
				txt = new String(data, 0, receivePacket.getLength());
				JSONObject obj = new JSONObject(txt);
				System.out.println("Server: Request received: ");
				System.out.println("Contents (String): " + obj.toString());
				System.out.println("Contents (Bytes): " + receivePacket.getData() + "\n");
				JSONObject ack = new JSONObject();
				ack.put("message", "ACK");
				byte[] data1 = ack.toString().getBytes();
				DatagramPacket ackPacket = new DatagramPacket(data1, data1.length, schedulerAddress, ackPort);
				System.out.println("Elevator: sending ack to scheduler...");
				System.out.println("Contents(String) " + ack.toString());
				ackSocket.send(ackPacket);
				System.out.println("Elevator: ACK sent\n");
				moveElevator(obj);

			} catch (Exception e1) {
				e1.printStackTrace();
				sendSocket.close();
				receiveSocket.close();
				System.exit(1);
			}
		}
	}

	private void receiveACK() {
		byte replyData[] = new byte[100];
		ackPacket = new DatagramPacket(replyData, replyData.length);
		try {
			ackSocket.receive(ackPacket);
			String txt = new String(replyData, 0, ackPacket.getLength());
			JSONObject ack = new JSONObject(txt);
			System.out.println("elevator: ACK received from scheduler");
		} catch (IOException e) {
			e.printStackTrace();
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		try {
			(new Elevator(1, InetAddress.getLocalHost())).receiveAndRespond();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
	}

	public int getCurrentFloor() {
		return currFloor;
	}

	public void checkDoorFaultTest(int x) {

		if (x >= 6) {
			state = ElevatorState.FIXING_DOORS;
			System.out.println("Door is jamed. Please stand by while fixing ....");
			fault = true;

			try {
				Thread.sleep(3000);.
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			System.out.println("Fixed!");
			return;
		}

		fault = false;
	}

	public void goToDestinationTest(JSONObject obj) {
		int destinationFloor;
		try {
			timer = timer_time;
			destinationFloor = obj.getInt("destinationFloor");
			int goToDestination = currFloor - destinationFloor;
			if (goToDestination < 0) { 
				for (int i = currFloor; i < destinationFloor; i++) {
					updateTimer();
					System.out.println("Elevator: moving to floor " + ++currFloor);
					
					Thread.sleep(500);
				}
			} else {
				for (int i = currFloor; i > destinationFloor; i--) {
					updateTimer();
					System.out.println("Elevator: moving to floor " + --currFloor);
					Thread.sleep(500);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();

		}
	}

	public void setFaultFlag(boolean x) {
		this.fault = x;
	}

	public boolean getFaultFlag() {
		return this.fault;
	}

	public boolean getTFaultFlag() {
		return tFault;
	}
}
