package problem;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Scanner;

public class Instance {
	private String instanceName;
	
	private int locations = 251;
	
	private int[][] distMatrix;
	private int[][] timeMatrix;
	
	private ArrayList<Resource> resources;
	private int mandays = 0;
	private int shiftDuration = 0;
	
	private Hashtable<Integer,Task> tasks;
	private Hashtable<Integer,Task> callbacks;
	private Hashtable<Integer, Integer> collaborations;
	
	public Instance(String instanceName) {
		this.instanceName = instanceName;
		this.distMatrix = new int[locations][locations];
		this.timeMatrix = new int[locations][locations];
		
		this.resources = new ArrayList<Resource>();
		this.tasks = new Hashtable<Integer, Task>();
		this.callbacks = new Hashtable<Integer, Task>();
		this.collaborations = new Hashtable<Integer, Integer>();
		
		readInstance();
	}
	
	
	public String getInstanceName() {
		return instanceName;
	}


	public int getLocations() {
		return locations;
	}


	public int[][] getDistMatrix() {
		return distMatrix;
	}
	
	public int[] getRowDistMatrix(int i) {
		return distMatrix[i];
	}
	
	public int getDistance(int i, int j) {
		return distMatrix[i][j];
	}

	
	public int[][] getTimeMatrix() {
		return timeMatrix;
	}

	public int[] getRowTimeMatrix(int i) {
		return timeMatrix[i];
	}
	
	public int[] getColTimeMatrix(int j) {
		int[] column = new int[locations];
		for(int i = 0; i < locations; i++)
			column[i] = timeMatrix[i][j]; 
		return column;
	}
	
	public int getTravTime(int i, int j) {
		return timeMatrix[i][j];
	}
	

	public ArrayList<Resource> getResources() {
		return resources;
	}
	
	
	public Hashtable<Integer, ArrayList<Integer>> getResourcesByShift() {
		Hashtable<Integer,ArrayList<Integer>> resourcesByShift = new Hashtable<Integer, ArrayList<Integer>>();
		
		for(Resource resource : resources) {
			if(!resourcesByShift.containsKey(resource.getShift())) {
				resourcesByShift.put(resource.getShift(), new ArrayList<Integer>());
				resourcesByShift.get(resource.getShift()).add(resource.getResourceID());
			} else {
				resourcesByShift.get(resource.getShift()).add(resource.getResourceID());
			}
		}
		
		return resourcesByShift;
	}


	public int getMandays() {
		return mandays;
	}
	
	public int getShiftDuration() {
		return shiftDuration;
	}


	public Hashtable<Integer, Task> getTasks() {
		return tasks;
	}
	
	public Task getTask(int i) {
		return tasks.get(i);
	}


	public Hashtable<Integer, Task> getCallbacks() {
		return callbacks;
	}
	
	public Task getCallback(int i) {
		return callbacks.get(i);
	}


	public Hashtable<Integer, Integer> getCollaborations() {
		return collaborations;
	}
	
	public int getCollabPair(int i) {
		return collaborations.get(i);
	}
	
	public boolean isCollaboration(int i) {
		return collaborations.containsKey(i);
	}


	private void readInstance() {
		readCostMatrix();
		
		try {
			FileInputStream file = new FileInputStream(instanceName);
			Scanner in = new Scanner(file);
			
			readResources(in);
			readTasks(in);
			readCallbacks(in);
			readCollaborations(in);
			
			in.close();
			file.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void readCostMatrix() {
		try {
			FileInputStream file = new FileInputStream("instances/FSE_costmat.txt");
			Scanner in = new Scanner(file);
			
			while(in.hasNextLine()) {
				String[] line = in.nextLine().split("\t");
				
				int i = Integer.valueOf(line[0].trim()).intValue() - 1;
				int j = Integer.valueOf(line[1].trim()).intValue() - 1;
				int dist = Integer.valueOf(line[2].trim()).intValue();
				int time = Integer.valueOf(line[3].trim()).intValue();
				
				distMatrix[i][j] = dist;
				timeMatrix[i][j] = time;
			}
			
			in.close();
			file.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void readResources(Scanner in) {
		int resID = 0;
		int timeRef = 0;
		int shift = 1;
		int shiftRefTime = 0;
		ArrayList<Integer> startTimes = new ArrayList<Integer>();
		ArrayList<Integer> endTimes = new ArrayList<Integer>();;
		ArrayList<Integer> availability = new ArrayList<Integer>();;
		
		in.nextLine();
		in.nextLine();
		
		while(in.hasNextLine()) {
			String line = in.nextLine();
			
			if(!line.isEmpty()) {
				String[] fields = line.split("\t");
				int fieldID = Integer.valueOf(fields[0]).intValue();
				int fieldStart = Integer.valueOf(fields[1]).intValue()*60;
				int fieldEnd = Integer.valueOf(fields[2]).intValue()*60;
				int fieldAvail = Integer.valueOf(fields[3]).intValue()*60;
				
				mandays++;
				
				if(fieldID == resID) {
					timeRef++;
					if(timeRef % 5 == 0) timeRef += 2;
					startTimes.add(fieldStart + timeRef * 1440);
					endTimes.add(fieldEnd + timeRef * 1440);
					availability.add(fieldAvail);
				} else {
					if(!startTimes.isEmpty()) {
						resources.add(new Resource(resID, startTimes, endTimes, availability, shift));
						startTimes.clear();
						endTimes.clear();
						availability.clear();
						
						timeRef = 0;
						resID = fieldID;
						startTimes.add(fieldStart);
						endTimes.add(fieldEnd);
						availability.add(fieldAvail);
						if(fieldStart != shiftRefTime) {
							shiftRefTime = fieldStart;
							shift++;
						}
					} else {
						timeRef = 0;
						resID = fieldID;
						startTimes.add(fieldStart);
						endTimes.add(fieldEnd);
						availability.add(fieldAvail);
						shiftDuration = fieldAvail;
						shiftRefTime = fieldStart;
					}
				}
			} else break;
		}
		resources.add(new Resource(resID, startTimes, endTimes, availability, shift));
	}
	
	private void readTasks(Scanner in) {
		in.nextLine();
		in.nextLine();
		
		while(in.hasNextLine()) {
			String line = in.nextLine();
			
			if(!line.isEmpty()) {
				String[] fields = line.split("\t");
				int fieldID = Integer.valueOf(fields[0]).intValue();
				int fieldLocation = Integer.valueOf(fields[1]).intValue();
				int fieldPriority = Integer.valueOf(fields[2]).intValue();
				int fieldDuration = Integer.valueOf(fields[3]).intValue();
				
				tasks.put(fieldID, new Task(fieldID, fieldLocation - 1, fieldDuration, fieldPriority));
			} else break;
		}
	}
	
	private void readCallbacks(Scanner in) {
		in.nextLine();
		in.nextLine();
		
		while(in.hasNextLine()) {
			String line = in.nextLine();
			
			if(!line.isEmpty()) {
				String[] fields = line.split("\t");
				int fieldID = Integer.valueOf(fields[0]).intValue();
				int fieldLocation = Integer.valueOf(fields[1]).intValue();
				int fieldTime = Integer.valueOf(fields[2]).intValue();
				int fieldPriority = Integer.valueOf(fields[3]).intValue();
				int fieldDuration = Integer.valueOf(fields[4]).intValue();
				
				callbacks.put(fieldID, new Task(fieldID, fieldLocation - 1, fieldDuration, fieldPriority, fieldTime));
			} else break;
		}
	}
	
	private void readCollaborations(Scanner in) {
		in.nextLine();
		
		while(in.hasNextLine()) {
			String line = in.nextLine();
			if(!line.isEmpty()) {
				String[] fields = line.split("\t");
				int task1 = Integer.valueOf(fields[0]).intValue();
				int task2 = Integer.valueOf(fields[1]).intValue();
				
				collaborations.put(task1, task2);
				collaborations.put(task2,  task1);
			} else break;
		}
	}
}
