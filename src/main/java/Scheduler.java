package allena.elevatorsimulator;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;

import org.json.JSONException;
import org.json.JSONObject;

import allena.elevatorsimulator.Elevator.ElevatorState;

public class Scheduler {

	public enum States {
		UPDATING
	}
	
	public JSONObject currReq;

	private HashMap<Integer, JSONObject> elevators;
	
    private InetAddress floorAddress;
	
    private DatagramSocket floorSocket, ackSocket, updateElevatorSocket;
	
    private static int floorPort = 1000, serverPort = 69, elevatorACKport = 1040;

	private Queue<JSONObject> requestQueue;

	private static States state = States.UPDATING; 

	ControlDate c;

	public Scheduler(InetAddress floorAddress) {
		requestQueue = new LinkedList<>();
		elevators = new HashMap<>();
		try {
			requestQueue = new LinkedList<>();
			elevators = new HashMap<>();
			this.floorAddress = floorAddress;
			floorSocket = new DatagramSocket(23);
			updateElevatorSocket = new DatagramSocket(1026);
			ackSocket = new DatagramSocket();
		} catch (SocketException e) {
			e.printStackTrace();
		}
		addSubscribers();
		receiveFromFloor();
		scheduleAndSendCmd();
		updateElevatorStatus();
	}
	
	public int getNumRequests() {
		return requestQueue.size();
	}

	private void receiveFromFloor() {
		Runnable receive = new Runnable() {

			@Override
			public void run() {

				while (true) {
					try {
						byte[] data = new byte[100]; 
						DatagramPacket receivePacket = new DatagramPacket(data, data.length); 
						String txt;
						floorSocket.receive(receivePacket);
						txt = new String(data, 0, receivePacket.getLength());
						JSONObject obj2 = new JSONObject(txt);
						currReq = obj2;
						synchronized (requestQueue) {
							requestQueue.add(obj2);
						}
						System.out.println("Queue " + requestQueue.size());
						System.out.println("Scheduler: Packet received from floor");
						System.out.println("Contents (String): " + txt);
						System.out.println("Contents (Bytes): " + receivePacket.getData() + "\n");
						sendACK(floorPort, "floor", floorAddress);

					} catch (IOException e) {
						System.out.println("socket timeout :/");
						
						floorSocket.close();
						System.exit(1);
					} catch (JSONException e) {
						e.printStackTrace();
					}
				}
			}
		};
		Thread rThread = new Thread(receive);
		rThread.start();
	}

	private void addSubscribers() {

		Runnable getSub = new Runnable() {
			byte[] arr = new byte[100];
			DatagramPacket newElevPacket;
			DatagramSocket getSubSocket;
			@Override
			public void run() {
				while (true) {
					newElevPacket = new DatagramPacket(arr, arr.length);
					try {
						getSubSocket = new DatagramSocket(1035);
						
						getSubSocket.receive(newElevPacket);
						String elev = new String(arr, 0, newElevPacket.getLength());
						System.out.println("This elevator is subscribing to the scheduler: " + elev);
						JSONObject newElev = new JSONObject(elev);
						synchronized (elevators) {
							elevators.put(newElev.getInt("id"), newElev);
						}
						getSubSocket.close();
					} catch (IOException e) {
						e.printStackTrace();
					} catch (JSONException e) {
						e.printStackTrace();
					}

				}
			}
		};
		Thread sThread = new Thread(getSub);
		sThread.start();
	}
	private synchronized void updateElevatorStatus() {
		Runnable updateStatus = new Runnable() {

			@Override
			public void run() {
				while (true) {
					try {
						byte[] data = new byte[100]; 
						DatagramPacket receivePacket = new DatagramPacket(data, data.length); 
						String txt;
						updateElevatorSocket.receive(receivePacket);
						txt = new String(data, 0, receivePacket.getLength());
						JSONObject obj2 = new JSONObject(txt);
						System.out.println("receieved from elevator: " + obj2.toString());
						int elevId = obj2.getInt("id");

						synchronized (elevators) {
							elevators.put(elevId, obj2);
						}
						System.out.println("Scheduler: Status Update received");
						System.out.println("Contents (String): " + txt);
						System.out.println("Contents (Bytes): " + receivePacket.getData() + "\n");

					} catch (IOException e) {
						System.out.println("socket timeout :/");
						e.printStackTrace();
					} catch (JSONException e) {
						
						e.printStackTrace();
					}
				}
			}
		};
		Thread uThread = new Thread(updateStatus);
		uThread.start();
	}

	private synchronized void scheduleAndSendCmd() {

		
		Runnable sAndS = new Runnable() {
			@Override
			public void run() {
				DatagramPacket cmdPacket;
				DatagramSocket sendCmdSocket;
				while (true) {
					int elevToSchedule = 1; 
					int minDistance = 1000; 
					try {
						sendCmdSocket = new DatagramSocket();
						synchronized (requestQueue) {
							synchronized (elevators) {
								if (!requestQueue.isEmpty() && !elevators.isEmpty()) {
									JSONObject firstReq = requestQueue.remove();
									int currFloor = firstReq.getInt("floor");
									int destFloor = firstReq.getInt("destinationFloor");
									ElevatorState direction;
									if((currFloor - destFloor) < 0 ) {
										direction = ElevatorState.UP;
									}else {
										direction = ElevatorState.DOWN;
									}
									for (int elev : elevators.keySet()) {
										int distance = Math.abs(currFloor - (elevators.get(elev)).getInt("currFloor"));
									    boolean checkState = ((elevators.get(elev)).get("State") == direction); 
										if (distance < minDistance && checkState) {
											elevToSchedule = elev;
											minDistance = distance;
										}
									}
									byte[] cmd = firstReq.toString().getBytes();
									String inetAdd = (String) elevators.get(elevToSchedule).get("InetAddress");
									cmdPacket = new DatagramPacket(cmd, cmd.length, InetAddress.getByName(inetAdd),
											serverPort);
									sendCmdSocket.send(cmdPacket);
									sendACK(elevatorACKport, "elevator", InetAddress.getByName(inetAdd));
								}
							}
						}
						sendCmdSocket.close();
					} catch (JSONException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}

		};
		Thread thread = new Thread(sAndS);
		thread.start();
	}
	private void sendACK(int port, String sendToSource, InetAddress sourceAddress) {
		try {
			JSONObject ack = new JSONObject();
			ack.put("message", "ACK");
			byte[] data = ack.toString().getBytes();
			DatagramPacket ackPacket = new DatagramPacket(data, data.length, sourceAddress, port);
			System.out.println("Scheduler: Sending ACK to " + sendToSource + "...");
			System.out.println("Contents(String) " + ack.toString());
			ackSocket.send(ackPacket);
			System.out.println("Scheduler: ACK sent\n");
		} catch (JSONException e1) {
			e1.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public int getNumElevators() {
		return elevators.size();
	}
	
	public JSONObject getElevatorInfo(int elevID){
		return elevators.get(elevID);
	}

	public static void main(String[] args) {
		try {
			new Scheduler(InetAddress.getLocalHost());
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}

	}

}
