package problem;

import java.util.ArrayList;

public class Resource implements Comparable<Resource>{
	
	private int resourceID;
	private int[] startShift;
	private int[] endShift;
	private int[] availability;
	private int shift;
	
	public Resource(int resourceID, int[] startShift, int[] endShift, int[] availability, int shift) {
		this.resourceID = resourceID;
		this.startShift = startShift;
		this.endShift = endShift;
		this.availability = availability;
		this.shift = shift;
	}
	
	public Resource(int resourceID, ArrayList<Integer> startShift, ArrayList<Integer> endShift, ArrayList<Integer> availability, int shift) {
		this.resourceID = resourceID;
		
		startShift.trimToSize();
		endShift.trimToSize();
		availability.trimToSize();
		this.startShift = new int[startShift.size()];
		this.endShift = new int[endShift.size()];
		this.availability = new int[availability.size()];
		this.shift = shift;
		
		for(int i = 0; i < startShift.size(); i++) {
			this.startShift[i] = startShift.get(i); 
			this.endShift[i] = endShift.get(i);
			this.availability[i] = availability.get(i);	
		}
	}

	public int getResourceID() {
		return resourceID;
	}

	public int[] getStartShift() {
		return startShift;
	}

	public int[] getEndShift() {
		return endShift;
	}
	
	public int getShift() {
		return shift;
	}

	public int[] getAvailability() {
		return availability;
	}
	
	public int getTotalAvailability() {
		int sum = 0;
		for(int i = 0; i < availability.length; i++) sum += availability[i];
		return sum;
	}
	
	public void printResource() {
		for(int i = 0; i < startShift.length; i++)
			System.out.println(resourceID + "\t" + startShift[i] + "\t" + endShift[i] + "\t" + availability[i]);
	}

	@Override
	public int compareTo(Resource o) {
		// TODO Auto-generated method stub
		return 0;
	}
	
	public boolean equals(Object r) {
		return (this.resourceID == ((Resource) r).getResourceID());
	}

}
