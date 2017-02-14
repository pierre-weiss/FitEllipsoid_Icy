package kovac.saving;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;

import org.apache.commons.io.FilenameUtils;
import org.w3c.dom.Document;

import icy.gui.dialog.MessageDialog;
import icy.sequence.Sequence;
import icy.util.XMLUtil;
import kovac.gui.ChoiceSavingFileFrame;
import kovac.res.util.LinkedViewersUtil;
import kovac.saving.persistent.PersistentEllipsoids;

public class Saving {

	private static File savingDirectory;
	private static File savingFile;

	private static Document savedXML;

	private static PersistentEllipsoids savedEllipsoids;

	public static void initializeSaving(Sequence seq) {

		if (seq == null) {
			MessageDialog.showDialog("Couldn't save sequence, please check you have confirmed a sequence to work on");
			return;
		}
		File seqFile = null;
		if (seq.getFilename() == null) {
			seqFile = new File(System.getProperties().getProperty("user.home") + File.separator + seq.getName());
		} else {
			seqFile = new File(seq.getFilename());
		}

		savingDirectory = seqFile.getParentFile();
		if (!savingDirectory.exists() || !savingDirectory.isDirectory()) {
			savingDirectory = new File(System.getProperties().getProperty("user.home"));
			System.err.print("Couldn't find image's directory, saving to " + savingDirectory.getPath() + " instead.");
		}
		System.out.println("Saving in directory : " + savingDirectory.getPath());
		String path = savingDirectory.getPath() + File.separator;

		String nameFile = FilenameUtils.removeExtension(seqFile.getName()) + "_saving.xml";
		savingFile = new File(path + nameFile);
		if (savingFile.exists()) {
			new ChoiceSavingFileFrame(savingDirectory, savingFile);
		} else {
			try {
				savingFile.createNewFile();
			} catch (IOException e) {
				System.err.println("Couldn't create saving file, please check the saving directory's accessibility");
			}
		}
		System.out.println("Saving in file : " + savingFile.getPath());
		savedXML = XMLUtil.createDocument(false);
		savedEllipsoids = new PersistentEllipsoids();
	}

	public static void setSavingFile(File file) {
		if (file == null)
			return;

		savingFile = file;
	}

	public static void loadXMLFile(File file) {
		if (file == null) {
			return;
		}
		savingFile = file;
		Document loadedDoc = XMLUtil.loadDocument(savingFile);
		if (loadedDoc != null) {
			savedEllipsoids = new PersistentEllipsoids();
			if (savedEllipsoids.loadFromXML(loadedDoc)) {
				System.out.println("Loading completed with success");
				if (LinkedViewersUtil.areSet()) {
					SavingStatic.regenerate();
					LinkedViewersUtil.getOrthCanvas().repaint();
				}
			} else {
				System.out.println("Loading failed");
			}
		}
	}

	public static void saveCurrentStatic() {
		if (!savedEllipsoids.saveToXML(savedXML)) {
			System.err.print("Error while saving file");
		}
		PrintWriter writer;
		try {
			writer = new PrintWriter(savingFile);
			writer.print("");
			writer.close();
		} catch (FileNotFoundException e) {
			System.err.println("Couldn't find saving file");
		}

		XMLUtil.saveDocument(savedXML, savingFile);
	}

	public static File getSavingDirectory() {
		return savingDirectory;
	}

}
