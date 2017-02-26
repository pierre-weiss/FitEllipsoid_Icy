package kovac.res;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import icy.painter.Overlay;
import icy.sequence.DimensionId;
import icy.type.point.Point3D;
import kovac.maths.EllipsoidAlgorithm;
import kovac.res.util.LinkedViewersUtil;
import kovac.saving.SavingStatic;
import kovac.shapes.EllipsoidOverlay;
import kovac.shapes.GroupPointsOverlay;
import kovac.shapes.PointOverlay;
import plugins.weiss.fitellipsoid.fitellipsoid;

/**
 * This class is here to save and display the clicked points from
 * LockedOrthoCanvas
 * 
 * @author bastien.kovac
 * 
 */
public class Points {

	/**
	 * Saves the clicked points
	 */
	private static List<PointInSpace> points = new ArrayList<PointInSpace>();
	/**
	 * All group of points created
	 */
	private static List<GroupPointsOverlay> groups = new ArrayList<GroupPointsOverlay>();
	private static Map<Integer, List<PointInSpace>> multiplePointsEllipse = new HashMap<Integer, List<PointInSpace>>();
	
	public static class PointInSpace {
		private Point3D pt;
		private DimensionId dim;
		private Overlay overlay;

		public PointInSpace(Point3D pt, DimensionId dim) {
			this.pt = pt;
			this.dim = dim;
			this.overlay = new PointOverlay("Point " + points.size(), this.pt);
		}

		public Point3D getPoint() {
			return pt;
		}

		public DimensionId getDim() {
			return dim;
		}

		public Overlay getOverlay() {
			return overlay;
		}
	}

	/**
	 * Add a point to the saving list
	 * 
	 * @param p
	 *            The point to save
	 */
	public static void addPoint(Point3D p, DimensionId dim) {
		points.add(new PointInSpace(p, dim));
	}

	/**
	 * Clears the point overlays displayed in the VTK view, removing them from
	 * the sequence
	 */
	private static void clearPointsOverlays() {
		for (Overlay o : LinkedViewersUtil.getVTKOverlays()) {
			if (o instanceof PointOverlay)
				LinkedViewersUtil.removeOverlayFromVTK(o);
		}
	}

	/**
	 * Clears the ellipsoids overlays displayed in the VTK view, removing them from
	 * the sequence
	 */
	public static void clearEllipsoidsOverlays() {
		for (Overlay o : LinkedViewersUtil.getVTKOverlays()) {
			if (o instanceof EllipsoidOverlay)
				LinkedViewersUtil.removeOverlayFromVTK(o);
		}
	}
	
	public static void remove(PointInSpace pt) {
		for (List<PointInSpace> list : multiplePointsEllipse.values())
			list.remove(pt);
		points.remove(pt);
	}
	
	public static void saveCurrentList() {
		if (points.isEmpty())
			return;
		List<PointInSpace> save = new ArrayList<Points.PointInSpace>(points);
		multiplePointsEllipse.put(multiplePointsEllipse.keySet().size(), save);
		points.clear();
	}

	public static List<PointInSpace> getAllPoints() {
		List<PointInSpace> ret = new ArrayList<Points.PointInSpace>();
		for (List<PointInSpace> pts : multiplePointsEllipse.values())
			ret.addAll(pts);
		// We also add current, unsaved, list
		ret.addAll(points);
		return ret;
	}

	public static void removeLastOne() {
		if (points.isEmpty()){
			System.out.println("No remaining points. Cannot remove.");
			return;
		}
		points.get(points.size() - 1).getOverlay().remove();
		points.remove(points.size() - 1);
		LinkedViewersUtil.getOrthCanvas().repaint();		
	}
	
	public static void clear() {
		multiplePointsEllipse.clear();
		clearPointsOverlays();
	}

	/**
	 * Calls the algorithm creating the ellipsoid fitting the saved points
	 */
	public static void createEllipsoids() {
		EllipsoidAlgorithm algo = null;
		EllipsoidOverlay ellipsoid = null;
		
			List<Point3D> pointsToUse = new ArrayList<Point3D>();
			int i = 1;
			for (List<PointInSpace> currentList : multiplePointsEllipse.values()) {
				System.out.println("Generating ellipsoid number " + i++);
				for (PointInSpace pt : currentList)
					pointsToUse.add(pt.getPoint());
			}

			if (pointsToUse.size()>12){
				algo = new EllipsoidAlgorithm(pointsToUse);
				ellipsoid = (EllipsoidOverlay) algo.generateEllipsoid();
				ellipsoid.setName("Ellipsoid " + SavingStatic.getNumberOfEllipsoids());
				ellipsoid.validate();
				LinkedViewersUtil.addOverlayToVTK(ellipsoid);

				if (fitellipsoid.isDisplayingPoints()) {
					GroupPointsOverlay group = new GroupPointsOverlay("Group number " + groups.size(), pointsToUse);
					groups.add(group);
					LinkedViewersUtil.addOverlayToVTK(group);
				}
				ellipsoid.checkValues();
				pointsToUse.clear();		
				clear();
			}
			else{
				System.out.println("Not enough points. Minimum is 12. Please add some.");
			}
	}
}
