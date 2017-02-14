package kovac.saving;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import icy.gui.dialog.ConfirmDialog;
import kovac.res.util.LinkedViewersUtil;
import kovac.res.util.ReaderUtils;
import kovac.shapes.Ellipsoid;

public class ExportCSV {

	private static File exportedFile;
	private static List<Ellipsoid> ellipsoidsToExport;

	private static final String HEADER = "Name;Unit;Center;;;First Vector;;;Second Vector;;;Thrid Vector;;;Vectors Semi-Length;;;Volume \n"
			+ ";;X;Y;Z;X Coeff.;Y Coeff.;Z Coeff.;X Coeff.;Y Coeff.;Z Coeff.;X Coeff.;Y Coeff.;Z Coeff.;First Vector;Second Vector;Thrid Vector; \n";

	public static void initializeExport(File export) {
		if (export == null) {
			System.err.println("Error while accessing to export file, can't export data");
			return;
		}
		exportedFile = export;
		if (!exportedFile.getName().endsWith(".csv")) {
			exportedFile = new File(exportedFile.getPath() + ".csv");
		}
		ellipsoidsToExport = new ArrayList<Ellipsoid>(SavingStatic.getAllEllipsoids());
		if (!exportedFile.exists()) {
			try {
				exportedFile.createNewFile();
			} catch (IOException e) {
				System.err.println("Can't create export file, please check system accessibility");
			}
		}
		generateCSVFile();
	}

	private static void generateCSVFile() {

		if (exportedFile == null)
			return;

		if (LinkedViewersUtil.areSet()) {

			int nbLines = 0;

			try {
				nbLines = ReaderUtils.countLines(exportedFile.getPath());
			} catch (IOException e1) {
				System.err.println("Error while counting lines of existing file, considered it full");
				nbLines = 10;
			}

			if (nbLines > 2) {
				if (!ConfirmDialog.confirm("This file already contains previous data, confirm to erase them")) {
					return;
				}
			}

			try {
				FileWriter writer = new FileWriter(exportedFile);

				writer.write("");

				writer.append(HEADER + "\n");
				for (Ellipsoid e : ellipsoidsToExport) {
					writer.append(e.printAsCSV());
				}

				writer.flush();
				writer.close();

			} catch (IOException e) {
				System.err.println("Error while exporting data, exiting...");
			}

		}

	}

}
