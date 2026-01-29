package solver;

import java.util.ArrayList;
import java.util.Hashtable;

public class Solution {

	private int distanceCost;
	private int timeCost;
	private int waitingCost;
	private int totalCost;
	private int numberRoutes;
	
	private ArrayList<Route> routes;
	private int[][] resourceDayRoute;
	Hashtable<Integer, ArrayList<Integer>> routesByDay;
	
	public Solution() {
		this.distanceCost = 0;
		this.timeCost = 0;
		this.waitingCost = 0;
		this.totalCost = 0;
		this.numberRoutes = 0;
		this.routes = new ArrayList<Route>();
		routesByDay = new Hashtable<Integer, ArrayList<Integer>>();
	}
	
	public Solution(int maxRoutes, int capacityTime) {
		this.distanceCost = 0;
		this.timeCost = 0;
		this.waitingCost = 0;
		this.totalCost = 0;
		this.numberRoutes = 0;
		this.routes = new ArrayList<Route>(maxRoutes);
		for(Route route : routes) route.setCapacityTime(capacityTime);
		routesByDay = new Hashtable<Integer, ArrayList<Integer>>();
	}

	public int getDistanceCost() {
		calcDistanceCost();
		return distanceCost;
	}

	public int getTimeCost() {
		calcTimeCost();
		return timeCost;
	}

	public int getWaitingCost() {
		calcWaitingCost();
		return waitingCost;
	}
	
	public int getWorkingTime() {
		int sum = 0;
		for(Route route : routes) {
			sum += route.getWorkingTime();
		}
		return sum;
	}

	public int getTotalCost() {
		calcTotalCost();
		return totalCost;
	}
	
	public int getNumberRoutes() {
		calcNumberRoutes();
		return numberRoutes;
	}

	public ArrayList<Route> getRoutes() {
		return routes;
	}
	
	public int[][] getResourceDayRoute() {
		return resourceDayRoute;
	}
	
	public Hashtable<Integer, ArrayList<Integer>> getRoutesByDay() {
		return routesByDay;
	}
	
	public ArrayList<Integer> getRoutesByDay(int i) {
		return routesByDay.get(i);
	}

	public void setDistanceCost(int distanceCost) {
		this.distanceCost = distanceCost;
	}

	public void setTimeCost(int timeCost) {
		this.timeCost = timeCost;
	}

	public void setWaitingCost(int waitingCost) {
		this.waitingCost = waitingCost;
	}

	public void setTotalCost(int totalCost) {
		this.totalCost = totalCost;
	}

	public void setRoutes(ArrayList<Route> routes) {
		this.routes = routes;
	}
	
	public void setResourceDayRoute(int[][] resourceDayRoute) {
		this.resourceDayRoute = resourceDayRoute;
	}
	
	public void setRoutesByDay(Hashtable<Integer, ArrayList<Integer>> routesByDay) {
		this.routesByDay = routesByDay;
	}
	
	private void calcDistanceCost() {
		int sumDistance = 0;
		for(int i = 0; i < routes.size(); i++)
			sumDistance += routes.get(i).getTotalDistance();
		this.distanceCost = sumDistance;
	}
	
	private void calcTimeCost() {
		int sumTime = 0;
		for(int i = 0; i < routes.size(); i++)
			sumTime += routes.get(i).getTravelTime();
		this.timeCost = sumTime;
	}
	
	private void calcWaitingCost() {
		int sumWaiting = 0;
		for(int i = 0; i < routes.size(); i++)
			sumWaiting += routes.get(i).getWaitingTime();
		this.waitingCost = sumWaiting;
	}
	
	private void calcTotalCost() {
		this.totalCost = getTimeCost() + getWaitingCost();
	}
	
	private void calcNumberRoutes() {
		int numRoutes = 0;
		for(Route route : routes) 
			if(!route.getRouteTasks().isEmpty())
				numRoutes++;
		this.numberRoutes = numRoutes;
	}
	
	public void printSolution() {
		System.out.println("Route\tTrav\tWait\tWork\tTotal\tTasks\n-----\t----\t----\t----\t-----\t-----");
		for(int i = 0; i < routes.size(); i++)
			System.out.print(i + "\t" + routes.get(i).printRoute() + "\n");
		System.out.println("\n=================\n");
		System.out.println("Working Time:\t" + getWorkingTime());
		System.out.println("Travel Time:\t" + getTimeCost());
		System.out.println("Waiting Time:\t" + getWaitingCost());
		System.out.println("Total Cost:\t" + getTotalCost());
		System.out.println("Routes:\t\t" + getNumberRoutes());
	}
}
