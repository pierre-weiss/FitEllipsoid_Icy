package kovac.tests;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang.ArrayUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import Jama.Matrix;
import icy.type.point.Point3D;
import kovac.maths.EllipsoidAlgorithm;
import kovac.res.util.MathUtils;

public class TestAlgoEllipsoid {

	double[][] points;
	double[][] pointsReal;
	double[] expectedCenterOfMass;
	Matrix pointsMat;
	Matrix realMat;

	double[] testSum;
	double[] testAvg;

	@Before
	public void runBeforeEveryTest() {

		points = new double[][] { { 1, -1, 0, 0, 0, 0, 1, 1, 1, -1, -1, 1, -1, -1 },
				{ 0, 0, 1, -1, 0, 0, 1, 1, -1, 1, -1, -1, 1, -1 }, { 0, 0, 0, 0, -1, 1, 1, -1, 1, 1, 1, -1, -1, -1 } };

		pointsReal = new double[][] {
				{ 0.9830, -2.0564, -1.6455, -0.1094, -0.9148, -1.4511, 2.1918, 1.4543, -3.6706, -1.1111 },
				{ 0.9389, 4.0447, 5.0776, -0.4229, 3.6163, -1.0237, -4.2298, -2.6732, 3.9565, -2.8577 },
				{ -2.0251, -0.2228, -2.3638, 1.8667, -4.1673, 0.7226, -0.6484, -0.0602, -1.1241, 3.5619 } };

		pointsMat = new Matrix(points);
		realMat = new Matrix(pointsReal);

		testSum = new double[] { 1.5, 2.5, 3.5, 4.0, 5.0, 6.3 };
		testAvg = testSum;

		expectedCenterOfMass = new double[] { 0, 0, 0 };

	}

	@After
	public void clear() {
		points = null;
	}

	@Test
	public void testSum() {
		assertEquals(1.5 + 2.5 + 3.5 + 4.0 + 5.0 + 6.3, MathUtils.sum(testSum), 0);
	}

	@Test
	public void testAvg() {
		assertEquals((1.5 + 2.5 + 3.5 + 4.0 + 5.0 + 6.3) / 6, MathUtils.avg(testAvg), 0);
	}

	@Test
	public void testCenterOfMass() {
		assertArrayEquals(expectedCenterOfMass, MathUtils.getCenterOfMass(pointsMat), 0);
	}

	@Test
	public void testProjsplx() {
		double[] input = new double[] { 1, 2, 3, 4, 5, 6 };
		List<Double> listInput = Arrays.asList(ArrayUtils.toObject(input));
		double[] expected = new double[] { 0, 0, 0, 0, 0, 1 };
		List<Double> res = EllipsoidAlgorithm.projsplx(listInput);
		if (res.size() != expected.length)
			fail("Wrong length");
		for (int i = 0; i < expected.length; i++) {
			assertEquals(expected[i], res.get(i), 0);
		}
	}

	@Test
	public void testGetK() {
		double[][] expectedK = new double[][] { { 10, 8, 8, 0, 0, 0, 0, 0, 0, 10 }, { 8, 10, 8, 0, 0, 0, 0, 0, 0, 10 },
				{ 8, 8, 10, 0, 0, 0, 0, 0, 0, 10 }, { 0, 0, 0, 8, 0, 0, 0, 0, 0, 0 }, { 0, 0, 0, 0, 8, 0, 0, 0, 0, 0 },
				{ 0, 0, 0, 0, 0, 8, 0, 0, 0, 0 }, { 0, 0, 0, 0, 0, 0, 10, 0, 0, 0 }, { 0, 0, 0, 0, 0, 0, 0, 10, 0, 0 },
				{ 0, 0, 0, 0, 0, 0, 0, 0, 10, 0 }, { 10, 10, 10, 0, 0, 0, 0, 0, 0, 14 } };
		for (int i = 0; i < 10; i++)
			assertArrayEquals(expectedK[i], EllipsoidAlgorithm.getK(pointsMat).getArray()[i], 0);
	}

	@Test
	public void testCalculateM() {
		double[][] expectedM = new double[][] { { 1.1, 0.08, 0.08, 0, 0, 0, 0, 0, 0, 0.1 },
				{ 0.08, 1.1, 0.08, 0, 0, 0, 0, 0, 0, 0.1 }, { 0.08, 0.08, 1.1, 0, 0, 0, 0, 0, 0, 0.1 },
				{ 0, 0, 0, 1.08, 0, 0, 0, 0, 0, 0 }, { 0, 0, 0, 0, 1.08, 0, 0, 0, 0, 0 },
				{ 0, 0, 0, 0, 0, 1.08, 0, 0, 0, 0 }, { 0, 0, 0, 0, 0, 0, 1.1, 0, 0, 0 },
				{ 0, 0, 0, 0, 0, 0, 0, 1.1, 0, 0 }, { 0, 0, 0, 0, 0, 0, 0, 0, 1.1, 0 },
				{ 0.1, 0.1, 0.1, 0, 0, 0, 0, 0, 0, 1.14 } };
		for (int i = 0; i < 10; i++)
			assertArrayEquals(expectedM[i], EllipsoidAlgorithm.getM(EllipsoidAlgorithm.getK(pointsMat)).getArray()[i],
					0.0001);
	}

	@Test
	public void testProxf2() {
		Matrix u = EllipsoidAlgorithm.getQ0(pointsMat);
		for (int i = 0; i < u.getRowDimension(); i++) {
			assertArrayEquals(u.getArray()[i], EllipsoidAlgorithm.proxf2(u).getArray()[i], 0.0001);
		}
	}

	@Test
	public void testProxf1() {
		double[][] expectedProxf1 = new double[][] { { 0.3249 }, { 0.3249 }, { 0.3249 }, { 0 }, { 0 }, { 0 }, { 0 },
				{ 0 }, { 0 }, { -0.7603 } };
		for (int i = 0; i < expectedProxf1.length; i++)
			assertArrayEquals(expectedProxf1[i],
					EllipsoidAlgorithm.proxf1(EllipsoidAlgorithm.getM(EllipsoidAlgorithm.getK(pointsMat)),
							EllipsoidAlgorithm.getQ0(pointsMat)).getArray()[i],
					0.0001);
	}

	@Test
	public void testDiagSquare() {
		double[][] expectedDiag = new double[][] { { 1, 5, 9 } };
		double[][] baseArray = new double[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } };
		Matrix baseMat = new Matrix(baseArray);
		assertArrayEquals(expectedDiag[0], EllipsoidAlgorithm.diag(baseMat).transpose().getArray()[0], 0.0001);
	}

	@Test
	public void testDiagColumnVector() {
		double[][] baseArray = new double[][] { { 1 }, { 5 }, { 9 } };
		Matrix baseMat = new Matrix(baseArray);
		double[][] expectedArray = new double[][] { { 1, 0, 0 }, { 0, 5, 0 }, { 0, 0, 9 } };
		for (int i = 0; i < 3; i++) {
			assertArrayEquals(expectedArray[i], EllipsoidAlgorithm.diag(baseMat).getArray()[i], 0.0001);
		}
	}

	@Test
	public void testGetQ0() {
		double[][] expectedResult = new double[][] { { 0.3333 }, { 0.3333 }, { 0.3333 }, { 0 }, { 0 }, { 0 }, { 0 },
				{ 0 }, { 0 }, { -0.7692 } };
		for (int i = 0; i < 10; i++)
			assertArrayEquals(expectedResult[i], EllipsoidAlgorithm.getQ0(pointsMat).getArray()[i], 0.0001);
	}

	@Test
	public void testVariance() {
		double[] expectedVar = new double[] { 0.7692, 0.7692, 0.7692 };
		double[] results = new double[] { MathUtils.getVariance(points[0]), MathUtils.getVariance(points[1]),
				MathUtils.getVariance(points[2]) };
		assertArrayEquals(expectedVar, results, 0.0001);
	}

	@Test
	public void testSumSquare() {
		double expectedResult = 3.3 * 3.3 + 17.5 * 17.5 + 2.4 * 2.4;
		double[] input = new double[] { 3.3, 17.5, 2.4 };
		assertEquals(expectedResult, MathUtils.sumSquare(input), 0);
	}
	
	@Test
	public void testFinal() {
		List<Point3D> basePoints = new ArrayList<Point3D>();
		for (int j = 0; j < pointsReal[0].length; j++) {
			basePoints.add(new Point3D.Double(pointsReal[0][j], pointsReal[1][j], pointsReal[2][j]));
		}
		EllipsoidAlgorithm algo = new EllipsoidAlgorithm(basePoints);
		double[][] expectedResult = new double[][] { { 0.6198 }, { 0.2149 }, { 0.1654 }, { 0.5952 }, { 0.4044 },
				{ 0.2441 }, { 1.1095 }, { 0.5197 }, { 0.5173 }, { -1.1745 } };
		double[][] realResult = algo.getFinalQuadric().getCoefficients().getArray();
		for (int i = 0; i < expectedResult.length; i++)
			assertArrayEquals(expectedResult[i], realResult[i], 0.0001);
	}

}
