package kovac.binaryMask;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import icy.gui.frame.progress.CancelableProgressFrame;
import icy.gui.frame.progress.ProgressFrame;
import icy.sequence.Sequence;
import icy.sequence.SequenceUtil;
import icy.system.thread.ThreadUtil;
import icy.type.collection.array.Array1DUtil;
import kovac.saving.SavingStatic;
import kovac.shapes.Ellipsoid;

public class BinaryMask {

	private Sequence seqExport;
	private List<BoundingBox> boundingBoxes;
	private int[] size;
	private static Collection<int[]> coordinates = Collections.synchronizedCollection(new ArrayList<int[]>());
	final ProgressFrame progress;

	public BinaryMask(Sequence initialSeq) {
		this.size = new int[] { initialSeq.getSizeX(), initialSeq.getSizeY(), initialSeq.getSizeZ() };
		this.seqExport = SequenceUtil.getCopy(initialSeq);
		this.seqExport.setName("Binary Mask for " + initialSeq.getName());
		this.boundingBoxes = new ArrayList<BoundingBox>();
		this.progress = new CancelableProgressFrame("Creating mask");
	}

	private void initializeSequence() {

		for (int z = 0; z < size[2]; z++) {
			Array1DUtil.fill(seqExport.getImage(0, z).getDataXY(0), 0, size[0] * size[1], 0);
		}

	}

	private void getBoundingBoxes() {
		for (Ellipsoid e : SavingStatic.getAllEllipsoids()) {
			boundingBoxes.add(new BoundingBox(e));
		}
	}

	private void treatBoundingBoxes() {

		ExecutorService exec = ThreadUtil.createThreadPool("Bounding Boxes");
		for (BoundingBox b : boundingBoxes) {
			exec.execute(new ConcurrentBoundingBoxTreatment(b));
		}
		exec.shutdown();
		try {
			exec.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
		} catch (InterruptedException e) {
			System.out.println("Threads didn't finish in time");
		}

	}

	public static void addCoordinates(int[] coord) {
		coordinates.add(coord);
	}

	public Sequence buildExitSequence() {

		initializeSequence();
		getBoundingBoxes();
		treatBoundingBoxes();

		progress.setLength(coordinates.size());

		final List<int[]> monoThreadCoordinates = new ArrayList<int[]>(coordinates);
		coordinates.clear();

		ExecutorService exec = ThreadUtil.createThreadPool("Creating Mask");

		for (int[] coordinates : monoThreadCoordinates) {
			exec.execute(new AddCoordinates(coordinates));
		}
		exec.shutdown();
		try {
			exec.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
		} catch (InterruptedException e) {
			System.out.println("Threads didn't finish in time");
		}
		

		return seqExport;

	}

	private class AddCoordinates implements Runnable {

		int[] coordinates;

		public AddCoordinates(int[] coordinates) {
			this.coordinates = coordinates;
		}

		@Override
		public void run() {
			seqExport.getImage(0, coordinates[2]).setData(coordinates[0], coordinates[1], 0, 255);
			progress.incPosition();
		}

	}

}
