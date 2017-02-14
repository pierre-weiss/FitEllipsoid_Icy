package kovac.gui.panels;

import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import kovac.gui.res.IntegerField;
import kovac.res.EllipsoidTransformations;
import kovac.res.enums.Axis;
import kovac.shapes.EllipsoidOverlay;

public class TransformationPanel extends JPanel {

	private static final String translation = "Translation";
	private static final String scaling = "Scaling";
	private static final String rotation = "Rotation";

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private EllipsoidOverlay overlay;
	private EllipsoidTransformations transform;

	public TransformationPanel(EllipsoidOverlay o) {
		super();
		if (o != null) {
			overlay = o;
			transform = overlay.getTransform();
		}

		JTabbedPane tabbedPane = new JTabbedPane();

		tabbedPane.addTab(translation, new ContentPanel(translation));
		tabbedPane.addTab(scaling, new ContentPanel(scaling));
		tabbedPane.addTab(rotation, new ContentPanel(rotation));

		this.add(tabbedPane);
	}

	private class ContentPanel extends JPanel {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		private String title;
		private double value;

		// GUI
		private JCheckBox axisX, axisY, axisZ;
		private JButton buttonPlus, buttonMinus, buttonValidate, buttonCancel;
		private JTextField fieldValue;

		public ContentPanel(String title) {
			super();
			this.title = title;
			initComponents();
			initListeners();
		}

		private void initComponents() {
			axisX = new JCheckBox("Axis X", false);
			axisY = new JCheckBox("Axis Y", false);
			axisZ = new JCheckBox("Axis Z", false);

			buttonPlus = new JButton("Plus");
			buttonMinus = new JButton("Minus");

			buttonValidate = new JButton("Validate");
			buttonCancel = new JButton("Cancel");

			fieldValue = new IntegerField("1.0");
			value = 1.0;
			fieldValue.setHorizontalAlignment(JLabel.CENTER);
			fieldValue.setMaximumSize(new Dimension(50, 15));

			this.setLayout(new GridLayout(3, 3, 3, 3));

			this.setBorder(new EmptyBorder(3, 3, 3, 3));

			this.add(axisX);
			this.add(axisY);
			this.add(axisZ);
			this.add(buttonMinus);
			this.add(fieldValue);
			this.add(buttonPlus);
			this.add(buttonCancel);
			this.add(new JPanel());
			this.add(buttonValidate);
		}

		private void initListeners() {

			axisX.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {

				}
			});

			fieldValue.getDocument().addDocumentListener(new DocumentListener() {

				@Override
				public void removeUpdate(DocumentEvent e) {
					changeValue();
				}

				@Override
				public void insertUpdate(DocumentEvent e) {
					changeValue();
				}

				@Override
				public void changedUpdate(DocumentEvent e) {
					changeValue();
				}

				private void changeValue() {
					try {
						value = Double.parseDouble(fieldValue.getText());
					} catch (NumberFormatException e) {
						// No need for treatment
					}
				}

			});

			buttonPlus.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					if (title.equals(translation)) {
						handleTranslation(value);
					} else {
						if (title.equals(scaling)) {
							handleScaling(value);
						} else {
							handleRotation(value);
						}
					}
					transform.validate();
				}
			});

			buttonMinus.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					if (title.equals(translation)) {
						handleTranslation(-value);
					} else {
						if (title.equals(scaling)) {
							handleScaling(-value);
						} else {
							handleRotation(-value);
						}
					}
					transform.validate();
				}
			});

			buttonCancel.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					transform.cancel();
					transform.validate();
				}
			});

			buttonValidate.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					transform.validate();
					overlay.goToGeneric();
					JFrame fen = (JFrame) TransformationPanel.this.getParent().getParent().getParent().getParent();
					fen.dispose();
				}
			});

		}

		private void handleTranslation(double value) {
			double x = 0;
			double y = 0;
			double z = 0;

			if (axisX.isSelected()) {
				x = value;
			}
			if (axisY.isSelected()) {
				y = value;
			}
			if (axisZ.isSelected()) {
				z = value;
			}
			transform.applyTranslation(x, y, z);
		}

		private void handleScaling(double value) {
			Axis[] selectedAxis = new Axis[3];
			if (axisX.isSelected()) {
				selectedAxis[0] = Axis.X;
			}
			if (axisY.isSelected()) {
				selectedAxis[1] = Axis.Y;
			}
			if (axisZ.isSelected()) {
				selectedAxis[2] = Axis.Z;
			}
			transform.applyScaling(value, selectedAxis);
		}

		private void handleRotation(double value) {
			List<Axis> selectedAxis = new ArrayList<Axis>();
			if (axisX.isSelected()) {
				selectedAxis.add(Axis.X);
			}
			if (axisY.isSelected()) {
				selectedAxis.add(Axis.Y);
			}
			if (axisZ.isSelected()) {
				selectedAxis.add(Axis.Z);
			}
			Axis[] axis = new Axis[selectedAxis.size()];
			for (int i = 0 ; i < axis.length ; i++) {
				axis[i] = selectedAxis.get(i);
			}
			
			transform.applyRotation(value, axis);
		}

	}

}
