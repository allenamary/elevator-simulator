package allena.elevatorsimulator;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.sql.Time;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.Queue;

/**
 * this class reads the data from the file and puts in a queue. It sends each
 * request to the Scheduler and waits for an ACK before sending another packet.
 * this class store read in event Time, floor or elevator number, and button
 * into a list of ControlData structure.
 * 
 * @author Mariam Almalki, Ruqaya Almalki, Zewen Chen
 *
 */
public class Floor {

	private DatagramSocket sendSocket, receiveSocket;
	
    private DatagramPacket sendPacket, receivePacket;
	
    private static int schedulerPort = 23;
	
    private InetAddress schedulerAddress;

	private int i = 0;

	File file;
	
    private Date date;
	
    private Time time;
	
    private int floor;
	
    private boolean floorButton; 
	
    private int destinationFloor;
	
    private ArrayList<ControlDate> datas;
	
    private static Queue<ControlDate> requestQueue;
	
    private SimpleDateFormat sdf;

	public Floor(InetAddress addr) {
		try {
			schedulerAddress = addr;
			this.file = new File("data.txt");
			this.datas = new ArrayList<ControlDate>();
			this.requestQueue = new LinkedList<>();
			sdf = new SimpleDateFormat("hh:mm:ss.S");
			getDataFromFile();
			sendSocket = new DatagramSocket();
			receiveSocket = new DatagramSocket(1000);
		} catch (SocketException se) {
			se.printStackTrace();
			System.exit(1);
		}
	}

	private void getDataFromFile() {
		try {
			BufferedReader br = new BufferedReader(new FileReader(file));
			String str;
			while ((str = br.readLine()) != null) {
				String[] x = str.split(" ");
				for (int i = 0; i < x.length; i++) {
					if (i == 0) {
						date = sdf.parse(x[i]);
						time = new Time(date.getTime());
					}
					if (i == 1) {
						floor = Integer.parseInt(x[i]);
					}
					if (i == 2) {
						if (x[i].equals("Up")) {
							floorButton = true;
						}
						if (x[i].equals("Down")) {
							floorButton = false;
						}
					}
					if (i == 3) {
						
						destinationFloor = Integer.parseInt(x[i]);
					}
				}
				datas.add(new ControlDate(time, floor, floorButton, destinationFloor));
				requestQueue.add(new ControlDate(time, floor, floorButton, destinationFloor));
			}
			br.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		}
	}

	public void sendAndReceive() {
		byte msg[] = null;
		String msgString = "";
		while (true) {
			if (!requestQueue.isEmpty()) {
				ControlDate c = requestQueue.remove();
				msgString = c.toString();
				msg = c.getByteArray();
			} else {
				System.exit(0);
			}
			try {
				sendPacket = new DatagramPacket(msg, msg.length, schedulerAddress, schedulerPort);
				System.out.println("Floor: request count " + i++);
				System.out.println("Contents (String): " + msgString);
				System.out.println("Contents (Bytes): " + sendPacket.getData());
				sendSocket.send(sendPacket);
				System.out.println("Floor: Packet sent.\n");
				try {
					Thread.sleep(2000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				byte data[] = new byte[100];
				receivePacket = new DatagramPacket(data, data.length);
				System.out.println("Floor: Waiting for ACK from host... ");
				receiveSocket.receive(receivePacket);
				System.out.println("Floor: Reply received from host");
				System.out.println("Contents [String]: " + data.toString());
				System.out.println("Contents [Bytes]: " + receivePacket.getData() + "\n");
			} catch (UnknownHostException e) {
				e.printStackTrace();
			} catch (IOException e) {
				System.out.println("\nsocket timedout :/");
				receiveSocket.close();
				sendSocket.close();
				System.exit(1);
			}
		}
	}

	public ControlDate getdata(int i) {
		return this.datas.get(i);
	}

	public static void main(String[] args) {
		try {
			(new Floor(InetAddress.getLocalHost())).sendAndReceive();;
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
	}
}
