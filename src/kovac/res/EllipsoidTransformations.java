package kovac.res;

import Jama.Matrix;
import kovac.res.enums.Axis;
import kovac.res.util.LinkedViewersUtil;
import kovac.shapes.EllipsoidOverlay;

public class EllipsoidTransformations {

	private Matrix matSR;
	private double[] center;

	private Matrix scalingMatrix;

	private Matrix newMatSR;
	private double[] newCenter;

	private EllipsoidOverlay ellipsoid;

	public EllipsoidTransformations(Matrix matSR, double[] center, EllipsoidOverlay o) {
		this.matSR = matSR;
		this.center = center;
		this.ellipsoid = o;

		newMatSR = this.matSR;
		newCenter = this.center;

		scalingMatrix = new Matrix(3, 3);
	}

	public void applyTranslation(double x, double y, double z) {
		newCenter[0] += x;
		newCenter[1] += y;
		newCenter[2] += z;
	}

	public void applyScaling(double s, Axis... axis) {
		for (Axis a : axis) {
			switch (a) {
			case X:
				scalingMatrix.set(0, 0, s);
				break;
			case Y:
				scalingMatrix.set(1, 1, s);
				break;
			case Z:
				scalingMatrix.set(2, 2, s);
				break;
			default:
				break;
			}
		}
		newMatSR = scalingMatrix.times(newMatSR);
	}

	public void applyRotation(double alpha, Axis... axis) {
		Matrix[] rotations = new Matrix[3];
		for (int i = 0; i < rotations.length; i++) {
			rotations[i] = Matrix.identity(3, 3);
		}

		Matrix rotX = getRotationMatrix(Axis.X, alpha);
		Matrix rotY = getRotationMatrix(Axis.Y, alpha);
		Matrix rotZ = getRotationMatrix(Axis.Z, alpha);

		Matrix finalRotation = rotX.times(rotY).times(rotZ);

		newMatSR = finalRotation.times(newMatSR);
	}

	public static Matrix getRotationMatrix(Axis a, double alpha) {
		double[][] rotArray = new double[3][3];	
		switch (a) {
		case X:
			rotArray[0][0] = 1;
			rotArray[1][1] = Math.cos(alpha);
			rotArray[1][2] = - Math.sin(alpha);
			rotArray[2][1] = Math.sin(alpha);
			rotArray[2][2] = Math.cos(alpha);
			break;
		case Y:
			rotArray[1][1] = 1;
			rotArray[0][0] = Math.cos(alpha);
			rotArray[2][0] = - Math.sin(alpha);
			rotArray[0][2] = Math.sin(alpha);
			rotArray[2][2] = Math.cos(alpha);
			break;
		case Z:
			rotArray[2][2] = 1;
			rotArray[0][0] = Math.cos(alpha);
			rotArray[0][1] = - Math.sin(alpha);
			rotArray[1][0] = Math.sin(alpha);
			rotArray[1][1] = Math.cos(alpha);
			break;
		default:
			for (int i = 0 ; i < rotArray.length ; i++) 
				rotArray[i][i] = 1;
			break;
		}
		
		return new Matrix(rotArray);
		
	}

	public Matrix getNewMatrix() {
		return newMatSR;
	}

	public double[] getNewCenter() {
		return newCenter;
	}

	public void cancel() {
		newMatSR = matSR;
		newCenter = center;
	}

	public void validate() {
		ellipsoid.applyTransformation();
		LinkedViewersUtil.getOrthCanvas().repaint();
	}

}
