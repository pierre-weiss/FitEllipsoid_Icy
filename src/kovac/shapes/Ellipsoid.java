package kovac.shapes;

import java.util.List;
import java.util.UUID;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import Jama.Matrix;
import icy.gui.dialog.MessageDialog;
import icy.util.XMLUtil;
import kovac.maths.EllipsoidAlgorithm;
import kovac.res.quadric.QuadricExpression;
import kovac.res.util.LinkedViewersUtil;
import kovac.saving.SavingStatic;

/**
 * This class is here to represent an ellipsoid
 * 
 * @author bastien.kovac
 * 
 */
public class Ellipsoid {

	private static final String ID_ELLIPSOID = "ellipsoid";
	private static final String ID_ID = "id";
	private static final String ID_NAME = "name";
	private static final String ID_CENTER = "center";
	private static final String[] ID_AXIS = new String[] { "x", "y", "z" };
	private static final String ID_VALUE = "value";
	private static final String ID_DIRECTION_VECTORS = "directionVectors";
	private static final String ID_VECTOR = "vector";
	private static final String ID_COEFF = "coefficient";
	private static final String ID_SEMI = "axisSemiLength";
	private static final String ID_VOLUME = "volume";
	private static final String ID_QUADRIC = "quadricExpression";
	private static final String ID_QUADRIC_MICRO = "quadricExpressionMicrometers";
	private static final String[] ID_QUADRIC_COEFFS = new String[] { "a1", "a2", "a3", "a4", "a5", "a6", "a7", "a8",
			"a9", "a10" };

	private String uniqueID;
	private double[] semiLength;
	private QuadricExpression quadric, quadricMicro;
	private Matrix vectors;
	private double[] center;
	private double[] bounds;

	/**
	 * The name of the ellipsoid
	 */
	private String name;

	public Ellipsoid(QuadricExpression quadric) {

		this.quadric = quadric;

		this.quadric.getRealParameters();

		this.semiLength = this.quadric.getSemiAxis();
		this.vectors = this.quadric.getAxisVector();
		Matrix centerMat = this.quadric.getCenterMat();
		this.center = new double[] { centerMat.get(0, 0), centerMat.get(1, 0), centerMat.get(2, 0) };

		uniqueID = UUID.randomUUID().toString();

	}

	public Ellipsoid(String uniqueID) {
		this.uniqueID = uniqueID;
	}

	/**
	 * @return The volume of the ellipsoid
	 */
	public double getVolume(double[] semiLength) {
		return ((4 / 3) * Math.PI * semiLength[0] * semiLength[1] * semiLength[2]);
	}

	/**
	 * Sets the name of the ellipsoid
	 * 
	 * @param name
	 *            The name to give to the ellipsoid
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @return The name of the ellipsoid
	 */
	public String getName() {
		return this.name;
	}

	public double[] getCenter() {
		return this.center;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((uniqueID == null) ? 0 : uniqueID.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Ellipsoid other = (Ellipsoid) obj;
		if (uniqueID == null) {
			if (other.uniqueID != null)
				return false;
		} else if (!uniqueID.equals(other.uniqueID))
			return false;
		return true;
	}

	public double[] getSemiLength() {
		return semiLength;
	}

	public QuadricExpression getQuadric() {
		return quadric;
	}

	public void regenerate() {
		if (!LinkedViewersUtil.areSet())
			return;
		EllipsoidAlgorithm dummyAlgo = new EllipsoidAlgorithm();
		EllipsoidOverlay e = dummyAlgo.generateEllipsoid(quadric, quadricMicro);
		e.setName(name);
		e.setEllipsoid(this);
		LinkedViewersUtil.addOverlayToVTK(e);
	}

	public boolean saveEllipsoidToXML(Node node) {
		if (node == null)
			return false;

		List<Element> previousEllipsoids = XMLUtil.getElements(node, ID_ELLIPSOID);

		for (Element e : previousEllipsoids) {
			String id = XMLUtil.getAttributeValue(e, ID_ID, "");
			if (id.equals(uniqueID)) {
				return true;
			}
		}

		Element ellipsoid = XMLUtil.addElement(node, ID_ELLIPSOID);
		XMLUtil.setAttributeValue(ellipsoid, ID_ID, uniqueID);

		XMLUtil.addElement(ellipsoid, ID_NAME, name);

		// Center
		Element center = XMLUtil.addElement(ellipsoid, ID_CENTER);
		for (int i = 0; i < ID_AXIS.length; i++) {
			Element axis = XMLUtil.addElement(center, ID_AXIS[i]);
			XMLUtil.setAttributeDoubleValue(axis, ID_VALUE, this.center[i]);
		}

		Element directionVectors = XMLUtil.addElement(ellipsoid, ID_DIRECTION_VECTORS);

		for (int i = 0; i < 3; i++) {
			Element vector = XMLUtil.addElement(directionVectors, ID_VECTOR);
			for (int j = 0; j < ID_AXIS.length; j++) {
				Element axis = XMLUtil.addElement(vector, ID_AXIS[j]);
				XMLUtil.setAttributeDoubleValue(axis, ID_COEFF, vectors.get(j, i));
			}
		}

		Element axisLength = XMLUtil.addElement(ellipsoid, ID_SEMI);
		for (int i = 0; i < 3; i++) {
			Element value = XMLUtil.addElement(axisLength, ID_AXIS[i]);
			XMLUtil.setDoubleValue(value, semiLength[i]);
		}

		Element volume = XMLUtil.addElement(ellipsoid, ID_VOLUME);
		XMLUtil.setAttributeDoubleValue(volume, ID_VALUE, getVolume(semiLength));

		Element quadric = XMLUtil.addElement(ellipsoid, ID_QUADRIC);
		for (int i = 0; i < ID_QUADRIC_COEFFS.length; i++) {
			Element quadricCoeff = XMLUtil.addElement(quadric, ID_QUADRIC_COEFFS[i]);
			XMLUtil.setAttributeDoubleValue(quadricCoeff, ID_VALUE, this.quadric.getSimpleArray()[i]);
		}
		
		if (quadricMicro == null) {
			MessageDialog.showDialog("One or more ellipsoids could only be saved in pixel units");
			System.out.println(name + " couldn't only be saved in pixel units");
			return true;
		}

		Element quadricMicro = XMLUtil.addElement(ellipsoid, ID_QUADRIC_MICRO);
		for (int i = 0; i < ID_QUADRIC_COEFFS.length; i++) {
			Element quadricCoeff = XMLUtil.addElement(quadricMicro, ID_QUADRIC_COEFFS[i]);
			XMLUtil.setAttributeDoubleValue(quadricCoeff, ID_VALUE, this.quadricMicro.getSimpleArray()[i]);
		}

		return true;
	}

	public boolean loadEllipsoidFromXML(Node node) {
		if (node == null)
			return false;

		this.name = XMLUtil.getElementValue(node, ID_NAME, "");

		Element quadric = XMLUtil.getElement(node, ID_QUADRIC);
		try {
			Element quadricMicro = XMLUtil.getElement(node, ID_QUADRIC_MICRO);
			List<Element> coeffsMicro = XMLUtil.getElements(quadricMicro);
			
			double[] quadricCoeffsMicro = new double[10];
			
			for (int i = 0; i < coeffsMicro.size(); i++) {
				quadricCoeffsMicro[i] = XMLUtil.getAttributeDoubleValue(coeffsMicro.get(i), ID_VALUE, 0);
			}
			
			this.quadricMicro = new QuadricExpression(quadricCoeffsMicro);
			
		} catch (NullPointerException e) {
			MessageDialog.showDialog("One or more ellipsoids could only be loaded in pixel units");
			System.out.println(name + " could only be loaded in pixel units");
		}

		double[] quadricCoeffs = new double[10];
		

		List<Element> coeffs = XMLUtil.getElements(quadric);
		

		for (int i = 0; i < coeffs.size(); i++) {
			quadricCoeffs[i] = XMLUtil.getAttributeDoubleValue(coeffs.get(i), ID_VALUE, 0);
		}

		this.quadric = new QuadricExpression(quadricCoeffs);

		this.quadric.getRealParameters();

		this.semiLength = this.quadric.getSemiAxis();
		this.vectors = this.quadric.getAxisVector();
		Matrix centerMat = this.quadric.getCenterMat();
		this.center = new double[] { centerMat.get(0, 0), centerMat.get(1, 0), centerMat.get(2, 0) };

		SavingStatic.saveEllipsoid(this, uniqueID);

		return true;
	}

	public String printAsCSV() {
		String res = "";

		res += (name + "; Pixels ;");
		for (int i = 0; i < 3; i++) {
			res += (center[i]) + ";";
		}

		double[] vectorsArray = vectors.getColumnPackedCopy();

		for (int i = 0; i < vectorsArray.length; i++) {
			res += (vectorsArray[i]) + ";";
		}

		for (int i = 0; i < semiLength.length; i++) {
			res += (semiLength[i]) + ";";
		}

		res += (getVolume(semiLength)) + "\n";

		res += "; Micrometers ;";
		
		quadricMicro.getRealParameters();

		Matrix centerMicroMat = quadricMicro.getCenterMat();
		double[] centerMicro = new double[] { centerMicroMat.get(0, 0), centerMicroMat.get(1, 0),
				centerMicroMat.get(2, 0) };
		
		Matrix vectorsMicro = quadricMicro.getAxisVector();
		double[] semiLengthMicro = quadricMicro.getSemiAxis();

		for (int i = 0; i < 3; i++) {
			res += (centerMicro[i]) + ";";
		}

		double[] vectorsArrayMicro = vectorsMicro.getColumnPackedCopy();

		for (int i = 0; i < vectorsArrayMicro.length; i++) {
			res += (vectorsArrayMicro[i]) + ";";
		}

		for (int i = 0; i < semiLengthMicro.length; i++) {
			res += (semiLengthMicro[i]) + ";";
		}

		res += (getVolume(semiLengthMicro)) + "\n\n";

		return res;
	}

	public double[] getBounds() {
		return this.bounds;
	}

	public void setBounds(double[] bounds) {
		this.bounds = bounds;
	}

	public void setMicroQuadric(QuadricExpression micro) {
		this.quadricMicro = micro;
	}

}