package plugins.weiss.fitellipsoid;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JSeparator;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;

import icy.common.Version;
import icy.gui.dialog.ConfirmDialog;
import icy.gui.dialog.MessageDialog;
import icy.gui.viewer.Viewer;
import icy.main.Icy;
import icy.sequence.Sequence;
import icy.sequence.SequenceUtil;
import icy.system.thread.ThreadUtil;
import kovac.binaryMask.ExportBinaryMask;
import kovac.res.Points;
import kovac.res.enums.Methods;
import kovac.res.gui.SegOrthoViewer.SegOrthoCanvas;
import kovac.res.util.LinkedViewersUtil;
import kovac.saving.ExportCSV;
import kovac.saving.Saving;
import kovac.saving.SavingStatic;

import plugins.adufour.ezplug.EzButton;
import plugins.adufour.ezplug.EzGUI;
import plugins.adufour.ezplug.EzPlug;
import plugins.adufour.ezplug.EzStoppable;
import plugins.adufour.ezplug.EzVar;
import plugins.adufour.ezplug.EzVarBoolean;
import plugins.adufour.ezplug.EzVarEnum;
import plugins.adufour.ezplug.EzVarListener;
import plugins.adufour.ezplug.EzVarSequence;

/**
 * Main class of the Plugin
 * 
 * @author bastien.kovac & Pierre Weiss
 * 
 */
public class fitellipsoid extends EzPlug implements EzStoppable {

	private static Methods chosenMethod;
	private static boolean hasSetUpImage;
	private static EzGUI currentUI;

	private static EzVarSequence sequenceChoice;
	private static EzVarEnum<Methods> methodChoice;
	private static EzButton exportCSV;
	private static EzButton confirmSequence;
	private static EzButton exportBinary;
	private static EzButton removeLastEllipsoid;
	private static EzButton removeGivenEllipsoid;
	private static EzVarBoolean displayPoints;

	private static ActionListener confirmListener;
	private static EzVarListener<Methods> methodListener;
	private static ActionListener exportListener;
	private static ActionListener exportBinaryListener;
	private static ActionListener removeListener;
	private static ActionListener removeGivenListener;

	@Override
	public void execute() {
		if (!LinkedViewersUtil.areSet()) {
			MessageDialog.showDialog("No sequence confirmed, the plugin will stop");
			return;
		}
		switch (getChosenMethod()) {
		case ELLIPSES:
			break;
		case POINTS:
			Points.saveCurrentList();
			Points.createEllipsoids();
			
			// We unlock the orthoview
			SegOrthoCanvas canvas = (SegOrthoCanvas) LinkedViewersUtil.getOrth().getCanvas();
			canvas.getLock().setSelected(false);
			canvas.setLock(false);
			
			LinkedViewersUtil.getOrthCanvas().repaint(); //refreshes orthoviews

			break;
		default:
			break;
		}
	}

	@Override
	public void clean() {
		LinkedViewersUtil.clear();
	}

	@Override
	protected void initialize() {

		Version currentVersion = Icy.version;
		if (currentVersion.isOlder(new Version("1.7.0.0"))) {
			MessageDialog.showDialog("This plugin works only for Version 1.7.0.0 and greater. Please update Icy",
					MessageDialog.ERROR_MESSAGE);
			return;
		}

		initializeListeners();
		initializeGUI();

		addEzComponent(sequenceChoice);
		addEzComponent(confirmSequence);
		addComponent(new JSeparator(JSeparator.HORIZONTAL));
		addEzComponent(displayPoints);
		addEzComponent(methodChoice);
		addEzComponent(exportCSV);
		addEzComponent(exportBinary);
		addEzComponent(removeGivenEllipsoid);
		addEzComponent(removeLastEllipsoid);

		methodChoice.addVarChangeListener(methodListener);
		methodChoice.setEnabled(false);

	}

	/**
	 * Initialize the graphic components of the UI
	 */
	private void initializeGUI() {
		sequenceChoice = new EzVarSequence("Sequence");
		sequenceChoice.setToolTipText("Choose the sequence to work on");
		methodChoice = new EzVarEnum<Methods>("Method", Methods.values(), Methods.POINTS);
		methodChoice.setToolTipText("Only points are implemented for now");
		exportCSV = new EzButton("Export as CSV", exportListener);
		exportCSV.setToolTipText("Export data as a CSV file");
		exportBinary = new EzButton("Export as Binary Mask", exportBinaryListener);
		exportBinary.setToolTipText("Create a new binary image, where every pixel's value is 0, except for the ones inside an ellipsoid");
		removeGivenEllipsoid  = new EzButton("Remove given ellipsoid", removeGivenListener);
		removeGivenEllipsoid.setToolTipText("Removes an ellipsoid given by a number");
		removeLastEllipsoid  = new EzButton("Remove last ellipsoid", removeListener);
		removeLastEllipsoid.setToolTipText("Removes the last ellipdoid from list");


		confirmSequence = new EzButton("Confirm sequence", confirmListener);
		displayPoints = new EzVarBoolean("Display points", true);

		currentUI = getUI();
	}

	/**
	 * Initialize the listeners for the UI
	 */
	private void initializeListeners() {

		
		removeGivenListener = new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {

				//ThreadUtil.bgRun(new Runnable() {

				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						String number = JOptionPane.showInputDialog(this, "Enter the number of your ellipsoid:");
						//String number = MessageDialog.showDialog("QUESTION", QUESTION_MESSAGE);
						SavingStatic.removeGiven(number);
					}
				});
			}
		};
		
		removeListener = new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {

				ThreadUtil.bgRun(new Runnable() {

					@Override
					public void run() {
						SavingStatic.removeLast();
					}
				});
			}
		};
		
		exportBinaryListener = new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {

				ThreadUtil.bgRun(new Runnable() {

					@Override
					public void run() {
						ExportBinaryMask.exportMask(LinkedViewersUtil.getBaseSeq());
					}
				});
			}
		};

		exportListener = new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				JFrame frame = new JFrame();
				JFileChooser fileChooser = new JFileChooser(Saving.getSavingDirectory());
				fileChooser.setFileFilter(new FileNameExtensionFilter("CSV Files", "csv"));
				int returnVal = fileChooser.showSaveDialog(frame);
				if (returnVal == JFileChooser.APPROVE_OPTION) {
					ExportCSV.initializeExport(fileChooser.getSelectedFile());
				}
			}
		};

		confirmListener = new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				if (LinkedViewersUtil.areSet()) {
					LinkedViewersUtil.minimizeViewers();
				}
				setUpViews();
				if (hasSetUpImage) {
					do {
						ThreadUtil.sleep(500);
					} while (LinkedViewersUtil.getBaseSeq() == null);
					Saving.initializeSaving(LinkedViewersUtil.getBaseSeq());
					confirmSequence.setText("Change sequence");
				}
			}
		};

		methodListener = new EzVarListener<Methods>() {

			@Override
			public void variableChanged(EzVar<Methods> source, Methods newValue) {
				chosenMethod = newValue;
				if (chosenMethod == Methods.ELLIPSES) {
					displayPoints.setVisible(false);
				} else {
					displayPoints.setVisible(true);
				}
			}
		};
	}

	/**
	 * Sets up the views from a base Sequence. Opens one orthoViewer and one vtk
	 * 3d viewer
	 */
	private void setUpViews() {
		if (LinkedViewersUtil.getBaseSeq() == sequenceChoice.getValue()) {
			return;
		}
		if (!checker(sequenceChoice.getValue()))
			return;
		final Sequence seq = SequenceUtil.getCopy(sequenceChoice.getValue());
		// Get base viewer
		final Viewer baseViewer = Icy.getMainInterface().getActiveViewer();
		LinkedViewersUtil.setBaseSeq(sequenceChoice.getValue());
		if (baseViewer != null) {

			ThreadUtil.invokeLater(new Runnable() {

				@Override
				public void run() {

					LinkedViewersUtil.setVTK(new Viewer(seq));
					LinkedViewersUtil.goToVTK();
					LinkedViewersUtil.getVTK().setTitle("3D VTK rendering");
					LinkedViewersUtil.getVTK().setSize(500, 500);

					LinkedViewersUtil.setOrth(new Viewer(seq));
					LinkedViewersUtil.goToSeg();
					LinkedViewersUtil.getOrth().setTitle("Orthogonal View");
					LinkedViewersUtil.getOrth().setSize(500, 500);

					baseViewer.close();

					LinkedViewersUtil.linkViewers();
					hasSetUpImage = true;

				}
			});
		}
	}

	/**
	 * This method checks if the given sequence is appropriate for the plugin,
	 * it must : - Have a Z (or T) size superior to 1 - If T > 1 but Z = 1, the
	 * user is asked if he agrees to convert the sequence to stack, if not, an
	 * exception is thrown
	 * 
	 * @param baseSeq
	 *            The sequence given as argument
	 * @return false if the image can't be found or used by the plugin, true if
	 *         it can be used
	 * @throws IllegalArgumentException
	 *             The exception thrown in case of wrong dimensions
	 */
	private boolean checker(Sequence baseSeq) throws IllegalArgumentException {
		if (baseSeq == null) {
			MessageDialog.showDialog("This plugin needs an image to work");
			confirmSequence.setText("Confirm sequence");
			return false;
		}
		// Check for a XYZ image
		if (baseSeq.getSizeZ() < 2) {
			if (baseSeq.getSizeT() > 2) {
				if (!ConfirmDialog.confirm(
						"This plugin requires a stacked image to work, would you like to convert your image to stack ?")) {
					return false;
				}
				SequenceUtil.convertToStack(baseSeq);
			}
		}
		return true;
	}

	/**
	 * Enables or disables the run button of the plugin's UI
	 * 
	 * @param flag
	 *            True to enable the button, false to disable it
	 */
	public static void setRunEnabled(boolean flag) {
		currentUI.setRunButtonEnabled(flag);
	}

	/**
	 * @return The method chosen by the user
	 */
	public static Methods getChosenMethod() {
		return chosenMethod;
	}

	/**
	 * Change the text of the confirmSequence EzButton
	 * 
	 * @param text
	 *            New text
	 */
	public static void setTextSequence(String text) {
		confirmSequence.setText(text);
	}

	public static boolean isDisplayingPoints() {
		return displayPoints.getValue();
	}

}
