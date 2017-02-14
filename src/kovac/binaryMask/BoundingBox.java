package kovac.binaryMask;

import kovac.res.quadric.QuadricExpression;
import kovac.shapes.Ellipsoid;

public class BoundingBox {

	private Ellipsoid baseEllipsoid;
	private double[] bounds;

	public BoundingBox(Ellipsoid baseEllipsoid) {
		this.baseEllipsoid = baseEllipsoid;
	}

	private void defineBounds() {

		bounds = baseEllipsoid.getBounds();
	}

	public double[] getBounds() {
		if (bounds == null)
			defineBounds();
		return bounds;
	}

	public QuadricExpression getAssociatedEquation() {
		return this.baseEllipsoid.getQuadric();
	}

}
