package kovac.res.quadric;

import Jama.Matrix;
import Jama.SingularValueDecomposition;
import icy.sequence.DimensionId;
import icy.type.point.Point3D;
import kovac.maths.EllipsoidAlgorithm;

/**
 * This class stores a quadratic expression as a 10x1 Matrix, representing the
 * 10 factors of this expression.
 * 
 * @author bastien.kovac
 *
 */
public class QuadricExpression_REVISED {

	private Matrix coefficients;

	private Matrix matSR;
	private Matrix centerMat;
	private Matrix axisVector;

	private double xSemiLength, ySemiLength, zSemiLength;

	public void QuadricExpression(Matrix coefficients) {
		this.coefficients = coefficients;
	}

	public void QuadricExpression(double[] coefficients) {
		if (coefficients.length != 10)
			throw new IllegalArgumentException("Needs 10 factors to build a QuadricExpression");
		double[][] matArrayCoeffs = new double[10][1];
		for (int i = 0; i < 10; i++) {
			matArrayCoeffs[i] = new double[] { coefficients[i] };
		}
		this.coefficients = new Matrix(matArrayCoeffs);
	}

	public void setCoefficients(double[] coeffs) {
		if (coeffs.length != 10)
			throw new RuntimeException("Needs 10 factors to build a QuadricExpression");
		double[][] matArrayCoeffs = new double[10][1];
		for (int i = 0; i < 10; i++) {
			matArrayCoeffs[i] = new double[] { coeffs[i] };
		}
		this.coefficients = new Matrix(matArrayCoeffs);
	}

	public Matrix getCoefficients() {
		return this.coefficients;
	}

	public double[] getSimpleArray() {
		return this.coefficients.getColumnPackedCopy();
	}

	private void updateMatAndCenter(double[] arrayQ) {
		// Initialize matrix
		matSR.set(0, 0, arrayQ[0]);
		matSR.set(0, 1, arrayQ[3]);
		matSR.set(0, 2, arrayQ[4]);

		matSR.set(1, 0, arrayQ[3]);
		matSR.set(1, 1, arrayQ[1]);
		matSR.set(1, 2, arrayQ[5]);

		matSR.set(2, 0, arrayQ[4]);
		matSR.set(2, 1, arrayQ[5]);
		matSR.set(2, 2, arrayQ[2]);

		Matrix lastCoeffs = new Matrix(3, 1);
		lastCoeffs.set(0, 0, arrayQ[6]);
		lastCoeffs.set(1, 0, arrayQ[7]);
		lastCoeffs.set(2, 0, arrayQ[8]);
		
		lastCoeffs = lastCoeffs.times(-1.0/2);

		centerMat = EllipsoidAlgorithm.proxf1(matSR, lastCoeffs);

	}

	public void getRealParameters() {
		this.matSR = new Matrix(3, 3);
		double[] arrayQ = getSimpleArray();

		updateMatAndCenter(arrayQ);

		Matrix scalarMat = matSR.times(centerMat);
		scalarMat = centerMat.transpose().times(scalarMat);
		double scalar = scalarMat.getColumnPackedCopy()[0];

		double alpha = 1 / (scalar - arrayQ[9]);
		matSR = matSR.times(alpha);

		// create S and R then SR (SU here)
		Matrix tmp = new Matrix(matSR.getArray());
		SingularValueDecomposition svd = tmp.svd();
		Matrix S = svd.getS();
		Matrix U = svd.getU();

		double[][] st = new double[3][3];

		xSemiLength = Math.sqrt(1 / S.get(0, 0));
		ySemiLength = Math.sqrt(1 / S.get(1, 1));
		zSemiLength = Math.sqrt(1 / S.get(2, 2));

		axisVector = U;

		st[0][0] = xSemiLength * 2;
		st[1][1] = ySemiLength * 2;
		st[2][2] = zSemiLength * 2;
		S = new Matrix(st);

		matSR = U.times(S.times(U.transpose()));
	}

	public void updateQuadric(Matrix matSR, Matrix centerMat) {
		this.matSR = matSR;
		this.centerMat = centerMat;

		this.coefficients = new Matrix(10, 1);
		// TODO => Needs to update quadric after matrix transformations

	}

	public Matrix getMatSR() {
		if (matSR == null)
			getRealParameters();
		return matSR;
	}

	public Matrix getCenterMat() {
		return centerMat;
	}

	public Matrix getAxisVector() {
		return axisVector;
	}

	public double[] getSemiAxis() {
		return new double[] { xSemiLength, ySemiLength, zSemiLength };
	}

	public double[] getIntersection(DimensionId dim, double[] truePos) {
		double[] coeffs = new double[6];
		double[] originalCoeffs = getSimpleArray();
		double fixedCoordinate;
		switch (dim) {
		case X:
			fixedCoordinate = truePos[0];
			coeffs[0] = originalCoeffs[1];
			coeffs[1] = originalCoeffs[2];
			coeffs[2] = originalCoeffs[5];
			coeffs[3] = originalCoeffs[3] * fixedCoordinate + originalCoeffs[7];
			coeffs[4] = originalCoeffs[4] * fixedCoordinate + originalCoeffs[8];
			coeffs[5] = originalCoeffs[0] * (fixedCoordinate * fixedCoordinate) + originalCoeffs[6] * fixedCoordinate
					+ originalCoeffs[9];
			break;
		case Y:
			fixedCoordinate = truePos[1];
			coeffs[0] = originalCoeffs[0];
			coeffs[1] = originalCoeffs[2];
			coeffs[2] = originalCoeffs[4];
			coeffs[3] = originalCoeffs[3] * fixedCoordinate + originalCoeffs[6];
			coeffs[4] = originalCoeffs[5] * fixedCoordinate + originalCoeffs[8];
			coeffs[5] = originalCoeffs[1] * (fixedCoordinate * fixedCoordinate) + originalCoeffs[7] * fixedCoordinate
					+ originalCoeffs[9];
			break;
		case Z:
			fixedCoordinate = truePos[2];
			coeffs[0] = originalCoeffs[0];
			coeffs[1] = originalCoeffs[1];
			coeffs[2] = originalCoeffs[3];
			coeffs[3] = originalCoeffs[4] * fixedCoordinate + originalCoeffs[6];
			coeffs[4] = originalCoeffs[5] * fixedCoordinate + originalCoeffs[7];
			coeffs[5] = originalCoeffs[2] * (fixedCoordinate * fixedCoordinate) + originalCoeffs[8] * fixedCoordinate
					+ originalCoeffs[9];
			break;
		default:
			break;
		}
		return coeffs;
	}

	public boolean isInsideQuadric(Point3D pt) {
		if (pt == null)
			return false;

		double[] simpleArray = getSimpleArray();
		double x = pt.getX();
		double y = pt.getY();
		double z = pt.getZ();

		return (simpleArray[0] * (x * x) + simpleArray[1] * (y * y) + simpleArray[2] * (z * z) + 
				2*simpleArray[3] * x * y + 2*simpleArray[4] * x * z + 2*simpleArray[5] * z * y 
				+ simpleArray[6] * x + simpleArray[7] * y + simpleArray[8] * z + simpleArray[9] <= 0);
	}

}
