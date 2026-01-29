package solver;

import java.util.ArrayList;

import problem.Instance;

public class Route {
	
	private int totalDistance;
	private int totalTime;
	private int workingTime;
	private int travelTime;
	private int waitingTime;
	private int availableTime;
	private int capacityTime;
	
	private int lastEndTime;
	
	private ArrayList<Integer> routeTasks;
	private ArrayList<Integer> startTimes;
	private ArrayList<Integer> endTimes;
	
	private boolean collaborative;
	
	public Route(int capacityTime) {
		this.totalDistance = 0;
		this.totalTime = 0;
		this.workingTime = 0;
		this.travelTime = 0;
		this.waitingTime = 0;
		this.capacityTime = capacityTime;
		this.availableTime = capacityTime - this.totalTime;
		
		this.lastEndTime = 0;
			
		this.routeTasks = new ArrayList<Integer>();
		this.startTimes = new ArrayList<Integer>();
		this.endTimes = new ArrayList<Integer>();
		
		this.collaborative = false;
	}
	
	
	public int getTotalDistance() {
		return totalDistance;
	}


	public int getTotalTime() {
		return totalTime;
	}


	public int getWorkingTime() {
		return workingTime;
	}


	public int getTravelTime() {
		return travelTime;
	}


	public int getWaitingTime() {
		return waitingTime;
	}


	public int getAvailableTime() {
		return availableTime;
	}


	public int getCapacityTime() {
		return capacityTime;
	}
	
	
	public int getLastEndTime() {
		return lastEndTime;
	}


	public ArrayList<Integer> getRouteTasks() {
		return routeTasks;
	}


	public ArrayList<Integer> getStartTimes() {
		return startTimes;
	}


	public ArrayList<Integer> getEndTimes() {
		return endTimes;
	}
	
	public int getTaskAtTime(int time) {
		int task = routeTasks.get(0);
		
		for(int i = 0; i < startTimes.size(); i++) {
			if(time >= startTimes.get(i) && time <= endTimes.get(i)) {
				task = routeTasks.get(i);
				break;
			} else if(time >= endTimes.get(endTimes.size()-1)){
				task = routeTasks.get(endTimes.size()-1);
				break;
			} else if (time >= endTimes.get(i) && time <= startTimes.get(i+1)) {
				task = routeTasks.get(i);
				break;
			} else if(time <= startTimes.get(0)) {
				task = routeTasks.get(0);
				break;
			}
		}
		
		return task;
	}
	
	public void setValuesToPosition(int position, Instance instance) {
		lastEndTime = endTimes.get(position);
		availableTime = Math.max(capacityTime - lastEndTime,0);
		totalTime = lastEndTime;
		workingTime = 0;
		travelTime = 0;
		totalDistance = 0;
		for(int i = 0; i < position; i++) {
			if(instance.getTasks().containsKey(routeTasks.get(i)))
				workingTime += instance.getTask(routeTasks.get(i)).getDuration();
			else
				workingTime += instance.getCallback(routeTasks.get(i)).getDuration();
		}
		for(int i = 1; i < routeTasks.size(); i++) {
			if(instance.getTasks().containsKey(routeTasks.get(i-1))) {
				if(instance.getTasks().containsKey(routeTasks.get(i))) {
					totalDistance += instance.getDistance(instance.getTask(routeTasks.get(i-1)).getLocation(), instance.getTask(routeTasks.get(i)).getLocation());
					travelTime += instance.getTravTime(instance.getTask(routeTasks.get(i-1)).getLocation(), instance.getTask(routeTasks.get(i)).getLocation());
				} else {
					totalDistance += instance.getDistance(instance.getTask(routeTasks.get(i-1)).getLocation(), instance.getCallback(routeTasks.get(i)).getLocation());
					travelTime += instance.getTravTime(instance.getTask(routeTasks.get(i-1)).getLocation(), instance.getCallback(routeTasks.get(i)).getLocation());
				}
			} else {
				if(instance.getTasks().containsKey(routeTasks.get(i))) {
					totalDistance += instance.getDistance(instance.getCallback(routeTasks.get(i-1)).getLocation(), instance.getTask(routeTasks.get(i)).getLocation());
					travelTime += instance.getTravTime(instance.getCallback(routeTasks.get(i-1)).getLocation(), instance.getTask(routeTasks.get(i)).getLocation());
				} else {
					totalDistance += instance.getDistance(instance.getCallback(routeTasks.get(i-1)).getLocation(), instance.getCallback(routeTasks.get(i)).getLocation());
					travelTime += instance.getTravTime(instance.getCallback(routeTasks.get(i-1)).getLocation(), instance.getCallback(routeTasks.get(i)).getLocation());
				}
			}
			
		}
		waitingTime = totalTime - workingTime - travelTime;
	}
	
	
	public void setTotalDistance(int totalDistance) {
		this.totalDistance = totalDistance;
	}


	public void setTotalTime(int totalTime) {
		this.totalTime = totalTime;
	}


	public void setWorkingTime(int workingTime) {
		this.workingTime = workingTime;
	}


	public void setTravelTime(int travelTime) {
		this.travelTime = travelTime;
	}


	public void setWaitingTime(int waitingTime) {
		this.waitingTime = waitingTime;
	}


	public void setAvailableTime(int availableTime) {
		this.availableTime = availableTime;
	}


	public void setCapacityTime(int capacityTime) {
		this.capacityTime = capacityTime;
	}

	public void setRouteTasks(ArrayList<Integer> newRoute) {
		this.routeTasks = newRoute;
	}

	public void setStartTimes(ArrayList<Integer> startTimes) {
		this.startTimes = startTimes;
	}


	public void setEndTimes(ArrayList<Integer> endTimes) {
		this.endTimes = endTimes;
	}
	
	public void setCollaborative(boolean collaborative) {
		this.collaborative = collaborative;
	}


	public int getRouteTask(int position) {
		return this.routeTasks.get(position);
	}

	public void addTask(int task, int distance, int travTime, int waitTime, int workTime) {
		routeTasks.add(task);
		startTimes.add(lastEndTime + travTime + waitTime);
		endTimes.add(lastEndTime + travTime + waitTime + workTime);
		updateValues(distance, travTime, waitTime, workTime);
	}
	
	public void addStartTime(int time) {
		this.startTimes.add(time);
	}
	
	public void addEndTime(int time) {
		this.endTimes.add(time);
	}

	
	private void updateValues(int distance, int travTime, int waitTime, int workTime) {
		updateDistance(distance);
		updateTravelTime(travTime);
		updateWaitingTime(waitTime);
		updateWorkingTime(workTime);
		updateTotalTime();
		updateAvailableTime();
		
		updateLastEndTime(travTime,waitTime,workTime);
	}
	
	
	public void updateDistance(int distance) {
		this.totalDistance += distance;
	}
	
	public void updateTravelTime(int time) {
		this.travelTime += time;
	}
	
	public void updateWaitingTime(int time) {
		this.waitingTime += time;
	}
		
	public void updateWorkingTime(int time) {
		this.workingTime += time;
	}
	
	public void updateTotalTime() {
		this.totalTime = travelTime + workingTime + waitingTime;
	}
	
	public void updateAvailableTime() {
		this.availableTime = capacityTime - totalTime;
	}
	
	
	private void updateLastEndTime(int travTime, int waitTime, int workTime) {
		this.lastEndTime += travTime + waitTime + workTime;
	}
	
	public boolean isCollaborative() {
		return collaborative;
	}
		
	public boolean isRelated(Instance instance, Route route) {
		boolean isRelated = false;
		
		for(Integer task : routeTasks) {
			if(instance.getCollaborations().containsKey(task)) {
				Integer supportTask = instance.getCollaborations().get(task);
				if(route.getRouteTasks().contains(supportTask)) {
					isRelated = true;
					break;
				}
			}
		}
					
		return isRelated;
	}
	
	public void clear() {
		this.totalDistance = 0;
		this.totalTime = 0;
		this.workingTime = 0;
		this.travelTime = 0;
		this.waitingTime = 0;
		this.availableTime = capacityTime - this.totalTime;
		
		this.lastEndTime = 0;
			
		this.routeTasks.clear();
		this.startTimes.clear();;
		this.endTimes.clear();;
		
		this.collaborative = false;
	}
	
	public String toString() {
		String route = "[";
		for(int i = 0; i < routeTasks.size() - 1; i++)
			route += routeTasks.get(i) + ", ";
		route += routeTasks.get(routeTasks.size() - 1) + "]";
		return route;
	}
	
	public String printRoute() {
		return travelTime + "\t" + waitingTime + "\t" + workingTime + "\t" + totalTime + "\t" + toString();
	}
}
