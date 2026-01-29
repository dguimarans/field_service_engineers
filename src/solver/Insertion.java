package solver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;

import problem.*;

public class Insertion {
	private Instance instance;
	private int numLocations;

	private int[][] nearestDist;
	private int[][] nearestTimeFrom;
	private int[][] nearestTimeTo;

	private Hashtable<Integer, ArrayList<Integer>> tasksByLocation;
	private ArrayList<Integer> tasksByDuration;
	private int[] locationsByDemand;

	public Insertion(Instance instance) {
		this.instance = instance;
		this.numLocations = instance.getLocations();

		this.nearestDist = new int[numLocations][numLocations - 1];
		this.nearestTimeFrom = new int[numLocations][numLocations - 1];
		this.nearestTimeTo = new int[numLocations][numLocations - 1];
		this.tasksByLocation = new Hashtable<Integer, ArrayList<Integer>>(numLocations);
		this.tasksByDuration = new ArrayList<Integer>();
		this.locationsByDemand = new int[numLocations];

		init();
	}

	public Solution solveByDuration() {
		Solution insertionSolution = new Solution(instance.getMandays(), instance.getShiftDuration());

		ArrayList<Integer> tasksToSchedule = new ArrayList<Integer>(tasksByDuration);

		int usedRoutes = 0;
		int currentPosition = 0;

		Route route = new Route(instance.getShiftDuration());

		while (!tasksToSchedule.isEmpty() && usedRoutes <= instance.getMandays()) {

			if (route.getRouteTasks().isEmpty()) {
				usedRoutes++;
				int task = tasksToSchedule.get(0);
				if (instance.getTask(task).getDuration() <= route.getAvailableTime()) {
					route.addTask(task, 0, 0, 0, instance.getTask(task).getDuration());

					currentPosition = instance.getTask(task).getLocation();

					tasksToSchedule.remove(0);
				} else {
					System.out.println("Error: Task " + task + " duration is longer than scheduled shifts.");
				}
			} else {
				ArrayList<Integer> feasibleTasks = getFeasibleTasksToSchedule(tasksToSchedule, currentPosition,
						route.getTotalTime(), route.getCapacityTime());
				if (!feasibleTasks.isEmpty()) {
					int task = feasibleTasks.get(0);

					int distance = instance.getDistance(currentPosition, instance.getTask(task).getLocation());
					int travTime = instance.getTravTime(currentPosition, instance.getTask(task).getLocation());

					route.addTask(task, distance, travTime, 0, instance.getTask(task).getDuration());

					currentPosition = instance.getTask(task).getLocation();

					tasksToSchedule.remove((Integer) task);
				} else {
					insertionSolution.getRoutes().add(route);
					route = new Route(instance.getShiftDuration());
				}
			}

		}

		insertionSolution.getRoutes().add(route);

		return insertionSolution;
	}

	public Solution solveByDistance() {

		Solution insertionSolution = new Solution(instance.getMandays(), instance.getShiftDuration());

		ArrayList<Integer> tasksToSchedule = new ArrayList<Integer>(tasksByDuration);

		int usedRoutes = 0;
		int currentPosition = 0;
		int currentLocationIndex = 0;

		Route route = new Route(instance.getShiftDuration());

		while (!tasksToSchedule.isEmpty() && usedRoutes <= instance.getMandays()
				&& currentLocationIndex < numLocations) {

			if (route.getRouteTasks().isEmpty()) {
				usedRoutes++;

				int selectedLocation = locationsByDemand[currentLocationIndex];
				boolean foundTask = false;
				int task;
				do {
					int i = 0;

					do {
						task = tasksToSchedule.get(i++);
						if (instance.getTask(task).getLocation() == selectedLocation) {
							foundTask = true;
							break;
						}
					} while (i < tasksToSchedule.size());

					if (!foundTask)
						selectedLocation = locationsByDemand[++currentLocationIndex];

				} while (!foundTask);

				if (instance.getTask(task).getDuration() <= route.getAvailableTime()) {
					route.addTask(task, 0, 0, 0, instance.getTask(task).getDuration());

					currentPosition = instance.getTask(task).getLocation();

					tasksToSchedule.remove((Integer) task);

					if (instance.getCollaborations().contains(task)) {
						route.setCollaborative(true);
						int supportTask = instance.getCollabPair(task);
						Route supportRoute = new Route(instance.getShiftDuration());
						completeRouteBackwards(supportRoute, tasksToSchedule, supportTask, route.getLastEndTime());
						completeRouteForward(insertionSolution, supportRoute, tasksToSchedule, currentPosition);
						supportRoute.setCollaborative(true);
						insertionSolution.getRoutes().add(supportRoute);
					}
				} else {
					System.out.println("Error: Task " + task + " duration is longer than scheduled shifts.");
				}
			} else {
				completeRouteForward(insertionSolution, route, tasksToSchedule, currentPosition);
				insertionSolution.getRoutes().add(route);
				route = new Route(instance.getShiftDuration());
			}

		}

		insertionSolution.getRoutes().add(route);

		mappingResourcesToRoutes(insertionSolution, instance.getResources());

		return insertionSolution;
	}

	private void completeRouteBackwards(Route supportRoute, ArrayList<Integer> tasksToSchedule, int supportTask,
			int endTime) {
		ArrayList<Integer> previousTasks = new ArrayList<Integer>();
		ArrayList<Integer> localTasksToSchedule = new ArrayList<Integer>(tasksToSchedule);

		previousTasks.add(supportTask);
		localTasksToSchedule.remove((Integer) supportTask);

		int currentPosition = instance.getTask(supportTask).getLocation();
		int duration = instance.getTask(supportTask).getDuration();
		int endPrevTime = endTime;

		ArrayList<Integer> feasibleTasks = getFeasiblePreviousTasks(localTasksToSchedule, currentPosition, endTime,
				duration);

		while (!feasibleTasks.isEmpty()) {
			int task;
			int i = 0;
			do {
				task = feasibleTasks.get(i++);
			} while (instance.getCollaborations().contains(task) && i < feasibleTasks.size());

			if (instance.getCollaborations().contains(task))
				break;

			int travTime = instance.getTravTime(instance.getTask(task).getLocation(), currentPosition);
			endPrevTime = endPrevTime - duration - travTime;
			duration = instance.getTask(task).getDuration();
			currentPosition = instance.getTask(task).getLocation();

			previousTasks.add(task);
			localTasksToSchedule.remove((Integer) task);

			feasibleTasks = getFeasiblePreviousTasks(localTasksToSchedule, currentPosition, endPrevTime, duration);
		}

		int distance = 0;
		int travTime = 0;
		int waitTime = 0;
		if (previousTasks.size() > 1) {
			supportRoute.addTask(previousTasks.get(previousTasks.size() - 1), 0, 0, 0,
					instance.getTask(previousTasks.get(previousTasks.size() - 1)).getDuration());
			tasksToSchedule.remove(previousTasks.get(previousTasks.size() - 1));
			for (int i = previousTasks.size() - 2; i > 0; i--) {
				distance = instance.getDistance(instance.getTask(previousTasks.get(i + 1)).getLocation(),
						instance.getTask(previousTasks.get(i)).getLocation());
				travTime = instance.getTravTime(instance.getTask(previousTasks.get(i + 1)).getLocation(),
						instance.getTask(previousTasks.get(i)).getLocation());
				supportRoute.addTask(previousTasks.get(i), distance, travTime, 0,
						instance.getTask(previousTasks.get(i)).getDuration());
				tasksToSchedule.remove(previousTasks.get(i));
			}
			distance = instance.getDistance(instance.getTask(previousTasks.get(1)).getLocation(),
					instance.getTask(supportTask).getLocation());
			travTime = instance.getTravTime(instance.getTask(previousTasks.get(1)).getLocation(),
					instance.getTask(supportTask).getLocation());
			waitTime = endTime - instance.getTask(supportTask).getDuration() - supportRoute.getLastEndTime() - travTime;
		} else {
			waitTime = endTime - instance.getTask(supportTask).getDuration();
		}

		supportRoute.addTask(supportTask, distance, travTime, waitTime, instance.getTask(supportTask).getDuration());
		tasksToSchedule.remove((Integer) supportTask);

	}

	private void completeRouteForward(Solution insertionSolution, Route route, ArrayList<Integer> tasksToSchedule,
			int currentPosition) {

		ArrayList<Integer> feasibleTasks = getFeasibleTasksToSchedule(tasksToSchedule, currentPosition,
				route.getTotalTime(), route.getCapacityTime());
		while (!feasibleTasks.isEmpty()) {
			int task = feasibleTasks.get(0);

			int distance = instance.getDistance(currentPosition, instance.getTask(task).getLocation());
			int travTime = instance.getTravTime(currentPosition, instance.getTask(task).getLocation());

			route.addTask(task, distance, travTime, 0, instance.getTask(task).getDuration());

			currentPosition = instance.getTask(task).getLocation();

			tasksToSchedule.remove((Integer) task);

			if (instance.getCollaborations().contains(task)) {
				route.setCollaborative(true);
				int supportTask = instance.getCollabPair(task);
				Route supportRoute = new Route(instance.getShiftDuration());

				completeRouteBackwards(supportRoute, tasksToSchedule, supportTask, route.getLastEndTime());
				completeRouteForward(insertionSolution, supportRoute, tasksToSchedule, currentPosition);
				supportRoute.setCollaborative(true);
				insertionSolution.getRoutes().add(supportRoute);
			}

			feasibleTasks = getFeasibleTasksToSchedule(tasksToSchedule, currentPosition, route.getTotalTime(),
					route.getCapacityTime());
		}
	}
	
	private void completeRouteBackwardsCallback(Route supportRoute, ArrayList<Integer> tasksToSchedule, int supportTask,
			int endTime) {
		ArrayList<Integer> previousTasks = new ArrayList<Integer>();
		ArrayList<Integer> localTasksToSchedule = new ArrayList<Integer>(tasksToSchedule);

		previousTasks.add(supportTask);
		localTasksToSchedule.remove((Integer) supportTask);
		
		Task supportTaskData = new Task();
		if(instance.getTasks().containsKey(supportTask)) supportTaskData = instance.getTask(supportTask);
		else if(instance.getCallbacks().containsKey(supportTask)) supportTaskData = instance.getCallback(supportTask);
		else System.err.println("Task " + supportTask + " not found.");

		int currentPosition = supportTaskData.getLocation();
		int duration = supportTaskData.getDuration();
		int endPrevTime = endTime;

		ArrayList<Integer> feasibleTasks = getFeasiblePreviousTasks(localTasksToSchedule, currentPosition, endTime,
				duration);

		while (!feasibleTasks.isEmpty()) {
			int task;
			int i = 0;
			do {
				task = feasibleTasks.get(i++);
			} while (instance.getCollaborations().contains(task) && i < feasibleTasks.size());

			if (instance.getCollaborations().contains(task))
				break;
			
			Task taskData = new Task();
			if(instance.getTasks().containsKey(task)) taskData = instance.getTask(task);
			else if(instance.getCallbacks().containsKey(task)) taskData = instance.getCallback(task);
			else System.err.println("Task " + task + " not found.");

			int travTime = instance.getTravTime(taskData.getLocation(), currentPosition);
			endPrevTime = endPrevTime - duration - travTime;
			duration = taskData.getDuration();
			currentPosition = taskData.getLocation();

			previousTasks.add(task);
			localTasksToSchedule.remove((Integer) task);

			feasibleTasks = getFeasiblePreviousTasks(localTasksToSchedule, currentPosition, endPrevTime, duration);
		}

		int distance = 0;
		int travTime = 0;
		int waitTime = 0;
		if (previousTasks.size() > 1) {
			if(instance.getTasks().containsKey(previousTasks.get(previousTasks.size() - 1))) {
				supportRoute.addTask(previousTasks.get(previousTasks.size() - 1), 0, 0, 0,
						instance.getTask(previousTasks.get(previousTasks.size() - 1)).getDuration());
				tasksToSchedule.remove(previousTasks.get(previousTasks.size() - 1));
			} else if(instance.getCallbacks().containsKey(previousTasks.get(previousTasks.size() - 1))) {
				supportRoute.addTask(previousTasks.get(previousTasks.size() - 1), 0, 0, 0,
						instance.getCallback(previousTasks.get(previousTasks.size() - 1)).getDuration());
				tasksToSchedule.remove(previousTasks.get(previousTasks.size() - 1));
			}
			for (int i = previousTasks.size() - 2; i > 0; i--) {
				if(instance.getTasks().containsKey(previousTasks.get(previousTasks.get(i)))) {
					if(instance.getTasks().containsKey(previousTasks.get(i + 1))) {
						distance = instance.getDistance(instance.getTask(previousTasks.get(i + 1)).getLocation(),
								instance.getTask(previousTasks.get(i)).getLocation());
						travTime = instance.getTravTime(instance.getTask(previousTasks.get(i + 1)).getLocation(),
								instance.getTask(previousTasks.get(i)).getLocation());
						supportRoute.addTask(previousTasks.get(i), distance, travTime, 0,
								instance.getTask(previousTasks.get(i)).getDuration());
						tasksToSchedule.remove(previousTasks.get(i));
					} else if(instance.getCallbacks().containsKey(previousTasks.get(i + 1))) {
						distance = instance.getDistance(instance.getCallback(previousTasks.get(i + 1)).getLocation(),
								instance.getTask(previousTasks.get(i)).getLocation());
						travTime = instance.getTravTime(instance.getCallback(previousTasks.get(i + 1)).getLocation(),
								instance.getTask(previousTasks.get(i)).getLocation());
						supportRoute.addTask(previousTasks.get(i), distance, travTime, 0,
								instance.getTask(previousTasks.get(i)).getDuration());
						tasksToSchedule.remove(previousTasks.get(i));
					}
				} else if(instance.getCallbacks().containsKey(previousTasks.get(previousTasks.get(i)))) {
					if(instance.getTasks().containsKey(previousTasks.get(i + 1))) {
						distance = instance.getDistance(instance.getTask(previousTasks.get(i + 1)).getLocation(),
							instance.getCallback(previousTasks.get(i)).getLocation());
						travTime = instance.getTravTime(instance.getTask(previousTasks.get(i + 1)).getLocation(),
							instance.getCallback(previousTasks.get(i)).getLocation());
						supportRoute.addTask(previousTasks.get(i), distance, travTime, 0,
							instance.getCallback(previousTasks.get(i)).getDuration());
						tasksToSchedule.remove(previousTasks.get(i));
					} else if(instance.getCallbacks().containsKey(previousTasks.get(i + 1))) {
						distance = instance.getDistance(instance.getCallback(previousTasks.get(i + 1)).getLocation(),
								instance.getCallback(previousTasks.get(i)).getLocation());
						travTime = instance.getTravTime(instance.getCallback(previousTasks.get(i + 1)).getLocation(),
								instance.getCallback(previousTasks.get(i)).getLocation());
						supportRoute.addTask(previousTasks.get(i), distance, travTime, 0,
								instance.getCallback(previousTasks.get(i)).getDuration());
						tasksToSchedule.remove(previousTasks.get(i));
					}
				}
			}
			if(instance.getTasks().containsKey(previousTasks.get(1))) {
				distance = instance.getDistance(instance.getTask(previousTasks.get(1)).getLocation(),
					supportTaskData.getLocation());
				travTime = instance.getTravTime(instance.getTask(previousTasks.get(1)).getLocation(),
					supportTaskData.getLocation());
				waitTime = endTime - supportTaskData.getDuration() - supportRoute.getLastEndTime() - travTime;
			} else if(instance.getCallbacks().containsKey(previousTasks.get(1))) {
				distance = instance.getDistance(instance.getCallback(previousTasks.get(1)).getLocation(),
						supportTaskData.getLocation());
					travTime = instance.getTravTime(instance.getCallback(previousTasks.get(1)).getLocation(),
						supportTaskData.getLocation());
					waitTime = endTime - supportTaskData.getDuration() - supportRoute.getLastEndTime() - travTime;
			}
		} else {
			waitTime = endTime - supportTaskData.getDuration();
		}

		supportRoute.addTask(supportTask, distance, travTime, waitTime, supportTaskData.getDuration());
		tasksToSchedule.remove((Integer) supportTask);

	}
	
	public void completeRouteForwardCallback(Route route, ArrayList<Integer> tasksToSchedule,
			int currentPosition) { 

		ArrayList<Integer> feasibleTasks = getFeasibleTasksToSchedule(tasksToSchedule, currentPosition,
				route.getTotalTime(), route.getCapacityTime());
		while (!feasibleTasks.isEmpty()) {
			int task = feasibleTasks.get(0);

			Task taskData = new Task();
			if(instance.getTasks().containsKey(task)) taskData = instance.getTask(task);
			else if(instance.getCallbacks().containsKey(task)) taskData = instance.getCallback(task);
			else System.err.println("Task " + task + " not found.");
			
			int distance = instance.getDistance(currentPosition, taskData.getLocation());
			int travTime = instance.getTravTime(currentPosition, taskData.getLocation());

			route.addTask(task, distance, travTime, 0, taskData.getDuration());

			currentPosition = taskData.getLocation();

			tasksToSchedule.remove((Integer) task);

			feasibleTasks = getFeasibleTasksToSchedule(tasksToSchedule, currentPosition, route.getTotalTime(),
					route.getCapacityTime());
		}
	}
	
	// Store last selected route for use by Simulation
	private int lastSelectedRoute = -1;

	public int getLastSelectedRoute() {
		return lastSelectedRoute;
	}

	public ArrayList<Integer> scheduleCallback(Solution solution, Task callback, int timeEvent, int dayEvent) {
		ArrayList<Integer> tasksToSchedule = new ArrayList<Integer>();
		ArrayList<Integer> affectedRoutes = new ArrayList<Integer>();

		affectedRoutes = solution.getRoutesByDay(dayEvent);


		int selectedRoute = selectedRoute(solution, affectedRoutes, timeEvent, callback.getLocation());
		this.lastSelectedRoute = selectedRoute;  // Store for Simulation to access
		int currentTask = solution.getRoutes().get(selectedRoute).getTaskAtTime(timeEvent);

		Route route = solution.getRoutes().get(selectedRoute);
		int indexInRoute = route.getRouteTasks().indexOf(currentTask);

		Task current = getTaskOrCallback(currentTask);
		int currentPosition = current.getLocation();
		int travel = instance.getTravTime(currentPosition, callback.getLocation());

		System.out.println(route.printRoute());

			ArrayList<Integer> tempRoute = new ArrayList<Integer>(route.getRouteTasks());
			route.clear();

			for(int i = 0; i <= indexInRoute; i++) {
				if(route.getRouteTasks().isEmpty()) {
					route.addTask(tempRoute.get(i), 0, 0, 0, getTaskOrCallback(tempRoute.get(i)).getDuration());
				} else {
					int distance = instance.getDistance(getTaskOrCallback(tempRoute.get(i-1)).getLocation(), getTaskOrCallback(tempRoute.get(i)).getLocation());
					int travTime = instance.getTravTime(getTaskOrCallback(tempRoute.get(i-1)).getLocation(), getTaskOrCallback(tempRoute.get(i)).getLocation());
					route.addTask(tempRoute.get(i), distance, travTime, 0, getTaskOrCallback(tempRoute.get(i)).getDuration());
				}
			}

			for(int i = indexInRoute + 1; i < tempRoute.size(); i++) {
				if(instance.getCallbacks().containsKey(tempRoute.get(i))) {
					// This is a previously scheduled callback - keep it in the route
					int lastTaskIndex = route.getRouteTasks().get(route.getRouteTasks().size() - 1);
					Task lastTask = getTaskOrCallback(lastTaskIndex);
					Task callbackTask = instance.getCallback(tempRoute.get(i));
					int distance = instance.getDistance(lastTask.getLocation(), callbackTask.getLocation());
					int travTime = instance.getTravTime(lastTask.getLocation(), callbackTask.getLocation());
					route.addTask(tempRoute.get(i), distance, travTime, 0, callbackTask.getDuration());
				} else {
					// Regular task - add to displaced tasks list
					tasksToSchedule.add(tempRoute.get(i));
				}
			}

			int lastTaskIndex = route.getRouteTasks().get(route.getRouteTasks().size() - 1);
			Task lastTask = getTaskOrCallback(lastTaskIndex);
			int distance = instance.getDistance(lastTask.getLocation(), callback.getLocation());
			travel = instance.getTravTime(lastTask.getLocation(), callback.getLocation());
			route.addTask(callback.getTaskID(), distance, travel, 0, callback.getDuration());
			completeRouteForwardCallback(route, tasksToSchedule, currentPosition);

			System.out.println(route.printRoute());

			if(!tasksToSchedule.isEmpty()) System.out.println("Displaced tasks remaining: " + tasksToSchedule);

		return tasksToSchedule;
	}

	// Helper method to get a task whether it's a regular task or callback
	private Task getTaskOrCallback(int taskId) {
		if(instance.getTasks().containsKey(taskId)) {
			return instance.getTask(taskId);
		} else if(instance.getCallbacks().containsKey(taskId)) {
			return instance.getCallback(taskId);
		}
		System.err.println("Task/Callback " + taskId + " not found!");
		return null;
	}
	
	public ArrayList<Integer> insertAfter(Solution solution, int selectedRoute, int currentTask, Task callback) {
		ArrayList<Integer> tasksToSchedule;
		
		Route route = solution.getRoutes().get(selectedRoute);
		
		int indexInRoute = route.getRouteTasks().indexOf(currentTask);
		int currentLocation = instance.getTask(currentTask).getLocation();
		
		int distance = instance.getDistance(currentLocation, callback.getLocation());
		int travTime = instance.getTravTime(currentLocation, callback.getLocation());
		route.getRouteTasks().add(indexInRoute+1, callback.getTaskID());
		route.getStartTimes().add(indexInRoute+1, route.getEndTimes().get(indexInRoute) + travTime);
		route.getEndTimes().add(indexInRoute+1, route.getStartTimes().get(indexInRoute+1) + callback.getDuration());
		route.setValuesToPosition(indexInRoute+1, instance);
		
		ArrayList<Integer> tempRoute = new ArrayList<Integer>(route.getRouteTasks().subList(0, indexInRoute + 1));
		
		tasksToSchedule = new ArrayList<Integer>(route.getRouteTasks().subList(indexInRoute+2, route.getRouteTasks().size()));
		
		route.getRouteTasks().clear();
		route.getStartTimes().clear();
		route.getEndTimes().clear();
		
		route.addTask(tempRoute.get(0), 0, 0, 0, instance.getTask(tempRoute.get(0)).getDuration());
		for(int j = 1; j < tempRoute.size(); j++) {
			int dist = instance.getDistance(instance.getTask(tempRoute.get(j-1)).getLocation(), instance.getTask(tempRoute.get(j)).getLocation());
			int trav = instance.getTravTime(instance.getTask(tempRoute.get(j-1)).getLocation(), instance.getTask(tempRoute.get(j)).getLocation());
			route.addTask(tempRoute.get(j), dist, trav, 0, instance.getTask(tempRoute.get(j)).getDuration());
		}
		
		for(Integer task : tasksToSchedule) {
			if(instance.getCallbacks().containsKey(task)) {
				route.addTask(task, 
						instance.getDistance(callback.getLocation(), instance.getCallback(task).getLocation()), 
						instance.getTravTime(callback.getLocation(), instance.getCallback(task).getLocation()), 
						0, instance.getCallback(task).getDuration());
				tasksToSchedule.remove((Integer) task);
			}
		}
		
//		for(Integer taskToRemove : tasksToSchedule) {
//			if(!instance.getCallbacks().containsKey(taskToRemove)) {
//				int i = route.getRouteTasks().indexOf(taskToRemove);
//				route.getRouteTasks().remove(i);
//				route.getStartTimes().remove(i);
//				route.getEndTimes().remove(i);
//			} else {
//				Task formerCallback = instance.getCallback((Integer) taskToRemove);
//				int i = route.getRouteTasks().indexOf(taskToRemove);
//				route.getRouteTasks().remove(i);
//				route.getStartTimes().remove(i);
//				route.getEndTimes().remove(i);
//				
//				route.addTask(formerCallback.getTaskID(), 
//						instance.getDistance(callback.getLocation(), formerCallback.getLocation()), 
//						instance.getTravTime(callback.getLocation(), formerCallback.getLocation()), 
//						0, formerCallback.getDuration());
//				tasksToSchedule.remove((Integer) taskToRemove);
//			}
//		}
		
		completeRouteForwardCallback(route, tasksToSchedule, callback.getLocation());
		
		return tasksToSchedule;
	}

	private void mappingResourcesToRoutes(Solution solution, ArrayList<Resource> resources) {
		Hashtable<Integer,ArrayList<Integer>> resourcesByShift = instance.getResourcesByShift();
		Hashtable<Integer, ArrayList<Integer>> routesByDay = solution.getRoutesByDay();
		
		for(int i = 0; i < instance.getResources().get(0).getStartShift().length; i++)
			routesByDay.put(i, new ArrayList<Integer>());
		
		int[][] assignmentMatrix = new int[instance.getResources().size()][instance.getResources().get(0).getStartShift().length];
		for(int i = 0; i < assignmentMatrix.length; i++)
			for(int j = 0; j < assignmentMatrix[0].length; j++)
				assignmentMatrix[i][j] = -1;
		
		ArrayList<Integer> routesToAssign = new ArrayList<Integer>();
		for(int i = 0; i < solution.getNumberRoutes(); i++)
			routesToAssign.add(i);
		
		ArrayList<Integer> indexes = new ArrayList<Integer>();
		for(int j = 0; j < resourcesByShift.get(1).size(); j++)
			for(int i = 0; i < resourcesByShift.size(); i++)
				indexes.add(resourcesByShift.get(i+1).get(j) - 1);
		
		while(!routesToAssign.isEmpty()) {
			int routeIndex = routesToAssign.get(0);
			boolean assigned = false;
			
			for(Integer i : indexes) {
//			for(int i = 0; i < assignmentMatrix.length; i++) {
				for(int j = 0; j < assignmentMatrix[0].length; j++) {

					if(assignmentMatrix[i][j] == -1) {
						assignmentMatrix[i][j] = routeIndex;
						routesToAssign.remove((Integer) routeIndex);
						if(solution.getRoutes().get(routeIndex).isCollaborative()) {
							ArrayList<Integer> relatedRoutes = new ArrayList<Integer>();
							for(int k = 0; k < routesToAssign.size(); k++) {
								if(solution.getRoutes().get(routeIndex).isRelated(instance, solution.getRoutes().get(routesToAssign.get(k)))) {
									relatedRoutes.add(routesToAssign.get(k));
								}
							}
							
							for(int t = 0; t < relatedRoutes.size(); t++) {
								for(int k = 0; k < routesToAssign.size(); k++) {
									if(solution.getRoutes().get(relatedRoutes.get(t)).isRelated(instance, solution.getRoutes().get(routesToAssign.get(k))) 
											&& !relatedRoutes.contains(routesToAssign.get(k))) {
										relatedRoutes.add(routesToAssign.get(k));
									}
								}
							}
							
							int auxi = 1;
							for(Integer relRouteIndex : relatedRoutes) {
									assignmentMatrix[i+auxi][j] = relRouteIndex;
									routesToAssign.remove((Integer) relRouteIndex);
									auxi++;
							}							
						}

						assigned = true;
						break;
					}
				}
				if(assigned) break;
			}
		}
		
		solution.setResourceDayRoute(assignmentMatrix);
		
		for(int j = 0; j < assignmentMatrix[0].length; j++) {
			for(int i = 0; i < assignmentMatrix.length; i++) {
				if(assignmentMatrix[i][j] != -1) routesByDay.get(j).add(assignmentMatrix[i][j]);
			}
		}
			
	}

	private void init() {
		calcNearestDist();
		calcNearestTimeFrom();
		calcNearestTimeTo();
		sortTasksByLocation();
		sortTasksByDuration();
		sortLocationsByDemand();
	}

	private void calcNearestDist() {
		int index = 0;
		int lb = 0;
		int ub = Integer.MAX_VALUE;
		ArrayList<Integer> assigned = new ArrayList<Integer>();

		for (int i = 0; i < numLocations; i++) {
			int[] row = instance.getRowDistMatrix(i);
			for (int t = 0; t < numLocations - 1; t++) {
				for (int j = 0; j < numLocations; j++) {
					if (row[j] < ub && row[j] >= lb && j != i && !assigned.contains(j)) {
						ub = row[j];
						index = j;
					}
				}
				lb = row[index];
				ub = Integer.MAX_VALUE;
				nearestDist[i][t] = index;
				assigned.add(index);
			}

			lb = 0;
			ub = Integer.MAX_VALUE;
			assigned.clear();
		}

	}

	private void calcNearestTimeFrom() {
		int index = 0;
		int lb = 0;
		int ub = Integer.MAX_VALUE;
		ArrayList<Integer> assigned = new ArrayList<Integer>();

		for (int i = 0; i < numLocations; i++) {
			int[] row = instance.getRowTimeMatrix(i);
			for (int t = 0; t < numLocations - 1; t++) {
				for (int j = 0; j < numLocations; j++) {
					if (row[j] < ub && row[j] >= lb && j != i && !assigned.contains(j)) {
						ub = row[j];
						index = j;
					}
				}
				lb = row[index];
				ub = Integer.MAX_VALUE;
				nearestTimeFrom[i][t] = index;
				assigned.add(index);
			}

			lb = 0;
			ub = Integer.MAX_VALUE;
			assigned.clear();
		}

	}

	private void calcNearestTimeTo() {
		int index = 0;
		int lb = 0;
		int ub = Integer.MAX_VALUE;
		ArrayList<Integer> assigned = new ArrayList<Integer>();

		for (int j = 0; j < numLocations; j++) {
			int[] column = instance.getColTimeMatrix(j);
			for (int t = 0; t < numLocations - 1; t++) {
				for (int i = 0; i < numLocations; i++) {
					if (column[i] < ub && column[i] >= lb && i != j && !assigned.contains(i)) {
						ub = column[i];
						index = i;
					}
				}
				lb = column[index];
				ub = Integer.MAX_VALUE;
				nearestTimeTo[j][t] = index;
				assigned.add(index);
			}

			lb = 0;
			ub = Integer.MAX_VALUE;
			assigned.clear();
		}

	}

	private void sortTasksByLocation() {
		for (int i = 0; i < numLocations; i++) {
			ArrayList<Integer> tasksList = new ArrayList<Integer>();
			for (int j = 1; j <= instance.getTasks().size(); j++)
				if (instance.getTask(j).getLocation() == i)
					tasksList.add(j);
			tasksByLocation.put(i, tasksList);
		}

	}

	private void sortTasksByDuration() {
		ArrayList<Task> tasks = new ArrayList<Task>();
		for (int i = 1; i <= instance.getTasks().size(); i++)
			tasks.add(instance.getTask(i));

		Collections.sort(tasks);

		for (Task task : tasks)
			tasksByDuration.add(task.getTaskID());

	}

	private void sortLocationsByDemand() {
		ArrayList<Integer> indexes = new ArrayList<Integer>();
		int minSize = 0;
		int indexMin = 0;
		for (int i = 0; i < numLocations; i++) {
			if (tasksByLocation.get(i).size() <= minSize) {
				indexMin = i;
			}
		}

		for (int i = 0; i < numLocations; i++) {
			int maxindex = indexMin;
			for (int j = 0; j < numLocations; j++) {
				if (tasksByLocation.get(j).size() >= tasksByLocation.get(maxindex).size() && !indexes.contains(j)) {
					maxindex = j;
				}
			}
			locationsByDemand[i] = maxindex;
			indexes.add(maxindex);
		}
	}

	public ArrayList<Integer> getFeasibleTasks(int currentLocation, int consumedTime, int availability) {
		ArrayList<Integer> feasibleJobs = new ArrayList<Integer>();

		int[] rankedLocations = nearestTimeFrom[currentLocation];
		for (int i = 0; i < rankedLocations.length; i++) {
			ArrayList<Integer> candidateTasks = tasksByLocation.get(rankedLocations[i]);
			for (int j = 0; j < candidateTasks.size(); j++) {
				if (consumedTime + instance.getTravTime(currentLocation, rankedLocations[i])
						+ instance.getTask(candidateTasks.get(j)).getDuration() <= availability) {
					feasibleJobs.add(candidateTasks.get(j));
				}
			}
		}

		return feasibleJobs;
	}

	public ArrayList<Integer> getFeasibleTasksToSchedule(ArrayList<Integer> tasksToSchedule, int currentLocation,
			int consumedTime, int availability) {
		ArrayList<Integer> feasibleJobs = new ArrayList<Integer>();

		int[] rankedLocations = nearestTimeFrom[currentLocation];
		for (int i = 0; i < rankedLocations.length; i++) {
			ArrayList<Integer> candidateTasks = tasksByLocation.get(rankedLocations[i]);
			for (int j = 0; j < candidateTasks.size(); j++) {
				if (consumedTime + instance.getTravTime(currentLocation, rankedLocations[i])
						+ instance.getTask(candidateTasks.get(j)).getDuration() <= availability
						&& tasksToSchedule.contains(candidateTasks.get(j))) {
					feasibleJobs.add(candidateTasks.get(j));
				}
			}
		}

		return feasibleJobs;
	}

	public ArrayList<Integer> getFeasiblePreviousTasks(ArrayList<Integer> tasksToSchedule, int currentLocation,
			int endColTime, int durationCol) {
		int availability = endColTime - durationCol;
		ArrayList<Integer> feasibleJobs = new ArrayList<Integer>();

		int[] rankedLocations = nearestTimeTo[currentLocation];
		for (int i = 0; i < rankedLocations.length; i++) {
			ArrayList<Integer> candidateTasks = tasksByLocation.get(rankedLocations[i]);
			for (int j = 0; j < candidateTasks.size(); j++) {
				if (instance.getTravTime(rankedLocations[i], currentLocation)
						+ instance.getTask(candidateTasks.get(j)).getDuration() <= availability
						&& tasksToSchedule.contains(candidateTasks.get(j))) {
					feasibleJobs.add(candidateTasks.get(j));
				}
			}
		}

		return feasibleJobs;
	}
	
	private int selectedRoute(Solution solution, ArrayList<Integer> affectedRoutes, int timeEvent, int callbackLocation) {
		int selectedRoute = affectedRoutes.get(0);
		int taskAtTime = solution.getRoutes().get(affectedRoutes.get(0)).getTaskAtTime(timeEvent);
		int currentLocation = getTaskOrCallback(taskAtTime).getLocation();
		int closestTime = instance.getTravTime(currentLocation, callbackLocation);
		for(Integer route : affectedRoutes) {
			taskAtTime = solution.getRoutes().get(route).getTaskAtTime(timeEvent);
			currentLocation = getTaskOrCallback(taskAtTime).getLocation();
			int candTime = instance.getTravTime(currentLocation, callbackLocation);
			if(candTime < closestTime) {
				selectedRoute = route;
				closestTime = candTime;
			}
		}

		return selectedRoute;
	}
}
