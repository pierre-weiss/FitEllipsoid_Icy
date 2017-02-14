package kovac.res.util;

import Jama.Matrix;

public class MathUtils {

	/**
	 * Return the center of mass from a Matrix where its number of rows is the
	 * number of points, and the number of columns the dimensions of those
	 * points
	 * 
	 * @param m
	 *            The input matrix
	 * @return An array of double coordinates of length equals to the number of
	 *         columns of the input Matrix
	 */
	public static double[] getCenterOfMass(Matrix m) {
		double[][] matArray = m.getArray();
		double[] centerOfMass = new double[] { avg(matArray[0]), avg(matArray[1]), avg(matArray[2]) };
		return centerOfMass;
	}

	/**
	 * This methods computes the variance of an array of double. The algorithm
	 * used is the same as the Matlab's function var(X), which means that it
	 * uses a normalization factor of N-1 instead of the regular N
	 * 
	 * @param d The input data
	 * @return the variance of the data
	 */
	public static double getVariance(double[] d) {
		double avgD = avg(d);
		double[] aMinusAvg = new double[d.length];
		for (int i = 0; i < d.length; i++)
			aMinusAvg[i] = Math.abs(d[i] - avgD);
		return sumSquare(aMinusAvg) / (d.length - 1);
	}

	/**
	 * Return the sum of all element of an input array
	 * 
	 * @param t
	 *            The input array
	 * @return A double equals to the sum of all element of the array
	 */
	public static double sum(double[] t) {
		double res = 0;
		for (int i = 0; i < t.length; i++)
			res += t[i];
		return res;
	}

	public static double sumSquare(double[] d) {
		double res = 0;
		for (int i = 0; i < d.length; i++)
			res += d[i] * d[i];
		return res;
	}

	/**
	 * Return the average value of an array of double
	 * 
	 * @param t
	 *            The input array
	 * @return The average value
	 */
	public static double avg(double[] t) {
		return sum(t) / t.length;
	}
	
	/**
	 * Return the minimum value in a group of double
	 * @param ds The doubles to analyze
	 * @return The minimum value within ds
	 */
	public static double min(double...ds) {
		double min = ds[0];
		for (double d : ds)
			if (d < min)
				min = d;
		return min;
	}
	
	/**
	 * Return the maximum value in a group of double
	 * @param ds The doubles to analyze
	 * @return The maximum value within ds
	 */
	public static double max(double...ds) {
		double max = ds[0];
		for (double d : ds)
			if (d > max)
				max = d;
		return max;
	}
	
	public static double times(double...ds) {
		double res = 1;
		for (double d : ds)
			res *= d;
		return res;
	}

}
