package problem;


public class Task implements Comparable<Task> {
	private int taskID;
	private int location;
	private int duration;
	private int priority;
	private int timeEvent;
	
	public Task() {
		super();
	}
	
	public Task(int taskID, int location, int duration, int priority) {
		this.taskID = taskID;
		this.location = location;
		this.duration = duration;
		this.priority = priority;
	}
	
	public Task(int taskID, int location, int duration, int priority, int timeEvent) {
		this.taskID = taskID;
		this.location = location;
		this.duration = duration;
		this.priority = priority;
		this.timeEvent = timeEvent;
	}

	public int getTaskID() {
		return taskID;
	}

	public int getLocation() {
		return location;
	}

	public int getDuration() {
		return duration;
	}

	public int getPriority() {
		return priority;
	}
	
	public int getTimeEvent() {
		return timeEvent;
	}

	public void setTaskID(int taskID) {
		this.taskID = taskID;
	}

	public void setLocation(int location) {
		this.location = location;
	}

	public void setDuration(int duration) {
		this.duration = duration;
	}

	public void setPriority(int priority) {
		this.priority = priority;
	}
	
	public void setTimeEvent(int time) {
		this.timeEvent = time;
	}

//	Comparison defined this way to obtain decreasing order
	public int compareTo(Task t) {
		return(t.getDuration() - this.duration);
	}
	
	public boolean equals(Object t) {
		return (this.taskID == ((Task) t).getTaskID());
	}
	
	public String toString() {
		return "[TaskID = " + this.taskID + ", Location = " + this.location + ", Duration = " + this.duration + ", Priority = " + this.priority + "]";
	}

}
