package kovac.shapes;

import java.util.List;

import icy.painter.Overlay;
import icy.painter.VtkPainter;
import icy.type.point.Point3D;
import kovac.res.util.LinkedViewersUtil;
import vtk.vtkActor;
import vtk.vtkCellArray;
import vtk.vtkPoints;
import vtk.vtkPolyData;
import vtk.vtkPolyDataMapper;
import vtk.vtkProp;

/**
 * This overlay represents a group of points that has been used to generate a
 * fitting ellipsoid.
 * 
 * @author bastien.kovac
 *
 */
public class GroupPointsOverlay extends Overlay implements VtkPainter {

	/**
	 * The points used
	 */
	private List<Point3D> points;

	/**
	 * The vtkActor
	 */
	private vtkActor groupActor;

	/**
	 * Builds a new Overlay from a list of points
	 * 
	 * @param name
	 *            The name of the overlay
	 * @param points
	 *            The list of points used to build the overlay
	 */
	public GroupPointsOverlay(String name, List<Point3D> points) {
		super(name);
		this.points = points;
		init();
	}

	/**
	 * Initialize the vtkActor
	 */
	private void init() {

		vtkPoints pointsVTK = new vtkPoints();
		vtkCellArray cellArray = new vtkCellArray();
		for (Point3D pt : points) {
			id = pointsVTK.InsertNextPoint(pt.getX(), pt.getY(), pt.getZ());
			cellArray.InsertNextCell(1);
			cellArray.InsertNextCell(id);
		}
		
		vtkPolyData polyData = new vtkPolyData();
		
		polyData.SetPoints(pointsVTK);
		polyData.SetVerts(cellArray);
		
		vtkPolyDataMapper mapper = new vtkPolyDataMapper();
		mapper.SetInputData(polyData);

		groupActor = new vtkActor();
		groupActor.SetMapper(mapper);
		groupActor.SetScale(LinkedViewersUtil.getScale());
		groupActor.GetProperty().SetColor(0, 1, 0);
		groupActor.GetProperty().SetPointSize(5);
		groupActor.SetPickable(0);

	}

	@Override
	public vtkProp[] getProps() {
		return new vtkProp[] { groupActor };
	}

}
