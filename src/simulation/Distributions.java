package simulation;

import java.util.Random;

public class Distributions {

	private Random random;
	
	public Distributions() {
		this.random = new Random();
	}
	
	public Distributions(long seed) {
		this.random = new Random(seed);
	}
	
	public double sampleUniform() {
		return random.nextDouble();
	}
	
	public double sampleUniform(double lower, double upper) {
		return (upper - lower) * random.nextDouble() + lower;
	}
	
	public double sampleNormal() {
		return random.nextGaussian();
	}
	
	public double sampleNormal(double mean, double stdev) {
		return mean - random.nextGaussian() * stdev;
	}
	
//	Very naive truncated normal distribution	
	public double sampleTruncatedNormal(double mean, double variance, double lb, double ub) {
		double genNumber;
		
		do {
			genNumber = mean - random.nextGaussian() * variance;
		} while(genNumber < lb || genNumber > ub);
		
		return genNumber;
	}
	
	public double sampleLogNormal(double mean, double stdev) {
//		return Math.exp(sampleNormal(mean, stdev));
		double varx = Math.pow(stdev, 2);
	    double ess = Math.log(1.0 + (varx/Math.pow(mean,2)));
	    double mu = Math.log(mean) - (0.5*Math.pow(ess, 2));
	    return Math.exp(mu+(ess*sampleNormal()));
	}
	
	/** Return a random double drawn from a Gamma distribution with mean alpha*beta+lamba and variance alpha*beta^2. */
	  public double sampleGamma(double alpha,double beta,double lambda) {
	    double gamma=0;
	    if (alpha <= 0 || beta <= 0) {
	      throw new IllegalArgumentException ("alpha and beta must be strictly positive.");
	    }
	    if (alpha < 1) {
	      double b,p;
	      boolean flag=false;
	      b=1+alpha*Math.exp(-1);
	      while(!flag) {
	        p=b*sampleUniform();
	        if (p>1) {
	          gamma=-Math.log((b-p)/alpha);
	          if (sampleUniform()<=Math.pow(gamma,alpha-1)) flag=true;
	        }
	        else {
	          gamma=Math.pow(p,1/alpha);
	          if (sampleUniform()<=Math.exp(-gamma)) flag=true;
	        }
	      }
	    }
	    else if (alpha == 1) {
	      gamma = -Math.log (sampleUniform ());
	    } else {
	      double y = -Math.log (sampleUniform ());
	      while (sampleUniform () > Math.pow (y * Math.exp (1 - y), alpha - 1))
	        y = -Math.log (sampleUniform ());
	      gamma = alpha * y;
	    }
	    return beta*gamma+lambda;
	  }
	  
	  /** Return a random double drawn from a Beta distribution with mean a/(a+b) and variance ab/((a+b+1)(a+b)^2).  */
	  public synchronized double sampleBeta(double alpha,double beta) {
	    if (alpha <= 0 || beta <= 0) {
	      throw new IllegalArgumentException ("alpha and beta must be strictly positive.");
	    }
	    if (alpha == 1 && beta == 1) {
	      return sampleUniform();
	    } else if (alpha >= 1 && beta >= 1) {
	      double A = alpha - 1,
	              B = beta - 1,
	              C = A + B,
	              L = C * Math.log (C),
	              mu = A / C,
	              sigma = 0.5 / Math.sqrt (C);
	      double y = sampleNormal(), x = sigma * y + mu;
	      while (x < 0 || x > 1) {
	        y = sampleNormal();
	        x = sigma * y + mu;
	      }
	      double u = sampleUniform();
	      while (Math.log (u) >= A * Math.log (x / A) + B * Math.log ((1 - x) / B) + L + 0.5 * y * y) {
	        y = sampleNormal();
	        x = sigma * y + mu;
	        while (x < 0 || x > 1) {
	          y = sampleNormal();
	          x = sigma * y + mu;
	        }
	        u = sampleUniform();
	      }
	      return x;
	    } else {
	      double v1 = Math.pow (sampleUniform(), 1 / alpha),
	              v2 = Math.pow (sampleUniform(), 1 / beta);
	      while (v1 + v2 > 1) {
	        v1 = Math.pow (sampleUniform(), 1 / alpha);
	        v2 = Math.pow (sampleUniform(), 1 / beta);
	      }
	      return v1 / (v1 + v2);
	    }
	  }
}
