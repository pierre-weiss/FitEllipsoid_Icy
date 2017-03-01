package kovac.res.util;

import java.util.ArrayList;
import java.util.List;

import icy.canvas.IcyCanvas;
import icy.gui.dialog.MessageDialog;
import icy.gui.viewer.Viewer;
import icy.painter.Overlay;
import icy.sequence.DimensionId;
import icy.sequence.Sequence;
import kovac.res.gui.LinkListener;
import kovac.res.gui.SegOrthoViewer;
import kovac.res.gui.SegOrthoViewer.SegOrthoCanvas;
import kovac.res.gui.SegOrthoViewer.SegOrthoCanvas.SegOrthoView;
import kovac.shapes.AxisOverlay;
import kovac.shapes.GroupPointsOverlay;
import kovac.shapes.EllipsoidOverlay;
import plugins.kernel.canvas.VtkCanvas;

/**
 * This class is here both to store the views the plugin works on
 * (Simultaneously an OrthoView and a 3D VTK view) and to ease the operations on
 * these views
 * 
 * @author bastien.kovac
 *
 */
public class LinkedViewersUtil {

	/**
	 * The stored views
	 */
	private static Viewer vOrth, vVTK;
	/**
	 * The base sequence that was used to launch the plugin
	 */
	private static Sequence baseSeq;

	private static boolean vtkSetUp = false;

	/**
	 * Set a Viewer as the OrthoViewer
	 * 
	 * @param v
	 *            The viewer
	 */
	public static void setOrth(Viewer v) {
		vOrth = v;
	}

	/**
	 * Set a Viewer as the VTK viewer
	 * 
	 * @param v
	 *            The viewer
	 */
	public static void setVTK(Viewer v) {
		vVTK = v;
	}

	/**
	 * Used to link the two viewers with one another (Should only be called
	 * after the two viewers are set up, the method will return if one of them
	 * is null)
	 */
	public static void linkViewers() {
		if (!areSet() || !vtkSetUp)
			return;
		vOrth.addFrameListener(new LinkListener(vVTK));
		vVTK.addFrameListener(new LinkListener(vOrth));
	}

	/**
	 * Set a Sequence as the base sequence <b>Should only be called once during
	 * the plugin execution</b>
	 * 
	 * @param s
	 *            The sequence
	 */
	public static void setBaseSeq(Sequence s) {
		baseSeq = s;
	}

	/**
	 * @return The orthogonal viewer
	 */
	public static Viewer getOrth() {
		return vOrth;
	}

	/**
	 * @return The VTK viewer
	 */
	public static Viewer getVTK() {
		return vVTK;
	}

	/**
	 * @return The base sequence
	 */
	public static Sequence getBaseSeq() {
		return baseSeq;
	}

	/**
	 * @return The canvas from the orthogonal viewer
	 */
	public static SegOrthoCanvas getOrthCanvas() {
		if (!(vOrth.getCanvas() instanceof SegOrthoCanvas)) {
			MessageDialog.showDialog(
					"This plugin does not work for regular 2D Canvas, the sequence will now return to OrthoViewer");
			vOrth.setCanvas(SegOrthoCanvas.class.getName());
		}
		return (SegOrthoCanvas) vOrth.getCanvas();
	}

	/**
	 * @return The canvas from the VTK viewer
	 */
	public static IcyCanvas getVTKCanvas() {
		return vVTK.getCanvas();
	}

	/**
	 * Return the OrthoView from the orthogonal viewer corresponding to the
	 * given dimension
	 * 
	 * @param dim
	 *            The wanted dimension
	 * @return The corresponding OrthoView
	 */
	public static SegOrthoView getView(DimensionId dim) {
		switch (dim) {
		case X:
			return getOrthCanvas().getZYView();
		case Y:
			return getOrthCanvas().getXZView();
		case Z:
			return getOrthCanvas().getXYView();
		default:
			return null;
		}
	}

	/**
	 * @return The sequence from the orthogonal viewer
	 */
	public static Sequence getOrthSequence() {
		return vOrth.getSequence();
	}

	/**
	 * @return The sequence from the VTK viewer
	 */
	public static Sequence getVTKSequence() {
		return vVTK.getSequence();
	}

	/**
	 * @return The list of all overlays from the sequence of the VTK viewer
	 */
	public static List<Overlay> getVTKOverlays() {
		if (!vtkSetUp || vVTK == null)
			return new ArrayList<Overlay>();
		if (getVTKSequence() != null) {
			return getVTKSequence().getOverlays();
		} else {
			return new ArrayList<Overlay>();
		}		
	}

	/**
	 * Sets the orthogonal viewer to the specified location (should be used to
	 * transition between Custom and Locked OrthoCanvas, so the position isn't
	 * reset on every change)
	 * 
	 * @param coords
	 *            The coordinates of the point we want to observe
	 */
	public static void setPositionOrth(double... coords) {
		vOrth.getCanvas().setPositionX((int) coords[0]);
		vOrth.getCanvas().setPositionY((int) coords[1]);
		vOrth.getCanvas().setPositionZ((int) coords[2]);
	}

	/**
	 * Sets the rotation of the orthogonal viewer to the given values (should be
	 * used to transition between Custom and Locked OrthoCanvas, so the rotation
	 * isn't reset on every change)
	 * 
	 * @param rotation
	 *            The value of the rotation (first X, then Y)
	 */
	public static void setRotationOrth(double... rotation) {
		vOrth.getCanvas().setRotationX(rotation[0]);
		vOrth.getCanvas().setRotationY(rotation[1]);
	}

	/**
	 * Switch the VTK viewer's current canvas to a VtkCanvas (usually only
	 * called when starting the plugin)
	 */
	public static void goToVTK() {
		vtkSetUp = true;
		try {
			vVTK.setCanvas(VtkCanvas.class.getName());
		} catch (Throwable e) {
			// This is really ugly and I know it ..
			System.out.println("Couldn't initialize VTK viewer");
			vtkSetUp = false;
		}
	}

	public static void goToSeg() {
		vOrth.setCanvas(SegOrthoViewer.class.getName());
	}

	/**
	 * Remove the given Overlay from the VTK sequence
	 * 
	 * @param o
	 *            The overlay to remove
	 */
	public static void removeOverlayFromVTK(Overlay o) {
		if (!vtkSetUp)
			return;
		o.remove();
	}
	
	/**
	 * Display all Overlays from the VTK sequence
	 * 
	 */
	public static void displayAllOverlaysFromVTK() {
		if (!vtkSetUp)
			return;

		System.out.println("Below are all overlays:");
		for (Overlay o : getVTKOverlays()) {
			System.out.println("Overlay " + o.getName() + "  of " + o.getClass());
		}
		System.out.println("");

	}

	
	/**
	 * Remove all LineOverlay from the VTK sequence, used to update and/or
	 * delete the three axis
	 */
	public static void removeAllLinesOverlayFromVTK() {
		if (!vtkSetUp)
			return;
		for (Overlay o : getVTKOverlays()) {
			if (o instanceof AxisOverlay) {
				removeOverlayFromVTK(o);
			}
		}
	}

	
	/**
	 * Remove all EllipsoidOverlay from the VTK sequence
	 */
	public static void removeEllipsoidOverlays(){
		if (!vtkSetUp)
			return;
		
		for (Overlay o : getVTKOverlays()){
			if (o instanceof EllipsoidOverlay){
				o.setCanBeRemoved(true);
				removeOverlayFromVTK(o);
			}
			if (o instanceof GroupPointsOverlay){
				removeOverlayFromVTK(o);
			}
		}
	}
	
	
	
	public static void removeOverlays() {
		if (!vtkSetUp)
			return;
		
		for (Overlay o : getVTKOverlays())
			removeOverlayFromVTK(o);		
	}

	/**
	 * Add the given Overlay to the VTK sequence
	 * 
	 * @param o
	 *            The Overlay to add
	 */
	public static void addOverlayToVTK(Overlay o) {
		if (!vtkSetUp)
			return;
		getVTKSequence().addOverlay(o);
	}

	/**
	 * @return The scale of the base sequence as an array of pixel sizes [x, y,
	 *         z]
	 */
	public static double[] getScale() {
		return new double[] { baseSeq.getPixelSizeX(), baseSeq.getPixelSizeY(), baseSeq.getPixelSizeZ() };
	}

	/**
	 * @return An array representing the size of the base sequence in each
	 *         direction [x, y, z]
	 */
	public static double[] getSizes() {
		return new double[] { baseSeq.getSizeX(), baseSeq.getSizeY(), baseSeq.getSizeZ() };
	}

	/**
	 * @return The current position of the orthogonal viewer
	 */
	public static double[] getCurrentOrthCoordinates() {
		return getOrthCanvas().imageCoordinates();
	}

	/**
	 * @return The current rotation values of the orthogonal viewer
	 */
	public static double[] getCurrentOrthRotation() {
		return getOrthCanvas().imageRotation2D();
	}

	/**
	 * @return True if both vOrt and vVTK are set, false if not
	 */
	public static boolean areSet() {
		return (vOrth != null && vVTK != null);
	}

	/**
	 * Called to minimize the orthogonal and VTK viewers
	 */
	public static void minimizeViewers() {
		vOrth.setMinimized(true);
		vVTK.setMinimized(true);
	}

	/**
	 * Called to clear the viewers and the base sequence
	 */
	public static void clear() {
		if (vtkSetUp)
			vVTK.close();
		vOrth.close();
	}

}
