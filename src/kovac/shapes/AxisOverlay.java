package kovac.shapes;

import icy.painter.Overlay;
import icy.painter.VtkPainter;
import kovac.res.util.LinkedViewersUtil;
import vtk.vtkActor;
import vtk.vtkLineSource;
import vtk.vtkPolyDataMapper;
import vtk.vtkProp;

/**
 * This class is here to represent the point observed in a CustomOrthoCanvas
 * with the intersection of 3 lines, each included in one the three planes
 * (X,Y), (X,Z), (Y,Z). It is an overlay which can be added to the VTK viewer
 * 
 * @author bastien.kovac
 * 
 */
public class AxisOverlay extends Overlay implements VtkPainter {

	/**
	 * The (x;y;z) coordinates of the observed point
	 */
	private double xCoord, yCoord, zCoord;

	/**
	 * The vtkActors corresponding to each line
	 */
	private vtkActor lineXY, lineXZ, lineYZ;

	/**
	 * Constructor
	 * 
	 * @param name
	 *            The name of the created Overlay
	 * @param pointCoordinates
	 *            The coordinates of the observed point
	 */
	public AxisOverlay(String name, double[] pointCoordinates) {
		super(name);
		this.xCoord = pointCoordinates[0];
		this.yCoord = pointCoordinates[1];
		this.zCoord = pointCoordinates[2];
		init();
	}

	/**
	 * Initialize the vtkActors
	 */
	private void init() {

		vtkLineSource[] lineSources = new vtkLineSource[] { new vtkLineSource(), new vtkLineSource(),
				new vtkLineSource() };
		double[] maxCoords = LinkedViewersUtil.getSizes();

		// XY
		lineSources[0].SetPoint1(xCoord, yCoord, 0);
		lineSources[0].SetPoint2(xCoord, yCoord, maxCoords[2]);

		// XZ
		lineSources[1].SetPoint1(xCoord, 0, zCoord);
		lineSources[1].SetPoint2(xCoord, maxCoords[1], zCoord);

		// YZ
		lineSources[2].SetPoint1(0, yCoord, zCoord);
		lineSources[2].SetPoint2(maxCoords[0], yCoord, zCoord);

		vtkPolyDataMapper[] mappers = new vtkPolyDataMapper[] { new vtkPolyDataMapper(), new vtkPolyDataMapper(),
				new vtkPolyDataMapper() };

		vtkActor[] actors = new vtkActor[] { new vtkActor(), new vtkActor(), new vtkActor() };

		for (int i = 0; i < 3; i++) {
			mappers[i].SetInputConnection(lineSources[i].GetOutputPort());
			actors[i].SetMapper(mappers[i]);
			actors[i].GetProperty().SetColor(255, 0, 0);
			actors[i].SetScale(LinkedViewersUtil.getScale());
		}

		lineXY = actors[0];
		lineXZ = actors[1];
		lineYZ = actors[2];

	}

	@Override
	public vtkProp[] getProps() {
		return new vtkProp[] { lineXY, lineXZ, lineYZ };
	}

}
