package kovac.binaryMask;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import icy.gui.frame.progress.CancelableProgressFrame;
import icy.gui.frame.progress.ProgressFrame;
import icy.system.thread.ThreadUtil;
import icy.type.point.Point3D;
import kovac.res.quadric.QuadricExpression;

public class ConcurrentBoundingBoxTreatment extends Thread {

	private BoundingBox box;
	int[] bounds;
	QuadricExpression quad;
	ProgressFrame progress;

	public ConcurrentBoundingBoxTreatment(BoundingBox b) {
		box = b;
		progress = new CancelableProgressFrame("Handling stacks");
	}

	@Override
	public void run() {
		treatBox();
	}

	private void treatBox() {
		double[] preciseBounds = null;

		quad = box.getAssociatedEquation();
		preciseBounds = box.getBounds();
		bounds = new int[preciseBounds.length];

		for (int i = 0; i < preciseBounds.length; i++)
			bounds[i] = (int) preciseBounds[i];

		ExecutorService exec = ThreadUtil.createThreadPool("Handling Stacks");
		for (int z = bounds[4]; z < bounds[5]; z++) {
			exec.execute(new ConcurrentStackTreatment(z));
		}
		progress.setLength(bounds[5] - bounds[4]);
		exec.shutdown();
		try {
			exec.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
		} catch (InterruptedException e) {
			System.out.println("Threads didn't finish in time");
		}

	}

	private class ConcurrentStackTreatment extends Thread {

		int stack;

		public ConcurrentStackTreatment(int stack) {
			this.stack = stack;
		}

		@Override
		public void run() {

			Point3D testedPoint = null;

			for (int x = bounds[0]; x < bounds[1]; x++) {
				for (int y = bounds[2]; y < bounds[3]; y++) {

					if (x < bounds[0] || x > bounds[1])
						continue;
					if (y < bounds[2] || y > bounds[3])
						continue;
					if (stack < bounds[4] || stack > bounds[5])
						continue;

					testedPoint = new Point3D.Double(x, y, stack);

					if (quad.isInsideQuadric(testedPoint)) {
						BinaryMask.addCoordinates(new int[] { x, y, stack });
					}

				}

			}
			progress.incPosition();

		}

	}

}
