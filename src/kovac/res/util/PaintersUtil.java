package kovac.res.util;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;

import Jama.Matrix;

import icy.sequence.DimensionId;
import icy.type.point.Point3D;
import kovac.res.Points;
import kovac.res.Points.PointInSpace;
import kovac.res.enums.Methods;
import kovac.saving.SavingStatic;
import kovac.shapes.Ellipsoid;
import plugins.weiss.fitellipsoid.fitellipsoid;

public class PaintersUtil {

	private static Shape eraserZone;

	public static void paintIntersection(Graphics2D g2, DimensionId dim, double[] truePos) {
		ArrayList<Ellipsoid> ellipsoids = new ArrayList<Ellipsoid>(SavingStatic.getAllEllipsoids());

		double x,y,z;
		double a11,a12,a13,a23,a22,a33,b1,b2,b3,c;
		double b1b,b2b,b3b,cb;
		
		x=truePos[0];y=truePos[1];z=truePos[2];
		
		Font myFont = new Font("Serif", Font.ITALIC, 4);
		g2.setFont(myFont);
		
		switch (dim) {		
		case X:	
			for (Ellipsoid e : ellipsoids) {			
				Matrix M=e.getQuadric().getCoefficients();
				a11=M.get(0,0);a22=M.get(1,0);a33=M.get(2,0);
				a12=M.get(3,0);a13=M.get(4,0);a23=M.get(5,0);
				b1=M.get(6,0);b2=M.get(7,0);b3=M.get(8,0);
				c=M.get(9, 0);
				
				// In the YZ plane, the ellipse implicit equation is J(y,z) = a22 y^2 + a33 z^2 + 2a23 yz + b2b y + b3b z + cb = 0
				b2b=b2+2*a12*x;
				b3b=b3+2*a13*x;
				cb=c+a11*x*x+b1*x;
				
				// Does this ellipse intersect the YZ plane at X=x?
				double ym,zm;
				ym=-1.0/(a22*a33-a23*a23)*(a33*b2b/2-a23*b3b/2);
				zm=-1.0/(a22*a33-a23*a23)*(-a23*b2b/2+a22*b3b/2);
				double val = a22*ym*ym + a33*zm*zm + 2*a23*ym*zm + b2b*ym + b3b*zm + cb;

				if (val<=-1e-10){
					// Explanation of code in Case Z
					double D=Math.sqrt((a22-a33)*(a22-a33)+4*a23*a23);
					double sigma1=0.5*(a22+a33+D);
					double sigma2=0.5*(a22+a33-D);
					
					double r2 = (a22*ym+a23*zm)*ym+(a23*ym+a33*zm)*zm - cb; //r^2 = <Az,z> - c
					double l1=Math.sqrt(r2 / sigma1);
					double l2=Math.sqrt(r2 / sigma2);
					
					double[] v1={2.0*a23,a33-a22+D};
					double theta = Math.atan2(-v1[1],v1[0])-Math.PI/2;

					Shape ellipse = new Ellipse2D.Double(-l1,-l2, 2*l1, 2*l2);
					ellipse=AffineTransform.getRotateInstance(theta).createTransformedShape(ellipse);
					ellipse=AffineTransform.getTranslateInstance(zm,ym).createTransformedShape(ellipse);
					g2.draw(ellipse);
					g2.fill(ellipse);
					
					String numberOnly= e.getName().replaceAll("[^0-9]", "");
			        g2.drawString(numberOnly, (int) zm, (int) ym);
				}
			}		
			
			break;
		case Y:		
			for (Ellipsoid e : ellipsoids) {	
				Matrix M=e.getQuadric().getCoefficients();
				a11=M.get(0,0);a22=M.get(1,0);a33=M.get(2,0);
				a12=M.get(3,0);a13=M.get(4,0);a23=M.get(5,0);
				b1=M.get(6,0);b2=M.get(7,0);b3=M.get(8,0);
				c=M.get(9, 0);
				
				// In the XZ plane, the ellipse implicit equation is J(x,z) = a11 x^2 + a33 z^2 + 2a13 xz + b1b x + b3b z + cb = 0
				b1b=b1+2*a12*y;
				b3b=b3+2*a23*y;
				cb=c+a22*y*y+b2*y;
				
				// Does this ellipse intersect the XZ plane at Y=y?
				double xm,zm;
				xm=-1.0/(a11*a33-a13*a13)*(a33*b1b/2-a13*b3b/2);
				zm=-1.0/(a11*a33-a13*a13)*(-a13*b1b/2+a11*b3b/2);
				double val = a11*xm*xm + a33*zm*zm + 2*a13*xm*zm + b1b*xm + b3b*zm + cb;

				if (val<=-1e-10){
					// Explanation of code in Case Z
					double D=Math.sqrt((a11-a33)*(a11-a33)+4*a13*a13);
					double sigma1=0.5*(a11+a33+D);
					double sigma2=0.5*(a11+a33-D);
					
					double r2 = (a11*xm+a13*zm)*xm+(a13*xm+a33*zm)*zm - cb; //r^2 = <Az,z> - c
					double l1=Math.sqrt(r2 / sigma1);
					double l2=Math.sqrt(r2 / sigma2);
					
					double[] v1={2.0*a13,a33-a11+D};					
					double theta = -Math.atan2(-v1[1],v1[0]);

					Shape ellipse = new Ellipse2D.Double(-l1,-l2, 2*l1, 2*l2);
					ellipse=AffineTransform.getRotateInstance(theta).createTransformedShape(ellipse);
					ellipse=AffineTransform.getTranslateInstance(xm, zm).createTransformedShape(ellipse);
					g2.draw(ellipse);
					g2.fill(ellipse);
					
					String numberOnly= e.getName().replaceAll("[^0-9]", "");
			        g2.drawString(numberOnly, (int) xm, (int) zm);
				}
			}
			
			break;
		case Z:						
			for (Ellipsoid e : ellipsoids) {		
				Matrix M=e.getQuadric().getCoefficients();
				a11=M.get(0,0);a22=M.get(1,0);a33=M.get(2,0);
				a12=M.get(3,0);a13=M.get(4,0);a23=M.get(5,0);
				b1=M.get(6,0);b2=M.get(7,0);b3=M.get(8,0);
				c=M.get(9, 0);
				
				// In the XY plane, the ellipse implicit equation is J(x,y) = a11 x^2 + a22 y^2 + 2a12 xy + b1b x + b2b y + cb = 0
				b1b=b1+2*a13*z;
				b2b=b2+2*a23*z;
				cb=c+a33*z*z+b3*z;
				
				// Does this ellipse intersect the XY plane at Z=z?
				// To answer this, we find the minimum of J(x,y)
				// This amounts to solving a11 x + a12 y = -b1b/2 and a12 x + a22 y = -b2b/2 
				double xm,ym;
				xm=-1.0/(a11*a22-a12*a12)*(a22*b1b/2-a12*b2b/2);
				ym=-1.0/(a11*a22-a12*a12)*(-a12*b1b/2+a11*b2b/2);
				double val = a11*xm*xm + a22*ym*ym + 2*a12*xm*ym + b1b*xm + b2b*ym + cb;

				if (val<=-1e-10){
					// Now, we know that the ellipse is all the points (x,y) such that
					// a11*x*x + a22*y*y + 2*a12*x*y + b1b*x + b2b*y + cb = 0
					// What does this mean in terms of center, axes lengths and rotation angle?
					// First, notice that center is (xm,ym)
					// Then Length of each axis l1>=l2>=0
					double D=Math.sqrt((a11-a22)*(a11-a22)+4*a12*a12);
					double sigma1=0.5*(a11+a22+D);
					double sigma2=0.5*(a11+a22-D);
					
					double r2 = (a11*xm+a12*ym)*xm+(a12*xm+a22*ym)*ym - cb; //r^2 = <Az,z> - c
					double l1=Math.sqrt(r2 / sigma1);
					double l2=Math.sqrt(r2 / sigma2);
					
					//First eigenvector's direction is given by v1=(2c, b-a+D)
					double[] v1={2.0*a12,a22-a11+D};
					double theta = -Math.atan2(-v1[1],v1[0]);

					// Now we have everything to draw the ellipse with Java!
					Shape ellipse = new Ellipse2D.Double(-l1,-l2, 2*l1, 2*l2);
					ellipse=AffineTransform.getRotateInstance(theta).createTransformedShape(ellipse);
					ellipse=AffineTransform.getTranslateInstance(xm, ym).createTransformedShape(ellipse);
					g2.draw(ellipse);
					g2.fill(ellipse);
					
					String numberOnly= e.getName().replaceAll("[^0-9]", "");
			        g2.drawString(numberOnly, (int) xm, (int) ym);
				}
				
			}
			break;
		default:
			break;
		}
		

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
		if (fitellipsoid.getChosenMethod() == Methods.POINTS) {
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
