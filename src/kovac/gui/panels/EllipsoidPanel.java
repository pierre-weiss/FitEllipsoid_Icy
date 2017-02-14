package kovac.gui.panels;

import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextField;

import kovac.shapes.EllipsoidOverlay;

/**
 * This panel is here to handle every information about a selected ellipsoid or
 * group of ellipsoid. It supports some basic operations : change of name, color
 * ... When an ellipsoid or group is selected, every other one is hidden from
 * the VTK viewer to allow better visibility
 * 
 * @author bastien.kovac
 *
 */
public class EllipsoidPanel extends JPanel {

	/**
	 * To avoid warnings
	 */
	private static final long serialVersionUID = 1L;
	
	private EllipsoidOverlay overlay;
	
	private JButton chooseColorButton;
	private JButton chooseNameButton;
	private JButton transformations;
	
	private JTextField changeName;

	/**
	 * Builds a new EllipsoidPanel for the given Ellipsoid
	 */
	public EllipsoidPanel(EllipsoidOverlay e) {
		super();
		this.overlay = e;
		initComponents();
		initListeners();
	}

	/**
	 * Initialize the graphic components of the Panel
	 */
	private void initComponents() {
		this.setLayout(new GridLayout(3, 2));
		changeName = new JTextField(overlay.getName());
		chooseNameButton = new JButton("Change Name");
		chooseColorButton = new JButton("Change Color");
		transformations = new JButton("Transform Ellipsoid");
		
		this.add(changeName);
		this.add(chooseNameButton);
		this.add(new JPanel());
		this.add(chooseColorButton);
		this.add(new JPanel());
		this.add(transformations);
		
		this.transformations.setEnabled(false);
		
	}

	/**
	 * Initialize the listeners for the graphic components of the panel
	 */
	private void initListeners() {
		chooseColorButton.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				Color c = JColorChooser.showDialog(null, "Choose a Color", Color.RED);
				overlay.setColor(c);
			}
		});
		
		chooseNameButton.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				overlay.setName(changeName.getText());
			}
		});
		
		transformations.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				JFrame fen = new JFrame("Transformations");
				fen.add(new TransformationPanel(overlay));
				fen.pack();
				fen.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
				fen.setVisible(true);
				fen.setResizable(false);
				fen.setAlwaysOnTop(true);
				fen.addWindowListener(new WindowListener() {
					
					@Override
					public void windowOpened(WindowEvent e) {
						overlay.goToWireframe();
					}
					
					@Override
					public void windowIconified(WindowEvent e) {
						// TODO Auto-generated method stub
						
					}
					
					@Override
					public void windowDeiconified(WindowEvent e) {
						// TODO Auto-generated method stub
						
					}
					
					@Override
					public void windowDeactivated(WindowEvent e) {
						// TODO Auto-generated method stub

					}
					
					@Override
					public void windowClosing(WindowEvent e) {
						overlay.goToGeneric();
					}
					
					@Override
					public void windowClosed(WindowEvent e) {
						// TODO Auto-generated method stub
						
					}
					
					@Override
					public void windowActivated(WindowEvent e) {
						// TODO Auto-generated method stub
						
					}
				});
			}
		});
	}

}
