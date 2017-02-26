package kovac.maths;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang.ArrayUtils;

import Jama.EigenvalueDecomposition;
import Jama.Matrix;
import icy.gui.dialog.MessageDialog;
import icy.type.point.Point3D;
import kovac.res.Points;
import kovac.res.quadric.QuadricExpression;
import kovac.res.util.LinkedViewersUtil;
import kovac.res.util.MathUtils;
import kovac.shapes.Ellipsoid;
import kovac.shapes.EllipsoidOverlay;
import vtk.vtkPoints;

/**
 * This class handles the generation of the ellipsoid fitting a set of given
 * points. It uses Singular Value Decomposition to generate the characteristic
 * Matrix of the ellipsoid, and center of mass to calculate its center.
 * 
 * 
 * @author bastien.kovac & pierre.weiss
 *
 */
public class EllipsoidAlgorithm {

	// Note that the notations for the variables have been chosen accordingly to
	// the ones used in the original Matlab code, to improve clarity during
	// debug phase

	/**
	 * Parameter for Douglas-Rachford in ]0,+infty[
	 */
	private static double gamma = 10;
	/**
	 * Maximum number of iterations of the algorithm
	 */
	private static int nbIterations = 10000;
	/**
	 * The matrix representing the base points. It is a nbPoints x 3 matrix,
	 * with each column representing a coordinate
	 */
	private Matrix basePoints;
	/**
	 * The quadratic equation of the ellipsoid
	 */
	private QuadricExpression quadricExpression, quadricExpressionMicro;
	/**
	 * The vtk object representing the real center of the ellipsoid
	 */
	private vtkPoints realCenter;
	private static double[] c;
	
	private Matrix basePointsMicro;

	/**
	 * Builds a new EllipsoidAlgorithm from a given list of three dimensional
	 * points
	 * 
	 * @param basePoints
	 *            The base points
	 */
	public EllipsoidAlgorithm(List<Point3D> basePoints) {
		this.basePoints = new Matrix(3, basePoints.size());
		this.basePointsMicro = new Matrix(3, basePoints.size());
		double[] scale = LinkedViewersUtil.getScale();
		
		
		for (int i = 0; i < basePoints.size(); i++) {			
			this.basePoints.set(0, i, basePoints.get(i).getX());
			this.basePointsMicro.set(0, i, basePoints.get(i).getX() * scale[0]);
			this.basePoints.set(1, i, basePoints.get(i).getY());
			this.basePointsMicro.set(1, i, basePoints.get(i).getY() * scale[1]);
			this.basePoints.set(2, i, basePoints.get(i).getZ());
			this.basePointsMicro.set(2, i, basePoints.get(i).getZ() * scale[2]);
		}
	}

	public EllipsoidAlgorithm() {
		
	}

	public static Matrix getQ0(Matrix baseMatrix) {
		double[] baseCenter = MathUtils.getCenterOfMass(baseMatrix);
		double[] matVariance = new double[baseMatrix.getRowDimension()];
		for (int i = 0; i < baseMatrix.getRowDimension(); i++)
			matVariance[i] = MathUtils.getVariance(baseMatrix.getArray()[i]);
		double avgRadius = MathUtils.sum(matVariance);
		double[][] baseSphereArray = new double[][] { { 1.0 / 3.0 }, { 1.0 / 3.0 }, { 1.0 / 3.0 }, { 0.0 }, { 0.0 },
				{ 0.0 }, { -2 * baseCenter[0] / 3.0 }, { -2 * baseCenter[1] / 3.0 }, { -2 * baseCenter[2] / 3.0 },
				{ (baseCenter[0] * baseCenter[0] + baseCenter[1] * baseCenter[1] + baseCenter[2] * baseCenter[2]
						- avgRadius) / 3.0 } };
		return new Matrix(baseSphereArray);
	}

	public static void WriteMatrix(String name, Matrix M){
		PrintWriter writer = null;
		try {
			writer = new PrintWriter(name, "UTF-8");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		M.print(writer, 2, 4);
		writer.flush();
		writer.close();	
	}
	
	
	
	/**
	 * Implements the Douglas-Rachford algorithm
	 */
	private QuadricExpression douglasRachford(Matrix basePoints) {
		
		// Saves the points locations for comparison with Matlab
		System.out.println("Number of points " + basePoints.getColumnDimension());
		//WriteMatrix("/media/weiss/Donnees/Works/Workspace_Eclipse/FitEllipsoid/dataPoints.txt",basePoints);
		
		// First rescales everything to be affine invariant
		c = MathUtils.getCenterOfMass(basePoints);
		double[][] shifting = new double[3][basePoints.getColumnDimension()];
		for (int i = 0; i < 3; i++) {
			for (int j = 0; j < basePoints.getColumnDimension(); j++) {
				shifting[i][j] = c[i];
			}
		}
		
		Matrix basePointsShifted = basePoints.minus(new Matrix(shifting));
		Matrix Cov = basePointsShifted.times(basePointsShifted.transpose());
		EigenvalueDecomposition eig = new EigenvalueDecomposition(Cov);
		Matrix U = eig.getV();
		Matrix S = eig.getD();
		
		for (int i=0;i<3;i++){
			S.set(i,i, Math.sqrt(1.0/(S.get(i,i)+1e-16)));
		}
		Matrix P=S.times(U.transpose());
		Matrix basePointsInvariant=P.times(basePointsShifted);
		
		// Now Douglas-Rachford
		Matrix K = getK(basePointsInvariant);
		Matrix M = getM(K);
				
		Matrix p = getQ0(basePointsShifted);
		Matrix q = null;
		for (int i = 0; i < nbIterations; i++) {
			q = proxf2(p);
			Matrix update = proxf1(M, q.times(2).minus(p)).minus(q);
			p = p.plus(update);
			if (update.norm2() < p.norm2() * 1e-8) {
				System.out.println("Ellipsoid found in " + i + " iterations");
				break;
			}
			if (i % 10000 == 0) {
				System.out.println("Current iteration : " + i);
			}
			if (i == nbIterations) {
				System.err.println("No acceptable ellipsoid could be found");
				throw new IllegalArgumentException("No acceptable ellipsoid could be found");
			}
		}
		q = proxf2(q);

		// Now goes back to the original domain
		double[] et={c[0],c[1],c[2]};
		Matrix t=new Matrix(et,3);
		
		double[][] eA2={
				{q.get(0,0),q.get(3,0)/Math.sqrt(2),q.get(4, 0)/Math.sqrt(2)},
					{q.get(3,0)/Math.sqrt(2),q.get(1,0),q.get(5,0)/Math.sqrt(2)},
					{q.get(4,0)/Math.sqrt(2),q.get(5,0)/Math.sqrt(2),q.get(2,0)}};
		Matrix A2=new Matrix(eA2);

		double[] eb2={q.get(6,0),q.get(7,0),q.get(8,0)};
		Matrix b2=new Matrix(eb2,3);

		double c2=q.get(9, 0);

		Matrix A=P.transpose().times(A2.times(P)); // P'*A2*P
		Matrix b=A.times(t.times(-2.0)); // -2*A*t+P'*b2
		b.plusEquals(P.transpose().times(b2));
		Matrix Pt=P.times(t);
		Matrix A2Pt=A2.times(Pt);
		A2Pt=A2Pt.transpose();
		Matrix c=A2Pt.times(Pt); // c= (A2*P*t)'*(P*t)-b2'*P*t+c2		
		Matrix b2t = b2.transpose();
		c.minusEquals(b2t.times(Pt));		

		// FOR MATLAB DISPLAY
		//double[] eq = {A.get(0,0),A.get(1,1),A.get(2,2),
		//		A.get(1,0)*Math.sqrt(2),A.get(2,0)*Math.sqrt(2),A.get(2,1)*Math.sqrt(2),
		//		b.get(0,0),b.get(1,0),b.get(2,0),c.get(0,0)+c2};
		
		// FOR JAVA DISPLAY
		double[] eq = {A.get(0,0),A.get(1,1),A.get(2,2),
				A.get(1,0),A.get(2,0),A.get(2,1),
				b.get(0,0),b.get(1,0),b.get(2,0),c.get(0,0)+c2};
			
	    
		Matrix Q=new Matrix(eq,10);
		Q.timesEquals(1.0/(A.get(0,0)+A.get(1,1)+A.get(2,2)));
		// The ellipsoid is given by <x,Ax> + <b,x> + c = 0 
		// A=[[Q(0),Q(3),Q(4)];[Q(3),Q(1),Q(5)];[Q(4),Q(5),Q(3)]]
		// b=[Q(6),Q(7),Q(8)]
		// c=Q(9)
		System.out.println("Finished -- ");
				
		return new QuadricExpression(Q);
	}

	/**
	 * Returns the mlDivide result for the baseSphereMatrix and the input matrix
	 * 
	 * @param q
	 *            The input matrix
	 * @param m
	 *            The other input matrix
	 * @return A result matrix A as A = M\q
	 */
	public static Matrix proxf1(Matrix M, Matrix q) {
		Matrix res = Matrix.identity(q.getRowDimension(), q.getColumnDimension());
		try {
			res =  M.solve(q);
		} catch (RuntimeException e) {
			MessageDialog.showDialog("An error has occured, please check your points configuration");
		}
		return res;
	}

	/**
	 * Function Project_On_B from matlab (VALIDATED)
	 * 
	 * @param q
	 *            Input Matrix
	 * @return a vector column
	 */
	public static Matrix proxf2(Matrix q0) {
		Matrix Q0 = new Matrix(3, 3);
		double[] arrayCopy = q0.getColumnPackedCopy();

		// Initialize q0
		Q0.set(0, 0, arrayCopy[0]);
		Q0.set(0, 1, arrayCopy[3] / Math.sqrt(2));
		Q0.set(0, 2, arrayCopy[4] / Math.sqrt(2));

		Q0.set(1, 0, arrayCopy[3] / Math.sqrt(2));
		Q0.set(1, 1, arrayCopy[1]);
		Q0.set(1, 2, arrayCopy[5] / Math.sqrt(2));

		Q0.set(2, 0, arrayCopy[4] / Math.sqrt(2));
		Q0.set(2, 1, arrayCopy[5] / Math.sqrt(2));
		Q0.set(2, 2, arrayCopy[2]);

		EigenvalueDecomposition eig = new EigenvalueDecomposition(Q0);

		Matrix U = eig.getV();
		Matrix S0 = eig.getD();

		// Diagonalize s0
		Matrix s0 = diag(S0);
		// Use projsplx on diagS0
		List<Double> asList = new ArrayList<Double>(Arrays.asList(ArrayUtils.toObject(s0.getColumnPackedCopy())));
		List<Double> result = projsplx(asList);
		Matrix s = new Matrix(result.size(), 1);
		for (int i = 0; i < result.size(); i++)
			s.set(i, 0, result.get(i));

		// Diag s
		Matrix S = diag(s);
		Matrix Q = U.times(S.times(U.transpose()));

		// Initialize final result
		Matrix q = new Matrix(q0.getRowDimension(), 1);

		q.set(0, 0, Q.get(0, 0));
		q.set(1, 0, Q.get(1, 1));
		q.set(2, 0, Q.get(2, 2));
		q.set(3, 0, Math.sqrt(2) * Q.get(1, 0));
		q.set(4, 0, Math.sqrt(2) * Q.get(2, 0));
		q.set(5, 0, Math.sqrt(2) * Q.get(2, 1));
		for (int i = 6; i < q0.getRowDimension(); i++) {
			q.set(i, 0, q0.get(i, 0));
		}

		return q;

	}

	/**
	 * Return a vector column (nx1 Matrix) corresponding to the diagonal of the
	 * input Matrix. If the input matrix is a vector column, it returns a square
	 * Matrix with these diagonal values instead (VALIDATED)
	 * 
	 * @param q
	 *            A square input matrix of n x n dimension
	 * @return The diagonal values
	 */
	public static Matrix diag(Matrix q) {
		Matrix res = null;
		if (q.getColumnDimension() == q.getRowDimension()) {
			res = new Matrix(q.getColumnDimension(), 1);
			for (int i = 0; i < q.getRowDimension(); i++) {
				res.set(i, 0, q.get(i, i));
			}
		} else {
			if (q.getColumnDimension() == 1) {
				res = new Matrix(q.getRowDimension(), q.getRowDimension());
				for (int i = 0; i < q.getRowDimension(); i++) {
					res.set(i, i, q.get(i, 0));
				}
			}

		}
		return res;
	}

	/**
	 * @return The K matrix corresponding to the points given to the algorithm
	 *         as parameters (VALIDATED)
	 */
	public static Matrix getK(Matrix basePoints) {
		// Initialize Matrix
		Matrix D = new Matrix(10, basePoints.getColumnDimension());
		for (int i = 0; i < basePoints.getColumnDimension(); i++) {
			// X^2, Y^2 and Z^2 coefficients
			D.set(0, i, basePoints.get(0, i) * basePoints.get(0, i));
			D.set(1, i, basePoints.get(1, i) * basePoints.get(1, i));
			D.set(2, i, basePoints.get(2, i) * basePoints.get(2, i));

			// XY, XZ and YZ coefficients
			D.set(3, i, Math.sqrt(2)*basePoints.get(0, i) * basePoints.get(1, i));
			D.set(4, i, Math.sqrt(2)*basePoints.get(0, i) * basePoints.get(2, i));
			D.set(5, i, Math.sqrt(2)*basePoints.get(1, i) * basePoints.get(2, i));

			// X, Y and Z coefficients
			D.set(6, i, basePoints.get(0, i));
			D.set(7, i, basePoints.get(1, i));
			D.set(8, i, basePoints.get(2, i));

			// Gamma coefficient
			D.set(9, i, 1);
		}
		// Return K
		return D.times(D.transpose());
	}

	/**
	 * Original Matlab/C code from Xiaojing Ye Algorithm explained here :
	 * http://arxiv.org/abs/1101.6081 (VALIDATED)
	 * 
	 * @param y
	 *            An input n-dimension vector
	 * @return ?
	 */
	public static List<Double> projsplx(List<Double> y) {
		boolean bget = false;
		int m = y.size();

		List<Double> sortedList = new ArrayList<Double>(y);
		Collections.sort(sortedList, Collections.reverseOrder());

		double tmpSum = 0;
		double tMax = 0;

		for (int i = 0; i < m - 1; i++) {
			tmpSum += sortedList.get(i);
			tMax = (tmpSum - 1) / (i + 1);
			if (tMax >= sortedList.get(i + 1)) {
				bget = true;
				break;
			}
		}

		if (!bget) {
			tMax = (tmpSum + sortedList.get(m - 1) - 1) / m;
		}

		for (int i = 0; i < y.size(); i++) {
			y.set(i, Math.max(y.get(i) - tMax, 0));
		}

		return y;
	}

	public static Matrix getM(Matrix K) {
		Matrix identity = new Matrix(K.getRowDimension(), K.getColumnDimension());
		for (int i = 0; i < identity.getRowDimension(); i++) {
			identity.set(i, i, 1);
		}
		Matrix M = K.times(gamma).plus(identity);
		return M;
	}
	
	public QuadricExpression getFinalQuadric() {
		if (quadricExpression == null)
			quadricExpression = douglasRachford(basePoints);
		return quadricExpression;
	}

	/**
	 * Builds and return the ellipsoid calculated by the algorithm as an Overlay
	 * 
	 * @return An overlay displaying the ellipsoid
	 */
	public EllipsoidOverlay generateEllipsoid() {
		try {
			quadricExpressionMicro = douglasRachford(basePointsMicro);
			quadricExpression = douglasRachford(basePoints);
		} catch (IllegalArgumentException e) {
			MessageDialog.showDialog("No acceptable fitting ellipsoid could be found for this set of point",
					MessageDialog.ERROR_MESSAGE);
			Points.clear();
			return new EllipsoidOverlay();
		} catch (RuntimeException e) {
			MessageDialog.showDialog("The original point configuration was not correct. Please try again");
			Points.clear();
			return new EllipsoidOverlay();
		}
		return getOverlay();
	}

	public EllipsoidOverlay generateEllipsoid(QuadricExpression quadric, QuadricExpression quadricMicro) {
		this.quadricExpression = quadric;
		this.quadricExpressionMicro = quadricMicro;
		quadricExpressionMicro.getRealParameters();
		quadricExpression.getRealParameters();
		return getOverlay();
	}
	
	private EllipsoidOverlay getOverlay() {
		quadricExpression.getRealParameters();
		realCenter = new vtkPoints();
		realCenter.InsertNextPoint(quadricExpression.getCenterMat().getColumnPackedCopy());
		EllipsoidOverlay ret = new EllipsoidOverlay(quadricExpression.getMatSR(), realCenter);
		ret.saveQuadric(quadricExpression, quadricExpressionMicro);
		ret.setEllipsoid(new Ellipsoid(quadricExpression));
		ret.setCanBeRemoved(true);
		ret.setPersistent(false);
		ret.setReadOnly(false);
		return ret;
	}
}