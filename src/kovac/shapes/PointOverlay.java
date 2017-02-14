package kovac.shapes;

import icy.painter.Overlay;
import icy.painter.VtkPainter;
import icy.type.point.Point3D;
import kovac.res.util.LinkedViewersUtil;
import vtk.vtkActor;
import vtk.vtkPolyDataMapper;
import vtk.vtkProp;
import vtk.vtkSphereSource;

/**
 * This class is here to display a point as Overlay, represented by a spherical
 * VTK object
 * 
 * @author bastien.kovac
 * 
 */
public class PointOverlay extends Overlay implements VtkPainter {

	/**
	 * The point to display
	 */
	private Point3D point;
	/**
	 * The corresponding vtkActor
	 */
	private vtkActor pointActor;

	/**
	 * Constructor
	 * 
	 * @param name
	 *            The name of the created Overlay
	 * @param pt
	 *            The point to display
	 */
	public PointOverlay(String name, Point3D pt) {
		super(name);
		this.point = pt;
		init();
	}

	/**
	 * Initialize the vtkActor
	 */
	private void init() {

		vtkSphereSource sphere = new vtkSphereSource();
		sphere.SetCenter(point.getX(), point.getY(), point.getZ());
		sphere.SetPhiResolution(18);
		sphere.SetThetaResolution(18);
		sphere.SetRadius(1);

		vtkPolyDataMapper map = new vtkPolyDataMapper();
		map.SetInputConnection(sphere.GetOutputPort());

		pointActor = new vtkActor();
		pointActor.SetMapper(map);
		pointActor.GetProperty().SetColor(0, 255, 0);
		pointActor.SetScale(LinkedViewersUtil.getScale());

	}

	@Override
	public vtkProp[] getProps() {
		return new vtkProp[] { pointActor };
	}

}
