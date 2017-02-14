package kovac.shapes;

import Jama.Matrix;
import Jama.SingularValueDecomposition;
import icy.sequence.DimensionId;
import kovac.maths.EllipsoidAlgorithm;

/**
 * This class is here to represent an Ellipse
 * 
 * @author bastien.kovac
 * 
 */
public class Ellipse {

	/**
	 * The (x;y) coordinates of the center
	 */
	private double xCenter, yCenter;
	/**
	 * In usual notation, semiWidth represents a and semiHeight represents b
	 */
	private double semiWidth, semiHeight;
	/**
	 * The inclination angle of the ellipse
	 */
	private double angleWithAxis;
	/**
	 * The dimension in which the ellipse is contained (Meaning the plane from
	 * the OrthoView -> (X,Y), (X,Z) or (Y,Z))
	 */
	private DimensionId dim;
	private boolean isGenerated;
	private double[] quadCoeffs;

	public Ellipse(double[] quadCoeffs) {
		this.quadCoeffs = quadCoeffs;
	}

	/**
	 * @return The dimension (plane) in which the ellipse is contained
	 */
	public DimensionId getDim() {
		return dim;
	}

	/**
	 * Sets the ellipse defining dimension
	 * 
	 * @param dim
	 *            The dimension (plane) in which the ellipse is contained
	 */
	public void setDim(DimensionId dim) {
		this.dim = dim;
	}

	private void generateParams() {

		Matrix matEll = new Matrix(2, 2);
		matEll.set(0, 0, quadCoeffs[0]);
		matEll.set(0, 1, quadCoeffs[2] / 2);

		matEll.set(1, 0, quadCoeffs[2] / 2);
		matEll.set(1, 1, quadCoeffs[1]);

		Matrix lastCoeffs = new Matrix(2, 1);
		lastCoeffs.set(0, 0, quadCoeffs[3] / 2);
		lastCoeffs.set(1, 0, quadCoeffs[4] / 2);

		Matrix centerMat = EllipsoidAlgorithm.proxf1(matEll, lastCoeffs.times(-1.0));

		xCenter = centerMat.get(0, 0);
		yCenter = centerMat.get(1, 0);
		
		Matrix scalarMat = matEll.times(centerMat);
		scalarMat = centerMat.transpose().times(scalarMat);
		double scalar = scalarMat.getColumnPackedCopy()[0];

		double alpha = 1 / (scalar - quadCoeffs[5]);
		matEll = matEll.times(alpha);

		SingularValueDecomposition svd = matEll.svd();

		Matrix semiLength = svd.getS();
		semiWidth = Math.sqrt(1 / semiLength.get(0, 0));
		semiHeight = Math.sqrt(1 / semiLength.get(1, 1));

		Matrix vectors = svd.getU();

		double xCoeff = vectors.get(0, 0);
		double yCoeff = vectors.get(1, 0);
		
		angleWithAxis = Math.atan(yCoeff / xCoeff);
		if (dim == DimensionId.X) {
			angleWithAxis = Math.atan(xCoeff / yCoeff);
			xCenter = centerMat.get(1, 0);
			yCenter = centerMat.get(0, 0);
			
		}
		
		isGenerated = true;
	}

	/**
	 * @return All the ellipse's parameters as an array
	 */
	public double[] getParams() {
		if (!isGenerated)
			generateParams();
		return new double[] { xCenter, yCenter, semiWidth, semiHeight, angleWithAxis };
	}

}
