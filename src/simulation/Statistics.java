package simulation;

public class Statistics {

	public Statistics() {
		super();
	}
	
	public static double getMean(int[] data) {
		double mean = 0;
		
		for(int i = 0; i < data.length; i++)
			mean += data[i];
		
		return (double) mean/data.length;
	}
	
	public static double getVariance(int[] data) {
		double variance = 0;
		
		double mean = getMean(data);
		
		for(int i = 0; i < data.length; i++)
			variance += (mean - data[i]) * (mean - data[i]);
		
		return (double) variance/(data.length-1);	// Bessel's correction: 1/(n-1) instead of 1/n
	}
	
	public static double getStdDev(int[] data) {
		return (double) Math.sqrt(getVariance(data));
	}
	
	public static double getMax(int[] data) {
		double max = 0;
		
		for(int i = 0; i < data.length; i++)
			if(data[i] > max) max = data[i];
		
		return (double) max;
	}
	
	public static double getMin(int[] data) {
		double min = data[0];
		
		for(int i = 1; i < data.length; i++)
			if(data[i] < min) min = data[i];
		
		return (double) min;
		
	}
}
