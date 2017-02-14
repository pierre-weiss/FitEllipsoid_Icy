package kovac.binaryMask;

import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.apache.commons.io.FilenameUtils;

import icy.file.Saver;
import icy.sequence.Sequence;
import icy.system.thread.ThreadUtil;
import kovac.saving.Saving;

public class ExportBinaryMask {

	private static Sequence maskSequence;
	private static File savingFile;
	private static boolean isFileChosen;

	private static void createMask(Sequence initialSeq) {
		BinaryMask mask = new BinaryMask(initialSeq);
		maskSequence = mask.buildExitSequence();
	}

	private static void askForSavingFile() {

		JFrame fen = new JFrame("Saving binary mask");
		File defaultDirectory = Saving.getSavingDirectory();
		JFileChooser fileChooser = new JFileChooser(defaultDirectory);
		fileChooser.setFileFilter(new FileNameExtensionFilter("TIFF file", "tif"));
		int returnVal = fileChooser.showSaveDialog(fen);

		if (returnVal == JFileChooser.APPROVE_OPTION) {
			savingFile = fileChooser.getSelectedFile();
			if (FilenameUtils.getExtension(savingFile.getName()).equalsIgnoreCase("tif")) {
				savingFile = new File(savingFile.getParentFile(),
						FilenameUtils.getBaseName(savingFile.getName() + ".tif"));
			}
			isFileChosen = true;
		}

	}

	public static void exportMask(Sequence initialSeq) {

		ThreadUtil.invokeNow(new Runnable() {
			
			@Override
			public void run() {
				askForSavingFile();
			}
		});
		
		if (!isFileChosen) {
			return;
		}
		createMask(initialSeq);

		Saver.save(maskSequence, savingFile, false, true);

	}

}
