package kovac.res.util;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import icy.sequence.DimensionId;
import icy.type.point.Point3D;
import kovac.res.Points;
import kovac.res.Points.PointInSpace;
import kovac.res.enums.Methods;
import plugins.weiss.segmentation3d.Segmentation3D;

public class PaintersUtil {

	private static Shape eraserZone;

	public static void paintIntersection(Graphics2D g2, DimensionId currentDimension, double[] truePos) {

	}

	public static boolean isInsideEraser(Point3D pt, DimensionId dim) {
		if (eraserZone == null)
			return false;
		return eraserZone.contains(translatePoint(pt, dim));
	}

	public static Point2D translatePoint(Point3D pt, DimensionId dim) {
		Point2D ptReturn = null;
		double[] scale = LinkedViewersUtil.getScale();
		double minScale = MathUtils.min(scale);
		
		switch (dim) {
		case X:
			ptReturn = new Point2D.Double(pt.getZ() * (scale[2] / minScale), pt.getY() * (scale[1] / minScale));
			break;
		case Y:
			ptReturn = new Point2D.Double(pt.getX() * (scale[0] / minScale), pt.getZ() * (scale[2] / minScale));
			break;
		case Z:
			ptReturn = new Point2D.Double(pt.getX() * (scale[0] / minScale), pt.getY() * (scale[1] / minScale));
			break;
		default:
			break;
		}
		
		return ptReturn;
	}

	public static void paintPoints(Graphics2D g2, DimensionId currentDimension, double[] truePos) {
		if (Segmentation3D.getChosenMethod() == Methods.POINTS) {
			g2.setColor(Color.RED);
			Rectangle2D point = null;
			double[] scale = LinkedViewersUtil.getScale();
			double minScale = MathUtils.min(scale);
			for (PointInSpace pt : Points.getAllPoints()) {
				if (pt.getDim() == currentDimension) {
					switch (currentDimension) {
					case X:
						if (pt.getPoint().getX() != truePos[0])
							break;
						point = new Rectangle2D.Double(pt.getPoint().getZ() * (scale[2] / minScale), 
								pt.getPoint().getY() * (scale[1] / minScale), 1, 1);
						break;
					case Y:
						if (pt.getPoint().getY() != truePos[1])
							break;
						point = new Rectangle2D.Double(pt.getPoint().getX() * (scale[0] / minScale), 
								pt.getPoint().getZ() * (scale[2] / minScale), 1, 1);
						break;
					case Z:
						if (pt.getPoint().getZ() != truePos[2])
							break;
						point = new Rectangle2D.Double(pt.getPoint().getX() * (scale[0] / minScale), 
								pt.getPoint().getY() * (scale[1] / minScale), 1, 1);
						break;
					default:
						break;
					}
					if (point != null) {
						g2.draw(point);
						g2.fill(point);
					}
				}
			}
		} else {
			// TODO : ellipses
		}
	}

	public static void paintEraser(Graphics2D g2, DimensionId currentDimension, Point2D startDrag, Point2D endDrag,
			Point2D crossPosition) {
		g2.setColor(Color.LIGHT_GRAY);
		g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.50f));
		if (startDrag == null || endDrag == null) {
			if (crossPosition == null)
				return;
			Shape lineHorizontal = new Line2D.Double(crossPosition.getX() - 2, crossPosition.getY(),
					crossPosition.getX() + 2, crossPosition.getY());
			Shape lineVertical = new Line2D.Double(crossPosition.getX(), crossPosition.getY() - 2, crossPosition.getX(),
					crossPosition.getY() + 2);
			g2.draw(lineVertical);
			g2.draw(lineHorizontal);
			return;
		}
		makeRectangle(startDrag.getX(), startDrag.getY(), endDrag.getX(), endDrag.getY());
		g2.draw(eraserZone);
		g2.setColor(Color.WHITE);
		g2.fill(eraserZone);
	}

	public static void eraseRectangle() {
		eraserZone = null;
	}

	private static void makeRectangle(double x1, double y1, double x2, double y2) {
		eraserZone = new Rectangle2D.Double(Math.min(x1, x2), Math.min(y1, y2), Math.abs(x1 - x2), Math.abs(y1 - y2));
	}

}
