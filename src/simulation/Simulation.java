package simulation;

import java.util.ArrayList;
import java.util.Collections;

import problem.Instance;
import problem.Task;
import solver.Insertion;
import solver.Route;
import solver.Solution;

public class Simulation {

	private Instance instance;
	private Solution solution;
	private Insertion insertion;

	public Simulation(Instance instance, Solution solution) {
		this.instance = instance;
		this.solution = solution;
		this.insertion = new Insertion(instance);
	}

	public void run() {
		ArrayList<Integer> callbackKeys = new ArrayList<Integer>(Collections.list(instance.getCallbacks().keys()));
		Collections.sort(callbackKeys);

		for (Integer i : callbackKeys) {
			Task callback = instance.getCallback(i);
			int timeEvent = callback.getTimeEvent();
			int dayEvent = (int)(timeEvent / 1440);
			System.out.println(callback.getTaskID() + " - Day: " + dayEvent);

			ArrayList<Integer> tasksToSchedule = insertion.scheduleCallback(solution, callback, timeEvent, dayEvent);
			int selectedRoute = insertion.getLastSelectedRoute();

			// Handle displaced tasks that couldn't fit in the same route
			if(!tasksToSchedule.isEmpty()) {
				System.out.println("Handling " + tasksToSchedule.size() + " displaced tasks...");

				// Find which resource owns the selected route
				int resource = 0;
				while(resource < solution.getResourceDayRoute().length &&
					  solution.getResourceDayRoute()[resource][dayEvent] != selectedRoute) {
					resource++;
				}

				if(resource < solution.getResourceDayRoute().length) {
					// Try to fit displaced tasks in subsequent days for the same resource
					int day = dayEvent + 1;
					while(!tasksToSchedule.isEmpty() && day < solution.getResourceDayRoute()[resource].length) {
						int checkedRoute = solution.getResourceDayRoute()[resource][day];
						if(checkedRoute >= 0 && checkedRoute < solution.getRoutes().size()) {
							Route routeToExtend = solution.getRoutes().get(checkedRoute);
							// Iterate backwards to avoid index issues when removing
							for(int j = tasksToSchedule.size() - 1; j >= 0; j--) {
								int taskId = tasksToSchedule.get(j);
								Task task = instance.getTask(taskId);
								if(task != null) {
									int lastTaskIndex = routeToExtend.getRouteTasks().get(routeToExtend.getRouteTasks().size() - 1);
									Task lastTask = getTaskOrCallback(lastTaskIndex);
									if(lastTask != null) {
										int travTime = instance.getTravTime(lastTask.getLocation(), task.getLocation());
										if(travTime + task.getDuration() <= routeToExtend.getAvailableTime()) {
											int distance = instance.getDistance(lastTask.getLocation(), task.getLocation());
											routeToExtend.addTask(taskId, distance, travTime, 0, task.getDuration());
											tasksToSchedule.remove(j);
											System.out.println("  Task " + taskId + " rescheduled to day " + day);
										}
									}
								}
							}
						}
						day++;
					}
				}

				// If still tasks remaining, create extra route(s)
				if(!tasksToSchedule.isEmpty()) {
					System.out.println("Creating extra route for " + tasksToSchedule.size() + " remaining tasks");
					Route extraRoute = new Route(instance.getShiftDuration());
					Task firstTask = instance.getTask(tasksToSchedule.get(0));
					extraRoute.addTask(tasksToSchedule.get(0), 0, 0, 0, firstTask.getDuration());
					tasksToSchedule.remove(0);

					for(int j = 0; j < tasksToSchedule.size(); j++) {
						int taskId = tasksToSchedule.get(j);
						Task task = instance.getTask(taskId);
						int lastTaskIndex = extraRoute.getRouteTasks().get(extraRoute.getRouteTasks().size() - 1);
						Task lastTask = instance.getTask(lastTaskIndex);
						int distance = instance.getDistance(lastTask.getLocation(), task.getLocation());
						int travTime = instance.getTravTime(lastTask.getLocation(), task.getLocation());
						extraRoute.addTask(taskId, distance, travTime, 0, task.getDuration());
					}
					tasksToSchedule.clear();
					solution.getRoutes().add(extraRoute);
				}
			}
		}
	}

	// Helper method to get a task whether it's a regular task or callback
	private Task getTaskOrCallback(int taskId) {
		if(instance.getTasks().containsKey(taskId)) {
			return instance.getTask(taskId);
		} else if(instance.getCallbacks().containsKey(taskId)) {
			return instance.getCallback(taskId);
		}
		return null;
	}

}
