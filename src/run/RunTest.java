package run;

import problem.Instance;
import problem.Resource;
import simulation.*;
import solver.Insertion;
import solver.Solution;

public class RunTest {
	
	private static String instanceFile = "instances/scenario1.txt";

	public static void main(String[] args) {

		Instance instance = new Instance(instanceFile);
		Insertion insertion = new Insertion(instance);
		
		long start = System.currentTimeMillis();
		Solution solution = insertion.solveByDistance();
		
//		solution.printSolution();
		
		Simulation simulation = new Simulation(instance, solution);
		simulation.run();
		long end = System.currentTimeMillis();
		
		solution.printSolution();
		System.out.println("CPU: " + (end - start)/1000.0);
		
		int[][] assignmentMatrix = solution.getResourceDayRoute();
		for(int i = 0; i < assignmentMatrix.length; i++) {
			for(int j = 0; j < assignmentMatrix[0].length; j++) {
				System.out.print(assignmentMatrix[i][j] + "\t");
			}
			System.out.print("\n");
		}
//		
//		for(int i = 0; i < solution.getRoutesByDay().size(); i++)
//			System.out.println(i + "\t" + solution.getRoutesByDay(i));
	}

}
