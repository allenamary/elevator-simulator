package allena.elevatorsimulator;

import java.sql.Time;

import org.json.*;

public class ControlDate {

	private Time time;

	private int floor;

	private boolean floorButton;

	private int destinationFloor;

	public ControlDate(Time time, int floor, boolean floorButton, int destinationFloor) {
		this.time=time;
		this.floor=floor;
		this.floorButton=floorButton;
		this.destinationFloor=destinationFloor;
	}
	
	public String toString() {
		JSONObject controlDate = new JSONObject();
		try {
			controlDate.put("time", time);
			controlDate.put("floor", floor);
			controlDate.put("floorButton", floorButton);
			controlDate.put("destinationFloor", destinationFloor);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return controlDate.toString(); 
	}
	
	public byte[] getByteArray() {
		return this.toString().getBytes();
	}

	public Time getTime() {
		return time;
	}

	public int getFloor() {
		return floor;
	}

	public boolean getFloorButton() {
		return floorButton;
	}

	public int getDestinationFloor() {
		return destinationFloor;
	}
	
	public boolean equals(Object o) {
		if (o == this) { 
            return true; 
        } 
        if (!(o instanceof ControlDate)) { 
            return false; 
        } 
           
        ControlDate c = (ControlDate) o; 
          
        return (this.getTime()).equals(c.getTime())
                && Integer.compare(this.floor, c.floor) == 0
                && this.floorButton == c.floorButton
                && Integer.compare(this.destinationFloor, c.destinationFloor) == 0; 
	}
	
}
