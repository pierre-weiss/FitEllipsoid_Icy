package kovac.shapes;

import java.awt.Color;

import javax.swing.JPanel;

import Jama.Matrix;
import icy.gui.dialog.ConfirmDialog;
import icy.painter.Overlay;
import icy.painter.VtkPainter;
import icy.sequence.DimensionId;
import kovac.gui.panels.EllipsoidPanel;
import kovac.res.EllipsoidTransformations;
import kovac.res.quadric.QuadricExpression;
import kovac.res.util.LinkedViewersUtil;
import kovac.res.util.MathUtils;
import kovac.saving.SavingStatic;
import vtk.vtkActor;
import vtk.vtkDoubleArray;
import vtk.vtkPoints;
import vtk.vtkPolyData;
import vtk.vtkPolyDataMapper;
import vtk.vtkProp;
import vtk.vtkSphereSource;
import vtk.vtkTensorGlyph;

/**
 * This class is here to display an Ellipsoid as a three dimensional VTK Object,
 * in the VTK view
 * 
 * @author bastien.kovac
 *
 */
public class EllipsoidOverlay extends Overlay implements VtkPainter {

	/**
	 * The vtkActor corresponding to the displayed ellipsoid
	 */
	private vtkActor ellipsoidActor;
	/**
	 * The ellipsoid to display
	 */
	private Ellipsoid ellipsoid;
	/**
	 * This is the quadratic expression of the ellipsoid
	 */
	private QuadricExpression quadric, quadricMicro;
	/**
	 * The name of the Overlay
	 */
	private String name;
	/**
	 * True if this Overlay is saved in kovac.groups.Saving, false if it isn't
	 */
	private boolean isSaved;
	private Matrix matSR;
	private vtkPoints center;

	private EllipsoidTransformations linkedTransformation;
	private double[] bounds;

	public EllipsoidOverlay(Matrix matSR, vtkPoints center) {
		super("Ellipsoid");
		this.isSaved = false;
		this.matSR = matSR;
		this.center = center;
		linkedTransformation = new EllipsoidTransformations(matSR, center.GetPoint(0), this);

		initMat();
	}

	public EllipsoidOverlay() {
		super("Empty overlay");
	}

	public void saveQuadric(QuadricExpression quadric, QuadricExpression quadricMicro) {
		this.quadric = quadric;
		this.quadricMicro = quadricMicro;
	}

	public void applyTransformation() {
		this.remove();
		this.matSR = linkedTransformation.getNewMatrix();
		this.center = new vtkPoints();
		double[] newCenter = linkedTransformation.getNewCenter();
		center.InsertNextPoint(newCenter);
		Matrix centerMat = new Matrix(3, 1);
		centerMat.set(0, 0, newCenter[0]);
		centerMat.set(1, 0, newCenter[1]);
		centerMat.set(2, 0, newCenter[2]);
		// TODO => quadric.updateQuadric(matSR, centerMat);
		initMat();
		goToWireframe();
		this.setCanBeRemoved(true);
		LinkedViewersUtil.addOverlayToVTK(this);
		LinkedViewersUtil.getOrthCanvas().repaint();
	}

	public EllipsoidTransformations getTransform() {
		return linkedTransformation;
	}

	private void initMat() {
		vtkPolyData polyData = new vtkPolyData();
		polyData.SetPoints(center);

		vtkDoubleArray tensors = new vtkDoubleArray();

		tensors.SetNumberOfComponents(9);

		double[][] tabTmp;

		tabTmp = matSR.getArray();
		tensors.InsertTuple9(0, tabTmp[0][0], tabTmp[0][1], tabTmp[0][2], tabTmp[1][0], tabTmp[1][1], tabTmp[1][2],
				tabTmp[2][0], tabTmp[2][1], tabTmp[2][2]);

		// apply tensor to the polydata
		polyData.GetPointData().SetTensors(tensors);

		// create the sphere
		vtkSphereSource sphereSource = new vtkSphereSource();
		sphereSource.SetPhiResolution(18);
		sphereSource.SetThetaResolution(18);
		sphereSource.Update();

		// create the tensor
		vtkTensorGlyph tensorGlyph = new vtkTensorGlyph();

		// set the Data
		tensorGlyph.SetInputData(polyData);

		// connect the tensorGlyph with the sphere
		tensorGlyph.SetSourceConnection(sphereSource.GetOutputPort());

		// define the tensor
		tensorGlyph.ColorGlyphsOff();
		tensorGlyph.ThreeGlyphsOff();
		tensorGlyph.ExtractEigenvaluesOff();
		tensorGlyph.Update();

		// mapper
		vtkPolyDataMapper mapper = new vtkPolyDataMapper();
		mapper.SetInputData(tensorGlyph.GetOutput());
		mapper.Update();
		this.bounds = mapper.GetBounds();

		// actor
		ellipsoidActor = new vtkActor();
		ellipsoidActor.SetMapper(mapper);
		ellipsoidActor.SetScale(LinkedViewersUtil.getScale());
		ellipsoidActor.GetProperty().SetColor(1, 0, 0);
	}

	@Override
	public vtkProp[] getProps() {
		return new vtkProp[] { this.ellipsoidActor };
	}

	@Override
	public void setName(String name) {
		if (isSaved) {
			SavingStatic.rename(this.name, name);
		}
		this.name = name;
		super.setName(this.name);
		try {
			this.ellipsoid.setName(name);
		} catch (NullPointerException e) {
			System.err.println(e.getMessage());
		}
		
	}

	@Override
	public String getName() {
		return this.name;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
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
		EllipsoidOverlay other = (EllipsoidOverlay) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}

	/**
	 * Saves the ellipsoidOverlay in kovac.groups.Saving
	 */
	public void validate() {
		SavingStatic.saveEllipsoid(ellipsoid, name);
		this.isSaved = true;
	}

	/**
	 * Change the color of the vtkActor
	 * 
	 * @param rgb
	 *            The RGB values of the color, throw an exception if different
	 *            from 3
	 */
	public void setColor(Color c) {
		double[] rgb = new double[] { c.getRed() / 255.0, c.getGreen() / 255.0, c.getBlue() / 255.0 };
		this.ellipsoidActor.GetProperty().SetColor(rgb);
	}

	/**
	 * @return The volume of the ellipsoid
	 */
	public double getVolume() {
		if (this.ellipsoid == null)
			return Double.NaN;
		return this.ellipsoid.getVolume(this.ellipsoid.getSemiLength());
	}

	/**
	 * @return The current color of the vtkActor
	 */
	public double[] getColor() {
		return this.ellipsoidActor.GetProperty().GetColor();
	}

	public QuadricExpression getQuadricExpression() {
		return quadric;
	}

	@Override
	public JPanel getOptionsPanel() {
		return new EllipsoidPanel(this);
	}

	public void goToWireframe() {
		this.ellipsoidActor.GetProperty().SetRepresentationToWireframe();
	}

	public void goToGeneric() {
		this.ellipsoidActor.GetProperty().SetRepresentationToSurface();
	}

	public void setEllipsoid(Ellipsoid e) {
		this.ellipsoid = e;
		this.ellipsoid.setMicroQuadric(quadricMicro);
		this.ellipsoid.setBounds(bounds);
	}

	public Ellipsoid getEllipsoid() {
		return ellipsoid;
	}

	public double[] getIntersection(DimensionId dim, double[] truePos) {
		return this.quadric.getIntersection(dim, truePos);
	}

	public void checkValues() {
		boolean needsToDelete = false;
		double[] length = quadric.getSemiAxis();
		double max = MathUtils.max(length);
		for (int i = 0; i < length.length; i++) {
			length[i] /= max;
		}
		for (double d1 : length) {
			for (double d2 : length) {
				if (d1 != d2) {
					if (d1/d2 > 5) {
						needsToDelete = true;
					}
				}
			}
		}
		if (needsToDelete
				&& !ConfirmDialog.confirm("The ellipsoid elongation is higher than 5, do you want to proceed anyway ?")) {
			SavingStatic.deleteEllipsoid(ellipsoid.getName());
			this.remove();
		}
	}

}
