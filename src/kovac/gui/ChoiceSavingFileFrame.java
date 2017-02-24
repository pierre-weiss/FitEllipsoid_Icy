package kovac.gui;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextPane;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import kovac.saving.Saving;

public class ChoiceSavingFileFrame extends JFrame {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private static final String errorMessage = "A previous file is associated to this image. \n"
			+ "Would you like to save your work in another one, or to load it ?";

	private File saveDirectory, saveFile;

	public ChoiceSavingFileFrame(File saveDirectory, File savingFile) {
		super();
		this.saveDirectory = saveDirectory;
		this.saveFile = savingFile;
		this.setTitle("Existing file found !");
		this.setLocationRelativeTo(null);
		this.add(new PanelChoice());
		this.pack();
		this.setAlwaysOnTop(true);
		this.setResizable(false);
		this.setVisible(true);
	}

	private class PanelChoice extends JPanel {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		private JButton fileSaveChooser;
		private JTextPane errorMessageDisplay;
		private JButton loadFoundFile;

		public PanelChoice() {

			this.setLayout(new GridLayout(2, 1));
			this.setBorder(new EmptyBorder(3, 3, 3, 3));

			JPanel panelTop = new JPanel(new BorderLayout());

			errorMessageDisplay = new JTextPane();
			errorMessageDisplay.setText(errorMessage);
			StyledDocument doc = errorMessageDisplay.getStyledDocument();
			SimpleAttributeSet center = new SimpleAttributeSet();
			StyleConstants.setAlignment(center, StyleConstants.ALIGN_CENTER);
			doc.setParagraphAttributes(0, doc.getLength(), center, false);
			panelTop.add(errorMessageDisplay, BorderLayout.CENTER);
			errorMessageDisplay.setEditable(false);

			this.add(panelTop);

			JPanel panelBottom = new JPanel(new BorderLayout());
			panelBottom.setBorder(new EmptyBorder(5, 5, 5, 5));
			fileSaveChooser = new JButton("Save in another file");
			fileSaveChooser.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent arg0) {
					JFileChooser fileChooser = new JFileChooser(saveDirectory);
					fileChooser.setFileFilter(new FileNameExtensionFilter("XML File", "xml"));
					int returnVal = fileChooser.showSaveDialog(ChoiceSavingFileFrame.this);
					if (returnVal == JFileChooser.APPROVE_OPTION) {
						Saving.setSavingFile(fileChooser.getSelectedFile());
						ChoiceSavingFileFrame.this.dispose();
					}
				}
			});

			panelBottom.add(fileSaveChooser, BorderLayout.WEST);

			loadFoundFile = new JButton("Load " + saveFile.getName());
			loadFoundFile.setPreferredSize(fileSaveChooser.getPreferredSize());
			loadFoundFile.addActionListener(new ActionListener() {
				
				@Override
				public void actionPerformed(ActionEvent e) {
					Saving.loadXMLFile(saveFile);
					ChoiceSavingFileFrame.this.dispose();
				}
			});

			panelBottom.add(loadFoundFile, BorderLayout.EAST);

			this.add(panelBottom);

		}

	}

}
